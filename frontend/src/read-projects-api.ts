import type { Project } from "./projects-api";
export type ProjectSummary = Pick<
  Project,
  "id" | "name" | "status" | "createdAt" | "updatedAt"
>;
export type ProjectPage = {
  items: ProjectSummary[];
  nextCursor: string | null;
};
function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
function isSummary(value: unknown): value is ProjectSummary {
  return (
    isRecord(value) &&
    ["id", "name", "createdAt", "updatedAt"].every(
      (key) => typeof value[key] === "string",
    ) &&
    value.status === "idea" &&
    Number.isFinite(Date.parse(value.createdAt as string)) &&
    Number.isFinite(Date.parse(value.updatedAt as string))
  );
}
export async function readProjects(
  route: string,
  signal: AbortSignal,
): Promise<ProjectPage | Project> {
  const response = await fetch(
    "/api/v1/projects" + route.slice("/proyectos".length),
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (route.startsWith("/proyectos/")) {
    if (
      isSummary(data) &&
      "ownerId" in data &&
      "description" in data &&
      typeof data.ownerId === "string" &&
      typeof data.description === "string" &&
      data.id === route.slice("/proyectos/".length)
    )
      return data as Project;
  } else if (
    isRecord(data) &&
    Array.isArray(data.items) &&
    data.items.every(isSummary) &&
    (data.nextCursor === null ||
      (typeof data.nextCursor === "string" && data.nextCursor.length > 0))
  )
    return data as ProjectPage;
  throw new Error("invalid_read_response");
}
