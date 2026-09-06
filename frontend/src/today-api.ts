import { apiRequest } from "./api-client";
import {
  exact,
  text,
  integer,
  isBlock,
  uuid,
  instant,
  type Block,
} from "./schedule-block-api";
export type TodayItem = {
  block: Block;
  projectName: string;
  taskTitle: string;
};
export type TodaySnapshot = {
  serverNow: string;
  date: string;
  zoneId: string;
  zoneSource: "AVAILABILITY" | "UNCONFIGURED" | "UNAVAILABLE";
  availabilityZoneId: string | null;
  dayStartAt: string;
  dayEndAt: string;
  budgetMinutes: number | null;
  plannedSeconds: number;
  remainingSeconds: number | null;
  excessSeconds: number | null;
  currentBlockId: string | null;
  nextBlockId: string | null;
  closingAt: string | null;
  items: TodayItem[];
};
export async function readToday(signal?: AbortSignal): Promise<TodaySnapshot> {
  const response = await apiRequest("/api/v1/today", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (!isToday(value)) throw new Error("Respuesta de Hoy inválida");
  return value;
}
function isToday(value: unknown): value is TodaySnapshot {
  if (!(
    exact(
      value,
      "serverNow date zoneId zoneSource availabilityZoneId dayStartAt dayEndAt budgetMinutes plannedSeconds remainingSeconds excessSeconds currentBlockId nextBlockId closingAt items",
    ) &&
    text(value.zoneId) &&
    typeof value.zoneSource === "string" &&
    ["AVAILABILITY", "UNCONFIGURED", "UNAVAILABLE"].includes(
      value.zoneSource,
    ) &&
    integer(value.plannedSeconds, 0) &&
    (value.budgetMinutes === null || integer(value.budgetMinutes, 0, 1440)) &&
    (value.remainingSeconds === null || integer(value.remainingSeconds, 0)) &&
    (value.excessSeconds === null || integer(value.excessSeconds, 0)) &&
    Array.isArray(value.items) &&
    value.items.every(
      (item) =>
        exact(item, "block projectName taskTitle") &&
        text(item.projectName) &&
        text(item.taskTitle) &&
        exact(
          item.block,
          "id projectId taskId objective startAt endAt zoneId durationMinutes createdAt",
        ) &&
        uuid(item.block.projectId) &&
        uuid(item.block.taskId) &&
        isBlock(item.block, item.block.projectId, item.block.taskId),
    )
  ))
    return false;
  if (
    !instant(value.serverNow) ||
    !instant(value.dayStartAt) ||
    !instant(value.dayEndAt) ||
    typeof value.date !== "string" ||
    !instant(value.date + "T00:00:00Z")
  )
    return false;
  const now = Date.parse(value.serverNow),
    start = Date.parse(value.dayStartAt),
    end = Date.parse(value.dayEndAt);
  if (now < start || now >= end) return false;
  if (value.zoneSource === "AVAILABILITY") {
    if (
      value.availabilityZoneId !== value.zoneId ||
      !integer(value.budgetMinutes, 0, 1440) ||
      value.remainingSeconds !==
        Math.max(
          0,
          value.budgetMinutes * 60 - (value.plannedSeconds as number),
        ) ||
      value.excessSeconds !==
        Math.max(0, (value.plannedSeconds as number) - value.budgetMinutes * 60)
    )
      return false;
  } else if (
    value.zoneId !== "UTC" ||
    value.budgetMinutes !== null ||
    value.remainingSeconds !== null ||
    value.excessSeconds !== null ||
    (value.zoneSource === "UNCONFIGURED"
      ? value.availabilityZoneId !== null
      : !text(value.availabilityZoneId))
  )
    return false;
  const items = value.items as TodayItem[];
  if (
    items.some(({ block }, index) => {
      if (index === 0) return false;
      const previous = items[index - 1].block;
      return (
        Date.parse(previous.startAt) > Date.parse(block.startAt) ||
        (Date.parse(previous.startAt) === Date.parse(block.startAt) &&
          previous.id.toLowerCase() >= block.id.toLowerCase())
      );
    })
  )
    return false;
  const ids = new Set<string>();
  let planned = 0;
  for (const { block } of items) {
    const id = block.id.toLowerCase();
    if (ids.has(id)) return false;
    ids.add(id);
    const seconds =
      (Math.min(end, Date.parse(block.endAt)) -
        Math.max(start, Date.parse(block.startAt))) /
      1000;
    if (seconds <= 0) return false;
    planned += seconds;
  }
  const current = items.find(
    ({ block }) =>
      Date.parse(block.startAt) <= now && now < Date.parse(block.endAt),
  );
  const next = items.find(({ block }) => Date.parse(block.startAt) > now);
  const closing = items.reduce<string | null>(
    (latest, { block }) =>
      latest === null || Date.parse(block.endAt) > Date.parse(latest)
        ? block.endAt
        : latest,
    null,
  );
  return (
    value.plannedSeconds === planned &&
    value.currentBlockId === (current?.block.id ?? null) &&
    value.nextBlockId === (next?.block.id ?? null) &&
    value.closingAt === closing
  );
}
