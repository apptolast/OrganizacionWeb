// @ts-nocheck
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { WorkSessionEndPanel } from "./work-session-end";
import { WorkSessionStatePanel } from "./work-session-state";
import { useWorkSessionDecision } from "./use-work-session-decision";
import { observeAccess } from "./api-client";

const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "Europe/Madrid",
};
const state = {
  session,
  status: "running" as const,
  revision: "1",
  changedAt: session.startedAt,
  runningSince: session.startedAt,
  workedMicroseconds: "0",
};
const snapshot = {
  state,
  serverNow: session.plannedEndAt,
  effectiveEndAt: session.plannedEndAt,
};
function response(value = snapshot) {
  return Response.json(value, {
    headers: {
      "Work-Session-Revision": `work-session-${session.id}-${value.state.revision}`,
    },
  });
}

it("@s42 withdraws an end snapshot with the known session id but a different task context", async () => {
  const access = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(
        response({
          ...snapshot,
          state: {
            ...state,
            session: {
              ...session,
              taskId: "62345678-1234-1234-1234-123456789abc",
            },
          },
        }),
      ),
  );
  render(<WorkSessionEndPanel session={session} onAccessFailure={access} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  await waitFor(() => expect(access).toHaveBeenCalledWith(404));
  expect(
    screen.queryByLabelText("Minutos adicionales"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Ampliar tiempo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
});

it("@s34 a later clock rollback does not withdraw an already confirmed notice for the same end", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      response({ ...snapshot, serverNow: session.startedAt }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByText("Ha llegado el fin acordado")).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByText("Comprobando el fin actual"),
    ).not.toBeInTheDocument(),
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByText("Ha llegado el fin acordado")).toBeVisible();
});
afterEach(() => {
  observeAccess(undefined);
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

it("@s41 starting a sibling pause aborts an older end lookup before its late HTTP 401 reaches the access observer", async () => {
  let finish!: (value: Response) => void;
  let endReads = 0;
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: snapshot.serverNow,
            netMicroseconds: "1500000000",
          },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      );
    if (url.endsWith("/end-time")) {
      if (++endReads === 1) return Promise.resolve(response());
      return new Promise<Response>((resolve) => {
        finish = resolve;
      });
    }
    return new Promise<Response>(() => {});
  });
  vi.stubGlobal("fetch", fetcher);
  const observer = vi.fn();
  const onAccessFailure = vi.fn();
  observeAccess(observer);
  render(<DecisionSurface onAccessFailure={onAccessFailure} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  await waitFor(() => expect(endReads).toBe(2));
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finish(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(onAccessFailure).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
  expect(screen.getByText("Procesando cambio de sesión")).toHaveAttribute(
    "role",
    "status",
  );
});

function DecisionSurface({
  onAccessFailure,
}: {
  onAccessFailure: (status: number) => void;
}) {
  const decision = useWorkSessionDecision();
  return (
    <>
      <WorkSessionStatePanel
        session={session}
        onAccessFailure={onAccessFailure}
        decision={decision}
      />
      <WorkSessionEndPanel
        session={session}
        onAccessFailure={onAccessFailure}
        decision={decision}
      />
    </>
  );
}

it("@s42 a current HTTP 401 withdraws the end and draft and reports loss of access", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 401 }));
  vi.stubGlobal("fetch", fetcher);
  const onAccessFailure = vi.fn();
  render(
    <WorkSessionEndPanel session={session} onAccessFailure={onAccessFailure} />,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  await waitFor(() => expect(onAccessFailure).toHaveBeenCalledWith(401));
  expect(
    screen.queryByLabelText("Minutos adicionales"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Fin previsto original:", { exact: false }),
  ).not.toBeInTheDocument();
});

it("@s41 starting an extension aborts an older sibling state lookup before its late HTTP 401 reaches the access observer", async () => {
  let finish!: (value: Response) => void;
  let stateReads = 0;
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/end-time")) return Promise.resolve(response());
    if (url.endsWith("/state")) {
      if (++stateReads === 1)
        return Promise.resolve(
          Response.json(
            {
              state,
              serverNow: snapshot.serverNow,
              netMicroseconds: "1500000000",
            },
            {
              headers: {
                "Work-Session-Revision": `work-session-${session.id}-1`,
              },
            },
          ),
        );
      return new Promise<Response>((resolve) => {
        finish = resolve;
      });
    }
    return new Promise<Response>(() => {});
  });
  vi.stubGlobal("fetch", fetcher);
  const observer = vi.fn();
  const onAccessFailure = vi.fn();
  observeAccess(observer);
  render(<DecisionSurface onAccessFailure={onAccessFailure} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  await screen.findByRole("button", { name: "Pausar" });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar estado de la sesión" }),
  );
  await waitFor(() => expect(stateReads).toBe(2));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finish(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(onAccessFailure).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
  expect(screen.getByText("Ampliando tiempo")).toHaveAttribute(
    "role",
    "status",
  );
});

it("@s40 a confirmed extension refreshes sibling state before pausing with the new revision", async () => {
  let current = state;
  let finishState!: (value: Response) => void;
  const receipt = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "EXTEND",
    occurredAt: session.plannedEndAt,
    before: state,
    after: { ...state, revision: "2" },
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  const fetcher = vi.fn<(url: string, init?: RequestInit) => Promise<Response>>(
    (url) => {
      const headers = {
        "Work-Session-Revision": `work-session-${session.id}-${current.revision}`,
      };
      if (url.endsWith("/state") && current.revision === "2")
        return new Promise<Response>((resolve) => {
          finishState = resolve;
        });
      if (url.endsWith("/state"))
        return Promise.resolve(
          Response.json(
            {
              state: current,
              serverNow: snapshot.serverNow,
              netMicroseconds: "1500000000",
            },
            { headers },
          ),
        );
      if (url.endsWith("/end-time"))
        return Promise.resolve(
          Response.json(
            {
              ...snapshot,
              state: current,
              effectiveEndAt:
                current.revision === "1"
                  ? session.plannedEndAt
                  : receipt.extension.effectiveEndAt,
            },
            { headers },
          ),
        );
      if (url.endsWith("/extend")) {
        current = receipt.after;
        return Promise.resolve(
          Response.json(receipt, {
            status: 201,
            headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
          }),
        );
      }
      return new Promise<Response>(() => {});
    },
  );
  vi.stubGlobal("fetch", fetcher);
  render(<DecisionSurface onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(await screen.findByText("Ampliación confirmada")).toBeVisible();
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([url]) => url.endsWith("/state")),
    ).toHaveLength(2),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/pause")),
  ).toHaveLength(0);
  expect(screen.getByRole("button", { name: "Pausar" })).toHaveAttribute(
    "aria-disabled",
    "true",
  );
  expect(screen.getByText("Consultando estado de la sesión")).toHaveAttribute(
    "role",
    "status",
  );
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finishState(
      Response.json(
        {
          state: receipt.after,
          serverNow: snapshot.serverNow,
          netMicroseconds: "1500000000",
        },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
  });
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([url]) => url.endsWith("/pause")),
    ).toHaveLength(1),
  );
  expect(
    fetcher.mock.calls.find(([url]) => url.endsWith("/pause"))?.[1]?.headers,
  ).toEqual(
    expect.objectContaining({
      "Work-Session-Revision": `work-session-${session.id}-2`,
    }),
  );
});

it("@s40 waits for the sibling end refresh after a pause before accepting another decision", async () => {
  let paused = false;
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1500000000",
    runningSince: null,
  };
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/end-time"))
      return paused
        ? new Promise<Response>(() => {})
        : Promise.resolve(response());
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state: paused ? after : state,
            serverNow: snapshot.serverNow,
            netMicroseconds: "1500000000",
          },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-${paused ? "2" : "1"}`,
            },
          },
        ),
      );
    if (url.endsWith("/pause")) {
      paused = true;
      const id = "42345678-1234-1234-1234-123456789abc";
      return Promise.resolve(
        Response.json(
          {
            id,
            action: "PAUSE",
            sessionId: session.id,
            occurredAt: snapshot.serverNow,
            before: state,
            after,
          },
          {
            status: 201,
            headers: { Location: `/api/v1/work-session-changes/${id}` },
          },
        ),
      );
    }
    return new Promise<Response>(() => {});
  });
  vi.stubGlobal("fetch", fetcher);
  render(<DecisionSurface onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByText("Pausa confirmada")).toBeVisible();
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
    ).toHaveLength(2),
  );
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/extend")),
  ).toHaveLength(0);
  expect(
    screen.getByRole("button", { name: "Confirmar ampliación" }),
  ).toHaveAttribute("aria-disabled", "true");
  expect(screen.getByText("Comprobando el fin actual")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
});

it("@s35 preserves the historical extension facts when the current end refresh fails", async () => {
  const receipt = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "EXTEND",
    occurredAt: session.plannedEndAt,
    before: state,
    after: { ...state, revision: "2" },
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    await screen.findByText("No se ha podido comprobar el fin actual."),
  ).toBeVisible();
  const receiptView = screen.getByRole("article", {
    name: "Ampliación guardada",
  });
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  expect(receiptView).toHaveTextContent("5 minutos adicionales");
  expect(
    Array.from(receiptView.querySelectorAll("time")).map(
      (element) => element.dateTime,
    ),
  ).toEqual([
    receipt.extension.previousEndAt,
    receipt.extension.effectiveEndAt,
  ]);
  expect(screen.getByText("Ampliación confirmada")).toHaveAttribute(
    "role",
    "status",
  );
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s43 moves focus to the panel heading when the focused submit disappears after an uncertain response", async () => {
  let finish!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  const submit = screen.getByRole("button", { name: "Confirmar ampliación" });
  submit.focus();
  fireEvent.click(submit);
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finish(new Response(null, { status: 503 }));
  });
  expect(
    await screen.findByRole("button", { name: "Comprobar ampliación" }),
  ).toBeVisible();
  expect(
    screen.getByRole("heading", { name: "Fin de la sesión" }),
  ).toHaveFocus();
});

it("@s43 does not reclaim focus after the user deliberately leaves the pending submit", async () => {
  let finish!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  const submit = screen.getByRole("button", { name: "Confirmar ampliación" });
  submit.focus();
  fireEvent.click(submit);
  submit.blur();
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finish(new Response(null, { status: 503 }));
  });
  expect(
    await screen.findByRole("button", { name: "Comprobar ampliación" }),
  ).toBeVisible();
  expect(document.body).toHaveFocus();
  fireEvent.click(screen.getByRole("button", { name: "Ampliar tiempo" }));
  expect(document.body).toHaveFocus();
});

it("@s38 permits a new end lookup after a pending refresh is aborted by an extension rejected with 412", async () => {
  let endReads = 0;
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/end-time")) {
      endReads += 1;
      if (endReads === 2) return new Promise<Response>(() => {});
      return Promise.resolve(
        response({
          ...snapshot,
          state: { ...state, revision: endReads === 1 ? "1" : "2" },
        }),
      );
    }
    if (url.endsWith("/extend"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:precondition_failed",
            title: "La sesión ha cambiado",
            status: 412,
            code: "PRECONDITION_FAILED",
          },
          { status: 412 },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  await waitFor(() => expect(endReads).toBe(2));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar fin actual" }),
  );
  await waitFor(() => expect(endReads).toBe(3));
  expect(
    await screen.findByRole("button", { name: "Confirmar ampliación" }),
  ).toBeVisible();
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
});

it("@s42 a current extension POST reporting a missing session withdraws private end and draft", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_not_found",
          title: "No se ha encontrado la sesión de trabajo.",
          status: 404,
          code: "WORK_SESSION_NOT_FOUND",
        },
        { status: 404 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  const onAccessFailure = vi.fn();
  render(
    <WorkSessionEndPanel session={session} onAccessFailure={onAccessFailure} />,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  await waitFor(() => expect(onAccessFailure).toHaveBeenCalledWith(404));
  expect(
    screen.queryByLabelText("Minutos adicionales"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Fin previsto original:", { exact: false }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Consultar fin actual" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s28 shows the confirmed due notice and both deliberate choices", async () => {
  const fetcher = vi.fn().mockResolvedValue(response());
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByText("Ha llegado el fin acordado")).toBeVisible();
  expect(
    screen.getByText("Fin previsto original:", { exact: false }),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Cerrar sesión de trabajo" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`,
  );
  expect(screen.getByRole("button", { name: "Ampliar tiempo" })).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s29 opens an empty amount without transmitting a decision", async () => {
  const fetcher = vi.fn().mockResolvedValue(response());
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(null);
  expect(
    screen.getByRole("button", { name: "Confirmar ampliación" }),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s35 confirms an explicit extension and refreshes the current end separately", async () => {
  const receipt = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "EXTEND",
    occurredAt: session.plannedEndAt,
    before: state,
    after: { ...state, revision: "2" },
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    )
    .mockResolvedValueOnce(
      response({
        ...snapshot,
        state: receipt.after,
        effectiveEndAt: receipt.extension.effectiveEndAt,
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(await screen.findByText("Ampliación confirmada")).toBeVisible();
  expect(
    await screen.findByText("Fin acordado actual:", { exact: false }),
  ).toBeVisible();
  await waitFor(() =>
    expect(
      screen.queryByText("Ha llegado el fin acordado"),
    ).not.toBeInTheDocument(),
  );
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(fetcher.mock.calls[1][1]).toEqual(
    expect.objectContaining({
      body: '{"additionalMinutes":5}',
      headers: expect.objectContaining({
        "Work-Session-Revision": `work-session-${session.id}-1`,
        "Idempotency-Key": expect.any(String),
      }),
    }),
  );
});

it("@s4 keeps an invalid empty amount local with an associated error", async () => {
  const fetcher = vi.fn().mockResolvedValue(response());
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  const input = screen.getByLabelText("Minutos adicionales");
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Introduce un número entero de minutos entre 1 y 1.440.",
  );
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(input).toHaveAccessibleDescription(
    "Introduce un número entero de minutos entre 1 y 1.440.",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s43 announces a pending POST and prevents a duplicate submit while keeping focus", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockReturnValueOnce(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  const input = screen.getByLabelText("Minutos adicionales");
  fireEvent.change(input, { target: { value: "5" } });
  const submit = screen.getByRole("button", { name: "Confirmar ampliación" });
  submit.focus();
  fireEvent.click(submit);
  fireEvent.submit(submit.closest("form")!);
  expect(screen.getByText("Ampliando tiempo")).toHaveAttribute(
    "role",
    "status",
  );
  expect(submit).toHaveFocus();
  expect(submit).toHaveAttribute("aria-disabled", "true");
  expect(input).toHaveAttribute("readonly");
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s37 retains an uncertain extension and checks its key without another POST", async () => {
  const receipt = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "EXTEND",
    occurredAt: session.plannedEndAt,
    before: state,
    after: { ...state, revision: "2" },
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(receipt))
    .mockResolvedValueOnce(
      response({
        ...snapshot,
        state: receipt.after,
        effectiveEndAt: receipt.extension.effectiveEndAt,
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  const check = await screen.findByRole("button", {
    name: "Comprobar ampliación",
  });
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(5);
  expect(screen.getByLabelText("Minutos adicionales")).toHaveAttribute(
    "readonly",
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar ampliación" }),
  ).not.toBeInTheDocument();
  expect(screen.getByText(/Salir no revoca/)).toBeVisible();
  fireEvent.click(check);
  expect(await screen.findByText("Ampliación confirmada")).toBeVisible();
  const key = fetcher.mock.calls[1][1].headers["Idempotency-Key"];
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-session-changes/by-request/${key}`,
  );
  expect(
    fetcher.mock.calls.filter(([, options]) => options.method === "POST"),
  ).toHaveLength(1);
});

it("@s37 keeps an unknown failed receipt lookup uncertain without offering resend or posting on Enter", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:unknown",
          status: 503,
          code: "UNKNOWN",
          title: "Unavailable",
        },
        { status: 503 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  const amount = screen.getByLabelText("Minutos adicionales");
  fireEvent.change(amount, { target: { value: "5" } });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar ampliación" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByText("Comprobando ampliación"),
    ).not.toBeInTheDocument(),
  );
  expect(
    screen.queryByRole("button", { name: "Reenviar ampliación" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Comprobar ampliación" }),
  ).toBeVisible();
  const { default: userEvent } = await import("@testing-library/user-event");
  const user = userEvent.setup();
  await user.click(amount);
  await user.keyboard("{Enter}");
  fireEvent.submit(amount.closest("form")!);
  expect(amount).toHaveValue(5);
  expect(
    fetcher.mock.calls.filter(([, options]) => options.method === "POST"),
  ).toHaveLength(1);
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s34 presents a failed initial end query as an error and retries with loading", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockReturnValueOnce(pending);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido comprobar el fin actual.",
  );
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  const retry = screen.getByRole("button", {
    name: "Reintentar consulta del fin",
  });
  retry.focus();
  fireEvent.click(retry);
  fireEvent.click(retry);
  expect(screen.getByText("Comprobando el fin actual")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(retry).toHaveFocus();
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  finish(response());
  expect(await screen.findByText("Ha llegado el fin acordado")).toBeVisible();
});

it("@s39 a closed snapshot keeps its end but offers no extension or due notice", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      response({
        ...snapshot,
        state: { ...state, status: "closed", runningSince: null },
      } as unknown as typeof snapshot),
    ),
  );
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  expect(
    await screen.findByText("Fin previsto original:", { exact: false }),
  ).toBeVisible();
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Ampliar tiempo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("link", { name: "Cerrar sesión de trabajo" }),
  ).not.toBeInTheDocument();
});

it("@s30 the monotonic deadline queries the server before announcing a due end", async () => {
  vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
  let monotonic = 50;
  vi.spyOn(performance, "now").mockImplementation(() => monotonic);
  let finish!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      response({ ...snapshot, serverNow: "2026-09-07T10:24:59.123456Z" }),
    )
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  const { act } = await import("@testing-library/react");
  await act(async () => {
    render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  });
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  monotonic = 1050;
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1000);
  });
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByText("Comprobando el fin actual")).toHaveAttribute(
    "role",
    "status",
  );
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  await act(async () => {
    finish(response());
  });
  expect(screen.getByText("Ha llegado el fin acordado")).toBeVisible();
});

it("@s31 fragments a 25-day deadline without an early request", async () => {
  vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
  const scheduled = vi.spyOn(globalThis, "setTimeout");
  let monotonic = 0;
  vi.spyOn(performance, "now").mockImplementation(() => monotonic);
  const fetcher = vi.fn().mockResolvedValueOnce(
    response({
      ...snapshot,
      serverNow: "2026-09-07T10:25:00.123456Z",
      effectiveEndAt: "2026-10-02T10:25:00.123456Z",
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const { act } = await import("@testing-library/react");
  await act(async () => {
    render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  });
  expect(scheduled.mock.calls.at(-1)?.[1]).toBe(2147483647);
  monotonic = 2147483647;
  await act(async () => {
    await vi.advanceTimersByTimeAsync(2147483647);
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  expect(vi.getTimerCount()).toBe(1);
});

it("@s32 a remaining microsecond uses a positive millisecond rather than a zero-delay loop", async () => {
  vi.useFakeTimers({ toFake: ["setTimeout", "clearTimeout"] });
  vi.spyOn(performance, "now").mockReturnValue(0);
  const scheduled = vi.spyOn(globalThis, "setTimeout");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      response({ ...snapshot, serverNow: "2026-09-07T10:25:00.123455Z" }),
    );
  vi.stubGlobal("fetch", fetcher);
  const { act } = await import("@testing-library/react");
  await act(async () => {
    render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  });
  expect(scheduled.mock.calls.at(-1)?.[1]).toBe(1);
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1);
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(scheduled.mock.calls.at(-1)?.[1]).toBe(1);
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
});

it("@s33 returning visible checks once and coalesces another visibility event", async () => {
  let finish!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  const visibility = vi
    .spyOn(document, "visibilityState", "get")
    .mockReturnValue("hidden");
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  await screen.findByText("Ha llegado el fin acordado");
  visibility.mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  fireEvent(document, new Event("visibilitychange"));
  expect(screen.getByText("Comprobando el fin actual")).toHaveAttribute(
    "role",
    "status",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  finish(response());
  await waitFor(() =>
    expect(
      screen.queryByText("Comprobando el fin actual"),
    ).not.toBeInTheDocument(),
  );
  visibility.mockRestore();
});

it("@s41 an old end HTTP401 after navigation cannot revoke the new context", async () => {
  let finish!: (value: Response) => void;
  const nextSession = {
    ...session,
    id: "62345678-1234-1234-1234-123456789abc",
  };
  const nextSnapshot = {
    ...snapshot,
    state: { ...state, session: nextSession },
  };
  const fetcher = vi
    .fn()
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    )
    .mockResolvedValueOnce(
      Response.json(nextSnapshot, {
        headers: {
          "Work-Session-Revision": `work-session-${nextSession.id}-1`,
        },
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  const { observeAccess } = await import("./api-client");
  const observer = vi.fn();
  observeAccess(observer);
  const onAccessFailure = vi.fn();
  const view = render(
    <WorkSessionEndPanel session={session} onAccessFailure={onAccessFailure} />,
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(1));
  view.rerender(
    <WorkSessionEndPanel
      session={nextSession}
      onAccessFailure={onAccessFailure}
    />,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finish(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(onAccessFailure).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  observeAccess(undefined);
});

it("@s42 a current missing-session response removes private draft and end", async () => {
  const missing = Response.json(
    {
      type: "urn:organization:problem:work_session_not_found",
      title: "Sesión no encontrada",
      status: 404,
      code: "WORK_SESSION_NOT_FOUND",
    },
    { status: 404 },
  );
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValueOnce(response()).mockResolvedValueOnce(missing),
  );
  const onAccessFailure = vi.fn();
  render(
    <WorkSessionEndPanel session={session} onAccessFailure={onAccessFailure} />,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  await waitFor(() => expect(onAccessFailure).toHaveBeenCalledWith(404));
  expect(
    screen.queryByLabelText("Minutos adicionales"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Ha llegado el fin acordado"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Fin previsto original:", { exact: false }),
  ).not.toBeInTheDocument();
});

it("@s37 only a recognized missing receipt permits a manual identical resend", async () => {
  const missing = Response.json(
    {
      type: "urn:organization:problem:work_session_change_not_found",
      title: "Cambio no encontrado",
      status: 404,
      code: "WORK_SESSION_CHANGE_NOT_FOUND",
    },
    { status: 404 },
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(missing)
    .mockReturnValueOnce(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar ampliación" }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar ampliación",
  });
  expect(fetcher).toHaveBeenCalledTimes(3);
  fireEvent.click(resend);
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(4));
  expect(fetcher.mock.calls[3][1].body).toBe(fetcher.mock.calls[1][1].body);
  expect(fetcher.mock.calls[3][1].headers).toEqual(
    fetcher.mock.calls[1][1].headers,
  );
});

it("@s37 recognized CSRF permits only a later manual resend with the renewed token", async () => {
  const { setCsrfToken } = await import("./api-client");
  setCsrfToken("before");
  const csrf = Response.json(
    {
      type: "urn:organization:problem:csrf_invalid",
      title: "Acceso caducado",
      status: 403,
      code: "CSRF_INVALID",
    },
    { status: 403 },
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(csrf)
    .mockReturnValueOnce(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  const resend = await screen.findByRole("button", {
    name: "Reenviar ampliación",
  });
  setCsrfToken("renewed");
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(resend);
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  const original = new Headers(fetcher.mock.calls[1][1].headers);
  const repeated = new Headers(fetcher.mock.calls[2][1].headers);
  expect(repeated.get("X-CSRF-TOKEN")).toBe("renewed");
  expect(repeated.get("Idempotency-Key")).toBe(original.get("Idempotency-Key"));
  expect(repeated.get("Work-Session-Revision")).toBe(
    original.get("Work-Session-Revision"),
  );
  expect(fetcher.mock.calls[2][1].body).toBe(fetcher.mock.calls[1][1].body);
  setCsrfToken(undefined);
});

it("@s41 a pending extension HTTP401 is ignored after changing the session", async () => {
  let finish!: (value: Response) => void;
  const nextSession = {
    ...session,
    id: "62345678-1234-1234-1234-123456789abc",
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...snapshot, state: { ...state, session: nextSession } },
        {
          headers: {
            "Work-Session-Revision": `work-session-${nextSession.id}-1`,
          },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  const { observeAccess } = await import("./api-client");
  const observer = vi.fn();
  observeAccess(observer);
  const view = render(
    <WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  view.rerender(
    <WorkSessionEndPanel session={nextSession} onAccessFailure={vi.fn()} />,
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  const { act } = await import("@testing-library/react");
  await act(async () => {
    finish(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
  expect(
    screen.queryByText("No podemos confirmar la ampliación."),
  ).not.toBeInTheDocument();
  observeAccess(undefined);
});

it("@s38 a 412 keeps the amount but requires a fresh state and a new manual intention", async () => {
  const conflict = Response.json(
    {
      type: "urn:organization:problem:precondition_failed",
      title: "La sesión cambió",
      status: 412,
      code: "PRECONDITION_FAILED",
    },
    { status: 412 },
  );
  let finish!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(conflict)
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
    )
    .mockReturnValueOnce(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionEndPanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar fin actual" }),
  );
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
  expect(
    screen.queryByRole("button", { name: "Confirmar ampliación" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
  finish(response({ ...snapshot, state: { ...state, revision: "2" } }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Confirmar ampliación" }),
  );
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(4));
  expect(fetcher.mock.calls[3][1].body).toBe('{"additionalMinutes":7}');
  expect(fetcher.mock.calls[3][1].headers["Work-Session-Revision"]).toBe(
    `work-session-${session.id}-2`,
  );
  expect(fetcher.mock.calls[3][1].headers["Idempotency-Key"]).not.toBe(
    fetcher.mock.calls[1][1].headers["Idempotency-Key"],
  );
});

it("@s36 a pending pause blocks an extension in its sibling panel", async () => {
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/end-time")) return Promise.resolve(response());
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: snapshot.serverNow,
            netMicroseconds: "1500000000",
          },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      );
    return new Promise<Response>(() => {});
  });
  vi.stubGlobal("fetch", fetcher);
  const { WorkSessionStatePanel } = await import("./work-session-state");
  const { useWorkSessionDecision } =
    await import("./use-work-session-decision");
  const onAccessFailure = vi.fn();
  function Surface() {
    const decision = useWorkSessionDecision();
    return (
      <>
        <WorkSessionStatePanel
          session={session}
          onAccessFailure={onAccessFailure}
          decision={decision}
        />
        <WorkSessionEndPanel
          session={session}
          onAccessFailure={onAccessFailure}
          decision={decision}
        />
      </>
    );
  }
  render(<Surface />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    screen.getByRole("button", { name: "Confirmar ampliación" }),
  ).toHaveAttribute("aria-disabled", "true");
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/pause")),
  ).toHaveLength(1);
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/extend")),
  ).toHaveLength(0);
});

it("@s40 a confirmed pause refreshes the sibling end before an extension uses its new revision", async () => {
  const paused = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1500000000",
    runningSince: null,
  };
  let current = state as typeof state | typeof paused;
  const fetcher = vi.fn((url: string) => {
    const headers = {
      "Work-Session-Revision": `work-session-${session.id}-${current.revision}`,
    };
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json({ ...snapshot, state: current }, { headers }),
      );
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state: current,
            serverNow: snapshot.serverNow,
            netMicroseconds: "1500000000",
          },
          { headers },
        ),
      );
    if (url.endsWith("/pause")) {
      current = paused;
      const id = "423e4567-e89b-42d3-a456-426614174000";
      return Promise.resolve(
        Response.json(
          {
            id,
            action: "PAUSE",
            sessionId: session.id,
            occurredAt: snapshot.serverNow,
            before: state,
            after: paused,
          },
          {
            status: 201,
            headers: { Location: `/api/v1/work-session-changes/${id}` },
          },
        ),
      );
    }
    return new Promise<Response>(() => {});
  });
  vi.stubGlobal("fetch", fetcher);
  const { WorkSessionStatePanel } = await import("./work-session-state");
  const { useWorkSessionDecision } =
    await import("./use-work-session-decision");
  const onAccessFailure = vi.fn();
  function Surface() {
    const decision = useWorkSessionDecision();
    return (
      <>
        <WorkSessionStatePanel
          session={session}
          onAccessFailure={onAccessFailure}
          decision={decision}
        />
        <WorkSessionEndPanel
          session={session}
          onAccessFailure={onAccessFailure}
          decision={decision}
        />
      </>
    );
  }
  render(<Surface />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "7" },
  });
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByText("Pausa confirmada")).toBeVisible();
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([url]) => url.endsWith("/end-time")),
    ).toHaveLength(2),
  );
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(([url]) => url.endsWith("/extend")),
    ).toHaveLength(1),
  );
  const call = fetcher.mock.calls.find(([url]) => url.endsWith("/extend"));
  expect((call as unknown as [string, RequestInit])[1].headers).toEqual(
    expect.objectContaining({
      "Work-Session-Revision": `work-session-${session.id}-2`,
    }),
  );
  expect(screen.getByLabelText("Minutos adicionales")).toHaveValue(7);
});
