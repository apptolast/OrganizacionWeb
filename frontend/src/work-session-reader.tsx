import { useEffect, useState, useId, useRef, useLayoutEffect } from "react";
import {
  readWorkSessionState,
  readWorkSessionClosure,
  changeWorkSession,
  type WorkSessionSnapshot,
  validNote,
  recoverWorkSessionChange,
  type WorkSessionIntent,
  readWorkSessionStateError,
} from "./work-session-state-api";
import { seconds, SnapshotTime, rejectionMessage } from "./work-session-state";
import { RouteLink } from "./navigation";
import { sameId } from "./schedule-block-api";
import { readActiveWorkSession } from "./work-session-api";
import "./work-session.scss";
type ReaderProps = { id: string; projectId: string; taskId: string };
export function WorkSessionReader(props: ReaderProps) {
  return (
    <Reader key={`${props.projectId}:${props.taskId}:${props.id}`} {...props} />
  );
}
function Reader({ id, projectId, taskId }: ReaderProps) {
  const [closure, setClosure] =
    useState<Awaited<ReturnType<typeof readWorkSessionClosure>>>();
  const [failure, setFailure] = useState<string>();
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (interacted.current && document.activeElement === document.body) {
      interacted.current = false;
      heading.current?.focus();
    }
  });
  const [snapshot, setSnapshot] = useState<WorkSessionSnapshot>();
  const [progressNote, setProgressNote] = useState("");
  const [nextStep, setNextStep] = useState("");
  const fieldsId = useId();
  const [busy, setBusy] = useState(false);
  const [checking, setChecking] = useState(false);
  const command = useRef<AbortController | undefined>(undefined);
  useEffect(() => () => command.current?.abort(), []);
  const [noteError, setNoteError] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const [mayResend, setMayResend] = useState(false);
  const [conflict, setConflict] = useState<string>();
  const [confirmedHere, setConfirmedHere] = useState(false);
  const retained = useRef<WorkSessionIntent | undefined>(undefined);
  async function close(check = false) {
    if (
      (!snapshot && !retained.current) ||
      command.current ||
      (uncertain && !check && !mayResend)
    )
      return;
    if (!validNote(progressNote) || !validNote(nextStep)) {
      setNoteError(true);
      return;
    }
    setNoteError(false);
    const controller = new AbortController();
    command.current = controller;
    setBusy(true);
    setChecking(check);
    interacted.current = document.activeElement !== document.body;
    setMayResend(false);
    retained.current ??= {
      state: snapshot!.state,
      token: snapshot!.token,
      key: crypto.randomUUID(),
      action: "CLOSE",
      progressNote,
      nextStep,
    };
    try {
      const result = await (
        check ? recoverWorkSessionChange : changeWorkSession
      )(retained.current, controller.signal);
      if (controller.signal.aborted) return;
      if (result.action === "CLOSE") {
        setClosure(result);
        setConfirmedHere(true);
        setSnapshot(undefined);
        retained.current = undefined;
        setUncertain(false);
      }
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await readWorkSessionStateError(error);
      if (controller.signal.aborted) return;
      const rejection = problem && rejectionMessage(problem.code);
      if (rejection) {
        retained.current = undefined;
        setSnapshot(undefined);
        setUncertain(false);
        setConflict(rejection);
        return;
      }
      setMayResend(
        problem?.code === "CSRF_INVALID" ||
          (check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),
      );
      setUncertain(true);
    } finally {
      if (!controller.signal.aborted) {
        command.current = undefined;
        setBusy(false);
      }
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    void readWorkSessionState(id, controller.signal)
      .then(async (snapshot) => {
        if (controller.signal.aborted) return;
        if (
          !sameId(snapshot.state.session.projectId, projectId) ||
          !sameId(snapshot.state.session.taskId, taskId)
        ) {
          setFailure("Esta sesión no está disponible en esta tarea.");
          return;
        }
        if (snapshot.state.status !== "closed") {
          setSnapshot(snapshot);
          return;
        }
        const result = await readWorkSessionClosure(id, controller.signal);
        if (controller.signal.aborted) return;
        if (
          !sameId(result.after.session.projectId, projectId) ||
          !sameId(result.after.session.taskId, taskId)
        ) {
          setFailure("Esta sesión no está disponible en esta tarea.");
          return;
        }
        setClosure(result);
      })
      .catch(() => {
        if (!controller.signal.aborted)
          setFailure("No se ha podido consultar la sesión de trabajo.");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, projectId, taskId, refresh]);
  return (
    <main
      id="proyectos"
      tabIndex={-1}
      className="reader work-session"
      onBlurCapture={() => {
        interacted.current = false;
      }}
    >
      <RouteLink href={`/proyectos/${projectId}/tareas/${taskId}`}>
        Volver a la tarea
      </RouteLink>
      <h1 ref={heading} tabIndex={-1}>
        Sesión de trabajo
      </h1>
      {loading && <p role="status">Consultando sesión de trabajo</p>}
      {busy && (
        <p role="status">
          {checking ? "Comprobando cierre" : "Cerrando sesión de trabajo"}
        </p>
      )}
      {(busy || uncertain) && (
        <p>
          Salir no revoca el cierre transmitido. Al volver puedes consultar esta
          sesión.
        </p>
      )}
      {conflict && (
        <>
          <p role="alert">{conflict}</p>
          <button
            onClick={() => {
              interacted.current = document.activeElement !== document.body;
              setLoading(true);
              setConflict(undefined);
              setRefresh((value) => value + 1);
            }}
          >
            Consultar estado actual
          </button>
        </>
      )}
      {uncertain && (
        <>
          <p role="alert">No podemos confirmar el cierre.</p>
          <button aria-disabled={busy} onClick={() => void close(true)}>
            Comprobar cierre
          </button>
          {mayResend && (
            <button aria-disabled={busy} onClick={() => void close()}>
              Reenviar el mismo cierre
            </button>
          )}
        </>
      )}
      {snapshot && !uncertain && (
        <form
          className="task-form"
          onSubmit={(event) => {
            event.preventDefault();
            void close();
          }}
        >
          <h2>Cerrar sesión de trabajo</h2>
          <p>
            Las notas quedarán guardadas y no se podrán editar después del
            cierre. La tarea seguirá en su estado actual.
          </p>
          <p>
            Inicio:{" "}
            <SnapshotTime
              instant={snapshot.state.session.startedAt}
              zone={snapshot.state.session.zoneId}
            />
          </p>
          <p>
            Fin previsto:{" "}
            <SnapshotTime
              instant={snapshot.state.session.plannedEndAt}
              zone={snapshot.state.session.zoneId}
            />
          </p>
          {noteError && (
            <p id={`${fieldsId}-error`} role="alert">
              Cada nota admite hasta 2.000 caracteres válidos.
            </p>
          )}
          <div className="field">
            <label htmlFor={`${fieldsId}-progress`}>
              Avance anotado (opcional)
            </label>
            <textarea
              id={`${fieldsId}-progress`}
              aria-invalid={noteError && !validNote(progressNote)}
              aria-describedby={noteError ? `${fieldsId}-error` : undefined}
              readOnly={busy}
              value={progressNote}
              onChange={(event) => setProgressNote(event.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor={`${fieldsId}-next`}>
              Siguiente paso (opcional)
            </label>
            <textarea
              id={`${fieldsId}-next`}
              aria-invalid={noteError && !validNote(nextStep)}
              aria-describedby={noteError ? `${fieldsId}-error` : undefined}
              readOnly={busy}
              value={nextStep}
              onChange={(event) => setNextStep(event.target.value)}
            />
          </div>
          <button type="submit" aria-disabled={busy}>
            Confirmar cierre
          </button>
        </form>
      )}
      {failure && (
        <>
          <p role="alert">{failure}</p>
          <button
            onClick={() => {
              setFailure(undefined);
              interacted.current = document.activeElement !== document.body;
              setLoading(true);
              setRefresh((value) => value + 1);
            }}
          >
            Reintentar lectura
          </button>
        </>
      )}
      {closure && (
        <section>
          <p role="status">Sesión cerrada</p>
          <p>
            Tiempo de trabajo total: {seconds(closure.after.workedMicroseconds)}{" "}
            s
          </p>
          <p>Día atribuido: {closure.closure.workDate}</p>
          <p>
            Cerrada:{" "}
            <SnapshotTime
              instant={closure.occurredAt}
              zone={closure.closure.closeZoneId}
            />
          </p>
          <h2>Avance anotado</h2>
          <p className="closure-note">
            {closure.closure.progressNote || "Sin avance anotado"}
          </p>
          <h2>Siguiente paso</h2>
          <p className="closure-note">
            {closure.closure.nextStep || "Sin siguiente paso anotado"}
          </p>
          <p>El cierre de la sesión no completa la tarea.</p>
        </section>
      )}
      {confirmedHere && <OpenSessionAfterClosure />}
    </main>
  );
}

function OpenSessionAfterClosure() {
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [active, setActive] =
    useState<Awaited<ReturnType<typeof readActiveWorkSession>>>();
  useEffect(() => {
    const controller = new AbortController();
    void readActiveWorkSession(controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setActive(value);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [refresh]);
  return (
    <section aria-label="Sesión abierta">
      {loading ? (
        <p role="status">Consultando sesión abierta</p>
      ) : failed ? (
        <p role="alert">
          No se ha podido consultar si hay otra sesión abierta.
        </p>
      ) : active === null ? (
        <p>No hay ninguna sesión abierta.</p>
      ) : null}
      {!loading && !failed && active && (
        <>
          <p>Hay otra sesión abierta.</p>
          <RouteLink
            href={`/proyectos/${active.projectId}/tareas/${active.taskId}/sesiones/${active.id}`}
          >
            Ir a la sesión abierta
          </RouteLink>
        </>
      )}
      <button
        aria-disabled={loading}
        onClick={() => {
          if (loading) return;
          setLoading(true);
          setFailed(false);
          setActive(undefined);
          setRefresh((value) => value + 1);
        }}
      >
        Consultar sesión abierta
      </button>
    </section>
  );
}
