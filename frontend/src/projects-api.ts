import type { ProjectStatus } from "./project-status";
export type Project = {
  id: string;
  ownerId: string;
  name: string;
  description: string;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
};
export type FieldError = { field: string; message: string };
export type CreateResult =
  | { kind: "created"; project: Project }
  | { kind: "failed"; message: string; errors: FieldError[] };
const uncertain =
  "No podemos confirmar si el proyecto se guardó. Comprueba la conexión antes de decidir si vuelves a enviarlo; podrías crear un duplicado.";
const serviceFailure =
  "El servicio no ha confirmado la creación. Conservamos tus datos.";
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
function isProject(value: unknown): value is Project {
  return (
    isRecord(value) &&
    ["id", "ownerId", "name", "description", "createdAt", "updatedAt"].every(
      (key) => typeof value[key] === "string",
    ) &&
    value.status === "idea" &&
    Number.isFinite(Date.parse(value.createdAt as string))
  );
}
export async function createProject(input: {
  name: string;
  description: string;
}): Promise<CreateResult> {
  try {
    const response = await fetch("/api/v1/projects", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
    if (response.status === 401)
      return {
        kind: "failed",
        message:
          "Necesitas autenticarte para crear proyectos. Copia tus datos antes de recargar la página para iniciar sesión.",
        errors: [],
      };
    if (response.status === 201) {
      const data: unknown = await response.json();
      return isProject(data)
        ? { kind: "created", project: data }
        : { kind: "failed", message: uncertain, errors: [] };
    }
    const data: unknown = await response.json().catch(() => null);
    const message =
      isRecord(data) && typeof data.title === "string" && data.title.length > 0
        ? data.title
        : serviceFailure;
    const errors =
      isRecord(data) && Array.isArray(data.errors)
        ? data.errors.filter(
            (error): error is FieldError =>
              isRecord(error) &&
              typeof error.field === "string" &&
              typeof error.message === "string",
          )
        : [];
    return { kind: "failed", message, errors };
  } catch {
    return { kind: "failed", message: uncertain, errors: [] };
  }
}
