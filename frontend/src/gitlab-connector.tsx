import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { RouteLink } from "./navigation";
import {
  GitlabConnectorError,
  connectGitlab,
  readGitlabConnection,
  type GitlabConnection,
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
  const [token, setToken] = useState("");
  const [projectPath, setProjectPath] = useState("");
  const [connecting, setConnecting] = useState(false);
  const [connectError, setConnectError] = useState<GitlabConnectorError | null>(
    null,
  );

  const heading = useRef<HTMLHeadingElement>(null);
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
    void loadConnection();
  }, [loadConnection]);

  async function submitConnection(event: React.FormEvent) {
    event.preventDefault();
    if (connecting) return;
    setConnecting(true);
    setConnectError(null);
    const controller = new AbortController();
    pending.current = controller;
    try {
      const view = await connectGitlab(
        { token, projectPath },
        controller.signal,
      );
      if (!live(controller)) return;
      setConnection(view);
      // El token deja de existir en la página en cuanto el servidor confirma que lo guardó.
      setToken("");
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

  const isConnected = connection?.status === "connected";
  const showForm = !disabled && !loading && !isConnected;

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

      {isConnected && connection ? (
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
              value={token}
              readOnly={connecting}
              aria-invalid={connectError?.fields.token ? true : undefined}
              aria-describedby="gitlab-token-help"
              onChange={(event) => setToken(event.target.value)}
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

      <p role="status" aria-live="polite" aria-atomic="true">
        {connecting ? "Guardando…" : ""}
      </p>
    </main>
  );
}
