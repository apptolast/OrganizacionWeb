import { setCsrfToken, observeAccess } from "./api-client";
import { expect, it, vi } from "vitest";
import {
  readAvailability,
  readAvailabilityZones,
  saveAvailability,
  DAY_KEYS,
} from "./availability-api";
const absent = {
  configured: false,
  zoneId: null,
  dailyMinutes: null,
  updatedAt: null,
};
const emptyTag = '"availability:unconfigured"';
it("@s1 reads confirmed absence without fabricating preferences", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(absent, { headers: { ETag: emptyTag } }),
    );
  await expect(readAvailability(signal)).resolves.toEqual({
    ...absent,
    etag: emptyTag,
  });
  expect(fetcher).toHaveBeenCalledExactlyOnceWith("/api/v1/me/availability", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
});
it.each([201, 204, 400, 401, 403, 412, 500, 503])(
  "@s23 @s25 refuses HTTP %s instead of inventing absence",
  async (status) => {
    const response =
      status === 204
        ? new Response(null, { status })
        : Response.json(absent, { status, headers: { ETag: emptyTag } });
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(response);
    await expect(readAvailability()).rejects.toBe(response);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each([
  null,
  [],
  true,
  1,
  "text",
  {},
  { ...absent, extra: 1 },
  { ...absent, configured: 0 },
  { ...absent, zoneId: "UTC" },
  { ...absent, dailyMinutes: {} },
  { ...absent, updatedAt: "2026-09-06T00:00:00Z" },
  ...Object.keys(absent).map((key) =>
    Object.fromEntries(Object.entries(absent).filter(([name]) => name !== key)),
  ),
])(
  "@s41 rejects malformed absence %# with a controlled error",
  async (body) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, { headers: { ETag: emptyTag } }),
    );
    await expect(readAvailability()).rejects.toThrow(
      "Respuesta de disponibilidad inválida",
    );
  },
);
it.each([
  undefined,
  '"availability:00000000-0000-0000-0000-000000000000:0"',
  'W/"availability:unconfigured"',
  '"other"',
])("@s41 rejects absence tag %#", async (tag) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(absent, { headers: tag ? { ETag: tag } : {} }),
  );
  await expect(readAvailability()).rejects.toThrow(
    "Respuesta de disponibilidad inválida",
  );
});

const minutes = {
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 0,
  SUNDAY: 1440,
};
const configured = {
  configured: true,
  zoneId: "Europe/Madrid",
  dailyMinutes: minutes,
  updatedAt: "2026-09-06T00:00:00.123456Z",
};
const tag = '"availability:a0000000-0000-0000-0000-000000000001:0"';
it.each(["Europe/Madrid", "UTC", "Historical/Unavailable"])(
  "@s3 @s45 preserves configured zone %s without a catalogue lookup",
  async (zoneId) => {
    const value = { ...configured, zoneId };
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(value, { headers: { ETag: tag } }));
    await expect(readAvailability()).resolves.toEqual({ ...value, etag: tag });
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(DAY_KEYS).toEqual([
      "MONDAY",
      "TUESDAY",
      "WEDNESDAY",
      "THURSDAY",
      "FRIDAY",
      "SATURDAY",
      "SUNDAY",
    ]);
  },
);
it.each([
  ...Object.keys(configured).map((key) =>
    Object.fromEntries(
      Object.entries(configured).filter(([name]) => name !== key),
    ),
  ),
  { ...configured, extra: 1 },
  { ...configured, zoneId: null },
  { ...configured, zoneId: 1 },
  { ...configured, zoneId: "" },
  { ...configured, dailyMinutes: null },
  { ...configured, dailyMinutes: [] },
  { ...configured, dailyMinutes: "text" },
  { ...configured, dailyMinutes: { ...minutes, HOLIDAY: 1 } },
  ...DAY_KEYS.map((key) => ({
    ...configured,
    dailyMinutes: Object.fromEntries(
      Object.entries(minutes).filter(([name]) => name !== key),
    ),
  })),
  ...DAY_KEYS.map((key) => ({
    ...configured,
    dailyMinutes: { ...minutes, [key]: -1 },
  })),
  ...[null, "1", true, {}, [], 1.5, 1441].map((value) => ({
    ...configured,
    dailyMinutes: { ...minutes, MONDAY: value },
  })),
  ...[
    null,
    "invalid",
    "2026-02-30T00:00:00Z",
    "2026-09-06T00:00:00+02:00",
    "2026-09-06T00:00:00.1234567Z",
  ].map((updatedAt) => ({ ...configured, updatedAt })),
])(
  "@s41 rejects malformed configured snapshot %# without TypeError",
  async (body) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, { headers: { ETag: tag } }),
    );
    await expect(readAvailability()).rejects.toThrow(
      "Respuesta de disponibilidad inválida",
    );
  },
);
it.each([
  undefined,
  emptyTag,
  "",
  `W/${tag}`,
  `${tag}, ${tag}`,
  '"availability:1-1-1-1-1:0"',
  tag.toUpperCase(),
  tag.replace(':0"', ':01"'),
  tag.replace(':0"', ':-1"'),
  tag.replace(':0"', ':1.5"'),
  tag.replace(':0"', ':9223372036854775808"'),
])("@s41 rejects configured ETag %#", async (etag) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(configured, {
      headers: etag === undefined ? {} : { ETag: etag },
    }),
  );
  await expect(readAvailability()).rejects.toThrow(
    "Respuesta de disponibilidad inválida",
  );
});
it.each([
  "2026-09-06T00:00:00Z",
  "2026-09-06T00:00:00.1Z",
  "2026-09-06T00:00:00.123456Z",
])(
  "@s9 preserves valid instant %s and opaque BIGINT tag",
  async (updatedAt) => {
    const etag = tag.replace(':0"', ':9223372036854775807"');
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...configured, updatedAt }, { headers: { ETag: etag } }),
    );
    await expect(readAvailability()).resolves.toEqual({
      ...configured,
      updatedAt,
      etag,
    });
  },
);

it("@s2 returns exact sorted backend zone catalogue including aliases without Intl normalization", async () => {
  const signal = new AbortController().signal;
  const items = ["CET", "Europe/Madrid", "Historical/Unavailable", "UTC"];
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json({ items }));
  await expect(readAvailabilityZones(signal)).resolves.toEqual(items);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    "/api/v1/me/availability/zones",
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([
  null,
  [],
  true,
  {},
  { items: null },
  { items: "UTC" },
  { items: [] },
  { items: ["UTC"], extra: true },
  { items: [1, "UTC"] },
  { items: ["", "UTC"] },
  { items: [" ", "UTC"] },
  { items: ["UTC", "CET"] },
  { items: ["CET", "CET", "UTC"] },
  { items: ["CET"] },
])("@s30 rejects malformed catalogue %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(readAvailabilityZones()).rejects.toThrow(
    "Catálogo de zonas inválido",
  );
});
it.each([201, 400, 401, 503])(
  "@s23 catalogue rejects HTTP %s without retries",
  async (status) => {
    const response = Response.json({ items: ["UTC"] }, { status });
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(response);
    await expect(readAvailabilityZones()).rejects.toBe(response);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);

it("@s3 sends one exact PUT with CSRF, revision and signal then returns confirmed snapshot", async () => {
  const input = {
    zoneId: configured.zoneId,
    dailyMinutes: minutes,
    ownerId: "must-not-send",
  };
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(configured, { headers: { ETag: tag } }),
    );
  setCsrfToken("synthetic-csrf");
  try {
    await expect(saveAvailability(input, emptyTag, signal)).resolves.toEqual({
      ...configured,
      etag: tag,
    });
  } finally {
    setCsrfToken();
  }
  expect(fetcher).toHaveBeenCalledTimes(1);
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/availability");
  expect(options?.method).toBe("PUT");
  expect(options?.credentials).toBe("same-origin");
  expect(options?.signal).toBe(signal);
  expect(JSON.parse(options?.body as string)).toEqual({
    zoneId: configured.zoneId,
    dailyMinutes: minutes,
  });
  const headers = new Headers(options?.headers);
  expect(headers.get("If-Match")).toBe(emptyTag);
  expect(headers.get("Content-Type")).toBe("application/json");
  expect(headers.get("X-CSRF-TOKEN")).toBe("synthetic-csrf");
});
it.each([
  { ...configured, zoneId: "UTC" },
  ...DAY_KEYS.map((key) => ({
    ...configured,
    dailyMinutes: { ...minutes, [key]: minutes[key] === 0 ? 1 : 0 },
  })),
  absent,
])(
  "@s44 rejects valid confirmation different from sent intent %#",
  async (body) => {
    const fetcher = vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, {
        headers: { ETag: body.configured ? tag : emptyTag },
      }),
    );
    await expect(
      saveAvailability(
        { zoneId: configured.zoneId, dailyMinutes: minutes },
        tag,
      ),
    ).rejects.toThrow("Confirmación de disponibilidad inválida");
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it("@s44 compares against the sent values even if caller mutates its object while awaiting", async () => {
  const input = { zoneId: "Europe/Madrid", dailyMinutes: { ...minutes } };
  let finish!: (response: Response) => void;
  const fetcher = vi.spyOn(globalThis, "fetch").mockImplementationOnce(
    () =>
      new Promise<Response>((resolve) => {
        finish = resolve;
      }),
  );
  const pending = saveAvailability(input, tag);
  input.zoneId = "UTC";
  input.dailyMinutes.MONDAY = 200;
  finish(
    Response.json(
      { ...configured, zoneId: "UTC", dailyMinutes: input.dailyMinutes },
      { headers: { ETag: tag } },
    ),
  );
  await expect(pending).rejects.toThrow(
    "Confirmación de disponibilidad inválida",
  );
  expect(JSON.parse(fetcher.mock.calls[0][1]?.body as string)).toEqual({
    zoneId: "Europe/Madrid",
    dailyMinutes: minutes,
  });
});
it.each([{}, [], true, 1, "+999999-01-01T00:00:00Z", "2026-01-01T00:00:00.Z"])(
  "@s41 rejects invalid instant type or range %#",
  async (updatedAt) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...configured, updatedAt }, { headers: { ETag: tag } }),
    );
    await expect(readAvailability()).rejects.toThrow(
      "Respuesta de disponibilidad inválida",
    );
  },
);
it.each(["+010000-01-01T00:00:00Z", "-000001-01-01T00:00:00.123456Z"])(
  "@s9 preserves expanded UTC year %s",
  async (updatedAt) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...configured, updatedAt }, { headers: { ETag: tag } }),
    );
    await expect(readAvailability()).resolves.toEqual({
      ...configured,
      updatedAt,
      etag: tag,
    });
  },
);
it.each([0, 1440])(
  "@s6 accepts every day at %s and exact no-op confirmation",
  async (value) => {
    const dailyMinutes = Object.fromEntries(
      DAY_KEYS.map((key) => [key, value]),
    ) as typeof minutes;
    const input = { zoneId: "UTC", dailyMinutes };
    const result = {
      configured: true,
      ...input,
      updatedAt: configured.updatedAt,
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(result, { headers: { ETag: tag } }),
    );
    await expect(saveAvailability(input, tag)).resolves.toEqual({
      ...result,
      etag: tag,
    });
  },
);
it.each([201, 400, 401, 403, 412, 415, 428, 500, 503])(
  "@s32 preserves PUT HTTP %s without retry",
  async (status) => {
    const response = Response.json(configured, {
      status,
      headers: { ETag: tag },
    });
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(response);
    await expect(saveAvailability(configured, tag)).rejects.toBe(response);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each([
  { body: { ...configured, extra: 1 }, etag: tag },
  { body: configured, etag: emptyTag },
])(
  "@s32 does not bypass snapshot validation on PUT %#",
  async ({ body, etag }) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, { headers: { ETag: etag } }),
    );
    await expect(saveAvailability(configured, tag)).rejects.toThrow(
      "Respuesta de disponibilidad inválida",
    );
  },
);
const operations = [
  ["preference", (signal?: AbortSignal) => readAvailability(signal)],
  ["zones", (signal?: AbortSignal) => readAvailabilityZones(signal)],
  ["save", (signal?: AbortSignal) => saveAvailability(configured, tag, signal)],
] as const;
it.each(operations)(
  "@s32 %s preserves network failure without retry",
  async (_, operation) => {
    const error = new Error("synthetic network");
    const fetcher = vi.spyOn(globalThis, "fetch").mockRejectedValueOnce(error);
    await expect(operation()).rejects.toBe(error);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each(operations)(
  "@s41 %s preserves malformed JSON as a controlled failure",
  async (_, operation) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      new Response("{", { status: 200, headers: { ETag: tag } }),
    );
    await expect(operation()).rejects.toBeInstanceOf(SyntaxError);
  },
);
it.each(operations)(
  "@s37 cancelled %s never revokes a newer access context",
  async (_, operation) => {
    const controller = new AbortController();
    const listener = vi.fn();
    observeAccess(listener);
    let finish!: (response: Response) => void;
    const fetcher = vi.spyOn(globalThis, "fetch").mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve;
        }),
    );
    try {
      const pending = operation(controller.signal);
      controller.abort();
      const response = Response.json(
        { code: "UNAUTHENTICATED" },
        { status: 401 },
      );
      finish(response);
      await expect(pending).rejects.toBe(response);
      expect(listener).not.toHaveBeenCalled();
      expect(fetcher.mock.calls[0][1]?.signal).toBe(controller.signal);
    } finally {
      observeAccess();
    }
  },
);
it.each(operations)(
  "@s36 current %s still reports lost access",
  async (_, operation) => {
    const listener = vi.fn();
    observeAccess(listener);
    const response = Response.json(
      { code: "UNAUTHENTICATED" },
      { status: 401 },
    );
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    try {
      await expect(operation()).rejects.toBe(response);
      expect(listener).toHaveBeenCalledExactlyOnceWith(401);
    } finally {
      observeAccess();
    }
  },
);
it("@s41 rejects uppercase UUID while preserving lowercase availability prefix", async () => {
  const uppercaseId = tag.replace("a0000000", "A0000000");
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(configured, { headers: { ETag: uppercaseId } }),
  );
  await expect(readAvailability()).rejects.toThrow(
    "Respuesta de disponibilidad inválida",
  );
});

it("@s41 rejects false discriminant despite otherwise valid configured snapshot", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(
        { ...configured, configured: false },
        { headers: { ETag: tag } },
      ),
    );
  await expect(readAvailability()).rejects.toThrow(
    "Respuesta de disponibilidad inválida",
  );
  expect(fetcher).toHaveBeenCalledTimes(1);
});
