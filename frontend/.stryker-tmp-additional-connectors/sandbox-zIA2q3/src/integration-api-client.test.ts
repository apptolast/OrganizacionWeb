// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import { setCsrfToken, observeAccess } from "./api-client";
import {
  createApiCredential,
  readApiCredential,
  revokeApiCredential,
  listApiCredentials,
} from "./integration-api-client";

const id = "12345678-1234-4234-8234-123456789abc";
const credential = {
  id,
  name: "Mi integración",
  scopes: ["projects:read"],
  createdAt: "2026-09-08T10:00:00.123456Z",
  expiresAt: "2026-10-08T10:00:00.123456Z",
  revokedAt: null,
};
const intent = {
  name: credential.name,
  scopes: credential.scopes,
  expiresInDays: 30,
};
const secret = `owp_${id}.${"A".repeat(43)}`;
afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
  observeAccess();
});

it("@s1 @s33 creates through the existing cookie and CSRF transport with the stable id", async () => {
  const body = { credential, secret };
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(body, {
      status: 201,
      headers: { Location: `/api/v1/me/api-credentials/${id}` },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("csrf-own-session");
  const signal = new AbortController().signal;
  expect(await createApiCredential(id, intent, signal)).toEqual(body);
  expect(fetcher).toHaveBeenCalledTimes(1);
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/me/api-credentials/${id}`);
  expect(options.method).toBe("PUT");
  expect(options.signal).toBe(signal);
  expect(JSON.parse(options.body)).toEqual(intent);
  expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe(
    "csrf-own-session",
  );
  expect(new Headers(options.headers).get("Content-Type")).toBe(
    "application/json",
  );
  expect(new Headers(options.headers).has("Authorization")).toBe(false);
});

it("@s1 rejects a creation envelope carrying unexpected private fields", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          credential,
          secret,
          verifier: "must-not-reach-ui",
        },
        { status: 201 },
      ),
    ),
  );
  await expect(
    createApiCredential(id, intent, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});

it("@s1 @s10 refuses a confirmation belonging to another credential identity", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          credential: {
            ...credential,
            id: "aaaaaaaa-1234-4234-8234-123456789abc",
          },
          secret,
        },
        { status: 201 },
      ),
    ),
  );
  await expect(
    createApiCredential(id, intent, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});

it("@s17 preserves an HTTP failure even when its payload looks like a confirmation", async () => {
  const failure = Response.json({ credential, secret }, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  await expect(
    createApiCredential(id, intent, new AbortController().signal),
  ).rejects.toBe(failure);
});

it("@s9 a replay confirms metadata but must never return another secret", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ credential, secret: null }))
    .mockResolvedValueOnce(Response.json({ credential, secret }));
  vi.stubGlobal("fetch", fetcher);
  expect(
    await createApiCredential(id, intent, new AbortController().signal),
  ).toEqual({ credential, secret: null });
  await expect(
    createApiCredential(id, intent, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});

it("@s1 validates the new secret as canonical bytes tied to the stable id", async () => {
  for (const invalid of [
    null,
    `owp_aaaaaaaa-1234-4234-8234-123456789abc.${"A".repeat(43)}`,
    `owp_${id}.${"A".repeat(42)}B`,
    `owp_${id}.${"A".repeat(43)}=`,
  ]) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json({ credential, secret: invalid }, { status: 201 }),
        ),
    );
    await expect(
      createApiCredential(id, intent, new AbortController().signal),
    ).rejects.toThrow("Confirmación incompatible");
  }
});

it("@s37 never sends an already retired creation", async () => {
  const controller = new AbortController();
  controller.abort();
  const fetcher = vi
    .fn()
    .mockResolvedValue(Response.json({ credential, secret }, { status: 201 }));
  vi.stubGlobal("fetch", fetcher);
  await expect(
    createApiCredential(id, intent, controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s37 discards a secret resolved after cancellation during JSON decoding", async () => {
  const controller = new AbortController();
  const response = Response.json({ credential, secret }, { status: 201 });
  vi.spyOn(response, "json").mockImplementation(async () => {
    controller.abort();
    return { credential, secret };
  });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    createApiCredential(id, intent, controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
});

it("@s37 a retired fetch response is not decoded", async () => {
  const controller = new AbortController();
  const response = Response.json({ credential, secret }, { status: 201 });
  const json = vi.spyOn(response, "json");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(async () => {
      controller.abort();
      return response;
    }),
  );
  await expect(
    createApiCredential(id, intent, controller.signal),
  ).rejects.toMatchObject({ name: "AbortError" });
  expect(json).not.toHaveBeenCalled();
});

it("@s1 @s5 refuses malformed metadata instead of confirming incompatible credential state", async () => {
  for (const change of [
    { name: 12 },
    { name: "" },
    { name: "a".repeat(81) },
    { name: " name " },
    { name: "bad\nname" },
    { scopes: [] },
    { scopes: ["projects:read", "projects:read"] },
    { scopes: ["unknown"] },
    { scopes: ["tasks:read", "projects:read"] },
    { createdAt: "2026-02-30T10:00:00.123456Z" },
    { expiresAt: credential.createdAt },
    { revokedAt: "2026-09-07T10:00:00.123456Z" },
  ]) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json(
            { credential: { ...credential, ...change }, secret },
            { status: 201 },
          ),
        ),
    );
    await expect(
      createApiCredential(id, intent, new AbortController().signal),
    ).rejects.toThrow("Confirmación incompatible");
  }
});

it("@s1 @s5 a successful creation must confirm the submitted intention and lifetime", async () => {
  for (const change of [
    { name: "Otra" },
    { scopes: ["tasks:read"] },
    { expiresAt: "2026-10-09T10:00:00.123456Z" },
    { revokedAt: credential.createdAt },
  ]) {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json(
            { credential: { ...credential, ...change }, secret },
            { status: 201 },
          ),
        ),
    );
    await expect(
      createApiCredential(id, intent, new AbortController().signal),
    ).rejects.toThrow("Confirmación incompatible");
  }
});

it("@s3 accepts the server White_Space strip including NBSP and NEL without rejecting a valid name", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ credential, secret }, { status: 201 }),
      ),
  );
  expect(
    await createApiCredential(
      id,
      { ...intent, name: `\u0085\u00a0${intent.name}\u0085` },
      new AbortController().signal,
    ),
  ).toEqual({ credential, secret });
});

it("@s5 accepts omitted and short UTC fractions as the same precise lifetime", async () => {
  for (const [createdAt, expiresAt] of [
    ["2026-09-08T10:00:00Z", "2026-10-08T10:00:00Z"],
    ["2026-09-08T10:00:00.123Z", "2026-10-08T10:00:00.123000Z"],
  ]) {
    const body = {
      credential: { ...credential, createdAt, expiresAt },
      secret,
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json(body, { status: 201 })),
    );
    expect(
      await createApiCredential(id, intent, new AbortController().signal),
    ).toEqual(body);
  }
});

it("@s9 retains a replay revocation recorded after the clock moved backwards", async () => {
  const body = {
    credential: { ...credential, revokedAt: "2026-09-07T10:00:00Z" },
    secret: null,
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(body)));
  expect(
    await createApiCredential(id, intent, new AbortController().signal),
  ).toEqual(body);
});

it("@s12 @s35 retrieves metadata by the retained identity without sending a write", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(credential));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  expect(await readApiCredential(id, signal)).toEqual(credential);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/me/api-credentials/${id}`,
    { signal },
  );
});

it("@s35 keeps a missing receipt response available to manual recovery", async () => {
  const missing = Response.json(
    { code: "API_CREDENTIAL_NOT_FOUND" },
    { status: 404 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(missing));
  await expect(
    readApiCredential(id, new AbortController().signal),
  ).rejects.toBe(missing);
});

it("@s12 a lookup cannot resolve the retained intention with a different credential", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...credential,
        id: "aaaaaaaa-1234-4234-8234-123456789abc",
      }),
    ),
  );
  await expect(
    readApiCredential(id, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});

it("@s37 recovery respects cancellation before transport, before JSON and after JSON", async () => {
  for (const phase of ["before", "fetch", "json"]) {
    const controller = new AbortController();
    const response = Response.json(credential);
    const json = vi.spyOn(response, "json").mockImplementation(async () => {
      if (phase === "json") controller.abort();
      return credential;
    });
    const fetcher = vi.fn().mockImplementation(async () => {
      if (phase === "fetch") controller.abort();
      return response;
    });
    vi.stubGlobal("fetch", fetcher);
    if (phase === "before") controller.abort();
    await expect(
      readApiCredential(id, controller.signal),
    ).rejects.toMatchObject({ name: "AbortError" });
    if (phase === "before") expect(fetcher).not.toHaveBeenCalled();
    if (phase !== "json") expect(json).not.toHaveBeenCalled();
  }
});

it("@s2 never builds an endpoint from an invalid or noncanonical intention id", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(credential));
  vi.stubGlobal("fetch", fetcher);
  for (const invalid of [id.toUpperCase(), "../session", "", id + "\n"]) {
    await expect(
      readApiCredential(invalid, new AbortController().signal),
    ).rejects.toThrow("Identidad incompatible");
    await expect(
      createApiCredential(invalid, intent, new AbortController().signal),
    ).rejects.toThrow("Identidad incompatible");
  }
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s15 @s16 revokes the selected credential with an empty CSRF-protected PUT", async () => {
  const revoked = { ...credential, revokedAt: credential.createdAt };
  const fetcher = vi.fn().mockResolvedValue(Response.json(revoked));
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("csrf-revoke");
  const signal = new AbortController().signal;
  expect(await revokeApiCredential(credential, signal)).toEqual(revoked);
  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe(`/api/v1/me/api-credentials/${id}/revocation`);
  expect(options.method).toBe("PUT");
  expect(options.body).toBeUndefined();
  expect(options.signal).toBe(signal);
  expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe("csrf-revoke");
});

it("@s15 @s16 revocation must confirm only the irreversible change of the same credential", async () => {
  const revoked = { ...credential, revokedAt: credential.createdAt };
  for (const change of [
    { revokedAt: null },
    { id: "aaaaaaaa-1234-4234-8234-123456789abc" },
    { name: "Changed" },
    { scopes: ["tasks:read"] },
    { expiresAt: "2026-10-09T10:00:00Z" },
    { revokedAt: "2026-09-09T10:00:00Z" },
  ]) {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json({ ...revoked, ...change })),
    );
    await expect(
      revokeApiCredential(revoked, new AbortController().signal),
    ).rejects.toThrow("Confirmación incompatible");
  }
});

it("@s17 @s37 revocation preserves HTTP failure and all cancellation boundaries", async () => {
  const revoked = { ...credential, revokedAt: credential.createdAt };
  const failure = Response.json(revoked, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  await expect(
    revokeApiCredential(credential, new AbortController().signal),
  ).rejects.toBe(failure);
  for (const phase of ["before", "fetch", "json"]) {
    const controller = new AbortController();
    const response = Response.json(revoked);
    const json = vi.spyOn(response, "json").mockImplementation(async () => {
      if (phase === "json") controller.abort();
      return revoked;
    });
    const fetcher = vi.fn().mockImplementation(async () => {
      if (phase === "fetch") controller.abort();
      return response;
    });
    vi.stubGlobal("fetch", fetcher);
    if (phase === "before") controller.abort();
    await expect(
      revokeApiCredential(credential, controller.signal),
    ).rejects.toMatchObject({ name: "AbortError" });
    if (phase === "before") expect(fetcher).not.toHaveBeenCalled();
    if (phase !== "json") expect(json).not.toHaveBeenCalled();
  }
});

it("@s13 reads a metadata page and encodes the opaque cursor without adding owner query", async () => {
  const page = { items: [credential], nextCursor: "opaque-next" };
  const fetcher = vi.fn().mockResolvedValue(Response.json(page));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  expect(await listApiCredentials(signal, "cursor+/=?")).toEqual(page);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    "/api/v1/me/api-credentials?cursor=cursor%2B%2F%3D%3F",
    { signal },
  );
});

it("@s12 @s13 rejects incompatible or secret-bearing pages before presenting the list", async () => {
  for (const page of [
    { items: [credential], nextCursor: null, secret: "unexpected" },
    { items: [{ ...credential, secret: "unexpected" }], nextCursor: null },
    { items: [{ ...credential, id: "not-an-id" }], nextCursor: null },
    { items: [credential, credential], nextCursor: null },
    { items: Array.from({ length: 51 }, () => credential), nextCursor: null },
    { items: [], nextCursor: 42 },
    { items: [], nextCursor: "" },
  ]) {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(page)));
    await expect(
      listApiCredentials(new AbortController().signal),
    ).rejects.toThrow("Confirmación incompatible");
  }
});

it("@s13 @s37 page reading preserves HTTP errors and cancellation boundaries", async () => {
  const page = { items: [], nextCursor: null };
  const failure = Response.json(page, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(failure));
  await expect(listApiCredentials(new AbortController().signal)).rejects.toBe(
    failure,
  );
  for (const phase of ["before", "fetch", "json"]) {
    const controller = new AbortController();
    const response = Response.json(page);
    const json = vi.spyOn(response, "json").mockImplementation(async () => {
      if (phase === "json") controller.abort();
      return page;
    });
    const fetcher = vi.fn().mockImplementation(async () => {
      if (phase === "fetch") controller.abort();
      return response;
    });
    vi.stubGlobal("fetch", fetcher);
    if (phase === "before") controller.abort();
    await expect(listApiCredentials(controller.signal)).rejects.toMatchObject({
      name: "AbortError",
    });
    if (phase === "before") expect(fetcher).not.toHaveBeenCalled();
    if (phase !== "json") expect(json).not.toHaveBeenCalled();
  }
});

it("@s37 the shared transport does not retire the next session for a cancelled late 401", async () => {
  const controller = new AbortController();
  const access = vi.fn();
  observeAccess(access);
  let resolve!: (value: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation(
      () =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    ),
  );
  const pending = createApiCredential(id, intent, controller.signal);
  controller.abort();
  resolve(new Response(null, { status: 401 }));
  await expect(pending).rejects.toMatchObject({ name: "AbortError" });
  expect(access).not.toHaveBeenCalled();
});
