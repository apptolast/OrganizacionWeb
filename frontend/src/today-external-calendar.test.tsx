import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import { TodayExternalCalendar } from "./today-external-calendar";

const DAY_START = "2030-01-06T23:00:00Z";
const DAY_END = "2030-01-07T23:00:00Z";
const ZONE = "Europe/Madrid";
const meeting = {
  uid: "u1",
  summary: "Reunión",
  startAt: "2030-01-07T08:00:00Z",
  endAt: "2030-01-07T09:00:00Z",
  allDay: false,
};
const allDay = {
  uid: "u2",
  summary: "Festivo",
  startAt: "2030-01-06T23:00:00Z",
  endAt: "2030-01-07T23:00:00Z",
  allDay: true,
};
const subscription = {
  id: "11111111-2222-3333-4444-555555555555",
  label: "Trabajo",
  urlHost: "calendar.google.com",
  urlTail: ".ics",
  lastAttemptAt: "2030-01-07T11:00:00Z",
  lastSyncAt: "2030-01-07T11:00:00Z",
  lastStatus: "OK",
  lastError: null,
  snapshotZoneId: ZONE,
  imported: 1,
  skippedRecurring: 0,
  skippedCancelled: 0,
  skippedInvalid: 0,
  truncated: false,
  updatedAt: "2030-01-07T11:00:00Z",
};

let calls: string[];
let syncAnswer: () => Promise<Response>;
let eventsAnswer: () => Promise<Response>;

beforeEach(() => {
  calls = [];
  syncAnswer = async () => Response.json({ performed: true, subscription });
  eventsAnswer = async () =>
    Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [meeting],
    });
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, options: RequestInit = {}) => {
      calls.push(`${options.method ?? "GET"} ${url}`);
      return String(url).includes("/sync")
        ? await syncAnswer()
        : await eventsAnswer();
    }),
  );
});
afterEach(() => vi.unstubAllGlobals());

function paint(revision = "r1") {
  return render(
    <TodayExternalCalendar
      zoneId={ZONE}
      dayStartAt={DAY_START}
      dayEndAt={DAY_END}
      revision={revision}
    />,
  );
}

it("@s35 sincroniza con onlyIfStale y luego pide el día completo, en ese orden", async () => {
  paint();
  await screen.findByText("Reunión");
  expect(calls).toEqual([
    "POST /api/v1/me/external-calendar/sync",
    `GET /api/v1/me/external-calendar/events?from=${encodeURIComponent(DAY_START)}&to=${encodeURIComponent(DAY_END)}`,
  ]);
});

it("@s35 muestra el evento en hora local y la marca de la última sincronización", async () => {
  paint();
  const section = await screen.findByRole("region", {
    name: "Calendario externo",
  });
  expect(within(section).getByText("Reunión")).toBeInTheDocument();
  expect(within(section).getByText("09:00–10:00")).toBeInTheDocument();
  expect(
    within(section).getByText(/Según sincronización de 12:00/),
  ).toBeInTheDocument();
  expect(within(section).queryAllByRole("button")).toHaveLength(0);
});

it("@s35 un evento de todo el día se muestra sin horas", async () => {
  eventsAnswer = async () =>
    Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [allDay],
    });
  paint();
  expect(await screen.findByText("Todo el día")).toBeInTheDocument();
  expect(screen.queryByText(/00:00–/)).not.toBeInTheDocument();
});

it("@s35 avisa de la sincronización fallida y sigue mostrando los datos anteriores", async () => {
  syncAnswer = async () =>
    Response.json({
      performed: true,
      subscription: {
        ...subscription,
        lastStatus: "FAILED",
        lastError: "FEED_HTTP_ERROR",
        lastSyncAt: "2030-01-07T09:00:00Z",
      },
    });
  eventsAnswer = async () =>
    Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T09:00:00Z",
      lastStatus: "FAILED",
      items: [meeting],
    });
  paint();
  expect(
    await screen.findByText(
      /Sincronización fallida, se muestran datos de 10:00/,
    ),
  ).toBeInTheDocument();
  expect(screen.getByText("Reunión")).toBeInTheDocument();
});

it("@s35 con lista vacía muestra la ausencia y la marca de sincronización", async () => {
  eventsAnswer = async () =>
    Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [],
    });
  paint();
  expect(
    await screen.findByText(/no hay eventos del calendario externo/i),
  ).toBeInTheDocument();
  expect(screen.getByText(/Según sincronización de/)).toBeInTheDocument();
});

it("@s36 sin suscripción la sección no se muestra ni avisa de nada", async () => {
  syncAnswer = async () =>
    Response.json(
      { status: 404, code: "EXTERNAL_CALENDAR_NOT_CONFIGURED" },
      { status: 404 },
    );
  eventsAnswer = async () =>
    Response.json({
      configured: false,
      lastSyncAt: null,
      lastStatus: null,
      items: [],
    });
  const { container } = paint();
  await waitFor(() => expect(calls).toHaveLength(2));
  await waitFor(() => expect(container.textContent).toBe(""));
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s36 un 503 en la sincronización deja ver los eventos con el aviso de pendiente", async () => {
  syncAnswer = async () =>
    Response.json(
      { status: 503, code: "CONNECTORS_DISABLED" },
      { status: 503 },
    );
  paint();
  expect(await screen.findByText("Reunión")).toBeInTheDocument();
  expect(screen.getByText(/sincronización pendiente/i)).toBeInTheDocument();
});

it("@s36 un fallo de red al leer eventos avisa con enlace y sin error global", async () => {
  eventsAnswer = async () => {
    throw new TypeError("Failed to fetch");
  };
  paint();
  const link = await screen.findByRole("link", { name: /calendario externo/i });
  expect(link).toHaveAttribute("href", "/calendario-externo");
  expect(screen.queryByText("Reunión")).not.toBeInTheDocument();
});

it("@s36 una lectura inválida avisa y no pinta ningún evento", async () => {
  eventsAnswer = async () =>
    Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [{ ...meeting, allDay: undefined }],
    });
  paint();
  expect(
    await screen.findByText(/no se ha podido leer el calendario externo/i),
  ).toBeInTheDocument();
  expect(screen.queryByText("Reunión")).not.toBeInTheDocument();
  expect(
    screen.getByRole("link", { name: /calendario externo/i }),
  ).toBeInTheDocument();
});

it("@s36 una actualización nueva cancela la anterior y su respuesta tardía no repinta", async () => {
  let release: (() => void) | undefined;
  const held = new Promise<void>((resolve) => (release = resolve));
  eventsAnswer = async () => {
    await held;
    return Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [{ ...meeting, summary: "Vieja" }],
    });
  };
  const view = paint("r1");
  await waitFor(() => expect(calls.length).toBeGreaterThanOrEqual(2));
  eventsAnswer = async () =>
    Response.json({
      configured: true,
      lastSyncAt: "2030-01-07T11:00:00Z",
      lastStatus: "OK",
      items: [{ ...meeting, summary: "Nueva" }],
    });
  view.rerender(
    <TodayExternalCalendar
      zoneId={ZONE}
      dayStartAt={DAY_START}
      dayEndAt={DAY_END}
      revision="r2"
    />,
  );
  expect(await screen.findByText("Nueva")).toBeInTheDocument();
  release?.();
  await new Promise((resolve) => setTimeout(resolve, 10));
  expect(screen.queryByText("Vieja")).not.toBeInTheDocument();
  expect(screen.getByText("Nueva")).toBeInTheDocument();
});

it("@s36 salir de Hoy cancela las llamadas sin actualizar una vista desmontada", async () => {
  let release: (() => void) | undefined;
  const held = new Promise<void>((resolve) => (release = resolve));
  const signals: AbortSignal[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string, options: RequestInit = {}) => {
      calls.push(`${options.method ?? "GET"} ${url}`);
      if (options.signal) signals.push(options.signal);
      if (String(url).includes("/sync"))
        return Response.json({ performed: true, subscription });
      await held;
      return Response.json({
        configured: true,
        lastSyncAt: null,
        lastStatus: "OK",
        items: [meeting],
      });
    }),
  );
  const view = paint();
  await waitFor(() => expect(signals.length).toBeGreaterThanOrEqual(2));
  expect(signals.every((signal) => !signal.aborted)).toBe(true);
  view.unmount();
  expect(signals.every((signal) => signal.aborted)).toBe(true);
  release?.();
  await new Promise((resolve) => setTimeout(resolve, 10));
  expect(document.body.textContent).toBe("");
});
