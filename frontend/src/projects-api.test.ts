import { expect, it, vi } from "vitest";
import { createProject } from "./projects-api";
const input = { name: "Idea", description: "" };
const project = {
  id: "project-id",
  ownerId: "owner-id",
  name: "Idea",
  description: "",
  status: "idea",
  createdAt: "2026-09-05T12:00:00Z",
  updatedAt: "2026-09-05T12:00:00Z",
};
it("rechaza confirmación con fecha no válida que no se puede mostrar", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json({ ...project, createdAt: "incorrecto" }, { status: 201 }),
  );
  expect(await createProject(input)).toMatchObject({
    kind: "failed",
    message: expect.stringContaining("No podemos confirmar"),
  });
});
it("un 401 pide recuperar la autenticación sin reenvío automático", async () => {
  const request = vi
    .spyOn(globalThis, "fetch")
    .mockResolvedValue(new Response("", { status: 401 }));
  expect(await createProject(input)).toEqual({
    kind: "failed",
    message:
      "Necesitas autenticarte para crear proyectos. Copia tus datos antes de recargar la página para iniciar sesión.",
    errors: [],
  });
  expect(request).toHaveBeenCalledTimes(1);
});
it.each([403, 415, 500, 503])(
  "HTTP %i sin errors conserva un fallo estructurado",
  async (status) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      Response.json(
        {
          type: "about:blank",
          title: "No se pudo crear el proyecto.",
          status,
          code: "ERROR",
        },
        { status },
      ),
    );
    expect(await createProject(input)).toEqual({
      kind: "failed",
      message: "No se pudo crear el proyecto.",
      errors: [],
    });
  },
);
it.each([
  null,
  [],
  42,
  "text",
  {},
  { ...project, status: "active" },
  { ...project, id: 7 },
  { ...project, ownerId: null },
  { ...project, name: [] },
  { ...project, description: 4 },
  { ...project, createdAt: null },
  { ...project, updatedAt: false },
])("rechaza representaciones 201 fuera de contrato: %j", async (data) => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json(data, { status: 201 }),
  );
  expect(await createProject(input)).toMatchObject({
    kind: "failed",
    message: expect.stringContaining("No podemos confirmar"),
    errors: [],
  });
});
it("filtra errores por campo mal formados de una respuesta no válida", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json(
      {
        title: "Revisa los campos.",
        errors: [
          null,
          42,
          {},
          { field: "name" },
          { message: "Sin campo" },
          { field: 5, message: "Campo inválido" },
          { field: "description", message: 55 },
          { field: "name", message: "Escribe un nombre." },
        ],
      },
      { status: 400 },
    ),
  );
  expect(await createProject(input)).toEqual({
    kind: "failed",
    message: "Revisa los campos.",
    errors: [{ field: "name", message: "Escribe un nombre." }],
  });
});
it("una respuesta de error sin mensaje útil muestra una explicación", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json({ title: "" }, { status: 500 }),
  );
  expect(await createProject(input)).toEqual({
    kind: "failed",
    message: "El servicio no ha confirmado la creación. Conservamos tus datos.",
    errors: [],
  });
});
it("clasifica fallo de transporte sin errores de validación ni reintentos", async () => {
  const request = vi
    .spyOn(globalThis, "fetch")
    .mockRejectedValue(new TypeError("connection reset"));
  expect(await createProject(input)).toEqual({
    kind: "failed",
    message:
      "No podemos confirmar si el proyecto se guardó. Comprueba la conexión antes de decidir si vuelves a enviarlo; podrías crear un duplicado.",
    errors: [],
  });
  expect(request).toHaveBeenCalledTimes(1);
});
it("un título de error no textual se sustituye por un mensaje seguro", async () => {
  vi.spyOn(globalThis, "fetch").mockResolvedValue(
    Response.json({ title: ["Detalle interno"] }, { status: 500 }),
  );
  expect(await createProject(input)).toEqual({
    kind: "failed",
    message: "El servicio no ha confirmado la creación. Conservamos tus datos.",
    errors: [],
  });
});
