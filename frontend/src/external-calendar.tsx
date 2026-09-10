import { useCallback, useEffect, useRef, useState } from "react";
import {
  ConnectorsDisabledError,
  ExternalCalendarNotConfiguredError,
  ExternalCalendarValidationError,
  deleteExternalCalendar,
  readExternalCalendar,
  readExternalEvents,
  saveExternalCalendar,
  syncExternalCalendar,
} from "./external-calendar-api";
import type {
  ExternalEvent,
  ExternalSubscription,
  FeedError,
} from "./external-calendar-api";

export const FEED_MESSAGES: Record<FeedError, string> = {
  FEED_REJECTED:
    "La dirección apunta a una red interna o su nombre ya no resuelve. Revisa la dirección y guárdala de nuevo.",
  FEED_UNREACHABLE:
    "No se ha podido contactar con el proveedor. Vuelve a intentarlo en unos minutos.",
  FEED_HTTP_ERROR:
    "El proveedor respondió con un error. Vuelve a generar la dirección secreta en tu calendario y pégala otra vez.",
  FEED_TOO_LARGE:
    "El calendario ocupa más de 1 MiB. Reduce el rango que publica tu proveedor.",
  FEED_UNSUPPORTED_TYPE:
    "Esa dirección no devuelve un calendario. Comprueba que copiaste la dirección iCal y no la de la página web.",
  FEED_MALFORMED:
    "Lo descargado no es un calendario iCalendar. Comprueba la dirección con tu proveedor.",
  SECRET_UNREADABLE:
    "Ya no se puede leer la dirección guardada. Vuelve a pegarla para seguir sincronizando.",
};
const UNCERTAIN =
  "No sabemos si se guardó. Vuelve a intentarlo cuando quieras.";
const WINDOW_BEFORE_MS = 24 * 3600 * 1000;
const WINDOW_AFTER_MS = 336 * 3600 * 1000;

export function storedWindow(now: number) {
  const stamp = (value: number) =>
    new Date(Math.floor(value / 1000) * 1000)
      .toISOString()
      .replace(/\.\d+Z$/, "Z");
  return {
    from: stamp(now - WINDOW_BEFORE_MS),
    to: stamp(now + WINDOW_AFTER_MS),
  };
}
export function countersOf(subscription: ExternalSubscription) {
  const plural = (count: number, one: string, many: string) =>
    `${count} ${count === 1 ? one : many}`;
  return [
    plural(subscription.imported, "evento", "eventos"),
    plural(
      subscription.skippedRecurring,
      "recurrente no incluido",
      "recurrentes no incluidos",
    ),
    plural(subscription.skippedCancelled, "cancelado", "cancelados"),
    plural(subscription.skippedInvalid, "inválido", "inválidos"),
  ].join(", ");
}
export function formatMoment(value: string | null, zone: string | null) {
  if (value === null) return "";
  return new Intl.DateTimeFormat("es", {
    dateStyle: "medium",
    timeStyle: "short",
    ...(zone ? { timeZone: zone } : {}),
  }).format(new Date(value));
}
function formatClock(value: string, zone: string | null) {
  return new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    ...(zone ? { timeZone: zone } : {}),
  }).format(new Date(value));
}
export function describeEvent(event: ExternalEvent, zone: string | null) {
  return event.allDay
    ? "Todo el día"
    : `${formatClock(event.startAt, zone)}–${formatClock(event.endAt, zone)}`;
}

type Busy = "" | "saving" | "syncing" | "deleting";

export function ExternalCalendar() {
  const [subscription, setSubscription] = useState<ExternalSubscription | null>(
    null,
  );
  const [loaded, setLoaded] = useState(false);
  const [events, setEvents] = useState<ExternalEvent[]>([]);
  const [label, setLabel] = useState("");
  const [address, setAddress] = useState("");
  const [busy, setBusy] = useState<Busy>("");
  const [announcement, setAnnouncement] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    label?: string;
    url?: string;
  }>({});
  const [failure, setFailure] = useState("");
  const [confirming, setConfirming] = useState(false);
  const [invalidList, setInvalidList] = useState(false);
  const inFlight = useRef<AbortController | null>(null);
  const focusOn = useRef<"label" | "url" | null>(null);
  const labelField = useRef<HTMLInputElement | null>(null);
  const addressField = useRef<HTMLInputElement | null>(null);

  const start = useCallback(() => {
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;
    return controller.signal;
  }, []);

  const forget = useCallback(() => {
    setSubscription(null);
    setEvents([]);
    setLabel("");
    setAddress("");
    setConfirming(false);
  }, []);

  const loadEvents = useCallback(async (signal: AbortSignal) => {
    const range = storedWindow(Date.now());
    try {
      const view = await readExternalEvents(range.from, range.to, signal);
      setEvents(view.items);
      setInvalidList(false);
    } catch (error) {
      if (signal.aborted) throw error;
      // Cualquier lectura que no llega deja la lista en desconocido: decir «no hay
      // eventos» seria afirmar un hecho que la pantalla no conoce. Antes solo se
      // marcaba para los rechazos que eran Error, asi que un 500 —que refuse()
      // relanza como Response— pintaba la ventana como vacia.
      setInvalidList(true);
      setEvents([]);
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    inFlight.current = controller;
    void (async () => {
      try {
        const snapshot = await readExternalCalendar(controller.signal);
        setSubscription(snapshot.subscription);
        setLoaded(true);
        if (snapshot.subscription) {
          setLabel(snapshot.subscription.label);
          await loadEvents(controller.signal);
        }
      } catch (error) {
        if (controller.signal.aborted) return;
        setLoaded(true);
        setFailure(
          error instanceof ConnectorsDisabledError
            ? error.message
            : "No se ha podido cargar tu calendario externo.",
        );
      }
    })();
    return () => controller.abort();
  }, [loadEvents]);

  useEffect(() => {
    if (!focusOn.current) return;
    const field =
      focusOn.current === "label" ? labelField.current : addressField.current;
    focusOn.current = null;
    field?.focus();
  }, [fieldErrors]);

  const failed = (error: unknown) => {
    if (error instanceof ExternalCalendarValidationError) {
      setFieldErrors(error.fields);
      focusOn.current = error.fields.label ? "label" : "url";
      return;
    }
    if (error instanceof ConnectorsDisabledError) {
      setFailure(`${error.message} ${UNCERTAIN}`);
      return;
    }
    if (error instanceof Response && error.status === 401) {
      forget();
      return;
    }
    setFailure(UNCERTAIN);
  };

  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (busy) return;
    const signal = start();
    setBusy("saving");
    setAnnouncement("Guardando…");
    setFieldErrors({});
    setFailure("");
    try {
      const saved = await saveExternalCalendar(label, address, signal);
      setSubscription(saved);
      setAddress("");
      setAnnouncement("Guardado.");
      await loadEvents(signal);
    } catch (error) {
      if (signal.aborted) return;
      setAnnouncement("");
      failed(error);
    } finally {
      if (!signal.aborted) setBusy("");
    }
  }

  async function synchronise() {
    if (busy) return;
    const signal = start();
    setBusy("syncing");
    setAnnouncement("Sincronizando…");
    setFailure("");
    try {
      const outcome = await syncExternalCalendar(false, signal);
      setSubscription(outcome.subscription);
      setAnnouncement(
        outcome.subscription.lastStatus === "OK"
          ? "Sincronizado."
          : "Sincronización fallida.",
      );
      if (outcome.subscription.lastStatus === "OK") await loadEvents(signal);
    } catch (error) {
      if (signal.aborted) return;
      setAnnouncement("");
      if (error instanceof ExternalCalendarNotConfiguredError) forget();
      else failed(error);
    } finally {
      if (!signal.aborted) setBusy("");
    }
  }

  async function confirmRemoval() {
    if (busy) return;
    const signal = start();
    setBusy("deleting");
    setAnnouncement("Eliminando…");
    try {
      await deleteExternalCalendar(signal);
      forget();
      setAnnouncement("Suscripción eliminada.");
    } catch (error) {
      if (signal.aborted) return;
      setAnnouncement("");
      failed(error);
    } finally {
      if (!signal.aborted) setBusy("");
    }
  }

  const zone = subscription?.snapshotZoneId ?? null;
  const locked = busy !== "";

  return (
    <main id="proyectos" tabIndex={-1} className="external-calendar">
      <div className="page-intro">
        <h1>Calendario externo</h1>
        <p>
          Muestra en Hoy los eventos de un calendario que ya usas. Solo se lee:
          esta aplicación nunca escribe en tu proveedor.
        </p>
      </div>
      <section className="form-card" aria-labelledby="external-calendar-form">
        <h2 id="external-calendar-form">
          {subscription
            ? "Cambiar la suscripción"
            : "Suscribirte a un calendario"}
        </h2>
        <form onSubmit={save} noValidate aria-busy={busy === "saving"}>
          <div className="field">
            <label htmlFor="external-calendar-label">Etiqueta</label>
            <input
              id="external-calendar-label"
              ref={labelField}
              type="text"
              autoComplete="off"
              value={label}
              readOnly={locked}
              aria-invalid={Boolean(fieldErrors.label)}
              aria-describedby="external-calendar-label-error"
              onChange={(event) => setLabel(event.target.value)}
            />
            <p className="field-error" id="external-calendar-label-error">
              {fieldErrors.label ?? ""}
            </p>
          </div>
          <div className="field">
            <label htmlFor="external-calendar-url">
              Dirección secreta iCal
            </label>
            <input
              id="external-calendar-url"
              ref={addressField}
              type="url"
              autoComplete="off"
              spellCheck={false}
              value={address}
              readOnly={locked}
              aria-invalid={Boolean(fieldErrors.url)}
              aria-describedby="external-calendar-url-help external-calendar-url-error"
              onChange={(event) => setAddress(event.target.value)}
            />
            <p className="field-hint" id="external-calendar-url-help">
              En Google Calendar: Configuración del calendario, Integrar
              calendario, Dirección secreta en formato iCal. Trátala como una
              contraseña.
            </p>
            <p className="field-error" id="external-calendar-url-error">
              {fieldErrors.url ?? ""}
            </p>
          </div>
          {failure ? (
            <p className="failure" role="alert">
              {failure}
            </p>
          ) : null}
          <div className="form-footer">
            <button type="submit" disabled={locked}>
              Guardar
            </button>
          </div>
        </form>
      </section>
      {subscription ? (
        <section
          className="result-card"
          aria-labelledby="external-calendar-state"
        >
          <h2 id="external-calendar-state">{subscription.label}</h2>
          <p className="external-calendar-address">
            <span>{subscription.urlHost}</span>
            <span aria-hidden="true"> … </span>
            <span>{subscription.urlTail}</span>
          </p>
          <dl>
            <dt>Última sincronización correcta</dt>
            <dd>{formatMoment(subscription.lastSyncAt, zone)}</dd>
            <dt>Último intento</dt>
            <dd>{formatMoment(subscription.lastAttemptAt, zone)}</dd>
          </dl>
          <p className="external-calendar-counters">
            {countersOf(subscription)}
          </p>
          {subscription.truncated ? (
            <p className="notice" role="note">
              Solo se conservan los 500 primeros eventos de la ventana.
            </p>
          ) : null}
          {subscription.lastStatus === "FAILED" && subscription.lastError ? (
            <p className="failure" role="alert">
              {FEED_MESSAGES[subscription.lastError]}
            </p>
          ) : null}
          <div className="form-footer">
            <button
              type="button"
              onClick={() => void synchronise()}
              disabled={locked}
            >
              Sincronizar ahora
            </button>
            <button
              type="button"
              className="danger"
              onClick={() => setConfirming(true)}
              disabled={locked}
            >
              Eliminar suscripción
            </button>
          </div>
          {confirming ? (
            <div role="alertdialog" aria-label="Confirmar la eliminación">
              <p>Se borrarán la suscripción y los eventos guardados.</p>
              <button type="button" onClick={() => void confirmRemoval()}>
                Sí, eliminar
              </button>
              <button type="button" onClick={() => setConfirming(false)}>
                Cancelar
              </button>
            </div>
          ) : null}
          <h3>Eventos</h3>
          {invalidList ? (
            <p className="failure" role="alert">
              No se ha podido leer la lista de eventos.
            </p>
          ) : events.length === 0 ? (
            <p>No hay eventos en la ventana guardada.</p>
          ) : (
            <ul className="external-calendar-events">
              {events.map((event) => (
                <li key={event.uid}>
                  <span className="external-calendar-summary">
                    {event.summary === "" ? "Sin título" : event.summary}
                  </span>{" "}
                  <span className="external-calendar-when">
                    {describeEvent(event, zone)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      ) : loaded ? (
        <p className="quiet-note">
          Todavía no tienes ningún calendario externo.
        </p>
      ) : null}
      <p
        className="save-status"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {announcement}
      </p>
    </main>
  );
}
