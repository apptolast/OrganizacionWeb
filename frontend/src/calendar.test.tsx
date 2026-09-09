import { render, screen, act, waitFor } from "@testing-library/react";
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
  expect(field).toHaveValue(URL_ONE);
  expect(field).toHaveAttribute("readonly");
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
  expect(field).toHaveValue(URL_ONE);
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
  expect(field).toHaveValue(URL_TWO);
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
    ).toHaveValue(URL_ONE),
  );
  const stored = JSON.stringify({
    local: { ...localStorage },
    session: { ...sessionStorage },
    logged,
  });
  expect(stored).not.toContain(TOKEN);
  expect(stored).not.toContain(URL_ONE);
});
