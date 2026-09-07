import {
  useEffect,
  useLayoutEffect,
  useState,
  useId,
  useRef,
  useCallback,
} from "react";
import { microseconds, type SessionStart } from "./work-session-api";
import {
  changeWorkSession,
  recoverWorkSessionChange,
  readWorkSessionStateError,
  type WorkSessionChange,
  type WorkSessionIntent,
} from "./work-session-state-api";
import {
  readWorkSessionEnd,
  type WorkSessionEnd,
} from "./work-session-end-api";
import { SnapshotTime, rejectionMessage } from "./work-session-state";
import { RouteLink } from "./navigation";
import { integer } from "./schedule-block-api";
import {
  useWorkSessionDecision,
  type SessionDecision,
} from "./use-work-session-decision";

type Props = {
  session: SessionStart;
  onAccessFailure: (status: number) => void;
  decision?: SessionDecision;
};
export function WorkSessionEndPanel(props: Props) {
  return (
    <EndPanel
      key={`${props.session.projectId}:${props.session.taskId}:${props.session.id}`}
      {...props}
    />
  );
}
function EndPanel({ session, onAccessFailure, decision: shared }: Props) {
  const local = useWorkSessionDecision();
  const decision = shared ?? local;
  const heading = useRef<HTMLHeadingElement>(null);
  const restoreFocus = useRef(false);
  useLayoutEffect(() => {
    if (restoreFocus.current && document.activeElement === document.body)
      heading.current?.focus();
    restoreFocus.current = false;
  });
  const { track, generation } = decision;
  const [snapshotGeneration, setSnapshotGeneration] = useState(generation);
  const awaitingSnapshot = snapshotGeneration !== generation;
  const blocked = Boolean(decision.owner && decision.owner !== "EXTEND");
  const [snapshot, setSnapshot] = useState<WorkSessionEnd>();
  const receivedAt = useRef(0);
  const [notifiedEnd, setNotifiedEnd] = useState<string>();
  const [editing, setEditing] = useState(false);
  const [minutes, setMinutes] = useState("");
  const inputId = useId();
  const [confirmed, setConfirmed] = useState<WorkSessionChange>();
  const [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true);
  const [lookupFailed, setLookupFailed] = useState(false);
  const lookupBusy = useRef(true);
  const refreshEnd = useCallback(() => {
    if (lookupBusy.current) return;
    lookupBusy.current = true;
    setLoading(true);
    setRefresh((value) => value + 1);
  }, []);
  const [invalid, setInvalid] = useState(false);
  const command = useRef<AbortController | undefined>(undefined);
  useEffect(() => () => command.current?.abort(), []);
  const [busy, setBusy] = useState(false);
  const [checking, setChecking] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const [mayResend, setMayResend] = useState(false);
  const [conflict, setConflict] = useState<string>();
  const retained = useRef<WorkSessionIntent | undefined>(undefined);
  const withdraw = useCallback(
    (status: number) => {
      setSnapshot(undefined);
      setConfirmed(undefined);
      setMinutes("");
      setEditing(false);
      retained.current = undefined;
      setUncertain(false);
      onAccessFailure(status);
    },
    [onAccessFailure],
  );
  async function send(check = false) {
    if (blocked || (!retained.current && awaitingSnapshot)) return;
    if (command.current || conflict || (uncertain && !check && !mayResend))
      return;
    if (!integer(Number(minutes), 1, 1440)) {
      setInvalid(true);
      return;
    }
    setInvalid(false);
    if (!decision.acquire("EXTEND")) return;
    const initiator = document.activeElement;
    const controller = new AbortController();
    command.current = controller;
    setBusy(true);
    setChecking(check);
    setMayResend(false);
    retained.current ??= {
      state: snapshot!.state,
      token: snapshot!.token,
      key: crypto.randomUUID(),
      action: "EXTEND",
      additionalMinutes: Number(minutes),
    };
    try {
      const result = await (
        check ? recoverWorkSessionChange : changeWorkSession
      )(retained.current, controller.signal);
      if (controller.signal.aborted) return;
      setConfirmed(result);
      setNotifiedEnd(undefined);
      decision.settle();
      retained.current = undefined;
      setUncertain(false);
      setEditing(false);
      setRefresh((value) => value + 1);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await readWorkSessionStateError(error);
      if (controller.signal.aborted) return;
      if (problem?.code === "WORK_SESSION_NOT_FOUND") {
        withdraw(404);
        return;
      }
      const rejection = problem && rejectionMessage(problem.code);
      if (rejection) {
        retained.current = undefined;
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
        restoreFocus.current =
          initiator !== document.body && initiator === document.activeElement;
        command.current = undefined;
        setBusy(false);
      }
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    const untrack = track(controller);
    lookupBusy.current = true;
    const aborted = () => {
      lookupBusy.current = false;
      setLoading(false);
    };
    controller.signal.addEventListener("abort", aborted, { once: true });
    void readWorkSessionEnd(session.id, controller.signal)
      .then((value) => {
        if (controller.signal.aborted) return;
        receivedAt.current = performance.now();
        setSnapshotGeneration(generation);
        if (
          microseconds(value.serverNow)! >= microseconds(value.effectiveEndAt)!
        )
          setNotifiedEnd(value.effectiveEndAt);
        setSnapshot(value);
        setLookupFailed(false);
        setConflict(undefined);
      })
      .catch(async (error) => {
        if (controller.signal.aborted) return;
        const problem = await readWorkSessionStateError(error);
        if (controller.signal.aborted) return;
        const unauthorized = error instanceof Response && error.status === 401;
        if (unauthorized || problem?.code === "WORK_SESSION_NOT_FOUND") {
          withdraw(unauthorized ? 401 : 404);
          return;
        }
        setLookupFailed(true);
      })
      .finally(() => {
        controller.signal.removeEventListener("abort", aborted);
        untrack();
        if (!controller.signal.aborted) {
          lookupBusy.current = false;
          setLoading(false);
        }
      });
    return () => {
      controller.signal.removeEventListener("abort", aborted);
      untrack();
      controller.abort();
    };
  }, [session.id, refresh, onAccessFailure, generation, track, withdraw]);
  useEffect(() => {
    const visible = () => {
      if (document.visibilityState === "visible") refreshEnd();
    };
    document.addEventListener("visibilitychange", visible);
    return () => document.removeEventListener("visibilitychange", visible);
  }, [refreshEnd]);
  useEffect(() => {
    if (!snapshot || snapshot.state.status === "closed") return;
    const duration =
      microseconds(snapshot.effectiveEndAt)! -
      microseconds(snapshot.serverNow)!;
    if (duration <= 0n) return;
    let timer: ReturnType<typeof setTimeout>;
    function arm() {
      const remaining =
        duration -
        BigInt(Math.floor((performance.now() - receivedAt.current) * 1000));
      if (remaining <= 0n) {
        refreshEnd();
        return;
      }
      const millis = (remaining + 999n) / 1000n;
      timer = setTimeout(
        arm,
        Number(millis > 2147483647n ? 2147483647n : millis),
      );
    }
    arm();
    return () => clearTimeout(timer);
  }, [snapshot, refreshEnd]);
  return (
    <section>
      <h3 ref={heading} tabIndex={-1}>
        Fin de la sesión
      </h3>
      {(loading || (awaitingSnapshot && !lookupFailed)) && (
        <p role="status">Comprobando el fin actual</p>
      )}
      {lookupFailed && !loading && (
        <p role="alert">No se ha podido comprobar el fin actual.</p>
      )}
      {conflict && (
        <>
          <p role="alert">{conflict}</p>
          <button type="button" aria-disabled={loading} onClick={refreshEnd}>
            Consultar fin actual
          </button>
        </>
      )}
      <button type="button" aria-disabled={loading} onClick={refreshEnd}>
        {lookupFailed
          ? "Reintentar consulta del fin"
          : "Actualizar fin acordado"}
      </button>
      {busy && (
        <p role="status">
          {checking ? "Comprobando ampliación" : "Ampliando tiempo"}
        </p>
      )}
      {(busy || uncertain) && (
        <p>
          Salir no revoca la ampliación transmitida. Al volver puedes consultar
          esta sesión.
        </p>
      )}
      {uncertain && (
        <>
          <p role="alert">No podemos confirmar la ampliación.</p>
          <button
            type="button"
            aria-disabled={busy || blocked}
            onClick={() => void send(true)}
          >
            Comprobar ampliación
          </button>
          {mayResend && (
            <button
              type="button"
              aria-disabled={busy}
              onClick={() => void send()}
            >
              Reenviar ampliación
            </button>
          )}
        </>
      )}
      {confirmed?.action === "EXTEND" && (
        <>
          <p role="status">Ampliación confirmada</p>
          <article aria-label="Ampliación guardada">
            <p>{confirmed.extension.additionalMinutes} minutos adicionales</p>
            <p>
              Fin anterior:{" "}
              <SnapshotTime
                instant={confirmed.extension.previousEndAt}
                zone={session.zoneId}
              />
            </p>
            <p>
              Fin guardado en esta ampliación:{" "}
              <SnapshotTime
                instant={confirmed.extension.effectiveEndAt}
                zone={session.zoneId}
              />
            </p>
          </article>
        </>
      )}
      {snapshot && (
        <>
          <p>
            Fin previsto original:{" "}
            <SnapshotTime
              instant={session.plannedEndAt}
              zone={session.zoneId}
            />
          </p>
          {snapshot.effectiveEndAt !== session.plannedEndAt && (
            <p>
              Fin acordado actual:{" "}
              <SnapshotTime
                instant={snapshot.effectiveEndAt}
                zone={session.zoneId}
              />
            </p>
          )}
          {snapshot.state.status !== "closed" && (
            <>
              {notifiedEnd === snapshot.effectiveEndAt && (
                <p role="status">Ha llegado el fin acordado</p>
              )}
              <RouteLink
                href={`/proyectos/${session.projectId}/tareas/${session.taskId}/sesiones/${session.id}`}
              >
                Cerrar sesión de trabajo
              </RouteLink>
              <button type="button" onClick={() => setEditing(true)}>
                Ampliar tiempo
              </button>
              {editing && (
                <form
                  className="task-form"
                  onSubmit={(event) => {
                    event.preventDefault();
                    void send();
                  }}
                >
                  <div className="field">
                    <label htmlFor={inputId}>Minutos adicionales</label>
                    <input
                      id={inputId}
                      type="number"
                      value={minutes}
                      readOnly={busy || uncertain}
                      aria-invalid={invalid}
                      aria-describedby={
                        invalid ? `${inputId}-error` : undefined
                      }
                      onChange={(event) => setMinutes(event.target.value)}
                    />
                  </div>
                  {invalid && (
                    <p id={`${inputId}-error`} role="alert">
                      Introduce un número entero de minutos entre 1 y 1.440.
                    </p>
                  )}
                  {!uncertain && !conflict && (
                    <button
                      type="submit"
                      aria-disabled={busy || blocked || awaitingSnapshot}
                    >
                      Confirmar ampliación
                    </button>
                  )}
                </form>
              )}
            </>
          )}
        </>
      )}
    </section>
  );
}
