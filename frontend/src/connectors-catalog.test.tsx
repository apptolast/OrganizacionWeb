import { afterEach, expect, it, vi } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import { ConnectorsCatalog, CONNECTOR_ROUTES } from "./connectors-catalog";
import { CONNECTOR_ORDER, type ConnectorId } from "./connectors-catalog-client";
import type { FeedError } from "./external-calendar-api";

const AT = "2026-09-10T08:30:00.123456Z";

const NAMES = [
  "API para integraciones",
  "Webhooks",
  "Calendario",
  "Conector de GitHub",
  "Calendario externo",
  "Conector de GitLab",
];

function row(id: string, overrides: Record<string, unknown> = {}) {
  return {
    id,
    status: "not_connected",
    lastActivityAt: null,
    lastError: null,
    ...overrides,
  };
}

function catalog(patches: Record<string, Record<string, unknown>> = {}) {
  return { connectors: CONNECTOR_ORDER.map((id) => row(id, patches[id])) };
}

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

const rows = () => screen.getAllByRole("listitem");

afterEach(() => {
  vi.unstubAllGlobals();
});

// ------------------------------------------------------- @s33 las seis filas y su orden

it("@s33 opens with exactly one GET of the catalogue and nothing else", async () => {
  const fetcher = stub(Response.json(catalog()));

  render(<ConnectorsCatalog />);

  await screen.findByRole("heading", { level: 1, name: "Conectores" });
  await waitFor(() => expect(rows()).toHaveLength(6));
  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/connectors");
});

it("@s33 lists the six connectors by name in the order of the catalogue", async () => {
  stub(Response.json(catalog()));

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  expect(
    rows().map((item) => within(item).getByRole("heading").textContent),
  ).toEqual(NAMES);
});

it("@s33 moves the focus to the heading when the screen opens", async () => {
  stub(Response.json(catalog()));

  render(<ConnectorsCatalog />);

  const heading = await screen.findByRole("heading", { level: 1 });
  expect(document.activeElement).toBe(heading);
});

// -------------------------------------------------- @s33 el estado, en texto y con marcador

it("@s33 shows each state as words and as a marker with the very same accessible text", async () => {
  stub(
    Response.json(
      catalog({
        api_credentials: { status: "connected" },
        webhooks: { status: "disabled" },
        github: {
          status: "error",
          lastError: { code: "CONNECTION_INVALID", at: AT },
        },
      }),
    ),
  );

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  const expected = [
    "Conectado",
    "Deshabilitado",
    "No conectado",
    "Error",
    "No conectado",
    "No conectado",
  ];
  expect(
    rows().map((item) =>
      within(item).getByRole("img").getAttribute("aria-label"),
    ),
  ).toEqual(expected);
  for (const [index, item] of rows().entries())
    expect(within(item).getByText(expected[index])).toBeTruthy();
});

it("@s33 gives each state a marker of its own, so colour is never the only cue", async () => {
  stub(
    Response.json(
      catalog({
        api_credentials: { status: "connected" },
        webhooks: { status: "disabled" },
        github: {
          status: "error",
          lastError: { code: "CONNECTION_INVALID", at: AT },
        },
      }),
    ),
  );

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  const unique = <T,>(all: T[]) => all.filter((v, i) => all.indexOf(v) === i);
  const markers = rows().map(
    (item) => within(item).getByRole("img").textContent,
  );
  const states = rows().map((item) =>
    within(item).getByRole("img").getAttribute("aria-label"),
  );
  // Tantos glifos distintos como estados distintos: ni uno repetido entre dos estados.
  expect(unique(markers)).toHaveLength(unique(states).length);
  expect(markers.every((glyph) => Boolean(glyph && glyph.trim()))).toBe(true);
});

// ------------------------------------------------------------- @s33 el error y la actividad

it("@s33 shows the GitHub error as a translated code and its activity in the user's zone", async () => {
  stub(
    Response.json(
      catalog({
        github: {
          status: "error",
          lastActivityAt: AT,
          lastError: { code: "CONNECTION_INVALID", at: AT },
        },
      }),
    ),
  );

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  const github = rows()[3];
  expect(within(github).getByText("La conexión ya no es válida")).toBeTruthy();
  expect(within(github).queryByText("CONNECTION_INVALID")).toBeNull();
  const readable = new Intl.DateTimeFormat("es", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(AT));
  expect(within(github).getByText(readable)).toBeTruthy();
});

it("@s33 says nothing about activity when the row has none", async () => {
  stub(Response.json(catalog()));

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  expect(within(rows()[3]).queryByText(/Última actividad/)).toBeNull();
});

// --------------------------------------------------------------------- @s33 los enlaces

it("@s33 links each row to the route of its own screen", async () => {
  stub(Response.json(catalog()));

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  expect(
    rows().map((item) => within(item).getByRole("link").getAttribute("href")),
  ).toEqual(CONNECTOR_ORDER.map((id) => CONNECTOR_ROUTES[id]));
});

it("@s33 sends the three named rows exactly where the contract says", async () => {
  expect(CONNECTOR_ROUTES.gitlab).toBe("/conectores/gitlab");
  expect(CONNECTOR_ROUTES.api_credentials).toBe("/integraciones");
  expect(CONNECTOR_ROUTES.github).toBe("/integraciones/github");
});

it("@s33 shows «No disponible» without a link for a screen that is not deployed", async () => {
  stub(Response.json(catalog()));

  render(
    <ConnectorsCatalog routes={{ ...CONNECTOR_ROUTES, webhooks: null }} />,
  );

  await waitFor(() => expect(rows()).toHaveLength(6));
  expect(within(rows()[1]).queryByRole("link")).toBeNull();
  expect(within(rows()[1]).getByText("No disponible")).toBeTruthy();
  expect(within(rows()[0]).getByRole("link")).toBeTruthy();
});

// ------------------------------------------------------------------------------- @s7

it("@s7 an unavailable storage shows the failure and no optimistic row at all", async () => {
  stub(problem(503, { code: "STORAGE_UNAVAILABLE" }));

  render(<ConnectorsCatalog />);

  await screen.findByRole("alert");
  expect(screen.queryAllByRole("listitem")).toHaveLength(0);
  expect(screen.queryByText("Conectado")).toBeNull();
});

it("@s7 a catalogue the client cannot read is not painted either", async () => {
  stub(Response.json({ connectors: [] }));

  render(<ConnectorsCatalog />);

  await screen.findByRole("alert");
  expect(screen.queryAllByRole("listitem")).toHaveLength(0);
});

// ------------------------------------------------------------------------------ @s37

it("@s37 a response that lands after the screen is gone changes nothing", async () => {
  let settle: (value: Response) => void = () => {};
  const pending = new Promise<Response>((resolve) => {
    settle = resolve;
  });
  vi.stubGlobal(
    "fetch",
    vi.fn(() => pending),
  );
  const failures: unknown[] = [];
  const previous = console.error;
  console.error = (...args: unknown[]) => failures.push(args);

  const view = render(<ConnectorsCatalog />);
  view.unmount();
  settle(Response.json(catalog()));
  await pending;

  console.error = previous;
  expect(failures).toHaveLength(0);
  expect(screen.queryAllByRole("listitem")).toHaveLength(0);
});

// ---------------------- @s33 el mapa de códigos frente a lo que el backend sabe emitir

/**
 * Los siete errores de feed, declarados como `Record<FeedError, string>` a propósito: así el
 * compilador exige la lista entera y añadir un octavo error de calendario rompe este fichero en
 * vez de degradar la pantalla en silencio.
 */
const FEED_TEXT: Record<FeedError, string> = {
  FEED_REJECTED: "La dirección del calendario ya no es válida",
  FEED_UNREACHABLE: "El calendario no responde",
  FEED_HTTP_ERROR: "El calendario respondió con un error",
  FEED_TOO_LARGE: "El calendario es demasiado grande",
  FEED_UNSUPPORTED_TYPE: "El calendario no es un archivo de texto",
  FEED_MALFORMED: "El calendario no se pudo interpretar",
  SECRET_UNREADABLE: "Hay que volver a introducir la dirección",
};

/**
 * Todo lo que el catálogo puede recibir de verdad en `lastError.code`, con la fila donde aparece
 * y el emisor al lado. Los códigos viven repetidos en sitios independientes y nada los ata: si el
 * backend emite uno que el mapa no conoce, el propietario lee el texto genérico y nadie se entera.
 * Es lo que llevaba pasando con los dos de GitLab, y @s33 promete «el último error como código
 * traducido».
 */
const TRANSLATED: Array<[ConnectorId, string, string]> = [
  // GithubStatusSource: la única cosa que puede ir mal en una conexión de 27.
  ["github", "CONNECTION_INVALID", "La conexión ya no es válida"],
  // WebhookStatusSource, con WebhookEndpoint.DELIVERY_EXHAUSTED.
  ["webhooks", "DELIVERY_EXHAUSTED", "Se agotaron los reintentos de entrega"],
  // ConnectorFailures.gitlabImportErrorCode: los tres que una importación de GitLab deja escritos.
  ["gitlab", "CONNECTION_INVALID", "La conexión ya no es válida"],
  ["gitlab", "RATE_LIMITED", "El proveedor limitó las peticiones"],
  ["gitlab", "GITLAB_UNAVAILABLE", "GitLab no responde"],
  // ExternalCalendarStatusSource, con el nombre del FeedError.
  ...(Object.entries(FEED_TEXT).map(([code, text]) => [
    "external_calendar",
    code,
    text,
  ]) as Array<[ConnectorId, string, string]>),
];

const GENERIC = "Hay un problema con esta integración";

function withError(id: ConnectorId, code: string) {
  return catalog({ [id]: { status: "error", lastError: { code, at: AT } } });
}

it.each(TRANSLATED)(
  "@s33 the %s row translates %s instead of falling back to the generic text",
  async (id, code, text) => {
    stub(Response.json(withError(id, code)));

    render(<ConnectorsCatalog />);

    await waitFor(() => expect(rows()).toHaveLength(6));
    const item = rows()[CONNECTOR_ORDER.indexOf(id)];
    expect(within(item).getByText(text)).toBeTruthy();
    expect(within(item).queryByText(code)).toBeNull();
    expect(within(item).queryByText(GENERIC)).toBeNull();
  },
);

it("@s33 a code this version does not know still says something honest", async () => {
  stub(Response.json(withError("gitlab", "UNKNOWN_TO_THIS_VERSION")));

  render(<ConnectorsCatalog />);

  await waitFor(() => expect(rows()).toHaveLength(6));
  expect(within(rows()[5]).getByText(GENERIC)).toBeTruthy();
  expect(within(rows()[5]).queryByText("UNKNOWN_TO_THIS_VERSION")).toBeNull();
});
