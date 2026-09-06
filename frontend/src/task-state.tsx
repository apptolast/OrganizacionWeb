import { useEffect, useState, useRef, useLayoutEffect } from "react";
import {
  readTaskStatus,
  changeTaskStatus,
  type TaskStatusSnapshot,
} from "./task-status-api";
import { TaskHistory } from "./task-history";

export function TaskState({
  projectId,
  id,
  onAccessFailure,
}: {
  projectId: string;
  id: string;
  onAccessFailure: (status: number) => void;
}) {
  const [snapshot, setSnapshot] = useState<TaskStatusSnapshot>();
  const [saving, setSaving] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [failure, setFailure] = useState<"read" | "write">();
  const [revision, setRevision] = useState(0);
  const [historyRevision, setHistoryRevision] = useState(0);
  const write = useRef<AbortController | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (
      interacted.current &&
      !saving &&
      (snapshot || failure) &&
      document.activeElement === document.body
    )
      heading.current?.focus();
  }, [snapshot, saving, failure]);
  useEffect(() => () => write.current?.abort(), []);
  async function change() {
    if (!snapshot || saving) return;
    interacted.current = true;
    setSaving(true);
    setConfirmed(false);
    const controller = new AbortController();
    write.current = controller;
    try {
      const result = await changeTaskStatus(
        projectId,
        id,
        snapshot.status === "pending" ? "completed" : "pending",
        snapshot.etag,
        controller.signal,
      );
      if (controller.signal.aborted) return;
      setSnapshot(result);
      setConfirmed(true);
      setHistoryRevision((value) => value + 1);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (
        error instanceof Response &&
        (error.status === 401 || error.status === 404)
      )
        onAccessFailure(error.status);
      else setFailure("write");
    } finally {
      if (!controller.signal.aborted) setSaving(false);
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    void readTaskStatus(projectId, id, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setSnapshot(result);
        if (revision > 0) setHistoryRevision((value) => value + 1);
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (
          error instanceof Response &&
          (error.status === 401 || error.status === 404)
        )
          onAccessFailure(error.status);
        else setFailure("read");
      });
    return () => controller.abort();
  }, [projectId, id, revision, onAccessFailure]);
  return (
    <>
      <section aria-label="Estado de la tarea" className="task-state">
        <h2 ref={heading} tabIndex={-1}>
          Estado de la tarea
        </h2>
        {failure ? (
          <div>
            <p role="alert">
              No podemos confirmar el estado actual. Consúltalo antes de volver
              a cambiarlo.
            </p>
            <button
              onClick={() => {
                interacted.current = true;
                setFailure(undefined);
                setSnapshot(undefined);
                setRevision((value) => value + 1);
              }}
            >
              {failure === "read"
                ? "Reintentar estado"
                : "Consultar estado vigente"}
            </button>
          </div>
        ) : snapshot ? (
          <>
            <span className="idea-badge">
              {snapshot.status === "pending" ? "Pendiente" : "Completada"}
            </span>
            {snapshot.completedAt && (
              <p>
                Finalizada el{" "}
                <time dateTime={snapshot.completedAt}>
                  {new Intl.DateTimeFormat("es", {
                    dateStyle: "medium",
                    timeStyle: "short",
                    timeZone: "UTC",
                  }).format(new Date(snapshot.completedAt))}{" "}
                  UTC
                </time>
              </p>
            )}
            <button disabled={saving} onClick={() => void change()}>
              {snapshot.status === "pending"
                ? "Completar tarea"
                : "Reabrir tarea"}
            </button>
            {saving && <p role="status">Cambiando estado de la tarea</p>}
            {confirmed && <p role="status">Estado de tarea actualizado</p>}
          </>
        ) : (
          <p role="status">Consultando estado de la tarea</p>
        )}
      </section>
      <TaskHistory
        key={historyRevision}
        projectId={projectId}
        id={id}
        onAccessFailure={onAccessFailure}
      />
    </>
  );
}
