import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";

const CATALOG_URL = "/api/v1/me/connectors";
const ROW_FIELDS = "id status lastActivityAt lastError";
const ERROR_FIELDS = "code at";
const INCOMPATIBLE = "Confirmación incompatible";

/**
 * Los seis conectores, en el orden que fija el contrato. La pantalla no los ordena: comprueba que
 * vengan así y se niega a pintar un catálogo que llegue de otra manera, porque una fila fuera de
 * sitio significa que el servidor no es el que esta versión sabe leer.
 */
export const CONNECTOR_ORDER = [
  "api_credentials",
  "webhooks",
  "ics_calendar",
  "github",
  "external_calendar",
  "gitlab",
] as const;

export type ConnectorId = (typeof CONNECTOR_ORDER)[number];

export const CONNECTOR_STATUSES = [
  "connected",
  "not_connected",
  "disabled",
  "error",
] as const;

export type ConnectorStatus = (typeof CONNECTOR_STATUSES)[number];

export type ConnectorFailure = { code: string; at: string };

export type ConnectorRow = {
  id: ConnectorId;
  status: ConnectorStatus;
  lastActivityAt: string | null;
  lastError: ConnectorFailure | null;
};

/** Un fallo del catálogo reducido a su código estable; nunca lleva texto del proveedor. */
export class CatalogError extends Error {
  readonly code: string;

  constructor(body: Record<string, unknown>) {
    super(typeof body.code === "string" ? body.code : "CATALOG_ERROR");
    this.name = "CatalogError";
    this.code = typeof body.code === "string" ? body.code : "CATALOG_ERROR";
  }
}

function decodeFailure(value: unknown): ConnectorFailure | null {
  if (value === null) return null;
  if (
    !exact(value, ERROR_FIELDS) ||
    typeof value.code !== "string" ||
    !value.code ||
    !instant(value.at)
  )
    throw new Error(INCOMPATIBLE);
  return value as unknown as ConnectorFailure;
}

function decodeRow(value: unknown, expectedId: ConnectorId): ConnectorRow {
  if (
    !exact(value, ROW_FIELDS) ||
    value.id !== expectedId ||
    !CONNECTOR_STATUSES.includes(value.status as ConnectorStatus) ||
    !(value.lastActivityAt === null || instant(value.lastActivityAt))
  )
    throw new Error(INCOMPATIBLE);
  return {
    ...(value as unknown as ConnectorRow),
    lastError: decodeFailure(value.lastError),
  };
}

function decodeCatalog(value: unknown): ConnectorRow[] {
  if (
    !exact(value, "connectors") ||
    !Array.isArray(value.connectors) ||
    value.connectors.length !== CONNECTOR_ORDER.length
  )
    throw new Error(INCOMPATIBLE);
  const rows = value.connectors;
  return CONNECTOR_ORDER.map((id, index) => decodeRow(rows[index], id));
}

/** Convierte un problema RFC 7807 en el error tipado; un cuerpo ilegible no tapa el estado. */
async function failure(response: Response): Promise<never> {
  const body: unknown = await response.json().catch(() => null);
  throw new CatalogError(
    body && typeof body === "object" ? (body as Record<string, unknown>) : {},
  );
}

export async function readConnectorCatalog(
  signal: AbortSignal,
): Promise<ConnectorRow[]> {
  signal.throwIfAborted();
  const response = await apiRequest(CATALOG_URL, { signal });
  signal.throwIfAborted();
  if (response.status !== 200) return failure(response);
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return decodeCatalog(body);
}
