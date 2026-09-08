import { useState, useRef, useLayoutEffect, useEffect } from "react";
import {
  previewImportData,
  confirmImportData,
  readImportReceipt,
  ImportFileMismatchError,
  type ImportPreview,
  type ImportReceipt,
} from "./import-data-api";
import {
  saveImportIntent,
  clearImportIntent,
  readImportIntent,
  type ImportIntent,
} from "./import-data-intent";
const labels: Record<string, string> = {
  projects: "Proyectos",
  tasks: "Tareas",
  taskStatusHistory: "Historial de tareas",
  availability: "Disponibilidad",
  plannedBlocks: "Reservas originales",
  blockProjections: "Estado de reservas",
  blockChanges: "Cambios de reservas",
  workSessions: "Sesiones de trabajo",
  workSessionIntervals: "Intervalos de trabajo",
  workSessionChanges: "Cambios de sesiones",
  appearance: "Apariencia",
  customization: "Personalización",
  projectCustomFieldValues: "Campos de proyectos",
  taskCustomFieldValues: "Campos de tareas",
};
const rejectionMessages: Record<string, string> = {
  "413:IMPORT_TOO_LARGE":
    "El archivo supera el límite de 32 MiB o 100.000 registros.",
  "400:IMPORT_INVALID_FILE":
    "El archivo no es una copia propia válida y compatible. Selecciona otra copia JSON v1.",
  "409:IMPORT_CONFLICT":
    "La copia entra en conflicto con los datos actuales. No se ha importado; selecciona una copia compatible.",
  "400:IMPORT_INVALID_REQUEST":
    "La solicitud de importación no es válida. Vuelve a elegir y validar la copia.",
  "412:IMPORT_FILE_CHANGED":
    "El archivo enviado no coincide con el validado. Vuelve a elegir y validar la copia.",
  "409:IMPORT_KEY_REUSED":
    "La clave ya se utilizó con otra copia. Esta solicitud no se ha aplicado; vuelve a elegir y validar el archivo.",
};
function rejectionMessage(error: unknown, detail: unknown) {
  return error instanceof Response &&
    detail &&
    typeof detail === "object" &&
    "code" in detail &&
    typeof detail.code === "string" &&
    "status" in detail &&
    detail.status === error.status &&
    "type" in detail &&
    detail.type === `urn:organization:problem:${detail.code.toLowerCase()}`
    ? rejectionMessages[`${error.status}:${detail.code}`]
    : undefined;
}

type ImportProps = {
  owner: string;
  onImported?: (receipt: ImportReceipt) => void | Promise<void>;
};
export function ImportData(props: ImportProps) {
  return <ImportPanel key={props.owner} {...props} />;
}
function ImportPanel({ owner, onImported }: ImportProps) {
  const imported = useRef(onImported);
  useLayoutEffect(() => {
    imported.current = onImported;
  }, [onImported]);
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
  const [file, setFile] = useState<File>();
  const fileInput = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<ImportPreview>();
  const [receipt, setReceipt] = useState<ImportReceipt>();
  const [initial] = useState(() => {
    try {
      return { intent: readImportIntent(owner), failed: false };
    } catch {
      return { intent: null, failed: true };
    }
  });
  const [pendingIntent, setPendingIntent] = useState(initial.intent);
  const [storageFailed, setStorageFailed] = useState(initial.failed);
  const [uncertain, setUncertain] = useState(Boolean(pendingIntent));
  const [rejected, setRejected] = useState(false);
  const [missingReceipt, setMissingReceipt] = useState(false);
  const [refreshFailed, setRefreshFailed] = useState(false);
  const pending = useRef<AbortController | null>(null);
  useLayoutEffect(
    () => () => {
      pending.current?.abort();
      pending.current = null;
    },
    [],
  );
  const [busy, setBusy] = useState(false);
  useLayoutEffect(() => {
    if (busy) return;
    const control = initiator.current;
    initiator.current = null;
    if (control && document.activeElement === document.body)
      heading.current?.focus();
  });
  const [busyMessage, setBusyMessage] = useState("Validando archivo");
  const [failure, setFailure] = useState<string>();
  async function acceptReceipt(confirmed: ImportReceipt) {
    setFailure(undefined);
    setMissingReceipt(false);
    setRejected(false);
    setReceipt(confirmed);
    setPreview(undefined);
    setUncertain(false);
    setPendingIntent(null);
    try {
      clearImportIntent();
    } catch {
      /* The receipt already proves confirmation. */
    }
    try {
      await imported.current?.(confirmed);
    } catch {
      setRefreshFailed(true);
    }
  }
  async function validate() {
    if (!file || pending.current) return;
    const controller = new AbortController();
    pending.current = controller;
    setBusyMessage("Validando archivo");
    setBusy(true);
    setFailure(undefined);
    setPreview(undefined);
    try {
      const next = await previewImportData(file, owner, controller.signal);
      if (!controller.signal.aborted && pending.current === controller)
        setPreview(next);
    } catch (error) {
      if (controller.signal.aborted) return;
      const detail: unknown =
        error instanceof Response ? await error.json().catch(() => null) : null;
      if (controller.signal.aborted) return;
      setFailure(
        rejectionMessage(error, detail) ??
          "No se ha podido validar el archivo. Inténtalo de nuevo.",
      );
    } finally {
      if (pending.current === controller) {
        pending.current = null;
        setBusy(false);
      }
    }
  }
  async function confirm() {
    if (!file || !preview || pending.current || storageFailed) return;
    setMissingReceipt(false);
    const intent = {
      owner,
      requestKey: crypto.randomUUID(),
      fileSha256: preview.fileSha256,
    };
    await sendIntent(file, intent);
  }
  async function sendIntent(file: File, intent: ImportIntent) {
    if (pending.current || storageFailed) return;
    try {
      saveImportIntent(intent);
    } catch {
      setStorageFailed(true);
      return;
    }
    setPendingIntent(intent);
    const controller = new AbortController();
    pending.current = controller;
    setBusyMessage("Confirmando importación");
    setBusy(true);
    try {
      const confirmed = await confirmImportData(
        file,
        intent,
        controller.signal,
      );
      if (controller.signal.aborted || pending.current !== controller) return;
      await acceptReceipt(confirmed);
    } catch (error) {
      if (!controller.signal.aborted) {
        const detail: unknown =
          error instanceof Response
            ? await error.json().catch(() => null)
            : null;
        if (controller.signal.aborted || pending.current !== controller) return;
        setUncertain(true);
        setPreview(undefined);
        const message = rejectionMessage(error, detail);
        if (message && !uncertain) {
          setRejected(true);
          setFailure(message);
        }
        if (error instanceof ImportFileMismatchError)
          setFailure(
            "El archivo no coincide con la intención pendiente. Selecciona los mismos bytes.",
          );
      }
    } finally {
      if (pending.current === controller) {
        pending.current = null;
        setBusy(false);
      }
    }
  }
  function cancelPreparation() {
    pending.current?.abort();
    pending.current = null;
    setBusy(false);
    setFile(undefined);
    setPreview(undefined);
    setFailure(undefined);
    if (fileInput.current) fileInput.current.value = "";
  }
  function correctRejection() {
    if (pending.current) return;
    try {
      clearImportIntent();
    } catch {
      setStorageFailed(true);
      return;
    }
    setPendingIntent(null);
    setUncertain(false);
    setRejected(false);
    cancelPreparation();
  }
  async function check() {
    if (!pendingIntent || pending.current) return;
    const controller = new AbortController();
    pending.current = controller;
    setBusyMessage("Comprobando resultado");
    setBusy(true);
    try {
      const confirmed = await readImportReceipt(
        pendingIntent,
        controller.signal,
      );
      if (controller.signal.aborted || pending.current !== controller) return;
      await acceptReceipt(confirmed);
    } catch (error) {
      if (controller.signal.aborted) return;
      const detail: unknown =
        error instanceof Response ? await error.json().catch(() => null) : null;
      if (controller.signal.aborted || pending.current !== controller) return;
      setUncertain(true);
      setMissingReceipt(
        error instanceof Response &&
          error.status === 404 &&
          Boolean(
            detail &&
            typeof detail === "object" &&
            "code" in detail &&
            detail.code === "IMPORT_NOT_FOUND",
          ),
      );
    } finally {
      if (pending.current === controller) {
        pending.current = null;
        setBusy(false);
      }
    }
  }
  return (
    <main
      id="proyectos"
      className="import-data"
      onClickCapture={(event) => {
        const button = (event.target as HTMLElement).closest("button");
        if (button === document.activeElement) initiator.current = button;
      }}
    >
      <h1 ref={heading} tabIndex={-1}>
        Importar mis datos
      </h1>
      <p>
        Incorpora una copia JSON v1 propia a los datos de {owner}, sin
        sobrescribir los existentes.
      </p>
      <label htmlFor="import-file">Archivo JSON</label>
      <input
        ref={fileInput}
        id="import-file"
        type="file"
        disabled={(uncertain && !missingReceipt) || busy}
        accept="application/json,.json"
        onChange={(event) => {
          setFile(event.target.files?.[0]);
          setPreview(undefined);
          setReceipt(undefined);
          setFailure(undefined);
          setRefreshFailed(false);
        }}
      />
      <button
        disabled={!file || uncertain || busy}
        onClick={() => void validate()}
      >
        Validar archivo
      </button>
      {file && !pendingIntent && !uncertain && (
        <button onClick={cancelPreparation}>Cancelar preparación</button>
      )}
      {busy && <p role="status">{busyMessage}</p>}
      {failure && <p role="alert">{failure}</p>}
      {storageFailed && (
        <p role="alert">
          No se puede acceder al almacenamiento de recuperación. No se enviará
          la importación.
        </p>
      )}
      {rejected && (
        <button disabled={busy} onClick={correctRejection}>
          Elegir otra copia
        </button>
      )}
      {uncertain && !rejected && (
        <section>
          <p role="alert">
            No podemos confirmar el resultado. Comprueba la misma intención
            antes de continuar.
          </p>
          <button disabled={busy} onClick={() => void check()}>
            Comprobar resultado
          </button>
        </section>
      )}
      {uncertain && missingReceipt && (
        <section>
          <p>
            Todavía no hay recibo. La petición anterior puede seguir en curso;
            selecciona los mismos bytes para reenviar deliberadamente la misma
            intención.
          </p>
          <button
            disabled={!file || busy || storageFailed}
            onClick={() => {
              if (file && pendingIntent) void sendIntent(file, pendingIntent);
            }}
          >
            Reenviar la misma importación
          </button>
        </section>
      )}
      {preview && (
        <section>
          <h2>Vista previa</h2>
          <p>
            No se sobrescriben datos. Un conflicto impide toda la importación.
          </p>
          <p>Propietario: {preview.owner}</p>
          <p>Exportado: {preview.exportedAt}</p>
          <p>Tamaño: {preview.byteLength} bytes</p>
          {preview.runningSessions.map((session) => (
            <div key={session.sessionId}>
              <p>
                La sesión seguirá en curso. Se conserva su inicio histórico y el
                cómputo puede incluir el tiempo transcurrido; no se pausa al
                importar.
              </p>
              <p>
                Inicio del cómputo:{" "}
                {session.runningSince ?? "no disponible en la copia"}
              </p>
            </div>
          ))}
          <p>
            Total: registros del archivo. Nuevos: se añadirán. Iguales: ya
            coinciden con tus datos.
          </p>
          <table>
            <caption>Registros del archivo</caption>
            <thead>
              <tr>
                <th scope="col">Colección</th>
                <th scope="col">Total</th>
                <th scope="col">Nuevos</th>
                <th scope="col">Iguales</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(labels).map(([key, label]) => (
                <tr key={key}>
                  <th scope="row">{label}</th>
                  <td>{preview.counts[key]}</td>
                  <td>{preview.insertCounts[key]}</td>
                  <td>{preview.identicalCounts[key]}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <button
            disabled={busy || storageFailed}
            onClick={() => void confirm()}
          >
            Confirmar importación
          </button>
        </section>
      )}
      {receipt && (
        <section>
          <p role="status">
            {receipt.outcome === "NO_CHANGE"
              ? "Importación confirmada. No había registros nuevos que añadir."
              : "Importación confirmada."}
          </p>
          <p>Registrado: {receipt.recordedAt}</p>
          <table>
            <caption>Resultado confirmado</caption>
            <thead>
              <tr>
                <th scope="col">Colección</th>
                <th scope="col">Añadidos</th>
                <th scope="col">Iguales</th>
              </tr>
            </thead>
            <tbody>
              {Object.entries(labels).map(([key, label]) => (
                <tr key={key}>
                  <th scope="row">{label}</th>
                  <td>{receipt.insertedCounts[key]}</td>
                  <td>{receipt.identicalCounts[key]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
      {refreshFailed && (
        <p role="alert">
          La importación está confirmada, pero no se han podido actualizar todas
          las vistas.
        </p>
      )}
    </main>
  );
}
