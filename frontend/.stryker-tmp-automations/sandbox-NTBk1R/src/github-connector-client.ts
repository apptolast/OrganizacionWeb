// @ts-nocheck
import { apiRequest } from "./api-client";
import { exact, instant, uuid } from "./schedule-block-api";
import { microseconds } from "./work-session-api";

const CONNECTION_URL = "/api/v1/me/connectors/github";
const IMPORTS_URL = `${CONNECTION_URL}/imports`;
// Doce campos desde que la feature 29 unificó la importación de GitHub y GitLab: el recibo
// nombra su origen (`source`) y la ruta del proyecto en él (`projectPath`), donde antes decía
// `repository`. additional_connectors.feature:242 exige que los dos recibos tengan las mismas
// claves; este cliente sólo habla con el extremo de GitHub, así que exige `source` "github".
const RECEIPT_FIELDS =
  "id source projectId projectPath status created skipped failed truncated errorCode startedAt finishedAt";
const GITHUB = "github";
const CONNECTION_FIELDS = "repository login status connectedAt lastImport";
const INCOMPATIBLE = "Confirmación incompatible";

export type GithubImportReceipt = {
  id: string;
  source: string;
  projectId: string;
  projectPath: string;
  status: "running" | "completed" | "failed";
  created: number;
  skipped: number;
  failed: number;
  truncated: boolean;
  errorCode: string | null;
  startedAt: string;
  finishedAt: string | null;
};

export type GithubConnection = {
  repository: string;
  login: string;
  status: "valid" | "invalid";
  connectedAt: string;
  lastImport: GithubImportReceipt | null;
};

/**
 * Un fallo del conector con el código que la interfaz necesita para ofrecer la acción que lo
 * resuelve. Nunca lleva el token: el servidor tampoco lo devuelve.
 */
export class ConnectorError extends Error {
  readonly code: string;
  readonly retryAfterSeconds: number | null;
  readonly importId: string | null;
  readonly created: number | null;
  readonly skipped: number | null;
  readonly failed: number | null;

  constructor(body: Record<string, unknown>) {
    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");
    this.name = "ConnectorError";
    this.code = typeof body.code === "string" ? body.code : "CONNECTOR_ERROR";
    this.retryAfterSeconds = counter(body.retryAfterSeconds);
    this.importId = uuid(body.importId) ? body.importId : null;
    this.created = counter(body.created);
    this.skipped = counter(body.skipped);
    this.failed = counter(body.failed);
  }
}

function counter(value: unknown): number | null {
  return typeof value === "number" && Number.isInteger(value) && value >= 0
    ? value
    : null;
}

function identifier(value: unknown): value is string {
  return (
    uuid(value) &&
    value === (value as string).toLowerCase() &&
    value.length === 36
  );
}

function decodeReceipt(value: unknown): GithubImportReceipt {
  if (
    !exact(value, RECEIPT_FIELDS) ||
    !identifier(value.id) ||
    !identifier(value.projectId) ||
    value.source !== GITHUB ||
    !nonEmpty(value.projectPath) ||
    !["running", "completed", "failed"].includes(value.status as string) ||
    !isCount(value.created) ||
    !isCount(value.skipped) ||
    !isCount(value.failed) ||
    typeof value.truncated !== "boolean" ||
    !instant(value.startedAt) ||
    !(value.errorCode === null || nonEmpty(value.errorCode)) ||
    // Sólo un recibo en curso carece de final, y sólo uno en curso carece de error.
    (value.status === "running") !== (value.finishedAt === null) ||
    (value.status === "running" && value.errorCode !== null) ||
    (value.finishedAt !== null &&
      (!instant(value.finishedAt) ||
        microseconds(value.finishedAt)! < microseconds(value.startedAt)!))
  )
    throw new Error(INCOMPATIBLE);
  return value as GithubImportReceipt;
}

function isCount(value: unknown): boolean {
  return typeof value === "number" && Number.isInteger(value) && value >= 0;
}

function nonEmpty(value: unknown): boolean {
  return typeof value === "string" && value.length > 0;
}

function decodeConnection(value: unknown): GithubConnection {
  if (
    !exact(value, CONNECTION_FIELDS) ||
    typeof value.repository !== "string" ||
    !/^[^/\s]+\/[^/\s]+$/.test(value.repository) ||
    typeof value.login !== "string" ||
    !value.login ||
    !["valid", "invalid"].includes(value.status as string) ||
    !instant(value.connectedAt)
  )
    throw new Error(INCOMPATIBLE);
  return {
    ...(value as unknown as GithubConnection),
    lastImport:
      value.lastImport === null ? null : decodeReceipt(value.lastImport),
  };
}

/** Convierte un problema RFC 7807 del conector en el error tipado que la pantalla entiende. */
async function failure(response: Response): Promise<never> {
  // Un cuerpo ilegible no debe tapar el estado que sí conocemos.
  const body: unknown = await response.json().catch(() => null);
  throw new ConnectorError(
    body && typeof body === "object" ? (body as Record<string, unknown>) : {},
  );
}

export async function readGithubConnection(
  signal: AbortSignal,
): Promise<GithubConnection | null> {
  signal.throwIfAborted();
  const response = await apiRequest(CONNECTION_URL, { signal });
  signal.throwIfAborted();
  if (response.status === 404) return null;
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeConnection(body);
}

export async function connectGithub(
  input: { repository: string; token: string },
  signal: AbortSignal,
): Promise<GithubConnection> {
  signal.throwIfAborted();
  const response = await apiRequest(CONNECTION_URL, {
    method: "PUT",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ repository: input.repository, token: input.token }),
  });
  signal.throwIfAborted();
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeConnection(body);
}

export async function disconnectGithub(signal: AbortSignal): Promise<void> {
  signal.throwIfAborted();
  const response = await apiRequest(CONNECTION_URL, {
    method: "DELETE",
    signal,
  });
  signal.throwIfAborted();
  if (response.status !== 204) return failure(response);
}

export async function startGithubImport(
  projectId: string,
  signal: AbortSignal,
): Promise<GithubImportReceipt> {
  signal.throwIfAborted();
  const response = await apiRequest(IMPORTS_URL, {
    method: "POST",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ projectId }),
  });
  signal.throwIfAborted();
  if (response.status !== 201) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeReceipt(body);
}

export async function readGithubImport(
  id: string,
  signal: AbortSignal,
): Promise<GithubImportReceipt> {
  signal.throwIfAborted();
  const response = await apiRequest(`${IMPORTS_URL}/${id}`, { signal });
  signal.throwIfAborted();
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeReceipt(body);
}
