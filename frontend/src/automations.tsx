import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import {
  AutomationConflict,
  AutomationFieldErrors,
  EVENT_TYPES,
  createAutomation,
  readAutomationRuns,
  readAutomations,
  replaceAutomation,
  simulateAutomation,
} from "./automations-api";
import type {
  Automation,
  AutomationAction,
  AutomationDraft,
  AutomationMatch,
  AutomationRun,
  EventType,
} from "./automations-api";
import { readProjects } from "./read-projects-api";
import type { ProjectSummary } from "./read-projects-api";

const TRIGGER_LABELS: Record<EventType, string> = {
  "ProjectCreated.v1": "Proyecto creado",
  "ProjectUpdated.v1": "Proyecto editado",
  "ProjectStatusChanged.v1": "Estado de proyecto cambiado",
  "TaskCreated.v1": "Tarea creada",
  "SubtaskCreated.v1": "Subtarea creada",
  "TaskStatusChanged.v1": "Estado de tarea cambiado",
  "BlockPlanned.v1": "Bloque planificado",
  "BlockChanged.v1": "Bloque modificado",
  "WorkSessionStarted.v1": "Sesión iniciada",
  "WorkSessionStateChanged.v1": "Sesión pausada o reanudada",
  "WorkSessionExtended.v1": "Sesión ampliada",
  "WorkSessionClosed.v1": "Sesión cerrada",
};

const RUN_LABELS: Record<AutomationRun["status"], string> = {
  succeeded: "Correcta",
  retry: "Reintento",
  failed: "Fallida",
};

const FAILURE_LABELS: Record<string, string> = {
  PROJECT_COMPLETED: "proyecto completado",
  TITLE_TOO_LONG: "título demasiado largo",
  CRITERION_TOO_LONG: "criterio demasiado largo",
  ENDPOINT_NOT_FOUND: "endpoint no encontrado",
  TARGET_NOT_FOUND: "destino no encontrado",
};

const PLACEHOLDERS = [
  "{{event.type}}",
  "{{task.title}}",
  "{{project.name}}",
  "{{occurredAt}}",
] as const;

const SAMPLES: Record<string, string> = {
  "{{event.type}}": "TaskCreated.v1",
  "{{task.title}}": "Redactar informe",
  "{{project.name}}": "Marketing",
  "{{occurredAt}}": "2026-09-08T10:15:30.123456Z",
};

/** Substitutes the four known markers with sample values; everything else stays literal text. */
export function previewTemplate(template: string) {
  return PLACEHOLDERS.reduce(
    (text, placeholder) => text.split(placeholder).join(SAMPLES[placeholder]),
    template,
  );
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
  return {
    rule: null,
    name: "",
    eventType: "TaskCreated.v1",
    conditionProjectId: "",
    projectId,
    titleTemplate: "Revisar {{task.title}}",
    criterionTemplate: "",
    estimatedMinutes: "",
  };
}

function editingOf(rule: Automation): Editing {
  return {
    rule,
    name: rule.name,
    eventType: rule.trigger.eventType,
    conditionProjectId: rule.condition?.projectId ?? "",
    projectId: rule.action.type === "CREATE_TASK" ? rule.action.projectId : "",
    titleTemplate:
      rule.action.type === "CREATE_TASK" ? rule.action.titleTemplate : "",
    criterionTemplate:
      rule.action.type === "CREATE_TASK"
        ? (rule.action.criterionTemplate ?? "")
        : "",
    estimatedMinutes:
      rule.action.type === "CREATE_TASK" &&
      rule.action.estimatedMinutes !== null
        ? String(rule.action.estimatedMinutes)
        : "",
  };
}

/** Este editor sólo compone acciones CREATE_TASK: la de cualquier otra regla vuelve intacta. */
function actionOf(editing: Editing): AutomationAction {
  if (editing.rule && editing.rule.action.type !== "CREATE_TASK")
    return editing.rule.action;
  return {
    type: "CREATE_TASK",
    projectId: editing.projectId,
    titleTemplate: editing.titleTemplate,
    criterionTemplate:
      editing.criterionTemplate === "" ? null : editing.criterionTemplate,
    estimatedMinutes:
      editing.estimatedMinutes === "" ? null : Number(editing.estimatedMinutes),
  };
}

function draftOf(editing: Editing): AutomationDraft {
  return {
    name: editing.name,
    enabled: editing.rule?.enabled ?? true,
    trigger: { eventType: editing.eventType },
    condition:
      editing.conditionProjectId === ""
        ? null
        : { projectId: editing.conditionProjectId },
    action: actionOf(editing),
  };
}

export function Automations({ owner }: { owner: string }) {
  return <AutomationsWorkspace key={owner} />;
}

function AutomationsWorkspace() {
  const [rules, setRules] = useState<Automation[] | null>(null);
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [failed, setFailed] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [editing, setEditing] = useState<Editing | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [conflict, setConflict] = useState(false);
  const [saving, setSaving] = useState(false);
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
  const mounted = useRef(true);
  const heading = useRef<HTMLHeadingElement>(null);
  const invalid = useRef<string | null>(null);

  useLayoutEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      live.current?.abort();
      runsRequest.current?.abort();
      writeRequest.current?.abort();
    };
  }, []);

  /** Only touches state after the answer, so it is safe to start from an effect. */
  const fetchRules = useCallback(async () => {
    live.current?.abort();
    const controller = new AbortController();
    live.current = controller;
    try {
      const items = await readAutomations(controller.signal);
      if (!mounted.current || live.current !== controller) return;
      setRules(items);
      setFailed(false);
    } catch {
      if (!mounted.current || live.current !== controller) return;
      setFailed(true);
    }
  }, []);

  /** The retry button may clear the screen at once: it is an event, not an effect. */
  function load() {
    setRules(null);
    setFailed(false);
    void fetchRules();
  }

  useEffect(() => {
    const controller = new AbortController();
    live.current = controller;
    readAutomations(controller.signal)
      .then((items) => {
        if (!mounted.current || live.current !== controller) return;
        setRules(items);
        setFailed(false);
      })
      .catch(() => {
        if (mounted.current && live.current === controller) setFailed(true);
      });
    return () => controller.abort();
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    void readProjects("/proyectos", controller.signal)
      .then((page) => {
        if (mounted.current && "items" in page) setProjects(page.items);
      })
      .catch(() => {});
    return () => controller.abort();
  }, []);

  useLayoutEffect(() => {
    if (!invalid.current) return;
    document.getElementById(invalid.current)?.focus();
    invalid.current = null;
  }, [fields]);

  function nameOfProject(id: string) {
    return projects.find((project) => project.id === id)?.name ?? id;
  }

  async function save() {
    if (!editing || saving) return;
    writeRequest.current?.abort();
    const controller = new AbortController();
    writeRequest.current = controller;
    setSaving(true);
    setFields({});
    setConflict(false);
    setNotice(null);
    try {
      const draft = draftOf(editing);
      const saved = editing.rule
        ? await replaceAutomation(
            editing.rule.id,
            editing.rule.version,
            draft,
            controller.signal,
          )
        : await createAutomation(draft, controller.signal);
      if (!mounted.current || writeRequest.current !== controller) return;
      setEditing(null);
      setRules((current) =>
        current === null
          ? [saved]
          : current.some((rule) => rule.id === saved.id)
            ? current.map((rule) => (rule.id === saved.id ? saved : rule))
            : [...current, saved],
      );
    } catch (error) {
      if (!mounted.current || writeRequest.current !== controller) return;
      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else if (error instanceof AutomationConflict) setConflict(true);
      else setNotice("No se ha podido guardar. Inténtalo de nuevo.");
    } finally {
      if (mounted.current && writeRequest.current === controller)
        setSaving(false);
    }
  }

  async function simulate() {
    if (!editing) return;
    const controller = new AbortController();
    writeRequest.current?.abort();
    writeRequest.current = controller;
    setFields({});
    setNotice(null);
    try {
      const result = await simulateAutomation(
        draftOf(editing),
        controller.signal,
      );
      if (!mounted.current || writeRequest.current !== controller) return;
      setSimulation(result);
    } catch (error) {
      if (!mounted.current || writeRequest.current !== controller) return;
      if (error instanceof AutomationFieldErrors) {
        invalid.current = controlIdOf(Object.keys(error.fields)[0]);
        setFields(error.fields);
      } else setNotice("No se ha podido simular. Inténtalo de nuevo.");
    }
  }

  async function toggle(rule: Automation) {
    if (busyToggle) return;
    const controller = new AbortController();
    writeRequest.current?.abort();
    writeRequest.current = controller;
    setBusyToggle(rule.id);
    setNotice(null);
    try {
      const saved = await replaceAutomation(
        rule.id,
        rule.version,
        { ...toDraft(rule), enabled: !rule.enabled },
        controller.signal,
      );
      if (!mounted.current || writeRequest.current !== controller) return;
      setRules((current) =>
        (current ?? []).map((item) => (item.id === saved.id ? saved : item)),
      );
    } catch {
      if (!mounted.current || writeRequest.current !== controller) return;
      setNotice("No se ha podido cambiar el estado de la regla.");
    } finally {
      if (mounted.current && writeRequest.current === controller)
        setBusyToggle(null);
    }
  }

  /** The owner asked for the current version on purpose; only now is the draft replaced. */
  async function reloadEditing() {
    const id = editing?.rule?.id;
    setConflict(false);
    live.current?.abort();
    const controller = new AbortController();
    live.current = controller;
    try {
      const items = await readAutomations(controller.signal);
      if (!mounted.current || live.current !== controller) return;
      setRules(items);
      const fresh = items.find((rule) => rule.id === id);
      setEditing(fresh ? editingOf(fresh) : null);
    } catch {
      if (!mounted.current || live.current !== controller) return;
      setNotice("No se ha podido cargar la versión actual.");
    }
  }

  async function openHistory(rule: Automation, cursor: string | null) {
    runsRequest.current?.abort();
    const controller = new AbortController();
    runsRequest.current = controller;
    if (cursor === null) setHistory({ rule, items: [], nextCursor: null });
    try {
      const page = await readAutomationRuns(rule.id, cursor, controller.signal);
      if (!mounted.current || runsRequest.current !== controller) return;
      setHistory((current) => ({
        rule,
        items:
          cursor === null
            ? page.items
            : [...(current?.items ?? []), ...page.items],
        nextCursor: page.nextCursor,
      }));
    } catch {
      if (!mounted.current || runsRequest.current !== controller) return;
      setNotice("No se ha podido cargar el historial.");
    }
  }

  return (
    <main id="proyectos" className="automations">
      <h1 ref={heading} tabIndex={-1}>
        Automatizaciones
      </h1>
      {notice && <p role="alert">{notice}</p>}
      {rules === null && !failed && (
        <p role="status">Cargando automatizaciones…</p>
      )}
      {failed && (
        <div role="alert">
          <p>No se han podido cargar tus automatizaciones.</p>
          <button type="button" onClick={load}>
            Reintentar
          </button>
        </div>
      )}
      {rules !== null && rules.length === 0 && (
        <div>
          <p>
            Todavía no tienes ninguna regla. Una regla observa un hecho de tus
            proyectos y, cuando ocurre, crea una tarea por ti.
          </p>
          <button
            type="button"
            onClick={() => setEditing(blank(projects[0]?.id ?? ""))}
          >
            Nueva regla
          </button>
        </div>
      )}
      {rules !== null && rules.length > 0 && (
        <>
          <ul aria-label="Reglas">
            {rules.map((rule) => (
              <li key={rule.id}>
                <span>{rule.name}</span>
                <span>{TRIGGER_LABELS[rule.trigger.eventType]}</span>
                <span>
                  {rule.action.type === "CREATE_TASK"
                    ? nameOfProject(rule.action.projectId)
                    : "Webhook"}
                </span>
                <button
                  type="button"
                  role="switch"
                  className={rule.enabled ? "is-active" : "is-inactive"}
                  aria-checked={rule.enabled}
                  aria-label={`Activar o desactivar ${rule.name}`}
                  disabled={busyToggle === rule.id}
                  onClick={() => void toggle(rule)}
                >
                  {rule.enabled ? "Activa" : "Inactiva"}
                </button>
                <button
                  type="button"
                  onClick={() => setEditing(editingOf(rule))}
                >
                  {`Editar ${rule.name}`}
                </button>
                <button
                  type="button"
                  onClick={() => void openHistory(rule, null)}
                >
                  {`Historial de ${rule.name}`}
                </button>
              </li>
            ))}
          </ul>
          <button
            type="button"
            onClick={() => setEditing(blank(projects[0]?.id ?? ""))}
          >
            Nueva regla
          </button>
        </>
      )}
      {editing && (
        <section aria-label="Editor de regla">
          <Field
            id="automation-name"
            label="Nombre"
            value={editing.name}
            error={fields.name}
            onChange={(value) => setEditing({ ...editing, name: value })}
          />
          <label htmlFor="automation-trigger">Disparador</label>
          <select
            id="automation-trigger"
            value={editing.eventType}
            onChange={(event) =>
              setEditing({
                ...editing,
                eventType: event.target.value as EventType,
              })
            }
          >
            {EVENT_TYPES.map((type) => (
              <option key={type} value={type}>
                {TRIGGER_LABELS[type]}
              </option>
            ))}
          </select>
          <label htmlFor="automation-condition">Sólo en el proyecto</label>
          <select
            id="automation-condition"
            value={editing.conditionProjectId}
            onChange={(event) =>
              setEditing({ ...editing, conditionProjectId: event.target.value })
            }
          >
            <option value="">Cualquiera</option>
            {projects.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </select>
          <Field
            id="automation-title"
            label="Título de la tarea"
            value={editing.titleTemplate}
            error={fields["action.titleTemplate"]}
            onChange={(value) =>
              setEditing({ ...editing, titleTemplate: value })
            }
          />
          <Field
            id="automation-criterion"
            label="Criterio de la tarea"
            value={editing.criterionTemplate}
            error={fields["action.criterionTemplate"]}
            onChange={(value) =>
              setEditing({ ...editing, criterionTemplate: value })
            }
          />
          <p data-testid="automation-placeholders">
            Marcadores disponibles: {PLACEHOLDERS.join(", ")}
          </p>
          <p data-testid="automation-preview">
            {previewTemplate(editing.titleTemplate)}
          </p>
          {conflict && (
            <div role="alert">
              <p>Otra pestaña cambió esta regla</p>
              <button type="button" onClick={() => void reloadEditing()}>
                Cargar versión actual
              </button>
            </div>
          )}
          <button type="button" disabled={saving} onClick={() => void save()}>
            Guardar
          </button>
          <button type="button" onClick={() => void simulate()}>
            Simular
          </button>
          {simulation && (
            <div role="status" aria-label="Resultado de la simulación">
              <p>
                {`${simulation.evaluatedEvents} eventos evaluados, ${simulation.matches.length} coincidencias`}
              </p>
              <ul aria-label="Coincidencias">
                {simulation.matches.map((match) => (
                  <li key={match.eventId}>
                    <span>{TRIGGER_LABELS[match.eventType]}</span>
                    <span>{match.occurredAt}</span>
                    <span>
                      {match.preview.type === "CREATE_TASK"
                        ? match.preview.title
                        : "Aviso al webhook"}
                    </span>
                    {match.preview.type === "CREATE_TASK" &&
                      match.preview.wouldFail && (
                        <span>
                          {`Fallaría: ${FAILURE_LABELS[match.preview.wouldFail] ?? match.preview.wouldFail}`}
                        </span>
                      )}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      )}
      {history && (
        <section aria-label="Historial">
          <h2 data-testid="automation-history-title">{`Ejecuciones de ${history.rule.name}`}</h2>
          <ul aria-label="Ejecuciones">
            {history.items.map((run) => (
              <li key={run.id}>
                <span>{RUN_LABELS[run.status]}</span>
                <span>{run.executedAt}</span>
                {run.errorCode && <span>{run.errorCode}</span>}
                {run.createdTaskId && (
                  <a
                    href={`/proyectos/${
                      history.rule.action.type === "CREATE_TASK"
                        ? history.rule.action.projectId
                        : ""
                    }/tareas/${run.createdTaskId}`}
                  >
                    Ver la tarea creada
                  </a>
                )}
              </li>
            ))}
          </ul>
          {history.nextCursor !== null && (
            <button
              type="button"
              onClick={() => void openHistory(history.rule, history.nextCursor)}
            >
              Cargar más
            </button>
          )}
        </section>
      )}
    </main>
  );
}

function toDraft(rule: Automation): AutomationDraft {
  return {
    name: rule.name,
    enabled: rule.enabled,
    trigger: rule.trigger,
    condition: rule.condition,
    action: rule.action,
  };
}

function controlIdOf(field: string) {
  return (
    {
      name: "automation-name",
      "action.titleTemplate": "automation-title",
      "action.criterionTemplate": "automation-criterion",
      "trigger.eventType": "automation-trigger",
      "condition.projectId": "automation-condition",
    }[field] ?? "automation-name"
  );
}

function Field({
  id,
  label,
  value,
  error,
  onChange,
}: {
  id: string;
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  return (
    <>
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && <p id={`${id}-error`}>{error}</p>}
    </>
  );
}
