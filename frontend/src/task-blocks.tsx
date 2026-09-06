import { BlockDetails, BlockTime } from "./block-details";
import type { BlockChange } from "./reschedule-api";
import { BlockActions } from "./reschedule-block";
import { BlockConfirmation } from "./block-confirmation";
import { BlockChangeHistory } from "./reschedule-history";
import { useEffect, useState, useRef, useLayoutEffect } from "react";
import { readAvailability, readAvailabilityZones } from "./availability-api";
import { RouteLink } from "./navigation";
import {
  previewBlock,
  createBlock,
  readBlocks,
  readBlock,
  readBlockByRequest,
  readBlockError,
  type BlockPreview,
  type Block,
  type BlockPage,
  type RetainedBlockRequest,
  type BlockError,
} from "./schedule-block-api";
export function TaskBlocks({
  projectId,
  taskId,
  taskStatus,
  projectStatus,
  onAccessFailure,
}: {
  projectId: string;
  taskId: string;
  taskStatus?: string;
  projectStatus?: string;
  onAccessFailure: (status: number) => void;
}) {
  const [selected, setSelected] = useState<{
    id: string;
    mode: "move" | "cancel";
  }>();
  const [change, setChange] = useState<BlockChange>();
  const [showChanges, setShowChanges] = useState(false);
  const [changeRevision, setChangeRevision] = useState(0);
  const [editing, setEditing] = useState(false);
  const [confirmed, setConfirmed] = useState<Block>();
  const [page, setPage] = useState<BlockPage>();
  const [cursor, setCursor] = useState<string>();
  const [listFailure, setListFailure] = useState(false);
  const [listRevision, setListRevision] = useState(0);
  const heading = useRef<HTMLHeadingElement>(null);
  function reload() {
    setPage(undefined);
    setListFailure(false);
    setListRevision((value) => value + 1);
  }
  useEffect(() => {
    const controller = new AbortController();
    void readBlocks(projectId, taskId, cursor, controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) {
          setPage(result);
          setListFailure(false);
        }
      })
      .catch(async (error: unknown) => {
        if (controller.signal.aborted) return;
        const problem = await readBlockError(error);
        if (controller.signal.aborted) return;
        if (problem?.code === "RESOURCE_NOT_FOUND") onAccessFailure(404);
        else setListFailure(true);
      });
    return () => controller.abort();
  }, [projectId, taskId, cursor, listRevision, onAccessFailure]);
  return (
    <section className="task-blocks" aria-label="Bloques planificados">
      <h2 ref={heading} tabIndex={-1}>
        Bloques planificados
      </h2>
      <p>Los bloques son tiempo planificado, no trabajo realizado.</p>
      {listFailure ? (
        <div>
          <p role="alert">No se pudieron consultar los bloques.</p>
          <button onClick={reload}>Reintentar bloques</button>
        </div>
      ) : !page ? (
        <p role="status">Consultando bloques</p>
      ) : page.items.length === 0 ? (
        <p>Todavía no hay bloques planificados para esta tarea.</p>
      ) : null}
      {page && (
        <>
          <ul className="task-list" aria-label="Bloques planificados">
            {page.items.map((item) => (
              <li key={item.id}>
                <h3>{item.objective}</h3>
                <BlockDetails block={item} />
                {!editing &&
                  !selected &&
                  taskStatus === "pending" &&
                  projectStatus &&
                  projectStatus !== "completed" && (
                    <button
                      onClick={() => setSelected({ id: item.id, mode: "move" })}
                      aria-label={"Mover bloque: " + item.objective}
                    >
                      Mover bloque
                    </button>
                  )}
                {!editing && !selected && (
                  <button
                    onClick={() => setSelected({ id: item.id, mode: "cancel" })}
                    aria-label={"Cancelar bloque: " + item.objective}
                  >
                    Cancelar bloque
                  </button>
                )}
              </li>
            ))}
          </ul>
          {page.nextCursor && (
            <button
              onClick={() => {
                setPage(undefined);
                setCursor(page.nextCursor!);
              }}
            >
              Ver bloques anteriores
            </button>
          )}
          {cursor && (
            <button
              onClick={() => {
                setPage(undefined);
                setCursor(undefined);
              }}
            >
              Volver a bloques recientes
            </button>
          )}
        </>
      )}
      {selected && (
        <BlockActions
          key={selected.id + selected.mode}
          projectId={projectId}
          taskId={taskId}
          blockId={selected.id}
          mode={selected.mode}
          onAccessFailure={onAccessFailure}
          eligible={
            taskStatus === "pending" &&
            !!projectStatus &&
            projectStatus !== "completed"
          }
          onClose={() => setSelected(undefined)}
          onConfirmed={(value) => {
            setConfirmed(undefined);
            setChange(value);
            setChangeRevision((revision) => revision + 1);
            setSelected(undefined);
            setCursor(undefined);
            reload();
          }}
          focusFallback={() => heading.current?.focus()}
        />
      )}
      {change && (
        <BlockConfirmation
          block={change.before}
          change={change}
          onAccessFailure={onAccessFailure}
        />
      )}
      {confirmed && (
        <>
          <BlockConfirmation
            block={confirmed}
            onAccessFailure={onAccessFailure}
          />
          <p>{confirmed.id}</p>
        </>
      )}
      {!editing &&
        !selected &&
        taskStatus === "pending" &&
        projectStatus &&
        projectStatus !== "completed" && (
          <button onClick={() => setEditing(true)}>Planificar bloque</button>
        )}
      {editing && (
        <BlockEditor
          focusFallback={() => heading.current?.focus()}
          eligible={
            taskStatus === "pending" &&
            !!projectStatus &&
            projectStatus !== "completed"
          }
          projectId={projectId}
          taskId={taskId}
          onAccessFailure={onAccessFailure}
          onCancel={() => setEditing(false)}
          onConfirmed={(block) => {
            setChange(undefined);
            setConfirmed(block);
            setEditing(false);
            setCursor(undefined);
            reload();
          }}
        />
      )}
      {showChanges ? (
        <BlockChangeHistory
          projectId={projectId}
          taskId={taskId}
          onAccessFailure={onAccessFailure}
          refreshToken={changeRevision}
        />
      ) : (
        <button type="button" onClick={() => setShowChanges(true)}>
          Ver cambios de bloques
        </button>
      )}
    </section>
  );
}
function BlockEditor({
  focusFallback,
  eligible,
  projectId,
  taskId,
  onConfirmed,
  onCancel,
  onAccessFailure,
}: {
  focusFallback: () => void;
  eligible: boolean;
  projectId: string;
  taskId: string;
  onConfirmed: (block: Block) => void;
  onCancel: () => void;
  onAccessFailure: (status: number) => void;
}) {
  const [objective, setObjective] = useState("");
  const [startLocal, setStartLocal] = useState("");
  const [endLocal, setEndLocal] = useState("");
  const [startOffset, setStartOffset] = useState<string | null>(null);
  const [endOffset, setEndOffset] = useState<string | null>(null);
  const [offsetChoices, setOffsetChoices] = useState<
    Partial<Record<"startOffset" | "endOffset", string[]>>
  >({});
  const [zoneId, setZoneId] = useState("");
  const [zones, setZones] = useState<string[]>([]);
  const [configurationFailure, setConfigurationFailure] = useState(false);
  const [configurationRevision, setConfigurationRevision] = useState(0);
  const [review, setReview] = useState<BlockPreview>();
  const [reviewing, setReviewing] = useState(false);
  const [reviewFailure, setReviewFailure] = useState(false);
  const [allowOverBudget, setAllowOverBudget] = useState(false);
  const reviewRequest = useRef<AbortController | null>(null);
  const [saving, setSaving] = useState(false);
  const [request, setRequest] = useState<RetainedBlockRequest>();
  const [uncertain, setUncertain] = useState(false);
  const [mayResend, setMayResend] = useState(false);
  const [saveFailure, setSaveFailure] = useState(false);
  const [csrfRejected, setCsrfRejected] = useState(false);
  const [issue, setIssue] = useState<BlockError | null>(null);
  const fieldError = (field: string) =>
    issue?.code === "VALIDATION_ERROR"
      ? issue.errors.find((error) => error.field === field)?.message
      : undefined;
  const objectiveError = fieldError("objective");
  const form = useRef<HTMLFormElement>(null);
  const origin = useRef<Element | null>(null);
  useLayoutEffect(() => {
    if (saving || reviewing || !origin.current) return;
    const previous = origin.current;
    origin.current = null;
    if (
      document.activeElement !== document.body &&
      document.activeElement !== previous
    )
      return;
    const field =
      issue?.code === "VALIDATION_ERROR" &&
      issue.errors.find((error) =>
        form.current?.elements.namedItem(error.field),
      );
    const element = field && form.current?.elements.namedItem(field.field);
    if (element instanceof HTMLElement) element.focus();
    else if (
      previous instanceof HTMLElement &&
      previous.isConnected &&
      !previous.matches(":disabled")
    )
      previous.focus();
    else focusFallback();
  }, [issue, saving, reviewing, focusFallback]);
  const writeRequest = useRef<AbortController | null>(null);
  function confirm(block: Block) {
    if (
      document.activeElement === document.body ||
      document.activeElement === origin.current
    )
      focusFallback();
    onConfirmed(block);
  }
  async function classify(error: unknown, signal: AbortSignal) {
    const problem = await readBlockError(error);
    if (!signal.aborted && problem?.code === "RESOURCE_NOT_FOUND")
      onAccessFailure(404);
    return problem;
  }
  useEffect(
    () => () => {
      reviewRequest.current?.abort();
      writeRequest.current?.abort();
    },
    [],
  );
  async function check() {
    if (!request || !review || saving) return;
    origin.current = document.activeElement;
    setSaving(true);
    const controller = new AbortController();
    writeRequest.current = controller;
    try {
      const result = await readBlockByRequest(
        projectId,
        taskId,
        request.key,
        review,
        controller.signal,
      );
      if (!controller.signal.aborted) confirm(result);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await classify(error, controller.signal);
      if (controller.signal.aborted) return;
      setUncertain(true);
      setMayResend(problem?.code === "BLOCK_NOT_FOUND");
    } finally {
      if (!controller.signal.aborted) setSaving(false);
    }
  }
  async function save() {
    if (!review || saving || (!eligible && !request)) return;
    const retained = request ?? {
      input: {
        objective: review.objective,
        startLocal,
        endLocal,
        zoneId,
        startOffset: review.startOffset,
        endOffset: review.endOffset,
        allowOverBudget,
      },
      key: crypto.randomUUID(),
      availabilityRevision: review.availabilityEtag,
    };
    origin.current = document.activeElement;
    setRequest(retained);
    setSaveFailure(false);
    setCsrfRejected(false);
    setSaving(true);
    const controller = new AbortController();
    writeRequest.current = controller;
    try {
      const result = await createBlock(
        projectId,
        taskId,
        retained,
        review,
        controller.signal,
      );
      if (!controller.signal.aborted) confirm(result);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await classify(error, controller.signal);
      if (controller.signal.aborted) return;
      setIssue(problem);
      if (problem?.code === "CSRF_INVALID") {
        setCsrfRejected(true);
        setMayResend(true);
        setUncertain(false);
      } else if (
        problem &&
        [
          "VALIDATION_ERROR",
          "BUDGET_EXCEEDED",
          "BLOCK_OVERLAP",
          "AVAILABILITY_CONFLICT",
          "PRECONDITION_REQUIRED",
          "PROJECT_COMPLETED",
          "TASK_COMPLETED",
          "AVAILABILITY_REQUIRED",
          "AVAILABILITY_ZONE_UNAVAILABLE",
        ].includes(problem.code)
      ) {
        invalidate();
        setRequest(undefined);
        setUncertain(false);
        setMayResend(false);
        setSaveFailure(true);
      } else setUncertain(true);
    } finally {
      if (!controller.signal.aborted) setSaving(false);
    }
  }
  function invalidate() {
    reviewRequest.current?.abort();
    setReview(undefined);
    setAllowOverBudget(false);
    setReviewing(false);
  }
  async function inspect() {
    origin.current = document.activeElement;
    if (!eligible || reviewing) return;
    const controller = new AbortController();
    reviewRequest.current = controller;
    setReview(undefined);
    setReviewFailure(false);
    setIssue(null);
    setReviewing(true);
    try {
      const result = await previewBlock(
        projectId,
        taskId,
        {
          objective,
          startLocal,
          endLocal,
          zoneId,
          startOffset,
          endOffset,
        },
        controller.signal,
      );
      if (controller.signal.aborted) return;
      setReview(result);
    } catch (error) {
      if (controller.signal.aborted) return;
      const problem = await classify(error, controller.signal);
      if (controller.signal.aborted) return;
      setIssue(problem);
      if (problem?.code === "VALIDATION_ERROR" && problem.validOffsets)
        setOffsetChoices((current) => ({
          ...current,
          ...problem.validOffsets,
        }));
      setReviewFailure(true);
    } finally {
      if (!controller.signal.aborted) setReviewing(false);
    }
  }
  useEffect(() => {
    const controller = new AbortController();
    void Promise.all([
      readAvailability(controller.signal),
      readAvailabilityZones(controller.signal),
    ])
      .then(([availability, catalog]) => {
        if (controller.signal.aborted) return;
        setZones(catalog);
        setZoneId(availability.configured ? availability.zoneId : "");
        setConfigurationFailure(false);
      })
      .catch(() => {
        if (!controller.signal.aborted) setConfigurationFailure(true);
      });
    return () => controller.abort();
  }, [configurationRevision]);
  return (
    <form
      className="task-form"
      ref={form}
      noValidate
      onSubmit={(event) => {
        event.preventDefault();
        void inspect();
      }}
    >
      <fieldset disabled={saving || uncertain || csrfRejected}>
        {configurationFailure && (
          <>
            <p role="alert">
              No se pudo consultar la configuración del bloque.
            </p>
            <button
              type="button"
              onClick={() => {
                setConfigurationFailure(false);
                setConfigurationRevision((value) => value + 1);
              }}
            >
              Reintentar configuración
            </button>
          </>
        )}
        <div className="field">
          <label htmlFor="block-objective">Objetivo del bloque</label>
          <textarea
            id="block-objective"
            name="objective"
            aria-invalid={objectiveError ? true : undefined}
            aria-describedby={
              objectiveError ? "block-objective-error" : undefined
            }
            value={objective}
            onChange={(event) => {
              invalidate();
              setObjective(event.target.value);
            }}
          />
          {objectiveError && (
            <p id="block-objective-error" className="field-error">
              {objectiveError}
            </p>
          )}
        </div>
        <div className="field">
          <label htmlFor="block-start">Inicio del bloque</label>
          <input
            id="block-start"
            name="startLocal"
            aria-invalid={fieldError("startLocal") ? true : undefined}
            aria-describedby={
              fieldError("startLocal") ? "block-start-error" : undefined
            }
            type="datetime-local"
            step="60"
            value={startLocal}
            onChange={(event) => {
              invalidate();
              setStartLocal(event.target.value);
              setStartOffset(null);
              setOffsetChoices((current) => ({
                ...current,
                startOffset: undefined,
              }));
            }}
          />
          {fieldError("startLocal") && (
            <p className="field-error" id="block-start-error">
              {fieldError("startLocal")}
            </p>
          )}
        </div>
        <div className="field">
          <label htmlFor="block-end">Fin del bloque</label>
          <input
            id="block-end"
            name="endLocal"
            aria-invalid={fieldError("endLocal") ? true : undefined}
            aria-describedby={
              fieldError("endLocal") ? "block-end-error" : undefined
            }
            type="datetime-local"
            step="60"
            value={endLocal}
            onChange={(event) => {
              invalidate();
              setEndLocal(event.target.value);
              setEndOffset(null);
              setOffsetChoices((current) => ({
                ...current,
                endOffset: undefined,
              }));
            }}
          />
          {fieldError("endLocal") && (
            <p className="field-error" id="block-end-error">
              {fieldError("endLocal")}
            </p>
          )}
        </div>
        <div className="field">
          <label htmlFor="block-zone">Zona del bloque</label>
          <select
            id="block-zone"
            name="zoneId"
            aria-invalid={fieldError("zoneId") ? true : undefined}
            aria-describedby={
              fieldError("zoneId") ? "block-zone-error" : undefined
            }
            value={zoneId}
            onChange={(event) => {
              invalidate();
              setZoneId(event.target.value);
              setStartOffset(null);
              setEndOffset(null);
              setOffsetChoices({});
            }}
          >
            <option value="">Selecciona una zona</option>
            {zones.map((zone) => (
              <option key={zone}>{zone}</option>
            ))}
          </select>
          {fieldError("zoneId") && (
            <p className="field-error" id="block-zone-error">
              {fieldError("zoneId")}
            </p>
          )}
        </div>
        {(["startOffset", "endOffset"] as const).map(
          (field) =>
            offsetChoices[field] && (
              <div className="field" key={field}>
                <label htmlFor={`block-${field}`}>
                  {field === "startOffset"
                    ? "Ocurrencia de inicio"
                    : "Ocurrencia de fin"}
                </label>
                <select
                  id={`block-${field}`}
                  name={field}
                  aria-invalid={fieldError(field) ? true : undefined}
                  aria-describedby={
                    fieldError(field) ? `block-${field}-error` : undefined
                  }
                  value={
                    (field === "startOffset" ? startOffset : endOffset) ?? ""
                  }
                  onChange={(event) => {
                    invalidate();
                    (field === "startOffset" ? setStartOffset : setEndOffset)(
                      event.target.value || null,
                    );
                  }}
                >
                  <option value="">Elige una ocurrencia</option>
                  {offsetChoices[field]!.map((offset) => (
                    <option key={offset} value={offset}>
                      UTC{offset === "Z" ? "+00:00" : offset}
                    </option>
                  ))}
                </select>
                {fieldError(field) && (
                  <p className="field-error" id={`block-${field}-error`}>
                    {fieldError(field)}
                  </p>
                )}
              </div>
            ),
        )}
        <button className="secondary-link" disabled={reviewing || !eligible}>
          Revisar bloque
        </button>
        <button
          type="button"
          onClick={() => void save()}
          disabled={
            !eligible ||
            !review ||
            (review.days.some((day) => day.excessSeconds > 0) &&
              !allowOverBudget)
          }
        >
          Guardar bloque
        </button>
        {reviewing && <p role="status">Revisando bloque</p>}
        {saving && <p role="status">Guardando bloque</p>}
        {reviewFailure && (
          <p role="alert">
            No se pudo revisar el bloque. Conservamos tus datos.
          </p>
        )}
        {saveFailure && (
          <p role="alert">
            No se guardó el bloque. Revisa los datos antes de volver a guardar.
          </p>
        )}
        {review && (
          <section className="block-review" aria-label="Revisión del bloque">
            <h3>Revisión del bloque</h3>
            <p>{review.objective}</p>
            <p>{review.durationMinutes} minutos</p>
            <p>Zona del bloque: {review.zoneId}</p>
            <p>Zona del presupuesto: {review.budgetZoneId}</p>
            <p>
              Inicio:{" "}
              <BlockTime value={review.startAt} zoneId={review.zoneId} />
            </p>
            <p>Desfase de inicio: {review.startOffset}</p>
            <p>
              Fin: <BlockTime value={review.endAt} zoneId={review.zoneId} />
            </p>
            <p>Desfase de fin: {review.endOffset}</p>
            {review.days.map((day) => (
              <div key={day.date}>
                <h4>{day.date}</h4>
                <p>Presupuesto: {day.budgetMinutes} minutos</p>
                <p>Reservado: {day.plannedSeconds} segundos</p>
                <p>Solicitado: {day.requestedSeconds} segundos</p>
                <p>Exceso: {day.excessSeconds} segundos</p>
              </div>
            ))}
            {review.days.some((day) => day.excessSeconds > 0) && (
              <label>
                <input
                  type="checkbox"
                  checked={allowOverBudget}
                  onChange={(event) => setAllowOverBudget(event.target.checked)}
                />
                Acepto superar el presupuesto para este bloque, aunque otras
                reservas aumenten el exceso antes de guardar. Esto no permite
                solapes.
              </label>
            )}
          </section>
        )}
      </fieldset>
      {issue?.code === "BLOCK_OVERLAP" && (
        <BlockConflict key={issue.conflict.id} conflict={issue.conflict} />
      )}
      {issue?.code === "BUDGET_EXCEEDED" && (
        <div>
          <p role="alert">
            El presupuesto cambió. Revisa este bloque de nuevo antes de decidir.
          </p>
          <p>Zona del presupuesto: {issue.budgetZoneId}</p>
          {issue.days.map((day) => (
            <p key={day.date}>
              {day.date}: presupuesto {day.budgetMinutes} minutos, reservado{" "}
              {day.plannedSeconds} segundos, solicitado {day.requestedSeconds}{" "}
              segundos, exceso {day.excessSeconds} segundos.
            </p>
          ))}
        </div>
      )}
      {(issue?.code === "AVAILABILITY_REQUIRED" ||
        issue?.code === "AVAILABILITY_ZONE_UNAVAILABLE") && (
        <div>
          <p>Revisa tu disponibilidad y su zona antes de planificar.</p>
          <p>Salir para configurar disponibilidad descarta este borrador.</p>
          <RouteLink href="/disponibilidad">
            Configurar disponibilidad
          </RouteLink>
        </div>
      )}
      <p>
        Los cambios sin guardar se pierden al cerrar. Cerrar no anula una
        petición enviada; podrás consultar los bloques después.
      </p>
      <button className="secondary-link" type="button" onClick={onCancel}>
        Cancelar bloque
      </button>
      {(uncertain || csrfRejected) && (
        <div>
          <p role="alert">
            {csrfRejected
              ? "La protección de sesión rechazó este envío. Después de recuperar acceso, puedes reenviar el mismo bloque."
              : "No podemos confirmar el guardado. Conservamos este bloque y su identificación para comprobarlo sin duplicarlo."}
          </p>
          {uncertain && (
            <button
              type="button"
              disabled={saving}
              onClick={() => void check()}
            >
              Comprobar guardado
            </button>
          )}
          {mayResend && (
            <button type="button" disabled={saving} onClick={() => void save()}>
              Reenviar el mismo bloque
            </button>
          )}
        </div>
      )}
    </form>
  );
}

function BlockConflict({
  conflict,
}: {
  conflict: { id: string; projectId: string; taskId: string };
}) {
  const [block, setBlock] = useState<Block>();
  const [loading, setLoading] = useState(false);
  const [failed, setFailed] = useState(false);
  const request = useRef<AbortController | null>(null);
  useEffect(() => () => request.current?.abort(), []);
  async function consult() {
    const controller = new AbortController();
    request.current = controller;
    setBlock(undefined);
    setLoading(true);
    setFailed(false);
    try {
      const result = await readBlock(
        conflict.projectId,
        conflict.taskId,
        conflict.id,
        controller.signal,
      );
      if (!controller.signal.aborted) setBlock(result);
    } catch {
      if (!controller.signal.aborted) setFailed(true);
    } finally {
      if (!controller.signal.aborted) setLoading(false);
    }
  }
  return (
    <section aria-label="Bloque en conflicto">
      <p>
        Este horario se solapa con otro bloque. Cambia las horas antes de
        revisar de nuevo.
      </p>
      <button type="button" disabled={loading} onClick={() => void consult()}>
        Consultar bloque en conflicto
      </button>
      {loading && <p role="status">Consultando bloque en conflicto</p>}
      {failed && (
        <p role="alert">
          No se pudo consultar el bloque en conflicto. Puedes volver a
          intentarlo.
        </p>
      )}
      {block && (
        <>
          <h3>{block.objective}</h3>
          <BlockDetails block={block} />
        </>
      )}
    </section>
  );
}
