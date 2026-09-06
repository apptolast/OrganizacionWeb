import { expect, it, vi } from "vitest";
import { setCsrfToken } from "./api-client";
import {
  previewBlock,
  createBlock,
  readBlockByRequest,
  readBlocks,
  readBlock,
  readBlockError,
} from "./schedule-block-api";
it("@s45 treats already consumed error body as unknown", async () => {
  const response = Response.json(problem("TASK_COMPLETED", 409), {
    status: 409,
  });
  await response.json();
  await expect(readBlockError(response)).resolves.toBeNull();
});
const problem = (code: string, status: number, extra = {}) => ({
  type: `urn:organization:problem:${code.toLowerCase()}`,
  title: "Revisa la petición.",
  status,
  code,
  ...extra,
});
it.each([
  { errors: [] },
  { errors: [{ field: "startLocal", code: "UNKNOWN", message: "Error" }] },
  {
    errors: [
      { field: "objective", code: "REQUIRED", message: "Indica objetivo" },
      { field: "startLocal", code: "UNKNOWN", message: "Error" },
    ],
  },
  {
    errors: [
      { field: "startLocal", code: "IN_PAST", message: "Error", extra: 1 },
    ],
  },
  ...[
    [],
    ["+01:00"],
    ["+01:00", "+02:00"],
    ["+01:00", "+01:00"],
    ["+00:00", "-01:00"],
    ["+01:00:00", "Z"],
    ["+18:01", "Z"],
    ["+01:60", "Z"],
    ["+01:00:60", "Z"],
    [null, "Z"],
    ["+1:00", "Z"],
    [["+01:00"], "Z"],
    ["prefix+01:00", "Z"],
    ["+01:00suffix", "Z"],
  ].map((options) => ({
    errors: [
      { field: "startOffset", code: "AMBIGUOUS_OFFSET", message: "Elige" },
    ],
    validOffsets: { startOffset: options },
  })),
  {
    errors: [
      { field: "startOffset", code: "AMBIGUOUS_OFFSET", message: "Elige" },
    ],
    validOffsets: { endOffset: ["+02:00", "+01:00"] },
  },
  {
    errors: [{ field: "startLocal", code: "IN_PAST", message: "Error" }],
    validOffsets: { startOffset: ["Z"] },
  },
  {
    errors: [
      { field: "startOffset", code: "AMBIGUOUS_OFFSET", message: "Elige" },
      { field: "endOffset", code: "INVALID_OFFSET", message: "Elige fin" },
    ],
    validOffsets: { startOffset: ["+02:00", "+01:00"] },
  },
  {
    errors: [{ field: "startOffset", code: "IN_PAST", message: "Error" }],
    validOffsets: { startOffset: ["+02:00", "+01:00"] },
  },
  {
    errors: [
      { field: "objective", code: "AMBIGUOUS_OFFSET", message: "Elige" },
    ],
    validOffsets: { objective: ["+02:00", "+01:00"] },
  },
  {
    errors: {
      length: 1,
      0: { field: "startOffset", code: "AMBIGUOUS_OFFSET", message: "Elige" },
    },
    validOffsets: { startOffset: ["+02:00", "+01:00"] },
  },
])(
  "@s40 @s41 @s45 refuses malformed validation and occurrence options %#",
  async (extra) => {
    await expect(
      readBlockError(
        Response.json(problem("VALIDATION_ERROR", 400, extra), { status: 400 }),
      ),
    ).resolves.toBeNull();
  },
);
it.each(
  [
    ["+01:00"],
    ["+02:00", "+01:00"],
    ["-00:44:30"],
    ["Z"],
    ["+18:00"],
    ["-18:00"],
  ].map((options) => ({
    options,
  })),
)(
  "@s8 reads invalid offset with canonical valid options %#",
  async ({ options }) => {
    const body = problem("VALIDATION_ERROR", 400, {
      errors: [
        {
          field: "endOffset",
          code: "INVALID_OFFSET",
          message: "Elige offset.",
        },
      ],
      validOffsets: { endOffset: options },
    });
    await expect(
      readBlockError(Response.json(body, { status: 400 })),
    ).resolves.toEqual(body);
  },
);
it.each(["startOffset", "endOffset"])(
  "@s8 @s41 parses closed occurrence options for %s",
  async (field) => {
    const body = problem("VALIDATION_ERROR", 400, {
      errors: [
        { field, code: "AMBIGUOUS_OFFSET", message: "Elige ocurrencia." },
      ],
      validOffsets: { [field]: ["+02:00", "+01:00"] },
    });
    await expect(
      readBlockError(Response.json(body, { status: 400 })),
    ).resolves.toEqual(body);
  },
);
it.each([
  "REQUIRED",
  "INVALID_TYPE",
  "UNKNOWN_FIELD",
  "INVALID_FORMAT",
  "INVALID_VALUE",
  "OUT_OF_RANGE",
  "TOO_LONG",
  "NONEXISTENT_LOCAL_TIME",
  "IN_PAST",
])("@s4 @s5 @s6 @s7 @s49 recognizes validation %s", async (code) => {
  const body = problem("VALIDATION_ERROR", 400, {
    errors: [{ field: "startLocal", code, message: "Revisa el campo." }],
  });
  await expect(
    readBlockError(Response.json(body, { status: 400 })),
  ).resolves.toEqual(body);
});
it.each([
  new Error("network"),
  new Response("broken", { status: 409 }),
  Response.json(problem("UNKNOWN", 409), { status: 409 }),
  Response.json(problem("TASK_COMPLETED", 400), { status: 400 }),
  Response.json(problem("TASK_COMPLETED", 409, { extra: 1 }), { status: 409 }),
  Response.json(
    { ...problem("TASK_COMPLETED", 409), type: "wrong" },
    { status: 409 },
  ),
  Response.json(
    { ...problem("TASK_COMPLETED", 409), title: null },
    { status: 409 },
  ),
  Response.json(problem("TASK_COMPLETED", 409), { status: 503 }),
  Response.json(null, { status: 409 }),
])(
  "@s45 rejects unknown malformed or status-mismatched error %#",
  async (error) => {
    await expect(readBlockError(error)).resolves.toBeNull();
  },
);
it.each([
  ["BLOCK_NOT_FOUND", 404],
  ["RESOURCE_NOT_FOUND", 404],
  ["AVAILABILITY_REQUIRED", 409],
  ["AVAILABILITY_ZONE_UNAVAILABLE", 409],
  ["PROJECT_COMPLETED", 409],
  ["TASK_COMPLETED", 409],
  ["IDEMPOTENCY_CONFLICT", 409],
  ["AVAILABILITY_CONFLICT", 412],
  ["PRECONDITION_REQUIRED", 428],
  ["CSRF_INVALID", 403],
  ["STORAGE_UNAVAILABLE", 503],
  ["MALFORMED_JSON", 400],
] as const)(
  "@s47 @s49 @s50 @s51 @s52 @s62 reads recognized %s without consuming response",
  async (code, status) => {
    const body = problem(code, status);
    const response = Response.json(body, {
      status,
      headers: { "Content-Type": "application/problem+json" },
    });
    await expect(readBlockError(response)).resolves.toEqual(body);
    await expect(response.json()).resolves.toEqual(body);
  },
);
const projectId = "11111111-1111-1111-1111-111111111111";
const taskId = "22222222-2222-2222-2222-222222222222";
const input = {
  objective: "\u00a0Preparar borrador\u0085",
  startLocal: "2030-01-07T10:00",
  endLocal: "2030-01-07T11:00",
  zoneId: "UTC",
  startOffset: null,
  endOffset: null,
};
const preview = {
  objective: "Preparar borrador",
  zoneId: "UTC",
  startAt: "2030-01-07T10:00:00Z",
  endAt: "2030-01-07T11:00:00Z",
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
const block = {
  id: "44444444-4444-4444-4444-444444444444",
  projectId,
  taskId,
  objective: preview.objective,
  startAt: preview.startAt,
  endAt: preview.endAt,
  zoneId: preview.zoneId,
  durationMinutes: preview.durationMinutes,
  createdAt: "2030-01-06T10:00:00.123456Z",
};
const request = {
  input: { ...input, startOffset: "Z", endOffset: "Z", allowOverBudget: false },
  key: "55555555-5555-5555-5555-555555555555",
  availabilityRevision: preview.availabilityEtag,
};
it("@s4 accepts a preview normalized across repeated Unicode whitespace", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(preview));
  await expect(
    previewBlock(projectId, taskId, {
      ...input,
      objective: `\u00a0\u0085${preview.objective}\u0085\u00a0`,
    }),
  ).resolves.toEqual(preview);
});
it.each(["separator", "calendar"])(
  "@s40 refuses noncanonical local input despite parseable instants %s",
  async (defect) => {
    const localDate = defect === "calendar" ? "2030-02-30" : "2030-01-07";
    const separator = defect === "separator" ? " " : "T";
    const sent = {
      ...input,
      startLocal: `${localDate}${separator}10:00`,
      endLocal: `${localDate}${separator}11:00`,
    };
    const utcDate = defect === "calendar" ? "2030-03-02" : "2030-01-07";
    const body = {
      ...preview,
      startAt: `${utcDate}T10:00:00Z`,
      endAt: `${utcDate}T11:00:00Z`,
      days: [{ ...preview.days[0], date: utcDate }],
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
    await expect(previewBlock(projectId, taskId, sent)).rejects.toThrow();
  },
);
it.each(["mixedExcess", "invalidSecond", "reversed", "duplicate"])(
  "@s15 reads all budget days as one ordered collection %s",
  async (condition) => {
    const first = preview.days[0];
    const second = {
      ...first,
      date: "2030-01-08",
      budgetMinutes: 0,
      excessSeconds: 3600,
    };
    const days =
      condition === "mixedExcess"
        ? [first, second]
        : condition === "invalidSecond"
          ? [second, { ...second, date: "2030-01-09", requestedSeconds: -1 }]
          : condition === "reversed"
            ? [second, first]
            : [second, second];
    const body = problem("BUDGET_EXCEEDED", 409, { budgetZoneId: "UTC", days });
    await expect(
      readBlockError(Response.json(body, { status: 409 })),
    ).resolves.toEqual(condition === "mixedExcess" ? body : null);
  },
);
it.each([
  {
    budgetZoneId: "",
    days: [{ ...preview.days[0], budgetMinutes: 0, excessSeconds: 3600 }],
  },
  { days: [{ ...preview.days[0], budgetMinutes: 0, excessSeconds: 3600 }] },
  { budgetZoneId: "UTC", days: { length: 1 } },
  { budgetZoneId: "UTC", days: null },
  {
    budgetZoneId: "UTC",
    days: [
      {
        ...preview.days[0],
        date: [preview.days[0].date],
        budgetMinutes: 0,
        excessSeconds: 3600,
      },
    ],
  },
])(
  "@s45 treats incompatible budget containers as unknown %#",
  async (extra) => {
    await expect(
      readBlockError(
        Response.json(problem("BUDGET_EXCEEDED", 409, extra), { status: 409 }),
      ),
    ).resolves.toBeNull();
  },
);
const specializedProblems = [
  problem("BUDGET_EXCEEDED", 409, {
    budgetZoneId: "UTC",
    days: [{ ...preview.days[0], budgetMinutes: 0, excessSeconds: 3600 }],
  }),
  problem("BLOCK_OVERLAP", 409, {
    conflict: { id: block.id, projectId, taskId },
  }),
  problem("VALIDATION_ERROR", 400, {
    errors: [
      { field: "startOffset", code: "AMBIGUOUS_OFFSET", message: "Elige" },
    ],
    validOffsets: { startOffset: ["+02:00", "+01:00"] },
  }),
  problem("VALIDATION_ERROR", 400, {
    errors: [
      { field: "objective", code: "REQUIRED", message: "Indica objetivo" },
    ],
  }),
];
it.each(
  specializedProblems.flatMap((body, family) =>
    ["code", "status", "bodyStatus", "extra"].map((defect) => ({
      body,
      family,
      defect,
    })),
  ),
)(
  "@s45 rejects inconsistent specialized problem $family $defect",
  async ({ body, defect }) => {
    const wrongStatus = body.status === 400 ? 409 : 400;
    const changed =
      defect === "code"
        ? { ...body, ...problem("UNKNOWN", body.status) }
        : defect === "status" || defect === "bodyStatus"
          ? { ...body, status: wrongStatus }
          : { ...body, extra: true };
    await expect(
      readBlockError(
        Response.json(changed, {
          status: defect === "status" ? wrongStatus : body.status,
        }),
      ),
    ).resolves.toBeNull();
  },
);
it("@s6 accepts zero fractional notation as whole seconds", async () => {
  const body = {
    items: [
      {
        ...block,
        startAt: block.startAt.replace("Z", ".000Z"),
        endAt: block.endAt.replace("Z", ".000Z"),
      },
    ],
    nextCursor: null,
  };
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(readBlocks(projectId, taskId)).resolves.toEqual(body);
});
it.each(["1", "10", "100", "9223372036854775807"])(
  "@s20 accepts canonical availability revision %s",
  async (version) => {
    const body = {
      ...preview,
      availabilityEtag: preview.availabilityEtag.replace(':0"', `:${version}"`),
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
    await expect(previewBlock(projectId, taskId, input)).resolves.toEqual(body);
  },
);
it.each(["list", "preview"])(
  "@s4 accepts exactly five hundred Unicode points in %s",
  async (operation) => {
    const objective = "🧭".repeat(500);
    const body =
      operation === "list"
        ? { items: [{ ...block, objective }], nextCursor: null }
        : { ...preview, objective };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
    await expect(
      operation === "list"
        ? readBlocks(projectId, taskId)
        : previewBlock(projectId, taskId, { ...input, objective }),
    ).resolves.toEqual(body);
  },
);
it.each([
  problem("BLOCK_OVERLAP", 409),
  problem("BLOCK_OVERLAP", 409, {
    conflict: { id: block.id, projectId, taskId, extra: 1 },
  }),
  problem("BLOCK_OVERLAP", 409, {
    conflict: { id: "invalid", projectId, taskId },
  }),
  problem("BLOCK_OVERLAP", 409, {
    conflict: { id: block.id, projectId: "invalid", taskId },
  }),
  problem("BLOCK_OVERLAP", 409, {
    conflict: { id: block.id, projectId, taskId: "invalid" },
  }),
  problem("BUDGET_EXCEEDED", 409, { budgetZoneId: "UTC", days: [] }),
  problem("BUDGET_EXCEEDED", 409, { budgetZoneId: "", days: preview.days }),
  problem("BUDGET_EXCEEDED", 409, { budgetZoneId: "UTC", days: preview.days }),
  problem("BUDGET_EXCEEDED", 409, {
    budgetZoneId: "UTC",
    days: [{ ...preview.days[0], excessSeconds: 1 }],
  }),
])(
  "@s45 @s49 does not treat malformed conflict as definitive %#",
  async (body) => {
    await expect(
      readBlockError(Response.json(body, { status: 409 })),
    ).resolves.toBeNull();
  },
);
it.each(["preview", "create", "recover"] as const)(
  "@s44 @s46 @s53 retains expected values while %s waits",
  async (operation) => {
    let resolve!: (response: Response) => void;
    vi.spyOn(globalThis, "fetch").mockImplementationOnce(
      () =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    );
    const edited = structuredClone(input),
      expected = structuredClone(preview);
    const result =
      operation === "preview"
        ? previewBlock(projectId, taskId, edited)
        : operation === "create"
          ? createBlock(projectId, taskId, request, expected)
          : readBlockByRequest(projectId, taskId, request.key, expected);
    edited.objective = "Editado";
    expected.objective = "Editado";
    resolve(Response.json(operation === "preview" ? preview : block));
    await expect(result).resolves.toEqual(
      operation === "preview" ? preview : block,
    );
  },
);
it.each([
  {
    zone: "Europe/Madrid",
    start: "2026-10-25T02:45",
    end: "2026-10-25T02:15",
    startOffset: "+02:00",
    endOffset: "+01:00",
    startAt: "2026-10-25T00:45:00Z",
    endAt: "2026-10-25T01:15:00Z",
    duration: 30,
  },
  {
    zone: "Australia/Lord_Howe",
    start: "2026-04-05T01:45",
    end: "2026-04-05T02:15",
    startOffset: "+11:00",
    endOffset: "+10:30",
    startAt: "2026-04-04T14:45:00Z",
    endAt: "2026-04-04T15:45:00Z",
    duration: 60,
  },
  {
    zone: "Historical/ServerZone",
    start: "1972-01-06T22:00",
    end: "1972-01-06T23:00",
    startOffset: "-00:44:30",
    endOffset: "-00:44:30",
    startAt: "1972-01-06T22:44:30Z",
    endAt: "1972-01-06T23:44:30Z",
    duration: 60,
  },
  {
    zone: "UTC",
    start: "0001-01-01T00:00",
    end: "0001-01-01T00:01",
    startOffset: "Z",
    endOffset: "Z",
    startAt: "0001-01-01T00:00:00Z",
    endAt: "0001-01-01T00:01:00Z",
    duration: 1,
  },
  {
    zone: "UTC",
    start: "9999-12-30T23:59",
    end: "9999-12-31T23:59",
    startOffset: "Z",
    endOffset: "Z",
    startAt: "9999-12-30T23:59:00Z",
    endAt: "9999-12-31T23:59:00Z",
    duration: 1440,
  },
])(
  "@s6 @s8 @s10 @s40 accepts server-resolved instants without browser TZDB %#",
  async (row) => {
    const sent = {
      ...input,
      zoneId: row.zone,
      startLocal: row.start,
      endLocal: row.end,
      startOffset: row.startOffset,
      endOffset: row.endOffset,
    };
    const result = {
      ...preview,
      zoneId: row.zone,
      startAt: row.startAt,
      endAt: row.endAt,
      startOffset: row.startOffset,
      endOffset: row.endOffset,
      durationMinutes: row.duration,
      days: [
        {
          date: row.startAt.slice(0, 10),
          budgetMinutes: 1440,
          plannedSeconds: 0,
          requestedSeconds: row.duration === 1440 ? 60 : row.duration * 60,
          excessSeconds: 0,
        },
      ],
    };
    if (row.duration === 1440)
      result.days.push({
        date: "9999-12-31",
        budgetMinutes: 1440,
        plannedSeconds: 0,
        requestedSeconds: 86340,
        excessSeconds: 0,
      });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(result));
    await expect(previewBlock(projectId, taskId, sent)).resolves.toEqual(
      result,
    );
  },
);
it("@s44 @s48 @s51 transmits identical retained intent and precondition on manual resend with current CSRF", async () => {
  setCsrfToken("renewed-token");
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(
      Response.json(problem("STORAGE_UNAVAILABLE", 503), { status: 503 }),
    )
    .mockResolvedValueOnce(Response.json(block));
  const retained = structuredClone(request);
  try {
    await expect(
      createBlock(projectId, taskId, retained, preview),
    ).rejects.toBeInstanceOf(Response);
    expect(fetcher).toHaveBeenCalledTimes(1);
    await expect(
      createBlock(projectId, taskId, retained, preview),
    ).resolves.toEqual(block);
    expect(fetcher.mock.calls[0]).toEqual(fetcher.mock.calls[1]);
    const headers = new Headers(fetcher.mock.calls[1][1]?.headers);
    expect(headers.get("X-CSRF-TOKEN")).toBe("renewed-token");
    expect(headers.get("Idempotency-Key")).toBe(request.key);
    expect(headers.get("Availability-Revision")).toBe(preview.availabilityEtag);
    expect(retained).toEqual(request);
  } finally {
    setCsrfToken();
  }
});
it("@s14 @s49 reads closed overlap identity", async () => {
  const body = problem("BLOCK_OVERLAP", 409, {
    conflict: { id: block.id, projectId, taskId },
  });
  await expect(
    readBlockError(Response.json(body, { status: 409 })),
  ).resolves.toEqual(body);
});
it("@s15 @s49 reads recalculated excess days", async () => {
  const body = problem("BUDGET_EXCEEDED", 409, {
    budgetZoneId: "UTC",
    days: [{ ...preview.days[0], budgetMinutes: 0, excessSeconds: 3600 }],
  });
  await expect(
    readBlockError(Response.json(body, { status: 409 })),
  ).resolves.toEqual(body);
});
it.each([
  { status: 404, body: block },
  { status: 503, body: block },
  { status: 200, body: { ...block, id: taskId } },
  { status: 200, body: { ...block, projectId: taskId } },
  { status: 200, body: { ...block, extra: 1 } },
])(
  "@s26 rejects wrong detail and preserves HTTP error %#",
  async ({ status, body }) => {
    const response = Response.json(body, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    const operation = readBlock(projectId, taskId, block.id);
    if (status === 200)
      await expect(operation).rejects.toThrow("Respuesta de bloque inválida");
    else await expect(operation).rejects.toBe(response);
  },
);
it("@s26 reads a specific block using signal", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(block));
  await expect(readBlock(projectId, taskId, block.id, signal)).resolves.toEqual(
    block,
  );
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/${block.id}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([201, 401, 404, 503])(
  "@s25 @s26 @s52 @s54 does not invent empty list on HTTP %s",
  async (status) => {
    const response = Response.json({ items: [], nextCursor: null }, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readBlocks(projectId, taskId)).rejects.toBe(response);
  },
);
it.each([
  null,
  [],
  {},
  { items: [] },
  { items: [], nextCursor: 1 },
  { items: [], nextCursor: "" },
  { items: [], nextCursor: null, extra: true },
  { items: Array(21).fill(block), nextCursor: null },
  { items: [{ ...block, taskId: projectId }], nextCursor: null },
  { items: [{ ...block, objective: "" }], nextCursor: null },
  { items: [{ ...block, durationMinutes: 0 }], nextCursor: null },
  { items: [{ ...block, durationMinutes: 1441 }], nextCursor: null },
  { items: [{ ...block, startAt: "0000-01-01T00:00:00Z" }], nextCursor: null },
  ...[
    { id: [block.id] },
    { id: `prefix${block.id}` },
    { id: `${block.id}suffix` },
    { objective: "🧭".repeat(501) },
    { objective: "  Objetivo  " },
    { durationMinutes: 59 },
    {
      startAt: block.startAt.replace("Z", ".100Z"),
      endAt: block.endAt.replace("Z", ".100Z"),
    },
  ].map((change) => ({ items: [{ ...block, ...change }], nextCursor: null })),
])("@s25 @s26 @s54 rejects malformed list %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(readBlocks(projectId, taskId)).rejects.toThrow(
    "Lista de bloques inválida",
  );
});
it.each([undefined, "cursor+/="])(
  "@s25 @s26 reads private blocks page and opaque cursor %s",
  async (cursor) => {
    const signal = new AbortController().signal;
    const page = { items: [block], nextCursor: "next" };
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(page));
    await expect(
      readBlocks(projectId, taskId, cursor, signal),
    ).resolves.toEqual(page);
    expect(fetcher).toHaveBeenCalledExactlyOnceWith(
      `/api/v1/projects/${projectId}/tasks/${taskId}/blocks` +
        (cursor ? `?cursor=${encodeURIComponent(cursor)}` : ""),
      { credentials: "same-origin", cache: "no-store", signal },
    );
  },
);
it("@s25 accepts a full terminal page of twenty blocks", async () => {
  const page = {
    items: Array.from({ length: 20 }, (_, index) => ({
      ...block,
      id: `44444444-4444-4444-4444-${String(index).padStart(12, "0")}`,
    })),
    nextCursor: null,
  };
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(page));
  await expect(readBlocks(projectId, taskId)).resolves.toEqual(page);
});
it.each([
  { status: 404, body: { code: "BLOCK_NOT_FOUND" } },
  { status: 503, body: {} },
  { status: 200, body: { ...block, objective: "Otro" } },
  { status: 200, body: { ...block, extra: true } },
  { status: 200, body: { ...block, taskId: projectId } },
])(
  "@s47 rejects missing failed or foreign recovery %#",
  async ({ status, body }) => {
    const response = Response.json(body, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    const operation = readBlockByRequest(
      projectId,
      taskId,
      request.key,
      preview,
    );
    if (status === 200)
      await expect(operation).rejects.toThrow(
        "Confirmación de bloque inválida",
      );
    else await expect(operation).rejects.toBe(response);
  },
);
it("@s46 recovers only matching retained confirmation with signal", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(block));
  await expect(
    readBlockByRequest(projectId, taskId, request.key, preview, signal),
  ).resolves.toEqual(block);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/by-request/${request.key}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});
it.each([
  { input: { ...request.input, objective: "Otro" } },
  { input: { ...request.input, startOffset: "+01:00" } },
  { input: { ...request.input, endOffset: "+01:00" } },
  {
    availabilityRevision:
      '"availability:33333333-3333-3333-3333-333333333333:1"',
  },
])(
  "@s44 refuses mismatched retained request before creation %#",
  async (change) => {
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(block, { status: 201 }));
    await expect(
      createBlock(projectId, taskId, { ...request, ...change }, preview),
    ).rejects.toThrow("Intención de bloque inválida");
    expect(fetcher).not.toHaveBeenCalled();
  },
);
it.each([400, 401, 403, 404, 409, 412, 428, 503])(
  "@s45 @s49 @s50 @s51 @s52 preserves creation HTTP rejection %s",
  async (status) => {
    const response = Response.json(block, { status });
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(response);
    await expect(createBlock(projectId, taskId, request, preview)).rejects.toBe(
      response,
    );
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
it.each([{ objective: "" }, { objective: "x".repeat(501) }, { zoneId: "" }])(
  "@s40 rejects intrinsically invalid matching text %#",
  async (change) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...preview, ...change }),
    );
    await expect(
      previewBlock(projectId, taskId, { ...input, ...change }),
    ).rejects.toThrow("Revisión de bloque inválida");
  },
);
it.each(["startAt", "endAt"] as const)(
  "@s40 @s45 rejects fractional second mismatches in %s without truncation",
  async (field) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({
        ...preview,
        [field]: preview[field].replace("Z", ".000001Z"),
      }),
    );
    await expect(previewBlock(projectId, taskId, input)).rejects.toThrow(
      "Revisión de bloque inválida",
    );
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(
        { ...block, [field]: block[field].replace("Z", ".000001Z") },
        { status: 201 },
      ),
    );
    await expect(
      createBlock(projectId, taskId, request, preview),
    ).rejects.toThrow("Confirmación de bloque inválida");
  },
);
it.each([
  null,
  [],
  { ...block, extra: 1 },
  ...Object.keys(block).map((key) =>
    Object.fromEntries(Object.entries(block).filter(([name]) => key !== name)),
  ),
  { ...block, id: "invalid" },
  { ...block, projectId: taskId },
  { ...block, taskId: projectId },
  { ...block, objective: "Otro" },
  { ...block, startAt: "2030-01-07T09:00:00Z" },
  { ...block, endAt: "2030-01-07T12:00:00Z" },
  { ...block, zoneId: "Europe/Madrid" },
  { ...block, durationMinutes: 61 },
  { ...block, createdAt: "2030-02-30T00:00:00Z" },
  { ...block, createdAt: "2030-01-06T00:00:00.1234567Z" },
])("@s45 @s46 rejects incompatible creation DTO %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(body, { status: 201 }),
  );
  await expect(
    createBlock(projectId, taskId, request, preview),
  ).rejects.toThrow("Confirmación de bloque inválida");
});
it.each([200, 201])(
  "@s2 @s44 @s46 sends retained creation and accepts confirmation HTTP %s",
  async (status) => {
    const signal = new AbortController().signal;
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json(block, { status }));
    await expect(
      createBlock(projectId, taskId, request, preview, signal),
    ).resolves.toEqual(block);
    expect(fetcher).toHaveBeenCalledExactlyOnceWith(
      `/api/v1/projects/${projectId}/tasks/${taskId}/blocks`,
      {
        method: "POST",
        credentials: "same-origin",
        cache: "no-store",
        signal,
        headers: {
          "Content-Type": "application/json",
          "Idempotency-Key": request.key,
          "Availability-Revision": request.availabilityRevision,
        },
        body: JSON.stringify(request.input),
      },
    );
  },
);
it("@s1 @s3 sends a preview without creating, retaining signal and exact input", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(preview));
  await expect(previewBlock(projectId, taskId, input, signal)).resolves.toEqual(
    preview,
  );
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${projectId}/tasks/${taskId}/blocks/preview`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    },
  );
});
it.each([201, 400, 401, 403, 404, 409, 412, 428, 503])(
  "@s40 @s52 preview preserves HTTP rejection %s",
  async (status) => {
    const response = Response.json(preview, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(previewBlock(projectId, taskId, input)).rejects.toBe(response);
  },
);
it.each([
  null,
  [],
  { ...preview, extra: 1 },
  ...Object.keys(preview).map((key) =>
    Object.fromEntries(
      Object.entries(preview).filter(([name]) => name !== key),
    ),
  ),
  { ...preview, objective: "Otro" },
  { ...preview, zoneId: "Europe/Madrid" },
  { ...preview, startAt: "2030-01-07T09:00:00Z" },
  { ...preview, endAt: "2030-01-07T12:00:00Z" },
  { ...preview, startOffset: "+01:00" },
  { ...preview, durationMinutes: 59 },
  {
    ...preview,
    durationMinutes: 59,
    days: [{ ...preview.days[0], requestedSeconds: 3540 }],
  },
  { ...preview, availabilityEtag: '"availability:unconfigured"' },
  ...[
    [preview.availabilityEtag],
    `prefix${preview.availabilityEtag}`,
    `${preview.availabilityEtag}suffix`,
    preview.availabilityEtag.replace(':0"', ':-1"'),
    preview.availabilityEtag.replace(':0"', ':01"'),
  ].map((availabilityEtag) => ({ ...preview, availabilityEtag })),
  {
    ...preview,
    availabilityEtag:
      '"availability:33333333-3333-3333-3333-333333333333:9223372036854775808"',
  },
  { ...preview, days: [] },
  { ...preview, days: [preview.days[0], preview.days[0]] },
  ...[
    { budgetMinutes: -1 },
    { budgetMinutes: 1441 },
    { plannedSeconds: -1 },
    { plannedSeconds: 0.5 },
    { requestedSeconds: 0 },
    { requestedSeconds: -1 },
    { requestedSeconds: 0.5 },
    { requestedSeconds: 3000 },
    { excessSeconds: -1 },
    { excessSeconds: 0.5 },
    { excessSeconds: 1 },
    { date: "2030-02-30" },
    { extra: true },
  ].map((day) => ({ ...preview, days: [{ ...preview.days[0], ...day }] })),
])("@s40 rejects incompatible preview %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(previewBlock(projectId, taskId, input)).rejects.toThrow(
    "Revisión de bloque inválida",
  );
});
