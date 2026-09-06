import "./today.scss";
import { useEffect, useState, useRef, useCallback } from "react";
import { readToday, type TodaySnapshot } from "./today-api";
import { RouteLink } from "./navigation";
function at(instant: string, zoneId: string) {
  try {
    return (
      new Intl.DateTimeFormat("es", {
        timeZone: zoneId,
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(instant)) +
      " · " +
      zoneId
    );
  } catch {
    return `${instant} · UTC (zona ${zoneId} no disponible)`;
  }
}
export function Today() {
  const [snapshot, setSnapshot] = useState<TodaySnapshot | null>(null);
  const pending = useRef(true);
  const awaitingVisibleSnapshot = useRef(false);
  const active = useRef<AbortController | null>(null);
  const receivedAt = useRef(0);
  const dayDeadline = useRef(Infinity);
  const [revision, setRevision] = useState(0);
  const [failure, setFailure] = useState(false);
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    const controller = new AbortController();
    active.current = controller;
    void readToday(controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) {
          awaitingVisibleSnapshot.current = false;
          receivedAt.current = performance.now();
          dayDeadline.current =
            receivedAt.current +
            Date.parse(value.dayEndAt) -
            Date.parse(value.serverNow);
          setSnapshot(value);
          setFailure(false);
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailure(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          pending.current = false;
          setLoading(false);
        }
      });
    return () => controller.abort();
  }, [revision]);
  const refresh = useCallback((force = false, rollover = false) => {
    rollover ||= performance.now() >= dayDeadline.current;
    force ||= rollover;
    if (pending.current && !force) return;
    if (force) active.current?.abort();
    if (rollover) {
      dayDeadline.current = Infinity;
      setSnapshot(null);
    }
    pending.current = true;
    setLoading(true);
    setFailure(false);
    setRevision((value) => value + 1);
  }, []);
  useEffect(() => {
    const recover = () => {
      if (document.visibilityState !== "hidden") {
        awaitingVisibleSnapshot.current = true;
        refresh();
      }
    };
    document.addEventListener("visibilitychange", recover);
    window.addEventListener("focus", recover);
    return () => {
      document.removeEventListener("visibilitychange", recover);
      window.removeEventListener("focus", recover);
    };
  }, [refresh]);
  useEffect(() => {
    if (!snapshot || document.visibilityState === "hidden") return;
    const now = Date.parse(snapshot.serverNow);
    const elapsed = performance.now() - receivedAt.current;
    const next =
      failure || awaitingVisibleSnapshot.current
        ? Date.parse(snapshot.dayEndAt)
        : Math.min(
            ...[
              snapshot.dayEndAt,
              ...snapshot.items.flatMap(({ block }) => [
                block.startAt,
                block.endAt,
              ]),
            ]
              .map(Date.parse)
              .filter((time) => time > now + elapsed),
          );
    let timer = window.setTimeout(
      () => refresh(true, next === Date.parse(snapshot.dayEndAt)),
      Math.max(0, next - now - elapsed),
    );
    const visibility = () => {
      window.clearTimeout(timer);
      if (document.visibilityState !== "hidden")
        timer = window.setTimeout(
          () => refresh(true, true),
          Math.max(0, dayDeadline.current - performance.now()),
        );
    };
    document.addEventListener("visibilitychange", visibility);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener("visibilitychange", visibility);
    };
  }, [snapshot, refresh, failure, revision]);
  return (
    <main id="proyectos" tabIndex={-1} className="today">
      <h1>Hoy</h1>
      <button type="button" aria-disabled={loading} onClick={() => refresh()}>
        Actualizar
      </button>
      <p role="status">
        {loading
          ? snapshot
            ? "Actualizando Hoy…"
            : "Cargando Hoy…"
          : failure
            ? ""
            : "Agenda actualizada."}
      </p>
      {failure && (
        <p role="alert">
          {snapshot && "Sin actualizar. "}No se pudo actualizar Hoy.{" "}
          <button type="button" onClick={() => refresh()}>
            Reintentar
          </button>
        </p>
      )}
      {snapshot && (
        <>
          {snapshot.zoneSource !== "AVAILABILITY" && (
            <p className="today-notice">
              {snapshot.zoneSource === "UNCONFIGURED"
                ? "Disponibilidad no configurada. Mostramos UTC; capacidad desconocida."
                : `Zona guardada no disponible (${snapshot.availabilityZoneId}). Mostramos UTC; capacidad desconocida.`}{" "}
              <RouteLink href="/disponibilidad">
                Configurar disponibilidad
              </RouteLink>
            </p>
          )}
          <p>
            {snapshot.date} · {snapshot.zoneId}
          </p>
          <p>
            Según actualización de {at(snapshot.serverNow, snapshot.zoneId)}
          </p>
          <dl className="today-summary">
            <dt>Tiempo planificado</dt>
            <dd>{snapshot.plannedSeconds / 60} min</dd>
            <dt>Presupuesto del día</dt>
            <dd>
              {snapshot.budgetMinutes === null
                ? "Desconocido"
                : `${snapshot.budgetMinutes} min`}
            </dd>
            <dt>Presupuesto sin reservar</dt>
            <dd>
              {snapshot.remainingSeconds === null
                ? "Desconocido"
                : `${snapshot.remainingSeconds / 60} min`}
            </dd>
            <dt>Exceso planificado</dt>
            <dd>
              {snapshot.excessSeconds === null
                ? "Desconocido"
                : `${snapshot.excessSeconds / 60} min`}
            </dd>
            <dt>Cierre previsto</dt>
            <dd>
              {snapshot.closingAt
                ? at(snapshot.closingAt, snapshot.zoneId)
                : "Sin bloques"}
            </dd>
          </dl>
          {snapshot.items.length === 0 ? (
            <p>No hay bloques planificados</p>
          ) : (
            <ol className="today-agenda">
              {snapshot.items.map(({ block, projectName, taskTitle }) => (
                <li key={block.id}>
                  <p>{projectName}</p>
                  <h2>
                    <RouteLink
                      href={`/proyectos/${block.projectId}/tareas/${block.taskId}`}
                    >
                      {taskTitle}
                    </RouteLink>
                  </h2>
                  {block.id === snapshot.currentBlockId && (
                    <p>En horario planificado</p>
                  )}
                  {block.id === snapshot.nextBlockId && (
                    <p>Próximo inicio planificado</p>
                  )}
                  <p>{block.objective}</p>
                  <p>
                    {at(block.startAt, snapshot.zoneId)} —{" "}
                    {at(block.endAt, snapshot.zoneId)}
                  </p>
                  {block.zoneId !== snapshot.zoneId && (
                    <p>
                      Zona original: {at(block.startAt, block.zoneId)} —{" "}
                      {at(block.endAt, block.zoneId)}
                    </p>
                  )}
                </li>
              ))}
            </ol>
          )}
          <RouteLink href="/proyectos">Ver proyectos</RouteLink>
        </>
      )}
    </main>
  );
}
