import { File as NodeFile } from "node:buffer";
import { webcrypto } from "node:crypto";
import { afterEach, expect, it, vi } from "vitest";
import { setCsrfToken } from "./api-client";
import {
  previewImportData,
  confirmImportData,
  readImportReceipt,
} from "./import-data-api";

// Native File supplies arrayBuffer; jsdom's File omits it. The constructors
// agree at runtime, while Node and DOM declarations differ in stream BYOB types.
const File = NodeFile as unknown as typeof globalThis.File;

const collections =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
    " ",
  );
const counts = Object.fromEntries(collections.map((key) => [key, 0]));
const source = {
  format: "organizationweb-export",
  schemaVersion: 1,
  owner: "Ana",
  exportedAt: "2026-09-08T10:20:30.123456Z",
  data: Object.fromEntries(collections.map((key) => [key, []])),
  counts,
};
it("@s17 @s18 accepts exactly 32 MiB and one hundred thousand records", async () => {
  const projects = Array.from({ length: 100_000 }, (_, index) => ({
    id: `00000000-0000-4000-8000-${index.toString(16).padStart(12, "0")}`,
    name: `P${index}`,
    description: "",
    status: "ARCHIVED",
    version: "0",
    createdAt: source.exportedAt,
    updatedAt: source.exportedAt,
  }));
  const total = { ...counts, projects: projects.length };
  const json = JSON.stringify({
    ...source,
    data: { ...source.data, projects },
    counts: total,
  });
  const size = new TextEncoder().encode(json).length;
  expect(size).toBeLessThan(33_554_432);
  const file = new File([json, " ".repeat(33_554_432 - size)], "limit.json");
  const hash = Buffer.from(
    await webcrypto.subtle.digest("SHA-256", await file.arrayBuffer()),
  ).toString("hex");
  const preview = {
    format: "organizationweb-import-preview",
    schemaVersion: 1,
    fileSha256: hash,
    byteLength: file.size,
    owner: "Ana",
    exportedAt: source.exportedAt,
    counts: total,
    insertCounts: total,
    identicalCounts: counts,
    runningSessions: [],
  };
  vi.stubGlobal("crypto", webcrypto);
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(preview)));
  expect(
    await previewImportData(file, "Ana", new AbortController().signal),
  ).toEqual(preview);
});
afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
});

async function previewFixture() {
  const file = new File([JSON.stringify(source)], "copy.json");
  const hash = Buffer.from(
    await webcrypto.subtle.digest("SHA-256", await file.arrayBuffer()),
  ).toString("hex");
  const preview = {
    format: "organizationweb-import-preview",
    schemaVersion: 1,
    fileSha256: hash,
    byteLength: file.size,
    owner: "Ana",
    exportedAt: source.exportedAt,
    counts,
    insertCounts: counts,
    identicalCounts: counts,
    runningSessions: [],
  };
  vi.stubGlobal("crypto", webcrypto);
  return { file, preview };
}

async function receiptFixture() {
  const { file, preview } = await previewFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  const receipt = {
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
    byteLength: file.size,
    recordedAt: "2026-09-08T10:21:30.123456Z",
    outcome: "NO_CHANGE",
    insertedCounts: counts,
    identicalCounts: counts,
  };
  return { file, intent, receipt };
}

it("@s20 @s38 accepts positive imported counts at the inclusive recovered boundary", async () => {
  const { intent, receipt } = await receiptFixture();
  const expected = {
    ...receipt,
    byteLength: 33_554_432,
    outcome: "IMPORTED",
    insertedCounts: { ...counts, tasks: 60_000 },
    identicalCounts: { ...counts, projects: 40_000 },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(expected)));
  expect(await readImportReceipt(intent, new AbortController().signal)).toEqual(
    expected,
  );
});

it("@s38 preserves a missing receipt as 404 rather than a failed or confirmed import", async () => {
  const { intent } = await receiptFixture();
  const response = Response.json({ code: "IMPORT_NOT_FOUND" }, { status: 404 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    readImportReceipt(intent, new AbortController().signal),
  ).rejects.toBe(response);
});

it("@s38 refuses an invalid recovery key before constructing a route", async () => {
  const { intent } = await receiptFixture();
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  await expect(
    readImportReceipt(
      { ...intent, requestKey: "../../other" },
      new AbortController().signal,
    ),
  ).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s38 rejects an impossible length in a recovered receipt", async () => {
  const { intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...receipt, byteLength: -1 })),
  );
  await expect(
    readImportReceipt(intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s38 recovers a confirmed receipt by its key without retaining or sending a File", async () => {
  const { intent, receipt } = await receiptFixture();
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  expect(await readImportReceipt(intent, signal)).toEqual(receipt);
  expect(fetcher).toHaveBeenCalledOnce();
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/me/imports/by-key/${intent.requestKey}`);
  expect(options.method ?? "GET").toBe("GET");
  expect(options.body).toBeUndefined();
  expect(options.signal).toBe(signal);
});

it("@s20 rejects a receipt whose combined counts exceed the file record limit", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...receipt,
        outcome: "IMPORTED",
        insertedCounts: { ...counts, tasks: 60_000 },
        identicalCounts: { ...counts, projects: 40_001 },
      }),
    ),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects IMPORTED without any inserted records", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...receipt, outcome: "IMPORTED" })),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects incomplete receipt counts", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...receipt, insertedCounts: { projects: 0 } }),
      ),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects a receipt with an invalid recorded instant", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...receipt,
        recordedAt: "2026-02-30T10:21:30.123456Z",
      }),
    ),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects a receipt with a different file length", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...receipt, byteLength: file.size + 1 }),
      ),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects a receipt for different bytes", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...receipt, fileSha256: "a".repeat(64) }),
      ),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects a receipt for a different intention", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...receipt,
        requestKey: "00000000-0000-4000-8000-000000000002",
      }),
    ),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s20 rejects an unexpected field in a confirmation receipt", async () => {
  const { file, intent, receipt } = await receiptFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...receipt, privateExtra: true })),
  );
  await expect(
    confirmImportData(file, intent, new AbortController().signal),
  ).rejects.toThrow();
});

it("@s30 preserves a rejected confirmation response for safe classification", async () => {
  const { file, preview } = await previewFixture();
  const response = Response.json({ code: "IMPORT_CONFLICT" }, { status: 409 });
  const read = vi.spyOn(response, "json");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    confirmImportData(
      file,
      {
        owner: "Ana",
        requestKey: "00000000-0000-4000-8000-000000000001",
        fileSha256: preview.fileSha256,
      },
      new AbortController().signal,
    ),
  ).rejects.toBe(response);
  expect(read).not.toHaveBeenCalled();
});

it("@s21 does not send different bytes under the previous file hash", async () => {
  const { file, preview } = await previewFixture();
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  await expect(
    confirmImportData(
      file,
      {
        owner: "Ana",
        requestKey: "00000000-0000-4000-8000-000000000001",
        fileSha256: preview.fileSha256.replace(
          /^./,
          preview.fileSha256[0] === "a" ? "b" : "a",
        ),
      },
      new AbortController().signal,
    ),
  ).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s20 confirms the same File with its one key and hash and preserves receipt time", async () => {
  const { file, preview } = await previewFixture();
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: preview.fileSha256,
  };
  const receipt = {
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
    byteLength: file.size,
    recordedAt: "2026-09-08T10:21:30.123456Z",
    outcome: "NO_CHANGE",
    insertedCounts: counts,
    identicalCounts: counts,
  };
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("confirm-csrf");
  const signal = new AbortController().signal;
  expect(await confirmImportData(file, intent, signal)).toEqual(receipt);
  expect(fetcher).toHaveBeenCalledOnce();
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/import");
  expect(options.body).toBe(file);
  expect(options.method).toBe("POST");
  expect(options.signal).toBe(signal);
  const headers = new Headers(options.headers);
  expect(headers.get("Idempotency-Key")).toBe(intent.requestKey);
  expect(headers.get("X-Import-Content-SHA256")).toBe(intent.fileSha256);
  expect(headers.get("X-CSRF-TOKEN")).toBe("confirm-csrf");
});

it("@s18 preserves historical running time without advancing or rounding it", async () => {
  const { file, preview } = await previewFixture();
  const expected = {
    ...preview,
    runningSessions: [
      {
        sessionId: "00000000-0000-4000-8000-000000000001",
        runningSince: "2020-01-01T00:00:00.123456Z",
      },
    ],
    counts: { ...counts, workSessions: 1 },
    insertCounts: { ...counts, workSessions: 1 },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(expected)));
  expect(
    await previewImportData(file, "Ana", new AbortController().signal),
  ).toEqual(expected);
});

it("@s18 accepts a running warning with preserved legacy null", async () => {
  const { file, preview } = await previewFixture();
  const expected = {
    ...preview,
    runningSessions: [
      { sessionId: "00000000-0000-4000-8000-000000000001", runningSince: null },
    ],
    counts: { ...counts, workSessions: 1 },
    insertCounts: { ...counts, workSessions: 1 },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(expected)));
  expect(
    await previewImportData(file, "Ana", new AbortController().signal),
  ).toEqual(expected);
});

it("@s30 preserves a non-200 response without treating its JSON as a preview", async () => {
  const { file, preview } = await previewFixture();
  const response = Response.json(preview, { status: 503 });
  const read = vi.spyOn(response, "json");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toBe(response);
  expect(read).not.toHaveBeenCalled();
});

it("@s18 rejects a warning for a session that is not being inserted", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        runningSessions: [
          {
            sessionId: "00000000-0000-4000-8000-000000000001",
            runningSince: null,
          },
        ],
        counts: { ...counts, workSessions: 1 },
        identicalCounts: { ...counts, workSessions: 1 },
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects invalid historical time in a running warning", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        runningSessions: [
          {
            sessionId: "00000000-0000-4000-8000-000000000001",
            runningSince: "2026-09-08T00:00:00Z",
          },
        ],
        counts: { ...counts, workSessions: 1 },
        insertCounts: { ...counts, workSessions: 1 },
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects a running session warning with an invalid identity", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        runningSessions: [{ sessionId: "not-an-id", runningSince: null }],
        counts: { ...counts, workSessions: 1 },
        insertCounts: { ...counts, workSessions: 1 },
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects extra private fields in a running session warning", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        runningSessions: [
          {
            sessionId: "00000000-0000-4000-8000-000000000001",
            runningSince: null,
            notes: "private",
          },
        ],
        counts: { ...counts, workSessions: 1 },
        insertCounts: { ...counts, workSessions: 1 },
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects two running sessions in one preview", async () => {
  const { file, preview } = await previewFixture();
  const runningSessions = [
    "00000000-0000-4000-8000-000000000001",
    "00000000-0000-4000-8000-000000000002",
  ].map((sessionId) => ({ sessionId, runningSince: null }));
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        runningSessions,
        counts: { ...counts, workSessions: 2 },
        insertCounts: { ...counts, workSessions: 2 },
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects runningSessions when it is not an array", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...preview, runningSessions: null })),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s17 @s18 rejects a combined count above one hundred thousand", async () => {
  const { file, preview } = await previewFixture();
  const excessive = { ...counts, tasks: 60_000, projects: 40_001 };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        counts: excessive,
        insertCounts: excessive,
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects totals inconsistent with insertion and identical counts", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...preview, counts: { ...counts, tasks: 1 } }),
      ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects fractional counts even when their sum is consistent", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        counts: { ...counts, projects: 0.5 },
        identicalCounts: { ...counts, projects: 0.5 },
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects an unknown collection in insertion counts", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...preview, insertCounts: { ...counts, outbox: 0 } }),
      ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects missing collection counts rather than displaying a partial preview", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...preview, counts: { projects: 0 } }),
      ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects an exportedAt outside the canonical microsecond timestamp", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...preview,
        exportedAt: "2026-02-30T10:20:30.123456Z",
      }),
    ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects a string schemaVersion instead of version one", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...preview, schemaVersion: "1" })),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects a preview for another format", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...preview, format: "organizationweb-export" }),
      ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s40 rejects a digest delivered after cancellation", async () => {
  const { file, preview } = await previewFixture();
  const controller = new AbortController();
  vi.stubGlobal("crypto", {
    subtle: {
      digest: async (algorithm: string, bytes: ArrayBuffer) => {
        const result = await webcrypto.subtle.digest(algorithm, bytes);
        controller.abort();
        return result;
      },
    },
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(preview)));
  await expect(
    previewImportData(file, "Ana", controller.signal),
  ).rejects.toThrow();
});

it("@s40 stops before Web Crypto when reading File bytes is cancelled", async () => {
  const { file, preview } = await previewFixture();
  const bytes = await file.arrayBuffer();
  const controller = new AbortController();
  vi.spyOn(file, "arrayBuffer").mockImplementation(async () => {
    controller.abort();
    return bytes;
  });
  const digest = vi.fn();
  vi.stubGlobal("crypto", { subtle: { digest } });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(preview)));
  await expect(
    previewImportData(file, "Ana", controller.signal),
  ).rejects.toThrow();
  expect(digest).not.toHaveBeenCalled();
});

it("@s40 rejects JSON delivered after cancellation without hashing the File", async () => {
  const { file, preview } = await previewFixture();
  const controller = new AbortController();
  const response = Response.json(preview);
  vi.spyOn(response, "json").mockImplementation(async () => {
    controller.abort();
    return preview;
  });
  const read = vi.spyOn(file, "arrayBuffer");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    previewImportData(file, "Ana", controller.signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
});

it("@s40 does not consume a response after cancellation during fetch", async () => {
  const { file, preview } = await previewFixture();
  const controller = new AbortController();
  const response = Response.json(preview);
  const read = vi.spyOn(response, "json");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(async () => {
      controller.abort();
      return response;
    }),
  );
  await expect(
    previewImportData(file, "Ana", controller.signal),
  ).rejects.toThrow();
  expect(read).not.toHaveBeenCalled();
});

it("@s40 does not send an already aborted preparation", async () => {
  const { file } = await previewFixture();
  const controller = new AbortController();
  controller.abort();
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  await expect(
    previewImportData(file, "Ana", controller.signal),
  ).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s18 rejects preview length different from the File", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...preview, byteLength: file.size - 1 }),
      ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s17 refuses an oversized File before sending or reading it", async () => {
  const file = new File([new Uint8Array(33_554_433)], "large.json");
  const read = vi.spyOn(file, "arrayBuffer");
  const fetcher = vi.fn();
  vi.stubGlobal("fetch", fetcher);
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
  expect(read).not.toHaveBeenCalled();
});

it("@s18 rejects a preview hash for different file bytes", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...preview, fileSha256: "a".repeat(64) }),
      ),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects another owner's preview", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...preview, owner: "Bruno" })),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 rejects a preview with an unknown envelope field", async () => {
  const { file, preview } = await previewFixture();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...preview, privateExtra: true })),
  );
  await expect(
    previewImportData(file, "Ana", new AbortController().signal),
  ).rejects.toThrow();
});

it("@s18 @s34 previews original File bytes with existing CSRF and fourteen empty counts", async () => {
  const file = new File([JSON.stringify(source, null, 2)], "mi copia.json", {
    type: "application/json",
  });
  const hash = Buffer.from(
    await webcrypto.subtle.digest("SHA-256", await file.arrayBuffer()),
  ).toString("hex");
  const preview = {
    format: "organizationweb-import-preview",
    schemaVersion: 1,
    fileSha256: hash,
    byteLength: file.size,
    owner: "Ana",
    exportedAt: source.exportedAt,
    counts,
    insertCounts: counts,
    identicalCounts: counts,
    runningSessions: [],
  };
  const fetcher = vi.fn().mockResolvedValue(Response.json(preview));
  vi.stubGlobal("fetch", fetcher);
  vi.stubGlobal("crypto", webcrypto);
  setCsrfToken("token-real-api-client");
  const signal = new AbortController().signal;
  expect(await previewImportData(file, "Ana", signal)).toEqual(preview);
  expect(fetcher).toHaveBeenCalledOnce();
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/import/preview");
  expect(options.method).toBe("POST");
  expect(options.body).toBe(file);
  expect(options.signal).toBe(signal);
  expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe(
    "token-real-api-client",
  );
  expect(new Headers(options.headers).get("Content-Type")).toBe(
    "application/json;charset=utf-8",
  );
});
