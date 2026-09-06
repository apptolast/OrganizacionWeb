import { expect, it, vi } from "vitest";
import { createTask, readTask, readTaskParent, readTasks } from "./tasks-api";
const projectId = "6c5dbd10-9ad5-4000-8000-000000000001";
const id = "7c5dbd10-9ad5-4000-8000-000000000002";
const parentId = "7c5dbd10-9ad5-4000-8000-000000000003";
const task = {
  id,
  projectId,
  title: "Confirmada",
  completionCriterion: "Resultado",
  estimatedMinutes: 5,
  status: "completed",
  createdAt: "2026-09-06T10:00:00Z",
  updatedAt: "2026-09-06T11:00:00Z",
};
it.each(["detail", "flat", "children", "parent"])(
  "@s9 completed task retains DTO8 in %s",
  async (operation) => {
    const body =
      operation === "detail"
        ? task
        : operation === "parent"
          ? { parent: task }
          : { items: [task], nextCursor: null };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
    const signal = new AbortController().signal;
    const response =
      operation === "detail"
        ? readTask(projectId, id, signal)
        : operation === "parent"
          ? readTaskParent(projectId, parentId, signal)
          : readTasks(
              projectId,
              undefined,
              signal,
              operation === "children" ? parentId : undefined,
            );
    await expect(response).resolves.toEqual(body);
    expect(Object.keys(task)).toHaveLength(8);
  },
);
it.each([undefined, parentId])(
  "@s9 preserves pending-only POST confirmation for parent %s",
  async (parent) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...task, updatedAt: task.createdAt }, { status: 201 }),
    );
    await expect(
      createTask(
        projectId,
        {
          title: task.title,
          completionCriterion: task.completionCriterion,
          estimatedMinutes: task.estimatedMinutes,
        },
        undefined,
        parent,
      ),
    ).rejects.toThrow("Respuesta de tarea inválida");
  },
);
it("@s9 rechaza estado de proyecto en un DTO8 de tarea", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json({ ...task, status: "active" }),
  );
  await expect(
    readTask(projectId, id, new AbortController().signal),
  ).rejects.toThrow("Respuesta de tarea inválida");
});
