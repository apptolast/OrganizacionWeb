import { afterEach, expect, it, vi } from "vitest";
import { readWorkSessionEnd } from "./work-session-end-api";

const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "Europe/Madrid",
};
const state = {
  session,
  status: "running" as const,
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const snapshot = {
  state,
  serverNow: "2026-09-07T10:01:00.123456Z",
  effectiveEndAt: session.plannedEndAt,
};
const token = `work-session-${session.id}-1`;
afterEach(() => vi.unstubAllGlobals());

it("@s13 reads the original end with the state revision and abort signal", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json(snapshot, { headers: { "Work-Session-Revision": token } }),
    );
  vi.stubGlobal("fetch", fetcher);
  const controller = new AbortController();
  await expect(
    readWorkSessionEnd(session.id, controller.signal),
  ).resolves.toEqual({
    ...snapshot,
    token,
  });
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-sessions/${session.id}/end-time`,
    expect.objectContaining({
      credentials: "same-origin",
      cache: "no-store",
      signal: controller.signal,
    }),
  );
});

it("@s26 rejects an end snapshot with an extra field", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, netMicroseconds: "0" },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionEnd(session.id)).rejects.toThrow(
    "Fin de sesión inválido",
  );
});

it("@s26 rejects an unknown state without relaxing State6", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, state: { ...state, status: "extended" } },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionEnd(session.id)).rejects.toThrow(
    "Fin de sesión inválido",
  );
});

it("@s26 rejects a revision header inconsistent with the snapshot", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(snapshot, {
        headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
      }),
    ),
  );
  await expect(readWorkSessionEnd(session.id)).rejects.toThrow(
    "Fin de sesión inválido",
  );
});

it("@s26 rejects an end before the original by one microsecond", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, effectiveEndAt: "2026-09-07T10:25:00.123455Z" },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionEnd(session.id)).rejects.toThrow(
    "Fin de sesión inválido",
  );
});

it("@s26 rejects an end resource belonging to another session", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(snapshot, {
        headers: { "Work-Session-Revision": token },
      }),
    ),
  );
  await expect(
    readWorkSessionEnd("42345678-1234-1234-1234-123456789abc"),
  ).rejects.toThrow("Fin de sesión inválido");
});

it("@s16 preserves the HTTP storage failure for classification", async () => {
  const response = Response.json(
    { code: "STORAGE_UNAVAILABLE" },
    { status: 503 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readWorkSessionEnd(session.id)).rejects.toBe(response);
});

it("@s26 rejects a server clock before the exposed last state change", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, serverNow: "2026-09-07T10:00:00.123455Z" },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionEnd(session.id)).rejects.toThrow(
    "Fin de sesión inválido",
  );
});

it("@s1 confirms EXTEND without changing the state interval or original start", async () => {
  const receipt = {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "EXTEND",
    occurredAt: snapshot.serverNow,
    before: state,
    after: { ...state, revision: "2" },
    extension: {
      additionalMinutes: 1,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:26:00.123456Z",
    },
  };
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(receipt, {
      status: 201,
      headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const { changeWorkSession } = await import("./work-session-state-api");
  const signal = new AbortController().signal;
  await expect(
    changeWorkSession(
      {
        state,
        token,
        key: "52345678-1234-1234-1234-123456789abc",
        action: "EXTEND",
        additionalMinutes: 1,
      },
      signal,
    ),
  ).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-sessions/${session.id}/extend`,
    expect.objectContaining({
      method: "POST",
      signal,
      credentials: "same-origin",
      cache: "no-store",
      body: JSON.stringify({ additionalMinutes: 1 }),
      headers: expect.objectContaining({
        "Idempotency-Key": "52345678-1234-1234-1234-123456789abc",
        "Work-Session-Revision": token,
      }),
    }),
  );
});

function extensionReceipt() {
  return {
    id: "42345678-1234-1234-1234-123456789abc",
    sessionId: session.id,
    action: "EXTEND",
    occurredAt: snapshot.serverNow,
    before: state,
    after: { ...state, revision: "2" },
    extension: {
      additionalMinutes: 1,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-07T10:26:00.123456Z",
    },
  };
}

it("@s26 requires the extension member on an EXTEND receipt read by ID", async () => {
  const { extension, ...invalid } = extensionReceipt();
  expect(extension.additionalMinutes).toBe(1);
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(invalid)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(invalid.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an extension end wrong by one microsecond", async () => {
  const receipt = extensionReceipt();
  receipt.extension.effectiveEndAt = "2026-09-07T10:26:00.123457Z";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an extension whose amount is a numeric string", async () => {
  const receipt = extensionReceipt();
  const invalid = {
    ...receipt,
    extension: { ...receipt.extension, additionalMinutes: "1" },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(invalid)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an extension with a zero amount even when its formula agrees", async () => {
  const receipt = extensionReceipt();
  receipt.extension.additionalMinutes = 0;
  receipt.extension.effectiveEndAt = receipt.extension.previousEndAt;
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an extension exceeding 1440 minutes despite a matching end", async () => {
  const receipt = extensionReceipt();
  receipt.extension.additionalMinutes = 1441;
  receipt.extension.effectiveEndAt = "2026-09-08T10:26:00.123456Z";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects a fractional extension with the controlled decoder error", async () => {
  const receipt = extensionReceipt();
  receipt.extension.additionalMinutes = 1.5;
  receipt.extension.effectiveEndAt = "2026-09-07T10:26:30.123456Z";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an EXTEND that advances a valid running interval", async () => {
  const receipt = extensionReceipt();
  receipt.after = {
    ...receipt.after,
    changedAt: "2026-09-07T10:00:00.123457Z",
    runningSince: "2026-09-07T10:00:00.123457Z",
    workedMicroseconds: "1",
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an EXTEND decision before the exposed state change", async () => {
  const receipt = extensionReceipt();
  receipt.occurredAt = "2026-09-07T10:00:00.123455Z";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 rejects an extension of an end earlier than the original", async () => {
  const receipt = extensionReceipt();
  receipt.extension.previousEndAt = "2026-09-07T10:25:00.123455Z";
  receipt.extension.effectiveEndAt = "2026-09-07T10:26:00.123455Z";
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});

it("@s26 refuses a recovered receipt for another amount of the same intention", async () => {
  const receipt = extensionReceipt();
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  const { recoverWorkSessionChange } = await import("./work-session-state-api");
  await expect(
    recoverWorkSessionChange({
      state,
      token,
      key: "52345678-1234-1234-1234-123456789abc",
      action: "EXTEND",
      additionalMinutes: 2,
    }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 accepts an exact paused extension and end whose epoch microseconds exceed Number precision", async () => {
  const historical = {
    ...session,
    startedAt: "1600-01-01T10:00:00.123456Z",
    plannedEndAt: "1600-01-01T10:25:00.123456Z",
  };
  const before = {
    ...state,
    session: historical,
    status: "paused" as const,
    changedAt: historical.startedAt,
    runningSince: null,
  };
  const receipt = {
    ...extensionReceipt(),
    before,
    after: { ...before, revision: "2" },
    occurredAt: "1600-01-01T11:30:00.123457Z",
    extension: {
      additionalMinutes: 1440,
      previousEndAt: historical.plannedEndAt,
      effectiveEndAt: "1600-01-02T11:30:00.123457Z",
    },
  };
  const end = {
    state: receipt.after,
    serverNow: receipt.occurredAt,
    effectiveEndAt: receipt.extension.effectiveEndAt,
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(receipt))
      .mockResolvedValueOnce(
        Response.json(end, {
          headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
        }),
      ),
  );
  const { readWorkSessionChange } = await import("./work-session-state-api");
  await expect(readWorkSessionChange(receipt.id)).resolves.toEqual(receipt);
  await expect(readWorkSessionEnd(session.id)).resolves.toEqual({
    ...end,
    token: `work-session-${session.id}-2`,
  });
});

it("@s27 recovers a late paused EXTEND without changing its interval or requiring Location", async () => {
  const before = { ...state, status: "paused" as const, runningSince: null };
  const receipt = {
    ...extensionReceipt(),
    before,
    after: { ...before, revision: "2" },
    occurredAt: "2026-09-07T11:30:00.123457Z",
    extension: {
      additionalMinutes: 1440,
      previousEndAt: session.plannedEndAt,
      effectiveEndAt: "2026-09-08T11:30:00.123457Z",
    },
  };
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const { recoverWorkSessionChange } = await import("./work-session-state-api");
  const key = "52345678-1234-1234-1234-123456789abc";
  await expect(
    recoverWorkSessionChange({
      state: before,
      token,
      key,
      action: "EXTEND",
      additionalMinutes: 1440,
    }),
  ).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-session-changes/by-request/${key}`,
    expect.objectContaining({ cache: "no-store", credentials: "same-origin" }),
  );
});
