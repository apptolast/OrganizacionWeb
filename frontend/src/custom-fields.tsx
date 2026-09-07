import { useEffect, useLayoutEffect, useId, useRef, useState } from "react";
import { CustomizationValidationError } from "./customization-api";
import type { CustomizationSession } from "./customization-state";

export function CustomFieldsPanel({
  projectId,
  taskId,
  session,
  onAccessFailure,
}: {
  projectId: string;
  taskId?: string;
  session: CustomizationSession;
  onAccessFailure?: (status: number) => void;
}) {
  const heading = useRef<HTMLHeadingElement>(null);
  const initiator = useRef<HTMLElement | null>(null);
  useEffect(() => {
    const moved = (event: FocusEvent) => {
      if (event.target !== initiator.current) initiator.current = null;
    };
    document.addEventListener("focusin", moved);
    return () => document.removeEventListener("focusin", moved);
  }, []);
  useLayoutEffect(() => {
    const control = initiator.current;
    if (control && !control.isConnected) {
      initiator.current = null;
      if (document.activeElement === document.body) heading.current?.focus();
    }
  });
  const errorId = useId();
  const accessFailure = useRef(onAccessFailure);
  useEffect(() => {
    accessFailure.current = onAccessFailure;
  }, [onAccessFailure]);
  const [formError, setFormError] = useState<string>();
  const [errors, setErrors] = useState<Record<string, string>>({});
  const key = `${projectId}/${taskId ?? ""}`;
  const draft = session.valueDrafts[key] ?? {};
  const setDraft = (change: React.SetStateAction<Record<string, string>>) => {
    setSaved(false);
    setErrors({});
    setFormError(undefined);
    session.setValueDraft(key, change);
  };
  const [saved, setSaved] = useState(false);
  const { loadValues, retireValues } = session;
  const generation = session.valueGenerations[key] ?? 0;
  useEffect(() => {
    let current = true;
    void loadValues(projectId, taskId).catch((error: unknown) => {
      if (
        current &&
        error instanceof Response &&
        (error.status === 401 || error.status === 404)
      )
        accessFailure.current?.(error.status);
    });
    return () => {
      current = false;
      retireValues(projectId, taskId);
    };
  }, [projectId, taskId, loadValues, retireValues, generation]);
  const snapshot = session.values[key];
  const stale = session.isValuesStale(key);
  const reloadSaved = () => {
    setSaved(false);
    void session.reloadValues(projectId, taskId)?.catch((error: unknown) => {
      if (
        error instanceof Response &&
        (error.status === 401 || error.status === 404)
      )
        accessFailure.current?.(error.status);
    });
  };
  return (
    <section
      className="custom-fields"
      onClickCapture={(event) => {
        const button = (event.target as HTMLElement).closest("button");
        if (button === document.activeElement) initiator.current = button;
      }}
      onBlurCapture={(event) => {
        if (
          event.target === initiator.current &&
          event.target.isConnected &&
          !(
            event.target instanceof HTMLButtonElement &&
            event.target.matches(":disabled")
          )
        )
          initiator.current = null;
      }}
    >
      <h2 ref={heading} tabIndex={-1}>
        Campos personales
      </h2>
      {stale && !session.valueFailures[key] && (
        <div>
          <p role="alert">
            Los campos han cambiado. Recargar guardado reemplazará este borrador
            por lo guardado.
          </p>
          {session.valueReading[key] && (
            <p role="status">Consultando campos personales</p>
          )}
          <button disabled={session.valueReading[key]} onClick={reloadSaved}>
            Recargar guardado
          </button>
        </div>
      )}
      {session.valueFailures[key] ? (
        <>
          <p role="alert">{session.valueFailures[key]}</p>
          {session.valueReading[key] && (
            <p role="status">Consultando campos personales</p>
          )}
          <button disabled={session.valueReading[key]} onClick={reloadSaved}>
            {session.isValuesUncertain(key)
              ? "Recargar guardado"
              : "Reintentar"}
          </button>
        </>
      ) : !snapshot ? (
        <p role="status">Consultando campos personales</p>
      ) : snapshot.values.length === 0 ? (
        <p>No hay campos personales activos</p>
      ) : (
        <form
          onSubmit={async (event) => {
            event.preventDefault();
            setFormError(undefined);
            const invalid: Record<string, string> = {};
            for (const entry of snapshot.values) {
              const text = draft[entry.fieldId];
              if (
                entry.type === "TEXT" &&
                text &&
                ([...text].length > 1000 ||
                  text.includes(String.fromCharCode(0)) ||
                  /[\uD800-\uDFFF]/u.test(text))
              )
                invalid[entry.fieldId] =
                  "Escribe hasta 1000 caracteres válidos.";
              if (
                entry.type === "NUMBER" &&
                text &&
                (!/^[+-]?[0-9]+$/.test(text) ||
                  !Number.isFinite(Number(text)) ||
                  Math.abs(Number(text)) > 1000000000)
              )
                invalid[entry.fieldId] =
                  "Escribe un número entero entre -1000000000 y 1000000000.";
            }
            setErrors(invalid);
            if (Object.keys(invalid).length) return;
            const sent = snapshot.values.map((entry) => ({
              fieldId: entry.fieldId,
              value: Object.hasOwn(draft, entry.fieldId)
                ? draft[entry.fieldId] === ""
                  ? null
                  : entry.type === "NUMBER"
                    ? Number(draft[entry.fieldId])
                    : entry.type === "BOOLEAN"
                      ? draft[entry.fieldId] === "true"
                      : draft[entry.fieldId]
                : entry.value,
            }));
            try {
              if (!(await session.saveValues(projectId, sent, taskId))) return;
            } catch (error) {
              if (
                error instanceof Response &&
                (error.status === 401 || error.status === 404)
              )
                onAccessFailure?.(error.status);
              if (error instanceof CustomizationValidationError) {
                const next: Record<string, string> = {};
                for (const item of error.errors) {
                  if (item.field === "values") setFormError(item.message);
                  const index = item.field.match(
                    /^values\[([0-9]+)\]\.(?:fieldId|value)$/,
                  )?.[1];
                  if (index !== undefined)
                    next[sent[Number(index)].fieldId] = item.message;
                }
                setErrors(next);
              }
              return;
            }
            setDraft({});
            setSaved(true);
          }}
        >
          {session.valueSaving[key] && (
            <p role="status">Guardando campos personales</p>
          )}
          {formError && <p role="alert">{formError}</p>}
          <fieldset disabled={session.valueSaving[key] || stale}>
            <legend>Valores personales</legend>
            {snapshot.values.map((entry) => {
              const id = `${errorId}-${entry.fieldId}-control`;
              const props = {
                id,
                value:
                  draft[entry.fieldId] ??
                  (entry.value === null ? "" : String(entry.value)),
                "aria-invalid": Boolean(errors[entry.fieldId]),
                "aria-describedby": errors[entry.fieldId]
                  ? `${errorId}-${entry.fieldId}`
                  : undefined,
                onChange: (
                  event: React.ChangeEvent<
                    HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
                  >,
                ) =>
                  setDraft((previous) => ({
                    ...previous,
                    [entry.fieldId]: event.target.value,
                  })),
              };
              return (
                <div key={entry.fieldId}>
                  <label htmlFor={id}>{entry.label}</label>
                  {entry.type === "TEXT" ? (
                    <textarea {...props} />
                  ) : entry.type === "BOOLEAN" ? (
                    <select {...props}>
                      <option value="">Sin valor</option>
                      <option value="true">Sí</option>
                      <option value="false">No</option>
                    </select>
                  ) : (
                    <input
                      {...props}
                      type={entry.type === "DATE" ? "date" : "text"}
                      min={entry.type === "DATE" ? "0001-01-01" : undefined}
                      max={entry.type === "DATE" ? "9999-12-31" : undefined}
                      inputMode="numeric"
                    />
                  )}
                  <button
                    type="button"
                    aria-label={`Vaciar ${entry.label}`}
                    onClick={() =>
                      setDraft((previous) => ({
                        ...previous,
                        [entry.fieldId]: "",
                      }))
                    }
                  >
                    Vaciar
                  </button>
                  {errors[entry.fieldId] && (
                    <span role="alert" id={`${errorId}-${entry.fieldId}`}>
                      {errors[entry.fieldId]}
                    </span>
                  )}
                </div>
              );
            })}
            <button type="button" onClick={() => setDraft({})}>
              Cancelar
            </button>
            <button>Guardar campos</button>
          </fieldset>
        </form>
      )}
      {saved && <p role="status">Campos guardados</p>}
    </section>
  );
}
