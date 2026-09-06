import { useEffect, useState, useRef, useLayoutEffect } from "react";
import { BlockDetails, BlockTime } from "./block-details";
import {
  readBlockState,
  readChangeError,
  type BlockChange,
  type BlockState,
} from "./reschedule-api";
import type { Block } from "./schedule-block-api";

type ConfirmationProps = {
  block: Block;
  change?: BlockChange;
  onAccessFailure: (status: number) => void;
};
export function BlockConfirmation(props: ConfirmationProps) {
  return (
    <Confirmation
      key={JSON.stringify([props.block, props.change])}
      {...props}
    />
  );
}
function Confirmation({ block, change, onAccessFailure }: ConfirmationProps) {
  const heading = useRef<HTMLHeadingElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (interacted.current && document.activeElement === document.body)
      heading.current?.focus();
  });
  const [state, setState] = useState<BlockState>();
  const [failure, setFailure] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    void readBlockState(
      block.projectId,
      block.taskId,
      block.id,
      controller.signal,
    )
      .then((result) => {
        if (!controller.signal.aborted) setState(result);
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
  }, [block, revision, onAccessFailure]);
  return (
    <article>
      {change ? (
        <>
          <p role="status">Cambio confirmado (hecho histórico)</p>
          <p>{change.kind === "CANCELLED" ? "Cancelación" : "Movimiento"}</p>
          <h3 ref={heading} tabIndex={-1}>
            {change.before.objective}
          </h3>
          <p>{change.id}</p>
          <BlockTime value={change.occurredAt} zoneId="UTC" />
          <h4>Antes</h4>
          <BlockDetails block={change.before} />
          {change.after ? (
            <>
              <h4>Después</h4>
              <BlockDetails block={change.after} />
            </>
          ) : (
            <p>Reserva cancelada; historial conservado.</p>
          )}
        </>
      ) : (
        <>
          <p role="status">Bloque guardado</p>
          <p>Confirmación original de creación (hecho histórico)</p>
          <h3 ref={heading} tabIndex={-1}>
            {block.objective}
          </h3>
          <BlockDetails block={block} />
        </>
      )}
      {state ? (
        <section aria-label="Estado actual del bloque">
          <h4>Estado actual</h4>
          <p>{state.status === "cancelled" ? "Cancelado" : "Planificado"}</p>
          <BlockDetails block={state.block} />
        </section>
      ) : failure ? (
        <>
          <p role="alert">Operación confirmada; estado actual sin comprobar.</p>
          <button
            type="button"
            onClick={() => {
              interacted.current = true;
              setFailure(false);
              setRevision((value) => value + 1);
            }}
          >
            Reintentar estado actual
          </button>
        </>
      ) : (
        <p role="status">Consultando estado actual</p>
      )}
    </article>
  );
}
