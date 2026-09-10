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
  // El primer render no lo fijaba nadie, y ahí viven cuatro supervivientes: la
  // nota de «todavía no tienes» sólo aparece con loaded (:148), la etiqueta
  // arranca vacía (:93), la región de estado arranca callada (:96) y una carga
  // que va bien no deja ningún aviso de error (:149, que con `true` intenta leer
  // la etiqueta de una suscripción nula y acaba en el catch).
  expect(
    screen.getByText("Todavía no tienes ningún calendario externo."),
  ).toBeInTheDocument();
  expect(screen.getByLabelText("Etiqueta")).toHaveValue("");
  expect(screen.getByRole("status")).toHaveTextContent("");
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

// El andamiaje de la pantalla —la promesa de solo lectura, los nombres accesibles
// de las dos secciones y los atributos de los campos— no lo afirmaba nadie.
it("@s37 el formulario se anuncia por su título y promete que solo se lee", async () => {
  withoutSubscription();
  render(<ExternalCalendar />);
  expect(
    await screen.findByRole("region", { name: "Suscribirte a un calendario" }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      "Muestra en Hoy los eventos de un calendario que ya usas. Solo se lee: esta aplicación nunca escribe en tu proveedor.",
    ),
  ).toBeInTheDocument();
  const label = screen.getByLabelText("Etiqueta");
  expect(label).toHaveAttribute("type", "text");
  expect(label).toHaveAttribute("autocomplete", "off");
  const address = screen.getByLabelText("Dirección secreta iCal");
  expect(address).toHaveAttribute("autocomplete", "off");
  expect(address).toHaveAttribute("spellcheck", "false");
  const described = (address.getAttribute("aria-describedby") ?? "").split(" ");
  expect(
    described.map((id) => document.getElementById(id)?.textContent),
  ).toContain(
    "En Google Calendar: Configuración del calendario, Integrar calendario, Dirección secreta en formato iCal. Trátala como una contraseña.",
  );
  const status = screen.getByRole("status");
  expect(status).toHaveAttribute("aria-atomic", "true");
});

it("@s39 la ficha y su diálogo se nombran solos", async () => {
  withSubscription(synced, [meeting]);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  const card = await screen.findByRole("region", { name: "Trabajo" });
  expect(
    within(card).getByRole("heading", { level: 3, name: "Eventos" }),
  ).toBeInTheDocument();
  await user.click(
    within(card).getByRole("button", { name: "Eliminar suscripción" }),
  );
  const dialog = screen.getByRole("alertdialog", {
    name: "Confirmar la eliminación",
  });
  expect(
    within(dialog).getByText(
      "Se borrarán la suscripción y los eventos guardados.",
    ),
  ).toBeInTheDocument();
});

it("@s37 muestra host y cola pero nunca la dirección completa", async () => {
  withSubscription(subscription);
  render(<ExternalCalendar />);
  const host = await screen.findByText("calendar.google.com");
  expect(screen.getByText(".ics")).toBeInTheDocument();
  // `not.toContain("https://")` por sí solo no puede fallar: el DTO no transporta
  // la dirección completa, así que ningún mutante la haría aparecer. Lo que sí
  // falla si la producción se rompe es la forma exacta del recorte y que el
  // separador quede fuera del árbol de accesibilidad.
  expect(document.body.textContent).not.toContain("https://");
  const shown = host.closest("p")!;
  expect(shown.textContent).toBe("calendar.google.com … .ics");
  expect(shown.querySelector('[aria-hidden="true"]')?.textContent).toBe(" … ");
  expect(screen.getByLabelText("Etiqueta")).toHaveValue("Trabajo");
  expect(
    screen.getByRole("button", { name: "Sincronizar ahora" }),
  ).toBeInTheDocument();
  // La confirmación de borrado arranca cerrada: nadie lo afirmaba, así que el
  // estado inicial podía ser `true` y el diálogo salir solo (superviviente :102).
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
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

// Ninguna prueba fijaba una fecha formateada: el único oráculo era «no es cadena
// vacía», que pasa igual con el objeto de opciones vacío o con la zona ignorada.
// Se usa Asia/Tokyo a propósito, para que el resultado no dependa de la zona de la
// máquina que ejecuta la suite.
it("@s37 las dos fechas de la ficha y las horas de la lista van en la zona de la instantánea", async () => {
  withSubscription(
    {
      ...synced,
      snapshotZoneId: "Asia/Tokyo",
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastAttemptAt: "2030-01-07T12:00:00Z",
    },
    [meeting],
  );
  render(<ExternalCalendar />);
  const sync = await screen.findByText("Última sincronización correcta");
  expect(sync.nextElementSibling?.textContent).toBe("7 ene 2030, 20:00");
  expect(
    screen.getByText("Último intento").nextElementSibling?.textContent,
  ).toBe("7 ene 2030, 21:00");
  expect(screen.getByText("17:00–18:00")).toBeInTheDocument();
});

it("@s37 sin zona de instantánea se usa la del navegador", async () => {
  withSubscription({ ...synced, snapshotZoneId: null }, [meeting]);
  render(<ExternalCalendar />);
  const hour = (value: string) =>
    String(new Date(value).getHours()).padStart(2, "0");
  const attempt = await screen.findByText("Último intento");
  expect(attempt.nextElementSibling?.textContent).toContain(
    `${hour("2030-01-07T11:00:00Z")}:00`,
  );
  expect(
    screen.getByText(
      `${hour("2030-01-07T08:00:00Z")}:00–${hour("2030-01-07T09:00:00Z")}:00`,
    ),
  ).toBeInTheDocument();
});

it("@s37 avisa cuando la instantánea quedó truncada, y solo entonces", async () => {
  withSubscription({ ...synced, truncated: true });
  render(<ExternalCalendar />);
  const notice = await screen.findByRole("note");
  expect(notice).toHaveTextContent(
    "Solo se conservan los 500 primeros eventos de la ventana.",
  );
});

it("@s37 no avisa de truncado cuando la instantánea está completa", async () => {
  withSubscription({ ...synced, truncated: false });
  render(<ExternalCalendar />);
  await screen.findByText("calendar.google.com");
  expect(screen.queryByRole("note")).not.toBeInTheDocument();
  expect(
    screen.queryByText(/solo se conservan los 500 primeros eventos/i),
  ).not.toBeInTheDocument();
});

// @s19: un VEVENT sin SUMMARY se guarda como "". La API ya lo declara válido; el
// hueco estaba solo en la vista, que nunca pintó un resumen vacío.
it("@s19 un evento sin resumen se lista como Sin título", async () => {
  withSubscription(synced, [{ ...meeting, summary: "" }]);
  render(<ExternalCalendar />);
  const item = await screen.findByRole("listitem");
  expect(item).toHaveTextContent("Sin título");
  expect(item.textContent).toBe("Sin título 09:00–10:00");
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

// @s12 y la línea 15 del contrato: los siete códigos son un contrato cerrado. Si
// FEED_MESSAGES perdiera una clave, el role="alert" saldría vacío, que es peor que
// no mostrarlo. Cuatro de los siete no se renderizaban en ninguna prueba.
it.each([
  ["FEED_REJECTED", "apunta a una red interna"],
  ["FEED_UNREACHABLE", "no se ha podido contactar con el proveedor"],
  ["FEED_HTTP_ERROR", "vuelve a generar la dirección secreta"],
  ["FEED_TOO_LARGE", "ocupa más de 1 mib"],
  ["FEED_UNSUPPORTED_TYPE", "esa dirección no devuelve un calendario"],
  ["FEED_MALFORMED", "no es un calendario icalendar"],
  ["SECRET_UNREADABLE", "ya no se puede leer la dirección guardada"],
])("@s12 explica qué hacer ante %s", async (lastError, fragment) => {
  withSubscription({ ...synced, lastStatus: "FAILED", lastError });
  render(<ExternalCalendar />);
  const alert = await screen.findByRole("alert");
  expect(alert.textContent?.toLowerCase()).toContain(fragment);
  expect(alert.textContent?.length).toBeGreaterThan(fragment.length);
});

it.each([
  ["un estado correcto con código de fallo", "OK", "FEED_MALFORMED"],
  ["un fallo sin código", "FAILED", null],
])(
  "@s12 no pinta ninguna alerta con %s",
  async (_name, lastStatus, lastError) => {
    withSubscription({ ...synced, lastStatus, lastError });
    render(<ExternalCalendar />);
    await screen.findByText("calendar.google.com");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  },
);

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
  // Lo que el usuario teclea tiene que LLEGAR a la petición, los dos campos. Sólo
  // se afirmaba de la dirección, así que el onChange de Etiqueta podía no hacer
  // nada y la suscripción se guardaba con la etiqueta vacía sin que cayera una
  // prueba (superviviente medido external-calendar.tsx:291, ArrowFunction).
  expect(
    JSON.parse(calls.find((call) => call.method === "PUT")!.body!),
  ).toEqual({
    label: "Trabajo",
    url: "https://calendar.google.com/a.ics",
  });
  // @s40: aria-busy es la única señal programática de que el formulario está
  // ocupado, y hasta ahora solo se comprobaba la región role="status".
  const form = screen.getByLabelText("Etiqueta").closest("form")!;
  expect(form).toHaveAttribute("aria-busy", "true");
  expect(
    screen.getByRole("heading", { name: "Suscribirte a un calendario" }),
  ).toBeInTheDocument();
  release?.();
  expect(await screen.findByText("Guardado.")).toBeInTheDocument();
  expect(form).toHaveAttribute("aria-busy", "false");
  expect(screen.getByLabelText("Dirección secreta iCal")).toHaveValue("");
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
  expect(
    screen.getByRole("heading", { name: "Cambiar la suscripción" }),
  ).toBeInTheDocument();
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

// @s38 fila 3 pide un mensaje distinto del genérico cuando los conectores están
// deshabilitados. Las dos filas compartían la aserción /no sabemos si se guardó/,
// que es cierta en los dos casos: borrando entero el bloque de
// ConnectorsDisabledError de failed() las dos seguían verdes. Ahora cada fila fija
// su texto completo y niega el de la otra.
const UNCERTAIN =
  "No sabemos si se guardó. Vuelve a intentarlo cuando quieras.";
const DISABLED_PREFIX = "Los conectores externos no están disponibles.";

it.each([
  [
    "503",
    { status: 503, code: "CONNECTORS_DISABLED" },
    503,
    `${DISABLED_PREFIX} ${UNCERTAIN}`,
  ],
  ["red", "network", 200, UNCERTAIN],
])(
  "@s38 deja estado incierto y conserva el borrador con %s",
  async (_name, body, status, message) => {
    withoutSubscription();
    answer(ROUTE, "PUT", body, status);
    const user = userEvent.setup();
    render(<ExternalCalendar />);
    await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
    const address = screen.getByLabelText("Dirección secreta iCal");
    await user.type(address, "https://calendar.google.com/a.ics");
    await user.click(screen.getByRole("button", { name: "Guardar" }));
    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(message);
    expect(alert.textContent).toBe(message);
    expect(address).toHaveValue("https://calendar.google.com/a.ics");
    expect(screen.getByRole("button", { name: "Guardar" })).toBeEnabled();
    // Y la región viva deja de decir «Guardando…»: si no, el role=status
    // anunciaría un guardado en curso mientras el role=alert dice que falló.
    expect(screen.getByRole("status").textContent).toBe("");
  },
);

// @s4 tiene cinco filas de label. El ternario de failed() solo se ejercía por la
// rama url, así que un error de etiqueta podía estar enfocando el campo de
// dirección sin que nadie se enterara.
it("@s38 un error de etiqueta se asocia a Etiqueta y le devuelve el foco", async () => {
  withoutSubscription();
  answer(
    ROUTE,
    "PUT",
    {
      status: 400,
      code: "VALIDATION_ERROR",
      errors: [
        {
          field: "label",
          code: "TOO_LONG",
          message: "La etiqueta es demasiado larga.",
        },
      ],
    },
    400,
  );
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  const labelField = await screen.findByLabelText("Etiqueta");
  await user.type(labelField, "Trabajo");
  const address = screen.getByLabelText("Dirección secreta iCal");
  await user.type(address, "https://calendar.google.com/a.ics");
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(
    await screen.findByText("La etiqueta es demasiado larga."),
  ).toBeInTheDocument();
  expect(labelField).toHaveAttribute("aria-invalid", "true");
  expect(address).toHaveAttribute("aria-invalid", "false");
  await waitFor(() => expect(labelField).toHaveFocus());
  // «El error se asocia al campo» (@s38 fila 2) es una asociación programática, no
  // una proximidad visual: el mensaje tiene que estar entre los descritos por el
  // campo. Sin esto, vaciar el aria-describedby no rompía ninguna prueba.
  const described = (labelField.getAttribute("aria-describedby") ?? "").split(
    " ",
  );
  expect(
    described.map((id) => document.getElementById(id)?.textContent),
  ).toContain("La etiqueta es demasiado larga.");
});

// La guarda es `instanceof Response && status === 401`: con el mutante && -> ||
// cualquier respuesta de error borraría la suscripción de la pantalla.
it("@s38 un 500 al guardar deja el estado incierto sin retirar la suscripción", async () => {
  withSubscription(synced, [meeting]);
  answer(ROUTE, "PUT", {}, 500);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("calendar.google.com");
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://x.test/a.ics",
  );
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(await screen.findByText(UNCERTAIN)).toBeInTheDocument();
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
  expect(screen.getByText(".ics")).toBeInTheDocument();
  expect(screen.getByText("Reunión")).toBeInTheDocument();
});

// Ninguna prueba encadenaba error y reintento, así que se podían borrar los
// reinicios de estado (setFieldErrors({}), setFailure("")) sin romper nada.
it("@s38 al reintentar se limpian el error de campo y el aviso anterior", async () => {
  withoutSubscription();
  answer(
    ROUTE,
    "PUT",
    {
      status: 400,
      code: "VALIDATION_ERROR",
      errors: [
        { field: "url", code: "BLOCKED_ADDRESS", message: "Red interna." },
      ],
    },
    400,
  );
  answer(ROUTE, "PUT", { configured: true, subscription: synced });
  answer(`${ROUTE}/events`, "GET", emptyEvents);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
  const address = screen.getByLabelText("Dirección secreta iCal");
  await user.type(address, "https://10.0.0.5/a.ics");
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(await screen.findByText("Red interna.")).toBeInTheDocument();
  await user.clear(address);
  await user.type(address, "https://calendar.google.com/a.ics");
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  await waitFor(() =>
    expect(screen.queryByText("Red interna.")).not.toBeInTheDocument(),
  );
  expect(screen.getByLabelText("Dirección secreta iCal")).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  // @s40: los estados se anuncian sin mover el foco. Al limpiar los errores el
  // efecto de foco vuelve a dispararse, y solo el `focusOn.current = null` impide
  // que le robe el foco al botón que el propietario acaba de pulsar.
  expect(screen.getByRole("button", { name: "Guardar" })).toHaveFocus();
});

it("@s38 un reintento con éxito retira el aviso de estado incierto", async () => {
  withoutSubscription();
  answer(ROUTE, "PUT", { status: 503, code: "CONNECTORS_DISABLED" }, 503);
  answer(ROUTE, "PUT", { configured: true, subscription: synced });
  answer(`${ROUTE}/events`, "GET", emptyEvents);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://calendar.google.com/a.ics",
  );
  const save = screen.getByRole("button", { name: "Guardar" });
  await user.click(save);
  expect(await screen.findByRole("alert")).toBeInTheDocument();
  await user.click(save);
  expect(await screen.findByText("Guardado.")).toBeInTheDocument();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

// forget() cierra el diálogo además de vaciar la ficha. Que lo cierre no se nota
// al borrar —la sección entera desaparece— pero sí después: si el estado se
// quedara en «confirmando», al dar de alta una suscripción nueva la pantalla
// aparecería con el diálogo de eliminación abierto sin que nadie lo pidiera.
it("@s39 tras eliminar y volver a suscribirse no reaparece el diálogo de confirmación", async () => {
  withSubscription(synced, [meeting]);
  answer(ROUTE, "DELETE", null, 204);
  answer(ROUTE, "PUT", { configured: true, subscription: synced });
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.click(
    await screen.findByRole("button", { name: "Eliminar suscripción" }),
  );
  await user.click(screen.getByRole("button", { name: "Sí, eliminar" }));
  await waitFor(() =>
    expect(screen.queryByText("calendar.google.com")).not.toBeInTheDocument(),
  );
  await user.type(await screen.findByLabelText("Etiqueta"), "Trabajo");
  await user.type(
    screen.getByLabelText("Dirección secreta iCal"),
    "https://calendar.google.com/a.ics",
  );
  await user.click(screen.getByRole("button", { name: "Guardar" }));
  expect(await screen.findByText("calendar.google.com")).toBeInTheDocument();
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
});

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
  // Segundo intento con la primera petición aún en vuelo. Quien lo impide aquí es
  // el atributo disabled: user-event no despacha el clic sobre un botón
  // deshabilitado, así que synchronise() ni se ejecuta. El guardián de reentrada
  // de synchronise no es alcanzable desde la interfaz; el que sí lo es, y tiene su
  // propia prueba, es el de confirmRemoval.
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
  // @s38 fila 7: «la lista anterior permanece». Permanece porque una sincronización
  // fallida NO vuelve a pedir /events; contar la petición es lo único que distingue
  // eso de recargarla y que por casualidad devuelva lo mismo.
  expect(
    calls.filter(
      (call) => call.method === "GET" && call.url.includes("/events"),
    ),
  ).toHaveLength(1);
  expect(screen.getByRole("status").textContent).toBe(
    "Sincronización fallida.",
  );
});

it("@s38 una sincronización correcta sí vuelve a pedir la lista y lo anuncia", async () => {
  withSubscription(synced, [meeting]);
  answer(`${ROUTE}/sync`, "POST", { performed: true, subscription: synced });
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("Reunión");
  await user.click(screen.getByRole("button", { name: "Sincronizar ahora" }));
  await waitFor(() =>
    expect(screen.getByRole("status").textContent).toBe("Sincronizado."),
  );
  expect(
    calls.filter(
      (call) => call.method === "GET" && call.url.includes("/events"),
    ),
  ).toHaveLength(2);
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

// El catch de synchronise() tiene dos ramas y ninguna prueba pasaba por la
// segunda: el 404 de EXTERNAL_CALENDAR_NOT_CONFIGURED sí, pero ningún otro
// error. Por eso external-calendar.tsx:236 sobrevivía con `true` (cualquier
// fallo retiraba la suscripción de la pantalla) y :237 salía sin cobertura
// (`else failed(error)` no se ejecutaba nunca). Un 500 distingue las dos.
it("@s38 un 500 al sincronizar deja el estado incierto sin retirar la suscripción", async () => {
  withSubscription(synced, [meeting]);
  answer(`${ROUTE}/sync`, "POST", {}, 500);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await screen.findByText("Reunión");
  await user.click(screen.getByRole("button", { name: "Sincronizar ahora" }));
  expect(await screen.findByText(UNCERTAIN)).toBeInTheDocument();
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
  expect(screen.getByLabelText("Etiqueta")).toHaveValue("Trabajo");
  expect(screen.getByText("Reunión")).toBeInTheDocument();
});

it("@s39 no envía DELETE si se cancela la confirmación", async () => {
  withSubscription(synced, [meeting]);
  const user = userEvent.setup();
  render(<ExternalCalendar />);
  await user.click(
    await screen.findByRole("button", { name: "Eliminar suscripción" }),
  );
  expect(screen.getByRole("alertdialog")).toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Cancelar" }));
  expect(calls.some((call) => call.method === "DELETE")).toBe(false);
  expect(screen.getByText("calendar.google.com")).toBeInTheDocument();
  // Sin esto, borrar el setConfirming(false) de Cancelar deja el diálogo abierto
  // y la prueba seguía verde porque solo miraba que no se enviara el DELETE.
  expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
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
