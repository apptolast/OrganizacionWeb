import { expect, it, vi } from "vitest";
import { readProjects } from "./read-projects-api";
const project = {
  id: "6c5dbd10-9ad5-4000-8000-000000000001",
  ownerId: "owner",
  name: "Idea",
  description: "",
  status: "idea",
  createdAt: "2026-09-05T12:00:00Z",
  updatedAt: "2026-09-05T12:00:00Z",
};
it.each([
  [
    "/proyectos",
    { items: [{ ...project, status: "unknown" }], nextCursor: null },
  ],
  [`/proyectos/${project.id}`, { ...project, ownerId: 42 }],
  ["/proyectos", { items: [], nextCursor: ["not-a-cursor"] }],
])(
  "rechaza estado, propietario o cursor incompatibles en %s",
  async (route, data) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(Response.json(data));
    await expect(
      readProjects(route, new AbortController().signal),
    ).rejects.toBeDefined();
  },
);
it.each([
  Response.json({ items: [], nextCursor: null }, { status: 201 }),
  Response.json({ items: [], nextCursor: "" }),
])(
  "no interpreta como una página válida una respuesta fuera del contrato",
  async (response) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(response);
    await expect(
      readProjects("/proyectos", new AbortController().signal),
    ).rejects.toBeDefined();
  },
);
