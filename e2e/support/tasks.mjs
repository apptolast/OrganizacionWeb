import { expect } from "@playwright/test";
import { csrfHeaders } from "../../scripts/session-client.mjs";

export const taskFields = [
  "id",
  "projectId",
  "title",
  "completionCriterion",
  "estimatedMinutes",
  "status",
  "createdAt",
  "updatedAt",
].sort();

export async function saveTask(
  request,
  projectId,
  title,
  extra = {},
  parentId,
) {
  const path =
    `/api/v1/projects/${projectId}/tasks` +
    (parentId ? `/${parentId}/subtasks` : "");
  const response = await request.post(path, {
    headers: await csrfHeaders(request),
    data: { title, ...extra },
  });
  expect(response.status()).toBe(201);
  const task = await response.json();
  expect(Object.keys(task).sort()).toEqual(taskFields);
  expect(response.headers().location).toBe(
    `/api/v1/projects/${projectId}/tasks/${task.id}`,
  );
  return task;
}
