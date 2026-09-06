import { apiRequest } from "./api-client";
export type Task = {
  id: string;
  projectId: string;
  title: string;
  completionCriterion: string;
  estimatedMinutes: number | null;
  status: "pending";
  createdAt: string;
  updatedAt: string;
};
export type TaskPage = { items: Task[]; nextCursor: string | null };
function isTask(value: unknown, projectId: string): value is Task {
  if (
    typeof value !== "object" ||
    value === null ||
    Object.keys(value).length !== 8
  )
    return false;
  const data = value as Record<string, unknown>;
  return (
    typeof data.id === "string" &&
    /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(
      data.id,
    ) &&
    data.projectId === projectId &&
    typeof data.title === "string" &&
    [...data.title].length >= 1 &&
    [...data.title].length <= 160 &&
    typeof data.completionCriterion === "string" &&
    [...data.completionCriterion].length <= 2000 &&
    (data.estimatedMinutes === null ||
      (typeof data.estimatedMinutes === "number" &&
        Number.isInteger(data.estimatedMinutes) &&
        data.estimatedMinutes >= 1 &&
        data.estimatedMinutes <= 1440)) &&
    data.status === "pending" &&
    typeof data.createdAt === "string" &&
    Number.isFinite(Date.parse(data.createdAt)) &&
    typeof data.updatedAt === "string" &&
    Number.isFinite(Date.parse(data.updatedAt))
  );
}
export async function createTask(
  projectId: string,
  draft: {
    title: string;
    completionCriterion: string;
    estimatedMinutes: number | null;
  },
  signal?: AbortSignal,
): Promise<Task> {
  const response = await apiRequest(`/api/v1/projects/${projectId}/tasks`, {
    method: "POST",
    signal,
    credentials: "same-origin",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(draft),
  });
  if (response.status !== 201) throw response;
  const data: unknown = await response.json();
  if (!isTask(data, projectId) || data.createdAt !== data.updatedAt)
    throw new Error("Respuesta de tarea inválida");
  return data;
}
export async function readTasks(
  projectId: string,
  cursor?: string,
  signal?: AbortSignal,
): Promise<TaskPage> {
  const response = await apiRequest(
    `/api/v1/projects/${projectId}/tasks` +
      (cursor ? `?cursor=${encodeURIComponent(cursor)}` : ""),
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    typeof data !== "object" ||
    data === null ||
    Object.keys(data).length !== 2 ||
    !("items" in data) ||
    !("nextCursor" in data) ||
    !Array.isArray(data.items) ||
    data.items.length > 20 ||
    !data.items.every((item) => isTask(item, projectId)) ||
    !(
      data.nextCursor === null ||
      (typeof data.nextCursor === "string" && data.nextCursor.length > 0)
    )
  )
    throw new Error("Respuesta de tareas inválida");
  return data as TaskPage;
}
