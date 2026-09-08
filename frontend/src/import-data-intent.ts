import { exact, uuid } from "./schedule-block-api";
const storageKey = "organizationweb.import.pending.v1";
export function clearImportIntent() {
  sessionStorage.removeItem(storageKey);
}
export type ImportIntent = {
  owner: string;
  requestKey: string;
  fileSha256: string;
};
export function saveImportIntent(intent: ImportIntent) {
  const raw = JSON.stringify({
    owner: intent.owner,
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
  });
  sessionStorage.setItem(storageKey, raw);
  if (sessionStorage.getItem(storageKey) !== raw)
    throw new Error("No se puede conservar la intención de importación");
}

export function readImportIntent(owner: string): ImportIntent | null {
  const raw = sessionStorage.getItem(storageKey);
  let intent: unknown;
  try {
    intent = raw ? JSON.parse(raw) : null;
  } catch {
    sessionStorage.removeItem(storageKey);
    return null;
  }
  if (
    !exact(intent, "owner requestKey fileSha256") ||
    intent.owner !== owner ||
    !uuid(intent.requestKey) ||
    intent.requestKey !== intent.requestKey.toLowerCase() ||
    typeof intent.fileSha256 !== "string" ||
    !/^[0-9a-f]{64}$/.test(intent.fileSha256)
  ) {
    sessionStorage.removeItem(storageKey);
    return null;
  }
  return {
    owner,
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
  };
}
