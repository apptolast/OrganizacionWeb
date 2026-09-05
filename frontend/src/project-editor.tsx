import { useEditProject } from "./use-edit-project";
import { RouteLink } from "./navigation";
import { useLayoutEffect, useRef } from "react";
export function ProjectEditor({ route }: { route: string }) {
  const heading = useRef<HTMLHeadingElement>(null);
  useLayoutEffect(() => {
    heading.current?.focus();
  }, []);
  const {
    loadFailure,
    formRef,
    fields,
    loading,
    detailRoute,
    draft,
    setDraft,
    success,
    saving,
    failure,
    setFailure,
    revision,
    setRevision,
    submit,
  } = useEditProject(route);
  const reload = () => {
    setFailure(null);
    setRevision(revision + 1);
  };
  if (failure === 401 || failure === 404)
    return (
      <main id="proyectos" tabIndex={-1} className="reader">
        <div className="page-intro">
          <h1>
            {failure === 401
              ? "Autenticación requerida"
              : "Proyecto no encontrado"}
          </h1>
        </div>
        <section className="read-notice">
          <p role="alert">
            {failure === 401
              ? "Autenticación requerida. Vuelve a autenticarte y recarga la página."
              : "Proyecto no encontrado. No está disponible para tu cuenta."}
          </p>
          <RouteLink className="secondary-link" href="/proyectos">
            Volver a proyectos
          </RouteLink>
        </section>
      </main>
    );
  return (
    <main id="proyectos" tabIndex={-1} className="reader editor">
      <div className="page-intro">
        <p className="eyebrow">ESPACIO PARA EVOLUCIONAR</p>
        <h1 ref={heading} tabIndex={-1}>
          Editar proyecto
        </h1>
        <p>Ajusta tu idea. Cada cambio se guarda cuando tú decides.</p>
      </div>
      {!draft &&
        (failure === null ? (
          <section className="read-notice">
            <p role="status">Cargando proyecto</p>
          </section>
        ) : (
          <section className="read-notice read-error">
            <p role="alert">No hemos podido cargar el proyecto.</p>
            <button onClick={reload}>Reintentar</button>
          </section>
        ))}
      {draft && (
        <section
          className="form-card edit-card"
          aria-label="Datos del proyecto"
        >
          <div className="card-heading">
            <span className="section-icon" aria-hidden="true">
              ✎
            </span>
            <div>
              <h2>La idea, en tus palabras</h2>
              <p>Nombre y descripción de tu proyecto.</p>
            </div>
          </div>
          <form
            ref={formRef}
            onSubmit={submit}
            noValidate
            aria-busy={saving || loading}
          >
            <div className="field">
              <label htmlFor="name">
                Nombre del proyecto{" "}
                <span className="required-word">Obligatorio</span>
              </label>
              <input
                id="name"
                name="name"
                autoComplete="off"
                aria-required="true"
                aria-invalid={fields.includes("name")}
                aria-describedby="name-help name-error"
                readOnly={saving || loading}
                value={draft.name}
                onChange={(event) =>
                  setDraft({ ...draft, name: event.target.value })
                }
              />
              <p id="name-help" className="field-hint">
                Hasta 120 caracteres. Un nombre que tenga sentido para ti.
              </p>
              <p id="name-error" className="field-error">
                {fields.includes("name")
                  ? "Revisa el nombre: obligatorio y hasta 120 caracteres."
                  : ""}
              </p>
            </div>
            <div className="field">
              <label htmlFor="description">
                Descripción <span className="optional-word">Opcional</span>
              </label>
              <textarea
                id="description"
                name="description"
                autoComplete="off"
                aria-invalid={fields.includes("description")}
                aria-describedby="description-help description-error"
                readOnly={saving || loading}
                value={draft.description}
                onChange={(event) =>
                  setDraft({ ...draft, description: event.target.value })
                }
              />
              <p id="description-help" className="field-hint">
                Hasta 4000 caracteres. Puedes dejarla vacía.
              </p>
              <p id="description-error" className="field-error">
                {fields.includes("description")
                  ? "Revisa la descripción: hasta 4000 caracteres."
                  : ""}
              </p>
            </div>
            <div className="edit-actions">
              <button type="submit" disabled={saving || loading}>
                Guardar cambios
              </button>
              <RouteLink href={detailRoute} className="secondary-link">
                Cancelar
              </RouteLink>
            </div>
          </form>
          {failure !== null && (
            <div className="edit-feedback read-error">
              <p role="alert">
                {loadFailure
                  ? "No hemos podido cargar la versión guardada. Conservamos tu borrador; puedes volver a recargarla."
                  : failure === 412
                    ? "Existe una versión más reciente. Conservamos tu borrador. Recargar versión guardada sustituye lo escrito por esa versión."
                    : failure === 0
                      ? "No podemos confirmar si los cambios se guardaron. Conservamos tu borrador; comprueba la conexión antes de reintentar."
                      : "No se han confirmado los cambios. Conservamos tu borrador para que puedas corregir o reintentar."}
              </p>
              {(failure === 412 || loadFailure) && (
                <button type="button" onClick={reload}>
                  Recargar versión guardada
                </button>
              )}
            </div>
          )}
          {(success || saving || loading) && (
            <p className="edit-status" role="status">
              {loading
                ? "Cargando proyecto"
                : saving
                  ? "Guardando cambios"
                  : "Proyecto actualizado"}
            </p>
          )}
        </section>
      )}
      {!draft && (
        <RouteLink href={detailRoute} className="secondary-link">
          Cancelar
        </RouteLink>
      )}
      <footer className="page-footer">
        <span>Tu proyecto puede cambiar contigo.</span>
        <span>Con calma. Con intención.</span>
      </footer>
    </main>
  );
}
