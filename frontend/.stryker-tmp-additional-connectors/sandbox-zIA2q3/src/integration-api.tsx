// @ts-nocheck
import {
  useState,
  useRef,
  useLayoutEffect,
  useEffect,
  useCallback,
} from "react";
import {
  apiScopes,
  createApiCredential,
  readApiCredential,
  listApiCredentials,
  revokeApiCredential,
  type ApiCredential,
} from "./integration-api-client";
import {
  saveApiCredentialIntent,
  clearApiCredentialIntent,
  readApiCredentialIntent,
} from "./integration-api-intent";
const scopeLabels = [
  "Leer proyectos",
  "Crear y editar proyectos",
  "Leer tareas",
  "Crear tareas",
  "Leer agenda",
  "Leer historial",
];
const credentialDate = new Intl.DateTimeFormat("es-ES", {
  dateStyle: "medium",
  timeStyle: "long",
  timeZone: "UTC",
});
export function IntegrationApi({ owner }: { owner: string }) {
  return <CredentialPanel key={owner} owner={owner} />;
}
function CredentialPanel({ owner }: { owner: string }) {
  const heading = useRef<HTMLHeadingElement>(null);
  const initiator = useRef<HTMLButtonElement | null>(null);
  useEffect(() => {
    function moved(event: FocusEvent) {
      if (event.target !== initiator.current) initiator.current = null;
    }
    document.addEventListener("focusin", moved);
    return () => document.removeEventListener("focusin", moved);
  }, []);
  useLayoutEffect(() => {
    heading.current?.focus();
  }, []);
  const [recovery] = useState(() => {
    try {
      return { intent: readApiCredentialIntent(owner), failed: false };
    } catch {
      return { intent: null, failed: true };
    }
  });
  const [intent, setIntent] = useState(recovery.intent);
  const [name, setName] = useState("");
  const [scopes, setScopes] = useState<string[]>([]);
  const [days, setDays] = useState(30);
  const [secret, setSecret] = useState<string | null>(null);
  const secretInput = useRef<HTMLInputElement>(null);
  const [copyMessage, setCopyMessage] = useState<string>();
  const [failure, setFailure] = useState<string>();
  const [revoking, setRevoking] = useState<ApiCredential>();
  const [uncertainRevocations, setUncertainRevocations] = useState<Set<string>>(
    () => new Set(),
  );
  const revokeUncertain = !!revoking && uncertainRevocations.has(revoking.id);
  const [items, setItems] = useState<ApiCredential[]>([]);
  const [listFailure, setListFailure] = useState(false);
  const [listBusy, setListBusy] = useState(true);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const listRequest = useRef<AbortController | null>(null);
  const [observedAt, setObservedAt] = useState(Date.now);
  function showConfirmed(credential: ApiCredential) {
    listRequest.current?.abort();
    setObservedAt(Date.now());
    setListBusy(false);
    setItems((current) => [
      credential,
      ...current.filter((item) => item.id !== credential.id),
    ]);
  }

  const loadList = useCallback((cursor?: string) => {
    listRequest.current?.abort();
    const controller = new AbortController();
    listRequest.current = controller;

    void listApiCredentials(controller.signal, cursor)
      .then((page) => {
        if (controller.signal.aborted) return;
        setItems((current) =>
          cursor
            ? [
                ...current,
                ...page.items.filter(
                  (item) =>
                    !current.some((existing) => existing.id === item.id),
                ),
              ]
            : page.items,
        );
        setNextCursor(page.nextCursor);
        setObservedAt(Date.now());
      })
      .catch(() => {
        if (!controller.signal.aborted) setListFailure(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setListBusy(false);
      });
  }, []);
  function reloadList(cursor?: string) {
    setListBusy(true);
    setListFailure(false);
    void loadList(cursor);
  }
  useEffect(() => {
    void loadList();
    return () => listRequest.current?.abort();
  }, [loadList]);
  const [lostSecret, setLostSecret] = useState(false);
  const [cleanupFailed, setCleanupFailed] = useState(false);
  const [missing, setMissing] = useState(false);
  const [correctionAllowed, setCorrectionAllowed] = useState(false);
  const [busy, setBusy] = useState(false);
  useLayoutEffect(() => {
    if (busy) return;
    const control = initiator.current;
    initiator.current = null;
    if (control && document.activeElement === document.body)
      heading.current?.focus();
  });
  const [uncertain, setUncertain] = useState(!!intent);
  const pending = useRef(false);
  const request = useRef<AbortController | null>(null);
  const creationRequest = useRef<AbortController | null>(null);
  useLayoutEffect(
    () => () => {
      request.current?.abort();
      listRequest.current?.abort();
    },
    [],
  );
  const canonicalName = name.replace(
    /^\p{White_Space}+|\p{White_Space}+$/gu,
    "",
  );
  const validName =
    canonicalName.length > 0 &&
    [...canonicalName].length <= 80 &&
    !/[\p{Cc}\p{Cs}]/u.test(canonicalName);
  const valid = validName && scopes.length > 0;
  function accept(credential: ApiCredential, newSecret: string | null) {
    showConfirmed(credential);
    setSecret(newSecret);
    setLostSecret(newSecret === null);
    setUncertain(false);
    setIntent(null);
    setFailure(undefined);
    setMissing(false);
    setCorrectionAllowed(false);
    try {
      clearApiCredentialIntent();
      setCleanupFailed(false);
    } catch {
      setCleanupFailed(true);
    }
  }
  async function create(existingId?: string) {
    if (
      recovery.failed ||
      !valid ||
      pending.current ||
      (uncertain && existingId === undefined)
    )
      return;
    const id = existingId ?? crypto.randomUUID();
    try {
      saveApiCredentialIntent({ owner, id });
      setIntent({ owner, id });
    } catch {
      setFailure(
        "No se puede conservar la recuperación. No se enviará la creación.",
      );
      return;
    }
    pending.current = true;
    setBusy(true);
    setFailure(undefined);
    setCorrectionAllowed(false);
    const controller = new AbortController();
    request.current = controller;
    creationRequest.current = controller;
    try {
      const result = await createApiCredential(
        id,
        { name, scopes, expiresInDays: days },
        controller.signal,
      );
      if (controller.signal.aborted) return;
      accept(result.credential, result.secret);
    } catch (error) {
      if (controller.signal.aborted) return;
      if (error instanceof Response) {
        const problem = await error
          .clone()
          .json()
          .catch(() => null);
        if (controller.signal.aborted) return;
        if (
          error.status === 409 &&
          problem?.status === 409 &&
          problem?.code === "API_CREDENTIAL_CONFLICT" &&
          problem?.type === "urn:organization:problem:api_credential_conflict"
        ) {
          setUncertain(true);
          setMissing(false);
          setFailure(
            "Esta identidad ya corresponde a otra intención. Comprueba la credencial antes de continuar.",
          );
          return;
        }
        if (
          error.status === 400 &&
          problem?.status === 400 &&
          problem?.code === "API_CREDENTIAL_INVALID" &&
          problem?.type === "urn:organization:problem:api_credential_invalid"
        ) {
          setUncertain(true);
          setCorrectionAllowed(true);
          setMissing(false);
          setFailure(
            "Revisa el nombre, los permisos y la caducidad. La creación fue rechazada.",
          );
          return;
        }
        if (
          error.status === 409 &&
          problem?.status === 409 &&
          problem?.code === "API_CREDENTIAL_LIMIT" &&
          problem?.type === "urn:organization:problem:api_credential_limit"
        ) {
          setUncertain(true);
          setCorrectionAllowed(true);
          setMissing(false);
          setFailure(
            "Ya hay diez credenciales válidas. Revoca una o espera a su caducidad antes de reintentar.",
          );
          return;
        }
      }
      setUncertain(true);
      setFailure(
        "No podemos confirmar la creación. Comprueba la misma intención antes de continuar.",
      );
    } finally {
      if (creationRequest.current === controller)
        creationRequest.current = null;
      if (request.current === controller) {
        pending.current = false;
        setBusy(false);
      }
    }
  }
  function closeSecret() {
    setSecret(null);
    setCopyMessage(undefined);
    const creation = creationRequest.current;
    if (creation) {
      creation.abort();
      creationRequest.current = null;
      request.current = null;
      pending.current = false;
      setBusy(false);
      setUncertain(true);
      setMissing(false);
      setCorrectionAllowed(false);
      setFailure(
        "La creación puede haberse guardado. Comprueba la misma intención antes de continuar.",
      );
    }
  }
  async function check() {
    if (!intent || pending.current) return;
    pending.current = true;
    setBusy(true);
    const controller = new AbortController();
    request.current = controller;
    try {
      const credential = await readApiCredential(intent.id, controller.signal);
      if (controller.signal.aborted) return;
      accept(credential, null);
    } catch (error) {
      if (controller.signal.aborted) return;
      const notFound = error instanceof Response && error.status === 404;
      setMissing(notFound);
      setFailure(
        notFound
          ? "Todavía no se encuentra la credencial. El envío anterior puede seguir en curso."
          : "No se puede comprobar la creación. Conserva el mismo intento.",
      );
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }
  async function revoke(checkOnly = false) {
    if (!revoking || pending.current) return;
    if (revokeUncertain && !checkOnly) return;
    const controller = new AbortController();
    request.current = controller;
    pending.current = true;
    setBusy(true);
    try {
      const credential = checkOnly
        ? await readApiCredential(revoking.id, controller.signal)
        : await revokeApiCredential(revoking, controller.signal);
      if (controller.signal.aborted) return;
      setUncertainRevocations((current) => {
        const next = new Set(current);
        next.delete(revoking.id);
        return next;
      });
      setFailure(undefined);
      if (credential.revokedAt !== null) {
        showConfirmed(credential);
        setRevoking(undefined);
      } else {
        setRevoking(credential);
      }
    } catch {
      if (controller.signal.aborted) return;
      setUncertainRevocations((current) => new Set(current).add(revoking.id));
      setFailure(
        "No se puede confirmar la revocación. Comprueba su estado antes de repetirla.",
      );
    } finally {
      pending.current = false;
      setBusy(false);
    }
  }
  const visibleItems = items;
  async function copySecret() {
    if (!secret) return;
    try {
      if (!navigator.clipboard?.writeText) throw new Error();
      await navigator.clipboard.writeText(secret);
      setCopyMessage("Secreto copiado.");
    } catch {
      secretInput.current?.select();
      setCopyMessage(
        "Selecciona y copia el secreto manualmente. No podremos mostrarlo de nuevo.",
      );
    }
  }
  return (
    <main
      id="proyectos"
      className="integration-api"
      onClickCapture={(event) => {
        const button = (event.target as HTMLElement).closest("button");
        if (button === document.activeElement) initiator.current = button;
      }}
    >
      <h1 ref={heading} tabIndex={-1}>
        Credenciales para integraciones
      </h1>
      <p>
        El secreto se mostrará una sola vez. Guarda una copia antes de cerrar.
      </p>
      <p>
        Elige sólo los permisos que necesites. Los permisos de escritura no
        incluyen lectura. Estas credenciales actúan sobre tus propios datos.
      </p>
      {secret && (
        <div className="credential-secret">
          <p>Guárdalo ahora. No podremos mostrarlo de nuevo.</p>
          <label>
            Secreto de la credencial
            <input ref={secretInput} readOnly value={secret} />
          </label>
          <button onClick={() => void copySecret()}>Copiar</button>
          <button onClick={closeSecret}>Cerrar secreto</button>
          {copyMessage && <p role="status">{copyMessage}</p>}
        </div>
      )}
      <label>
        Nombre
        <input
          value={name}
          aria-invalid={name && !validName ? true : undefined}
          aria-describedby={
            name && !validName ? "credential-name-error" : undefined
          }
          onChange={(event) => setName(event.target.value)}
        />
      </label>
      {name && !validName && (
        <p role="alert" id="credential-name-error">
          El nombre debe tener entre 1 y 80 caracteres y no incluir controles.
        </p>
      )}
      <fieldset>
        <legend>Permisos</legend>
        {apiScopes.map((scope, index) => (
          <label key={scope}>
            <input
              type="checkbox"
              checked={scopes.includes(scope)}
              onChange={(event) =>
                setScopes((current) =>
                  event.target.checked
                    ? [...current, scope]
                    : current.filter((value) => value !== scope),
                )
              }
            />
            {scopeLabels[index]}
          </label>
        ))}
      </fieldset>
      <label>
        Caducidad
        <select
          value={days}
          onChange={(event) => setDays(Number(event.target.value))}
        >
          {[7, 30, 90].map((value) => (
            <option key={value} value={value}>
              {value} días
            </option>
          ))}
        </select>
      </label>
      <button
        disabled={recovery.failed || busy || uncertain || !valid}
        onClick={() => void create()}
      >
        Crear
      </button>
      {recovery.failed && (
        <p role="alert">
          No se puede leer la recuperación local. No se crearán credenciales
          hasta recuperarla.
        </p>
      )}
      {failure && <p role="alert">{failure}</p>}
      {busy && <p role="status">Consultando o guardando la credencial…</p>}
      {uncertain && (
        <p>
          Conservamos la misma identidad del intento. El reenvío compara nombre,
          permisos y caducidad: si cambias esos datos y el primer envío ya se
          guardó, habrá un conflicto que deberás comprobar.
        </p>
      )}
      {cleanupFailed && (
        <p role="alert">
          La operación está confirmada, pero no se pudo limpiar la recuperación
          local.
        </p>
      )}
      {uncertain && (
        <button disabled={busy} onClick={() => void check()}>
          Comprobar creación
        </button>
      )}
      {uncertain && missing && intent && (
        <button
          disabled={busy || !valid}
          onClick={() => void create(intent.id)}
        >
          Reenviar el mismo intento
        </button>
      )}
      {uncertain && correctionAllowed && intent && (
        <button
          disabled={busy || !valid}
          onClick={() => void create(intent.id)}
        >
          Corregir y reenviar el mismo intento
        </button>
      )}
      {lostSecret && (
        <p role="status">
          La credencial existe, pero su secreto no se puede recuperar.
        </p>
      )}
      {listFailure && (
        <div>
          <p role="alert">No se puede consultar el listado.</p>
        </div>
      )}
      {listBusy && <p role="status">Consultando credenciales…</p>}
      <p>
        El estado corresponde a la última consulta. Puedes recargar el listado
        para actualizarlo.
      </p>
      <button disabled={listBusy} onClick={() => reloadList()}>
        Recargar listado
      </button>
      {!listBusy && !listFailure && items.length === 0 && (
        <p>Todavía no tienes credenciales.</p>
      )}
      {visibleItems.map((item) => (
        <article key={item.id}>
          <h2>{item.name}</h2>
          <p>
            {item.scopes
              .map(
                (scope) =>
                  scopeLabels[
                    apiScopes.indexOf(scope as (typeof apiScopes)[number])
                  ],
              )
              .join(", ")}
          </p>
          <p>
            Caduca:{" "}
            <time dateTime={item.expiresAt}>
              {credentialDate.format(new Date(item.expiresAt))}
            </time>
          </p>
          <p>
            {item.revokedAt
              ? "Revocada"
              : Date.parse(item.expiresAt) <= observedAt
                ? "Caducada"
                : "Vigente"}
          </p>
          {!item.revokedAt && (
            <button disabled={busy} onClick={() => setRevoking(item)}>
              Revocar {item.name}
            </button>
          )}
        </article>
      ))}
      {revoking && (
        <section>
          <h2>Revocar credencial</h2>
          <p>
            {revoking.name} dejará de autorizar nuevas solicitudes. Esta acción
            no se puede deshacer.
          </p>
          <button disabled={busy} onClick={() => setRevoking(undefined)}>
            Cancelar revocación
          </button>
          <button
            disabled={busy || revokeUncertain}
            onClick={() => void revoke()}
          >
            Confirmar revocación
          </button>
          {revokeUncertain && (
            <button disabled={busy} onClick={() => void revoke(true)}>
              Comprobar revocación
            </button>
          )}
        </section>
      )}
      {nextCursor && (
        <button disabled={listBusy} onClick={() => reloadList(nextCursor)}>
          Mostrar más credenciales
        </button>
      )}
    </main>
  );
}
