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
