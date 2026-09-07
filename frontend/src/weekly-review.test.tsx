import {
  render,
  screen,
  within,
  fireEvent,
  waitFor,
  act,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { App } from "./App";
import { observeAccess } from "./api-client";

afterEach(() => {
  vi.unstubAllGlobals();
  observeAccess();
  window.history.replaceState(null, "", "/");
});
const week = {
  weekStart: "2026-09-07",
  weekEnd: "2026-09-13",
  zoneId: "UTC",
  zoneSource: "UNCONFIGURED",
  availabilityZoneId: null,
  serverNow: "2026-09-09T12:00:00.123456Z",
  startAt: "2026-09-07T00:00:00Z",
  endAt: "2026-09-14T00:00:00Z",
  days: Array.from({ length: 7 }, (_, index) => {
    const date = `2026-09-${String(7 + index).padStart(2, "0")}`;
    return {
      date,
      startAt: `${date}T00:00:00Z`,
      endAt: `2026-09-${String(8 + index).padStart(2, "0")}T00:00:00Z`,
      plannedMicroseconds: "0",
      workedMicroseconds: "0",
      capacityMicroseconds: null,
    };
  }),
  totals: {
    plannedMicroseconds: "0",
    workedMicroseconds: "0",
    capacityMicroseconds: null,
  },
  unquantifiedSessionCount: "0",
};

it("@s27 opens a private seven-day review from navigation without configuring availability", async () => {
  window.history.replaceState(null, "", "/no-existe");
  const fetcher = vi.fn().mockResolvedValue(Response.json(week));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await userEvent.click(
    within(screen.getByRole("navigation", { name: "Principal" })).getByRole(
      "link",
      { name: "Revisión semanal" },
    ),
  );
  expect(
    await screen.findByRole("heading", { level: 1, name: "Revisión semanal" }),
  ).toHaveFocus();
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(
    within(
      screen.getByRole("list", { name: "Días de la semana" }),
    ).getAllByRole("listitem"),
  ).toHaveLength(7);
  expect(screen.getByText("Plan vigente", { selector: "dt" })).toBeVisible();
  expect(
    screen.getByText("Trabajo registrado", { selector: "dt" }),
  ).toBeVisible();
  expect(
    screen.getByText("Presupuesto actual", { selector: "dt" }),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Ver planificación" }),
  ).toHaveAttribute("href", "/proyectos");
  expect(window.location.pathname).toBe("/revision-semanal");
  expect(
    screen.getByRole("link", { name: "Revisión semanal" }),
  ).toHaveAttribute("aria-current", "page");
  expect(fetcher).toHaveBeenCalledExactlyOnceWith(
    "/api/v1/weekly-review",
    expect.objectContaining({ cache: "no-store" }),
  );
});
it("@s29 announces an initial read without showing provisional zero totals", () => {
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal("fetch", vi.fn().mockReturnValue(new Promise(() => {})));
  render(<App />);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando revisión semanal",
  );
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
});

it("@s30 reports a failed read without claiming an empty week or endless loading", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(new Response(null, { status: 503 })),
  );
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo consultar la revisión semanal",
  );
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByText("Consultando revisión semanal…"),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Reintentar" })).toBeVisible();
});

it("@s30 retries the same selection once while announcing its pending read", async () => {
  window.history.replaceState(null, "", "/revision-semanal?date=2026-09-09");
  let release!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(new Response(null, { status: 503 }))
    .mockReturnValue(
      new Promise<Response>((resolve) => {
        release = resolve;
      }),
    );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  const retry = await screen.findByRole("button", { name: "Reintentar" });
  await userEvent.dblClick(retry);
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando revisión semanal",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(fetcher.mock.calls[1][0]).toBe(
    "/api/v1/weekly-review?date=2026-09-09",
  );
  await act(async () => release(Response.json(week)));
  expect(
    screen.getByRole("heading", { level: 1, name: "Revisión semanal" }),
  ).toHaveFocus();
});

it("@s28 applies the draft date explicitly and withdraws the previous week's data", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(week))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-16" },
  });
  expect(fetcher).toHaveBeenCalledTimes(1);
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  expect(window.location.search).toBe("?date=2026-09-16");
  expect(fetcher.mock.calls[1][0]).toBe(
    "/api/v1/weekly-review?date=2026-09-16",
  );
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando revisión semanal",
  );
});

it("@s28 selects a catalog zone without changing availability or reading a new week until apply", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  const fetcher = vi.fn((url: string) =>
    Promise.resolve(
      Response.json(
        url.endsWith("/zones")
          ? { items: ["Europe/Madrid", "UTC"] }
          : url.includes("zoneId=UTC")
            ? { ...week, zoneSource: "EXPLICIT" }
            : week,
      ),
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  const zone = screen.getByLabelText("Zona horaria");
  fireEvent.focus(zone);
  await screen.findByRole("option", { name: "UTC" });
  await userEvent.selectOptions(zone, "UTC");
  expect(
    fetcher.mock.calls.filter(([url]) =>
      url.startsWith("/api/v1/weekly-review"),
    ),
  ).toHaveLength(1);
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await waitFor(() => expect(window.location.search).toBe("?zoneId=UTC"));
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(
    fetcher.mock.calls.filter(([url]) =>
      url.startsWith("/api/v1/weekly-review"),
    ),
  ).toHaveLength(2);
  expect(
    fetcher.mock.calls.every(
      ([url]) =>
        url.startsWith("/api/v1/weekly-review") ||
        url.endsWith("/availability/zones"),
    ),
  ).toBe(true);
});

it("@s28 navigates adjacent civil weeks and keeps the explicit zone for This week", async () => {
  window.history.replaceState(
    null,
    "",
    "/revision-semanal?date=2026-09-09&zoneId=UTC",
  );
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json({ ...week, zoneSource: "EXPLICIT" }))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(screen.getByLabelText("Zona horaria")).toHaveValue("UTC");
  expect(
    screen.getByRole("link", { name: "Semana siguiente" }),
  ).toHaveAttribute("href", "/revision-semanal?date=2026-09-14&zoneId=UTC");
  expect(screen.getByRole("link", { name: "Esta semana" })).toHaveAttribute(
    "href",
    "/revision-semanal?zoneId=UTC",
  );
  await userEvent.click(screen.getByRole("link", { name: "Semana anterior" }));
  expect(window.location.search).toBe("?date=2026-08-31&zoneId=UTC");
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
});

it("@s29 keeps the previous snapshot explicitly dated while a manual refresh is pending", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(week))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  await userEvent.dblClick(screen.getByRole("button", { name: "Actualizar" }));
  expect(screen.getByRole("status")).toHaveTextContent(
    "Actualizando revisión semanal",
  );
  expect(screen.getByRole("list", { name: "Días de la semana" })).toBeVisible();
  expect(
    screen.getByText(/Datos anteriores. Datos consultados a/),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s32 withdraws the private snapshot and selection when the current read loses access", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  const observer = vi.fn();
  observeAccess(observer);
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(week))
      .mockResolvedValueOnce(new Response(null, { status: 401 })),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  await userEvent.click(screen.getByRole("button", { name: "Actualizar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Tu sesión ya no está disponible",
  );
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Fecha de la semana")).not.toBeInTheDocument();
  expect(observer).toHaveBeenCalledExactlyOnceWith(401);
});

it("@s31 aborts a replaced read before a late HTTP401 can revoke the current page", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  let deliver!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    )
    .mockResolvedValueOnce(Response.json(week));
  const observer = vi.fn();
  observeAccess(observer);
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-09" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(fetcher.mock.calls[0][1].signal.aborted).toBe(true);
  await act(async () => deliver(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
  expect(screen.getByRole("list", { name: "Días de la semana" })).toBeVisible();
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
});

it("@s28 restores the applied date through Back instead of keeping the later draft", async () => {
  window.history.replaceState(null, "", "/revision-semanal?date=2026-09-09");
  const fetcher = vi.fn<(url: string) => Promise<Response>>(() =>
    Promise.resolve(Response.json(week)),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-08" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await waitFor(() => expect(window.location.search).toBe("?date=2026-09-08"));
  await act(async () => window.history.back());
  await waitFor(() =>
    expect(screen.getByLabelText("Fecha de la semana")).toHaveValue(
      "2026-09-09",
    ),
  );
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(fetcher.mock.calls.at(-1)?.[0]).toBe(
    "/api/v1/weekly-review?date=2026-09-09",
  );
});

it("@s28 prevents navigating before the first representable complete week", async () => {
  const value = {
    ...week,
    weekStart: "0001-01-01",
    weekEnd: "0001-01-07",
    startAt: "0001-01-01T00:00:00Z",
    endAt: "0001-01-08T00:00:00Z",
    days: week.days.map((day, index) => ({
      ...day,
      date: `0001-01-0${index + 1}`,
      startAt: `0001-01-0${index + 1}T00:00:00Z`,
      endAt: `0001-01-0${index + 2}T00:00:00Z`,
    })),
  };
  window.history.replaceState(null, "", "/revision-semanal?date=0001-01-01");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(
    screen.getByRole("button", { name: "Semana anterior" }),
  ).toBeDisabled();
  expect(
    screen.queryByRole("link", { name: "Semana anterior" }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole("link", { name: "Semana siguiente" }),
  ).toHaveAttribute("href", "/revision-semanal?date=0001-01-08");
});

it("@s28 prevents navigating to a week whose Sunday exceeds year9999", async () => {
  const value = {
    ...week,
    weekStart: "9999-12-20",
    weekEnd: "9999-12-26",
    startAt: "9999-12-20T00:00:00Z",
    endAt: "9999-12-27T00:00:00Z",
    days: week.days.map((day, index) => ({
      ...day,
      date: `9999-12-${index + 20}`,
      startAt: `9999-12-${index + 20}T00:00:00Z`,
      endAt: `9999-12-${index + 21}T00:00:00Z`,
    })),
  };
  window.history.replaceState(null, "", "/revision-semanal?date=9999-12-20");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(
    screen.getByRole("button", { name: "Semana siguiente" }),
  ).toBeDisabled();
  expect(
    screen.queryByRole("link", { name: "Semana siguiente" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("link", { name: "Semana anterior" })).toHaveAttribute(
    "href",
    "/revision-semanal?date=9999-12-13",
  );
});

it("@s30 makes an invalid date response correctable at the field without blindly retrying", async () => {
  window.history.replaceState(null, "", "/revision-semanal?date=9999-12-31");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          type: "urn:organization:problem:validation_error",
          title: "Revisa los campos",
          status: 400,
          code: "VALIDATION_ERROR",
          errors: [
            {
              field: "date",
              code: "INVALID_VALUE",
              message: "Semana incompleta",
            },
          ],
        },
        { status: 400 },
      ),
    )
    .mockResolvedValueOnce(Response.json(week));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  const error = await screen.findByRole("alert");
  const date = screen.getByLabelText("Fecha de la semana");
  expect(date).toHaveAttribute("aria-invalid", "true");
  expect(date).toHaveAccessibleDescription(error.textContent!);
  expect(
    screen.queryByRole("button", { name: "Reintentar" }),
  ).not.toBeInTheDocument();
  fireEvent.change(date, { target: { value: "2026-09-09" } });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(screen.getByLabelText("Fecha de la semana")).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s30 associates a rejected zone with its selector and permits the availability choice", async () => {
  window.history.replaceState(
    null,
    "",
    "/revision-semanal?zoneId=Missing%2FZone",
  );
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      Promise.resolve(
        url.includes("zoneId=")
          ? Response.json(
              {
                code: "VALIDATION_ERROR",
                errors: [{ field: "zoneId", code: "INVALID_VALUE" }],
              },
              { status: 400 },
            )
          : Response.json(url.endsWith("/zones") ? { items: ["UTC"] } : week),
      ),
    ),
  );
  render(<App />);
  const error = await screen.findByRole("alert");
  const zone = screen.getByLabelText("Zona horaria");
  expect(zone).toHaveAttribute("aria-invalid", "true");
  expect(zone).toHaveAccessibleDescription(error.textContent!);
  await userEvent.selectOptions(zone, "");
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(window.location.search).toBe("");
});

it("@s30 explains a temporal conflict and lets the user retry the read manually", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        { code: "WEEKLY_REVIEW_TIME_OUT_OF_RANGE", status: 409 },
        { status: 409 },
      ),
    )
    .mockResolvedValueOnce(Response.json(week));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se puede calcular esta semana con la fecha o la hora actual",
  );
  await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(fetcher).toHaveBeenCalledTimes(2);
});

it("@s30 lets an invalid URL query be corrected by submitting the visible filters", async () => {
  window.history.replaceState(null, "", "/revision-semanal?owner=invalid");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json(
        {
          code: "VALIDATION_ERROR",
          errors: [{ field: "query", code: "INVALID_VALUE" }],
        },
        { status: 400 },
      ),
    )
    .mockResolvedValueOnce(Response.json(week));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Revisa la fecha y la zona y pulsa Mostrar semana",
  );
  expect(
    screen.queryByRole("button", { name: "Reintentar" }),
  ).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  expect(
    await screen.findByRole("list", { name: "Días de la semana" }),
  ).toBeVisible();
  expect(window.location.search).toBe("");
});

it("@s30 reports catalog failure separately and retries zones without reloading the week", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  let zoneReads = 0;
  const fetcher = vi.fn((url: string) =>
    Promise.resolve(
      url.endsWith("/zones")
        ? ++zoneReads === 1
          ? new Response(null, { status: 503 })
          : Response.json({ items: ["UTC"] })
        : Response.json(week),
    ),
  );
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.focus(screen.getByLabelText("Zona horaria"));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "No se pudo consultar el catálogo de zonas",
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Reintentar zonas" }),
  );
  expect(
    await screen.findByRole("option", { name: "UTC" }),
  ).toBeInTheDocument();
  expect(
    fetcher.mock.calls.filter(([url]) => url === "/api/v1/weekly-review"),
  ).toHaveLength(1);
  expect(screen.getByRole("list", { name: "Días de la semana" })).toBeVisible();
});

it("@s33 restores heading focus when applying filters removes the still-focused initiator", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal(
    "fetch",
    vi.fn(() => Promise.resolve(Response.json(week))),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-09" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await waitFor(() =>
    expect(
      screen.getByRole("heading", { level: 1, name: "Revisión semanal" }),
    ).toHaveFocus(),
  );
});

it("@s33 leaves focus on a deliberately chosen control when the new selection finishes", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  let deliver!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(Response.json(week))
      .mockReturnValueOnce(
        new Promise<Response>((resolve) => {
          deliver = resolve;
        }),
      ),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-09" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  const chosen = screen.getByLabelText("Fecha de la semana");
  chosen.focus();
  await act(async () => deliver(Response.json(week)));
  expect(chosen).toHaveFocus();
  expect(screen.getByRole("list", { name: "Días de la semana" })).toBeVisible();
});

it("@s33 restores focus after the current-week link is replaced by its new snapshot", async () => {
  window.history.replaceState(null, "", "/revision-semanal?date=2026-09-09");
  vi.stubGlobal(
    "fetch",
    vi.fn(() => Promise.resolve(Response.json(week))),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  await userEvent.click(screen.getByRole("link", { name: "Esta semana" }));
  await waitFor(() =>
    expect(
      screen.getByRole("heading", { name: "Revisión semanal", level: 1 }),
    ).toHaveFocus(),
  );
  expect(window.location.search).toBe("");
});

it("@s34 presents readable exact time and labels zero current budget without claiming real rest", async () => {
  const value = {
    ...week,
    zoneSource: "AVAILABILITY",
    availabilityZoneId: "UTC",
    days: week.days.map((day) => ({ ...day, capacityMicroseconds: "0" })),
    totals: {
      plannedMicroseconds: "5400000000",
      workedMicroseconds: "1000001",
      capacityMicroseconds: "0",
    },
  };
  value.days[0].plannedMicroseconds = value.totals.plannedMicroseconds;
  value.days[0].workedMicroseconds = value.totals.workedMicroseconds;
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(value)));
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(screen.getByText("1 h 30 min", { selector: "dd" })).toBeVisible();
  expect(screen.getByText("1,000001 s", { selector: "dd" })).toBeVisible();
  expect(screen.getByText(/no acredita descanso real/)).toBeVisible();
  expect(
    screen.getAllByText(
      "Sin tiempo presupuestado actualmente: descanso planificado actual.",
    ),
  ).toHaveLength(7);
  expect(screen.getByText(/no indica tareas terminadas/)).toBeVisible();
});

it("@s17 distinguishes unquantified old sessions from a complete zero-work report", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValue(
        Response.json({ ...week, unquantifiedSessionCount: "1" }),
      ),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(
    screen.getByText(
      /Hay sesiones antiguas iniciadas esta semana sin duración registrada \(1\)/,
    ),
  ).toBeVisible();
  expect(
    screen.getByText(/El trabajo mostrado es sólo el cuantificable/),
  ).toBeVisible();
  expect(screen.getByText(/Disponibilidad no configurada/)).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Configurar disponibilidad" }),
  ).toHaveAttribute("href", "/disponibilidad");
});

it("@s2 explains UTC fallback when the saved availability zone is unavailable", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue(
      Response.json({
        ...week,
        zoneSource: "UNAVAILABLE",
        availabilityZoneId: "Missing/Zone",
      }),
    ),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  expect(
    screen.getByText(
      /La zona guardada Missing\/Zone no está disponible. Mostramos UTC; presupuesto desconocido/,
    ),
  ).toBeVisible();
  expect(
    screen.getByRole("link", { name: "Configurar disponibilidad" }),
  ).toHaveAttribute("href", "/disponibilidad");
});

it("@s31 discards validation errors classified after a newer selection is already visible", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  let deliver!: (value: unknown) => void;
  const response = new Response(null, { status: 400 });
  const json = vi.spyOn(response, "json").mockReturnValue(
    new Promise((resolve) => {
      deliver = resolve;
    }),
  );
  vi.stubGlobal(
    "fetch",
    vi
      .fn()
      .mockResolvedValueOnce(response)
      .mockResolvedValueOnce(Response.json(week)),
  );
  render(<App />);
  await waitFor(() => expect(json).toHaveBeenCalledOnce());
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-09" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await screen.findByRole("list", { name: "Días de la semana" });
  await act(async () =>
    deliver({
      code: "VALIDATION_ERROR",
      errors: [{ field: "date", code: "INVALID_VALUE" }],
    }),
  );
  expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  expect(screen.getByRole("list", { name: "Días de la semana" })).toBeVisible();
});

it("@s34 announces the deferred catalog read when the native zone selector receives focus", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string) =>
      url.endsWith("/zones")
        ? new Promise(() => {})
        : Promise.resolve(Response.json(week)),
    ),
  );
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.focus(screen.getByLabelText("Zona horaria"));
  expect(screen.getByText("Consultando zonas…")).toHaveAttribute(
    "role",
    "status",
  );
  expect(screen.getByRole("list", { name: "Días de la semana" })).toBeVisible();
});

it("@s28 does not revive an earlier snapshot when Back starts another pending read", async () => {
  window.history.replaceState(null, "", "/revision-semanal?date=2026-09-09");
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(week))
    .mockReturnValue(new Promise(() => {}));
  vi.stubGlobal("fetch", fetcher);
  render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  fireEvent.change(screen.getByLabelText("Fecha de la semana"), {
    target: { value: "2026-09-16" },
  });
  await userEvent.click(screen.getByRole("button", { name: "Mostrar semana" }));
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(2));
  await act(async () => window.history.back());
  await waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole("status")).toHaveTextContent(
    "Consultando revisión semanal",
  );
});

it("@s32 aborts the pending private read when the session view is removed", async () => {
  window.history.replaceState(null, "", "/revision-semanal");
  let deliver!: (response: Response) => void;
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(Response.json(week))
    .mockReturnValueOnce(
      new Promise<Response>((resolve) => {
        deliver = resolve;
      }),
    );
  const observer = vi.fn();
  observeAccess(observer);
  vi.stubGlobal("fetch", fetcher);
  const view = render(<App />);
  await screen.findByRole("list", { name: "Días de la semana" });
  await userEvent.click(screen.getByRole("button", { name: "Actualizar" }));
  view.unmount();
  expect(fetcher.mock.calls[1][1].signal.aborted).toBe(true);
  await act(async () => deliver(new Response(null, { status: 401 })));
  expect(observer).not.toHaveBeenCalled();
  expect(
    screen.queryByRole("list", { name: "Días de la semana" }),
  ).not.toBeInTheDocument();
});
