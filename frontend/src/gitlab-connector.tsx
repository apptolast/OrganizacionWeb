import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { RouteLink } from "./navigation";
import { readProjects, type ProjectSummary } from "./read-projects-api";
import { readConnectorCatalog } from "./connectors-catalog-client";
import {
  GitlabConnectorError,
  connectGitlab,
  disconnectGitlab,
  readGitlabConnection,
  startGitlabImport,
  type GitlabConnection,
  type GitlabImportReceipt,
} from "./gitlab-connector-client";

/**
 * Pantalla del conector de GitLab. La sesión es la frontera: al cambiar de propietario se remonta
 * entera, de modo que ni el token escrito ni la ruta de un proyecto ajeno sobreviven al cambio.
 */
export function GitlabConnector({ owner }: { owner: string }) {
  return <GitlabConnectorScreen key={owner} />;
}

const MESSAGES: Record<string, string> = {
  CONNECTION_INVALID: "La conexión ya no es válida",
  CONNECTION_NOT_FOUND: "La conexión ya no existe",
  GITLAB_UNAVAILABLE: "GitLab no responde. Inténtalo más tarde",
  IMPORT_IN_PROGRESS: "Hay una importación en curso",
  PROJECT_COMPLETED: "El proyecto está terminado",
  RESOURCE_NOT_FOUND: "El proyecto ya no está disponible",
  STORAGE_UNAVAILABLE: "No se pudo completar. Inténtalo más tarde",
  VALIDATION_ERROR: "Revisa la ruta del proyecto y el token",
};

/**
 * Los fallos que no dicen nada del token pero sí dejan la vista desfasada: la acción honesta es
 * releer, nunca reintentar sola.
 */
const NEEDS_REFRESH = new Set(["IMPORT_IN_PROGRESS", "CONNECTOR_ERROR"]);

const STATUS_TEXT: Record<GitlabConnection["status"], string> = {
  connected: "Conectado",
  error: "Error",
  not_connected: "No conectado",
};

export function describeFailure(error: GitlabConnectorError): string {
  if (error.code === "RATE_LIMITED")
    return `GitLab limita las peticiones. Reintenta en ${error.retryAfterSeconds ?? 60} segundos`;
  return MESSAGES[error.code] ?? "No se pudo completar. Inténtalo más tarde";
}

/** Cuatro asteriscos y los cuatro caracteres que el servidor sí publica: el token no cabe aquí. */
export function maskHint(hint: string): string {
  return `••••${hint}`;
}

function GitlabConnectorScreen() {
  const [connection, setConnection] = useState<GitlabConnection | null>(null);
  const [disabled, setDisabled] = useState(false);
  const [loading, setLoading] = useState(true);
  const [projectPath, setProjectPath] = useState("");
  const [replacing, setReplacing] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [connectError, setConnectError] = useState<GitlabConnectorError | null>(
    null,
  );
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [selected, setSelected] = useState("");
  const [importing, setImporting] = useState(false);
  const [receipt, setReceipt] = useState<GitlabImportReceipt | null>(null);
  const [actionError, setActionError] = useState<GitlabConnectorError | null>(
    null,
  );
  const [confirming, setConfirming] = useState(false);

  const heading = useRef<HTMLHeadingElement>(null);
  const tokenField = useRef<HTMLInputElement>(null);
  const replaceButton = useRef<HTMLButtonElement>(null);
  const receiptBox = useRef<HTMLElement>(null);
  const focusReplace = useRef(false);
  const focusHeading = useRef(false);
  const focusReceipt = useRef(false);
  const pending = useRef<AbortController | null>(null);
  const mounted = useRef(true);

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
    mounted.current && !controller.signal.aborted;

  const loadConnection = useCallback(async () => {
    const controller = new AbortController();
    pending.current = controller;
    try {
      const view = await readGitlabConnection(controller.signal);
      if (!live(controller)) return;
      setConnection(view);
      setDisabled(false);
    } catch (error) {
      if (!live(controller)) return;
      if (
        error instanceof GitlabConnectorError &&
        error.code === "CONNECTORS_DISABLED"
      )
        setDisabled(true);
      setConnection(null);
    } finally {
      if (live(controller)) {
        setLoading(false);
        if (pending.current === controller) pending.current = null;
      }
    }
  }, []);

  useEffect(() => {
    void (async () => {
      await loadConnection();
    })();
  }, [loadConnection]);

  /**
   * Cada cambio de estado deja el foco donde el resultado se cuenta: en el encabezado o en el
   * aviso. Perderlo en el body obliga a quien navega con teclado o lector a buscar qué ha pasado.
   */
  useEffect(() => {
    if (focusReplace.current) {
      focusReplace.current = false;
      replaceButton.current?.focus();
      return;
    }
    if (focusReceipt.current) {
      focusReceipt.current = false;
      receiptBox.current?.focus();
      return;
    }
    if (focusHeading.current) {
      focusHeading.current = false;
      heading.current?.focus();
    }
  });

  useEffect(() => {
    const controller = new AbortController();
    void (async () => {
      try {
        const page = await readProjects("/proyectos", controller.signal);
        if (controller.signal.aborted || !mounted.current || !("items" in page))
          return;
        // Un proyecto terminado no admite tareas nuevas: ofrecerlo sería ofrecer un 409.
        const open = page.items.filter((item) => item.status !== "completed");
        setProjects(open);
        setSelected((current) => current || (open[0]?.id ?? ""));
      } catch {
        // El selector sin proyectos ya cuenta por sí mismo que no hay dónde importar.
      }
    })();
    return () => controller.abort();
  }, []);

  async function submitConnection(event: React.FormEvent) {
    event.preventDefault();
    if (connecting) return;
    setConnecting(true);
    setConnectError(null);
    const controller = new AbortController();
    pending.current = controller;
    try {
      const view = await connectGitlab(
        { token: tokenField.current?.value ?? "", projectPath },
        controller.signal,
      );
      if (!live(controller)) return;
      setConnection(view);
      // Al ocultarse el formulario, el nodo que tenía el token desaparece con él.
      setReplacing(false);
      focusHeading.current = true;
    } catch (error) {
      if (!live(controller)) return;
      setConnectError(
        error instanceof GitlabConnectorError
          ? error
          : new GitlabConnectorError({}),
      );
    } finally {
      if (live(controller)) {
        setConnecting(false);
        if (pending.current === controller) pending.current = null;
      }
    }
  }

  async function startImport() {
    if (importing || !selected) return;
    setImporting(true);
    setActionError(null);
    setReceipt(null);
    const controller = new AbortController();
    pending.current = controller;
    try {
      const started = await startGitlabImport(selected, controller.signal);
      if (!live(controller)) return;
      setReceipt(started);
      focusReceipt.current = true;
    } catch (error) {
      if (!live(controller)) return;
      const failure =
        error instanceof GitlabConnectorError
          ? error
          : new GitlabConnectorError({});
      setActionError(failure);
      if (failure.code === "CONNECTION_INVALID") {
        // El 409 es el servidor confirmando que el token dejó de valer: no es optimismo.
        setConnection((current) =>
          current
            ? {
                ...current,
                status: "error",
                lastError: {
                  code: failure.code,
                  at: new Date().toISOString(),
                },
              }
            : current,
        );
        focusReplace.current = true;
      }
    } finally {
      if (live(controller)) {
        setImporting(false);
        if (pending.current === controller) pending.current = null;
      }
    }
  }

  async function confirmDisconnect() {
    setConfirming(false);
    setActionError(null);
    const controller = new AbortController();
    pending.current = controller;
    try {
      await disconnectGitlab(controller.signal);
      if (!live(controller)) return;
      // Sólo tras el 204 se vuelve al estado sin conexión, nunca antes.
      setConnection(null);
      setReceipt(null);
      setProjectPath("");
      setReplacing(false);
      focusHeading.current = true;
    } catch (error) {
      if (!live(controller)) return;
      setActionError(
        error instanceof GitlabConnectorError
          ? error
          : new GitlabConnectorError({}),
      );
    } finally {
      if (live(controller)) {
        if (pending.current === controller) pending.current = null;
      }
    }
  }

  /**
   * Relee lo que el servidor sabe: la conexión y el catálogo. No reintenta la acción que falló;
   * un reintento automático sobre una importación en curso es exactamente lo que la agravaría.
   */
  async function refreshStatus() {
    setActionError(null);
    const controller = new AbortController();
    await loadConnection();
    try {
      await readConnectorCatalog(controller.signal);
    } catch {
      // El catálogo es contexto: que no se pueda leer no cambia esta pantalla.
    }
  }

  function replaceToken() {
    setConnectError(null);
    setProjectPath(connection?.projectPath ?? "");
    setReplacing(true);
  }

  const isConnected = connection?.status === "connected";
  const showPanel = !disabled && !loading && Boolean(connection) && !replacing;
  const showForm = !disabled && !loading && (!isConnected || replacing);

  return (
    <main id="proyectos" className="gitlab-connector" tabIndex={-1}>
      <h1 ref={heading} tabIndex={-1}>
        Conector de GitLab
      </h1>
      <p>
        <RouteLink href="/conectores">Conectores</RouteLink>
      </p>

      {disabled ? (
        <p role="alert">
          Falta configuración del servidor para usar los conectores. Pide a
          quien administra esta instalación que configure la clave de
          conectores.
        </p>
      ) : null}

      {showPanel && connection && connection.status !== "not_connected" ? (
        <section aria-label="Conexión">
          <dl>
            <dt>Estado</dt>
            <dd>{STATUS_TEXT[connection.status]}</dd>
            <dt>Proyecto</dt>
            <dd>{connection.projectPath}</dd>
            <dt>Identificador del proyecto</dt>
            <dd>{connection.projectId}</dd>
            <dt>Token</dt>
            <dd>{maskHint(connection.tokenHint ?? "")}</dd>
          </dl>

          <div>
            <label htmlFor="gitlab-project">Proyecto de destino</label>
            <select
              id="gitlab-project"
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
              Importar issues
            </button>
          </div>

          <button type="button" ref={replaceButton} onClick={replaceToken}>
            Actualizar token
          </button>

          {confirming ? (
            <div role="group" aria-label="Confirmar desconexión">
              <p>
                Se borrará el token guardado. Las tareas ya importadas y sus
                enlaces se conservan.
              </p>
              <button type="button" onClick={() => void confirmDisconnect()}>
                Confirmar desconexión
              </button>
              <button type="button" onClick={() => setConfirming(false)}>
                Cancelar
              </button>
            </div>
          ) : (
            <button type="button" onClick={() => setConfirming(true)}>
              Desconectar
            </button>
          )}
        </section>
      ) : null}

      {showForm ? (
        <form onSubmit={(event) => void submitConnection(event)} noValidate>
          <div>
            <label htmlFor="gitlab-path">Ruta del proyecto</label>
            <input
              id="gitlab-path"
              name="projectPath"
              autoComplete="off"
              value={projectPath}
              readOnly={connecting}
              aria-invalid={connectError?.fields.projectPath ? true : undefined}
              aria-describedby="gitlab-path-help"
              onChange={(event) => setProjectPath(event.target.value)}
            />
            <p id="gitlab-path-help">
              Con la forma grupo/proyecto, como aparece en la dirección del
              proyecto.
            </p>
          </div>
          <div>
            <label htmlFor="gitlab-token">Token de acceso personal</label>
            <input
              id="gitlab-token"
              name="token"
              type="password"
              autoComplete="off"
              ref={tokenField}
              defaultValue=""
              readOnly={connecting}
              aria-invalid={connectError?.fields.token ? true : undefined}
              aria-describedby="gitlab-token-help"
            />
            <p id="gitlab-token-help">
              Usa un token personal con alcance read_api y nada más.
            </p>
          </div>
          {connectError ? (
            <p role="alert">{describeFailure(connectError)}</p>
          ) : null}
          <button type="submit" disabled={connecting}>
            Conectar
          </button>
        </form>
      ) : null}

      {actionError ? (
        <div>
          <p role="alert">{describeFailure(actionError)}</p>
          {NEEDS_REFRESH.has(actionError.code) ? (
            <button type="button" onClick={() => void refreshStatus()}>
              Actualizar estado
            </button>
          ) : null}
        </div>
      ) : null}

      {receipt ? <Receipt receipt={receipt} boxRef={receiptBox} /> : null}

      <p role="status" aria-live="polite" aria-atomic="true">
        {connecting
          ? "Guardando…"
          : importing
            ? "Importando issues. Esto puede tardar un poco…"
            : ""}
      </p>
    </main>
  );
}

/** Cuatro cifras etiquetadas, sin porcentajes ni barras: lo que el recibo dice y nada más. */
function Receipt({
  receipt,
  boxRef,
}: {
  receipt: GitlabImportReceipt;
  boxRef: React.RefObject<HTMLElement | null>;
}) {
  return (
    <section
      aria-label="Resultado de la importación"
      ref={boxRef}
      tabIndex={-1}
    >
      <dl>
        <dt>Creadas</dt>
        <dd>{receipt.created}</dd>
        <dt>Omitidas</dt>
        <dd>{receipt.skipped}</dd>
        <dt>Fallidas</dt>
        <dd>{receipt.failed}</dd>
        <dt>Truncado</dt>
        <dd>{receipt.truncated ? "Sí" : "No"}</dd>
      </dl>
      {receipt.truncated ? (
        <p>
          El proyecto tiene más issues abiertas de las que caben en una
          importación: quedaron issues sin traer. Vuelve a importar para
          continuar.
        </p>
      ) : null}
    </section>
  );
}
