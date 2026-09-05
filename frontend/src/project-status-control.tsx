import { useRef, useLayoutEffect } from "react";
import { RouteLink } from "./navigation";
import { useProjectStatus } from "./use-project-status";
import type { ProjectSnapshot } from "./read-projects-api";
import { statusActions } from "./project-status";
import { isStrongEtag } from "./edit-project-api";
export function ProjectStatusControl({
  snapshot,
  onConfirmed,
  onAccessFailure,
  onReload,
}: {
  snapshot: ProjectSnapshot;
  onConfirmed: (snapshot: ProjectSnapshot) => void;
  onAccessFailure: (status: number) => void;
  onReload: () => void;
}) {
  const { success, saving, change, failure, capacity } = useProjectStatus(
    snapshot,
    onConfirmed,
    onAccessFailure,
  );
  const heading = useRef<HTMLHeadingElement>(null);
  const returnFocus = useRef<HTMLButtonElement | null>(null);
  useLayoutEffect(() => {
    if (
      !saving &&
      document.activeElement === document.body &&
      returnFocus.current
    ) {
      (returnFocus.current.isConnected
        ? returnFocus.current
        : heading.current
      )?.focus();
    }
  }, [saving]);
  return (
    <section
      className="project-status-control"
      aria-labelledby="status-heading"
    >
      <h2 id="status-heading" ref={heading} tabIndex={-1}>
        Estado del proyecto
      </h2>
      <p className="status-help">
        Decide qué sigue para este proyecto, a tu ritmo.
      </p>
      <div className="status-actions" aria-busy={saving}>
        {statusActions[snapshot.project.status].map((action) => (
          <button
            key={action.status}
            className={
              action.status === "completed" ? "secondary-link" : undefined
            }
            type="button"
            disabled={saving || !isStrongEtag(snapshot.etag)}
            onClick={(event) => {
              returnFocus.current = event.currentTarget;
              void change(action.status);
            }}
          >
            {action.label}
          </button>
        ))}
      </div>
      {failure !== null && failure !== 412 && !capacity && (
        <p className="status-notice" role="alert">
          {failure === 0
            ? "No podemos confirmar si el estado se guardó. Comprueba la conexión antes de decidir si reintentas."
            : "El servicio no ha confirmado el cambio de estado. Puedes volver a intentarlo cuando esté disponible."}
        </p>
      )}
      {capacity && (
        <>
          <p className="status-notice" role="alert">
            Tienes {capacity.activeCount} proyectos activos y un límite de{" "}
            {capacity.limit}. Elige qué proyecto pausar antes de activar otro.
          </p>
          <RouteLink className="secondary-link" href="/proyectos">
            Elegir qué pausar
          </RouteLink>
        </>
      )}
      {failure === 412 && (
        <>
          <p className="status-notice" role="alert">
            Existe una versión más reciente. Recarga los datos guardados antes
            de decidir el cambio.
          </p>
          <button type="button" onClick={onReload}>
            Recargar versión guardada
          </button>
        </>
      )}
      {!isStrongEtag(snapshot.etag) && (
        <>
          <p className="status-notice" role="alert">
            No podemos cambiar el estado con esta versión. Recarga los datos
            guardados.
          </p>
          <button type="button" onClick={onReload}>
            Recargar versión guardada
          </button>
        </>
      )}
      {(success || saving) && (
        <p className="edit-status" role="status">
          {saving ? "Cambiando estado" : "Estado actualizado"}
        </p>
      )}
    </section>
  );
}
