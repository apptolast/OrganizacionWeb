// @ts-nocheck
import { useEffect, useLayoutEffect, useRef, useId, useState } from "react";
import {
  CustomizationValidationError,
  isCustomFieldLabel,
  isCustomFieldType,
  type CustomFieldType,
  type CustomizationScope,
} from "./customization-api";
import type { CustomizationSession } from "./customization-state";

export function CustomizationControls({
  scope,
  session,
}: {
  scope: CustomizationScope;
  session: CustomizationSession;
}) {
  const anchor = useRef<HTMLElement>(null);
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
      if (document.activeElement === document.body) anchor.current?.focus();
    }
  });
  const [validation, setValidation] = useState<string>();
  const [saved, setSaved] = useState(false);
  const draft = session.viewDrafts[scope];
  const setDraft = (next: string[] | undefined) => {
    setSaved(false);
    setValidation(undefined);
    session.setViewDraft(scope, next);
  };
  const { loadConfig } = session;
  useEffect(() => {
    void loadConfig(scope);
  }, [scope, loadConfig]);
  const snapshot = session.configs[scope];
  const fields =
    scope === "PROJECT"
      ? [
          ["createdAt", "Creado"],
          ["updatedAt", "Actualizado"],
        ]
      : [
          ["completionCriterion", "Criterio de finalización"],
          ["estimatedMinutes", "Estimación"],
          ["createdAt", "Creado"],
          ["updatedAt", "Actualizado"],
        ];
  const defaults =
    scope === "PROJECT"
      ? ["createdAt"]
      : ["completionCriterion", "estimatedMinutes"];
  return (
    <section
      className="customization"
      ref={anchor}
      tabIndex={-1}
      aria-label="Personalización de vista"
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
      {session.failures[scope] ? (
        <>
          <p role="alert">{session.failures[scope]}</p>
          <button
            onClick={() => {
              setSaved(false);
              void session.reloadConfig(scope);
            }}
          >
            {session.isConfigUncertain(scope)
              ? "Recargar guardado"
              : "Reintentar"}
          </button>
        </>
      ) : !snapshot || session.loading[scope] ? (
        <p role="status">Consultando vista</p>
      ) : (
        <>
          <button onClick={() => setDraft([...snapshot.visibleFields])}>
            Personalizar vista
          </button>
          {session.saving[scope] && (
            <p role="status">Guardando personalización</p>
          )}
          {draft && (
            <fieldset disabled={session.saving[scope]}>
              <legend>Metadatos visibles</legend>
              {validation && <p role="alert">{validation}</p>}
              {fields.map(([field, label]) => (
                <label key={field}>
                  <input
                    type="checkbox"
                    checked={draft.includes(field)}
                    onChange={(event) =>
                      setDraft(
                        event.target.checked
                          ? [...draft, field]
                          : draft.filter((item) => item !== field),
                      )
                    }
                  />
                  {label}
                </label>
              ))}
              <ol>
                {draft.map((field, index) => {
                  const label = fields.find(([id]) => id === field)?.[1];
                  const move = (offset: number) => {
                    const next = [...draft];
                    [next[index], next[index + offset]] = [
                      next[index + offset],
                      next[index],
                    ];
                    setDraft(next);
                  };
                  return (
                    <li key={field}>
                      {label}
                      <button
                        aria-label={`Subir ${label}`}
                        disabled={index === 0}
                        onClick={() => move(-1)}
                      >
                        Subir
                      </button>
                      <button
                        aria-label={`Bajar ${label}`}
                        disabled={index === draft.length - 1}
                        onClick={() => move(1)}
                      >
                        Bajar
                      </button>
                    </li>
                  );
                })}
              </ol>
              <button onClick={() => setDraft(undefined)}>Cancelar</button>
              <button onClick={() => setDraft(defaults)}>
                Restaurar vista
              </button>
              <button
                onClick={async () => {
                  try {
                    if (!(await session.saveView(scope, draft))) return;
                    setDraft(undefined);
                    setSaved(true);
                  } catch (error) {
                    if (error instanceof CustomizationValidationError)
                      setValidation(
                        error.errors.map((item) => item.message).join(" "),
                      );
                  }
                }}
              >
                Guardar vista
              </button>
            </fieldset>
          )}
          {saved && <p role="status">Vista guardada</p>}
          <CustomFieldDefinitions scope={scope} session={session} />
        </>
      )}
    </section>
  );
}

function CustomFieldDefinitions({
  scope,
  session,
}: {
  scope: CustomizationScope;
  session: CustomizationSession;
}) {
  const errorId = useId();
  const [open, setOpen] = useState(false);
  const [label, setLabel] = useState("");
  const [type, setType] = useState<CustomFieldType>("TEXT");
  const [confirmation, setConfirmation] = useState<string>();
  const [editing, setEditing] = useState<string>();
  const [active, setActive] = useState(true);
  const [failure, setFailure] = useState<string>();
  return (
    <section>
      <button onClick={() => setOpen(!open)}>
        Gestionar campos personales
      </button>
      {open && (
        <>
          <p>
            Hasta 12 campos, incluidos los desactivados. El tipo no se puede
            cambiar. Desactivar oculta el campo y conserva sus valores. Puedes
            volver a activarlo.
          </p>
          <ul>
            {session.configs[scope]?.customFields.map((field) => (
              <li key={field.id}>
                {field.label}
                {!field.active && " — Desactivado"}
                <button
                  onClick={() => {
                    setEditing(field.id);
                    setLabel(field.label);
                    setType(field.type);
                    setActive(field.active);
                    setFailure(undefined);
                    setConfirmation(undefined);
                  }}
                >
                  Editar {field.label}
                </button>
              </li>
            ))}
          </ul>
          <form
            onChange={() => {
              setConfirmation(undefined);
              setFailure(undefined);
            }}
            onSubmit={async (event) => {
              event.preventDefault();
              if (
                !isCustomFieldLabel(
                  label.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, ""),
                )
              ) {
                setFailure(
                  "Escribe una etiqueta de 1 a 60 caracteres válidos.",
                );
                return;
              }
              if (
                session.configs[scope]?.customFields.some(
                  (field) =>
                    field.id !== editing &&
                    field.label ===
                      label.replace(
                        /^\p{White_Space}+|\p{White_Space}+$/gu,
                        "",
                      ),
                )
              ) {
                setFailure(
                  "Ya existe un campo con esa etiqueta, aunque esté desactivado.",
                );
                return;
              }
              try {
                const saved = editing
                  ? await session.updateField(scope, editing, { label, active })
                  : await session.createField(scope, { label, type });
                if (saved) {
                  setConfirmation(editing ? "Campo guardado" : "Campo creado");
                  setEditing(undefined);
                  setLabel("");
                }
              } catch (error) {
                if (error instanceof CustomizationValidationError)
                  setFailure(
                    error.errors.map((item) => item.message).join(" "),
                  );
              }
            }}
          >
            <fieldset disabled={session.saving[scope]}>
              <legend>{editing ? "Editar campo" : "Nuevo campo"}</legend>
              <label>
                Etiqueta
                <input
                  aria-invalid={Boolean(failure)}
                  aria-describedby={failure ? errorId : undefined}
                  value={label}
                  onChange={(event) => setLabel(event.target.value)}
                />
              </label>
              {!editing && (
                <label>
                  Tipo
                  <select
                    value={type}
                    onChange={(event) => {
                      if (isCustomFieldType(event.target.value))
                        setType(event.target.value);
                    }}
                  >
                    <option value="TEXT">Texto</option>
                    <option value="NUMBER">Número entero</option>
                    <option value="BOOLEAN">Sí o no</option>
                    <option value="DATE">Fecha</option>
                  </select>
                </label>
              )}
              {editing && (
                <label>
                  <input
                    type="checkbox"
                    checked={active}
                    onChange={(event) => setActive(event.target.checked)}
                  />
                  Activo
                </label>
              )}
              <button
                type="button"
                onClick={() => {
                  setEditing(undefined);
                  setLabel("");
                  setType("TEXT");
                  setActive(true);
                  setFailure(undefined);
                  setConfirmation(undefined);
                }}
              >
                Cancelar edición
              </button>
              <button
                disabled={
                  !editing &&
                  (session.configs[scope]?.customFields.length ?? 0) >= 12
                }
              >
                {editing ? "Guardar campo" : "Crear campo"}
              </button>
            </fieldset>
          </form>
          {failure && (
            <p id={errorId} role="alert">
              {failure}
            </p>
          )}
          {confirmation && <p role="status">{confirmation}</p>}
        </>
      )}
    </section>
  );
}
