import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { RouteLink } from "./navigation";
import {
  CONNECTOR_ORDER,
  readConnectorCatalog,
  type ConnectorId,
  type ConnectorRow,
  type ConnectorStatus,
} from "./connectors-catalog-client";

/** El nombre con el que cada conector se anuncia al propietario, no su identificador. */
const NAMES: Record<ConnectorId, string> = {
  api_credentials: "API para integraciones",
  webhooks: "Webhooks",
  ics_calendar: "Calendario",
  github: "Conector de GitHub",
  external_calendar: "Calendario externo",
  gitlab: "Conector de GitLab",
};

/**
 * A dónde lleva cada fila. El valor puede ser {@code null}: una instalación puede no tener
 * desplegada la pantalla de un conector, y entonces la fila informa igual pero no enlaza a una
 * ruta que daría un 404. Se inyecta como propiedad para que esa rama tenga oráculo.
 */
export const CONNECTOR_ROUTES: Record<ConnectorId, string | null> = {
  api_credentials: "/integraciones",
  webhooks: "/webhooks",
  ics_calendar: "/calendario",
  github: "/integraciones/github",
  external_calendar: "/calendario-externo",
  gitlab: "/conectores/gitlab",
};

const STATUS_TEXT: Record<ConnectorStatus, string> = {
  connected: "Conectado",
  not_connected: "No conectado",
  disabled: "Deshabilitado",
  error: "Error",
};

/** Un glifo distinto por estado: el color nunca es la única señal. */
const STATUS_MARKER: Record<ConnectorStatus, string> = {
  connected: "●",
  not_connected: "○",
  disabled: "⊘",
  error: "▲",
};

const ERROR_TEXT: Record<string, string> = {
  CONNECTION_INVALID: "La conexión ya no es válida",
  DELIVERY_EXHAUSTED: "Se agotaron los reintentos de entrega",
  FEED_REJECTED: "La dirección del calendario ya no es válida",
  FEED_UNREACHABLE: "El calendario no responde",
  FEED_HTTP_ERROR: "El calendario respondió con un error",
  FEED_TOO_LARGE: "El calendario es demasiado grande",
  FEED_UNSUPPORTED_TYPE: "El calendario no es un archivo de texto",
  FEED_MALFORMED: "El calendario no se pudo interpretar",
  SECRET_UNREADABLE: "Hay que volver a introducir la dirección",
};

const CATALOG_ERROR_TEXT: Record<string, string> = {
  STORAGE_UNAVAILABLE: "No se pudo consultar el estado. Inténtalo más tarde",
};

function describeError(code: string): string {
  return ERROR_TEXT[code] ?? "Hay un problema con esta integración";
}

/** Sin zona explícita, {@code Intl} usa la del navegador, que es la del propietario. */
function formatMoment(value: string): string {
  return new Intl.DateTimeFormat("es", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

/**
 * Pantalla del catálogo de conectores. Sólo lee: una petición al abrir, ninguna escritura y
 * ninguna llamada a terceros. Si el catálogo no se puede leer entero no se pinta ninguna fila,
 * porque media lista es peor que ninguna: invita a creer que lo que falta está desconectado.
 */
export function ConnectorsCatalog({
  routes = CONNECTOR_ROUTES,
}: {
  routes?: Record<ConnectorId, string | null>;
}) {
  const [rows, setRows] = useState<ConnectorRow[] | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);

  useLayoutEffect(() => {
    heading.current?.focus();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let live = true;
    void (async () => {
      try {
        const catalog = await readConnectorCatalog(controller.signal);
        if (live) setRows(catalog);
      } catch (error) {
        if (!live || controller.signal.aborted) return;
        const code =
          error instanceof Error && "code" in error
            ? String((error as { code: unknown }).code)
            : "CATALOG_ERROR";
        setFailure(
          CATALOG_ERROR_TEXT[code] ??
            "No se pudo consultar el estado de las integraciones",
        );
      }
    })();
    return () => {
      live = false;
      controller.abort();
    };
  }, []);

  return (
    <main id="proyectos" className="connectors-catalog" tabIndex={-1}>
      <h1 ref={heading} tabIndex={-1}>
        Conectores
      </h1>
      {failure ? <p role="alert">{failure}</p> : null}
      {rows ? (
        <ul aria-label="Conectores">
          {rows.map((row) => (
            <Row key={row.id} row={row} route={routes[row.id]} />
          ))}
        </ul>
      ) : null}
    </main>
  );
}

function Row({ row, route }: { row: ConnectorRow; route: string | null }) {
  const status = STATUS_TEXT[row.status];
  return (
    <li>
      <h2>{NAMES[row.id]}</h2>
      <p>
        <span role="img" aria-label={status}>
          {STATUS_MARKER[row.status]}
        </span>{" "}
        <span>{status}</span>
      </p>
      {row.lastError ? <p>{describeError(row.lastError.code)}</p> : null}
      {row.lastActivityAt ? (
        <p>
          Última actividad: <span>{formatMoment(row.lastActivityAt)}</span>
        </p>
      ) : null}
      {route ? (
        <RouteLink href={route}>{NAMES[row.id]}</RouteLink>
      ) : (
        <span>No disponible</span>
      )}
    </li>
  );
}

export { CONNECTOR_ORDER };
