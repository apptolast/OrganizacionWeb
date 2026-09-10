// @ts-nocheck
import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";

export type SyncStatus = "OK" | "FAILED";
export type FeedError =
  | "FEED_REJECTED"
  | "FEED_UNREACHABLE"
  | "FEED_HTTP_ERROR"
  | "FEED_TOO_LARGE"
  | "FEED_UNSUPPORTED_TYPE"
  | "FEED_MALFORMED"
  | "SECRET_UNREADABLE";
export type ExternalSubscription = {
  id: string;
  label: string;
  urlHost: string;
  urlTail: string;
  lastAttemptAt: string | null;
  lastSyncAt: string | null;
  lastStatus: SyncStatus | null;
  lastError: FeedError | null;
  snapshotZoneId: string | null;
  imported: number;
  skippedRecurring: number;
  skippedCancelled: number;
  skippedInvalid: number;
  truncated: boolean;
  updatedAt: string;
};
export type ExternalEvent = {
  uid: string;
  summary: string;
  startAt: string;
  endAt: string;
  allDay: boolean;
};
export type ExternalEvents = {
  configured: boolean;
  lastSyncAt: string | null;
  lastStatus: SyncStatus | null;
  items: ExternalEvent[];
};

const ROUTE = "/api/v1/me/external-calendar";
const SUBSCRIPTION_FIELDS =
  "id label urlHost urlTail lastAttemptAt lastSyncAt lastStatus lastError snapshotZoneId imported skippedRecurring skippedCancelled skippedInvalid truncated updatedAt";
const EVENT_FIELDS = "uid summary startAt endAt allDay";
const STATUSES = ["OK", "FAILED"];
const ERRORS = [
  "FEED_REJECTED",
  "FEED_UNREACHABLE",
  "FEED_HTTP_ERROR",
  "FEED_TOO_LARGE",
  "FEED_UNSUPPORTED_TYPE",
  "FEED_MALFORMED",
  "SECRET_UNREADABLE",
];
const FIELDS = ["label", "url"] as const;
export type ExternalCalendarField = (typeof FIELDS)[number];

export class ExternalCalendarValidationError extends Error {
  constructor(
    public readonly fields: Partial<Record<ExternalCalendarField, string>>,
    public readonly codes: Partial<Record<ExternalCalendarField, string>>,
  ) {
    super("Revisa los campos indicados.");
    this.name = "ExternalCalendarValidationError";
  }
}
export class ConnectorsDisabledError extends Error {
  constructor() {
    super("Los conectores externos no están disponibles.");
    this.name = "ConnectorsDisabledError";
  }
}
export class ExternalCalendarNotConfiguredError extends Error {
  constructor() {
    super("No hay ningún calendario externo configurado.");
    this.name = "ExternalCalendarNotConfiguredError";
  }
}
function invalid(): never {
  throw new Error("Respuesta de calendario externo inválida.");
}
function counter(value: unknown): value is number {
  return Number.isInteger(value) && (value as number) >= 0;
}
function optionalInstant(value: unknown) {
  return value === null || instant(value);
}
function subscriptionOf(value: unknown): ExternalSubscription {
  if (
    !exact(value, SUBSCRIPTION_FIELDS) ||
    typeof value.id !== "string" ||
    !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
      value.id,
    ) ||
    typeof value.label !== "string" ||
    value.label.length === 0 ||
    typeof value.urlHost !== "string" ||
    value.urlHost.length === 0 ||
    typeof value.urlTail !== "string" ||
    value.urlTail.length !== 4 ||
    !optionalInstant(value.lastAttemptAt) ||
    !optionalInstant(value.lastSyncAt) ||
    !(
      value.lastStatus === null || STATUSES.includes(value.lastStatus as string)
    ) ||
    !(value.lastError === null || ERRORS.includes(value.lastError as string)) ||
    !(
      value.snapshotZoneId === null || typeof value.snapshotZoneId === "string"
    ) ||
    !counter(value.imported) ||
    !counter(value.skippedRecurring) ||
    !counter(value.skippedCancelled) ||
    !counter(value.skippedInvalid) ||
    typeof value.truncated !== "boolean" ||
    !instant(value.updatedAt)
  )
    invalid();
  return value as unknown as ExternalSubscription;
}
function eventOf(value: unknown): ExternalEvent {
  if (
    !exact(value, EVENT_FIELDS) ||
    typeof value.uid !== "string" ||
    value.uid.length === 0 ||
    typeof value.summary !== "string" ||
    !instant(value.startAt) ||
    !instant(value.endAt) ||
    Date.parse(value.endAt) <= Date.parse(value.startAt) ||
    typeof value.allDay !== "boolean"
  )
    invalid();
  return value as unknown as ExternalEvent;
}
function snapshotOf(value: unknown) {
  if (
    !exact(value, "configured subscription") ||
    typeof value.configured !== "boolean"
  )
    invalid();
  if (!value.configured) {
    if (value.subscription !== null) invalid();
    return { configured: false as const, subscription: null };
  }
  return {
    configured: true as const,
    subscription: subscriptionOf(value.subscription),
  };
}
async function problem(response: Response) {
  return (await response
    .clone()
    .json()
    .catch(() => null)) as { code?: unknown; errors?: unknown } | null;
}
async function refuse(response: Response): Promise<never> {
  const body = await problem(response);
  if (response.status === 503 && body?.code === "CONNECTORS_DISABLED")
    throw new ConnectorsDisabledError();
  if (
    response.status === 404 &&
    body?.code === "EXTERNAL_CALENDAR_NOT_CONFIGURED"
  )
    throw new ExternalCalendarNotConfiguredError();
  if (response.status === 400 && body?.code === "VALIDATION_ERROR") {
    const fields: Partial<Record<ExternalCalendarField, string>> = {};
    const codes: Partial<Record<ExternalCalendarField, string>> = {};
    for (const entry of Array.isArray(body.errors) ? body.errors : []) {
      if (
        entry &&
        typeof entry === "object" &&
        FIELDS.includes(
          (entry as { field?: string }).field as ExternalCalendarField,
        ) &&
        typeof (entry as { message?: unknown }).message === "string" &&
        typeof (entry as { code?: unknown }).code === "string"
      ) {
        const field = (entry as { field: ExternalCalendarField }).field;
        fields[field] ??= (entry as { message: string }).message;
        codes[field] ??= (entry as { code: string }).code;
      }
    }
    if (Object.keys(fields).length > 0)
      throw new ExternalCalendarValidationError(fields, codes);
  }
  throw response;
}
async function json(url: string, options: RequestInit, signal?: AbortSignal) {
  signal?.throwIfAborted();
  const response = await apiRequest(url, {
    ...options,
    signal,
    headers: { Accept: "application/json", ...options.headers },
  });
  signal?.throwIfAborted();
  if (response.status !== 200) await refuse(response);
  return (await response.json().catch(() => invalid())) as unknown;
}
const writing = (body: unknown) => ({
  method: "PUT",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify(body),
});

export async function readExternalCalendar(signal?: AbortSignal) {
  return snapshotOf(await json(ROUTE, {}, signal));
}

export async function saveExternalCalendar(
  label: string,
  url: string,
  signal?: AbortSignal,
) {
  const body = await json(ROUTE, writing({ label, url }), signal);
  const snapshot = snapshotOf(body);
  if (!snapshot.configured) invalid();
  return snapshot.subscription;
}

export async function deleteExternalCalendar(signal?: AbortSignal) {
  signal?.throwIfAborted();
  const response = await apiRequest(ROUTE, { method: "DELETE", signal });
  signal?.throwIfAborted();
  if (response.status !== 204) await refuse(response);
}

export async function syncExternalCalendar(
  onlyIfStale: boolean,
  signal?: AbortSignal,
) {
  const body = await json(
    `${ROUTE}/sync`,
    { ...writing({ onlyIfStale }), method: "POST" },
    signal,
  );
  if (
    !exact(body, "performed subscription") ||
    typeof body.performed !== "boolean"
  )
    invalid();
  return {
    performed: body.performed,
    subscription: subscriptionOf(body.subscription),
  };
}

export async function readExternalEvents(
  from: string,
  to: string,
  signal?: AbortSignal,
): Promise<ExternalEvents> {
  const query = new URLSearchParams({ from, to }).toString();
  const body = await json(`${ROUTE}/events?${query}`, {}, signal);
  if (
    !exact(body, "configured lastSyncAt lastStatus items") ||
    typeof body.configured !== "boolean" ||
    !optionalInstant(body.lastSyncAt) ||
    !(
      body.lastStatus === null || STATUSES.includes(body.lastStatus as string)
    ) ||
    !Array.isArray(body.items)
  )
    invalid();
  return {
    configured: body.configured,
    lastSyncAt: body.lastSyncAt as string | null,
    lastStatus: body.lastStatus as SyncStatus | null,
    items: body.items.map(eventOf),
  };
}
