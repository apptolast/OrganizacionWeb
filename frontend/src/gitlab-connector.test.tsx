import { afterEach, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { setCsrfToken } from "./api-client";
import { GitlabConnector } from "./gitlab-connector";

const TOKEN = "glpat-SECRETOSECRETO1234";

const connected = {
  status: "connected",
  apiBase: "https://gitlab.example.com/api/v4",
  projectPath: "grupo/proyecto",
  projectId: 4821,
  tokenHint: "WXYZ",
  lastActivityAt: "2026-09-09T10:00:00.123456Z",
  lastError: null,
  version: 1,
};

const notConnected = {
  status: "not_connected",
  apiBase: null,
  projectPath: null,
  projectId: null,
  tokenHint: null,
  lastActivityAt: null,
  lastError: null,
  version: null,
};

function problem(status: number, body: Record<string, unknown>) {
  return Response.json(body, {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

function stub(...responses: Response[]) {
  const fetcher = vi.fn();
  for (const response of responses) fetcher.mockResolvedValueOnce(response);
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}

const tokenField = () => screen.getByLabelText(/Token de acceso personal/);
const pathField = () => screen.getByLabelText(/Ruta del proyecto/);
const connectButton = () => screen.getByRole("button", { name: "Conectar" });

async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>) {
  await user.type(tokenField(), TOKEN);
  await user.type(pathField(), "grupo/proyecto");
  await user.click(connectButton());
}

afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
});

// ------------------------------------------------------------- @s34 el formulario

it("@s34 offers a token field that is a password, never autofilled and never prefilled", async () => {
  stub(Response.json(notConnected));

  render(<GitlabConnector owner="owner" />);

  const field = await screen.findByLabelText(/Token de acceso personal/);
  expect(field.getAttribute("type")).toBe("password");
  expect(field.getAttribute("autocomplete")).toBe("off");
  expect((field as HTMLInputElement).value).toBe("");
});

it("@s34 shows the shape of the project path and asks for a read_api token", async () => {
  stub(Response.json(notConnected));

  render(<GitlabConnector owner="owner" />);

  await screen.findByLabelText(/Ruta del proyecto/);
  expect(screen.getByText(/grupo\/proyecto/)).toBeTruthy();
  expect(screen.getByText(/read_api/)).toBeTruthy();
});

it("@s34 does not announce success before there is any", async () => {
  stub(Response.json(notConnected));

  render(<GitlabConnector owner="owner" />);

  await screen.findByLabelText(/Token de acceso personal/);
  expect(screen.queryByText("Conectado")).toBeNull();
});

// ---------------------------------------------------- @s34 mientras la petición viaja

it("@s34 disables the button and says «Guardando…» while the request is in flight", async () => {
  const user = userEvent.setup();
  let settle: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(notConnected));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settle = resolve;
    }),
  );
  vi.stubGlobal("fetch", fetcher);

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(connectButton()).toBeDisabled());
  const notice = screen.getByRole("status");
  expect(notice.getAttribute("aria-live")).toBe("polite");
  expect(notice.textContent).toContain("Guardando…");

  settle(Response.json(connected));
  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
});

// ------------------------------------------------------- @s34 sólo tras el 200

it("@s34 shows «Conectado», the masked hint and the project only after the 200", async () => {
  const user = userEvent.setup();
  stub(Response.json(notConnected), Response.json(connected));

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
  expect(screen.getByText("••••WXYZ")).toBeTruthy();
  expect(screen.getByText("grupo/proyecto")).toBeTruthy();
});

it("@s34 empties the token field once the connection is confirmed", async () => {
  const user = userEvent.setup();
  stub(Response.json(notConnected), Response.json(connected));

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
  expect(screen.queryByDisplayValue(TOKEN)).toBeNull();
});

// ------------------------------------------------------------ @s34 cuando falla

it("@s34 shows the translated code, keeps the path and says nothing about being connected", async () => {
  const user = userEvent.setup();
  stub(
    Response.json(notConnected),
    problem(409, { code: "CONNECTION_INVALID" }),
  );

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  const alert = await screen.findByRole("alert");
  expect(alert.textContent).toContain("La conexión ya no es válida");
  expect(alert.textContent).not.toContain("CONNECTION_INVALID");
  expect((pathField() as HTMLInputElement).value).toBe("grupo/proyecto");
  expect(screen.queryByText("Conectado")).toBeNull();
});

it("@s34 marks the field the server complained about", async () => {
  const user = userEvent.setup();
  stub(
    Response.json(notConnected),
    problem(400, {
      code: "VALIDATION_ERROR",
      errors: [{ field: "projectPath", code: "INVALID_FORMAT" }],
    }),
  );

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() =>
    expect(pathField().getAttribute("aria-invalid")).toBe("true"),
  );
  expect(tokenField().getAttribute("aria-invalid")).not.toBe("true");
});

// ------------------------------------------------------------------------ @s32

it("@s32 never leaves the token in localStorage, sessionStorage or cookies", async () => {
  const user = userEvent.setup();
  stub(Response.json(notConnected), Response.json(connected));

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
  expect(JSON.stringify(window.localStorage)).not.toContain(TOKEN);
  expect(JSON.stringify(window.sessionStorage)).not.toContain(TOKEN);
  expect(document.cookie).not.toContain(TOKEN);
  expect(document.body.innerHTML).not.toContain(TOKEN);
});

it("@s32 does not paint the token even while the request is still travelling", async () => {
  const user = userEvent.setup();
  let settle: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(notConnected));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settle = resolve;
    }),
  );
  vi.stubGlobal("fetch", fetcher);

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(connectButton()).toBeDisabled());
  // El único sitio del documento donde el token puede estar es el campo que lo recoge.
  const holders = [...document.querySelectorAll("*")].filter((node) =>
    node.outerHTML.includes(TOKEN),
  );
  expect(holders.at(-1)).toBe(tokenField());
  expect(document.body.textContent).not.toContain(TOKEN);

  settle(Response.json(connected));
  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
});

// ------------------------------------------------------------------------ @s29

it("@s29 without the server key it explains the missing configuration and offers no form", async () => {
  stub(problem(503, { code: "CONNECTORS_DISABLED" }));

  render(<GitlabConnector owner="owner" />);

  const alert = await screen.findByRole("alert");
  expect(alert.textContent).toContain("configuración del servidor");
  expect(screen.queryByLabelText(/Token de acceso personal/)).toBeNull();
  expect(screen.queryByRole("button")).toBeNull();
});
