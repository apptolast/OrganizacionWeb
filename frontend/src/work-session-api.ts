import { apiRequest } from "./api-client";
import {
  exact,
  instant,
  sameId,
  uuid,
  text,
  integer,
  readBlockError,
} from "./schedule-block-api";

export type SessionStart = {
  id: string;
  projectId: string;
  taskId: string;
  startedAt: string;
  plannedMinutes: number;
  plannedEndAt: string;
  zoneId: string;
};

export async function readWorkSessionError(error: unknown) {
  const inherited = await readBlockError(error);
  if (inherited) return inherited;
  if (!(error instanceof Response) || error.bodyUsed) return null;
  const value: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  if (
    exact(value, "type title status code sessionId") &&
    value.code === "WORK_SESSION_ALREADY_ACTIVE" &&
    value.type === "urn:organization:problem:work_session_already_active" &&
    value.status === 409 &&
    error.status === 409 &&
    text(value.title) &&
    uuid(value.sessionId)
  )
    return value as {
      type: string;
      title: string;
      status: number;
      code: "WORK_SESSION_ALREADY_ACTIVE";
      sessionId: string;
    };
  if (
    !exact(value, "type title status code") ||
    !text(value.title) ||
    !text(value.code) ||
    value.type !== "urn:organization:problem:" + value.code.toLowerCase() ||
    value.status !== error.status ||
    !(
      (value.code === "WORK_SESSION_NOT_FOUND" && error.status === 404) ||
      (value.code === "WORK_SESSION_TIME_OUT_OF_RANGE" && error.status === 409)
    )
  )
    return null;
  return value as {
    type: string;
    title: string;
    status: number;
    code: "WORK_SESSION_NOT_FOUND" | "WORK_SESSION_TIME_OUT_OF_RANGE";
  };
}

export async function recoverWorkSession(
  projectId: string,
  taskId: string,
  plannedMinutes: number,
  requestKey: string,
  signal?: AbortSignal,
): Promise<SessionStart> {
  const response = await apiRequest(
    `/api/v1/work-sessions/by-request/${requestKey}`,
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !isSessionStart(value) ||
    !sameId(value.projectId, projectId) ||
    !sameId(value.taskId, taskId) ||
    value.plannedMinutes !== plannedMinutes
  )
    throw new Error("Inicio de trabajo inválido");
  return value;
}

export async function readWorkSession(
  id: string,
  signal?: AbortSignal,
): Promise<SessionStart> {
  const response = await apiRequest(`/api/v1/work-sessions/${id}`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (!isSessionStart(value) || !sameId(value.id, id))
    throw new Error("Inicio de trabajo inválido");
  return value;
}

export async function startWorkSession(
  projectId: string,
  taskId: string,
  plannedMinutes: number,
  requestKey: string,
  signal?: AbortSignal,
): Promise<SessionStart> {
  const response = await apiRequest(
    `/api/v1/projects/${projectId}/tasks/${taskId}/work-sessions`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": requestKey,
      },
      body: JSON.stringify({ plannedMinutes }),
    },
  );
  if (response.status !== 201 && response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !isSessionStart(value) ||
    value.plannedMinutes !== plannedMinutes ||
    !sameId(value.projectId, projectId) ||
    !sameId(value.taskId, taskId) ||
    response.headers.get("Location") !== `/api/v1/work-sessions/${value.id}`
  )
    throw new Error("Inicio de trabajo inválido");
  return value;
}

export function isSessionStart(value: unknown): value is SessionStart {
  if (
    !exact(
      value,
      "id projectId taskId startedAt plannedMinutes plannedEndAt zoneId",
    ) ||
    !integer(value.plannedMinutes, 1, 1440) ||
    !uuid(value.id) ||
    !uuid(value.projectId) ||
    !uuid(value.taskId) ||
    !text(value.zoneId)
  )
    return false;
  const start = microseconds(value.startedAt);
  const end = microseconds(value.plannedEndAt);
  return (
    start !== null &&
    end !== null &&
    end - start === BigInt(value.plannedMinutes) * 60_000_000n
  );
}

export function microseconds(value: unknown): bigint | null {
  if (!instant(value)) return null;
  const wholeMilliseconds = Date.parse(value.replace(/(?:\.\d+)?Z$/, "Z"));
  const fraction = value.match(/\.(\d+)Z$/)?.[1] ?? "";
  return BigInt(wholeMilliseconds) * 1000n + BigInt(fraction.padEnd(6, "0"));
}

export async function readActiveWorkSession(
  signal?: AbortSignal,
): Promise<SessionStart | null> {
  const response = await apiRequest("/api/v1/work-sessions/active", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !exact(value, "session") ||
    (value.session !== null && !isSessionStart(value.session))
  )
    throw new Error("Sesión activa inválida");
  return value.session;
}
