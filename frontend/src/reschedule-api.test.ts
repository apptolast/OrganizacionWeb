import { afterEach, expect, it, vi } from "vitest";
import {
  readBlockState,
  previewMove,
  sendBlockChange,
  checkBlockChange,
  readBlockChanges,
  readBlockChange,
  readChangeError,
} from "./reschedule-api";
import { agendaToday } from "./today-fixture";
const block = agendaToday().items[0].block;
const base = `/api/v1/projects/${block.projectId}/tasks/${block.taskId}/blocks`;
const revision = `"block:${block.id}:9007199254740993"`;
afterEach(() => vi.unstubAllGlobals());
it("@s1 @s4 reads state with exact BIGINT revision and no extra requests", async () => {
  const value = { block, status: "planned", updatedAt: block.createdAt };
  const fetch = vi
    .fn()
    .mockResolvedValue(Response.json(value, { headers: { ETag: revision } }));
  vi.stubGlobal("fetch", fetch);
  const signal = new AbortController().signal;
  await expect(
    readBlockState(block.projectId, block.taskId, block.id, signal),
  ).resolves.toEqual({ ...value, revision });
  expect(fetch).toHaveBeenCalledExactlyOnceWith(base + `/${block.id}/state`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
});
it("@s31 rejects a valid state ETag followed by trailing garbage", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(
      Response.json(
        { block, status: "planned", updatedAt: block.createdAt },
        { headers: { ETag: revision + "garbage" } },
      ),
    );
  vi.stubGlobal("fetch", fetch);
  await expect(
    readBlockState(block.projectId, block.taskId, block.id),
  ).rejects.toThrow();
  expect(fetch).toHaveBeenCalledTimes(1);
});
it("@s31 rejects incompatible state bodies and revisions", async () => {
  const value = { block, status: "planned", updatedAt: block.createdAt };
  for (const [body, tag] of [
    [null, revision],
    [[], revision],
    [{ ...value, extra: 1 }, revision],
    [{ status: "planned", updatedAt: block.createdAt }, revision],
    [{ ...value, block: { ...block, taskId: block.id } }, revision],
    [{ ...value, status: "done" }, revision],
    [{ ...value, updatedAt: "2030-01-07" }, revision],
    [value, null],
    [value, "W/" + revision],
    [value, `"block:${block.taskId}:2"`],
    [value, `"block:${block.id}:0"`],
    [value, `"block:${block.id}:02"`],
    [value, `"block:${block.id}:9223372036854775808"`],
  ] as const) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json(body, { headers: tag ? { ETag: tag } : {} }),
        ),
    );
    await expect(
      readBlockState(block.projectId, block.taskId, block.id),
    ).rejects.toThrow("Estado de bloque inválido");
  }
});
it("@s18 preserves HTTP failures rather than confirming their valid-looking body", async () => {
  for (const status of [401, 404, 503, 201]) {
    const response = Response.json(
      { block, status: "planned", updatedAt: block.createdAt },
      { status, headers: { ETag: revision } },
    );
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
    await expect(
      readBlockState(block.projectId, block.taskId, block.id),
    ).rejects.toBe(response);
  }
});

const input = {
  startLocal: "2030-01-07T14:00",
  endLocal: "2030-01-07T15:00",
  zoneId: "UTC",
  startOffset: null,
  endOffset: null,
};
const preview = {
  objective: block.objective,
  zoneId: "UTC",
  startAt: "2030-01-07T14:00:00Z",
  endAt: "2030-01-07T15:00:00Z",
  startOffset: "Z",
  endOffset: "Z",
  durationMinutes: 60,
  availabilityEtag: '"availability:00000000-0000-0000-0000-000000000004:1"',
  budgetZoneId: "UTC",
  days: [
    {
      date: "2030-01-07",
      budgetMinutes: 120,
      plannedSeconds: 1800,
      requestedSeconds: 3600,
      excessSeconds: 0,
    },
  ],
};
it("@s7 requests movement preview with the original objective and exact block revision", async () => {
  const fetch = vi
    .fn()
    .mockResolvedValue(Response.json(preview, { headers: { ETag: revision } }));
  vi.stubGlobal("fetch", fetch);
  const state = {
    block,
    status: "planned" as const,
    updatedAt: block.createdAt,
    revision,
  };
  await expect(previewMove(state, input)).resolves.toEqual(preview);
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/" + block.id + "/reschedule/preview",
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal: undefined,
      headers: { "Content-Type": "application/json", "If-Match": revision },
      body: JSON.stringify(input),
    },
  );
});
it("@s31 rejects incompatible previews and never replaces the retained objective", async () => {
  const state = {
    block,
    status: "planned" as const,
    updatedAt: block.createdAt,
    revision,
  };
  for (const [body, tag, status] of [
    [{ ...preview, objective: "Otro" }, revision, 200],
    [{ ...preview, startAt: block.startAt }, revision, 200],
    [{ ...preview, days: [] }, revision, 200],
    [preview, `"block:${block.id}:2"`, 200],
    [{ ...preview, availabilityEtag: '"availability:bad:1"' }, revision, 200],
    [preview, revision, 503],
  ] as const) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json(body, { status, headers: { ETag: tag } }),
        ),
    );
    await expect(previewMove(state, input)).rejects.toBeDefined();
  }
});
it("@s4 accepts a canonical state for an uppercase UUID route without rounding revision", async () => {
  const id = "abcdef00-0000-0000-0000-000000000001";
  const value = {
    block: { ...block, id },
    status: "cancelled",
    updatedAt: "2029-01-01T00:00:00Z",
  };
  const tag = `"block:${id}:9223372036854775807"`;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json(value, { headers: { ETag: tag } })),
  );
  await expect(
    readBlockState(block.projectId, block.taskId, id.toUpperCase()),
  ).resolves.toEqual({ ...value, revision: tag });
});

const state = {
  block,
  status: "planned" as const,
  updatedAt: block.createdAt,
  revision,
};
const changeId = "00000000-0000-0000-0000-000000000005";
const key = "00000000-0000-0000-0000-000000000006";
const cancellation = {
  id: changeId,
  blockId: block.id,
  kind: "CANCELLED",
  revision: '"block:' + block.id + ':9007199254740994"',
  occurredAt: "2029-01-01T00:00:00.123456Z",
  before: block,
  after: null,
};
it("@s12 @s13 transmits cancellation without availability and accepts a historical clock", async () => {
  const fetch = vi.fn().mockResolvedValue(
    Response.json(cancellation, {
      status: 201,
      headers: { Location: base + "/changes/" + changeId },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  await expect(
    sendBlockChange({ state, key, kind: "CANCELLED" }),
  ).resolves.toEqual(cancellation);
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/" + block.id + "/cancel",
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal: undefined,
      headers: {
        "Content-Type": "application/json",
        "If-Match": revision,
        "Idempotency-Key": key,
      },
      body: "{}",
    },
  );
});
it("@s32 only confirms an exact cancellation receipt and POST Location", async () => {
  for (const [value, location, status] of [
    [null, base + "/changes/" + changeId, 201],
    [{ ...cancellation, extra: 1 }, base + "/changes/" + changeId, 201],
    [{ ...cancellation, id: "not-id" }, base + "/changes/" + changeId, 201],
    [
      { ...cancellation, kind: "RESCHEDULED" },
      base + "/changes/" + changeId,
      201,
    ],
    [{ ...cancellation, after: block }, base + "/changes/" + changeId, 201],
    [
      { ...cancellation, blockId: block.taskId },
      base + "/changes/" + changeId,
      201,
    ],
    [
      { ...cancellation, before: { ...block, objective: "Otro" } },
      base + "/changes/" + changeId,
      201,
    ],
    [{ ...cancellation, revision }, base + "/changes/" + changeId, 201],
    [
      { ...cancellation, occurredAt: "yesterday" },
      base + "/changes/" + changeId,
      201,
    ],
    [cancellation, "https://outside.invalid/receipt", 200],
    [cancellation, base + "/changes/" + changeId, 503],
  ] as const) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json(value, { status, headers: { Location: location } }),
        ),
    );
    await expect(
      sendBlockChange({ state, key, kind: "CANCELLED" }),
    ).rejects.toBeDefined();
  }
});
const destination = {
  ...block,
  startAt: preview.startAt,
  endAt: preview.endAt,
  zoneId: preview.zoneId,
  durationMinutes: preview.durationMinutes,
};
const movement = { ...cancellation, kind: "RESCHEDULED", after: destination };
const moveInput = {
  ...input,
  startOffset: "Z",
  endOffset: "Z",
  allowOverBudget: false,
};
it("@s11 @s15 sends movement and replays the same receipt with exact retained headers", async () => {
  const fetch = vi.fn().mockResolvedValue(
    Response.json(movement, {
      status: 200,
      headers: { Location: base + "/changes/" + changeId },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  await expect(
    sendBlockChange({
      state,
      key,
      kind: "RESCHEDULED",
      input: moveInput,
      preview,
    }),
  ).resolves.toEqual(movement);
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/" + block.id + "/reschedule",
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal: undefined,
      headers: {
        "Content-Type": "application/json",
        "If-Match": revision,
        "Idempotency-Key": key,
        "Availability-Revision": preview.availabilityEtag,
      },
      body: JSON.stringify(moveInput),
    },
  );
});
it("@s29 @s32 refuses a movement whose retained review no longer matches input", async () => {
  const fetch = vi.fn().mockResolvedValue(
    Response.json(movement, {
      status: 201,
      headers: { Location: base + "/changes/" + changeId },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  await expect(
    sendBlockChange({
      state,
      key,
      kind: "RESCHEDULED",
      input: { ...moveInput, endLocal: "2030-01-07T16:00" },
      preview,
    }),
  ).rejects.toThrow("Intención de cambio inválida");
  expect(fetch).not.toHaveBeenCalled();
});

it("@s25 @s34 recovers the retained receipt by key without requiring GET Location", async () => {
  const fetch = vi.fn().mockResolvedValue(Response.json(movement));
  vi.stubGlobal("fetch", fetch);
  const signal = new AbortController().signal;
  await expect(
    checkBlockChange(
      { state, key, kind: "RESCHEDULED", input: moveInput, preview },
      signal,
    ),
  ).resolves.toEqual(movement);
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/changes/by-request/" + key,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it("@s34 rejects recovery with a well-formed after whose objective differs from the retained movement", async () => {
  const fetch = vi.fn().mockResolvedValue(
    Response.json({
      ...movement,
      after: { ...destination, objective: "Otro objetivo válido" },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  await expect(
    checkBlockChange({
      state,
      key,
      kind: "RESCHEDULED",
      input: moveInput,
      preview,
    }),
  ).rejects.toThrow();
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/changes/by-request/" + key,
    {
      credentials: "same-origin",
      cache: "no-store",
      signal: undefined,
    },
  );
});
it("@s37 retains request snapshots across asynchronous POST and recovery", async () => {
  for (const operation of [sendBlockChange, checkBlockChange]) {
    const request = {
      state: { ...state, block: { ...block } },
      key,
      kind: "CANCELLED" as const,
    };
    let finish!: (r: Response) => void;
    vi.stubGlobal(
      "fetch",
      vi.fn().mockReturnValue(
        new Promise<Response>((r) => {
          finish = r;
        }),
      ),
    );
    const result = operation(request);
    request.state.block.objective = "Modified while waiting";
    request.state.revision = `"block:${block.id}:2"`;
    finish(
      Response.json(cancellation, {
        headers: { Location: base + "/changes/" + changeId },
      }),
    );
    await expect(result).resolves.toEqual(cancellation);
  }
});

it("@s16 @s39 reads paginated changes independently of active blocks", async () => {
  const value = {
    items: [
      movement,
      { ...cancellation, id: "00000000-0000-0000-0000-000000000004" },
    ],
    nextCursor: "opaque+/cursor",
  };
  const fetch = vi.fn().mockResolvedValue(Response.json(value));
  vi.stubGlobal("fetch", fetch);
  const signal = new AbortController().signal;
  await expect(
    readBlockChanges(block.projectId, block.taskId, "prior+/cursor", signal),
  ).resolves.toEqual(value);
  expect(fetch).toHaveBeenCalledExactlyOnceWith(
    base + "/changes?cursor=prior%2B%2Fcursor",
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it("@s16 @s31 rejects malformed history instead of showing an empty or incoherent fact", async () => {
  for (const value of [
    null,
    [],
    { items: [], nextCursor: null, extra: true },
    { items: [], nextCursor: 0 },
    { items: [...Array(21).fill(cancellation)], nextCursor: null },
    ...[
      null,
      { ...movement, extra: 1 },
      { ...movement, id: "bad" },
      { ...movement, kind: "OTHER" },
      { ...movement, revision: `"block:${block.taskId}:2"` },
      { ...movement, occurredAt: "bad" },
      { ...movement, before: { ...block, taskId: block.id } },
      { ...movement, after: null },
      { ...movement, after: { ...destination, objective: "Other" } },
      { ...cancellation, after: block },
    ].map((item) => ({ items: [item], nextCursor: null })),
  ]) {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
    await expect(
      readBlockChanges(block.projectId, block.taskId),
    ).rejects.toThrow("Historial de bloques inválido");
  }
});
it("@s32 never confirms a revision beyond BIGINT even after reading its valid maximum", async () => {
  const maximum = {
    ...state,
    revision: `"block:${block.id}:9223372036854775807"`,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...cancellation,
          revision: `"block:${block.id}:9223372036854775808"`,
        },
        { status: 201, headers: { Location: base + "/changes/" + changeId } },
      ),
    ),
  );
  await expect(
    sendBlockChange({ state: maximum, key, kind: "CANCELLED" }),
  ).rejects.toThrow("Confirmación de cambio inválida");
});

it("@s18 reads a historical receipt by id without requiring Location", async () => {
  const fetch = vi.fn().mockResolvedValue(Response.json(cancellation));
  vi.stubGlobal("fetch", fetch);
  await expect(
    readBlockChange(block.projectId, block.taskId, changeId),
  ).resolves.toEqual(cancellation);
  expect(fetch).toHaveBeenCalledExactlyOnceWith(base + "/changes/" + changeId, {
    credentials: "same-origin",
    cache: "no-store",
    signal: undefined,
  });
});

it("@s33 @s35 classifies new definitive errors without trusting unknown bodies", async () => {
  for (const [code, status] of [
    ["BLOCK_CONFLICT", 412],
    ["BLOCK_CANCELLED", 409],
    ["BLOCK_UNCHANGED", 409],
    ["BLOCK_VERSION_EXHAUSTED", 409],
    ["BLOCK_CHANGE_NOT_FOUND", 404],
  ] as const) {
    const value = {
      type: "urn:organization:problem:" + code.toLowerCase(),
      title: "Revisa el bloque",
      code,
      status,
    };
    await expect(
      readChangeError(Response.json(value, { status })),
    ).resolves.toEqual(value);
    await expect(
      readChangeError(Response.json({ ...value, extra: 1 }, { status })),
    ).resolves.toBeNull();
    await expect(
      readChangeError(Response.json(value, { status: 500 })),
    ).resolves.toBeNull();
  }
});
it("@s35 preserves inherited budget and CSRF errors without weakening their schema", async () => {
  for (const value of [
    {
      type: "urn:organization:problem:csrf_invalid",
      title: "Renueva acceso",
      status: 403,
      code: "CSRF_INVALID",
    },
    {
      type: "urn:organization:problem:budget_exceeded",
      title: "Revisa capacidad",
      status: 409,
      code: "BUDGET_EXCEEDED",
      budgetZoneId: "UTC",
      days: [{ ...preview.days[0], budgetMinutes: 0, excessSeconds: 5400 }],
    },
  ])
    await expect(
      readChangeError(Response.json(value, { status: value.status })),
    ).resolves.toEqual(value);
});
it("@s16 @s18 rejects failed, duplicate or unordered history pages", async () => {
  const later = {
    ...cancellation,
    id: "00000000-0000-0000-0000-000000000007",
    occurredAt: "2029-01-01T00:00:00.123457Z",
  };
  for (const [items, status] of [
    [[], 503],
    [[cancellation, cancellation], 200],
    [[cancellation, later], 200],
    [[cancellation, { ...cancellation, id: later.id }], 200],
  ] as const) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json({ items, nextCursor: null }, { status }),
        ),
    );
    await expect(
      readBlockChanges(block.projectId, block.taskId),
    ).rejects.toBeDefined();
  }
});
