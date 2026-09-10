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
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { AutomationConflict, AutomationFieldErrors, EVENT_TYPES, createAutomation, readAutomationRuns, readAutomations, replaceAutomation, simulateAutomation } from "./automations-api";
import type { Automation, AutomationDraft, AutomationMatch, AutomationRun, EventType } from "./automations-api";
import { readProjects } from "./read-projects-api";
import type { ProjectSummary } from "./read-projects-api";
const TRIGGER_LABELS: Record<EventType, string> = stryMutAct_9fa48("379") ? {} : (stryCov_9fa48("379"), {
  "ProjectCreated.v1": stryMutAct_9fa48("380") ? "" : (stryCov_9fa48("380"), "Proyecto creado"),
  "ProjectUpdated.v1": stryMutAct_9fa48("381") ? "" : (stryCov_9fa48("381"), "Proyecto editado"),
  "ProjectStatusChanged.v1": stryMutAct_9fa48("382") ? "" : (stryCov_9fa48("382"), "Estado de proyecto cambiado"),
  "TaskCreated.v1": stryMutAct_9fa48("383") ? "" : (stryCov_9fa48("383"), "Tarea creada"),
  "SubtaskCreated.v1": stryMutAct_9fa48("384") ? "" : (stryCov_9fa48("384"), "Subtarea creada"),
  "TaskStatusChanged.v1": stryMutAct_9fa48("385") ? "" : (stryCov_9fa48("385"), "Estado de tarea cambiado"),
  "BlockPlanned.v1": stryMutAct_9fa48("386") ? "" : (stryCov_9fa48("386"), "Bloque planificado"),
  "BlockChanged.v1": stryMutAct_9fa48("387") ? "" : (stryCov_9fa48("387"), "Bloque modificado"),
  "WorkSessionStarted.v1": stryMutAct_9fa48("388") ? "" : (stryCov_9fa48("388"), "Sesión iniciada"),
  "WorkSessionStateChanged.v1": stryMutAct_9fa48("389") ? "" : (stryCov_9fa48("389"), "Sesión pausada o reanudada"),
  "WorkSessionExtended.v1": stryMutAct_9fa48("390") ? "" : (stryCov_9fa48("390"), "Sesión ampliada"),
  "WorkSessionClosed.v1": stryMutAct_9fa48("391") ? "" : (stryCov_9fa48("391"), "Sesión cerrada")
});
const RUN_LABELS: Record<AutomationRun["status"], string> = stryMutAct_9fa48("392") ? {} : (stryCov_9fa48("392"), {
  succeeded: stryMutAct_9fa48("393") ? "" : (stryCov_9fa48("393"), "Correcta"),
  retry: stryMutAct_9fa48("394") ? "" : (stryCov_9fa48("394"), "Reintento"),
  failed: stryMutAct_9fa48("395") ? "" : (stryCov_9fa48("395"), "Fallida")
});
const FAILURE_LABELS: Record<string, string> = stryMutAct_9fa48("396") ? {} : (stryCov_9fa48("396"), {
  PROJECT_COMPLETED: stryMutAct_9fa48("397") ? "" : (stryCov_9fa48("397"), "proyecto completado"),
  TITLE_TOO_LONG: stryMutAct_9fa48("398") ? "" : (stryCov_9fa48("398"), "título demasiado largo"),
  CRITERION_TOO_LONG: stryMutAct_9fa48("399") ? "" : (stryCov_9fa48("399"), "criterio demasiado largo"),
  ENDPOINT_NOT_FOUND: stryMutAct_9fa48("400") ? "" : (stryCov_9fa48("400"), "endpoint no encontrado"),
  TARGET_NOT_FOUND: stryMutAct_9fa48("401") ? "" : (stryCov_9fa48("401"), "destino no encontrado")
});
const PLACEHOLDERS = ["{{event.type}}", "{{task.title}}", "{{project.name}}", "{{occurredAt}}"] as const;
const SAMPLES: Record<string, string> = stryMutAct_9fa48("402") ? {} : (stryCov_9fa48("402"), {
  "{{event.type}}": stryMutAct_9fa48("403") ? "" : (stryCov_9fa48("403"), "TaskCreated.v1"),
  "{{task.title}}": stryMutAct_9fa48("404") ? "" : (stryCov_9fa48("404"), "Redactar informe"),
  "{{project.name}}": stryMutAct_9fa48("405") ? "" : (stryCov_9fa48("405"), "Marketing"),
  "{{occurredAt}}": stryMutAct_9fa48("406") ? "" : (stryCov_9fa48("406"), "2026-09-08T10:15:30.123456Z")
});

/** Substitutes the four known markers with sample values; everything else stays literal text. */
export function previewTemplate(template: string) {
  if (stryMutAct_9fa48("407")) {
    {}
  } else {
    stryCov_9fa48("407");
    return PLACEHOLDERS.reduce(stryMutAct_9fa48("408") ? () => undefined : (stryCov_9fa48("408"), (text, placeholder) => text.split(placeholder).join(SAMPLES[placeholder])), template);
  }
}
type Editing = {
  rule: Automation | null;
  name: string;
  eventType: EventType;
  conditionProjectId: string;
  projectId: string;
  titleTemplate: string;
  criterionTemplate: string;
  estimatedMinutes: string;
};
function blank(projectId: string): Editing {
  if (stryMutAct_9fa48("409")) {
    {}
  } else {
    stryCov_9fa48("409");
    return stryMutAct_9fa48("410") ? {} : (stryCov_9fa48("410"), {
      rule: null,
      name: stryMutAct_9fa48("411") ? "Stryker was here!" : (stryCov_9fa48("411"), ""),
      eventType: stryMutAct_9fa48("412") ? "" : (stryCov_9fa48("412"), "TaskCreated.v1"),
      conditionProjectId: stryMutAct_9fa48("413") ? "Stryker was here!" : (stryCov_9fa48("413"), ""),
      projectId,
      titleTemplate: stryMutAct_9fa48("414") ? "" : (stryCov_9fa48("414"), "Revisar {{task.title}}"),
      criterionTemplate: stryMutAct_9fa48("415") ? "Stryker was here!" : (stryCov_9fa48("415"), ""),
      estimatedMinutes: stryMutAct_9fa48("416") ? "Stryker was here!" : (stryCov_9fa48("416"), "")
    });
  }
}
function editingOf(rule: Automation): Editing {
  if (stryMutAct_9fa48("417")) {
    {}
  } else {
    stryCov_9fa48("417");
    return stryMutAct_9fa48("418") ? {} : (stryCov_9fa48("418"), {
      rule,
      name: rule.name,
      eventType: rule.trigger.eventType,
      conditionProjectId: stryMutAct_9fa48("419") ? rule.condition?.projectId && "" : (stryCov_9fa48("419"), (stryMutAct_9fa48("420") ? rule.condition.projectId : (stryCov_9fa48("420"), rule.condition?.projectId)) ?? (stryMutAct_9fa48("421") ? "Stryker was here!" : (stryCov_9fa48("421"), ""))),
      projectId: (stryMutAct_9fa48("424") ? rule.action.type !== "CREATE_TASK" : stryMutAct_9fa48("423") ? false : stryMutAct_9fa48("422") ? true : (stryCov_9fa48("422", "423", "424"), rule.action.type === (stryMutAct_9fa48("425") ? "" : (stryCov_9fa48("425"), "CREATE_TASK")))) ? rule.action.projectId : stryMutAct_9fa48("426") ? "Stryker was here!" : (stryCov_9fa48("426"), ""),
      titleTemplate: (stryMutAct_9fa48("429") ? rule.action.type !== "CREATE_TASK" : stryMutAct_9fa48("428") ? false : stryMutAct_9fa48("427") ? true : (stryCov_9fa48("427", "428", "429"), rule.action.type === (stryMutAct_9fa48("430") ? "" : (stryCov_9fa48("430"), "CREATE_TASK")))) ? rule.action.titleTemplate : stryMutAct_9fa48("431") ? "Stryker was here!" : (stryCov_9fa48("431"), ""),
      criterionTemplate: (stryMutAct_9fa48("434") ? rule.action.type !== "CREATE_TASK" : stryMutAct_9fa48("433") ? false : stryMutAct_9fa48("432") ? true : (stryCov_9fa48("432", "433", "434"), rule.action.type === (stryMutAct_9fa48("435") ? "" : (stryCov_9fa48("435"), "CREATE_TASK")))) ? stryMutAct_9fa48("436") ? rule.action.criterionTemplate && "" : (stryCov_9fa48("436"), rule.action.criterionTemplate ?? (stryMutAct_9fa48("437") ? "Stryker was here!" : (stryCov_9fa48("437"), ""))) : stryMutAct_9fa48("438") ? "Stryker was here!" : (stryCov_9fa48("438"), ""),
      estimatedMinutes: (stryMutAct_9fa48("441") ? rule.action.type === "CREATE_TASK" || rule.action.estimatedMinutes !== null : stryMutAct_9fa48("440") ? false : stryMutAct_9fa48("439") ? true : (stryCov_9fa48("439", "440", "441"), (stryMutAct_9fa48("443") ? rule.action.type !== "CREATE_TASK" : stryMutAct_9fa48("442") ? true : (stryCov_9fa48("442", "443"), rule.action.type === (stryMutAct_9fa48("444") ? "" : (stryCov_9fa48("444"), "CREATE_TASK")))) && (stryMutAct_9fa48("446") ? rule.action.estimatedMinutes === null : stryMutAct_9fa48("445") ? true : (stryCov_9fa48("445", "446"), rule.action.estimatedMinutes !== null)))) ? String(rule.action.estimatedMinutes) : stryMutAct_9fa48("447") ? "Stryker was here!" : (stryCov_9fa48("447"), "")
    });
  }
}
function draftOf(editing: Editing): AutomationDraft {
  if (stryMutAct_9fa48("448")) {
    {}
  } else {
    stryCov_9fa48("448");
    return stryMutAct_9fa48("449") ? {} : (stryCov_9fa48("449"), {
      name: editing.name,
      enabled: stryMutAct_9fa48("450") ? editing.rule?.enabled && true : (stryCov_9fa48("450"), (stryMutAct_9fa48("451") ? editing.rule.enabled : (stryCov_9fa48("451"), editing.rule?.enabled)) ?? (stryMutAct_9fa48("452") ? false : (stryCov_9fa48("452"), true))),
      trigger: stryMutAct_9fa48("453") ? {} : (stryCov_9fa48("453"), {
        eventType: editing.eventType
      }),
      condition: (stryMutAct_9fa48("456") ? editing.conditionProjectId !== "" : stryMutAct_9fa48("455") ? false : stryMutAct_9fa48("454") ? true : (stryCov_9fa48("454", "455", "456"), editing.conditionProjectId === (stryMutAct_9fa48("457") ? "Stryker was here!" : (stryCov_9fa48("457"), "")))) ? null : stryMutAct_9fa48("458") ? {} : (stryCov_9fa48("458"), {
        projectId: editing.conditionProjectId
      }),
      action: stryMutAct_9fa48("459") ? {} : (stryCov_9fa48("459"), {
        type: stryMutAct_9fa48("460") ? "" : (stryCov_9fa48("460"), "CREATE_TASK"),
        projectId: editing.projectId,
        titleTemplate: editing.titleTemplate,
        criterionTemplate: (stryMutAct_9fa48("463") ? editing.criterionTemplate !== "" : stryMutAct_9fa48("462") ? false : stryMutAct_9fa48("461") ? true : (stryCov_9fa48("461", "462", "463"), editing.criterionTemplate === (stryMutAct_9fa48("464") ? "Stryker was here!" : (stryCov_9fa48("464"), "")))) ? null : editing.criterionTemplate,
        estimatedMinutes: (stryMutAct_9fa48("467") ? editing.estimatedMinutes !== "" : stryMutAct_9fa48("466") ? false : stryMutAct_9fa48("465") ? true : (stryCov_9fa48("465", "466", "467"), editing.estimatedMinutes === (stryMutAct_9fa48("468") ? "Stryker was here!" : (stryCov_9fa48("468"), "")))) ? null : Number(editing.estimatedMinutes)
      })
    });
  }
}
export function Automations({
  owner
}: {
  owner: string;
}) {
  if (stryMutAct_9fa48("469")) {
    {}
  } else {
    stryCov_9fa48("469");
    return <AutomationsWorkspace key={owner} />;
  }
}
function AutomationsWorkspace() {
  if (stryMutAct_9fa48("470")) {
    {}
  } else {
    stryCov_9fa48("470");
    const [rules, setRules] = useState<Automation[] | null>(null);
    const [projects, setProjects] = useState<ProjectSummary[]>(stryMutAct_9fa48("471") ? ["Stryker was here"] : (stryCov_9fa48("471"), []));
    const [failed, setFailed] = useState(stryMutAct_9fa48("472") ? true : (stryCov_9fa48("472"), false));
    const [notice, setNotice] = useState<string | null>(null);
    const [editing, setEditing] = useState<Editing | null>(null);
    const [fields, setFields] = useState<Record<string, string>>({});
    const [conflict, setConflict] = useState(stryMutAct_9fa48("473") ? true : (stryCov_9fa48("473"), false));
    const [saving, setSaving] = useState(stryMutAct_9fa48("474") ? true : (stryCov_9fa48("474"), false));
    const [simulation, setSimulation] = useState<{
      evaluatedEvents: number;
      matches: AutomationMatch[];
    } | null>(null);
    const [history, setHistory] = useState<{
      rule: Automation;
      items: AutomationRun[];
      nextCursor: string | null;
    } | null>(null);
    const [busyToggle, setBusyToggle] = useState<string | null>(null);
    const live = useRef<AbortController | null>(null);
    const runsRequest = useRef<AbortController | null>(null);
    const writeRequest = useRef<AbortController | null>(null);
    const mounted = useRef(stryMutAct_9fa48("475") ? false : (stryCov_9fa48("475"), true));
    const heading = useRef<HTMLHeadingElement>(null);
    const invalid = useRef<string | null>(null);
    useLayoutEffect(() => {
      if (stryMutAct_9fa48("477")) {
        {}
      } else {
        stryCov_9fa48("477");
        mounted.current = stryMutAct_9fa48("478") ? false : (stryCov_9fa48("478"), true);
        return () => {
          if (stryMutAct_9fa48("479")) {
            {}
          } else {
            stryCov_9fa48("479");
            mounted.current = stryMutAct_9fa48("480") ? true : (stryCov_9fa48("480"), false);
            stryMutAct_9fa48("481") ? live.current.abort() : (stryCov_9fa48("481"), live.current?.abort());
            stryMutAct_9fa48("482") ? runsRequest.current.abort() : (stryCov_9fa48("482"), runsRequest.current?.abort());
            stryMutAct_9fa48("483") ? writeRequest.current.abort() : (stryCov_9fa48("483"), writeRequest.current?.abort());
          }
        };
      }
    }, stryMutAct_9fa48("484") ? ["Stryker was here"] : (stryCov_9fa48("484"), []));

    /** Only touches state after the answer, so it is safe to start from an effect. */
    const fetchRules = useCallback(async () => {
      if (stryMutAct_9fa48("485")) {
        {}
      } else {
        stryCov_9fa48("485");
        stryMutAct_9fa48("486") ? live.current.abort() : (stryCov_9fa48("486"), live.current?.abort());
        const controller = new AbortController();
        live.current = controller;
        try {
          if (stryMutAct_9fa48("487")) {
            {}
          } else {
            stryCov_9fa48("487");
            const items = await readAutomations(controller.signal);
            if (stryMutAct_9fa48("490") ? !mounted.current && live.current !== controller : stryMutAct_9fa48("489") ? false : stryMutAct_9fa48("488") ? true : (stryCov_9fa48("488", "489", "490"), (stryMutAct_9fa48("491") ? mounted.current : (stryCov_9fa48("491"), !mounted.current)) || (stryMutAct_9fa48("493") ? live.current === controller : stryMutAct_9fa48("492") ? false : (stryCov_9fa48("492", "493"), live.current !== controller)))) return;
            if (stryMutAct_9fa48("494")) {
              ;
            } else {
              stryCov_9fa48("494");
              setRules(items);
            }
            setFailed(stryMutAct_9fa48("496") ? true : (stryCov_9fa48("496"), false));
          }
        } catch {
          if (stryMutAct_9fa48("497")) {
            {}
          } else {
            stryCov_9fa48("497");
            if (stryMutAct_9fa48("500") ? !mounted.current && live.current !== controller : stryMutAct_9fa48("499") ? false : stryMutAct_9fa48("498") ? true : (stryCov_9fa48("498", "499", "500"), (stryMutAct_9fa48("501") ? mounted.current : (stryCov_9fa48("501"), !mounted.current)) || (stryMutAct_9fa48("503") ? live.current === controller : stryMutAct_9fa48("502") ? false : (stryCov_9fa48("502", "503"), live.current !== controller)))) return;
            setFailed(stryMutAct_9fa48("505") ? false : (stryCov_9fa48("505"), true));
          }
        }
      }
    }, stryMutAct_9fa48("506") ? ["Stryker was here"] : (stryCov_9fa48("506"), []));

    /** The retry button may clear the screen at once: it is an event, not an effect. */
    function load() {
      if (stryMutAct_9fa48("507")) {
        {}
      } else {
        stryCov_9fa48("507");
        if (stryMutAct_9fa48("508")) {
          ;
        } else {
          stryCov_9fa48("508");
          setRules(null);
        }
        setFailed(stryMutAct_9fa48("510") ? true : (stryCov_9fa48("510"), false));
        void fetchRules();
      }
    }
    useEffect(() => {
      if (stryMutAct_9fa48("512")) {
        {}
      } else {
        stryCov_9fa48("512");
        const controller = new AbortController();
        live.current = controller;
        readAutomations(controller.signal).then(items => {
          if (stryMutAct_9fa48("514")) {
            {}
          } else {
            stryCov_9fa48("514");
            if (stryMutAct_9fa48("517") ? !mounted.current && live.current !== controller : stryMutAct_9fa48("516") ? false : stryMutAct_9fa48("515") ? true : (stryCov_9fa48("515", "516", "517"), (stryMutAct_9fa48("518") ? mounted.current : (stryCov_9fa48("518"), !mounted.current)) || (stryMutAct_9fa48("520") ? live.current === controller : stryMutAct_9fa48("519") ? false : (stryCov_9fa48("519", "520"), live.current !== controller)))) return;
            if (stryMutAct_9fa48("521")) {
              ;
            } else {
              stryCov_9fa48("521");
              setRules(items);
            }
            setFailed(stryMutAct_9fa48("523") ? true : (stryCov_9fa48("523"), false));
          }
        }).catch(() => {
          if (stryMutAct_9fa48("524")) {
            {}
          } else {
            stryCov_9fa48("524");
            if (stryMutAct_9fa48("527") ? mounted.current || live.current === controller : stryMutAct_9fa48("526") ? false : stryMutAct_9fa48("525") ? true : (stryCov_9fa48("525", "526", "527"), mounted.current && (stryMutAct_9fa48("529") ? live.current !== controller : stryMutAct_9fa48("528") ? true : (stryCov_9fa48("528", "529"), live.current === controller)))) setFailed(stryMutAct_9fa48("531") ? false : (stryCov_9fa48("531"), true));
          }
        });
        return stryMutAct_9fa48("532") ? () => undefined : (stryCov_9fa48("532"), () => controller.abort());
      }
    }, stryMutAct_9fa48("533") ? ["Stryker was here"] : (stryCov_9fa48("533"), []));
    useEffect(() => {
      if (stryMutAct_9fa48("535")) {
        {}
      } else {
        stryCov_9fa48("535");
        const controller = new AbortController();
        void readProjects(stryMutAct_9fa48("536") ? "" : (stryCov_9fa48("536"), "/proyectos"), controller.signal).then(page => {
          if (stryMutAct_9fa48("537")) {
            {}
          } else {
            stryCov_9fa48("537");
            if (stryMutAct_9fa48("540") ? mounted.current || "items" in page : stryMutAct_9fa48("539") ? false : stryMutAct_9fa48("538") ? true : (stryCov_9fa48("538", "539", "540"), mounted.current && (stryMutAct_9fa48("541") ? "" : (stryCov_9fa48("541"), "items")) in page)) if (stryMutAct_9fa48("542")) {
              ;
            } else {
              stryCov_9fa48("542");
              setProjects(page.items);
            }
          }
        }).catch(() => {});
        return stryMutAct_9fa48("543") ? () => undefined : (stryCov_9fa48("543"), () => controller.abort());
      }
    }, stryMutAct_9fa48("544") ? ["Stryker was here"] : (stryCov_9fa48("544"), []));
    useLayoutEffect(() => {
      if (stryMutAct_9fa48("546")) {
        {}
      } else {
        stryCov_9fa48("546");
        if (stryMutAct_9fa48("549") ? false : stryMutAct_9fa48("548") ? true : stryMutAct_9fa48("547") ? invalid.current : (stryCov_9fa48("547", "548", "549"), !invalid.current)) return;
        stryMutAct_9fa48("550") ? document.getElementById(invalid.current).focus() : (stryCov_9fa48("550"), document.getElementById(invalid.current)?.focus());
        invalid.current = null;
      }
    }, stryMutAct_9fa48("551") ? [] : (stryCov_9fa48("551"), [fields]));
    function nameOfProject(id: string) {
      if (stryMutAct_9fa48("552")) {
        {}
      } else {
        stryCov_9fa48("552");
        return stryMutAct_9fa48("553") ? projects.find(project => project.id === id)?.name && id : (stryCov_9fa48("553"), (stryMutAct_9fa48("554") ? projects.find(project => project.id === id).name : (stryCov_9fa48("554"), projects.find(stryMutAct_9fa48("555") ? () => undefined : (stryCov_9fa48("555"), project => stryMutAct_9fa48("558") ? project.id !== id : stryMutAct_9fa48("557") ? false : stryMutAct_9fa48("556") ? true : (stryCov_9fa48("556", "557", "558"), project.id === id)))?.name)) ?? id);
      }
    }
    async function save() {
      if (stryMutAct_9fa48("559")) {
        {}
      } else {
        stryCov_9fa48("559");
        if (stryMutAct_9fa48("562") ? !editing && saving : stryMutAct_9fa48("561") ? false : stryMutAct_9fa48("560") ? true : (stryCov_9fa48("560", "561", "562"), (stryMutAct_9fa48("563") ? editing : (stryCov_9fa48("563"), !editing)) || saving)) return;
        stryMutAct_9fa48("564") ? writeRequest.current.abort() : (stryCov_9fa48("564"), writeRequest.current?.abort());
        const controller = new AbortController();
        writeRequest.current = controller;
        setSaving(stryMutAct_9fa48("566") ? false : (stryCov_9fa48("566"), true));
        if (stryMutAct_9fa48("567")) {
          ;
        } else {
          stryCov_9fa48("567");
          setFields({});
        }
        setConflict(stryMutAct_9fa48("569") ? true : (stryCov_9fa48("569"), false));
        if (stryMutAct_9fa48("570")) {
          ;
        } else {
          stryCov_9fa48("570");
          setNotice(null);
        }
        try {
          if (stryMutAct_9fa48("571")) {
            {}
          } else {
            stryCov_9fa48("571");
            const draft = draftOf(editing);
            const saved = editing.rule ? await replaceAutomation(editing.rule.id, editing.rule.version, draft, controller.signal) : await createAutomation(draft, controller.signal);
            if (stryMutAct_9fa48("574") ? !mounted.current && writeRequest.current !== controller : stryMutAct_9fa48("573") ? false : stryMutAct_9fa48("572") ? true : (stryCov_9fa48("572", "573", "574"), (stryMutAct_9fa48("575") ? mounted.current : (stryCov_9fa48("575"), !mounted.current)) || (stryMutAct_9fa48("577") ? writeRequest.current === controller : stryMutAct_9fa48("576") ? false : (stryCov_9fa48("576", "577"), writeRequest.current !== controller)))) return;
            if (stryMutAct_9fa48("578")) {
              ;
            } else {
              stryCov_9fa48("578");
              setEditing(null);
            }
            setRules(stryMutAct_9fa48("580") ? () => undefined : (stryCov_9fa48("580"), current => (stryMutAct_9fa48("583") ? current !== null : stryMutAct_9fa48("582") ? false : stryMutAct_9fa48("581") ? true : (stryCov_9fa48("581", "582", "583"), current === null)) ? stryMutAct_9fa48("584") ? [] : (stryCov_9fa48("584"), [saved]) : (stryMutAct_9fa48("585") ? current.every(rule => rule.id === saved.id) : (stryCov_9fa48("585"), current.some(stryMutAct_9fa48("586") ? () => undefined : (stryCov_9fa48("586"), rule => stryMutAct_9fa48("589") ? rule.id !== saved.id : stryMutAct_9fa48("588") ? false : stryMutAct_9fa48("587") ? true : (stryCov_9fa48("587", "588", "589"), rule.id === saved.id))))) ? current.map(stryMutAct_9fa48("590") ? () => undefined : (stryCov_9fa48("590"), rule => (stryMutAct_9fa48("593") ? rule.id !== saved.id : stryMutAct_9fa48("592") ? false : stryMutAct_9fa48("591") ? true : (stryCov_9fa48("591", "592", "593"), rule.id === saved.id)) ? saved : rule)) : stryMutAct_9fa48("594") ? [] : (stryCov_9fa48("594"), [...current, saved])));
          }
        } catch (error) {
          if (stryMutAct_9fa48("595")) {
            {}
          } else {
            stryCov_9fa48("595");
            if (stryMutAct_9fa48("598") ? !mounted.current && writeRequest.current !== controller : stryMutAct_9fa48("597") ? false : stryMutAct_9fa48("596") ? true : (stryCov_9fa48("596", "597", "598"), (stryMutAct_9fa48("599") ? mounted.current : (stryCov_9fa48("599"), !mounted.current)) || (stryMutAct_9fa48("601") ? writeRequest.current === controller : stryMutAct_9fa48("600") ? false : (stryCov_9fa48("600", "601"), writeRequest.current !== controller)))) return;
            if (stryMutAct_9fa48("603") ? false : stryMutAct_9fa48("602") ? true : (stryCov_9fa48("602", "603"), error instanceof AutomationFieldErrors)) {
              if (stryMutAct_9fa48("604")) {
                {}
              } else {
                stryCov_9fa48("604");
                invalid.current = controlIdOf(Object.keys(error.fields)[0]);
                if (stryMutAct_9fa48("605")) {
                  ;
                } else {
                  stryCov_9fa48("605");
                  setFields(error.fields);
                }
              }
            } else if (stryMutAct_9fa48("607") ? false : stryMutAct_9fa48("606") ? true : (stryCov_9fa48("606", "607"), error instanceof AutomationConflict)) setConflict(stryMutAct_9fa48("609") ? false : (stryCov_9fa48("609"), true));else setNotice(stryMutAct_9fa48("611") ? "" : (stryCov_9fa48("611"), "No se ha podido guardar. Inténtalo de nuevo."));
          }
        } finally {
          if (stryMutAct_9fa48("612")) {
            {}
          } else {
            stryCov_9fa48("612");
            if (stryMutAct_9fa48("615") ? mounted.current || writeRequest.current === controller : stryMutAct_9fa48("614") ? false : stryMutAct_9fa48("613") ? true : (stryCov_9fa48("613", "614", "615"), mounted.current && (stryMutAct_9fa48("617") ? writeRequest.current !== controller : stryMutAct_9fa48("616") ? true : (stryCov_9fa48("616", "617"), writeRequest.current === controller)))) setSaving(stryMutAct_9fa48("619") ? true : (stryCov_9fa48("619"), false));
          }
        }
      }
    }
    async function simulate() {
      if (stryMutAct_9fa48("620")) {
        {}
      } else {
        stryCov_9fa48("620");
        if (stryMutAct_9fa48("623") ? false : stryMutAct_9fa48("622") ? true : stryMutAct_9fa48("621") ? editing : (stryCov_9fa48("621", "622", "623"), !editing)) return;
        const controller = new AbortController();
        stryMutAct_9fa48("624") ? writeRequest.current.abort() : (stryCov_9fa48("624"), writeRequest.current?.abort());
        writeRequest.current = controller;
        if (stryMutAct_9fa48("625")) {
          ;
        } else {
          stryCov_9fa48("625");
          setFields({});
        }
        if (stryMutAct_9fa48("626")) {
          ;
        } else {
          stryCov_9fa48("626");
          setNotice(null);
        }
        try {
          if (stryMutAct_9fa48("627")) {
            {}
          } else {
            stryCov_9fa48("627");
            const result = await simulateAutomation(draftOf(editing), controller.signal);
            if (stryMutAct_9fa48("630") ? !mounted.current && writeRequest.current !== controller : stryMutAct_9fa48("629") ? false : stryMutAct_9fa48("628") ? true : (stryCov_9fa48("628", "629", "630"), (stryMutAct_9fa48("631") ? mounted.current : (stryCov_9fa48("631"), !mounted.current)) || (stryMutAct_9fa48("633") ? writeRequest.current === controller : stryMutAct_9fa48("632") ? false : (stryCov_9fa48("632", "633"), writeRequest.current !== controller)))) return;
            if (stryMutAct_9fa48("634")) {
              ;
            } else {
              stryCov_9fa48("634");
              setSimulation(result);
            }
          }
        } catch (error) {
          if (stryMutAct_9fa48("635")) {
            {}
          } else {
            stryCov_9fa48("635");
            if (stryMutAct_9fa48("638") ? !mounted.current && writeRequest.current !== controller : stryMutAct_9fa48("637") ? false : stryMutAct_9fa48("636") ? true : (stryCov_9fa48("636", "637", "638"), (stryMutAct_9fa48("639") ? mounted.current : (stryCov_9fa48("639"), !mounted.current)) || (stryMutAct_9fa48("641") ? writeRequest.current === controller : stryMutAct_9fa48("640") ? false : (stryCov_9fa48("640", "641"), writeRequest.current !== controller)))) return;
            if (stryMutAct_9fa48("643") ? false : stryMutAct_9fa48("642") ? true : (stryCov_9fa48("642", "643"), error instanceof AutomationFieldErrors)) {
              if (stryMutAct_9fa48("644")) {
                {}
              } else {
                stryCov_9fa48("644");
                invalid.current = controlIdOf(Object.keys(error.fields)[0]);
                if (stryMutAct_9fa48("645")) {
                  ;
                } else {
                  stryCov_9fa48("645");
                  setFields(error.fields);
                }
              }
            } else setNotice(stryMutAct_9fa48("647") ? "" : (stryCov_9fa48("647"), "No se ha podido simular. Inténtalo de nuevo."));
          }
        }
      }
    }
    async function toggle(rule: Automation) {
      if (stryMutAct_9fa48("648")) {
        {}
      } else {
        stryCov_9fa48("648");
        if (stryMutAct_9fa48("650") ? false : stryMutAct_9fa48("649") ? true : (stryCov_9fa48("649", "650"), busyToggle)) return;
        const controller = new AbortController();
        stryMutAct_9fa48("651") ? writeRequest.current.abort() : (stryCov_9fa48("651"), writeRequest.current?.abort());
        writeRequest.current = controller;
        if (stryMutAct_9fa48("652")) {
          ;
        } else {
          stryCov_9fa48("652");
          setBusyToggle(rule.id);
        }
        if (stryMutAct_9fa48("653")) {
          ;
        } else {
          stryCov_9fa48("653");
          setNotice(null);
        }
        try {
          if (stryMutAct_9fa48("654")) {
            {}
          } else {
            stryCov_9fa48("654");
            const saved = await replaceAutomation(rule.id, rule.version, stryMutAct_9fa48("655") ? {} : (stryCov_9fa48("655"), {
              ...toDraft(rule),
              enabled: stryMutAct_9fa48("656") ? rule.enabled : (stryCov_9fa48("656"), !rule.enabled)
            }), controller.signal);
            if (stryMutAct_9fa48("659") ? !mounted.current && writeRequest.current !== controller : stryMutAct_9fa48("658") ? false : stryMutAct_9fa48("657") ? true : (stryCov_9fa48("657", "658", "659"), (stryMutAct_9fa48("660") ? mounted.current : (stryCov_9fa48("660"), !mounted.current)) || (stryMutAct_9fa48("662") ? writeRequest.current === controller : stryMutAct_9fa48("661") ? false : (stryCov_9fa48("661", "662"), writeRequest.current !== controller)))) return;
            setRules(stryMutAct_9fa48("664") ? () => undefined : (stryCov_9fa48("664"), current => (stryMutAct_9fa48("665") ? current && [] : (stryCov_9fa48("665"), current ?? (stryMutAct_9fa48("666") ? ["Stryker was here"] : (stryCov_9fa48("666"), [])))).map(stryMutAct_9fa48("667") ? () => undefined : (stryCov_9fa48("667"), item => (stryMutAct_9fa48("670") ? item.id !== saved.id : stryMutAct_9fa48("669") ? false : stryMutAct_9fa48("668") ? true : (stryCov_9fa48("668", "669", "670"), item.id === saved.id)) ? saved : item))));
          }
        } catch {
          if (stryMutAct_9fa48("671")) {
            {}
          } else {
            stryCov_9fa48("671");
            if (stryMutAct_9fa48("674") ? !mounted.current && writeRequest.current !== controller : stryMutAct_9fa48("673") ? false : stryMutAct_9fa48("672") ? true : (stryCov_9fa48("672", "673", "674"), (stryMutAct_9fa48("675") ? mounted.current : (stryCov_9fa48("675"), !mounted.current)) || (stryMutAct_9fa48("677") ? writeRequest.current === controller : stryMutAct_9fa48("676") ? false : (stryCov_9fa48("676", "677"), writeRequest.current !== controller)))) return;
            setNotice(stryMutAct_9fa48("679") ? "" : (stryCov_9fa48("679"), "No se ha podido cambiar el estado de la regla."));
          }
        } finally {
          if (stryMutAct_9fa48("680")) {
            {}
          } else {
            stryCov_9fa48("680");
            if (stryMutAct_9fa48("683") ? mounted.current || writeRequest.current === controller : stryMutAct_9fa48("682") ? false : stryMutAct_9fa48("681") ? true : (stryCov_9fa48("681", "682", "683"), mounted.current && (stryMutAct_9fa48("685") ? writeRequest.current !== controller : stryMutAct_9fa48("684") ? true : (stryCov_9fa48("684", "685"), writeRequest.current === controller)))) if (stryMutAct_9fa48("686")) {
              ;
            } else {
              stryCov_9fa48("686");
              setBusyToggle(null);
            }
          }
        }
      }
    }

    /** The owner asked for the current version on purpose; only now is the draft replaced. */
    async function reloadEditing() {
      if (stryMutAct_9fa48("687")) {
        {}
      } else {
        stryCov_9fa48("687");
        const id = stryMutAct_9fa48("689") ? editing.rule?.id : stryMutAct_9fa48("688") ? editing?.rule.id : (stryCov_9fa48("688", "689"), editing?.rule?.id);
        setConflict(stryMutAct_9fa48("691") ? true : (stryCov_9fa48("691"), false));
        stryMutAct_9fa48("692") ? live.current.abort() : (stryCov_9fa48("692"), live.current?.abort());
        const controller = new AbortController();
        live.current = controller;
        try {
          if (stryMutAct_9fa48("693")) {
            {}
          } else {
            stryCov_9fa48("693");
            const items = await readAutomations(controller.signal);
            if (stryMutAct_9fa48("696") ? !mounted.current && live.current !== controller : stryMutAct_9fa48("695") ? false : stryMutAct_9fa48("694") ? true : (stryCov_9fa48("694", "695", "696"), (stryMutAct_9fa48("697") ? mounted.current : (stryCov_9fa48("697"), !mounted.current)) || (stryMutAct_9fa48("699") ? live.current === controller : stryMutAct_9fa48("698") ? false : (stryCov_9fa48("698", "699"), live.current !== controller)))) return;
            if (stryMutAct_9fa48("700")) {
              ;
            } else {
              stryCov_9fa48("700");
              setRules(items);
            }
            const fresh = items.find(stryMutAct_9fa48("701") ? () => undefined : (stryCov_9fa48("701"), rule => stryMutAct_9fa48("704") ? rule.id !== id : stryMutAct_9fa48("703") ? false : stryMutAct_9fa48("702") ? true : (stryCov_9fa48("702", "703", "704"), rule.id === id)));
            if (stryMutAct_9fa48("705")) {
              ;
            } else {
              stryCov_9fa48("705");
              setEditing(fresh ? editingOf(fresh) : null);
            }
          }
        } catch {
          if (stryMutAct_9fa48("706")) {
            {}
          } else {
            stryCov_9fa48("706");
            if (stryMutAct_9fa48("709") ? !mounted.current && live.current !== controller : stryMutAct_9fa48("708") ? false : stryMutAct_9fa48("707") ? true : (stryCov_9fa48("707", "708", "709"), (stryMutAct_9fa48("710") ? mounted.current : (stryCov_9fa48("710"), !mounted.current)) || (stryMutAct_9fa48("712") ? live.current === controller : stryMutAct_9fa48("711") ? false : (stryCov_9fa48("711", "712"), live.current !== controller)))) return;
            setNotice(stryMutAct_9fa48("714") ? "" : (stryCov_9fa48("714"), "No se ha podido cargar la versión actual."));
          }
        }
      }
    }
    async function openHistory(rule: Automation, cursor: string | null) {
      if (stryMutAct_9fa48("715")) {
        {}
      } else {
        stryCov_9fa48("715");
        stryMutAct_9fa48("716") ? runsRequest.current.abort() : (stryCov_9fa48("716"), runsRequest.current?.abort());
        const controller = new AbortController();
        runsRequest.current = controller;
        if (stryMutAct_9fa48("719") ? cursor !== null : stryMutAct_9fa48("718") ? false : stryMutAct_9fa48("717") ? true : (stryCov_9fa48("717", "718", "719"), cursor === null)) setHistory(stryMutAct_9fa48("721") ? {} : (stryCov_9fa48("721"), {
          rule,
          items: stryMutAct_9fa48("722") ? ["Stryker was here"] : (stryCov_9fa48("722"), []),
          nextCursor: null
        }));
        try {
          if (stryMutAct_9fa48("723")) {
            {}
          } else {
            stryCov_9fa48("723");
            const page = await readAutomationRuns(rule.id, cursor, controller.signal);
            if (stryMutAct_9fa48("726") ? !mounted.current && runsRequest.current !== controller : stryMutAct_9fa48("725") ? false : stryMutAct_9fa48("724") ? true : (stryCov_9fa48("724", "725", "726"), (stryMutAct_9fa48("727") ? mounted.current : (stryCov_9fa48("727"), !mounted.current)) || (stryMutAct_9fa48("729") ? runsRequest.current === controller : stryMutAct_9fa48("728") ? false : (stryCov_9fa48("728", "729"), runsRequest.current !== controller)))) return;
            setHistory(stryMutAct_9fa48("731") ? () => undefined : (stryCov_9fa48("731"), current => stryMutAct_9fa48("732") ? {} : (stryCov_9fa48("732"), {
              rule,
              items: (stryMutAct_9fa48("735") ? cursor !== null : stryMutAct_9fa48("734") ? false : stryMutAct_9fa48("733") ? true : (stryCov_9fa48("733", "734", "735"), cursor === null)) ? page.items : stryMutAct_9fa48("736") ? [] : (stryCov_9fa48("736"), [...(stryMutAct_9fa48("737") ? current?.items && [] : (stryCov_9fa48("737"), (stryMutAct_9fa48("738") ? current.items : (stryCov_9fa48("738"), current?.items)) ?? (stryMutAct_9fa48("739") ? ["Stryker was here"] : (stryCov_9fa48("739"), [])))), ...page.items]),
              nextCursor: page.nextCursor
            })));
          }
        } catch {
          if (stryMutAct_9fa48("740")) {
            {}
          } else {
            stryCov_9fa48("740");
            if (stryMutAct_9fa48("743") ? !mounted.current && runsRequest.current !== controller : stryMutAct_9fa48("742") ? false : stryMutAct_9fa48("741") ? true : (stryCov_9fa48("741", "742", "743"), (stryMutAct_9fa48("744") ? mounted.current : (stryCov_9fa48("744"), !mounted.current)) || (stryMutAct_9fa48("746") ? runsRequest.current === controller : stryMutAct_9fa48("745") ? false : (stryCov_9fa48("745", "746"), runsRequest.current !== controller)))) return;
            setNotice(stryMutAct_9fa48("748") ? "" : (stryCov_9fa48("748"), "No se ha podido cargar el historial."));
          }
        }
      }
    }
    return <main id="proyectos" className="automations">
      <h1 ref={heading} tabIndex={stryMutAct_9fa48("749") ? +1 : (stryCov_9fa48("749"), -1)}>
        Automatizaciones
      </h1>
      {stryMutAct_9fa48("752") ? notice || <p role="alert">{notice}</p> : stryMutAct_9fa48("751") ? false : stryMutAct_9fa48("750") ? true : (stryCov_9fa48("750", "751", "752"), notice && <p role="alert">{notice}</p>)}
      {stryMutAct_9fa48("755") ? rules === null && !failed || <p role="status">Cargando automatizaciones…</p> : stryMutAct_9fa48("754") ? false : stryMutAct_9fa48("753") ? true : (stryCov_9fa48("753", "754", "755"), (stryMutAct_9fa48("757") ? rules === null || !failed : stryMutAct_9fa48("756") ? true : (stryCov_9fa48("756", "757"), (stryMutAct_9fa48("759") ? rules !== null : stryMutAct_9fa48("758") ? true : (stryCov_9fa48("758", "759"), rules === null)) && (stryMutAct_9fa48("760") ? failed : (stryCov_9fa48("760"), !failed)))) && <p role="status">Cargando automatizaciones…</p>)}
      {stryMutAct_9fa48("763") ? failed || <div role="alert">
          <p>No se han podido cargar tus automatizaciones.</p>
          <button type="button" onClick={load}>
            Reintentar
          </button>
        </div> : stryMutAct_9fa48("762") ? false : stryMutAct_9fa48("761") ? true : (stryCov_9fa48("761", "762", "763"), failed && <div role="alert">
          <p>No se han podido cargar tus automatizaciones.</p>
          <button type="button" onClick={load}>
            Reintentar
          </button>
        </div>)}
      {stryMutAct_9fa48("766") ? rules !== null && rules.length === 0 || <div>
          <p>
            Todavía no tienes ninguna regla. Una regla observa un hecho de tus
            proyectos y, cuando ocurre, crea una tarea por ti.
          </p>
          <button type="button" onClick={() => setEditing(blank(projects[0]?.id ?? ""))}>
            Nueva regla
          </button>
        </div> : stryMutAct_9fa48("765") ? false : stryMutAct_9fa48("764") ? true : (stryCov_9fa48("764", "765", "766"), (stryMutAct_9fa48("768") ? rules !== null || rules.length === 0 : stryMutAct_9fa48("767") ? true : (stryCov_9fa48("767", "768"), (stryMutAct_9fa48("770") ? rules === null : stryMutAct_9fa48("769") ? true : (stryCov_9fa48("769", "770"), rules !== null)) && (stryMutAct_9fa48("772") ? rules.length !== 0 : stryMutAct_9fa48("771") ? true : (stryCov_9fa48("771", "772"), rules.length === 0)))) && <div>
          <p>
            Todavía no tienes ninguna regla. Una regla observa un hecho de tus
            proyectos y, cuando ocurre, crea una tarea por ti.
          </p>
          <button type="button" onClick={stryMutAct_9fa48("773") ? () => undefined : (stryCov_9fa48("773"), () => setEditing(blank(stryMutAct_9fa48("774") ? projects[0]?.id && "" : (stryCov_9fa48("774"), (stryMutAct_9fa48("775") ? projects[0].id : (stryCov_9fa48("775"), projects[0]?.id)) ?? (stryMutAct_9fa48("776") ? "Stryker was here!" : (stryCov_9fa48("776"), ""))))))}>
            Nueva regla
          </button>
        </div>)}
      {stryMutAct_9fa48("779") ? rules !== null && rules.length > 0 || <>
          <ul aria-label="Reglas">
            {rules.map(rule => <li key={rule.id}>
                <span>{rule.name}</span>
                <span>{TRIGGER_LABELS[rule.trigger.eventType]}</span>
                <span>
                  {rule.action.type === "CREATE_TASK" ? nameOfProject(rule.action.projectId) : "Webhook"}
                </span>
                <button type="button" role="switch" className={rule.enabled ? "is-active" : "is-inactive"} aria-checked={rule.enabled} aria-label={`Activar o desactivar ${rule.name}`} disabled={busyToggle === rule.id} onClick={() => void toggle(rule)}>
                  {rule.enabled ? "Activa" : "Inactiva"}
                </button>
                <button type="button" onClick={() => setEditing(editingOf(rule))}>
                  {`Editar ${rule.name}`}
                </button>
                <button type="button" onClick={() => void openHistory(rule, null)}>
                  {`Historial de ${rule.name}`}
                </button>
              </li>)}
          </ul>
          <button type="button" onClick={() => setEditing(blank(projects[0]?.id ?? ""))}>
            Nueva regla
          </button>
        </> : stryMutAct_9fa48("778") ? false : stryMutAct_9fa48("777") ? true : (stryCov_9fa48("777", "778", "779"), (stryMutAct_9fa48("781") ? rules !== null || rules.length > 0 : stryMutAct_9fa48("780") ? true : (stryCov_9fa48("780", "781"), (stryMutAct_9fa48("783") ? rules === null : stryMutAct_9fa48("782") ? true : (stryCov_9fa48("782", "783"), rules !== null)) && (stryMutAct_9fa48("786") ? rules.length <= 0 : stryMutAct_9fa48("785") ? rules.length >= 0 : stryMutAct_9fa48("784") ? true : (stryCov_9fa48("784", "785", "786"), rules.length > 0)))) && <>
          <ul aria-label="Reglas">
            {rules.map(stryMutAct_9fa48("787") ? () => undefined : (stryCov_9fa48("787"), rule => <li key={rule.id}>
                <span>{rule.name}</span>
                <span>{TRIGGER_LABELS[rule.trigger.eventType]}</span>
                <span>
                  {(stryMutAct_9fa48("790") ? rule.action.type !== "CREATE_TASK" : stryMutAct_9fa48("789") ? false : stryMutAct_9fa48("788") ? true : (stryCov_9fa48("788", "789", "790"), rule.action.type === (stryMutAct_9fa48("791") ? "" : (stryCov_9fa48("791"), "CREATE_TASK")))) ? nameOfProject(rule.action.projectId) : stryMutAct_9fa48("792") ? "" : (stryCov_9fa48("792"), "Webhook")}
                </span>
                <button type="button" role="switch" className={rule.enabled ? stryMutAct_9fa48("793") ? "" : (stryCov_9fa48("793"), "is-active") : stryMutAct_9fa48("794") ? "" : (stryCov_9fa48("794"), "is-inactive")} aria-checked={rule.enabled} aria-label={stryMutAct_9fa48("795") ? `` : (stryCov_9fa48("795"), `Activar o desactivar ${rule.name}`)} disabled={stryMutAct_9fa48("798") ? busyToggle !== rule.id : stryMutAct_9fa48("797") ? false : stryMutAct_9fa48("796") ? true : (stryCov_9fa48("796", "797", "798"), busyToggle === rule.id)} onClick={stryMutAct_9fa48("799") ? () => undefined : (stryCov_9fa48("799"), () => void toggle(rule))}>
                  {rule.enabled ? stryMutAct_9fa48("800") ? "" : (stryCov_9fa48("800"), "Activa") : stryMutAct_9fa48("801") ? "" : (stryCov_9fa48("801"), "Inactiva")}
                </button>
                <button type="button" onClick={stryMutAct_9fa48("802") ? () => undefined : (stryCov_9fa48("802"), () => setEditing(editingOf(rule)))}>
                  {stryMutAct_9fa48("803") ? `` : (stryCov_9fa48("803"), `Editar ${rule.name}`)}
                </button>
                <button type="button" onClick={stryMutAct_9fa48("804") ? () => undefined : (stryCov_9fa48("804"), () => void openHistory(rule, null))}>
                  {stryMutAct_9fa48("805") ? `` : (stryCov_9fa48("805"), `Historial de ${rule.name}`)}
                </button>
              </li>))}
          </ul>
          <button type="button" onClick={stryMutAct_9fa48("806") ? () => undefined : (stryCov_9fa48("806"), () => setEditing(blank(stryMutAct_9fa48("807") ? projects[0]?.id && "" : (stryCov_9fa48("807"), (stryMutAct_9fa48("808") ? projects[0].id : (stryCov_9fa48("808"), projects[0]?.id)) ?? (stryMutAct_9fa48("809") ? "Stryker was here!" : (stryCov_9fa48("809"), ""))))))}>
            Nueva regla
          </button>
        </>)}
      {stryMutAct_9fa48("812") ? editing || <section aria-label="Editor de regla">
          <Field id="automation-name" label="Nombre" value={editing.name} error={fields.name} onChange={value => setEditing({
          ...editing,
          name: value
        })} />
          <label htmlFor="automation-trigger">Disparador</label>
          <select id="automation-trigger" value={editing.eventType} onChange={event => setEditing({
          ...editing,
          eventType: event.target.value as EventType
        })}>
            {EVENT_TYPES.map(type => <option key={type} value={type}>
                {TRIGGER_LABELS[type]}
              </option>)}
          </select>
          <label htmlFor="automation-condition">Sólo en el proyecto</label>
          <select id="automation-condition" value={editing.conditionProjectId} onChange={event => setEditing({
          ...editing,
          conditionProjectId: event.target.value
        })}>
            <option value="">Cualquiera</option>
            {projects.map(project => <option key={project.id} value={project.id}>
                {project.name}
              </option>)}
          </select>
          <Field id="automation-title" label="Título de la tarea" value={editing.titleTemplate} error={fields["action.titleTemplate"]} onChange={value => setEditing({
          ...editing,
          titleTemplate: value
        })} />
          <Field id="automation-criterion" label="Criterio de la tarea" value={editing.criterionTemplate} error={fields["action.criterionTemplate"]} onChange={value => setEditing({
          ...editing,
          criterionTemplate: value
        })} />
          <p data-testid="automation-placeholders">
            Marcadores disponibles: {PLACEHOLDERS.join(", ")}
          </p>
          <p data-testid="automation-preview">
            {previewTemplate(editing.titleTemplate)}
          </p>
          {conflict && <div role="alert">
              <p>Otra pestaña cambió esta regla</p>
              <button type="button" onClick={() => void reloadEditing()}>
                Cargar versión actual
              </button>
            </div>}
          <button type="button" disabled={saving} onClick={() => void save()}>
            Guardar
          </button>
          <button type="button" onClick={() => void simulate()}>
            Simular
          </button>
          {simulation && <div role="status" aria-label="Resultado de la simulación">
              <p>
                {`${simulation.evaluatedEvents} eventos evaluados, ${simulation.matches.length} coincidencias`}
              </p>
              <ul aria-label="Coincidencias">
                {simulation.matches.map(match => <li key={match.eventId}>
                    <span>{TRIGGER_LABELS[match.eventType]}</span>
                    <span>{match.occurredAt}</span>
                    <span>
                      {match.preview.type === "CREATE_TASK" ? match.preview.title : "Aviso al webhook"}
                    </span>
                    {match.preview.type === "CREATE_TASK" && match.preview.wouldFail && <span>
                          {`Fallaría: ${FAILURE_LABELS[match.preview.wouldFail] ?? match.preview.wouldFail}`}
                        </span>}
                  </li>)}
              </ul>
            </div>}
        </section> : stryMutAct_9fa48("811") ? false : stryMutAct_9fa48("810") ? true : (stryCov_9fa48("810", "811", "812"), editing && <section aria-label="Editor de regla">
          <Field id="automation-name" label="Nombre" value={editing.name} error={fields.name} onChange={stryMutAct_9fa48("813") ? () => undefined : (stryCov_9fa48("813"), value => setEditing(stryMutAct_9fa48("814") ? {} : (stryCov_9fa48("814"), {
          ...editing,
          name: value
        })))} />
          <label htmlFor="automation-trigger">Disparador</label>
          <select id="automation-trigger" value={editing.eventType} onChange={stryMutAct_9fa48("815") ? () => undefined : (stryCov_9fa48("815"), event => setEditing(stryMutAct_9fa48("816") ? {} : (stryCov_9fa48("816"), {
          ...editing,
          eventType: event.target.value as EventType
        })))}>
            {EVENT_TYPES.map(stryMutAct_9fa48("817") ? () => undefined : (stryCov_9fa48("817"), type => <option key={type} value={type}>
                {TRIGGER_LABELS[type]}
              </option>))}
          </select>
          <label htmlFor="automation-condition">Sólo en el proyecto</label>
          <select id="automation-condition" value={editing.conditionProjectId} onChange={stryMutAct_9fa48("818") ? () => undefined : (stryCov_9fa48("818"), event => setEditing(stryMutAct_9fa48("819") ? {} : (stryCov_9fa48("819"), {
          ...editing,
          conditionProjectId: event.target.value
        })))}>
            <option value="">Cualquiera</option>
            {projects.map(stryMutAct_9fa48("820") ? () => undefined : (stryCov_9fa48("820"), project => <option key={project.id} value={project.id}>
                {project.name}
              </option>))}
          </select>
          <Field id="automation-title" label="Título de la tarea" value={editing.titleTemplate} error={fields[stryMutAct_9fa48("821") ? "" : (stryCov_9fa48("821"), "action.titleTemplate")]} onChange={stryMutAct_9fa48("822") ? () => undefined : (stryCov_9fa48("822"), value => setEditing(stryMutAct_9fa48("823") ? {} : (stryCov_9fa48("823"), {
          ...editing,
          titleTemplate: value
        })))} />
          <Field id="automation-criterion" label="Criterio de la tarea" value={editing.criterionTemplate} error={fields[stryMutAct_9fa48("824") ? "" : (stryCov_9fa48("824"), "action.criterionTemplate")]} onChange={stryMutAct_9fa48("825") ? () => undefined : (stryCov_9fa48("825"), value => setEditing(stryMutAct_9fa48("826") ? {} : (stryCov_9fa48("826"), {
          ...editing,
          criterionTemplate: value
        })))} />
          <p data-testid="automation-placeholders">
            Marcadores disponibles: {PLACEHOLDERS.join(stryMutAct_9fa48("827") ? "" : (stryCov_9fa48("827"), ", "))}
          </p>
          <p data-testid="automation-preview">
            {previewTemplate(editing.titleTemplate)}
          </p>
          {stryMutAct_9fa48("830") ? conflict || <div role="alert">
              <p>Otra pestaña cambió esta regla</p>
              <button type="button" onClick={() => void reloadEditing()}>
                Cargar versión actual
              </button>
            </div> : stryMutAct_9fa48("829") ? false : stryMutAct_9fa48("828") ? true : (stryCov_9fa48("828", "829", "830"), conflict && <div role="alert">
              <p>Otra pestaña cambió esta regla</p>
              <button type="button" onClick={stryMutAct_9fa48("831") ? () => undefined : (stryCov_9fa48("831"), () => void reloadEditing())}>
                Cargar versión actual
              </button>
            </div>)}
          <button type="button" disabled={saving} onClick={stryMutAct_9fa48("832") ? () => undefined : (stryCov_9fa48("832"), () => void save())}>
            Guardar
          </button>
          <button type="button" onClick={stryMutAct_9fa48("833") ? () => undefined : (stryCov_9fa48("833"), () => void simulate())}>
            Simular
          </button>
          {stryMutAct_9fa48("836") ? simulation || <div role="status" aria-label="Resultado de la simulación">
              <p>
                {`${simulation.evaluatedEvents} eventos evaluados, ${simulation.matches.length} coincidencias`}
              </p>
              <ul aria-label="Coincidencias">
                {simulation.matches.map(match => <li key={match.eventId}>
                    <span>{TRIGGER_LABELS[match.eventType]}</span>
                    <span>{match.occurredAt}</span>
                    <span>
                      {match.preview.type === "CREATE_TASK" ? match.preview.title : "Aviso al webhook"}
                    </span>
                    {match.preview.type === "CREATE_TASK" && match.preview.wouldFail && <span>
                          {`Fallaría: ${FAILURE_LABELS[match.preview.wouldFail] ?? match.preview.wouldFail}`}
                        </span>}
                  </li>)}
              </ul>
            </div> : stryMutAct_9fa48("835") ? false : stryMutAct_9fa48("834") ? true : (stryCov_9fa48("834", "835", "836"), simulation && <div role="status" aria-label="Resultado de la simulación">
              <p>
                {stryMutAct_9fa48("837") ? `` : (stryCov_9fa48("837"), `${simulation.evaluatedEvents} eventos evaluados, ${simulation.matches.length} coincidencias`)}
              </p>
              <ul aria-label="Coincidencias">
                {simulation.matches.map(stryMutAct_9fa48("838") ? () => undefined : (stryCov_9fa48("838"), match => <li key={match.eventId}>
                    <span>{TRIGGER_LABELS[match.eventType]}</span>
                    <span>{match.occurredAt}</span>
                    <span>
                      {(stryMutAct_9fa48("841") ? match.preview.type !== "CREATE_TASK" : stryMutAct_9fa48("840") ? false : stryMutAct_9fa48("839") ? true : (stryCov_9fa48("839", "840", "841"), match.preview.type === (stryMutAct_9fa48("842") ? "" : (stryCov_9fa48("842"), "CREATE_TASK")))) ? match.preview.title : stryMutAct_9fa48("843") ? "" : (stryCov_9fa48("843"), "Aviso al webhook")}
                    </span>
                    {stryMutAct_9fa48("846") ? match.preview.type === "CREATE_TASK" && match.preview.wouldFail || <span>
                          {`Fallaría: ${FAILURE_LABELS[match.preview.wouldFail] ?? match.preview.wouldFail}`}
                        </span> : stryMutAct_9fa48("845") ? false : stryMutAct_9fa48("844") ? true : (stryCov_9fa48("844", "845", "846"), (stryMutAct_9fa48("848") ? match.preview.type === "CREATE_TASK" || match.preview.wouldFail : stryMutAct_9fa48("847") ? true : (stryCov_9fa48("847", "848"), (stryMutAct_9fa48("850") ? match.preview.type !== "CREATE_TASK" : stryMutAct_9fa48("849") ? true : (stryCov_9fa48("849", "850"), match.preview.type === (stryMutAct_9fa48("851") ? "" : (stryCov_9fa48("851"), "CREATE_TASK")))) && match.preview.wouldFail)) && <span>
                          {stryMutAct_9fa48("852") ? `` : (stryCov_9fa48("852"), `Fallaría: ${stryMutAct_9fa48("853") ? FAILURE_LABELS[match.preview.wouldFail] && match.preview.wouldFail : (stryCov_9fa48("853"), FAILURE_LABELS[match.preview.wouldFail] ?? match.preview.wouldFail)}`)}
                        </span>)}
                  </li>))}
              </ul>
            </div>)}
        </section>)}
      {stryMutAct_9fa48("856") ? history || <section aria-label="Historial">
          <h2 data-testid="automation-history-title">{`Ejecuciones de ${history.rule.name}`}</h2>
          <ul aria-label="Ejecuciones">
            {history.items.map(run => <li key={run.id}>
                <span>{RUN_LABELS[run.status]}</span>
                <span>{run.executedAt}</span>
                {run.errorCode && <span>{run.errorCode}</span>}
                {run.createdTaskId && <a href={`/proyectos/${history.rule.action.type === "CREATE_TASK" ? history.rule.action.projectId : ""}/tareas/${run.createdTaskId}`}>
                    Ver la tarea creada
                  </a>}
              </li>)}
          </ul>
          {history.nextCursor !== null && <button type="button" onClick={() => void openHistory(history.rule, history.nextCursor)}>
              Cargar más
            </button>}
        </section> : stryMutAct_9fa48("855") ? false : stryMutAct_9fa48("854") ? true : (stryCov_9fa48("854", "855", "856"), history && <section aria-label="Historial">
          <h2 data-testid="automation-history-title">{stryMutAct_9fa48("857") ? `` : (stryCov_9fa48("857"), `Ejecuciones de ${history.rule.name}`)}</h2>
          <ul aria-label="Ejecuciones">
            {history.items.map(stryMutAct_9fa48("858") ? () => undefined : (stryCov_9fa48("858"), run => <li key={run.id}>
                <span>{RUN_LABELS[run.status]}</span>
                <span>{run.executedAt}</span>
                {stryMutAct_9fa48("861") ? run.errorCode || <span>{run.errorCode}</span> : stryMutAct_9fa48("860") ? false : stryMutAct_9fa48("859") ? true : (stryCov_9fa48("859", "860", "861"), run.errorCode && <span>{run.errorCode}</span>)}
                {stryMutAct_9fa48("864") ? run.createdTaskId || <a href={`/proyectos/${history.rule.action.type === "CREATE_TASK" ? history.rule.action.projectId : ""}/tareas/${run.createdTaskId}`}>
                    Ver la tarea creada
                  </a> : stryMutAct_9fa48("863") ? false : stryMutAct_9fa48("862") ? true : (stryCov_9fa48("862", "863", "864"), run.createdTaskId && <a href={stryMutAct_9fa48("865") ? `` : (stryCov_9fa48("865"), `/proyectos/${(stryMutAct_9fa48("868") ? history.rule.action.type !== "CREATE_TASK" : stryMutAct_9fa48("867") ? false : stryMutAct_9fa48("866") ? true : (stryCov_9fa48("866", "867", "868"), history.rule.action.type === (stryMutAct_9fa48("869") ? "" : (stryCov_9fa48("869"), "CREATE_TASK")))) ? history.rule.action.projectId : stryMutAct_9fa48("870") ? "Stryker was here!" : (stryCov_9fa48("870"), "")}/tareas/${run.createdTaskId}`)}>
                    Ver la tarea creada
                  </a>)}
              </li>))}
          </ul>
          {stryMutAct_9fa48("873") ? history.nextCursor !== null || <button type="button" onClick={() => void openHistory(history.rule, history.nextCursor)}>
              Cargar más
            </button> : stryMutAct_9fa48("872") ? false : stryMutAct_9fa48("871") ? true : (stryCov_9fa48("871", "872", "873"), (stryMutAct_9fa48("875") ? history.nextCursor === null : stryMutAct_9fa48("874") ? true : (stryCov_9fa48("874", "875"), history.nextCursor !== null)) && <button type="button" onClick={stryMutAct_9fa48("876") ? () => undefined : (stryCov_9fa48("876"), () => void openHistory(history.rule, history.nextCursor))}>
              Cargar más
            </button>)}
        </section>)}
    </main>;
  }
}
function toDraft(rule: Automation): AutomationDraft {
  if (stryMutAct_9fa48("877")) {
    {}
  } else {
    stryCov_9fa48("877");
    return stryMutAct_9fa48("878") ? {} : (stryCov_9fa48("878"), {
      name: rule.name,
      enabled: rule.enabled,
      trigger: rule.trigger,
      condition: rule.condition,
      action: rule.action
    });
  }
}
function controlIdOf(field: string) {
  if (stryMutAct_9fa48("879")) {
    {}
  } else {
    stryCov_9fa48("879");
    return stryMutAct_9fa48("880") ? {
      name: "automation-name",
      "action.titleTemplate": "automation-title",
      "action.criterionTemplate": "automation-criterion",
      "trigger.eventType": "automation-trigger",
      "condition.projectId": "automation-condition"
    }[field] && "automation-name" : (stryCov_9fa48("880"), (stryMutAct_9fa48("881") ? {} : (stryCov_9fa48("881"), {
      name: stryMutAct_9fa48("882") ? "" : (stryCov_9fa48("882"), "automation-name"),
      "action.titleTemplate": stryMutAct_9fa48("883") ? "" : (stryCov_9fa48("883"), "automation-title"),
      "action.criterionTemplate": stryMutAct_9fa48("884") ? "" : (stryCov_9fa48("884"), "automation-criterion"),
      "trigger.eventType": stryMutAct_9fa48("885") ? "" : (stryCov_9fa48("885"), "automation-trigger"),
      "condition.projectId": stryMutAct_9fa48("886") ? "" : (stryCov_9fa48("886"), "automation-condition")
    }))[field] ?? (stryMutAct_9fa48("887") ? "" : (stryCov_9fa48("887"), "automation-name")));
  }
}
function Field({
  id,
  label,
  value,
  error,
  onChange
}: {
  id: string;
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  if (stryMutAct_9fa48("888")) {
    {}
  } else {
    stryCov_9fa48("888");
    return <>
      <label htmlFor={id}>{label}</label>
      <input id={id} value={value} aria-invalid={error ? stryMutAct_9fa48("889") ? false : (stryCov_9fa48("889"), true) : undefined} aria-describedby={error ? stryMutAct_9fa48("890") ? `` : (stryCov_9fa48("890"), `${id}-error`) : undefined} onChange={stryMutAct_9fa48("891") ? () => undefined : (stryCov_9fa48("891"), event => onChange(event.target.value))} />
      {stryMutAct_9fa48("894") ? error || <p id={`${id}-error`}>{error}</p> : stryMutAct_9fa48("893") ? false : stryMutAct_9fa48("892") ? true : (stryCov_9fa48("892", "893", "894"), error && <p id={stryMutAct_9fa48("895") ? `` : (stryCov_9fa48("895"), `${id}-error`)}>{error}</p>)}
    </>;
  }
}