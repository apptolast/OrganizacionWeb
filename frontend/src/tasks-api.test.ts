import { expect, it, vi } from "vitest";
import { createTask, readTasks } from "./tasks-api";
const projectId = "6c5dbd10-9ad5-4000-8000-000000000001";
const task = {
  id: "7c5dbd10-9ad5-4000-8000-000000000002",
  projectId,
  title: "Una tarea",
  completionCriterion: "",
  estimatedMinutes: null,
  status: "pending",
  createdAt: "2026-09-06T12:00:00Z",
  updatedAt: "2026-09-06T12:00:00Z",
};
const draft = {
  title: task.title,
  completionCriterion: "",
  estimatedMinutes: null,
};
it.each([201, 503])(
  "GET HTTP %s con cuerpo válido sigue siendo un fallo",
  async (status) => {
    const response = Response.json({ items: [], nextCursor: null }, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readTasks(projectId)).rejects.toBe(response);
  },
);
it.each([
  { ...task, id: [task.id] },
  { ...task, id: "prefix" + task.id },
  { ...task, id: task.id + "suffix" },
  { ...task, createdAt: 42 },
  { ...task, updatedAt: 42 },
])(
  "rechaza tareas de lectura con tipo o UUID incompatible %#",
  async (value) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ items: [value], nextCursor: null }),
    );
    await expect(readTasks(projectId)).rejects.toThrow(
      "Respuesta de tareas inválida",
    );
  },
);
it.each(["ab", { items: [], nextCursor: ["cursor"] }])(
  "descarta envoltura y cursor de tipos incompatibles %#",
  async (value) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(value));
    await expect(readTasks(projectId)).rejects.toThrow(
      "Respuesta de tareas inválida",
    );
  },
);
it("declara JSON explícito al crear, sin depender de la interpretación del servidor", async () => {
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(task, { status: 201 }));
  await createTask(projectId, draft);
  expect(
    new Headers(fetcher.mock.calls[0][1]?.headers).get("Content-Type"),
  ).toBe("application/json");
});
it.each([1, 1440, null])(
  "@s1 @s7 acepta valores confirmados en límites de estimación %s",
  async (estimate) => {
    const value = {
      ...task,
      title: estimate === 1 ? "A" : "😀".repeat(160),
      completionCriterion: "😀".repeat(2000),
      estimatedMinutes: estimate,
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json(value, { status: 201 }),
    );
    await expect(createTask(projectId, draft)).resolves.toEqual(value);
  },
);
it.each([200, 400, 401, 403, 404, 409, 415, 500, 503])(
  "@s27 HTTP %s no se interpreta como creación confirmada",
  async (status) => {
    const response = Response.json(task, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(createTask(projectId, draft)).rejects.toBe(response);
  },
);
it("@s20 permite página completa de veinte tareas y no interpreta el cursor", async () => {
  const page = {
    items: Array.from({ length: 20 }, (_, index) => ({
      ...task,
      id: `7c5dbd10-9ad5-4000-8000-${String(index).padStart(12, "0")}`,
    })),
    nextCursor: "serveropaque",
  };
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(page));
  await expect(readTasks(projectId)).resolves.toEqual(page);
});
it("@s1 rechaza fechas iniciales distintas en la creación", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(
      { ...task, updatedAt: "2026-09-07T12:00:00Z" },
      { status: 201 },
    ),
  );
  await expect(createTask(projectId, draft)).rejects.toThrow(
    "Respuesta de tarea inválida",
  );
});
it.each([
  null,
  [],
  {},
  { items: [], nextCursor: null, private: "extra" },
  { items: {}, nextCursor: null },
  { items: [task], nextCursor: "" },
  { items: [task], nextCursor: 42 },
  { items: [{ ...task, projectId: "another" }], nextCursor: null },
  { items: Array.from({ length: 21 }, () => task), nextCursor: null },
])("@s20 descarta colección inválida %# sin exponer datos", async (value) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(value));
  await expect(readTasks(projectId)).rejects.toThrow(
    "Respuesta de tareas inválida",
  );
});
it.each([
  null,
  [],
  {},
  { ...task, ownerId: "private" },
  { ...task, id: "bad" },
  { ...task, projectId: "6c5dbd10-9ad5-4000-8000-000000000099" },
  { ...task, title: 42 },
  { ...task, title: "" },
  { ...task, title: "a".repeat(161) },
  { ...task, completionCriterion: null },
  { ...task, completionCriterion: "a".repeat(2001) },
  { ...task, estimatedMinutes: "1" },
  { ...task, estimatedMinutes: 0 },
  { ...task, estimatedMinutes: 1441 },
  { ...task, estimatedMinutes: 1.5 },
  { ...task, status: "completed" },
  { ...task, createdAt: "bad" },
  { ...task, updatedAt: null },
])("@s1 no confirma una respuesta de tarea inválida %#", async (value) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json(value, { status: 201 }),
  );
  await expect(createTask(projectId, draft)).rejects.toThrow(
    "Respuesta de tarea inválida",
  );
});
