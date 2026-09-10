// @ts-nocheck
import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { RouteLink } from "./navigation";
import { readProjects, type ProjectSummary } from "./read-projects-api";
import {
  ConnectorError,
  connectGithub,
  disconnectGithub,
  readGithubConnection,
  startGithubImport,
  type GithubConnection,
  type GithubImportReceipt,
} from "./github-connector-client";

/**
 * Pantalla del conector de GitHub. La sesión es la frontera: al cambiar de propietario se remonta
 * entera, de modo que ni el repositorio escrito ni el resumen de una importación ajena sobreviven.
 */
export function GithubConnector({ owner }: { owner: string }) {
  return <GithubConnectorScreen key={owner} />;
}

/** Lo que el resumen necesita saber, venga de un recibo completo o de un error con contadores. */
type Summary = {
  projectId: string | null;
  created: number;
  skipped: number;
  failed: number;
  truncated: boolean;
};

const MESSAGES: Record<string, string> = {
  CONNECTION_INVALID: "La conexión ya no es válida",
  IMPORT_IN_PROGRESS: "Hay una importación en curso",
  PROJECT_COMPLETED: "El proyecto está terminado",
  GITHUB_UNAVAILABLE: "GitHub no responde. Inténtalo más tarde",
  GITHUB_TOKEN_REJECTED: "GitHub rechazó el token",
  GITHUB_REPOSITORY_UNAVAILABLE:
    "El repositorio no está disponible con ese token",
  CONNECTION_NOT_FOUND: "La conexión ya no existe",
  RESOURCE_NOT_FOUND: "El proyecto ya no está disponible",
  VALIDATION_ERROR: "Revisa el repositorio y el token",
};

function importMessage(error: ConnectorError): string {
  if (error.code === "RATE_LIMITED")
    return `GitHub limita las peticiones. Reintenta en ${error.retryAfterSeconds ?? 60} segundos`;
  if (error.code === "STORAGE_UNAVAILABLE") return "No se pudo completar";
  return MESSAGES[error.code] ?? "No se pudo importar. Inténtalo más tarde";
}

function GithubConnectorScreen() {
  const [connection, setConnection] = useState<GithubConnection | null>(null);
  const [disabled, setDisabled] = useState(false);
  const [loading, setLoading] = useState(true);
  const [repository, setRepository] = useState("");
  const [token, setToken] = useState("");
  const [reconnecting, setReconnecting] = useState(false);
  const [connectError, setConnectError] = useState<ConnectorError | null>(null);
  const [connecting, setConnecting] = useState(false);
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [selected, setSelected] = useState("");
  const [importing, setImporting] = useState(false);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [importError, setImportError] = useState<ConnectorError | null>(null);
  const [confirming, setConfirming] = useState(false);

  const heading = useRef<HTMLHeadingElement>(null);
  const tokenField = useRef<HTMLInputElement>(null);
  const disconnectButton = useRef<HTMLButtonElement>(null);
  const pending = useRef<AbortController | null>(null);
  const mounted = useRef(true);
  const focusHeading = useRef(false);
  const focusToken = useRef(false);
  const focusDisconnect = useRef(false);

  useLayoutEffect(() => {
    mounted.current = true;
    heading.current?.focus();
    return () => {
      mounted.current = false;
      pending.current?.abort();
      pending.current = null;
    };
  }, []);

  const live = (controller: AbortController) =>
    mounted.current &&
    !controller.signal.aborted &&
    pending.current === controller;

  const loadConnection = useCallback(async () => {
    pending.current?.abort();
    const controller = new AbortController();
    pending.current = controller;
    try {
      const found = await readGithubConnection(controller.signal);
      if (!live(controller)) return;
      setConnection(found);
      setDisabled(false);
      if (found === null) setReconnecting(false);
    } catch (error) {
      if (!live(controller)) return;
      if (
        error instanceof ConnectorError &&
        error.code === "CONNECTORS_DISABLED"
      )
        setDisabled(true);
      setConnection(null);
    } finally {
      if (live(controller)) {
        setLoading(false);
        pending.current = null;
      }
    }
  }, []);

  // La lectura inicial va dentro de una función asíncrona: el estado sólo cambia cuando la
  // respuesta llega, nunca de forma síncrona mientras React está pintando.
  useEffect(() => {
    void (async () => {
      await loadConnection();
    })();
  }, [loadConnection]);

  useEffect(() => {
    if (disabled) return;
    const controller = new AbortController();
    void (async () => {
      try {
        const page = await readProjects("/proyectos", controller.signal);
        if (controller.signal.aborted || !mounted.current || !("items" in page))
          return;
        const open = page.items.filter((item) => item.status !== "completed");
        setProjects(open);
        setSelected((current) => current || (open[0]?.id ?? ""));
      } catch {
        // El selector sin proyectos ya cuenta por sí mismo que no hay dónde importar.
      }
    })();
    return () => controller.abort();
  }, [disabled]);

  // Escape cancela la confirmación de desconexión desde cualquier sitio, también cuando el foco
  // se ha quedado en el cuerpo al reemplazarse el botón que la abrió. Sólo escucha mientras la
  // confirmación está abierta, así que no roba la tecla al resto de la pantalla.
  useEffect(() => {
    if (!confirming) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      event.preventDefault();
      cancelDisconnect();
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [confirming]);

  // El foco se mueve después de pintar, nunca durante el manejador del evento.
  useLayoutEffect(() => {
    if (focusHeading.current) {
      focusHeading.current = false;
      heading.current?.focus();
    }
    if (focusToken.current && tokenField.current) {
      focusToken.current = false;
      tokenField.current.focus();
    }
    if (focusDisconnect.current && disconnectButton.current) {
      focusDisconnect.current = false;
      disconnectButton.current.focus();
    }
  });

  async function submitConnection(event: React.FormEvent) {
    event.preventDefault();
    if (connecting) return;
    const controller = new AbortController();
    pending.current = controller;
    setConnecting(true);
    setConnectError(null);
    try {
      const saved = await connectGithub(
        { repository, token },
        controller.signal,
      );
      if (!live(controller)) return;
      setConnection(saved);
      setReconnecting(false);
      setRepository("");
      setSummary(null);
      setImportError(null);
    } catch (error) {
      if (!live(controller)) return;
      setConnectError(
        error instanceof ConnectorError ? error : new ConnectorError({}),
      );
      focusToken.current = true;
    } finally {
      if (mounted.current) {
        // El token en claro no sobrevive al envío, salga bien o mal.
        setToken("");
        setConnecting(false);
        if (pending.current === controller) pending.current = null;
      }
    }
  }

  async function startImport() {
    if (importing || !selected) return;
    const controller = new AbortController();
    pending.current = controller;
    setImporting(true);
    setImportError(null);
    setSummary(null);
    try {
      const receipt = await startGithubImport(selected, controller.signal);
      if (!live(controller)) return;
      setSummary(fromReceipt(receipt));
      focusHeading.current = true;
    } catch (error) {
      if (!live(controller)) return;
      const failure =
        error instanceof ConnectorError ? error : new ConnectorError({});
      setImportError(failure);
      if (failure.code === "STORAGE_UNAVAILABLE")
        setSummary({
          projectId: selected,
          created: failure.created ?? 0,
          skipped: failure.skipped ?? 0,
          failed: failure.failed ?? 0,
          truncated: false,
        });
    } finally {
      if (mounted.current) {
        setImporting(false);
        if (pending.current === controller) pending.current = null;
      }
    }
  }

  /** Cierra la confirmación sin borrar nada y devuelve el foco a donde estaba. */
  function cancelDisconnect() {
    setConfirming(false);
    focusDisconnect.current = true;
  }

  async function confirmDisconnect() {
    setConfirming(false);
    const controller = new AbortController();
    pending.current = controller;
    try {
      await disconnectGithub(controller.signal);
      if (!live(controller)) return;
      setConnection(null);
      setSummary(null);
      setImportError(null);
      setRepository("");
      setToken("");
      focusHeading.current = true;
    } catch (error) {
      if (live(controller))
        setImportError(
          error instanceof ConnectorError ? error : new ConnectorError({}),
        );
    } finally {
      if (mounted.current && pending.current === controller)
        pending.current = null;
    }
  }

  const invalid = connection?.status === "invalid";
  const running = connection?.lastImport?.status === "running";
  const showForm =
    !disabled && !loading && (connection === null || reconnecting);
  const shownSummary = summary ?? fromLastImport(connection);
  const canImport = Boolean(connection) && !invalid && !running;

  return (
    <main id="proyectos" className="github-connector" tabIndex={-1}>
      <h1 ref={heading} tabIndex={-1}>
        Conector de GitHub
      </h1>
      <p>
        <RouteLink href="/integraciones">Integraciones</RouteLink>
      </p>

      {disabled ? (
        <p role="alert">
          Falta configuración del servidor para usar los conectores. Pide a
          quien administra esta instalación que configure la clave de
          conectores.
        </p>
      ) : null}

      {!disabled && connection && !reconnecting ? (
        <section aria-label="Conexión">
          <dl>
            <dt>Repositorio</dt>
            <dd>{connection.repository}</dd>
            <dt>Cuenta</dt>
            <dd>{connection.login}</dd>
            <dt>Estado</dt>
            <dd>{invalid ? "Conexión inválida" : "Conectada"}</dd>
          </dl>
          {invalid ? (
            <button type="button" onClick={() => setReconnecting(true)}>
              Reconectar
            </button>
          ) : null}
          {running ? (
            <>
              <p>Hay una importación en curso</p>
              <button type="button" onClick={() => void loadConnection()}>
                Consultar estado
              </button>
            </>
          ) : null}
          {canImport ? (
            <div>
              <label htmlFor="github-project">Proyecto de destino</label>
              <select
                id="github-project"
                value={selected}
                disabled={importing}
                onChange={(event) => setSelected(event.target.value)}
              >
                {projects.map((project) => (
                  <option key={project.id} value={project.id}>
                    {project.name}
                  </option>
                ))}
              </select>
              <button
                type="button"
                disabled={importing}
                onClick={() => void startImport()}
              >
                Importar issues abiertas
              </button>
            </div>
          ) : null}
          {confirming ? (
            <div role="group" aria-label="Confirmar desconexión">
              <p>
                Se borrará el token. Las tareas ya importadas y sus enlaces se
                conservan.
              </p>
              <button type="button" onClick={() => void confirmDisconnect()}>
                Confirmar desconexión
              </button>
              <button type="button" onClick={cancelDisconnect}>
                Cancelar
              </button>
            </div>
          ) : (
            <button
              type="button"
              ref={disconnectButton}
              onClick={() => setConfirming(true)}
            >
              Desconectar
            </button>
          )}
        </section>
      ) : null}

      {showForm ? (
        <form onSubmit={(event) => void submitConnection(event)} noValidate>
          <div>
            <label htmlFor="github-repository">Repositorio</label>
            <input
              id="github-repository"
              name="repository"
              autoComplete="off"
              value={repository}
              readOnly={connecting}
              onChange={(event) => setRepository(event.target.value)}
              placeholder="propietario/nombre"
            />
          </div>
          <div>
            <label htmlFor="github-token">Token de acceso personal</label>
            <input
              id="github-token"
              name="token"
              type="password"
              autoComplete="off"
              ref={tokenField}
              value={token}
              readOnly={connecting}
              aria-invalid={Boolean(connectError)}
              aria-describedby="github-token-help github-token-error"
              onChange={(event) => setToken(event.target.value)}
            />
            <p id="github-token-help">
              Necesita únicamente permiso de lectura de issues del repositorio.
            </p>
            <p
              id="github-token-error"
              role={connectError ? "alert" : undefined}
            >
              {connectError ? importMessage(connectError) : ""}
            </p>
          </div>
          <button type="submit" disabled={connecting}>
            Conectar
          </button>
        </form>
      ) : null}

      {importError && importError.code !== "GITHUB_TOKEN_REJECTED" ? (
        <div>
          <p>{importMessage(importError)}</p>
          {importError.code === "CONNECTION_INVALID" && !reconnecting ? (
            <button type="button" onClick={() => setReconnecting(true)}>
              Reconectar
            </button>
          ) : null}
          {importError.code === "IMPORT_IN_PROGRESS" ? (
            <button type="button" onClick={() => void loadConnection()}>
              Consultar estado
            </button>
          ) : null}
        </div>
      ) : null}

      {shownSummary ? (
        <section aria-label="Resultado de la importación">
          <p>Creadas {shownSummary.created}</p>
          <p>Omitidas {shownSummary.skipped}</p>
          <p>Fallidas {shownSummary.failed}</p>
          {shownSummary.truncated ? (
            <p>
              El repositorio tiene más issues que las 200 importadas. Vuelve a
              importar para traer las siguientes.
            </p>
          ) : null}
          {shownSummary.projectId ? (
            <RouteLink href={`/proyectos/${shownSummary.projectId}`}>
              Ver el proyecto {nameOf(projects, shownSummary.projectId)}
            </RouteLink>
          ) : null}
        </section>
      ) : null}

      <p role="status" aria-live="polite" aria-atomic="true">
        {importing
          ? "Importando issues…"
          : summary
            ? `Importación terminada. Creadas ${summary.created}, omitidas ${summary.skipped}, fallidas ${summary.failed}.`
            : ""}
      </p>
    </main>
  );
}

function fromReceipt(receipt: GithubImportReceipt): Summary {
  return {
    projectId: receipt.projectId,
    created: receipt.created,
    skipped: receipt.skipped,
    failed: receipt.failed,
    truncated: receipt.truncated,
  };
}

function fromLastImport(connection: GithubConnection | null): Summary | null {
  const last = connection?.lastImport;
  return last && last.status !== "running" ? fromReceipt(last) : null;
}

function nameOf(projects: ProjectSummary[], id: string) {
  return projects.find((project) => project.id === id)?.name ?? id;
}
