import { afterEach, expect, it, vi } from "vitest";
import {
  readWorkSessionState,
  changeWorkSession,
  readWorkSessionChange,
  recoverWorkSessionChange,
  readWorkSessionStateError,
} from "./work-session-state-api";
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
  serverNow: "2026-09-07T10:00:01.123456Z",
  netMicroseconds: "1000000",
};
const token = `work-session-${session.id}-1`;
afterEach(() => vi.unstubAllGlobals());
it("@s1 reads the existing start state with its own revision header", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValue(
      Response.json(snapshot, { headers: { "Work-Session-Revision": token } }),
    );
  vi.stubGlobal("fetch", fetcher);
  const controller = new AbortController();
  await expect(
    readWorkSessionState(session.id, controller.signal),
  ).resolves.toEqual({ ...snapshot, token });
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-sessions/${session.id}/state`,
    expect.objectContaining({
      credentials: "same-origin",
      cache: "no-store",
      signal: controller.signal,
    }),
  );
});

it("@s27 rejects a state header for a different revision", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(snapshot, {
        headers: { "Work-Session-Revision": `work-session-${session.id}-2` },
      }),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s24 preserves storage failure as the HTTP response", async () => {
  const response = Response.json({ status: 503 }, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readWorkSessionState(session.id)).rejects.toBe(response);
});

it("@s27 rejects an extra snapshot field", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, extra: true },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects an extra state field", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, state: { ...state, extra: true } },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects a nested start whose planned end differs by one microsecond", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...snapshot,
          state: {
            ...state,
            session: {
              ...session,
              plannedEndAt: "2026-09-07T10:25:00.123457Z",
            },
          },
        },
        { headers: { "Work-Session-Revision": token } },
      ),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects a coherent state belonging to another requested session", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(snapshot, {
        headers: { "Work-Session-Revision": token },
      }),
    ),
  );
  await expect(
    readWorkSessionState("42345678-1234-1234-1234-123456789abc"),
  ).rejects.toThrow("Estado de sesión inválido");
});

it("@s27 rejects a revision beyond BIGINT even with a matching header", async () => {
  const revision = "9223372036854775808";
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...snapshot, state: { ...state, revision } },
        {
          headers: {
            "Work-Session-Revision": `work-session-${session.id}-${revision}`,
          },
        },
      ),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects changedAt preceding the original start by one microsecond", async () => {
  const changedAt = "2026-09-07T10:00:00.123455Z";
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...snapshot,
          state: { ...state, changedAt, runningSince: changedAt },
          netMicroseconds: "1000001",
        },
        { headers: { "Work-Session-Revision": token } },
      ),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects an unknown operative status", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, state: { ...state, status: "closed" } },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects a running interval which differs from changedAt", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...snapshot,
          state: { ...state, runningSince: "2026-09-07T10:00:00.123457Z" },
          netMicroseconds: "999999",
        },
        { headers: { "Work-Session-Revision": token } },
      ),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects an open interval in paused state", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...snapshot,
          state: { ...state, status: "paused" },
          netMicroseconds: "0",
        },
        { headers: { "Work-Session-Revision": token } },
      ),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects accumulated work exceeding elapsed time by one microsecond", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...snapshot,
          state: {
            ...state,
            status: "paused",
            runningSince: null,
            workedMicroseconds: "1",
          },
          netMicroseconds: "1",
        },
        { headers: { "Work-Session-Revision": token } },
      ),
    ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s27 rejects a net amount off by one microsecond", async () => {
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { ...snapshot, netMicroseconds: "1000001" },
          { headers: { "Work-Session-Revision": token } },
        ),
      ),
  );
  await expect(readWorkSessionState(session.id)).rejects.toThrow(
    "Estado de sesión inválido",
  );
});

it("@s23 accepts an earlier server clock without negative open work", async () => {
  const value = {
    ...snapshot,
    serverNow: "2026-09-07T10:00:00.123455Z",
    netMicroseconds: "0",
  };
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(value, { headers: { "Work-Session-Revision": token } }),
      ),
  );
  await expect(readWorkSessionState(session.id)).resolves.toEqual({
    ...value,
    token,
  });
});

it("@s28 preserves a paused snapshot with exact values above Number precision", async () => {
  const old = {
    ...session,
    startedAt: "1700-01-01T00:00:00Z",
    plannedEndAt: "1700-01-01T00:25:00Z",
  };
  const value = {
    state: {
      ...state,
      session: old,
      status: "paused",
      revision: "9007199254740993",
      workedMicroseconds: "9007199254740993",
      changedAt: snapshot.serverNow,
      runningSince: null,
    },
    serverNow: snapshot.serverNow,
    netMicroseconds: "9007199254740993",
  };
  const header = `work-session-${session.id}-${value.state.revision}`;
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json(value, { headers: { "Work-Session-Revision": header } }),
      ),
  );
  await expect(readWorkSessionState(session.id)).resolves.toEqual({
    ...value,
    token: header,
  });
});

const key = "42345678-1234-1234-1234-123456789abc";
const paused = {
  ...state,
  status: "paused" as const,
  revision: "2",
  changedAt: snapshot.serverNow,
  workedMicroseconds: "1000000",
  runningSince: null,
};
const receipt = {
  id: "52345678-1234-1234-1234-123456789abc",
  sessionId: session.id,
  action: "PAUSE" as const,
  occurredAt: snapshot.serverNow,
  before: state,
  after: paused,
};
it("@s2 transmits one pause intention and accepts its committed receipt", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(receipt, {
      status: 201,
      headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }, signal),
  ).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-sessions/${session.id}/pause`,
    expect.objectContaining({
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      body: "{}",
      headers: expect.objectContaining({
        "Content-Type": "application/json",
        "Idempotency-Key": key,
        "Work-Session-Revision": token,
      }),
    }),
  );
});

it("@s3 sends resume with the paused revision without adding pause duration", async () => {
  const resumed = {
    ...state,
    revision: "3",
    changedAt: "2026-09-07T11:00:00Z",
    runningSince: "2026-09-07T11:00:00Z",
    workedMicroseconds: paused.workedMicroseconds,
  };
  const result = {
    ...receipt,
    action: "RESUME",
    occurredAt: resumed.changedAt,
    before: paused,
    after: resumed,
  };
  const header = `work-session-${session.id}-2`;
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(result, {
      status: 201,
      headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  await expect(
    changeWorkSession({ state: paused, token: header, key, action: "RESUME" }),
  ).resolves.toEqual(result);
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-sessions/${session.id}/resume`,
    expect.objectContaining({
      headers: expect.objectContaining({ "Work-Session-Revision": header }),
      body: "{}",
    }),
  );
});

it("@s33 preserves a rejected revision response rather than confirming it", async () => {
  const response = Response.json(
    { code: "PRECONDITION_FAILED" },
    { status: 412 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toBe(response);
});

it("@s27 rejects a command receipt with an extra field", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, extra: true },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a command receipt with a malformed before state", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, before: { ...state, runningSince: null } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a command receipt with an impossible after state", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, after: { ...paused, workedMicroseconds: "1000001" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a receipt whose revision does not advance exactly once", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, after: { ...paused, revision: "3" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a receipt which changes the captured session zone", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        {
          ...receipt,
          after: { ...paused, session: { ...session, zoneId: "UTC" } },
        },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a change occurrence differing from its after timestamp", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, occurredAt: "2026-09-07T10:00:01.123457Z" },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a receipt whose action contradicts its transition", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, action: "RESUME" },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a pause receipt omitting one worked microsecond", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, after: { ...paused, workedMicroseconds: "999999" } },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a receipt for another session identity", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, sessionId: "62345678-1234-1234-1234-123456789abc" },
        {
          status: 201,
          headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a valid change from a different expected revision", async () => {
  const result = {
    ...receipt,
    before: { ...state, revision: "3" },
    after: { ...paused, revision: "4" },
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
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a valid receipt for a different captured session", async () => {
  const other = { ...session, id: "62345678-1234-1234-1234-123456789abc" };
  const result = {
    ...receipt,
    sessionId: other.id,
    before: { ...state, session: other },
    after: { ...paused, session: other },
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
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a coherent opposite action for the retained intention", async () => {
  const before = { ...state, status: "paused", runningSince: null };
  const after = {
    ...state,
    revision: "2",
    changedAt: snapshot.serverNow,
    runningSince: snapshot.serverNow,
  };
  const result = { ...receipt, action: "RESUME", before, after };
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
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a POST location which identifies another receipt", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(receipt, {
        status: 201,
        headers: {
          Location:
            "/api/v1/work-session-changes/62345678-1234-1234-1234-123456789abc",
        },
      }),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a malformed change identity despite matching Location", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, id: "invalid" },
        {
          status: 201,
          headers: { Location: "/api/v1/work-session-changes/invalid" },
        },
      ),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s27 rejects a resume receipt moving the transition clock backwards", async () => {
  const before = { ...paused, workedMicroseconds: "0" };
  const after = {
    ...state,
    revision: "3",
    changedAt: "2026-09-07T10:00:00.623456Z",
    runningSince: "2026-09-07T10:00:00.623456Z",
  };
  const result = {
    ...receipt,
    action: "RESUME",
    occurredAt: after.changedAt,
    before,
    after,
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
    changeWorkSession({
      state: before,
      token: `work-session-${session.id}-2`,
      key,
      action: "RESUME",
    }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s25 reads a historical change by ID without Location", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(readWorkSessionChange(receipt.id, signal)).resolves.toEqual(
    receipt,
  );
  expect(fetcher).toHaveBeenCalledWith(
    `/api/v1/work-session-changes/${receipt.id}`,
    expect.objectContaining({
      credentials: "same-origin",
      cache: "no-store",
      signal,
    }),
  );
});

it("@s27 rejects a different historical change than the requested ID", async () => {
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(receipt)));
  await expect(
    readWorkSessionChange("62345678-1234-1234-1234-123456789abc"),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s24 preserves a storage error while reading a historical change", async () => {
  const response = Response.json({ status: 503 }, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readWorkSessionChange(receipt.id)).rejects.toBe(response);
});

it("@s25 recovers the retained change by its key without another POST", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(
    recoverWorkSessionChange({ state, token, key, action: "PAUSE" }, signal),
  ).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/work-session-changes/by-request/${key}`,
    expect.objectContaining({
      credentials: "same-origin",
      cache: "no-store",
      signal,
    }),
  );
});

it("@s32 refuses a by-key receipt for another retained revision", async () => {
  const result = {
    ...receipt,
    before: { ...state, revision: "3" },
    after: { ...paused, revision: "4" },
  };
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(result)));
  await expect(
    recoverWorkSessionChange({ state, token, key, action: "PAUSE" }),
  ).rejects.toThrow("Cambio de sesión inválido");
});

it("@s32 preserves a by-key 404 for deliberate recovery decisions", async () => {
  const response = Response.json(
    { code: "WORK_SESSION_CHANGE_NOT_FOUND" },
    { status: 404 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    recoverWorkSessionChange({ state, token, key, action: "PAUSE" }),
  ).rejects.toBe(response);
});

it("@s32 recognizes the missing change problem for explicit checking", async () => {
  const problem = {
    type: "urn:organization:problem:work_session_change_not_found",
    title: "No se ha encontrado el cambio de sesión.",
    status: 404,
    code: "WORK_SESSION_CHANGE_NOT_FOUND",
  };
  await expect(
    readWorkSessionStateError(Response.json(problem, { status: 404 })),
  ).resolves.toEqual(problem);
});

it("@s33 recognizes a stale session revision", async () => {
  const problem = {
    type: "urn:organization:problem:precondition_failed",
    title: "La revisión ha cambiado.",
    status: 412,
    code: "PRECONDITION_FAILED",
  };
  await expect(
    readWorkSessionStateError(Response.json(problem, { status: 412 })),
  ).resolves.toEqual(problem);
});

it("@s34 preserves the inherited CSRF problem for manual SessionGate recovery", async () => {
  const problem = {
    type: "urn:organization:problem:csrf_invalid",
    title: "Acceso pendiente de recuperar.",
    status: 403,
    code: "CSRF_INVALID",
  };
  await expect(
    readWorkSessionStateError(Response.json(problem, { status: 403 })),
  ).resolves.toEqual(problem);
});

it("@s14 recognizes an incompatible session state", async () => {
  const problem = {
    type: "urn:organization:problem:work_session_state_conflict",
    title: "El estado de la sesión no permite esta acción.",
    status: 409,
    code: "WORK_SESSION_STATE_CONFLICT",
  };
  await expect(
    readWorkSessionStateError(Response.json(problem, { status: 409 })),
  ).resolves.toEqual(problem);
});

it("@s15 recognizes exhausted session revisions", async () => {
  const problem = {
    type: "urn:organization:problem:work_session_revision_exhausted",
    title: "La sesión no admite más revisiones.",
    status: 409,
    code: "WORK_SESSION_REVISION_EXHAUSTED",
  };
  await expect(
    readWorkSessionStateError(Response.json(problem, { status: 409 })),
  ).resolves.toEqual(problem);
});

it("@s32 ignores a contradictory status in a recognized change problem", async () => {
  const problem = {
    type: "urn:organization:problem:work_session_change_not_found",
    title: "No se ha encontrado el cambio de sesión.",
    status: 409,
    code: "WORK_SESSION_CHANGE_NOT_FOUND",
  };
  await expect(
    readWorkSessionStateError(Response.json(problem, { status: 404 })),
  ).resolves.toBeNull();
});

it("@s16 accepts an unchanged replay receipt at HTTP200", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(receipt, {
        status: 200,
        headers: { Location: `/api/v1/work-session-changes/${receipt.id}` },
      }),
    ),
  );
  await expect(
    changeWorkSession({ state, token, key, action: "PAUSE" }),
  ).resolves.toEqual(receipt);
});
