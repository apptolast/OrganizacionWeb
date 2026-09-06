import { useEffect, useState, useRef, useLayoutEffect } from "react";
import {
  readBlockChanges,
  readBlockState,
  readChangeError,
  type BlockChangePage,
  type BlockChange,
  type BlockState,
} from "./reschedule-api";
import { BlockDetails, BlockTime } from "./block-details";

type HistoryProps = {
  projectId: string;
  taskId: string;
  onAccessFailure: (status: number) => void;
  refreshToken?: number;
};
export function BlockChangeHistory(props: HistoryProps) {
  return (
    <History
      key={`${props.projectId}:${props.taskId}:${props.refreshToken ?? 0}`}
      {...props}
    />
  );
}
function History({ projectId, taskId, onAccessFailure }: HistoryProps) {
  const [page, setPage] = useState<BlockChangePage>();
  const [cursor, setCursor] = useState<string>();
  const [failure, setFailure] = useState(false);
  const [revision, setRevision] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (interacted.current && document.activeElement === document.body)
      heading.current?.focus();
  }, [page, failure]);
  useEffect(() => {
    const controller = new AbortController();
    void readBlockChanges(projectId, taskId, cursor, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setPage(result);
      })
      .catch(async (error: unknown) => {
        if (controller.signal.aborted) return;
        const problem = await readChangeError(error);
        if (controller.signal.aborted) return;
        if (error instanceof Response && error.status === 401)
          onAccessFailure(401);
        else if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure(404);
        setFailure(true);
      });
    return () => controller.abort();
  }, [projectId, taskId, cursor, revision, onAccessFailure]);
  return (
    <section className="task-history" aria-label="Cambios de bloques">
      <h2 ref={heading} tabIndex={-1}>
        Cambios de bloques
      </h2>
      {failure ? (
        <>
          <p role="alert">No se pudieron consultar los cambios de bloques.</p>
          <button
            type="button"
            onClick={() => {
              interacted.current = true;
              setFailure(false);
              setPage(undefined);
              setRevision((value) => value + 1);
            }}
          >
            Reintentar cambios
          </button>
        </>
      ) : page ? (
        page.items.length ? (
          <ol className="task-list" aria-label="Historial de bloques">
            {page.items.map((entry) => (
              <ChangeEntry
                key={entry.id}
                entry={entry}
                onAccessFailure={onAccessFailure}
              />
            ))}
          </ol>
        ) : (
          <p>Todavía no hay cambios de bloques.</p>
        )
      ) : (
        <p role="status">Consultando cambios de bloques</p>
      )}
      {page?.nextCursor && (
        <button
          type="button"
          onClick={() => {
            interacted.current = true;
            setPage(undefined);
            setCursor(page.nextCursor!);
          }}
        >
          Más cambios anteriores
        </button>
      )}
      {cursor && page && (
        <button
          type="button"
          onClick={() => {
            interacted.current = true;
            setPage(undefined);
            setCursor(undefined);
          }}
        >
          Volver a cambios recientes
        </button>
      )}
    </section>
  );
}
function ChangeEntry({
  entry,
  onAccessFailure,
}: {
  entry: BlockChange;
  onAccessFailure: (status: number) => void;
}) {
  const [state, setState] = useState<BlockState>();
  const [loading, setLoading] = useState(false);
  const [failure, setFailure] = useState(false);
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  async function consult() {
    if (request.current) return;
    const controller = new AbortController();
    request.current = controller;
    setState(undefined);
    setFailure(false);
    setLoading(true);
    try {
      const result = await readBlockState(
        entry.before.projectId,
        entry.before.taskId,
        entry.blockId,
        controller.signal,
      );
      if (!controller.signal.aborted) setState(result);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await readChangeError(error);
      if (controller.signal.aborted) return;
      if (error instanceof Response && error.status === 401)
        onAccessFailure(401);
      else if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure(404);
      setFailure(true);
    } finally {
      if (!controller.signal.aborted) {
        request.current = null;
        setLoading(false);
      }
    }
  }
  return (
    <li>
      <h3>{entry.kind === "CANCELLED" ? "Cancelación" : "Movimiento"}</h3>
      <p>{entry.before.objective}</p>
      <p>Confirmación histórica</p>
      <BlockTime value={entry.occurredAt} zoneId="UTC" />
      <h4>Antes</h4>
      <BlockDetails block={entry.before} />
      {entry.after ? (
        <>
          <h4>Después</h4>
          <BlockDetails block={entry.after} />
        </>
      ) : (
        <p>Reserva cancelada; historial conservado.</p>
      )}
      <button
        type="button"
        aria-disabled={loading}
        onClick={() => void consult()}
      >
        Consultar estado actual
      </button>
      {loading && <p role="status">Consultando estado actual</p>}
      {failure && (
        <p role="alert">Operación confirmada; estado actual sin comprobar.</p>
      )}
      {state && (
        <section aria-label="Estado actual del bloque">
          <h4>Estado actual</h4>
          <p>{state.status === "cancelled" ? "Cancelado" : "Planificado"}</p>
          <BlockDetails block={state.block} />
        </section>
      )}
    </li>
  );
}
