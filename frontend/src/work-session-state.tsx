import { useEffect, useLayoutEffect, useRef, useState } from "react";
import type { SessionStart } from "./work-session-api";
import { RouteLink } from "./navigation";
import {
  useWorkSessionDecision,
  type SessionDecision,
} from "./use-work-session-decision";
import {
  readWorkSessionStateError,
  recoverWorkSessionChange,
  type WorkSessionIntent,
  changeWorkSession,
  type WorkSessionChange,
  readWorkSessionState,
  type WorkSessionSnapshot,
} from "./work-session-state-api";

type StatePanelProps = {
  session: SessionStart;
  onAccessFailure: (status: number) => void;
  decision?: SessionDecision;
};
export function WorkSessionStatePanel(props: StatePanelProps) {
  return <StatePanel key={props.session.id} {...props} />;
}
function StatePanel({
  session,
  onAccessFailure,
  decision: shared,
}: StatePanelProps) {
  const local = useWorkSessionDecision();
  const decision = shared ?? local;
  const { track, generation } = decision;
  const [snapshotGeneration, setSnapshotGeneration] = useState(generation);
  const awaitingSnapshot = snapshotGeneration !== generation;
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (interacted.current && document.activeElement === document.body)
      heading.current?.focus();
  });
  const [snapshot, setSnapshot] = useState<WorkSessionSnapshot>();
  const [confirmed, setConfirmed] = useState<WorkSessionChange>();
  const [loading, setLoading] = useState(true);
  const [lookupFailed, setLookupFailed] = useState(false);
  const [refresh, setRefresh] = useState(0);
  const [busy, setBusy] = useState(false);
  const lookup = useRef<AbortController>(undefined);
  const command = useRef<AbortController>(undefined);
  useEffect(() => () => command.current?.abort(), []);
  const retained = useRef<WorkSessionIntent>(undefined);
  const [uncertain, setUncertain] = useState(false);
  const [conflict, setConflict] = useState<string>();
  const [mayResend, setMayResend] = useState(false);
  async function send(check = false) {
    if (!retained.current && awaitingSnapshot) return;
    if (busy || (!snapshot && !retained.current)) return;
    if (
      !decision.acquire(
        retained.current?.action ??
          (snapshot!.state.status === "running" ? "PAUSE" : "RESUME"),
      )
    )
      return;
    retained.current ??= {
      state: snapshot!.state,
      token: snapshot!.token,
      key: crypto.randomUUID(),
      action: snapshot!.state.status === "running" ? "PAUSE" : "RESUME",
    };
    const controller = new AbortController();
    command.current = controller;
    lookup.current?.abort();
    setLoading(false);
    interacted.current = true;
    setBusy(true);
    setMayResend(false);
    try {
      const result = await (check
        ? recoverWorkSessionChange(retained.current, controller.signal)
        : changeWorkSession(retained.current, controller.signal));
      if (controller.signal.aborted) return;
      setConfirmed(result);
      decision.settle();
      setUncertain(false);
      retained.current = undefined;
      setSnapshot(undefined);
      setLookupFailed(false);
      setLoading(true);
      setRefresh((value) => value + 1);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof Response && error.status === 401) {
        onAccessFailure(401);
        return;
      }
      const problem = await readWorkSessionStateError(error);
      if (controller.signal.aborted) return;
      const rejection = problem && rejectionMessage(problem.code);
      if (rejection) {
        decision.release();
        retained.current = undefined;
        setConflict(rejection);
        setUncertain(false);
        setSnapshot(undefined);
        return;
      }
      setMayResend(
        problem?.code === "CSRF_INVALID" ||
          (check && problem?.code === "WORK_SESSION_CHANGE_NOT_FOUND"),
      );
      setUncertain(true);
    } finally {
      if (!controller.signal.aborted) setBusy(false);
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    const untrack = track(controller);
    lookup.current = controller;
    void readWorkSessionState(session.id, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setSnapshotGeneration(generation);
          setSnapshot(result);
          setLookupFailed(false);
        }
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        if (error instanceof Response && error.status === 401) {
          onAccessFailure(401);
          return;
        }
        setLookupFailed(true);
      })
      .finally(() => {
        untrack();
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => {
      untrack();
      controller.abort();
    };
  }, [session.id, refresh, onAccessFailure, generation, track]);
  return (
    <div>
      <h3 ref={heading} tabIndex={-1}>
        Estado de la sesión
      </h3>
      {(loading || (awaitingSnapshot && !lookupFailed)) && snapshot && (
        <p role="status">Consultando estado de la sesión</p>
      )}
      <button
        type="button"
        aria-disabled={loading || busy}
        onClick={() => {
          if (loading || busy) return;
          setLoading(true);
          setLookupFailed(false);
          setRefresh((value) => value + 1);
        }}
      >
        Actualizar estado de la sesión
      </button>
      {busy && <p role="status">Procesando cambio de sesión</p>}
      {(busy || uncertain) && (
        <p>
          Salir no revoca el cambio transmitido. Al volver puedes consultar el
          estado de la sesión.
        </p>
      )}
      {confirmed && (
        <p role="status">
          {confirmed.action === "PAUSE"
            ? "Pausa confirmada"
            : "Reanudación confirmada"}
        </p>
      )}
      {conflict ? (
        <>
          <p role="alert">{conflict}</p>
          <button
            type="button"
            onClick={() => {
              setConflict(undefined);
              setRefresh((value) => value + 1);
            }}
          >
            Consultar estado actual
          </button>
        </>
      ) : uncertain ? (
        <>
          <p role="alert">No podemos confirmar el cambio</p>
          <button
            type="button"
            aria-disabled={busy}
            onClick={() => void send(true)}
          >
            Comprobar cambio
          </button>
          {mayResend && (
            <button
              type="button"
              aria-disabled={busy}
              onClick={() => void send()}
            >
              Reenviar cambio
            </button>
          )}
        </>
      ) : lookupFailed ? (
        <>
          <p role="alert">No podemos consultar el estado de la sesión</p>
          <button
            type="button"
            onClick={() => {
              interacted.current = true;
              setLoading(true);
              setLookupFailed(false);
              setRefresh((value) => value + 1);
            }}
          >
            Reintentar consulta
          </button>
        </>
      ) : !snapshot ? (
        <p role="status">Consultando estado de la sesión</p>
      ) : (
        <>
          <p>
            {snapshot.state.status === "closed"
              ? "Sesión cerrada"
              : snapshot.state.status === "running"
                ? "En curso"
                : "En pausa"}
          </p>
          <p>
            Tiempo de trabajo hasta la actualización:{" "}
            {seconds(snapshot.netMicroseconds)} s
          </p>
          <p>
            Actualizado:{" "}
            <SnapshotTime
              instant={snapshot.serverNow}
              zone={snapshot.state.session.zoneId}
            />
          </p>
          {snapshot.state.status !== "closed" && (
            <button
              type="button"
              aria-disabled={busy || awaitingSnapshot}
              onClick={() => void send()}
            >
              {snapshot.state.status === "running" ? "Pausar" : "Reanudar"}
            </button>
          )}
          {!busy && (
            <RouteLink
              href={`/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`}
            >
              {snapshot.state.status === "closed"
                ? "Ver cierre de la sesión"
                : "Cerrar sesión de trabajo"}
            </RouteLink>
          )}
        </>
      )}
    </div>
  );
}

export function seconds(value: string) {
  const micros = BigInt(value);
  const fraction = String(micros % 1000000n)
    .padStart(6, "0")
    .replace(/0+$/, "");
  return String(micros / 1000000n) + (fraction ? "," + fraction : "");
}

export function SnapshotTime({
  instant,
  zone,
}: {
  instant: string;
  zone: string;
}) {
  let formatter: Intl.DateTimeFormat;
  let label = zone;
  try {
    formatter = new Intl.DateTimeFormat("es-ES", {
      timeZone: zone,
      dateStyle: "long",
      timeStyle: "long",
    });
  } catch {
    formatter = new Intl.DateTimeFormat("es-ES", {
      timeZone: "UTC",
      dateStyle: "long",
      timeStyle: "long",
    });
    label = "UTC (zona histórica no disponible: " + zone + ")";
  }
  return (
    <>
      <time dateTime={instant}>{formatter.format(new Date(instant))}</time>{" "}
      <span>{label}</span>
    </>
  );
}

export function rejectionMessage(code: string) {
  switch (code) {
    case "WORK_SESSION_NOT_FOUND":
      return "No se ha encontrado la sesión de trabajo.";
    case "PRECONDITION_FAILED":
      return "La sesión ha cambiado. Consulta su estado antes de decidir.";
    case "WORK_SESSION_STATE_CONFLICT":
      return "El estado de la sesión no permite esta acción.";
    case "WORK_SESSION_REVISION_EXHAUSTED":
      return "No se pueden registrar más cambios en esta sesión.";
    case "WORK_SESSION_TIME_OUT_OF_RANGE":
      return "No se puede registrar el cambio en este instante.";
  }
}
