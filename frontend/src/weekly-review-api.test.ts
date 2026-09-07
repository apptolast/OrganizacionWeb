import { afterEach, expect, it, vi } from "vitest";
import { readWeeklyReview } from "./weekly-review-api";
import { observeAccess } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
});

const week = {
  weekStart: "2026-09-07",
  weekEnd: "2026-09-13",
  zoneId: "UTC",
  zoneSource: "UNCONFIGURED",
  availabilityZoneId: null,
  serverNow: "2026-09-09T12:00:00.123456Z",
  startAt: "2026-09-07T00:00:00Z",
  endAt: "2026-09-14T00:00:00Z",
  days: Array.from({ length: 7 }, (_, index) => {
    const date = `2026-09-${String(7 + index).padStart(2, "0")}`;
    return {
      date,
      startAt: `${date}T00:00:00Z`,
      endAt: `2026-09-${String(8 + index).padStart(2, "0")}T00:00:00Z`,
      plannedMicroseconds: "0",
      workedMicroseconds: "0",
      capacityMicroseconds: null,
    };
  }),
  totals: {
    plannedMicroseconds: "0",
    workedMicroseconds: "0",
    capacityMicroseconds: null,
  },
  unquantifiedSessionCount: "0",
};

it("@s1 reads the seven-day empty week with private request options and cancellation", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(week));
  vi.stubGlobal("fetch", fetcher);
  const controller = new AbortController();
  await expect(
    readWeeklyReview(new URLSearchParams(), controller.signal),
  ).resolves.toEqual(week);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith("/api/v1/weekly-review", {
    credentials: "same-origin",
    cache: "no-store",
    signal: controller.signal,
  });
});

it("@s25 rejects an extra envelope field instead of accepting a weekly snapshot", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...week, owner: "private" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s23 preserves an unavailable HTTP response for manual recovery", async () => {
  const response = new Response(null, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toBe(response);
});

it("@s25 rejects a day missing workedMicroseconds", async () => {
  const value = structuredClone(week);
  const { workedMicroseconds: omitted, ...day } = value.days[0];
  expect(omitted).toBe("0");
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...value, days: [day, ...value.days.slice(1)] }),
      ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a snapshot with only six days", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...week, days: week.days.slice(0, 6) }),
      ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects totals with an additional field", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...week, totals: { ...week.totals, score: 0 } }),
      ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a negative daily duration even when its total matches", async () => {
  const value = structuredClone(week);
  value.days[0].workedMicroseconds = "-1";
  value.totals.workedMicroseconds = "-1";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a duration above long before any integer conversion", async () => {
  const value = structuredClone(week);
  value.days[0].workedMicroseconds = "9223372036854775808";
  value.totals.workedMicroseconds = value.days[0].workedMicroseconds;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects noncanonical planned time without treating it as zero", async () => {
  const value = structuredClone(week);
  value.days[0].plannedMicroseconds = "00";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a capacity encoded as a JSON number", async () => {
  const value = {
    ...week,
    days: week.days.map((day) => ({ ...day, capacityMicroseconds: 0 })),
    totals: { ...week.totals, capacityMicroseconds: 0 },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a numeric total even when the daily amount is zero", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...week,
        totals: { ...week.totals, workedMicroseconds: 0 },
      }),
    ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects an oversized unknown-session counter", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...week, unquantifiedSessionCount: "9".repeat(200) }),
      ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a total differing by one microsecond from its days", async () => {
  const value = structuredClone(week);
  value.days[0].workedMicroseconds = "1000001";
  value.totals.workedMicroseconds = "1000000";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects known capacity totals when one day is unknown", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...week,
        totals: { ...week.totals, capacityMicroseconds: "0" },
      }),
    ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects known capacity whose total differs from seven daily budgets", async () => {
  const value = {
    ...week,
    days: week.days.map((day) => ({ ...day, capacityMicroseconds: "1" })),
    totals: { ...week.totals, capacityMicroseconds: "6" },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s26 preserves exact long amounts above Number safe integer and known zero budgets", async () => {
  const value = {
    ...week,
    zoneSource: "AVAILABILITY",
    availabilityZoneId: "UTC",
    days: week.days.map((day) => ({ ...day, capacityMicroseconds: "0" })),
    totals: { ...week.totals, capacityMicroseconds: "0" },
  };
  value.days[0].workedMicroseconds = "9223372036854775806";
  value.days[1].workedMicroseconds = "1";
  value.totals.workedMicroseconds = "9223372036854775807";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).resolves.toEqual(value);
});

it("@s25 rejects an instant with submicrosecond precision", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...week, serverNow: "2026-09-09T12:00:00.1234567Z" }),
      ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a week whose first civil date is not Monday", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, weekStart: "2026-09-08" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a repeated civil date among the seven days", async () => {
  const value = structuredClone(week);
  value.days[1].date = value.days[0].date;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a one-microsecond discontinuity between daily boundaries", async () => {
  const value = structuredClone(week);
  value.days[1].startAt = "2026-09-08T00:00:00.000001Z";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects reversed daily bounds even if adjacent boundaries match", async () => {
  const value = structuredClone(week);
  value.days[0].endAt = "2026-09-06T23:59:59.999999Z";
  value.days[1].startAt = value.days[0].endAt;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a default week whose exclusive end equals serverNow", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, serverNow: week.endAt })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a valid week different from the explicitly requested civil week", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(week)));
  await expect(
    readWeeklyReview(new URLSearchParams("date=2026-09-16")),
  ).rejects.toThrow("Revisión semanal inválida");
});

it("@s26 accepts a historical selected week while preserving request parameters", async () => {
  const value = { ...week, serverNow: "2026-10-01T12:00:00.123457Z" };
  const fetcher = vi.fn().mockResolvedValue(Response.json(value));
  vi.stubGlobal("fetch", fetcher);
  await expect(
    readWeeklyReview(new URLSearchParams("date=2026-09-09")),
  ).resolves.toEqual(value);
  expect(fetcher).toHaveBeenCalledWith(
    "/api/v1/weekly-review?date=2026-09-09",
    expect.anything(),
  );
});

it("@s25 rejects a snapshot in a different explicitly requested zone", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, zoneSource: "EXPLICIT" })),
  );
  await expect(
    readWeeklyReview(new URLSearchParams("zoneId=Europe%2FMadrid")),
  ).rejects.toThrow("Revisión semanal inválida");
});

it("@s25 rejects an unknown zone-source discriminator", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, zoneSource: "BROWSER" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects an empty effective zone instead of choosing the browser zone", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...week, zoneId: "" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a non-string stored zone while preserving unavailable zones as text", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...week,
        zoneSource: "UNAVAILABLE",
        availabilityZoneId: 12,
      }),
    ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a fallback source for an explicitly selected zone", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(week)));
  await expect(
    readWeeklyReview(new URLSearchParams("zoneId=UTC")),
  ).rejects.toThrow("Revisión semanal inválida");
});

it("@s31 discards an aborted late HTTP401 before consuming or reporting it", async () => {
  const observer = vi.fn();
  observeAccess(observer);
  let deliver!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    ),
  );
  const controller = new AbortController();
  const pending = readWeeklyReview(new URLSearchParams(), controller.signal);
  controller.abort();
  const response = new Response(null, { status: 401 });
  const read = vi.spyOn(response, "json");
  deliver(response);
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
  expect(observer).not.toHaveBeenCalled();
  expect(read).not.toHaveBeenCalled();
});

it("@s31 discards JSON delivered after its request was replaced", async () => {
  let deliver!: (value: unknown) => void;
  const response = Response.json(week);
  const json = vi.spyOn(response, "json").mockReturnValue(
    new Promise((resolve) => {
      deliver = resolve;
    }),
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  const controller = new AbortController();
  const pending = readWeeklyReview(new URLSearchParams(), controller.signal);
  await vi.waitFor(() => expect(json).toHaveBeenCalledOnce());
  controller.abort();
  deliver(week);
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
});

it("@s31 validates against the selection sent even when the caller edits its query later", async () => {
  let deliver!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockReturnValue(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    ),
  );
  const query = new URLSearchParams("date=2026-09-09");
  const pending = readWeeklyReview(query);
  query.set("date", "2026-09-16");
  deliver(Response.json(week));
  await expect(pending).resolves.toEqual(week);
});

it("@s25 rejects a non-UTC zone advertised as the unconfigured fallback", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, zoneId: "Europe/Madrid" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects availability provenance without the matching stored zone", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...week,
        zoneSource: "AVAILABILITY",
        availabilityZoneId: "Europe/Madrid",
      }),
    ),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects an unavailable saved zone represented by null", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, zoneSource: "UNAVAILABLE" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s25 rejects a stored zone advertised as unconfigured", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(Response.json({ ...week, availabilityZoneId: "UTC" })),
  );
  await expect(readWeeklyReview(new URLSearchParams())).rejects.toThrow(
    "Revisión semanal inválida",
  );
});

it("@s7 accepts the server's skipped Apia day without reconstructing timezone boundaries", async () => {
  const dates = [
    "2011-12-26",
    "2011-12-27",
    "2011-12-28",
    "2011-12-29",
    "2011-12-30",
    "2011-12-31",
    "2012-01-01",
  ];
  const bounds = [
    "2011-12-26",
    "2011-12-27",
    "2011-12-28",
    "2011-12-29",
    "2011-12-30",
    "2011-12-30",
    "2011-12-31",
    "2012-01-01",
  ].map((date) => `${date}T10:00:00Z`);
  const value = {
    ...week,
    weekStart: dates[0],
    weekEnd: dates[6],
    zoneId: "Pacific/Apia",
    zoneSource: "EXPLICIT",
    startAt: bounds[0],
    endAt: bounds[7],
    days: dates.map((date, index) => ({
      ...week.days[0],
      date,
      startAt: bounds[index],
      endAt: bounds[index + 1],
    })),
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(
    readWeeklyReview(
      new URLSearchParams("date=2011-12-30&zoneId=Pacific%2FApia"),
    ),
  ).resolves.toEqual(value);
});

it("@s26 preserves microseconds in a 1600 snapshot before the safe Number epoch range", async () => {
  const value = structuredClone(week);
  value.weekStart = "1600-01-03";
  value.weekEnd = "1600-01-09";
  value.startAt = "1600-01-03T00:00:00Z";
  value.endAt = "1600-01-10T00:00:00Z";
  value.serverNow = "1600-01-05T12:00:00.123457Z";
  value.days = value.days.map((day, index) => ({
    ...day,
    date: `1600-01-0${3 + index}`,
    startAt: `1600-01-0${3 + index}T00:00:00Z`,
    endAt: `1600-01-${String(4 + index).padStart(2, "0")}T00:00:00Z`,
  }));
  value.days[0].workedMicroseconds = "1000001";
  value.totals.workedMicroseconds = "1000001";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  await expect(readWeeklyReview(new URLSearchParams())).resolves.toEqual(value);
});
