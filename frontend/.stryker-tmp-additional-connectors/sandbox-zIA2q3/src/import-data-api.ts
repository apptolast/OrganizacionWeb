// @ts-nocheck
import { apiRequest } from "./api-client";
import type { ImportIntent } from "./import-data-intent";
export class ImportFileMismatchError extends Error {}
import { exact, instant, uuid } from "./schedule-block-api";
const collections =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues";
function isCounts(value: unknown): value is Record<string, number> {
  return (
    exact(value, collections) &&
    Object.values(value).every(
      (count) =>
        typeof count === "number" && Number.isInteger(count) && count >= 0,
    )
  );
}
function isTimestamp(value: unknown): value is string {
  return instant(value) && /\.\d{6}Z$/.test(value);
}
type RunningSession = { sessionId: string; runningSince: string | null };
function isRunningSession(session: unknown): session is RunningSession {
  return (
    exact(session, "sessionId runningSince") &&
    uuid(session.sessionId) &&
    (session.runningSince === null || isTimestamp(session.runningSince))
  );
}

export async function previewImportData(
  file: Blob,
  owner: string,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  if (file.size > 33_554_432) throw new Error("El archivo supera 32 MiB");
  const response = await apiRequest("/api/v1/me/import/preview", {
    method: "POST",
    headers: { "Content-Type": "application/json;charset=utf-8" },
    body: file,
    signal,
  });
  const preview = await readResponse(response, signal);
  signal.throwIfAborted();
  if (
    !exact(
      preview,
      "format schemaVersion fileSha256 byteLength owner exportedAt counts insertCounts identicalCounts runningSessions",
    ) ||
    preview.owner !== owner ||
    preview.byteLength !== file.size ||
    preview.format !== "organizationweb-import-preview" ||
    preview.schemaVersion !== 1
  )
    throw new Error("Vista previa incompatible");
  if (!isTimestamp(preview.exportedAt))
    throw new Error("Vista previa incompatible");
  if (
    !isCounts(preview.counts) ||
    !isCounts(preview.insertCounts) ||
    !isCounts(preview.identicalCounts)
  )
    throw new Error("Vista previa incompatible");
  for (const key of collections.split(" ")) {
    if (
      preview.counts[key] !==
      preview.insertCounts[key] + preview.identicalCounts[key]
    )
      throw new Error("Vista previa incompatible");
  }
  if (
    Object.values(preview.counts).reduce((sum, count) => sum + count, 0) >
    100_000
  )
    throw new Error("Vista previa incompatible");
  if (
    !Array.isArray(preview.runningSessions) ||
    preview.runningSessions.length > 1
  )
    throw new Error("Vista previa incompatible");
  if (preview.runningSessions.length > preview.insertCounts.workSessions)
    throw new Error("Vista previa incompatible");
  if (!preview.runningSessions.every(isRunningSession))
    throw new Error("Vista previa incompatible");
  const hash = await fileHash(file, signal);
  signal.throwIfAborted();
  if (preview.fileSha256 !== hash) throw new Error("Vista previa incompatible");
  return {
    format: preview.format,
    schemaVersion: preview.schemaVersion,
    fileSha256: hash,
    byteLength: file.size,
    owner,
    exportedAt: preview.exportedAt,
    counts: preview.counts,
    insertCounts: preview.insertCounts,
    identicalCounts: preview.identicalCounts,
    runningSessions: preview.runningSessions,
  };
}
export type ImportPreview = Awaited<ReturnType<typeof previewImportData>>;

async function readResponse(
  response: Response,
  signal: AbortSignal,
): Promise<unknown> {
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  const body: unknown = await response.json();
  signal.throwIfAborted();
  return body;
}

async function fileHash(file: Blob, signal: AbortSignal) {
  signal.throwIfAborted();
  if (file.size > 33_554_432) throw new Error("El archivo supera 32 MiB");
  const bytes = await file.arrayBuffer();
  signal.throwIfAborted();
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  signal.throwIfAborted();
  return Array.from(new Uint8Array(digest), (byte) =>
    byte.toString(16).padStart(2, "0"),
  ).join("");
}

export async function confirmImportData(
  file: Blob,
  intent: ImportIntent,
  signal: AbortSignal,
) {
  if ((await fileHash(file, signal)) !== intent.fileSha256)
    throw new ImportFileMismatchError("El archivo ha cambiado");
  signal.throwIfAborted();
  const response = await apiRequest("/api/v1/me/import", {
    method: "POST",
    body: file,
    signal,
    headers: {
      "Content-Type": "application/json;charset=utf-8",
      "Idempotency-Key": intent.requestKey,
      "X-Import-Content-SHA256": intent.fileSha256,
    },
  });
  const receipt = await readResponse(response, signal);
  signal.throwIfAborted();
  return decodeReceipt(receipt, intent, file.size);
}

export async function readImportReceipt(
  intent: ImportIntent,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  if (!uuid(intent.requestKey)) throw new Error("Intención incompatible");
  const response = await apiRequest(
    `/api/v1/me/imports/by-key/${intent.requestKey}`,
    { signal },
  );
  const receipt = await readResponse(response, signal);
  signal.throwIfAborted();
  return decodeReceipt(receipt, intent);
}

function decodeReceipt(
  receipt: unknown,
  intent: ImportIntent,
  expectedLength?: number,
) {
  if (
    !exact(
      receipt,
      "requestKey fileSha256 byteLength recordedAt outcome insertedCounts identicalCounts",
    )
  )
    throw new Error("Confirmación incompatible");
  if (receipt.requestKey !== intent.requestKey)
    throw new Error("Confirmación incompatible");
  if (receipt.fileSha256 !== intent.fileSha256)
    throw new Error("Confirmación incompatible");
  if (
    typeof receipt.byteLength !== "number" ||
    !Number.isInteger(receipt.byteLength) ||
    receipt.byteLength <= 0 ||
    receipt.byteLength > 33_554_432
  )
    throw new Error("Confirmación incompatible");
  if (expectedLength !== undefined && receipt.byteLength !== expectedLength)
    throw new Error("Confirmación incompatible");
  if (!isTimestamp(receipt.recordedAt))
    throw new Error("Confirmación incompatible");
  if (!isCounts(receipt.insertedCounts) || !isCounts(receipt.identicalCounts))
    throw new Error("Confirmación incompatible");
  const inserted = Object.values(receipt.insertedCounts).reduce(
    (sum, count) => sum + count,
    0,
  );
  if (receipt.outcome !== (inserted === 0 ? "NO_CHANGE" : "IMPORTED"))
    throw new Error("Confirmación incompatible");
  if (
    inserted +
      Object.values(receipt.identicalCounts).reduce(
        (sum, count) => sum + count,
        0,
      ) >
    100_000
  )
    throw new Error("Confirmación incompatible");
  return {
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
    byteLength: receipt.byteLength,
    recordedAt: receipt.recordedAt,
    outcome: inserted === 0 ? ("NO_CHANGE" as const) : ("IMPORTED" as const),
    insertedCounts: receipt.insertedCounts,
    identicalCounts: receipt.identicalCounts,
  };
}
export type ImportReceipt = ReturnType<typeof decodeReceipt>;
