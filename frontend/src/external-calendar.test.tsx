import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  ExternalCalendar,
  countersOf,
  describeEvent,
  storedWindow,
} from "./external-calendar";
import { observeAccess, setCsrfToken } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  setCsrfToken();
});

const subscription = {
  id: "11111111-2222-3333-4444-555555555555",
  label: "Trabajo",
  urlHost: "calendar.google.com",
  urlTail: ".ics",
  lastAttemptAt: null as string | null,
  lastSyncAt: null as string | null,
  lastStatus: null as string | null,
  lastError: null as string | null,
  snapshotZoneId: null as string | null,
  imported: 0,
  skippedRecurring: 0,
  skippedCancelled: 0,
  skippedInvalid: 0,
  truncated: false,
  updatedAt: "2030-01-07T11:00:00Z",
};
const synced = {
  ...subscription,
  lastAttemptAt: "2030-01-07T11:00:00Z",
  lastSyncAt: "2030-01-07T11:00:00Z",
  lastStatus: "OK",
  snapshotZoneId: "Europe/Madrid",
  imported: 12,
  skippedRecurring: 3,
  skippedCancelled: 1,
  skippedInvalid: 0,
};
const meeting = {
  uid: "u1",
  summary: "Reunión",
  startAt: "2030-01-07T08:00:00Z",
  endAt: "2030-01-07T09:00:00Z",
  allDay: false,
};
const emptyEvents = {
  configured: true,
  lastSyncAt: null,
  lastStatus: null,
  items: [] as unknown[],
};

type Route = { body: unknown; status?: number };
let routes: Map<string, Route[]>;
let calls: { url: string; method: string; body?: string }[];

function key(url: string, method: string) {
  return `${method} ${url.split("?")[0]}`;
}
function answer(url: string, method: string, body: unknown, status = 200) {
  const list = routes.get(key(url, method)) ?? [];
  list.push({ body, status });
  routes.set(key(url, method), list);
}
beforeEach(() => {
  routes = new Map();
  calls = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, options: RequestInit = {}) => {
      const method = options.method ?? "GET";
      calls.push({ url, method, body: options.body as string | undefined });
      const list = routes.get(key(url, method));
      const next = list && (list.length > 1 ? list.shift()! : list[0]);
      if (!next)
        throw new Error(`sin respuesta preparada para ${key(url, method)}`);
      if (next.body === "network") throw new TypeError("Failed to fetch");
      if (next.status === 204) return new Response(null, { status: 204 });
      return Response.json(next.body, { status: next.status });
    }),
  );
});

const ROUTE = "/api/v1/me/external-calendar";
function withoutSubscription() {
  answer(ROUTE, "GET", { configured: false, subscription: null });
}
function withSubscription(value: unknown, items: unknown[] = []) {
  answer(ROUTE, "GET", { configured: true, subscription: value });
  answer(`${ROUTE}/events`, "GET", { ...emptyEvents, items });
}

it("@s37 muestra el formulario de alta cuando no hay suscripción", async () => {
  withoutSubscription();
  render(<ExternalCalendar />);
  expect(await screen.findByLabelText("Etiqueta")).toBeInTheDocument();
  const address = screen.getByLabelText("Dirección secreta iCal");
  expect(address).toHaveAttribute("type", "url");
  expect(address).toHaveValue("");
  expect(screen.getByRole("button", { name: "Guardar" })).toBeEnabled();
  expect(screen.getByText(/Google Calendar/)).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Sincronizar ahora" }),
  ).not.toBeInTheDocument();
});

it("@s37 muestra host y cola pero nunca la dirección completa", async () => {
  withSubscription(subscription);
  render(<ExternalCalendar />);
  expect(await screen.findByText("calendar.google.com")).toBeInTheDocument();
  expect(screen.getByText(".ics")).toBeInTheDocument();
  expect(document.body.textContent).not.toContain("https://");
  expect(
    screen.getByRole("button", { name: "Sincronizar ahora" }),
  ).toBeInTheDocument();
});

it("@s37 deja sin fecha la última sincronización correcta cuando nunca hubo", async () => {
  withSubscription(subscription);
  render(<ExternalCalendar />);
  const term = await screen.findByText("Última sincronización correcta");
  expect(term.nextElementSibling).toHaveTextContent("");
});

it("@s37 resume los contadores y lista los eventos en hora local", async () => {
  withSubscription(synced, [meeting]);
  render(<ExternalCalendar />);
  expect(
    await screen.findByText(
      "12 eventos, 3 recurrentes no incluidos, 1 cancelado, 0 inválidos",
    ),
  ).toBeInTheDocument();
  const list = screen.getByRole("list");
  expect(within(list).getByText("Reunión")).toBeInTheDocument();
  expect(within(list).getByText("09:00–10:00")).toBeInTheDocument();
});

it("@s37 avisa cuando la instantánea quedó truncada", async () => {
  withSubscription({ ...synced, truncated: true });
  render(<ExternalCalendar />);
  expect(
    await screen.findByText(/solo se conservan los 500 primeros eventos/i),
  ).toBeInTheDocument();
});

it("@s37 muestra el mensaje accionable de FEED_HTTP_ERROR con la fecha del último intento", async () => {
  withSubscription({
    ...synced,
    lastStatus: "FAILED",
    lastError: "FEED_HTTP_ERROR",
    lastAttemptAt: "2030-01-07T12:00:00Z",
  });
  render(<ExternalCalendar />);
  expect(
    await screen.findByText(/vuelve a generar la dirección secreta/i),
  ).toBeInTheDocument();
  const attempt = screen.getByText("Último intento");
  expect(attempt.nextElementSibling?.textContent).not.toBe("");
});

it("@s37 pide volver a pegar la dirección cuando el secreto ya no se lee", async () => {
  withSubscription({
    ...synced,
    lastStatus: "FAILED",
    lastError: "SECRET_UNREADABLE",
  });
  render(<ExternalCalendar />);
  expect(await screen.findByText(/vuelve a pegarla/i)).toBeInTheDocument();
  const address = screen.getByLabelText("Dirección secreta iCal");
  expect(address).toHaveValue("");
  expect(address).not.toHaveAttribute("readonly");
});

it("@s38 anuncia Guardando, envía una sola petición y bloquea los controles", async () => {
  withoutSubscription();
  let release: (() => void) | undefined;
  const pending = new Promise<void>((resolve) => (release = resolve));
  const original = globalThis.fetch as unknown as typeof fetch;
  vi.stubGlobal("fetch", async (url: string, options: RequestInit = {}) => {
    if ((options.method ?? "GET") === "PUT") {
      calls.push({ url, method: "PUT", body: options.body as string });
      await pending;
      return Response.json({ configured: true, subscription });
    }
    return original(url as never, options);
  });
  answer(`${ROUTE}/events`, "GET", emptyEvents);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://calendar.google.com/a.ics",
  );
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(await screen.findByText("Guardando…")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Guardar" })).toBeDisabled();
  expect(calls.filter((call) => call.method === "PUT")).toHaveLength(1);
  release?.();
  expect(await screen.findByText("Guardado.")).toBeInTheDocument();
  expect(screen.getByLabelText("Dirección secreta iCal")).toHaveValue("");
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
});

it("@s38 conserva el borrador y enfoca el campo cuando la dirección se rechaza", async () => {
  withoutSubscription();
  answer(
    ROUTE,
    "PUT",
    {
      status: 400,
      code: "VALIDATION_ERROR",
      errors: [
        {
          field: "url",
          code: "BLOCKED_ADDRESS",
          message: "Apunta a una red interna.",
        },
      ],
    },
    400,
  );
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
  const address = screen.getByLabelText("Dirección secreta iCal");
  await user.type(address, "https://10.0.0.5/a.ics");
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(
    await screen.findByText("Apunta a una red interna."),
  ).toBeInTheDocument();
  expect(address).toHaveValue("https://10.0.0.5/a.ics");
  expect(address).toHaveAttribute("aria-invalid", "true");
  await waitFor(() => expect(address).toHaveFocus());
});

it.each([
  ["503", { status: 503, code: "CONNECTORS_DISABLED" }, 503],
  ["red", "network", 200],
])(
  "@s38 deja estado incierto y conserva el borrador con %s",
  async (_name, body, status) => {
    withoutSubscription();
    answer(ROUTE, "PUT", body, status);
    const user = userEvent.setup();
    render(<ExternalCalendar />);
    await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
    const address = screen.getByLabelText("Dirección secreta iCal");
    await user.type(address, "https://calendar.google.com/a.ics");
    await user.click(screen.getByRole("button", { name: "Guardar" }));
    expect(
      await screen.findByText(/no sabemos si se guardó/i),
    ).toBeInTheDocument();
    expect(address).toHaveValue("https://calendar.google.com/a.ics");
    expect(screen.getByRole("button", { name: "Guardar" })).toBeEnabled();
  },
);

it("@s38 retira los datos privados cuando la sesión ha caducado", async () => {
  withSubscription(synced, [meeting]);
  answer(ROUTE, "PUT", { status: 401, code: "UNAUTHENTICATED" }, 401);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("calendar.google.com");
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://x.test/a.ics",
  );
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  await waitFor(() =>
    expect(screen.queryByText("calendar.google.com")).not.toBeInTheDocument(),
  );
  expect(screen.queryByText("Reunión")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Dirección secreta iCal")).toHaveValue("");
});

it("@s38 sincroniza, actualiza contadores y refresca la lista", async () => {
  withSubscription(subscription);
  answer(`${ROUTE}/sync`, "POST", { performed: true, subscription: synced });
  routes.set(`GET ${ROUTE}/events`, [
    { body: emptyEvents, status: 200 },
    { body: { ...emptyEvents, items: [meeting] }, status: 200 },
  ]);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.click(
    await screen.findByRole("button", { name: "Sincronizar ahora" }),
  );
  expect(
    await screen.findByText(
      "12 eventos, 3 recurrentes no incluidos, 1 cancelado, 0 inválidos",
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText("Reunión")).toBeInTheDocument();
  expect(
    JSON.parse(calls.find((call) => call.method === "POST")!.body!),
  ).toEqual({
    onlyIfStale: false,
  });
});

// @s38 filas 6, 7 y 8: el Then exige feedback antes de 400 ms, UNA sola petición
// y los controles bloqueados hasta la respuesta. Sin retener la respuesta no hay
// forma de observar nada de eso, que es justo lo que faltaba (hallazgo 8).
it("@s38 anuncia Sincronizando, envía una sola petición y bloquea los controles", async () => {
  withSubscription(synced, [meeting]);
  let release: (() => void) | undefined;
  const pending = new Promise<void>((resolve) => (release = resolve));
  const original = globalThis.fetch as unknown as typeof fetch;
  vi.stubGlobal("fetch", async (url: string, options: RequestInit = {}) => {
    if ((options.method ?? "GET") === "POST") {
      calls.push({ url, method: "POST", body: options.body as string });
      await pending;
      return Response.json({ performed: true, subscription: synced });
    }
    return original(url as never, options);
  });
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  const sync = await screen.findByRole("button", {
    name: "Sincronizar ahora",
  });
  await user.click(sync);
  expect(await screen.findByText("Sincronizando…")).toBeInTheDocument();
  expect(sync).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Eliminar suscripción" }),
  ).toBeDisabled();
  expect(screen.getByRole("button", { name: "Guardar" })).toBeDisabled();
  expect(screen.getByLabelText("Etiqueta")).toHaveAttribute("readonly");
  expect(screen.getByLabelText("Dirección secreta iCal")).toHaveAttribute(
    "readonly",
  );
  // Segundo intento con la primera petición aún en vuelo: ni el bloqueo del
  // botón ni el guardián de reentrada pueden dejar salir un segundo POST.
  await user.click(sync);
  expect(calls.filter((call) => call.method === "POST")).toHaveLength(1);
  release?.();
  expect(await screen.findByText("Sincronizado.")).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Sincronizar ahora" }),
  ).toBeEnabled();
  expect(screen.getByLabelText("Etiqueta")).not.toHaveAttribute("readonly");
});

it("@s38 muestra el mensaje del código y conserva la lista cuando la sincronización falla", async () => {
  withSubscription(synced, [meeting]);
  answer(`${ROUTE}/sync`, "POST", {
    performed: true,
    subscription: {
      ...synced,
      lastStatus: "FAILED",
      lastError: "FEED_UNREACHABLE",
    },
  });
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("Reunión");
  await user.click(screen.getByRole("button", { name: "Sincronizar ahora" }));
  expect(
    await screen.findByText(/no se ha podido contactar con el proveedor/i),
  ).toBeInTheDocument();
  expect(screen.getByText("Reunión")).toBeInTheDocument();
  expect(calls.filter((call) => call.method === "POST")).toHaveLength(1);
});

it("@s38 vuelve al formulario vacío cuando la suscripción ya no existe", async () => {
  withSubscription(synced, [meeting]);
  answer(
    `${ROUTE}/sync`,
    "POST",
    { status: 404, code: "EXTERNAL_CALENDAR_NOT_CONFIGURED" },
    404,
  );
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("Reunión");
  await user.click(screen.getByRole("button", { name: "Sincronizar ahora" }));
  await waitFor(() =>
    expect(screen.queryByText("calendar.google.com")).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText("Etiqueta")).toHaveValue("");
  expect(calls.filter((call) => call.method === "POST")).toHaveLength(1);
});

it("@s39 no envía DELETE si se cancela la confirmación", async () => {
  withSubscription(synced, [meeting]);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.click(
    await screen.findByRole("button", { name: "Eliminar suscripción" }),
  );
  await user.click(screen.getByRole("button", { name: "Cancelar" }));
  expect(calls.some((call) => call.method === "DELETE")).toBe(false);
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
});

it("@s39 elimina con una sola petición y deja el formulario vacío", async () => {
  withSubscription(synced, [meeting]);
  answer(ROUTE, "DELETE", null, 204);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.click(
    await screen.findByRole("button", { name: "Eliminar suscripción" }),
  );
  await user.click(screen.getByRole("button", { name: "Sí, eliminar" }));
  await waitFor(() =>
    expect(screen.queryByText("calendar.google.com")).not.toBeInTheDocument(),
  );
  expect(calls.filter((call) => call.method === "DELETE")).toHaveLength(1);
  expect(screen.queryByText("Reunión")).not.toBeInTheDocument();
  expect(screen.getByLabelText("Etiqueta")).toHaveValue("");
});

it("@s39 cancela la petición en curso al desmontar la vista", async () => {
  let release: (() => void) | undefined;
  const held = new Promise<void>((resolve) => (release = resolve));
  const signals: AbortSignal[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, options: RequestInit = {}) => {
      calls.push({ url, method: options.method ?? "GET" });
      if (options.signal) signals.push(options.signal);
      await held;
      return Response.json({ configured: true, subscription: synced });
    }),
  );
  const view = render(<ExternalCalendar />);
  await waitFor(() => expect(signals.length).toBeGreaterThanOrEqual(1));
  expect(signals.every((signal) => !signal.aborted)).toBe(true);
  view.unmount();
  expect(signals.every((signal) => signal.aborted)).toBe(true);
  release?.();
  await new Promise((resolve) => setTimeout(resolve, 10));
  expect(screen.queryByText("calendar.google.com")).not.toBeInTheDocument();
  expect(document.body.textContent).toBe("");
});

// @s39 fila 3: «navego a /hoy con una petición en curso -> la petición se cancela
// y su respuesta tardía no modifica la vista destino». Hasta ahora solo estaba
// probado para el GET de montaje; ninguna prueba abortaba una escritura.
const held = () => {
  let release: (() => void) | undefined;
  const promise = new Promise<void>((resolve) => (release = resolve));
  return { promise, release: () => release?.() };
};

it.each([
  [
    "un guardado",
    "PUT",
    { configured: true, subscription: synced },
    async (user: ReturnType<typeof userEvent.setup>) => {
      await user.type(
        screen.getByLabelText("Dirección secreta iCal"),
        "https://calendar.google.com/a.ics",
      );
      await user.click(screen.getByRole("button", { name: "Guardar" }));
    },
  ],
  [
    "una sincronización",
    "POST",
    { performed: true, subscription: synced },
    async (user: ReturnType<typeof userEvent.setup>) => {
      await user.click(
        screen.getByRole("button", { name: "Sincronizar ahora" }),
      );
    },
  ],
  [
    "un borrado",
    "DELETE",
    null,
    async (user: ReturnType<typeof userEvent.setup>) => {
      await user.click(
        screen.getByRole("button", { name: "Eliminar suscripción" }),
      );
      await user.click(screen.getByRole("button", { name: "Sí, eliminar" }));
    },
  ],
])(
  "@s39 salir de la vista con %s en curso cancela la petición y su respuesta tardía no repinta",
  async (_name, method, reply, launch) => {
    const gate = held();
    const signals: AbortSignal[] = [];
    const original = globalThis.fetch as unknown as typeof fetch;
    withSubscription(synced, [meeting]);
    vi.stubGlobal("fetch", async (url: string, options: RequestInit = {}) => {
      if ((options.method ?? "GET") !== method)
        return original(url as never, options);
      calls.push({ url, method });
      if (options.signal) signals.push(options.signal);
      await gate.promise;
      return reply === null
        ? new Response(null, { status: 204 })
        : Response.json(reply);
    });
    const user = userEvent.setup();
    const view = render(<ExternalCalendar />);
    await screen.findByText("calendar.google.com");
    await launch(user);
    await waitFor(() => expect(signals).toHaveLength(1));
    expect(signals[0].aborted).toBe(false);
    view.unmount();
    expect(signals[0].aborted).toBe(true);
    gate.release();
    await new Promise((resolve) => setTimeout(resolve, 10));
    expect(document.body.textContent).toBe("");
  },
);

// Guardar está habilitado mientras la carga inicial sigue en vuelo, así que el
// único solapamiento real de la pantalla es este: el GET de montaje contra un PUT.
// start() aborta el anterior; sin ese aborto, la lectura tardía pisaría lo guardado.
it("@s38 guardar mientras la carga inicial sigue en vuelo la cancela y su respuesta tardía no pisa lo guardado", async () => {
  const gate = held();
  const signals: AbortSignal[] = [];
  vi.stubGlobal("fetch", async (url: string, options: RequestInit = {}) => {
    const method = options.method ?? "GET";
    calls.push({ url, method });
    if (options.signal) signals.push(options.signal);
    if (method === "GET" && !url.includes("/events")) {
      await gate.promise;
      return Response.json({ configured: false, subscription: null });
    }
    if (method === "PUT")
      return Response.json({ configured: true, subscription: synced });
    return Response.json(emptyEvents);
  });
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://calendar.google.com/a.ics",
  );
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(await screen.findByText("Guardado.")).toBeInTheDocument();
  expect(signals[0].aborted).toBe(true);
  gate.release();
  await new Promise((resolve) => setTimeout(resolve, 10));
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(
    screen.queryByText("Todavía no tienes ningún calendario externo."),
  ).not.toBeInTheDocument();
});

// El botón «Sí, eliminar» del diálogo NO lleva disabled={locked}: se puede pulsar
// otra vez con el DELETE en vuelo. Lo único que impide el segundo borrado es el
// guardián `if (busy) return` de confirmRemoval, que hasta ahora no tenía oráculo.
it("@s39 un segundo Sí, eliminar con el borrado en vuelo no envía otro DELETE", async () => {
  const gate = held();
  const original = globalThis.fetch as unknown as typeof fetch;
  withSubscription(synced, [meeting]);
  vi.stubGlobal("fetch", async (url: string, options: RequestInit = {}) => {
    if ((options.method ?? "GET") !== "DELETE")
      return original(url as never, options);
    calls.push({ url, method: "DELETE" });
    await gate.promise;
    return new Response(null, { status: 204 });
  });
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("calendar.google.com");
  await user.click(
    screen.getByRole("button", { name: "Eliminar suscripción" }),
  );
  const confirm = screen.getByRole("button", { name: "Sí, eliminar" });
  await user.click(confirm);
  expect(await screen.findByText("Eliminando…")).toBeInTheDocument();
  expect(confirm).toBeEnabled();
  await user.click(confirm);
  expect(calls.filter((call) => call.method === "DELETE")).toHaveLength(1);
  gate.release();
  expect(await screen.findByText("Suscripción eliminada.")).toBeInTheDocument();
});

it("@s36 avisa de lectura inválida sin perder el resto de la pantalla", async () => {
  answer(ROUTE, "GET", { configured: true, subscription: synced });
  answer(`${ROUTE}/events`, "GET", {
    ...emptyEvents,
    items: [{ ...meeting, allDay: undefined }],
  });
  render(<ExternalCalendar />);
  expect(
    await screen.findByText(/no se ha podido leer la lista/i),
  ).toBeInTheDocument();
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
});

// @s8 exige que las cinco rutas respondan 503 CONNECTORS_DISABLED cuando falta
// APP_CONNECTOR_KEY: es el estado de un despliegue real sin clave. Hasta ahora
// ninguna prueba hacía fallar el GET de montaje, así que el catch entero
// —incluido el mensaje que distingue ese estado— no se ejecutaba nunca.
it("@s8 la carga con conectores deshabilitados lo dice, sin el mensaje genérico", async () => {
  answer(ROUTE, "GET", { status: 503, code: "CONNECTORS_DISABLED" }, 503);
  render(<ExternalCalendar />);
  expect(
    await screen.findByText("Los conectores externos no están disponibles."),
  ).toBeInTheDocument();
  expect(
    screen.queryByText("No se ha podido cargar tu calendario externo."),
  ).not.toBeInTheDocument();
});

it("@s37 una carga que falla deja la pantalla usable, no un vacío permanente", async () => {
  answer(ROUTE, "GET", {}, 500);
  render(<ExternalCalendar />);
  expect(
    await screen.findByText("No se ha podido cargar tu calendario externo."),
  ).toBeInTheDocument();
  expect(
    screen.queryByText("Los conectores externos no están disponibles."),
  ).not.toBeInTheDocument();
  expect(
    screen.getByText("Todavía no tienes ningún calendario externo."),
  ).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Guardar" })).toBeEnabled();
});

it("@s37 mientras la carga no ha respondido no promete que no haya suscripción", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(() => new Promise<Response>(() => {})),
  );
  render(<ExternalCalendar />);
  await screen.findByLabelText("Etiqueta");
  expect(
    screen.queryByText("Todavía no tienes ningún calendario externo."),
  ).not.toBeInTheDocument();
});

it("@s37 dice que la ventana está vacía solo cuando la lectura sí ha llegado", async () => {
  withSubscription(synced);
  render(<ExternalCalendar />);
  expect(
    await screen.findByText("No hay eventos en la ventana guardada."),
  ).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it.each([
  ["500 sin cuerpo reconocible", {}, 500],
  ["503 de conectores", { status: 503, code: "CONNECTORS_DISABLED" }, 503],
  ["fallo de red", "network", 200],
])(
  "@s36 no afirma que la ventana esté vacía cuando la lista falla con %s",
  async (_name, body, status) => {
    answer(ROUTE, "GET", { configured: true, subscription: synced });
    answer(`${ROUTE}/events`, "GET", body, status);
    render(<ExternalCalendar />);
    expect(
      await screen.findByText("No se ha podido leer la lista de eventos."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("No hay eventos en la ventana guardada."),
    ).not.toBeInTheDocument();
    expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
  },
);

it("@s40 recorre con el teclado Etiqueta, Dirección, Guardar, Sincronizar y Eliminar", async () => {
  withSubscription(synced);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("calendar.google.com");
  const order = [
    screen.getByLabelText("Etiqueta"),
    screen.getByLabelText("Dirección secreta iCal"),
    screen.getByRole("button", { name: "Guardar" }),
    screen.getByRole("button", { name: "Sincronizar ahora" }),
    screen.getByRole("button", { name: "Eliminar suscripción" }),
  ];
  for (const control of order) {
    await user.tab();
    expect(control).toHaveFocus();
  }
});

it("@s40 anuncia los estados en una región viva que no roba el foco", async () => {
  withSubscription(subscription);
  answer(`${ROUTE}/sync`, "POST", { performed: true, subscription: synced });
  answer(`${ROUTE}/events`, "GET", emptyEvents);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  const button = await screen.findByRole("button", {
    name: "Sincronizar ahora",
  });
  button.focus();
  await user.click(button);
  const status = screen.getByRole("status");
  expect(status).toHaveAttribute("aria-live", "polite");
  await waitFor(() => expect(status).toHaveTextContent("Sincronizado."));
  expect(document.activeElement).toBe(button);
});

it("@s31 la ventana pedida cubre 24 horas antes y 336 después, en segundos exactos", () => {
  const range = storedWindow(Date.parse("2030-01-07T12:00:00.500Z"));
  expect(range).toEqual({
    from: "2030-01-06T12:00:00Z",
    to: "2030-01-21T12:00:00Z",
  });
});

it("@s37 el resumen usa singular cuando corresponde", () => {
  expect(
    countersOf({
      ...synced,
      imported: 1,
      skippedRecurring: 1,
      skippedCancelled: 0,
      skippedInvalid: 1,
    } as never),
  ).toBe("1 evento, 1 recurrente no incluido, 0 cancelados, 1 inválido");
});

it("@s35 un evento de todo el día se describe sin horas", () => {
  expect(describeEvent({ ...meeting, allDay: true }, "Europe/Madrid")).toBe(
    "Todo el día",
  );
});
