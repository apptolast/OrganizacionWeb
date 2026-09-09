import { useCallback, useEffect, useId, useRef, useState } from "react";
import {
  createWebhook,
  deleteWebhook,
  listWebhookDeliveries,
  listWebhooks,
  pingWebhook,
  redeliverWebhook,
  setWebhookStatus,
  webhookEventTypes,
  type WebhookDelivery,
  type WebhookEndpoint,
} from "./webhooks-client";
import "./webhooks.scss";

/** Human labels for the twelve subscribable types, in catalogue order. */
const eventLabels = [
  "Crear proyecto",
  "Editar proyecto",
  "Cambiar estado de proyecto",
  "Crear tarea",
  "Crear subtarea",
  "Cambiar estado de tarea",
  "Planificar bloque",
  "Cambiar bloque",
  "Iniciar sesión de trabajo",
  "Cambiar estado de sesión",
  "Extender sesión",
  "Cerrar sesión de trabajo",
];

const deliveryColumns = [
  "Tipo",
  "Intento",
  "Código HTTP",
  "Latencia",
  "Clase de error",
  "Estado",
  "Fecha",
  "Acciones",
];

const deliveryStatusLabels: Record<string, string> = {
  pending: "Pendiente",
  succeeded: "Entregada",
  exhausted: "Agotada",
};

/** A remount per identity guarantees no state of one owner survives into another. */
export function Webhooks({ owner }: { owner: string }) {
  return <WebhookPanel key={owner} />;
}

function statusLabel(endpoint: WebhookEndpoint) {
  if (endpoint.status === "active") return "Activo";
  if (endpoint.disabledReason === "DELIVERY_EXHAUSTED")
    return `Desactivado por entregas agotadas el ${endpoint.disabledAt!.slice(0, 10)}`;
  return "Desactivado manualmente";
}

async function problemCode(error: unknown) {
  if (!(error instanceof Response)) return null;
  const body: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  return body && typeof body === "object" && "code" in body
    ? String((body as { code: unknown }).code)
    : null;
}

function WebhookPanel() {
  const [items, setItems] = useState<WebhookEndpoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [url, setUrl] = useState("");
  const [description, setDescription] = useState("");
  const [types, setTypes] = useState<string[]>([]);
  const [creating, setCreating] = useState(false);
  const [secret, setSecret] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [urlError, setUrlError] = useState<string | null>(null);
  const [uncertain, setUncertain] = useState(false);
  const [confirming, setConfirming] = useState<WebhookEndpoint | null>(null);
  const [deliveriesOf, setDeliveriesOf] = useState<string | null>(null);
  const [deliveries, setDeliveries] = useState<WebhookDelivery[]>([]);

  const listHeading = useRef<HTMLHeadingElement>(null);
  const createButton = useRef<HTMLButtonElement>(null);
  // Whatever opened the confirmation gets the focus back when it closes.
  const confirmOpener = useRef<HTMLButtonElement | null>(null);
  const requests = useRef<AbortController[]>([]);
  const urlErrorId = useId();

  /** Every in-flight request is tracked so leaving the view aborts all of them. */
  const track = useCallback(() => {
    const controller = new AbortController();
    requests.current.push(controller);
    return controller;
  }, []);

  useEffect(
    () => () => {
      for (const controller of requests.current) controller.abort();
    },
    [],
  );

  /** Only ever writes state from the resolved promise, never synchronously. */
  const fetchList = useCallback(() => {
    const controller = track();
    void listWebhooks(controller.signal)
      .then((next) => {
        if (!controller.signal.aborted) setItems(next);
      })
      .catch(() => {
        if (!controller.signal.aborted) setLoadFailed(true);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
  }, [track]);

  // The list starts busy, so the first pass has nothing to set before fetching.
  useEffect(fetchList, [fetchList]);

  function load() {
    setLoading(true);
    setLoadFailed(false);
    fetchList();
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (creating) return;
    setCreating(true);
    setFormError(null);
    setUrlError(null);
    setUncertain(false);
    const controller = track();
    try {
      const created = await createWebhook(
        { url, description, eventTypes: types },
        controller.signal,
      );
      if (controller.signal.aborted) return;
      setSecret(created.secret);
      setItems((current) => [created.endpoint, ...current]);
      setUrl("");
      setDescription("");
      setTypes([]);
    } catch (error) {
      if (controller.signal.aborted) return;
      await report(error);
    } finally {
      if (!controller.signal.aborted) setCreating(false);
    }
  }

  async function report(error: unknown) {
    const code = await problemCode(error);
    if (code === "WEBHOOK_URL_BLOCKED") {
      setUrlError("La dirección de destino no está permitida.");
      return;
    }
    if (code === "CONNECTORS_DISABLED") {
      setFormError("Falta configuración del servidor para conectores.");
      return;
    }
    if (code === "WEBHOOK_LIMIT") {
      setFormError(
        "Ya tienes cinco webhooks. Elimina uno antes de crear otro.",
      );
      return;
    }
    if (code === "WEBHOOK_INVALID") {
      setFormError("Revisa los datos del formulario.");
      return;
    }
    if (error instanceof Response) {
      setFormError("No se ha podido crear el webhook.");
      return;
    }
    // No response at all: the result is genuinely unknown, so never retry on our own.
    setUncertain(true);
  }

  async function act(run: (signal: AbortSignal) => Promise<void>) {
    const controller = track();
    try {
      await run(controller.signal);
    } catch (error) {
      if (!controller.signal.aborted) await report(error);
    }
  }

  function changeStatus(
    endpoint: WebhookEndpoint,
    status: "active" | "disabled",
  ) {
    void act(async (signal) => {
      const updated = await setWebhookStatus(endpoint.id, status, signal);
      if (signal.aborted) return;
      setItems((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
    });
  }

  function ping(endpoint: WebhookEndpoint) {
    void act(async (signal) => {
      const sent = await pingWebhook(endpoint.id, signal);
      if (signal.aborted) return;
      setDeliveriesOf(endpoint.id);
      setDeliveries((current) => [
        sent,
        ...current.filter((d) => d.id !== sent.id),
      ]);
    });
  }

  function remove(endpoint: WebhookEndpoint) {
    void act(async (signal) => {
      await deleteWebhook(endpoint.id, signal);
      if (signal.aborted) return;
      setItems((current) => current.filter((item) => item.id !== endpoint.id));
      setConfirming(null);
      listHeading.current?.focus();
    });
  }

  function openDeliveries(endpoint: WebhookEndpoint) {
    setDeliveriesOf(endpoint.id);
    void act(async (signal) => {
      const rows = await listWebhookDeliveries(endpoint.id, signal);
      if (!signal.aborted) setDeliveries(rows);
    });
  }

  function redeliver(endpointId: string, row: WebhookDelivery) {
    void act(async (signal) => {
      const reopened = await redeliverWebhook(endpointId, row.id, signal);
      if (signal.aborted) return;
      setDeliveries((current) =>
        current.map((item) => (item.id === reopened.id ? reopened : item)),
      );
    });
  }

  function closeSecret() {
    setSecret(null);
    createButton.current?.focus();
  }

  function closeConfirmation() {
    setConfirming(null);
    confirmOpener.current?.focus();
    confirmOpener.current = null;
  }

  return (
    <main id="proyectos" tabIndex={-1} className="webhooks">
      <h1>Webhooks</h1>

      <div aria-live="polite">{loading && <p>Cargando webhooks…</p>}</div>

      {loadFailed && (
        <p role="alert">
          No se ha podido cargar la lista de webhooks.{" "}
          <button type="button" onClick={load}>
            Reintentar
          </button>
        </p>
      )}

      {secret && (
        <div className="webhook-secret">
          <h2>Guarda el secreto ahora</h2>
          <p>
            Este secreto no volverá a mostrarse. Guárdalo antes de cerrar este
            panel.
          </p>
          <label>
            Secreto
            <input
              readOnly
              value={secret}
              onFocus={(e) => e.currentTarget.select()}
            />
          </label>
          <button
            type="button"
            onClick={() => void navigator.clipboard?.writeText(secret)}
          >
            Copiar
          </button>
          <button type="button" onClick={closeSecret}>
            Cerrar
          </button>
        </div>
      )}

      <form onSubmit={(event) => void submit(event)}>
        <h2>Crear un webhook</h2>
        <p>
          Un webhook envía tus hechos de trabajo ya confirmados a una URL https
          que elijas, firmados con la cabecera X-OrganizationWeb-Signature para
          que puedas verificarlos. Puedes tener hasta cinco.
        </p>
        <label>
          URL
          <input
            type="url"
            value={url}
            required
            aria-describedby={urlError ? urlErrorId : undefined}
            onChange={(event) => setUrl(event.target.value)}
          />
        </label>
        {urlError && (
          <p id={urlErrorId} role="alert">
            {urlError}
          </p>
        )}
        <label>
          Descripción
          <input
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
        </label>
        <fieldset>
          <legend>Tipos de evento</legend>
          <label>
            <input
              type="checkbox"
              checked={types.length === webhookEventTypes.length}
              onChange={(event) =>
                setTypes(event.target.checked ? [...webhookEventTypes] : [])
              }
            />
            Seleccionar todos
          </label>
          {webhookEventTypes.map((type, index) => (
            <label key={type}>
              <input
                type="checkbox"
                checked={types.includes(type)}
                onChange={(event) =>
                  setTypes((current) =>
                    event.target.checked
                      ? webhookEventTypes.filter(
                          (candidate) =>
                            candidate === type || current.includes(candidate),
                        )
                      : current.filter((candidate) => candidate !== type),
                  )
                }
              />
              {eventLabels[index]}
            </label>
          ))}
        </fieldset>
        <button type="submit" ref={createButton} disabled={creating}>
          Crear webhook
        </button>
      </form>

      {formError && <p role="alert">{formError}</p>}
      {uncertain && (
        <p role="alert">
          No sabemos si el webhook llegó a crearse. Actualiza la lista para
          comprobarlo.{" "}
          <button type="button" onClick={load}>
            Actualizar lista
          </button>
        </p>
      )}

      <h2 tabIndex={-1} ref={listHeading}>
        Tus webhooks
      </h2>
      <ul>
        {items.map((endpoint) => (
          <li key={endpoint.id}>
            <span>{endpoint.url}</span>
            <span>{endpoint.description}</span>
            <span>
              {endpoint.eventTypes
                .map(
                  (type) =>
                    eventLabels[webhookEventTypes.indexOf(type as never)],
                )
                .join(", ")}
            </span>
            <span aria-label={statusLabel(endpoint)}>
              {statusLabel(endpoint)}
            </span>
            {endpoint.status === "active" ? (
              <>
                <button type="button" onClick={() => ping(endpoint)}>
                  Enviar ping
                </button>
                <button
                  type="button"
                  onClick={() => changeStatus(endpoint, "disabled")}
                >
                  Desactivar
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => changeStatus(endpoint, "active")}
              >
                Activar
              </button>
            )}
            <button type="button" onClick={() => openDeliveries(endpoint)}>
              Ver entregas
            </button>
            <button
              type="button"
              onClick={(event) => {
                confirmOpener.current = event.currentTarget;
                setConfirming(endpoint);
              }}
            >
              Eliminar
            </button>
          </li>
        ))}
      </ul>

      {confirming && (
        <div role="dialog" aria-label="Confirmar eliminación">
          <p>
            Se eliminará el webhook {confirming.url} y su registro de entregas.
            Esta acción no se puede deshacer.
          </p>
          <button type="button" onClick={() => remove(confirming)}>
            Confirmar eliminación
          </button>
          <button type="button" onClick={closeConfirmation}>
            Cancelar
          </button>
        </div>
      )}

      {deliveriesOf && (
        <div className="webhook-deliveries">
          <h2>Entregas</h2>
          <button
            type="button"
            onClick={() => {
              const endpoint = items.find((item) => item.id === deliveriesOf);
              if (endpoint) openDeliveries(endpoint);
            }}
          >
            Actualizar
          </button>
          {/*
            Explicit ARIA roles: at narrow widths the stylesheet stacks these elements,
            and overriding `display` on table elements would otherwise strip the table
            semantics from the accessibility tree. Each cell also carries its own label
            so nothing is lost when the header row is visually stacked away.
          */}
          <table role="table">
            <thead>
              <tr role="row">
                {deliveryColumns.map((column) => (
                  <th key={column} role="columnheader" scope="col">
                    {column}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {deliveries.map((row) => (
                <tr key={row.id} role="row">
                  <td role="cell" data-label="Tipo">
                    {row.eventType}
                  </td>
                  <td role="cell" data-label="Intento">
                    {row.attempt}
                  </td>
                  <td role="cell" data-label="Código HTTP">
                    {row.httpStatus ?? "—"}
                  </td>
                  <td role="cell" data-label="Latencia">
                    {row.latencyMs === null ? "—" : `${row.latencyMs} ms`}
                  </td>
                  <td role="cell" data-label="Clase de error">
                    {row.errorClass ?? "—"}
                  </td>
                  <td role="cell" data-label="Estado">
                    {deliveryStatusLabels[row.status]}
                  </td>
                  <td role="cell" data-label="Fecha">
                    {row.updatedAt.slice(0, 10)}
                  </td>
                  <td role="cell" data-label="Acciones">
                    {row.status !== "pending" && (
                      <button
                        type="button"
                        onClick={() => redeliver(deliveriesOf, row)}
                      >
                        Reenviar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  );
}
