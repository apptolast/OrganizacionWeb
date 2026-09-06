import { afterEach, expect, it, vi } from "vitest";
import { setCsrfToken } from "./api-client";
import { readActiveWorkSession, startWorkSession } from "./work-session-api";

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
