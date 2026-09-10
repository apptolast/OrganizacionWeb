// @ts-nocheck
import {
  render,
  screen,
  act,
  waitFor,
  fireEvent,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { Calendar } from "./calendar";
import { observeAccess } from "./api-client";

const TOKEN = "a".repeat(43);
const URL_ONE = `https://organizacion.apptolast.com/calendar/${TOKEN}.ics`;
const URL_TWO = `https://organizacion.apptolast.com/calendar/${"b".repeat(43)}.ics`;
const CREATED_AT = "2026-09-08T12:00:00.000000Z";
const DOCUMENT = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n";

const readable = (value: string) =>
  new Intl.DateTimeFormat("es", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));

type Call = { url: string; method: string };
let calls: Call[] = [];
let routes: Record<string, () => Promise<Response> | Response>;

function statusResponse(active: boolean, createdAt: string | null = null) {
  return Response.json({ active, createdAt });
}

function calendarResponse(body = DOCUMENT) {
  const bytes = new TextEncoder().encode(body);
  return new Response(bytes, {
    headers: {
      "Content-Type": "text/calendar; charset=utf-8",
      "Content-Length": String(bytes.length),
    },
  });
}

beforeEach(() => {
  calls = [];
  routes = {
    "GET /api/v1/me/calendar-feed": () => statusResponse(false),
    "POST /api/v1/me/calendar-feed": () =>
      new Response(JSON.stringify({ url: URL_ONE, createdAt: CREATED_AT }), {
        status: 201,
        headers: { "Content-Type": "application/json" },
      }),
    "DELETE /api/v1/me/calendar-feed": () =>
      new Response(null, { status: 204 }),
    "GET /api/v1/me/calendar.ics": () => calendarResponse(),
  };
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, options: RequestInit = {}) => {
      const method = options.method ?? "GET";
      calls.push({ url, method });
      const handler = routes[`${method} ${url}`];
      if (!handler) throw new Error(`sin ruta para ${method} ${url}`);
      return await handler();
    }),
  );
  vi.spyOn(globalThis.URL, "createObjectURL").mockReturnValue(
    "blob:calendario",
  );
  vi.spyOn(globalThis.URL, "revokeObjectURL").mockImplementation(() => {});
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  observeAccess();
  localStorage.clear();
  sessionStorage.clear();
});

const countOf = (method: string, url: string) =>
  calls.filter((call) => call.method === method && call.url === url).length;

async function open() {
  const view = render(<Calendar owner="ana" />);
  await screen.findByRole("heading", { level: 1, name: "Calendario ICS" });
  return view;
}

it("@s31 announces loading and asks only for the status", async () => {
  let release: (() => void) | undefined;
  routes["GET /api/v1/me/calendar-feed"] = () =>
    new Promise<Response>((resolve) => {
      release = () => resolve(statusResponse(false));
    });
  render(<Calendar owner="ana" />);
  expect(screen.getByRole("status")).toHaveTextContent("Cargando…");
  expect(
    screen.queryByRole("button", { name: "Crear enlace de suscripción" }),
  ).toBeNull();
  expect(screen.queryByRole("button", { name: "Regenerar enlace" })).toBeNull();
  expect(screen.queryByRole("button", { name: "Revocar enlace" })).toBeNull();
  await act(async () => {
    release!();
  });
  expect(calls).toEqual([{ url: "/api/v1/me/calendar-feed", method: "GET" }]);
});

it("@s31 explains the feed and offers only creation when there is no link", async () => {
  await open();
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(screen.getByRole("main")).toHaveTextContent(
    /30 días atrás[\s\S]*un año/,
  );
  expect(screen.getByRole("main")).toHaveTextContent(/secreta/);
  expect(screen.getByRole("main")).toHaveTextContent(
    /solo lectura|sólo lectura/,
  );
  expect(screen.getByRole("main")).toHaveTextContent(/no sincroniza/i);
  expect(screen.getByRole("main")).toHaveTextContent(
    /títulos de tareas y objetivos/,
  );
  expect(screen.queryByRole("button", { name: "Regenerar enlace" })).toBeNull();
  expect(screen.queryByRole("button", { name: "Revocar enlace" })).toBeNull();
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(0);
  expect(countOf("DELETE", "/api/v1/me/calendar-feed")).toBe(0);
});

it("@s31 shows the creation date and both actions when the link is active", async () => {
  routes["GET /api/v1/me/calendar-feed"] = () =>
    statusResponse(true, CREATED_AT);
  await open();
  await screen.findByRole("button", { name: "Regenerar enlace" });
  expect(screen.getByRole("button", { name: "Revocar enlace" })).toBeVisible();
  expect(screen.getByRole("main")).toHaveTextContent(readable(CREATED_AT));
  expect(
    screen.queryByRole("button", { name: "Crear enlace de suscripción" }),
  ).toBeNull();
  expect(screen.queryByRole("textbox")).toBeNull();
  expect(screen.queryByRole("button", { name: "Copiar enlace" })).toBeNull();
});

it("@s31 offers a manual retry that repeats only the status read", async () => {
  routes["GET /api/v1/me/calendar-feed"] = () =>
    new Response(null, { status: 503 });
  await open();
  expect(await screen.findByRole("alert")).toHaveTextContent(/no se pudo/i);
  expect(
    screen.queryByRole("button", { name: "Crear enlace de suscripción" }),
  ).toBeNull();
  routes["GET /api/v1/me/calendar-feed"] = () => statusResponse(false);
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(countOf("GET", "/api/v1/me/calendar-feed")).toBe(2);
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(0);
});

it("@s32 sends exactly one creation on a double activation and shows the url once", async () => {
  await open();
  const create = await screen.findByRole("button", {
    name: "Crear enlace de suscripción",
  });
  await userEvent.dblClick(create);
  const field = await screen.findByRole("textbox", {
    name: "Enlace de suscripción",
  });
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(1);
  expect(field).toHaveTextContent(URL_ONE);
  expect(field).toHaveAttribute("aria-readonly", "true");
  expect(screen.getByRole("button", { name: "Copiar enlace" })).toBeVisible();
  expect(screen.getByRole("main")).toHaveTextContent(readable(CREATED_AT));
  expect(screen.getByRole("main")).toHaveTextContent(/no volverá a mostrarse/);
  expect(screen.getByRole("main")).toHaveTextContent(
    /títulos de tareas y objetivos/,
  );
  expect(
    screen.getByRole("button", { name: "Regenerar enlace" }),
  ).toBeVisible();
  expect(screen.getByRole("button", { name: "Revocar enlace" })).toBeVisible();
  expect(
    screen.queryByRole("button", { name: "Crear enlace de suscripción" }),
  ).toBeNull();
});

it("@s32 announces the creation while it is in flight", async () => {
  await open();
  let release: (() => void) | undefined;
  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Promise<Response>((resolve) => {
      release = () =>
        resolve(
          new Response(
            JSON.stringify({ url: URL_ONE, createdAt: CREATED_AT }),
            {
              status: 201,
              headers: { "Content-Type": "application/json" },
            },
          ),
        );
    });
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Creando enlace…");
  await act(async () => {
    release!();
  });
});

it("@s32 forgets the url when the view is mounted again", async () => {
  const view = await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  view.unmount();
  routes["GET /api/v1/me/calendar-feed"] = () =>
    statusResponse(true, CREATED_AT);
  await open();
  await screen.findByRole("button", { name: "Regenerar enlace" });
  expect(screen.queryByRole("textbox")).toBeNull();
  expect(screen.queryByRole("button", { name: "Copiar enlace" })).toBeNull();
  expect(screen.getByRole("main")).toHaveTextContent(readable(CREATED_AT));
});

async function created() {
  await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  return await screen.findByRole("textbox", { name: "Enlace de suscripción" });
}

it("@s33 copies the exact url with the native clipboard", async () => {
  const writeText = vi.fn().mockResolvedValue(undefined);
  vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } });
  await created();
  const before = calls.length;
  await userEvent.click(screen.getByRole("button", { name: "Copiar enlace" }));
  expect(writeText).toHaveBeenCalledExactlyOnceWith(URL_ONE);
  expect(await screen.findByRole("status")).toHaveTextContent("Enlace copiado");
  expect(calls).toHaveLength(before);
});

it("@s33 never claims success when the clipboard rejects", async () => {
  const writeText = vi.fn().mockRejectedValue(new Error("denegado"));
  vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } });
  await created();
  await userEvent.click(screen.getByRole("button", { name: "Copiar enlace" }));
  await screen.findByText(/selecciona el campo/i);
  expect(screen.queryByText("Enlace copiado")).toBeNull();
});

it("@s33 keeps the url selectable when there is no clipboard at all", async () => {
  vi.stubGlobal("navigator", { ...navigator, clipboard: undefined });
  const field = await created();
  await userEvent.click(screen.getByRole("button", { name: "Copiar enlace" }));
  await screen.findByText(/selecciona el campo/i);
  expect(screen.queryByText("Enlace copiado")).toBeNull();
  expect(field).toHaveTextContent(URL_ONE);
});

async function active() {
  routes["GET /api/v1/me/calendar-feed"] = () =>
    statusResponse(true, CREATED_AT);
  await open();
  await screen.findByRole("button", { name: "Regenerar enlace" });
}

it.each([
  ["Regenerar enlace", "Confirmar regeneración"],
  ["Revocar enlace", "Confirmar revocación"],
])(
  "@s34 %s asks for a deliberate confirmation before any request",
  async (action, confirm) => {
    await active();
    const before = calls.length;
    await userEvent.click(screen.getByRole("button", { name: action }));
    const confirmation = await screen.findByRole("group", {
      name: /el enlace anterior dejará de funcionar/i,
    });
    expect(confirmation).toHaveTextContent(/en cualquier calendario/i);
    expect(confirmation).toHaveFocus();
    expect(screen.getByRole("button", { name: confirm })).toBeVisible();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeVisible();
    expect(calls).toHaveLength(before);
  },
);

it.each(["Regenerar enlace", "Revocar enlace"])(
  "@s35 cancelling %s sends nothing and keeps the active state",
  async (action) => {
    await active();
    await userEvent.click(screen.getByRole("button", { name: action }));
    const before = calls.length;
    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(calls).toHaveLength(before);
    expect(
      screen.getByRole("button", { name: "Regenerar enlace" }),
    ).toBeVisible();
    expect(screen.getByRole("main")).toHaveTextContent(readable(CREATED_AT));
  },
);

it("@s35 confirming the regeneration sends one POST and shows the new url once", async () => {
  await active();
  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(
      JSON.stringify({
        url: URL_TWO,
        createdAt: "2026-09-08T12:05:00.000000Z",
      }),
      { status: 201, headers: { "Content-Type": "application/json" } },
    );
  await userEvent.click(
    screen.getByRole("button", { name: "Regenerar enlace" }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar regeneración" }),
  );
  const field = await screen.findByRole("textbox", {
    name: "Enlace de suscripción",
  });
  expect(field).toHaveTextContent(URL_TWO);
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(1);
  expect(screen.getByRole("main")).toHaveTextContent(
    readable("2026-09-08T12:05:00.000000Z"),
  );
});

it("@s35 confirming the revocation sends one DELETE and returns to the empty state", async () => {
  await active();
  await userEvent.click(screen.getByRole("button", { name: "Revocar enlace" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar revocación" }),
  );
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(countOf("DELETE", "/api/v1/me/calendar-feed")).toBe(1);
  expect(screen.queryByRole("button", { name: "Regenerar enlace" })).toBeNull();
});

it("@s35 moves the focus to the heading when the initiating control disappears", async () => {
  await active();
  await userEvent.click(screen.getByRole("button", { name: "Revocar enlace" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar revocación" }),
  );
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(screen.getByRole("heading", { level: 1 })).toHaveFocus();
});

it("@s35 a failed regeneration retries only by hand and claims nothing", async () => {
  await active();
  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(
    screen.getByRole("button", { name: "Regenerar enlace" }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar regeneración" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(/no se pudo/i);
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(1);
  expect(screen.queryByRole("textbox")).toBeNull();
  expect(screen.getByRole("main")).not.toHaveTextContent(
    /dejó de funcionar|ha dejado de funcionar/,
  );
  expect(screen.getByRole("button", { name: "Reintentar" })).toBeVisible();
});

it("@s36 prepares the file only after validating the answer", async () => {
  await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("status")).toHaveTextContent(
    "Archivo preparado",
  );
  const link = screen.getByRole("link", {
    name: /organizationweb-bloques\.ics/,
  });
  expect(link).toHaveAttribute("download", "organizationweb-bloques.ics");
  expect(link).toHaveAttribute("href", "blob:calendario");
  const blob = vi.mocked(globalThis.URL.createObjectURL).mock
    .calls[0][0] as Blob;
  expect(await blob.text()).toBe(DOCUMENT);
});

it.each([
  [
    "un tipo distinto de text/calendar",
    () =>
      new Response(new TextEncoder().encode(DOCUMENT), {
        headers: { "Content-Type": "text/plain", "Content-Length": "43" },
      }),
  ],
  [
    "menos bytes que Content-Length",
    () =>
      new Response(new TextEncoder().encode(DOCUMENT), {
        headers: {
          "Content-Type": "text/calendar; charset=utf-8",
          "Content-Length": "9999",
        },
      }),
  ],
  [
    "un cuerpo sin END:VCALENDAR final",
    () => calendarResponse("BEGIN:VCALENDAR\r\nEND:VCALENDAR"),
  ],
])("@s36 refuses %s without creating any download", async (_name, handler) => {
  await open();
  routes["GET /api/v1/me/calendar.ics"] = handler;
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("alert")).toBeVisible();
  expect(screen.queryByRole("link", { name: /\.ics/ })).toBeNull();
  expect(globalThis.URL.createObjectURL).not.toHaveBeenCalled();
});

it("@s36 explains the two thousand event limit without offering a file", async () => {
  await open();
  routes["GET /api/v1/me/calendar.ics"] = () =>
    new Response(JSON.stringify({ code: "CALENDAR_TOO_LARGE" }), {
      status: 413,
      headers: { "Content-Type": "application/problem+json" },
    });
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(/2000 eventos/);
  expect(screen.queryByRole("link", { name: /\.ics/ })).toBeNull();
  expect(countOf("GET", "/api/v1/me/calendar.ics")).toBe(1);
});

it("@s37 aborts the flight and drops the url when the view is left", async () => {
  const view = await open();
  let release: ((value: Response) => void) | undefined;
  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Promise<Response>((resolve) => {
      release = resolve;
    });
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  const dropped = vi.fn();
  observeAccess(dropped);
  view.unmount();
  await act(async () => {
    release!(new Response(null, { status: 401 }));
    await Promise.resolve();
  });
  expect(document.body.textContent).not.toContain(URL_ONE);
  expect(dropped).not.toHaveBeenCalled();
});

it("@s31 opens Calendario from the Principal navigation, right after Exportación", async () => {
  window.history.replaceState(null, "", "/no-existe");
  const { App } = await import("./App");
  const { within } = await import("@testing-library/react");
  render(<App username="ana" />);
  const navigation = screen.getByRole("navigation", { name: "Principal" });
  const links = within(navigation).getAllByRole("link");
  const exports = within(navigation).getByRole("link", { name: "Exportación" });
  const calendar = within(navigation).getByRole("link", { name: "Calendario" });
  expect(links.indexOf(calendar)).toBe(links.indexOf(exports) + 1);
  await userEvent.click(calendar);
  expect(window.location.pathname).toBe("/calendario");
  const title = await screen.findByRole("heading", {
    level: 1,
    name: "Calendario ICS",
  });
  expect(title).toHaveFocus();
  expect(calls.map((call) => `${call.method} ${call.url}`)).toEqual([
    "GET /api/v1/me/calendar-feed",
  ]);
  expect(calendar).toHaveAttribute("aria-current", "page");
});

it("@s31 @s37 never writes the url or the token to storage", async () => {
  const logged: unknown[] = [];
  vi.spyOn(console, "log").mockImplementation((...args) => logged.push(args));
  vi.spyOn(console, "error").mockImplementation((...args) => logged.push(args));
  await created();
  await waitFor(() =>
    expect(
      screen.getByRole("textbox", { name: "Enlace de suscripción" }),
    ).toHaveTextContent(URL_ONE),
  );
  const stored = JSON.stringify({
    local: { ...localStorage },
    session: { ...sessionStorage },
    logged,
  });
  expect(stored).not.toContain(TOKEN);
  expect(stored).not.toContain(URL_ONE);
});

// ---------------------------------------------------------------------------
// Huecos señalados por la campaña de mutación (informe frontend, bloques B1–B10).
// ---------------------------------------------------------------------------

function deferred<T>() {
  let settle!: (value: T) => void;
  const promise = new Promise<T>((resolve) => {
    settle = resolve;
  });
  return { promise, resolve: settle };
}

/** Retiene la respuesta de una ruta y devuelve el gatillo para soltarla. */
function hold(key: string) {
  const gate = deferred<Response>();
  routes[key] = () => gate.promise;
  return gate;
}

// --- B8: las tres ramas de reintento ---------------------------------------

it("@s35 reintentar una creación fallida repite el POST y nada más", async () => {
  await open();
  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  expect(await screen.findByRole("alert")).toBeVisible();

  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(JSON.stringify({ url: URL_ONE, createdAt: CREATED_AT }), {
      status: 201,
      headers: { "Content-Type": "application/json" },
    });
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(2);
  expect(countOf("DELETE", "/api/v1/me/calendar-feed")).toBe(0);
  expect(countOf("GET", "/api/v1/me/calendar.ics")).toBe(0);
  expect(countOf("GET", "/api/v1/me/calendar-feed")).toBe(1);
});

it("@s35 reintentar una revocación fallida repite el DELETE y nada más", async () => {
  await active();
  routes["DELETE /api/v1/me/calendar-feed"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(screen.getByRole("button", { name: "Revocar enlace" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar revocación" }),
  );
  expect(await screen.findByRole("alert")).toBeVisible();

  routes["DELETE /api/v1/me/calendar-feed"] = () =>
    new Response(null, { status: 204 });
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(countOf("DELETE", "/api/v1/me/calendar-feed")).toBe(2);
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(0);
  expect(countOf("GET", "/api/v1/me/calendar.ics")).toBe(0);
});

it("@s36 reintentar una descarga fallida repite la descarga y nada más", async () => {
  await open();
  routes["GET /api/v1/me/calendar.ics"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("alert")).toBeVisible();

  routes["GET /api/v1/me/calendar.ics"] = () => calendarResponse();
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));

  await screen.findByText("Archivo preparado");
  expect(countOf("GET", "/api/v1/me/calendar.ics")).toBe(2);
  expect(countOf("POST", "/api/v1/me/calendar-feed")).toBe(0);
  expect(countOf("DELETE", "/api/v1/me/calendar-feed")).toBe(0);
});

// --- B7: el 413 no es un fallo cualquiera ----------------------------------

it("@s36 un fallo de descarga que no es 413 ofrece reintentar", async () => {
  await open();
  routes["GET /api/v1/me/calendar.ics"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(
    /no se pudo completar/i,
  );
  expect(screen.getByRole("alert")).not.toHaveTextContent(/2000 eventos/);
  expect(screen.getByRole("button", { name: "Reintentar" })).toBeVisible();
});

it("@s36 el límite de 2000 eventos no ofrece reintentar, porque repetir no lo resuelve", async () => {
  await open();
  routes["GET /api/v1/me/calendar.ics"] = () =>
    new Response(JSON.stringify({ code: "CALENDAR_TOO_LARGE" }), {
      status: 413,
      headers: { "Content-Type": "application/problem+json" },
    });
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("alert")).toHaveTextContent(/2000 eventos/);
  expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull();
});

// --- B1 y B2: el contrato de foco -------------------------------------------

it("@s31 al abrir la vista el h1 recibe el foco", async () => {
  await open();
  expect(screen.getByRole("heading", { level: 1 })).toHaveFocus();
});

/**
 * El navegador real quita el foco de un control cuando React lo deshabilita y lo deja en el body;
 * jsdom no lo hace, así que aquí se coloca el foco en el body antes de activar y se usa
 * `fireEvent`, que no mueve el foco. Los focusin se emiten explícitamente para ejercer justo el
 * predicado que decide si la persona se movió o no.
 */
function focusOnBody() {
  (screen.getByRole("heading", { level: 1 }) as HTMLElement).blur();
  expect(document.body).toHaveFocus();
}

const enters = (element: Element) =>
  element.dispatchEvent(new FocusEvent("focusin", { bubbles: true }));

it("@s35 si la persona no mueve el foco, vuelve al control que inició la operación", async () => {
  await open();
  const button = await screen.findByRole("button", {
    name: "Descargar archivo .ics",
  });
  const gate = hold("GET /api/v1/me/calendar.ics");
  focusOnBody();
  fireEvent.click(button);

  await act(async () => {
    gate.resolve(calendarResponse());
  });
  await screen.findByText("Archivo preparado");
  expect(button).toHaveFocus();
});

it("@s35 si la persona mueve el foco durante la operación, no se le arrebata", async () => {
  await open();
  const button = await screen.findByRole("button", {
    name: "Descargar archivo .ics",
  });
  const gate = hold("GET /api/v1/me/calendar.ics");
  focusOnBody();
  fireEvent.click(button);
  enters(screen.getByRole("button", { name: "Crear enlace de suscripción" }));

  await act(async () => {
    gate.resolve(calendarResponse());
  });
  await screen.findByText("Archivo preparado");
  expect(button).not.toHaveFocus();
  expect(screen.getByRole("heading", { level: 1 })).not.toHaveFocus();
  expect(document.body).toHaveFocus();
});

it("@s35 que el foco entre en el propio control que inició no cuenta como moverse", async () => {
  await open();
  const button = await screen.findByRole("button", {
    name: "Descargar archivo .ics",
  });
  const gate = hold("GET /api/v1/me/calendar.ics");
  focusOnBody();
  fireEvent.click(button);
  enters(button);

  await act(async () => {
    gate.resolve(calendarResponse());
  });
  await screen.findByText("Archivo preparado");
  expect(button).toHaveFocus();
});

it("@s37 al salir de la vista no queda ninguna escucha de foco colgando", async () => {
  const added = vi.spyOn(document, "addEventListener");
  const removed = vi.spyOn(document, "removeEventListener");
  const view = await open();
  view.unmount();
  const registered = added.mock.calls
    .filter(([type]) => type === "focusin")
    .map(([, handler]) => handler);
  const released = removed.mock.calls
    .filter(([type]) => type === "focusin")
    .map(([, handler]) => handler);
  expect(registered).not.toHaveLength(0);
  expect(released).toEqual(registered);
});

// --- B3: una sola lectura de estado por montaje ----------------------------

it("@s31 la vista lee el estado una sola vez aunque vuelva a renderizar", async () => {
  const view = await open();
  view.rerender(<Calendar owner="ana" />);
  view.rerender(<Calendar owner="ana" />);
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(countOf("GET", "/api/v1/me/calendar-feed")).toBe(1);
});

// --- B4: la respuesta que llega tarde no toca nada -------------------------

it("@s37 una descarga que llega tarde tras salir no prepara ningún archivo", async () => {
  const view = await open();
  const gate = hold("GET /api/v1/me/calendar.ics");
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  view.unmount();
  await act(async () => {
    gate.resolve(calendarResponse());
    await Promise.resolve();
  });
  expect(globalThis.URL.createObjectURL).not.toHaveBeenCalled();
});

// --- B5 y B6: el estado y los anuncios de cada operación -------------------

it("@s35 anuncia Revocando enlace… mientras la revocación está en vuelo", async () => {
  await active();
  const gate = hold("DELETE /api/v1/me/calendar-feed");
  await userEvent.click(screen.getByRole("button", { name: "Revocar enlace" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar revocación" }),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Revocando enlace…");
  await act(async () => {
    gate.resolve(new Response(null, { status: 204 }));
  });
});

it("@s36 anuncia Preparando archivo… mientras la descarga está en vuelo", async () => {
  await open();
  const gate = hold("GET /api/v1/me/calendar.ics");
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(screen.getByRole("status")).toHaveTextContent("Preparando archivo…");
  await act(async () => {
    gate.resolve(calendarResponse());
  });
  await screen.findByText("Archivo preparado");
});

it("@s35 tras revocar no queda enlace, ni aviso de copia, ni confirmación", async () => {
  const writeText = vi.fn().mockResolvedValue(undefined);
  vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } });
  await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  await userEvent.click(screen.getByRole("button", { name: "Copiar enlace" }));
  await screen.findByText("Enlace copiado");

  await userEvent.click(screen.getByRole("button", { name: "Revocar enlace" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar revocación" }),
  );

  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(screen.queryByRole("textbox")).toBeNull();
  expect(screen.queryByText("Enlace copiado")).toBeNull();
  expect(screen.queryByRole("group")).toBeNull();
  expect(screen.getByRole("main")).not.toHaveTextContent(/Enlace creado el/);
});

it("@s35 al regenerar se retira el aviso de copia del enlace anterior", async () => {
  const writeText = vi.fn().mockResolvedValue(undefined);
  vi.stubGlobal("navigator", { ...navigator, clipboard: { writeText } });
  await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  await userEvent.click(screen.getByRole("button", { name: "Copiar enlace" }));
  await screen.findByText("Enlace copiado");

  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(
      JSON.stringify({
        url: URL_TWO,
        createdAt: "2026-09-08T12:05:00.000000Z",
      }),
      { status: 201, headers: { "Content-Type": "application/json" } },
    );
  await userEvent.click(
    screen.getByRole("button", { name: "Regenerar enlace" }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar regeneración" }),
  );

  await screen.findByText(URL_TWO);
  expect(screen.queryByText("Enlace copiado")).toBeNull();
});

it("@s35 al empezar un reintento desaparece el aviso de fallo anterior", async () => {
  await open();
  routes["GET /api/v1/me/calendar.ics"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  expect(await screen.findByRole("alert")).toBeVisible();

  const gate = hold("GET /api/v1/me/calendar.ics");
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(screen.queryByRole("alert")).toBeNull();

  await act(async () => {
    gate.resolve(calendarResponse());
  });
  await screen.findByText("Archivo preparado");
});

it("@s36 el archivo preparado se ofrece como text/calendar", async () => {
  await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  await screen.findByText("Archivo preparado");
  const blob = vi.mocked(globalThis.URL.createObjectURL).mock
    .calls[0][0] as Blob;
  expect(blob.type).toBe("text/calendar;charset=utf-8");
});

// --- B9: las object URL no se quedan colgando ------------------------------

it("@s37 al salir de la vista se revoca la url del archivo preparado", async () => {
  const view = await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Descargar archivo .ics" }),
  );
  await screen.findByText("Archivo preparado");
  expect(globalThis.URL.revokeObjectURL).not.toHaveBeenCalled();
  view.unmount();
  expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith(
    "blob:calendario",
  );
});

it("@s36 una segunda descarga revoca la url de la primera", async () => {
  vi.mocked(globalThis.URL.createObjectURL)
    .mockReturnValueOnce("blob:primera")
    .mockReturnValueOnce("blob:segunda");
  await open();
  const button = await screen.findByRole("button", {
    name: "Descargar archivo .ics",
  });
  await userEvent.click(button);
  await screen.findByText("Archivo preparado");
  await userEvent.click(button);
  await waitFor(() =>
    expect(
      screen.getByRole("link", { name: /organizationweb-bloques\.ics/ }),
    ).toHaveAttribute("href", "blob:segunda"),
  );
  expect(globalThis.URL.revokeObjectURL).toHaveBeenCalledWith("blob:primera");
});

// --- B10: detalles del DOM que sostienen la accesibilidad ------------------

it("@s38 ni el h1 ni la confirmación fuerzan el orden de tabulación", async () => {
  await active();
  expect(screen.getByRole("heading", { level: 1 })).toHaveAttribute(
    "tabindex",
    "-1",
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Regenerar enlace" }),
  );
  expect(screen.getByRole("group")).toHaveAttribute("tabindex", "-1");
});

it("@s38 al enfocar el campo de url su contenido queda seleccionado entero", async () => {
  const selectNodeContents = vi.spyOn(Range.prototype, "selectNodeContents");
  await open();
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  const field = await screen.findByRole("textbox", {
    name: "Enlace de suscripción",
  });
  field.focus();
  // La Selection de jsdom no conserva la extensión del rango, así que aquí se fija la conducta
  // —seleccionar el contenido del campo entero— y el texto realmente seleccionado se afirma en el
  // E2E, con un motor de verdad.
  expect(selectNodeContents).toHaveBeenCalledWith(field);
  expect(window.getSelection()?.rangeCount).toBe(1);
  expect(field).toHaveTextContent(URL_ONE);
});

it("@s35 al confirmar desaparece la confirmación inline", async () => {
  await active();
  await userEvent.click(
    screen.getByRole("button", { name: "Regenerar enlace" }),
  );
  expect(screen.getByRole("group")).toBeVisible();
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar regeneración" }),
  );
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  expect(screen.queryByRole("group")).toBeNull();
});

/**
 * Esta prueba sostiene la invariante de la que depende la equivalencia declarada para las guardas
 * de carrera de `run` (bloque B4 del informe de mutación): mientras una operación está en vuelo
 * ningún control puede iniciar otra, así que `pending.current` nunca llega ocupado a `run`. Si
 * alguien retira el `disabled`, esta prueba se pone roja y aquel argumento deja de valer.
 */
it("@s32 mientras una operación está en vuelo ningún control puede iniciar otra", async () => {
  await active();
  const gate = hold("GET /api/v1/me/calendar.ics");
  await userEvent.click(
    screen.getByRole("button", { name: "Descargar archivo .ics" }),
  );
  for (const name of [
    "Regenerar enlace",
    "Revocar enlace",
    "Descargar archivo .ics",
  ])
    expect(screen.getByRole("button", { name })).toBeDisabled();

  await act(async () => {
    gate.resolve(calendarResponse());
  });
  await screen.findByText("Archivo preparado");
  for (const name of [
    "Regenerar enlace",
    "Revocar enlace",
    "Descargar archivo .ics",
  ])
    expect(screen.getByRole("button", { name })).toBeEnabled();
});

it("@s32 sin enlace, crear también queda bloqueado mientras la creación está en vuelo", async () => {
  await open();
  const gate = hold("POST /api/v1/me/calendar-feed");
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  expect(
    screen.getByRole("button", { name: "Crear enlace de suscripción" }),
  ).toBeDisabled();
  expect(
    screen.getByRole("button", { name: "Descargar archivo .ics" }),
  ).toBeDisabled();

  await act(async () => {
    gate.resolve(
      new Response(JSON.stringify({ url: URL_ONE, createdAt: CREATED_AT }), {
        status: 201,
        headers: { "Content-Type": "application/json" },
      }),
    );
  });
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
});

/**
 * Superviviente 23 de la campaña final (`calendar.tsx:187:21`): forzar el operando izquierdo de
 * `retriable` deja `failure !== "limit"`, de modo que con `failure === null` el botón «Reintentar»
 * aparece en pantalla sin que nada haya fallado. De las 96 pruebas anteriores, la única que afirmaba
 * su ausencia lo hacía con `failure === "limit"`, donde mutante y original coinciden. Reintentar es
 * una respuesta a un fallo: si no hay fallo, no hay nada que reintentar.
 */
it("@s31 @s35 @s36 no ofrece reintentar mientras nada ha fallado", async () => {
  await open();
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull();

  await userEvent.click(
    screen.getByRole("button", { name: "Crear enlace de suscripción" }),
  );
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull();

  await userEvent.click(
    screen.getByRole("button", { name: "Descargar archivo .ics" }),
  );
  await screen.findByText("Archivo preparado");
  expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull();

  await userEvent.click(screen.getByRole("button", { name: "Revocar enlace" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar revocación" }),
  );
  await screen.findByRole("button", { name: "Crear enlace de suscripción" });
  expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull();
});

/** Y la cara complementaria: tras un fallo recuperable sí aparece, y desaparece al resolverlo. */
it("@s35 reintentar aparece con el fallo y se retira cuando el paso sale bien", async () => {
  await open();
  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(null, { status: 503 });
  await userEvent.click(
    await screen.findByRole("button", { name: "Crear enlace de suscripción" }),
  );
  expect(
    await screen.findByRole("button", { name: "Reintentar" }),
  ).toBeVisible();

  routes["POST /api/v1/me/calendar-feed"] = () =>
    new Response(JSON.stringify({ url: URL_ONE, createdAt: CREATED_AT }), {
      status: 201,
      headers: { "Content-Type": "application/json" },
    });
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  await screen.findByRole("textbox", { name: "Enlace de suscripción" });
  expect(screen.queryByRole("button", { name: "Reintentar" })).toBeNull();
});
