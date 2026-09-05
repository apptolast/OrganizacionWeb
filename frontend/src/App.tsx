import { useCreateProject } from "./use-create-project";
import { ProjectReader } from "./project-reader";
import { useRoute, isProjectRoute } from "./navigation";
import { Workspace } from "./workspace";
import { ProjectEditor } from "./project-editor";
export function App() {
  const route = useRoute();
  return (
    <Workspace>
      {route.endsWith("/editar") ? (
        <ProjectEditor key={route} route={route} />
      ) : isProjectRoute(route) ? (
        <ProjectReader key={route} route={route} />
      ) : (
        <CreateProjectScreen />
      )}
    </Workspace>
  );
}
function CreateProjectScreen() {
  const {
    name,
    setName,
    description,
    setDescription,
    saving,
    project,
    failure,
    formRef,
    nameError,
    descriptionError,
    submit,
  } = useCreateProject();
  return (
    <main id="proyectos" tabIndex={-1}>
      <div className="page-intro">
        <p className="eyebrow">MENOS RUIDO. MÁS INTENCIÓN.</p>
        <h1>
          Dale espacio a tu
          <br className="desktop-break" /> próxima idea.
        </h1>
        <p>
          No necesitas tenerlo todo resuelto. Empieza por darle un nombre
          <br className="desktop-break" /> a eso que quieres construir.
        </p>
      </div>
      <div className="capture-grid">
        <section className="form-card" aria-labelledby="create-heading">
          <div className="card-heading">
            <span className="section-icon" aria-hidden="true">
              ＋
            </span>
            <div>
              <h2 id="create-heading">Un nuevo comienzo</h2>
              <p>Guarda tu proyecto como una idea.</p>
            </div>
          </div>
          <form ref={formRef} onSubmit={submit} noValidate aria-busy={saving}>
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
                aria-invalid={Boolean(nameError)}
                aria-describedby="name-help name-error"
                readOnly={saving}
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Por ejemplo, mi próxima web…"
              />
              <p className="field-hint" id="name-help">
                Hasta 120 caracteres. Un nombre que tenga sentido para ti.
              </p>
              <p className="field-error" id="name-error">
                {nameError}
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
                aria-invalid={Boolean(descriptionError)}
                aria-describedby="description-help description-error"
                readOnly={saving}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="¿Qué te gustaría conseguir con este proyecto?…"
                rows={5}
              />
              <p className="field-hint" id="description-help">
                Una intención, algo de contexto o ese primer paso. Hasta 4000
                caracteres.
              </p>
              <p className="field-error" id="description-error">
                {descriptionError}
              </p>
            </div>
            {failure ? (
              <p className="failure" role="alert">
                {failure}
              </p>
            ) : null}
            <div className="form-footer">
              <span className="save-note">
                <span aria-hidden="true">◇</span> Sin fechas ni compromisos
                todavía
              </span>
              <button type="submit" disabled={saving}>
                Crear proyecto <span aria-hidden="true">↗</span>
              </button>
            </div>
          </form>
          <p
            className="save-status"
            role="status"
            aria-live="polite"
            aria-atomic="true"
          >
            {saving ? "Guardando…" : project ? "Proyecto guardado." : ""}
          </p>
        </section>
        <aside className="idea-column" aria-label="Resultado de creación">
          {project ? (
            <section className="result-card">
              <span className="idea-badge">Idea guardada</span>
              <h2>{project.name}</h2>
              <p className="project-description">{project.description}</p>
              <dl>
                <dt>Identificador del proyecto</dt>
                <dd>{project.id}</dd>
                <dt>Creado</dt>
                <dd>
                  {new Intl.DateTimeFormat("es", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  }).format(new Date(project.createdAt))}
                </dd>
              </dl>
            </section>
          ) : (
            <section className="empty-card">
              <div className="seed-art" aria-hidden="true">
                <span className="seed-ring" />
                <span className="seed-stem" />
                <span className="seed-leaf left" />
                <span className="seed-leaf right" />
                <span className="seed-soil" />
              </div>
              <span className="idea-badge">Primero, una idea</span>
              <h2>Tu idea empieza aquí</h2>
              <p>
                Una vez guardada, verás aquí
                <br className="desktop-break" /> la confirmación de tu proyecto.
              </p>
              <div className="empty-divider" />
              <p className="quiet-note">
                Capturar una idea no te obliga
                <br className="desktop-break" /> a empezarla hoy.
              </p>
            </section>
          )}
          <div className="gentle-note">
            <span aria-hidden="true">✦</span>
            <p>
              Deja espacio para pensar.
              <br />
              Lo importante es poder volver.
            </p>
          </div>
        </aside>
      </div>
      <footer className="page-footer">
        <span>Un lugar para lo que quieres construir.</span>
        <span>Con calma. Con intención.</span>
      </footer>
    </main>
  );
}
