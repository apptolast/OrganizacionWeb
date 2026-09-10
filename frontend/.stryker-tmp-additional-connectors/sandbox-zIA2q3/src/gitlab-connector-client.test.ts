// @ts-nocheck
import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { afterEach, expect, it, vi } from "vitest";
import { observeAccess, setCsrfToken } from "./api-client";
import {
  GitlabConnectorError,
  connectGitlab,
  disconnectGitlab,
  readGitlabConnection,
  readGitlabImport,
  startGitlabImport,
  CONNECTION_KEYS,
  ERROR_FIELDS,
  RECEIPT_KEYS,
} from "./gitlab-connector-client";

const projectId = "11111111-2222-4333-8444-555555555555";
const importId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
const TOKEN = "glpat-xxxxxxxxxxxxxxxxWXYZ";

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

const receipt = {
  id: importId,
  source: "gitlab",
  projectId,
  projectPath: "grupo/proyecto",
  status: "completed",
  created: 3,
  skipped: 1,
  failed: 0,
  truncated: false,
  errorCode: null,
  startedAt: "2026-09-09T12:00:00.123456Z",
  finishedAt: "2026-09-09T12:00:04.123456Z",
};

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

// ------------------------------------------------------------------------ @s8 leer

it("@s8 reads the connection without asking for anything else", async () => {
  const fetcher = stub(Response.json(connected));

  const view = await readGitlabConnection(signal());

  expect(fetcher).toHaveBeenCalledTimes(1);
  expect(fetcher.mock.calls[0][0]).toBe("/api/v1/me/connectors/gitlab");
  expect(view).toEqual(connected);
});

it("@s8 accepts not_connected with its seven nulls instead of treating it as a failure", async () => {
  stub(Response.json(notConnected));

  await expect(readGitlabConnection(signal())).resolves.toEqual(notConnected);
});

it("@s8 refuses a connection carrying a ninth field", async () => {
  stub(Response.json({ ...connected, token: TOKEN }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 refuses a connected row that forgot its project", async () => {
  stub(Response.json({ ...connected, projectPath: null }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 refuses a status outside the three agreed ones", async () => {
  stub(Response.json({ ...connected, status: "valid" }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

// ------------------------------------------------------------------------ @s9 conectar

it("@s9 sends exactly the token and the project path, and nothing else", async () => {
  setCsrfToken("csrf-1");
  const fetcher = stub(Response.json(connected));

  await connectGitlab(
    { token: TOKEN, projectPath: "grupo/proyecto" },
    signal(),
  );

  const [url, options] = fetcher.mock.calls[0] as [string, RequestInit];
  expect(url).toBe("/api/v1/me/connectors/gitlab");
  expect(options.method).toBe("PUT");
  expect(JSON.parse(String(options.body))).toEqual({
    token: TOKEN,
    projectPath: "grupo/proyecto",
  });
  expect(new Headers(options.headers).get("X-CSRF-TOKEN")).toBe("csrf-1");
});

it("@s32 never puts the token in the URL", async () => {
  const fetcher = stub(Response.json(connected));

  await connectGitlab(
    { token: TOKEN, projectPath: "grupo/proyecto" },
    signal(),
  );

  expect(String(fetcher.mock.calls[0][0])).not.toContain(TOKEN);
});

// ------------------------------------------------------------------- @s14 desconectar

it("@s14 accepts the empty 204 of a disconnection", async () => {
  const fetcher = stub(new Response(null, { status: 204 }));

  await expect(disconnectGitlab(signal())).resolves.toBeUndefined();
  expect((fetcher.mock.calls[0][1] as RequestInit).method).toBe("DELETE");
});

// -------------------------------------------------------------------- @s15 importar

it("@s15 starts an import with exactly the destination project", async () => {
  const fetcher = stub(Response.json(receipt, { status: 201 }));

  const started = await startGitlabImport(projectId, signal());

  const [url, options] = fetcher.mock.calls[0] as [string, RequestInit];
  expect(url).toBe("/api/v1/me/connectors/gitlab/imports");
  expect(options.method).toBe("POST");
  expect(JSON.parse(String(options.body))).toEqual({ projectId });
  expect(started).toEqual(receipt);
});

it("@s15 refuses a receipt whose source is not gitlab", async () => {
  stub(Response.json({ ...receipt, source: "github" }, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s15 refuses a finished receipt with no ending", async () => {
  stub(Response.json({ ...receipt, finishedAt: null }, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s30 reads a receipt back by its identifier", async () => {
  const fetcher = stub(Response.json(receipt));

  await expect(readGitlabImport(importId, signal())).resolves.toEqual(receipt);
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/me/connectors/gitlab/imports/${importId}`,
  );
});

// ---------------------------------------------------------------- @s36 los errores

it("@s36 keeps the seconds of a rate limit so the screen can name them", async () => {
  stub(problem(503, { code: "RATE_LIMITED", retryAfterSeconds: 30 }));

  const error = await startGitlabImport(projectId, signal()).catch(
    (caught: unknown) => caught,
  );

  expect(error).toBeInstanceOf(GitlabConnectorError);
  expect((error as GitlabConnectorError).code).toBe("RATE_LIMITED");
  expect((error as GitlabConnectorError).retryAfterSeconds).toBe(30);
});

it("@s36 keeps the partial counters that travel with a failed import", async () => {
  stub(
    problem(503, {
      code: "GITLAB_UNAVAILABLE",
      importId,
      created: 100,
      skipped: 0,
      failed: 0,
    }),
  );

  const error = (await startGitlabImport(projectId, signal()).catch(
    (caught: unknown) => caught,
  )) as GitlabConnectorError;

  expect(error.importId).toBe(importId);
  expect(error.created).toBe(100);
});

it("@s36 turns an unreadable problem body into a code the screen can still act on", async () => {
  stub(new Response("no es json", { status: 503 }));

  const error = (await readGitlabConnection(signal()).catch(
    (caught: unknown) => caught,
  )) as GitlabConnectorError;

  expect(error.code).toBe("CONNECTOR_ERROR");
  expect(error.retryAfterSeconds).toBeNull();
});

it("@s36 refuses a negative retry, which would make the screen promise the past", async () => {
  stub(problem(503, { code: "RATE_LIMITED", retryAfterSeconds: -1 }));

  const error = (await startGitlabImport(projectId, signal()).catch(
    (caught: unknown) => caught,
  )) as GitlabConnectorError;

  expect(error.retryAfterSeconds).toBeNull();
});

// ---------------------------------------------------------------------- @s37

it("@s37 does not decode anything once the caller aborted", async () => {
  const controller = new AbortController();
  stub(Response.json(connected));
  controller.abort();

  await expect(readGitlabConnection(controller.signal)).rejects.toThrow();
});

// ------------------------------------------------- el contrato, atado a los dos lados

/**
 * La regresión de la noche del 9 de septiembre: el recibo pasó a llevar `source` y `projectPath`
 * en vez de `repository` y el cliente del otro conector siguió decodificando la forma vieja. No
 * había ninguna prueba que atara las dos orillas, así que el cambio pasó los dos lados por
 * separado y falló al juntarlos. Ésta lee la fuente Java y compara las claves, una a una.
 */
function componentsOf(record: string, source: string): string[] {
  const body = new RegExp(`record ${record}\\(([^)]*)\\)`, "s").exec(source);
  if (!body) throw new Error(`no se encontró el record ${record}`);
  return body[1]
    .split(",")
    .map((each) => each.trim().split(/\s+/).at(-1) ?? "")
    .filter(Boolean);
}

const CONTROLLER =
  "backend/src/main/java/com/apptolast/organization/adapter/http/GitlabConnectorController.java";

/** Sube desde el directorio de trabajo hasta encontrar la raíz del repositorio. */
function controller(): string {
  for (let where = process.cwd(), step = 0; step < 6; step++) {
    const candidate = resolve(where, CONTROLLER);
    if (existsSync(candidate)) return readFileSync(candidate, "utf8");
    where = dirname(where);
  }
  throw new Error(`no se encontró ${CONTROLLER} desde ${process.cwd()}`);
}

/**
 * Conjuntos, no listas. El orden de los componentes de un `record` no es parte del contrato JSON:
 * `exact()` compara cardinalidad y presencia, y un objeto JSON no tiene orden. Reordenarlos es un
 * no-op en producción, y una guarda que se pone roja por un cambio cosmético acaba relajada.
 */
function sorted(fields: string[]): string[] {
  return [...fields].sort();
}

it("@s15 decodes exactly the receipt the controller publishes", () => {
  expect(sorted(componentsOf("ImportResponse", controller()))).toEqual(
    sorted(RECEIPT_KEYS.split(" ")),
  );
});

it("@s8 decodes exactly the connection the controller publishes", () => {
  expect(sorted(componentsOf("ConnectionResponse", controller()))).toEqual(
    sorted(CONNECTION_KEYS.split(" ")),
  );
});

/**
 * El tercer `record` que el cliente decodifica, y que la guarda se dejaba fuera. `decodeFailure`
 * valida `lastError` contra `ERROR_FIELDS`; renombrar `code` por `errorCode` en el `ErrorResponse`
 * del controlador pasaba la prueba de contrato entera y rompía la decodificación en ejecución.
 * Es la misma clase de fallo que la regresión de `a347936`, un nivel más abajo.
 */
it("@s2 decodes exactly the error the controller publishes", () => {
  expect(sorted(componentsOf("ErrorResponse", controller()))).toEqual(
    sorted(ERROR_FIELDS.split(" ")),
  );
});
