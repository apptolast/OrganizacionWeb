// @ts-nocheck
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { readExportData } from "./export-data-api";

export function ExportData({ owner }: { owner: string }) {
  return <ExportPreparation key={owner} owner={owner} />;
}
function ExportPreparation({ owner }: { owner: string }) {
  const [archive, setArchive] = useState<{
    url: string;
    fileName: string;
  } | null>(null);
  const pending = useRef<AbortController | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const initiator = useRef<HTMLButtonElement | null>(null);
  const objectUrl = useRef<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<"temporary" | "limit" | null>(null);
  useLayoutEffect(() => {
    heading.current?.focus();
  }, []);
  useEffect(() => {
    const moved = (event: FocusEvent) => {
      if (event.target !== initiator.current) initiator.current = null;
    };
    document.addEventListener("focusin", moved);
    return () => document.removeEventListener("focusin", moved);
  }, []);
  useLayoutEffect(() => {
    if (busy) return;
    const control = initiator.current;
    initiator.current = null;
    if (control && document.activeElement === document.body) {
      if (control.isConnected) control.focus();
      else heading.current?.focus();
    }
  }, [busy]);
  useLayoutEffect(
    () => () => {
      pending.current?.abort();
      pending.current = null;
      if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
      objectUrl.current = null;
    },
    [],
  );
  async function prepare() {
    if (pending.current) return;
    if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
    objectUrl.current = null;
    setArchive(null);
    const controller = new AbortController();
    pending.current = controller;
    setBusy(true);
    setFailure(null);
    try {
      const file = await readExportData(owner, controller.signal);
      if (controller.signal.aborted || pending.current !== controller) return;
      const url = URL.createObjectURL(
        new Blob([file.bytes], { type: "application/json;charset=utf-8" }),
      );
      objectUrl.current = url;
      setArchive({ url, fileName: file.fileName });
    } catch (error) {
      if (controller.signal.aborted || pending.current !== controller) return;
      const detail: unknown =
        error instanceof Response && error.status === 413
          ? await error.json().catch(() => null)
          : null;
      if (!controller.signal.aborted)
        setFailure(
          detail &&
            typeof detail === "object" &&
            "code" in detail &&
            detail.code === "EXPORT_TOO_LARGE"
            ? "limit"
            : "temporary",
        );
    } finally {
      if (pending.current === controller) {
        pending.current = null;
        setBusy(false);
      }
    }
  }
  function cancel() {
    pending.current?.abort();
    pending.current = null;
    setBusy(false);
  }
  return (
    <main id="proyectos" className="export-data">
      <h1 ref={heading} tabIndex={-1}>
        Exportar mis datos
      </h1>
      <p>Prepara una copia de los datos de {owner} en un archivo JSON.</p>
      <p>
        Incluye tus proyectos, tareas, planificación, trabajo registrado y
        preferencias en JSON versionado. Es un archivo personal: decide dónde
        conservarlo y con quién compartirlo. En Importación puedes validar una
        copia propia antes de confirmar su incorporación.
      </p>
      <p>
        El enlace sólo permanece disponible en esta vista. Salir no elimina un
        archivo que ya hayas descargado.
      </p>
      {failure !== "limit" && (
        <button
          className={archive ? "secondary-link" : undefined}
          disabled={busy}
          onClick={(event) => {
            initiator.current = event.currentTarget;
            void prepare();
          }}
        >
          {failure
            ? "Reintentar preparación"
            : archive
              ? "Preparar de nuevo"
              : "Preparar exportación"}
        </button>
      )}
      {failure && (
        <p role="alert">
          {failure === "limit"
            ? "La exportación supera el límite de 100000 registros o 32 MiB. No se ha preparado ningún archivo. Repetir la solicitud con los mismos datos no resolverá este límite."
            : "No se pudo preparar la exportación. Puedes intentarlo de nuevo."}
        </p>
      )}
      {busy && (
        <>
          <p role="status">Preparando exportación…</p>
          <button
            onClick={(event) => {
              initiator.current = event.currentTarget;
              cancel();
            }}
          >
            Cancelar preparación
          </button>
        </>
      )}
      {archive && (
        <>
          <p role="status">Archivo preparado</p>
          <a
            className="primary-link"
            href={archive.url}
            download={archive.fileName}
          >
            Descargar archivo JSON
          </a>
        </>
      )}
    </main>
  );
}
