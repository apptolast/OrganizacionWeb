import { exact, sameId, uuid } from "./schedule-block-api";
import { apiRequest } from "./api-client";
import {
  isSessionStart,
  readWorkSessionError,
  microseconds,
  type SessionStart,
} from "./work-session-api";
export type WorkSessionState = {
  session: SessionStart;
  status: "running" | "paused" | "closed";
  revision: string;
  changedAt: string;
  workedMicroseconds: string;
  runningSince: string | null;
};
export type WorkSessionSnapshot = {
  state: WorkSessionState;
  serverNow: string;
  netMicroseconds: string;
  token: string;
};
export async function readWorkSessionState(
  id: string,
  signal?: AbortSignal,
): Promise<WorkSessionSnapshot> {
  const response = await apiRequest(`/api/v1/work-sessions/${id}/state`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value = await response.json();
  if (
    !exact(value, "state serverNow netMicroseconds") ||
    !isState(value.state) ||
    !sameId(value.state.session.id, id)
  )
    throw new Error("Estado de sesión inválido");
  const now = microseconds(value.serverNow);
  const since = microseconds(value.state.runningSince);
  if (
    now === null ||
    !decimal(value.netMicroseconds) ||
    BigInt(value.netMicroseconds) !==
      BigInt(value.state.workedMicroseconds) +
        (since === null || now < since ? 0n : now - since)
  )
    throw new Error("Estado de sesión inválido");
  const token = response.headers.get("Work-Session-Revision");
  if (
    token !== `work-session-${value.state.session.id}-${value.state.revision}`
  )
    throw new Error("Estado de sesión inválido");
  return { ...value, token } as WorkSessionSnapshot;
}

function isState(value: unknown): value is WorkSessionState {
  if (!(
    exact(
      value,
      "session status revision changedAt workedMicroseconds runningSince",
    ) &&
    (value.status === "running" ||
      value.status === "paused" ||
      value.status === "closed") &&
    isSessionStart(value.session) &&
    typeof value.revision === "string" &&
    /^[1-9][0-9]*$/.test(value.revision) &&
    BigInt(value.revision) <= 9223372036854775807n
  ))
    return false;
  const changed = microseconds(value.changedAt);
  const started = microseconds(value.session.startedAt);
  return (
    changed !== null &&
    started !== null &&
    changed >= started &&
    decimal(value.workedMicroseconds) &&
    BigInt(value.workedMicroseconds) <= changed - started &&
    (value.status === "running"
      ? microseconds(value.runningSince) === changed
      : value.runningSince === null)
  );
}

function decimal(value: unknown): value is string {
  return typeof value === "string" && /^(0|[1-9][0-9]*)$/.test(value);
}

export type WorkSessionChange = {
  id: string;
  sessionId: string;
  occurredAt: string;
  before: WorkSessionState;
  after: WorkSessionState;
} & (
  | { action: "PAUSE" | "RESUME" }
  | {
      action: "CLOSE";
      closure: {
        progressNote: string;
        nextStep: string;
        workDate: string;
        closeZoneId: string;
      };
    }
);
export type WorkSessionIntent = {
  state: WorkSessionState;
  token: string;
  key: string;
} & (
  | { action: "PAUSE" | "RESUME" }
  | { action: "CLOSE"; progressNote: string; nextStep: string }
);
export async function changeWorkSession(
  intent: WorkSessionIntent,
  signal?: AbortSignal,
): Promise<WorkSessionChange> {
  const response = await apiRequest(
    `/api/v1/work-sessions/${intent.state.session.id}/${intent.action.toLowerCase()}`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      body:
        intent.action === "CLOSE"
          ? JSON.stringify({
              progressNote: intent.progressNote,
              nextStep: intent.nextStep,
            })
          : "{}",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": intent.key,
        "Work-Session-Revision": intent.token,
      },
    },
  );
  if (response.status !== 201 && response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !isChange(value) ||
    response.headers.get("Location") !==
      `/api/v1/work-session-changes/${value.id}` ||
    value.action !== intent.action ||
    value.before.revision !== intent.state.revision ||
    !sameSession(value.before.session, intent.state.session) ||
    !sameNotes(value, intent)
  )
    throw new Error("Cambio de sesión inválido");
  return value;
}

function isChange(value: unknown): value is WorkSessionChange {
  return (
    ((exact(value, "id sessionId action occurredAt before after closure") &&
      value.action === "CLOSE" &&
      exact(value.closure, "progressNote nextStep workDate closeZoneId") &&
      validNote(value.closure.progressNote) &&
      validNote(value.closure.nextStep) &&
      typeof value.closure.closeZoneId === "string" &&
      typeof value.closure.workDate === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(value.closure.workDate) &&
      microseconds(value.closure.workDate + "T00:00:00Z") !== null) ||
      (exact(value, "id sessionId action occurredAt before after") &&
        value.action !== "CLOSE")) &&
    uuid(value.id) &&
    isState(value.before) &&
    isState(value.after) &&
    (value.action === "PAUSE"
      ? value.before.status === "running" && value.after.status === "paused"
      : value.action === "CLOSE"
        ? (value.before.status === "running" ||
            value.before.status === "paused") &&
          value.after.status === "closed"
        : value.action === "RESUME" &&
          value.before.status === "paused" &&
          value.after.status === "running") &&
    microseconds(value.occurredAt) === microseconds(value.after.changedAt) &&
    microseconds(value.after.changedAt)! >=
      microseconds(value.before.changedAt)! &&
    sameId(value.sessionId, value.before.session.id) &&
    sameSession(value.before.session, value.after.session) &&
    BigInt(value.after.revision) === BigInt(value.before.revision) + 1n &&
    BigInt(value.after.workedMicroseconds) ===
      BigInt(value.before.workedMicroseconds) +
        (value.before.status === "running"
          ? microseconds(value.after.changedAt)! -
            microseconds(value.before.changedAt)!
          : 0n)
  );
}

export function validNote(value: unknown): value is string {
  return (
    typeof value === "string" &&
    !/[\uD800-\uDFFF]/u.test(value) &&
    !value.includes("\u0000") &&
    [...value].length <= 2000
  );
}

function sameSession(a: SessionStart, b: SessionStart) {
  return Object.entries(a).every(
    ([key, value]) => value === b[key as keyof SessionStart],
  );
}

export async function readWorkSessionChange(
  id: string,
  signal?: AbortSignal,
): Promise<WorkSessionChange> {
  const response = await apiRequest(`/api/v1/work-session-changes/${id}`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (!isChange(value) || !sameId(value.id, id))
    throw new Error("Cambio de sesión inválido");
  return value;
}

export async function recoverWorkSessionChange(
  intent: WorkSessionIntent,
  signal?: AbortSignal,
): Promise<WorkSessionChange> {
  const response = await apiRequest(
    `/api/v1/work-session-changes/by-request/${intent.key}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !isChange(value) ||
    value.action !== intent.action ||
    value.before.revision !== intent.state.revision ||
    !sameSession(value.before.session, intent.state.session) ||
    !sameNotes(value, intent)
  )
    throw new Error("Cambio de sesión inválido");
  return value;
}

export async function readWorkSessionClosure(id: string, signal?: AbortSignal) {
  const response = await apiRequest(`/api/v1/work-sessions/${id}/closure`, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !isChange(value) ||
    value.action !== "CLOSE" ||
    !sameId(value.sessionId, id)
  )
    throw new Error("Cambio de sesión inválido");
  return value;
}

function sameNotes(value: WorkSessionChange, intent: WorkSessionIntent) {
  return (
    intent.action !== "CLOSE" ||
    (value.action === "CLOSE" &&
      value.closure.progressNote === intent.progressNote &&
      value.closure.nextStep === intent.nextStep)
  );
}

const stateErrors = {
  WORK_SESSION_CHANGE_NOT_FOUND: 404,
  PRECONDITION_FAILED: 412,
  WORK_SESSION_STATE_CONFLICT: 409,
  WORK_SESSION_REVISION_EXHAUSTED: 409,
} as const;
export async function readWorkSessionStateError(error: unknown) {
  const inherited = await readWorkSessionError(error);
  if (inherited) return inherited;
  if (!(error instanceof Response) || error.bodyUsed) return null;
  const value: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  if (
    !exact(value, "type title status code") ||
    typeof value.code !== "string" ||
    !Object.hasOwn(stateErrors, value.code) ||
    typeof value.title !== "string" ||
    !value.title.trim() ||
    value.type !== "urn:organization:problem:" + value.code.toLowerCase() ||
    value.status !== error.status ||
    stateErrors[value.code as keyof typeof stateErrors] !== error.status
  )
    return null;
  return value as {
    type: string;
    title: string;
    status: number;
    code: keyof typeof stateErrors;
  };
}
