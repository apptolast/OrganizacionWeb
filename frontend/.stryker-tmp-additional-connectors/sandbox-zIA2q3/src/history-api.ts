// @ts-nocheck
import { apiRequest } from "./api-client";
import {
  exact,
  isBlock,
  text,
  uuid,
  sameId,
  type Block,
} from "./schedule-block-api";
import {
  isSessionStart,
  microseconds,
  type SessionStart,
} from "./work-session-api";
import { isHistoryEntry, type TaskHistoryEntry } from "./task-status-api";
import { isChange as isBlockChange, type BlockChange } from "./reschedule-api";
import {
  isChange as isSessionChange,
  type WorkSessionChange,
} from "./work-session-state-api";

type HistoryDetails = {
  BLOCK_PLANNED: Block;
  BLOCK_CHANGED: BlockChange;
  TASK_STATUS_CHANGED: TaskHistoryEntry;
  SESSION_STARTED: SessionStart;
  SESSION_CHANGED: WorkSessionChange;
};
export type HistoryEntry = {
  [Kind in keyof HistoryDetails]: {
    id: string;
    type: Kind;
    occurredAt: string;
    projectId: string;
    projectName: string;
    taskId: string;
    taskTitle: string;
    details: HistoryDetails[Kind];
  };
}[keyof HistoryDetails];
export type HistoryPage = { items: HistoryEntry[]; nextCursor: string | null };

export async function readHistory(
  query: URLSearchParams,
  signal?: AbortSignal,
) {
  query = new URLSearchParams(query);
  const search = query.toString();
  const response = await apiRequest(
    `/api/v1/history${search ? `?${search}` : ""}`,
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !exact(value, "items nextCursor") ||
    !(value.nextCursor === null || text(value.nextCursor)) ||
    !Array.isArray(value.items) ||
    value.items.length > 20 ||
    !value.items.every(
      (item) =>
        exact(
          item,
          "id type occurredAt projectId projectName taskId taskTitle details",
        ) &&
        uuid(item.id) &&
        item.id === item.id.toLowerCase() &&
        microseconds(item.occurredAt) !== null &&
        uuid(item.projectId) &&
        item.projectId === item.projectId.toLowerCase() &&
        uuid(item.taskId) &&
        item.taskId === item.taskId.toLowerCase() &&
        text(item.projectName) &&
        text(item.taskTitle) &&
        ((item.type === "SESSION_STARTED" &&
          isSessionStart(item.details) &&
          item.id === item.details.id &&
          item.projectId === item.details.projectId &&
          item.taskId === item.details.taskId &&
          microseconds(item.occurredAt) ===
            microseconds(item.details.startedAt)) ||
          (item.type === "TASK_STATUS_CHANGED" &&
            isHistoryEntry(item.details) &&
            item.id === item.details.id &&
            microseconds(item.occurredAt) ===
              microseconds(item.details.occurredAt)) ||
          (item.type === "BLOCK_PLANNED" &&
            isBlock(item.details, item.projectId, item.taskId) &&
            item.id === item.details.id &&
            microseconds(item.occurredAt) ===
              microseconds(item.details.createdAt)) ||
          (item.type === "BLOCK_CHANGED" &&
            isBlockChange(item.details, item.projectId, item.taskId) &&
            item.id === item.details.id &&
            microseconds(item.occurredAt) ===
              microseconds(item.details.occurredAt)) ||
          (item.type === "SESSION_CHANGED" &&
            isSessionChange(item.details) &&
            item.id === item.details.id &&
            microseconds(item.occurredAt) ===
              microseconds(item.details.occurredAt) &&
            item.taskId === item.details.before.session.taskId &&
            item.projectId === item.details.before.session.projectId)),
    )
  )
    throw new Error("Historial inválido");
  const page = value as HistoryPage;
  if (
    !page.items.every(
      (item, index) =>
        index === 0 || compare(page.items[index - 1], item) >= 0n,
    )
  )
    throw new Error("Historial inválido");
  if (
    new Set(page.items.map((item) => `${item.type}:${item.id}`)).size !==
    page.items.length
  )
    throw new Error("Historial inválido");
  const projectId = query.get("projectId");
  if (
    projectId !== null &&
    !page.items.every((item) => sameId(item.projectId, projectId))
  )
    throw new Error("Historial inválido");
  const taskId = query.get("taskId");
  if (
    taskId !== null &&
    !page.items.every((item) => sameId(item.taskId, taskId))
  )
    throw new Error("Historial inválido");
  const category = query.get("category");
  if (
    category !== null &&
    !page.items.every((item) => categories[item.type] === category)
  )
    throw new Error("Historial inválido");
  const from = query.get("from");
  if (
    from !== null &&
    !page.items.every((item) => item.occurredAt.slice(0, 10) >= from)
  )
    throw new Error("Historial inválido");
  const to = query.get("to");
  if (
    to !== null &&
    !page.items.every((item) => item.occurredAt.slice(0, 10) <= to)
  )
    throw new Error("Historial inválido");
  return page;
}

const ranks = {
  BLOCK_PLANNED: 0,
  BLOCK_CHANGED: 1,
  TASK_STATUS_CHANGED: 2,
  SESSION_STARTED: 3,
  SESSION_CHANGED: 4,
};
function compare(left: HistoryEntry, right: HistoryEntry) {
  return (
    microseconds(left.occurredAt)! - microseconds(right.occurredAt)! ||
    BigInt(ranks[left.type] - ranks[right.type]) ||
    (left.id > right.id ? 1n : left.id < right.id ? -1n : 0n)
  );
}
const categories = {
  BLOCK_PLANNED: "planning",
  BLOCK_CHANGED: "planning",
  TASK_STATUS_CHANGED: "task-status",
  SESSION_STARTED: "sessions",
  SESSION_CHANGED: "sessions",
};
