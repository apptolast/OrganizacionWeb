import { useEffect, useState, useLayoutEffect, useRef } from "react";
import { RouteLink } from "./navigation";
import { readTaskParent, type Task } from "./tasks-api";

export function TaskParent({
  projectId,
  id,
}: {
  projectId: string;
  id: string;
}) {
  const [failure, setFailure] = useState(false);
  const [revision, setRevision] = useState(0);
  const [relation, setRelation] = useState<{ parent: Task | null }>();
  const region = useRef<HTMLElement>(null);
  const retried = useRef(false);
  useLayoutEffect(() => {
    if (
      retried.current &&
      (relation || failure) &&
      document.activeElement === document.body
    )
      region.current?.focus();
  }, [relation, failure]);
  useEffect(() => {
    const controller = new AbortController();
    void readTaskParent(projectId, id, controller.signal)
      .then((data) => {
        if (!controller.signal.aborted) setRelation(data);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailure(true);
      });
    return () => controller.abort();
  }, [projectId, id, revision]);
  return (
    <section
      className="task-parent"
      aria-label="Padre directo"
      tabIndex={-1}
      ref={region}
    >
      {failure ? (
        <div>
          <p role="alert">No se ha podido consultar la relación.</p>
          <button
            onClick={() => {
              retried.current = true;
              setFailure(false);
              setRevision((value) => value + 1);
            }}
          >
            Reintentar relación
          </button>
        </div>
      ) : relation ? (
        relation.parent ? (
          <p role="status">
            Padre directo:{" "}
            <RouteLink
              href={`/proyectos/${projectId}/tareas/${relation.parent.id}`}
            >
              {relation.parent.title}
            </RouteLink>
          </p>
        ) : (
          <p role="status">Tarea principal confirmada</p>
        )
      ) : (
        <p role="status">Consultando relación</p>
      )}
    </section>
  );
}
