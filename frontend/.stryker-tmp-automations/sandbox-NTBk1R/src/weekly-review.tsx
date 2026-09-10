// @ts-nocheck
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import {
  readWeeklyReview,
  type WeeklyReview as Snapshot,
} from "./weekly-review-api";
import { readAvailabilityZones } from "./availability-api";
import { RouteLink } from "./navigation";
import { seconds, SnapshotTime } from "./work-session-state";

export function WeeklyReview({ route }: { route: string }) {
  const filters = new URLSearchParams(route.split("?")[1]);
  const [result, setResult] = useState<{ route: string; snapshot: Snapshot }>();
  const snapshot = result?.route === route ? result.snapshot : undefined;
  const [failed, setFailed] = useState<{
    route: string;
    refresh: number;
    status: number;
    fields: string[];
    temporal: boolean;
  }>();
  const [settled, setSettled] = useState<{ route: string; refresh: number }>();
  const [refresh, setRefresh] = useState(0);
  const [selection, setSelection] = useState(route);
  if (selection !== route) {
    setSelection(route);
    setResult(undefined);
    setSettled(undefined);
    setFailed(undefined);
  }
  const failure =
    failed?.route === route && failed.refresh === refresh ? failed : undefined;
  const loading = settled?.route !== route || settled.refresh !== refresh;
  const [zones, setZones] = useState<string[]>();
  const [zoneLoading, setZoneLoading] = useState(false);
  const [zoneFailure, setZoneFailure] = useState(false);
  const zoneLookup = useRef<AbortController | null>(null);
  useEffect(() => () => zoneLookup.current?.abort(), []);
  const initiator = useRef<HTMLElement | null>(null);
  useEffect(() => {
    const moved = (event: FocusEvent) => {
      if (event.target !== initiator.current) initiator.current = null;
    };
    document.addEventListener("focusin", moved);
    return () => document.removeEventListener("focusin", moved);
  }, []);
  useLayoutEffect(() => {
    if (loading) return;
    const control = initiator.current;
    initiator.current = null;
    if (
      control &&
      !control.isConnected &&
      document.activeElement === document.body
    )
      heading.current?.focus();
  }, [loading]);
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus();
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    void readWeeklyReview(
      new URLSearchParams(route.split("?")[1]),
      controller.signal,
    )
      .then((snapshot) => {
        if (!controller.signal.aborted) setResult({ route, snapshot });
      })
      .catch(async (error: unknown) => {
        if (controller.signal.aborted) return;
        const status = error instanceof Response ? error.status : 0;
        const body: unknown =
          (status === 400 || status === 409) && error instanceof Response
            ? await error.json().catch(() => null)
            : null;
        if (controller.signal.aborted) return;
        const fields =
          body &&
          typeof body === "object" &&
          "code" in body &&
          body.code === "VALIDATION_ERROR" &&
          "errors" in body &&
          Array.isArray(body.errors)
            ? body.errors.flatMap((entry) =>
                entry &&
                typeof entry === "object" &&
                entry.code === "INVALID_VALUE" &&
                (entry.field === "date" || entry.field === "zoneId")
                  ? [entry.field as string]
                  : [],
              )
            : [];
        setFailed({
          route,
          refresh,
          status,
          fields,
          temporal: Boolean(
            status === 409 &&
            body &&
            typeof body === "object" &&
            "code" in body &&
            body.code === "WEEKLY_REVIEW_TIME_OUT_OF_RANGE",
          ),
        });
      })
      .finally(() => {
        if (!controller.signal.aborted) setSettled({ route, refresh });
      });
    return () => controller.abort();
  }, [route, refresh]);
  function loadZones() {
    if (zones || zoneLookup.current) return;
    const controller = new AbortController();
    zoneLookup.current = controller;
    setZoneFailure(false);
    setZoneLoading(true);
    void readAvailabilityZones(controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setZones(value);
      })
      .catch(() => {
        if (!controller.signal.aborted) setZoneFailure(true);
      })
      .finally(() => {
        if (zoneLookup.current === controller) zoneLookup.current = null;
        if (!controller.signal.aborted) setZoneLoading(false);
      });
  }
  function weekUrl(date: string | null) {
    const query = new URLSearchParams(filters);
    if (date) query.set("date", date);
    else query.delete("date");
    return `/revision-semanal${query.size ? `?${query}` : ""}`;
  }
  if (failure?.status === 401)
    return (
      <main id="proyectos" className="reader weekly-review" tabIndex={-1}>
        <h1 ref={heading} tabIndex={-1}>
          Revisión semanal
        </h1>
        <p role="alert">
          Tu sesión ya no está disponible. Recupera el acceso para consultar tus
          datos.
        </p>
      </main>
    );
  return (
    <main
      id="proyectos"
      className="reader weekly-review"
      tabIndex={-1}
      onClickCapture={(event) => {
        if (
          event.button !== 0 ||
          event.ctrlKey ||
          event.metaKey ||
          event.shiftKey ||
          event.altKey
        )
          return;
        const link =
          event.target instanceof Element ? event.target.closest("a") : null;
        const href = link?.getAttribute("href");
        if (
          link?.contains(document.activeElement) &&
          (href === "/revision-semanal" ||
            href?.startsWith("/revision-semanal?"))
        )
          initiator.current = link;
      }}
    >
      <h1 ref={heading} tabIndex={-1}>
        Revisión semanal
      </h1>
      <form
        key={route}
        className="task-form"
        onSubmit={(event) => {
          event.preventDefault();
          if (
            document.activeElement instanceof HTMLElement &&
            event.currentTarget.contains(document.activeElement)
          )
            initiator.current = document.activeElement;
          const data = new FormData(event.currentTarget);
          const query = new URLSearchParams();
          const date = String(data.get("date") ?? "");
          if (date) query.set("date", date);
          const zone = String(data.get("zoneId") ?? "");
          if (zone) query.set("zoneId", zone);
          window.history.pushState(
            null,
            "",
            `/revision-semanal${query.size ? `?${query}` : ""}`,
          );
          window.dispatchEvent(new PopStateEvent("popstate"));
        }}
      >
        <div className="field">
          <label htmlFor="weekly-date">Fecha de la semana</label>
          <input
            aria-invalid={failure?.fields.includes("date") ?? false}
            aria-describedby={
              failure?.fields.includes("date") ? "weekly-date-error" : undefined
            }
            id="weekly-date"
            name="date"
            type="date"
            min="0001-01-01"
            max="9999-12-31"
            defaultValue={
              new URLSearchParams(route.split("?")[1]).get("date") ?? ""
            }
          />
          {failure?.fields.includes("date") && (
            <p id="weekly-date-error" role="alert">
              Elige una fecha válida para consultar una semana completa.
            </p>
          )}
        </div>
        <div className="field">
          <label htmlFor="weekly-zone">Zona horaria</label>
          <select
            aria-invalid={failure?.fields.includes("zoneId") ?? false}
            aria-describedby={
              failure?.fields.includes("zoneId")
                ? "weekly-zone-error"
                : undefined
            }
            id="weekly-zone"
            name="zoneId"
            defaultValue={
              new URLSearchParams(route.split("?")[1]).get("zoneId") ?? ""
            }
            onFocus={loadZones}
          >
            <option value="">Zona de disponibilidad</option>
            {filters.get("zoneId") &&
              !zones?.includes(filters.get("zoneId")!) && (
                <option value={filters.get("zoneId")!}>
                  {filters.get("zoneId")}
                </option>
              )}
            {zones?.map((zone) => (
              <option key={zone} value={zone}>
                {zone}
              </option>
            ))}
          </select>
          {zoneLoading && <p role="status">Consultando zonas…</p>}
          {zoneFailure && (
            <p role="alert">
              No se pudo consultar el catálogo de zonas.{" "}
              <button type="button" onClick={loadZones}>
                Reintentar zonas
              </button>
            </p>
          )}
          {failure?.fields.includes("zoneId") && (
            <p id="weekly-zone-error" role="alert">
              Elige una zona del catálogo o usa la zona de disponibilidad.
            </p>
          )}
        </div>
        <button>Mostrar semana</button>
      </form>
      {failure && failure.fields.length === 0 && (
        <p role="alert">
          {failure.status === 400
            ? "Revisa la fecha y la zona y pulsa Mostrar semana."
            : failure.temporal
              ? "No se puede calcular esta semana con la fecha o la hora actual. Elige otra fecha o reintenta la consulta."
              : "No se pudo consultar la revisión semanal."}{" "}
          {failure.status !== 400 && (
            <button
              type="button"
              onClick={(event) => {
                if (!loading) {
                  if (event.currentTarget.contains(document.activeElement))
                    initiator.current = event.currentTarget;
                  setRefresh((value) => value + 1);
                }
              }}
            >
              Reintentar
            </button>
          )}
        </p>
      )}
      <p role="status">
        {loading
          ? snapshot
            ? "Actualizando revisión semanal…"
            : "Consultando revisión semanal…"
          : ""}
      </p>
      {snapshot && (
        <>
          <button
            type="button"
            aria-disabled={loading}
            onClick={() => {
              if (!loading) setRefresh((value) => value + 1);
            }}
          >
            Actualizar
          </button>
          <p>
            {(loading || failure) && "Datos anteriores. "}Datos consultados a{" "}
            <SnapshotTime instant={snapshot.serverNow} zone={snapshot.zoneId} />
          </p>
          <nav aria-label="Semanas">
            {shiftWeek(snapshot.weekStart, -7) ? (
              <RouteLink href={weekUrl(shiftWeek(snapshot.weekStart, -7))}>
                Semana anterior
              </RouteLink>
            ) : (
              <button type="button" disabled>
                Semana anterior
              </button>
            )}
            <RouteLink href={weekUrl(null)}>Esta semana</RouteLink>
            {shiftWeek(snapshot.weekStart, 7) ? (
              <RouteLink href={weekUrl(shiftWeek(snapshot.weekStart, 7))}>
                Semana siguiente
              </RouteLink>
            ) : (
              <button type="button" disabled>
                Semana siguiente
              </button>
            )}
          </nav>
          <p>
            {snapshot.weekStart} — {snapshot.weekEnd} · {snapshot.zoneId}
          </p>
          <p>
            El plan vigente muestra las reservas actuales; moverlas o
            cancelarlas puede cambiar semanas pasadas.
          </p>
          <p>
            El trabajo registrado suma intervalos efectivos hasta la consulta;
            no indica tareas terminadas.
          </p>
          <p>
            El presupuesto es el configurado actualmente. Un día sin presupuesto
            contempla descanso planificado, no acredita descanso real.
          </p>
          {snapshot.unquantifiedSessionCount !== "0" && (
            <p>
              Hay sesiones antiguas iniciadas esta semana sin duración
              registrada ({snapshot.unquantifiedSessionCount}). El trabajo
              mostrado es sólo el cuantificable; el contador no descarta
              sesiones antiguas iniciadas fuera de esta semana.
            </p>
          )}
          {snapshot.zoneSource === "UNCONFIGURED" && (
            <p>
              Disponibilidad no configurada. Mostramos UTC; presupuesto
              desconocido.{" "}
              <RouteLink href="/disponibilidad">
                Configurar disponibilidad
              </RouteLink>
            </p>
          )}
          {snapshot.zoneSource === "UNAVAILABLE" && (
            <p>
              La zona guardada {snapshot.availabilityZoneId} no está disponible.
              Mostramos UTC; presupuesto desconocido.{" "}
              <RouteLink href="/disponibilidad">
                Configurar disponibilidad
              </RouteLink>
            </p>
          )}
          <dl>
            <dt>Plan vigente</dt>
            <dd>{duration(snapshot.totals.plannedMicroseconds)}</dd>
            <dt>Trabajo registrado</dt>
            <dd>{duration(snapshot.totals.workedMicroseconds)}</dd>
            <dt>Presupuesto actual</dt>
            <dd>
              {snapshot.totals.capacityMicroseconds === null
                ? "Desconocido"
                : duration(snapshot.totals.capacityMicroseconds)}
            </dd>
          </dl>
          <ol aria-label="Días de la semana">
            {snapshot.days.map((day) => (
              <li key={day.date}>
                <h2>{day.date}</h2>
                <p>Plan vigente: {duration(day.plannedMicroseconds)}</p>
                <p>Trabajo registrado: {duration(day.workedMicroseconds)}</p>
                <p>
                  Presupuesto actual:{" "}
                  {day.capacityMicroseconds === null
                    ? "Desconocido"
                    : duration(day.capacityMicroseconds)}
                </p>
                {day.capacityMicroseconds === "0" && (
                  <p>
                    Sin tiempo presupuestado actualmente: descanso planificado
                    actual.
                  </p>
                )}
              </li>
            ))}
          </ol>
          <RouteLink href="/proyectos">Ver planificación</RouteLink>
        </>
      )}
    </main>
  );
}

function shiftWeek(date: string, days: number) {
  const day = new Date(`${date}T00:00:00Z`);
  day.setUTCDate(day.getUTCDate() + days);
  const dateText = day.toISOString().slice(0, 10);
  day.setUTCDate(day.getUTCDate() + 6);
  return /^(?!0000)\d{4}-\d{2}-\d{2}$/.test(dateText) &&
    day.getUTCFullYear() <= 9999
    ? dateText
    : null;
}
function duration(value: string) {
  const micros = BigInt(value);
  const hours = micros / 3_600_000_000n;
  const minutes = (micros % 3_600_000_000n) / 60_000_000n;
  const remaining = micros % 60_000_000n;
  return (
    [
      hours ? `${hours} h` : "",
      minutes ? `${minutes} min` : "",
      remaining ? `${seconds(String(remaining))} s` : "",
    ]
      .filter(Boolean)
      .join(" ") || "0 min"
  );
}
