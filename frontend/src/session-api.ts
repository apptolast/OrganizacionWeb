export type Session = {
  authenticated: boolean;
  username: string | null;
  csrfToken: string;
  csrfHeaderName: "X-CSRF-TOKEN";
};
export async function readSession(signal: AbortSignal): Promise<Session> {
  const response = await fetch("/api/session", {
    credentials: "same-origin",
    cache: "no-store",
    signal,
  });
  if (response.status !== 200) throw response;
  const data: unknown = await response.json();
  if (
    !data ||
    typeof data !== "object" ||
    Object.keys(data).length !== 4 ||
    !("authenticated" in data) ||
    typeof data.authenticated !== "boolean" ||
    !("username" in data) ||
    (data.authenticated
      ? typeof data.username !== "string" || !data.username
      : data.username !== null) ||
    !("csrfToken" in data) ||
    typeof data.csrfToken !== "string" ||
    !data.csrfToken ||
    !("csrfHeaderName" in data) ||
    data.csrfHeaderName !== "X-CSRF-TOKEN"
  )
    throw new Error("invalid_session");
  return data as Session;
}
