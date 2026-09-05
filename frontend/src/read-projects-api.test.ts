import { expect, it, vi } from "vitest";
import { readProjects } from "./read-projects-api";
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
