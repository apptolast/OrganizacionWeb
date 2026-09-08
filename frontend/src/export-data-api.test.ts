import { afterEach, expect, it, vi } from "vitest";
import { readExportData } from "./export-data-api";
import { observeAccess } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});

const collections = [
  "projects",
  "tasks",
  "taskStatusHistory",
  "availability",
  "plannedBlocks",
  "blockProjections",
  "blockChanges",
  "workSessions",
  "workSessionIntervals",
  "workSessionChanges",
  "appearance",
  "customization",
  "projectCustomFieldValues",
  "taskCustomFieldValues",
];
const document = {
  format: "organizationweb-export",
  schemaVersion: 1,
  exportedAt: "2026-09-08T10:20:30.123456Z",
  owner: "dueña",
  data: Object.fromEntries(collections.map((name) => [name, []])),
  counts: Object.fromEntries(collections.map((name) => [name, 0])),
};
const fileName = "organizationweb-export-v1-20260908T102030123456Z.json";
it("@s25 cancels an incompatible open transport without requesting its payload", async () => {
  const pull = vi.fn();
  const cancel = vi.fn();
  const body = new ReadableStream<Uint8Array>(
    { pull, cancel },
    { highWaterMark: 0 },
  );
  const file = new Response(body, { headers: { "Content-Type": "text/html" } });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(pull).not.toHaveBeenCalled();
  expect(cancel).toHaveBeenCalledOnce();
  expect(body.locked).toBe(false);
});
it("@s25 rejects incomplete JSON with exact byte length", async () => {
  const file = new Response(bytes.slice(0, -2), {
    headers: response().headers,
  });
  file.headers.set("Content-Length", String(bytes.length - 2));
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked ?? false).toBe(false);
});
it("@s25 rejects interrupted stream", async () => {
  const file = new Response(
    new ReadableStream({
      start(stream) {
        stream.enqueue(bytes.slice(0, 10));
        stream.error(new Error("interrupted"));
      },
    }),
    { headers: response().headers },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked ?? false).toBe(false);
});
it("@s25 rejects fractional count", async () => {
  const file = response({
    ...document,
    counts: { ...document.counts, projects: 0.5 },
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked ?? false).toBe(false);
});
it("@s25 rejects non-array collection", async () => {
  const file = response({
    ...document,
    data: { ...document.data, projects: {} },
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked ?? false).toBe(false);
});
it("@s25 rejects additional filename star", async () => {
  const file = response();
  file.headers.set(
    "Content-Disposition",
    `attachment; filename="${fileName}"; filename*=UTF-8''extra.json`,
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked ?? false).toBe(false);
});
it("@s25 rejects zero Content-Length", async () => {
  const file = response();
  file.headers.set("Content-Length", "0");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked ?? false).toBe(false);
});
it("@s24 preserves a complete maximum-sized archive and textual long values without interpreting records", async () => {
  const value = {
    ...document,
    data: {
      ...document.data,
      workSessions: [{ version: "9223372036854775807", note: "🙂 texto" }],
    },
    counts: { ...document.counts, workSessions: 1 },
  };
  const prefix = new TextEncoder().encode(JSON.stringify(value));
  const body = new Uint8Array(33554432).fill(32);
  body.set(prefix);
  const file = new Response(body, { headers: response().headers });
  file.headers.set("Content-Length", String(body.length));
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  const archive = await readExportData("dueña", new AbortController().signal);
  expect(archive.bytes.length).toBe(33554432);
  expect(archive.bytes.every((value, index) => value === body[index])).toBe(
    true,
  );
  expect(archive.fileName).toBe(fileName);
});
it("@s29 settles cancellation while waiting for a stream with no incoming data", async () => {
  const controller = new AbortController();
  let reading!: () => void;
  const ready = new Promise<void>((resolve) => {
    reading = resolve;
  });
  let cancelled = false;
  const body = new ReadableStream<Uint8Array>(
    {
      pull() {
        reading();
      },
      cancel() {
        cancelled = true;
      },
    },
    { highWaterMark: 0 },
  );
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(new Response(body, { headers: response().headers })),
  );
  const result = readExportData("dueña", controller.signal);
  const rejected = expect(result).rejects.toMatchObject({ name: "AbortError" });
  await ready;
  controller.abort();
  await rejected;
  expect(cancelled).toBe(true);
  expect(body.locked).toBe(false);
});
it("@s25 refuses a truncated stream and releases the reader", async () => {
  const file = new Response(bytes.slice(0, -1), {
    headers: response().headers,
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(file.body?.locked).toBe(false);
});
it("@s29 does not revoke the current session for an old request's late 401", async () => {
  const observer = vi.fn();
  observeAccess(observer);
  const controller = new AbortController();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(async () => {
      controller.abort();
      return new Response(JSON.stringify({ code: "UNAUTHENTICATED" }), {
        status: 401,
      });
    }),
  );
  await expect(
    readExportData("dueña", controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
  expect(observer).not.toHaveBeenCalled();
});
it("@s29 cancels and releases an active stream without publishing its late chunk", async () => {
  const controller = new AbortController();
  let cancelled = false;
  const body = new ReadableStream<Uint8Array>(
    {
      pull(stream) {
        stream.enqueue(bytes);
        controller.abort();
      },
      cancel() {
        cancelled = true;
      },
    },
    { highWaterMark: 0 },
  );
  const file = new Response(body, { headers: response().headers });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
  expect(cancelled).toBe(true);
  expect(body.locked).toBe(false);
});
it("@s29 discards an HTTP response arriving after cancellation before reading bytes", async () => {
  const controller = new AbortController();
  const file = response();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(async () => {
      controller.abort();
      return file;
    }),
  );
  await expect(
    readExportData("dueña", controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
  expect(file.bodyUsed).toBe(false);
});
it("@s29 does not start a request after its identity was cancelled", async () => {
  const fetcher = vi.fn().mockResolvedValue(response());
  vi.stubGlobal("fetch", fetcher);
  const controller = new AbortController();
  controller.abort();
  await expect(
    readExportData("dueña", controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
  expect(fetcher).not.toHaveBeenCalled();
});
it("@s28 preserves a rejected HTTP response for the existing access and recovery flow", async () => {
  const rejected = new Response(JSON.stringify({ code: "EXPORT_TOO_LARGE" }), {
    status: 413,
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(rejected));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toBe(rejected);
  expect(rejected.bodyUsed).toBe(false);
});
it("@s25 refuses a UTF-8 BOM rather than silently removing it", async () => {
  const body = new Uint8Array([239, 187, 191, ...bytes]);
  const file = new Response(body, { headers: response().headers });
  file.headers.set("Content-Length", String(body.length));
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});
it("@s25 refuses malformed UTF-8 instead of replacing private record bytes", async () => {
  const valid = new TextEncoder().encode(
    JSON.stringify({
      ...document,
      data: { ...document.data, projects: ["x"] },
      counts: { ...document.counts, projects: 1 },
    }),
  );
  const position = new TextDecoder().decode(valid).indexOf('["x"]');
  const bytePosition = new TextEncoder().encode(
    new TextDecoder().decode(valid).slice(0, position + 2),
  ).length;
  valid[bytePosition] = 255;
  const file = new Response(valid, { headers: response().headers });
  file.headers.set("Content-Length", String(valid.length));
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});
it("@s25 rejects a nonexistent calendar day even when its filename agrees", async () => {
  const file = response({
    ...document,
    exportedAt: "2026-02-30T10:20:30.123456Z",
  });
  file.headers.set(
    "Content-Disposition",
    'attachment; filename="organizationweb-export-v1-20260230T102030123456Z.json"',
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});
it("@s25 requires the attachment timestamp to match exportedAt", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        response({ ...document, exportedAt: "2026-09-08T10:20:30.123457Z" }),
      ),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});
const bytes = new TextEncoder().encode(
  JSON.stringify(document, null, 2) + "\n",
);
function response(value: unknown = document) {
  const body =
    value === document
      ? bytes
      : new TextEncoder().encode(JSON.stringify(value));
  return new Response(body, {
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Content-Length": String(body.length),
      "Content-Disposition": `attachment; filename="${fileName}"`,
    },
  });
}

it("@s1 @s24 receives the complete empty export and preserves its original UTF-8 bytes", async () => {
  const fetcher = vi.fn().mockResolvedValue(response());
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  const archive = await readExportData("dueña", signal);
  expect(Array.from(archive.bytes)).toEqual(Array.from(bytes));
  expect(archive.fileName).toBe(fileName);
  expect(archive.exportedAt).toBe(document.exportedAt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith("/api/v1/me/export", {
    signal,
    headers: { Accept: "application/json" },
  });
});

it("@s25 rejects a path in the attachment name before consuming data", async () => {
  const file = response();
  const read = vi.spyOn(file.body!, "getReader");
  file.headers.set(
    "Content-Disposition",
    `attachment; filename="../${fileName}"`,
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
  expect(file.body?.locked).toBe(false);
});

it("@s25 refuses transformed bytes before reading a gzip response", async () => {
  const file = response();
  const read = vi.spyOn(file.body!, "getReader");
  file.headers.set("Content-Encoding", "gzip");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
  expect(file.body?.locked).toBe(false);
});

it("@s25 refuses a non-JSON content type before reading the body", async () => {
  const file = response();
  const read = vi.spyOn(file.body!, "getReader");
  file.headers.set("Content-Type", "text/html");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
  expect(file.body?.locked).toBe(false);
});

it("@s25 refuses counts that disagree with the received collection", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        response({ ...document, counts: { ...document.counts, projects: 1 } }),
      ),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s25 requires all fourteen collections even when counts are zero", async () => {
  const data = { ...document.data };
  delete data.workSessions;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(response({ ...document, data })),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s25 requires the numeric schema version one", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(response({ ...document, schemaVersion: "1" })),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s25 refuses an unsupported export format", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(response({ ...document, format: "other-export" })),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s25 refuses unknown envelope members without reconstructing a valid subset", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(response({ ...document, extra: "private" })),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s25 refuses an archive belonging to a different authenticated owner", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response()));
  await expect(
    readExportData("otra persona", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s25 refuses a missing length without consuming private bytes", async () => {
  const file = response();
  const read = vi.spyOn(file.body!, "getReader");
  file.headers.delete("Content-Length");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(file));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
  expect(file.body?.locked).toBe(false);
});

it("@s25 stops the stream when a chunk exceeds the declared length", async () => {
  let cancelled = false;
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(bytes);
      controller.enqueue(new TextEncoder().encode(" "));
    },
    pull(controller) {
      controller.close();
    },
    cancel() {
      cancelled = true;
    },
  });
  const headers = response().headers;
  headers.set("Content-Length", String(bytes.length - 1));
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(body, { headers })),
  );
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(cancelled).toBe(true);
  expect(body.locked).toBe(false);
});

it("@s25 rejects an oversized declared file before reading its body", async () => {
  const oversized = response();
  oversized.headers.set("Content-Length", "33554433");
  const read = vi.spyOn(oversized.body!, "getReader");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(oversized));
  await expect(
    readExportData("dueña", new AbortController().signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
  expect(oversized.body?.locked).toBe(false);
});
