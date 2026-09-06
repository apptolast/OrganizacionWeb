import { useEffect, useRef, useState } from "react";
import {
  readBlockState,
  sendBlockChange,
  type BlockState,
  type BlockChange,
} from "./reschedule-api";
type Props = {
  projectId: string;
  taskId: string;
  blockId: string;
  mode: "move" | "cancel";
  onClose: () => void;
  onConfirmed: (change: BlockChange) => void;
  focusFallback: () => void;
};
export function BlockActions({
  projectId,
  taskId,
  blockId,
  mode,
  onClose,
  onConfirmed,
  focusFallback,
}: Props) {
  const [state, setState] = useState<BlockState>();
  const [failed, setFailed] = useState(false);
  const [revision, setRevision] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    void readBlockState(projectId, taskId, blockId, controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setState(value);
      })
      .catch(() => {
        if (!controller.signal.aborted) setFailed(true);
      });
    return () => controller.abort();
  }, [projectId, taskId, blockId, revision]);
  const title = mode === "move" ? "Mover bloque" : "Cancelar bloque";
  return (
    <section aria-label={title}>
      <h3>{title}</h3>
      {failed ? (
        <>
          <p role="alert">No se pudo consultar el estado del bloque.</p>
          <button
            onClick={() => {
              setFailed(false);
              setRevision((value) => value + 1);
            }}
          >
            Consultar estado actual
          </button>
        </>
      ) : state ? (
        mode === "move" ? (
          <MoveFields state={state} />
        ) : (
          <CancelBlock
            state={state}
            onConfirmed={onConfirmed}
            focusFallback={focusFallback}
          />
        )
      ) : (
        <p role="status">Consultando estado del bloque</p>
      )}
      <button type="button" onClick={onClose}>
        Cancelar edición
      </button>
    </section>
  );
}
function CancelBlock({
  state,
  onConfirmed,
  focusFallback,
}: {
  state: BlockState;
  onConfirmed: Props["onConfirmed"];
  focusFallback: () => void;
}) {
  const active = useRef<AbortController | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => () => active.current?.abort(), []);
  async function cancel() {
    if (active.current) return;
    const origin = document.activeElement;
    const controller = new AbortController();
    active.current = controller;
    setBusy(true);
    const value = await sendBlockChange(
      { state, key: crypto.randomUUID(), kind: "CANCELLED" },
      controller.signal,
    );
    if (controller.signal.aborted) return;
    if (
      document.activeElement === origin ||
      document.activeElement === document.body
    )
      focusFallback();
    onConfirmed(value);
  }
  return (
    <>
      <p>{state.block.objective}</p>
      <p>
        Retirar esta reserva libera su tiempo planificado y conserva el
        historial.
      </p>
      <button type="button" aria-disabled={busy} onClick={() => void cancel()}>
        Confirmar cancelación del bloque
      </button>
    </>
  );
}
function MoveFields({ state }: { state: BlockState }) {
  const [start, setStart] = useState(
    localTime(state.block.startAt, state.block.zoneId),
  );
  const [end, setEnd] = useState(
    localTime(state.block.endAt, state.block.zoneId),
  );
  return (
    <>
      <p>{state.block.objective}</p>
      <label>
        Inicio local
        <input
          type="datetime-local"
          value={start}
          onChange={(e) => setStart(e.target.value)}
        />
      </label>
      <label>
        Fin local
        <input
          type="datetime-local"
          value={end}
          onChange={(e) => setEnd(e.target.value)}
        />
      </label>
    </>
  );
}
function localTime(value: string, zoneId: string) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: zoneId,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(value));
  const part = (name: string) => parts.find((p) => p.type === name)!.value;
  return `${part("year").padStart(4, "0")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`;
}
