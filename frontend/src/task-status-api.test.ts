import { expect, it, vi } from "vitest";
import {
  readTaskStatus,
  changeTaskStatus,
  readTaskHistory,
} from "./task-status-api";
const projectId = "6c5dbd10-9ad5-4000-8000-000000000001";
const taskId = "7c5dbd10-9ad5-4000-8000-000000000002";
const state = {
  status: "pending",
  completedAt: null,
  updatedAt: "2026-09-06T12:00:00.123456Z",
};
const etag = `"task:${taskId}:0"`;
it("@s1 reads the confirmed three-field state and opaque ETag with session and cancellation", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(state, { headers: { ETag: etag } }));
  await expect(readTaskStatus(projectId, taskId, signal)).resolves.toEqual({
    ...state,
    etag,
  });
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/status`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([201, 400, 401, 403, 404, 500, 503])(
  "@s14 @s18 rejects status HTTP %s even with a valid body",
  async (status) => {
    const response = Response.json(state, { status, headers: { ETag: etag } });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readTaskStatus(projectId, taskId)).rejects.toBe(response);
  },
);
it.each([
  null,
  "abc",
  [],
  3,
  true,
  {},
  { ...state, extra: 1 },
  { status: "pending", completedAt: null },
  { ...state, status: "done" },
  { ...state, status: null },
  { ...state, status: "completed", completedAt: null },
  { ...state, completedAt: state.updatedAt },
  { ...state, updatedAt: null },
  { ...state, updatedAt: "not-a-date" },
  { ...state, updatedAt: "2026-02-30T12:00:00Z" },
  { ...state, updatedAt: "2026-09-06T12:00:00+02:00" },
  { ...state, updatedAt: "2026-09-06T12:00:00.1234567Z" },
  { ...state, status: "completed", completedAt: "2026-09-06T12:00:01Z" },
])(
  "@s34 rejects incompatible three-field status body %# with a controlled error",
  async (body) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, { headers: { ETag: etag } }),
    );
    await expect(readTaskStatus(projectId, taskId)).rejects.toThrow(
      "Respuesta de estado de tarea inválida",
    );
  },
);
it.each([
  null,
  "",
  "*",
  `W/${etag}`,
  `${etag}, ${etag}`,
  `"${taskId}:0"`,
  `"task:${projectId}:0"`,
  `"task:${taskId.toUpperCase()}:0"`,
  `"task:${taskId}:00"`,
  `"task:${taskId}:+1"`,
  `"task:${taskId}:-1"`,
  `"task:${taskId}:1.5"`,
  `"task:${taskId}:9223372036854775808"`,
  `"task:1-1-1-1-1:0"`,
])("@s34 rejects unusable status ETag %#", async (tag) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(state, { headers: tag === null ? {} : { ETag: tag } }),
  );
  await expect(readTaskStatus(projectId, taskId)).rejects.toThrow(
    "Respuesta de estado de tarea inválida",
  );
});
it.each([
  "2026-09-06T12:00:00Z",
  "2026-09-06T12:00:00.1Z",
  "2026-09-06T12:00:00.123456Z",
])(
  "@s2 preserves completed dates %s and BIGINT ETag on a case-insensitive route",
  async (time) => {
    const completed = {
      status: "completed",
      completedAt: time,
      updatedAt: time,
    };
    const tag = `"task:${taskId}:9223372036854775807"`;
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(completed, { headers: { ETag: tag } }),
    );
    await expect(
      readTaskStatus(projectId.toUpperCase(), taskId.toUpperCase()),
    ).resolves.toEqual({ ...completed, etag: tag });
  },
);
import { setCsrfToken } from "./api-client";
it.each(["pending", "completed"] as const)(
  "@s2 @s3 PUT %s sends only intended status, precondition and actual CSRF",
  async (status) => {
    const confirmed = {
      ...state,
      status,
      completedAt: status === "completed" ? state.updatedAt : null,
    };
    const nextTag = `"task:${taskId}:1"`;
    const signal = new AbortController().signal;
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(confirmed, { headers: { ETag: nextTag } }),
      );
    setCsrfToken("synthetic-csrf");
    try {
      await expect(
        changeTaskStatus(projectId, taskId, status, etag, signal),
      ).resolves.toEqual({ ...confirmed, etag: nextTag });
      expect(fetcher).toHaveBeenCalledTimes(1);
      const [url, options] = fetcher.mock.calls[0];
      expect(url).toBe(`/api/v1/projects/${projectId}/tasks/${taskId}/status`);
      expect(options).toMatchObject({
        method: "PUT",
        credentials: "same-origin",
        signal,
        body: JSON.stringify({ status }),
      });
      const headers = new Headers(options?.headers);
      expect(headers.get("Content-Type")).toBe("application/json");
      expect(headers.get("If-Match")).toBe(etag);
      expect(headers.get("X-CSRF-TOKEN")).toBe("synthetic-csrf");
    } finally {
      setCsrfToken(undefined);
    }
  },
);
it("@s23 rejects a valid-looking PUT body that does not confirm the requested transition", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(state, { headers: { ETag: etag } }),
  );
  await expect(
    changeTaskStatus(projectId, taskId, "completed", etag),
  ).rejects.toThrow("Respuesta de estado de tarea inválida");
});
it("@s10 reads empty confirmed history without inventing entries", async () => {
  const signal = new AbortController().signal;
  const page = { items: [], nextCursor: null };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(page));
  await expect(
    readTaskHistory(projectId, taskId, undefined, signal),
  ).resolves.toEqual(page);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/history`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
const entry = {
  id: taskId,
  fromStatus: "pending",
  toStatus: "completed",
  occurredAt: state.updatedAt,
};
it("@s12 @s29 forwards opaque cursor unchanged and preserves twenty entries in server order", async () => {
  const cursor = "opaque+cursor/=9223372036854775807";
  const page = {
    items: Array.from({ length: 20 }, (_, index) => ({
      ...entry,
      id: `7c5dbd10-9ad5-4000-8000-${String(index).padStart(12, "0")}`,
      fromStatus: index % 2 ? "completed" : "pending",
      toStatus: index % 2 ? "pending" : "completed",
    })),
    nextCursor: "next-opaque",
  };
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(page));
  await expect(readTaskHistory(projectId, taskId, cursor)).resolves.toEqual(
    page,
  );
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/projects/${projectId}/tasks/${taskId}/history?cursor=${encodeURIComponent(cursor)}`,
  );
});
it.each([201, 400, 401, 403, 404, 500, 503])(
  "@s14 @s18 history HTTP %s never becomes empty success",
  async (status) => {
    const response = Response.json({ items: [], nextCursor: null }, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readTaskHistory(projectId, taskId)).rejects.toBe(response);
  },
);
it.each([
  null,
  "ab",
  [],
  2,
  {},
  { items: [] },
  { items: [], nextCursor: null, extra: true },
  { items: null, nextCursor: null },
  { items: {}, nextCursor: null },
  { items: Array(21).fill(entry), nextCursor: "next" },
  ...["", 1, false, {}, []].map((nextCursor) => ({ items: [], nextCursor })),
  ...[
    null,
    "abcd",
    [],
    {},
    { ...entry, extra: 1 },
    { ...entry, id: "1-1-1-1-1" },
    { ...entry, id: null },
    { ...entry, fromStatus: "done" },
    { ...entry, toStatus: null },
    { ...entry, toStatus: "pending" },
    { ...entry, occurredAt: "2026-02-30T00:00:00Z" },
  ].map((item) => ({ items: [item], nextCursor: null })),
])(
  "@s11 @s29 incompatible history body %# fails with a controlled error",
  async (body) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
    await expect(readTaskHistory(projectId, taskId)).rejects.toThrow(
      "Respuesta de historial de tarea inválida",
    );
  },
);

it.each([201, 400, 401, 403, 404, 412, 415, 428, 503])(
  "@s6 @s14 @s15 @s23 PUT HTTP %s remains a single rejected attempt",
  async (status) => {
    const response = Response.json(state, { status, headers: { ETag: etag } });
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(response);
    await expect(
      changeTaskStatus(projectId, taskId, "pending", etag),
    ).rejects.toBe(response);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each([
  { body: state, tag: null },
  { body: state, tag: `"task:${projectId}:0"` },
  { body: { ...state, extra: 1 }, tag: etag },
])(
  "@s23 PUT invalid confirmation %# never reports success",
  async ({ body, tag }) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(body, { headers: tag ? { ETag: tag } : {} }),
    );
    await expect(
      changeTaskStatus(projectId, taskId, "pending", etag),
    ).rejects.toThrow("Respuesta de estado de tarea inválida");
  },
);
it("@s4 preserves a no-op confirmation without demanding a different ETag", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(state, { headers: { ETag: etag } }),
  );
  await expect(
    changeTaskStatus(projectId, taskId, "pending", etag),
  ).resolves.toEqual({ ...state, etag });
});
it.each(["status", "change", "history"])(
  "@s23 %s network failure is not retried",
  async (operation) => {
    const failure = new Error("synthetic offline");
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockRejectedValueOnce(failure);
    const call =
      operation === "status"
        ? readTaskStatus(projectId, taskId)
        : operation === "change"
          ? changeTaskStatus(projectId, taskId, "pending", etag)
          : readTaskHistory(projectId, taskId);
    await expect(call).rejects.toBe(failure);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each(["status", "change", "history"])(
  "@s23 %s malformed response JSON is not accepted or retried",
  async (operation) => {
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response("{", { headers: { ETag: etag } }));
    const call =
      operation === "status"
        ? readTaskStatus(projectId, taskId)
        : operation === "change"
          ? changeTaskStatus(projectId, taskId, "pending", etag)
          : readTaskHistory(projectId, taskId);
    await expect(call).rejects.toBeInstanceOf(SyntaxError);
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each([[state.updatedAt], { toString: null }])(
  "estado rechaza fecha JSON no textual %# con error controlado",
  async (updatedAt) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...state, updatedAt }, { headers: { ETag: etag } }),
    );
    await expect(readTaskStatus(projectId, taskId)).rejects.toThrow(
      "Respuesta de estado de tarea inválida",
    );
  },
);
it.each(["+010000-09-06T12:00:00Z", "-000001-09-06T12:00:00Z"])(
  "conserva instante UTC de año expandido %s",
  async (updatedAt) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...state, updatedAt }, { headers: { ETag: etag } }),
    );
    await expect(readTaskStatus(projectId, taskId)).resolves.toEqual({
      ...state,
      updatedAt,
      etag,
    });
  },
);
it.each([["cursor"], { length: 1 }])(
  "historial rechaza cursor JSON no textual %#",
  async (nextCursor) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ items: [], nextCursor }),
    );
    await expect(readTaskHistory(projectId, taskId)).rejects.toThrow(
      "Respuesta de historial de tarea inválida",
    );
  },
);
it.each([[entry.id], { toString: null }, `x${entry.id}`, `${entry.id}x`])(
  "historial rechaza identificador no UUID textual completo %#",
  async (id) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ items: [{ ...entry, id }], nextCursor: null }),
    );
    await expect(readTaskHistory(projectId, taskId)).rejects.toThrow(
      "Respuesta de historial de tarea inválida",
    );
  },
);
