// @ts-nocheck
import { existsSync, readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { afterEach, expect, it, vi } from "vitest";
import { setCsrfToken, observeAccess } from "./api-client";
import {
  CONNECTION_FIELDS,
  ConnectorError,
  RECEIPT_FIELDS,
  connectGithub,
  disconnectGithub,
  readGithubConnection,
  readGithubImport,
  startGithubImport,
} from "./github-connector-client";

const projectId = "11111111-2222-4333-8444-555555555555";
const importId = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
const receipt = {
  id: importId,
  source: "github",
  projectId,
  projectPath: "octocat/Hello-World",
  status: "completed",
  created: 3,
  skipped: 1,
  failed: 0,
  truncated: false,
  errorCode: null,
  startedAt: "2026-09-09T12:00:00.123456Z",
  finishedAt: "2026-09-09T12:00:04.123456Z",
};
const connection = {
  repository: "octocat/Hello-World",
  login: "octocat",
  status: "valid",
  connectedAt: "2026-09-09T10:00:00.123456Z",
  lastImport: null,
};

function stub(...responses: Response[]) {
  const fetcher = vi.fn();
  for (const response of responses) fetcher.mockResolvedValueOnce(response);
  vi.stubGlobal("fetch", fetcher);
  return fetcher;
}

function problem(status: number, body: Record<string, unknown>, headers = {}) {
  return Response.json(body, {
    status,
    headers: { "Content-Type": "application/problem+json", ...headers },
  });
}

const signal = () => new AbortController().signal;

afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
  observeAccess();
});

// -------------------------------------------------------------------- @s10 leer la conexión

it("@s10 reads the connection through the cookie transport", async () => {
  const fetcher = stub(Response.json(connection));

  expect(await readGithubConnection(signal())).toEqual(connection);

  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/connectors/github");
  expect(options.method ?? "GET").toBe("GET");
});

it("@s10 answers null when there is no connection yet", async () => {
  stub(problem(404, { code: "CONNECTION_NOT_FOUND" }));

  expect(await readGithubConnection(signal())).toBeNull();
});

it("@s36 surfaces a disabled connector as its own error code", async () => {
  stub(problem(503, { code: "CONNECTORS_DISABLED" }));

  await expect(readGithubConnection(signal())).rejects.toMatchObject({
    code: "CONNECTORS_DISABLED",
  });
});

it("@s10 carries the last receipt whole", async () => {
  stub(Response.json({ ...connection, lastImport: receipt }));

  expect((await readGithubConnection(signal()))?.lastImport).toEqual(receipt);
});

it("@s10 rejects a connection whose shape is not the agreed one", async () => {
  for (const body of [
    { ...connection, extra: 1 },
    { ...connection, status: "caducada" },
    { ...connection, repository: "" },
    { ...connection, connectedAt: "ayer" },
    { ...connection, lastImport: { ...receipt, status: "cancelado" } },
    { ...connection, lastImport: { ...receipt, created: -1 } },
    { ...connection, lastImport: { ...receipt, finishedAt: null } },
  ]) {
    stub(Response.json(body));
    await expect(readGithubConnection(signal())).rejects.toThrow(
      "Confirmación incompatible",
    );
    vi.unstubAllGlobals();
  }
});

it("@s10 accepts a running receipt without end nor error", async () => {
  const running = {
    ...receipt,
    status: "running",
    errorCode: null,
    finishedAt: null,
  };
  stub(Response.json({ ...connection, lastImport: running }));

  expect((await readGithubConnection(signal()))?.lastImport).toEqual(running);
});

// ----------------------------------------------------------------------- @s1 @s7 conectar

it("@s1 connects with the CSRF header and never keeps the token", async () => {
  const fetcher = stub(Response.json(connection));
  setCsrfToken("csrf-own-session");

  expect(
    await connectGithub(
      { repository: "octocat/Hello-World", token: "ghp_secreto123" },
      signal(),
    ),
  ).toEqual(connection);

  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/connectors/github");
  expect(options.method).toBe("PUT");
  expect(options.headers.get("X-CSRF-TOKEN")).toBe("csrf-own-session");
  expect(JSON.parse(options.body)).toEqual({
    repository: "octocat/Hello-World",
    token: "ghp_secreto123",
  });
});

it("@s37 turns a rejected token into an error the form can place by the field", async () => {
  stub(problem(409, { code: "GITHUB_TOKEN_REJECTED" }));

  const error = await connectGithub(
    { repository: "octocat/Hello-World", token: "ghp_malo" },
    signal(),
  ).catch((failure: unknown) => failure);

  expect(error).toBeInstanceOf(ConnectorError);
  expect((error as ConnectorError).code).toBe("GITHUB_TOKEN_REJECTED");
});

it("@s21 keeps the delay GitHub asked for", async () => {
  stub(problem(503, { code: "RATE_LIMITED", retryAfterSeconds: 45 }));

  const error = await connectGithub(
    { repository: "octocat/Hello-World", token: "ghp_x" },
    signal(),
  ).catch((failure: unknown) => failure);

  expect((error as ConnectorError).code).toBe("RATE_LIMITED");
  expect((error as ConnectorError).retryAfterSeconds).toBe(45);
});

// ------------------------------------------------------------------------ @s11 desconectar

it("@s11 disconnects with the CSRF header and expects no body", async () => {
  const fetcher = stub(new Response(null, { status: 204 }));
  setCsrfToken("csrf-own-session");

  await disconnectGithub(signal());

  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/connectors/github");
  expect(options.method).toBe("DELETE");
  expect(options.headers.get("X-CSRF-TOKEN")).toBe("csrf-own-session");
});

// -------------------------------------------------------------------------- @s12 importar

it("@s12 starts an import and returns the receipt", async () => {
  const fetcher = stub(Response.json(receipt, { status: 201 }));
  setCsrfToken("csrf-own-session");

  expect(await startGithubImport(projectId, signal())).toEqual(receipt);

  const [url, options] = fetcher.mock.calls[0];
  expect(url).toBe("/api/v1/me/connectors/github/imports");
  expect(options.method).toBe("POST");
  expect(JSON.parse(options.body)).toEqual({ projectId });
});

// El juego de claves que emite de verdad la frontera HTTP, escrito a mano y no derivado del
// fixture: es el mismo que fija GithubConnectorApiTest.RECEIPT_FIELDS y el que exige
// additional_connectors.feature:242 (@s20), donde el recibo de GitHub y el de GitLab tienen las
// mismas claves y se distinguen por `source`. Si el backend vuelve a cambiarlo, esta prueba cae.
const BOUNDARY_RECEIPT = {
  id: importId,
  source: "github",
  projectId,
  projectPath: "octocat/Hello-World",
  status: "completed",
  created: 1,
  skipped: 0,
  failed: 1,
  truncated: false,
  errorCode: null,
  startedAt: "2026-09-09T12:00:00.123456Z",
  finishedAt: "2026-09-09T12:00:04.123456Z",
};

it("@s12 decodes the receipt the HTTP boundary really emits, with source and projectPath", async () => {
  stub(Response.json(BOUNDARY_RECEIPT, { status: 201 }));

  const decoded = await startGithubImport(projectId, signal());

  expect(decoded).toEqual(BOUNDARY_RECEIPT);
  // Los contadores llegan enteros: son los que la región de resultado pinta.
  expect([decoded.created, decoded.skipped, decoded.failed]).toEqual([1, 0, 1]);
});

it("@s12 a receipt still shaped like the old one no longer passes", async () => {
  const { source, projectPath, ...rest } = BOUNDARY_RECEIPT;
  stub(Response.json({ ...rest, repository: projectPath }, { status: 201 }));

  await expect(startGithubImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
  expect(source).toBe("github");
});

it("@s30 reads a receipt back", async () => {
  const fetcher = stub(Response.json(receipt));

  expect(await readGithubImport(importId, signal())).toEqual(receipt);
  expect(fetcher.mock.calls[0][0]).toBe(
    `/api/v1/me/connectors/github/imports/${importId}`,
  );
});

it("@s39 carries the partial counters of a broken import", async () => {
  stub(
    problem(503, {
      code: "STORAGE_UNAVAILABLE",
      importId,
      created: 2,
      skipped: 1,
      failed: 0,
    }),
  );

  const error = (await startGithubImport(projectId, signal()).catch(
    (failure: unknown) => failure,
  )) as ConnectorError;

  expect(error.code).toBe("STORAGE_UNAVAILABLE");
  expect(error.importId).toBe(importId);
  expect(error.created).toBe(2);
  expect(error.skipped).toBe(1);
  expect(error.failed).toBe(0);
});

it.each([
  [409, "CONNECTION_INVALID"],
  [409, "IMPORT_IN_PROGRESS"],
  [409, "PROJECT_COMPLETED"],
  [404, "CONNECTION_NOT_FOUND"],
  [404, "RESOURCE_NOT_FOUND"],
  [503, "GITHUB_UNAVAILABLE"],
])("@s39 turns %i %s into a typed error", async (status, code) => {
  stub(problem(status, { code }));

  const error = (await startGithubImport(projectId, signal()).catch(
    (failure: unknown) => failure,
  )) as ConnectorError;

  expect(error).toBeInstanceOf(ConnectorError);
  expect(error.code).toBe(code);
  expect(error.importId).toBeNull();
});

it("@s20 keeps the delay of a rate limited import", async () => {
  stub(problem(503, { code: "RATE_LIMITED", retryAfterSeconds: 90, importId }));

  const error = (await startGithubImport(projectId, signal()).catch(
    (failure: unknown) => failure,
  )) as ConnectorError;

  expect(error.retryAfterSeconds).toBe(90);
  expect(error.importId).toBe(importId);
});

it("@s12 rejects a receipt whose shape is not the agreed one", async () => {
  for (const body of [
    { ...receipt, extra: 1 },
    { ...receipt, status: "running" },
    { ...receipt, truncated: "no" },
    { ...receipt, id: "no-es-uuid" },
    { ...receipt, errorCode: "" },
    // Este cliente sólo habla con el extremo de GitHub: un recibo de otro origen,
    // o sin ruta de proyecto, no es un recibo suyo.
    { ...receipt, source: "gitlab" },
    { ...receipt, source: "" },
    { ...receipt, projectPath: "" },
    { ...receipt, projectPath: 7 },
  ]) {
    stub(Response.json(body, { status: 201 }));
    await expect(startGithubImport(projectId, signal())).rejects.toThrow(
      "Confirmación incompatible",
    );
    vi.unstubAllGlobals();
  }
});

it("@s41 stops as soon as the caller aborts", async () => {
  const controller = new AbortController();
  const fetcher = stub(Response.json(connection));
  controller.abort();

  await expect(readGithubConnection(controller.signal)).rejects.toThrow();
  expect(fetcher).not.toHaveBeenCalled();
});

it("@s39 an unexpected status is not silently taken for success", async () => {
  stub(new Response(null, { status: 418 }));

  await expect(startGithubImport(projectId, signal())).rejects.toBeDefined();
});

// --------------------------------------------------- @s41 disciplina de cancelación, punto a punto

/**
 * Las cinco operaciones consultan la señal tres veces: antes de tocar la red,
 * al volver la respuesta y tras leer el cuerpo. Cada llamada necesita su propio
 * oráculo, porque borrar cualquiera de ellas deja las demás verdes.
 */
const operations = [
  {
    name: "readGithubConnection",
    ok: () => Response.json(connection),
    call: (s: AbortSignal) => readGithubConnection(s),
    decodes: true,
  },
  {
    name: "connectGithub",
    ok: () => Response.json(connection),
    call: (s: AbortSignal) =>
      connectGithub({ repository: "octocat/Hello-World", token: "t" }, s),
    decodes: true,
  },
  {
    name: "disconnectGithub",
    ok: () => new Response(null, { status: 204 }),
    call: (s: AbortSignal) => disconnectGithub(s),
    decodes: false,
  },
  {
    name: "startGithubImport",
    ok: () => Response.json(receipt, { status: 201 }),
    call: (s: AbortSignal) => startGithubImport(projectId, s),
    decodes: true,
  },
  {
    name: "readGithubImport",
    ok: () => Response.json(receipt),
    call: (s: AbortSignal) => readGithubImport(importId, s),
    decodes: true,
  },
];

for (const operation of operations) {
  it(`@s41 ${operation.name} does not touch the network with an aborted signal`, async () => {
    const controller = new AbortController();
    const fetcher = stub(operation.ok());
    controller.abort();

    await expect(operation.call(controller.signal)).rejects.toThrow();
    expect(fetcher).not.toHaveBeenCalled();
  });

  it(`@s41 ${operation.name} stops when the abort lands while the request is in flight`, async () => {
    const controller = new AbortController();
    const fetcher = vi.fn(async () => {
      controller.abort();
      return operation.ok();
    });
    vi.stubGlobal("fetch", fetcher);

    await expect(operation.call(controller.signal)).rejects.toThrow();
    expect(fetcher).toHaveBeenCalledTimes(1);
  });

  if (operation.decodes)
    it(`@s41 ${operation.name} stops when the abort lands while the body is read`, async () => {
      const controller = new AbortController();
      const answer = operation.ok();
      const late = {
        status: answer.status,
        json: async () => {
          controller.abort();
          return answer.json();
        },
      } as unknown as Response;
      vi.stubGlobal(
        "fetch",
        vi.fn(async () => late),
      );

      await expect(operation.call(controller.signal)).rejects.toThrow();
    });
}

// ------------------------------------------- @s41 la petición lleva señal y tipo de contenido

it("@s41 every operation hands its signal to the transport", async () => {
  for (const operation of operations) {
    const controller = new AbortController();
    const fetcher = stub(operation.ok());

    await operation.call(controller.signal);

    expect(fetcher.mock.calls[0][1].signal, operation.name).toBe(
      controller.signal,
    );
    vi.unstubAllGlobals();
  }
});

it("@s41 the two writes with a body declare application/json", async () => {
  const writes = [
    {
      ok: () => Response.json(connection),
      call: (s: AbortSignal) =>
        connectGithub({ repository: "octocat/Hello-World", token: "t" }, s),
    },
    {
      ok: () => Response.json(receipt, { status: 201 }),
      call: (s: AbortSignal) => startGithubImport(projectId, s),
    },
  ];
  for (const write of writes) {
    const fetcher = stub(write.ok());

    await write.call(signal());

    const headers = new Headers(fetcher.mock.calls[0][1].headers);
    expect(headers.get("Content-Type")).toBe("application/json");
    vi.unstubAllGlobals();
  }
});

// ------------------------------------------ @s39 el error tipado y sus contadores, campo a campo

/**
 * ConnectorError normaliza el problema RFC 7807. Cada rama tenía mutantes vivos:
 * el código por defecto, el mensaje, y el filtro de contadores, que sólo admite
 * enteros no negativos y devuelve null para todo lo demás.
 */
it("@s39 a problem without a usable code falls back to CONNECTOR_ERROR", async () => {
  for (const body of [{}, { code: 7 }, { code: null }]) {
    stub(problem(500, body));

    const error = await startGithubImport(projectId, signal()).catch(
      (raised) => raised,
    );

    expect(error).toBeInstanceOf(ConnectorError);
    expect(error.code).toBe("CONNECTOR_ERROR");
    expect(error.message).toBe("CONNECTOR_ERROR");
    vi.unstubAllGlobals();
  }
});

it("@s39 a problem with a code keeps it as code and as message", async () => {
  stub(problem(429, { code: "RATE_LIMITED" }));

  const error = await startGithubImport(projectId, signal()).catch(
    (raised) => raised,
  );

  expect(error.code).toBe("RATE_LIMITED");
  expect(error.message).toBe("RATE_LIMITED");
});

it("@s39 the counters of a problem only accept non negative integers", async () => {
  const cases = [
    { sent: 0, kept: 0 },
    { sent: 12, kept: 12 },
    { sent: -1, kept: null },
    { sent: 1.5, kept: null },
    { sent: "3", kept: null },
    { sent: null, kept: null },
  ];
  for (const { sent, kept } of cases) {
    stub(
      problem(409, {
        code: "IMPORT_FAILED",
        retryAfterSeconds: sent,
        created: sent,
        skipped: sent,
        failed: sent,
      }),
    );

    const error = await startGithubImport(projectId, signal()).catch(
      (raised) => raised,
    );

    expect(error.retryAfterSeconds, `retryAfterSeconds ${sent}`).toBe(kept);
    expect(error.created, `created ${sent}`).toBe(kept);
    expect(error.skipped, `skipped ${sent}`).toBe(kept);
    expect(error.failed, `failed ${sent}`).toBe(kept);
    vi.unstubAllGlobals();
  }
});

it("@s39 the importId of a problem survives only as a canonical uuid", async () => {
  const cases = [
    { sent: importId, kept: importId },
    { sent: importId.toUpperCase(), kept: importId.toUpperCase() },
    { sent: "no-es-uuid", kept: null },
    { sent: 7, kept: null },
  ];
  for (const { sent, kept } of cases) {
    stub(problem(409, { code: "IMPORT_FAILED", importId: sent }));

    const error = await startGithubImport(projectId, signal()).catch(
      (raised) => raised,
    );

    expect(error.importId, String(sent)).toBe(kept);
    vi.unstubAllGlobals();
  }
});

it("@s39 an unreadable problem body still yields the typed error", async () => {
  stub(
    new Response("no es json", {
      status: 503,
      headers: { "Content-Type": "application/problem+json" },
    }),
  );

  const error = await startGithubImport(projectId, signal()).catch(
    (raised) => raised,
  );

  expect(error).toBeInstanceOf(ConnectorError);
  expect(error.code).toBe("CONNECTOR_ERROR");
  expect(error.retryAfterSeconds).toBeNull();
});

// ------------------------------------------------- el contrato, atado a los dos lados

/**
 * La guarda que faltaba. La regresión de `a347936` fue **este** fichero contra
 * `GithubConnectorController`: el recibo compartido pasó de once claves con `repository` a doce
 * con `source` y `projectPath`, el cliente siguió exigiendo las once y `exact()` —que compara
 * cardinalidad— tumbó la sección entera. La prueba equivalente existía para GitLab, o sea que la
 * lección se había aprendido en el conector que no se rompió.
 *
 * <p>Ata las dos orillas leyendo la fuente Java. Lo que puede y lo que no puede prometer:
 * comprueba que el `record` tiene esos componentes, no que Jackson los serialice con esos nombres.
 * Un `@JsonProperty("repo")` la dejaría verde y rompería el cliente. Hoy no hay anotaciones, así
 * que el riesgo es teórico, pero es el techo de la técnica y conviene que esté escrito antes de
 * que alguien la crea infalible.
 *
 * <p>El ayudante está duplicado a propósito con el de `gitlab-connector-client.test.ts`: cada
 * prueba de contrato lee el fichero que vigila y no depende de ninguna otra. Extraerlo a un módulo
 * compartido es una mejora razonable para quien tenga los dos ficheros a la vez.
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
  "backend/src/main/java/com/apptolast/organization/adapter/http/GithubConnectorController.java";

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

it("@s16 decodes exactly the receipt the controller publishes", () => {
  expect(sorted(componentsOf("ImportResponse", controller()))).toEqual(
    sorted(RECEIPT_FIELDS.split(" ")),
  );
});

it("@s5 decodes exactly the connection the controller publishes", () => {
  expect(sorted(componentsOf("ConnectionResponse", controller()))).toEqual(
    sorted(CONNECTION_FIELDS.split(" ")),
  );
});
