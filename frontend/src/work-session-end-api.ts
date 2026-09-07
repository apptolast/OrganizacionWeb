import { apiRequest } from "./api-client";
import { exact, sameId } from "./schedule-block-api";
import { microseconds } from "./work-session-api";
import { isState, type WorkSessionState } from "./work-session-state-api";

export type WorkSessionEnd = {
  state: WorkSessionState;
  serverNow: string;
  effectiveEndAt: string;
  token: string;
};

export async function readWorkSessionEnd(
  id: string,
  signal?: AbortSignal,
): Promise<WorkSessionEnd> {
  const response = await apiRequest(`/api/v1/work-sessions/${id}/end-time`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value = await response.json();
  if (
    !exact(value, "state serverNow effectiveEndAt") ||
    !isState(value.state) ||
    !sameId(value.state.session.id, id)
  )
    throw new Error("Fin de sesión inválido");
  const end = microseconds(value.effectiveEndAt);
  const now = microseconds(value.serverNow);
  if (now === null || now < microseconds(value.state.changedAt)!)
    throw new Error("Fin de sesión inválido");
  if (end === null || end < microseconds(value.state.session.plannedEndAt)!)
    throw new Error("Fin de sesión inválido");
  const token = response.headers.get("Work-Session-Revision");
  if (
    token !== `work-session-${value.state.session.id}-${value.state.revision}`
  )
    throw new Error("Fin de sesión inválido");
  return { ...value, token } as WorkSessionEnd;
}
