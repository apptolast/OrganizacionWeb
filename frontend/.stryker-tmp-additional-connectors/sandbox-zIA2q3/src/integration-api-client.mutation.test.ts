// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import { apiScopes, readApiCredential } from "./integration-api-client";

const id = "12345678-1234-4234-8234-123456789abc";
const credential = {
  id,
  name: "Mi integración",
  scopes: ["projects:read"],
  createdAt: "2026-09-08T10:00:00Z",
  expiresAt: "2026-10-08T10:00:00Z",
  revokedAt: null,
};
afterEach(() => vi.unstubAllGlobals());

it("@s13 accepts all six canonical independent scopes and exactly eighty Unicode points", async () => {
  const body = { ...credential, name: "😀".repeat(80), scopes: [...apiScopes] };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(body)));
  expect(await readApiCredential(id, new AbortController().signal)).toEqual(
    body,
  );
});

it.each([
  [],
  ["projects:admin"],
  ["projects:read", "projects:read"],
  ["history:read", "projects:read"],
])("@s13 rejects noncanonical permission metadata %j", async (...scopes) => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(Response.json({ ...credential, scopes })),
  );
  await expect(
    readApiCredential(id, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});

it("@s13 rejects a name exceeding eighty Unicode points in otherwise valid metadata", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...credential, name: "😀".repeat(81) }),
      ),
  );
  await expect(
    readApiCredential(id, new AbortController().signal),
  ).rejects.toThrow("Confirmación incompatible");
});
