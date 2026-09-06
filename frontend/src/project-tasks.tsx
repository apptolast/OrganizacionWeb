import { useProjectTasks } from "./use-project-tasks";
import { taskMessages } from "./task-validation";
import type { ProjectSnapshot } from "./read-projects-api";
import { useLayoutEffect, useRef } from "react";
import { RouteLink } from "./navigation";
export function ProjectTasks({
  projectId,
  projectStatus,
  onProjectConfirmed,
  parentTaskId,
}: {
  projectId: string;
  projectStatus: string;
  onProjectConfirmed: (snapshot: ProjectSnapshot) => void;
  parentTaskId?: string;
}) {
  const {
    title,
    setTitle,
    criterion,
    setCriterion,
    estimate,
    setEstimate,
    errors,
    saving,
    saved,
    saveFailure,
    page,
    failure,
    cursor,
    completedConflict,
    reviewing,
    submit,
    reviewProject,
    reload,
    changePage,
  } = useProjectTasks(projectId, onProjectConfirmed, parentTaskId);
  const heading = useRef<HTMLHeadingElement>(null);
  const returnFocus = useRef<HTMLElement | null>(null);
  useLayoutEffect(() => {
    if (
      !saving &&
      !reviewing &&
      (page || failure) &&
      document.activeElement === document.body &&
      returnFocus.current
    ) {
      (returnFocus.current.isConnected
        ? returnFocus.current
        : heading.current
      )?.focus();
    }
  }, [saving, reviewing, page, failure]);
  return (
    <section
      className="project-tasks"
      aria-labelledby="tasks-heading"
      onClickCapture={(event) => {
        if (event.target instanceof HTMLButtonElement)
          returnFocus.current = event.target;
      }}
    >
      <h2 id="tasks-heading" tabIndex={-1} ref={heading}>
        {parentTaskId ? "Subtareas" : "Tareas"}
      </h2>
      <p className="tasks-intro">Pasos pequeños, con un resultado claro.</p>
      {parentTaskId && (
        <p>
          Cada estimación es independiente; las subtareas no se suman
          automáticamente a la tarea principal.
        </p>
      )}
      {failure ? (
        <div>
          <p role="alert">No hemos podido cargar las tareas.</p>
          <button onClick={reload}>
            {parentTaskId ? "Reintentar subtareas" : "Reintentar tareas"}
          </button>
        </div>
      ) : !page ? (
        <p role="status">
          {parentTaskId ? "Cargando subtareas" : "Cargando tareas"}
        </p>
      ) : page.items.length === 0 ? (
        <p>
          {parentTaskId
            ? "Esta tarea todavía no tiene subtareas."
            : "Todavía no hay tareas en este proyecto."}
        </p>
      ) : (
        <ul
          className="task-list"
          aria-label={parentTaskId ? "Subtareas guardadas" : "Tareas guardadas"}
        >
          {page.items.map((task) => (
            <li key={task.id}>
              <h3>
                <RouteLink href={`/proyectos/${projectId}/tareas/${task.id}`}>
                  {task.title}
                </RouteLink>
              </h3>
              <p>{task.completionCriterion}</p>
              <span className="idea-badge">Pendiente</span>
              <p>
                {task.estimatedMinutes === null
                  ? "Sin estimación"
                  : `Estimación: ${task.estimatedMinutes} min`}
              </p>
            </li>
          ))}
        </ul>
      )}
      {page?.nextCursor && (
        <button onClick={() => changePage(page.nextCursor!)}>
          {parentTaskId ? "Más subtareas antiguas" : "Más tareas antiguas"}
        </button>
      )}
      {cursor && (
        <button onClick={() => changePage()}>
          {parentTaskId
            ? "Volver a subtareas recientes"
            : "Volver a tareas recientes"}
        </button>
      )}
      <p>Terminar el proyecto no completa sus tareas pendientes.</p>
      {projectStatus === "completed" ? (
        <p>Reabre el proyecto en pausa para añadir tareas.</p>
      ) : (
        <form className="task-form" noValidate onSubmit={submit}>
          <div className="field">
            <label htmlFor="task-title">Título de la tarea</label>
            <input
              id="task-title"
              name="title"
              aria-invalid={errors.includes("title") || undefined}
              aria-describedby={
                errors.includes("title") ? "task-title-error" : undefined
              }
              value={title}
              onChange={(event) => setTitle(event.target.value)}
            />
            {errors.includes("title") && (
              <p id="task-title-error" role="alert">
                {taskMessages.title}
              </p>
            )}
          </div>
          <div className="field">
            <label htmlFor="task-criterion">Criterio de finalización</label>
            <textarea
              id="task-criterion"
              name="completionCriterion"
              aria-invalid={errors.includes("completionCriterion") || undefined}
              aria-describedby={
                errors.includes("completionCriterion")
                  ? "task-criterion-error"
                  : undefined
              }
              value={criterion}
              onChange={(event) => setCriterion(event.target.value)}
            />
            {errors.includes("completionCriterion") && (
              <p id="task-criterion-error" role="alert">
                {taskMessages.completionCriterion}
              </p>
            )}
          </div>
          <div className="field">
            <label htmlFor="task-estimate">Estimación en minutos</label>
            <input
              id="task-estimate"
              name="estimatedMinutes"
              min="1"
              max="1440"
              step="1"
              aria-invalid={errors.includes("estimatedMinutes") || undefined}
              aria-describedby={
                errors.includes("estimatedMinutes")
                  ? "task-estimate-error"
                  : undefined
              }
              type="number"
              value={estimate}
              onChange={(event) => setEstimate(event.target.value)}
            />
            {errors.includes("estimatedMinutes") && (
              <p id="task-estimate-error" role="alert">
                {taskMessages.estimatedMinutes}
              </p>
            )}
          </div>
          <p>La estimación no es tiempo trabajado.</p>
          <button disabled={saving || completedConflict || reviewing}>
            {parentTaskId ? "Crear subtarea" : "Crear tarea"}
          </button>
          {saving && (
            <p role="status">
              {parentTaskId ? "Guardando subtarea" : "Guardando tarea"}
            </p>
          )}
        </form>
      )}
      {saveFailure && <p role="alert">{saveFailure}</p>}
      {completedConflict && (
        <div>
          <p>El proyecto se ha terminado. Tu borrador se conserva.</p>
          <button disabled={reviewing} onClick={reviewProject}>
            Revisar estado del proyecto
          </button>
          {reviewing && <p role="status">Revisando estado del proyecto</p>}
        </div>
      )}
      {saved && (
        <p role="status">
          {parentTaskId ? "Subtarea guardada" : "Tarea guardada"}
        </p>
      )}
      {saved && !page?.items.some((item) => item.id === saved.id) && (
        <article aria-label="Última tarea guardada">
          <h3>
            <RouteLink href={`/proyectos/${projectId}/tareas/${saved.id}`}>
              {saved.title}
            </RouteLink>
          </h3>
          <p>{saved.completionCriterion}</p>
          <span>Pendiente</span>
          <p>
            {saved.estimatedMinutes === null
              ? "Sin estimación"
              : `Estimación: ${saved.estimatedMinutes} min`}
          </p>
        </article>
      )}
    </section>
  );
}
