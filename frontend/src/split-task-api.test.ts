import { expect, it, vi } from "vitest";
import { createTask, readTask, readTaskParent, readTasks } from "./tasks-api";

const projectId = "6c5dbd10-9ad5-4000-8000-000000000001";
const taskId = "7c5dbd10-9ad5-4000-8000-000000000002";
const parent = {
  id: "7c5dbd10-9ad5-4000-8000-000000000003",
  projectId,
  title: "Tarea padre",
  completionCriterion: "Criterio privado",
  estimatedMinutes: 60,
  status: "pending",
  createdAt: "2026-09-06T12:00:00Z",
  updatedAt: "2026-09-06T12:00:00Z",
};
it.each([null, 42])(
  "rechaza projectId no textual %s con error controlado",
  async (projectValue) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
      Response.json({ ...parent, projectId: projectValue }),
    );
    await expect(
      readTask(projectId, parent.id, new AbortController().signal),
    ).rejects.toThrow("Respuesta de tarea inválida");
  },
);
it("rechaza relación JSON de un carácter con error controlado", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json("a"));
  await expect(readTaskParent(projectId, taskId)).rejects.toThrow(
    "Respuesta de relación inválida",
  );
});

it.each([null, parent])(
  "@s9 consulta relación confirmada %# sin ampliar DTO8",
  async (value) => {
    const signal = new AbortController().signal;
    const fetcher = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(Response.json({ parent: value }));
    await expect(readTaskParent(projectId, taskId, signal)).resolves.toEqual({
      parent: value,
    });
    expect(fetcher).toHaveBeenCalledWith(
      `/api/v1/projects/${projectId}/tasks/${taskId}/parent`,
      {
        credentials: "same-origin",
        cache: "no-store",
        signal,
      },
    );
  },
);

it.each([201, 400, 401, 403, 404, 500, 503])(
  "@s6 @s19 @s20 relación HTTP %s conserva rechazo aunque el cuerpo parezca válido",
  async (status) => {
    const response = Response.json({ parent: null }, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(readTaskParent(projectId, taskId)).rejects.toBe(response);
  },
);

it.each([
  null,
  false,
  [],
  {},
  { private: null },
  { parent: null, private: "extra" },
  { parent: "root" },
  { parent: {} },
  { parent: { ...parent, projectId: "foreign" } },
  { parent: { ...parent, id: taskId } },
  { parent: { ...parent, ownerId: "private" } },
  { parent: { ...parent, estimatedMinutes: 1441 } },
])(
  "@s9 @s20 no convierte relación inválida %# en raíz confirmada",
  async (body) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
    await expect(readTaskParent(projectId, taskId)).rejects.toThrow(
      "Respuesta de relación inválida",
    );
  },
);

it("@s1 crea en la colección del padre con JSON y señal sin cambiar el borrador", async () => {
  const value = { ...parent, id: taskId };
  const draft = {
    title: "Nuevo paso",
    completionCriterion: "  Criterio\nprivado  ",
    estimatedMinutes: 5,
  };
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(value, { status: 201 }));
  await expect(
    createTask(projectId, draft, signal, parent.id),
  ).resolves.toEqual(value);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${projectId}/tasks/${parent.id}/subtasks`,
    expect.objectContaining({
      method: "POST",
      signal,
      credentials: "same-origin",
      body: JSON.stringify(draft),
    }),
  );
  expect(
    new Headers(fetcher.mock.calls[0][1]?.headers).get("Content-Type"),
  ).toBe("application/json");
});

it("@s12 @s35 consulta página del padre codificando el cursor opaco", async () => {
  const page = { items: [{ ...parent, id: taskId }], nextCursor: "opaque" };
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(page));
  await expect(
    readTasks(projectId, "opaque+/=?", signal, parent.id),
  ).resolves.toEqual(page);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${projectId}/tasks/${parent.id}/subtasks?cursor=opaque%2B%2F%3D%3F`,
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
});

it("@s23 acepta detalle canónico cuando la ruta UUID usa mayúsculas", async () => {
  const signal = new AbortController().signal;
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(parent));
  await expect(
    readTask(projectId.toUpperCase(), parent.id.toUpperCase(), signal),
  ).resolves.toEqual(parent);
});

it("@s9 acepta padre canónico del mismo proyecto con ruta en mayúsculas", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json({ parent }),
  );
  await expect(
    readTaskParent(projectId.toUpperCase(), taskId.toUpperCase()),
  ).resolves.toEqual({ parent });
});

it("@s9 rechaza una autorrelación aunque cambie la caja del UUID", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json({ parent }),
  );
  await expect(
    readTaskParent(projectId, parent.id.toUpperCase()),
  ).rejects.toThrow("Respuesta de relación inválida");
});

it("@s1 conserva DTO canónico al crear en proyecto con ruta en mayúsculas", async () => {
  const value = { ...parent, id: taskId };
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(value, { status: 201 }),
  );
  await expect(
    createTask(
      projectId.toUpperCase(),
      { title: value.title, completionCriterion: "", estimatedMinutes: null },
      undefined,
      parent.id.toUpperCase(),
    ),
  ).resolves.toEqual(value);
});

it("@s1 no confirma al padre como nueva subtarea", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json(parent, { status: 201 }),
  );
  await expect(
    createTask(
      projectId,
      { title: "Nuevo paso", completionCriterion: "", estimatedMinutes: null },
      undefined,
      parent.id.toUpperCase(),
    ),
  ).rejects.toThrow("Respuesta de tarea inválida");
});

it("@s11 no acepta el padre dentro de su colección de hijos", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(
    Response.json({ items: [parent], nextCursor: null }),
  );
  await expect(
    readTasks(projectId, undefined, undefined, parent.id.toUpperCase()),
  ).rejects.toThrow("Respuesta de tareas inválida");
});

it("@s23 consulta detalle propio con señal, cookies y no-store", async () => {
  const signal = new AbortController().signal;
  const fetcher = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValueOnce(Response.json(parent));
  await expect(readTask(projectId, parent.id, signal)).resolves.toEqual(parent);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/projects/${projectId}/tasks/${parent.id}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
});

it.each([201, 401, 404, 503])(
  "@s19 detalle HTTP %s no se convierte en éxito por su cuerpo",
  async (status) => {
    const response = Response.json(parent, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(
      readTask(projectId, parent.id, new AbortController().signal),
    ).rejects.toBe(response);
  },
);

it.each([
  null,
  "abcdefgh",
  { ...parent, id: taskId },
  { ...parent, projectId: taskId },
  { ...parent, private: true },
])("@s23 rechaza detalle fuera del contrato o identidad %#", async (body) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(body));
  await expect(
    readTask(projectId, parent.id, new AbortController().signal),
  ).rejects.toThrow("Respuesta de tarea inválida");
});

it.each([201, 503])(
  "@s19 hijos HTTP %s con página válida no son éxito",
  async (status) => {
    const response = Response.json({ items: [], nextCursor: null }, { status });
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(response);
    await expect(
      readTasks(projectId, undefined, undefined, parent.id),
    ).rejects.toBe(response);
  },
);

it.each([0, 20])(
  "@s11 @s35 acepta %s hijos confirmados y no altera cursor",
  async (count) => {
    const value = {
      items: Array.from({ length: count }, (_, index) => ({
        ...parent,
        id: `aaaaaaaa-aaaa-4aaa-8aaa-${String(index).padStart(12, "0")}`,
      })),
      nextCursor: count ? "opaque" : null,
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(value));
    await expect(
      readTasks(projectId.toUpperCase(), undefined, undefined, parent.id),
    ).resolves.toEqual(value);
  },
);

it.each([
  {
    items: Array.from({ length: 21 }, () => ({ ...parent, id: taskId })),
    nextCursor: null,
  },
  { items: [{ ...parent, id: taskId, projectId: taskId }], nextCursor: null },
  { items: [], nextCursor: ["opaque"] },
  { items: [], nextCursor: null, private: true },
])("@s11 @s35 rechaza página de hijos inválida %#", async (value) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValueOnce(Response.json(value));
  await expect(
    readTasks(projectId, undefined, undefined, parent.id),
  ).rejects.toThrow("Respuesta de tareas inválida");
});

it.each(["network", "json"])(
  "@s20 fallo de relación %s no produce raíz ni reintento",
  async (failure) => {
    const fetcher = vi.spyOn(globalThis, "fetch");
    if (failure === "network")
      fetcher.mockRejectedValueOnce(new TypeError("offline"));
    else fetcher.mockResolvedValueOnce(new Response("{"));
    await expect(readTaskParent(projectId, taskId)).rejects.toThrow();
    expect(fetcher).toHaveBeenCalledTimes(1);
  },
);
