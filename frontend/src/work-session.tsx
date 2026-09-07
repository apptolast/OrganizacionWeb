import { WorkSessionStatePanel } from "./work-session-state";
import { useEffect, useState, useRef, useLayoutEffect, useId } from "react";
import {
  readActiveWorkSession,
  startWorkSession,
  recoverWorkSession,
  readWorkSessionError,
  type SessionStart,
} from "./work-session-api";
import { RouteLink } from "./navigation";
import "./work-session.scss";

type Props = {
  projectId: string;
  taskId: string;
  taskTitle: string;
  taskStatus?: "pending" | "completed";
  projectStatus?: "idea" | "active" | "paused" | "completed";
  onAccessFailure: (status: number) => void;
};
export function WorkSession(props: Props) {
  return <Session key={`${props.projectId}:${props.taskId}`} {...props} />;
}
function Session(props: Props) {
  const id = useId();
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (interacted.current && document.activeElement === document.body)
      heading.current?.focus();
  });
  const onAccessFailure = props.onAccessFailure;
  const [active, setActive] = useState<SessionStart | null>();
  const [lookupFailed, setLookupFailed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [minutes, setMinutes] = useState("");
  const [busy, setBusy] = useState(false);
  const [checking, setChecking] = useState(false);
  const [confirmed, setConfirmed] = useState<SessionStart>();
  const [uncertain, setUncertain] = useState(false);
  const [mayResend, setMayResend] = useState(false);
  const [fieldError, setFieldError] = useState("");
  const [rejection, setRejection] = useState("");
  const [formOpen, setFormOpen] = useState(true);
  const retained = useRef<{ key: string; minutes: number } | null>(null);
  const command = useRef<AbortController | null>(null);
  const lookup = useRef<AbortController | null>(null);
  const absenceKnown = active === null && !loading && !lookupFailed;
  const eligible =
    props.taskStatus === "pending" &&
    props.projectStatus !== undefined &&
    props.projectStatus !== "completed";
  const canStart = absenceKnown && eligible;
  useEffect(() => () => command.current?.abort(), []);
  async function send(check = false) {
    if (command.current) return;
    interacted.current = true;
    if (!retained.current && !canStart) return;
    if (
      !retained.current &&
      (!Number.isInteger(Number(minutes)) ||
        Number(minutes) < 1 ||
        Number(minutes) > 1440)
    ) {
      setFieldError("Elige una duración entera entre 1 y 1440 minutos.");
      return;
    }
    setFieldError("");
    setRejection("");
    const controller = new AbortController();
    command.current = controller;
    retained.current ??= { key: crypto.randomUUID(), minutes: Number(minutes) };
    setBusy(true);
    setChecking(check);
    try {
      const result = await (check ? recoverWorkSession : startWorkSession)(
        props.projectId,
        props.taskId,
        retained.current.minutes,
        retained.current.key,
        command.current.signal,
      );
      if (controller.signal.aborted) return;
      lookup.current?.abort();
      setLoading(false);
      setLookupFailed(false);
      setConfirmed(result);
      setActive(result);
      setUncertain(false);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof Response && error.status === 401) {
        onAccessFailure(401);
        return;
      }
      const problem = await readWorkSessionError(error);
      if (controller.signal.aborted) return;
      if (problem?.code === "RESOURCE_NOT_FOUND") {
        onAccessFailure(404);
        return;
      }
      if (
        problem?.code === "PROJECT_COMPLETED" ||
        problem?.code === "TASK_COMPLETED" ||
        problem?.code === "WORK_SESSION_TIME_OUT_OF_RANGE" ||
        problem?.code === "WORK_SESSION_ALREADY_ACTIVE"
      ) {
        if (problem.code === "WORK_SESSION_ALREADY_ACTIVE")
          setActive(undefined);
        setRejection(problem.title);
        retained.current = null;
        setUncertain(false);
        setMayResend(false);
        return;
      }
      if (problem?.code === "VALIDATION_ERROR") {
        setFieldError(
          problem.errors.find((error) => error.field === "plannedMinutes")
            ?.message ?? problem.title,
        );
        retained.current = null;
        setUncertain(false);
        setMayResend(false);
        return;
      }
      setUncertain(true);
      setMayResend(
        problem?.code === "CSRF_INVALID" ||
          (check && problem?.code === "WORK_SESSION_NOT_FOUND"),
      );
    } finally {
      if (!controller.signal.aborted && command.current === controller) {
        setBusy(false);
        command.current = null;
      }
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    lookup.current = controller;
    void readActiveWorkSession(controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setActive(value);
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (error instanceof Response && error.status === 401)
          onAccessFailure(401);
        else setLookupFailed(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [refresh, onAccessFailure]);
  return (
    <section
      className="task-blocks work-session"
      aria-labelledby={`${id}-heading`}
    >
      <h2 id={`${id}-heading`} ref={heading} tabIndex={-1}>
        Sesión de trabajo
      </h2>
      <p>
        Elige cuánto tiempo te propones trabajar. El fin previsto no cierra la
        sesión automáticamente ni acredita tiempo neto o una tarea completada.
      </p>
      {loading && <p role="status">Consultando sesión activa</p>}
      {busy && (
        <p role="status">
          {checking ? "Comprobando inicio" : "Iniciando sesión de trabajo"}
        </p>
      )}
      {lookupFailed && (
        <p role="alert">No se ha podido consultar la sesión activa.</p>
      )}
      {rejection && <p role="alert">{rejection}</p>}
      <button
        type="button"
        onClick={() => {
          setLoading(true);
          setLookupFailed(false);
          setRefresh((value) => value + 1);
        }}
      >
        Actualizar sesión activa
      </button>
      {confirmed && (
        <>
          <p role="status">Sesión iniciada</p>
          <SessionFacts session={confirmed} />
        </>
      )}
      {active && active.id !== confirmed?.id && (
        <SessionFacts session={active} />
      )}
      {active && (
        <>
          <p>La pausa no desplaza el fin previsto de la sesión.</p>
          <WorkSessionStatePanel
            session={active}
            onAccessFailure={props.onAccessFailure}
          />
        </>
      )}
      {(active === null || uncertain || busy) && (
        <>
          {absenceKnown && !busy && !uncertain && (
            <p>No hay una sesión de trabajo activa.</p>
          )}
          <p>{props.taskTitle}</p>
          {!eligible && !uncertain && (
            <p>
              Para iniciar trabajo necesitamos confirmar una tarea pendiente y
              un proyecto no completado.
            </p>
          )}
          {formOpen ? (
            <form
              className="task-form"
              aria-label="Iniciar sesión de trabajo"
              noValidate
              onSubmit={(event) => {
                event.preventDefault();
                if (!uncertain) void send();
              }}
            >
              <div className="field">
                <label htmlFor={`${id}-minutes`}>
                  Duración prevista (minutos)
                </label>
                <input
                  id={`${id}-minutes`}
                  type="number"
                  min="1"
                  max="1440"
                  step="1"
                  value={minutes}
                  readOnly={busy || uncertain}
                  aria-invalid={Boolean(fieldError)}
                  aria-describedby={
                    fieldError ? `${id}-minutes-error` : undefined
                  }
                  onChange={(event) => setMinutes(event.target.value)}
                />
                {fieldError && (
                  <p id={`${id}-minutes-error`} role="alert">
                    {fieldError}
                  </p>
                )}
              </div>
              {uncertain ? (
                <>
                  <p role="alert">
                    No podemos confirmar el inicio. Conservamos su duración e
                    identificación.
                  </p>
                  <button
                    type="button"
                    aria-disabled={busy}
                    onClick={() => void send(true)}
                  >
                    Comprobar inicio
                  </button>
                </>
              ) : (
                <button aria-disabled={busy || !canStart}>
                  Empezar a trabajar
                </button>
              )}
              {uncertain && mayResend && (
                <button
                  type="button"
                  aria-disabled={busy}
                  onClick={() => void send()}
                >
                  Reenviar el mismo inicio
                </button>
              )}
              {(busy || uncertain) && (
                <p>
                  Cerrar este formulario no revoca el inicio transmitido. Al
                  volver podrás consultar la sesión activa.
                </p>
              )}
              <button
                type="button"
                onClick={() => {
                  interacted.current = true;
                  setFormOpen(false);
                }}
              >
                Cerrar formulario
              </button>
            </form>
          ) : (
            <button type="button" onClick={() => setFormOpen(true)}>
              Mostrar formulario
            </button>
          )}
        </>
      )}
    </section>
  );
}

function SessionFacts({ session }: { session: SessionStart }) {
  let fallback = false;
  let formatter: Intl.DateTimeFormat;
  try {
    formatter = new Intl.DateTimeFormat("es-ES", {
      timeZone: session.zoneId,
      dateStyle: "long",
      timeStyle: "long",
    });
  } catch {
    fallback = true;
    formatter = new Intl.DateTimeFormat("es-ES", {
      timeZone: "UTC",
      dateStyle: "long",
      timeStyle: "long",
    });
  }
  return (
    <article>
      <p>
        Inicio:{" "}
        <time dateTime={session.startedAt}>
          {formatter.format(new Date(session.startedAt))}
        </time>
      </p>
      <p>Duración prevista: {session.plannedMinutes} minutos</p>
      <p>
        Fin previsto:{" "}
        <time dateTime={session.plannedEndAt}>
          {formatter.format(new Date(session.plannedEndAt))}
        </time>
      </p>
      <p>
        Zona: {session.zoneId}
        {fallback && " (horas mostradas en UTC)"}
      </p>
      <RouteLink
        href={`/proyectos/${session.projectId}/tareas/${session.taskId}`}
      >
        Ir a la tarea de esta sesión
      </RouteLink>
    </article>
  );
}
