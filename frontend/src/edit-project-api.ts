import type { Project } from "./projects-api";
import { isProjectDetail } from "./read-projects-api";
export type EditSnapshot = { project: Project; etag: string };
export async function editRequest(
  detailRoute: string,
  signal: AbortSignal,
  input?: { name: string; description: string },
  etag?: string,
): Promise<EditSnapshot> {
  const response = await fetch(
    "/api/v1/projects" + detailRoute.slice("/proyectos".length),
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
  if (!responseTag || !/^"[^"\s]+"$/.test(responseTag))
    throw new Error("invalid_edit_response");
  const project: unknown = await response.json();
  if (!isProjectDetail(project, detailRoute.slice("/proyectos/".length)))
    throw new Error("invalid_edit_response");
  return {
    project,
    etag: responseTag,
  };
}
