import { apiRequest } from "./api-client";
import { microseconds } from "./work-session-api";
import { exact, instant, text } from "./schedule-block-api";

export type WeeklyAmounts = {
  plannedMicroseconds: string;
  workedMicroseconds: string;
  capacityMicroseconds: string | null;
};
export type WeeklyDay = WeeklyAmounts & {
  date: string;
  startAt: string;
  endAt: string;
};
export type WeeklyReview = {
  weekStart: string;
  weekEnd: string;
  zoneId: string;
  zoneSource: "EXPLICIT" | "AVAILABILITY" | "UNCONFIGURED" | "UNAVAILABLE";
  availabilityZoneId: string | null;
  serverNow: string;
  startAt: string;
  endAt: string;
  days: WeeklyDay[];
  totals: WeeklyAmounts;
  unquantifiedSessionCount: string;
};

export async function readWeeklyReview(
  query: URLSearchParams,
  signal?: AbortSignal,
): Promise<WeeklyReview> {
  query = new URLSearchParams(query);
  const search = query.toString();
  const response = await apiRequest(
    `/api/v1/weekly-review${search ? `?${search}` : ""}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  signal?.throwIfAborted();
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  signal?.throwIfAborted();
  if (
    !exact(
      value,
      "weekStart weekEnd zoneId zoneSource availabilityZoneId serverNow startAt endAt days totals unquantifiedSessionCount",
    ) ||
    !exact(
      value.totals,
      "plannedMicroseconds workedMicroseconds capacityMicroseconds",
    ) ||
    !["EXPLICIT", "AVAILABILITY", "UNCONFIGURED", "UNAVAILABLE"].includes(
      value.zoneSource as string,
    ) ||
    !text(value.zoneId) ||
    !(value.availabilityZoneId === null || text(value.availabilityZoneId)) ||
    !instant(value.serverNow) ||
    !decimal(value.unquantifiedSessionCount) ||
    !amounts(value.totals) ||
    !Array.isArray(value.days) ||
    value.days.length !== 7 ||
    !value.days.every(
      (day) =>
        exact(
          day,
          "date startAt endAt plannedMicroseconds workedMicroseconds capacityMicroseconds",
        ) && amounts(day),
    )
  )
    throw new Error("Revisión semanal inválida");
  if (
    (value.zoneSource === "UNCONFIGURED" ||
      value.zoneSource === "UNAVAILABLE") &&
    value.zoneId !== "UTC"
  )
    throw new Error("Revisión semanal inválida");
  if (
    value.zoneSource === "AVAILABILITY" &&
    value.zoneId !== value.availabilityZoneId
  )
    throw new Error("Revisión semanal inválida");
  if (value.zoneSource === "UNAVAILABLE" && value.availabilityZoneId === null)
    throw new Error("Revisión semanal inválida");
  if (value.zoneSource === "UNCONFIGURED" && value.availabilityZoneId !== null)
    throw new Error("Revisión semanal inválida");
  const days = value.days as WeeklyDay[];
  const totals = value.totals as WeeklyAmounts;
  if (query.has("zoneId") !== (value.zoneSource === "EXPLICIT"))
    throw new Error("Revisión semanal inválida");
  if (query.has("zoneId") && value.zoneId !== query.get("zoneId"))
    throw new Error("Revisión semanal inválida");
  const monday = civilDay(value.weekStart);
  if (monday === null || (((monday + 3n) % 7n) + 7n) % 7n !== 0n)
    throw new Error("Revisión semanal inválida");
  if (
    civilDay(value.weekEnd) !== monday + 6n ||
    !days.every((day, index) => civilDay(day.date) === monday + BigInt(index))
  )
    throw new Error("Revisión semanal inválida");
  const start = microseconds(value.startAt);
  const end = microseconds(value.endAt);
  if (
    start === null ||
    end === null ||
    !days.every((day, index) => {
      const left = microseconds(day.startAt);
      const right = microseconds(day.endAt);
      return (
        left !== null &&
        right !== null &&
        right >= left &&
        left === (index === 0 ? start : microseconds(days[index - 1].endAt)) &&
        (index !== 6 || right === end)
      );
    })
  )
    throw new Error("Revisión semanal inválida");
  if (query.has("date")) {
    const requestedDay = civilDay(query.get("date"));
    if (
      requestedDay === null ||
      requestedDay < monday ||
      requestedDay > monday + 6n
    )
      throw new Error("Revisión semanal inválida");
  }
  const now = microseconds(value.serverNow)!;
  if (!query.has("date") && (now < start || now >= end))
    throw new Error("Revisión semanal inválida");
  for (const field of ["plannedMicroseconds", "workedMicroseconds"] as const) {
    if (
      days.reduce((sum, day) => sum + BigInt(day[field]), 0n) !==
      BigInt(totals[field] as string)
    )
      throw new Error("Revisión semanal inválida");
  }
  if (
    !days.every(
      (day) =>
        (day.capacityMicroseconds === null) ===
        (totals.capacityMicroseconds === null),
    )
  )
    throw new Error("Revisión semanal inválida");
  if (
    totals.capacityMicroseconds !== null &&
    days.reduce((sum, day) => sum + BigInt(day.capacityMicroseconds!), 0n) !==
      BigInt(totals.capacityMicroseconds as string)
  )
    throw new Error("Revisión semanal inválida");
  return value as WeeklyReview;
}

function decimal(value: unknown): value is string {
  return (
    typeof value === "string" &&
    value.length <= 19 &&
    /^(0|[1-9]\d*)$/.test(value) &&
    (value.length < 19 || value <= "9223372036854775807")
  );
}

function amounts(value: Record<string, unknown>) {
  return (
    decimal(value.plannedMicroseconds) &&
    decimal(value.workedMicroseconds) &&
    (value.capacityMicroseconds === null || decimal(value.capacityMicroseconds))
  );
}
function civilDay(value: unknown): bigint | null {
  if (typeof value !== "string" || !/^(?!0000)\d{4}-\d{2}-\d{2}$/.test(value))
    return null;
  const at = microseconds(`${value}T00:00:00Z`);
  return at === null ? null : at / 86_400_000_000n;
}
