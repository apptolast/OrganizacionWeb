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
import { exact, instant, uuid } from "./schedule-block-api";
import { microseconds } from "./work-session-api";
const CONNECTION_URL = stryMutAct_9fa48("163") ? "" : (stryCov_9fa48("163"), "/api/v1/me/connectors/gitlab");
const IMPORTS_URL = stryMutAct_9fa48("164") ? `` : (stryCov_9fa48("164"), `${CONNECTION_URL}/imports`);
export const CONNECTION_KEYS = stryMutAct_9fa48("165") ? "" : (stryCov_9fa48("165"), "status apiBase projectPath projectId tokenHint lastActivityAt lastError version");
export const RECEIPT_KEYS = stryMutAct_9fa48("166") ? "" : (stryCov_9fa48("166"), "id source projectId projectPath status created skipped failed truncated errorCode startedAt finishedAt");
export const ERROR_FIELDS = stryMutAct_9fa48("167") ? "" : (stryCov_9fa48("167"), "code at");
const INCOMPATIBLE = stryMutAct_9fa48("168") ? "" : (stryCov_9fa48("168"), "Confirmación incompatible");
const SOURCE = stryMutAct_9fa48("169") ? "" : (stryCov_9fa48("169"), "gitlab");
export type ConnectorFailure = {
  code: string;
  at: string;
};
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
    if (stryMutAct_9fa48("170")) {
      {}
    } else {
      stryCov_9fa48("170");
      super((stryMutAct_9fa48("173") ? typeof body.code !== "string" : stryMutAct_9fa48("172") ? false : stryMutAct_9fa48("171") ? true : (stryCov_9fa48("171", "172", "173"), typeof body.code === (stryMutAct_9fa48("174") ? "" : (stryCov_9fa48("174"), "string")))) ? body.code : stryMutAct_9fa48("175") ? "" : (stryCov_9fa48("175"), "CONNECTOR_ERROR"));
      this.name = stryMutAct_9fa48("176") ? "" : (stryCov_9fa48("176"), "GitlabConnectorError");
      this.code = (stryMutAct_9fa48("179") ? typeof body.code !== "string" : stryMutAct_9fa48("178") ? false : stryMutAct_9fa48("177") ? true : (stryCov_9fa48("177", "178", "179"), typeof body.code === (stryMutAct_9fa48("180") ? "" : (stryCov_9fa48("180"), "string")))) ? body.code : stryMutAct_9fa48("181") ? "" : (stryCov_9fa48("181"), "CONNECTOR_ERROR");
      this.retryAfterSeconds = counter(body.retryAfterSeconds);
      this.importId = identifier(body.importId) ? body.importId : null;
      this.created = counter(body.created);
      this.skipped = counter(body.skipped);
      this.failed = counter(body.failed);
      this.fields = fieldErrors(body.errors);
    }
  }
}
function fieldErrors(value: unknown): Record<string, string> {
  if (stryMutAct_9fa48("182")) {
    {}
  } else {
    stryCov_9fa48("182");
    if (stryMutAct_9fa48("185") ? false : stryMutAct_9fa48("184") ? true : stryMutAct_9fa48("183") ? Array.isArray(value) : (stryCov_9fa48("183", "184", "185"), !Array.isArray(value))) return {};
    const found: Record<string, string> = {};
    for (const entry of value) {
      if (stryMutAct_9fa48("186")) {
        {}
      } else {
        stryCov_9fa48("186");
        if (stryMutAct_9fa48("189") ? entry && typeof entry === "object" && nonEmpty((entry as Record<string, unknown>).field) || nonEmpty((entry as Record<string, unknown>).code) : stryMutAct_9fa48("188") ? false : stryMutAct_9fa48("187") ? true : (stryCov_9fa48("187", "188", "189"), (stryMutAct_9fa48("191") ? entry && typeof entry === "object" || nonEmpty((entry as Record<string, unknown>).field) : stryMutAct_9fa48("190") ? true : (stryCov_9fa48("190", "191"), (stryMutAct_9fa48("193") ? entry || typeof entry === "object" : stryMutAct_9fa48("192") ? true : (stryCov_9fa48("192", "193"), entry && (stryMutAct_9fa48("195") ? typeof entry !== "object" : stryMutAct_9fa48("194") ? true : (stryCov_9fa48("194", "195"), typeof entry === (stryMutAct_9fa48("196") ? "" : (stryCov_9fa48("196"), "object")))))) && nonEmpty((entry as Record<string, unknown>).field))) && nonEmpty((entry as Record<string, unknown>).code))) found[(entry as Record<string, string>).field] = (entry as Record<string, string>).code;
      }
    }
    return found;
  }
}
function counter(value: unknown): number | null {
  if (stryMutAct_9fa48("197")) {
    {}
  } else {
    stryCov_9fa48("197");
    return (stryMutAct_9fa48("200") ? typeof value === "number" && Number.isInteger(value) || value >= 0 : stryMutAct_9fa48("199") ? false : stryMutAct_9fa48("198") ? true : (stryCov_9fa48("198", "199", "200"), (stryMutAct_9fa48("202") ? typeof value === "number" || Number.isInteger(value) : stryMutAct_9fa48("201") ? true : (stryCov_9fa48("201", "202"), (stryMutAct_9fa48("204") ? typeof value !== "number" : stryMutAct_9fa48("203") ? true : (stryCov_9fa48("203", "204"), typeof value === (stryMutAct_9fa48("205") ? "" : (stryCov_9fa48("205"), "number")))) && Number.isInteger(value))) && (stryMutAct_9fa48("208") ? value < 0 : stryMutAct_9fa48("207") ? value > 0 : stryMutAct_9fa48("206") ? true : (stryCov_9fa48("206", "207", "208"), value >= 0)))) ? value : null;
  }
}
function isCount(value: unknown): boolean {
  if (stryMutAct_9fa48("209")) {
    {}
  } else {
    stryCov_9fa48("209");
    return stryMutAct_9fa48("212") ? counter(value) === null : stryMutAct_9fa48("211") ? false : stryMutAct_9fa48("210") ? true : (stryCov_9fa48("210", "211", "212"), counter(value) !== null);
  }
}
function identifier(value: unknown): value is string {
  if (stryMutAct_9fa48("213")) {
    {}
  } else {
    stryCov_9fa48("213");
    return stryMutAct_9fa48("216") ? uuid(value) && value === (value as string).toLowerCase() || value.length === 36 : stryMutAct_9fa48("215") ? false : stryMutAct_9fa48("214") ? true : (stryCov_9fa48("214", "215", "216"), (stryMutAct_9fa48("218") ? uuid(value) || value === (value as string).toLowerCase() : stryMutAct_9fa48("217") ? true : (stryCov_9fa48("217", "218"), uuid(value) && (stryMutAct_9fa48("220") ? value !== (value as string).toLowerCase() : stryMutAct_9fa48("219") ? true : (stryCov_9fa48("219", "220"), value === (stryMutAct_9fa48("221") ? (value as string).toUpperCase() : (stryCov_9fa48("221"), (value as string).toLowerCase())))))) && (stryMutAct_9fa48("223") ? value.length !== 36 : stryMutAct_9fa48("222") ? true : (stryCov_9fa48("222", "223"), value.length === 36)));
  }
}
function nonEmpty(value: unknown): value is string {
  if (stryMutAct_9fa48("224")) {
    {}
  } else {
    stryCov_9fa48("224");
    return stryMutAct_9fa48("227") ? typeof value === "string" || value.length > 0 : stryMutAct_9fa48("226") ? false : stryMutAct_9fa48("225") ? true : (stryCov_9fa48("225", "226", "227"), (stryMutAct_9fa48("229") ? typeof value !== "string" : stryMutAct_9fa48("228") ? true : (stryCov_9fa48("228", "229"), typeof value === (stryMutAct_9fa48("230") ? "" : (stryCov_9fa48("230"), "string")))) && (stryMutAct_9fa48("233") ? value.length <= 0 : stryMutAct_9fa48("232") ? value.length >= 0 : stryMutAct_9fa48("231") ? true : (stryCov_9fa48("231", "232", "233"), value.length > 0)));
  }
}
function decodeFailure(value: unknown): ConnectorFailure | null {
  if (stryMutAct_9fa48("234")) {
    {}
  } else {
    stryCov_9fa48("234");
    if (stryMutAct_9fa48("237") ? value !== null : stryMutAct_9fa48("236") ? false : stryMutAct_9fa48("235") ? true : (stryCov_9fa48("235", "236", "237"), value === null)) return null;
    if (stryMutAct_9fa48("240") ? (!exact(value, ERROR_FIELDS) || !nonEmpty(value.code)) && !instant(value.at) : stryMutAct_9fa48("239") ? false : stryMutAct_9fa48("238") ? true : (stryCov_9fa48("238", "239", "240"), (stryMutAct_9fa48("242") ? !exact(value, ERROR_FIELDS) && !nonEmpty(value.code) : stryMutAct_9fa48("241") ? false : (stryCov_9fa48("241", "242"), (stryMutAct_9fa48("243") ? exact(value, ERROR_FIELDS) : (stryCov_9fa48("243"), !exact(value, ERROR_FIELDS))) || (stryMutAct_9fa48("244") ? nonEmpty(value.code) : (stryCov_9fa48("244"), !nonEmpty(value.code))))) || (stryMutAct_9fa48("245") ? instant(value.at) : (stryCov_9fa48("245"), !instant(value.at))))) if (stryMutAct_9fa48("246")) {
      ;
    } else {
      stryCov_9fa48("246");
      throw new Error(INCOMPATIBLE);
    }
    return value as unknown as ConnectorFailure;
  }
}

/** Sin conexión los siete campos restantes son nulos; con conexión, ninguno de los cinco lo es. */
function decodeConnection(value: unknown): GitlabConnection {
  if (stryMutAct_9fa48("247")) {
    {}
  } else {
    stryCov_9fa48("247");
    if (stryMutAct_9fa48("250") ? !exact(value, CONNECTION_KEYS) && !["connected", "error", "not_connected"].includes(value.status as string) : stryMutAct_9fa48("249") ? false : stryMutAct_9fa48("248") ? true : (stryCov_9fa48("248", "249", "250"), (stryMutAct_9fa48("251") ? exact(value, CONNECTION_KEYS) : (stryCov_9fa48("251"), !exact(value, CONNECTION_KEYS))) || (stryMutAct_9fa48("252") ? ["connected", "error", "not_connected"].includes(value.status as string) : (stryCov_9fa48("252"), !(stryMutAct_9fa48("253") ? [] : (stryCov_9fa48("253"), [stryMutAct_9fa48("254") ? "" : (stryCov_9fa48("254"), "connected"), stryMutAct_9fa48("255") ? "" : (stryCov_9fa48("255"), "error"), stryMutAct_9fa48("256") ? "" : (stryCov_9fa48("256"), "not_connected")])).includes(value.status as string))))) if (stryMutAct_9fa48("257")) {
      ;
    } else {
      stryCov_9fa48("257");
      throw new Error(INCOMPATIBLE);
    }
    const absent = stryMutAct_9fa48("260") ? value.status !== "not_connected" : stryMutAct_9fa48("259") ? false : stryMutAct_9fa48("258") ? true : (stryCov_9fa48("258", "259", "260"), value.status === (stryMutAct_9fa48("261") ? "" : (stryCov_9fa48("261"), "not_connected")));
    if (stryMutAct_9fa48("263") ? false : stryMutAct_9fa48("262") ? true : (stryCov_9fa48("262", "263"), absent ? stryMutAct_9fa48("266") ? (value.apiBase !== null || value.projectPath !== null || value.projectId !== null || value.tokenHint !== null || value.lastActivityAt !== null || value.lastError !== null) && value.version !== null : stryMutAct_9fa48("265") ? false : stryMutAct_9fa48("264") ? true : (stryCov_9fa48("264", "265", "266"), (stryMutAct_9fa48("268") ? (value.apiBase !== null || value.projectPath !== null || value.projectId !== null || value.tokenHint !== null || value.lastActivityAt !== null) && value.lastError !== null : stryMutAct_9fa48("267") ? false : (stryCov_9fa48("267", "268"), (stryMutAct_9fa48("270") ? (value.apiBase !== null || value.projectPath !== null || value.projectId !== null || value.tokenHint !== null) && value.lastActivityAt !== null : stryMutAct_9fa48("269") ? false : (stryCov_9fa48("269", "270"), (stryMutAct_9fa48("272") ? (value.apiBase !== null || value.projectPath !== null || value.projectId !== null) && value.tokenHint !== null : stryMutAct_9fa48("271") ? false : (stryCov_9fa48("271", "272"), (stryMutAct_9fa48("274") ? (value.apiBase !== null || value.projectPath !== null) && value.projectId !== null : stryMutAct_9fa48("273") ? false : (stryCov_9fa48("273", "274"), (stryMutAct_9fa48("276") ? value.apiBase !== null && value.projectPath !== null : stryMutAct_9fa48("275") ? false : (stryCov_9fa48("275", "276"), (stryMutAct_9fa48("278") ? value.apiBase === null : stryMutAct_9fa48("277") ? false : (stryCov_9fa48("277", "278"), value.apiBase !== null)) || (stryMutAct_9fa48("280") ? value.projectPath === null : stryMutAct_9fa48("279") ? false : (stryCov_9fa48("279", "280"), value.projectPath !== null)))) || (stryMutAct_9fa48("282") ? value.projectId === null : stryMutAct_9fa48("281") ? false : (stryCov_9fa48("281", "282"), value.projectId !== null)))) || (stryMutAct_9fa48("284") ? value.tokenHint === null : stryMutAct_9fa48("283") ? false : (stryCov_9fa48("283", "284"), value.tokenHint !== null)))) || (stryMutAct_9fa48("286") ? value.lastActivityAt === null : stryMutAct_9fa48("285") ? false : (stryCov_9fa48("285", "286"), value.lastActivityAt !== null)))) || (stryMutAct_9fa48("288") ? value.lastError === null : stryMutAct_9fa48("287") ? false : (stryCov_9fa48("287", "288"), value.lastError !== null)))) || (stryMutAct_9fa48("290") ? value.version === null : stryMutAct_9fa48("289") ? false : (stryCov_9fa48("289", "290"), value.version !== null))) : stryMutAct_9fa48("293") ? (!nonEmpty(value.apiBase) || !nonEmpty(value.projectPath) || !isCount(value.projectId) || !nonEmpty(value.tokenHint) || !isCount(value.version)) && !(value.lastActivityAt === null || instant(value.lastActivityAt)) : stryMutAct_9fa48("292") ? false : stryMutAct_9fa48("291") ? true : (stryCov_9fa48("291", "292", "293"), (stryMutAct_9fa48("295") ? (!nonEmpty(value.apiBase) || !nonEmpty(value.projectPath) || !isCount(value.projectId) || !nonEmpty(value.tokenHint)) && !isCount(value.version) : stryMutAct_9fa48("294") ? false : (stryCov_9fa48("294", "295"), (stryMutAct_9fa48("297") ? (!nonEmpty(value.apiBase) || !nonEmpty(value.projectPath) || !isCount(value.projectId)) && !nonEmpty(value.tokenHint) : stryMutAct_9fa48("296") ? false : (stryCov_9fa48("296", "297"), (stryMutAct_9fa48("299") ? (!nonEmpty(value.apiBase) || !nonEmpty(value.projectPath)) && !isCount(value.projectId) : stryMutAct_9fa48("298") ? false : (stryCov_9fa48("298", "299"), (stryMutAct_9fa48("301") ? !nonEmpty(value.apiBase) && !nonEmpty(value.projectPath) : stryMutAct_9fa48("300") ? false : (stryCov_9fa48("300", "301"), (stryMutAct_9fa48("302") ? nonEmpty(value.apiBase) : (stryCov_9fa48("302"), !nonEmpty(value.apiBase))) || (stryMutAct_9fa48("303") ? nonEmpty(value.projectPath) : (stryCov_9fa48("303"), !nonEmpty(value.projectPath))))) || (stryMutAct_9fa48("304") ? isCount(value.projectId) : (stryCov_9fa48("304"), !isCount(value.projectId))))) || (stryMutAct_9fa48("305") ? nonEmpty(value.tokenHint) : (stryCov_9fa48("305"), !nonEmpty(value.tokenHint))))) || (stryMutAct_9fa48("306") ? isCount(value.version) : (stryCov_9fa48("306"), !isCount(value.version))))) || (stryMutAct_9fa48("307") ? value.lastActivityAt === null || instant(value.lastActivityAt) : (stryCov_9fa48("307"), !(stryMutAct_9fa48("310") ? value.lastActivityAt === null && instant(value.lastActivityAt) : stryMutAct_9fa48("309") ? false : stryMutAct_9fa48("308") ? true : (stryCov_9fa48("308", "309", "310"), (stryMutAct_9fa48("312") ? value.lastActivityAt !== null : stryMutAct_9fa48("311") ? false : (stryCov_9fa48("311", "312"), value.lastActivityAt === null)) || instant(value.lastActivityAt)))))))) if (stryMutAct_9fa48("313")) {
      ;
    } else {
      stryCov_9fa48("313");
      throw new Error(INCOMPATIBLE);
    }
    return stryMutAct_9fa48("314") ? {} : (stryCov_9fa48("314"), {
      ...(value as unknown as GitlabConnection),
      lastError: decodeFailure(value.lastError)
    });
  }
}
function decodeReceipt(value: unknown): GitlabImportReceipt {
  if (stryMutAct_9fa48("315")) {
    {}
  } else {
    stryCov_9fa48("315");
    if (stryMutAct_9fa48("318") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped) || !isCount(value.failed) || typeof value.truncated !== "boolean" || !instant(value.startedAt) || !(value.errorCode === null || nonEmpty(value.errorCode)) ||
    // Sólo un recibo en curso carece de final, y sólo uno en curso carece de error.
    value.status === "running" !== (value.finishedAt === null) || value.status === "running" && value.errorCode !== null) && value.finishedAt !== null && (!instant(value.finishedAt) || microseconds(value.finishedAt)! < microseconds(value.startedAt)!) : stryMutAct_9fa48("317") ? false : stryMutAct_9fa48("316") ? true : (stryCov_9fa48("316", "317", "318"), (stryMutAct_9fa48("320") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped) || !isCount(value.failed) || typeof value.truncated !== "boolean" || !instant(value.startedAt) || !(value.errorCode === null || nonEmpty(value.errorCode)) ||
    // Sólo un recibo en curso carece de final, y sólo uno en curso carece de error.
    value.status === "running" !== (value.finishedAt === null)) && value.status === "running" && value.errorCode !== null : stryMutAct_9fa48("319") ? false : (stryCov_9fa48("319", "320"), (stryMutAct_9fa48("322") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped) || !isCount(value.failed) || typeof value.truncated !== "boolean" || !instant(value.startedAt) || !(value.errorCode === null || nonEmpty(value.errorCode))) &&
    // Sólo un recibo en curso carece de final, y sólo uno en curso carece de error.
    value.status === "running" !== (value.finishedAt === null) : stryMutAct_9fa48("321") ? false : (stryCov_9fa48("321", "322"), (stryMutAct_9fa48("324") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped) || !isCount(value.failed) || typeof value.truncated !== "boolean" || !instant(value.startedAt)) && !(value.errorCode === null || nonEmpty(value.errorCode)) : stryMutAct_9fa48("323") ? false : (stryCov_9fa48("323", "324"), (stryMutAct_9fa48("326") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped) || !isCount(value.failed) || typeof value.truncated !== "boolean") && !instant(value.startedAt) : stryMutAct_9fa48("325") ? false : (stryCov_9fa48("325", "326"), (stryMutAct_9fa48("328") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped) || !isCount(value.failed)) && typeof value.truncated !== "boolean" : stryMutAct_9fa48("327") ? false : (stryCov_9fa48("327", "328"), (stryMutAct_9fa48("330") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created) || !isCount(value.skipped)) && !isCount(value.failed) : stryMutAct_9fa48("329") ? false : (stryCov_9fa48("329", "330"), (stryMutAct_9fa48("332") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string) || !isCount(value.created)) && !isCount(value.skipped) : stryMutAct_9fa48("331") ? false : (stryCov_9fa48("331", "332"), (stryMutAct_9fa48("334") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath) || !["running", "completed", "failed"].includes(value.status as string)) && !isCount(value.created) : stryMutAct_9fa48("333") ? false : (stryCov_9fa48("333", "334"), (stryMutAct_9fa48("336") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId) || !nonEmpty(value.projectPath)) && !["running", "completed", "failed"].includes(value.status as string) : stryMutAct_9fa48("335") ? false : (stryCov_9fa48("335", "336"), (stryMutAct_9fa48("338") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE || !identifier(value.projectId)) && !nonEmpty(value.projectPath) : stryMutAct_9fa48("337") ? false : (stryCov_9fa48("337", "338"), (stryMutAct_9fa48("340") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id) || value.source !== SOURCE) && !identifier(value.projectId) : stryMutAct_9fa48("339") ? false : (stryCov_9fa48("339", "340"), (stryMutAct_9fa48("342") ? (!exact(value, RECEIPT_KEYS) || !identifier(value.id)) && value.source !== SOURCE : stryMutAct_9fa48("341") ? false : (stryCov_9fa48("341", "342"), (stryMutAct_9fa48("344") ? !exact(value, RECEIPT_KEYS) && !identifier(value.id) : stryMutAct_9fa48("343") ? false : (stryCov_9fa48("343", "344"), (stryMutAct_9fa48("345") ? exact(value, RECEIPT_KEYS) : (stryCov_9fa48("345"), !exact(value, RECEIPT_KEYS))) || (stryMutAct_9fa48("346") ? identifier(value.id) : (stryCov_9fa48("346"), !identifier(value.id))))) || (stryMutAct_9fa48("348") ? value.source === SOURCE : stryMutAct_9fa48("347") ? false : (stryCov_9fa48("347", "348"), value.source !== SOURCE)))) || (stryMutAct_9fa48("349") ? identifier(value.projectId) : (stryCov_9fa48("349"), !identifier(value.projectId))))) || (stryMutAct_9fa48("350") ? nonEmpty(value.projectPath) : (stryCov_9fa48("350"), !nonEmpty(value.projectPath))))) || (stryMutAct_9fa48("351") ? ["running", "completed", "failed"].includes(value.status as string) : (stryCov_9fa48("351"), !(stryMutAct_9fa48("352") ? [] : (stryCov_9fa48("352"), [stryMutAct_9fa48("353") ? "" : (stryCov_9fa48("353"), "running"), stryMutAct_9fa48("354") ? "" : (stryCov_9fa48("354"), "completed"), stryMutAct_9fa48("355") ? "" : (stryCov_9fa48("355"), "failed")])).includes(value.status as string))))) || (stryMutAct_9fa48("356") ? isCount(value.created) : (stryCov_9fa48("356"), !isCount(value.created))))) || (stryMutAct_9fa48("357") ? isCount(value.skipped) : (stryCov_9fa48("357"), !isCount(value.skipped))))) || (stryMutAct_9fa48("358") ? isCount(value.failed) : (stryCov_9fa48("358"), !isCount(value.failed))))) || (stryMutAct_9fa48("360") ? typeof value.truncated === "boolean" : stryMutAct_9fa48("359") ? false : (stryCov_9fa48("359", "360"), typeof value.truncated !== (stryMutAct_9fa48("361") ? "" : (stryCov_9fa48("361"), "boolean")))))) || (stryMutAct_9fa48("362") ? instant(value.startedAt) : (stryCov_9fa48("362"), !instant(value.startedAt))))) || (stryMutAct_9fa48("363") ? value.errorCode === null || nonEmpty(value.errorCode) : (stryCov_9fa48("363"), !(stryMutAct_9fa48("366") ? value.errorCode === null && nonEmpty(value.errorCode) : stryMutAct_9fa48("365") ? false : stryMutAct_9fa48("364") ? true : (stryCov_9fa48("364", "365", "366"), (stryMutAct_9fa48("368") ? value.errorCode !== null : stryMutAct_9fa48("367") ? false : (stryCov_9fa48("367", "368"), value.errorCode === null)) || nonEmpty(value.errorCode))))))) || (// Sólo un recibo en curso carece de final, y sólo uno en curso carece de error.
    stryMutAct_9fa48("370") ?
    // Sólo un recibo en curso carece de final, y sólo uno en curso carece de error.
    value.status === "running" === (value.finishedAt === null) : stryMutAct_9fa48("369") ? false : (stryCov_9fa48("369", "370"), (stryMutAct_9fa48("373") ? value.status !== "running" : stryMutAct_9fa48("372") ? false : stryMutAct_9fa48("371") ? true : (stryCov_9fa48("371", "372", "373"), value.status === (stryMutAct_9fa48("374") ? "" : (stryCov_9fa48("374"), "running")))) !== (stryMutAct_9fa48("377") ? value.finishedAt !== null : stryMutAct_9fa48("376") ? false : stryMutAct_9fa48("375") ? true : (stryCov_9fa48("375", "376", "377"), value.finishedAt === null)))))) || (stryMutAct_9fa48("379") ? value.status === "running" || value.errorCode !== null : stryMutAct_9fa48("378") ? false : (stryCov_9fa48("378", "379"), (stryMutAct_9fa48("381") ? value.status !== "running" : stryMutAct_9fa48("380") ? true : (stryCov_9fa48("380", "381"), value.status === (stryMutAct_9fa48("382") ? "" : (stryCov_9fa48("382"), "running")))) && (stryMutAct_9fa48("384") ? value.errorCode === null : stryMutAct_9fa48("383") ? true : (stryCov_9fa48("383", "384"), value.errorCode !== null)))))) || (stryMutAct_9fa48("386") ? value.finishedAt !== null || !instant(value.finishedAt) || microseconds(value.finishedAt)! < microseconds(value.startedAt)! : stryMutAct_9fa48("385") ? false : (stryCov_9fa48("385", "386"), (stryMutAct_9fa48("388") ? value.finishedAt === null : stryMutAct_9fa48("387") ? true : (stryCov_9fa48("387", "388"), value.finishedAt !== null)) && (stryMutAct_9fa48("390") ? !instant(value.finishedAt) && microseconds(value.finishedAt)! < microseconds(value.startedAt)! : stryMutAct_9fa48("389") ? true : (stryCov_9fa48("389", "390"), (stryMutAct_9fa48("391") ? instant(value.finishedAt) : (stryCov_9fa48("391"), !instant(value.finishedAt))) || (stryMutAct_9fa48("394") ? microseconds(value.finishedAt)! >= microseconds(value.startedAt)! : stryMutAct_9fa48("393") ? microseconds(value.finishedAt)! <= microseconds(value.startedAt)! : stryMutAct_9fa48("392") ? false : (stryCov_9fa48("392", "393", "394"), microseconds(value.finishedAt)! < microseconds(value.startedAt)!)))))))) if (stryMutAct_9fa48("395")) {
      ;
    } else {
      stryCov_9fa48("395");
      throw new Error(INCOMPATIBLE);
    }
    return value as unknown as GitlabImportReceipt;
  }
}

/** Convierte un problema RFC 7807 en el error tipado; un cuerpo ilegible no tapa el estado. */
async function failure(response: Response): Promise<never> {
  if (stryMutAct_9fa48("396")) {
    {}
  } else {
    stryCov_9fa48("396");
    const body: unknown = await response.json().catch(stryMutAct_9fa48("397") ? () => undefined : (stryCov_9fa48("397"), () => null));
    if (stryMutAct_9fa48("398")) {
      ;
    } else {
      stryCov_9fa48("398");
      throw new GitlabConnectorError((stryMutAct_9fa48("401") ? body || typeof body === "object" : stryMutAct_9fa48("400") ? false : stryMutAct_9fa48("399") ? true : (stryCov_9fa48("399", "400", "401"), body && (stryMutAct_9fa48("403") ? typeof body !== "object" : stryMutAct_9fa48("402") ? true : (stryCov_9fa48("402", "403"), typeof body === (stryMutAct_9fa48("404") ? "" : (stryCov_9fa48("404"), "object")))))) ? body as Record<string, unknown> : {});
    }
  }
}
export async function readGitlabConnection(signal: AbortSignal): Promise<GitlabConnection> {
  if (stryMutAct_9fa48("405")) {
    {}
  } else {
    stryCov_9fa48("405");
    if (stryMutAct_9fa48("406")) {
      ;
    } else {
      stryCov_9fa48("406");
      signal.throwIfAborted();
    }
    const response = await apiRequest(CONNECTION_URL, stryMutAct_9fa48("407") ? {} : (stryCov_9fa48("407"), {
      signal
    }));
    if (stryMutAct_9fa48("408")) {
      ;
    } else {
      stryCov_9fa48("408");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("411") ? response.status === 200 : stryMutAct_9fa48("410") ? false : stryMutAct_9fa48("409") ? true : (stryCov_9fa48("409", "410", "411"), response.status !== 200)) return failure(response);
    const body: unknown = await response.json();
    if (stryMutAct_9fa48("412")) {
      ;
    } else {
      stryCov_9fa48("412");
      signal.throwIfAborted();
    }
    return decodeConnection(body);
  }
}
export async function connectGitlab(input: {
  token: string;
  projectPath: string;
}, signal: AbortSignal): Promise<GitlabConnection> {
  if (stryMutAct_9fa48("413")) {
    {}
  } else {
    stryCov_9fa48("413");
    if (stryMutAct_9fa48("414")) {
      ;
    } else {
      stryCov_9fa48("414");
      signal.throwIfAborted();
    }
    const response = await apiRequest(CONNECTION_URL, stryMutAct_9fa48("415") ? {} : (stryCov_9fa48("415"), {
      method: stryMutAct_9fa48("416") ? "" : (stryCov_9fa48("416"), "PUT"),
      signal,
      headers: stryMutAct_9fa48("417") ? {} : (stryCov_9fa48("417"), {
        "Content-Type": stryMutAct_9fa48("418") ? "" : (stryCov_9fa48("418"), "application/json")
      }),
      body: JSON.stringify(stryMutAct_9fa48("419") ? {} : (stryCov_9fa48("419"), {
        token: input.token,
        projectPath: input.projectPath
      }))
    }));
    if (stryMutAct_9fa48("420")) {
      ;
    } else {
      stryCov_9fa48("420");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("423") ? response.status === 200 : stryMutAct_9fa48("422") ? false : stryMutAct_9fa48("421") ? true : (stryCov_9fa48("421", "422", "423"), response.status !== 200)) return failure(response);
    const body: unknown = await response.json();
    if (stryMutAct_9fa48("424")) {
      ;
    } else {
      stryCov_9fa48("424");
      signal.throwIfAborted();
    }
    return decodeConnection(body);
  }
}
export async function disconnectGitlab(signal: AbortSignal): Promise<void> {
  if (stryMutAct_9fa48("425")) {
    {}
  } else {
    stryCov_9fa48("425");
    if (stryMutAct_9fa48("426")) {
      ;
    } else {
      stryCov_9fa48("426");
      signal.throwIfAborted();
    }
    const response = await apiRequest(CONNECTION_URL, stryMutAct_9fa48("427") ? {} : (stryCov_9fa48("427"), {
      method: stryMutAct_9fa48("428") ? "" : (stryCov_9fa48("428"), "DELETE"),
      signal
    }));
    if (stryMutAct_9fa48("429")) {
      ;
    } else {
      stryCov_9fa48("429");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("432") ? response.status === 204 : stryMutAct_9fa48("431") ? false : stryMutAct_9fa48("430") ? true : (stryCov_9fa48("430", "431", "432"), response.status !== 204)) return failure(response);
  }
}
export async function startGitlabImport(projectId: string, signal: AbortSignal): Promise<GitlabImportReceipt> {
  if (stryMutAct_9fa48("433")) {
    {}
  } else {
    stryCov_9fa48("433");
    if (stryMutAct_9fa48("434")) {
      ;
    } else {
      stryCov_9fa48("434");
      signal.throwIfAborted();
    }
    const response = await apiRequest(IMPORTS_URL, stryMutAct_9fa48("435") ? {} : (stryCov_9fa48("435"), {
      method: stryMutAct_9fa48("436") ? "" : (stryCov_9fa48("436"), "POST"),
      signal,
      headers: stryMutAct_9fa48("437") ? {} : (stryCov_9fa48("437"), {
        "Content-Type": stryMutAct_9fa48("438") ? "" : (stryCov_9fa48("438"), "application/json")
      }),
      body: JSON.stringify(stryMutAct_9fa48("439") ? {} : (stryCov_9fa48("439"), {
        projectId
      }))
    }));
    if (stryMutAct_9fa48("440")) {
      ;
    } else {
      stryCov_9fa48("440");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("443") ? response.status === 201 : stryMutAct_9fa48("442") ? false : stryMutAct_9fa48("441") ? true : (stryCov_9fa48("441", "442", "443"), response.status !== 201)) return failure(response);
    const body: unknown = await response.json();
    if (stryMutAct_9fa48("444")) {
      ;
    } else {
      stryCov_9fa48("444");
      signal.throwIfAborted();
    }
    return decodeReceipt(body);
  }
}
export async function readGitlabImport(id: string, signal: AbortSignal): Promise<GitlabImportReceipt> {
  if (stryMutAct_9fa48("445")) {
    {}
  } else {
    stryCov_9fa48("445");
    if (stryMutAct_9fa48("446")) {
      ;
    } else {
      stryCov_9fa48("446");
      signal.throwIfAborted();
    }
    const response = await apiRequest(stryMutAct_9fa48("447") ? `` : (stryCov_9fa48("447"), `${IMPORTS_URL}/${id}`), stryMutAct_9fa48("448") ? {} : (stryCov_9fa48("448"), {
      signal
    }));
    if (stryMutAct_9fa48("449")) {
      ;
    } else {
      stryCov_9fa48("449");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("452") ? response.status === 200 : stryMutAct_9fa48("451") ? false : stryMutAct_9fa48("450") ? true : (stryCov_9fa48("450", "451", "452"), response.status !== 200)) return failure(response);
    const body: unknown = await response.json();
    if (stryMutAct_9fa48("453")) {
      ;
    } else {
      stryCov_9fa48("453");
      signal.throwIfAborted();
    }
    return decodeReceipt(body);
  }
}