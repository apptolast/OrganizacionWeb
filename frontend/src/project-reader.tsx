import { useRef, useLayoutEffect } from "react";
import { RouteLink } from "./navigation";
import { useReadProjects } from "./use-read-projects";
import { statusLabels } from "./project-status";
import { ProjectStatusControl } from "./project-status-control";

export function ProjectReader({ route }: { route: string }) {
  const {
    page,
    project,
    snapshot,
    confirmProject,
    revokeProject,
    failure,
    retry,
    isDetail,
    isContinuation,
  } = useReadProjects(route);
  const heading = useRef<HTMLHeadingElement>(null);
  const hasFocused = useRef(false);
  useLayoutEffect(() => {
    if (
      (page || project) &&
      (!hasFocused.current || document.activeElement === document.body)
    ) {
      heading.current?.focus();
      hasFocused.current = true;
    }
  }, [page, project]);
  const title =
    failure === 401
      ? "Autenticación requerida"
      : failure === 404 && isDetail
        ? "Proyecto no encontrado"
        : (project?.name ?? (isDetail ? "Detalle del proyecto" : "Proyectos"));
  return (
    <main id="proyectos" tabIndex={-1} className="reader">
      {isDetail && (
        <RouteLink className="back-link" href="/proyectos">
          <span aria-hidden="true">←</span> Volver a proyectos
        </RouteLink>
      )}
      <div className="reader-heading">
        <div className="page-intro">
          <p className="eyebrow">
            {isDetail
              ? "UN ESPACIO PARA TU IDEA"
              : "TUS IDEAS, CON PERSPECTIVA."}
          </p>
          <h1 ref={heading} tabIndex={-1}>
            {title}
          </h1>
          {!isDetail && failure !== 401 && (
            <p>
              Vuelve a lo que quieres construir. Un proyecto, un pequeño paso.
            </p>
          )}
        </div>
        {page && page.items.length > 0 && (
          <RouteLink className="primary-link" href="/">
            <span aria-hidden="true">＋</span> Crear proyecto
          </RouteLink>
        )}
        {project && (
          <RouteLink
            className="primary-link"
            href={`/proyectos/${project.id}/editar`}
          >
            Editar proyecto
          </RouteLink>
        )}
      </div>
      {failure === 401 ? (
        <section className="read-notice">
          <p role="alert">
            Autenticación requerida. Vuelve a autenticarte y recarga la página
            para consultar tus proyectos.
          </p>
          {!isDetail && (
            <RouteLink className="secondary-link" href="/proyectos">
              Volver a proyectos
            </RouteLink>
          )}
        </section>
      ) : failure === 404 && isDetail ? (
        <section className="read-notice">
          <p role="alert">Este proyecto no está disponible para tu cuenta.</p>
        </section>
      ) : failure !== null ? (
        <section className="read-notice read-error">
          <p role="alert">
            {isDetail
              ? "No hemos podido cargar el proyecto."
              : "No hemos podido cargar los proyectos."}
          </p>
          <p>Prueba de nuevo cuando la conexión esté disponible.</p>
          <button onClick={retry}>Reintentar</button>
        </section>
      ) : project ? (
        <article className="detail-card">
          <span className="idea-badge" data-status={project.status}>
            {statusLabels[project.status]}
          </span>
          <ProjectStatusControl
            snapshot={snapshot!}
            onConfirmed={confirmProject}
            onAccessFailure={revokeProject}
            onReload={retry}
          />
          <h2>La idea, en tus palabras</h2>
          <p className="project-description">
            {project.description || "Sin descripción."}
          </p>
          <dl className="project-meta">
            <div>
              <dt>Creado</dt>
              <dd>
                <ProjectTime value={project.createdAt} />
              </dd>
            </div>
            <div>
              <dt>Actualizado</dt>
              <dd>
                <ProjectTime value={project.updatedAt} />
              </dd>
            </div>
          </dl>
        </article>
      ) : !page ? (
        <section className="read-notice loading-notice">
          <span className="loading-seed" aria-hidden="true">
            ◌
          </span>
          <p role="status">
            {isDetail ? "Cargando proyecto" : "Cargando proyectos"}
          </p>
        </section>
      ) : (
        <>
          {page.items.length === 0 ? (
            <section className="collection-empty">
              <div className="empty-symbol" aria-hidden="true">
                ✧
              </div>
              <h2>
                {isContinuation
                  ? "No hay más proyectos en esta página"
                  : "Todavía no tienes proyectos"}
              </h2>
              <p>
                {isContinuation
                  ? "Vuelve al inicio para ver tus proyectos más recientes."
                  : "Dale un nombre a esa idea que quieres guardar. Podrás volver a ella cuando quieras."}
              </p>
              {!isContinuation && (
                <RouteLink className="primary-link" href="/">
                  Crear proyecto <span aria-hidden="true">↗</span>
                </RouteLink>
              )}
            </section>
          ) : (
            <>
              <p className="collection-order">
                Más recientes primero · Fechas en UTC
              </p>
              <ul className="project-list" aria-label="Proyectos guardados">
                {page.items.map((item) => (
                  <li key={item.id} className="project-row">
                    <h2>
                      <RouteLink href={`/proyectos/${item.id}`}>
                        {item.name}
                        <span aria-hidden="true">↗</span>
                      </RouteLink>
                    </h2>
                    <div className="project-row-meta">
                      <span className="idea-badge" data-status={item.status}>
                        {statusLabels[item.status]}
                      </span>
                      <span>
                        Creado <ProjectTime value={item.createdAt} />
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
          {(page.nextCursor || isContinuation) && (
            <nav className="page-navigation" aria-label="Páginas de proyectos">
              {isContinuation && (
                <RouteLink className="secondary-link" href="/proyectos">
                  Volver al inicio
                </RouteLink>
              )}
              {page.nextCursor && (
                <RouteLink
                  className="secondary-link"
                  href={`/proyectos?cursor=${encodeURIComponent(page.nextCursor)}`}
                >
                  Más antiguos <span aria-hidden="true">→</span>
                </RouteLink>
              )}
            </nav>
          )}
        </>
      )}
      <footer className="page-footer">
        <span>Un lugar para lo que quieres construir.</span>
        <span>Con calma. Con intención.</span>
      </footer>
    </main>
  );
}
function ProjectTime({ value }: { value: string }) {
  return (
    <time dateTime={value}>
      {new Intl.DateTimeFormat("es", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "UTC",
      }).format(new Date(value))}{" "}
      UTC
    </time>
  );
}
