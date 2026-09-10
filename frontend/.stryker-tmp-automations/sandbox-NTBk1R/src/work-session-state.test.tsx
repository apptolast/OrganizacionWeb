// @ts-nocheck
import { setCsrfToken } from "./api-client";
import { afterEach, expect, it, vi } from "vitest";
import {
  cleanup,
  render,
  screen,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import { WorkSessionStatePanel } from "./work-session-state";
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
  status: "running",
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const snapshot = {
  state,
  serverNow: "2026-09-07T10:00:01.123456Z",
  netMicroseconds: "1000000",
};
const token = `work-session-${session.id}-1`;
const response = () =>
  Response.json(snapshot, { headers: { "Work-Session-Revision": token } });
afterEach(() => {
  setCsrfToken(undefined);
  cleanup();
  vi.unstubAllGlobals();
});
it("@s29 shows running and snapshot time only after a valid state response", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response()));
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByText("En curso", {})).toBeVisible();
  expect(screen.getByRole("button", { name: "Pausar" })).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Reanudar" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText("Tiempo de trabajo hasta la actualización: 1 s"),
  ).toBeVisible();
  expect(
    document.querySelector(`time[datetime="${snapshot.serverNow}"]`),
  ).toBeVisible();
  expect(screen.queryByText(token)).not.toBeInTheDocument();
});
it("@s29 paused state offers only resume and preserves accumulated time", async () => {
  const paused = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...snapshot, state: paused },
        {
          headers: {
            "Work-Session-Revision": `work-session-${session.id}-2`,
          },
        },
      ),
    ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByText("En pausa", {})).toBeVisible();
  expect(screen.getByRole("button", { name: "Reanudar" })).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText("Tiempo de trabajo hasta la actualización: 1 s"),
  ).toBeVisible();
});
it("@s29 announces pending state without inferring running from the start receipt", () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando estado de la sesión",
  );
  expect(screen.queryByText("En curso", {})).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
});

it("@s30 explicit pause confirms a receipt then reads current state", async () => {
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const change = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: snapshot.serverNow,
    before: state,
    after,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(change, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${change.id}` },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...snapshot, state: after },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByText("Pausa confirmada", {})).toBeVisible();
  expect(await screen.findByText("En pausa", {})).toBeVisible();
  expect(fetcher.mock.calls[1][0]).toBe(
    `/api/v1/work-sessions/${session.id}/pause`,
  );
  expect(fetcher.mock.calls[1][1]).toMatchObject({
    method: "POST",
    body: "{}",
    headers: expect.objectContaining({
      "Work-Session-Revision": token,
      "Idempotency-Key": expect.any(String),
    }),
  });
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-sessions/${session.id}/state`,
  );
});
it("@s38 preserves the pause button focus and prevents duplicate activation while pending", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  const pause = await screen.findByRole("button", {
    name: "Pausar",
  });
  pause.focus();
  fireEvent.click(pause);
  fireEvent.click(pause);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Procesando cambio de sesión",
  );
  expect(pause).toHaveAttribute("aria-disabled", "true");
  expect(pause).toHaveFocus();
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s29 failed state lookup offers a deliberate retry without a mutation", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(response());
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No podemos consultar el estado de la sesión",
  );
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar consulta" }));
  expect(await screen.findByText("En curso", {})).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(fetcher.mock.calls.every(([, init]) => !init.method)).toBe(true);
});
it("@s31 preserves historical confirmation when its subsequent state lookup fails", async () => {
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const change = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: snapshot.serverNow,
    before: state,
    after,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(
        Response.json(change, {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${change.id}` },
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 503 })),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No podemos consultar el estado de la sesión",
  );
  expect(screen.getByText("Pausa confirmada", {})).toBeVisible();
  expect(screen.queryByText("En curso", {})).not.toBeInTheDocument();
  expect(screen.queryByText("En pausa", {})).not.toBeInTheDocument();
});
it("@s32 an uncertain pause is checked by its retained key without another POST", async () => {
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const change = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: snapshot.serverNow,
    before: state,
    after,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(change))
    .mockResolvedValueOnce(
      Response.json(
        { ...snapshot, state: after },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cambio",
  );
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
  const key = fetcher.mock.calls[1][1].headers["Idempotency-Key"];
  fireEvent.click(screen.getByRole("button", { name: "Comprobar cambio" }));
  expect(await screen.findByText("Pausa confirmada", {})).toBeVisible();
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(4));
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-session-changes/by-request/${key}`,
  );
  expect(
    fetcher.mock.calls.filter(([, init]) => init.method === "POST"),
  ).toHaveLength(1);
});
it("@s32 missing change permits only deliberate resend of the same intention", async () => {
  const problem = Response.json(
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
    .mockResolvedValueOnce(problem)
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  fireEvent.click(
    await screen.findByRole("button", {
      name: "Comprobar cambio",
    }),
  );
  const resend = await screen.findByRole("button", {
    name: "Reenviar cambio",
  });
  expect(fetcher).toHaveBeenCalledTimes(3);
  fireEvent.click(resend);
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(4));
  expect(fetcher.mock.calls[3][0]).toBe(fetcher.mock.calls[1][0]);
  expect(fetcher.mock.calls[3][1].headers).toEqual(
    fetcher.mock.calls[1][1].headers,
  );
  expect(fetcher.mock.calls[3][1].body).toBe("{}");
});

it("@s36 aborts a transmitted command when its panel unmounts", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const view = render(
    <WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />,
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  const signal = fetcher.mock.calls[1][1].signal;
  view.unmount();
  expect(signal).toBeInstanceOf(AbortSignal);
  expect(signal.aborted).toBe(true);
});
it("@s36 current command HTTP 401 retires the private parent", async () => {
  const onAccessFailure = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(new Response(null, { status: 401 })),
  );
  render(
    <WorkSessionStatePanel
      session={session}
      onAccessFailure={onAccessFailure}
    />,
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  await waitFor(() => expect(onAccessFailure).toHaveBeenCalledWith(401));
});

it("@s36 late HTTP 401 from a retired command cannot remove its still living parent", async () => {
  let resolve!: (value: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const onAccessFailure = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValueOnce(response()).mockReturnValueOnce(pending),
  );
  const view = render(
    <WorkSessionStatePanel
      session={session}
      onAccessFailure={onAccessFailure}
    />,
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  view.unmount();
  await act(async () => {
    resolve(new Response(null, { status: 401 }));
    await pending;
  });
  expect(onAccessFailure).not.toHaveBeenCalled();
});
it("@s33 obsolete revision requires a new state lookup and explicit new decision", async () => {
  const paused = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:precondition_failed",
          title: "Precondición fallida",
          status: 412,
          code: "PRECONDITION_FAILED",
        },
        { status: 412 },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...snapshot, state: paused },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "La sesión ha cambiado",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(
    screen.getByRole("button", {
      name: "Consultar estado actual",
    }),
  );
  fireEvent.click(await screen.findByRole("button", { name: "Reanudar" }));
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${session.id}/resume`,
  );
  expect(fetcher.mock.calls[3][1].headers["Idempotency-Key"]).not.toBe(
    fetcher.mock.calls[1][1].headers["Idempotency-Key"],
  );
  expect(fetcher.mock.calls[3][1].headers["Work-Session-Revision"]).toBe(
    `work-session-${session.id}-2`,
  );
});
it("@s36 current state lookup HTTP 401 retires the private parent", async () => {
  const onAccessFailure = vi.fn();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(null, { status: 401 })),
  );
  render(
    <WorkSessionStatePanel
      session={session}
      onAccessFailure={onAccessFailure}
    />,
  );
  await waitFor(() => expect(onAccessFailure).toHaveBeenCalledWith(401));
});
it("@s36 changing session identity retires its pending command and prior state", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const onAccessFailure = vi.fn();
  const view = render(
    <WorkSessionStatePanel
      session={session}
      onAccessFailure={onAccessFailure}
    />,
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  const signal = fetcher.mock.calls[1][1].signal;
  view.rerender(
    <WorkSessionStatePanel
      session={{ ...session, id: "52345678-1234-1234-1234-123456789abc" }}
      onAccessFailure={onAccessFailure}
    />,
  );
  expect(signal.aborted).toBe(true);
  expect(screen.queryByText("En curso", {})).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando estado de la sesión",
  );
});
it("@s29 refresh is deliberate and keeps the snapshot visibly dated until replaced", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  await screen.findByText("En curso", {});
  expect(fetcher).toHaveBeenCalledTimes(1);
  fireEvent.click(
    screen.getByRole("button", {
      name: "Actualizar estado de la sesión",
    }),
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(screen.getByText("En curso", {})).toBeVisible();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando estado de la sesión",
  );
});
it("@s39 an older state request is retired when a pause is transmitted", async () => {
  let resolve!: (response: Response) => void;
  const old = new Promise<Response>((done) => {
    resolve = done;
  });
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const change = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: snapshot.serverNow,
    before: state,
    after,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockReturnValueOnce(old)
    .mockResolvedValueOnce(
      Response.json(change, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${change.id}` },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...snapshot, state: after },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  await screen.findByText("En curso", {});
  fireEvent.click(
    screen.getByRole("button", {
      name: "Actualizar estado de la sesión",
    }),
  );
  fireEvent.click(screen.getByRole("button", { name: "Pausar" }));
  expect(fetcher.mock.calls[1][1].signal.aborted).toBe(true);
  expect(await screen.findByText("En pausa", {})).toBeVisible();
  await act(async () => {
    resolve(response());
    await old;
  });
  expect(screen.getByText("En pausa", {})).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
});
it("@s34 CSRF rejection retains intention for a separate manual resend with renewed access", async () => {
  setCsrfToken("before");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:csrf_invalid",
          title: "Token CSRF inválido",
          status: 403,
          code: "CSRF_INVALID",
        },
        { status: 403 },
      ),
    )
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  const resend = await screen.findByRole("button", {
    name: "Reenviar cambio",
  });
  setCsrfToken("renewed");
  expect(fetcher).toHaveBeenCalledTimes(2);
  fireEvent.click(resend);
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  const before = new Headers(fetcher.mock.calls[1][1].headers),
    after = new Headers(fetcher.mock.calls[2][1].headers);
  expect(after.get("Idempotency-Key")).toBe(before.get("Idempotency-Key"));
  expect(after.get("Work-Session-Revision")).toBe(token);
  expect(after.get("X-CSRF-TOKEN")).toBe("renewed");
});
it("@s28 presents fractional work without rounding away microseconds", async () => {
  const tiny = {
    ...snapshot,
    serverNow: "2026-09-07T10:00:00.123457Z",
    netMicroseconds: "1",
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(tiny, { headers: { "Work-Session-Revision": token } }),
      ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  expect(
    await screen.findByText(
      "Tiempo de trabajo hasta la actualización: 0,000001 s",
    ),
  ).toBeVisible();
});
it("@s35 explains that leaving does not revoke a transmitted change", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(
    screen.getByText(
      "Salir no revoca el cambio transmitido. Al volver puedes consultar el estado de la sesión.",
    ),
  ).toBeVisible();
});
it("@s37 disappearance of the focused initiator moves focus to the session heading", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(new Response(null, { status: 503 })),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  const pause = await screen.findByRole("button", {
    name: "Pausar",
  });
  pause.focus();
  fireEvent.click(pause);
  await screen.findByRole("button", { name: "Comprobar cambio" });
  expect(
    screen.getByRole("heading", { name: "Estado de la sesión" }),
  ).toHaveFocus();
});
it("@s37 an error does not steal focus moved to another control", async () => {
  let resolve!: (value: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValueOnce(response()).mockReturnValueOnce(pending),
  );
  render(
    <>
      <button>Otro control</button>
      <WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />
    </>,
  );
  const pause = await screen.findByRole("button", {
    name: "Pausar",
  });
  pause.focus();
  fireEvent.click(pause);
  const other = screen.getByRole("button", {
    name: "Otro control",
  });
  other.focus();
  await act(async () => {
    resolve(new Response(null, { status: 503 }));
    await pending;
  });
  await screen.findByRole("alert");
  expect(other).toHaveFocus();
});
it("@s30 snapshot time falls back to labelled UTC for an unresolved historical zone", async () => {
  const historical = { ...session, zoneId: "Historic/Unavailable" };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, state: { ...state, session: historical } },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  render(
    <WorkSessionStatePanel session={historical} onAccessFailure={vi.fn()} />,
  );
  expect(
    await screen.findByText(
      "UTC (zona histórica no disponible: Historic/Unavailable)",
    ),
  ).toBeVisible();
  const time = document.querySelector(`time[datetime="${snapshot.serverNow}"]`);
  expect(time).toHaveTextContent("7 de septiembre de 2026");
  expect(time).not.toHaveTextContent("2026-09-07T");
});
it("@s14 a known state conflict explains the rejection and requires a state query", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:work_session_state_conflict",
            title: "El estado de la sesión no permite esta acción.",
            status: 409,
            code: "WORK_SESSION_STATE_CONFLICT",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "El estado de la sesión no permite esta acción.",
  );
  expect(
    screen.getByRole("button", {
      name: "Consultar estado actual",
    }),
  ).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Comprobar cambio" }),
  ).not.toBeInTheDocument();
});
it("@s15 exhausted revision reports a definitive rejection without uncertain recovery", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:work_session_revision_exhausted",
            title: "La sesión no admite más revisiones.",
            status: 409,
            code: "WORK_SESSION_REVISION_EXHAUSTED",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pueden registrar más cambios en esta sesión.",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar cambio" }),
  ).not.toBeInTheDocument();
});
it("@s6 rejected transition time is explained without claiming an uncertain commit", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(
        Response.json(
          {
            type: "urn:organization:problem:work_session_time_out_of_range",
            title: "No se puede registrar la transición en ese instante.",
            status: 409,
            code: "WORK_SESSION_TIME_OUT_OF_RANGE",
          },
          { status: 409 },
        ),
      ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se puede registrar el cambio en este instante.",
  );
  expect(
    screen.queryByRole("button", { name: "Comprobar cambio" }),
  ).not.toBeInTheDocument();
});
it("@s12 missing session rejects the command without exposing or recovering another session", async () => {
  vi.stubGlobal(
    "fetch",
    vi
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
      ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se ha encontrado la sesión de trabajo.",
  );
  expect(screen.queryByText("En curso", {})).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Comprobar cambio" }),
  ).not.toBeInTheDocument();
});
it("@s4 pause then resume preserves planned end and replaces the latest historical confirmation", async () => {
  const paused = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const resumed = {
    ...paused,
    status: "running",
    revision: "3",
    changedAt: "2026-09-07T10:05:00.123456Z",
    runningSince: "2026-09-07T10:05:00.123456Z",
  };
  const pause = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: paused.changedAt,
    before: state,
    after: paused,
  };
  const resume = {
    id: "52345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "RESUME",
    occurredAt: resumed.changedAt,
    before: paused,
    after: resumed,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(
      Response.json(pause, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${pause.id}` },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        { ...snapshot, state: paused },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(resume, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${resume.id}` },
      }),
    )
    .mockResolvedValueOnce(
      Response.json(
        {
          state: resumed,
          serverNow: resumed.changedAt,
          netMicroseconds: "1000000",
        },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-3` },
        },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  fireEvent.click(await screen.findByRole("button", { name: "Reanudar" }));
  expect(await screen.findByText("Reanudación confirmada", {})).toBeVisible();
  expect(await screen.findByText("En curso", {})).toBeVisible();
  expect(screen.queryByText("Pausa confirmada", {})).not.toBeInTheDocument();
  expect(
    screen.getByText("Tiempo de trabajo hasta la actualización: 1 s"),
  ).toBeVisible();
  expect(fetcher.mock.calls[3][0]).toBe(
    `/api/v1/work-sessions/${session.id}/resume`,
  );
  expect(fetcher.mock.calls[3][1].body).toBe("{}");
});
it("@s37 retrying the first failed lookup gives focus a destination while waiting", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  const retry = await screen.findByRole("button", {
    name: "Reintentar consulta",
  });
  retry.focus();
  fireEvent.click(retry);
  expect(
    screen.getByRole("heading", { name: "Estado de la sesión" }),
  ).toHaveFocus();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando estado de la sesión",
  );
});
it("@s31 recovery clears a failed independent refresh when the new state succeeds", async () => {
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const change = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: snapshot.serverNow,
    before: state,
    after,
  };
  let resolve!: (value: Response) => void;
  const pending = new Promise<Response>((done) => {
    resolve = done;
  });
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(Response.json(change))
    .mockReturnValueOnce(pending);
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  await screen.findByRole("button", { name: "Comprobar cambio" });
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar estado de la sesión" }),
  );
  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Actualizar estado de la sesión" }),
    ).toHaveAttribute("aria-disabled", "false"),
  );
  fireEvent.click(screen.getByRole("button", { name: "Comprobar cambio" }));
  await screen.findByText("Pausa confirmada");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.getByText("Consultando estado de la sesión")).toBeVisible();
  expect(
    screen.getByRole("button", { name: "Actualizar estado de la sesión" }),
  ).toHaveAttribute("aria-disabled", "true");
  await act(async () => {
    resolve(
      Response.json(
        { ...snapshot, state: after },
        {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        },
      ),
    );
    await pending;
  });
  expect(await screen.findByText("En pausa")).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s36 late command JSON cannot restore a retired session or trigger its state query", async () => {
  let resolve!: (value: unknown) => void;
  const json = new Promise<unknown>((done) => {
    resolve = done;
  });
  const after = {
    ...state,
    status: "paused",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  const change = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "PAUSE",
    occurredAt: snapshot.serverNow,
    before: state,
    after,
  };
  const result = Response.json(change, {
    status: 200,
    headers: { Location: `/api/v1/work-session-changes/${change.id}` },
  });
  const reader = vi.spyOn(result, "json").mockReturnValue(json);
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(result)
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const onAccessFailure = vi.fn();
  const view = render(
    <WorkSessionStatePanel
      session={session}
      onAccessFailure={onAccessFailure}
    />,
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  await waitFor(() => expect(reader).toHaveBeenCalled());
  view.rerender(
    <WorkSessionStatePanel
      session={{ ...session, id: "62345678-1234-1234-1234-123456789abc" }}
      onAccessFailure={onAccessFailure}
    />,
  );
  await act(async () => {
    resolve(change);
    await json;
  });
  expect(screen.queryByText("Pausa confirmada")).not.toBeInTheDocument();
  expect(screen.queryByText("En pausa")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(onAccessFailure).not.toHaveBeenCalled();
});
it("@s36 delayed non-401 error classification cannot alter the new context", async () => {
  let resolve!: (value: unknown) => void;
  const json = new Promise<unknown>((done) => {
    resolve = done;
  });
  const error = Response.json({}, { status: 412 });
  const reader = vi.fn().mockReturnValue(json);
  vi.spyOn(error, "clone").mockImplementation(
    () => ({ json: reader }) as unknown as Response,
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(error)
    .mockImplementation(() => new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  const onAccessFailure = vi.fn();
  const view = render(
    <WorkSessionStatePanel
      session={session}
      onAccessFailure={onAccessFailure}
    />,
  );
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  await waitFor(() => expect(reader).toHaveBeenCalled());
  view.rerender(
    <WorkSessionStatePanel
      session={{ ...session, id: "62345678-1234-1234-1234-123456789abc" }}
      onAccessFailure={onAccessFailure}
    />,
  );
  await act(async () => {
    resolve({
      type: "urn:organization:problem:precondition_failed",
      title: "Precondición fallida",
      status: 412,
      code: "PRECONDITION_FAILED",
    });
    await json;
  });
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando estado de la sesión",
  );
  expect(onAccessFailure).not.toHaveBeenCalled();
  expect(fetcher).toHaveBeenCalledTimes(3);
});
it("@s37 retry after a failed refresh announces the next lookup while retaining its dated snapshot", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response())
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockImplementation(() => new Promise(() => {})),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  await screen.findByText("En curso");
  fireEvent.click(
    screen.getByRole("button", { name: "Actualizar estado de la sesión" }),
  );
  const retry = await screen.findByRole("button", {
    name: "Reintentar consulta",
  });
  retry.focus();
  fireEvent.click(retry);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando estado de la sesión",
  );
  expect(
    screen.getByRole("heading", { name: "Estado de la sesión" }),
  ).toHaveFocus();
  expect(
    screen.getByRole("button", { name: "Actualizar estado de la sesión" }),
  ).toHaveAttribute("aria-disabled", "true");
});

it("@s32 a failed by-key check remains uncertain without authorizing resend", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(response())
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:unknown_storage_failure",
          title: "Consulta no disponible",
          status: 503,
          code: "UNKNOWN_STORAGE_FAILURE",
        },
        { status: 503 },
      ),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  fireEvent.click(await screen.findByRole("button", { name: "Pausar" }));
  const check = await screen.findByRole("button", { name: "Comprobar cambio" });
  const key = fetcher.mock.calls[1][1].headers["Idempotency-Key"];
  fireEvent.click(check);
  await waitFor(() => expect(check).toHaveAttribute("aria-disabled", "false"));
  expect(screen.getByRole("alert")).toHaveTextContent(
    "No podemos confirmar el cambio",
  );
  expect(check).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Reenviar cambio" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Pausar" }),
  ).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(3);
  expect(fetcher.mock.calls[2][0]).toBe(
    `/api/v1/work-session-changes/by-request/${key}`,
  );
  expect(
    fetcher.mock.calls.filter(([, init]) => init.method === "POST"),
  ).toHaveLength(1);
});

it("@s32 enters the stable close URL without transmitting a command", async () => {
  const fetcher = vi.fn().mockResolvedValue(response());
  vi.stubGlobal("fetch", fetcher);
  const previous = location.pathname;
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  const entry = await screen.findByRole("link", {
    name: "Cerrar sesión de trabajo",
  });
  expect(entry).toHaveAttribute(
    "href",
    `/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`,
  );
  fireEvent.click(entry);
  expect(location.pathname).toBe(
    `/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`,
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
  history.replaceState(null, "", previous);
});

it("@s24 a closed state cannot offer pause resume or another close", async () => {
  const closed = {
    ...state,
    status: "closed",
    revision: "2",
    changedAt: snapshot.serverNow,
    workedMicroseconds: "1000000",
    runningSince: null,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...snapshot, state: closed },
        {
          headers: {
            "Work-Session-Revision": `work-session-${session.id}-2`,
          },
        },
      ),
    ),
  );
  render(<WorkSessionStatePanel session={session} onAccessFailure={vi.fn()} />);
  expect(await screen.findByText("Sesión cerrada")).toBeVisible();
  expect(
    screen.queryByRole("button", { name: /^(Pausar|Reanudar)$/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("link", { name: "Cerrar sesión de trabajo" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("link", { name: "Ver cierre de la sesión" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`,
  );
});
