let csrfToken: string | undefined;
let onUnauthorized: ((status: 401 | 403) => void) | undefined;
export function setCsrfToken(token?: string) {
  csrfToken = token;
}
export function observeAccess(listener?: (status: 401 | 403) => void) {
  onUnauthorized = listener;
}
export async function isCsrfFailure(error: unknown) {
  if (!(error instanceof Response) || error.status !== 403) return false;
  const body: unknown = await error
    .clone()
    .json()
    .catch(() => null);
  return Boolean(
    body &&
    typeof body === "object" &&
    "code" in body &&
    body.code === "CSRF_INVALID",
  );
}
export async function apiRequest(url: string, options: RequestInit = {}) {
  if (options.method && options.method !== "GET" && csrfToken) {
    const headers = new Headers(options.headers);
    headers.set("X-CSRF-TOKEN", csrfToken);
    options = { ...options, headers };
  }
  const response = await fetch(url, {
    ...options,
  });
  if (!options.signal?.aborted) {
    if (response.status === 401) onUnauthorized?.(401);
    if ((await isCsrfFailure(response)) && !options.signal?.aborted)
      onUnauthorized?.(403);
  }
  return response;
}
