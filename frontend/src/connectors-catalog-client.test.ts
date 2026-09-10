import { afterEach, expect, it, vi } from "vitest";
import { observeAccess, setCsrfToken } from "./api-client";
import {
  CONNECTOR_ORDER,
  CatalogError,
  readConnectorCatalog,
} from "./connectors-catalog-client";

const AT = "2026-09-10T08:30:00.123456Z";

function row(id: string, overrides: Record<string, unknown> = {}) {
  return {
    id,
    status: "not_connected",
    lastActivityAt: null,
    lastError: null,
    ...overrides,
  };
}

function catalog(...overrides: Record<string, Record<string, unknown>>[]) {
  const patches = Object.assign({}, ...overrides) as Record<
    string,
    Record<string, unknown>
  >;
  return { connectors: CONNECTOR_ORDER.map((id) => row(id, patches[id])) };
}

function stub(...responses: Response[]) {
  const fetcher = vi.fn();
  for (const response of responses) fetcher.mockResolvedValueOnce(response);
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}

function problem(status: number, body: Record<string, unknown>) {
  return Response.json(body, {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });
}

const signal = () => new AbortController().signal;

afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
  observeAccess();
});

// ------------------------------------------------------------------------------------ @s33

it("@s33 reads the catalogue with exactly one GET through the cookie transport", async () => {
  const fetcher = stub(Response.json(catalog()));

  const rows = await readConnectorCatalog(signal());

  expect(fetcher).toHaveBeenCalledTimes(1);
  const [url, options] = fetcher.mock.calls[0] as [string, RequestInit];
  expect(url).toBe("/api/v1/me/connectors");
  expect(options.method ?? "GET").toBe("GET");
  expect(rows.map((each) => each.id)).toEqual([...CONNECTOR_ORDER]);
});

it("@s33 keeps the six rows in the order the server sent them", async () => {
  stub(Response.json(catalog({ gitlab: { status: "connected" } })));

  const rows = await readConnectorCatalog(signal());

  expect(rows).toHaveLength(6);
  expect(rows[5]).toEqual({
    id: "gitlab",
    status: "connected",
    lastActivityAt: null,
    lastError: null,
  });
});

it("@s33 decodes an error row as a stable code and an instant", async () => {
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

  const rows = await readConnectorCatalog(signal());

  expect(rows[3].lastError).toEqual({ code: "CONNECTION_INVALID", at: AT });
  expect(rows[3].lastActivityAt).toBe(AT);
});

// -------------------------------------------------------- lo que el cliente se niega a creer

it("@s33 refuses a catalogue that is not the six agreed rows", async () => {
  stub(Response.json({ connectors: [row("gitlab")] }));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s33 refuses a catalogue whose rows arrive in another order", async () => {
  const reversed = {
    connectors: [...CONNECTOR_ORDER].reverse().map((id) => row(id)),
  };
  stub(Response.json(reversed));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s33 refuses a row with a status outside the four agreed ones", async () => {
  stub(Response.json(catalog({ webhooks: { status: "conectado" } })));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s33 refuses a row that smuggles in a seventh field", async () => {
  const body = catalog();
  (body.connectors[5] as Record<string, unknown>).projectPath =
    "grupo/proyecto";
  stub(Response.json(body));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s33 refuses an envelope carrying anything besides connectors", async () => {
  stub(Response.json({ ...catalog(), total: 6 }));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s5 refuses a lastError that carries the provider text", async () => {
  stub(
    Response.json(
      catalog({
        gitlab: {
          status: "error",
          lastError: {
            code: "CONNECTION_INVALID",
            at: AT,
            detail: "invalid_token: glpat-abcdef1234",
          },
        },
      }),
    ),
  );

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s33 refuses a lastError whose code is not even a string", async () => {
  stub(
    Response.json(
      catalog({
        gitlab: { status: "error", lastError: { code: 42, at: AT } },
      }),
    ),
  );

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s33 refuses a last activity that is not an instant", async () => {
  stub(Response.json(catalog({ webhooks: { lastActivityAt: "ayer" } })));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

/** Una séptima fila significa un servidor que esta versión no sabe leer, no una fila de más. */
it("@s33 refuses a catalogue with a seventh connector", async () => {
  const body = catalog();
  body.connectors.push(row("gitlab") as never);
  stub(Response.json(body));

  await expect(readConnectorCatalog(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

// ------------------------------------------------------------------------------------- @s7

it("@s7 turns a storage failure into a typed error and never an empty catalogue", async () => {
  stub(problem(503, { code: "STORAGE_UNAVAILABLE" }));

  await expect(readConnectorCatalog(signal())).rejects.toMatchObject({
    name: "CatalogError",
    code: "STORAGE_UNAVAILABLE",
    // Lo unico que se lee en un volcado del navegador cuando el error escapa.
    message: "STORAGE_UNAVAILABLE",
  });
});

it("@s7 keeps a code even when the problem body is unreadable", async () => {
  stub(new Response("no es json", { status: 503 }));

  const error = await readConnectorCatalog(signal()).catch(
    (caught: unknown) => caught,
  );

  expect(error).toBeInstanceOf(CatalogError);
  expect((error as CatalogError).code).toBe("CATALOG_ERROR");
  expect((error as CatalogError).message).toBe("CATALOG_ERROR");
});

// ------------------------------------------------------------------------------------ @s37

it("@s37 does not decode anything once the caller aborted", async () => {
  const controller = new AbortController();
  stub(Response.json(catalog()));
  controller.abort();

  await expect(readConnectorCatalog(controller.signal)).rejects.toThrow();
});

/** Las tres paradas de aborto de la lectura, cada una con lo que promete que NO pasará. */
it("@s37 asks the server for nothing when the caller already aborted", async () => {
  const controller = new AbortController();
  const fetcher = stub();
  controller.abort();

  await expect(readConnectorCatalog(controller.signal)).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s37 does not read the body of a catalogue that landed after the abort", async () => {
  const controller = new AbortController();
  const json = vi.fn(() => Promise.resolve(catalog()));
  vi.stubGlobal(
    "fetch",
    vi.fn(() => {
      controller.abort();
      return Promise.resolve({ status: 200, json });
    }),
  );

  await expect(readConnectorCatalog(controller.signal)).rejects.toThrow();
  expect(json).not.toHaveBeenCalled();
});

it("@s37 decodes nothing when the abort lands while the body is being read", async () => {
  const controller = new AbortController();
  vi.stubGlobal(
    "fetch",
    vi.fn(() =>
      Promise.resolve({
        status: 200,
        json: () => {
          controller.abort();
          return Promise.resolve(catalog());
        },
      }),
    ),
  );

  await expect(readConnectorCatalog(controller.signal)).rejects.toThrow();
});

it("@s37 sends the caller's own signal, so leaving the screen cancels the read", async () => {
  const controller = new AbortController();
  const fetcher = stub(Response.json(catalog()));

  await readConnectorCatalog(controller.signal);

  expect((fetcher.mock.calls[0][1] as RequestInit).signal).toBe(
    controller.signal,
  );
});
