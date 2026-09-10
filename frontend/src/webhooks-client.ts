import { apiRequest } from "./api-client";
import { exact, instant, uuid } from "./schedule-block-api";

/** The twelve subscribable types, in catalogue order: exactly the backend's own list. */
export const webhookEventTypes = [
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

const disabledReasons = ["MANUAL", "DELIVERY_EXHAUSTED"] as const;
const deliveryStatuses = ["pending", "succeeded", "exhausted"] as const;
/**
 * Las clases de error que el backend puede escribir en una entrega, tal cual las restringe la
 * columna error_class. SECRET_UNREADABLE (V31) es la octava: el secreto guardado no se pudo abrir,
 * así que no hubo envío, ni respuesta ni tiempo transcurrido.
 */
export const webhookErrorClasses = [
  "HTTP_ERROR",
  "REDIRECT",
  "TIMEOUT",
  "CONNECTION",
  "TLS",
  "DNS",
  "BLOCKED_ADDRESS",
  "SECRET_UNREADABLE",
] as const;

export type WebhookEndpoint = {
  id: string;
  url: string;
  description: string;
  eventTypes: string[];
  status: "active" | "disabled";
  disabledReason: string | null;
  disabledAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type WebhookDelivery = {
  id: string;
  eventId: string;
  eventType: string;
  status: string;
  attempt: number;
  httpStatus: number | null;
  latencyMs: number | null;
  errorClass: string | null;
  nextAttemptAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type WebhookInput = {
  url: string;
  description: string;
  eventTypes: string[];
};

const incompatible = () => new Error("Confirmación incompatible");

function identifier(value: unknown): value is string {
  return uuid(value) && (value as string).length === 36;
}

function whole(value: unknown, max: number): value is number {
  return (
    typeof value === "number" &&
    Number.isInteger(value) &&
    value >= 0 &&
    value <= max
  );
}

function decodeEndpoint(value: unknown): WebhookEndpoint {
  if (
    !exact(
      value,
      "id url description eventTypes status disabledReason disabledAt createdAt updatedAt",
    ) ||
    !identifier(value.id) ||
    typeof value.url !== "string" ||
    !value.url.startsWith("https://") ||
    typeof value.description !== "string" ||
    [...value.description].length > 80 ||
    !Array.isArray(value.eventTypes) ||
    value.eventTypes.length === 0 ||
    // Every type known, no repeats, and in catalogue order.
    value.eventTypes.some(
      (type, index, types) =>
        !(webhookEventTypes as readonly string[]).includes(type) ||
        (index > 0 &&
          webhookEventTypes.indexOf(types[index - 1] as never) >=
            webhookEventTypes.indexOf(type as never)),
    ) ||
    (value.status !== "active" && value.status !== "disabled") ||
    !instant(value.createdAt) ||
    !instant(value.updatedAt)
  )
    throw incompatible();
  // A disabled endpoint always carries both its reason and its instant; an active one, neither.
  const disabled = value.status === "disabled";
  if (
    disabled !==
      (value.disabledReason !== null &&
        (disabledReasons as readonly string[]).includes(
          value.disabledReason as string,
        )) ||
    disabled !== (value.disabledAt !== null && instant(value.disabledAt))
  )
    throw incompatible();
  return value as WebhookEndpoint;
}

function decodeDelivery(value: unknown): WebhookDelivery {
  if (
    !exact(
      value,
      "id eventId eventType status attempt httpStatus latencyMs errorClass nextAttemptAt createdAt updatedAt",
    ) ||
    !identifier(value.id) ||
    !identifier(value.eventId) ||
    typeof value.eventType !== "string" ||
    !value.eventType ||
    !(deliveryStatuses as readonly string[]).includes(value.status as string) ||
    !whole(value.attempt, 6) ||
    !(value.httpStatus === null || whole(value.httpStatus, 599)) ||
    !(
      value.latencyMs === null ||
      whole(value.latencyMs, Number.MAX_SAFE_INTEGER)
    ) ||
    !(
      value.errorClass === null ||
      (webhookErrorClasses as readonly string[]).includes(
        value.errorClass as string,
      )
    ) ||
    !(value.nextAttemptAt === null || instant(value.nextAttemptAt)) ||
    !instant(value.createdAt) ||
    !instant(value.updatedAt)
  )
    throw incompatible();
  // Only a pending delivery is ever scheduled for another attempt.
  if ((value.status === "pending") !== (value.nextAttemptAt !== null))
    throw incompatible();
  return value as WebhookDelivery;
}

function decodeList<T>(body: unknown, decode: (value: unknown) => T): T[] {
  if (!exact(body, "items") || !Array.isArray(body.items)) throw incompatible();
  const items = body.items.map(decode);
  if (
    new Set(items.map((item) => (item as { id: string }).id)).size !==
    items.length
  )
    throw incompatible();
  return items;
}

function webhookUrl(id: string) {
  if (!identifier(id)) throw new Error("Identidad incompatible");
  return `/api/v1/me/webhooks/${id}`;
}

async function json(
  response: Response,
  signal: AbortSignal,
  expected: number[],
) {
  signal.throwIfAborted();
  if (!expected.includes(response.status)) throw response;
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return body;
}

export async function listWebhooks(signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest("/api/v1/me/webhooks", { signal });
  return decodeList(await json(response, signal, [200]), decodeEndpoint);
}

export async function createWebhook(input: WebhookInput, signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest("/api/v1/me/webhooks", {
    method: "POST",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  const body = await json(response, signal, [201]);
  if (!exact(body, "endpoint secret")) throw incompatible();
  const endpoint = decodeEndpoint(body.endpoint);
  // whsec_ followed by exactly 43 base64url characters, with no padding.
  if (
    typeof body.secret !== "string" ||
    !/^whsec_[A-Za-z0-9_-]{43}$/.test(body.secret)
  )
    throw incompatible();
  return { endpoint, secret: body.secret };
}

export async function setWebhookStatus(
  id: string,
  status: "active" | "disabled",
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  const response = await apiRequest(`${webhookUrl(id)}/status`, {
    method: "PUT",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  });
  const endpoint = decodeEndpoint(await json(response, signal, [200]));
  if (endpoint.id !== id || endpoint.status !== status) throw incompatible();
  return endpoint;
}

export async function deleteWebhook(id: string, signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest(webhookUrl(id), {
    method: "DELETE",
    signal,
  });
  signal.throwIfAborted();
  if (response.status !== 204) throw response;
}

async function acceptedDelivery(response: Response, signal: AbortSignal) {
  const body = await json(response, signal, [202]);
  if (!exact(body, "delivery")) throw incompatible();
  return decodeDelivery(body.delivery);
}

export async function pingWebhook(id: string, signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest(`${webhookUrl(id)}/ping`, {
    method: "POST",
    signal,
  });
  return acceptedDelivery(response, signal);
}

export async function listWebhookDeliveries(id: string, signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest(`${webhookUrl(id)}/deliveries`, { signal });
  return decodeList(await json(response, signal, [200]), decodeDelivery);
}

export async function redeliverWebhook(
  id: string,
  deliveryId: string,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  if (!identifier(deliveryId)) throw new Error("Identidad incompatible");
  const response = await apiRequest(
    `${webhookUrl(id)}/deliveries/${deliveryId}/redeliver`,
    { method: "POST", signal },
  );
  return acceptedDelivery(response, signal);
}
