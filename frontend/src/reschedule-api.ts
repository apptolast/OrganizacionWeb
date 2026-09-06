import { apiRequest } from "./api-client";
import {
  readBlockError,
  type BlockError,
  text,
  uuid,
  sameId,
  isPreview,
  exact,
  isBlock,
  instant,
  type Block,
  type BlockInput,
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
    collection(projectId, taskId) + `/${blockId}/state`,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  const revision = response.headers.get("ETag");
  if (
    !exact(value, "block status updatedAt") ||
    !isBlock(value.block, projectId, taskId) ||
    !sameId(value.block.id, blockId) ||
    !["planned", "cancelled"].includes(value.status as string) ||
    !instant(value.updatedAt) ||
    !blockRevision(revision, blockId)
  )
    throw new Error("Estado de bloque inválido");
  return { ...value, revision } as BlockState;
}

export function blockRevision(
  value: unknown,
  blockId: string,
): value is string {
  if (typeof value !== "string") return false;
  const match =
    /^"block:([\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}):([1-9]\d*)"$/.exec(
      value,
    );
  return Boolean(
    match &&
    sameId(match[1], blockId) &&
    BigInt(match[2]) <= 9223372036854775807n,
  );
}

export type MoveInput = Omit<BlockInput, "objective">;
export async function previewMove(
  state: BlockState,
  input: MoveInput,
  signal?: AbortSignal,
): Promise<BlockPreview> {
  const expected = { ...input, objective: state.block.objective };
  const expectedRevision = state.revision;
  const response = await apiRequest(
    collection(state.block.projectId, state.block.taskId) +
      "/" +
      state.block.id +
      "/reschedule/preview",
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
  const value: unknown = await response.json();
  if (
    !isPreview(value, expected) ||
    response.headers.get("ETag") !== expectedRevision
  )
    throw new Error("Revisión de movimiento inválida");
  return value;
}
export type BlockChange = {
  id: string;
  blockId: string;
  kind: "CANCELLED" | "RESCHEDULED";
  revision: string;
  occurredAt: string;
  before: Block;
  after: Block | null;
};
export type RetainedChange = { state: BlockState; key: string } & (
  | { kind: "CANCELLED" }
  | {
      kind: "RESCHEDULED";
      input: MoveInput & {
        startOffset: string;
        endOffset: string;
        allowOverBudget: boolean;
      };
      preview: BlockPreview;
    }
);
export async function sendBlockChange(
  request: RetainedChange,
  signal?: AbortSignal,
): Promise<BlockChange> {
  request = structuredClone(request);
  const { block } = request.state;
  if (
    request.kind === "RESCHEDULED" &&
    !isPreview(request.preview, {
      ...request.input,
      objective: block.objective,
    })
  )
    throw new Error("Intención de cambio inválida");
  const response = await apiRequest(
    collection(block.projectId, block.taskId) +
      "/" +
      block.id +
      (request.kind === "CANCELLED" ? "/cancel" : "/reschedule"),
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: {
        "Content-Type": "application/json",
        "If-Match": request.state.revision,
        "Idempotency-Key": request.key,
        ...(request.kind === "RESCHEDULED"
          ? { "Availability-Revision": request.preview.availabilityEtag }
          : {}),
      },
      body: JSON.stringify(request.kind === "CANCELLED" ? {} : request.input),
    },
  );
  if (response.status !== 200 && response.status !== 201) throw response;
  const value: unknown = await response.json();
  if (
    !matchesChange(value, request) ||
    response.headers.get("Location") !==
      collection(block.projectId, block.taskId) + "/changes/" + value.id
  )
    throw new Error("Confirmación de cambio inválida");
  return value;
}

function sameBlock(a: Block, b: Block) {
  return Object.keys(b).every(
    (key) => a[key as keyof Block] === b[key as keyof Block],
  );
}
function matchesChange(
  value: unknown,
  request: RetainedChange,
): value is BlockChange {
  const before = request.state.block;
  return (
    exact(value, "id blockId kind revision occurredAt before after") &&
    uuid(value.id) &&
    sameId(value.blockId, before.id) &&
    value.kind === request.kind &&
    blockRevision(value.revision, before.id) &&
    instant(value.occurredAt) &&
    isBlock(value.before, before.projectId, before.taskId) &&
    sameBlock(value.before, before) &&
    (request.kind === "CANCELLED"
      ? value.after === null
      : isBlock(value.after, before.projectId, before.taskId) &&
        sameBlock(value.after, {
          ...before,
          startAt: request.preview.startAt,
          endAt: request.preview.endAt,
          zoneId: request.preview.zoneId,
          durationMinutes: request.preview.durationMinutes,
        })) &&
    value.revision ===
      '"block:' +
        before.id +
        ":" +
        (BigInt(request.state.revision.split(":")[2].slice(0, -1)) + 1n) +
        '"'
  );
}
export async function checkBlockChange(
  request: RetainedChange,
  signal?: AbortSignal,
): Promise<BlockChange> {
  request = structuredClone(request);
  const { block } = request.state;
  const response = await apiRequest(
    collection(block.projectId, block.taskId) +
      "/changes/by-request/" +
      request.key,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (!matchesChange(value, request))
    throw new Error("Confirmación de cambio inválida");
  return value;
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
    collection(projectId, taskId) +
      "/changes" +
      (cursor ? "?cursor=" + encodeURIComponent(cursor) : ""),
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (
    !exact(value, "items nextCursor") ||
    !Array.isArray(value.items) ||
    value.items.length > 20 ||
    !value.items.every((item) => isChange(item, projectId, taskId)) ||
    !(value.nextCursor === null || text(value.nextCursor))
  )
    throw new Error("Historial de bloques inválido");
  const page = value as BlockChangePage;
  if (
    new Set(page.items.map((item) => item.id.toLowerCase())).size !==
      page.items.length ||
    page.items.some((item, index) => {
      if (index === 0) return false;
      const prior = page.items[index - 1];
      return (
        timeKey(prior.occurredAt) < timeKey(item.occurredAt) ||
        (timeKey(prior.occurredAt) === timeKey(item.occurredAt) &&
          prior.id.toLowerCase() <= item.id.toLowerCase())
      );
    })
  )
    throw new Error("Historial de bloques inválido");
  return page;
}

function isChange(
  value: unknown,
  projectId: string,
  taskId: string,
): value is BlockChange {
  if (
    !exact(value, "id blockId kind revision occurredAt before after") ||
    !uuid(value.id) ||
    !uuid(value.blockId) ||
    !blockRevision(value.revision, value.blockId) ||
    !instant(value.occurredAt) ||
    !isBlock(value.before, projectId, taskId) ||
    !sameId(value.before.id, value.blockId)
  )
    return false;
  if (value.kind === "CANCELLED") return value.after === null;
  return (
    value.kind === "RESCHEDULED" &&
    isBlock(value.after, projectId, taskId) &&
    sameId(value.after.id, value.blockId) &&
    value.after.objective === value.before.objective &&
    value.after.createdAt === value.before.createdAt
  );
}
export async function readBlockChange(
  projectId: string,
  taskId: string,
  id: string,
  signal?: AbortSignal,
): Promise<BlockChange> {
  const response = await apiRequest(
    collection(projectId, taskId) + "/changes/" + id,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const value: unknown = await response.json();
  if (!isChange(value, projectId, taskId) || !sameId(value.id, id))
    throw new Error("Recibo de bloque inválido");
  return value;
}
const changeErrors = {
  BLOCK_CONFLICT: 412,
  BLOCK_CANCELLED: 409,
  BLOCK_UNCHANGED: 409,
  BLOCK_VERSION_EXHAUSTED: 409,
  BLOCK_CHANGE_NOT_FOUND: 404,
} as const;
export type ChangeError =
  | BlockError
  | {
      type: string;
      title: string;
      status: number;
      code: keyof typeof changeErrors;
    };
export async function readChangeError(
  error: unknown,
): Promise<ChangeError | null> {
  const inherited = await readBlockError(error);
  if (inherited) return inherited;
  if (!(error instanceof Response) || error.bodyUsed) return null;
  const value: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  if (
    !exact(value, "type title status code") ||
    !text(value.code) ||
    !text(value.title) ||
    value.type !== "urn:organization:problem:" + value.code.toLowerCase() ||
    value.status !== error.status ||
    !Object.hasOwn(changeErrors, value.code) ||
    changeErrors[value.code as keyof typeof changeErrors] !== error.status
  )
    return null;
  return value as ChangeError;
}

function timeKey(value: string) {
  return value.replace(
    /(?:\.(\d+))?Z$/,
    (_match, fraction: string = "") => "." + fraction.padEnd(6, "0") + "Z",
  );
}
