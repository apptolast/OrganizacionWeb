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
export const EVENT_TYPES = ["ProjectCreated.v1", "ProjectUpdated.v1", "ProjectStatusChanged.v1", "TaskCreated.v1", "SubtaskCreated.v1", "TaskStatusChanged.v1", "BlockPlanned.v1", "BlockChanged.v1", "WorkSessionStarted.v1", "WorkSessionStateChanged.v1", "WorkSessionExtended.v1", "WorkSessionClosed.v1"] as const;
export type EventType = typeof EVENT_TYPES[number];
export type AutomationAction = {
  type: "CREATE_TASK";
  projectId: string;
  titleTemplate: string;
  criterionTemplate: string | null;
  estimatedMinutes: number | null;
} | {
  type: "NOTIFY_WEBHOOK";
  endpointId: string;
};
export type AutomationDraft = {
  name: string;
  enabled: boolean;
  trigger: {
    eventType: EventType;
  };
  condition: {
    projectId: string;
  } | null;
  action: AutomationAction;
};
export type Automation = AutomationDraft & {
  id: string;
  version: number;
  createdAt: string;
  updatedAt: string;
};
export type ActionPreview = {
  type: "CREATE_TASK";
  projectId: string;
  title: string;
  completionCriterion: string;
  estimatedMinutes: number | null;
  wouldFail: string | null;
} | {
  type: "NOTIFY_WEBHOOK";
  endpointId: string;
  eventId: string;
};
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
export type RunStatus = typeof RUN_STATUSES[number];
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
    super(stryMutAct_9fa48("8") ? "" : (stryCov_9fa48("8"), "Revisa los campos indicados."));
    this.name = stryMutAct_9fa48("9") ? "" : (stryCov_9fa48("9"), "AutomationFieldErrors");
  }
}

/** Somebody else changed this rule; the draft survives until the owner reloads on purpose. */
export class AutomationConflict extends Error {
  constructor() {
    if (stryMutAct_9fa48("10")) {
      {}
    } else {
      stryCov_9fa48("10");
      super(stryMutAct_9fa48("11") ? "" : (stryCov_9fa48("11"), "Otra pestaña cambió esta regla."));
      this.name = stryMutAct_9fa48("12") ? "" : (stryCov_9fa48("12"), "AutomationConflict");
    }
  }
}
const MESSAGES: Record<string, string> = stryMutAct_9fa48("13") ? {} : (stryCov_9fa48("13"), {
  UNKNOWN_PLACEHOLDER: stryMutAct_9fa48("14") ? "" : (stryCov_9fa48("14"), "Revisa los marcadores de esta plantilla."),
  UNCLOSED_PLACEHOLDER: stryMutAct_9fa48("15") ? "" : (stryCov_9fa48("15"), "Falta cerrar un marcador con dos llaves."),
  PLACEHOLDER_NOT_AVAILABLE: stryMutAct_9fa48("16") ? "" : (stryCov_9fa48("16"), "Ese marcador no existe para este disparador."),
  UNKNOWN_EVENT_TYPE: stryMutAct_9fa48("17") ? "" : (stryCov_9fa48("17"), "Elige uno de los tipos de evento publicados."),
  TARGET_NOT_FOUND: stryMutAct_9fa48("18") ? "" : (stryCov_9fa48("18"), "Elige un proyecto propio existente."),
  ENDPOINT_NOT_FOUND: stryMutAct_9fa48("19") ? "" : (stryCov_9fa48("19"), "Elige un endpoint propio y activo."),
  TOO_LONG: stryMutAct_9fa48("20") ? "" : (stryCov_9fa48("20"), "El valor es demasiado largo."),
  REQUIRED: stryMutAct_9fa48("21") ? "" : (stryCov_9fa48("21"), "Este campo es obligatorio."),
  OUT_OF_RANGE: stryMutAct_9fa48("22") ? "" : (stryCov_9fa48("22"), "El valor está fuera del rango permitido.")
});
const incompatible = stryMutAct_9fa48("23") ? () => undefined : (stryCov_9fa48("23"), (() => {
  const incompatible = () => new Error(stryMutAct_9fa48("24") ? "" : (stryCov_9fa48("24"), "Respuesta de automatizaciones incompatible."));
  return incompatible;
})());
function uuid(value: unknown): value is string {
  if (stryMutAct_9fa48("25")) {
    {}
  } else {
    stryCov_9fa48("25");
    return stryMutAct_9fa48("28") ? typeof value === "string" || /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value) : stryMutAct_9fa48("27") ? false : stryMutAct_9fa48("26") ? true : (stryCov_9fa48("26", "27", "28"), (stryMutAct_9fa48("30") ? typeof value !== "string" : stryMutAct_9fa48("29") ? true : (stryCov_9fa48("29", "30"), typeof value === (stryMutAct_9fa48("31") ? "" : (stryCov_9fa48("31"), "string")))) && (stryMutAct_9fa48("43") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[^0-9a-f]{12}$/i : stryMutAct_9fa48("42") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]$/i : stryMutAct_9fa48("41") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[^0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("40") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]-[0-9a-f]{12}$/i : stryMutAct_9fa48("39") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[^0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("38") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]-[0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("37") ? /^[0-9a-f]{8}-[^0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("36") ? /^[0-9a-f]{8}-[0-9a-f]-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("35") ? /^[^0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("34") ? /^[0-9a-f]-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i : stryMutAct_9fa48("33") ? /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i : stryMutAct_9fa48("32") ? /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i : (stryCov_9fa48("32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43"), /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)).test(value));
  }
}
function nullableUuid(value: unknown): value is string | null {
  if (stryMutAct_9fa48("44")) {
    {}
  } else {
    stryCov_9fa48("44");
    return stryMutAct_9fa48("47") ? value === null && uuid(value) : stryMutAct_9fa48("46") ? false : stryMutAct_9fa48("45") ? true : (stryCov_9fa48("45", "46", "47"), (stryMutAct_9fa48("49") ? value !== null : stryMutAct_9fa48("48") ? false : (stryCov_9fa48("48", "49"), value === null)) || uuid(value));
  }
}
function action(value: unknown): value is AutomationAction {
  if (stryMutAct_9fa48("50")) {
    {}
  } else {
    stryCov_9fa48("50");
    if (stryMutAct_9fa48("53") ? exact(value, "type projectId titleTemplate criterionTemplate estimatedMinutes") || value.type === "CREATE_TASK" : stryMutAct_9fa48("52") ? false : stryMutAct_9fa48("51") ? true : (stryCov_9fa48("51", "52", "53"), exact(value, stryMutAct_9fa48("54") ? "" : (stryCov_9fa48("54"), "type projectId titleTemplate criterionTemplate estimatedMinutes")) && (stryMutAct_9fa48("56") ? value.type !== "CREATE_TASK" : stryMutAct_9fa48("55") ? true : (stryCov_9fa48("55", "56"), value.type === (stryMutAct_9fa48("57") ? "" : (stryCov_9fa48("57"), "CREATE_TASK")))))) return stryMutAct_9fa48("60") ? uuid(value.projectId) && typeof value.titleTemplate === "string" && (value.criterionTemplate === null || typeof value.criterionTemplate === "string") || value.estimatedMinutes === null || Number.isInteger(value.estimatedMinutes) : stryMutAct_9fa48("59") ? false : stryMutAct_9fa48("58") ? true : (stryCov_9fa48("58", "59", "60"), (stryMutAct_9fa48("62") ? uuid(value.projectId) && typeof value.titleTemplate === "string" || value.criterionTemplate === null || typeof value.criterionTemplate === "string" : stryMutAct_9fa48("61") ? true : (stryCov_9fa48("61", "62"), (stryMutAct_9fa48("64") ? uuid(value.projectId) || typeof value.titleTemplate === "string" : stryMutAct_9fa48("63") ? true : (stryCov_9fa48("63", "64"), uuid(value.projectId) && (stryMutAct_9fa48("66") ? typeof value.titleTemplate !== "string" : stryMutAct_9fa48("65") ? true : (stryCov_9fa48("65", "66"), typeof value.titleTemplate === (stryMutAct_9fa48("67") ? "" : (stryCov_9fa48("67"), "string")))))) && (stryMutAct_9fa48("69") ? value.criterionTemplate === null && typeof value.criterionTemplate === "string" : stryMutAct_9fa48("68") ? true : (stryCov_9fa48("68", "69"), (stryMutAct_9fa48("71") ? value.criterionTemplate !== null : stryMutAct_9fa48("70") ? false : (stryCov_9fa48("70", "71"), value.criterionTemplate === null)) || (stryMutAct_9fa48("73") ? typeof value.criterionTemplate !== "string" : stryMutAct_9fa48("72") ? false : (stryCov_9fa48("72", "73"), typeof value.criterionTemplate === (stryMutAct_9fa48("74") ? "" : (stryCov_9fa48("74"), "string")))))))) && (stryMutAct_9fa48("76") ? value.estimatedMinutes === null && Number.isInteger(value.estimatedMinutes) : stryMutAct_9fa48("75") ? true : (stryCov_9fa48("75", "76"), (stryMutAct_9fa48("78") ? value.estimatedMinutes !== null : stryMutAct_9fa48("77") ? false : (stryCov_9fa48("77", "78"), value.estimatedMinutes === null)) || Number.isInteger(value.estimatedMinutes))));
    return stryMutAct_9fa48("81") ? exact(value, "type endpointId") && value.type === "NOTIFY_WEBHOOK" || uuid(value.endpointId) : stryMutAct_9fa48("80") ? false : stryMutAct_9fa48("79") ? true : (stryCov_9fa48("79", "80", "81"), (stryMutAct_9fa48("83") ? exact(value, "type endpointId") || value.type === "NOTIFY_WEBHOOK" : stryMutAct_9fa48("82") ? true : (stryCov_9fa48("82", "83"), exact(value, stryMutAct_9fa48("84") ? "" : (stryCov_9fa48("84"), "type endpointId")) && (stryMutAct_9fa48("86") ? value.type !== "NOTIFY_WEBHOOK" : stryMutAct_9fa48("85") ? true : (stryCov_9fa48("85", "86"), value.type === (stryMutAct_9fa48("87") ? "" : (stryCov_9fa48("87"), "NOTIFY_WEBHOOK")))))) && uuid(value.endpointId));
  }
}
function automation(value: unknown): value is Automation {
  if (stryMutAct_9fa48("88")) {
    {}
  } else {
    stryCov_9fa48("88");
    return stryMutAct_9fa48("91") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" && exact(value.trigger, "eventType") && EVENT_TYPES.includes(value.trigger.eventType as EventType) && (value.condition === null || exact(value.condition, "projectId") && uuid(value.condition.projectId)) && action(value.action) && Number.isInteger(value.version) && instant(value.createdAt) || instant(value.updatedAt) : stryMutAct_9fa48("90") ? false : stryMutAct_9fa48("89") ? true : (stryCov_9fa48("89", "90", "91"), (stryMutAct_9fa48("93") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" && exact(value.trigger, "eventType") && EVENT_TYPES.includes(value.trigger.eventType as EventType) && (value.condition === null || exact(value.condition, "projectId") && uuid(value.condition.projectId)) && action(value.action) && Number.isInteger(value.version) || instant(value.createdAt) : stryMutAct_9fa48("92") ? true : (stryCov_9fa48("92", "93"), (stryMutAct_9fa48("95") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" && exact(value.trigger, "eventType") && EVENT_TYPES.includes(value.trigger.eventType as EventType) && (value.condition === null || exact(value.condition, "projectId") && uuid(value.condition.projectId)) && action(value.action) || Number.isInteger(value.version) : stryMutAct_9fa48("94") ? true : (stryCov_9fa48("94", "95"), (stryMutAct_9fa48("97") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" && exact(value.trigger, "eventType") && EVENT_TYPES.includes(value.trigger.eventType as EventType) && (value.condition === null || exact(value.condition, "projectId") && uuid(value.condition.projectId)) || action(value.action) : stryMutAct_9fa48("96") ? true : (stryCov_9fa48("96", "97"), (stryMutAct_9fa48("99") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" && exact(value.trigger, "eventType") && EVENT_TYPES.includes(value.trigger.eventType as EventType) || value.condition === null || exact(value.condition, "projectId") && uuid(value.condition.projectId) : stryMutAct_9fa48("98") ? true : (stryCov_9fa48("98", "99"), (stryMutAct_9fa48("101") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" && exact(value.trigger, "eventType") || EVENT_TYPES.includes(value.trigger.eventType as EventType) : stryMutAct_9fa48("100") ? true : (stryCov_9fa48("100", "101"), (stryMutAct_9fa48("103") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" && typeof value.enabled === "boolean" || exact(value.trigger, "eventType") : stryMutAct_9fa48("102") ? true : (stryCov_9fa48("102", "103"), (stryMutAct_9fa48("105") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) && typeof value.name === "string" || typeof value.enabled === "boolean" : stryMutAct_9fa48("104") ? true : (stryCov_9fa48("104", "105"), (stryMutAct_9fa48("107") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") && uuid(value.id) || typeof value.name === "string" : stryMutAct_9fa48("106") ? true : (stryCov_9fa48("106", "107"), (stryMutAct_9fa48("109") ? exact(value, "id name enabled trigger condition action version createdAt updatedAt") || uuid(value.id) : stryMutAct_9fa48("108") ? true : (stryCov_9fa48("108", "109"), exact(value, stryMutAct_9fa48("110") ? "" : (stryCov_9fa48("110"), "id name enabled trigger condition action version createdAt updatedAt")) && uuid(value.id))) && (stryMutAct_9fa48("112") ? typeof value.name !== "string" : stryMutAct_9fa48("111") ? true : (stryCov_9fa48("111", "112"), typeof value.name === (stryMutAct_9fa48("113") ? "" : (stryCov_9fa48("113"), "string")))))) && (stryMutAct_9fa48("115") ? typeof value.enabled !== "boolean" : stryMutAct_9fa48("114") ? true : (stryCov_9fa48("114", "115"), typeof value.enabled === (stryMutAct_9fa48("116") ? "" : (stryCov_9fa48("116"), "boolean")))))) && exact(value.trigger, stryMutAct_9fa48("117") ? "" : (stryCov_9fa48("117"), "eventType")))) && EVENT_TYPES.includes(value.trigger.eventType as EventType))) && (stryMutAct_9fa48("119") ? value.condition === null && exact(value.condition, "projectId") && uuid(value.condition.projectId) : stryMutAct_9fa48("118") ? true : (stryCov_9fa48("118", "119"), (stryMutAct_9fa48("121") ? value.condition !== null : stryMutAct_9fa48("120") ? false : (stryCov_9fa48("120", "121"), value.condition === null)) || (stryMutAct_9fa48("123") ? exact(value.condition, "projectId") || uuid(value.condition.projectId) : stryMutAct_9fa48("122") ? false : (stryCov_9fa48("122", "123"), exact(value.condition, stryMutAct_9fa48("124") ? "" : (stryCov_9fa48("124"), "projectId")) && uuid(value.condition.projectId))))))) && action(value.action))) && Number.isInteger(value.version))) && instant(value.createdAt))) && instant(value.updatedAt));
  }
}
function preview(value: unknown): value is ActionPreview {
  if (stryMutAct_9fa48("125")) {
    {}
  } else {
    stryCov_9fa48("125");
    if (stryMutAct_9fa48("128") ? exact(value, "type projectId title completionCriterion estimatedMinutes wouldFail") || value.type === "CREATE_TASK" : stryMutAct_9fa48("127") ? false : stryMutAct_9fa48("126") ? true : (stryCov_9fa48("126", "127", "128"), exact(value, stryMutAct_9fa48("129") ? "" : (stryCov_9fa48("129"), "type projectId title completionCriterion estimatedMinutes wouldFail")) && (stryMutAct_9fa48("131") ? value.type !== "CREATE_TASK" : stryMutAct_9fa48("130") ? true : (stryCov_9fa48("130", "131"), value.type === (stryMutAct_9fa48("132") ? "" : (stryCov_9fa48("132"), "CREATE_TASK")))))) return stryMutAct_9fa48("135") ? uuid(value.projectId) && typeof value.title === "string" && typeof value.completionCriterion === "string" && (value.estimatedMinutes === null || Number.isInteger(value.estimatedMinutes)) || value.wouldFail === null || typeof value.wouldFail === "string" : stryMutAct_9fa48("134") ? false : stryMutAct_9fa48("133") ? true : (stryCov_9fa48("133", "134", "135"), (stryMutAct_9fa48("137") ? uuid(value.projectId) && typeof value.title === "string" && typeof value.completionCriterion === "string" || value.estimatedMinutes === null || Number.isInteger(value.estimatedMinutes) : stryMutAct_9fa48("136") ? true : (stryCov_9fa48("136", "137"), (stryMutAct_9fa48("139") ? uuid(value.projectId) && typeof value.title === "string" || typeof value.completionCriterion === "string" : stryMutAct_9fa48("138") ? true : (stryCov_9fa48("138", "139"), (stryMutAct_9fa48("141") ? uuid(value.projectId) || typeof value.title === "string" : stryMutAct_9fa48("140") ? true : (stryCov_9fa48("140", "141"), uuid(value.projectId) && (stryMutAct_9fa48("143") ? typeof value.title !== "string" : stryMutAct_9fa48("142") ? true : (stryCov_9fa48("142", "143"), typeof value.title === (stryMutAct_9fa48("144") ? "" : (stryCov_9fa48("144"), "string")))))) && (stryMutAct_9fa48("146") ? typeof value.completionCriterion !== "string" : stryMutAct_9fa48("145") ? true : (stryCov_9fa48("145", "146"), typeof value.completionCriterion === (stryMutAct_9fa48("147") ? "" : (stryCov_9fa48("147"), "string")))))) && (stryMutAct_9fa48("149") ? value.estimatedMinutes === null && Number.isInteger(value.estimatedMinutes) : stryMutAct_9fa48("148") ? true : (stryCov_9fa48("148", "149"), (stryMutAct_9fa48("151") ? value.estimatedMinutes !== null : stryMutAct_9fa48("150") ? false : (stryCov_9fa48("150", "151"), value.estimatedMinutes === null)) || Number.isInteger(value.estimatedMinutes))))) && (stryMutAct_9fa48("153") ? value.wouldFail === null && typeof value.wouldFail === "string" : stryMutAct_9fa48("152") ? true : (stryCov_9fa48("152", "153"), (stryMutAct_9fa48("155") ? value.wouldFail !== null : stryMutAct_9fa48("154") ? false : (stryCov_9fa48("154", "155"), value.wouldFail === null)) || (stryMutAct_9fa48("157") ? typeof value.wouldFail !== "string" : stryMutAct_9fa48("156") ? false : (stryCov_9fa48("156", "157"), typeof value.wouldFail === (stryMutAct_9fa48("158") ? "" : (stryCov_9fa48("158"), "string")))))));
    return stryMutAct_9fa48("161") ? exact(value, "type endpointId eventId") && value.type === "NOTIFY_WEBHOOK" && uuid(value.endpointId) || uuid(value.eventId) : stryMutAct_9fa48("160") ? false : stryMutAct_9fa48("159") ? true : (stryCov_9fa48("159", "160", "161"), (stryMutAct_9fa48("163") ? exact(value, "type endpointId eventId") && value.type === "NOTIFY_WEBHOOK" || uuid(value.endpointId) : stryMutAct_9fa48("162") ? true : (stryCov_9fa48("162", "163"), (stryMutAct_9fa48("165") ? exact(value, "type endpointId eventId") || value.type === "NOTIFY_WEBHOOK" : stryMutAct_9fa48("164") ? true : (stryCov_9fa48("164", "165"), exact(value, stryMutAct_9fa48("166") ? "" : (stryCov_9fa48("166"), "type endpointId eventId")) && (stryMutAct_9fa48("168") ? value.type !== "NOTIFY_WEBHOOK" : stryMutAct_9fa48("167") ? true : (stryCov_9fa48("167", "168"), value.type === (stryMutAct_9fa48("169") ? "" : (stryCov_9fa48("169"), "NOTIFY_WEBHOOK")))))) && uuid(value.endpointId))) && uuid(value.eventId));
  }
}
function match(value: unknown): value is AutomationMatch {
  if (stryMutAct_9fa48("170")) {
    {}
  } else {
    stryCov_9fa48("170");
    return stryMutAct_9fa48("173") ? exact(value, "eventId eventType occurredAt preview loopGuarded") && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) && preview(value.preview) || typeof value.loopGuarded === "boolean" : stryMutAct_9fa48("172") ? false : stryMutAct_9fa48("171") ? true : (stryCov_9fa48("171", "172", "173"), (stryMutAct_9fa48("175") ? exact(value, "eventId eventType occurredAt preview loopGuarded") && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) || preview(value.preview) : stryMutAct_9fa48("174") ? true : (stryCov_9fa48("174", "175"), (stryMutAct_9fa48("177") ? exact(value, "eventId eventType occurredAt preview loopGuarded") && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) || instant(value.occurredAt) : stryMutAct_9fa48("176") ? true : (stryCov_9fa48("176", "177"), (stryMutAct_9fa48("179") ? exact(value, "eventId eventType occurredAt preview loopGuarded") && uuid(value.eventId) || EVENT_TYPES.includes(value.eventType as EventType) : stryMutAct_9fa48("178") ? true : (stryCov_9fa48("178", "179"), (stryMutAct_9fa48("181") ? exact(value, "eventId eventType occurredAt preview loopGuarded") || uuid(value.eventId) : stryMutAct_9fa48("180") ? true : (stryCov_9fa48("180", "181"), exact(value, stryMutAct_9fa48("182") ? "" : (stryCov_9fa48("182"), "eventId eventType occurredAt preview loopGuarded")) && uuid(value.eventId))) && EVENT_TYPES.includes(value.eventType as EventType))) && instant(value.occurredAt))) && preview(value.preview))) && (stryMutAct_9fa48("184") ? typeof value.loopGuarded !== "boolean" : stryMutAct_9fa48("183") ? true : (stryCov_9fa48("183", "184"), typeof value.loopGuarded === (stryMutAct_9fa48("185") ? "" : (stryCov_9fa48("185"), "boolean")))));
  }
}
function run(value: unknown): value is AutomationRun {
  if (stryMutAct_9fa48("186")) {
    {}
  } else {
    stryCov_9fa48("186");
    return stryMutAct_9fa48("189") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) && Number.isInteger(value.attempt) && RUN_STATUSES.includes(value.status as RunStatus) && nullableUuid(value.createdTaskId) && nullableUuid(value.deliveryId) && (value.errorCode === null || typeof value.errorCode === "string") || instant(value.executedAt) : stryMutAct_9fa48("188") ? false : stryMutAct_9fa48("187") ? true : (stryCov_9fa48("187", "188", "189"), (stryMutAct_9fa48("191") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) && Number.isInteger(value.attempt) && RUN_STATUSES.includes(value.status as RunStatus) && nullableUuid(value.createdTaskId) && nullableUuid(value.deliveryId) || value.errorCode === null || typeof value.errorCode === "string" : stryMutAct_9fa48("190") ? true : (stryCov_9fa48("190", "191"), (stryMutAct_9fa48("193") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) && Number.isInteger(value.attempt) && RUN_STATUSES.includes(value.status as RunStatus) && nullableUuid(value.createdTaskId) || nullableUuid(value.deliveryId) : stryMutAct_9fa48("192") ? true : (stryCov_9fa48("192", "193"), (stryMutAct_9fa48("195") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) && Number.isInteger(value.attempt) && RUN_STATUSES.includes(value.status as RunStatus) || nullableUuid(value.createdTaskId) : stryMutAct_9fa48("194") ? true : (stryCov_9fa48("194", "195"), (stryMutAct_9fa48("197") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) && Number.isInteger(value.attempt) || RUN_STATUSES.includes(value.status as RunStatus) : stryMutAct_9fa48("196") ? true : (stryCov_9fa48("196", "197"), (stryMutAct_9fa48("199") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) && instant(value.occurredAt) || Number.isInteger(value.attempt) : stryMutAct_9fa48("198") ? true : (stryCov_9fa48("198", "199"), (stryMutAct_9fa48("201") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) && EVENT_TYPES.includes(value.eventType as EventType) || instant(value.occurredAt) : stryMutAct_9fa48("200") ? true : (stryCov_9fa48("200", "201"), (stryMutAct_9fa48("203") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) && uuid(value.eventId) || EVENT_TYPES.includes(value.eventType as EventType) : stryMutAct_9fa48("202") ? true : (stryCov_9fa48("202", "203"), (stryMutAct_9fa48("205") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") && uuid(value.id) || uuid(value.eventId) : stryMutAct_9fa48("204") ? true : (stryCov_9fa48("204", "205"), (stryMutAct_9fa48("207") ? exact(value, "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt") || uuid(value.id) : stryMutAct_9fa48("206") ? true : (stryCov_9fa48("206", "207"), exact(value, stryMutAct_9fa48("208") ? "" : (stryCov_9fa48("208"), "id eventId eventType occurredAt attempt status createdTaskId deliveryId errorCode executedAt")) && uuid(value.id))) && uuid(value.eventId))) && EVENT_TYPES.includes(value.eventType as EventType))) && instant(value.occurredAt))) && Number.isInteger(value.attempt))) && RUN_STATUSES.includes(value.status as RunStatus))) && nullableUuid(value.createdTaskId))) && nullableUuid(value.deliveryId))) && (stryMutAct_9fa48("210") ? value.errorCode === null && typeof value.errorCode === "string" : stryMutAct_9fa48("209") ? true : (stryCov_9fa48("209", "210"), (stryMutAct_9fa48("212") ? value.errorCode !== null : stryMutAct_9fa48("211") ? false : (stryCov_9fa48("211", "212"), value.errorCode === null)) || (stryMutAct_9fa48("214") ? typeof value.errorCode !== "string" : stryMutAct_9fa48("213") ? false : (stryCov_9fa48("213", "214"), typeof value.errorCode === (stryMutAct_9fa48("215") ? "" : (stryCov_9fa48("215"), "string")))))))) && instant(value.executedAt));
  }
}
async function failure(response: Response): Promise<never> {
  if (stryMutAct_9fa48("216")) {
    {}
  } else {
    stryCov_9fa48("216");
    if (stryMutAct_9fa48("219") ? response.status !== 412 : stryMutAct_9fa48("218") ? false : stryMutAct_9fa48("217") ? true : (stryCov_9fa48("217", "218", "219"), response.status === 412)) if (stryMutAct_9fa48("220")) {
      ;
    } else {
      stryCov_9fa48("220");
      throw new AutomationConflict();
    }
    if (stryMutAct_9fa48("223") ? response.status === 400 && response.status === 422 : stryMutAct_9fa48("222") ? false : stryMutAct_9fa48("221") ? true : (stryCov_9fa48("221", "222", "223"), (stryMutAct_9fa48("225") ? response.status !== 400 : stryMutAct_9fa48("224") ? false : (stryCov_9fa48("224", "225"), response.status === 400)) || (stryMutAct_9fa48("227") ? response.status !== 422 : stryMutAct_9fa48("226") ? false : (stryCov_9fa48("226", "227"), response.status === 422)))) {
      if (stryMutAct_9fa48("228")) {
        {}
      } else {
        stryCov_9fa48("228");
        const body: unknown = await response.json().catch(stryMutAct_9fa48("229") ? () => undefined : (stryCov_9fa48("229"), () => null));
        if (stryMutAct_9fa48("232") ? body && typeof body === "object" && "errors" in body || Array.isArray(body.errors) : stryMutAct_9fa48("231") ? false : stryMutAct_9fa48("230") ? true : (stryCov_9fa48("230", "231", "232"), (stryMutAct_9fa48("234") ? body && typeof body === "object" || "errors" in body : stryMutAct_9fa48("233") ? true : (stryCov_9fa48("233", "234"), (stryMutAct_9fa48("236") ? body || typeof body === "object" : stryMutAct_9fa48("235") ? true : (stryCov_9fa48("235", "236"), body && (stryMutAct_9fa48("238") ? typeof body !== "object" : stryMutAct_9fa48("237") ? true : (stryCov_9fa48("237", "238"), typeof body === (stryMutAct_9fa48("239") ? "" : (stryCov_9fa48("239"), "object")))))) && (stryMutAct_9fa48("240") ? "" : (stryCov_9fa48("240"), "errors")) in body)) && Array.isArray(body.errors))) {
          if (stryMutAct_9fa48("241")) {
            {}
          } else {
            stryCov_9fa48("241");
            const fields: Record<string, string> = {};
            for (const error of body.errors as unknown[]) if (stryMutAct_9fa48("244") ? error && typeof error === "object" && "field" in error && typeof error.field === "string" && "code" in error || typeof error.code === "string" : stryMutAct_9fa48("243") ? false : stryMutAct_9fa48("242") ? true : (stryCov_9fa48("242", "243", "244"), (stryMutAct_9fa48("246") ? error && typeof error === "object" && "field" in error && typeof error.field === "string" || "code" in error : stryMutAct_9fa48("245") ? true : (stryCov_9fa48("245", "246"), (stryMutAct_9fa48("248") ? error && typeof error === "object" && "field" in error || typeof error.field === "string" : stryMutAct_9fa48("247") ? true : (stryCov_9fa48("247", "248"), (stryMutAct_9fa48("250") ? error && typeof error === "object" || "field" in error : stryMutAct_9fa48("249") ? true : (stryCov_9fa48("249", "250"), (stryMutAct_9fa48("252") ? error || typeof error === "object" : stryMutAct_9fa48("251") ? true : (stryCov_9fa48("251", "252"), error && (stryMutAct_9fa48("254") ? typeof error !== "object" : stryMutAct_9fa48("253") ? true : (stryCov_9fa48("253", "254"), typeof error === (stryMutAct_9fa48("255") ? "" : (stryCov_9fa48("255"), "object")))))) && (stryMutAct_9fa48("256") ? "" : (stryCov_9fa48("256"), "field")) in error)) && (stryMutAct_9fa48("258") ? typeof error.field !== "string" : stryMutAct_9fa48("257") ? true : (stryCov_9fa48("257", "258"), typeof error.field === (stryMutAct_9fa48("259") ? "" : (stryCov_9fa48("259"), "string")))))) && (stryMutAct_9fa48("260") ? "" : (stryCov_9fa48("260"), "code")) in error)) && (stryMutAct_9fa48("262") ? typeof error.code !== "string" : stryMutAct_9fa48("261") ? true : (stryCov_9fa48("261", "262"), typeof error.code === (stryMutAct_9fa48("263") ? "" : (stryCov_9fa48("263"), "string")))))) stryMutAct_9fa48("264") ? fields[error.field] &&= MESSAGES[error.code] ?? "Revisa el valor de este campo." : (stryCov_9fa48("264"), fields[error.field] ??= stryMutAct_9fa48("265") ? MESSAGES[error.code] && "Revisa el valor de este campo." : (stryCov_9fa48("265"), MESSAGES[error.code] ?? (stryMutAct_9fa48("266") ? "" : (stryCov_9fa48("266"), "Revisa el valor de este campo."))));
            if (stryMutAct_9fa48("270") ? Object.keys(fields).length <= 0 : stryMutAct_9fa48("269") ? Object.keys(fields).length >= 0 : stryMutAct_9fa48("268") ? false : stryMutAct_9fa48("267") ? true : (stryCov_9fa48("267", "268", "269", "270"), Object.keys(fields).length > 0)) if (stryMutAct_9fa48("271")) {
              ;
            } else {
              stryCov_9fa48("271");
              throw new AutomationFieldErrors(fields);
            }
          }
        }
      }
    }
    throw response;
  }
}
async function json(response: Response, signal: AbortSignal) {
  if (stryMutAct_9fa48("272")) {
    {}
  } else {
    stryCov_9fa48("272");
    if (stryMutAct_9fa48("273")) {
      ;
    } else {
      stryCov_9fa48("273");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("276") ? response.status === 200 : stryMutAct_9fa48("275") ? false : stryMutAct_9fa48("274") ? true : (stryCov_9fa48("274", "275", "276"), response.status !== 200)) await failure(response);
    return (await response.json()) as unknown;
  }
}
export async function readAutomations(signal: AbortSignal) {
  if (stryMutAct_9fa48("277")) {
    {}
  } else {
    stryCov_9fa48("277");
    if (stryMutAct_9fa48("278")) {
      ;
    } else {
      stryCov_9fa48("278");
      signal.throwIfAborted();
    }
    const body = await json(await apiRequest(stryMutAct_9fa48("279") ? "" : (stryCov_9fa48("279"), "/api/v1/me/automations"), stryMutAct_9fa48("280") ? {} : (stryCov_9fa48("280"), {
      signal,
      headers: stryMutAct_9fa48("281") ? {} : (stryCov_9fa48("281"), {
        Accept: stryMutAct_9fa48("282") ? "" : (stryCov_9fa48("282"), "application/json")
      })
    })), signal);
    if (stryMutAct_9fa48("285") ? (!exact(body, "items") || !Array.isArray(body.items)) && !body.items.every(automation) : stryMutAct_9fa48("284") ? false : stryMutAct_9fa48("283") ? true : (stryCov_9fa48("283", "284", "285"), (stryMutAct_9fa48("287") ? !exact(body, "items") && !Array.isArray(body.items) : stryMutAct_9fa48("286") ? false : (stryCov_9fa48("286", "287"), (stryMutAct_9fa48("288") ? exact(body, "items") : (stryCov_9fa48("288"), !exact(body, stryMutAct_9fa48("289") ? "" : (stryCov_9fa48("289"), "items")))) || (stryMutAct_9fa48("290") ? Array.isArray(body.items) : (stryCov_9fa48("290"), !Array.isArray(body.items))))) || (stryMutAct_9fa48("291") ? body.items.every(automation) : (stryCov_9fa48("291"), !(stryMutAct_9fa48("292") ? body.items.some(automation) : (stryCov_9fa48("292"), body.items.every(automation))))))) throw incompatible();
    return body.items as Automation[];
  }
}
async function write(url: string, method: string, draft: AutomationDraft, signal: AbortSignal, version?: number) {
  if (stryMutAct_9fa48("293")) {
    {}
  } else {
    stryCov_9fa48("293");
    if (stryMutAct_9fa48("294")) {
      ;
    } else {
      stryCov_9fa48("294");
      signal.throwIfAborted();
    }
    const headers: Record<string, string> = stryMutAct_9fa48("295") ? {} : (stryCov_9fa48("295"), {
      Accept: stryMutAct_9fa48("296") ? "" : (stryCov_9fa48("296"), "application/json"),
      "Content-Type": stryMutAct_9fa48("297") ? "" : (stryCov_9fa48("297"), "application/json")
    });
    if (stryMutAct_9fa48("300") ? version === undefined : stryMutAct_9fa48("299") ? false : stryMutAct_9fa48("298") ? true : (stryCov_9fa48("298", "299", "300"), version !== undefined)) headers[stryMutAct_9fa48("301") ? "" : (stryCov_9fa48("301"), "If-Match")] = stryMutAct_9fa48("302") ? `` : (stryCov_9fa48("302"), `"${version}"`);
    return apiRequest(url, stryMutAct_9fa48("303") ? {} : (stryCov_9fa48("303"), {
      method,
      signal,
      headers,
      body: JSON.stringify(draft)
    }));
  }
}
export async function createAutomation(draft: AutomationDraft, signal: AbortSignal) {
  if (stryMutAct_9fa48("304")) {
    {}
  } else {
    stryCov_9fa48("304");
    const response = await write(stryMutAct_9fa48("305") ? "" : (stryCov_9fa48("305"), "/api/v1/me/automations"), stryMutAct_9fa48("306") ? "" : (stryCov_9fa48("306"), "POST"), draft, signal);
    if (stryMutAct_9fa48("307")) {
      ;
    } else {
      stryCov_9fa48("307");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("310") ? response.status === 201 : stryMutAct_9fa48("309") ? false : stryMutAct_9fa48("308") ? true : (stryCov_9fa48("308", "309", "310"), response.status !== 201)) await failure(response);
    const body: unknown = await response.json();
    if (stryMutAct_9fa48("313") ? false : stryMutAct_9fa48("312") ? true : stryMutAct_9fa48("311") ? automation(body) : (stryCov_9fa48("311", "312", "313"), !automation(body))) throw incompatible();
    return body;
  }
}
export async function replaceAutomation(id: string, version: number, draft: AutomationDraft, signal: AbortSignal) {
  if (stryMutAct_9fa48("314")) {
    {}
  } else {
    stryCov_9fa48("314");
    const body = await json(await write(stryMutAct_9fa48("315") ? `` : (stryCov_9fa48("315"), `/api/v1/me/automations/${id}`), stryMutAct_9fa48("316") ? "" : (stryCov_9fa48("316"), "PUT"), draft, signal, version), signal);
    if (stryMutAct_9fa48("319") ? false : stryMutAct_9fa48("318") ? true : stryMutAct_9fa48("317") ? automation(body) : (stryCov_9fa48("317", "318", "319"), !automation(body))) throw incompatible();
    return body;
  }
}
export async function deleteAutomation(id: string, version: number, signal: AbortSignal) {
  if (stryMutAct_9fa48("320")) {
    {}
  } else {
    stryCov_9fa48("320");
    if (stryMutAct_9fa48("321")) {
      ;
    } else {
      stryCov_9fa48("321");
      signal.throwIfAborted();
    }
    const response = await apiRequest(stryMutAct_9fa48("322") ? `` : (stryCov_9fa48("322"), `/api/v1/me/automations/${id}`), stryMutAct_9fa48("323") ? {} : (stryCov_9fa48("323"), {
      method: stryMutAct_9fa48("324") ? "" : (stryCov_9fa48("324"), "DELETE"),
      signal,
      headers: stryMutAct_9fa48("325") ? {} : (stryCov_9fa48("325"), {
        "If-Match": stryMutAct_9fa48("326") ? `` : (stryCov_9fa48("326"), `"${version}"`)
      })
    }));
    if (stryMutAct_9fa48("327")) {
      ;
    } else {
      stryCov_9fa48("327");
      signal.throwIfAborted();
    }
    if (stryMutAct_9fa48("330") ? response.status === 204 : stryMutAct_9fa48("329") ? false : stryMutAct_9fa48("328") ? true : (stryCov_9fa48("328", "329", "330"), response.status !== 204)) await failure(response);
  }
}
export async function simulateAutomation(draft: AutomationDraft, signal: AbortSignal) {
  if (stryMutAct_9fa48("331")) {
    {}
  } else {
    stryCov_9fa48("331");
    const body = await json(await write(stryMutAct_9fa48("332") ? "" : (stryCov_9fa48("332"), "/api/v1/me/automations/simulate"), stryMutAct_9fa48("333") ? "" : (stryCov_9fa48("333"), "POST"), draft, signal), signal);
    if (stryMutAct_9fa48("336") ? (!exact(body, "evaluatedEvents matches") || !Number.isInteger(body.evaluatedEvents) || !Array.isArray(body.matches)) && !body.matches.every(match) : stryMutAct_9fa48("335") ? false : stryMutAct_9fa48("334") ? true : (stryCov_9fa48("334", "335", "336"), (stryMutAct_9fa48("338") ? (!exact(body, "evaluatedEvents matches") || !Number.isInteger(body.evaluatedEvents)) && !Array.isArray(body.matches) : stryMutAct_9fa48("337") ? false : (stryCov_9fa48("337", "338"), (stryMutAct_9fa48("340") ? !exact(body, "evaluatedEvents matches") && !Number.isInteger(body.evaluatedEvents) : stryMutAct_9fa48("339") ? false : (stryCov_9fa48("339", "340"), (stryMutAct_9fa48("341") ? exact(body, "evaluatedEvents matches") : (stryCov_9fa48("341"), !exact(body, stryMutAct_9fa48("342") ? "" : (stryCov_9fa48("342"), "evaluatedEvents matches")))) || (stryMutAct_9fa48("343") ? Number.isInteger(body.evaluatedEvents) : (stryCov_9fa48("343"), !Number.isInteger(body.evaluatedEvents))))) || (stryMutAct_9fa48("344") ? Array.isArray(body.matches) : (stryCov_9fa48("344"), !Array.isArray(body.matches))))) || (stryMutAct_9fa48("345") ? body.matches.every(match) : (stryCov_9fa48("345"), !(stryMutAct_9fa48("346") ? body.matches.some(match) : (stryCov_9fa48("346"), body.matches.every(match))))))) throw incompatible();
    return body as unknown as AutomationSimulation;
  }
}
export async function readAutomationRuns(id: string, cursor: string | null, signal: AbortSignal) {
  if (stryMutAct_9fa48("347")) {
    {}
  } else {
    stryCov_9fa48("347");
    if (stryMutAct_9fa48("348")) {
      ;
    } else {
      stryCov_9fa48("348");
      signal.throwIfAborted();
    }
    const query = (stryMutAct_9fa48("351") ? cursor !== null : stryMutAct_9fa48("350") ? false : stryMutAct_9fa48("349") ? true : (stryCov_9fa48("349", "350", "351"), cursor === null)) ? stryMutAct_9fa48("352") ? "Stryker was here!" : (stryCov_9fa48("352"), "") : stryMutAct_9fa48("353") ? `` : (stryCov_9fa48("353"), `?cursor=${encodeURIComponent(cursor)}`);
    const body = await json(await apiRequest(stryMutAct_9fa48("354") ? `` : (stryCov_9fa48("354"), `/api/v1/me/automations/${id}/runs${query}`), stryMutAct_9fa48("355") ? {} : (stryCov_9fa48("355"), {
      signal,
      headers: stryMutAct_9fa48("356") ? {} : (stryCov_9fa48("356"), {
        Accept: stryMutAct_9fa48("357") ? "" : (stryCov_9fa48("357"), "application/json")
      })
    })), signal);
    if (stryMutAct_9fa48("360") ? (!exact(body, "items nextCursor") || !Array.isArray(body.items) || !body.items.every(run)) && !(body.nextCursor === null || typeof body.nextCursor === "string") : stryMutAct_9fa48("359") ? false : stryMutAct_9fa48("358") ? true : (stryCov_9fa48("358", "359", "360"), (stryMutAct_9fa48("362") ? (!exact(body, "items nextCursor") || !Array.isArray(body.items)) && !body.items.every(run) : stryMutAct_9fa48("361") ? false : (stryCov_9fa48("361", "362"), (stryMutAct_9fa48("364") ? !exact(body, "items nextCursor") && !Array.isArray(body.items) : stryMutAct_9fa48("363") ? false : (stryCov_9fa48("363", "364"), (stryMutAct_9fa48("365") ? exact(body, "items nextCursor") : (stryCov_9fa48("365"), !exact(body, stryMutAct_9fa48("366") ? "" : (stryCov_9fa48("366"), "items nextCursor")))) || (stryMutAct_9fa48("367") ? Array.isArray(body.items) : (stryCov_9fa48("367"), !Array.isArray(body.items))))) || (stryMutAct_9fa48("368") ? body.items.every(run) : (stryCov_9fa48("368"), !(stryMutAct_9fa48("369") ? body.items.some(run) : (stryCov_9fa48("369"), body.items.every(run))))))) || (stryMutAct_9fa48("370") ? body.nextCursor === null || typeof body.nextCursor === "string" : (stryCov_9fa48("370"), !(stryMutAct_9fa48("373") ? body.nextCursor === null && typeof body.nextCursor === "string" : stryMutAct_9fa48("372") ? false : stryMutAct_9fa48("371") ? true : (stryCov_9fa48("371", "372", "373"), (stryMutAct_9fa48("375") ? body.nextCursor !== null : stryMutAct_9fa48("374") ? false : (stryCov_9fa48("374", "375"), body.nextCursor === null)) || (stryMutAct_9fa48("377") ? typeof body.nextCursor !== "string" : stryMutAct_9fa48("376") ? false : (stryCov_9fa48("376", "377"), typeof body.nextCursor === (stryMutAct_9fa48("378") ? "" : (stryCov_9fa48("378"), "string")))))))))) throw incompatible();
    return body as unknown as AutomationRunPage;
  }
}