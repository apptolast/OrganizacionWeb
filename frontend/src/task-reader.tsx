import { useEffect, useState, useRef, useLayoutEffect } from "react";
import { readTask, type Task } from "./tasks-api";
import { RouteLink } from "./navigation";
import { readProjects, type ProjectSnapshot } from "./read-projects-api";
import { ProjectTasks } from "./project-tasks";
import { TaskParent } from "./task-parent";
import { ProjectStatusControl } from "./project-status-control";
export function TaskReader({
  projectId,
  id,
}: {
  projectId: string;
  id: string;
}) {
  const [snapshot, setSnapshot] = useState<ProjectSnapshot>();
  const [task, setTask] = useState<Task>();
  const [failure, setFailure] = useState<number | null>(null);
  const [revision, setRevision] = useState(0);
  const [projectRevision, setProjectRevision] = useState(0);
  const [projectLoading, setProjectLoading] = useState(true);
  const [projectFailure, setProjectFailure] = useState(false);
  const heading = useRef<HTMLHeadingElement>(null);
  useLayoutEffect(() => {
    if (document.activeElement === document.body) heading.current?.focus();
  }, [task]);
  useLayoutEffect(() => {
    if (!projectLoading && document.activeElement === document.body)
      heading.current?.focus();
  }, [projectLoading]);
  function reloadProject() {
    setProjectLoading(true);
    setProjectFailure(false);
    setProjectRevision((value) => value + 1);
  }
  useEffect(() => {
    const controller = new AbortController();
    void readTask(projectId, id, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;
        setTask(data);
      })
      .catch((error) => {
        if (!controller.signal.aborted)
          setFailure(error instanceof Response ? error.status : 0);
      });
    return () => controller.abort();
  }, [projectId, id, revision]);
  useEffect(() => {
    if (!task) return;
    const controller = new AbortController();
    void readProjects(`/proyectos/${task.projectId}`, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted && "project" in result) {
          setSnapshot(result);
          setProjectFailure(false);
        }
      })
      .catch((error) => {
        if (controller.signal.aborted) return;
        if (
          error instanceof Response &&
          (error.status === 401 || error.status === 404)
        )
          setFailure(error.status);
        else setProjectFailure(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setProjectLoading(false);
      });
    return () => controller.abort();
  }, [task, projectRevision]);
  return (
    <main id="proyectos" tabIndex={-1} className="reader task-reader">
      <RouteLink
        href={`/proyectos/${task?.projectId ?? projectId}`}
        className="back-link"
      >
        Volver al proyecto
      </RouteLink>
      {failure !== null ? (
        <section>
          <p role="alert">
            {failure === 404
              ? "Esta tarea no está disponible para tu cuenta."
              : "No se ha podido cargar la tarea."}
          </p>
          <button
            onClick={() => {
              setTask(undefined);
              setSnapshot(undefined);
              setProjectLoading(true);
              setProjectFailure(false);
              setFailure(null);
              setRevision((value) => value + 1);
            }}
          >
            Reintentar tarea
          </button>
        </section>
      ) : task ? (
        <article className="project-detail">
          <h1 ref={heading} tabIndex={-1}>
            {task.title}
          </h1>
          <p>{task.completionCriterion}</p>
          <span className="idea-badge">Pendiente</span>
          <p>
            {task.estimatedMinutes === null
              ? "Sin estimación"
              : `Estimación: ${task.estimatedMinutes} min`}
          </p>
          <TaskParent projectId={task.projectId} id={task.id} />
          {projectLoading && <p role="status">Consultando proyecto</p>}
          {projectFailure && (
            <div>
              <p role="alert">No se ha podido consultar el proyecto.</p>
              <button onClick={reloadProject}>Reintentar proyecto</button>
            </div>
          )}
          {snapshot && !projectLoading && !projectFailure && (
            <ProjectStatusControl
              snapshot={snapshot}
              onConfirmed={setSnapshot}
              onAccessFailure={setFailure}
              onReload={reloadProject}
            />
          )}
          {snapshot && (
            <ProjectTasks
              projectId={task.projectId}
              projectStatus={snapshot.project.status}
              parentTaskId={task.id}
              onProjectConfirmed={setSnapshot}
            />
          )}
        </article>
      ) : (
        <p role="status">Cargando tarea</p>
      )}
    </main>
  );
}
