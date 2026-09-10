import { apiRequest } from "./api-client";
import { exact, instant, uuid } from "./schedule-block-api";
import { microseconds } from "./work-session-api";

const CONNECTION_URL = "/api/v1/me/connectors/gitlab";
const IMPORTS_URL = `${CONNECTION_URL}/imports`;
export const CONNECTION_KEYS =
  "status apiBase projectPath projectId tokenHint lastActivityAt lastError version";
export const RECEIPT_KEYS =
  "id source projectId projectPath status created skipped failed truncated errorCode startedAt finishedAt";
export const ERROR_FIELDS = "code at";
const INCOMPATIBLE = "Confirmación incompatible";
const SOURCE = "gitlab";

export type ConnectorFailure = { code: string; at: string };

export type GitlabConnection = {
  status: "connected" | "error" | "not_connected";
  apiBase: string | null;
  projectPath: string | null;
  projectId: number | null;
  tokenHint: string | null;
  lastActivityAt: string | null;
  lastError: ConnectorFailure | null;
  version: number | null;
};

export type GitlabImportReceipt = {
  id: string;
  source: "gitlab";
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

/**
 * Un fallo del conector con el código que la pantalla necesita para ofrecer la acción que lo
 * resuelve. Nunca lleva el token: el servidor tampoco lo devuelve.
 */
export class GitlabConnectorError extends Error {
  readonly code: string;
  readonly retryAfterSeconds: number | null;
  readonly importId: string | null;
  readonly created: number | null;
  readonly skipped: number | null;
  readonly failed: number | null;
  /** Campo → código, para poder señalar el control que el servidor rechazó. */
  readonly fields: Record<string, string>;

  constructor(body: Record<string, unknown>) {
    super(typeof body.code === "string" ? body.code : "CONNECTOR_ERROR");
    this.name = "GitlabConnectorError";
    this.code = typeof body.code === "string" ? body.code : "CONNECTOR_ERROR";
    this.retryAfterSeconds = counter(body.retryAfterSeconds);
    this.importId = identifier(body.importId) ? body.importId : null;
    this.created = counter(body.created);
    this.skipped = counter(body.skipped);
    this.failed = counter(body.failed);
    this.fields = fieldErrors(body.errors);
  }
}

function fieldErrors(value: unknown): Record<string, string> {
  if (!Array.isArray(value)) return {};
  const found: Record<string, string> = {};
  for (const entry of value) {
    if (
      entry &&
      typeof entry === "object" &&
      nonEmpty((entry as Record<string, unknown>).field) &&
      nonEmpty((entry as Record<string, unknown>).code)
    )
      found[(entry as Record<string, string>).field] = (
        entry as Record<string, string>
      ).code;
  }
  return found;
}

function counter(value: unknown): number | null {
  return typeof value === "number" && Number.isInteger(value) && value >= 0
    ? value
    : null;
}

function isCount(value: unknown): boolean {
  return counter(value) !== null;
}

function identifier(value: unknown): value is string {
  return (
    uuid(value) &&
    value === (value as string).toLowerCase() &&
    value.length === 36
  );
}

function nonEmpty(value: unknown): value is string {
  return typeof value === "string" && value.length > 0;
}

function decodeFailure(value: unknown): ConnectorFailure | null {
  if (value === null) return null;
  if (
    !exact(value, ERROR_FIELDS) ||
    !nonEmpty(value.code) ||
    !instant(value.at)
  )
    throw new Error(INCOMPATIBLE);
  return value as unknown as ConnectorFailure;
}

/** Sin conexión los siete campos restantes son nulos; con conexión, ninguno de los cinco lo es. */
function decodeConnection(value: unknown): GitlabConnection {
  if (
    !exact(value, CONNECTION_KEYS) ||
    !["connected", "error", "not_connected"].includes(value.status as string)
  )
    throw new Error(INCOMPATIBLE);
  const absent = value.status === "not_connected";
  if (
    absent
      ? value.apiBase !== null ||
        value.projectPath !== null ||
        value.projectId !== null ||
        value.tokenHint !== null ||
        value.lastActivityAt !== null ||
        value.lastError !== null ||
        value.version !== null
      : !nonEmpty(value.apiBase) ||
        !nonEmpty(value.projectPath) ||
        !isCount(value.projectId) ||
        !nonEmpty(value.tokenHint) ||
        !isCount(value.version) ||
        !(value.lastActivityAt === null || instant(value.lastActivityAt))
  )
    throw new Error(INCOMPATIBLE);
  return {
    ...(value as unknown as GitlabConnection),
    lastError: decodeFailure(value.lastError),
  };
}

function decodeReceipt(value: unknown): GitlabImportReceipt {
  if (
    !exact(value, RECEIPT_KEYS) ||
    !identifier(value.id) ||
    value.source !== SOURCE ||
    !identifier(value.projectId) ||
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
  return value as unknown as GitlabImportReceipt;
}

/** Convierte un problema RFC 7807 en el error tipado; un cuerpo ilegible no tapa el estado. */
async function failure(response: Response): Promise<never> {
  const body: unknown = await response.json().catch(() => null);
  throw new GitlabConnectorError(
    body && typeof body === "object" ? (body as Record<string, unknown>) : {},
  );
}

export async function readGitlabConnection(
  signal: AbortSignal,
): Promise<GitlabConnection> {
  signal.throwIfAborted();
  const response = await apiRequest(CONNECTION_URL, { signal });
  signal.throwIfAborted();
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeConnection(body);
}

export async function connectGitlab(
  input: { token: string; projectPath: string },
  signal: AbortSignal,
): Promise<GitlabConnection> {
  signal.throwIfAborted();
  const response = await apiRequest(CONNECTION_URL, {
    method: "PUT",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      token: input.token,
      projectPath: input.projectPath,
    }),
  });
  signal.throwIfAborted();
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeConnection(body);
}

export async function disconnectGitlab(signal: AbortSignal): Promise<void> {
  signal.throwIfAborted();
  const response = await apiRequest(CONNECTION_URL, {
    method: "DELETE",
    signal,
  });
  signal.throwIfAborted();
  if (response.status !== 204) return failure(response);
}

export async function startGitlabImport(
  projectId: string,
  signal: AbortSignal,
): Promise<GitlabImportReceipt> {
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

export async function readGitlabImport(
  id: string,
  signal: AbortSignal,
): Promise<GitlabImportReceipt> {
  signal.throwIfAborted();
  const response = await apiRequest(`${IMPORTS_URL}/${id}`, { signal });
  signal.throwIfAborted();
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeReceipt(body);
}
