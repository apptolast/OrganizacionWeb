import { afterEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { observeAccess, setCsrfToken } from "./api-client";
import { ConnectorsCatalog } from "./connectors-catalog";
import { CONNECTOR_ORDER } from "./connectors-catalog-client";
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
  observeAccess();
});

// ------------------------------------------------------------- @s34 el formulario

it("@s34 offers a token field that is a password, never autofilled and never prefilled", async () => {
  stub(Response.json(notConnected), Response.json(projects));

  render(<GitlabConnector owner="owner" />);

  const field = await screen.findByLabelText(/Token de acceso personal/);
  expect(field.getAttribute("type")).toBe("password");
  expect(field.getAttribute("autocomplete")).toBe("off");
  expect((field as HTMLInputElement).value).toBe("");
});

it("@s34 shows the shape of the project path and asks for a read_api token", async () => {
  stub(Response.json(notConnected), Response.json(projects));

  render(<GitlabConnector owner="owner" />);

  await screen.findByLabelText(/Ruta del proyecto/);
  expect(screen.getByText(/grupo\/proyecto/)).toBeTruthy();
  expect(screen.getByText(/read_api/)).toBeTruthy();
});

it("@s34 does not announce success before there is any", async () => {
  stub(Response.json(notConnected), Response.json(projects));

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
  fetcher.mockResolvedValueOnce(Response.json(projects));
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
  stub(
    Response.json(notConnected),
    Response.json(projects),
    Response.json(connected),
  );

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
  expect(screen.getByText("••••WXYZ")).toBeTruthy();
  expect(screen.getByText("grupo/proyecto")).toBeTruthy();
});

it("@s34 empties the token field once the connection is confirmed", async () => {
  const user = userEvent.setup();
  stub(
    Response.json(notConnected),
    Response.json(projects),
    Response.json(connected),
  );

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
    Response.json(projects),
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
    Response.json(projects),
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
  stub(
    Response.json(notConnected),
    Response.json(projects),
    Response.json(connected),
  );

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
  fetcher.mockResolvedValueOnce(Response.json(projects));
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
  // El token vive sólo en la propiedad value del campo: no está en el HTML serializado
  // de ningún elemento, así que ningún volcado del documento puede publicarlo.
  expect((tokenField() as HTMLInputElement).value).toBe(TOKEN);
  expect(
    [...document.querySelectorAll("*")].some((node) =>
      node.outerHTML.includes(TOKEN),
    ),
  ).toBe(false);
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

// ============================================================ @s35 con conexión existente

const projectId = "11111111-2222-4333-8444-555555555555";
const otherProjectId = "22222222-3333-4444-8555-666666666666";
const doneProjectId = "33333333-4444-4555-8666-777777777777";
const importId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

function summary(id: string, name: string, status: string) {
  return {
    id,
    name,
    status,
    createdAt: "2026-09-01T08:00:00.000000Z",
    updatedAt: "2026-09-01T08:00:00.000000Z",
  };
}

const projects = {
  items: [
    summary(projectId, "Primero", "idea"),
    summary(otherProjectId, "Segundo", "active"),
    summary(doneProjectId, "Terminado", "completed"),
  ],
  nextCursor: null,
};

function receipt(overrides: Record<string, unknown> = {}) {
  return {
    id: importId,
    source: "gitlab",
    projectId,
    projectPath: "grupo/proyecto",
    status: "completed",
    created: 7,
    skipped: 2,
    failed: 1,
    truncated: false,
    errorCode: null,
    startedAt: "2026-09-09T12:00:00.123456Z",
    finishedAt: "2026-09-09T12:00:04.123456Z",
    ...overrides,
  };
}

/** La pantalla arranca pidiendo la conexión y la lista de proyectos, en ese orden. */
function openConnected(...rest: Response[]) {
  return stub(Response.json(connected), Response.json(projects), ...rest);
}

async function renderConnected() {
  render(<GitlabConnector owner="owner" />);
  await screen.findByRole("button", { name: "Importar issues" });
}

it("@s35 shows the hint, the path and the identifier next to the three buttons", async () => {
  openConnected();

  await renderConnected();

  expect(screen.getByText("••••WXYZ")).toBeTruthy();
  expect(screen.getByText("grupo/proyecto")).toBeTruthy();
  expect(screen.getByText("4821")).toBeTruthy();
  for (const name of ["Importar issues", "Actualizar token", "Desconectar"])
    expect(screen.getByRole("button", { name })).toBeTruthy();
});

/*
 * @s5 @s8: hasta aqui todos los fixtures del carril traian lastError null, asi que el lastError que
 * el servidor si publica no llegaba nunca al decodificador desde la pantalla. El conector no pinta
 * el codigo —eso es la fila del catalogo, @s33— pero si tiene que aceptarlo y decir «Error», y
 * sobre todo no sacar por pantalla nada que venga del proveedor.
 */
it("@s5 accepts a connection whose lastError comes from the server and says Error", async () => {
  stub(
    Response.json({
      ...connected,
      status: "error",
      lastError: {
        code: "CONNECTION_INVALID",
        at: "2026-09-09T10:00:00.123456Z",
      },
    }),
    Response.json(projects),
  );

  render(<GitlabConnector owner="owner" />);

  expect(await screen.findByText("Error")).toBeTruthy();
  expect(screen.getByText("••••WXYZ")).toBeTruthy();
  expect(document.body.textContent).not.toContain(TOKEN);
  expect(document.body.textContent).not.toContain("glpat");
});

it("@s5 refuses a lastError that carries anything beyond a code and an instant", async () => {
  stub(
    Response.json({
      ...connected,
      status: "error",
      lastError: {
        code: "CONNECTION_INVALID",
        at: "2026-09-09T10:00:00.123456Z",
        detail: "invalid_token: glpat-abcdef1234",
      },
    }),
    Response.json(projects),
  );

  render(<GitlabConnector owner="owner" />);

  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Importar issues" }),
    ).toBeNull(),
  );
  expect(document.body.textContent).not.toContain("glpat");
  expect(document.body.textContent).not.toContain("invalid_token");
});

it("@s35 lists only the open projects in the native selector", async () => {
  openConnected();

  await renderConnected();

  const options = screen.getAllByRole("option");
  expect(options.map((option) => option.textContent)).toEqual([
    "Primero",
    "Segundo",
  ]);
});

it("@s35 announces progress without a percentage while the import travels", async () => {
  const user = userEvent.setup();
  let settle: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(connected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settle = resolve;
    }),
  );
  vi.stubGlobal("fetch", fetcher);

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Importar issues" }));

  await waitFor(() =>
    expect(
      screen.getByRole("button", { name: "Importar issues" }),
    ).toBeDisabled(),
  );
  const notice = screen.getByRole("status");
  expect(notice.textContent).toContain("Importando");
  expect(notice.textContent).not.toMatch(/\d+\s*%/);

  settle(Response.json(receipt(), { status: 201 }));
  await screen.findByText("Creadas");
});

it("@s35 presents the receipt as four labelled figures", async () => {
  const user = userEvent.setup();
  openConnected(Response.json(receipt(), { status: 201 }));

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Importar issues" }));

  const summaryBox = await screen.findByRole("region", {
    name: "Resultado de la importación",
  });
  for (const [label, value] of [
    ["Creadas", "7"],
    ["Omitidas", "2"],
    ["Fallidas", "1"],
    ["Truncado", "No"],
  ]) {
    expect(within(summaryBox).getByText(label)).toBeTruthy();
    expect(within(summaryBox).getByText(value)).toBeTruthy();
  }
});

it("@s35 warns visibly when the import came back truncated", async () => {
  const user = userEvent.setup();
  openConnected(Response.json(receipt({ truncated: true }), { status: 201 }));

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Importar issues" }));

  const summaryBox = await screen.findByRole("region", {
    name: "Resultado de la importación",
  });
  expect(within(summaryBox).getByText("Sí")).toBeTruthy();
  expect(screen.getByText(/quedaron issues sin traer/i)).toBeTruthy();
});

it("@s35 asks for an explicit confirmation that promises the imported tasks survive", async () => {
  const user = userEvent.setup();
  openConnected();

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Desconectar" }));

  const confirmation = screen.getByRole("group", {
    name: "Confirmar desconexión",
  });
  expect(within(confirmation).getByText(/se conservan/i)).toBeTruthy();
  expect(screen.getByText("••••WXYZ")).toBeTruthy();
});

it("@s35 only goes back to the disconnected state after the 204", async () => {
  const user = userEvent.setup();
  openConnected(new Response(null, { status: 204 }));

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Desconectar" }));
  await user.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  await screen.findByLabelText(/Token de acceso personal/);
  expect(screen.queryByText("••••WXYZ")).toBeNull();
  expect(screen.queryByRole("button", { name: "Importar issues" })).toBeNull();
});

it("@s35 keeps the connection when the disconnection fails", async () => {
  const user = userEvent.setup();
  openConnected(problem(503, { code: "STORAGE_UNAVAILABLE" }));

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Desconectar" }));
  await user.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  await screen.findByRole("alert");
  expect(screen.getByText("••••WXYZ")).toBeTruthy();
});

it("@s35 opens the form with an empty token field when the token is replaced", async () => {
  const user = userEvent.setup();
  openConnected();

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Actualizar token" }));

  expect((tokenField() as HTMLInputElement).value).toBe("");
  expect((pathField() as HTMLInputElement).value).toBe("grupo/proyecto");
});

it("@s35 leaves no trace of a previous token in the form it reopens", async () => {
  const user = userEvent.setup();
  stub(
    Response.json(notConnected),
    Response.json(projects),
    Response.json(connected),
    Response.json(projects),
  );

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);
  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
  await user.click(screen.getByRole("button", { name: "Actualizar token" }));

  expect((tokenField() as HTMLInputElement).value).toBe("");
});

// ================================================== @s36 los errores y su acción

const importButton = () =>
  screen.getByRole("button", { name: "Importar issues" });

it("@s36 names the seconds of a rate limit and leaves the button usable again", async () => {
  const user = userEvent.setup();
  openConnected(problem(503, { code: "RATE_LIMITED", retryAfterSeconds: 30 }));

  await renderConnected();
  await user.click(importButton());

  const alert = await screen.findByRole("alert");
  expect(alert.textContent).toContain("30 segundos");
  expect(importButton()).not.toBeDisabled();
  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
});

it("@s36 turns the state into «Error» and suggests replacing the token, with the focus on it", async () => {
  const user = userEvent.setup();
  openConnected(problem(409, { code: "CONNECTION_INVALID" }));

  await renderConnected();
  await user.click(importButton());

  await waitFor(() => expect(screen.getByText("Error")).toBeTruthy());
  const suggested = screen.getByRole("button", { name: "Actualizar token" });
  expect(document.activeElement).toBe(suggested);
  expect(screen.queryByText("Conectado")).toBeNull();
});

it("@s36 offers «Actualizar estado» for an import already running, and it rereads both", async () => {
  const user = userEvent.setup();
  const fetcher = openConnected(
    problem(409, { code: "IMPORT_IN_PROGRESS" }),
    Response.json(connected),
    Response.json({ connectors: [] }),
  );

  await renderConnected();
  await user.click(importButton());

  const alert = await screen.findByRole("alert");
  expect(alert.textContent).toContain("importación en curso");
  await user.click(screen.getByRole("button", { name: "Actualizar estado" }));

  await waitFor(() => {
    const asked = fetcher.mock.calls.map((call) => String(call[0]));
    expect(asked).toContain("/api/v1/me/connectors/gitlab");
    expect(asked).toContain("/api/v1/me/connectors");
  });
});

it("@s36 warns that the provider is unavailable and keeps the path for a manual retry", async () => {
  const user = userEvent.setup();
  stub(
    Response.json(notConnected),
    Response.json(projects),
    problem(503, { code: "GITLAB_UNAVAILABLE" }),
  );

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  const alert = await screen.findByRole("alert");
  expect(alert.textContent).toContain("GitLab no responde");
  expect((pathField() as HTMLInputElement).value).toBe("grupo/proyecto");
  expect(connectButton()).not.toBeDisabled();
  expect(screen.queryByText("Conectado")).toBeNull();
});

it("@s36 after a network failure offers «Actualizar estado» and retries nothing on its own", async () => {
  const user = userEvent.setup();
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(connected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  fetcher.mockRejectedValueOnce(new TypeError("Failed to fetch"));
  vi.stubGlobal("fetch", fetcher);

  await renderConnected();
  await user.click(importButton());

  await screen.findByRole("button", { name: "Actualizar estado" });
  const asked = fetcher.mock.calls.length;
  await new Promise((resolve) => setTimeout(resolve, 60));
  expect(fetcher.mock.calls.length).toBe(asked);
  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
});

// ============================== @s37 cancelación, cierre de sesión y respuestas tardías

it("@s37 leaving for the catalogue drops the import: the list loads on its own and shows no receipt", async () => {
  const user = userEvent.setup();
  let settle: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(connected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settle = resolve;
    }),
  );
  fetcher.mockResolvedValueOnce(
    Response.json({
      connectors: CONNECTOR_ORDER.map((id) => ({
        id,
        status: "not_connected",
        lastActivityAt: null,
        lastError: null,
      })),
    }),
  );
  vi.stubGlobal("fetch", fetcher);

  const view = render(<GitlabConnector owner="owner" />);
  await screen.findByRole("button", { name: "Importar issues" });
  await user.click(screen.getByRole("button", { name: "Importar issues" }));
  const inFlight = (fetcher.mock.calls[2][1] as RequestInit).signal!;
  view.unmount();

  // Salir cancela de verdad: la petición deja de estar en vuelo, no se ignora al volver.
  expect(inFlight.aborted).toBe(true);
  render(<ConnectorsCatalog />);
  await waitFor(() => expect(screen.getAllByRole("listitem")).toHaveLength(6));
  settle(Response.json(receipt(), { status: 201 }));
  await Promise.resolve();

  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
  expect(fetcher.mock.calls.at(-1)?.[0]).toBe("/api/v1/me/connectors");
});

/*
 * @s37: la respuesta tardia no modifica la vista. Sin abortar la peticion anterior al empezar una
 * nueva, dos operaciones del propietario conviven y gana la que llega ultima, no la que pidio
 * ultima: una importacion lenta repinta «Creadas/Omitidas/Fallidas» sobre una pantalla que el
 * propietario ya desconecto, y el aviso aria-live sigue diciendo que importa algo que ya no existe.
 */
it("@s37 a slow import does not repaint its receipt over an already disconnected screen", async () => {
  const user = userEvent.setup();
  setCsrfToken("csrf-1");
  let settleImport: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(connected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settleImport = resolve;
    }),
  );
  fetcher.mockResolvedValueOnce(new Response(null, { status: 204 }));
  vi.stubGlobal("fetch", fetcher);

  render(<GitlabConnector owner="owner" />);
  await screen.findByRole("button", { name: "Importar issues" });
  await user.click(screen.getByRole("button", { name: "Importar issues" }));
  const inFlight = (fetcher.mock.calls[2][1] as RequestInit).signal!;

  await user.click(screen.getByRole("button", { name: "Desconectar" }));
  await user.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("button", { name: "Importar issues" }),
    ).toBeNull(),
  );

  // Empezar la desconexion cancela la importacion: no basta con ignorar su respuesta despues.
  expect(inFlight.aborted).toBe(true);
  settleImport(Response.json(receipt(), { status: 201 }));
  await waitFor(() => expect(screen.getByRole("status").textContent).toBe(""));

  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
  expect(document.body.textContent).not.toContain("Creadas");
});

it("@s37 closing the session leaves neither the token nor the path anywhere", async () => {
  const user = userEvent.setup();
  let settle: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(notConnected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settle = resolve;
    }),
  );
  fetcher.mockResolvedValueOnce(Response.json(notConnected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  vi.stubGlobal("fetch", fetcher);

  const view = render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);
  const inFlight = (fetcher.mock.calls[2][1] as RequestInit).signal!;

  // La sesión siguiente entra en la misma raíz: es ahí donde el cambio de propietario decide.
  view.rerender(<GitlabConnector owner="otra" />);
  expect(inFlight.aborted).toBe(true);
  settle(Response.json(connected));
  await Promise.resolve();

  expect(JSON.stringify(window.localStorage)).not.toContain(TOKEN);
  expect(JSON.stringify(window.sessionStorage)).not.toContain(TOKEN);
  expect(document.cookie).not.toContain(TOKEN);
  expect(document.body.innerHTML).not.toContain(TOKEN);

  await screen.findByLabelText(/Token de acceso personal/);
  expect((tokenField() as HTMLInputElement).value).toBe("");
  expect((pathField() as HTMLInputElement).value).toBe("");
});

it("@s37 an expired session is announced upwards and paints no connector data", async () => {
  const user = userEvent.setup();
  const seen: number[] = [];
  observeAccess((status) => seen.push(status));
  openConnected(problem(401, { code: "UNAUTHENTICATED" }));

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Importar issues" }));

  await waitFor(() => expect(seen).toContain(401));
  expect(
    screen.queryByRole("region", { name: "Resultado de la importación" }),
  ).toBeNull();
});

it("@s37 a late response after leaving announces nothing through aria-live", async () => {
  const user = userEvent.setup();
  let settle: (value: Response) => void = () => {};
  const fetcher = vi.fn();
  fetcher.mockResolvedValueOnce(Response.json(connected));
  fetcher.mockResolvedValueOnce(Response.json(projects));
  fetcher.mockReturnValueOnce(
    new Promise<Response>((resolve) => {
      settle = resolve;
    }),
  );
  vi.stubGlobal("fetch", fetcher);
  const noise: unknown[] = [];
  const previous = console.error;
  console.error = (...args: unknown[]) => noise.push(args);

  const view = render(<GitlabConnector owner="owner" />);
  await screen.findByRole("button", { name: "Importar issues" });
  await user.click(screen.getByRole("button", { name: "Importar issues" }));
  const inFlight = (fetcher.mock.calls[2][1] as RequestInit).signal!;
  view.unmount();
  settle(Response.json(receipt(), { status: 201 }));
  await Promise.resolve();

  console.error = previous;
  expect(inFlight.aborted).toBe(true);
  expect(noise).toHaveLength(0);
  expect(screen.queryByRole("status")).toBeNull();
});

// ============================ @s38 el foco acompaña a cada cambio de estado

/** El foco tiene que quedarse en el h1 o en el aviso de resultado, nunca perdido en el body. */
function focusLanded() {
  const heading = screen.getByRole("heading", { level: 1 });
  const notices = [
    ...document.querySelectorAll(
      '[role="status"], [aria-label="Resultado de la importación"]',
    ),
  ];
  return (
    document.activeElement === heading ||
    notices.includes(document.activeElement!)
  );
}

it("@s38 keeps the focus on the heading or the result notice when the connection is made", async () => {
  const user = userEvent.setup();
  stub(
    Response.json(notConnected),
    Response.json(projects),
    Response.json(connected),
  );

  render(<GitlabConnector owner="owner" />);
  await screen.findByLabelText(/Token de acceso personal/);
  await fillAndSubmit(user);

  await waitFor(() => expect(screen.getByText("Conectado")).toBeTruthy());
  expect(focusLanded()).toBe(true);
});

it("@s38 keeps the focus on the result notice when the receipt arrives", async () => {
  const user = userEvent.setup();
  openConnected(Response.json(receipt(), { status: 201 }));

  await renderConnected();
  document.body.focus();
  await user.click(importButton());

  await screen.findByRole("region", { name: "Resultado de la importación" });
  expect(focusLanded()).toBe(true);
});

it("@s38 keeps the focus on the heading when the connection is dropped", async () => {
  const user = userEvent.setup();
  openConnected(new Response(null, { status: 204 }));

  await renderConnected();
  await user.click(screen.getByRole("button", { name: "Desconectar" }));
  await user.click(
    screen.getByRole("button", { name: "Confirmar desconexión" }),
  );

  await screen.findByLabelText(/Token de acceso personal/);
  expect(document.activeElement).toBe(
    screen.getByRole("heading", { level: 1 }),
  );
});
