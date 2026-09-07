import { useEffect, useState, type FormEvent } from "react";
import {
  readHistory,
  type HistoryPage,
  type HistoryEntry,
} from "./history-api";
import { RouteLink } from "./navigation";
import "./history.scss";
import type { Block } from "./schedule-block-api";
import { SnapshotTime, seconds } from "./work-session-state";

export function History({ route }: { route: string }) {
  const [result, setResult] = useState<{
    route: string;
    refresh: number;
    page: HistoryPage | null;
    failure: number | null;
  }>();

  const [refresh, setRefresh] = useState(0);
  const current = result?.route === route && result.refresh === refresh;
  const page = current ? result.page : null;
  const failure = current ? result.failure : null;
  useEffect(() => {
    const controller = new AbortController();
    void readHistory(
      new URLSearchParams(route.split("?")[1]),
      controller.signal,
    )
      .then((result) => {
        if (!controller.signal.aborted)
          setResult({ route, refresh, page: result, failure: null });
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted)
          setResult({
            route,
            refresh,
            page: null,
            failure: error instanceof Response ? error.status : 0,
          });
      });
    return () => controller.abort();
  }, [route, refresh]);
  const filters = new URLSearchParams(route.split("?")[1]);
  const filtered = ["category", "projectId", "taskId", "from", "to"].some(
    (field) => filters.has(field),
  );
  const contextFree = new URLSearchParams(filters);
  for (const field of ["projectId", "taskId", "cursor"])
    contextFree.delete(field);
  const contextFreeUrl = `/historial${contextFree.size ? `?${contextFree}` : ""}`;
  function pageUrl(cursor: string | null) {
    const query = new URLSearchParams(filters);
    if (cursor) query.set("cursor", cursor);
    else query.delete("cursor");
    const search = query.toString();
    return `/historial${search ? `?${search}` : ""}`;
  }
  function apply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const query = new URLSearchParams(filters);
    query.delete("cursor");
    for (const field of ["category", "from", "to"]) {
      const value = String(data.get(field) ?? "");
      if (value) query.set(field, value);
      else query.delete(field);
    }
    const search = query.toString();
    window.history.pushState(
      null,
      "",
      `/historial${search ? `?${search}` : ""}`,
    );
    window.dispatchEvent(new PopStateEvent("popstate"));
  }
  return (
    <main id="proyectos" tabIndex={-1} className="reader history">
      <h1>Historial</h1>
      {failure !== 401 && (
        <form key={route} onSubmit={apply} aria-label="Filtros del historial">
          <label htmlFor="history-category">Categoría</label>
          <select
            id="history-category"
            name="category"
            defaultValue={filters.get("category") ?? ""}
          >
            <option value="">Todos los hechos</option>
            <option value="sessions">Sesiones</option>
            <option value="task-status">Estado de tareas</option>
            <option value="planning">Planificación</option>
          </select>
          <fieldset>
            <legend>Fecha del hecho (UTC)</legend>
            <label htmlFor="history-from">Desde (UTC)</label>
            <input
              id="history-from"
              name="from"
              type="date"
              min="0001-01-01"
              max="9999-12-31"
              defaultValue={filters.get("from") ?? ""}
            />
            <label htmlFor="history-to">Hasta (UTC)</label>
            <input
              id="history-to"
              name="to"
              type="date"
              min="0001-01-01"
              max="9999-12-31"
              defaultValue={filters.get("to") ?? ""}
            />
          </fieldset>
          <button type="submit">Aplicar filtros</button>
          <RouteLink href="/historial">Limpiar filtros</RouteLink>
        </form>
      )}
      {!page && failure === null && <p role="status">Consultando historial…</p>}
      {failure !== null && (
        <p role="alert">
          {failure === 401
            ? "Autenticación requerida. Vuelve a autenticarte para consultar tu historial."
            : failure === 404
              ? "Este proyecto o tarea no está disponible para tu cuenta."
              : "No hemos podido consultar el historial."}
        </p>
      )}
      {failure === 404 && (
        <RouteLink href={contextFreeUrl}>Quitar filtro de contexto</RouteLink>
      )}
      {failure !== null && failure !== 401 && failure !== 404 && (
        <button
          onClick={() => {
            setRefresh((value) => value + 1);
          }}
        >
          Reintentar consulta
        </button>
      )}
      {page && page.items.length > 0 && (
        <ol aria-label="Hechos del historial">
          {page.items.map((item) => (
            <li key={`${item.type}:${item.id}`}>
              <h2>{actionLabel(item)}</h2>
              <SnapshotTime instant={item.occurredAt} zone="UTC" />
              <p>
                Proyecto actual:{" "}
                <RouteLink href={`/proyectos/${item.projectId}`}>
                  {item.projectName}
                </RouteLink>
              </p>
              <p>
                Tarea actual:{" "}
                <RouteLink
                  href={`/proyectos/${item.projectId}/tareas/${item.taskId}`}
                >
                  {item.taskTitle}
                </RouteLink>
              </p>
              {(item.type === "SESSION_STARTED" ||
                item.type === "SESSION_CHANGED") && (
                <RouteLink
                  href={`/proyectos/${item.projectId}/tareas/${item.taskId}/sesiones/${item.type === "SESSION_STARTED" ? item.id : item.details.sessionId}`}
                >
                  Ver sesión de trabajo
                </RouteLink>
              )}
              <details>
                <summary>Ver detalles del hecho</summary>
                {item.type === "TASK_STATUS_CHANGED" && (
                  <>
                    <p>
                      Estado anterior:{" "}
                      {item.details.fromStatus === "completed"
                        ? "Completada"
                        : "Pendiente"}
                    </p>
                    <p>
                      Estado registrado:{" "}
                      {item.details.toStatus === "completed"
                        ? "Completada"
                        : "Pendiente"}
                    </p>
                  </>
                )}
                {item.type === "BLOCK_CHANGED" && (
                  <>
                    <p>
                      Revisión de la reserva:{" "}
                      {item.details.revision.match(/:(\d+)"$/)![1]}
                    </p>
                    <ReservationDetails
                      block={item.details.before}
                      heading="Reserva anterior"
                    />
                    {item.details.after && (
                      <ReservationDetails
                        block={item.details.after}
                        heading="Reserva resultante"
                      />
                    )}
                  </>
                )}
                {item.type === "BLOCK_PLANNED" && (
                  <>
                    <p>{item.details.objective}</p>
                    <p>
                      Inicio reservado:{" "}
                      <SnapshotTime
                        instant={item.details.startAt}
                        zone={item.details.zoneId}
                      />
                    </p>
                    <p>
                      Fin reservado:{" "}
                      <SnapshotTime
                        instant={item.details.endAt}
                        zone={item.details.zoneId}
                      />
                    </p>
                    <p>
                      Tiempo reservado: {item.details.durationMinutes} minutos
                    </p>
                    <p>Una reserva no acredita tiempo trabajado.</p>
                  </>
                )}
                {item.type === "SESSION_STARTED" && (
                  <>
                    <p>
                      Tiempo previsto al iniciar: {item.details.plannedMinutes}{" "}
                      minutos
                    </p>
                    <p>
                      Fin previsto al iniciar:{" "}
                      <SnapshotTime
                        instant={item.details.plannedEndAt}
                        zone={item.details.zoneId}
                      />
                    </p>
                  </>
                )}
                {item.type === "SESSION_CHANGED" && (
                  <p>Revisión de la sesión: {item.details.after.revision}</p>
                )}
                {item.type === "SESSION_CHANGED" &&
                  (item.details.action === "PAUSE" ||
                    item.details.action === "RESUME") && (
                    <p>
                      Trabajo acumulado en este hecho:{" "}
                      {seconds(item.details.after.workedMicroseconds)} s
                    </p>
                  )}
                {item.type === "SESSION_CHANGED" &&
                  item.details.action === "EXTEND" && (
                    <>
                      <p>
                        Ampliación: {item.details.extension.additionalMinutes}{" "}
                        minutos
                      </p>
                      <p>
                        Fin anterior:{" "}
                        <SnapshotTime
                          instant={item.details.extension.previousEndAt}
                          zone={item.details.after.session.zoneId}
                        />
                      </p>
                      <p>
                        Fin ampliado:{" "}
                        <SnapshotTime
                          instant={item.details.extension.effectiveEndAt}
                          zone={item.details.after.session.zoneId}
                        />
                      </p>
                      <p>La ampliación no añade tiempo trabajado.</p>
                    </>
                  )}
                {item.type === "SESSION_CHANGED" &&
                  item.details.action === "CLOSE" && (
                    <>
                      <p>
                        Tiempo trabajado:{" "}
                        {seconds(item.details.after.workedMicroseconds)} s
                      </p>
                      <p>Día atribuido: {item.details.closure.workDate}</p>
                      <p>
                        Cerrada:{" "}
                        <SnapshotTime
                          instant={item.occurredAt}
                          zone={item.details.closure.closeZoneId}
                        />
                      </p>
                      <h3>Avance anotado</h3>
                      <p className="history-note">
                        {item.details.closure.progressNote ||
                          "Sin avance anotado"}
                      </p>
                      <h3>Siguiente paso</h3>
                      <p className="history-note">
                        {item.details.closure.nextStep ||
                          "Sin siguiente paso anotado"}
                      </p>
                      <p>El cierre de la sesión no completa la tarea.</p>
                    </>
                  )}
              </details>
            </li>
          ))}
        </ol>
      )}
      {failure !== 401 && (
        <nav aria-label="Paginación del historial">
          {page?.nextCursor && (
            <RouteLink href={pageUrl(page.nextCursor)}>Más antiguos</RouteLink>
          )}
          {filters.has("cursor") && (
            <RouteLink href={pageUrl(null)}>Volver a recientes</RouteLink>
          )}
        </nav>
      )}
      {page?.items.length === 0 && (
        <p>
          {filtered
            ? "No hay hechos con estos filtros."
            : filters.has("cursor")
              ? "No hay hechos en esta página."
              : "Todavía no hay hechos en tu historial."}
        </p>
      )}
    </main>
  );
}

function actionLabel(item: HistoryEntry) {
  switch (item.type) {
    case "BLOCK_PLANNED":
      return "Tiempo reservado";
    case "BLOCK_CHANGED":
      return item.details.kind === "CANCELLED"
        ? "Reserva cancelada"
        : "Reserva replanificada";
    case "TASK_STATUS_CHANGED":
      return item.details.toStatus === "completed"
        ? "Tarea completada"
        : "Tarea reabierta";
    case "SESSION_STARTED":
      return "Sesión iniciada";
    case "SESSION_CHANGED":
      return {
        PAUSE: "Sesión pausada",
        RESUME: "Sesión reanudada",
        CLOSE: "Sesión cerrada",
        EXTEND: "Tiempo ampliado",
      }[item.details.action];
  }
}
function ReservationDetails({
  block,
  heading,
}: {
  block: Block;
  heading: string;
}) {
  return (
    <section>
      <h3>{heading}</h3>
      <p>{block.objective}</p>
      <p>
        Inicio reservado:{" "}
        <SnapshotTime instant={block.startAt} zone={block.zoneId} />
      </p>
      <p>
        Fin reservado:{" "}
        <SnapshotTime instant={block.endAt} zone={block.zoneId} />
      </p>
      <p>Tiempo reservado: {block.durationMinutes} minutos</p>
      <p>Una reserva no acredita tiempo trabajado.</p>
    </section>
  );
}
