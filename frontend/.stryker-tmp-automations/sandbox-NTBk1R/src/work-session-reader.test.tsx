// @ts-nocheck
import { setCsrfToken, observeAccess } from "./api-client";
import { App } from "./App";
import { afterEach, expect, it, vi } from "vitest";
import {
  cleanup,
  render,
  screen,
  fireEvent,
  act,
  waitFor,
} from "@testing-library/react";
import { WorkSessionReader } from "./work-session-reader";
const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "UTC",
};
const before = {
  session,
  status: "running",
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const closed = {
  ...before,
  status: "closed",
  revision: "2",
  changedAt: "2026-09-07T10:00:01.123457Z",
  workedMicroseconds: "1000001",
  runningSince: null,
};
const closure = {
  progressNote: "Avance parcial",
  nextStep: "Continuar mañana",
  workDate: "2026-09-07",
  closeZoneId: "UTC",
};
const receipt = {
  id: "42345678-1234-1234-1234-123456789abc",
  sessionId: session.id,
  action: "CLOSE",
  occurredAt: closed.changedAt,
  before,
  after: closed,
  closure,
};
const props = {
  id: session.id,
  projectId: session.projectId,
  taskId: session.taskId,
};

// Legacy closure oracles observe their S/F/C/K/A requests; E has its own routed fixture.
// Feature17 composition cases below install the complete fetch dispatcher directly.
function stubClosureRequests(
  request: (url: string, init?: RequestInit) => unknown,
) {
  vi.stubGlobal("fetch", (url: string, init?: RequestInit) => {
    const match = /^\/api\/v1\/work-sessions\/([^/]+)\/end-time$/.exec(url);
    if (match) {
      const current = { ...before, session: { ...session, id: match[1] } };
      return Promise.resolve(
        Response.json(
          {
            state: current,
            serverNow: session.startedAt,
            effectiveEndAt: session.plannedEndAt,
          },
          {
            headers: { "Work-Session-Revision": `work-session-${match[1]}-1` },
          },
        ),
      );
    }
    return request(url, init);
  });
}

it("@s28 feature17 offers the end notice and a separate extension form on the known session route", async () => {
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state: before,
            serverNow: session.plannedEndAt,
            netMicroseconds: "1500000000",
          },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: before,
            serverNow: session.plannedEndAt,
            effectiveEndAt: session.plannedEndAt,
          },
          { headers },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  expect(await screen.findByText("Ha llegado el fin acordado")).toBeVisible();
  expect(
    screen.getByRole("heading", { name: "Fin de la sesión", level: 2 }),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Ampliar tiempo" }));
  const amount = screen.getByLabelText("Minutos adicionales");
  expect(amount).toHaveValue(null);
  expect(amount.closest("form")).not.toBe(
    screen.getByRole("button", { name: "Confirmar cierre" }).closest("form"),
  );
  expect(fetcher.mock.calls.map(([url]) => url)).toEqual([
    `/api/v1/work-sessions/${session.id}/state`,
    `/api/v1/work-sessions/${session.id}/end-time`,
  ]);
});

function closedResponse() {
  return Response.json(
    {
      state: closed,
      serverNow: "2026-09-08T12:00:00Z",
      netMicroseconds: "1000001",
    },
    { headers: { "Work-Session-Revision": `work-session-${session.id}-2` } },
  );
}

it("@s36 feature17 releases a definitively rejected closure so the user can choose extension after refresh", async () => {
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  let extensions = 0;
  let deliverEnd!: () => void;
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return new Promise<Response>((resolve) => {
        deliverEnd = () =>
          resolve(
            Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            ),
          );
      });
    if (url.endsWith("/close"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:precondition_failed",
            title: "Conflict",
            status: 412,
            code: "PRECONDITION_FAILED",
          },
          { status: 412 },
        ),
      );
    if (url.endsWith("/extend")) {
      extensions++;
      return new Promise<Response>(() => {});
    }
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Avance conservado" },
  });
  await waitFor(() => expect(deliverEnd).toBeTypeOf("function"));
  await act(async () => deliverEnd());
  expect(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));

  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(extensions).toBe(1);
  expect(screen.getByLabelText("Avance anotado (opcional)")).toHaveValue(
    "Avance conservado",
  );
});

it("@s36 feature17 releases a definitively rejected extension so the user can choose closure", async () => {
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  let closes = 0;
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: before,
            serverNow: session.startedAt,
            effectiveEndAt: session.plannedEndAt,
          },
          { headers },
        ),
      );
    if (url.endsWith("/extend"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:precondition_failed",
            title: "Conflict",
            status: 412,
            code: "PRECONDITION_FAILED",
          },
          { status: 412 },
        ),
      );
    if (url.endsWith("/close")) {
      closes++;
      return new Promise<Response>(() => {});
    }
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Avance conservado" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar fin actual" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Consultar fin actual" }),
    ).not.toBeInTheDocument(),
  );
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(closes).toBe(1);
  expect(screen.getByLabelText("Avance anotado (opcional)")).toHaveValue(
    "Avance conservado",
  );
});

it("@s41 feature17 invalidates the reader state before a sibling extension can receive an old HTTP401", async () => {
  const extended = { ...before, revision: "2" };
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  let stateReads = 0;
  let posts = 0;
  let finishState!: (value: Response) => void;
  const access = vi.fn();
  observeAccess(access);
  vi.stubGlobal("fetch", (url: string) => {
    const state = posts ? extended : before;
    const headers = {
      "Work-Session-Revision": `work-session-${session.id}-${state.revision}`,
    };
    if (url.endsWith("/state")) {
      if (++stateReads === 2)
        return new Promise<Response>((resolve) => {
          finishState = resolve;
        });
      return Promise.resolve(
        Response.json(
          { state, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    }
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: session.startedAt,
            effectiveEndAt: posts
              ? extension.extension.effectiveEndAt
              : session.plannedEndAt,
          },
          { headers },
        ),
      );
    if (url.endsWith("/extend"))
      return ++posts === 1
        ? Promise.resolve(
            Response.json(extension, {
              status: 201,
              headers: {
                Location: `/api/v1/work-session-changes/${extension.id}`,
              },
            }),
          )
        : new Promise<Response>(() => {});
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Conservar borrador" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(await screen.findByText("Ampliación confirmada")).toBeVisible();
  await waitFor(() => expect(stateReads).toBe(2));
  fireEvent.click(screen.getByRole("button", { name: "Ampliar tiempo" }));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  await waitFor(() => expect(posts).toBe(2));
  await act(async () => {
    finishState(new Response(null, { status: 401 }));
  });
  expect(access).not.toHaveBeenCalled();
  expect(screen.getByLabelText("Avance anotado (opcional)")).toHaveValue(
    "Conservar borrador",
  );
  expect(screen.getByText("Ampliación confirmada")).toBeVisible();
});

it("@s39 feature17 retires end controls immediately after its close receipt while the end refresh is pending", async () => {
  let endReads = 0;
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return ++endReads === 1
        ? Promise.resolve(
            Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            ),
          )
        : new Promise<Response>(() => {});
    if (url.endsWith("/close"))
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        }),
      );
    if (url.endsWith("/active"))
      return Promise.resolve(
        Response.json({
          session: { ...session, id: "62345678-1234-1234-1234-123456789abc" },
        }),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.change(screen.getByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Ampliar tiempo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByLabelText("Minutos adicionales"),
  ).not.toBeInTheDocument();
  expect(await screen.findByText("Hay otra sesión abierta.")).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(endReads).toBe(2);
  expect(
    screen.queryByText("Consultando sesión de trabajo"),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Comprobando el fin actual")).toHaveAttribute(
    "role",
    "status",
  );
});

it("@s42 feature17 cannot restore a pending closure after the current end lookup withdraws access", async () => {
  let finishClose!: (value: Response) => void;
  let reads = 0;
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        ++reads === 1
          ? Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            )
          : Response.json(
              {
                type: "urn:organization:problem:work_session_not_found",
                title: "Missing",
                status: 404,
                code: "WORK_SESSION_NOT_FOUND",
              },
              { status: 404 },
            ),
      );
    if (url.endsWith("/close"))
      return new Promise<Response>((resolve) => {
        finishClose = resolve;
      });
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  await screen.findByRole("button", { name: "Ampliar tiempo" });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar fin acordado" }),
  );
  expect(
    await screen.findByText("Esta sesión no está disponible en esta tarea."),
  ).toBeVisible();
  await act(async () => {
    finishClose(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  });
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
});

it("@s42 feature17 cannot restore a pending state after the current end lookup withdraws access", async () => {
  const extended = { ...before, revision: "2" };
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  let changed = false;
  let endReads = 0;
  let finishState!: (value: Response) => void;
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state"))
      return changed
        ? new Promise<Response>((resolve) => {
            finishState = resolve;
          })
        : Promise.resolve(
            Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                netMicroseconds: "0",
              },
              { headers },
            ),
          );
    if (url.endsWith("/end-time")) {
      endReads++;
      return Promise.resolve(
        changed
          ? Response.json(
              {
                type: "urn:organization:problem:work_session_not_found",
                title: "Missing",
                status: 404,
                code: "WORK_SESSION_NOT_FOUND",
              },
              { status: 404 },
            )
          : Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            ),
      );
    }
    if (url.endsWith("/extend")) {
      changed = true;
      return Promise.resolve(
        Response.json(extension, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${extension.id}` },
        }),
      );
    }
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    await screen.findByText("Esta sesión no está disponible en esta tarea."),
  ).toBeVisible();
  expect(finishState).toBeTypeOf("function");
  await act(async () => {
    finishState(
      Response.json(
        { state: extended, serverNow: session.startedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
  });
  expect(
    screen.queryByLabelText("Avance anotado (opcional)"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Ampliar tiempo" }),
  ).not.toBeInTheDocument();
  expect(endReads).toBe(2);
});

it("@s35 feature17 preserves its extension receipt when a later closure is definitively rejected", async () => {
  const extended = { ...before, revision: "2" };
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  let changed = false;
  vi.stubGlobal("fetch", (url: string) => {
    const state = changed ? extended : before;
    const headers = {
      "Work-Session-Revision": `work-session-${session.id}-${state.revision}`,
    };
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: session.startedAt,
            effectiveEndAt: changed
              ? extension.extension.effectiveEndAt
              : session.plannedEndAt,
          },
          { headers },
        ),
      );
    if (url.endsWith("/extend")) {
      changed = true;
      return Promise.resolve(
        Response.json(extension, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${extension.id}` },
        }),
      );
    }
    if (url.endsWith("/close"))
      return Promise.resolve(
        Response.json(
          {
            type: "urn:organization:problem:precondition_failed",
            title: "Conflict",
            status: 412,
            code: "PRECONDITION_FAILED",
          },
          { status: 412 },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    await screen.findByRole("article", { name: "Ampliación guardada" }),
  ).toBeVisible();
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Confirmar cierre" }),
    ).toHaveAttribute("aria-disabled", "false"),
  );
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  ).toBeVisible();
  expect(
    screen.getByRole("article", { name: "Ampliación guardada" }),
  ).toBeVisible();
  fireEvent.click(
    screen.getByRole("button", { name: "Consultar estado actual" }),
  );
  expect(
    await screen.findByRole("button", { name: "Confirmar cierre" }),
  ).toBeVisible();
  expect(
    screen.getByRole("article", { name: "Ampliación guardada" }),
  ).toBeVisible();
});

it("@s42 feature17 withdraws the reader draft and receipt when its fresh state belongs to another task", async () => {
  const extended = { ...before, revision: "2" };
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  let changed = false;
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        changed
          ? Response.json(
              {
                state: {
                  ...before,
                  session: {
                    ...session,
                    taskId: "62345678-1234-1234-1234-123456789abc",
                  },
                },
                serverNow: session.startedAt,
                netMicroseconds: "0",
              },
              { headers },
            )
          : Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                netMicroseconds: "0",
              },
              { headers },
            ),
      );
    if (url.endsWith("/end-time"))
      return changed
        ? new Promise<Response>(() => {})
        : Promise.resolve(
            Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            ),
          );
    if (url.endsWith("/extend")) {
      changed = true;
      return Promise.resolve(
        Response.json(extension, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${extension.id}` },
        }),
      );
    }
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Nota privada" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    await screen.findByText("Esta sesión no está disponible en esta tarea."),
  ).toBeVisible();
  expect(
    screen.queryByLabelText("Avance anotado (opcional)"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("article", { name: "Ampliación guardada" }),
  ).not.toBeInTheDocument();
});

it("@s42 feature17 withdraws the reader draft and receipt when its fresh state reports a missing session", async () => {
  const extended = { ...before, revision: "2" };
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  let changed = false;
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        changed
          ? Response.json(
              {
                type: "urn:organization:problem:work_session_not_found",
                title: "Missing",
                status: 404,
                code: "WORK_SESSION_NOT_FOUND",
              },
              { status: 404 },
            )
          : Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                netMicroseconds: "0",
              },
              { headers },
            ),
      );
    if (url.endsWith("/end-time"))
      return changed
        ? new Promise<Response>(() => {})
        : Promise.resolve(
            Response.json(
              {
                state: before,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            ),
          );
    if (url.endsWith("/extend")) {
      changed = true;
      return Promise.resolve(
        Response.json(extension, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${extension.id}` },
        }),
      );
    }
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Nota privada" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    await screen.findByText("Esta sesión no está disponible en esta tarea."),
  ).toBeVisible();
  expect(
    screen.queryByLabelText("Avance anotado (opcional)"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("article", { name: "Ampliación guardada" }),
  ).not.toBeInTheDocument();
});

it("@s39 feature17 withdraws open controls when refreshed state discovers external closure before its receipt arrives", async () => {
  const extended = { ...before, revision: "2" };
  const finalState = { ...closed, revision: "3" };
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  let changed = false;
  let closureReads = 0;
  vi.stubGlobal("fetch", (url: string) => {
    const state = changed ? finalState : before;
    const headers = {
      "Work-Session-Revision": `work-session-${session.id}-${state.revision}`,
    };
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state,
            serverNow: state.changedAt,
            netMicroseconds: state.workedMicroseconds,
          },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return changed
        ? new Promise<Response>(() => {})
        : Promise.resolve(
            Response.json(
              {
                state,
                serverNow: session.startedAt,
                effectiveEndAt: session.plannedEndAt,
              },
              { headers },
            ),
          );
    if (url.endsWith("/extend")) {
      changed = true;
      return Promise.resolve(
        Response.json(extension, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${extension.id}` },
        }),
      );
    }
    if (url.endsWith("/closure")) {
      closureReads++;
      return new Promise<Response>(() => {});
    }
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  await waitFor(() => expect(closureReads).toBe(1));
  expect(
    screen.queryByRole("button", { name: "Confirmar cierre" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Ampliar tiempo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("article", { name: "Ampliación guardada" }),
  ).toBeVisible();
  expect(screen.getByText("Consultando sesión de trabajo")).toHaveAttribute(
    "role",
    "status",
  );
});

it("@s39 feature17 reloads the durable agreed end of a closed session without offering another extension", async () => {
  const finalReceipt = {
    ...receipt,
    before: { ...before, revision: "2" },
    after: { ...closed, revision: "3" },
  };
  const headers = { "Work-Session-Revision": `work-session-${session.id}-3` };
  const effectiveEndAt = "2026-09-07T10:30:00.123456Z";
  const fetcher = vi.fn((url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          {
            state: finalReceipt.after,
            serverNow: closed.changedAt,
            netMicroseconds: closed.workedMicroseconds,
          },
          { headers },
        ),
      );
    if (url.endsWith("/closure"))
      return Promise.resolve(Response.json(finalReceipt));
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: finalReceipt.after,
            serverNow: closed.changedAt,
            effectiveEndAt,
          },
          { headers },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  expect(
    await screen.findByText("Fin acordado actual:", { exact: false }),
  ).toBeVisible();
  expect(
    screen
      .getByText("Fin acordado actual:", { exact: false })
      .querySelector("time"),
  ).toHaveAttribute("datetime", effectiveEndAt);
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Ampliar tiempo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Confirmar cierre" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s40 feature17 refreshes the reader state after extension and waits before closing with its new revision", async () => {
  const extended = { ...before, revision: "2" };
  let current = before;
  let finishState!: (value: Response) => void;
  let stateReads = 0;
  const extension = {
    id: "42345678-1234-1234-1234-123456789abc",
    action: "EXTEND",
    sessionId: session.id,
    occurredAt: session.startedAt,
    before,
    after: extended,
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
      if (url.endsWith("/state")) {
        if (++stateReads === 2)
          return new Promise<Response>((resolve) => {
            finishState = resolve;
          });
        return Promise.resolve(
          Response.json(
            {
              state: current,
              serverNow: session.startedAt,
              netMicroseconds: "0",
            },
            { headers },
          ),
        );
      }
      if (url.endsWith("/end-time"))
        return Promise.resolve(
          Response.json(
            {
              state: current,
              serverNow: session.startedAt,
              effectiveEndAt:
                current.revision === "1"
                  ? session.plannedEndAt
                  : extension.extension.effectiveEndAt,
            },
            { headers },
          ),
        );
      if (url.endsWith("/extend")) {
        current = extended;
        return Promise.resolve(
          Response.json(extension, {
            status: 201,
            headers: {
              Location: `/api/v1/work-session-changes/${extension.id}`,
            },
          }),
        );
      }
      return new Promise<Response>(() => {});
    },
  );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Borrador conservado" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(await screen.findByText("Ampliación confirmada")).toBeVisible();
  await waitFor(() => expect(stateReads).toBe(2));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/close")),
  ).toHaveLength(0);
  expect(
    screen.getByRole("button", { name: "Confirmar cierre" }),
  ).toHaveAttribute("aria-disabled", "true");
  expect(screen.getByText("Consultando sesión de trabajo")).toHaveAttribute(
    "role",
    "status",
  );
  await act(async () => {
    finishState(
      Response.json(
        { state: extended, serverNow: session.startedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const closeRequest = fetcher.mock.calls.find(([url]) =>
    url.endsWith("/close"),
  )?.[1];
  expect(closeRequest?.headers).toEqual(
    expect.objectContaining({
      "Work-Session-Revision": `work-session-${session.id}-2`,
    }),
  );
  expect(JSON.parse(String(closeRequest?.body))).toEqual({
    progressNote: "Borrador conservado",
    nextStep: "",
  });
});

it("@s36 feature17 an uncertain extension keeps closure disabled and its draft intact", async () => {
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  const fetcher = vi.fn((url: string, init?: RequestInit) => {
    if (init?.method === "POST")
      return Promise.resolve(new Response(null, { status: 503 }));
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: before,
            serverNow: session.startedAt,
            effectiveEndAt: session.plannedEndAt,
          },
          { headers },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Borrador conservado" },
  });
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    await screen.findByText("No podemos confirmar la ampliación."),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/close")),
  ).toHaveLength(0);
  expect(
    screen.getByRole("button", { name: "Confirmar cierre" }),
  ).toHaveAttribute("aria-disabled", "true");
  expect(screen.getByLabelText("Avance anotado (opcional)")).toHaveValue(
    "Borrador conservado",
  );
});

it("@s36 feature17 a pending close prevents an extension from the separate form", async () => {
  const headers = { "Work-Session-Revision": `work-session-${session.id}-1` };
  const fetcher = vi.fn((url: string, init?: RequestInit) => {
    if (init?.method === "POST") return new Promise<Response>(() => {});
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: session.startedAt, netMicroseconds: "0" },
          { headers },
        ),
      );
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: before,
            serverNow: session.startedAt,
            effectiveEndAt: session.plannedEndAt,
          },
          { headers },
        ),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.click(
    await screen.findByRole("button", { name: "Ampliar tiempo" }),
  );
  fireEvent.change(screen.getByLabelText("Minutos adicionales"), {
    target: { value: "5" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(screen.getByRole("button", { name: "Confirmar ampliación" }));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/extend")),
  ).toHaveLength(0);
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/close")),
  ).toHaveLength(1);
  expect(
    screen.getByRole("button", { name: "Confirmar ampliación" }),
  ).toHaveAttribute("aria-disabled", "true");
});
afterEach(() => {
  cleanup();
  setCsrfToken(undefined);
  observeAccess(undefined);
  vi.unstubAllGlobals();
});
it("@s33 recovers the closure by known session URL without an active session or key", async () => {
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url.endsWith("/state")) return Promise.resolve(closedResponse());
    if (url.endsWith("/closure"))
      return Promise.resolve(Response.json(receipt));
    throw new Error(`Unexpected request ${url}`);
  });
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(screen.getByText(closure.nextStep)).toBeVisible();
  expect(screen.getByText("Tiempo de trabajo total: 1,000001 s")).toBeVisible();
  expect(screen.getByText("Día atribuido: 2026-09-07")).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s33 rejects a session snapshot belonging to another task route", async () => {
  const fetcher = vi.fn().mockResolvedValue(closedResponse());
  stubClosureRequests(fetcher);
  render(
    <WorkSessionReader
      {...props}
      taskId="62345678-1234-1234-1234-123456789abc"
    />,
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Esta sesión no está disponible en esta tarea.",
  );
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(
    screen.queryByText("Consultando sesión de trabajo"),
  ).not.toBeInTheDocument();
});
it("@s27 retries a failed closure lookup without claiming a missing closure", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(closedResponse())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(closedResponse())
    .mockResolvedValueOnce(Response.json(receipt));
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha podido consultar la sesión de trabajo.",
  );
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar lectura" }));
  expect(await screen.findByText(closure.progressNote)).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(4);
});
it("@s33 opens the stable session URL through App after reload", async () => {
  const previous = window.location.pathname;
  window.history.replaceState(
    null,
    "",
    `/proyectos/${props.projectId}/tareas/${props.taskId}/sesiones/${props.id}`,
  );
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(Response.json(receipt)),
  );
  try {
    render(<App />);
    expect(await screen.findByText(closure.nextStep)).toBeVisible();
    expect(
      screen.getByRole("link", { name: "Volver a la tarea" }),
    ).toHaveAttribute(
      "href",
      `/proyectos/${props.projectId}/tareas/${props.taskId}`,
    );
  } finally {
    window.history.replaceState(null, "", previous);
  }
});
it("@s32 closes explicitly from the stable URL with both optional notes", async () => {
  const openResponse = Response.json(
    { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
    { headers: { "Work-Session-Revision": `work-session-${session.id}-1` } },
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(openResponse)
    .mockResolvedValueOnce(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[1][0]).toBe(
    `/api/v1/work-sessions/${session.id}/close`,
  );
  expect(JSON.parse(fetcher.mock.calls[1][1].body)).toEqual({
    progressNote: closure.progressNote,
    nextStep: closure.nextStep,
  });
});
it("@s40 announces a pending close and prevents duplicate submission while preserving focus", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  const input = await screen.findByLabelText("Avance anotado (opcional)");
  const submit = screen.getByRole("button", { name: "Confirmar cierre" });
  submit.focus();
  fireEvent.click(submit);
  fireEvent.submit(submit.closest("form")!);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Cerrando sesión de trabajo",
  );
  expect(submit).toHaveFocus();
  expect(submit).toHaveAttribute("aria-disabled", "true");
  expect(input).toHaveAttribute("readonly");
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s5 preserves an overlong draft and rejects it before transmitting", async () => {
  const fetcher = vi.fn().mockResolvedValueOnce(
    Response.json(
      { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
      {
        headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
      },
    ),
  );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  const input = await screen.findByLabelText("Siguiente paso (opcional)");
  const text = "😀".repeat(2001);
  fireEvent.change(input, { target: { value: text } });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(screen.getByRole("alert")).toHaveTextContent(
    "Cada nota admite hasta 2.000 caracteres válidos.",
  );
  expect(input).toHaveValue(text);
  expect(input).toHaveAttribute("aria-invalid", "true");
  expect(screen.getByLabelText("Avance anotado (opcional)")).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  expect(input).toHaveAttribute(
    "aria-describedby",
    screen.getByRole("alert").id,
  );
  expect(input).toHaveAccessibleDescription(
    "Cada nota admite hasta 2.000 caracteres válidos.",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
it("@s35 checks an uncertain close by its retained key without another POST", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(receipt));
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const check = await screen.findByRole("button", { name: "Comprobar cierre" });
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cierre.",
  );
  expect(
    screen.queryByRole("button", { name: "Confirmar cierre" }),
  ).not.toBeInTheDocument();
  const key = fetcher.mock.calls[1][1].headers["Idempotency-Key"];
  fireEvent.click(check);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-session-changes/by-request/${key}`,
  );
  expect(
    fetcher.mock.calls.filter(([, init]) => init.method === "POST"),
  ).toHaveLength(1);
});
it("@s35 permits only deliberate identical resend after a recognized missing change", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_change_not_found",
          title: "No se ha encontrado el cambio de sesión.",
          status: 404,
          code: "WORK_SESSION_CHANGE_NOT_FOUND",
        },
        { status: 404 },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar cierre" }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo cierre",
  });
  expect(fetcher).toHaveBeenCalledTimes(3);
  fireEvent.click(resend);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[3][1].body).toBe(fetcher.mock.calls[1][1].body);
  expect(fetcher.mock.calls[3][1].headers).toEqual(
    fetcher.mock.calls[1][1].headers,
  );
});
it("@s36 retains the draft after 412 and uses a new revision and key only after a new decision", async () => {
  const paused = {
    ...before,
    status: "paused",
    revision: "2",
    runningSince: null,
  };
  const result = {
    ...receipt,
    before: paused,
    after: { ...closed, revision: "3", workedMicroseconds: "0" },
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:precondition_failed",
          title: "La revisión ha cambiado.",
          status: 412,
          code: "PRECONDITION_FAILED",
        },
        { status: 412 },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { state: paused, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(result, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const consult = await screen.findByRole("button", {
    name: "Consultar estado actual",
  });
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(consult);
  expect(await screen.findByLabelText("Avance anotado (opcional)")).toHaveValue(
    closure.progressNote,
  );
  expect(screen.getByLabelText("Siguiente paso (opcional)")).toHaveValue(
    closure.nextStep,
  );
  expect(fetcher).toHaveBeenCalledTimes(3);
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(fetcher.mock.calls[3][1].headers["Idempotency-Key"]).not.toBe(
    fetcher.mock.calls[1][1].headers["Idempotency-Key"],
  );
  expect(fetcher.mock.calls[3][1].headers["Work-Session-Revision"]).toBe(
    `work-session-${session.id}-2`,
  );
});
it("@s37 renews CSRF separately and resends only the same retained closure manually", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:csrf_invalid",
          title: "Recupera el acceso.",
          status: 403,
          code: "CSRF_INVALID",
        },
        { status: 403 },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  stubClosureRequests(fetcher);
  setCsrfToken("before");
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: " Mi avance " },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const resend = await screen.findByRole("button", {
    name: "Reenviar el mismo cierre",
  });
  setCsrfToken("renewed");
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(resend);
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(fetcher.mock.calls[2][1].body).toBe(fetcher.mock.calls[1][1].body);
  const previous = new Headers(fetcher.mock.calls[1][1].headers);
  const current = new Headers(fetcher.mock.calls[2][1].headers);
  expect(current.get("Idempotency-Key")).toBe(previous.get("Idempotency-Key"));
  expect(current.get("Work-Session-Revision")).toBe(
    previous.get("Work-Session-Revision"),
  );
  expect(current.get("X-CSRF-TOKEN")).toBe("renewed");
});
it("@s38 aborts a transmitted closure when its private reader unmounts", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  stubClosureRequests(fetcher);
  const view = render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const signal = fetcher.mock.calls[1][1].signal;
  view.unmount();
  expect(signal.aborted).toBe(true);
});
it("@s38 replaces private drafts on route change and ignores a late command response", async () => {
  let resolve!: (value: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockReturnValueOnce(pending)
    .mockImplementation(() => new Promise(() => {}));
  stubClosureRequests(fetcher);
  const view = render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Borrador privado" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const signal = fetcher.mock.calls[1][1].signal;
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  expect(
    screen.queryByDisplayValue("Borrador privado"),
  ).not.toBeInTheDocument();
  expect(signal.aborted).toBe(true);
  await act(async () => {
    resolve(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    );
    await pending;
  });
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
});
it("@s40 retrying a failed read announces loading and gives disappearing initiator a focus destination", async () => {
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  const retry = await screen.findByRole("button", {
    name: "Reintentar lectura",
  });
  retry.focus();
  fireEvent.click(retry);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando sesión de trabajo",
  );
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toHaveFocus();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s40 distinguishes a pending recovery read from transmitting a closure", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockImplementation(() => new Promise(() => {}));
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const check = await screen.findByRole("button", { name: "Comprobar cierre" });
  check.focus();
  fireEvent.click(check);
  expect(screen.getByRole("status")).toHaveTextContent("Comprobando cierre");
  expect(check).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s32 presents immutable closure date and explicit empty notes without implying task completion", async () => {
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(
        Response.json({
          ...receipt,
          closure: { ...closure, progressNote: "", nextStep: "" },
        }),
      ),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByText("Sesión cerrada");
  expect(screen.getByText("Sin avance anotado")).toBeVisible();
  expect(screen.getByText("Sin siguiente paso anotado")).toBeVisible();
  expect(
    document.querySelector(`time[datetime="${receipt.occurredAt}"]`),
  ).toBeVisible();
  expect(
    document
      .querySelector(`time[datetime="${receipt.occurredAt}"]`)
      ?.closest("p"),
  ).toHaveTextContent("UTC");
  expect(
    screen.getByText("El cierre de la sesión no completa la tarea."),
  ).toBeVisible();
});

it("@s34 preserves confirmed notes when the separate active lookup fails and retries that lookup only", async () => {
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url.endsWith("/state"))
      return Promise.resolve(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      );
    if (url.endsWith("/close"))
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        }),
      );
    if (url.endsWith("/active"))
      return Promise.resolve(new Response(null, { status: 503 }));
    throw new Error(`Unexpected ${url}`);
  });
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    await screen.findByText(
      "No se ha podido consultar si hay otra sesión abierta.",
    ),
  ).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  fetcher.mockImplementation((url: string) => {
    expect(url).toBe("/api/v1/work-sessions/active");
    return Promise.resolve(Response.json({ session: null }));
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Consultar sesión abierta" }),
  );
  expect(
    await screen.findByText("No hay ninguna sesión abierta."),
  ).toBeVisible();
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(4);
});

it("@s34 displays a different legitimate active session separately from the closed receipt", async () => {
  const other = {
    ...session,
    id: "62345678-1234-1234-1234-123456789abc",
    taskId: "72345678-1234-1234-1234-123456789abc",
  };
  stubClosureRequests(
    vi.fn().mockImplementation((url: string) => {
      if (url.endsWith("/state"))
        return Promise.resolve(
          Response.json(
            {
              state: before,
              serverNow: before.changedAt,
              netMicroseconds: "0",
            },
            {
              headers: {
                "Work-Session-Revision": `work-session-${session.id}-1`,
              },
            },
          ),
        );
      if (url.endsWith("/close"))
        return Promise.resolve(
          Response.json(receipt, {
            status: 201,
            headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
          }),
        );
      if (url.endsWith("/active"))
        return Promise.resolve(Response.json({ session: other }));
      throw new Error(`Unexpected ${url}`);
    }),
  );
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    await screen.findByRole("link", { name: "Ir a la sesión abierta" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${other.projectId}/tareas/${other.taskId}/sesiones/${other.id}`,
  );
  expect(screen.getByText(closure.progressNote)).toBeVisible();
  expect(
    screen.queryByText("No hay ninguna sesión abierta."),
  ).not.toBeInTheDocument();
});

it("@s33 rejects a closure whose immutable task context differs from the validated state", async () => {
  const otherSession = {
    ...session,
    taskId: "72345678-1234-1234-1234-123456789abc",
  };
  let finishClosure!: (value: Response) => void;
  vi.stubGlobal("fetch", (url: string) => {
    if (url.endsWith("/state")) return Promise.resolve(closedResponse());
    if (url.endsWith("/end-time"))
      return Promise.resolve(
        Response.json(
          {
            state: closed,
            serverNow: closed.changedAt,
            effectiveEndAt: session.plannedEndAt,
          },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-2`,
            },
          },
        ),
      );
    if (url.endsWith("/closure"))
      return new Promise<Response>((resolve) => {
        finishClosure = resolve;
      });
    throw new Error(`Unexpected request ${url}`);
  });
  render(<WorkSessionReader {...props} />);
  expect(
    await screen.findByText("Fin previsto original:", { exact: false }),
  ).toBeVisible();
  await act(async () => {
    finishClosure(
      Response.json({
        ...receipt,
        before: { ...before, session: otherSession },
        after: { ...closed, session: otherSession },
      }),
    );
  });
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Esta sesión no está disponible en esta tarea.",
  );
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  expect(
    screen.queryByText("Fin previsto original:", { exact: false }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("heading", { name: "Fin de la sesión" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Consultando sesión de trabajo"),
  ).not.toBeInTheDocument();
});
it("@s32 the close form identifies its session times and explains that leaving does not revoke a sent close", async () => {
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  expect(
    document.querySelector(`time[datetime="${session.startedAt}"]`),
  ).toBeVisible();
  expect(
    document.querySelector(`time[datetime="${session.plannedEndAt}"]`),
  ).toBeVisible();
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(
    screen.getByText(
      "Salir no revoca el cierre transmitido. Al volver puedes consultar esta sesión.",
    ),
  ).toBeVisible();
});

it("@s36 a definitive state conflict consults the closure without resubmitting the retained draft", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:work_session_state_conflict",
          title: "Ya cerrada",
          status: 409,
          code: "WORK_SESSION_STATE_CONFLICT",
        },
        { status: 409 },
      ),
    )
    .mockResolvedValueOnce(closedResponse())
    .mockResolvedValueOnce(Response.json(receipt));
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: "Mi intención sin confirmar" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Consultar estado actual" }),
  );
  expect(await screen.findByText(closure.progressNote)).toBeVisible();
  expect(
    screen.queryByText("Mi intención sin confirmar"),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo cierre" }),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([, options]) => options.method === "POST"),
  ).toHaveLength(1);
});

it("@s14 a definitive revision limit reports the rejection without treating the close as uncertain", async () => {
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:work_session_revision_exhausted",
            title: "Límite",
            status: 409,
            code: "WORK_SESSION_REVISION_EXHAUSTED",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pueden registrar más cambios en esta sesión.",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar cierre" }),
  ).not.toBeInTheDocument();
});

it("@s35 a failed key lookup preserves uncertainty and cannot enable resend", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
        },
      ),
    )
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json({ code: "UNKNOWN", status: 503 }, { status: 503 }),
    );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  fireEvent.click(
    await screen.findByRole("button", { name: "Comprobar cierre" }),
  );
  await act(async () => {});
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cierre.",
  );
  expect(
    screen.getByRole("button", { name: "Comprobar cierre" }),
  ).toHaveAttribute("aria-disabled", "false");
  expect(
    screen.queryByRole("button", { name: "Reenviar el mismo cierre" }),
  ).not.toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([, options]) => options.method === "POST"),
  ).toHaveLength(1);
});

it("@s38 an old HTTP401 from a retired reader cannot revoke current access", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  const fetcher = vi
    .fn()
    .mockReturnValueOnce(pending)
    .mockImplementation(() => new Promise(() => {}));
  stubClosureRequests(fetcher);
  const access = vi.fn();
  observeAccess(access);
  const view = render(<WorkSessionReader {...props} />);
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  await act(async () => {
    finish(new Response(null, { status: 401 }));
    await pending;
  });
  expect(access).not.toHaveBeenCalled();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s40 a deliberate blur before the close response does not move focus back to the reader", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockReturnValueOnce(pending)
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  const button = screen.getByRole("button", { name: "Confirmar cierre" });
  button.focus();
  fireEvent.click(button);
  button.blur();
  await act(async () => {
    finish(
      Response.json(
        { ...receipt, closure: { ...closure, progressNote: "", nextStep: "" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    );
    await pending;
  });
  expect(screen.getByText("Sesión cerrada")).toBeVisible();
  expect(document.body).toHaveFocus();
});

it("@s32 explains the irreversible notes and unchanged task before confirmation", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
      {
        headers: { "Work-Session-Revision": `work-session-${session.id}-1` },
      },
    ),
  );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  expect(
    screen.getByRole("heading", { name: "Cerrar sesión de trabajo" }),
  ).toBeVisible();
  expect(
    screen.getByText(
      "Las notas quedarán guardadas y no se podrán editar después del cierre. La tarea seguirá en su estado actual.",
    ),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s38 discards closure JSON decoded after navigating to another session", async () => {
  let finish!: (value: unknown) => void;
  const pending = new Promise((resolve) => {
    finish = resolve;
  });
  const delayed = Response.json(receipt);
  vi.spyOn(delayed, "json").mockReturnValue(pending);
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(closedResponse())
      .mockResolvedValueOnce(delayed)
      .mockImplementation(() => new Promise(() => {})),
  );
  const view = render(<WorkSessionReader {...props} />);
  await act(async () => {});
  expect(delayed.json).toHaveBeenCalledOnce();
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  await act(async () => {
    finish(receipt);
    await pending;
  });
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
});

it("@s38 a late problem classification cannot restore the retired close draft", async () => {
  let finish!: (value: unknown) => void;
  const pending = new Promise((resolve) => {
    finish = resolve;
  });
  const failure = new Response(null, { status: 503 });
  const copy = new Response(null, { status: 503 });
  vi.spyOn(copy, "json").mockReturnValue(pending);
  vi.spyOn(failure, "clone").mockReturnValue(copy);
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockResolvedValueOnce(failure)
      .mockImplementation(() => new Promise(() => {})),
  );
  const view = render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  await act(async () => {});
  expect(copy.json).toHaveBeenCalled();
  view.rerender(
    <WorkSessionReader {...props} id="62345678-1234-1234-1234-123456789abc" />,
  );
  await act(async () => {
    finish({ code: "UNKNOWN", status: 503 });
    await pending;
  });
  expect(
    screen.queryByRole("button", { name: "Comprobar cierre" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s40 a disappearing focused confirm button transfers focus to the session heading", async () => {
  let finish!: (value: Response) => void;
  const pending = new Promise<Response>((resolve) => {
    finish = resolve;
  });
  stubClosureRequests(
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json(
          { state: before, serverNow: before.changedAt, netMicroseconds: "0" },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      )
      .mockReturnValueOnce(pending)
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionReader {...props} />);
  await screen.findByLabelText("Avance anotado (opcional)");
  const button = screen.getByRole("button", { name: "Confirmar cierre" });
  button.focus();
  fireEvent.click(button);
  await act(async () => {
    finish(
      Response.json(
        { ...receipt, closure: { ...closure, progressNote: "", nextStep: "" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    );
    await pending;
  });
  expect(
    screen.getByRole("heading", { name: "Sesión de trabajo" }),
  ).toHaveFocus();
});

it("@s38 a late active HTTP401 after confirmed close cannot revoke the new reader access", async () => {
  let finishActive!: (response: Response) => void;
  const pendingActive = new Promise<Response>((resolve) => {
    finishActive = resolve;
  });
  const nextSession = {
    ...session,
    id: "62345678-1234-1234-1234-123456789abc",
  };
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/v1/work-sessions/active") return pendingActive;
    if (url === `/api/v1/work-sessions/${session.id}/close`)
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        }),
      );
    if (
      url === `/api/v1/work-sessions/${session.id}/state` ||
      url === `/api/v1/work-sessions/${nextSession.id}/state`
    ) {
      const selected = url.includes(nextSession.id) ? nextSession : session;
      return Promise.resolve(
        Response.json(
          {
            state: { ...before, session: selected },
            serverNow: before.changedAt,
            netMicroseconds: "0",
          },
          {
            headers: {
              "Work-Session-Revision": `work-session-${selected.id}-1`,
            },
          },
        ),
      );
    }
    throw new Error(`Unexpected request ${url}`);
  });
  stubClosureRequests(fetcher);
  const access = vi.fn();
  observeAccess(access);
  const view = render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(screen.getByText("Consultando sesión abierta")).toBeVisible();
  await waitFor(() =>
    expect(
      fetcher.mock.calls.filter(
        ([url]) => url === "/api/v1/work-sessions/active",
      ),
    ).toHaveLength(1),
  );
  view.rerender(<WorkSessionReader {...props} id={nextSession.id} />);
  const currentNote = await screen.findByLabelText("Avance anotado (opcional)");
  fireEvent.change(currentNote, { target: { value: "Nueva nota segura" } });
  await act(async () => {
    finishActive(new Response(null, { status: 401 }));
    await pendingActive;
  });
  expect(access).not.toHaveBeenCalled();
  expect(currentNote).toHaveValue("Nueva nota segura");
  expect(screen.queryByText(closure.progressNote)).not.toBeInTheDocument();
  expect(screen.queryByText("Sesión cerrada")).not.toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s5 associates a progress-only note error without marking the valid next step invalid", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(
      {
        state: before,
        serverNow: before.changedAt,
        netMicroseconds: "0",
      },
      { headers: { "Work-Session-Revision": `work-session-${session.id}-1` } },
    ),
  );
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  const progress = await screen.findByLabelText("Avance anotado (opcional)");
  const next = screen.getByLabelText("Siguiente paso (opcional)");
  fireEvent.change(progress, { target: { value: "🧭".repeat(2001) } });
  fireEvent.change(next, { target: { value: "Continuar mañana" } });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  const error = screen.getByRole("alert");
  expect(progress).toHaveAttribute("aria-invalid", "true");
  expect(next).toHaveAttribute("aria-invalid", "false");
  expect(progress).toHaveAttribute("aria-describedby", error.id);
  expect(progress).toHaveAccessibleDescription(error.textContent!);
  expect(next).toHaveValue("Continuar mañana");
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s40 retrying the active lookup announces pending and coalesces another click", async () => {
  let activeCalls = 0;
  const fetcher = vi.fn().mockImplementation((url: string) => {
    if (url === "/api/v1/work-sessions/active") {
      activeCalls += 1;
      return activeCalls === 1
        ? Promise.resolve(new Response(null, { status: 503 }))
        : new Promise<Response>(() => {});
    }
    if (url === `/api/v1/work-sessions/${session.id}/state`)
      return Promise.resolve(
        Response.json(
          {
            state: before,
            serverNow: before.changedAt,
            netMicroseconds: "0",
          },
          {
            headers: {
              "Work-Session-Revision": `work-session-${session.id}-1`,
            },
          },
        ),
      );
    if (url === `/api/v1/work-sessions/${session.id}/close`)
      return Promise.resolve(
        Response.json(receipt, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        }),
      );
    throw new Error(`Unexpected request ${url}`);
  });
  stubClosureRequests(fetcher);
  render(<WorkSessionReader {...props} />);
  fireEvent.change(await screen.findByLabelText("Avance anotado (opcional)"), {
    target: { value: closure.progressNote },
  });
  fireEvent.change(screen.getByLabelText("Siguiente paso (opcional)"), {
    target: { value: closure.nextStep },
  });
  fireEvent.click(screen.getByRole("button", { name: "Confirmar cierre" }));
  await screen.findByText(
    "No se ha podido consultar si hay otra sesión abierta.",
  );
  const retry = screen.getByRole("button", {
    name: "Consultar sesión abierta",
  });
  retry.focus();
  fireEvent.click(retry);
  expect(screen.getByText("Consultando sesión abierta")).toHaveAttribute(
    "role",
    "status",
  );
  expect(retry).toHaveAttribute("aria-disabled", "true");
  expect(retry).toHaveFocus();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  fireEvent.click(retry);
  await waitFor(() => expect(activeCalls).toBe(2));
  expect(
    fetcher.mock.calls.filter(([url]) => url.endsWith("/close")),
  ).toHaveLength(1);
  expect(screen.getByText(closure.progressNote)).toBeVisible();
});
