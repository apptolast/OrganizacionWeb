import { afterEach, expect, it, vi } from "vitest";
import { readToday } from "./today-api";

import { emptyToday, agendaToday } from "./today-fixture";
afterEach(() => vi.unstubAllGlobals());
it.each([
  null,
  [],
  { ...emptyToday(), extra: true },
  ...Object.keys(emptyToday()).map((key) =>
    Object.fromEntries(
      Object.entries(emptyToday()).filter(([name]) => name !== key),
    ),
  ),
  ...[
    "plannedSeconds",
    "budgetMinutes",
    "remainingSeconds",
    "excessSeconds",
  ].map((key) => ({ ...emptyToday(), [key]: "0" })),
  { ...emptyToday(), items: {} },
  { ...emptyToday(), zoneId: " " },
  { ...emptyToday(), zoneSource: "DEVICE" },
])("@s16 rejects a malformed closed snapshot %#", async (value) => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it.each([401, 503, 201])(
  "@s15 @s22 rejects HTTP %s without confirming its body",
  async (status) => {
    const response = Response.json(emptyToday(), { status });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
    await expect(readToday()).rejects.toBe(response);
  },
);
it("@s1 @s21 reads the complete empty snapshot through the private GET", async () => {
  const value = emptyToday();
  const fetch = vi.fn().mockResolvedValue(Response.json(value));
  vi.stubGlobal("fetch", fetch);
  const signal = new AbortController().signal;
  await expect(readToday(signal)).resolves.toEqual(value);
  expect(fetch).toHaveBeenCalledExactlyOnceWith("/api/v1/today", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
});
it.each([
  { extra: 1 },
  { projectName: 1 },
  { taskTitle: null },
  { block: {} },
  { block: { ...agendaToday().items[0].block, extra: true } },
  { block: { ...agendaToday().items[0].block, taskId: [] } },
  { block: { ...agendaToday().items[0].block, durationMinutes: 59 } },
])("@s16 rejects malformed agenda items %#", async (change) => {
  const value = agendaToday();
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...value, items: [{ ...value.items[0], ...change }] }),
      ),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it.each([
  { serverNow: "2030-01-08T00:00:00Z" },
  { serverNow: "2030-01-06T23:59:59Z" },
  { serverNow: 1 },
  { dayStartAt: "bad" },
  { dayEndAt: null },
  { date: "2030-02-30" },
  { date: "0000-01-07" },
  { date: 20300107 },
  { plannedSeconds: 1 },
  { remainingSeconds: 7199 },
  { excessSeconds: 1 },
  { budgetMinutes: null },
  { budgetMinutes: -1 },
  { budgetMinutes: 1441 },
  { zoneSource: "UNCONFIGURED" },
  { availabilityZoneId: null },
  { currentBlockId: "00000000-0000-0000-0000-000000000001" },
  { nextBlockId: "00000000-0000-0000-0000-000000000001" },
  { closingAt: "2030-01-07T13:00:00Z" },
])("@s17 rejects incoherent empty snapshot %#", async (change) => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...emptyToday(), ...change })),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it.each([
  { items: [agendaToday().items[0], agendaToday().items[0]] },
  {
    items: [
      {
        ...agendaToday().items[0],
        block: {
          ...agendaToday().items[0].block,
          startAt: "2030-01-08T12:00:00Z",
          endAt: "2030-01-08T13:00:00Z",
        },
      },
    ],
  },
  { currentBlockId: null },
  { nextBlockId: agendaToday().currentBlockId },
  { closingAt: null },
  { plannedSeconds: 3599, remainingSeconds: 3601 },
  { serverNow: "2030-01-07T13:00:00Z" },
])("@s16 @s17 rejects incoherent agenda %#", async (change) => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...agendaToday(), ...change })),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it("@s16 rejects out-of-order otherwise consistent reservations", async () => {
  const value = agendaToday();
  const later = {
    ...value.items[0],
    block: {
      ...value.items[0].block,
      id: "00000000-0000-0000-0000-000000000004",
      startAt: "2030-01-07T14:00:00Z",
      endAt: "2030-01-07T15:00:00Z",
    },
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...value,
        items: [later, ...value.items],
        plannedSeconds: 7200,
        remainingSeconds: 0,
        nextBlockId: later.block.id,
        closingAt: later.block.endAt,
      }),
    ),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it.each([
  agendaToday(),
  {
    ...agendaToday(),
    zoneId: "Server/Only",
    availabilityZoneId: "Server/Only",
  },
  {
    ...agendaToday(),
    zoneSource: "UNCONFIGURED",
    availabilityZoneId: null,
    budgetMinutes: null,
    remainingSeconds: null,
    excessSeconds: null,
  },
  {
    ...agendaToday(),
    zoneSource: "UNAVAILABLE",
    availabilityZoneId: "Historical/Missing",
    budgetMinutes: null,
    remainingSeconds: null,
    excessSeconds: null,
  },
  {
    ...agendaToday(),
    serverNow: "2030-01-07T11:59:59Z",
    currentBlockId: null,
    nextBlockId: agendaToday().currentBlockId,
  },
  { ...agendaToday(), serverNow: "2030-01-07T13:00:00Z", currentBlockId: null },
])(
  "@s3 @s7 @s20 accepts coherent snapshots without client TZDB or createdAt-clock comparison %#",
  async (value) => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
    await expect(readToday()).resolves.toEqual(value);
  },
);
it("@s16 rejects a coercible non-string zone source", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...emptyToday(),
        zoneSource: ["UNAVAILABLE"],
        availabilityZoneId: "Old/Zone",
        budgetMinutes: null,
        remainingSeconds: null,
        excessSeconds: null,
      }),
    ),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it("@s4 @s9 accepts all twenty-one intersections, including both midnight crossings", async () => {
  const first = agendaToday().items[0];
  const items = Array.from({ length: 21 }, (_, index) => {
    const hour =
      index === 0
        ? "2030-01-06T23:30:00Z"
        : index === 20
          ? "2030-01-07T23:30:00Z"
          : `2030-01-07T${String(index).padStart(2, "0")}:00:00Z`;
    return {
      ...first,
      block: {
        ...first.block,
        id: `00000000-0000-0000-0000-${String(index + 1).padStart(12, "0")}`,
        startAt: hour,
        endAt: new Date(Date.parse(hour) + 3600000).toISOString(),
      },
    };
  });
  const value = {
    ...emptyToday(),
    items,
    plannedSeconds: 72000,
    remainingSeconds: 0,
    excessSeconds: 64800,
    currentBlockId: items[12].block.id,
    nextBlockId: items[13].block.id,
    closingAt: items[20].block.endAt,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readToday()).resolves.toEqual(value);
});
it("@s16 @s17 rejects an ISO date wrapped in a coercible JSON array", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...emptyToday(),
        date: ["2030-01-07"],
      }),
    ),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it.each(
  ["UNCONFIGURED", "UNAVAILABLE"].flatMap((source) => [
    [source, "zoneId", "Europe/Madrid"],
    [source, "budgetMinutes", 0],
    [source, "remainingSeconds", 0],
    [source, "excessSeconds", 0],
    [source, "availabilityZoneId", source === "UNCONFIGURED" ? "UTC" : null],
  ]),
)(
  "@s17 rejects the isolated %s fallback defect in %s",
  async (source, field, change) => {
    const value = {
      ...emptyToday(),
      zoneSource: source,
      availabilityZoneId: source === "UNCONFIGURED" ? null : "Legacy/Retired",
      budgetMinutes: null,
      remainingSeconds: null,
      excessSeconds: null,
      [field as string]: change,
    };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
    await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
  },
);
it.each([false, true])(
  "@s16 rejects a non-adjacent repeated UUID with independent intervals and mixedCase=%s",
  async (mixedCase) => {
    const first = agendaToday().items[0];
    const repeated = "00000000-0000-0000-0000-00000000000a";
    const items = [
      repeated,
      "00000000-0000-0000-0000-00000000000b",
      mixedCase ? repeated.toUpperCase() : repeated,
    ].map((id, index) => ({
      ...first,
      block: {
        ...first.block,
        id,
        startAt: `2030-01-07T${String(9 + index).padStart(2, "0")}:00:00Z`,
        endAt: `2030-01-07T${String(10 + index).padStart(2, "0")}:00:00Z`,
      },
    }));
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          ...emptyToday(),
          items,
          plannedSeconds: 10800,
          remainingSeconds: 0,
          excessSeconds: 3600,
          closingAt: "2030-01-07T12:00:00Z",
        }),
      ),
    );
    await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
  },
);
it("@s9 @s16 accepts chronological reservations even when UUID lexical order is reversed", async () => {
  const first = agendaToday().items[0];
  const items = [
    "00000000-0000-0000-0000-0000000000FF",
    "00000000-0000-0000-0000-00000000000a",
  ].map((id, index) => ({
    ...first,
    block: {
      ...first.block,
      id,
      startAt: `2030-01-07T${String(9 + index * 2).padStart(2, "0")}:00:00Z`,
      endAt: `2030-01-07T${String(10 + index * 2).padStart(2, "0")}:00:00Z`,
    },
  }));
  const value = {
    ...emptyToday(),
    items,
    plannedSeconds: 7200,
    remainingSeconds: 0,
    closingAt: "2030-01-07T12:00:00Z",
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readToday()).resolves.toEqual(value);
});
it("@s16 rejects manipulated tied starts with descending canonical UUIDs", async () => {
  const first = agendaToday().items[0];
  // Deliberately malformed: tied starts also overlap; no valid server agenda is claimed.
  const items = [
    "00000000-0000-0000-0000-0000000000bb",
    "00000000-0000-0000-0000-0000000000Aa",
  ].map((id) => ({
    ...first,
    block: {
      ...first.block,
      id,
      startAt: "2030-01-07T09:00:00Z",
      endAt: "2030-01-07T10:00:00Z",
    },
  }));
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...emptyToday(),
        items,
        plannedSeconds: 7200,
        remainingSeconds: 0,
        closingAt: "2030-01-07T10:00:00Z",
      }),
    ),
  );
  await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
});
it.each([
  ["2030-01-06T23:00:00Z", "2030-01-07T00:00:00Z", false],
  ["2030-01-08T00:00:00Z", "2030-01-08T01:00:00Z", true],
])(
  "@s17 rejects an item touching the day only at %s with otherwise coherent summaries",
  async (startAt, endAt, future) => {
    const item = agendaToday().items[0];
    const value = {
      ...emptyToday(),
      items: [{ ...item, block: { ...item.block, startAt, endAt } }],
      nextBlockId: future ? item.block.id : null,
      closingAt: endAt,
    };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
    await expect(readToday()).rejects.toThrow("Respuesta de Hoy inválida");
  },
);
