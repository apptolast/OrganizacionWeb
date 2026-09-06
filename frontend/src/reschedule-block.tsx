import { ChangeSubmit } from "./change-submit";
import { useEffect, useRef, useState, useId, useLayoutEffect } from "react";
import { readAvailabilityZones } from "./availability-api";
import { BlockDetails } from "./block-details";
import type { BlockPreview, BlockFieldError } from "./schedule-block-api";
import {
  readBlockState,
  previewMove,
  readChangeError,
  type BlockState,
  type BlockChange,
  type ChangeError,
} from "./reschedule-api";
type Props = {
  projectId: string;
  taskId: string;
  blockId: string;
  mode: "move" | "cancel";
  eligible: boolean;
  onAccessFailure: (status: number) => void;
  onClose: () => void;
  onConfirmed: (change: BlockChange) => void;
  focusFallback: () => void;
};
export function BlockActions({
  projectId,
  taskId,
  blockId,
  mode,
  eligible,
  onAccessFailure,
  onClose,
  onConfirmed,
  focusFallback,
}: Props) {
  const [state, setState] = useState<BlockState>();
  const [failed, setFailed] = useState(false);
  const [revision, setRevision] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  useLayoutEffect(() => {
    if (document.activeElement === document.body) heading.current?.focus();
  });
  useEffect(() => {
    const controller = new AbortController();
    void readBlockState(projectId, taskId, blockId, controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setState(value);
      })
      .catch(async (error: unknown) => {
        if (controller.signal.aborted) return;
        const problem = await readChangeError(error);
        if (controller.signal.aborted) return;
        if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure(404);
        setFailed(true);
      });
    return () => controller.abort();
  }, [projectId, taskId, blockId, revision, onAccessFailure]);
  const title = mode === "move" ? "Mover bloque" : "Cancelar bloque";
  return (
    <section aria-label={title}>
      <h3 ref={heading} tabIndex={-1}>
        {title}
      </h3>
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
      ) : state?.status === "cancelled" ? (
        <p role="status">Este bloque está cancelado.</p>
      ) : state ? (
        mode === "move" ? (
          <MoveFields
            state={state}
            onAccessFailure={onAccessFailure}
            onConfirmed={onConfirmed}
            eligible={eligible}
            focusFallback={focusFallback}
            focusRecovery={() => heading.current?.focus()}
          />
        ) : (
          <>
            <BlockDetails block={state.block} />
            <ChangeSubmit
              state={state}
              onAccessFailure={onAccessFailure}
              onConfirmed={onConfirmed}
              focusFallback={focusFallback}
              onReload={() => {
                setState(undefined);
                setRevision((value) => value + 1);
              }}
            />
          </>
        )
      ) : (
        <p role="status">Consultando estado del bloque</p>
      )}
      <p>
        Cerrar la edición no revoca una operación ya enviada. Consulta el
        historial para comprobar su resultado.
      </p>
      <button
        type="button"
        onClick={(event) => {
          if (
            document.activeElement === event.currentTarget ||
            document.activeElement === document.body
          )
            focusFallback();
          onClose();
        }}
      >
        Cancelar edición
      </button>
    </section>
  );
}
function MoveFields({
  state: initialState,
  onAccessFailure,
  onConfirmed,
  eligible,
  focusFallback,
  focusRecovery,
}: {
  state: BlockState;
  onAccessFailure: Props["onAccessFailure"];
  onConfirmed: Props["onConfirmed"];
  eligible: boolean;
  focusFallback: () => void;
  focusRecovery: () => void;
}) {
  const [state, setState] = useState(initialState);
  const [currentFailed, setCurrentFailed] = useState(false);
  const [currentLoading, setCurrentLoading] = useState(false);
  useLayoutEffect(() => {
    if (currentLoading && document.activeElement === document.body)
      focusRecovery();
  }, [currentLoading, focusRecovery]);
  const [previewConflict, setPreviewConflict] = useState(false);
  const [rejection, setRejection] = useState<ChangeError>();
  const currentRequest = useRef<AbortController | null>(null);
  useEffect(() => () => currentRequest.current?.abort(), []);
  const pendingPreview = useRef<AbortController | null>(null);
  const [reviewing, setReviewing] = useState(false);
  useEffect(() => () => pendingPreview.current?.abort(), []);
  const [preview, setPreview] = useState<BlockPreview>();
  const [previewFailed, setPreviewFailed] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<BlockFieldError[]>([]);
  const errorId = useId();
  const startError = fieldErrors.find((error) => error.field === "startLocal");
  const endError = fieldErrors.find((error) => error.field === "endLocal");
  const zoneError = fieldErrors.find((error) => error.field === "zoneId");
  const [offsetChoices, setOffsetChoices] = useState<
    Partial<Record<"startOffset" | "endOffset", string[]>>
  >({});
  const [offsets, setOffsets] = useState<{
    startOffset: string | null;
    endOffset: string | null;
  }>({ startOffset: null, endOffset: null });
  const [allowOverBudget, setAllowOverBudget] = useState(false);
  const [locked, setLocked] = useState(false);
  const [previousEligibility, setPreviousEligibility] = useState(eligible);
  useLayoutEffect(() => {
    if (!eligible && !locked) pendingPreview.current?.abort();
  }, [eligible, locked]);
  if (eligible !== previousEligibility) {
    setPreviousEligibility(eligible);
    if (!eligible && !locked) {
      setReviewing(false);
      setPreview(undefined);
      setAllowOverBudget(false);
    }
  }
  const unknownZone = !localTime(state.block.startAt, state.block.zoneId);
  const [zoneId, setZoneId] = useState(unknownZone ? "" : state.block.zoneId);
  const [zones, setZones] = useState<string[]>([]);
  const zoneListId = useId();
  const zonesRequest = useRef<AbortController | null>(null);
  useEffect(() => () => zonesRequest.current?.abort(), []);
  const [zonesFailed, setZonesFailed] = useState(false);
  function loadZones() {
    if (zonesRequest.current) return;
    const controller = new AbortController();
    zonesRequest.current = controller;
    setZonesFailed(false);
    void readAvailabilityZones(controller.signal)
      .then((value) => {
        if (!controller.signal.aborted) setZones(value);
      })
      .catch(() => {
        if (controller.signal.aborted) return;
        setZonesFailed(true);
        zonesRequest.current = null;
      });
  }
  const [start, setStart] = useState(
    localTime(state.block.startAt, state.block.zoneId),
  );
  const [end, setEnd] = useState(
    localTime(state.block.endAt, state.block.zoneId),
  );
  function invalidate() {
    pendingPreview.current?.abort();
    setReviewing(false);
    setPreview(undefined);
    setAllowOverBudget(false);
  }
  function loadCurrent() {
    invalidate();
    setPreviewConflict(false);
    setLocked(true);
    setCurrentFailed(false);
    setCurrentLoading(true);
    const controller = new AbortController();
    currentRequest.current = controller;
    void readBlockState(
      state.block.projectId,
      state.block.taskId,
      state.block.id,
      controller.signal,
    )
      .then((value) => {
        if (controller.signal.aborted) return;
        setState(value);
        setCurrentLoading(false);
        setLocked(false);
      })
      .catch(async (error: unknown) => {
        if (controller.signal.aborted) return;
        const problem = await readChangeError(error);
        if (controller.signal.aborted) return;
        if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure(404);
        setCurrentFailed(true);
        setCurrentLoading(false);
      });
  }
  function review() {
    if (
      state.status === "cancelled" ||
      !eligible ||
      locked ||
      reviewing ||
      !start ||
      !end ||
      !zoneId
    )
      return;
    invalidate();
    const controller = new AbortController();
    pendingPreview.current = controller;
    setReviewing(true);
    setPreviewFailed(false);
    setRejection(undefined);
    void previewMove(
      state,
      {
        startLocal: start,
        endLocal: end,
        zoneId,
        ...offsets,
      },
      controller.signal,
    )
      .then((value) => {
        if (!controller.signal.aborted) setPreview(value);
      })
      .catch(async (error: unknown) => {
        if (controller.signal.aborted) return;
        const problem = await readChangeError(error);
        if (controller.signal.aborted) return;
        if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure(404);
        if (problem?.code === "BLOCK_CONFLICT") setPreviewConflict(true);
        if (problem?.code === "VALIDATION_ERROR" && problem.validOffsets) {
          setOffsetChoices((previous) => ({
            ...previous,
            ...problem.validOffsets,
          }));
        }
        if (problem?.code === "VALIDATION_ERROR")
          setFieldErrors(problem.errors);
        setPreviewFailed(true);
      })
      .finally(() => {
        if (!controller.signal.aborted && pendingPreview.current === controller)
          setReviewing(false);
      });
  }
  return (
    <form
      className="task-form"
      aria-label="Destino del movimiento"
      onSubmit={(event) => {
        event.preventDefault();
        review();
      }}
    >
      <p>{state.block.objective}</p>
      {state.status === "cancelled" && (
        <p role="status">Este bloque está cancelado.</p>
      )}
      {unknownZone && (
        <p>Elige una zona resoluble e introduce las horas del nuevo destino.</p>
      )}
      <div className="field">
        <label htmlFor={errorId + "-zone-input"}>Zona del movimiento</label>
        <input
          id={errorId + "-zone-input"}
          readOnly={locked}
          value={zoneId}
          aria-invalid={zoneError ? true : undefined}
          aria-describedby={zoneError ? errorId + "-zone" : undefined}
          list={zoneListId}
          onFocus={loadZones}
          onChange={(event) => {
            if (locked) return;
            invalidate();
            setZoneId(event.target.value);
            setFieldErrors((previous) =>
              previous.filter(
                (error) =>
                  !["zoneId", "startOffset", "endOffset"].includes(error.field),
              ),
            );
            setOffsets({ startOffset: null, endOffset: null });
            setOffsetChoices({});
          }}
        />
      </div>
      {zoneError && <p id={errorId + "-zone"}>{zoneError.message}</p>}
      <datalist id={zoneListId}>
        {zones.map((zone) => (
          <option key={zone} value={zone} />
        ))}
      </datalist>
      {zonesFailed && (
        <>
          <p role="alert">No se pudieron consultar las sugerencias de zonas.</p>
          <button type="button" onClick={loadZones}>
            Reintentar sugerencias de zonas
          </button>
        </>
      )}
      <div className="field">
        <label htmlFor={errorId + "-start-input"}>Inicio local</label>
        <input
          id={errorId + "-start-input"}
          type="datetime-local"
          readOnly={locked}
          value={start}
          aria-invalid={startError ? true : undefined}
          aria-describedby={startError ? errorId + "-start" : undefined}
          onChange={(e) => {
            if (locked) return;
            invalidate();
            setStart(e.target.value);
            setFieldErrors((previous) =>
              previous.filter(
                (error) =>
                  error.field !== "startLocal" && error.field !== "startOffset",
              ),
            );
            setOffsets((previous) => ({ ...previous, startOffset: null }));
            setOffsetChoices((previous) => ({
              ...previous,
              startOffset: undefined,
            }));
          }}
        />
      </div>
      {startError && <p id={errorId + "-start"}>{startError.message}</p>}
      <div className="field">
        <label htmlFor={errorId + "-end-input"}>Fin local</label>
        <input
          id={errorId + "-end-input"}
          type="datetime-local"
          readOnly={locked}
          value={end}
          aria-invalid={endError ? true : undefined}
          aria-describedby={endError ? errorId + "-end" : undefined}
          onChange={(e) => {
            if (locked) return;
            invalidate();
            setEnd(e.target.value);
            setFieldErrors((previous) =>
              previous.filter(
                (error) =>
                  error.field !== "endLocal" && error.field !== "endOffset",
              ),
            );
            setOffsets((previous) => ({ ...previous, endOffset: null }));
            setOffsetChoices((previous) => ({
              ...previous,
              endOffset: undefined,
            }));
          }}
        />
      </div>
      {endError && <p id={errorId + "-end"}>{endError.message}</p>}
      <button
        type="submit"
        aria-disabled={
          state.status === "cancelled" ||
          !eligible ||
          locked ||
          reviewing ||
          !start ||
          !end ||
          !zoneId
        }
      >
        Revisar movimiento
      </button>
      {previewFailed && <p role="alert">No se pudo revisar el movimiento.</p>}
      {reviewing && <p role="status">Revisando movimiento</p>}
      {currentLoading && <p role="status">Consultando estado actual</p>}
      {rejection?.code === "BUDGET_EXCEEDED" && (
        <p role="alert">
          El presupuesto cambió. Revisa el movimiento antes de decidir.
        </p>
      )}
      {rejection?.code === "BLOCK_UNCHANGED" && (
        <p role="alert">
          El destino no cambia el bloque. Corrige el borrador y vuelve a
          revisar.
        </p>
      )}
      {rejection &&
        rejection.code !== "BLOCK_UNCHANGED" &&
        rejection.code !== "BUDGET_EXCEEDED" && (
          <p role="alert">
            El cambio fue rechazado. Corrige el borrador y vuelve a revisar.
          </p>
        )}
      {(currentFailed || previewConflict) && (
        <>
          <p role="alert">
            {previewConflict
              ? "El bloque ha cambiado. Consulta su estado actual."
              : "No se pudo consultar el estado actual del bloque."}
          </p>
          <button type="button" onClick={loadCurrent}>
            Consultar estado actual
          </button>
        </>
      )}
      {(["startOffset", "endOffset"] as const).map(
        (field) =>
          offsetChoices[field] && (
            <div className="field" key={field}>
              <label htmlFor={errorId + field}>
                {field === "startOffset"
                  ? "Ocurrencia de inicio"
                  : "Ocurrencia de fin"}
              </label>
              <select
                id={errorId + field}
                disabled={locked}
                value={offsets[field] ?? ""}
                aria-invalid={
                  fieldErrors.some((error) => error.field === field)
                    ? true
                    : undefined
                }
                aria-describedby={
                  fieldErrors.some((error) => error.field === field)
                    ? errorId + field + "-error"
                    : undefined
                }
                onChange={(event) => {
                  if (locked) return;
                  invalidate();
                  setFieldErrors((previous) =>
                    previous.filter((error) => error.field !== field),
                  );
                  setOffsets((previous) => ({
                    ...previous,
                    [field]: event.target.value || null,
                  }));
                }}
              >
                <option value="">Elige una ocurrencia</option>
                {offsetChoices[field]!.map((offset) => (
                  <option key={offset} value={offset}>
                    {offset}
                  </option>
                ))}
              </select>
              {fieldErrors.find((error) => error.field === field) && (
                <p id={errorId + field + "-error"}>
                  {fieldErrors.find((error) => error.field === field)!.message}
                </p>
              )}
            </div>
          ),
      )}
      {preview && (eligible || locked) && (
        <section aria-label="Revisión del movimiento">
          <h4>Antes</h4>
          <BlockDetails block={state.block} />
          <h4>Después</h4>
          <BlockDetails block={{ ...state.block, ...preview }} />
          <p>Zona del presupuesto: {preview.budgetZoneId}</p>
          {preview.days.map((day) => (
            <p key={day.date}>
              {day.date}: presupuesto {day.budgetMinutes} minutos, reservado{" "}
              {day.plannedSeconds} segundos, solicitado {day.requestedSeconds}{" "}
              segundos, exceso {day.excessSeconds} segundos.
            </p>
          ))}
          {preview.days.some((day) => day.excessSeconds > 0) && (
            <label>
              <input
                type="checkbox"
                disabled={locked}
                checked={allowOverBudget}
                onChange={(event) => {
                  if (!locked) setAllowOverBudget(event.target.checked);
                }}
              />
              Acepto superar el presupuesto para este movimiento. Esto no
              permite solapes.
            </label>
          )}
          <ChangeSubmit
            state={state}
            onAccessFailure={onAccessFailure}
            onRetained={() => setLocked(true)}
            onRejected={(issue) => {
              invalidate();
              setLocked(false);
              setRejection(issue);
              if (issue.code === "VALIDATION_ERROR") {
                setFieldErrors(issue.errors);
                if (issue.validOffsets)
                  setOffsetChoices((previous) => ({
                    ...previous,
                    ...issue.validOffsets,
                  }));
              }
            }}
            onConfirmed={onConfirmed}
            focusFallback={focusFallback}
            onReload={loadCurrent}
            blocked={
              preview.days.some((day) => day.excessSeconds > 0) &&
              !allowOverBudget
            }
            movement={{
              preview,
              input: {
                startLocal: start,
                endLocal: end,
                zoneId: preview.zoneId,
                startOffset: preview.startOffset,
                endOffset: preview.endOffset,
                allowOverBudget,
              },
            }}
          />
        </section>
      )}
    </form>
  );
}
function localTime(value: string, zoneId: string) {
  try {
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
  } catch {
    return "";
  }
}
