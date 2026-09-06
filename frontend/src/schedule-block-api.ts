import { apiRequest } from "./api-client";
const simpleErrors = {
  BLOCK_NOT_FOUND: 404,
  RESOURCE_NOT_FOUND: 404,
  AVAILABILITY_REQUIRED: 409,
  AVAILABILITY_ZONE_UNAVAILABLE: 409,
  PROJECT_COMPLETED: 409,
  TASK_COMPLETED: 409,
  IDEMPOTENCY_CONFLICT: 409,
  AVAILABILITY_CONFLICT: 412,
  PRECONDITION_REQUIRED: 428,
  CSRF_INVALID: 403,
  STORAGE_UNAVAILABLE: 503,
  MALFORMED_JSON: 400,
} as const;
type Problem = { type: string; title: string; status: number };
const fieldCodes = [
  "REQUIRED",
  "INVALID_TYPE",
  "UNKNOWN_FIELD",
  "INVALID_FORMAT",
  "INVALID_VALUE",
  "OUT_OF_RANGE",
  "TOO_LONG",
  "NONEXISTENT_LOCAL_TIME",
  "IN_PAST",
] as const;
export type BlockFieldError = {
  field: string;
  code: (typeof fieldCodes)[number] | "AMBIGUOUS_OFFSET" | "INVALID_OFFSET";
  message: string;
};
export type BlockError = Problem &
  (
    | { code: keyof typeof simpleErrors }
    | { code: "BUDGET_EXCEEDED"; budgetZoneId: string; days: BlockDay[] }
    | {
        code: "BLOCK_OVERLAP";
        conflict: { id: string; projectId: string; taskId: string };
      }
    | {
        code: "VALIDATION_ERROR";
        errors: BlockFieldError[];
        validOffsets?: Partial<Record<"startOffset" | "endOffset", string[]>>;
      }
  );
export async function readBlockError(
  error: unknown,
): Promise<BlockError | null> {
  if (!(error instanceof Response) || error.bodyUsed) return null;
  const value: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  if (
    !record(value) ||
    !text(value.code) ||
    !text(value.title) ||
    value.type !== "urn:organization:problem:" + value.code.toLowerCase() ||
    value.status !== error.status
  )
    return null;
  if (
    value.code === "BUDGET_EXCEEDED" &&
    error.status === 409 &&
    exact(value, "type title status code budgetZoneId days") &&
    text(value.budgetZoneId) &&
    days(value.days) &&
    value.days.some((day) => day.excessSeconds > 0)
  )
    return value as BlockError;
  if (
    value.code === "BLOCK_OVERLAP" &&
    error.status === 409 &&
    exact(value, "type title status code conflict") &&
    exact(value.conflict, "id projectId taskId") &&
    uuid(value.conflict.id) &&
    uuid(value.conflict.projectId) &&
    uuid(value.conflict.taskId)
  )
    return value as BlockError;
  if (
    value.code === "VALIDATION_ERROR" &&
    error.status === 400 &&
    exact(value, "type title status code errors validOffsets") &&
    Array.isArray(value.errors) &&
    value.errors.length === 1 &&
    exact(value.errors[0], "field code message") &&
    text(value.errors[0].message) &&
    (value.errors[0].code === "AMBIGUOUS_OFFSET" ||
      value.errors[0].code === "INVALID_OFFSET") &&
    (value.errors[0].field === "startOffset" ||
      value.errors[0].field === "endOffset") &&
    exact(value.validOffsets, value.errors[0].field)
  ) {
    const options = value.validOffsets[value.errors[0].field];
    if (
      Array.isArray(options) &&
      options.length >= (value.errors[0].code === "AMBIGUOUS_OFFSET" ? 2 : 1) &&
      options.every(
        (offset, index) =>
          Number.isFinite(offsetSeconds(offset)) &&
          (index === 0 ||
            offsetSeconds(options[index - 1]) > offsetSeconds(offset)),
      )
    )
      return value as BlockError;
  }
  if (
    value.code === "VALIDATION_ERROR" &&
    error.status === 400 &&
    exact(value, "type title status code errors") &&
    Array.isArray(value.errors) &&
    value.errors.length > 0 &&
    value.errors.every(
      (item) =>
        exact(item, "field code message") &&
        text(item.field) &&
        text(item.message) &&
        fieldCodes.includes(item.code as (typeof fieldCodes)[number]),
    )
  )
    return value as BlockError;
  if (
    !exact(value, "type title status code") ||
    !Object.hasOwn(simpleErrors, value.code) ||
    simpleErrors[value.code as keyof typeof simpleErrors] !== error.status
  )
    return null;
  return value as BlockError;
}
export type BlockInput = {
  objective: string;
  startLocal: string;
  endLocal: string;
  zoneId: string;
  startOffset: string | null;
  endOffset: string | null;
};
export type BlockCreateInput = Omit<BlockInput, "startOffset" | "endOffset"> & {
  startOffset: string;
  endOffset: string;
  allowOverBudget: boolean;
};
export type RetainedBlockRequest = {
  input: BlockCreateInput;
  key: string;
  availabilityRevision: string;
};
export type Block = {
  id: string;
  projectId: string;
  taskId: string;
  objective: string;
  startAt: string;
  endAt: string;
  zoneId: string;
  durationMinutes: number;
  createdAt: string;
};
export type BlockDay = {
  date: string;
  budgetMinutes: number;
  plannedSeconds: number;
  requestedSeconds: number;
  excessSeconds: number;
};
export type BlockPreview = {
  objective: string;
  zoneId: string;
  startAt: string;
  endAt: string;
  startOffset: string;
  endOffset: string;
  durationMinutes: number;
  availabilityEtag: string;
  budgetZoneId: string;
  days: BlockDay[];
};
const collection = (projectId: string, taskId: string) =>
  `/api/v1/projects/${projectId}/tasks/${taskId}/blocks`;
export async function readBlock(
  projectId: string,
  taskId: string,
  id: string,
  signal?: AbortSignal,
): Promise<Block> {
  const response = await apiRequest(collection(projectId, taskId) + "/" + id, {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (!isBlock(data, projectId, taskId) || !sameId(data.id, id))
    throw new Error("Respuesta de bloque inválida");
  return data;
}
export type BlockPage = { items: Block[]; nextCursor: string | null };
export async function readBlocks(
  projectId: string,
  taskId: string,
  cursor?: string,
  signal?: AbortSignal,
): Promise<BlockPage> {
  const response = await apiRequest(
    collection(projectId, taskId) +
      (cursor ? "?cursor=" + encodeURIComponent(cursor) : ""),
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    !exact(data, "items nextCursor") ||
    !Array.isArray(data.items) ||
    data.items.length > 20 ||
    !data.items.every((item) => isBlock(item, projectId, taskId)) ||
    !(data.nextCursor === null || text(data.nextCursor))
  )
    throw new Error("Lista de bloques inválida");
  return data as BlockPage;
}
export async function readBlockByRequest(
  projectId: string,
  taskId: string,
  key: string,
  expected: BlockPreview,
  signal?: AbortSignal,
): Promise<Block> {
  const retained = { ...expected };
  const response = await apiRequest(
    collection(projectId, taskId) + "/by-request/" + key,
    { credentials: "same-origin", cache: "no-store", signal },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (!isBlock(data, projectId, taskId) || !matches(data, retained))
    throw new Error("Confirmación de bloque inválida");
  return data;
}
export async function createBlock(
  projectId: string,
  taskId: string,
  request: RetainedBlockRequest,
  preview: BlockPreview,
  signal?: AbortSignal,
): Promise<Block> {
  if (
    !isPreview(preview, request.input) ||
    request.availabilityRevision !== preview.availabilityEtag
  )
    throw new Error("Intención de bloque inválida");
  const expected = { ...preview };
  const response = await apiRequest(collection(projectId, taskId), {
    method: "POST",
    credentials: "same-origin",
    cache: "no-store",
    signal,
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": request.key,
      "Availability-Revision": request.availabilityRevision,
    },
    body: JSON.stringify(request.input),
  });
  if (response.status !== 200 && response.status !== 201) throw response;
  const data: unknown = await response.json();
  if (!isBlock(data, projectId, taskId) || !matches(data, expected))
    throw new Error("Confirmación de bloque inválida");
  return data;
}
export function uuid(value: unknown): value is string {
  return (
    typeof value === "string" &&
    /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(value)
  );
}
export function sameId(value: unknown, expected: string) {
  return uuid(value) && value.toLowerCase() === expected.toLowerCase();
}
export function isBlock(
  value: unknown,
  projectId: string,
  taskId: string,
): value is Block {
  return (
    exact(
      value,
      "id projectId taskId objective startAt endAt zoneId durationMinutes createdAt",
    ) &&
    uuid(value.id) &&
    sameId(value.projectId, projectId) &&
    sameId(value.taskId, taskId) &&
    text(value.objective) &&
    value.objective === strip(value.objective) &&
    [...value.objective].length <= 500 &&
    text(value.zoneId) &&
    wholeInstant(value.startAt) &&
    wholeInstant(value.endAt) &&
    integer(value.durationMinutes, 1, 1440) &&
    Date.parse(value.endAt) - Date.parse(value.startAt) ===
      value.durationMinutes * 60000 &&
    instant(value.createdAt)
  );
}
function matches(block: Block, preview: BlockPreview) {
  return (
    block.objective === preview.objective &&
    block.zoneId === preview.zoneId &&
    Date.parse(block.startAt) === Date.parse(preview.startAt) &&
    Date.parse(block.endAt) === Date.parse(preview.endAt) &&
    block.durationMinutes === preview.durationMinutes
  );
}
export async function previewBlock(
  projectId: string,
  taskId: string,
  input: BlockInput,
  signal?: AbortSignal,
): Promise<BlockPreview> {
  const sent = { ...input };
  const response = await apiRequest(
    collection(projectId, taskId) + "/preview",
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    },
  );
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (!isPreview(data, sent)) throw new Error("Revisión de bloque inválida");
  return data;
}
function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
export function exact(
  value: unknown,
  keys: string,
): value is Record<string, unknown> {
  return (
    record(value) &&
    Object.keys(value).length === keys.split(" ").length &&
    keys.split(" ").every((key) => Object.hasOwn(value, key))
  );
}
const strip = (value: string) =>
  value.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, "");
export function text(value: unknown): value is string {
  return typeof value === "string" && strip(value).length > 0;
}
export function integer(
  value: unknown,
  min: number,
  max = Number.MAX_SAFE_INTEGER,
): value is number {
  return (
    typeof value === "number" &&
    Number.isSafeInteger(value) &&
    value >= min &&
    value <= max
  );
}
export function instant(value: unknown): value is string {
  return (
    typeof value === "string" &&
    /^(?!0000)\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$/.test(
      value,
    ) &&
    Number.isFinite(Date.parse(value)) &&
    new Date(value).toISOString().split(".")[0] ===
      value.replace(/(?:\.\d+)?Z$/, "")
  );
}
function offsetSeconds(value: unknown): number {
  if (value === "Z") return 0;
  if (typeof value !== "string") return NaN;
  const match = /^([+-])(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(value);
  if (!match) return NaN;
  const hours = Number(match[2]),
    minutes = Number(match[3]),
    seconds = Number(match[4] ?? 0);
  const total = hours * 3600 + minutes * 60 + seconds;
  return hours <= 18 &&
    minutes < 60 &&
    seconds < 60 &&
    total <= 64800 &&
    total > 0 &&
    (!match[4] || seconds > 0)
    ? total * (match[1] === "-" ? -1 : 1)
    : NaN;
}
function localMatches(local: string, offset: unknown, at: unknown) {
  const naive = local + ":00Z";
  return (
    /^(?!0000)\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(local) &&
    instant(naive) &&
    wholeInstant(at) &&
    Date.parse(naive) - offsetSeconds(offset) * 1000 === Date.parse(at)
  );
}
function days(value: unknown): value is BlockDay[] {
  return (
    Array.isArray(value) &&
    value.length > 0 &&
    value.every(
      (day, index) =>
        exact(
          day,
          "date budgetMinutes plannedSeconds requestedSeconds excessSeconds",
        ) &&
        typeof day.date === "string" &&
        instant(day.date + "T00:00:00Z") &&
        (index === 0 || value[index - 1].date < day.date) &&
        integer(day.budgetMinutes, 0, 1440) &&
        integer(day.plannedSeconds, 0) &&
        integer(day.requestedSeconds, 1) &&
        integer(day.excessSeconds, 0) &&
        day.excessSeconds ===
          Math.max(
            0,
            day.plannedSeconds + day.requestedSeconds - day.budgetMinutes * 60,
          ),
    )
  );
}
function availabilityTag(value: unknown) {
  const match =
    typeof value === "string" &&
    /^"availability:[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}:(0|[1-9]\d*)"$/.exec(
      value,
    );
  return Boolean(match && BigInt(match[1]) <= 9223372036854775807n);
}
export function isPreview(
  value: unknown,
  input: BlockInput,
): value is BlockPreview {
  return (
    exact(
      value,
      "objective zoneId startAt endAt startOffset endOffset durationMinutes availabilityEtag budgetZoneId days",
    ) &&
    text(value.objective) &&
    [...value.objective].length <= 500 &&
    text(value.zoneId) &&
    value.objective === strip(input.objective) &&
    value.zoneId === input.zoneId &&
    localMatches(input.startLocal, value.startOffset, value.startAt) &&
    localMatches(input.endLocal, value.endOffset, value.endAt) &&
    (input.startOffset === null || input.startOffset === value.startOffset) &&
    (input.endOffset === null || input.endOffset === value.endOffset) &&
    integer(value.durationMinutes, 1, 1440) &&
    Date.parse(value.endAt as string) - Date.parse(value.startAt as string) ===
      value.durationMinutes * 60000 &&
    availabilityTag(value.availabilityEtag) &&
    text(value.budgetZoneId) &&
    days(value.days) &&
    value.days.reduce((sum, day) => sum + day.requestedSeconds, 0) ===
      value.durationMinutes * 60
  );
}

function wholeInstant(value: unknown): value is string {
  return instant(value) && !/\.\d*[1-9]\d*Z$/.test(value);
}
