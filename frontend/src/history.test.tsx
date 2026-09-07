import { render, screen, within, act, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
import { History } from "./history";
import { observeAccess } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  window.history.replaceState(null, "", "/");
});

it("@s28 opens history from the existing navigation and presents confirmed emptiness", async () => {
  window.history.replaceState(null, "", "/no-existe");
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json({ items: [], nextCursor: null }));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await userEvent.click(
    within(screen.getByRole("navigation", { name: "Principal" })).getByRole(
      "link",
      { name: "Historial" },
    ),
  );
  expect(
    await screen.findByRole("heading", { level: 1, name: "Historial" }),
  ).toBeVisible();
  expect(
    await screen.findByText("Todavía no hay hechos en tu historial."),
  ).toBeVisible();
  expect(window.location.pathname).toBe("/historial");
  expect(screen.getByRole("link", { name: "Historial" })).toHaveAttribute(
    "aria-current",
    "page",
  );
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    "/api/v1/history",
    expect.objectContaining({ cache: "no-store" }),
  );
});

it("@s34 announces a pending read without claiming an empty history", () => {
  window.history.replaceState(null, "", "/historial");
  vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
  render(<App />);
  expect(screen.getByRole("status")).toHaveTextContent("Consultando historial");
  expect(
    screen.queryByText("Todavía no hay hechos en tu historial."),
  ).not.toBeInTheDocument();
});

it("@s34 presents storage failure without claiming absence or leaving a loading announcement", async () => {
  window.history.replaceState(null, "", "/historial");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(null, { status: 503 })),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No hemos podido consultar el historial.",
  );
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
  expect(
    screen.queryByText("Todavía no hay hechos en tu historial."),
  ).not.toBeInTheDocument();
});

it("@s35 retries only the same GET and announces the pending retry", async () => {
  window.history.replaceState(
    null,
    "",
    "/historial?category=sessions&cursor=opaque",
  );
  let resolve!: (value: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockReturnValueOnce(
      new Promise<Response>((done) => {
        resolve = done;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("alert");
  await userEvent.click(
    screen.getByRole("button", { name: "Reintentar consulta" }),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Consultando historial");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(fetcher.mock.calls.map(([url]) => url)).toEqual([
    "/api/v1/history?category=sessions&cursor=opaque",
    "/api/v1/history?category=sessions&cursor=opaque",
  ]);
  resolve(Response.json({ items: [], nextCursor: null }));
  expect(
    await screen.findByText("No hay hechos con estos filtros."),
  ).toBeVisible();
});

it("@s36 aborts a discarded read before its late HTTP401 reaches the session observer", async () => {
  let resolve!: (value: Response) => void;
  const observer = vi.fn();
  observeAccess(observer);
  const fetcher = vi.fn().mockReturnValue(
    new Promise<Response>((done) => {
      resolve = done;
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const view = render(<History route="/historial" />);
  expect(fetcher).toHaveBeenCalledTimes(1);
  view.unmount();
  await act(async () => {
    resolve(new Response(null, { status: 401 }));
  });
  expect(observer).not.toHaveBeenCalled();
});
const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "Europe/Madrid",
};
const started = {
  id: session.id,
  type: "SESSION_STARTED",
  occurredAt: session.startedAt,
  projectId: session.projectId,
  projectName: "Proyecto actual",
  taskId: session.taskId,
  taskTitle: "Tarea actual",
  details: session,
};
it("@s32 presents a session fact with current context and its existing session route", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [started], nextCursor: null })),
  );
  render(<History route="/historial" />);
  const list = await screen.findByRole("list", {
    name: "Hechos del historial",
  });
  expect(
    within(list).getByRole("heading", { name: "Sesión iniciada" }),
  ).toBeVisible();
  expect(
    within(list).getByRole("link", { name: "Proyecto actual" }),
  ).toHaveAttribute("href", `/proyectos/${session.projectId}`);
  expect(
    within(list).getByRole("link", { name: "Tarea actual" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${session.projectId}/tareas/${session.taskId}`,
  );
  expect(
    within(list).getByRole("link", { name: "Ver sesión de trabajo" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`,
  );
  expect(list.querySelector("time")).toHaveAttribute(
    "datetime",
    session.startedAt,
  );
  expect(within(list).getByText("UTC")).toBeVisible();
});

const block = {
  id: session.id,
  projectId: session.projectId,
  taskId: session.taskId,
  objective: "Reserva",
  startAt: "2026-10-01T10:00:00Z",
  endAt: "2026-10-01T10:25:00Z",
  zoneId: "UTC",
  durationMinutes: 25,
  createdAt: session.startedAt,
};
const running = {
  session,
  status: "running",
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const pause = {
  id: session.taskId,
  sessionId: session.id,
  action: "PAUSE",
  occurredAt: "2026-09-07T10:01:00.123456Z",
  before: running,
  after: {
    ...running,
    status: "paused",
    revision: "2",
    changedAt: "2026-09-07T10:01:00.123456Z",
    workedMicroseconds: "60000000",
    runningSince: null,
  },
};
it("@s33 distinguishes the five families and links a decision to its session rather than receipt ID", async () => {
  const items = [
    {
      ...started,
      id: pause.id,
      occurredAt: pause.occurredAt,
      type: "SESSION_CHANGED",
      details: pause,
    },
    started,
    {
      ...started,
      type: "TASK_STATUS_CHANGED",
      details: {
        id: session.id,
        fromStatus: "pending",
        toStatus: "completed",
        occurredAt: session.startedAt,
      },
    },
    {
      ...started,
      type: "BLOCK_CHANGED",
      details: {
        id: session.id,
        blockId: block.id,
        kind: "CANCELLED",
        revision: `"block:${block.id}:2"`,
        occurredAt: session.startedAt,
        before: block,
        after: null,
      },
    },
    { ...started, type: "BLOCK_PLANNED", details: block },
  ];
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json({ items, nextCursor: null }));
  vi.stubGlobal("fetch", fetcher);
  render(<History route="/historial" />);
  const list = await screen.findByRole("list", {
    name: "Hechos del historial",
  });
  expect(
    within(list)
      .getAllByRole("heading", { level: 2 })
      .map((heading) => heading.textContent),
  ).toEqual([
    "Sesión pausada",
    "Sesión iniciada",
    "Tarea completada",
    "Reserva cancelada",
    "Tiempo reservado",
  ]);
  const pausedRow = within(list).getAllByRole("listitem")[0];
  expect(
    within(pausedRow).getByRole("link", { name: "Ver sesión de trabajo" }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`,
  );
  expect(
    within(list).getAllByRole("link", { name: "Ver sesión de trabajo" }),
  ).toHaveLength(2);
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s34 retires previous results while a different URL query is pending", async () => {
  let resolve!: (value: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({ items: [started], nextCursor: null }),
      )
      .mockReturnValueOnce(
        new Promise<Response>((done) => {
          resolve = done;
        }),
      ),
  );
  const view = render(<History route="/historial" />);
  await screen.findByRole("link", { name: "Tarea actual" });
  view.rerender(<History route="/historial?category=planning" />);
  expect(
    screen.queryByRole("link", { name: "Tarea actual" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Consultando historial");
  await act(async () => {
    resolve(Response.json({ items: [], nextCursor: null }));
  });
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});

it("@s29 applies a filter draft explicitly and removes the previous cursor from the URL", async () => {
  window.history.replaceState(null, "", "/historial?cursor=old");
  const fetcher = vi
    .fn()
    .mockImplementation(() =>
      Promise.resolve(Response.json({ items: [], nextCursor: null })),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByText("No hay hechos en esta página.");
  await userEvent.selectOptions(screen.getByLabelText("Categoría"), "sessions");
  fireEvent.change(screen.getByLabelText("Desde (UTC)"), {
    target: { value: "2026-09-01" },
  });
  fireEvent.change(screen.getByLabelText("Hasta (UTC)"), {
    target: { value: "2026-09-07" },
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(window.location.search).toBe("?cursor=old");
  await userEvent.click(
    screen.getByRole("button", { name: "Aplicar filtros" }),
  );
  expect(new URLSearchParams(window.location.search).get("cursor")).toBeNull();
  expect(fetcher).toHaveBeenLastCalledWith(
    "/api/v1/history?category=sessions&from=2026-09-01&to=2026-09-07",
    expect.anything(),
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
});
it("@s34 distinguishes no matching facts and offers to clear the applied filters", async () => {
  window.history.replaceState(
    null,
    "",
    "/historial?category=planning&from=2026-09-07",
  );
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items: [], nextCursor: null })),
  );
  render(<App />);
  expect(
    await screen.findByText("No hay hechos con estos filtros."),
  ).toBeVisible();
  expect(
    screen.queryByText("Todavía no hay hechos en tu historial."),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Limpiar filtros" })).toHaveAttribute(
    "href",
    "/historial",
  );
});

it("@s31 replaces one page through its URL and returns to recent facts with the same filters", async () => {
  window.history.replaceState(null, "", "/historial?category=sessions");
  const oldSession = {
    ...session,
    id: session.taskId,
    startedAt: "2026-09-06T10:00:00Z",
    plannedEndAt: "2026-09-06T10:25:00Z",
  };
  const old = {
    ...started,
    id: oldSession.id,
    occurredAt: oldSession.startedAt,
    taskTitle: "Tarea anterior",
    details: oldSession,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ items: [started], nextCursor: "next-page" }),
    )
    .mockResolvedValueOnce(Response.json({ items: [old], nextCursor: null }))
    .mockResolvedValueOnce(
      Response.json({ items: [started], nextCursor: null }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("link", { name: "Tarea actual" });
  await userEvent.click(screen.getByRole("link", { name: "Más antiguos" }));
  expect(
    await screen.findByRole("link", { name: "Tarea anterior" }),
  ).toBeVisible();
  expect(
    screen.queryByRole("link", { name: "Tarea actual" }),
  ).not.toBeInTheDocument();
  expect(
    within(
      screen.getByRole("list", { name: "Hechos del historial" }),
    ).getAllByRole("listitem"),
  ).toHaveLength(1);
  expect(window.location.search).toBe("?category=sessions&cursor=next-page");
  expect(
    screen.queryByRole("link", { name: "Más antiguos" }),
  ).not.toBeInTheDocument();
  await userEvent.click(
    screen.getByRole("link", { name: "Volver a recientes" }),
  );
  expect(
    await screen.findByRole("link", { name: "Tarea actual" }),
  ).toBeVisible();
  expect(window.location.search).toBe("?category=sessions");
  expect(fetcher).toHaveBeenCalledTimes(3);
});

it("@s37 withdraws filter controls on a current authentication failure", async () => {
  const observer = vi.fn();
  observeAccess(observer);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(null, { status: 401 })),
  );
  render(<History route={`/historial?projectId=${session.projectId}`} />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Autenticación requerida. Vuelve a autenticarte para consultar tu historial.",
  );
  expect(
    screen.queryByRole("form", { name: "Filtros del historial" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Reintentar consulta" }),
  ).not.toBeInTheDocument();
  expect(observer).toHaveBeenCalledExactlyOnceWith(401);
});

it("@s37 removes unavailable context while retaining date and category filters", async () => {
  window.history.replaceState(
    null,
    "",
    `/historial?category=sessions&projectId=${session.projectId}&taskId=${session.taskId}&from=2026-09-01&cursor=old`,
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 404 }))
    .mockResolvedValueOnce(Response.json({ items: [], nextCursor: null }));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Este proyecto o tarea no está disponible para tu cuenta.",
  );
  await userEvent.click(
    screen.getByRole("link", { name: "Quitar filtro de contexto" }),
  );
  expect(
    await screen.findByText("No hay hechos con estos filtros."),
  ).toBeVisible();
  expect(window.location.search).toBe("?category=sessions&from=2026-09-01");
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s33 expands a closed receipt as plain notes and its own persisted net and work date", async () => {
  const note = "  <script>sin HTML</script>\nsegunda línea  ";
  const close = {
    ...pause,
    action: "CLOSE",
    after: { ...pause.after, status: "closed" },
    closure: {
      progressNote: note,
      nextStep: "Preparar otro intento",
      workDate: "2026-09-08",
      closeZoneId: "Historical/Unavailable",
    },
  };
  const entry = {
    ...started,
    id: close.id,
    type: "SESSION_CHANGED",
    occurredAt: close.occurredAt,
    details: close,
  };
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json({ items: [entry], nextCursor: null }));
  vi.stubGlobal("fetch", fetcher);
  render(<History route="/historial" />);
  await screen.findByRole("heading", { name: "Sesión cerrada" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const details = summary.closest("details")!;
  expect(details).toHaveAttribute("open");
  expect(
    within(details).getByText(note, { normalizer: (value) => value })
      .textContent,
  ).toBe(note);
  expect(details.querySelector("script")).toBeNull();
  expect(within(details).getByText("Tiempo trabajado: 60 s")).toBeVisible();
  expect(within(details).getByText("Día atribuido: 2026-09-08")).toBeVisible();
  expect(within(details).getByText("Preparar otro intento")).toBeVisible();
  expect(
    within(details).getByText(
      "UTC (zona histórica no disponible: Historical/Unavailable)",
    ),
  ).toBeVisible();
  expect(
    within(details).getByText("El cierre de la sesión no completa la tarea."),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s33 shows extension ends and local revision without claiming extra worked time", async () => {
  const extension = {
    ...pause,
    action: "EXTEND",
    after: { ...running, revision: "2" },
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  const entry = {
    ...started,
    id: extension.id,
    type: "SESSION_CHANGED",
    occurredAt: extension.occurredAt,
    details: extension,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  render(<History route="/historial" />);
  await screen.findByRole("heading", { name: "Tiempo ampliado" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const details = summary.closest("details")!;
  expect(within(details).getByText("Ampliación: 5 minutos")).toBeVisible();
  expect(
    Array.from(details.querySelectorAll("time")).map((time) => time.dateTime),
  ).toEqual([session.plannedEndAt, extension.extension.effectiveEndAt]);
  expect(within(details).getByText("Revisión de la sesión: 2")).toBeVisible();
  expect(
    within(details).getByText("La ampliación no añade tiempo trabajado."),
  ).toBeVisible();
  expect(
    within(details).queryByText(/Tiempo trabajado:/),
  ).not.toBeInTheDocument();
});

it("@s33 describes the original start as planned time rather than completed work", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [started], nextCursor: null })),
  );
  render(<History route="/historial" />);
  await screen.findByRole("heading", { name: "Sesión iniciada" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const details = summary.closest("details")!;
  expect(
    within(details).getByText("Tiempo previsto al iniciar: 25 minutos"),
  ).toBeVisible();
  expect(details.querySelector("time")).toHaveAttribute(
    "datetime",
    session.plannedEndAt,
  );
  expect(
    within(details).queryByText(/Tiempo trabajado:/),
  ).not.toBeInTheDocument();
});

it("@s33 presents pause work as an accumulated historical amount rather than a final total", async () => {
  const entry = {
    ...started,
    id: pause.id,
    type: "SESSION_CHANGED",
    occurredAt: pause.occurredAt,
    details: pause,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  render(<History route="/historial" />);
  await screen.findByRole("heading", { name: "Sesión pausada" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const details = summary.closest("details")!;
  expect(
    within(details).getByText("Trabajo acumulado en este hecho: 60 s"),
  ).toBeVisible();
  expect(within(details).getByText("Revisión de la sesión: 2")).toBeVisible();
  expect(
    within(details).queryByText(/Tiempo trabajado:/),
  ).not.toBeInTheDocument();
});

it("@s33 describes initial planning as a reservation with its original interval", async () => {
  const entry = { ...started, type: "BLOCK_PLANNED", details: block };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  render(<History route="/historial" />);
  await screen.findByRole("heading", { name: "Tiempo reservado" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const details = summary.closest("details")!;
  expect(within(details).getByText("Reserva")).toBeVisible();
  expect(
    Array.from(details.querySelectorAll("time")).map((time) => time.dateTime),
  ).toEqual([block.startAt, block.endAt]);
  expect(
    within(details).getByText("Tiempo reservado: 25 minutos"),
  ).toBeVisible();
  expect(
    within(details).getByText("Una reserva no acredita tiempo trabajado."),
  ).toBeVisible();
});

it("@s33 shows both reservation intervals and its local revision after rescheduling", async () => {
  const after = {
    ...block,
    startAt: "2026-10-02T10:00:00Z",
    endAt: "2026-10-02T10:25:00Z",
  };
  const details = {
    id: session.id,
    blockId: block.id,
    kind: "RESCHEDULED",
    revision: `"block:${block.id}:2"`,
    occurredAt: session.startedAt,
    before: block,
    after,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...started, type: "BLOCK_CHANGED", details }],
        nextCursor: null,
      }),
    ),
  );
  render(<History route="/historial" />);
  await screen.findByRole("heading", { name: "Reserva replanificada" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const disclosure = summary.closest("details")!;
  expect(
    Array.from(disclosure.querySelectorAll("time")).map(
      (time) => time.dateTime,
    ),
  ).toEqual([block.startAt, block.endAt, after.startAt, after.endAt]);
  expect(
    within(disclosure).getByText("Revisión de la reserva: 2"),
  ).toBeVisible();
  expect(disclosure.textContent).not.toContain("block:");
  expect(
    within(disclosure).getByRole("heading", { name: "Reserva anterior" }),
  ).toBeVisible();
  expect(
    within(disclosure).getByRole("heading", { name: "Reserva resultante" }),
  ).toBeVisible();
});

it("@s33 keeps a reopened task visible and describes the historical transition", async () => {
  const details = {
    id: session.id,
    fromStatus: "completed",
    toStatus: "pending",
    occurredAt: session.startedAt,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...started, type: "TASK_STATUS_CHANGED", details }],
        nextCursor: null,
      }),
    ),
  );
  render(<History route="/historial?category=task-status" />);
  await screen.findByRole("heading", { name: "Tarea reabierta" });
  const summary = screen.getByText("Ver detalles del hecho");
  await userEvent.click(summary);
  const disclosure = summary.closest("details")!;
  expect(
    within(disclosure).getByText("Estado anterior: Completada"),
  ).toBeVisible();
  expect(
    within(disclosure).getByText("Estado registrado: Pendiente"),
  ).toBeVisible();
  expect(within(disclosure).queryByRole("button")).not.toBeInTheDocument();
});
