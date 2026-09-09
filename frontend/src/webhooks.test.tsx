import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, expect, it, vi } from "vitest";
import { Webhooks } from "./webhooks";

const id = "12345678-1234-4234-8234-123456789abc";
const second = "22222222-2222-4222-8222-222222222222";
const deliveryId = "33333333-3333-4333-8333-333333333333";
const secret = `whsec_${"A".repeat(43)}`;

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  localStorage.clear();
  sessionStorage.clear();
  window.history.replaceState(null, "", "/");
});

function endpoint(overrides: Record<string, unknown> = {}) {
  return {
    id,
    url: "https://example.com/hooks",
    description: "Mi hook",
    eventTypes: ["TaskCreated.v1"],
    status: "active",
    disabledReason: null,
    disabledAt: null,
    createdAt: "2026-09-08T10:00:00.000000Z",
    updatedAt: "2026-09-08T10:00:00.000000Z",
    ...overrides,
  };
}

function delivery(overrides: Record<string, unknown> = {}) {
  return {
    id: deliveryId,
    eventId: deliveryId,
    eventType: "TaskCreated.v1",
    status: "succeeded",
    attempt: 1,
    httpStatus: 200,
    latencyMs: 12,
    errorClass: null,
    nextAttemptAt: null,
    createdAt: "2026-09-08T10:00:00.000000Z",
    updatedAt: "2026-09-08T10:00:00.000000Z",
    ...overrides,
  };
}

/** Answers the initial GET with the given list and routes everything else to the handler. */
function stubApi(items: unknown[], handler?: typeof fetch) {
  const other = vi.fn(
    handler ?? (() => Promise.reject(new Error("unexpected"))),
  );
  const fetcher = vi.fn((url: RequestInfo | URL, options?: RequestInit) =>
    url === "/api/v1/me/webhooks" &&
    (!options?.method || options.method === "GET")
      ? Promise.resolve(Response.json({ items }))
      : other(url as never, options as never),
  );
  vi.stubGlobal("fetch", fetcher);
  return { fetcher, other };
}

/** Typed accessors over the recorded fetch calls, so the tests stay readable. */
type Call = [RequestInfo | URL, RequestInit | undefined];
const callsOf = (mock: { mock: { calls: unknown[][] } }) =>
  mock.mock.calls as unknown as Call[];
const optionsOf = (mock: { mock: { calls: unknown[][] } }, index = 0) =>
  callsOf(mock)[index][1]!;
const bodyOf = (mock: { mock: { calls: unknown[][] } }, index = 0) =>
  JSON.parse(String(optionsOf(mock, index).body)) as Record<string, unknown>;
const urlOf = (mock: { mock: { calls: unknown[][] } }, index = 0) =>
  String(callsOf(mock)[index][0]);

async function shown() {
  await waitFor(() =>
    expect(screen.queryByText("Cargando webhooks…")).not.toBeInTheDocument(),
  );
}

it("@s42 provides the main landmark the skip link points at", async () => {
  stubApi([]);

  render(<Webhooks owner="Ana" />);
  await shown();

  const main = screen.getByRole("main");
  expect(main).toHaveAttribute("id", "proyectos");
  expect(main).toContainElement(
    screen.getByRole("heading", { level: 1, name: "Webhooks" }),
  );
});

it("@s36 announces the loading state before showing anything else", async () => {
  let reply!: (response: Response) => void;
  vi.stubGlobal(
    "fetch",
    vi.fn(() => new Promise<Response>((resolve) => (reply = resolve))),
  );

  render(<Webhooks owner="Ana" />);

  expect(
    screen.getByRole("heading", { level: 1, name: "Webhooks" }),
  ).toBeVisible();
  const loading = screen.getByText("Cargando webhooks…");
  expect(loading).toBeVisible();
  expect(loading.closest("[aria-live]")).not.toBeNull();

  reply(Response.json({ items: [] }));
  await shown();
});

it("@s36 explains what a webhook is when the list is empty and keeps the form visible", async () => {
  stubApi([]);

  render(<Webhooks owner="Ana" />);
  await shown();

  expect(screen.getByText(/X-OrganizationWeb-Signature/)).toBeVisible();
  expect(screen.getByText(/cinco/i)).toBeVisible();
  expect(screen.getByRole("textbox", { name: "URL" })).toBeVisible();
});

it("@s36 reports a failed load with an alert and retries only the GET", async () => {
  const fetcher = vi
    .fn()
    .mockResolvedValueOnce(
      Response.json({ code: "STORAGE_UNAVAILABLE" }, { status: 503 }),
    )
    .mockResolvedValueOnce(Response.json({ items: [endpoint()] }));
  vi.stubGlobal("fetch", fetcher);
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await waitFor(() => expect(screen.getByRole("alert")).toBeVisible());

  await user.click(screen.getByRole("button", { name: "Reintentar" }));

  await waitFor(() =>
    expect(screen.getByText("https://example.com/hooks")).toBeVisible(),
  );
  expect(fetcher).toHaveBeenCalledTimes(2);
  expect(callsOf(fetcher).every(([, options]) => !options?.method)).toBe(true);
});

it("@s36 lists each webhook with its url, description, types and status", async () => {
  stubApi([
    endpoint(),
    endpoint({
      id: second,
      url: "https://otro.example/h",
      description: "Otro",
      status: "disabled",
      disabledReason: "MANUAL",
      disabledAt: "2026-09-08T11:00:00.000000Z",
    }),
  ]);

  render(<Webhooks owner="Ana" />);
  await shown();

  expect(screen.getByText("https://example.com/hooks")).toBeVisible();
  expect(screen.getByText("Mi hook")).toBeVisible();
  expect(screen.getByText("https://otro.example/h")).toBeVisible();
  expect(screen.getAllByText("Activo").length).toBe(1);
  expect(screen.getByText("Desactivado manualmente")).toBeVisible();
});

it("@s39 shows an exhausted webhook with its date and the same value in ARIA", async () => {
  stubApi([
    endpoint({
      status: "disabled",
      disabledReason: "DELIVERY_EXHAUSTED",
      disabledAt: "2026-09-08T11:00:00.000000Z",
    }),
  ]);

  render(<Webhooks owner="Ana" />);
  await shown();

  const label = "Desactivado por entregas agotadas el 2026-09-08";
  expect(screen.getByText(label)).toBeVisible();
  expect(screen.getByLabelText(label)).toBeInTheDocument();
});

it("@s37 sends one POST with the twelve types and shows the secret once", async () => {
  let reply!: (response: Response) => void;
  const { other } = stubApi(
    [],
    () => new Promise<Response>((r) => (reply = r)),
  );
  const user = userEvent.setup();
  const write = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();

  render(<Webhooks owner="Ana" />);
  await shown();

  const url = screen.getByRole("textbox", { name: "URL" });
  expect(url).toHaveAttribute("type", "url");
  await user.type(url, "https://example.com/hooks");
  await user.click(screen.getByRole("checkbox", { name: "Seleccionar todos" }));

  const create = screen.getByRole("button", { name: "Crear webhook" });
  await user.click(create);
  await user.click(create);

  expect(other).toHaveBeenCalledTimes(1);
  expect(create).toBeDisabled();
  expect(bodyOf(other).eventTypes).toHaveLength(12);

  reply(Response.json({ endpoint: endpoint(), secret }, { status: 201 }));

  await waitFor(() => expect(screen.getByDisplayValue(secret)).toBeVisible());
  const field = screen.getByDisplayValue(secret);
  expect(field).toHaveAttribute("readonly");
  const copy = screen.getByRole("button", { name: "Copiar" });
  expect(copy).toBeVisible();
  expect(screen.getByText(/no volverá a mostrarse/i)).toBeVisible();
  expect(screen.getByText("https://example.com/hooks")).toBeVisible();

  expect(JSON.stringify(localStorage)).not.toContain(secret);
  expect(JSON.stringify(sessionStorage)).not.toContain(secret);
  expect(window.location.href).not.toContain(secret);
  // "no se copia sin activar Copiar": showing the secret must not reach the
  // clipboard, and the gesture must write it exactly once.
  expect(write).not.toHaveBeenCalled();

  await user.click(copy);

  expect(write).toHaveBeenCalledExactlyOnceWith(secret);
});

it("@s37 a second submit that bypasses the disabled button still sends nothing", async () => {
  // Disabling the button stops the second click, but a form can also be submitted
  // implicitly (Enter, requestSubmit). The guard inside the handler is what covers that.
  const { other } = stubApi([], () => new Promise<Response>(() => {}));
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.type(
    screen.getByRole("textbox", { name: "URL" }),
    "https://example.com/hooks",
  );
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));

  const form = document.querySelector("form")!;
  form.requestSubmit();
  await waitFor(() => expect(other).toHaveBeenCalledTimes(1));
  form.requestSubmit();
  form.requestSubmit();

  await new Promise((resolve) => setTimeout(resolve, 0));
  expect(other).toHaveBeenCalledTimes(1);
});

it("@s37 hides the secret only when Cerrar is activated", async () => {
  const { other } = stubApi([], () =>
    Promise.resolve(
      Response.json({ endpoint: endpoint(), secret }, { status: 201 }),
    ),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.type(
    screen.getByRole("textbox", { name: "URL" }),
    "https://example.com/hooks",
  );
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
  await user.click(screen.getByRole("button", { name: "Crear webhook" }));
  await waitFor(() => expect(screen.getByDisplayValue(secret)).toBeVisible());
  expect(other).toHaveBeenCalledTimes(1);

  await user.click(screen.getByRole("button", { name: "Cerrar" }));

  expect(screen.queryByDisplayValue(secret)).not.toBeInTheDocument();
});

it("@s38 aborts the pending creation when the view goes away and never shows its secret", async () => {
  let signal!: AbortSignal;
  const { other } = stubApi([], (_url, options) => {
    signal = options!.signal!;
    return new Promise<Response>(() => {});
  });
  const user = userEvent.setup();

  const view = render(<Webhooks owner="Ana" />);
  await shown();
  await user.type(
    screen.getByRole("textbox", { name: "URL" }),
    "https://example.com/hooks",
  );
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
  await user.click(screen.getByRole("button", { name: "Crear webhook" }));
  expect(other).toHaveBeenCalledTimes(1);

  view.unmount();

  expect(signal.aborted).toBe(true);
  expect(screen.queryByDisplayValue(secret)).not.toBeInTheDocument();
});

it("@s38 shows the secret for one identity and starts clean for another", async () => {
  // The Given of the outline is "un secreto visible en memoria": without first
  // proving the secret and Ana's list ARE on screen, the absence proves nothing.
  const bea = endpoint({
    id: second,
    url: "https://bea.example/h",
    description: "Hook de Bea",
  });
  const lists = [[], [bea]];
  let listed = 0;
  const fetcher = vi.fn((url: RequestInfo | URL, options?: RequestInit) =>
    url === "/api/v1/me/webhooks" &&
    (!options?.method || options.method === "GET")
      ? Promise.resolve(Response.json({ items: lists[listed++] ?? [] }))
      : Promise.resolve(
          Response.json({ endpoint: endpoint(), secret }, { status: 201 }),
        ),
  );
  vi.stubGlobal("fetch", fetcher);
  const user = userEvent.setup();

  const view = render(<Webhooks owner="Ana" />);
  await shown();
  await user.type(
    screen.getByRole("textbox", { name: "URL" }),
    "https://example.com/hooks",
  );
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
  await user.click(screen.getByRole("button", { name: "Crear webhook" }));

  // Presence: the secret and Ana's freshly created webhook are on screen.
  await waitFor(() => expect(screen.getByDisplayValue(secret)).toBeVisible());
  expect(screen.getByText("Mi hook")).toBeVisible();
  expect(screen.getByText("https://example.com/hooks")).toBeVisible();

  view.rerender(<Webhooks owner="Bea" />);
  await shown();

  // Absence: nothing of Ana survives into Bea's session.
  expect(screen.queryByDisplayValue(secret)).not.toBeInTheDocument();
  expect(screen.queryByText("Mi hook")).not.toBeInTheDocument();
  expect(
    screen.queryByText("https://example.com/hooks"),
  ).not.toBeInTheDocument();
  expect(screen.getByText("Hook de Bea")).toBeVisible();
});

it("@s39 pings an active webhook and shows the pending delivery", async () => {
  const { other } = stubApi([endpoint()], (url) =>
    String(url).endsWith("/ping")
      ? Promise.resolve(
          Response.json(
            {
              delivery: delivery({
                eventType: "webhook.ping.v1",
                status: "pending",
                attempt: 0,
                httpStatus: null,
                latencyMs: null,
                nextAttemptAt: "2026-09-08T10:00:00.000000Z",
              }),
            },
            { status: 202 },
          ),
        )
      : Promise.resolve(Response.json({ items: [] })),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.click(screen.getByRole("button", { name: "Enviar ping" }));

  await waitFor(() =>
    expect(screen.getByText("webhook.ping.v1")).toBeVisible(),
  );
  expect(screen.getAllByText("Pendiente").length).toBeGreaterThan(0);
  expect(optionsOf(other).method).toBe("POST");
});

it("@s39 disables an active webhook through the status route", async () => {
  const { other } = stubApi([endpoint()], () =>
    Promise.resolve(
      Response.json(
        endpoint({
          status: "disabled",
          disabledReason: "MANUAL",
          disabledAt: "2026-09-08T11:00:00.000000Z",
        }),
      ),
    ),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.click(screen.getByRole("button", { name: "Desactivar" }));

  await waitFor(() =>
    expect(screen.getByText("Desactivado manualmente")).toBeVisible(),
  );
  expect(urlOf(other)).toBe(`/api/v1/me/webhooks/${id}/status`);
  expect(bodyOf(other)).toEqual({ status: "disabled" });
});

it("@s39 reactivates a manually disabled webhook", async () => {
  const { other } = stubApi(
    [
      endpoint({
        status: "disabled",
        disabledReason: "MANUAL",
        disabledAt: "2026-09-08T11:00:00.000000Z",
      }),
    ],
    () => Promise.resolve(Response.json(endpoint())),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.click(screen.getByRole("button", { name: "Activar" }));

  await waitFor(() => expect(screen.getByText("Activo")).toBeVisible());
  expect(bodyOf(other)).toEqual({ status: "active" });
});

it("@s39 asks for confirmation before deleting and only then sends the DELETE", async () => {
  const { other } = stubApi([endpoint()], () =>
    Promise.resolve(new Response(null, { status: 204 })),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.click(screen.getByRole("button", { name: "Eliminar" }));

  expect(other).not.toHaveBeenCalled();
  expect(screen.getByRole("dialog")).toBeVisible();

  await user.click(
    screen.getByRole("button", { name: "Confirmar eliminación" }),
  );

  await waitFor(() =>
    expect(
      screen.queryByText("https://example.com/hooks"),
    ).not.toBeInTheDocument(),
  );
  expect(optionsOf(other).method).toBe("DELETE");
  expect(
    screen.getByRole("heading", { level: 2, name: "Tus webhooks" }),
  ).toHaveFocus();
});

it("@s40 opens the deliveries panel on demand, without polling, and redelivers terminal rows", async () => {
  const { other } = stubApi([endpoint()], (url) =>
    String(url).endsWith("/redeliver")
      ? Promise.resolve(
          Response.json(
            {
              delivery: delivery({
                status: "pending",
                attempt: 0,
                httpStatus: null,
                latencyMs: null,
                nextAttemptAt: "2026-09-08T13:00:00.000000Z",
              }),
            },
            { status: 202 },
          ),
        )
      : Promise.resolve(
          Response.json({
            items: [
              delivery(),
              delivery({
                id: second,
                eventId: second,
                status: "exhausted",
                attempt: 6,
                errorClass: "HTTP_ERROR",
                httpStatus: 500,
              }),
              delivery({
                id: "44444444-4444-4444-8444-444444444444",
                eventId: "44444444-4444-4444-8444-444444444444",
                status: "pending",
                attempt: 0,
                httpStatus: null,
                latencyMs: null,
                nextAttemptAt: "2026-09-08T13:00:00.000000Z",
              }),
            ],
          }),
        ),
  );
  const user = userEvent.setup();
  vi.useFakeTimers({ shouldAdvanceTime: true });

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.click(screen.getByRole("button", { name: "Ver entregas" }));

  await waitFor(() => expect(screen.getByRole("table")).toBeVisible());
  const headers = screen
    .getAllByRole("columnheader")
    .map((cell) => cell.textContent);
  expect(headers).toEqual([
    "Tipo",
    "Intento",
    "Código HTTP",
    "Latencia",
    "Clase de error",
    "Estado",
    "Fecha",
    "Acciones",
  ]);
  expect(screen.getAllByRole("row")).toHaveLength(4);
  expect(screen.getAllByRole("button", { name: "Reenviar" })).toHaveLength(2);

  const callsBefore = other.mock.calls.length;
  await vi.advanceTimersByTimeAsync(30_000);
  expect(other.mock.calls.length).toBe(callsBefore);

  await user.click(screen.getAllByRole("button", { name: "Reenviar" })[0]);
  await waitFor(() =>
    expect(callsOf(other).some(([u]) => String(u).endsWith("/redeliver"))).toBe(
      true,
    ),
  );
  vi.useRealTimers();
});

it("@s40 the Actualizar button repeats a single deliveries GET", async () => {
  const { other } = stubApi([endpoint()], () =>
    Promise.resolve(Response.json({ items: [delivery()] })),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.click(screen.getByRole("button", { name: "Ver entregas" }));
  await waitFor(() => expect(screen.getByRole("table")).toBeVisible());
  const before = other.mock.calls.length;

  await user.click(screen.getByRole("button", { name: "Actualizar" }));

  await waitFor(() => expect(other.mock.calls.length).toBe(before + 1));
});

it.each([
  ["CONNECTORS_DISABLED", 503, /configuración del servidor/i],
  ["WEBHOOK_LIMIT", 409, /cinco/i],
])(
  "@s41 explains a %s without hiding the form",
  async (code, status, message) => {
    const { other } = stubApi([], () =>
      Promise.resolve(Response.json({ code }, { status })),
    );
    const user = userEvent.setup();

    render(<Webhooks owner="Ana" />);
    await shown();
    await user.type(
      screen.getByRole("textbox", { name: "URL" }),
      "https://example.com/hooks",
    );
    await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
    await user.click(screen.getByRole("button", { name: "Crear webhook" }));

    await waitFor(() =>
      expect(screen.getByRole("alert")).toHaveTextContent(message),
    );
    expect(screen.getByRole("textbox", { name: "URL" })).toHaveValue(
      "https://example.com/hooks",
    );
    expect(other).toHaveBeenCalledTimes(1);
  },
);

it("@s41 ties a blocked URL to the field with aria-describedby", async () => {
  stubApi([], () =>
    Promise.resolve(
      Response.json({ code: "WEBHOOK_URL_BLOCKED" }, { status: 400 }),
    ),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  const url = screen.getByRole("textbox", { name: "URL" });
  await user.type(url, "https://10.0.0.1/h");
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
  await user.click(screen.getByRole("button", { name: "Crear webhook" }));

  await waitFor(() => expect(url).toHaveAttribute("aria-describedby"));
  const described = url.getAttribute("aria-describedby")!;
  expect(document.getElementById(described)!.textContent).toMatch(
    /no está permitida/i,
  );
});

it("@s41 offers to refresh the list after a network failure of uncertain result", async () => {
  const { other } = stubApi([], () => Promise.reject(new TypeError("network")));
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.type(
    screen.getByRole("textbox", { name: "URL" }),
    "https://example.com/hooks",
  );
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
  await user.click(screen.getByRole("button", { name: "Crear webhook" }));

  await waitFor(() =>
    expect(screen.getByRole("alert")).toHaveTextContent(/no sabemos|incierto/i),
  );
  expect(
    screen.getByRole("button", { name: "Actualizar lista" }),
  ).toBeVisible();
  expect(other).toHaveBeenCalledTimes(1);
});

it("@s42 cancelling the delete confirmation returns focus to the control that opened it", async () => {
  stubApi([endpoint()]);
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  const remove = screen.getByRole("button", { name: "Eliminar" });
  await user.click(remove);
  expect(screen.getByRole("dialog")).toBeVisible();

  await user.click(screen.getByRole("button", { name: "Cancelar" }));

  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  expect(remove).toHaveFocus();
});

it("@s42 gives every control an accessible name and returns focus after closing the secret", async () => {
  const { other } = stubApi([], () =>
    Promise.resolve(
      Response.json({ endpoint: endpoint(), secret }, { status: 201 }),
    ),
  );
  const user = userEvent.setup();

  render(<Webhooks owner="Ana" />);
  await shown();
  await user.type(
    screen.getByRole("textbox", { name: "URL" }),
    "https://example.com/hooks",
  );
  await user.click(screen.getByRole("checkbox", { name: "Crear tarea" }));
  const create = screen.getByRole("button", { name: "Crear webhook" });
  await user.click(create);
  await waitFor(() => expect(screen.getByDisplayValue(secret)).toBeVisible());
  expect(other).toHaveBeenCalledTimes(1);

  await user.click(screen.getByRole("button", { name: "Cerrar" }));

  expect(create).toHaveFocus();
});
