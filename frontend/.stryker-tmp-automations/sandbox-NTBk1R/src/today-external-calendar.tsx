// @ts-nocheck
import { useEffect, useState } from "react";
import { RouteLink } from "./navigation";
import {
  readExternalEvents,
  syncExternalCalendar,
} from "./external-calendar-api";
import type { ExternalEvent, SyncStatus } from "./external-calendar-api";
import { describeEvent } from "./external-calendar";

type Reading =
  | { kind: "hidden" }
  | { kind: "unreadable" }
  | { kind: "unreachable" }
  | {
      kind: "events";
      lastSyncAt: string | null;
      lastStatus: SyncStatus | null;
      items: ExternalEvent[];
    };

function clock(value: string | null, zoneId: string) {
  if (value === null) return "";
  try {
    return new Intl.DateTimeFormat("es", {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
      timeZone: zoneId,
    }).format(new Date(value));
  } catch {
    return value;
  }
}

/**
 * Sección de solo lectura de Hoy. Se monta con la instantánea de Hoy ya pintada, así que la agenda
 * de bloques nunca espera por estas dos llamadas ni se degrada si fallan.
 */
export function TodayExternalCalendar({
  zoneId,
  dayStartAt,
  dayEndAt,
  revision,
}: {
  zoneId: string;
  dayStartAt: string;
  dayEndAt: string;
  revision: string;
}) {
  const [reading, setReading] = useState<Reading>({ kind: "hidden" });
  const [pendingSync, setPendingSync] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    const signal = controller.signal;
    void (async () => {
      let pending = false;
      try {
        await syncExternalCalendar(true, signal);
      } catch {
        if (signal.aborted) return;
        pending = true;
      }
      try {
        const view = await readExternalEvents(dayStartAt, dayEndAt, signal);
        if (signal.aborted) return;
        setPendingSync(pending);
        setReading(
          view.configured
            ? {
                kind: "events",
                lastSyncAt: view.lastSyncAt,
                lastStatus: view.lastStatus,
                items: view.items,
              }
            : { kind: "hidden" },
        );
      } catch (error) {
        if (signal.aborted) return;
        setPendingSync(pending);
        setReading({
          kind:
            error instanceof Error &&
            error.message.startsWith("Respuesta de calendario externo inválida")
              ? "unreadable"
              : "unreachable",
        });
      }
    })();
    return () => controller.abort();
  }, [dayStartAt, dayEndAt, revision]);

  if (reading.kind === "hidden") return null;
  return (
    <section
      className="today-external-calendar"
      aria-label="Calendario externo"
      aria-live="polite"
    >
      <h2>Calendario externo</h2>
      {reading.kind === "unreadable" || reading.kind === "unreachable" ? (
        <p>
          {reading.kind === "unreadable"
            ? "No se ha podido leer el calendario externo."
            : "No se ha podido consultar el calendario externo."}{" "}
          <RouteLink href="/calendario-externo">
            Revisar el calendario externo
          </RouteLink>
        </p>
      ) : (
        <>
          <p className="today-external-calendar-stamp">
            {reading.lastStatus === "FAILED"
              ? `Sincronización fallida, se muestran datos de ${clock(reading.lastSyncAt, zoneId)}`
              : `Según sincronización de ${clock(reading.lastSyncAt, zoneId)}`}
          </p>
          {pendingSync ? <p>Sincronización pendiente.</p> : null}
          {reading.items.length === 0 ? (
            <p>No hay eventos del calendario externo.</p>
          ) : (
            <ul className="today-external-calendar-events">
              {reading.items.map((event) => (
                <li key={event.uid}>
                  <span>
                    {event.summary === "" ? "Sin título" : event.summary}
                  </span>{" "}
                  <span>{describeEvent(event, zoneId)}</span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  );
}
