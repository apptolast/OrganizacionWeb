// @ts-nocheck
import { afterEach, expect, it, vi } from "vitest";
import { setCsrfToken } from "./api-client";
import {
  readActiveWorkSession,
  startWorkSession,
  readWorkSession,
  recoverWorkSession,
  readWorkSessionError,
} from "./work-session-api";

const receipt = {
  id: "12345678-1234-1234-1234-123456789abc",
  projectId: "22345678-1234-1234-1234-123456789abc",
  taskId: "32345678-1234-1234-1234-123456789abc",
  startedAt: "2026-09-06T10:00:00.123456Z",
  plannedMinutes: 25,
  plannedEndAt: "2026-09-06T10:25:00.123456Z",
  zoneId: "Europe/Madrid",
};
const key = "42345678-1234-1234-1234-123456789abc";
it.each([
  {
    label: "time range at 404",
    code: "WORK_SESSION_TIME_OUT_OF_RANGE",
    httpStatus: 404,
    bodyStatus: 404,
    type: "urn:organization:problem:work_session_time_out_of_range",
  },
  {
    label: "missing receipt at 409",
    code: "WORK_SESSION_NOT_FOUND",
    httpStatus: 409,
    bodyStatus: 409,
    type: "urn:organization:problem:work_session_not_found",
  },
  {
    label: "unknown code at 409",
    code: "UNKNOWN",
    httpStatus: 409,
    bodyStatus: 409,
    type: "urn:organization:problem:unknown",
  },
  {
    label: "unknown code at 404",
    code: "UNKNOWN",
    httpStatus: 404,
    bodyStatus: 404,
    type: "urn:organization:problem:unknown",
  },
  {
    label: "body status",
    code: "WORK_SESSION_NOT_FOUND",
    httpStatus: 404,
    bodyStatus: 409,
    type: "urn:organization:problem:work_session_not_found",
  },
  {
    label: "type",
    code: "WORK_SESSION_NOT_FOUND",
    httpStatus: 404,
    bodyStatus: 404,
    type: "urn:organization:problem:other",
  },
])(
  "@s32 ignores a simple problem with contradictory $label",
  async ({ code, httpStatus, bodyStatus, type }) => {
    const response = Response.json(
      {
        type,
        title: "Problema",
        status: bodyStatus,
        code,
      },
      { status: httpStatus },
    );
    await expect(readWorkSessionError(response)).resolves.toBeNull();
    expect(response.bodyUsed).toBe(false);
  },
);

it.each([
  { label: "HTTP status", override: {}, httpStatus: 404 },
  { label: "body status", override: { status: 404 }, httpStatus: 409 },
  {
    label: "type",
    override: { type: "urn:organization:problem:other" },
    httpStatus: 409,
  },
  { label: "code", override: { code: "UNKNOWN" }, httpStatus: 409 },
])(
  "@s35 ignores an active conflict with contradictory $label",
  async ({ override, httpStatus }) => {
    const response = Response.json(
      {
        type: "urn:organization:problem:work_session_already_active",
        title: "Activa",
        status: 409,
        code: "WORK_SESSION_ALREADY_ACTIVE",
        sessionId: receipt.id,
        ...override,
      },
      { status: httpStatus },
    );
    await expect(readWorkSessionError(response)).resolves.toBeNull();
    expect(response.bodyUsed).toBe(false);
  },
);

it("@s30 rejects a coherent POST receipt for a different intended duration", async () => {
  respond({
    ...receipt,
    plannedMinutes: 26,
    plannedEndAt: "2026-09-06T10:26:00.123456Z",
  });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow();
});

it("@s35 does not recognize an active conflict with an extra private field", async () => {
  const response = Response.json(
    {
      type: "urn:organization:problem:work_session_already_active",
      title: "Activa",
      status: 409,
      code: "WORK_SESSION_ALREADY_ACTIVE",
      sessionId: receipt.id,
      ownerId: "other",
    },
    { status: 409 },
  );
  await expect(readWorkSessionError(response)).resolves.toBeNull();
  expect(response.bodyUsed).toBe(false);
});
it("@s31 preserves exact historical microseconds across the Unix epoch", async () => {
  const historical = {
    ...receipt,
    startedAt: "1969-12-31T23:50:00.000001Z",
    plannedEndAt: "1970-01-01T00:15:00.000001Z",
  };
  respond(historical);
  await expect(
    startWorkSession(
      receipt.projectId.toUpperCase(),
      receipt.taskId.toUpperCase(),
      25,
      key,
    ),
  ).resolves.toEqual(historical);
});
it("@s30 rejects a normalized but nonexistent calendar day", async () => {
  respond({
    ...receipt,
    startedAt: "2026-02-30T10:00:00Z",
    plannedEndAt: "2026-02-30T10:25:00Z",
  });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a duration beyond 1440 with a coherent end", async () => {
  respond({
    ...receipt,
    plannedMinutes: 1441,
    plannedEndAt: "2026-09-07T10:01:00.123456Z",
  });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 1441, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s33 recognizes a closed missing receipt problem without consuming its response", async () => {
  const problem = {
    type: "urn:organization:problem:work_session_not_found",
    title: "No se ha encontrado la sesión de trabajo.",
    status: 404,
    code: "WORK_SESSION_NOT_FOUND",
  };
  const response = Response.json(problem, { status: 404 });
  await expect(readWorkSessionError(response)).resolves.toEqual(problem);
  expect(response.bodyUsed).toBe(false);
});
it("@s24 preserves the original 503 response from ID lookup", async () => {
  const response = Response.json(
    { code: "STORAGE_UNAVAILABLE" },
    { status: 503 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readWorkSession(receipt.id)).rejects.toBe(response);
});
it("@s22 preserves the original 404 response from key recovery", async () => {
  const response = Response.json(
    { code: "WORK_SESSION_NOT_FOUND" },
    { status: 404 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    recoverWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toBe(response);
});
it("@s24 preserves the original 503 response from key recovery", async () => {
  const response = Response.json(
    { code: "STORAGE_UNAVAILABLE" },
    { status: 503 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    recoverWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toBe(response);
});
it("@s30 rejects a coherent recovered receipt for a different duration", async () => {
  respond(
    {
      ...receipt,
      plannedMinutes: 30,
      plannedEndAt: "2026-09-06T10:30:00.123456Z",
    },
    200,
  );
  await expect(
    recoverWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a recovered receipt from a different task", async () => {
  respond({ ...receipt, taskId: key }, 200);
  await expect(
    recoverWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a recovered receipt from a different project", async () => {
  respond({ ...receipt, projectId: key }, 200);
  await expect(
    recoverWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s21 recovers the receipt by key with its known intention and no Location", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(
    recoverWorkSession(receipt.projectId, receipt.taskId, 25, key, signal),
  ).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/work-sessions/by-request/${key}`,
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
});
it("@s22 preserves the original 404 response from ID lookup", async () => {
  const response = Response.json(
    { code: "WORK_SESSION_NOT_FOUND" },
    { status: 404 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readWorkSession(receipt.id)).rejects.toBe(response);
});
it("@s21 rejects a different identity returned by the ID lookup", async () => {
  respond({ ...receipt, id: key }, 200);
  await expect(readWorkSession(receipt.id)).rejects.toThrow(
    "Inicio de trabajo inválido",
  );
});
it("@s21 reads an original receipt by ID without requiring Location", async () => {
  const fetcher = vi.fn().mockResolvedValue(Response.json(receipt));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(readWorkSession(receipt.id, signal)).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/work-sessions/${receipt.id}`,
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
});
it("@s42 rejects an incompatible active receipt using the same exact temporal contract", async () => {
  respond(
    { session: { ...receipt, plannedEndAt: "2026-09-06T10:25:00.123457Z" } },
    200,
  );
  await expect(readActiveWorkSession()).rejects.toThrow(
    "Sesión activa inválida",
  );
});
function respond(value: unknown, status = 201) {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(value, {
        status,
        headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
      }),
    ),
  );
}
it("@s30 rejects a receipt with an extra field", async () => {
  respond({ ...receipt, elapsedSeconds: 0 });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a duration returned as text", async () => {
  respond({ ...receipt, plannedMinutes: "25" });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a planned end shifted by exactly one microsecond", async () => {
  respond({ ...receipt, plannedEndAt: "2026-09-06T10:25:00.123457Z" });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a receipt from another project", async () => {
  respond({ ...receipt, projectId: "52345678-1234-1234-1234-123456789abc" });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a receipt from another task", async () => {
  respond({ ...receipt, taskId: "52345678-1234-1234-1234-123456789abc" });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a POST Location for a different session", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(receipt, {
        status: 201,
        headers: { Location: `/api/v1/work-sessions/${key}` },
      }),
    ),
  );
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects an invalid session identity even with a matching Location", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json(
        { ...receipt, id: "not-a-uuid" },
        {
          status: 201,
          headers: { Location: "/api/v1/work-sessions/not-a-uuid" },
        },
      ),
    ),
  );
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a blank historical zone", async () => {
  respond({ ...receipt, zoneId: " " });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s30 rejects a zero duration even when it matches the sent value", async () => {
  respond({ ...receipt, plannedMinutes: 0, plannedEndAt: receipt.startedAt });
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 0, key),
  ).rejects.toThrow("Inicio de trabajo inválido");
});
it("@s23 reads the owner's active session without restricting it to an open task", async () => {
  const other = { ...receipt, projectId: key, taskId: receipt.id };
  const fetcher = vi.fn().mockResolvedValue(Response.json({ session: other }));
  vi.stubGlobal("fetch", fetcher);
  const signal = new AbortController().signal;
  await expect(readActiveWorkSession(signal)).resolves.toEqual(other);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    "/api/v1/work-sessions/active",
    {
      credentials: "same-origin",
      cache: "no-store",
      signal,
    },
  );
});
it("@s23 accepts only confirmed absence as null", async () => {
  respond({ session: null }, 200);
  await expect(readActiveWorkSession()).resolves.toBeNull();
});
it("@s24 preserves an active lookup failure instead of returning absence", async () => {
  const response = Response.json({ session: null }, { status: 503 });
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(readActiveWorkSession()).rejects.toBe(response);
});
it("@s42 rejects an active envelope with additional fields", async () => {
  respond({ session: null, ownerId: key }, 200);
  await expect(readActiveWorkSession()).rejects.toThrow(
    "Sesión activa inválida",
  );
});
afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
});

it("@s32 preserves an HTTP failure for uncertainty handling", async () => {
  const response = Response.json(
    { code: "STORAGE_UNAVAILABLE" },
    { status: 503 },
  );
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).rejects.toBe(response);
});

it("@s14 accepts the same historical receipt from a replay", async () => {
  respond(receipt, 200);
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key),
  ).resolves.toEqual(receipt);
});

it("@s1 sends an explicit start with its key and current CSRF token", async () => {
  const fetcher = vi.fn().mockResolvedValue(
    Response.json(receipt, {
      status: 201,
      headers: { Location: `/api/v1/work-sessions/${receipt.id}` },
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  setCsrfToken("current-token");
  const signal = new AbortController().signal;
  await expect(
    startWorkSession(receipt.projectId, receipt.taskId, 25, key, signal),
  ).resolves.toEqual(receipt);
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    `/api/v1/projects/${receipt.projectId}/tasks/${receipt.taskId}/work-sessions`,
    {
      method: "POST",
      credentials: "same-origin",
      cache: "no-store",
      signal,
      headers: new Headers({
        "Content-Type": "application/json",
        "Idempotency-Key": key,
        "X-CSRF-TOKEN": "current-token",
      }),
      body: JSON.stringify({ plannedMinutes: 25 }),
    },
  );
});
