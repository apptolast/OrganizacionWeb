import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { Automations } from "./automations";
import type { Automation, AutomationRun, EventType } from "./automations-api";

const RULE = "22222222-2222-4222-8222-222222222222";
const OTHER_RULE = "66666666-6666-4666-8666-666666666666";
const PROJECT = "11111111-1111-4111-8111-111111111111";

const rule: Automation = {
  id: RULE,
  name: "Seguimiento",
  enabled: true,
  trigger: { eventType: "TaskCreated.v1" },
  condition: null,
  action: {
    type: "CREATE_TASK",
    projectId: PROJECT,
    titleTemplate: "Revisar {{task.title}}",
    criterionTemplate: null,
    estimatedMinutes: 30,
  },
  version: 1,
  createdAt: "2026-09-08T10:15:30.123456Z",
  updatedAt: "2026-09-08T10:15:30.123456Z",
};

const disabled: Automation = {
  ...rule,
  id: OTHER_RULE,
  name: "Pausada",
  enabled: false,
  trigger: { eventType: "ProjectCreated.v1" },
};

const projects = {
  items: [
    {
      id: PROJECT,
      name: "Marketing",
      status: "active",
      createdAt: "2026-09-01T10:00:00Z",
      updatedAt: "2026-09-01T10:00:00Z",
    },
  ],
  nextCursor: null,
};

type Route = { status: number; body?: unknown; delay?: Promise<void> };
let routes: Map<string, Route[]>;
let calls: {
  url: string;
  method: string;
  headers: Headers;
  body?: string;
}[];

function key(url: string, method: string) {
  return `${method} ${url.split("?")[0]}`;
}

function route(
  method: string,
  url: string,
  status: number,
  body?: unknown,
  delay?: Promise<void>,
) {
  const list = routes.get(key(url, method)) ?? [];
  list.push({ status, body, delay });
  routes.set(key(url, method), list);
}

beforeEach(() => {
  routes = new Map();
  calls = [];
  route("GET", "/api/v1/projects", 200, projects);
  vi.stubGlobal("fetch", async (url: string, options: RequestInit = {}) => {
    const method = options.method ?? "GET";
    calls.push({
      url,
      method,
      headers: new Headers(options.headers),
      body: typeof options.body === "string" ? options.body : undefined,
    });
    const list = routes.get(key(url, method));
    const next = list && list.length > 1 ? list.shift()! : list?.[0];
    if (!next) return new Response(null, { status: 404 });
    if (next.delay) await next.delay;
    if (options.signal?.aborted)
      throw new DOMException("Aborted", "AbortError");
    return new Response(
      next.body === undefined ? null : JSON.stringify(next.body),
      { status: next.status, headers: { "Content-Type": "application/json" } },
    );
  });
});

afterEach(() => vi.unstubAllGlobals());

const listed = (...items: Automation[]) =>
  route("GET", "/api/v1/me/automations", 200, { items });

const sent = (method: string, url: string) =>
  calls
    .filter((call) => call.method === method && call.url === url)
    .map((call) => JSON.parse(call.body ?? "null") as unknown);

describe("automations page", () => {
  it("@s37 heads the page and announces the load before anything else", async () => {
    let release = () => {};
    route(
      "GET",
      "/api/v1/me/automations",
      200,
      { items: [] },
      new Promise<void>((resolve) => (release = resolve)),
    );
    render(<Automations owner="owner" />);
    expect(
      screen.getByRole("heading", { level: 1, name: "Automatizaciones" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent(/cargando/i);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    release();
    await screen.findByText(/todavía no tienes ninguna regla/i);
  });

  it("@s37 explains what a rule is when there is none and offers to make one", async () => {
    listed();
    render(<Automations owner="owner" />);
    await screen.findByText(/todavía no tienes ninguna regla/i);
    expect(
      screen.getByRole("button", { name: "Nueva regla" }),
    ).toBeInTheDocument();
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
  });

  it("@s37 shows name, readable trigger, destination and state as text", async () => {
    listed(rule, disabled);
    render(<Automations owner="owner" />);
    const rows = await screen.findAllByRole("listitem");
    expect(rows).toHaveLength(2);
    expect(within(rows[0]).getByText("Seguimiento")).toBeInTheDocument();
    expect(within(rows[0]).getByText("Tarea creada")).toBeInTheDocument();
    expect(within(rows[0]).getByText("Marketing")).toBeInTheDocument();
    expect(within(rows[0]).getByText("Activa")).toBeInTheDocument();
    expect(within(rows[1]).getByText("Proyecto creado")).toBeInTheDocument();
    expect(within(rows[1]).getByText("Inactiva")).toBeInTheDocument();
  });

  it("@s37 offers a retry and shows neither list nor empty state when the read fails", async () => {
    route("GET", "/api/v1/me/automations", 503, {
      code: "STORAGE_UNAVAILABLE",
    });
    render(<Automations owner="owner" />);
    await screen.findByRole("alert");
    expect(screen.queryByRole("list")).not.toBeInTheDocument();
    expect(
      screen.queryByText(/todavía no tienes ninguna regla/i),
    ).not.toBeInTheDocument();
    routes.delete("GET /api/v1/me/automations");
    listed(rule);
    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("Seguimiento")).toBeInTheDocument();
  });

  it("@s38 previews the template as text and never as markup", async () => {
    listed();
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    const title = screen.getByLabelText(/título de la tarea/i);
    await userEvent.clear(title);
    await userEvent.click(title);
    await userEvent.paste("Revisar <b>{{task.title}}</b>");
    const preview = screen.getByTestId("automation-preview");
    expect(preview).toHaveTextContent("Revisar <b>Redactar informe</b>");
    expect(preview.querySelector("b")).toBeNull();
  });

  it("@s38 shows the four placeholders inline as help", async () => {
    listed();
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    const help = screen.getByTestId("automation-placeholders");
    for (const placeholder of [
      "{{event.type}}",
      "{{task.title}}",
      "{{project.name}}",
      "{{occurredAt}}",
    ])
      expect(help).toHaveTextContent(placeholder);
  });

  it("@s38 pins a server field error to its control, focuses it and keeps the draft", async () => {
    listed();
    route("POST", "/api/v1/me/automations", 400, {
      code: "INVALID_TEMPLATE",
      errors: [
        {
          field: "action.criterionTemplate",
          code: "UNKNOWN_PLACEHOLDER",
          message: "x",
        },
      ],
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.type(screen.getByLabelText(/nombre/i), "Seguimiento");
    const title = screen.getByLabelText(/título de la tarea/i);
    await userEvent.clear(title);
    await userEvent.click(title);
    await userEvent.paste("Revisar {{task.title}}");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    const criterion = await screen.findByLabelText(/criterio/i);
    await waitFor(() =>
      expect(criterion).toHaveAttribute("aria-invalid", "true"),
    );
    expect(criterion).toHaveFocus();
    const described = criterion.getAttribute("aria-describedby");
    expect(described).toBeTruthy();
    expect(
      document.getElementById(described!.split(" ").at(-1)!),
    ).toHaveTextContent(/marcadores/i);
    expect(title).toHaveValue("Revisar {{task.title}}");
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it("@s39 simulates in place, announces the count and never saves", async () => {
    listed();
    route("POST", "/api/v1/me/automations/simulate", 200, {
      evaluatedEvents: 5,
      matches: [
        {
          eventId: "55555555-5555-4555-8555-555555555555",
          eventType: "TaskCreated.v1",
          occurredAt: "2026-09-08T10:15:30.123456Z",
          preview: {
            type: "CREATE_TASK",
            projectId: PROJECT,
            title: "Revisar Redactar informe",
            completionCriterion: "",
            estimatedMinutes: 30,
            wouldFail: "PROJECT_COMPLETED",
          },
          loopGuarded: false,
        },
        {
          eventId: "77777777-7777-4777-8777-777777777777",
          eventType: "TaskCreated.v1",
          occurredAt: "2026-09-08T10:14:30.123456Z",
          preview: {
            type: "CREATE_TASK",
            projectId: PROJECT,
            title: "Revisar Otra",
            completionCriterion: "",
            estimatedMinutes: 30,
            wouldFail: null,
          },
          loopGuarded: false,
        },
      ],
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.type(screen.getByLabelText(/nombre/i), "Seguimiento");
    await userEvent.click(screen.getByRole("button", { name: "Simular" }));
    await screen.findByText("Fallaría: proyecto completado");
    expect(
      screen.getByRole("status", { name: "Resultado de la simulación" }),
    ).toHaveTextContent("5 eventos evaluados, 2 coincidencias");
    expect(
      within(screen.getByRole("list", { name: "Coincidencias" })).getByText(
        "Revisar Redactar informe",
      ),
    ).toBeInTheDocument();
    expect(
      calls.filter(
        (call) => call.url.endsWith("/simulate") && call.method === "POST",
      ),
    ).toHaveLength(1);
    expect(
      calls.filter(
        (call) =>
          call.method === "POST" && call.url === "/api/v1/me/automations",
      ),
    ).toHaveLength(0);
    expect(screen.getByRole("button", { name: "Guardar" })).toBeEnabled();
  });

  it("@s40 disables save until the answer arrives and sends exactly one request", async () => {
    listed();
    let release = () => {};
    route(
      "POST",
      "/api/v1/me/automations",
      201,
      rule,
      new Promise<void>((resolve) => (release = resolve)),
    );
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.type(screen.getByLabelText(/nombre/i), "Seguimiento");
    const save = screen.getByRole("button", { name: "Guardar" });
    await userEvent.click(save);
    await waitFor(() => expect(save).toBeDisabled());
    await userEvent.click(save, { pointerEventsCheck: 0 });
    release();
    await waitFor(() =>
      expect(
        calls.filter(
          (call) =>
            call.method === "POST" && call.url === "/api/v1/me/automations",
        ),
      ).toHaveLength(1),
    );
  });

  it("@s40 explains a 412 and keeps the draft until the reload is asked for", async () => {
    listed({ ...rule, version: 2 });
    route("PUT", `/api/v1/me/automations/${RULE}`, 412, {
      code: "AUTOMATION_CONFLICT",
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Editar Seguimiento" }),
    );
    const name = screen.getByLabelText(/nombre/i);
    await userEvent.clear(name);
    await userEvent.type(name, "Renombrada");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await screen.findByText("Otra pestaña cambió esta regla");
    expect(name).toHaveValue("Renombrada");
    routes.delete("GET /api/v1/me/automations");
    listed({ ...rule, version: 3, name: "Del servidor" });
    await userEvent.click(
      screen.getByRole("button", { name: "Cargar versión actual" }),
    );
    await waitFor(() =>
      expect(screen.getByLabelText(/nombre/i)).toHaveValue("Del servidor"),
    );
  });

  const putsOf = (id: string) =>
    calls.filter(
      (call) =>
        call.method === "PUT" && call.url === `/api/v1/me/automations/${id}`,
    );

  it("@s40 flips the switch only after the confirmed answer and sends the live ETag", async () => {
    listed({ ...rule, version: 2 });
    let release = () => {};
    route(
      "PUT",
      `/api/v1/me/automations/${RULE}`,
      200,
      { ...rule, version: 3, enabled: false },
      new Promise<void>((resolve) => (release = resolve)),
    );
    route("PUT", `/api/v1/me/automations/${RULE}`, 200, {
      ...rule,
      version: 4,
      enabled: true,
    });
    render(<Automations owner="owner" />);
    const toggle = await screen.findByRole("switch", { name: /seguimiento/i });
    expect(toggle).toBeChecked();
    await userEvent.click(toggle);
    await waitFor(() => expect(putsOf(RULE)).toHaveLength(1));
    // In flight: nothing may change until the server confirms it.
    expect(toggle).toBeChecked();
    expect(screen.getByText("Activa")).toBeInTheDocument();
    release();
    await waitFor(() => expect(toggle).not.toBeChecked());
    expect(screen.getByText("Inactiva")).toBeInTheDocument();
    expect(putsOf(RULE)[0].headers.get("If-Match")).toBe('"2"');
    // The version the server just returned is the one the next write must send.
    await userEvent.click(toggle);
    await waitFor(() => expect(toggle).toBeChecked());
    expect(putsOf(RULE)[1].headers.get("If-Match")).toBe('"3"');
  });

  it("@s40 puts the switch back and announces the error when the server fails", async () => {
    listed({ ...rule, version: 2 });
    let release = () => {};
    route(
      "PUT",
      `/api/v1/me/automations/${RULE}`,
      503,
      { code: "STORAGE_UNAVAILABLE" },
      new Promise<void>((resolve) => (release = resolve)),
    );
    render(<Automations owner="owner" />);
    const toggle = await screen.findByRole("switch", { name: /seguimiento/i });
    await userEvent.click(toggle);
    await waitFor(() => expect(putsOf(RULE)).toHaveLength(1));
    // No optimistic flip: an unchecked switch here would be undone a moment later.
    expect(toggle).toBeChecked();
    expect(screen.getByText("Activa")).toBeInTheDocument();
    release();
    await screen.findByRole("alert");
    expect(toggle).toBeChecked();
    expect(screen.getByText("Activa")).toBeInTheDocument();
    expect(putsOf(RULE)).toHaveLength(1);
    expect(putsOf(RULE)[0].headers.get("If-Match")).toBe('"2"');
  });

  it("@s41 loads the history in pages with textual state and a link to the task", async () => {
    listed(rule);
    const run = (
      index: number,
      status: AutomationRun["status"],
    ): AutomationRun => ({
      id: `4444444${index}-4444-4444-8444-444444444444`,
      eventId: "55555555-5555-4555-8555-555555555555",
      eventType: "TaskCreated.v1",
      occurredAt: "2026-09-08T10:15:30.123456Z",
      attempt: 1,
      status,
      createdTaskId: status === "succeeded" ? PROJECT : null,
      deliveryId: null,
      errorCode: status === "failed" ? "PROJECT_COMPLETED" : null,
      executedAt: "2026-09-08T10:15:30.123456Z",
    });
    route("GET", `/api/v1/me/automations/${RULE}/runs`, 200, {
      items: [run(0, "succeeded"), run(1, "retry"), run(2, "failed")],
      nextCursor: "siguiente",
    });
    route("GET", `/api/v1/me/automations/${RULE}/runs`, 200, {
      items: [run(3, "succeeded")],
      nextCursor: null,
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Historial de Seguimiento" }),
    );
    expect(await screen.findByText("Correcta")).toBeInTheDocument();
    expect(screen.getByText("Reintento")).toBeInTheDocument();
    expect(screen.getByText("Fallida")).toBeInTheDocument();
    expect(screen.getByText("PROJECT_COMPLETED")).toBeInTheDocument();
    const history = screen.getByRole("list", { name: "Ejecuciones" });
    expect(within(history).getAllByRole("listitem")).toHaveLength(3);
    expect(within(history).getAllByRole("link")[0]).toHaveAttribute(
      "href",
      `/proyectos/${PROJECT}/tareas/${PROJECT}`,
    );
    await userEvent.click(screen.getByRole("button", { name: "Cargar más" }));
    await waitFor(() =>
      expect(within(history).getAllByRole("listitem")).toHaveLength(4),
    );
    expect(
      screen.queryByRole("button", { name: "Cargar más" }),
    ).not.toBeInTheDocument();
  });

  it("@s43 ignores a late answer after the component is gone and leaves no storage behind", async () => {
    listed();
    let release = () => {};
    route(
      "POST",
      "/api/v1/me/automations/simulate",
      200,
      { evaluatedEvents: 5, matches: [] },
      new Promise<void>((resolve) => (release = resolve)),
    );
    const view = render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.type(screen.getByLabelText(/nombre/i), "Seguimiento");
    await userEvent.click(screen.getByRole("button", { name: "Simular" }));
    view.unmount();
    release();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it("@s43 keeps no rule nor simulation of the identity that just left", async () => {
    listed(rule);
    listed(disabled);
    route("POST", "/api/v1/me/automations/simulate", 200, {
      evaluatedEvents: 5,
      matches: [
        {
          eventId: "55555555-5555-4555-8555-555555555555",
          eventType: "TaskCreated.v1",
          occurredAt: "2026-09-08T10:15:30.123456Z",
          preview: {
            type: "CREATE_TASK",
            projectId: PROJECT,
            title: "Revisar lo de Ana",
            completionCriterion: "",
            estimatedMinutes: 30,
            wouldFail: null,
          },
          loopGuarded: false,
        },
      ],
    });
    const view = render(<Automations owner="ana" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Editar Seguimiento" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Simular" }));
    await screen.findByRole("status", { name: "Resultado de la simulación" });
    expect(screen.getByText("Revisar lo de Ana")).toBeInTheDocument();

    view.rerender(<Automations owner="bruno" />);

    await screen.findByRole("switch", { name: /pausada/i });
    expect(screen.queryByText("Revisar lo de Ana")).not.toBeInTheDocument();
    expect(
      screen.queryByRole("status", { name: "Resultado de la simulación" }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole("switch", { name: /seguimiento/i }),
    ).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/nombre/i)).not.toBeInTheDocument();
    expect(
      calls.filter(
        (call) =>
          call.method === "GET" && call.url === "/api/v1/me/automations",
      ),
    ).toHaveLength(2);
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  const TRIGGERS: [EventType, string][] = [
    ["ProjectCreated.v1", "Proyecto creado"],
    ["ProjectUpdated.v1", "Proyecto editado"],
    ["ProjectStatusChanged.v1", "Estado de proyecto cambiado"],
    ["TaskCreated.v1", "Tarea creada"],
    ["SubtaskCreated.v1", "Subtarea creada"],
    ["TaskStatusChanged.v1", "Estado de tarea cambiado"],
    ["BlockPlanned.v1", "Bloque planificado"],
    ["BlockChanged.v1", "Bloque modificado"],
    ["WorkSessionStarted.v1", "Sesión iniciada"],
    ["WorkSessionStateChanged.v1", "Sesión pausada o reanudada"],
    ["WorkSessionExtended.v1", "Sesión ampliada"],
    ["WorkSessionClosed.v1", "Sesión cerrada"],
  ];

  it("@s37 writes each of the twelve published triggers in readable Spanish", async () => {
    listed(
      ...TRIGGERS.map(([eventType], index) => ({
        ...rule,
        id: `2222222${index.toString(16)}-2222-4222-8222-222222222222`,
        name: `Regla ${index}`,
        trigger: { eventType },
      })),
    );
    render(<Automations owner="owner" />);
    const rows = await screen.findAllByRole("listitem");
    expect(rows).toHaveLength(TRIGGERS.length);
    TRIGGERS.forEach(([, label], index) =>
      expect(within(rows[index]).getByText(label), label).toBeInTheDocument(),
    );
  });

  it("@s38 offers the twelve triggers in the editor, in the published order", async () => {
    listed();
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    const trigger = screen.getByLabelText("Disparador");
    expect(
      within(trigger).getAllByRole("option").map((option) => option.textContent),
    ).toEqual(TRIGGERS.map(([, label]) => label));
    expect(trigger).toHaveValue("TaskCreated.v1");
    const condition = screen.getByLabelText("Sólo en el proyecto");
    expect(
      within(condition)
        .getAllByRole("option")
        .map((option) => option.textContent),
    ).toEqual(["Cualquiera", "Marketing"]);
  });

  const FAILURES: [string, string][] = [
    ["PROJECT_COMPLETED", "proyecto completado"],
    ["TITLE_TOO_LONG", "título demasiado largo"],
    ["CRITERION_TOO_LONG", "criterio demasiado largo"],
    ["ENDPOINT_NOT_FOUND", "endpoint no encontrado"],
    ["TARGET_NOT_FOUND", "destino no encontrado"],
    ["LO_QUE_SEA", "LO_QUE_SEA"],
  ];

  it("@s39 explains every failure the simulation can foresee and repeats the code it does not know", async () => {
    listed();
    route("POST", "/api/v1/me/automations/simulate", 200, {
      evaluatedEvents: FAILURES.length,
      matches: FAILURES.map(([code], index) => ({
        eventId: `5555555${index.toString(16)}-5555-4555-8555-555555555555`,
        eventType: "TaskCreated.v1",
        occurredAt: "2026-09-08T10:15:30.123456Z",
        preview: {
          type: "CREATE_TASK",
          projectId: PROJECT,
          title: `Revisar ${index}`,
          completionCriterion: "",
          estimatedMinutes: null,
          wouldFail: code,
        },
        loopGuarded: false,
      })),
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Simular" }));
    await screen.findByRole("status", { name: "Resultado de la simulación" });
    for (const [code, text] of FAILURES)
      expect(screen.getByText(`Fallaría: ${text}`), code).toBeInTheDocument();
  });

  it("@s38 replaces the four markers with their sample values and lists them verbatim", async () => {
    listed();
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    const title = screen.getByLabelText(/título de la tarea/i);
    await userEvent.clear(title);
    await userEvent.click(title);
    await userEvent.paste(
      "{{event.type}} / {{task.title}} / {{project.name}} / {{occurredAt}} / {{otro}}",
    );
    expect(screen.getByTestId("automation-preview")).toHaveTextContent(
      "TaskCreated.v1 / Redactar informe / Marketing / 2026-09-08T10:15:30.123456Z / {{otro}}",
    );
    expect(screen.getByTestId("automation-placeholders")).toHaveTextContent(
      "Marcadores disponibles: {{event.type}}, {{task.title}}, {{project.name}}, {{occurredAt}}",
    );
  });

  const FIELDS: [string, string][] = [
    ["name", "automation-name"],
    ["action.titleTemplate", "automation-title"],
    ["action.criterionTemplate", "automation-criterion"],
    ["trigger.eventType", "automation-trigger"],
    ["condition.projectId", "automation-condition"],
    ["accion.desconocida", "automation-name"],
  ];

  it("@s38 focuses the control that owns the field the server complained about", async () => {
    for (const [field, control] of FIELDS) {
      routes = new Map();
      calls = [];
      route("GET", "/api/v1/projects", 200, projects);
      listed();
      route("POST", "/api/v1/me/automations", 422, {
        code: "VALIDATION_ERROR",
        errors: [{ field, code: "REQUIRED", message: "x" }],
      });
      const view = render(<Automations owner="owner" />);
      await userEvent.click(
        await screen.findByRole("button", { name: "Nueva regla" }),
      );
      await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
      await waitFor(() =>
        expect(document.getElementById(control), field).toHaveFocus(),
      );
      view.unmount();
    }
  });

  const CREATE = "/api/v1/me/automations";

  it("@s40 sends a brand new rule exactly as the editor shows it", async () => {
    listed();
    route("POST", CREATE, 201, { ...rule, name: "Seguimiento" });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    expect(screen.getByLabelText("Nombre")).toHaveValue("");
    expect(screen.getByLabelText("Disparador")).toHaveValue("TaskCreated.v1");
    expect(screen.getByLabelText("Sólo en el proyecto")).toHaveValue("");
    expect(screen.getByLabelText("Título de la tarea")).toHaveValue(
      "Revisar {{task.title}}",
    );
    expect(screen.getByLabelText("Criterio de la tarea")).toHaveValue("");
    await userEvent.type(screen.getByLabelText("Nombre"), "Seguimiento");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await waitFor(() => expect(sent("POST", CREATE)).toHaveLength(1));
    expect(sent("POST", CREATE)[0]).toEqual({
      name: "Seguimiento",
      enabled: true,
      trigger: { eventType: "TaskCreated.v1" },
      condition: null,
      action: {
        type: "CREATE_TASK",
        projectId: PROJECT,
        titleTemplate: "Revisar {{task.title}}",
        criterionTemplate: null,
        estimatedMinutes: null,
      },
    });
  });

  it("@s40 sends the trigger, the condition and the criterion the owner picked", async () => {
    listed();
    route("POST", CREATE, 201, rule);
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.type(screen.getByLabelText("Nombre"), "Seguimiento");
    await userEvent.selectOptions(
      screen.getByLabelText("Disparador"),
      "BlockPlanned.v1",
    );
    await userEvent.selectOptions(
      screen.getByLabelText("Sólo en el proyecto"),
      PROJECT,
    );
    await userEvent.type(
      screen.getByLabelText("Criterio de la tarea"),
      "Con el informe enviado",
    );
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await waitFor(() => expect(sent("POST", CREATE)).toHaveLength(1));
    expect(sent("POST", CREATE)[0]).toMatchObject({
      trigger: { eventType: "BlockPlanned.v1" },
      condition: { projectId: PROJECT },
      action: { criterionTemplate: "Con el informe enviado" },
    });
  });

  it("@s40 gives back untouched the parts of the rule the editor does not show", async () => {
    listed({ ...rule, version: 2 }, disabled);
    route("PUT", `${CREATE}/${RULE}`, 200, {
      ...rule,
      version: 3,
      name: "Renombrada",
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Editar Seguimiento" }),
    );
    const name = screen.getByLabelText("Nombre");
    await userEvent.clear(name);
    await userEvent.type(name, "Renombrada");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await waitFor(() => expect(sent("PUT", `${CREATE}/${RULE}`)).toHaveLength(1));
    expect(sent("PUT", `${CREATE}/${RULE}`)[0]).toEqual({
      name: "Renombrada",
      enabled: true,
      trigger: { eventType: "TaskCreated.v1" },
      condition: null,
      action: {
        type: "CREATE_TASK",
        projectId: PROJECT,
        titleTemplate: "Revisar {{task.title}}",
        criterionTemplate: null,
        estimatedMinutes: 30,
      },
    });
    // La regla guardada sustituye a la de la lista, no se añade otra.
    expect(await screen.findByText("Renombrada")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getByText("Pausada")).toBeInTheDocument();
    expect(screen.queryByText("Seguimiento")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Nombre")).not.toBeInTheDocument();
  });

  it("@s40 carries the condition and the criterion of the rule it is editing", async () => {
    listed({
      ...rule,
      enabled: false,
      condition: { projectId: PROJECT },
      action: {
        type: "CREATE_TASK",
        projectId: PROJECT,
        titleTemplate: "Revisar {{task.title}}",
        criterionTemplate: "Con el informe enviado",
        estimatedMinutes: null,
      },
    });
    route("PUT", `${CREATE}/${RULE}`, 200, rule);
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Editar Seguimiento" }),
    );
    expect(screen.getByLabelText("Sólo en el proyecto")).toHaveValue(PROJECT);
    const criterion = screen.getByLabelText("Criterio de la tarea");
    expect(criterion).toHaveValue("Con el informe enviado");
    await userEvent.clear(criterion);
    await userEvent.type(criterion, "Con el informe revisado");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await waitFor(() => expect(sent("PUT", `${CREATE}/${RULE}`)).toHaveLength(1));
    expect(sent("PUT", `${CREATE}/${RULE}`)[0]).toMatchObject({
      enabled: false,
      condition: { projectId: PROJECT },
      action: {
        criterionTemplate: "Con el informe revisado",
        estimatedMinutes: null,
      },
    });
  });

  it("@s37 adds the new rule to the list instead of replacing it", async () => {
    listed(rule);
    route("POST", CREATE, 201, { ...rule, id: OTHER_RULE, name: "Otra" });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.type(screen.getByLabelText("Nombre"), "Otra");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await waitFor(() => expect(screen.getAllByRole("listitem")).toHaveLength(2));
    expect(screen.getByText("Seguimiento")).toBeInTheDocument();
    expect(screen.getByText("Otra")).toBeInTheDocument();
  });

  it("@s37 still lets a rule be written when the projects could not be read", async () => {
    for (const existing of [[], [rule]]) {
      routes = new Map();
      calls = [];
      route("GET", "/api/v1/projects", 503, { code: "STORAGE_UNAVAILABLE" });
      listed(...existing);
      route("POST", CREATE, 201, { ...rule, id: OTHER_RULE, name: "Otra" });
      const view = render(<Automations owner="owner" />);
      await userEvent.click(
        await screen.findByRole("button", { name: "Nueva regla" }),
      );
      expect(
        within(screen.getByLabelText("Sólo en el proyecto"))
          .getAllByRole("option")
          .map((option) => option.textContent),
        `${existing.length} reglas`,
      ).toEqual(["Cualquiera"]);
      await userEvent.type(screen.getByLabelText("Nombre"), "Otra");
      await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
      await waitFor(() => expect(sent("POST", CREATE)).toHaveLength(1));
      expect(sent("POST", CREATE)[0], `${existing.length} reglas`).toMatchObject(
        { action: { type: "CREATE_TASK", projectId: "" } },
      );
      view.unmount();
    }
  });

  const ENDPOINT = "88888888-8888-4888-8888-888888888888";
  const webhookRule: Automation = {
    ...rule,
    id: OTHER_RULE,
    name: "Aviso",
    version: 2,
    action: { type: "NOTIFY_WEBHOOK", endpointId: ENDPOINT },
  };

  it("@s37 keeps the endpoint of a webhook rule when only its name changes", async () => {
    listed(webhookRule);
    route("PUT", `${CREATE}/${OTHER_RULE}`, 200, {
      ...webhookRule,
      version: 3,
      name: "Aviso renombrado",
    });
    render(<Automations owner="owner" />);
    const row = await screen.findByRole("listitem");
    expect(within(row).getByText("Webhook")).toBeInTheDocument();
    expect(within(row).queryByText("Marketing")).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Editar Aviso" }));
    const name = screen.getByLabelText("Nombre");
    await userEvent.clear(name);
    await userEvent.type(name, "Aviso renombrado");
    await userEvent.click(screen.getByRole("button", { name: "Guardar" }));
    await waitFor(() =>
      expect(sent("PUT", `${CREATE}/${OTHER_RULE}`)).toHaveLength(1),
    );
    // Renombrar no puede convertir un aviso en una tarea ni perder el endpoint.
    expect(sent("PUT", `${CREATE}/${OTHER_RULE}`)[0]).toEqual({
      name: "Aviso renombrado",
      enabled: true,
      trigger: { eventType: "TaskCreated.v1" },
      condition: null,
      action: { type: "NOTIFY_WEBHOOK", endpointId: ENDPOINT },
    });
  });

  it("@s40 keeps the endpoint of a webhook rule when the switch is flipped", async () => {
    listed({ ...webhookRule, enabled: false });
    route("PUT", `${CREATE}/${OTHER_RULE}`, 200, {
      ...webhookRule,
      version: 3,
    });
    render(<Automations owner="owner" />);
    await userEvent.click(await screen.findByRole("switch", { name: /aviso/i }));
    await waitFor(() =>
      expect(sent("PUT", `${CREATE}/${OTHER_RULE}`)).toHaveLength(1),
    );
    expect(sent("PUT", `${CREATE}/${OTHER_RULE}`)[0]).toEqual({
      name: "Aviso",
      enabled: true,
      trigger: { eventType: "TaskCreated.v1" },
      condition: null,
      action: { type: "NOTIFY_WEBHOOK", endpointId: ENDPOINT },
    });
  });

  it("@s39 previews a webhook notice instead of a resolved task title", async () => {
    listed();
    route("POST", "/api/v1/me/automations/simulate", 200, {
      evaluatedEvents: 1,
      matches: [
        {
          eventId: "55555555-5555-4555-8555-555555555555",
          eventType: "TaskCreated.v1",
          occurredAt: "2026-09-08T10:15:30.123456Z",
          preview: {
            type: "NOTIFY_WEBHOOK",
            endpointId: ENDPOINT,
            eventId: "55555555-5555-4555-8555-555555555555",
          },
          loopGuarded: false,
        },
      ],
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Nueva regla" }),
    );
    await userEvent.click(screen.getByRole("button", { name: "Simular" }));
    const match = await screen.findByRole("listitem");
    expect(within(match).getByText("Aviso al webhook")).toBeInTheDocument();
    expect(within(match).queryByText(/Fallaría/)).not.toBeInTheDocument();
  });

  it("@s37 shows the identifier of a destination it cannot name", async () => {
    listed({
      ...rule,
      action: { ...rule.action, projectId: OTHER_RULE } as Automation["action"],
    });
    render(<Automations owner="owner" />);
    expect(await screen.findByText(OTHER_RULE)).toBeInTheDocument();
  });

  it("@s43 drops the history of the rule the owner just left", async () => {
    listed(rule, disabled);
    let release = () => {};
    route(
      "GET",
      `/api/v1/me/automations/${RULE}/runs`,
      200,
      { items: [], nextCursor: null },
      new Promise<void>((resolve) => (release = resolve)),
    );
    route("GET", `/api/v1/me/automations/${OTHER_RULE}/runs`, 200, {
      items: [],
      nextCursor: null,
    });
    render(<Automations owner="owner" />);
    await userEvent.click(
      await screen.findByRole("button", { name: "Historial de Seguimiento" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Historial de Pausada" }),
    );
    release();
    await screen.findByRole("list", { name: "Ejecuciones" });
    expect(screen.getByTestId("automation-history-title")).toHaveTextContent(
      "Pausada",
    );
  });
});
