import { RouteLink } from "./navigation";
import { isCsrfFailure } from "./api-client";
import {
  useEffect,
  useState,
  useRef,
  useLayoutEffect,
  type FormEvent,
} from "react";
import {
  DAY_KEYS,
  readAvailability,
  readAvailabilityZones,
  saveAvailability,
  type DailyMinutes,
  type DayKey,
  type AvailabilitySnapshot,
} from "./availability-api";
const DAY_LABELS: Record<DayKey, string> = {
  MONDAY: "Lunes",
  TUESDAY: "Martes",
  WEDNESDAY: "Miércoles",
  THURSDAY: "Jueves",
  FRIDAY: "Viernes",
  SATURDAY: "Sábado",
  SUNDAY: "Domingo",
};
function validMinutes(value: string) {
  return (
    value.trim() !== "" &&
    Number.isInteger(Number(value)) &&
    Number(value) >= 0 &&
    Number(value) <= 1440
  );
}
export function Availability() {
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    heading.current?.focus();
  }, []);
  const [errors, setErrors] = useState<Partial<Record<DayKey, string>>>({});
  const [zoneError, setZoneError] = useState<string>();
  const formRef = useRef<HTMLFormElement>(null);
  const submitFocus = useRef<
    HTMLButtonElement | HTMLInputElement | HTMLSelectElement | null
  >(null);
  const writeRequest = useRef<AbortController | null>(null);
  useEffect(() => () => writeRequest.current?.abort(), []);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [writeFailure, setWriteFailure] = useState(false);
  const [csrfRejected, setCsrfRejected] = useState(false);
  useLayoutEffect(() => {
    if (
      !saving &&
      (document.activeElement === document.body ||
        document.activeElement === heading.current ||
        formRef.current?.contains(document.activeElement))
    ) {
      const day = DAY_KEYS.find((key) => errors[key]);
      if (zoneError)
        formRef.current
          ?.querySelector<HTMLSelectElement>("#availability-zone")
          ?.focus();
      else if (day)
        formRef.current
          ?.querySelector<HTMLInputElement>(`#minutes-${day}`)
          ?.focus();
    }
  }, [errors, zoneError, saving]);
  useLayoutEffect(() => {
    if (
      !saving &&
      submitFocus.current &&
      document.activeElement === document.body
    ) {
      if (submitFocus.current.disabled) heading.current?.focus();
      else submitFocus.current.focus();
    }
  }, [saving]);
  const [snapshot, setSnapshot] = useState<AvailabilitySnapshot>();
  const [readFailure, setReadFailure] = useState(false);
  const [loading, setLoading] = useState(true);
  const [revision, setRevision] = useState(0);
  const [draft, setDraft] = useState<Record<DayKey, string>>();
  const [zones, setZones] = useState<string[]>();
  const [zoneFailure, setZoneFailure] = useState(false);
  const [zoneRevision, setZoneRevision] = useState(0);
  const [zoneId, setZoneId] = useState<string>();
  useLayoutEffect(() => {
    if (
      interacted.current &&
      !saving &&
      !loading &&
      document.activeElement === document.body
    )
      heading.current?.focus();
  }, [saving, loading, zones, zoneFailure, readFailure]);
  const [browserZone] = useState(() => {
    try {
      return new Intl.DateTimeFormat().resolvedOptions().timeZone;
    } catch {
      return "";
    }
  });
  const selectedZone =
    zoneId ?? (zones?.includes(browserZone) ? browserZone : "");
  const total =
    draft && DAY_KEYS.every((day) => validMinutes(draft[day]))
      ? DAY_KEYS.reduce((sum, day) => sum + Number(draft[day]), 0)
      : undefined;
  useEffect(() => {
    const controller = new AbortController();
    void readAvailabilityZones(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setZones(result);
          setZoneFailure(false);
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) setZoneFailure(true);
      });
    return () => controller.abort();
  }, [zoneRevision]);
  useEffect(() => {
    const controller = new AbortController();
    void readAvailability(controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setSnapshot(result);
        setZoneId(result.configured ? result.zoneId : undefined);
        setDraft(
          Object.fromEntries(
            DAY_KEYS.map((day) => [
              day,
              String(result.dailyMinutes?.[day] ?? 0),
            ]),
          ) as Record<DayKey, string>,
        );
        setWriteFailure(false);
        setReadFailure(false);
        setErrors({});
        setZoneError(undefined);
      })
      .catch(() => {
        if (!controller.signal.aborted) setReadFailure(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [revision]);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!snapshot || !draft || saving || loading || writeFailure) return;
    setZoneError(undefined);
    if (!zones?.includes(selectedZone)) {
      setZoneError("Elige una zona del catálogo");
      return;
    }
    const invalid = DAY_KEYS.filter((day) => !validMinutes(draft[day]));
    setErrors(
      Object.fromEntries(
        invalid.map((day) => [day, "Introduce minutos enteros entre 0 y 1440"]),
      ),
    );
    if (invalid.length) {
      event.currentTarget
        .querySelector<HTMLInputElement>(`#minutes-${invalid[0]}`)
        ?.focus();
      return;
    }
    setSaving(true);
    submitFocus.current =
      document.activeElement instanceof HTMLButtonElement ||
      document.activeElement instanceof HTMLInputElement ||
      document.activeElement instanceof HTMLSelectElement
        ? document.activeElement
        : null;
    setSaved(false);
    setCsrfRejected(false);
    const controller = new AbortController();
    writeRequest.current = controller;
    try {
      const result = await saveAvailability(
        {
          zoneId: selectedZone,
          dailyMinutes: Object.fromEntries(
            DAY_KEYS.map((day) => [day, Number(draft[day])]),
          ) as DailyMinutes,
        },
        snapshot.etag,
        controller.signal,
      );
      if (controller.signal.aborted) return;
      setSnapshot(result);
      setZoneId(result.zoneId);
      setDraft(
        Object.fromEntries(
          DAY_KEYS.map((day) => [day, String(result.dailyMinutes[day])]),
        ) as Record<DayKey, string>,
      );
      setSaved(true);
    } catch (error) {
      if (controller.signal.aborted) return;
      const csrf = await isCsrfFailure(error);
      if (controller.signal.aborted) return;
      if (csrf) {
        setCsrfRejected(true);
        return;
      }
      if (error instanceof Response && error.status === 400) {
        const body: unknown = await error.json().catch(() => null);
        if (controller.signal.aborted) return;
        const fieldErrors: Partial<Record<DayKey, string>> = {};
        let zoneMessage: string | undefined;
        if (
          body &&
          typeof body === "object" &&
          "errors" in body &&
          Array.isArray(body.errors)
        ) {
          for (const entry of body.errors) {
            if (
              entry &&
              typeof entry === "object" &&
              "field" in entry &&
              "message" in entry &&
              typeof entry.message === "string" &&
              entry.message.trim().length > 0
            ) {
              const day = DAY_KEYS.find(
                (key) => entry.field === `dailyMinutes.${key}`,
              );
              if (day) fieldErrors[day] = entry.message;
              if (entry.field === "zoneId") zoneMessage = entry.message;
            }
          }
        }
        setErrors(fieldErrors);
        setZoneError(zoneMessage);
        setWriteFailure(Object.keys(fieldErrors).length === 0 && !zoneMessage);
      } else setWriteFailure(true);
    } finally {
      if (!controller.signal.aborted) setSaving(false);
    }
  }
  return (
    <main id="proyectos" tabIndex={-1} className="reader availability">
      <div className="page-intro">
        <p className="eyebrow">ESPACIO PARA LO QUE IMPORTA</p>
        <h1 ref={heading} tabIndex={-1}>
          Disponibilidad
        </h1>
        <p>
          Decide cuánto espacio quieres dedicar cada día. Descansar también
          forma parte del plan.
        </p>
      </div>
      <p className="availability-warning">
        Los cambios sin guardar se pierden al salir
      </p>
      {snapshot && draft ? (
        <form
          className="form-card availability-form"
          ref={formRef}
          noValidate
          onSubmit={(event) => void submit(event)}
        >
          <div className="availability-form-heading">
            <h2>Tu semana, a tu ritmo</h2>
            <p className="availability-badge">
              {snapshot.configured
                ? "Disponibilidad configurada"
                : "Sin configurar"}
            </p>
          </div>
          <p className="availability-hint">0 permite descansar</p>
          {!zones && !zoneFailure && <p role="status">Consultando zonas</p>}
          {zoneFailure && (
            <div role="alert">
              <p>No se pudieron consultar las zonas</p>
              <button
                type="button"
                onClick={() => {
                  interacted.current = true;
                  setZoneFailure(false);
                  setZoneRevision((value) => value + 1);
                }}
              >
                Reintentar zonas
              </button>
            </div>
          )}
          <div className="field">
            <label htmlFor="availability-zone">Zona horaria</label>
            <select
              disabled={saving || loading}
              id="availability-zone"
              aria-invalid={zoneError ? true : undefined}
              aria-describedby={zoneError ? "error-zone" : undefined}
              value={selectedZone}
              onChange={(event) => {
                setZoneId(event.target.value);
                setSaved(false);
              }}
            >
              <option value="">Selecciona una zona</option>
              {selectedZone && !zones?.includes(selectedZone) && (
                <option value={selectedZone} disabled>
                  {selectedZone} · {zones ? "No disponible" : "Zona guardada"}
                </option>
              )}
              {zones?.map((zone) => (
                <option key={zone} value={zone}>
                  {zone}
                </option>
              ))}
            </select>
            {zoneError && (
              <p className="field-error" id="error-zone">
                {zoneError}
              </p>
            )}
            {zoneId === undefined && selectedZone && (
              <p>Sugerencia sin guardar</p>
            )}
          </div>
          <div className="availability-days">
            {DAY_KEYS.map((day) => (
              <div className="field" key={day}>
                <label htmlFor={`minutes-${day}`}>
                  {DAY_LABELS[day]} · minutos
                </label>
                <input
                  disabled={saving || loading}
                  id={`minutes-${day}`}
                  aria-invalid={errors[day] ? true : undefined}
                  aria-describedby={errors[day] ? `error-${day}` : undefined}
                  type="number"
                  min={0}
                  max={1440}
                  step={1}
                  value={draft[day]}
                  onChange={(event) => {
                    setDraft({ ...draft, [day]: event.target.value });
                    setSaved(false);
                  }}
                />
                {errors[day] && (
                  <p className="field-error" id={`error-${day}`}>
                    {errors[day]}
                  </p>
                )}
              </div>
            ))}
          </div>
          <div className="availability-total">
            <p>
              {total === undefined
                ? "Completa los siete presupuestos para calcular el total"
                : `Total semanal del borrador: ${total} minutos`}
            </p>
            <p>Es disponibilidad prevista, no trabajo realizado</p>
          </div>
          <button type="submit" disabled={saving || loading || writeFailure}>
            Guardar disponibilidad
          </button>
          {writeFailure && (
            <div role="alert">
              {readFailure && (
                <p>
                  No se pudo recargar la disponibilidad. Tu borrador se
                  conserva.
                </p>
              )}
              <p>
                No podemos confirmar el guardado. Recarga antes de volver a
                guardar.
              </p>
              <p>
                La recarga descartará tu borrador sólo cuando recibamos una
                versión válida
              </p>
              <button
                type="button"
                disabled={loading}
                onClick={() => {
                  interacted.current = true;
                  setLoading(true);
                  setReadFailure(false);
                  setRevision((value) => value + 1);
                }}
              >
                Recargar versión guardada
              </button>
            </div>
          )}
          {loading && <p role="status">Consultando disponibilidad</p>}
          {csrfRejected && (
            <p role="alert">
              La protección de sesión rechazó el guardado. Después de recuperar
              el acceso, puedes volver a enviarlo.
            </p>
          )}
          {saving && <p role="status">Guardando disponibilidad</p>}
          {saved && <p role="status">Disponibilidad guardada</p>}
        </form>
      ) : readFailure ? (
        <div role="alert">
          <p>No se pudo consultar la disponibilidad</p>
          <button
            type="button"
            onClick={() => {
              interacted.current = true;
              setReadFailure(false);
              setRevision((value) => value + 1);
            }}
          >
            Reintentar disponibilidad
          </button>
        </div>
      ) : (
        <p role="status">Consultando disponibilidad</p>
      )}
      <RouteLink className="back-link availability-cancel" href="/proyectos">
        Cancelar y volver a Proyectos
      </RouteLink>
    </main>
  );
}
