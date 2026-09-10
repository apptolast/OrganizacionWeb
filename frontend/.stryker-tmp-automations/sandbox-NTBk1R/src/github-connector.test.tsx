// @ts-nocheck
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setCsrfToken } from "./api-client";
import { GithubConnector } from "./github-connector";

const projectId = "11111111-2222-4333-8444-555555555555";
const otherProjectId = "22222222-3333-4444-8555-666666666666";
const doneProjectId = "33333333-4444-4555-8666-777777777777";
const importId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

const receipt = {
  id: importId,
  source: "github",
  projectId,
  projectPath: "octocat/Hello-World",
  status: "completed",
  created: 199,
  skipped: 0,
  failed: 1,
  truncated: true,
  errorCode: null,
  startedAt: "2026-09-09T12:00:00.123456Z",
  finishedAt: "2026-09-09T12:00:09.123456Z",
};

const connection = {
  repository: "octocat/Hello-World",
  login: "octocat",
  status: "valid",
  connectedAt: "2026-09-09T10:00:00.123456Z",
  lastImport: null,
};

const projects = {
  items: [
    summary(projectId, "Primero", "idea"),
    summary(otherProjectId, "Segundo", "active"),
    summary(doneProjectId, "Terminado", "completed"),
  ],
  nextCursor: null,
};

function summary(id: string, name: string, status: string) {
  return {
    id,
    name,
    status,
    createdAt: "2026-09-01T08:00:00.000000Z",
    updatedAt: "2026-09-01T08:00:00.000000Z",
  };
}

function problem(status: number, body: Record<string, unknown>) {
  return Response.json(body, {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

/** Un servidor de mentira gobernado por ruta y método, para no depender del orden de las llamadas. */
type Route = (options: RequestInit) => Response | Promise<Response>;
let routes: Record<string, Route>;
let calls: { url: string; method: string }[];

function serve(url: string, method: string, route: Route) {
  routes[`${method} ${url}`] = route;
}

beforeEach(() => {
  calls = [];
  routes = {};
  serve("/api/v1/projects", "GET", () => Response.json(projects));
  serve("/api/v1/me/connectors/github", "GET", () => Response.json(connection));
  vi.stubGlobal(
    "fetch",
    vi.fn((url: string, options: RequestInit = {}) => {
      const method = options.method ?? "GET";
      calls.push({ url, method });
      const route = routes[`${method} ${url.split("?")[0]}`];
      if (!route) return Promise.resolve(new Response(null, { status: 404 }));
      return Promise.resolve(route(options));
    }),
  );
  setCsrfToken("csrf-own-session");
});

afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
});

const heading = () => screen.getByRole("heading", { level: 1 });
const importButton = () =>
  screen.getByRole("button", { name: "Importar issues abiertas" });

async function open() {
  render(<GithubConnector owner="owner" />);
  await screen.findByRole("heading", { level: 1, name: "Conector de GitHub" });
}

function callsTo(url: string, method: string) {
  return calls.filter(
    (call) => call.url.split("?")[0] === url && call.method === method,
  );
}

// ------------------------------------------------------------------ @s36 un estado a la vez

it("@s36 shows a server-configuration warning and nothing else when the connector is disabled", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(503, { code: "CONNECTORS_DISABLED" }),
  );

  await open();

  expect(await screen.findByRole("alert")).toHaveTextContent(
    /configuración del servidor/i,
  );
  expect(screen.queryByLabelText(/token/i)).toBeNull();
  expect(screen.queryByRole("combobox")).toBeNull();
  expect(
    screen.queryByRole("button", { name: "Importar issues abiertas" }),
  ).toBeNull();
});

it("@s36 shows the connect form and the permission help when there is no connection", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );

  await open();

  expect(await screen.findByLabelText(/repositorio/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/token/i)).toBeInTheDocument();
  expect(screen.getByText(/lectura de issues/i)).toBeInTheDocument();
  expect(screen.queryByRole("combobox")).toBeNull();
  expect(screen.queryByRole("button", { name: "Desconectar" })).toBeNull();
});

it("@s36 shows the connected state without the token form", async () => {
  await open();

  expect(await screen.findByText("octocat/Hello-World")).toBeInTheDocument();
  expect(screen.getByText("octocat")).toBeInTheDocument();
  expect(screen.getByText("Conectada")).toBeInTheDocument();
  expect(screen.getByRole("combobox")).toBeInTheDocument();
  expect(importButton()).toBeEnabled();
  expect(
    screen.getByRole("button", { name: "Desconectar" }),
  ).toBeInTheDocument();
  expect(screen.queryByLabelText(/token/i)).toBeNull();
});

it("@s36 shows the counters and a link to the project of the last import", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({ ...connection, lastImport: receipt }),
  );

  await open();

  expect(await screen.findByText("Creadas 199")).toBeInTheDocument();
  expect(screen.getByText("Omitidas 0")).toBeInTheDocument();
  expect(screen.getByText("Fallidas 1")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Primero/ })).toHaveAttribute(
    "href",
    `/proyectos/${projectId}`,
  );
  expect(screen.queryByLabelText(/token/i)).toBeNull();
});

it("@s36 offers Reconectar and no enabled import button when the connection is invalid", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({ ...connection, status: "invalid" }),
  );

  await open();

  expect(await screen.findByText("Conexión inválida")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Importar issues abiertas" }),
  ).toBeNull();
  await userEvent.click(screen.getByRole("button", { name: "Reconectar" }));
  expect(screen.getByLabelText(/token/i)).toBeInTheDocument();
});

it("@s36 warns about an import already running and offers to check its state", async () => {
  const running = {
    ...receipt,
    status: "running",
    created: 2,
    errorCode: null,
    finishedAt: null,
  };
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({ ...connection, lastImport: running }),
  );

  await open();

  expect(
    await screen.findByText("Hay una importación en curso"),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Consultar estado" }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Importar issues abiertas" }),
  ).toBeNull();
});

// ------------------------------------------------------------------- @s37 el formulario

it("@s37 keeps the token field a password that never autocompletes nor survives a send", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  serve("/api/v1/me/connectors/github", "PUT", () => Response.json(connection));
  await open();

  const token = await screen.findByLabelText(/token/i);
  expect(token).toHaveAttribute("type", "password");
  expect(token).toHaveAttribute("autocomplete", "off");

  await userEvent.type(
    screen.getByLabelText(/repositorio/i),
    "octocat/Hello-World",
  );
  await userEvent.type(token, "ghp_secreto123");
  await userEvent.click(screen.getByRole("button", { name: "Conectar" }));

  await screen.findByText("Conectada");
  expect(localStorage.length).toBe(0);
  expect(sessionStorage.length).toBe(0);
  expect(document.body.innerHTML).not.toContain("ghp_secreto123");
});

it("@s37 keeps the repository but clears the token and explains a rejected token by the field", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  serve("/api/v1/me/connectors/github", "PUT", () =>
    problem(409, { code: "GITHUB_TOKEN_REJECTED" }),
  );
  await open();

  await userEvent.type(
    await screen.findByLabelText(/repositorio/i),
    "octocat/Hello-World",
  );
  await userEvent.type(screen.getByLabelText(/token/i), "ghp_malo");
  await userEvent.click(screen.getByRole("button", { name: "Conectar" }));

  const token = await screen.findByLabelText(/token/i);
  await waitFor(() => expect(token).toHaveValue(""));
  expect(screen.getByLabelText(/repositorio/i)).toHaveValue(
    "octocat/Hello-World",
  );
  expect(token).toHaveAttribute("type", "password");
  expect(token).toHaveAttribute("autocomplete", "off");
  const described = token.getAttribute("aria-describedby")!.split(" ");
  expect(
    described
      .map((id) => document.getElementById(id)?.textContent ?? "")
      .join(" "),
  ).toContain("GitHub rechazó el token");
  expect(token).toHaveFocus();
  expect(document.body.innerHTML).not.toContain("ghp_malo");
});

// ---------------------------------------------------------------------- @s38 importar

it("@s38 offers only the projects that are not completed", async () => {
  await open();

  const options = within(await screen.findByRole("combobox")).getAllByRole(
    "option",
  );
  expect(options.map((option) => option.textContent)).toEqual([
    "Primero",
    "Segundo",
  ]);
  expect(callsTo("/api/v1/projects", "GET")).toHaveLength(1);
});

it("@s38 announces honest progress and then a measurable summary", async () => {
  let release: (value: Response) => void = () => {};
  serve(
    "/api/v1/me/connectors/github/imports",
    "POST",
    () => new Promise<Response>((resolve) => (release = resolve)),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  const live = screen.getByRole("status");
  expect(live).toHaveAttribute("aria-live", "polite");
  expect(live).toHaveTextContent("Importando issues…");
  expect(live.textContent).not.toMatch(/%/);
  expect(importButton()).toBeDisabled();

  release(Response.json(receipt, { status: 201 }));

  expect(await screen.findByText("Creadas 199")).toBeInTheDocument();
  expect(screen.getByText("Omitidas 0")).toBeInTheDocument();
  expect(screen.getByText("Fallidas 1")).toBeInTheDocument();
  expect(screen.getByText(/más issues/i)).toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Primero/ })).toHaveAttribute(
    "href",
    `/proyectos/${projectId}`,
  );
  expect(screen.getByRole("status")).toHaveTextContent("Creadas 199");
  expect(heading()).toHaveFocus();
});

it("@s38 imports into the project chosen in the selector", async () => {
  let sent: unknown = null;
  serve("/api/v1/me/connectors/github/imports", "POST", (options) => {
    sent = JSON.parse(options.body as string);
    return Response.json(
      { ...receipt, projectId: otherProjectId },
      { status: 201 },
    );
  });
  await open();

  await userEvent.selectOptions(
    await screen.findByRole("combobox"),
    otherProjectId,
  );
  await userEvent.click(importButton());

  await screen.findByText("Creadas 199");
  expect(sent).toEqual({ projectId: otherProjectId });
});

it("@s38 does not show the truncation notice when nothing was left behind", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    Response.json({ ...receipt, truncated: false }, { status: 201 }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  await screen.findByText("Creadas 199");
  expect(screen.queryByText(/más issues/i)).toBeNull();
});

// ------------------------------------------------------------------------ @s39 errores

it.each([
  [
    { code: "RATE_LIMITED", retryAfterSeconds: 90 },
    503,
    "GitHub limita las peticiones. Reintenta en 90 segundos",
  ],
  [{ code: "CONNECTION_INVALID" }, 409, "La conexión ya no es válida"],
  [{ code: "IMPORT_IN_PROGRESS" }, 409, "Hay una importación en curso"],
  [{ code: "PROJECT_COMPLETED" }, 409, "El proyecto está terminado"],
])(
  "@s39 explains %o and never retries by itself",
  async (body, status, message) => {
    serve("/api/v1/me/connectors/github/imports", "POST", () =>
      problem(status, body),
    );
    await open();
    await screen.findByRole("combobox");

    await userEvent.click(importButton());

    expect(await screen.findByText(new RegExp(message))).toBeInTheDocument();
    expect(
      callsTo("/api/v1/me/connectors/github/imports", "POST"),
    ).toHaveLength(1);
  },
);

it("@s39 offers Reconectar after an invalid connection", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(409, { code: "CONNECTION_INVALID" }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  await userEvent.click(
    await screen.findByRole("button", { name: "Reconectar" }),
  );
  expect(screen.getByLabelText(/token/i)).toBeInTheDocument();
});

it("@s39 offers Consultar estado after an import already in progress", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(409, { code: "IMPORT_IN_PROGRESS" }),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(importButton());

  await userEvent.click(
    await screen.findByRole("button", { name: "Consultar estado" }),
  );

  await waitFor(() =>
    expect(
      callsTo("/api/v1/me/connectors/github", "GET").length,
    ).toBeGreaterThan(1),
  );
});

it("@s39 keeps the selector usable after a completed project", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(409, { code: "PROJECT_COMPLETED" }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  await screen.findByText(/El proyecto está terminado/);
  expect(screen.getByRole("combobox")).toBeEnabled();
  expect(importButton()).toBeEnabled();
});

it("@s39 shows the partial counters and a link when storage failed", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(503, {
      code: "STORAGE_UNAVAILABLE",
      importId,
      created: 2,
      skipped: 1,
      failed: 0,
    }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  expect(await screen.findByText(/No se pudo completar/)).toBeInTheDocument();
  expect(screen.getByText("Creadas 2")).toBeInTheDocument();
  expect(screen.getByText("Omitidas 1")).toBeInTheDocument();
  expect(screen.getByText("Fallidas 0")).toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Primero/ })).toHaveAttribute(
    "href",
    `/proyectos/${projectId}`,
  );
  expect(importButton()).toBeEnabled();
});

// -------------------------------------------------------------------- @s40 desconectar

it("@s40 asks for confirmation, and cancelling changes nothing", async () => {
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));

  expect(callsTo("/api/v1/me/connectors/github", "DELETE")).toHaveLength(0);
  expect(screen.getByText("octocat/Hello-World")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Desconectar" })).toHaveFocus();
});

it("@s42 Escape closes the confirmation and returns the focus to Desconectar", async () => {
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  expect(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  ).toBeInTheDocument();

  await userEvent.keyboard("{Escape}");

  expect(
    screen.queryByRole("button", { name: "Confirmar desconexión" }),
  ).toBeNull();
  expect(callsTo("/api/v1/me/connectors/github", "DELETE")).toHaveLength(0);
  expect(screen.getByRole("button", { name: "Desconectar" })).toHaveFocus();
});

it("@s42 Escape works from any control inside the confirmation", async () => {
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  screen.getByRole("button", { name: "Confirmar desconexión" }).focus();

  await userEvent.keyboard("{Escape}");

  expect(
    screen.queryByRole("button", { name: "Confirmar desconexión" }),
  ).toBeNull();
  expect(callsTo("/api/v1/me/connectors/github", "DELETE")).toHaveLength(0);
  expect(screen.getByRole("button", { name: "Desconectar" })).toHaveFocus();
});

it("@s42 Escape outside the confirmation does not disturb the page", async () => {
  await open();
  await screen.findByRole("combobox");

  await userEvent.keyboard("{Escape}");

  expect(screen.getByText("Conectada")).toBeInTheDocument();
  expect(
    screen.getByRole("button", { name: "Desconectar" }),
  ).toBeInTheDocument();
});

it("@s40 sends a single DELETE when confirmed and returns to the disconnected state", async () => {
  serve(
    "/api/v1/me/connectors/github",
    "DELETE",
    () => new Response(null, { status: 204 }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  expect(await screen.findByLabelText(/token/i)).toBeInTheDocument();
  expect(callsTo("/api/v1/me/connectors/github", "DELETE")).toHaveLength(1);
  expect(heading()).toHaveFocus();
});

// ------------------------------------------------------- @s41 nada se filtra entre contextos

it("@s41 forgets the token but keeps the repository when the screen is remounted", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  const view = render(<GithubConnector owner="owner" />);
  await screen.findByLabelText(/token/i);
  await userEvent.type(
    screen.getByLabelText(/repositorio/i),
    "octocat/Hello-World",
  );
  await userEvent.type(screen.getByLabelText(/token/i), "ghp_secreto123");

  view.unmount();
  render(<GithubConnector owner="owner" />);

  expect(await screen.findByLabelText(/token/i)).toHaveValue("");
  expect(document.body.innerHTML).not.toContain("ghp_secreto123");
});

it("@s41 starts from scratch when another person signs in", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({ ...connection, lastImport: receipt }),
  );
  const view = render(<GithubConnector owner="owner" />);
  await screen.findByText("Creadas 199");

  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  view.rerender(<GithubConnector owner="otra-persona" />);

  expect(await screen.findByLabelText(/token/i)).toBeInTheDocument();
  expect(screen.queryByText("Creadas 199")).toBeNull();
  expect(screen.queryByText("octocat/Hello-World")).toBeNull();
});

it("@s41 ignores the late answer of an import started before unmounting", async () => {
  let release: (value: Response) => void = () => {};
  serve(
    "/api/v1/me/connectors/github/imports",
    "POST",
    () => new Promise<Response>((resolve) => (release = resolve)),
  );
  const view = render(<GithubConnector owner="owner" />);
  await screen.findByRole("combobox");
  await userEvent.click(importButton());

  view.unmount();
  release(Response.json(receipt, { status: 201 }));

  await new Promise((resolve) => setTimeout(resolve, 0));
  expect(screen.queryByText("Creadas 199")).toBeNull();
});

// ------------------------------- @s40 la desconexión que falla: la rama que nadie había ejercido

/**
 * El catch de disconnect() no lo ejecutaba ninguna prueba: la campaña de
 * mutación lo marcó como «sin cobertura», no como oráculo débil. Aquí se
 * ejerce entero, con las dos ramas de su ternario.
 */
it("@s40 announces a failed disconnection and keeps the connection on screen", async () => {
  serve("/api/v1/me/connectors/github", "DELETE", () =>
    problem(409, { code: "CONNECTION_NOT_FOUND" }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  expect(await screen.findByText("La conexión ya no existe")).toBeVisible();
  // Nada se limpió: la desconexión no llegó a ocurrir.
  expect(
    screen.getByRole("button", { name: "Desconectar" }),
  ).toBeInTheDocument();
  expect(screen.queryByLabelText(/token/i)).toBeNull();
});

it("@s40 a failure that is not a typed connector error falls back to the generic message", async () => {
  serve("/api/v1/me/connectors/github", "DELETE", () => {
    // Un fallo del transporte, con un code que NO debe leerse: no es un
    // ConnectorError, así que la pantalla ha de rehacerlo como genérico.
    throw Object.assign(new Error("transporte"), {
      code: "CONNECTION_NOT_FOUND",
    });
  });
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  expect(
    await screen.findByText("No se pudo importar. Inténtalo más tarde"),
  ).toBeVisible();
  expect(screen.queryByText("La conexión ya no existe")).toBeNull();
});

// ================= oráculos de la puerta de mutación de `github-connector.tsx`
//
// Nada de lo que sigue añade conducta: afirma la que ya existe y que ninguna
// prueba miraba. Cada bloque nombra el racimo de supervivientes que mata, con
// su línea en producción.

// ------------------- El escuchador de Escape (153-160), que el juez exigió

it("@s42 no escucha Escape mientras no hay ninguna confirmación abierta", async () => {
  await open();
  const select = await screen.findByRole("combobox");
  importButton().focus();

  await userEvent.keyboard("{Escape}");
  // Un repintado cualquiera delata el movimiento de foco que el manejador
  // hubiera dejado pedido.
  await userEvent.selectOptions(select, otherProjectId);

  expect(select).toHaveFocus();
  expect(screen.getByRole("button", { name: "Desconectar" })).not.toHaveFocus();
});

it("@s42 retira el escuchador de Escape al cerrarse la confirmación", async () => {
  await open();
  const select = await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.keyboard("{Escape}");
  expect(screen.getByRole("button", { name: "Desconectar" })).toHaveFocus();

  importButton().focus();
  await userEvent.keyboard("{Escape}");
  await userEvent.selectOptions(select, otherProjectId);

  // Si el escuchador siguiera puesto, esta segunda pulsación habría reclamado
  // el foco para el botón de desconectar.
  expect(select).toHaveFocus();
});

it("@s42 sólo Escape cierra la confirmación: otra tecla no la toca", async () => {
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));

  await userEvent.keyboard("a");

  expect(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  ).toBeInTheDocument();
});

it("@s42 el Escape que cierra la confirmación queda consumido", async () => {
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));

  const escape = new KeyboardEvent("keydown", {
    key: "Escape",
    bubbles: true,
    cancelable: true,
  });
  fireEvent(document, escape);

  expect(escape.defaultPrevented).toBe(true);
  expect(
    screen.queryByRole("button", { name: "Confirmar desconexión" }),
  ).toBeNull();
});

// ------- La tabla de mensajes (37-47) y la región de error de importación (416-424)

it.each([
  ["GITHUB_UNAVAILABLE", 503, "GitHub no responde. Inténtalo más tarde"],
  [
    "GITHUB_REPOSITORY_UNAVAILABLE",
    404,
    "El repositorio no está disponible con ese token",
  ],
  ["CONNECTION_NOT_FOUND", 404, "La conexión ya no existe"],
  ["RESOURCE_NOT_FOUND", 404, "El proyecto ya no está disponible"],
  ["VALIDATION_ERROR", 400, "Revisa el repositorio y el token"],
])("@s39 traduce %s a su mensaje exacto", async (code, status, message) => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(status as number, { code }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  expect(await screen.findByText(message as string)).toBeVisible();
});

it("@s39 un token rechazado al importar no se repite en la región de errores", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(401, { code: "GITHUB_TOKEN_REJECTED" }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  await waitFor(() =>
    expect(
      callsTo("/api/v1/me/connectors/github/imports", "POST"),
    ).toHaveLength(1),
  );
  expect(screen.queryByText("GitHub rechazó el token")).toBeNull();
});

it("@s39 sólo una conexión inválida ofrece Reconectar, y sólo una importación en curso Consultar estado", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(503, { code: "GITHUB_UNAVAILABLE" }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  await screen.findByText("GitHub no responde. Inténtalo más tarde");
  expect(screen.queryByRole("button", { name: "Reconectar" })).toBeNull();
  expect(screen.queryByRole("button", { name: "Consultar estado" })).toBeNull();
});

it("@s39 no vuelve a ofrecer Reconectar cuando el formulario ya está abierto", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(409, { code: "CONNECTION_INVALID" }),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(importButton());

  await userEvent.click(
    await screen.findByRole("button", { name: "Reconectar" }),
  );

  expect(screen.getByLabelText(/token/i)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Reconectar" })).toBeNull();
});

// ---- El hueco del mensaje del formulario (405-407) y la región aria-live (456)

it("@s37 el token rechazado se anuncia como alerta en el hueco de su campo", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  serve("/api/v1/me/connectors/github", "PUT", () =>
    problem(401, { code: "GITHUB_TOKEN_REJECTED" }),
  );
  await open();
  await userEvent.type(
    await screen.findByLabelText(/repositorio/i),
    "octocat/Hello-World",
  );
  await userEvent.type(screen.getByLabelText(/token/i), "ghp_malo");

  await userEvent.click(screen.getByRole("button", { name: "Conectar" }));

  const alert = await screen.findByRole("alert");
  expect(alert).toHaveTextContent("GitHub rechazó el token");
  expect(alert).toHaveAttribute("id", "github-token-error");
});

it("@s37 sin error, el hueco del mensaje está vacío y no es una alerta", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  await open();
  await screen.findByLabelText(/token/i);

  const slot = document.getElementById("github-token-error");
  expect(slot?.textContent).toBe("");
  expect(slot).not.toHaveAttribute("role");
});

it("@s42 la región de estado nace callada: no anuncia nada sin importación", async () => {
  await open();
  await screen.findByRole("combobox");

  expect(screen.getByRole("status").textContent).toBe("");
});

// --- El resumen heredado del último recibo (474) y el nombre de su enlace (478)

it("@s36 una importación en curso no adelanta contadores a medias", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({
      ...connection,
      lastImport: {
        ...receipt,
        status: "running",
        errorCode: null,
        finishedAt: null,
      },
    }),
  );
  await open();

  expect(await screen.findByText("Hay una importación en curso")).toBeVisible();
  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
  expect(screen.queryByText("Creadas 199")).toBeNull();
});

it("@s36 el enlace del resumen nombra el proyecto del recibo, no el primero de la lista", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({
      ...connection,
      lastImport: { ...receipt, projectId: otherProjectId },
    }),
  );
  await open();

  expect(
    await screen.findByRole("link", { name: "Ver el proyecto Segundo" }),
  ).toHaveAttribute("href", `/proyectos/${otherProjectId}`);
});

it("@s36 un recibo de un proyecto que ya no está en la lista enseña su identificador", async () => {
  const gone = "44444444-5555-4666-8777-888888888888";
  serve("/api/v1/me/connectors/github", "GET", () =>
    Response.json({
      ...connection,
      lastImport: { ...receipt, projectId: gone },
    }),
  );
  await open();

  expect(
    await screen.findByRole("link", { name: `Ver el proyecto ${gone}` }),
  ).toHaveAttribute("href", `/proyectos/${gone}`);
});

// ------------- El arranque de la pantalla (60, 66, 80, 132, 284-285)

it("@s42 el contenido principal y su encabezado son enfocables por programa", async () => {
  await open();

  expect(heading()).toHaveAttribute("tabindex", "-1");
  expect(document.querySelector("main")).toHaveAttribute("tabindex", "-1");
});

it("@s42 el foco arranca en el encabezado y ningún botón se lo lleva", async () => {
  await open();
  await screen.findByRole("combobox");

  expect(heading()).toHaveFocus();
});

it("@s36 mientras se consulta la conexión no se enseña el formulario", async () => {
  serve(
    "/api/v1/me/connectors/github",
    "GET",
    () => new Promise<Response>(() => {}),
  );
  render(<GithubConnector owner="owner" />);
  await screen.findByRole("heading", { level: 1 });

  expect(screen.queryByLabelText(/token/i)).toBeNull();
  expect(screen.queryByRole("button", { name: "Conectar" })).toBeNull();
});

it("@s36 sin lista de proyectos el selector no ofrece ninguna opción", async () => {
  serve("/api/v1/projects", "GET", () =>
    problem(503, { code: "STORAGE_UNAVAILABLE" }),
  );
  await open();

  await screen.findByText("Conectada");
  expect(
    within(screen.getByRole("combobox")).queryAllByRole("option"),
  ).toHaveLength(0);
});

it("@s36 el conector deshabilitado deja de pedir la lista de proyectos", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(503, { code: "CONNECTORS_DISABLED" }),
  );
  await open();
  await screen.findByRole("alert");

  // Una sola petición, la del montaje: al saberse deshabilitado no vuelve a pedirla.
  await waitFor(() =>
    expect(callsTo("/api/v1/projects", "GET").length).toBeGreaterThan(0),
  );
  expect(callsTo("/api/v1/projects", "GET")).toHaveLength(1);
});

// ------------ La importación: sus guardas y lo que limpia al empezar (207-236)

it("@s38 sin proyecto que elegir, el botón de importar no lanza ninguna importación", async () => {
  serve("/api/v1/projects", "GET", () =>
    Response.json({ items: [], nextCursor: null }),
  );
  await open();
  await screen.findByText("Conectada");

  await userEvent.click(importButton());

  expect(callsTo("/api/v1/me/connectors/github/imports", "POST")).toHaveLength(
    0,
  );
  expect(screen.getByRole("status").textContent).toBe("");
});

it("@s37 tras un token rechazado el formulario vuelve a estar operativo", async () => {
  serve("/api/v1/me/connectors/github", "GET", () =>
    problem(404, { code: "CONNECTION_NOT_FOUND" }),
  );
  serve("/api/v1/me/connectors/github", "PUT", () =>
    problem(401, { code: "GITHUB_TOKEN_REJECTED" }),
  );
  await open();
  await userEvent.type(
    await screen.findByLabelText(/repositorio/i),
    "octocat/Hello-World",
  );
  await userEvent.type(screen.getByLabelText(/token/i), "ghp_malo");

  await userEvent.click(screen.getByRole("button", { name: "Conectar" }));

  await screen.findByText("GitHub rechazó el token");
  expect(screen.getByRole("button", { name: "Conectar" })).toBeEnabled();
  expect(screen.getByLabelText(/repositorio/i)).not.toHaveAttribute("readonly");
  expect(screen.getByLabelText(/token/i)).not.toHaveAttribute("readonly");
});

it("@s38 una importación nueva borra el resultado de la anterior antes de fallar", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    Response.json(receipt, { status: 201 }),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(importButton());
  await screen.findByText("Creadas 199");

  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(503, { code: "GITHUB_UNAVAILABLE" }),
  );
  await userEvent.click(importButton());

  await screen.findByText("GitHub no responde. Inténtalo más tarde");
  expect(screen.queryByText("Creadas 199")).toBeNull();
  // Sólo un fallo de almacenamiento deja contadores parciales; éste no lo es.
  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
});

it("@s38 el error de una importación desaparece cuando la siguiente sale bien", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(503, { code: "GITHUB_UNAVAILABLE" }),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(importButton());
  await screen.findByText("GitHub no responde. Inténtalo más tarde");

  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    Response.json(receipt, { status: 201 }),
  );
  await userEvent.click(importButton());

  await screen.findByText("Creadas 199");
  expect(
    screen.queryByText("GitHub no responde. Inténtalo más tarde"),
  ).toBeNull();
});

it("@s39 un fallo de almacenamiento sin contadores los da por cero y no habla de truncamiento", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(503, { code: "STORAGE_UNAVAILABLE", importId }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  expect(await screen.findByText("Creadas 0")).toBeVisible();
  expect(screen.getByText("Omitidas 0")).toBeVisible();
  expect(screen.getByText("Fallidas 0")).toBeVisible();
  expect(screen.queryByText(/más issues/i)).toBeNull();
});

// --- La desconexión: lo que limpia (260-261) y a quién obedece (258, 266-271)

it("@s40 desconectar borra el resumen de la importación de esta sesión", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    Response.json(receipt, { status: 201 }),
  );
  serve(
    "/api/v1/me/connectors/github",
    "DELETE",
    () => new Response(null, { status: 204 }),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(importButton());
  await screen.findByText("Creadas 199");

  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  expect(await screen.findByLabelText(/token/i)).toBeInTheDocument();
  expect(screen.queryByText("Creadas 199")).toBeNull();
});

it("@s40 desconectar borra el error de la importación anterior", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(503, { code: "GITHUB_UNAVAILABLE" }),
  );
  serve(
    "/api/v1/me/connectors/github",
    "DELETE",
    () => new Response(null, { status: 204 }),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(importButton());
  await screen.findByText("GitHub no responde. Inténtalo más tarde");

  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  expect(await screen.findByLabelText(/token/i)).toBeInTheDocument();
  expect(
    screen.queryByText("GitHub no responde. Inténtalo más tarde"),
  ).toBeNull();
});

it("@s41 una desconexión relevada que falla tarde no habla ni estorba a la importación viva", async () => {
  let rejectDelete: (reason: unknown) => void = () => {};
  let releaseImport: (value: Response) => void = () => {};
  serve(
    "/api/v1/me/connectors/github",
    "DELETE",
    () => new Promise<Response>((_, reject) => (rejectDelete = reject)),
  );
  serve(
    "/api/v1/me/connectors/github/imports",
    "POST",
    () => new Promise<Response>((resolve) => (releaseImport = resolve)),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  // La importación releva a la desconexión: pending.current deja de ser su controlador.
  await userEvent.click(importButton());
  rejectDelete(new Error("el transporte se cayó tarde"));
  await new Promise((resolve) => setTimeout(resolve, 0));
  releaseImport(Response.json(receipt, { status: 201 }));

  // La operación viva llega entera: la relevada no le ha robado el turno.
  expect(await screen.findByText("Creadas 199")).toBeVisible();
  // Y la relevada calla: su fallo ya no le importa a nadie.
  expect(
    screen.queryByText("No se pudo importar. Inténtalo más tarde"),
  ).toBeNull();
});

it("@s41 una desconexión relevada que termina bien no borra la conexión de la pantalla", async () => {
  let releaseDelete: (value: Response) => void = () => {};
  let releaseImport: (value: Response) => void = () => {};
  serve(
    "/api/v1/me/connectors/github",
    "DELETE",
    () => new Promise<Response>((resolve) => (releaseDelete = resolve)),
  );
  serve(
    "/api/v1/me/connectors/github/imports",
    "POST",
    () => new Promise<Response>((resolve) => (releaseImport = resolve)),
  );
  await open();
  await screen.findByRole("combobox");
  await userEvent.click(screen.getByRole("button", { name: "Desconectar" }));
  await userEvent.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );
  await userEvent.click(importButton());

  releaseDelete(new Response(null, { status: 204 }));
  await new Promise((resolve) => setTimeout(resolve, 0));
  releaseImport(Response.json(receipt, { status: 201 }));

  expect(await screen.findByText("Creadas 199")).toBeVisible();
  expect(screen.getByText("octocat/Hello-World")).toBeInTheDocument();
  expect(screen.queryByLabelText(/token/i)).toBeNull();
});
