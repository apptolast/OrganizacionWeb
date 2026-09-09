import { useEffect, useLayoutEffect, useRef, useState } from "react";
import {
  createCalendarFeed,
  readCalendarFeed,
  readCalendarFile,
  revokeCalendarFeed,
  type CalendarFeedLink,
  type CalendarFeedStatus,
} from "./calendar-feed-api";

type Confirmation = "regenerate" | "revoke";
type Busy = "loading" | "creating" | "revoking" | "downloading";
type Failure = "status" | "generate" | "revoke" | "download" | "limit";
type Copy = "done" | "manual";

const READABLE = new Intl.DateTimeFormat("es", {
  dateStyle: "medium",
  timeStyle: "short",
});

const ANNOUNCEMENTS: Record<Busy, string> = {
  loading: "Cargando…",
  creating: "Creando enlace…",
  revoking: "Revocando enlace…",
  downloading: "Preparando archivo…",
};

/** Al enfocar el campo, su contenido queda seleccionado entero, como hacía `select()`. */
function selectAll(field: HTMLElement) {
  const selection = window.getSelection();
  if (!selection) return;
  const range = document.createRange();
  range.selectNodeContents(field);
  selection.removeAllRanges();
  selection.addRange(range);
}

const CONFIRMATIONS: Record<Confirmation, string> = {
  regenerate: "Confirmar regeneración",
  revoke: "Confirmar revocación",
};

/** A new identity gets a new view: no url, no state and no in-flight request survives. */
export function Calendar({ owner }: { owner: string }) {
  return <CalendarFeed key={owner} />;
}

function CalendarFeed() {
  const [status, setStatus] = useState<CalendarFeedStatus | null>(null);
  const [link, setLink] = useState<CalendarFeedLink | null>(null);
  const [busy, setBusy] = useState<Busy | null>("loading");
  const [failure, setFailure] = useState<Failure | null>(null);
  const [confirming, setConfirming] = useState<Confirmation | null>(null);
  const [copy, setCopy] = useState<Copy | null>(null);
  const [file, setFile] = useState<{ url: string; fileName: string } | null>(
    null,
  );
  const pending = useRef<AbortController | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const confirmation = useRef<HTMLDivElement>(null);
  const initiator = useRef<HTMLButtonElement | null>(null);
  const objectUrl = useRef<string | null>(null);

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
    if (confirming) confirmation.current?.focus();
  }, [confirming]);

  /** Only when the person did not move the focus themselves. */
  useLayoutEffect(() => {
    if (busy) return;
    const control = initiator.current;
    initiator.current = null;
    if (control && document.activeElement === document.body) {
      if (control.isConnected) control.focus();
      else heading.current?.focus();
    }
  }, [busy, status, link]);

  useLayoutEffect(
    () => () => {
      pending.current?.abort();
      pending.current = null;
      if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
      objectUrl.current = null;
    },
    [],
  );

  async function run<T>(
    kind: Busy,
    failureKind: Failure,
    work: (signal: AbortSignal) => Promise<T>,
    done: (value: T) => void,
    onFailure?: (error: unknown) => Failure,
  ) {
    if (pending.current) return;
    const controller = new AbortController();
    pending.current = controller;
    setBusy(kind);
    setFailure(null);
    try {
      const value = await work(controller.signal);
      if (controller.signal.aborted || pending.current !== controller) return;
      done(value);
    } catch (error) {
      if (controller.signal.aborted || pending.current !== controller) return;
      setFailure(onFailure ? onFailure(error) : failureKind);
    } finally {
      if (pending.current === controller) {
        pending.current = null;
        setBusy(null);
      }
    }
  }

  const load = () =>
    run("loading", "status", readCalendarFeed, (value) => {
      setStatus(value);
      setLink(null);
    });

  useEffect(() => {
    void load();
    // The view reads the status once per mount and never on its own again.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const generate = () =>
    run("creating", "generate", createCalendarFeed, (value) => {
      setLink(value);
      setStatus({ active: true, createdAt: value.createdAt });
      setCopy(null);
      setConfirming(null);
    });

  const revoke = () =>
    run("revoking", "revoke", revokeCalendarFeed, () => {
      setStatus({ active: false, createdAt: null });
      setLink(null);
      setCopy(null);
      setConfirming(null);
    });

  const download = () =>
    run(
      "downloading",
      "download",
      readCalendarFile,
      (value) => {
        if (objectUrl.current) URL.revokeObjectURL(objectUrl.current);
        const url = URL.createObjectURL(
          new Blob([value.bytes as BlobPart], {
            type: "text/calendar;charset=utf-8",
          }),
        );
        objectUrl.current = url;
        setFile({ url, fileName: value.fileName });
      },
      (error) =>
        error instanceof Response && error.status === 413
          ? "limit"
          : "download",
    );

  async function copyLink() {
    if (!link) return;
    const clipboard = navigator.clipboard;
    if (!clipboard?.writeText) return setCopy("manual");
    try {
      await clipboard.writeText(link.url);
      setCopy("done");
    } catch {
      setCopy("manual");
    }
  }

  function remember(event: { currentTarget: HTMLButtonElement }) {
    initiator.current = event.currentTarget;
  }

  const created = status?.createdAt ?? null;
  const showCreate = status !== null && !status.active;
  const showManage = status !== null && status.active;
  /** Retrying is always deliberate: it repeats the failed step and nothing else. */
  const retriable = failure !== null && failure !== "limit";
  function retry() {
    if (failure === "status") return void load();
    if (failure === "generate") return void generate();
    if (failure === "revoke") return void revoke();
    if (failure === "download") return void download();
  }

  return (
    <main id="proyectos" className="calendar-feed">
      <h1 ref={heading} tabIndex={-1}>
        Calendario ICS
      </h1>
      <p>
        Suscribe tu calendario a los bloques planificados vigentes, desde 30
        días atrás hasta un año por delante.
      </p>
      <p>
        La dirección es secreta y de solo lectura: quien la conozca verá los
        títulos de tareas y objetivos de esos bloques. El calendario externo no
        sincroniza cambios hacia aquí; lo que edites allí no llega a
        OrganizationWeb.
      </p>

      {busy && <p role="status">{ANNOUNCEMENTS[busy]}</p>}

      {failure && (
        <p role="alert">
          {failure === "limit"
            ? "El calendario supera los 2000 eventos de la ventana publicada. No se ha preparado ningún archivo."
            : "No se pudo completar la operación. Puedes intentarlo de nuevo."}
        </p>
      )}
      {retriable && (
        <button onClick={(event) => (remember(event), retry())}>
          Reintentar
        </button>
      )}

      {showCreate && !confirming && (
        <button
          onClick={(event) => (remember(event), void generate())}
          disabled={Boolean(busy)}
        >
          Crear enlace de suscripción
        </button>
      )}

      {link && (
        <section aria-labelledby="calendar-link-heading">
          <h2 id="calendar-link-heading">Enlace recién creado</h2>
          <p>
            Guárdalo ahora: no volverá a mostrarse. Quien lo conozca verá los
            títulos de tareas y objetivos de tus bloques.
          </p>
          <span className="field-label" id="calendar-link-label">
            Enlace de suscripción
          </span>
          {/*
            Campo de solo lectura cuya altura la fija su contenido. Un `textarea` no puede hacerlo
            sin JavaScript: con `rows` fijas recortaba la url por abajo a 320 px, que es el mismo
            defecto que tenía el `input` con `text-overflow: ellipsis`, sólo que en el otro eje.
            Aquí no hay caja que recortar: el texto envuelve y el elemento crece.
          */}
          <div
            id="calendar-link"
            data-calendar-link
            role="textbox"
            aria-readonly="true"
            aria-labelledby="calendar-link-label"
            tabIndex={0}
            onFocus={(event) => selectAll(event.currentTarget)}
          >
            {link.url}
          </div>
          <button onClick={(event) => (remember(event), void copyLink())}>
            Copiar enlace
          </button>
          {copy === "done" && <p role="status">Enlace copiado</p>}
          {copy === "manual" && (
            <p role="status">
              No se pudo usar el portapapeles: selecciona el campo y copia el
              enlace a mano.
            </p>
          )}
        </section>
      )}

      {created && <p>Enlace creado el {READABLE.format(new Date(created))}.</p>}

      {showManage && !confirming && (
        <>
          <button
            onClick={(event) => (remember(event), setConfirming("regenerate"))}
            disabled={Boolean(busy)}
          >
            Regenerar enlace
          </button>
          <button
            onClick={(event) => (remember(event), setConfirming("revoke"))}
            disabled={Boolean(busy)}
          >
            Revocar enlace
          </button>
        </>
      )}

      {confirming && (
        <div
          role="group"
          aria-label="El enlace anterior dejará de funcionar en cualquier calendario donde esté pegado"
          ref={confirmation}
          tabIndex={-1}
        >
          <p>
            El enlace anterior dejará de funcionar en cualquier calendario donde
            esté pegado. Tendrás que volver a pegar el nuevo enlace en cada uno.
          </p>
          <button
            onClick={(event) => {
              remember(event);
              const action = confirming;
              setConfirming(null);
              void (action === "regenerate" ? generate() : revoke());
            }}
          >
            {CONFIRMATIONS[confirming]}
          </button>
          <button onClick={(event) => (remember(event), setConfirming(null))}>
            Cancelar
          </button>
        </div>
      )}

      <h2>Descargar una copia</h2>
      <p>
        Descarga el mismo documento como archivo, sin crear ni cambiar ningún
        enlace.
      </p>
      <button
        onClick={(event) => (remember(event), void download())}
        disabled={Boolean(busy)}
      >
        Descargar archivo .ics
      </button>
      {file && (
        <>
          <p role="status">Archivo preparado</p>
          <a className="primary-link" href={file.url} download={file.fileName}>
            Guardar {file.fileName}
          </a>
        </>
      )}
    </main>
  );
}
