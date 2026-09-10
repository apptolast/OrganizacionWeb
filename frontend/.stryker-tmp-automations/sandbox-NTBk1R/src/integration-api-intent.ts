// @ts-nocheck
export type ApiCredentialIntent = { owner: string; id: string };
const storageKey = "organizationweb.api-credential.pending.v1";
export function clearApiCredentialIntent() {
  sessionStorage.removeItem(storageKey);
}
export function saveApiCredentialIntent(intent: ApiCredentialIntent) {
  const raw = JSON.stringify({ owner: intent.owner, id: intent.id });
  sessionStorage.setItem(storageKey, raw);
  if (sessionStorage.getItem(storageKey) !== raw)
    throw new Error("No se puede conservar la intención");
}
export function readApiCredentialIntent(
  owner: string,
): ApiCredentialIntent | null {
  const raw = sessionStorage.getItem(storageKey);
  let intent: unknown;
  try {
    intent = raw ? JSON.parse(raw) : null;
  } catch {
    intent = null;
  }
  if (
    !exact(intent, "owner id") ||
    intent.owner !== owner ||
    !uuid(intent.id) ||
    intent.id !== intent.id.toLowerCase()
  ) {
    sessionStorage.removeItem(storageKey);
    return null;
  }
  return { owner, id: intent.id };
}
import { exact, uuid } from "./schedule-block-api";
