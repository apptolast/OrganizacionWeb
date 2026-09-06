import { apiRequest } from "./api-client";
import {
  exact,
  instant,
  isBlock,
  isPreview,
  localMatches,
  readBlockError,
  sameId,
  text,
  uuid,
  type Block,
  type BlockError,
  type BlockPreview,
} from "./schedule-block-api";
export type BlockState = {
  block: Block;
  status: "planned" | "cancelled";
  updatedAt: string;
  revision: string;
};
const collection = (projectId: string, taskId: string) =>
  `/api/v1/projects/${projectId}/tasks/${taskId}/blocks`;
export async function readBlockState(
  projectId: string,
  taskId: string,
  blockId: string,
  signal?: AbortSignal,
): Promise<BlockState> {
  const response = await apiRequest(
    `${collection(projectId, taskId)}/${blockId}/state`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const revision = response.headers.get("ETag");
  const data: unknown = await response.json();
  if (
    !isState(data, projectId, taskId, blockId) ||
    !isRevision(revision, blockId)
  )
    throw new Error("Estado de bloque inválido");
  return { ...data, revision };
}
function isState(
  value: unknown,
  projectId: string,
  taskId: string,
  blockId: string,
): value is Omit<BlockState, "revision"> {
  return (
    exact(value, "block status updatedAt") &&
    isBlock(value.block, projectId, taskId) &&
    sameId(value.block.id, blockId) &&
    (value.status === "planned" || value.status === "cancelled") &&
    instant(value.updatedAt)
  );
}
const CHANGES_PER_PAGE = 20;
const MAX_BLOCK_VERSION = 9223372036854775807n;
function isRevision(value: unknown, blockId: string): value is string {
  const match =
    typeof value === "string" &&
    /^"block:([\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}):([1-9]\d*)"$/i.exec(
      value,
    );
  return Boolean(
    match && sameId(match[1], blockId) && BigInt(match[2]) <= MAX_BLOCK_VERSION,
  );
}
export type RescheduleInput = {
  startLocal: string;
  endLocal: string;
  zoneId: string;
  startOffset: string | null;
  endOffset: string | null;
};
export async function previewReschedule(
  projectId: string,
  taskId: string,
  state: BlockState,
  input: RescheduleInput,
  signal?: AbortSignal,
): Promise<BlockPreview> {
  const sent = { ...input, objective: state.block.objective };
  const revision = state.revision;
  const response = await apiRequest(
    `${collection(projectId, taskId)}/${state.block.id}/reschedule/preview`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": state.revision,
      },
      body: JSON.stringify(input),
    },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    response.headers.get("ETag") !== revision ||
    !isPreview(data, sent)
  )
    throw new Error("Revisión de movimiento inválida");
  return data;
}
export type BlockChange = {
  id: string;
  blockId: string;
  kind: "RESCHEDULED" | "CANCELLED";
  revision: string;
  occurredAt: string;
  before: Block;
  after: Block | null;
};
export type RetainedReschedule = {
  blockId: string;
  input: Omit<RescheduleInput, "startOffset" | "endOffset"> & {
    startOffset: string;
    endOffset: string;
    allowOverBudget: boolean;
  };
  key: string;
  revision: string;
  availabilityRevision: string;
};
export async function rescheduleBlock(
  projectId: string,
  taskId: string,
  request: RetainedReschedule,
  signal?: AbortSignal,
): Promise<BlockChange> {
  const retained = structuredClone(request);
  const response = await apiRequest(
    `${collection(projectId, taskId)}/${request.blockId}/reschedule`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": request.revision,
        "Availability-Revision": request.availabilityRevision,
        "Idempotency-Key": request.key,
      },
      body: JSON.stringify(request.input),
    },
  );
  return readConfirmedChange(response, projectId, taskId, retained);
}
function isBlockChange(
  value: unknown,
  projectId: string,
  taskId: string,
): value is BlockChange {
  return (
    exact(value, "id blockId kind revision occurredAt before after") &&
    uuid(value.id) &&
    uuid(value.blockId) &&
    (value.kind === "RESCHEDULED" || value.kind === "CANCELLED") &&
    isRevision(value.revision, value.blockId) &&
    instant(value.occurredAt) &&
    isBlock(value.before, projectId, taskId) &&
    sameId(value.before.id, value.blockId) &&
    (value.kind === "CANCELLED"
      ? value.after === null
      : isBlock(value.after, projectId, taskId))
  );
}
function confirmsReschedule(
  change: BlockChange,
  request: RetainedReschedule,
): boolean {
  const after = change.after;
  return (
    sameId(change.blockId, request.blockId) &&
    change.kind === "RESCHEDULED" &&
    after !== null &&
    keepsIdentity(change.before, after) &&
    after.zoneId === request.input.zoneId &&
    localMatches(
      request.input.startLocal,
      request.input.startOffset,
      after.startAt,
    ) &&
    localMatches(request.input.endLocal, request.input.endOffset, after.endAt)
  );
}
function keepsIdentity(before: Block, after: Block): boolean {
  return (
    sameId(after.id, before.id) &&
    after.objective === before.objective &&
    after.createdAt === before.createdAt
  );
}
export type RetainedCancellation = {
  blockId: string;
  key: string;
  revision: string;
};
export async function cancelBlock(
  projectId: string,
  taskId: string,
  request: RetainedCancellation,
  signal?: AbortSignal,
): Promise<BlockChange> {
  const retained = structuredClone(request);
  const response = await apiRequest(
    `${collection(projectId, taskId)}/${request.blockId}/cancel`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": request.revision,
        "Idempotency-Key": request.key,
      },
      body: "{}",
    },
  );
  return readConfirmedChange(response, projectId, taskId, retained);
}
async function readConfirmedChange(
  response: Response,
  projectId: string,
  taskId: string,
  request: RetainedReschedule | RetainedCancellation,
): Promise<BlockChange> {
  if (response.status !== 200 && response.status !== 201) throw response;
  const data: unknown = await response.json();
  if (
    !isBlockChange(data, projectId, taskId) ||
    !confirmsIntention(data, request) ||
    response.headers.get("Location") !==
      `${collection(projectId, taskId)}/changes/${data.id}`
  )
    throw new Error("Confirmación de cambio inválida");
  return data;
}
function confirmsCancellation(
  change: BlockChange,
  request: RetainedCancellation,
): boolean {
  return sameId(change.blockId, request.blockId) && change.kind === "CANCELLED";
}
export type BlockChangePage = {
  items: BlockChange[];
  nextCursor: string | null;
};
export async function readBlockChanges(
  projectId: string,
  taskId: string,
  cursor?: string,
  signal?: AbortSignal,
): Promise<BlockChangePage> {
  const response = await apiRequest(
    `${collection(projectId, taskId)}/changes` +
      (cursor ? `?cursor=${encodeURIComponent(cursor)}` : ""),
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    !exact(data, "items nextCursor") ||
    !Array.isArray(data.items) ||
    data.items.length > CHANGES_PER_PAGE ||
    !data.items.every((item) => isBlockChange(item, projectId, taskId)) ||
    !(data.nextCursor === null || text(data.nextCursor))
  )
    throw new Error("Historial de cambios inválido");
  return data as BlockChangePage;
}
export async function readBlockChange(
  projectId: string,
  taskId: string,
  changeId: string,
  signal?: AbortSignal,
): Promise<BlockChange> {
  const response = await apiRequest(
    `${collection(projectId, taskId)}/changes/${changeId}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (!isBlockChange(data, projectId, taskId) || !sameId(data.id, changeId))
    throw new Error("Confirmación de cambio inválida");
  return data;
}
export async function readBlockChangeByRequest(
  projectId: string,
  taskId: string,
  request: RetainedReschedule | RetainedCancellation,
  signal?: AbortSignal,
): Promise<BlockChange> {
  const retained = structuredClone(request);
  const response = await apiRequest(
    `${collection(projectId, taskId)}/changes/by-request/${request.key}`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    !isBlockChange(data, projectId, taskId) ||
    !confirmsIntention(data, retained)
  )
    throw new Error("Confirmación de cambio inválida");
  return data;
}
function confirmsIntention(
  change: BlockChange,
  request: RetainedReschedule | RetainedCancellation,
): boolean {
  return "input" in request
    ? confirmsReschedule(change, request)
    : confirmsCancellation(change, request);
}
const changeErrors = {
  BLOCK_CONFLICT: 412,
  BLOCK_CANCELLED: 409,
  BLOCK_UNCHANGED: 409,
  BLOCK_VERSION_EXHAUSTED: 409,
  BLOCK_CHANGE_NOT_FOUND: 404,
} as const;
export type RescheduleError =
  | BlockError
  | {
      type: string;
      title: string;
      status: number;
      code: keyof typeof changeErrors;
    };
export async function readRescheduleError(
  error: unknown,
): Promise<RescheduleError | null> {
  const inherited = await readBlockError(error);
  if (inherited || !(error instanceof Response) || error.bodyUsed)
    return inherited;
  const value: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  if (
    !exact(value, "type title status code") ||
    !text(value.title) ||
    typeof value.code !== "string" ||
    value.type !== "urn:organization:problem:" + value.code.toLowerCase() ||
    value.status !== error.status ||
    !Object.hasOwn(changeErrors, value.code) ||
    changeErrors[value.code as keyof typeof changeErrors] !== error.status
  )
    return null;
  return value as RescheduleError;
}
