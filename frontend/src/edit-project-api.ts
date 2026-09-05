import type { Project } from "./projects-api";
import { isProjectDetail } from "./read-projects-api";
import type { ProjectStatus } from "./project-status";
export type EditSnapshot = { project: Project; etag: string };
export function isStrongEtag(value: unknown): value is string {
  return typeof value === "string" && /^"[^"\s]+"$/.test(value);
}
export async function editRequest(
  detailRoute: string,
  signal: AbortSignal,
  input?: { name: string; description: string } | { status: ProjectStatus },
  etag?: string,
): Promise<EditSnapshot> {
  const response = await fetch(
    "/api/v1/projects" +
      detailRoute.slice("/proyectos".length) +
      (input && "status" in input ? "/status" : ""),
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
      ...(input
        ? {
            method: "PUT",
            headers: { "Content-Type": "application/json", "If-Match": etag! },
            body: JSON.stringify(input),
          }
        : {}),
    },
  );
  if (response.status !== 200) throw response;
  const responseTag = response.headers.get("ETag");
  if (!isStrongEtag(responseTag)) throw new Error("invalid_edit_response");
  const project: unknown = await response.json();
  if (!isProjectDetail(project, detailRoute.slice("/proyectos/".length)))
    throw new Error("invalid_edit_response");
  if (input && "status" in input && project.status !== input.status)
    throw new Error("invalid_edit_response");
  return {
    project,
    etag: responseTag,
  };
}
