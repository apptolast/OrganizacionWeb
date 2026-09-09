import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ConnectorsDisabledError,
  ExternalCalendarNotConfiguredError,
  ExternalCalendarValidationError,
  deleteExternalCalendar,
  readExternalCalendar,
  readExternalEvents,
  saveExternalCalendar,
  syncExternalCalendar,
} from "./external-calendar-api";
import { observeAccess, setCsrfToken } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  setCsrfToken();
});

const subscription = {
  id: "11111111-2222-3333-4444-555555555555",
  label: "Trabajo",
  urlHost: "calendar.google.com",
  urlTail: ".ics",
  lastAttemptAt: "2030-01-07T12:00:00Z",
  lastSyncAt: "2030-01-07T11:00:00Z",
  lastStatus: "OK",
  lastError: null,
  snapshotZoneId: "Europe/Madrid",
  imported: 12,
  skippedRecurring: 3,
  skippedCancelled: 1,
  skippedInvalid: 0,
  truncated: false,
  updatedAt: "2030-01-07T11:00:00Z",
};
const item = {
  uid: "u1",
  summary: "Reunión",
  startAt: "2030-01-07T08:00:00Z",
  endAt: "2030-01-07T09:00:00Z",
  allDay: false,
};
const disabled = {
  type: "urn:organization:problem:connectors_disabled",
  title: "No disponible",
  status: 503,
  code: "CONNECTORS_DISABLED",
};
function stub(body: unknown, init?: ResponseInit) {
  const fetcher = vi.fn().mockResolvedValue(Response.json(body, init));
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}

describe("@s37 lectura de la suscripción", () => {
  it("acepta la ausencia de suscripción", async () => {
    stub({ configured: false, subscription: null });
    await expect(readExternalCalendar()).resolves.toEqual({
      configured: false,
      subscription: null,
    });
  });

  it("acepta una suscripción completa", async () => {
    stub({ configured: true, subscription });
    await expect(readExternalCalendar()).resolves.toEqual({
      configured: true,
      subscription,
    });
  });

  it("no envía nada más que un GET sin cuerpo", async () => {
    const fetcher = stub({ configured: false, subscription: null });
    await readExternalCalendar();
    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/external-calendar");
    expect(fetcher.mock.calls[0][1]).not.toHaveProperty("body");
  });

  it.each([
    ["falta un campo", { ...subscription, truncated: undefined }],
    ["sobra un campo", { ...subscription, extra: 1 }],
    ["estado desconocido", { ...subscription, lastStatus: "RARO" }],
    [
      "código desconocido",
      { ...subscription, lastStatus: "FAILED", lastError: "RARO" },
    ],
    ["contador negativo", { ...subscription, imported: -1 }],
    ["contador no entero", { ...subscription, imported: 1.5 }],
    ["host vacío", { ...subscription, urlHost: "" }],
    [
      "cola con longitud distinta de cuatro",
      { ...subscription, urlTail: ".icsx" },
    ],
    ["instante inválido", { ...subscription, lastSyncAt: "ayer" }],
    ["identificador que no es uuid", { ...subscription, id: "1" }],
  ])("rechaza una suscripción con %s", async (_name, broken) => {
    stub({ configured: true, subscription: broken });
    await expect(readExternalCalendar()).rejects.toThrow(
      "Respuesta de calendario externo inválida",
    );
  });

  it("rechaza configured true sin suscripción", async () => {
    stub({ configured: true, subscription: null });
    await expect(readExternalCalendar()).rejects.toThrow(
      "Respuesta de calendario externo inválida",
    );
  });

  it("rechaza configured false con suscripción", async () => {
    stub({ configured: false, subscription });
    await expect(readExternalCalendar()).rejects.toThrow(
      "Respuesta de calendario externo inválida",
    );
  });

  it("@s8 traduce el 503 de conectores", async () => {
    stub(disabled, { status: 503 });
    await expect(readExternalCalendar()).rejects.toBeInstanceOf(
      ConnectorsDisabledError,
    );
  });

  it("deja pasar cualquier otro estado como respuesta", async () => {
    const response = Response.json({}, { status: 500 });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
    await expect(readExternalCalendar()).rejects.toBe(response);
  });
});

describe("@s38 guardado", () => {
  it("envía exactamente la etiqueta y la dirección", async () => {
    const fetcher = stub({ configured: true, subscription });
    setCsrfToken("token");
    await expect(
      saveExternalCalendar("Trabajo", "https://calendar.google.com/a.ics"),
    ).resolves.toEqual(subscription);
    const [url, options] = fetcher.mock.calls[0];
    expect(url).toBe("/api/v1/me/external-calendar");
    expect(options.method).toBe("PUT");
    expect(JSON.parse(options.body)).toEqual({
      label: "Trabajo",
      url: "https://calendar.google.com/a.ics",
    });
  });

  it.each([
    ["url", "BLOCKED_ADDRESS"],
    ["url", "UNRESOLVABLE_HOST"],
    ["url", "INVALID_VALUE"],
    ["label", "TOO_LONG"],
  ])("traduce el error de %s con código %s", async (field, code) => {
    stub(
      {
        status: 400,
        code: "VALIDATION_ERROR",
        errors: [{ field, code, message: "Revisa este campo." }],
      },
      { status: 400 },
    );
    const error = await saveExternalCalendar(
      "Trabajo",
      "https://x.test/a.ics",
    ).catch((failure: unknown) => failure);
    expect(error).toBeInstanceOf(ExternalCalendarValidationError);
    expect(error).toMatchObject({
      fields: { [field]: "Revisa este campo." },
      codes: { [field]: code },
    });
  });

  it("no inventa un error de campo si el problema no trae errores reconocibles", async () => {
    const response = Response.json(
      {
        status: 400,
        code: "VALIDATION_ERROR",
        errors: [{ field: "alien", code: "X", message: "m" }],
      },
      { status: 400 },
    );
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
    await expect(
      saveExternalCalendar("Trabajo", "https://x.test/a.ics"),
    ).rejects.toBe(response);
  });

  it("@s38 traduce el 503 de conectores para conservar el borrador", async () => {
    stub(disabled, { status: 503 });
    await expect(
      saveExternalCalendar("Trabajo", "https://x.test/a.ics"),
    ).rejects.toBeInstanceOf(ConnectorsDisabledError);
  });
});

describe("@s39 borrado", () => {
  it("acepta 204 sin cuerpo", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetcher);
    await expect(deleteExternalCalendar()).resolves.toBeUndefined();
    expect(fetcher.mock.calls[0][1].method).toBe("DELETE");
  });

  it("rechaza cualquier otro estado", async () => {
    const response = new Response(null, { status: 200 });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(response));
    await expect(deleteExternalCalendar()).rejects.toBe(response);
  });
});

describe("@s38 sincronización", () => {
  it("envía la frescura pedida y devuelve el resultado", async () => {
    const fetcher = stub({ performed: true, subscription });
    await expect(syncExternalCalendar(true)).resolves.toEqual({
      performed: true,
      subscription,
    });
    const [url, options] = fetcher.mock.calls[0];
    expect(url).toBe("/api/v1/me/external-calendar/sync");
    expect(options.method).toBe("POST");
    expect(JSON.parse(options.body)).toEqual({ onlyIfStale: true });
  });

  it("@s38 traduce el 404 de suscripción ausente", async () => {
    stub(
      { status: 404, code: "EXTERNAL_CALENDAR_NOT_CONFIGURED" },
      { status: 404 },
    );
    await expect(syncExternalCalendar(false)).rejects.toBeInstanceOf(
      ExternalCalendarNotConfiguredError,
    );
  });

  it("rechaza un performed que no es booleano", async () => {
    stub({ performed: "sí", subscription });
    await expect(syncExternalCalendar(false)).rejects.toThrow(
      "Respuesta de calendario externo inválida",
    );
  });
});

describe("@s35 lectura de eventos", () => {
  it("pide el rango tal cual y devuelve los items", async () => {
    const fetcher = stub({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [item],
    });
    await expect(
      readExternalEvents("2030-01-06T23:00:00Z", "2030-01-07T23:00:00Z"),
    ).resolves.toEqual({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [item],
    });
    expect(fetcher.mock.calls[0][0]).toBe(
      "/api/v1/me/external-calendar/events?from=2030-01-06T23%3A00%3A00Z&to=2030-01-07T23%3A00%3A00Z",
    );
  });

  it("@s36 acepta la ausencia de suscripción", async () => {
    stub({ configured: false, lastSyncAt: null, lastStatus: null, items: [] });
    await expect(
      readExternalEvents("2030-01-06T23:00:00Z", "2030-01-07T23:00:00Z"),
    ).resolves.toMatchObject({ configured: false, items: [] });
  });

  it.each([
    ["sin allDay", { ...item, allDay: undefined }],
    ["con un campo de más", { ...item, extra: 1 }],
    ["con fin anterior al inicio", { ...item, endAt: "2030-01-07T07:00:00Z" }],
    ["con fin igual al inicio", { ...item, endAt: item.startAt }],
    ["con uid vacío", { ...item, uid: "" }],
    ["con summary que no es texto", { ...item, summary: 3 }],
    ["con instante inválido", { ...item, startAt: "2030-01-07" }],
  ])("@s36 rechaza una lista con un item %s", async (_name, broken) => {
    stub({
      configured: true,
      lastSyncAt: null,
      lastStatus: "OK",
      items: [broken],
    });
    await expect(
      readExternalEvents("2030-01-06T23:00:00Z", "2030-01-07T23:00:00Z"),
    ).rejects.toThrow("Respuesta de calendario externo inválida");
  });

  it("@s36 rechaza una respuesta con campos de más", async () => {
    stub({
      configured: true,
      lastSyncAt: null,
      lastStatus: "OK",
      items: [],
      extra: 1,
    });
    await expect(
      readExternalEvents("2030-01-06T23:00:00Z", "2030-01-07T23:00:00Z"),
    ).rejects.toThrow("Respuesta de calendario externo inválida");
  });

  it("@s36 el summary vacío es válido", async () => {
    stub({
      configured: true,
      lastSyncAt: null,
      lastStatus: "OK",
      items: [{ ...item, summary: "" }],
    });
    await expect(
      readExternalEvents("2030-01-06T23:00:00Z", "2030-01-07T23:00:00Z"),
    ).resolves.toMatchObject({ items: [{ ...item, summary: "" }] });
  });
});

describe("@s36 cancelación", () => {
  it("propaga la señal abortada antes de pedir nada", async () => {
    const fetcher = vi.fn();
    vi.stubGlobal("fetch", fetcher);
    const controller = new AbortController();
    controller.abort();
    await expect(readExternalCalendar(controller.signal)).rejects.toThrow();
    expect(fetcher).not.toHaveBeenCalled();
  });

  it("no valida la respuesta de una lectura ya cancelada", async () => {
    const controller = new AbortController();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockImplementation(async () => {
        controller.abort();
        return Response.json({ configured: true, subscription: null });
      }),
    );
    await expect(readExternalCalendar(controller.signal)).rejects.toThrow();
  });
});
