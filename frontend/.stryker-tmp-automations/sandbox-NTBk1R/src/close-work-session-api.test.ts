// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import {
  readWorkSessionState,
  changeWorkSession,
  readWorkSessionClosure,
  recoverWorkSessionChange,
} from "./work-session-state-api";
const session = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-07T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-07T10:25:00.123456Z",
  zoneId: "UTC",
};
const before = {
  session,
  status: "running" as const,
  revision: "1",
  changedAt: session.startedAt,
  workedMicroseconds: "0",
  runningSince: session.startedAt,
};
const closed = {
  ...before,
  status: "closed" as const,
  revision: "2",
  changedAt: "2026-09-07T10:00:01.123457Z",
  workedMicroseconds: "1000001",
  runningSince: null,
};
afterEach(() => vi.unstubAllGlobals());
it("@s24 reads a closed snapshot with its fixed final work", async () => {
  const snapshot = {
    state: closed,
    serverNow: "2026-09-08T12:00:00Z",
    netMicroseconds: "1000001",
  };
  const token = `work-session-${session.id}-2`;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(snapshot, {
        headers: { "Work-Session-Revision": token },
      }),
    ),
  );
  await expect(readWorkSessionState(session.id)).resolves.toEqual({
    ...snapshot,
    token,
  });
});
const closure = {
  progressNote: "Avance parcial",
  nextStep: "Continuar",
  workDate: "2026-09-07",
  closeZoneId: "UTC",
};
const receipt = {
  id: "42345678-1234-1234-1234-123456789abc",
  sessionId: session.id,
  action: "CLOSE",
  occurredAt: closed.changedAt,
  before,
  after: closed,
  closure,
};
const intent = {
  state: before,
  token: `work-session-${session.id}-1`,
  key: "52345678-1234-1234-1234-123456789abc",
  action: "CLOSE" as const,
  progressNote: closure.progressNote,
  nextStep: closure.nextStep,
};
it("@s1 sends an explicit close and accepts its exact final receipt", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(receipt, {
      status: 201,
      headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  await expect(changeWorkSession(intent)).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/work-sessions/${session.id}/close`,
    expect.objectContaining({
      method: "POST",
      body: JSON.stringify({
        progressNote: closure.progressNote,
        nextStep: closure.nextStep,
      }),
      headers: expect.objectContaining({
        "Idempotency-Key": intent.key,
        "Work-Session-Revision": intent.token,
      }),
    }),
  );
});
it("@s2 accepts a close from paused without adding the break", async () => {
  const paused = { ...before, status: "paused" as const, runningSince: null };
  const result = {
    ...receipt,
    before: paused,
    after: { ...closed, workedMicroseconds: "0" },
  };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(result, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    ),
  );
  await expect(
    changeWorkSession({ ...intent, state: paused }),
  ).resolves.toEqual(result);
});
it("@s30 rejects a CLOSE receipt missing its closure", async () => {
  const { closure: omitted, ...invalid } = receipt;
  expect(omitted).toBe(closure);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(invalid, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    ),
  );
  await expect(changeWorkSession(intent)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s30 rejects a closure note which is null rather than normalized text", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, closure: { ...closure, nextStep: null } },
        {
          status: 200,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(readWorkSessionClosure(session.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s30 rejects a closure note longer than 2000 code points", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...receipt,
          closure: { ...closure, progressNote: "x".repeat(2001) },
        },
        {
          status: 200,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(readWorkSessionClosure(session.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s30 rejects a decoded NUL in a closure note", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, closure: { ...closure, nextStep: "a\u0000b" } },
        {
          status: 200,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(readWorkSessionClosure(session.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s30 rejects an isolated surrogate in a closure note", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, closure: { ...closure, progressNote: "\ud800" } },
        {
          status: 200,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(readWorkSessionClosure(session.id)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s4 preserves 2000 supplementary Unicode code points in notes", async () => {
  const text = "😀".repeat(2000);
  const result = { ...receipt, closure: { ...closure, progressNote: text } };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(result, {
        status: 201,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    ),
  );
  await expect(
    changeWorkSession({ ...intent, progressNote: text }),
  ).resolves.toEqual(result);
});
it("@s30 rejects a non-calendar workDate in the closure", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, closure: { ...closure, workDate: "2026-02-30" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(changeWorkSession(intent)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s30 rejects a non-string persisted closing zone", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, closure: { ...closure, closeZoneId: 1 } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(changeWorkSession(intent)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s26 reads the historical closure by session identity without Location", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(readWorkSessionClosure(session.id, signal)).resolves.toEqual(
    receipt,
  );
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/work-sessions/${session.id}/closure`,
    expect.objectContaining({
      signal,
      cache: "no-store",
      credentials: "same-origin",
    }),
  );
});
it("@s35 refuses a valid by-key closure with different retained notes", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...receipt,
        closure: { ...closure, nextStep: "Otra intención" },
      }),
    ),
  );
  await expect(recoverWorkSessionChange(intent)).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s28 recovers the original CLOSE by its retained key without Location", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  await expect(recoverWorkSessionChange(intent)).resolves.toEqual(receipt);
});
it("@s27 preserves a failed closure lookup as 503 rather than absence", async () => {
  const unavailable = new Response(null, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(unavailable));
  await expect(readWorkSessionClosure(session.id)).rejects.toBe(unavailable);
});
it("@s30 rejects a coherent closed state which adds paused time", async () => {
  const paused = { ...before, status: "paused" as const, runningSince: null };
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, before: paused },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(changeWorkSession({ ...intent, state: paused })).rejects.toThrow(
    "Cambio de sesión inválido",
  );
});
it("@s31 accepts persisted attribution without resolving the historical zone", async () => {
  const result = {
    ...receipt,
    closure: {
      ...closure,
      workDate: "2026-09-08",
      closeZoneId: "Historical/Unavailable",
    },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(result)));
  await expect(readWorkSessionClosure(session.id)).resolves.toEqual(result);
});
it("@s33 refuses a closure belonging to a different requested session", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  await expect(
    readWorkSessionClosure("62345678-1234-1234-1234-123456789abc"),
  ).rejects.toThrow("Cambio de sesión inválido");
});
