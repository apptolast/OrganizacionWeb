import { apiRequest } from "./api-client";
import {
  exact,
  instant,
  sameId,
  uuid,
  text,
  integer,
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

function isSessionStart(value: unknown): value is SessionStart {
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

function microseconds(value: unknown): bigint | null {
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
