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
import { apiRequest } from "./api-client";
import { exact, instant } from "./schedule-block-api";
const CATALOG_URL = stryMutAct_9fa48("0") ? "" : (stryCov_9fa48("0"), "/api/v1/me/connectors");
const ROW_FIELDS = stryMutAct_9fa48("1") ? "" : (stryCov_9fa48("1"), "id status lastActivityAt lastError");
const ERROR_FIELDS = stryMutAct_9fa48("2") ? "" : (stryCov_9fa48("2"), "code at");
const INCOMPATIBLE = stryMutAct_9fa48("3") ? "" : (stryCov_9fa48("3"), "Confirmación incompatible");

/**
 * Los seis conectores, en el orden que fija el contrato. La pantalla no los ordena: comprueba que
 * vengan así y se niega a pintar un catálogo que llegue de otra manera, porque una fila fuera de
 * sitio significa que el servidor no es el que esta versión sabe leer.
 */
export const CONNECTOR_ORDER = ["api_credentials", "webhooks", "ics_calendar", "github", "external_calendar", "gitlab"] as const;
export type ConnectorId = typeof CONNECTOR_ORDER[number];
export const CONNECTOR_STATUSES = ["connected", "not_connected", "disabled", "error"] as const;
export type ConnectorStatus = typeof CONNECTOR_STATUSES[number];
export type ConnectorFailure = {
  code: string;
  at: string;
};
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
    if (stryMutAct_9fa48("4")) {
      {}
    } else {
      stryCov_9fa48("4");
      super((stryMutAct_9fa48("7") ? typeof body.code !== "string" : stryMutAct_9fa48("6") ? false : stryMutAct_9fa48("5") ? true : (stryCov_9fa48("5", "6", "7"), typeof body.code === (stryMutAct_9fa48("8") ? "" : (stryCov_9fa48("8"), "string")))) ? body.code : stryMutAct_9fa48("9") ? "" : (stryCov_9fa48("9"), "CATALOG_ERROR"));
      this.name = stryMutAct_9fa48("10") ? "" : (stryCov_9fa48("10"), "CatalogError");
      this.code = (stryMutAct_9fa48("13") ? typeof body.code !== "string" : stryMutAct_9fa48("12") ? false : stryMutAct_9fa48("11") ? true : (stryCov_9fa48("11", "12", "13"), typeof body.code === (stryMutAct_9fa48("14") ? "" : (stryCov_9fa48("14"), "string")))) ? body.code : stryMutAct_9fa48("15") ? "" : (stryCov_9fa48("15"), "CATALOG_ERROR");
    }
  }
}
function decodeFailure(value: unknown): ConnectorFailure | null {
  if (stryMutAct_9fa48("16")) {
    {}
  } else {
    stryCov_9fa48("16");
    if (stryMutAct_9fa48("19") ? value !== null : stryMutAct_9fa48("18") ? false : stryMutAct_9fa48("17") ? true : (stryCov_9fa48("17", "18", "19"), value === null)) return null;
    if (stryMutAct_9fa48("22") ? (!exact(value, ERROR_FIELDS) || typeof value.code !== "string" || !value.code) && !instant(value.at) : stryMutAct_9fa48("21") ? false : stryMutAct_9fa48("20") ? true : (stryCov_9fa48("20", "21", "22"), (stryMutAct_9fa48("24") ? (!exact(value, ERROR_FIELDS) || typeof value.code !== "string") && !value.code : stryMutAct_9fa48("23") ? false : (stryCov_9fa48("23", "24"), (stryMutAct_9fa48("26") ? !exact(value, ERROR_FIELDS) && typeof value.code !== "string" : stryMutAct_9fa48("25") ? false : (stryCov_9fa48("25", "26"), (stryMutAct_9fa48("27") ? exact(value, ERROR_FIELDS) : (stryCov_9fa48("27"), !exact(value, ERROR_FIELDS))) || (stryMutAct_9fa48("29") ? typeof value.code === "string" : stryMutAct_9fa48("28") ? false : (stryCov_9fa48("28", "29"), typeof value.code !== (stryMutAct_9fa48("30") ? "" : (stryCov_9fa48("30"), "string")))))) || (stryMutAct_9fa48("31") ? value.code : (stryCov_9fa48("31"), !value.code)))) || (stryMutAct_9fa48("32") ? instant(value.at) : (stryCov_9fa48("32"), !instant(value.at))))) if (stryMutAct_9fa48("33")) {
      ;
    } else {
      stryCov_9fa48("33");
      throw new Error(INCOMPATIBLE);
    }
    return value as unknown as ConnectorFailure;
  }
}
function decodeRow(value: unknown, expectedId: ConnectorId): ConnectorRow {
  if (stryMutAct_9fa48("34")) {
    {}
  } else {
    stryCov_9fa48("34");
    if (stryMutAct_9fa48("37") ? (!exact(value, ROW_FIELDS) || value.id !== expectedId || !CONNECTOR_STATUSES.includes(value.status as ConnectorStatus)) && !(value.lastActivityAt === null || instant(value.lastActivityAt)) : stryMutAct_9fa48("36") ? false : stryMutAct_9fa48("35") ? true : (stryCov_9fa48("35", "36", "37"), (stryMutAct_9fa48("39") ? (!exact(value, ROW_FIELDS) || value.id !== expectedId) && !CONNECTOR_STATUSES.includes(value.status as ConnectorStatus) : stryMutAct_9fa48("38") ? false : (stryCov_9fa48("38", "39"), (stryMutAct_9fa48("41") ? !exact(value, ROW_FIELDS) && value.id !== expectedId : stryMutAct_9fa48("40") ? false : (stryCov_9fa48("40", "41"), (stryMutAct_9fa48("42") ? exact(value, ROW_FIELDS) : (stryCov_9fa48("42"), !exact(value, ROW_FIELDS))) || (stryMutAct_9fa48("44") ? value.id === expectedId : stryMutAct_9fa48("43") ? false : (stryCov_9fa48("43", "44"), value.id !== expectedId)))) || (stryMutAct_9fa48("45") ? CONNECTOR_STATUSES.includes(value.status as ConnectorStatus) : (stryCov_9fa48("45"), !CONNECTOR_STATUSES.includes(value.status as ConnectorStatus))))) || (stryMutAct_9fa48("46") ? value.lastActivityAt === null || instant(value.lastActivityAt) : (stryCov_9fa48("46"), !(stryMutAct_9fa48("49") ? value.lastActivityAt === null && instant(value.lastActivityAt) : stryMutAct_9fa48("48") ? false : stryMutAct_9fa48("47") ? true : (stryCov_9fa48("47", "48", "49"), (stryMutAct_9fa48("51") ? value.lastActivityAt !== null : stryMutAct_9fa48("50") ? false : (stryCov_9fa48("50", "51"), value.lastActivityAt === null)) || instant(value.lastActivityAt))))))) if (stryMutAct_9fa48("52")) {
      ;
    } else {
      stryCov_9fa48("52");
      throw new Error(INCOMPATIBLE);
    }
    return stryMutAct_9fa48("53") ? {} : (stryCov_9fa48("53"), {
      ...(value as unknown as ConnectorRow),
      lastError: decodeFailure(value.lastError)
    });
  }
}
function decodeCatalog(value: unknown): ConnectorRow[] {
  if (stryMutAct_9fa48("54")) {
    {}
  } else {
    stryCov_9fa48("54");
    if (stryMutAct_9fa48("57") ? (!exact(value, "connectors") || !Array.isArray(value.connectors)) && value.connectors.length !== CONNECTOR_ORDER.length : stryMutAct_9fa48("56") ? false : stryMutAct_9fa48("55") ? true : (stryCov_9fa48("55", "56", "57"), (stryMutAct_9fa48("59") ? !exact(value, "connectors") && !Array.isArray(value.connectors) : stryMutAct_9fa48("58") ? false : (stryCov_9fa48("58", "59"), (stryMutAct_9fa48("60") ? exact(value, "connectors") : (stryCov_9fa48("60"), !exact(value, stryMutAct_9fa48("61") ? "" : (stryCov_9fa48("61"), "connectors")))) || (stryMutAct_9fa48("62") ? Array.isArray(value.connectors) : (stryCov_9fa48("62"), !Array.isArray(value.connectors))))) || (stryMutAct_9fa48("64") ? value.connectors.length === CONNECTOR_ORDER.length : stryMutAct_9fa48("63") ? false : (stryCov_9fa48("63", "64"), value.connectors.length !== CONNECTOR_ORDER.length)))) if (stryMutAct_9fa48("65")) {
      ;
    } else {
      stryCov_9fa48("65");
      throw new Error(INCOMPATIBLE);
    }
    const rows = value.connectors;
    return CONNECTOR_ORDER.map(stryMutAct_9fa48("66") ? () => undefined : (stryCov_9fa48("66"), (id, index) => decodeRow(rows[index], id)));
  }
}

/** Convierte un problema RFC 7807 en el error tipado; un cuerpo ilegible no tapa el estado. */
async function failure(response: Response): Promise<never> {
  if (stryMutAct_9fa48("67")) {
    {}
  } else {
    stryCov_9fa48("67");
    const body: unknown = await response.json().catch(stryMutAct_9fa48("68") ? () => undefined : (stryCov_9fa48("68"), () => null));
    if (stryMutAct_9fa48("69")) {
      ;
    } else {
      stryCov_9fa48("69");
      throw new CatalogError((stryMutAct_9fa48("72") ? body || typeof body === "object" : stryMutAct_9fa48("71") ? false : stryMutAct_9fa48("70") ? true : (stryCov_9fa48("70", "71", "72"), body && (stryMutAct_9fa48("74") ? typeof body !== "object" : stryMutAct_9fa48("73") ? true : (stryCov_9fa48("73", "74"), typeof body === (stryMutAct_9fa48("75") ? "" : (stryCov_9fa48("75"), "object")))))) ? body as Record<string, unknown> : {});
    }
  }
}
export async function readConnectorCatalog(signal: AbortSignal): Promise<ConnectorRow[]> {
  if (stryMutAct_9fa48("76")) {
    {}
  } else {
    stryCov_9fa48("76");
    if (stryMutAct_9fa48("77")) {
      ;
    } else {
      stryCov_9fa48("77");
      signal.throwIfAborted();
    }
    const response = await apiRequest(CATALOG_URL, stryMutAct_9fa48("78") ? {} : (stryCov_9fa48("78"), {
      signal
    }));
    if (stryMutAct_9fa48("79")) {
      ;
    } else {
      stryCov_9fa48("79");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("82") ? response.status === 200 : stryMutAct_9fa48("81") ? false : stryMutAct_9fa48("80") ? true : (stryCov_9fa48("80", "81", "82"), response.status !== 200)) return failure(response);
    const body: unknown = await response.json();
    if (stryMutAct_9fa48("83")) {
      ;
    } else {
      stryCov_9fa48("83");
      signal.throwIfAborted();
    }
    return decodeCatalog(body);
  }
}