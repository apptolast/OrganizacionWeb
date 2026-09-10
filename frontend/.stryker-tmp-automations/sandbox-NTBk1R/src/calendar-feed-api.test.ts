// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import {
  createCalendarFeed,
  readCalendarFeed,
  readCalendarFile,
  revokeCalendarFeed,
} from "./calendar-feed-api";
import { observeAccess } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});

const url =
  "https://organizacion.apptolast.com/calendar/" + "a".repeat(43) + ".ics";
const document = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nEND:VCALENDAR\r\n";
const bytes = new TextEncoder().encode(document);

function calendar(
  overrides: {
    body?: BodyInit | null;
    headers?: HeadersInit;
    status?: number;
  } = {},
) {
  const headers = new Headers({
    "Content-Type": "text/calendar; charset=utf-8",
    "Content-Length": String(bytes.length),
    ...(overrides.headers as Record<string, string> | undefined),
  });
  return new Response(overrides.body === undefined ? bytes : overrides.body, {
    status: overrides.status ?? 200,
    headers,
  });
}

const signal = () => new AbortController().signal;

it("@s31 reads the inactive status as exactly active and createdAt", async () => {
  const fetched = vi.fn().mockResolvedValue(
    new Response(JSON.stringify({ active: false, createdAt: null }), {
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetched);
  await expect(readCalendarFeed(signal())).resolves.toEqual({
    active: false,
    createdAt: null,
  });
  expect(fetched.mock.calls[0][0]).toBe("/api/v1/me/calendar-feed");
  expect(fetched.mock.calls[0][1].method ?? "GET").toBe("GET");
});

it("@s31 reads the active status with its creation instant", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          active: true,
          createdAt: "2026-09-08T12:00:00.000000Z",
        }),
        { headers: { "Content-Type": "application/json" } },
      ),
    ),
  );
  await expect(readCalendarFeed(signal())).resolves.toEqual({
    active: true,
    createdAt: "2026-09-08T12:00:00.000000Z",
  });
});

it("@s31 refuses a status that is not exactly the two expected fields", async () => {
  for (const payload of [
    { active: true },
    { active: true, createdAt: "2026-09-08T12:00:00.000000Z", url },
    { active: "true", createdAt: null },
    { active: true, createdAt: "ayer" },
    { active: false, createdAt: "2026-09-08T12:00:00.000000Z" },
    { active: true, createdAt: null },
  ]) {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(payload), {
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );
    await expect(readCalendarFeed(signal())).rejects.toThrow();
  }
});

it("@s32 sends the creation with an empty body and returns the url once", async () => {
  const fetched = vi.fn().mockResolvedValue(
    new Response(
      JSON.stringify({ url, createdAt: "2026-09-08T12:00:00.000000Z" }),
      {
        status: 201,
        headers: { "Content-Type": "application/json" },
      },
    ),
  );
  vi.stubGlobal("fetch", fetched);
  await expect(createCalendarFeed(signal())).resolves.toEqual({
    url,
    createdAt: "2026-09-08T12:00:00.000000Z",
  });
  expect(fetched.mock.calls[0][0]).toBe("/api/v1/me/calendar-feed");
  expect(fetched.mock.calls[0][1].method).toBe("POST");
  expect(fetched.mock.calls[0][1].body).toBeUndefined();
});

it("@s32 accepts the address of a deployment served over plain http", async () => {
  const plain = `http://127.0.0.1:18092/calendar/${"a".repeat(43)}.ics`;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          url: plain,
          createdAt: "2026-09-08T12:00:00.000000Z",
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    ),
  );
  await expect(createCalendarFeed(signal())).resolves.toEqual({
    url: plain,
    createdAt: "2026-09-08T12:00:00.000000Z",
  });
});

it("@s32 refuses a creation answer whose url is not the public feed address", async () => {
  for (const payload of [
    {
      url: "https://otro.example/calendar/x.ics",
      createdAt: "2026-09-08T12:00:00.000000Z",
    },
    { url, createdAt: null },
    { url, createdAt: "2026-09-08T12:00:00.000000Z", active: true },
    { createdAt: "2026-09-08T12:00:00.000000Z" },
  ]) {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(payload), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );
    await expect(createCalendarFeed(signal())).rejects.toThrow();
  }
});

it("@s35 revokes with DELETE and accepts only 204", async () => {
  const fetched = vi
    .fn()
    .mockResolvedValue(new Response(null, { status: 204 }));
  vi.stubGlobal("fetch", fetched);
  await expect(revokeCalendarFeed(signal())).resolves.toBeUndefined();
  expect(fetched.mock.calls[0][1].method).toBe("DELETE");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(null, { status: 200 })),
  );
  await expect(revokeCalendarFeed(signal())).rejects.toBeDefined();
});

it("@s36 accepts a well formed calendar file and keeps its bytes", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(calendar()));
  const file = await readCalendarFile(signal());
  expect(Array.from(file.bytes)).toEqual(Array.from(bytes));
  expect(file.fileName).toBe("organizationweb-bloques.ics");
});

it("@s36 refuses a content type that is not text/calendar", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        calendar({ headers: { "Content-Type": "text/plain" } }),
      ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

it("@s36 refuses a body shorter than the declared length", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        body: bytes.slice(0, -3),
        headers: { "Content-Length": String(bytes.length) },
      }),
    ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

it("@s36 refuses a body that does not end with END:VCALENDAR and CRLF", async () => {
  const truncated = new TextEncoder().encode(
    "BEGIN:VCALENDAR\r\nEND:VCALENDAR",
  );
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        body: truncated,
        headers: { "Content-Length": String(truncated.length) },
      }),
    ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

it("@s36 refuses a body that does not begin with BEGIN:VCALENDAR", async () => {
  const other = new TextEncoder().encode("VERSION:2.0\r\nEND:VCALENDAR\r\n");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        body: other,
        headers: { "Content-Length": String(other.length) },
      }),
    ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

it("@s36 hands a 413 over as the response so the view can explain the limit", async () => {
  const refusal = new Response(JSON.stringify({ code: "CALENDAR_TOO_LARGE" }), {
    status: 413,
    headers: { "Content-Type": "application/problem+json" },
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(refusal));
  await expect(readCalendarFile(signal())).rejects.toBe(refusal);
});

it("@s37 stops before requesting when the signal is already aborted", async () => {
  const fetched = vi.fn();
  vi.stubGlobal("fetch", fetched);
  const controller = new AbortController();
  controller.abort();
  for (const call of [
    () => readCalendarFeed(controller.signal),
    () => createCalendarFeed(controller.signal),
    () => revokeCalendarFeed(controller.signal),
    () => readCalendarFile(controller.signal),
  ])
    await expect(call()).rejects.toBeDefined();
  expect(fetched).not.toHaveBeenCalled();
});

// ---------------------------------------------------------------------------
// Huecos señalados por la campaña de mutación (informe frontend, puntos 1–32).
// ---------------------------------------------------------------------------

/** Punto 20: la forma que Tomcat entrega de verdad, sin el espacio opcional tras el «;». */
it("@s36 acepta el Content-Type sin el espacio opcional, que es el que emite el servidor", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        headers: { "Content-Type": "text/calendar;charset=utf-8" },
      }),
    ),
  );
  const file = await readCalendarFile(signal());
  expect(Array.from(file.bytes)).toEqual(Array.from(bytes));
});

/** Puntos 17, 18 y 19: el tipo se compara entero, no por coincidencia parcial. */
it.each([
  ["application/json, text/calendar; charset=utf-8", "otro tipo delante"],
  ["text/calendar; charset=utf-8; boundary=x", "parámetros de más detrás"],
  ["text/calendarX;charset=utf-8", "subtipo distinto pegado"],
  ["text/calendar; charset=iso-8859-1", "otra codificación"],
])("@s36 rechaza el Content-Type %s (%s)", async (value) => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(calendar({ headers: { "Content-Type": value } })),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

/** Punto 21: sin cabecera de tipo no hay documento que valga. */
it("@s36 rechaza una descarga sin Content-Type", async () => {
  const response = new Response(bytes, {
    headers: { "Content-Length": String(bytes.length) },
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

/** Puntos 22, 23, 24, 25, 26 y 27: la longitud declarada es un entero decimal completo. */
it.each([
  [undefined, "ausente"],
  ["", "vacía"],
  ["x12", "con basura delante"],
  ["12x", "con basura detrás"],
  ["0", "cero"],
  ["012", "con cero a la izquierda"],
])("@s36 rechaza un Content-Length %s (%s)", async (...row) => {
  const value = row[0];
  const headers = new Headers({
    "Content-Type": "text/calendar; charset=utf-8",
  });
  if (value !== undefined) headers.set("Content-Length", value);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(bytes, { headers })),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

/** Punto 27: la longitud no tiene un número fijo de cifras. */
it.each([
  ["BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n", "dos cifras"],
  [
    "BEGIN:VCALENDAR\r\n" +
      "X-NOTA:relleno\r\n".repeat(20) +
      "END:VCALENDAR\r\n",
    "tres cifras",
  ],
])("@s36 acepta un documento cuya longitud tiene %s", async (body) => {
  const octets = new TextEncoder().encode(body);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        body: octets,
        headers: { "Content-Length": String(octets.length) },
      }),
    ),
  );
  const file = await readCalendarFile(signal());
  expect(file.bytes.byteLength).toBe(octets.length);
});

/** Punto 30: `fatal` es lo único que rechaza UTF-8 roto en vez de colar U+FFFD. */
it("@s36 rechaza un documento con una secuencia UTF-8 inválida", async () => {
  const broken = new Uint8Array([
    ...new TextEncoder().encode("BEGIN:VCALENDAR\r\nX-NOTA:"),
    0xff,
    0xfe,
    ...new TextEncoder().encode("\r\nEND:VCALENDAR\r\n"),
  ]);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        body: broken,
        headers: { "Content-Length": String(broken.length) },
      }),
    ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

/** Punto 31: con BOM el documento ya no empieza por BEGIN:VCALENDAR y no se acepta. */
it("@s36 rechaza un documento que empieza por BOM", async () => {
  const withBom = new Uint8Array([0xef, 0xbb, 0xbf, ...bytes]);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      calendar({
        body: withBom,
        headers: { "Content-Length": String(withBom.length) },
      }),
    ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow();
});

/** Puntos 14, 15 y 16: la descarga negocia texto de calendario y es cancelable. */
it("@s36 pide text/calendar y viaja con la señal de cancelación", async () => {
  const fetched = vi.fn().mockResolvedValue(calendar());
  vi.stubGlobal("fetch", fetched);
  const controller = new AbortController();
  await readCalendarFile(controller.signal);
  const options = fetched.mock.calls[0][1];
  expect(new Headers(options.headers).get("Accept")).toBe("text/calendar");
  expect(options.signal).toBe(controller.signal);
});

/** Punto 4: la lectura de estado también viaja con su señal. */
it("@s31 la lectura de estado viaja con la señal de cancelación", async () => {
  const fetched = vi
    .fn()
    .mockResolvedValue(Response.json({ active: false, createdAt: null }));
  vi.stubGlobal("fetch", fetched);
  const controller = new AbortController();
  await readCalendarFeed(controller.signal);
  expect(fetched.mock.calls[0][1].signal).toBe(controller.signal);
});

/** Puntos 5, 9, 13, 28 y 32: abortar después de la respuesta detiene cada lectura. */
it.each([
  [
    "el estado",
    () => Response.json({ active: false, createdAt: null }),
    readCalendarFeed,
  ],
  [
    "la creación",
    () =>
      new Response(
        JSON.stringify({ url, createdAt: "2026-09-08T12:00:00.000000Z" }),
        {
          status: 201,
          headers: { "Content-Type": "application/json" },
        },
      ),
    createCalendarFeed,
  ],
  [
    "la revocación",
    () => new Response(null, { status: 204 }),
    revokeCalendarFeed,
  ],
  ["la descarga", () => calendar(), readCalendarFile],
])(
  "@s37 abortar tras la respuesta detiene %s antes de entregar nada",
  async (_name, make, call) => {
    const controller = new AbortController();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(async () => {
        const response = make();
        controller.abort();
        return response;
      }),
    );
    await expect(call(controller.signal)).rejects.toMatchObject({
      name: "AbortError",
    });
  },
);

/** Punto 6: un 500 con cuerpo bien formado sigue siendo un fallo, no un estado. */
it("@s30 entrega la respuesta cuando el estado no es 200", async () => {
  const failure = Response.json(
    { active: true, createdAt: null },
    { status: 500 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  await expect(readCalendarFeed(signal())).rejects.toBe(failure);
});

/** Punto 10: crear responde 201; un 200 no es una creación. */
it("@s32 entrega la respuesta cuando la creación no responde 201", async () => {
  const wrong = Response.json({
    url,
    createdAt: "2026-09-08T12:00:00.000000Z",
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(wrong));
  await expect(createCalendarFeed(signal())).rejects.toBe(wrong);
});

/** Puntos 7 y 11: un cuerpo que no es JSON es incompatible, no un valor vacío. */
it.each([
  ["el estado", 200, readCalendarFeed],
  ["la creación", 201, createCalendarFeed],
])(
  "@s31 @s32 rechaza %s con un cuerpo que no es JSON",
  async (_name, status, call) => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response("no soy json", {
          status,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );
    await expect(call(signal())).rejects.toThrow();
  },
);

/** Punto 8: `active` es un booleano, no cualquier cosa que parezca verdadera. */
it("@s31 rechaza un active que no es booleano aunque el resto encaje", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        active: "true",
        createdAt: "2026-09-08T12:00:00.000000Z",
      }),
    ),
  );
  await expect(readCalendarFeed(signal())).rejects.toThrow();
});

/** Puntos 1 y 2: la dirección se compara entera, sin prefijo ni sufijo colgando. */
it.each([
  [`javascript:${url}`, "con basura antes del esquema"],
  [`${url}/../otra`, "con camino colgando detrás"],
])("@s32 rechaza una url %s", async (value) => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          url: value,
          createdAt: "2026-09-08T12:00:00.000000Z",
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    ),
  );
  await expect(createCalendarFeed(signal())).rejects.toThrow();
});

/** Punto 12: la url prometida es una cadena, no algo que se coacciona a una. */
it("@s32 rechaza una url que no es una cadena aunque se coaccione a la correcta", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          url: [url],
          createdAt: "2026-09-08T12:00:00.000000Z",
        }),
        { status: 201, headers: { "Content-Type": "application/json" } },
      ),
    ),
  );
  await expect(createCalendarFeed(signal())).rejects.toThrow();
});

/** Punto 3: el fallo se explica; un mensaje vacío no dice nada a quien lo lee. */
it("@s36 explica por qué la respuesta es incompatible", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        calendar({ headers: { "Content-Type": "text/plain" } }),
      ),
  );
  await expect(readCalendarFile(signal())).rejects.toThrow(
    "Respuesta del calendario incompatible.",
  );
});
