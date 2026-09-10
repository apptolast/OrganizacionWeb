// @ts-nocheck
import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";

export const EVENT_TYPES = [
  "ProjectCreated.v1",
  "ProjectUpdated.v1",
  "ProjectStatusChanged.v1",
  "TaskCreated.v1",
  "SubtaskCreated.v1",
  "TaskStatusChanged.v1",
  "BlockPlanned.v1",
  "BlockChanged.v1",
  "WorkSessionStarted.v1",
  "WorkSessionStateChanged.v1",
  "WorkSessionExtended.v1",
  "WorkSessionClosed.v1",
] as const;

export type EventType = (typeof EVENT_TYPES)[number];

export type AutomationAction =
  | {
      type: "CREATE_TASK";
      projectId: string;
      titleTemplate: string;
      criterionTemplate: string | null;
      estimatedMinutes: number | null;
    }
  | { type: "NOTIFY_WEBHOOK"; endpointId: string };

export type AutomationDraft = {
  name: string;
  enabled: boolean;
  trigger: { eventType: EventType };
  condition: { projectId: string } | null;
  action: AutomationAction;
};

export type Automation = AutomationDraft & {
  id: string;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type ActionPreview =
  | {
      type: "CREATE_TASK";
      projectId: string;
      title: string;
      completionCriterion: string;
      estimatedMinutes: number | null;
      wouldFail: string | null;
    }
  | { type: "NOTIFY_WEBHOOK"; endpointId: string; eventId: string };

export type AutomationMatch = {
  eventId: string;
  eventType: EventType;
  occurredAt: string;
  preview: ActionPreview;
  loopGuarded: boolean;
};

export type AutomationSimulation = {
  evaluatedEvents: number;
  matches: AutomationMatch[];
};

export const RUN_STATUSES = ["succeeded", "retry", "failed"] as const;
export type RunStatus = (typeof RUN_STATUSES)[number];

export type AutomationRun = {
  id: string;
  eventId: string;
  eventType: EventType;
  occurredAt: string;
  attempt: number;
  status: RunStatus;
  createdTaskId: string | null;
  deliveryId: string | null;
  errorCode: string | null;
  executedAt: string;
};

export type AutomationRunPage = {
  items: AutomationRun[];
  nextCursor: string | null;
};

/** The server said which field is wrong; the editor pins each message to its control. */
export class AutomationFieldErrors extends Error {
  constructor(public readonly fields: Record<string, string>) {
    super("Revisa los campos indicados.");
    this.name = "AutomationFieldErrors";
  }
}

/** Somebody else changed this rule; the draft survives until the owner reloads on purpose. */
export class AutomationConflict extends Error {
  constructor() {
    super("Otra pestaña cambió esta regla.");
    this.name = "AutomationConflict";
  }
}

const MESSAGES: Record<string, string> = {
  UNKNOWN_PLACEHOLDER: "Revisa los marcadores de esta plantilla.",
  UNCLOSED_PLACEHOLDER: "Falta cerrar un marcador con dos llaves.",
  PLACEHOLDER_NOT_AVAILABLE: "Ese marcador no existe para este disparador.",
  UNKNOWN_EVENT_TYPE: "Elige uno de los tipos de evento publicados.",
  TARGET_NOT_FOUND: "Elige un proyecto propio existente.",
  ENDPOINT_NOT_FOUND: "Elige un endpoint propio y activo.",
  TOO_LONG: "El valor es demasiado largo.",
  REQUIRED: "Este campo es obligatorio.",
  OUT_OF_RANGE: "El valor está fuera del rango permitido.",
};

const incompatible = () =>
  new Error("Respuesta de automatizaciones incompatible.");

function uuid(value: unknown): value is string {
  return (
    typeof value === "string" &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value,
    )
  );
}

function nullableUuid(value: unknown): value is string | null {
  return value === null || uuid(value);
}

function action(value: unknown): value is AutomationAction {
  if (
    exact(
      value,
      "type projectId titleTemplate criterionTemplate estimatedMinutes",
    ) &&
    value.type === "CREATE_TASK"
  )
    return (
      uuid(value.projectId) &&
      typeof value.titleTemplate === "string" &&
      (value.criterionTemplate === null ||
        typeof value.criterionTemplate === "string") &&
      (value.estimatedMinutes === null ||
        Number.isInteger(value.estimatedMinutes))
    );
  return (
    exact(value, "type endpointId") &&
    value.type === "NOTIFY_WEBHOOK" &&
    uuid(value.endpointId)
  );
}

function automation(value: unknown): value is Automation {
  return (
    exact(
      value,
      "id name enabled trigger condition action version createdAt updatedAt",
    ) &&
    uuid(value.id) &&
    typeof value.name === "string" &&
    typeof value.enabled === "boolean" &&
    exact(value.trigger, "eventType") &&
    EVENT_TYPES.includes(value.trigger.eventType as EventType) &&
    (value.condition === null ||
      (exact(value.condition, "projectId") &&
        uuid(value.condition.projectId))) &&
    action(value.action) &&
    Number.isInteger(value.version) &&
    instant(value.createdAt) &&
    instant(value.updatedAt)
  );
}

function preview(value: unknown): value is ActionPreview {
  if (
    exact(
      value,
      "type projectId title completionCriterion estimatedMinutes wouldFail",
    ) &&
    value.type === "CREATE_TASK"
  )
    return (
      uuid(value.projectId) &&
      typeof value.title === "string" &&
      typeof value.completionCriterion === "string" &&
      (value.estimatedMinutes === null ||
        Number.isInteger(value.estimatedMinutes)) &&
      (value.wouldFail === null || typeof value.wouldFail === "string")
    );
  return (
    exact(value, "type endpointId eventId") &&
    value.type === "NOTIFY_WEBHOOK" &&
    uuid(value.endpointId) &&
    uuid(value.eventId)
  );
}

function match(value: unknown): value is AutomationMatch {
  return (
    exact(value, "eventId eventType occurredAt preview loopGuarded") &&
    uuid(value.eventId) &&
    EVENT_TYPES.includes(value.eventType as EventType) &&
    instant(value.occurredAt) &&
    preview(value.preview) &&
    typeof value.loopGuarded === "boolean"
  );
}

function run(value: unknown): value is AutomationRun {
  return (
    exact(
      value,
      "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt",
    ) &&
    uuid(value.id) &&
    uuid(value.eventId) &&
    EVENT_TYPES.includes(value.eventType as EventType) &&
    instant(value.occurredAt) &&
    Number.isInteger(value.attempt) &&
    RUN_STATUSES.includes(value.status as RunStatus) &&
    nullableUuid(value.createdTaskId) &&
    nullableUuid(value.deliveryId) &&
    (value.errorCode === null || typeof value.errorCode === "string") &&
    instant(value.executedAt)
  );
}

async function failure(response: Response): Promise<never> {
  if (response.status === 412) throw new AutomationConflict();
  if (response.status === 400 || response.status === 422) {
    const body: unknown = await response.json().catch(() => null);
    if (
      body &&
      typeof body === "object" &&
      "errors" in body &&
      Array.isArray(body.errors)
    ) {
      const fields: Record<string, string> = {};
      for (const error of body.errors as unknown[])
        if (
          error &&
          typeof error === "object" &&
          "field" in error &&
          typeof error.field === "string" &&
          "code" in error &&
          typeof error.code === "string"
        )
          fields[error.field] ??=
            MESSAGES[error.code] ?? "Revisa el valor de este campo.";
      if (Object.keys(fields).length > 0)
        throw new AutomationFieldErrors(fields);
    }
  }
  throw response;
}

async function json(response: Response, signal: AbortSignal) {
  signal.throwIfAborted();
  if (response.status !== 200) await failure(response);
  return (await response.json()) as unknown;
}

export async function readAutomations(signal: AbortSignal) {
  signal.throwIfAborted();
  const body = await json(
    await apiRequest("/api/v1/me/automations", {
      signal,
      headers: { Accept: "application/json" },
    }),
    signal,
  );
  if (
    !exact(body, "items") ||
    !Array.isArray(body.items) ||
    !body.items.every(automation)
  )
    throw incompatible();
  return body.items as Automation[];
}

async function write(
  url: string,
  method: string,
  draft: AutomationDraft,
  signal: AbortSignal,
  version?: number,
) {
  signal.throwIfAborted();
  const headers: Record<string, string> = {
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  if (version !== undefined) headers["If-Match"] = `"${version}"`;
  return apiRequest(url, {
    method,
    signal,
    headers,
    body: JSON.stringify(draft),
  });
}

export async function createAutomation(
  draft: AutomationDraft,
  signal: AbortSignal,
) {
  const response = await write("/api/v1/me/automations", "POST", draft, signal);
  signal.throwIfAborted();
  if (response.status !== 201) await failure(response);
  const body: unknown = await response.json();
  if (!automation(body)) throw incompatible();
  return body;
}

export async function replaceAutomation(
  id: string,
  version: number,
  draft: AutomationDraft,
  signal: AbortSignal,
) {
  const body = await json(
    await write(`/api/v1/me/automations/${id}`, "PUT", draft, signal, version),
    signal,
  );
  if (!automation(body)) throw incompatible();
  return body;
}

export async function deleteAutomation(
  id: string,
  version: number,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  const response = await apiRequest(`/api/v1/me/automations/${id}`, {
    method: "DELETE",
    signal,
    headers: { "If-Match": `"${version}"` },
  });
  signal.throwIfAborted();
  if (response.status !== 204) await failure(response);
}

export async function simulateAutomation(
  draft: AutomationDraft,
  signal: AbortSignal,
) {
  const body = await json(
    await write("/api/v1/me/automations/simulate", "POST", draft, signal),
    signal,
  );
  if (
    !exact(body, "evaluatedEvents matches") ||
    !Number.isInteger(body.evaluatedEvents) ||
    !Array.isArray(body.matches) ||
    !body.matches.every(match)
  )
    throw incompatible();
  return body as unknown as AutomationSimulation;
}

export async function readAutomationRuns(
  id: string,
  cursor: string | null,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  const query = cursor === null ? "" : `?cursor=${encodeURIComponent(cursor)}`;
  const body = await json(
    await apiRequest(`/api/v1/me/automations/${id}/runs${query}`, {
      signal,
      headers: { Accept: "application/json" },
    }),
    signal,
  );
  if (
    !exact(body, "items nextCursor") ||
    !Array.isArray(body.items) ||
    !body.items.every(run) ||
    !(body.nextCursor === null || typeof body.nextCursor === "string")
  )
    throw incompatible();
  return body as unknown as AutomationRunPage;
}
