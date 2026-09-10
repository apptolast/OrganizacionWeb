// @ts-nocheck
import { apiRequest } from "./api-client";
import { exact, instant, uuid } from "./schedule-block-api";
import { microseconds } from "./work-session-api";

export const apiScopes = [
  "projects:read",
  "projects:write",
  "tasks:read",
  "tasks:write",
  "agenda:read",
  "history:read",
] as const;
export type ApiCredential = {
  id: string;
  name: string;
  scopes: string[];
  createdAt: string;
  expiresAt: string;
  revokedAt: string | null;
};
function timestamp(value: unknown): value is string {
  return instant(value);
}
function decodeCredential(value: unknown): ApiCredential {
  if (
    !exact(value, "id name scopes createdAt expiresAt revokedAt") ||
    !uuid(value.id) ||
    value.id !== value.id.toLowerCase() ||
    value.id.length !== 36 ||
    typeof value.name !== "string" ||
    !value.name ||
    /^\p{White_Space}|\p{White_Space}$/u.test(value.name) ||
    /\p{Cc}/u.test(value.name) ||
    [...value.name].length > 80 ||
    !Array.isArray(value.scopes) ||
    value.scopes.length === 0 ||
    value.scopes.some(
      (scope, i, scopes) =>
        !apiScopes.includes(scope) ||
        (i > 0 && apiScopes.indexOf(scopes[i - 1]) >= apiScopes.indexOf(scope)),
    ) ||
    !timestamp(value.createdAt) ||
    !timestamp(value.expiresAt) ||
    microseconds(value.expiresAt)! <= microseconds(value.createdAt)! ||
    !(value.revokedAt === null || timestamp(value.revokedAt))
  )
    throw new Error("Confirmación incompatible");
  return value as ApiCredential;
}

export type ApiCredentialInput = {
  name: string;
  scopes: string[];
  expiresInDays: number;
};
export async function listApiCredentials(signal: AbortSignal, cursor?: string) {
  signal.throwIfAborted();
  const response = await apiRequest(
    "/api/v1/me/api-credentials" +
      (cursor === undefined ? "" : `?cursor=${encodeURIComponent(cursor)}`),
    { signal },
  );
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  const body: unknown = await response.json();
  signal.throwIfAborted();
  if (
    !exact(body, "items nextCursor") ||
    !Array.isArray(body.items) ||
    body.items.length > 50 ||
    !(
      body.nextCursor === null ||
      (typeof body.nextCursor === "string" && body.nextCursor.length > 0)
    )
  )
    throw new Error("Confirmación incompatible");
  const items = body.items.map(decodeCredential);
  if (new Set(items.map((item) => item.id)).size !== items.length)
    throw new Error("Confirmación incompatible");
  return { items, nextCursor: body.nextCursor as string | null };
}
function credentialUrl(id: string) {
  if (!uuid(id) || id !== id.toLowerCase() || id.length !== 36)
    throw new Error("Identidad incompatible");
  return `/api/v1/me/api-credentials/${id}`;
}
export async function readApiCredential(id: string, signal: AbortSignal) {
  signal.throwIfAborted();
  const response = await apiRequest(credentialUrl(id), { signal });
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  const body: unknown = await response.json();
  signal.throwIfAborted();
  const credential = decodeCredential(body);
  if (credential.id !== id) throw new Error("Confirmación incompatible");
  return credential;
}
export async function revokeApiCredential(
  previous: ApiCredential,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  const response = await apiRequest(
    `${credentialUrl(previous.id)}/revocation`,
    { method: "PUT", signal },
  );
  signal.throwIfAborted();
  if (response.status !== 200) throw response;
  const body: unknown = await response.json();
  signal.throwIfAborted();
  const credential = decodeCredential(body);
  if (
    credential.id !== previous.id ||
    credential.name !== previous.name ||
    credential.scopes.join(" ") !== previous.scopes.join(" ") ||
    microseconds(credential.createdAt) !== microseconds(previous.createdAt) ||
    microseconds(credential.expiresAt) !== microseconds(previous.expiresAt) ||
    credential.revokedAt === null ||
    (previous.revokedAt !== null &&
      microseconds(credential.revokedAt) !== microseconds(previous.revokedAt))
  )
    throw new Error("Confirmación incompatible");
  return credential;
}
export async function createApiCredential(
  id: string,
  input: ApiCredentialInput,
  signal: AbortSignal,
) {
  signal.throwIfAborted();
  const response = await apiRequest(credentialUrl(id), {
    method: "PUT",
    signal,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  signal.throwIfAborted();
  if (response.status !== 201 && response.status !== 200) throw response;
  const body: unknown = await response.json();
  signal.throwIfAborted();
  if (!exact(body, "credential secret"))
    throw new Error("Confirmación incompatible");
  const credential = decodeCredential(body.credential);
  if (credential.id !== id) throw new Error("Confirmación incompatible");
  if (
    credential.name !==
      input.name.replace(/^\p{White_Space}+|\p{White_Space}+$/gu, "") ||
    credential.scopes.join(" ") !==
      apiScopes.filter((scope) => input.scopes.includes(scope)).join(" ") ||
    microseconds(credential.expiresAt)! -
      microseconds(credential.createdAt)! !==
      BigInt(input.expiresInDays) * 86_400_000_000n ||
    (response.status === 201 && credential.revokedAt !== null)
  )
    throw new Error("Confirmación incompatible");
  if (response.status === 200 && body.secret !== null)
    throw new Error("Confirmación incompatible");
  if (
    response.status === 201 &&
    (typeof body.secret !== "string" ||
      !body.secret.startsWith(`owp_${id}.`) ||
      !/^[A-Za-z0-9_-]{42}[AEIMQUYcgkosw048]$/.test(
        body.secret.slice(`owp_${id}.`.length),
      ))
  )
    throw new Error("Confirmación incompatible");
  return { credential, secret: body.secret as string | null };
}
