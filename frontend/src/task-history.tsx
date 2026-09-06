import { useEffect, useState, useRef, useLayoutEffect } from "react";
import { readTaskHistory, type TaskHistoryPage } from "./task-status-api";
export function TaskHistory({
  projectId,
  id,
  onAccessFailure,
}: {
  projectId: string;
  id: string;
  onAccessFailure: (status: number) => void;
}) {
  const [page, setPage] = useState<TaskHistoryPage>();
  const [cursor, setCursor] = useState<string>();
  const [failure, setFailure] = useState(false);
  const [revision, setRevision] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (
      interacted.current &&
      (page || failure) &&
      document.activeElement === document.body
    )
      heading.current?.focus();
  }, [page, failure]);
  useEffect(() => {
    const controller = new AbortController();
    void readTaskHistory(projectId, id, cursor, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setPage(result);
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (
          error instanceof Response &&
          (error.status === 401 || error.status === 404)
        )
          onAccessFailure(error.status);
        else setFailure(true);
      });
    return () => controller.abort();
  }, [projectId, id, cursor, revision, onAccessFailure]);
  return (
    <section className="task-history" aria-label="Historial de la tarea">
      <h2 ref={heading} tabIndex={-1}>
        Historial de la tarea
      </h2>
      {failure ? (
        <div>
          <p role="alert">No se ha podido consultar el historial.</p>
          <button
            onClick={() => {
              interacted.current = true;
              setFailure(false);
              setPage(undefined);
              setRevision((value) => value + 1);
            }}
          >
            Reintentar historial
          </button>
        </div>
      ) : page ? (
        <>
          {page.items.length ? (
            <ol aria-label="Transiciones de la tarea">
              {page.items.map((entry) => (
                <li key={entry.id}>
                  <strong>
                    {entry.toStatus === "completed"
                      ? "Completada"
                      : "Reabierta"}
                  </strong>
                  <time dateTime={entry.occurredAt}>
                    {new Intl.DateTimeFormat("es", {
                      dateStyle: "medium",
                      timeStyle: "short",
                      timeZone: "UTC",
                    }).format(new Date(entry.occurredAt))}{" "}
                    UTC
                  </time>
                </li>
              ))}
            </ol>
          ) : (
            <p>Todavía no hay cambios de estado.</p>
          )}
          {page.nextCursor && (
            <button
              onClick={() => {
                interacted.current = true;
                setCursor(page.nextCursor!);
                setPage(undefined);
              }}
            >
              Más transiciones antiguas
            </button>
          )}
        </>
      ) : (
        <p role="status">Consultando historial de la tarea</p>
      )}
      {cursor && (page || failure) && (
        <button
          onClick={() => {
            interacted.current = true;
            setFailure(false);
            setCursor(undefined);
            setPage(undefined);
          }}
        >
          Volver al historial reciente
        </button>
      )}
    </section>
  );
}
