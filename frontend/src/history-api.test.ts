import { afterEach, expect, it, vi } from "vitest";
import { readHistory } from "./history-api";

afterEach(() => vi.unstubAllGlobals());

it("@s6 reads an empty history with private request options and cancellation", async () => {
  const page = { items: [], nextCursor: null };
  const fetcher = vi.fn().mockResolvedValue(Response.json(page));
  vi.stubGlobal("fetch", fetcher);
  const controller = new AbortController();
  await expect(
    readHistory(new URLSearchParams(), controller.signal),
  ).resolves.toEqual(page);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith("/api/v1/history", {
    credentials: "same-origin",
    cache: "no-store",
    signal: controller.signal,
  });
});

it("@s35 preserves filters and cursor when reading the requested page", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json({ items: [], nextCursor: null }));
  vi.stubGlobal("fetch", fetcher);
  const query = new URLSearchParams(
    "category=sessions&from=2026-09-07&cursor=opaque",
  );
  await readHistory(query);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/history?${query}`,
    expect.anything(),
  );
});

it("@s23 preserves an unavailable response for the caller", async () => {
  const response = new Response(null, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readHistory(new URLSearchParams())).rejects.toBe(response);
});

it("@s26 rejects an envelope with an extra field instead of accepting an empty page", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ items: [], nextCursor: null, total: 0 }),
      ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a non-array items field", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items: null, nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "Europe/Madrid",
};
const startEntry = {
  id: session.id,
  type: "SESSION_STARTED",
  occurredAt: session.startedAt,
  projectId: session.projectId,
  projectName: "Proyecto actual",
  taskId: session.taskId,
  taskTitle: "Tarea actual",
  details: session,
};

it("@s1 reads an original session start without consulting its current state", async () => {
  const page = { items: [startEntry], nextCursor: null };
  const fetcher = vi.fn().mockResolvedValue(Response.json(page));
  vi.stubGlobal("fetch", fetcher);
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
  expect(fetcher).toHaveBeenCalledTimes(1);
});

it("@s26 rejects a session start whose exact planned end is inconsistent", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [
          {
            ...startEntry,
            details: {
              ...session,
              plannedEndAt: "2026-09-07T10:25:00.123457Z",
            },
          },
        ],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects an unknown family even when its details are a valid session", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, type: "SESSION_ENDED" }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s1 reads a task completion using its historical status details", async () => {
  const entry = {
    ...startEntry,
    type: "TASK_STATUS_CHANGED",
    details: {
      id: startEntry.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: startEntry.occurredAt,
    },
  };
  const page = { items: [entry], nextCursor: null };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
});

const block = {
  id: session.id,
  projectId: session.projectId,
  taskId: session.taskId,
  objective: "Reserva original",
  startAt: "2026-10-01T10:00:00Z",
  endAt: "2026-10-01T10:25:00Z",
  zoneId: session.zoneId,
  durationMinutes: 25,
  createdAt: session.startedAt,
};
it("@s1 reads initial planning at creation time rather than its future interval", async () => {
  const page = {
    items: [{ ...startEntry, type: "BLOCK_PLANNED", details: block }],
    nextCursor: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
});

it("@s1 reads a cancelled reservation from its immutable receipt", async () => {
  const details = {
    id: session.taskId,
    blockId: block.id,
    kind: "CANCELLED",
    revision: `"block:${block.id}:2"`,
    occurredAt: session.startedAt,
    before: block,
    after: null,
  };
  const page = {
    items: [{ ...startEntry, id: details.id, type: "BLOCK_CHANGED", details }],
    nextCursor: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
});

const running = {
  session,
  status: "running",
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const pause = {
  id: session.taskId,
  sessionId: session.id,
  action: "PAUSE",
  occurredAt: "2026-09-07T10:01:00.123456Z",
  before: running,
  after: {
    ...running,
    status: "paused",
    revision: "2",
    changedAt: "2026-09-07T10:01:00.123456Z",
    workedMicroseconds: "60000000",
    runningSince: null,
  },
};
it("@s1 reads a pause receipt with its exact closed interval", async () => {
  const page = {
    items: [
      {
        ...startEntry,
        id: pause.id,
        occurredAt: pause.occurredAt,
        type: "SESSION_CHANGED",
        details: pause,
      },
    ],
    nextCursor: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
});

it("@s26 rejects an entry missing its task title", async () => {
  const entry: Record<string, unknown> = { ...startEntry };
  delete entry.taskTitle;
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects an empty current project name", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, projectName: "" }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a null current task title", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, taskTitle: null }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects an outer ID different from the valid session start", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, id: session.taskId }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a start with an outer occurrence shifted by one microsecond", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, occurredAt: "2026-09-07T10:00:00.123457Z" }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects session details belonging to another outer project", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, projectId: session.taskId }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects session details belonging to another outer task", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, taskId: session.projectId }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a reservation whose outer occurrence is its future start instead of creation", async () => {
  const entry = {
    ...startEntry,
    type: "BLOCK_PLANNED",
    occurredAt: block.startAt,
    details: block,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a pause receipt from a different outer task", async () => {
  const entry = {
    ...startEntry,
    id: pause.id,
    type: "SESSION_CHANGED",
    occurredAt: pause.occurredAt,
    taskId: session.projectId,
    details: pause,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a pause receipt from a different outer project", async () => {
  const entry = {
    ...startEntry,
    id: pause.id,
    type: "SESSION_CHANGED",
    occurredAt: pause.occurredAt,
    projectId: session.taskId,
    details: pause,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a reservation whose outer ID differs from the original block", async () => {
  const entry = {
    ...startEntry,
    id: session.taskId,
    type: "BLOCK_PLANNED",
    details: block,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a pause with a different receipt ID in the envelope", async () => {
  const entry = {
    ...startEntry,
    type: "SESSION_CHANGED",
    occurredAt: pause.occurredAt,
    details: pause,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a pause with a different occurrence in the envelope", async () => {
  const entry = {
    ...startEntry,
    id: pause.id,
    type: "SESSION_CHANGED",
    details: pause,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a task fact with a different outer receipt ID", async () => {
  const entry = {
    ...startEntry,
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.taskId,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: session.startedAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a task fact with a different outer occurrence", async () => {
  const entry = {
    ...startEntry,
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: pause.occurredAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

const cancelled = {
  id: session.taskId,
  blockId: block.id,
  kind: "CANCELLED",
  revision: `"block:${block.id}:2"`,
  occurredAt: session.startedAt,
  before: block,
  after: null,
};
it("@s26 rejects a block change with a different outer receipt ID", async () => {
  const entry = { ...startEntry, type: "BLOCK_CHANGED", details: cancelled };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a block change with a different outer occurrence", async () => {
  const entry = {
    ...startEntry,
    id: cancelled.id,
    type: "BLOCK_CHANGED",
    occurredAt: pause.occurredAt,
    details: cancelled,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects an invalid project ID on a task fact without contextual details", async () => {
  const entry = {
    ...startEntry,
    projectId: "invalid",
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: session.startedAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects an invalid task ID on a task fact without contextual details", async () => {
  const entry = {
    ...startEntry,
    taskId: "invalid",
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: session.startedAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects twenty-one entries instead of exposing the lookahead candidate", async () => {
  const items = Array.from({ length: 21 }, (_, index) => {
    const id = `12345678-1234-1234-1234-${String(21 - index).padStart(12, "0")}`;
    return { ...startEntry, id, details: { ...session, id } };
  });
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items, nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects a non-text next cursor", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items: [], nextCursor: 42 })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects an empty next cursor", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items: [], nextCursor: "" })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

function historicalStart(id: string, fraction: string) {
  const details = {
    ...session,
    id,
    startedAt: `1600-01-01T10:00:00.${fraction}Z`,
    plannedEndAt: `1600-01-01T10:25:00.${fraction}Z`,
  };
  return { ...startEntry, id, occurredAt: details.startedAt, details };
}
it("@s27 rejects ascending microseconds beyond the safe Number epoch", async () => {
  const items = [
    historicalStart(session.id, "123456"),
    historicalStart(session.taskId, "123457"),
  ];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items, nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s27 rejects a reversed family rank at an equal microsecond", async () => {
  const items = [
    { ...startEntry, type: "BLOCK_PLANNED", details: block },
    startEntry,
  ];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items, nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s27 rejects ascending UUIDs within the same family and instant", async () => {
  const items = [
    startEntry,
    {
      ...startEntry,
      id: session.taskId,
      details: { ...session, id: session.taskId },
    },
  ];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items, nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s27 rejects a duplicate type and ID even with different valid occurrences", async () => {
  const items = [
    historicalStart(session.id, "123457"),
    historicalStart(session.id, "123456"),
  ];
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ items, nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s27 rejects a valid fact outside the requested project", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ items: [startEntry], nextCursor: null }),
      ),
  );
  await expect(
    readHistory(new URLSearchParams({ projectId: session.taskId })),
  ).rejects.toThrow("Historial inválido");
});

it("@s27 rejects a valid fact outside the requested task", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ items: [startEntry], nextCursor: null }),
      ),
  );
  await expect(
    readHistory(
      new URLSearchParams({
        projectId: session.projectId,
        taskId: session.projectId,
      }),
    ),
  ).rejects.toThrow("Historial inválido");
});

it("@s27 rejects a session fact under the planning category", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ items: [startEntry], nextCursor: null }),
      ),
  );
  await expect(
    readHistory(new URLSearchParams({ category: "planning" })),
  ).rejects.toThrow("Historial inválido");
});

it("@s27 rejects a fact before the inclusive UTC from date", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ items: [startEntry], nextCursor: null }),
      ),
  );
  await expect(
    readHistory(new URLSearchParams({ from: "2026-09-08" })),
  ).rejects.toThrow("Historial inválido");
});

it("@s27 rejects a fact after the inclusive UTC to date", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ items: [startEntry], nextCursor: null }),
      ),
  );
  await expect(
    readHistory(new URLSearchParams({ to: "2026-09-06" })),
  ).rejects.toThrow("Historial inválido");
});

it("@s26 rejects year zero even when the inherited task receipt accepts that instant", async () => {
  const occurredAt = "0000-01-01T00:00:00Z";
  const entry = {
    ...startEntry,
    occurredAt,
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects noncanonical uppercase fact identity despite matching inherited details", async () => {
  const id = session.id.toUpperCase();
  const entry = { ...startEntry, id, details: { ...session, id } };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects noncanonical project identity on a task history entry", async () => {
  const entry = {
    ...startEntry,
    projectId: session.projectId.toUpperCase(),
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: session.startedAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 rejects noncanonical task identity on a task history entry", async () => {
  const entry = {
    ...startEntry,
    taskId: session.taskId.toUpperCase(),
    type: "TASK_STATUS_CHANGED",
    details: {
      id: session.id,
      fromStatus: "pending",
      toStatus: "completed",
      occurredAt: session.startedAt,
    },
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ items: [entry], nextCursor: null })),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s36 validates against the query sent even if the caller changes its draft while pending", async () => {
  let resolve!: (value: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(
      new Promise<Response>((done) => {
        resolve = done;
      }),
    ),
  );
  const query = new URLSearchParams({ projectId: session.projectId });
  const result = readHistory(query);
  query.set("projectId", session.taskId);
  const page = { items: [startEntry], nextCursor: null };
  resolve(Response.json(page));
  await expect(result).resolves.toEqual(page);
});

it("@s6 accepts exactly twenty ordered facts with an opaque continuation and inclusive filters", async () => {
  const items = Array.from({ length: 20 }, (_, index) => {
    const id = `12345678-1234-1234-1234-${String(20 - index).padStart(12, "0")}`;
    return { ...startEntry, id, details: { ...session, id } };
  });
  const page = { items, nextCursor: "opaque-next-page" };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(
    readHistory(
      new URLSearchParams({
        category: "sessions",
        projectId: session.projectId.toUpperCase(),
        taskId: session.taskId,
        from: "2026-09-07",
        to: "2026-09-07",
      }),
    ),
  ).resolves.toEqual(page);
});

it("@s13 preserves six equal-instant facts in family and UUID order without ordering by revision", async () => {
  const stopped = {
    ...running,
    status: "paused",
    revision: "2",
    runningSince: null,
  };
  const pauseNow = { ...pause, occurredAt: session.startedAt, after: stopped };
  const resume = {
    ...pauseNow,
    id: session.projectId,
    action: "RESUME",
    before: stopped,
    after: { ...running, revision: "3" },
  };
  const task = {
    id: session.id,
    fromStatus: "pending",
    toStatus: "completed",
    occurredAt: session.startedAt,
  };
  const items = [
    {
      ...startEntry,
      id: pauseNow.id,
      type: "SESSION_CHANGED",
      details: pauseNow,
    },
    { ...startEntry, id: resume.id, type: "SESSION_CHANGED", details: resume },
    startEntry,
    { ...startEntry, type: "TASK_STATUS_CHANGED", details: task },
    {
      ...startEntry,
      id: cancelled.id,
      type: "BLOCK_CHANGED",
      details: cancelled,
    },
    { ...startEntry, type: "BLOCK_PLANNED", details: block },
  ];
  const page = { items, nextCursor: null };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
});

it("@s25 preserves a large exact closed net and historical notes and attribution", async () => {
  const original = {
    ...session,
    startedAt: "1000-01-01T10:00:00Z",
    plannedEndAt: "1000-01-01T10:25:00Z",
  };
  const before = {
    ...running,
    session: original,
    status: "paused",
    revision: "2",
    changedAt: "1600-01-01T10:00:00.123456Z",
    workedMicroseconds: "9007199254740992",
    runningSince: null,
  };
  const details = {
    ...pause,
    action: "CLOSE",
    occurredAt: "1600-01-01T10:00:00.123457Z",
    before,
    after: {
      ...before,
      status: "closed",
      revision: "3",
      changedAt: "1600-01-01T10:00:00.123457Z",
    },
    closure: {
      progressNote: "  <b>nota</b>\nsegunda línea  ",
      nextStep: "",
      workDate: "1600-01-02",
      closeZoneId: "Historical/Unavailable",
    },
  };
  const page = {
    items: [
      {
        ...startEntry,
        id: details.id,
        type: "SESSION_CHANGED",
        occurredAt: details.occurredAt,
        details,
      },
    ],
    nextCursor: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(
    readHistory(new URLSearchParams({ from: "1600-01-01", to: "1600-01-01" })),
  ).resolves.toEqual(page);
});

it("@s1 preserves an extension without increasing worked time", async () => {
  const details = {
    ...pause,
    action: "EXTEND",
    before: running,
    after: { ...running, revision: "2" },
    extension: {
      additionalMinutes: 5,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:30:00.123456Z",
    },
  };
  const page = {
    items: [
      {
        ...startEntry,
        id: details.id,
        type: "SESSION_CHANGED",
        occurredAt: details.occurredAt,
        details,
      },
    ],
    nextCursor: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(readHistory(new URLSearchParams())).resolves.toEqual(page);
});

it("@s1 preserves both historical intervals in a rescheduled reservation", async () => {
  const details = {
    ...cancelled,
    kind: "RESCHEDULED",
    after: {
      ...block,
      startAt: "2026-10-02T10:00:00Z",
      endAt: "2026-10-02T10:25:00Z",
    },
  };
  const page = {
    items: [{ ...startEntry, id: details.id, type: "BLOCK_CHANGED", details }],
    nextCursor: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
  await expect(
    readHistory(new URLSearchParams({ category: "planning" })),
  ).resolves.toEqual(page);
});

it("@s26 mutation18 rejects an extra entry field with otherwise valid start details", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, extra: "unexpected" }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects invalid task status details with coherent ID and occurrence", async () => {
  const details = {
    id: startEntry.id,
    fromStatus: "paused",
    toStatus: "completed",
    occurredAt: startEntry.occurredAt,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, type: "TASK_STATUS_CHANGED", details }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects invalid original reservation details with coherent context and creation", async () => {
  const details = { ...block, durationMinutes: 26 };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, type: "BLOCK_PLANNED", details }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects invalid block change details without relying on outer identity mismatch", async () => {
  const details = { ...cancelled, after: block };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [
          { ...startEntry, id: cancelled.id, type: "BLOCK_CHANGED", details },
        ],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects invalid session arithmetic with coherent outer ID time and context", async () => {
  const details = {
    ...pause,
    after: { ...pause.after, workedMicroseconds: "60000001" },
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [
          {
            ...startEntry,
            id: pause.id,
            occurredAt: pause.occurredAt,
            type: "SESSION_CHANGED",
            details,
          },
        ],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects valid task status details labelled as block change", async () => {
  const details = {
    id: startEntry.id,
    fromStatus: "pending",
    toStatus: "completed",
    occurredAt: startEntry.occurredAt,
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, type: "BLOCK_CHANGED", details }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects valid planning details labelled as session start", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [{ ...startEntry, type: "SESSION_STARTED", details: block }],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects valid block change details labelled as task status", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [
          {
            ...startEntry,
            id: cancelled.id,
            type: "TASK_STATUS_CHANGED",
            details: cancelled,
          },
        ],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});

it("@s26 mutation18 rejects valid session change details labelled as task status", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        items: [
          {
            ...startEntry,
            id: pause.id,
            occurredAt: pause.occurredAt,
            type: "TASK_STATUS_CHANGED",
            details: pause,
          },
        ],
        nextCursor: null,
      }),
    ),
  );
  await expect(readHistory(new URLSearchParams())).rejects.toThrow(
    "Historial inválido",
  );
});
