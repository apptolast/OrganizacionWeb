import { afterEach, expect, it, vi } from "vitest";
import {
  readImportIntent,
  saveImportIntent,
  clearImportIntent,
} from "./import-data-intent";

const key = "organizationweb.import.pending.v1";
it("@s38 discards an uppercase UUID that the receipt endpoint would reject", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "ABCDEFAB-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    }),
  );
  expect(readImportIntent("Ana")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
});
afterEach(() => sessionStorage.removeItem(key));

it("@s38 rejects a trailing newline after the stored digest", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64) + "\n",
    }),
  );
  expect(readImportIntent("Ana")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
});

it("@s39 clears only import metadata on deliberate logout", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    }),
  );
  sessionStorage.setItem("unrelated", "keep");
  clearImportIntent();
  expect(sessionStorage.getItem(key)).toBeNull();
  expect(sessionStorage.getItem("unrelated")).toBe("keep");
  sessionStorage.removeItem("unrelated");
});

it("@s36 never persists incidental file data carried by its caller", () => {
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: "a".repeat(64),
    fileName: "private.json",
  };
  saveImportIntent(intent);
  expect(JSON.parse(sessionStorage.getItem(key)!)).toEqual({
    owner: intent.owner,
    requestKey: intent.requestKey,
    fileSha256: intent.fileSha256,
  });
});

it("@s36 refuses to confirm when storage silently discards the intention", () => {
  vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {});
  expect(() =>
    saveImportIntent({
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    }),
  ).toThrow();
});

it("@s36 stores only owner key and digest before confirmation", () => {
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: "a".repeat(64),
  };
  saveImportIntent(intent);
  expect(JSON.parse(sessionStorage.getItem(key)!)).toEqual(intent);
  expect(readImportIntent("Ana")).toEqual(intent);
});

it("@s38 rejects a noncanonical stored digest", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "A".repeat(64),
    }),
  );
  expect(readImportIntent("Ana")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
});

it("@s38 rejects a noncanonical request key in stored metadata", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "not-a-uuid",
      fileSha256: "a".repeat(64),
    }),
  );
  expect(readImportIntent("Ana")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
});

it("@s38 removes malformed JSON without exposing its contents", () => {
  sessionStorage.setItem(key, '{"private":');
  expect(readImportIntent("Ana")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
});

it("@s38 discards metadata containing an unapproved field", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
      fileName: "private.json",
    }),
  );
  expect(readImportIntent("Ana")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
});

it("@s39 retires another owner's pending metadata while preserving other storage", () => {
  sessionStorage.setItem(
    key,
    JSON.stringify({
      owner: "Ana",
      requestKey: "00000000-0000-4000-8000-000000000001",
      fileSha256: "a".repeat(64),
    }),
  );
  sessionStorage.setItem("unrelated", "preserved");
  expect(readImportIntent("Bruno")).toBeNull();
  expect(sessionStorage.getItem(key)).toBeNull();
  expect(sessionStorage.getItem("unrelated")).toBe("preserved");
  sessionStorage.removeItem("unrelated");
});

it("@s38 reads only a pending intention owned by the current session", () => {
  const intent = {
    owner: "Ana",
    requestKey: "00000000-0000-4000-8000-000000000001",
    fileSha256: "a".repeat(64),
  };
  sessionStorage.setItem(key, JSON.stringify(intent));
  expect(readImportIntent("Ana")).toEqual(intent);
  expect(sessionStorage.getItem(key)).toBe(JSON.stringify(intent));
});
