import { useEffect, useLayoutEffect, useRef, useState } from "react";
import {
  checkBlockChange,
  readChangeError,
  sendBlockChange,
  type RetainedChange,
  type BlockState,
  type BlockChange,
  type ChangeError,
} from "./reschedule-api";
export function ChangeSubmit(props: Parameters<typeof Submit>[0]) {
  const { block, revision } = props.state;
  return (
    <Submit
      key={`${block.projectId}:${block.taskId}:${block.id}:${revision}`}
      {...props}
    />
  );
}
function Submit({
  state,
  onConfirmed,
  focusFallback,
  onReload,
  movement,
  blocked = false,
  onRetained,
  onRejected,
  onAccessFailure,
}: {
  state: BlockState;
  onConfirmed: (change: BlockChange) => void;
  focusFallback: () => void;
  onReload: () => void;
  movement?: Pick<
    Extract<RetainedChange, { kind: "RESCHEDULED" }>,
    "input" | "preview"
  >;
  blocked?: boolean;
  onRetained?: () => void;
  onRejected?: (issue: ChangeError) => void;
  onAccessFailure?: (status: number) => void;
}) {
  const active = useRef<AbortController | null>(null);
  const control = useRef<HTMLButtonElement>(null);
  const interacted = useRef(false);
  useLayoutEffect(() => {
    if (interacted.current && document.activeElement === document.body)
      control.current?.focus();
  });
  const [busy, setBusy] = useState(false);
  const retained = useRef<RetainedChange | null>(null);
  const [uncertain, setUncertain] = useState(false);
  const [mayResend, setMayResend] = useState(false);
  const [conflict, setConflict] = useState(false);
  const [rejected, setRejected] = useState(false);
  useEffect(() => () => active.current?.abort(), []);
  async function cancel(check = false) {
    if (active.current || blocked) return;
    const origin = document.activeElement;
    interacted.current = true;
    const controller = new AbortController();
    active.current = controller;
    retained.current ??= movement
      ? { state, key: crypto.randomUUID(), kind: "RESCHEDULED", ...movement }
      : { state, key: crypto.randomUUID(), kind: "CANCELLED" };
    onRetained?.();
    setBusy(true);
    try {
      const value = await (check ? checkBlockChange : sendBlockChange)(
        retained.current,
        controller.signal,
      );
      if (controller.signal.aborted) return;
      if (
        document.activeElement === origin ||
        document.activeElement === document.body
      )
        focusFallback();
      onConfirmed(value);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await readChangeError(error);
      if (controller.signal.aborted) return;
      if (error instanceof Response && error.status === 401)
        onAccessFailure?.(401);
      else if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure?.(404);
      if (problem?.code === "BLOCK_CONFLICT") {
        setConflict(true);
        return;
      }
      if (
        problem &&
        [
          "BLOCK_NOT_FOUND",
          "BLOCK_CANCELLED",
          "BLOCK_UNCHANGED",
          "BLOCK_VERSION_EXHAUSTED",
          "PROJECT_COMPLETED",
          "TASK_COMPLETED",
          "AVAILABILITY_REQUIRED",
          "AVAILABILITY_ZONE_UNAVAILABLE",
          "AVAILABILITY_CONFLICT",
          "PRECONDITION_REQUIRED",
          "MALFORMED_JSON",
          "VALIDATION_ERROR",
          "BLOCK_OVERLAP",
          "BUDGET_EXCEEDED",
        ].includes(problem.code)
      ) {
        setRejected(true);
        onRejected?.(problem);
        return;
      }
      setUncertain(true);
      setMayResend(
        problem?.code === "CSRF_INVALID" ||
          (check && problem?.code === "BLOCK_CHANGE_NOT_FOUND"),
      );
    } finally {
      if (!controller.signal.aborted && active.current === controller) {
        active.current = null;
        setBusy(false);
      }
    }
  }
  return (
    <>
      {busy && <p role="status">Procesando cambio</p>}
      {!movement && (
        <>
          <p>{state.block.objective}</p>
          <p>
            Retirar esta reserva libera su tiempo planificado y conserva el
            historial.
          </p>
        </>
      )}
      {rejected ? (
        <>
          <p role="alert">
            El cambio fue rechazado. Consulta el estado actual antes de
            corregirlo.
          </p>
          <button ref={control} type="button" onClick={onReload}>
            Consultar estado actual
          </button>
        </>
      ) : conflict ? (
        <>
          <p role="alert">El bloque ha cambiado. Consulta su estado actual.</p>
          <button ref={control} type="button" onClick={onReload}>
            Consultar estado actual
          </button>
        </>
      ) : uncertain ? (
        <>
          <p role="alert">No podemos confirmar el cambio.</p>
          <button
            ref={control}
            type="button"
            aria-disabled={busy}
            onClick={() => void cancel(true)}
          >
            Comprobar cambio
          </button>
          {mayResend && (
            <button
              type="button"
              aria-disabled={busy}
              onClick={() => void cancel()}
            >
              Reenviar el mismo cambio
            </button>
          )}
        </>
      ) : (
        <button
          type="button"
          aria-disabled={busy || blocked}
          onClick={() => void cancel()}
        >
          {movement
            ? "Confirmar movimiento"
            : "Confirmar cancelación del bloque"}
        </button>
      )}
    </>
  );
}
