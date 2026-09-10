// @ts-nocheck
function stryNS_9fa48() {
  var g = typeof globalThis === 'object' && globalThis && globalThis.Math === Math && globalThis || new Function("return this")();
  var ns = g.__stryker__ || (g.__stryker__ = {});
  if (ns.activeMutant === undefined && g.process && g.process.env && g.process.env.__STRYKER_ACTIVE_MUTANT__) {
    ns.activeMutant = g.process.env.__STRYKER_ACTIVE_MUTANT__;
  }
  function retrieveNS() {
    return ns;
  }
  stryNS_9fa48 = retrieveNS;
  return retrieveNS();
}
stryNS_9fa48();
function stryCov_9fa48() {
  var ns = stryNS_9fa48();
  var cov = ns.mutantCoverage || (ns.mutantCoverage = {
    static: {},
    perTest: {}
  });
  function cover() {
    var c = cov.static;
    if (ns.currentTestId) {
      c = cov.perTest[ns.currentTestId] = cov.perTest[ns.currentTestId] || {};
    }
    var a = arguments;
    for (var i = 0; i < a.length; i++) {
      c[a[i]] = (c[a[i]] || 0) + 1;
    }
  }
  stryCov_9fa48 = cover;
  cover.apply(null, arguments);
}
function stryMutAct_9fa48(id) {
  var ns = stryNS_9fa48();
  function isActive(id) {
    if (ns.activeMutant === id) {
      if (ns.hitCount !== void 0 && ++ns.hitCount > ns.hitLimit) {
        throw new Error('Stryker: Hit count limit reached (' + ns.hitCount + ')');
      }
      return true;
    }
    return false;
  }
  stryMutAct_9fa48 = isActive;
  return isActive(id);
}
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { RouteLink } from "./navigation";
import { CONNECTOR_ORDER, readConnectorCatalog, type ConnectorId, type ConnectorRow, type ConnectorStatus } from "./connectors-catalog-client";

/** El nombre con el que cada conector se anuncia al propietario, no su identificador. */
const NAMES: Record<ConnectorId, string> = stryMutAct_9fa48("84") ? {} : (stryCov_9fa48("84"), {
  api_credentials: stryMutAct_9fa48("85") ? "" : (stryCov_9fa48("85"), "API para integraciones"),
  webhooks: stryMutAct_9fa48("86") ? "" : (stryCov_9fa48("86"), "Webhooks"),
  ics_calendar: stryMutAct_9fa48("87") ? "" : (stryCov_9fa48("87"), "Calendario"),
  github: stryMutAct_9fa48("88") ? "" : (stryCov_9fa48("88"), "Conector de GitHub"),
  external_calendar: stryMutAct_9fa48("89") ? "" : (stryCov_9fa48("89"), "Calendario externo"),
  gitlab: stryMutAct_9fa48("90") ? "" : (stryCov_9fa48("90"), "Conector de GitLab")
});

/**
 * A dónde lleva cada fila. El valor puede ser {@code null}: una instalación puede no tener
 * desplegada la pantalla de un conector, y entonces la fila informa igual pero no enlaza a una
 * ruta que daría un 404. Se inyecta como propiedad para que esa rama tenga oráculo.
 */
export const CONNECTOR_ROUTES: Record<ConnectorId, string | null> = stryMutAct_9fa48("91") ? {} : (stryCov_9fa48("91"), {
  api_credentials: stryMutAct_9fa48("92") ? "" : (stryCov_9fa48("92"), "/integraciones"),
  webhooks: stryMutAct_9fa48("93") ? "" : (stryCov_9fa48("93"), "/webhooks"),
  ics_calendar: stryMutAct_9fa48("94") ? "" : (stryCov_9fa48("94"), "/calendario"),
  github: stryMutAct_9fa48("95") ? "" : (stryCov_9fa48("95"), "/integraciones/github"),
  external_calendar: stryMutAct_9fa48("96") ? "" : (stryCov_9fa48("96"), "/calendario-externo"),
  gitlab: stryMutAct_9fa48("97") ? "" : (stryCov_9fa48("97"), "/conectores/gitlab")
});
const STATUS_TEXT: Record<ConnectorStatus, string> = stryMutAct_9fa48("98") ? {} : (stryCov_9fa48("98"), {
  connected: stryMutAct_9fa48("99") ? "" : (stryCov_9fa48("99"), "Conectado"),
  not_connected: stryMutAct_9fa48("100") ? "" : (stryCov_9fa48("100"), "No conectado"),
  disabled: stryMutAct_9fa48("101") ? "" : (stryCov_9fa48("101"), "Deshabilitado"),
  error: stryMutAct_9fa48("102") ? "" : (stryCov_9fa48("102"), "Error")
});

/** Un glifo distinto por estado: el color nunca es la única señal. */
const STATUS_MARKER: Record<ConnectorStatus, string> = stryMutAct_9fa48("103") ? {} : (stryCov_9fa48("103"), {
  connected: stryMutAct_9fa48("104") ? "" : (stryCov_9fa48("104"), "●"),
  not_connected: stryMutAct_9fa48("105") ? "" : (stryCov_9fa48("105"), "○"),
  disabled: stryMutAct_9fa48("106") ? "" : (stryCov_9fa48("106"), "⊘"),
  error: stryMutAct_9fa48("107") ? "" : (stryCov_9fa48("107"), "▲")
});
const ERROR_TEXT: Record<string, string> = stryMutAct_9fa48("108") ? {} : (stryCov_9fa48("108"), {
  CONNECTION_INVALID: stryMutAct_9fa48("109") ? "" : (stryCov_9fa48("109"), "La conexión ya no es válida"),
  DELIVERY_EXHAUSTED: stryMutAct_9fa48("110") ? "" : (stryCov_9fa48("110"), "Se agotaron los reintentos de entrega"),
  FEED_REJECTED: stryMutAct_9fa48("111") ? "" : (stryCov_9fa48("111"), "La dirección del calendario ya no es válida"),
  FEED_UNREACHABLE: stryMutAct_9fa48("112") ? "" : (stryCov_9fa48("112"), "El calendario no responde"),
  FEED_HTTP_ERROR: stryMutAct_9fa48("113") ? "" : (stryCov_9fa48("113"), "El calendario respondió con un error"),
  FEED_TOO_LARGE: stryMutAct_9fa48("114") ? "" : (stryCov_9fa48("114"), "El calendario es demasiado grande"),
  FEED_UNSUPPORTED_TYPE: stryMutAct_9fa48("115") ? "" : (stryCov_9fa48("115"), "El calendario no es un archivo de texto"),
  FEED_MALFORMED: stryMutAct_9fa48("116") ? "" : (stryCov_9fa48("116"), "El calendario no se pudo interpretar"),
  SECRET_UNREADABLE: stryMutAct_9fa48("117") ? "" : (stryCov_9fa48("117"), "Hay que volver a introducir la dirección")
});
const CATALOG_ERROR_TEXT: Record<string, string> = stryMutAct_9fa48("118") ? {} : (stryCov_9fa48("118"), {
  STORAGE_UNAVAILABLE: stryMutAct_9fa48("119") ? "" : (stryCov_9fa48("119"), "No se pudo consultar el estado. Inténtalo más tarde")
});
function describeError(code: string): string {
  if (stryMutAct_9fa48("120")) {
    {}
  } else {
    stryCov_9fa48("120");
    return stryMutAct_9fa48("121") ? ERROR_TEXT[code] && "Hay un problema con esta integración" : (stryCov_9fa48("121"), ERROR_TEXT[code] ?? (stryMutAct_9fa48("122") ? "" : (stryCov_9fa48("122"), "Hay un problema con esta integración")));
  }
}

/** Sin zona explícita, {@code Intl} usa la del navegador, que es la del propietario. */
function formatMoment(value: string): string {
  if (stryMutAct_9fa48("123")) {
    {}
  } else {
    stryCov_9fa48("123");
    return new Intl.DateTimeFormat(stryMutAct_9fa48("124") ? "" : (stryCov_9fa48("124"), "es"), stryMutAct_9fa48("125") ? {} : (stryCov_9fa48("125"), {
      dateStyle: stryMutAct_9fa48("126") ? "" : (stryCov_9fa48("126"), "medium"),
      timeStyle: stryMutAct_9fa48("127") ? "" : (stryCov_9fa48("127"), "short")
    })).format(new Date(value));
  }
}

/**
 * Pantalla del catálogo de conectores. Sólo lee: una petición al abrir, ninguna escritura y
 * ninguna llamada a terceros. Si el catálogo no se puede leer entero no se pinta ninguna fila,
 * porque media lista es peor que ninguna: invita a creer que lo que falta está desconectado.
 */
export function ConnectorsCatalog({
  routes = CONNECTOR_ROUTES
}: {
  routes?: Record<ConnectorId, string | null>;
}) {
  if (stryMutAct_9fa48("128")) {
    {}
  } else {
    stryCov_9fa48("128");
    const [rows, setRows] = useState<ConnectorRow[] | null>(null);
    const [failure, setFailure] = useState<string | null>(null);
    const heading = useRef<HTMLHeadingElement>(null);
    useLayoutEffect(() => {
      if (stryMutAct_9fa48("130")) {
        {}
      } else {
        stryCov_9fa48("130");
        stryMutAct_9fa48("131") ? heading.current.focus() : (stryCov_9fa48("131"), heading.current?.focus());
      }
    }, stryMutAct_9fa48("132") ? ["Stryker was here"] : (stryCov_9fa48("132"), []));
    if (stryMutAct_9fa48("133")) {
      ;
    } else {
      stryCov_9fa48("133");
      useEffect(() => {
        if (stryMutAct_9fa48("134")) {
          {}
        } else {
          stryCov_9fa48("134");
          const controller = new AbortController();
          let live = stryMutAct_9fa48("135") ? false : (stryCov_9fa48("135"), true);
          void (async () => {
            if (stryMutAct_9fa48("136")) {
              {}
            } else {
              stryCov_9fa48("136");
              try {
                if (stryMutAct_9fa48("137")) {
                  {}
                } else {
                  stryCov_9fa48("137");
                  const catalog = await readConnectorCatalog(controller.signal);
                  if (stryMutAct_9fa48("139") ? false : stryMutAct_9fa48("138") ? true : (stryCov_9fa48("138", "139"), live)) if (stryMutAct_9fa48("140")) {
                    ;
                  } else {
                    stryCov_9fa48("140");
                    setRows(catalog);
                  }
                }
              } catch (error) {
                if (stryMutAct_9fa48("141")) {
                  {}
                } else {
                  stryCov_9fa48("141");
                  if (stryMutAct_9fa48("144") ? !live && controller.signal.aborted : stryMutAct_9fa48("143") ? false : stryMutAct_9fa48("142") ? true : (stryCov_9fa48("142", "143", "144"), (stryMutAct_9fa48("145") ? live : (stryCov_9fa48("145"), !live)) || controller.signal.aborted)) return;
                  const code = (stryMutAct_9fa48("148") ? error instanceof Error || "code" in error : stryMutAct_9fa48("147") ? false : stryMutAct_9fa48("146") ? true : (stryCov_9fa48("146", "147", "148"), error instanceof Error && (stryMutAct_9fa48("149") ? "" : (stryCov_9fa48("149"), "code")) in error)) ? String((error as {
                    code: unknown;
                  }).code) : stryMutAct_9fa48("150") ? "" : (stryCov_9fa48("150"), "CATALOG_ERROR");
                  setFailure(stryMutAct_9fa48("152") ? CATALOG_ERROR_TEXT[code] && "No se pudo consultar el estado de las integraciones" : (stryCov_9fa48("152"), CATALOG_ERROR_TEXT[code] ?? (stryMutAct_9fa48("153") ? "" : (stryCov_9fa48("153"), "No se pudo consultar el estado de las integraciones"))));
                }
              }
            }
          })();
          return () => {
            if (stryMutAct_9fa48("154")) {
              {}
            } else {
              stryCov_9fa48("154");
              live = stryMutAct_9fa48("155") ? true : (stryCov_9fa48("155"), false);
              if (stryMutAct_9fa48("156")) {
                ;
              } else {
                stryCov_9fa48("156");
                controller.abort();
              }
            }
          };
        }
      }, stryMutAct_9fa48("157") ? ["Stryker was here"] : (stryCov_9fa48("157"), []));
    }
    return <main id="proyectos" className="connectors-catalog" tabIndex={stryMutAct_9fa48("158") ? +1 : (stryCov_9fa48("158"), -1)}>
      <h1 ref={heading} tabIndex={stryMutAct_9fa48("159") ? +1 : (stryCov_9fa48("159"), -1)}>
        Conectores
      </h1>
      {failure ? <p role="alert">{failure}</p> : null}
      {rows ? <ul aria-label="Conectores">
          {rows.map(stryMutAct_9fa48("160") ? () => undefined : (stryCov_9fa48("160"), row => <Row key={row.id} row={row} route={routes[row.id]} />))}
        </ul> : null}
    </main>;
  }
}
function Row({
  row,
  route
}: {
  row: ConnectorRow;
  route: string | null;
}) {
  if (stryMutAct_9fa48("161")) {
    {}
  } else {
    stryCov_9fa48("161");
    const status = STATUS_TEXT[row.status];
    return <li>
      <h2>{NAMES[row.id]}</h2>
      <p>
        <span role="img" aria-label={status}>
          {STATUS_MARKER[row.status]}
        </span>{stryMutAct_9fa48("162") ? "" : (stryCov_9fa48("162"), " ")}
        <span>{status}</span>
      </p>
      {row.lastError ? <p>{describeError(row.lastError.code)}</p> : null}
      {row.lastActivityAt ? <p>
          Última actividad: <span>{formatMoment(row.lastActivityAt)}</span>
        </p> : null}
      {route ? <RouteLink href={route}>{NAMES[row.id]}</RouteLink> : <span>No disponible</span>}
    </li>;
  }
}
export { CONNECTOR_ORDER };