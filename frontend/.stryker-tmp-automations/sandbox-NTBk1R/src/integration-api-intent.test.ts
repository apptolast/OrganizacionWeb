// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import {
  saveApiCredentialIntent,
  readApiCredentialIntent,
  clearApiCredentialIntent,
} from "./integration-api-intent";
const id = "12345678-1234-4234-8234-123456789abc";
const key = "organizationweb.api-credential.pending.v1";
afterEach(() => {
  vi.restoreAllMocks();
  sessionStorage.clear();
});
it("@s33 stores only owner and stable id before creation and can recover them", () => {
  saveApiCredentialIntent({
    owner: "Ana",
    id,
    secret: "not-persisted",
    name: "Local draft",
  } as { owner: string; id: string });
  expect(JSON.parse(sessionStorage.getItem(key)!)).toEqual({
    owner: "Ana",
    id,
  });
  expect(readApiCredentialIntent("Ana")).toEqual({ owner: "Ana", id });
});

it("@s35 @s37 discards foreign, malformed or noncanonical stored intentions", () => {
  for (const raw of [
    "{",
    JSON.stringify({ owner: "Bruno", id }),
    JSON.stringify({ owner: "Ana", id: id.toUpperCase() }),
    JSON.stringify({ owner: "Ana", id, secret: "unexpected" }),
    JSON.stringify({ owner: "Ana", id: "wrong" }),
  ]) {
    sessionStorage.setItem(key, raw);
    expect(readApiCredentialIntent("Ana")).toBeNull();
    expect(sessionStorage.getItem(key)).toBeNull();
  }
});

it("@s34 refuses to proceed when storage silently fails to retain the identity", () => {
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
  expect(() => saveApiCredentialIntent({ owner: "Ana", id })).toThrow(
    "No se puede conservar la intención",
  );
});

it("@s38 clearing a resolved intention touches only its own storage key", () => {
  saveApiCredentialIntent({ owner: "Ana", id });
  sessionStorage.setItem("other-draft", "preserved");
  clearApiCredentialIntent();
  expect(sessionStorage.getItem(key)).toBeNull();
  expect(sessionStorage.getItem("other-draft")).toBe("preserved");
});
