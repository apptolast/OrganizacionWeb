import { apiRequest } from "./api-client";
export type TaskStatus = "pending" | "completed";
export type TaskStatusSnapshot = {
  status: TaskStatus;
  completedAt: string | null;
  updatedAt: string;
  etag: string;
};
export async function readTaskStatus(
  projectId: string,
  id: string,
  signal?: AbortSignal,
): Promise<TaskStatusSnapshot> {
  const response = await apiRequest(
    `/api/v1/projects/${projectId}/tasks/${id}/status`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  return statusSnapshot(response, id);
}
async function statusSnapshot(
  response: Response,
  id: string,
): Promise<TaskStatusSnapshot> {
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  const etag = response.headers.get("ETag");
  if (
    !isTaskEtag(etag, id) ||
    !isRecord(data) ||
    Object.keys(data).length !== 3 ||
    !isInstant(data.updatedAt) ||
    !(
      (data.status === "pending" && data.completedAt === null) ||
      (data.status === "completed" && data.completedAt === data.updatedAt)
    )
  )
    throw new Error("Respuesta de estado de tarea inválida");
  return {
    status: data.status,
    completedAt: data.completedAt,
    updatedAt: data.updatedAt,
    etag,
  };
}
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
function isInstant(value: unknown): value is string {
  return (
    typeof value === "string" &&
    /^(?:\d{4}|[+-]\d{6})-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$/.test(
      value,
    ) &&
    Number.isFinite(Date.parse(value)) &&
    new Date(value).toISOString().split(".")[0] ===
      value.replace(/(?:\.\d+)?Z$/, "")
  );
}
function isTaskEtag(value: string | null, id: string): value is string {
  const match = value?.match(
    /^"task:([\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}):(0|[1-9]\d*)"$/,
  );
  return Boolean(
    match &&
    match[1] === id.toLowerCase() &&
    BigInt(match[2]) <= 9223372036854775807n,
  );
}
export async function changeTaskStatus(
  projectId: string,
  id: string,
  status: TaskStatus,
  etag: string,
  signal?: AbortSignal,
): Promise<TaskStatusSnapshot> {
  const response = await apiRequest(
    `/api/v1/projects/${projectId}/tasks/${id}/status`,
    {
      method: "PUT",
      credentials: "same-origin",
      signal,
      headers: { "Content-Type": "application/json", "If-Match": etag },
      body: JSON.stringify({ status }),
    },
  );
  const snapshot = await statusSnapshot(response, id);
  if (snapshot.status !== status)
    throw new Error("Respuesta de estado de tarea inválida");
  return snapshot;
}
export type TaskHistoryEntry = {
  id: string;
  fromStatus: TaskStatus;
  toStatus: TaskStatus;
  occurredAt: string;
};
export type TaskHistoryPage = {
  items: TaskHistoryEntry[];
  nextCursor: string | null;
};
export async function readTaskHistory(
  projectId: string,
  id: string,
  cursor?: string,
  signal?: AbortSignal,
): Promise<TaskHistoryPage> {
  const response = await apiRequest(
    `/api/v1/projects/${projectId}/tasks/${id}/history` +
      (cursor ? `?cursor=${encodeURIComponent(cursor)}` : ""),
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    !isRecord(data) ||
    Object.keys(data).length !== 2 ||
    !Array.isArray(data.items) ||
    data.items.length > 20 ||
    !data.items.every(isHistoryEntry) ||
    !(
      data.nextCursor === null ||
      (typeof data.nextCursor === "string" && data.nextCursor.length > 0)
    )
  )
    throw new Error("Respuesta de historial de tarea inválida");
  return data as TaskHistoryPage;
}
function isHistoryEntry(value: unknown): value is TaskHistoryEntry {
  return (
    isRecord(value) &&
    Object.keys(value).length === 4 &&
    typeof value.id === "string" &&
    /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(
      value.id,
    ) &&
    (value.fromStatus === "pending" || value.fromStatus === "completed") &&
    (value.toStatus === "pending" || value.toStatus === "completed") &&
    value.fromStatus !== value.toStatus &&
    isInstant(value.occurredAt)
  );
}
