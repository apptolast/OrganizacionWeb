import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setCsrfToken } from "./api-client";
import { GithubConnector } from "./github-connector";

const projectId = "11111111-2222-4333-8444-555555555555";
const otherProjectId = "22222222-3333-4444-8555-666666666666";
const doneProjectId = "33333333-4444-4555-8666-777777777777";
const importId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

const receipt = {
  id: importId,
  projectId,
  repository: "octocat/Hello-World",
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
  return calls.filter((call) => call.url.split("?")[0] === url && call.method === method);
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
  expect(screen.getByRole("button", { name: "Desconectar" })).toBeInTheDocument();
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

  await userEvent.type(screen.getByLabelText(/repositorio/i), "octocat/Hello-World");
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
  expect(screen.getByLabelText(/repositorio/i)).toHaveValue("octocat/Hello-World");
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

  const options = within(await screen.findByRole("combobox")).getAllByRole("option");
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
    return Response.json({ ...receipt, projectId: otherProjectId }, { status: 201 });
  });
  await open();

  await userEvent.selectOptions(await screen.findByRole("combobox"), otherProjectId);
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
])("@s39 explains %o and never retries by itself", async (body, status, message) => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(status, body),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  expect(await screen.findByText(new RegExp(message))).toBeInTheDocument();
  expect(callsTo("/api/v1/me/connectors/github/imports", "POST")).toHaveLength(1);
});

it("@s39 offers Reconectar after an invalid connection", async () => {
  serve("/api/v1/me/connectors/github/imports", "POST", () =>
    problem(409, { code: "CONNECTION_INVALID" }),
  );
  await open();
  await screen.findByRole("combobox");

  await userEvent.click(importButton());

  await userEvent.click(await screen.findByRole("button", { name: "Reconectar" }));
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
    expect(callsTo("/api/v1/me/connectors/github", "GET").length).toBeGreaterThan(1),
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

it("@s40 sends a single DELETE when confirmed and returns to the disconnected state", async () => {
  serve("/api/v1/me/connectors/github", "DELETE", () => new Response(null, { status: 204 }));
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
