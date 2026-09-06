import { observeAccess } from "./api-client";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { Today } from "./today";
import { emptyToday, agendaToday } from "./today-fixture";
const deferred = <T,>() => {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
};
afterEach(() => {
  vi.unstubAllGlobals();
  vi.useRealTimers();
});
it("@s21 loads then confirms the empty day with known capacity and project link", async () => {
  const request = deferred<Response>();
  vi.stubGlobal("fetch", vi.fn().mockReturnValue(request.promise));
  render(<Today />);
  expect(screen.getByRole("status")).toHaveTextContent("Cargando Hoy");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.queryByText("No hay bloques planificados"),
  ).not.toBeInTheDocument();
  await act(async () => request.resolve(Response.json(emptyToday())));
  expect(screen.getByRole("heading", { name: "Hoy" })).toBeInTheDocument();
  expect(screen.getByText("No hay bloques planificados")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Ver proyectos" })).toHaveAttribute(
    "href",
    "/proyectos",
  );
  expect(
    screen.getByText("Presupuesto del día").nextElementSibling,
  ).toHaveTextContent("120 min");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
it("@s18 presents readable planned reservations and the real closing date as text", async () => {
  const value = agendaToday();
  const later = {
    block: {
      ...value.items[0].block,
      id: "00000000-0000-0000-0000-000000000004",
      startAt: "2030-01-07T23:30:00Z",
      endAt: "2030-01-08T00:30:00Z",
      zoneId: "UTC",
    },
    projectName: "<script>privado</script>",
    taskTitle: "Otra tarea",
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...value,
        items: [...value.items, later],
        plannedSeconds: 5400,
        remainingSeconds: 1800,
        nextBlockId: later.block.id,
        closingAt: later.block.endAt,
      }),
    ),
  );
  render(<Today />);
  expect(await screen.findByText("Proyecto personal")).toBeInTheDocument();
  expect(screen.getByText("<script>privado</script>")).toBeInTheDocument();
  expect(document.querySelector("script")).toBeNull();
  expect(screen.getByRole("link", { name: "Escribir" })).toHaveAttribute(
    "href",
    `/proyectos/${value.items[0].block.projectId}/tareas/${value.items[0].block.taskId}`,
  );
  expect(screen.getAllByText("Preparar borrador")).toHaveLength(2);
  expect(screen.getByText("En horario planificado")).toBeInTheDocument();
  expect(screen.getByText("Próximo inicio planificado")).toBeInTheDocument();
  expect(
    screen.getByText("Cierre previsto").nextElementSibling,
  ).toHaveTextContent("8 ene 2030");
  expect(screen.getByText(/Según actualización de/)).toHaveTextContent("12:00");
  expect(
    screen.getByText("Tiempo planificado").nextElementSibling,
  ).toHaveTextContent("90 min");
  expect(
    screen.getByText("Presupuesto sin reservar").nextElementSibling,
  ).toHaveTextContent("30 min");
  expect(
    screen.queryByText("No hay bloques planificados"),
  ).not.toBeInTheDocument();
});
it.each(["UNCONFIGURED", "UNAVAILABLE"])(
  "@s19 explains %s without pretending zero capacity",
  async (zoneSource) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          ...agendaToday(),
          zoneSource,
          availabilityZoneId: zoneSource === "UNCONFIGURED" ? null : "Old/Zone",
          budgetMinutes: null,
          remainingSeconds: null,
          excessSeconds: null,
        }),
      ),
    );
    render(<Today />);
    expect(
      await screen.findByText(
        zoneSource === "UNCONFIGURED"
          ? /Disponibilidad no configurada/
          : /Zona guardada no disponible/,
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Presupuesto del día").nextElementSibling,
    ).toHaveTextContent("Desconocido");
    expect(
      screen.getByText("Presupuesto sin reservar").nextElementSibling,
    ).toHaveTextContent("Desconocido");
    expect(
      screen.getByRole("link", { name: "Configurar disponibilidad" }),
    ).toHaveAttribute("href", "/disponibilidad");
    expect(screen.getByText("Proyecto personal")).toBeInTheDocument();
  },
);
it.each(["effective", "historical"])(
  "@s20 shows explicit UTC and original ID for an Intl-unavailable %s zone",
  async (which) => {
    const value = agendaToday();
    if (which === "effective") {
      value.zoneId = "Server/Only";
      value.availabilityZoneId = "Server/Only";
    }
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
    render(<Today />);
    expect(await screen.findByText("Proyecto personal")).toBeInTheDocument();
    expect(
      screen.getAllByText(
        new RegExp(
          which === "effective"
            ? "2030-01-07T12:00:00Z.*UTC.*Server/Only"
            : "2030-01-07T12:00:00Z.*UTC.*Historical/Unknown",
        ),
      ).length,
    ).toBeGreaterThan(0);
  },
);
it.each(["network", "invalid"])(
  "@s22 recovers an initial %s failure without a fake empty state",
  async (failure) => {
    const fetch = vi.fn();
    if (failure === "network")
      fetch.mockRejectedValueOnce(new TypeError("offline"));
    else fetch.mockResolvedValueOnce(Response.json({}));
    fetch.mockResolvedValueOnce(Response.json(agendaToday()));
    vi.stubGlobal("fetch", fetch);
    const storage = vi.spyOn(Storage.prototype, "setItem");
    render(<Today />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "No se pudo actualizar Hoy",
    );
    expect(
      screen.queryByText("No hay bloques planificados"),
    ).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("Proyecto personal")).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(storage).not.toHaveBeenCalled();
  },
);
it("@s23 @s37 manual refresh retains dated data and the chosen keyboard focus", async () => {
  const request = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json(agendaToday()))
    .mockReturnValueOnce(request.promise);
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  const link = await screen.findByRole("link", { name: "Escribir" });
  const button = screen.getByRole("button", { name: "Actualizar" });
  button.focus();
  await userEvent.keyboard("{Enter}");
  link.focus();
  expect(screen.getByRole("status")).toHaveTextContent("Actualizando Hoy");
  expect(screen.getByText("Proyecto personal")).toBeInTheDocument();
  expect(screen.getByText(/Según actualización de/)).toHaveTextContent("12:00");
  await act(async () =>
    request.resolve(
      Response.json({ ...agendaToday(), serverNow: "2030-01-07T12:05:00Z" }),
    ),
  );
  expect(link).toHaveFocus();
  expect(screen.getByText(/Según actualización de/)).toHaveTextContent("12:05");
  expect(fetch).toHaveBeenCalledTimes(2);
});
it.each(["json", "401"])(
  "@s30 ignores old %s delivered after unmount",
  async (kind) => {
    const pending = deferred<Response>();
    const body = deferred<unknown>();
    const old = Response.json(agendaToday());
    vi.spyOn(old, "json").mockReturnValue(body.promise);
    const fetch = vi
      .fn()
      .mockReturnValueOnce(
        kind === "json" ? Promise.resolve(old) : pending.promise,
      )
      .mockResolvedValueOnce(Response.json(emptyToday()));
    vi.stubGlobal("fetch", fetch);
    const access = vi.fn();
    observeAccess(access);
    const first = render(<Today />);
    await act(async () => {});
    const signal = fetch.mock.calls[0][1].signal as AbortSignal;
    first.unmount();
    render(<Today />);
    await screen.findByText("No hay bloques planificados");
    await act(async () => {
      body.resolve(agendaToday());
      pending.resolve(new Response(null, { status: 401 }));
    });
    expect(signal.aborted).toBe(true);
    expect(access).not.toHaveBeenCalled();
    expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
    observeAccess();
  },
);
it.each(["idle", "manual", "initial"])(
  "@s24 coalesces visible and focus while %s",
  async (phase) => {
    const request = deferred<Response>();
    const fetch = vi.fn();
    if (phase !== "initial")
      fetch.mockResolvedValueOnce(Response.json(emptyToday()));
    fetch.mockReturnValue(request.promise);
    vi.stubGlobal("fetch", fetch);
    render(<Today />);
    if (phase !== "initial")
      await screen.findByText("No hay bloques planificados");
    if (phase === "manual")
      fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
    act(() => {
      fireEvent(document, new Event("visibilitychange"));
      fireEvent(window, new Event("focus"));
    });
    expect(fetch).toHaveBeenCalledTimes(phase === "initial" ? 1 : 2);
    await act(async () => request.resolve(Response.json(emptyToday())));
  },
);
it.each([
  ["2030-01-07T11:45:00Z", 15 * 60000],
  ["2030-01-07T12:00:00Z", 60 * 60000],
  ["2030-01-07T13:00:00Z", 11 * 3600000],
])(
  "@s25 refreshes once at the next strictly future boundary from %s",
  async (serverNow, delay) => {
    vi.useFakeTimers();
    const value = {
      ...agendaToday(),
      serverNow,
      currentBlockId: serverNow.includes("12:00")
        ? agendaToday().currentBlockId
        : null,
      nextBlockId: serverNow.includes("11:45")
        ? agendaToday().currentBlockId
        : null,
    };
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(Response.json(value))
      .mockReturnValue(new Promise(() => {}));
    vi.stubGlobal("fetch", fetch);
    render(<Today />);
    await act(async () => {});
    expect(vi.getTimerCount()).toBe(1);
    await act(async () => vi.advanceTimersByTimeAsync(delay - 1));
    expect(fetch).toHaveBeenCalledTimes(1);
    await act(async () => vi.advanceTimersByTimeAsync(1));
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(vi.getTimerCount()).toBe(
      serverNow === "2030-01-07T13:00:00Z" ? 0 : 1,
    );
  },
);
it("@s26 @s27 cancels on hide and rebuilds only from the recovered server snapshot", async () => {
  vi.useFakeTimers();
  const value = { ...agendaToday(), serverNow: "2030-01-07T12:50:00Z" };
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json(value))
    .mockResolvedValueOnce(
      Response.json({ ...value, serverNow: "2030-01-07T12:52:00Z" }),
    )
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  await act(async () => vi.advanceTimersByTimeAsync(120000));
  vi.setSystemTime(new Date("2040-12-20T00:00:00Z"));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  expect(vi.getTimerCount()).toBe(0);
  await act(async () => vi.advanceTimersByTimeAsync(600000));
  expect(fetch).toHaveBeenCalledTimes(1);
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  await act(async () => fireEvent(document, new Event("visibilitychange")));
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(479999));
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(1));
  expect(fetch).toHaveBeenCalledTimes(3);
});
it.each([true, false])(
  "@s28 @s30 rollover supersedes pending JSON and clears yesterday even on failure=%s",
  async (failure) => {
    vi.useFakeTimers();
    const yesterday = {
      ...agendaToday(),
      serverNow: "2030-01-07T23:59:59Z",
      currentBlockId: null,
    };
    const body = deferred<unknown>();
    const old = Response.json(yesterday);
    vi.spyOn(old, "json").mockReturnValue(body.promise);
    const newDay = deferred<Response>();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(Response.json(yesterday))
      .mockResolvedValueOnce(old)
      .mockReturnValueOnce(newDay.promise);
    vi.stubGlobal("fetch", fetch);
    render(<Today />);
    await act(async () => {});
    fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
    await act(async () => {});
    await act(async () => vi.advanceTimersByTimeAsync(1000));
    expect(fetch).toHaveBeenCalledTimes(3);
    expect(fetch.mock.calls[1][1].signal.aborted).toBe(true);
    expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Cargando Hoy");
    await act(async () => {
      body.resolve(yesterday);
      newDay.resolve(
        failure
          ? new Response(null, { status: 503 })
          : Response.json({
              ...emptyToday(),
              serverNow: "2030-01-08T00:00:00Z",
              date: "2030-01-08",
              dayStartAt: "2030-01-08T00:00:00Z",
              dayEndAt: "2030-01-09T00:00:00Z",
            }),
      );
    });
    expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
    if (failure) {
      expect(screen.getByRole("alert")).toBeInTheDocument();
      expect(
        screen.queryByText("No hay bloques planificados"),
      ).not.toBeInTheDocument();
    } else expect(screen.getByText("2030-01-08 · UTC")).toBeInTheDocument();
  },
);
it("@s29 @s37 keeps failed refresh dated without repeating an expired boundary and recovers on focus", async () => {
  vi.useFakeTimers();
  const value = { ...agendaToday(), serverNow: "2030-01-07T12:59:59Z" };
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json(value))
    .mockRejectedValueOnce(new TypeError("offline"))
    .mockResolvedValueOnce(
      Response.json({
        ...value,
        serverNow: "2030-01-07T13:00:10Z",
        currentBlockId: null,
      }),
    );
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  screen.getByRole("link", { name: "Escribir" }).focus();
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  expect(screen.getByRole("alert")).toHaveTextContent("Sin actualizar");
  expect(screen.getByText(/Según actualización de/)).toHaveTextContent("12:59");
  expect(screen.getByRole("link", { name: "Escribir" })).toHaveFocus();
  await act(async () => vi.advanceTimersByTimeAsync(60000));
  expect(fetch).toHaveBeenCalledTimes(2);
  expect(vi.getTimerCount()).toBe(1); // Sólo queda el cambio de día, sin repetir el bloque vencido.
  await act(async () => fireEvent(window, new Event("focus")));
  expect(fetch).toHaveBeenCalledTimes(3);
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});
import { App } from "./App";
it.each([
  "/",
  "/proyectos/nuevo",
  "/desconocida",
  "/proyectos/nuevo/extra",
  "/proyectos/P/tareas/T/extra",
])(
  "@s32 @s35 resolves route %s without accidental project reads",
  async (route) => {
    window.history.replaceState(null, "", route);
    const fetch = vi.fn().mockResolvedValue(Response.json(emptyToday()));
    vi.stubGlobal("fetch", fetch);
    render(<App />);
    if (route === "/") {
      expect(
        await screen.findByText("No hay bloques planificados"),
      ).toBeInTheDocument();
      expect(
        screen.queryByLabelText(/Nombre del proyecto/),
      ).not.toBeInTheDocument();
    } else if (route === "/proyectos/nuevo")
      expect(screen.getByLabelText(/Nombre del proyecto/)).toBeInTheDocument();
    else {
      expect(
        screen.getByRole("heading", { name: "Página no encontrada" }),
      ).toBeInTheDocument();
      expect(
        within(screen.getByRole("main")).getByRole("link", { name: "Hoy" }),
      ).toHaveAttribute("href", "/");
    }
    if (route !== "/") expect(fetch).not.toHaveBeenCalled();
    window.history.replaceState(null, "", "/");
  },
);
it.each(["/", "/proyectos/nuevo", "/disponibilidad"])(
  "@s36 navigation and breadcrumb reflect %s with one active section",
  async (route) => {
    window.history.replaceState(null, "", route);
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
    render(<App />);
    const expected =
      route === "/"
        ? "Hoy"
        : route === "/disponibilidad"
          ? "Disponibilidad"
          : "Proyectos";
    const nav = screen.getByRole("navigation", { name: "Principal" });
    expect(within(nav).getByRole("link", { name: expected })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(nav.querySelectorAll('[aria-current="page"]')).toHaveLength(1);
    expect(within(nav).getByRole("link", { name: "Hoy" })).toHaveAttribute(
      "href",
      "/",
    );
    expect(document.querySelector(".topbar strong")).toHaveTextContent(
      expected,
    );
    expect(
      screen.getByRole("link", { name: "Saltar al contenido" }),
    ).toHaveAttribute("href", "#proyectos");
    expect(document.getElementById("proyectos")).toHaveAttribute(
      "tabindex",
      "-1",
    );
    window.history.replaceState(null, "", "/");
  },
);
import { SessionGate } from "./session-gate";
it("@s31 logout immediately retires private agenda and a new login loads only the new snapshot", async () => {
  window.history.replaceState(null, "", "/");
  const anonymous = {
    authenticated: false,
    username: null,
    csrfToken: "token",
    csrfHeaderName: "X-CSRF-TOKEN",
  };
  let signedIn = true;
  let reads = 0;
  const closing = deferred<Response>();
  const nextAgenda = deferred<Response>();
  vi.stubGlobal(
    "fetch",
    vi.fn((url, options) => {
      if (url === "/api/session/logout") {
        signedIn = false;
        return closing.promise;
      }
      if (url === "/api/session" && options?.method === "POST") {
        signedIn = true;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session")
        return Promise.resolve(
          Response.json({
            ...anonymous,
            authenticated: signedIn,
            username: signedIn ? "persona" : null,
          }),
        );
      if (url === "/api/v1/today")
        return ++reads === 1
          ? Promise.resolve(Response.json(agendaToday()))
          : nextAgenda.promise;
      throw new Error(String(url));
    }),
  );
  render(<SessionGate />);
  await screen.findByText("Proyecto personal");
  fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
  expect(window.location.pathname).toBe("/");
  await act(async () => closing.resolve(new Response(null, { status: 204 })));
  fireEvent.change(screen.getByLabelText("Usuario"), {
    target: { value: "nueva" },
  });
  fireEvent.change(screen.getByLabelText("Contraseña"), {
    target: { value: "secret" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Iniciar sesión" }));
  await screen.findByRole("heading", { name: "Hoy" });
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
  await act(async () => nextAgenda.resolve(Response.json(emptyToday())));
  expect(screen.getByText("No hay bloques planificados")).toBeInTheDocument();
});
it("@s28 returning after midnight retires the old day before the recovered response", async () => {
  vi.useFakeTimers();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({
        ...agendaToday(),
        serverNow: "2030-01-07T23:59:59Z",
        currentBlockId: null,
      }),
    )
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  await act(async () => vi.advanceTimersByTimeAsync(2000));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(2);
});
it.each([false, true])(
  "@s33 creation link from populated=%s retains the capture form",
  async (populated) => {
    window.history.replaceState(null, "", "/proyectos");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          items: populated
            ? [
                {
                  id: "00000000-0000-0000-0000-000000000002",
                  name: "Proyecto",
                  status: "idea",
                  createdAt: "2030-01-01T00:00:00Z",
                  updatedAt: "2030-01-01T00:00:00Z",
                },
              ]
            : [],
          nextCursor: null,
        }),
      ),
    );
    render(<App />);
    const create = await screen.findByRole("link", { name: "Crear proyecto" });
    expect(create).toHaveAttribute("href", "/proyectos/nuevo");
    fireEvent.click(create);
    expect(window.location.pathname).toBe("/proyectos/nuevo");
    expect(screen.getByLabelText(/Nombre del proyecto/)).toBeInTheDocument();
    window.history.replaceState(null, "", "/");
  },
);
it.each([true, false])(
  "@s34 @s35 distinguishes initial authenticated=%s from anonymous login on an unknown local route",
  async (authenticated) => {
    window.history.replaceState(null, "", "/desconocida");
    const anonymous = {
      authenticated: false,
      username: null,
      csrfToken: "token",
      csrfHeaderName: "X-CSRF-TOKEN",
    };
    let signedIn = authenticated;
    const fetch = vi.fn((url, options) => {
      if (url === "/api/session" && options?.method === "POST") {
        signedIn = true;
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url === "/api/session")
        return Promise.resolve(
          Response.json({
            ...anonymous,
            authenticated: signedIn,
            username: signedIn ? "persona" : null,
          }),
        );
      if (url === "/api/v1/today")
        return Promise.resolve(Response.json(emptyToday()));
      throw new Error(String(url));
    });
    vi.stubGlobal("fetch", fetch);
    render(<SessionGate />);
    if (!authenticated)
      fireEvent.click(
        await screen.findByRole("button", { name: "Iniciar sesión" }),
      );
    expect(
      await screen.findByRole("heading", {
        name: authenticated ? "Página no encontrada" : "Hoy",
      }),
    ).toBeInTheDocument();
    expect(window.location.pathname).toBe(authenticated ? "/desconocida" : "/");
    expect(
      fetch.mock.calls.filter(([url]) => url !== "/api/session"),
    ).toHaveLength(authenticated ? 0 : 1);
    window.history.replaceState(null, "", "/");
  },
);
it("@s28 @s29 a failed block boundary still retires the old agenda at midnight without focus", async () => {
  vi.useFakeTimers();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ ...agendaToday(), serverNow: "2030-01-07T12:59:59Z" }),
    )
    .mockRejectedValueOnce(new TypeError("offline"))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  expect(screen.getByRole("alert")).toHaveTextContent("Sin actualizar");
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(11 * 3600000));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Cargando Hoy");
  expect(fetch).toHaveBeenCalledTimes(3);
});
it("@s28 @s30 a pending block-boundary request is replaced at midnight", async () => {
  vi.useFakeTimers();
  const old = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ ...agendaToday(), serverNow: "2030-01-07T12:59:59Z" }),
    )
    .mockReturnValueOnce(old.promise)
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(11 * 3600000));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(3);
  expect(fetch.mock.calls[1][1].signal.aborted).toBe(true);
  await act(async () => old.resolve(Response.json(agendaToday())));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
});
it("@s24 @s27 @s28 returning visible with a manual request pending keeps only the day deadline", async () => {
  vi.useFakeTimers();
  const old = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ ...agendaToday(), serverNow: "2030-01-07T12:59:58Z" }),
    )
    .mockReturnValueOnce(old.promise)
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  expect(vi.getTimerCount()).toBe(0);
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  act(() => {
    fireEvent(document, new Event("visibilitychange"));
    fireEvent(window, new Event("focus"));
  });
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(11 * 3600000));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(3);
  expect(fetch.mock.calls[1][1].signal.aborted).toBe(true);
  await act(async () => old.resolve(Response.json(agendaToday())));
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
});
it("@s27 visibility recovery waits for its new snapshot before rearming a block boundary", async () => {
  vi.useFakeTimers();
  const fresh = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ ...agendaToday(), serverNow: "2030-01-07T12:59:58Z" }),
    )
    .mockReturnValueOnce(fresh.promise);
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => vi.advanceTimersByTimeAsync(2000));
  expect(fetch).toHaveBeenCalledTimes(2);
  expect(vi.getTimerCount()).toBe(1);
  await act(async () =>
    fresh.resolve(
      Response.json({
        ...agendaToday(),
        serverNow: "2030-01-07T13:00:00Z",
        currentBlockId: null,
      }),
    ),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Agenda actualizada");
});
it("@s37 update stays focusable but announces unavailability and coalesces repeated Enter", async () => {
  const pending = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json(agendaToday()))
    .mockReturnValueOnce(pending.promise);
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await screen.findByText("Proyecto personal");
  const update = screen.getByRole("button", { name: "Actualizar" });
  update.focus();
  await userEvent.keyboard("{Enter}");
  expect(screen.getByRole("status")).toHaveTextContent("Actualizando Hoy");
  expect(update).toHaveAttribute("aria-disabled", "true");
  expect(update).not.toBeDisabled();
  expect(update).toHaveFocus();
  await userEvent.keyboard("{Enter}{Enter}");
  expect(fetch).toHaveBeenCalledTimes(2);
  await act(async () => pending.resolve(new Response(null, { status: 503 })));
  expect(update).toHaveFocus();
  expect(update).toHaveAttribute("aria-disabled", "false");
  expect(screen.getByRole("alert")).toHaveTextContent("Sin actualizar");
});
it.each([30, null])(
  "@s18 @s19 distinguishes planned excess from unknown capacity: %s",
  async (budgetMinutes) => {
    const value = {
      ...agendaToday(),
      budgetMinutes,
      zoneSource: budgetMinutes === null ? "UNCONFIGURED" : "AVAILABILITY",
      availabilityZoneId: budgetMinutes === null ? null : "UTC",
      remainingSeconds: budgetMinutes === null ? null : 0,
      excessSeconds: budgetMinutes === null ? null : 1800,
    };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
    render(<Today />);
    await screen.findByText("Proyecto personal");
    expect(
      screen.getByText("Exceso planificado").nextElementSibling,
    ).toHaveTextContent(budgetMinutes === null ? "Desconocido" : "30 min");
    if (budgetMinutes !== null) {
      expect(
        screen.queryByText(/capacidad desconocida/),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByRole("link", { name: "Configurar disponibilidad" }),
      ).not.toBeInTheDocument();
    }
  },
);
it("@s18 labels only the matching current and next reservation and preserves readable intervals", async () => {
  const value = agendaToday();
  value.items[0].block.zoneId = "UTC";
  const later = {
    ...value.items[0],
    block: {
      ...value.items[0].block,
      id: "00000000-0000-0000-0000-000000000004",
      startAt: "2030-01-07T14:00:00Z",
      endAt: "2030-01-07T15:00:00Z",
    },
    taskTitle: "Segunda tarea",
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...value,
        items: [...value.items, later],
        plannedSeconds: 7200,
        remainingSeconds: 0,
        nextBlockId: later.block.id,
        closingAt: later.block.endAt,
      }),
    ),
  );
  render(<Today />);
  await screen.findByRole("link", { name: "Escribir" });
  const [current, next] = screen.getAllByRole("listitem");
  expect(
    within(current).getByText("En horario planificado"),
  ).toBeInTheDocument();
  expect(
    within(current).queryByText("Próximo inicio planificado"),
  ).not.toBeInTheDocument();
  expect(
    within(next).getByText("Próximo inicio planificado"),
  ).toBeInTheDocument();
  expect(
    within(next).queryByText("En horario planificado"),
  ).not.toBeInTheDocument();
  expect(within(current).getByText(/12:00.*—.*13:00/)).toBeInTheDocument();
  expect(within(next).getByText(/14:00.*—.*15:00/)).toBeInTheDocument();
  expect(screen.queryByText(/Zona original:/)).not.toBeInTheDocument();
});
it("@s30 obsolete failure and finally cannot replace the current loading state", async () => {
  vi.useFakeTimers();
  const old = deferred<Response>();
  const current = deferred<Response>();
  const value = { ...agendaToday(), serverNow: "2030-01-07T12:59:59Z" };
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(Response.json(value))
    .mockReturnValueOnce(old.promise)
    .mockReturnValueOnce(current.promise);
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  expect(fetch.mock.calls[1][1].signal.aborted).toBe(true);
  await act(async () => old.resolve(new Response(null, { status: 503 })));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Actualizando Hoy");
  fireEvent(window, new Event("focus"));
  expect(fetch).toHaveBeenCalledTimes(3);
  await act(async () =>
    current.resolve(
      Response.json({
        ...value,
        serverNow: "2030-01-07T13:00:00Z",
        currentBlockId: null,
      }),
    ),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Agenda actualizada.");
});
it("@s26 accepting a response while hidden leaves no scheduled refresh", async () => {
  vi.useFakeTimers();
  const pending = deferred<Response>();
  const fetch = vi
    .fn()
    .mockReturnValueOnce(pending.promise)
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  await act(async () =>
    pending.resolve(
      Response.json({ ...agendaToday(), serverNow: "2030-01-07T12:59:59Z" }),
    ),
  );
  expect(vi.getTimerCount()).toBe(0);
  await act(async () => vi.advanceTimersByTimeAsync(2000));
  expect(fetch).toHaveBeenCalledTimes(1);
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  expect(fetch).toHaveBeenCalledTimes(2);
});
it("@s22 retry removes the old alert while loading and confirms an explicit empty closing", async () => {
  const pending = deferred<Response>();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockRejectedValueOnce(new TypeError("offline"))
      .mockReturnValueOnce(pending.promise),
  );
  render(<Today />);
  await screen.findByRole("alert");
  expect(screen.getByRole("status")).toBeEmptyDOMElement();
  fireEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent("Cargando Hoy");
  await act(async () => pending.resolve(Response.json(emptyToday())));
  expect(
    screen.getByText("Cierre previsto").nextElementSibling,
  ).toHaveTextContent("Sin bloques");
});
it.each([
  "/extra/proyectos/id/editar",
  "/proyectos/id/editar/extra",
  "/extra/proyectos/id",
  "/desconocida",
])(
  "@s35 unknown route %s remains private 404 without reads or active navigation",
  async (route) => {
    window.history.replaceState(null, "", route);
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    render(<App />);
    const main = screen.getByRole("main");
    expect(
      within(main).getByRole("heading", { name: "Página no encontrada" }),
    ).toBeInTheDocument();
    expect(main).toHaveAttribute("tabindex", "-1");
    expect(within(main).getByRole("link", { name: "Hoy" })).toHaveAttribute(
      "href",
      "/",
    );
    expect(
      within(main).getByRole("link", { name: "Proyectos" }),
    ).toHaveAttribute("href", "/proyectos");
    if (!route.startsWith("/proyectos")) {
      expect(
        screen
          .getByRole("navigation", { name: "Principal" })
          .querySelector('[aria-current="page"]'),
      ).toBeNull();
      expect(document.querySelector(".topbar strong")).toHaveTextContent(
        "Página no encontrada",
      );
    }
    expect(fetch).not.toHaveBeenCalled();
    window.history.replaceState(null, "", "/");
  },
);
it("@s18 @s19 keeps a readable boundary between time and zone and between fallback explanation and action", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...emptyToday(),
        zoneSource: "UNCONFIGURED",
        availabilityZoneId: null,
        budgetMinutes: null,
        remainingSeconds: null,
        excessSeconds: null,
      }),
    ),
  );
  render(<Today />);
  expect(await screen.findByText(/Según actualización de/)).toHaveTextContent(
    /12:00\s+·\s+UTC/,
  );
  expect(screen.getByText(/Disponibilidad no configurada/)).toHaveTextContent(
    /desconocida\.\s+Configurar disponibilidad/,
  );
});
it("@s35 the recovery links on 404 remain visually distinct text", () => {
  window.history.replaceState(null, "", "/desconocida");
  vi.stubGlobal("fetch", vi.fn());
  render(<App />);
  expect(screen.getByRole("main")).toHaveTextContent(/Hoy\s+·\s+Proyectos/);
  window.history.replaceState(null, "", "/");
});
it("@s30 unmount releases its visibility and focus subscriptions", async () => {
  const documentAdd = vi.spyOn(document, "addEventListener");
  const documentRemove = vi.spyOn(document, "removeEventListener");
  const windowAdd = vi.spyOn(window, "addEventListener");
  const windowRemove = vi.spyOn(window, "removeEventListener");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json(agendaToday())),
  );
  const { unmount } = render(<Today />);
  await screen.findByText("Proyecto personal");
  const visibility = documentAdd.mock.calls.filter(
    ([type]) => type === "visibilitychange",
  );
  const focus = windowAdd.mock.calls.filter(([type]) => type === "focus");
  expect(visibility.length).toBeGreaterThan(0);
  expect(focus.length).toBeGreaterThan(0);
  unmount();
  for (const [type, listener] of visibility)
    expect(documentRemove).toHaveBeenCalledWith(type, listener);
  for (const [type, listener] of focus)
    expect(windowRemove).toHaveBeenCalledWith(type, listener);
});
it.each(["boundary", "visibility"])(
  "@s28 a fractional %s deadline retires the old day while its replacement is pending",
  async (entry) => {
    vi.useFakeTimers();
    const clockNow = performance.now.bind(performance);
    let sampled = false;
    vi.spyOn(performance, "now").mockImplementation(() => {
      const fraction = sampled ? 0.5 : 0;
      sampled = true;
      return clockNow() + fraction;
    });
    const schedule = window.setTimeout.bind(window);
    vi.spyOn(window, "setTimeout").mockImplementation(
      (handler, delay, ...args) =>
        schedule(
          handler,
          Math.trunc(delay ?? 0),
          ...args,
        ) as unknown as ReturnType<typeof setTimeout>,
    );
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({
          ...agendaToday(),
          serverNow: "2030-01-07T23:59:59Z",
          currentBlockId: null,
        }),
      )
      .mockReturnValue(new Promise(() => {}));
    vi.stubGlobal("fetch", fetch);
    render(<Today />);
    await act(async () => {});
    expect(screen.getByText("Proyecto personal")).toBeInTheDocument();
    if (entry === "visibility") {
      fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
      expect(fetch).toHaveBeenCalledTimes(2);
      vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
      fireEvent(document, new Event("visibilitychange"));
      vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
      fireEvent(document, new Event("visibilitychange"));
      expect(fetch).toHaveBeenCalledTimes(2);
    }
    await act(async () => vi.advanceTimersByTimeAsync(999));
    expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Cargando Hoy");
    expect(fetch).toHaveBeenCalledTimes(entry === "visibility" ? 3 : 2);
  },
);
it("@s28 returning exactly at the day deadline retires a pending old-day request", async () => {
  vi.useFakeTimers();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({
        ...agendaToday(),
        serverNow: "2030-01-07T23:59:59Z",
        currentBlockId: null,
      }),
    )
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  render(<Today />);
  await act(async () => {});
  fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("hidden");
  fireEvent(document, new Event("visibilitychange"));
  await act(async () => vi.advanceTimersByTimeAsync(1000));
  vi.spyOn(document, "visibilityState", "get").mockReturnValue("visible");
  fireEvent(document, new Event("visibilitychange"));
  expect(fetch.mock.calls[1][1].signal.aborted).toBe(true);
  expect(fetch).toHaveBeenCalledTimes(3);
  expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
});
it("@s28 @s30 an obsolete 401 delivered in the deadline turn cannot revoke current access", async () => {
  vi.useFakeTimers();
  const old = deferred<Response>();
  const fetch = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({
        ...agendaToday(),
        serverNow: "2030-01-07T23:59:59Z",
        currentBlockId: null,
      }),
    )
    .mockReturnValueOnce(old.promise)
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetch);
  const access = vi.fn();
  observeAccess(access);
  try {
    render(<Today />);
    await act(async () => {});
    fireEvent.click(screen.getByRole("button", { name: "Actualizar" }));
    expect(fetch).toHaveBeenCalledTimes(2);
    await act(async () => {
      vi.advanceTimersByTime(1000);
      old.resolve(new Response(null, { status: 401 }));
      await old.promise;
      expect(access).not.toHaveBeenCalled();
    });
    expect(fetch).toHaveBeenCalledTimes(3);
    expect(screen.queryByText("Proyecto personal")).not.toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Cargando Hoy");
  } finally {
    observeAccess();
  }
});
