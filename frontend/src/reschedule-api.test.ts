import { expect, it, vi } from "vitest";
import {
  cancelBlock,
  previewReschedule,
  readBlockChange,
  readBlockChangeByRequest,
  readBlockChanges,
  readBlockState,
  readRescheduleError,
  rescheduleBlock,
} from "./reschedule-api";
const projectId = "11111111-1111-1111-1111-111111111111";
const taskId = "22222222-2222-2222-2222-222222222222";
const blockId = "44444444-4444-4444-4444-444444444444";
const block = {
  id: blockId,
  projectId,
  taskId,
  objective: "Preparar borrador",
  startAt: "2030-01-07T10:00:00Z",
  endAt: "2030-01-07T11:00:00Z",
  zoneId: "UTC",
  durationMinutes: 60,
  createdAt: "2030-01-06T10:00:00.123456Z",
};
const revision = `"block:${blockId}:9007199254740993"`;
const state = {
  block,
  status: "planned",
  updatedAt: "2030-01-07T08:59:59.123456Z",
};
it("@s4 keeps an unsafe block revision as ETag text while reading state", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(state, { headers: { ETag: revision } }),
    );
  await expect(
    readBlockState(projectId, taskId, blockId, signal),
  ).resolves.toEqual({ ...state, revision });
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/${blockId}/state`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([201, 401, 404, 503])(
  "@s18 preserves the HTTP rejection of a state read %s",
  async (status) => {
    const response = Response.json(state, {
      status,
      headers: { ETag: revision },
    });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readBlockState(projectId, taskId, blockId)).rejects.toBe(
      response,
    );
  },
);
const invalid = "Estado de bloque inválido";
it.each([
  null,
  [],
  { ...state, extra: 1 },
  { block, status: "planned" },
  { block, updatedAt: state.updatedAt },
  { status: "planned", updatedAt: state.updatedAt },
  { ...state, status: "PLANNED" },
  { ...state, status: "planificado" },
  { ...state, status: null },
  { ...state, block: { ...block, extra: true } },
  { ...state, block: { ...block, taskId: projectId } },
  { ...state, block: { ...block, durationMinutes: 59 } },
  { ...state, block: { ...block, id: taskId } },
])("@s31 rejects an incompatible state body %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { headers: { ETag: revision } }),
  );
  await expect(readBlockState(projectId, taskId, blockId)).rejects.toThrow(
    invalid,
  );
});
it.each([
  "2030-01-07T08:59:59.1234567Z",
  "2030-01-07T08:59:59.123456+00:00",
  "2030-01-07T08:59:59.123456",
  "2030-02-30T08:59:59.123456Z",
  "0000-01-07T08:59:59.123456Z",
  1893574799123,
])(
  "@s31 rejects an updatedAt outside UTC microseconds %#",
  async (updatedAt) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...state, updatedAt }, { headers: { ETag: revision } }),
    );
    await expect(readBlockState(projectId, taskId, blockId)).rejects.toThrow(
      invalid,
    );
  },
);
it.each([
  "",
  `W/"block:${blockId}:1"`,
  `block:${blockId}:1`,
  `"block:22222222-2222-2222-2222-222222222222:1"`,
  `"block:${blockId}:0"`,
  `"block:${blockId}:01"`,
  `"block:${blockId}:-1"`,
  `"block:${blockId}:1.0"`,
  `"block:${blockId}:9223372036854775808"`,
  `"availability:${blockId}:1"`,
  `"block:${blockId}"`,
  `prefix"block:${blockId}:1"`,
  `"block:${blockId}:1"suffix`,
  `"block:${blockId}:1", "block:${blockId}:2"`,
])("@s31 rejects an unusable block revision header %#", async (etag) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(state, { headers: { ETag: etag } }),
  );
  await expect(readBlockState(projectId, taskId, blockId)).rejects.toThrow(
    invalid,
  );
});
it("@s4 accepts the largest canonical block revision", async () => {
  const largest = `"block:${blockId}:9223372036854775807"`;
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(state, { headers: { ETag: largest } }),
  );
  await expect(
    readBlockState(projectId, taskId, blockId),
  ).resolves.toHaveProperty("revision", largest);
});
const current = { ...state, status: "planned" as const, revision };
const input = {
  startLocal: "2030-01-07T12:00",
  endLocal: "2030-01-07T13:00",
  zoneId: "UTC",
  startOffset: null,
  endOffset: null,
};
const preview = {
  objective: "Preparar borrador",
  zoneId: "UTC",
  startAt: "2030-01-07T12:00:00Z",
  endAt: "2030-01-07T13:00:00Z",
  startOffset: "Z",
  endOffset: "Z",
  durationMinutes: 60,
  availabilityEtag: '"availability:33333333-3333-3333-3333-333333333333:0"',
  budgetZoneId: "UTC",
  days: [
    {
      date: "2030-01-07",
      budgetMinutes: 120,
      plannedSeconds: 0,
      requestedSeconds: 3600,
      excessSeconds: 0,
    },
  ],
};
it("@s7 reviews a destination with the retained block revision", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(preview, { headers: { ETag: revision } }));
  await expect(
    previewReschedule(projectId, taskId, current, input, signal),
  ).resolves.toEqual(preview);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/${blockId}/reschedule/preview`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": `"block:${blockId}:9007199254740993"`,
      },
      body: '{"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":null,"endOffset":null}',
    },
  );
});
it.each([201, 400, 401, 403, 404, 409, 412, 428, 503])(
  "@s6 preserves the HTTP rejection of a reschedule preview %s",
  async (status) => {
    const response = Response.json(preview, {
      status,
      headers: { ETag: revision },
    });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(
      previewReschedule(projectId, taskId, current, input),
    ).rejects.toBe(response);
  },
);
const unusable = "Revisión de movimiento inválida";
it.each([
  null,
  [],
  { ...preview, extra: 1 },
  ...Object.keys(preview).map((key) =>
    Object.fromEntries(
      Object.entries(preview).filter(([name]) => name !== key),
    ),
  ),
  { ...preview, objective: "Otro objetivo" },
  { ...preview, zoneId: "Europe/Madrid" },
  { ...preview, startAt: "2030-01-07T11:00:00Z" },
  { ...preview, endAt: "2030-01-07T14:00:00Z", durationMinutes: 120 },
  { ...preview, startOffset: "+01:00" },
  { ...preview, durationMinutes: 59 },
  { ...preview, availabilityEtag: '"availability:unconfigured"' },
  { ...preview, days: [{ ...preview.days[0], requestedSeconds: 3540 }] },
])("@s31 rejects a preview incompatible with the block or proposal %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { headers: { ETag: revision } }),
  );
  await expect(
    previewReschedule(projectId, taskId, current, input),
  ).rejects.toThrow(unusable);
});
it.each([
  "",
  `"block:${blockId}:9007199254740994"`,
  `"block:22222222-2222-2222-2222-222222222222:9007199254740993"`,
  `W/"block:${blockId}:9007199254740993"`,
])("@s31 rejects a preview answered for another block revision %#", async (etag) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(preview, { headers: { ETag: etag } }),
  );
  await expect(
    previewReschedule(projectId, taskId, current, input),
  ).rejects.toThrow(unusable);
});
const changeId = "66666666-6666-6666-6666-666666666666";
const location = `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/changes/${changeId}`;
const moved = {
  ...block,
  startAt: "2030-01-07T12:00:00Z",
  endAt: "2030-01-07T13:00:00Z",
};
const receipt = {
  id: changeId,
  blockId,
  kind: "RESCHEDULED",
  revision: `"block:${blockId}:9007199254740994"`,
  occurredAt: "2030-01-07T09:00:00.123456Z",
  before: block,
  after: moved,
};
const move = {
  blockId,
  input: {
    startLocal: "2030-01-07T12:00",
    endLocal: "2030-01-07T13:00",
    zoneId: "UTC",
    startOffset: "Z",
    endOffset: "Z",
    allowOverBudget: false,
  },
  key: "55555555-5555-5555-5555-555555555555",
  revision,
  availabilityRevision:
    '"availability:33333333-3333-3333-3333-333333333333:0"',
};
it.each([200, 201])(
  "@s11 @s15 sends the retained move and accepts its receipt on HTTP %s",
  async (status) => {
    const signal = new AbortController().signal;
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(receipt, { status, headers: { Location: location } }),
      );
    await expect(
      rescheduleBlock(projectId, taskId, move, signal),
    ).resolves.toEqual(receipt);
    expect(fetcher).toHaveBeenCalledExactlyOnceWith(
      `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/${blockId}/reschedule`,
      {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        signal,
        headers: {
          "Content-Type": "application/json",
          "If-Match": `"block:${blockId}:9007199254740993"`,
          "Availability-Revision":
            '"availability:33333333-3333-3333-3333-333333333333:0"',
          "Idempotency-Key": "55555555-5555-5555-5555-555555555555",
        },
        body: '{"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}',
      },
    );
  },
);
it.each([202, 400, 401, 403, 404, 409, 412, 428, 503])(
  "@s33 keeps a move uncertain on HTTP %s",
  async (status) => {
    const response = Response.json(receipt, {
      status,
      headers: { Location: location },
    });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(rescheduleBlock(projectId, taskId, move)).rejects.toBe(
      response,
    );
  },
);
const uncertain = "Confirmación de cambio inválida";
it.each([
  null,
  [],
  { ...receipt, extra: 1 },
  ...Object.keys(receipt).map((key) =>
    Object.fromEntries(
      Object.entries(receipt).filter(([name]) => name !== key),
    ),
  ),
  { ...receipt, id: "66666666666666666666666666666666" },
  { ...receipt, id: `${changeId}suffix` },
  { ...receipt, kind: "MOVED" },
  { ...receipt, kind: "rescheduled" },
  { ...receipt, revision: `"block:${blockId}:0"` },
  { ...receipt, revision: `"block:${blockId}:9223372036854775808"` },
  {
    ...receipt,
    revision: `"block:22222222-2222-2222-2222-222222222222:9007199254740994"`,
  },
  { ...receipt, revision: `W/"block:${blockId}:9007199254740994"` },
  { ...receipt, occurredAt: "2030-01-07T09:00:00.1234567Z" },
  { ...receipt, occurredAt: "2030-01-07T09:00:00.123456+00:00" },
  { ...receipt, before: { ...block, extra: true } },
  { ...receipt, before: { ...block, taskId: projectId } },
  { ...receipt, before: { ...block, durationMinutes: 61 } },
  { ...receipt, after: { ...moved, extra: true } },
  { ...receipt, after: { ...moved, projectId: taskId } },
  { ...receipt, after: { ...moved, durationMinutes: 61 } },
])("@s32 refuses a receipt that is not a closed change %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { status: 201, headers: { Location: location } }),
  );
  await expect(rescheduleBlock(projectId, taskId, move)).rejects.toThrow(
    uncertain,
  );
});
const otherId = "77777777-7777-7777-7777-777777777777";
it.each([
  {
    ...receipt,
    blockId: otherId,
    revision: `"block:${otherId}:9007199254740994"`,
    before: { ...block, id: otherId },
    after: { ...moved, id: otherId },
  },
  { ...receipt, kind: "CANCELLED", after: null },
  { ...receipt, after: null },
  { ...receipt, before: { ...block, id: otherId } },
  { ...receipt, after: { ...moved, id: otherId } },
  { ...receipt, after: { ...moved, objective: "Otro objetivo" } },
  { ...receipt, after: { ...moved, createdAt: "2030-01-05T10:00:00.123456Z" } },
  { ...receipt, after: { ...moved, zoneId: "Europe/Madrid" } },
  {
    ...receipt,
    after: {
      ...moved,
      startAt: "2030-01-07T14:00:00Z",
      endAt: "2030-01-07T15:00:00Z",
    },
  },
  {
    ...receipt,
    after: { ...moved, endAt: "2030-01-07T13:30:00Z", durationMinutes: 90 },
  },
])("@s32 refuses a receipt that does not confirm the retained move %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { status: 201, headers: { Location: location } }),
  );
  await expect(rescheduleBlock(projectId, taskId, move)).rejects.toThrow(
    uncertain,
  );
});
it.each(
  [200, 201].flatMap((status) =>
    [
      "",
      `${location}/`,
      `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/changes/${otherId}`,
      `/api/v1/projects/${taskId}/tasks/${taskId}/blocks/changes/${changeId}`,
      `https://example.test${location}`,
    ].map((header) => ({ status, header })),
  ),
)("@s32 refuses a confirmed move located elsewhere %#", async ({ status, header }) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(receipt, { status, headers: { Location: header } }),
  );
  await expect(rescheduleBlock(projectId, taskId, move)).rejects.toThrow(
    uncertain,
  );
});
const cancelId = "99999999-9999-9999-9999-999999999999";
const cancelLocation = `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/changes/${cancelId}`;
const cancelReceipt = {
  id: cancelId,
  blockId,
  kind: "CANCELLED",
  revision: `"block:${blockId}:9007199254740995"`,
  occurredAt: "2030-01-07T09:04:00.123456Z",
  before: moved,
  after: null,
};
const cancellation = {
  blockId,
  key: "88888888-8888-8888-8888-888888888888",
  revision: `"block:${blockId}:9007199254740994"`,
};
it.each([200, 201])(
  "@s12 @s13 cancels with an empty body and accepts an earlier occurredAt on HTTP %s",
  async (status) => {
    const signal = new AbortController().signal;
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(
        Response.json(cancelReceipt, {
          status,
          headers: { Location: cancelLocation },
        }),
      );
    await expect(
      cancelBlock(projectId, taskId, cancellation, signal),
    ).resolves.toEqual(cancelReceipt);
    expect(fetcher).toHaveBeenCalledExactlyOnceWith(
      `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/${blockId}/cancel`,
      {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        signal,
        headers: {
          "Content-Type": "application/json",
          "If-Match": `"block:${blockId}:9007199254740994"`,
          "Idempotency-Key": "88888888-8888-8888-8888-888888888888",
        },
        body: "{}",
      },
    );
  },
);
it.each([202, 400, 401, 403, 404, 409, 412, 428, 503])(
  "@s33 keeps a cancellation uncertain on HTTP %s",
  async (status) => {
    const response = Response.json(cancelReceipt, {
      status,
      headers: { Location: cancelLocation },
    });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(cancelBlock(projectId, taskId, cancellation)).rejects.toBe(
      response,
    );
  },
);
it.each([
  { body: { ...cancelReceipt, extra: 1 }, header: cancelLocation },
  { body: { ...cancelReceipt, after: moved }, header: cancelLocation },
  {
    body: { ...cancelReceipt, kind: "RESCHEDULED", after: moved },
    header: cancelLocation,
  },
  {
    body: { ...cancelReceipt, before: { ...moved, taskId: projectId } },
    header: cancelLocation,
  },
  {
    body: { ...cancelReceipt, before: { ...moved, id: otherId } },
    header: cancelLocation,
  },
  {
    body: {
      ...cancelReceipt,
      blockId: otherId,
      revision: `"block:${otherId}:9007199254740995"`,
      before: { ...moved, id: otherId },
    },
    header: cancelLocation,
  },
  { body: cancelReceipt, header: "" },
  { body: cancelReceipt, header: location },
])("@s32 refuses a receipt that does not confirm the cancellation %#", async ({ body, header }) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { status: 201, headers: { Location: header } }),
  );
  await expect(cancelBlock(projectId, taskId, cancellation)).rejects.toThrow(
    uncertain,
  );
});
it.each([undefined, "cursor+/="])(
  "@s16 @s39 reads a page of changes with its opaque cursor %s",
  async (cursor) => {
    const signal = new AbortController().signal;
    const page = { items: [cancelReceipt, receipt], nextCursor: "next" };
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(page));
    await expect(
      readBlockChanges(projectId, taskId, cursor, signal),
    ).resolves.toEqual(page);
    expect(fetcher).toHaveBeenCalledExactlyOnceWith(
      `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/changes` +
        (cursor ? `?cursor=${encodeURIComponent(cursor)}` : ""),
      { credentials: "same-origin", cache: "no-store", signal },
    );
  },
);
it.each([201, 401, 404, 503])(
  "@s18 @s39 does not invent an empty history on HTTP %s",
  async (status) => {
    const response = Response.json({ items: [], nextCursor: null }, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readBlockChanges(projectId, taskId)).rejects.toBe(response);
  },
);
const unreadable = "Historial de cambios inválido";
it.each([
  null,
  [],
  {},
  { items: [] },
  { nextCursor: null },
  { items: [], nextCursor: 1 },
  { items: [], nextCursor: "" },
  { items: [], nextCursor: null, extra: true },
  { items: {}, nextCursor: null },
  { items: Array.from({ length: 21 }, () => receipt), nextCursor: null },
  { items: [{ ...receipt, extra: 1 }], nextCursor: null },
  { items: [{ ...receipt, kind: "CREATED" }], nextCursor: null },
  {
    items: [{ ...receipt, before: { ...block, taskId: projectId } }],
    nextCursor: null,
  },
])("@s16 @s39 rejects a malformed change history %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(readBlockChanges(projectId, taskId)).rejects.toThrow(unreadable);
});
it.each([
  { items: [], nextCursor: null },
  {
    items: Array.from({ length: 20 }, (_, index) => ({
      ...receipt,
      id: `66666666-6666-6666-6666-${String(index).padStart(12, "0")}`,
    })),
    nextCursor: null,
  },
])("@s16 accepts an empty and a full terminal page %#", async (page) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(page));
  await expect(readBlockChanges(projectId, taskId)).resolves.toEqual(page);
});
it("@s32 @s36 reads a historical receipt without requiring Location", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(cancelReceipt));
  await expect(
    readBlockChange(projectId, taskId, cancelId, signal),
  ).resolves.toEqual(cancelReceipt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/changes/${cancelId}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([201, 401, 404, 503])(
  "@s18 preserves the HTTP rejection of a receipt read %s",
  async (status) => {
    const response = Response.json(cancelReceipt, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readBlockChange(projectId, taskId, cancelId)).rejects.toBe(
      response,
    );
  },
);
it.each([
  { ...cancelReceipt, extra: 1 },
  { ...cancelReceipt, after: moved },
  { ...cancelReceipt, id: otherId },
  { ...cancelReceipt, before: { ...moved, taskId: projectId } },
])("@s32 @s36 rejects a historical receipt that is not closed %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(readBlockChange(projectId, taskId, cancelId)).rejects.toThrow(
    uncertain,
  );
});
it.each([
  { request: move, body: receipt, key: "55555555-5555-5555-5555-555555555555" },
  {
    request: cancellation,
    body: cancelReceipt,
    key: "88888888-8888-8888-8888-888888888888",
  },
])("@s33 @s36 checks a transmitted change by its own key %#", async ({ request, body, key }) => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(body));
  await expect(
    readBlockChangeByRequest(projectId, taskId, request, signal),
  ).resolves.toEqual(body);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/changes/by-request/${key}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([201, 401, 404, 409, 503])(
  "@s34 exposes a missing receipt as its own response %s",
  async (status) => {
    const response = Response.json(
      { code: "BLOCK_CHANGE_NOT_FOUND" },
      { status },
    );
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(
      readBlockChangeByRequest(projectId, taskId, move),
    ).rejects.toBe(response);
  },
);
it.each([
  { request: move, body: cancelReceipt },
  { request: move, body: { ...receipt, extra: 1 } },
  { request: move, body: { ...receipt, after: { ...moved, zoneId: "UTC+1" } } },
  { request: cancellation, body: receipt },
  { request: cancellation, body: { ...cancelReceipt, blockId: otherId } },
])("@s32 @s34 refuses a recovered receipt of another intention %#", async ({ request, body }) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(
    readBlockChangeByRequest(projectId, taskId, request),
  ).rejects.toThrow(uncertain);
});
it.each(["preview", "reschedule", "cancel", "recover"] as const)(
  "@s29 @s32 @s37 retains the transmitted intention while %s waits",
  async (operation) => {
    let resolve!: (response: Response) => void;
    vi.spyOn(globalThis, "fetch").mockImplementationOnce(
      () =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    );
    const state = structuredClone(current);
    const edited = structuredClone(input);
    const retainedMove = structuredClone(move);
    const retainedCancel = structuredClone(cancellation);
    const result =
      operation === "preview"
        ? previewReschedule(projectId, taskId, state, edited)
        : operation === "reschedule"
          ? rescheduleBlock(projectId, taskId, retainedMove)
          : operation === "cancel"
            ? cancelBlock(projectId, taskId, retainedCancel)
            : readBlockChangeByRequest(projectId, taskId, retainedMove);
    state.revision = `"block:${blockId}:1"`;
    state.block.objective = "Editado";
    edited.startLocal = "2030-01-07T15:00";
    retainedMove.input.startLocal = "2030-01-07T15:00";
    retainedMove.blockId = otherId;
    retainedCancel.blockId = otherId;
    resolve(
      operation === "preview"
        ? Response.json(preview, { headers: { ETag: revision } })
        : Response.json(operation === "cancel" ? cancelReceipt : receipt, {
            status: operation === "recover" ? 200 : 201,
            headers: {
              Location:
                operation === "cancel" ? cancelLocation : location,
            },
          }),
    );
    await expect(result).resolves.toEqual(
      operation === "preview"
        ? preview
        : operation === "cancel"
          ? cancelReceipt
          : receipt,
    );
  },
);
const problem = (code: string, status: number, extra = {}) => ({
  type: `urn:organization:problem:${code.toLowerCase()}`,
  title: "Revisa la petición.",
  status,
  code,
  ...extra,
});
it.each([
  ["BLOCK_CONFLICT", 412],
  ["BLOCK_CANCELLED", 409],
  ["BLOCK_UNCHANGED", 409],
  ["BLOCK_VERSION_EXHAUSTED", 409],
  ["BLOCK_CHANGE_NOT_FOUND", 404],
])("@s6 @s35 reads the definitive rejection %s", async (code, status) => {
  const body = problem(code, status);
  await expect(
    readRescheduleError(Response.json(body, { status })),
  ).resolves.toEqual(body);
});
it.each([
  ["IDEMPOTENCY_CONFLICT", 409],
  ["PRECONDITION_REQUIRED", 428],
  ["STORAGE_UNAVAILABLE", 503],
])("@s33 keeps reading the inherited rejection %s", async (code, status) => {
  const body = problem(code, status);
  await expect(
    readRescheduleError(Response.json(body, { status })),
  ).resolves.toEqual(body);
});
it.each([
  Response.json(problem("BLOCK_CONFLICT", 409), { status: 409 }),
  Response.json(problem("BLOCK_CONFLICT", 412, { extra: 1 }), { status: 412 }),
  Response.json(
    { ...problem("BLOCK_CONFLICT", 412), type: "urn:organization:problem:x" },
    { status: 412 },
  ),
  Response.json(problem("BLOCK_FROZEN", 409), { status: 409 }),
  Response.json(null, { status: 412 }),
  new Response("no json", { status: 412 }),
])("@s33 treats an unknown rejection as uncertain %#", async (response) => {
  await expect(readRescheduleError(response)).resolves.toBeNull();
});
it.each([
  {
    body: { block, status: "planned", updatedAt: block.createdAt },
    etag: `"block:${blockId}:1"`,
  },
  {
    body: { block: moved, status: "cancelled", updatedAt: "2030-01-07T09:05:00Z" },
    etag: `"block:${blockId}:3"`,
  },
])("@s1 @s3 @s13 reads a first and a cancelled state %#", async ({ body, etag }) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { headers: { ETag: etag } }),
  );
  await expect(readBlockState(projectId, taskId, blockId)).resolves.toEqual({
    ...body,
    revision: etag,
  });
});
it("@s13 accepts a receipt that occurred before the last known state", async () => {
  vi.spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(
        { block: moved, status: "planned", updatedAt: "2030-01-07T09:05:00Z" },
        { headers: { ETag: `"block:${blockId}:9007199254740994"` } },
      ),
    )
    .mockResolvedValueOnce(
      Response.json(cancelReceipt, {
        status: 201,
        headers: { Location: cancelLocation },
      }),
    );
  const state = await readBlockState(projectId, taskId, blockId);
  const change = await cancelBlock(projectId, taskId, {
    ...cancellation,
    revision: state.revision,
  });
  expect(Date.parse(change.occurredAt)).toBeLessThan(
    Date.parse(state.updatedAt),
  );
});
