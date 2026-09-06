import assert from "node:assert/strict";

export async function readSession(request) {
  const response = await request.get("/api/session", { maxRedirects: 0 });
  assert.equal(response.status(), 200, "Session query must succeed");
  const session = await response.json();
  assert.deepEqual(Object.keys(session).sort(), [
    "authenticated",
    "csrfHeaderName",
    "csrfToken",
    "username",
  ]);
  assert.equal(typeof session.authenticated, "boolean");
  assert.equal(session.csrfHeaderName, "X-CSRF-TOKEN");
  assert.ok(
    typeof session.csrfToken === "string" && session.csrfToken.length > 0,
    "Expected opaque CSRF token",
  );
  return session;
}

export async function loginSession(request, { username, password }) {
  const anonymous = await readSession(request);
  const response = await request.post("/api/session", {
    form: { username, password },
    headers: { [anonymous.csrfHeaderName]: anonymous.csrfToken },
    maxRedirects: 0,
  });
  assert.equal(
    response.status(),
    204,
    "Login must confirm session persistence",
  );
  const authenticated = await readSession(request);
  assert.equal(authenticated.authenticated, true);
  assert.equal(authenticated.username, username);
  return authenticated;
}

export async function csrfHeaders(request) {
  const session = await readSession(request);
  assert.equal(
    session.authenticated,
    true,
    "Preparatory writes require an authenticated session",
  );
  return { [session.csrfHeaderName]: session.csrfToken };
}
