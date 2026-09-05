import { useState, useRef, useEffect } from "react";
import type { ProjectSnapshot } from "./read-projects-api";
import type { ProjectStatus } from "./project-status";
import { editRequest, isStrongEtag } from "./edit-project-api";
export function useProjectStatus(
  snapshot: ProjectSnapshot,
  onConfirmed: (snapshot: ProjectSnapshot) => void,
  onAccessFailure: (status: number) => void,
) {
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  const [success, setSuccess] = useState(false);
  const [saving, setSaving] = useState(false);
  const [failure, setFailure] = useState<number | null>(null);
  const [capacity, setCapacity] = useState<{
    activeCount: number;
    limit: number;
  }>();
  async function change(status: ProjectStatus) {
    if (saving || !isStrongEtag(snapshot.etag)) return;
    setSaving(true);
    setFailure(null);
    setCapacity(undefined);
    setSuccess(false);
    const controller = new AbortController();
    request.current = controller;
    try {
      const result = await editRequest(
        `/proyectos/${snapshot.project.id}`,
        controller.signal,
        { status },
        snapshot.etag!,
      );
      if (controller.signal.aborted) return;
      onConfirmed(result);
      setSuccess(true);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (
        error instanceof Response &&
        (error.status === 401 || error.status === 404)
      ) {
        onAccessFailure(error.status);
        return;
      }
      setFailure(error instanceof Response ? error.status : 0);
      if (error instanceof Response && error.status === 409) {
        const body: unknown = await error.json().catch(() => null);
        if (
          body &&
          typeof body === "object" &&
          "code" in body &&
          body.code === "ACTIVE_PROJECT_LIMIT" &&
          "activeCount" in body &&
          typeof body.activeCount === "number" &&
          Number.isInteger(body.activeCount) &&
          body.activeCount >= 0 &&
          "limit" in body &&
          typeof body.limit === "number" &&
          Number.isInteger(body.limit) &&
          body.limit > 0
        )
          setCapacity({ activeCount: body.activeCount, limit: body.limit });
      }
    }
    setSaving(false);
  }
  return { success, saving, change, failure, capacity };
}
