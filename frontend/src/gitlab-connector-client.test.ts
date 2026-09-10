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

const AT = "2026-09-09T10:05:00.123456Z";

/**
 * Conectado pero roto. El servidor conserva los cinco campos de la conexión y añade el motivo:
 * es la forma que devuelve GitlabConnectionView cuando el token dejó de valer, y la única que
 * lleva `lastError` distinto de null. Ninguna prueba la decodificaba.
 */
const broken = {
  ...connected,
  status: "error",
  lastError: { code: "CONNECTION_INVALID", at: AT },
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

/**
 * Los siete campos que @s8 exige nulos sin conexión. Una fila `not_connected` que arrastre uno
 * solo de ellos es el residuo de la conexión anterior que @s32 y @s37 prohíben: la pantalla no
 * lo pintaría hoy —el panel exige `status !== "not_connected"`— pero el decodificador es la
 * única guarda que hay, y ninguna prueba la falsificaba.
 */
const RESIDUES: [string, unknown][] = [
  ["apiBase", "https://gitlab.example.com/api/v4"],
  ["projectPath", "grupo/proyecto"],
  ["projectId", 4821],
  ["tokenHint", "WXYZ"],
  ["lastActivityAt", AT],
  ["lastError", { code: "CONNECTION_INVALID", at: AT }],
  ["version", 1],
];

it.each(RESIDUES)(
  "@s8 refuses a not_connected row still dragging %s from the previous connection",
  async (field, residue) => {
    stub(Response.json({ ...notConnected, [field]: residue }));

    await expect(readGitlabConnection(signal())).rejects.toThrow(
      "Confirmación incompatible",
    );
  },
);

it("@s34 refuses a connected row whose project path is empty", async () => {
  stub(Response.json({ ...connected, projectPath: "" }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

/** Con la pista vacía el panel enseñaría «••••» y nada más: una pista que no distingue nada. */
it("@s35 refuses a connected row whose token hint is empty", async () => {
  stub(Response.json({ ...connected, tokenHint: "" }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 refuses a project identifier that is not a whole number", async () => {
  stub(Response.json({ ...connected, projectId: 4821.5 }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 refuses a version that is not a whole number", async () => {
  stub(Response.json({ ...connected, version: 1.5 }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 accepts a connection that has not been used yet", async () => {
  stub(Response.json({ ...connected, lastActivityAt: null }));

  await expect(readGitlabConnection(signal())).resolves.toEqual({
    ...connected,
    lastActivityAt: null,
  });
});

it("@s8 refuses a last activity that is not an instant", async () => {
  stub(Response.json({ ...connected, lastActivityAt: "ayer" }));

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

it("@s8 decodes a connection that is broken, keeping the code and the instant of the failure", async () => {
  stub(Response.json(broken));

  const view = await readGitlabConnection(signal());

  expect(view).toEqual(broken);
  expect(view.lastError).toEqual({ code: "CONNECTION_INVALID", at: AT });
});

/**
 * La propiedad de @s5, probada en el lado que decodifica la respuesta donde el token podría
 * colarse: un `lastError` con un tercer campo es exactamente el hueco por el que entraría el
 * texto del proveedor («invalid_token: glpat-…»). El decodificador es la única guarda.
 */
it("@s5 refuses a lastError that smuggles the provider's words in a third field", async () => {
  stub(
    Response.json({
      ...broken,
      lastError: {
        code: "CONNECTION_INVALID",
        at: AT,
        detail: "invalid_token: glpat-abcdef1234",
      },
    }),
  );

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 refuses a lastError with no code, which would leave «Error» without a reason", async () => {
  stub(Response.json({ ...broken, lastError: { code: "", at: AT } }));

  await expect(readGitlabConnection(signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s8 refuses a lastError whose instant is not one", async () => {
  stub(
    Response.json({
      ...broken,
      lastError: { code: "CONNECTION_INVALID", at: "ayer" },
    }),
  );

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
  expect(new Headers(options.headers).get("Content-Type")).toBe(
    "application/json",
  );
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
  expect(new Headers(options.headers).get("Content-Type")).toBe(
    "application/json",
  );
  expect(started).toEqual(receipt);
});

/**
 * Los identificadores del recibo son UUID en minúsculas: así es como el contrato los publica y
 * como se comparan los enlaces. Ninguna prueba mandaba uno en mayúsculas ni uno con la forma de
 * un UUID que no lo es.
 */
it("@s15 refuses a receipt whose identifier comes in upper case", async () => {
  stub(
    Response.json({ ...receipt, id: importId.toUpperCase() }, { status: 201 }),
  );

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s15 refuses an identifier with the shape of a uuid that is not one", async () => {
  stub(
    Response.json(
      { ...receipt, id: "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeez" },
      { status: 201 },
    ),
  );

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s15 refuses a receipt whose source is not gitlab", async () => {
  stub(Response.json({ ...receipt, source: "github" }, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

const running = {
  ...receipt,
  status: "running",
  created: 0,
  skipped: 0,
  failed: 0,
  errorCode: null,
  finishedAt: null,
};

it("@s27 refuses a running receipt that already carries an error", async () => {
  stub(
    Response.json(
      { ...running, errorCode: "CONNECTION_INVALID" },
      {
        status: 201,
      },
    ),
  );

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s27 refuses a running receipt that already has an ending", async () => {
  stub(
    Response.json(
      { ...running, finishedAt: "2026-09-09T12:00:04.123456Z" },
      { status: 201 },
    ),
  );

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

/** @s21: un token rechazado durante la importación deja recibo failed con errorCode y final. */
it("@s21 accepts a failed receipt with the code that explains it", async () => {
  const stopped = {
    ...receipt,
    status: "failed",
    created: 0,
    skipped: 0,
    failed: 0,
    errorCode: "CONNECTION_INVALID",
  };
  stub(Response.json(stopped, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).resolves.toEqual(
    stopped,
  );
});

it("@s15 refuses a receipt whose errorCode is an empty string", async () => {
  stub(Response.json({ ...receipt, errorCode: "" }, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s16 refuses a receipt whose truncated is not a yes or a no", async () => {
  stub(Response.json({ ...receipt, truncated: "sí" }, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

it("@s15 refuses a receipt that ends before it starts", async () => {
  stub(
    Response.json(
      { ...receipt, finishedAt: "2026-09-09T11:59:59.123456Z" },
      { status: 201 },
    ),
  );

  await expect(startGitlabImport(projectId, signal())).rejects.toThrow(
    "Confirmación incompatible",
  );
});

/** La frontera: empezar y terminar en el mismo microsegundo es una importación instantánea. */
it("@s15 accepts a receipt that ends in the very microsecond it started", async () => {
  const instantaneous = { ...receipt, finishedAt: receipt.startedAt };
  stub(Response.json(instantaneous, { status: 201 }));

  await expect(startGitlabImport(projectId, signal())).resolves.toEqual(
    instantaneous,
  );
});

it("@s15 refuses a receipt carrying a thirteenth field", async () => {
  stub(Response.json({ ...receipt, tokenHint: "WXYZ" }, { status: 201 }));

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

/**
 * Un recibo en curso: el @s27 del contrato lo da por existente («un recibo de gitlab en status
 * running») y el @s30 lo recupera por su id. Es el único que carece de final y de error, y
 * ninguna prueba lo decodificaba: las dos fixtures eran `completed`.
 */
it("@s30 reads back an import still running, with no ending and no error", async () => {
  stub(Response.json(running));

  await expect(readGitlabImport(importId, signal())).resolves.toEqual(running);
});

/** @s30: el recibo ajeno y el inexistente dan el mismo 404, y la pantalla necesita el código. */
it("@s30 turns a receipt that is not there into the typed error, not into an incompatible body", async () => {
  stub(problem(404, { code: "IMPORT_NOT_FOUND" }));

  const error = (await readGitlabImport(importId, signal()).catch(
    (caught: unknown) => caught,
  )) as GitlabConnectorError;

  expect(error).toBeInstanceOf(GitlabConnectorError);
  expect(error.code).toBe("IMPORT_NOT_FOUND");
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
  // Lo único que se ve cuando el error escapa de la pantalla es su nombre y su mensaje.
  expect((error as GitlabConnectorError).name).toBe("GitlabConnectorError");
  expect((error as GitlabConnectorError).message).toBe("RATE_LIMITED");
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
  expect(error.message).toBe("CONNECTOR_ERROR");
  expect(error.retryAfterSeconds).toBeNull();
});

/**
 * El mapa campo → código es lo que enciende el `aria-invalid` del control que el servidor
 * rechazó. El único cuerpo con `errors` que se probaba traía una entrada bien formada, así que
 * las tres condiciones del filtro se cumplían a la vez y ninguna se podía falsificar.
 */
it("@s34 marks the two fields when the server complains about both", async () => {
  stub(
    problem(400, {
      code: "VALIDATION_ERROR",
      errors: [
        { field: "token", code: "REQUIRED" },
        { field: "projectPath", code: "INVALID_FORMAT" },
      ],
    }),
  );

  const error = (await connectGitlab(
    { token: "", projectPath: "x" },
    signal(),
  ).catch((caught: unknown) => caught)) as GitlabConnectorError;

  expect(error.fields).toEqual({
    token: "REQUIRED",
    projectPath: "INVALID_FORMAT",
  });
});

it("@s34 keeps out of the map the entries that are not a field and a code", async () => {
  stub(
    problem(400, {
      code: "VALIDATION_ERROR",
      errors: [
        null,
        { field: "", code: "SIN_CAMPO" },
        { field: "token", code: "" },
        { field: "projectPath", code: "INVALID_FORMAT" },
      ],
    }),
  );

  const error = (await connectGitlab(
    { token: "", projectPath: "x" },
    signal(),
  ).catch((caught: unknown) => caught)) as GitlabConnectorError;

  expect(error.fields).toEqual({ projectPath: "INVALID_FORMAT" });
});

it("@s34 ignores an errors that is not even a list", async () => {
  stub(
    problem(400, { code: "VALIDATION_ERROR", errors: { token: "REQUIRED" } }),
  );

  const error = (await connectGitlab(
    { token: "", projectPath: "x" },
    signal(),
  ).catch((caught: unknown) => caught)) as GitlabConnectorError;

  expect(error.fields).toEqual({});
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

/**
 * Las cinco llamadas del cliente, cada una con sus tres paradas de aborto: antes de pedir,
 * después de la respuesta y después de leer el cuerpo. Ninguna prueba distinguía una parada de
 * otra: bastaba con que la promesa acabara rechazando, y eso lo consigue cualquiera de las tres.
 * Aquí se afirma qué NO llegó a pasar en cada parada, que es lo que el aborto promete.
 */
type Call = [
  string,
  (signal: AbortSignal) => Promise<unknown>,
  number,
  unknown,
];

const CALLS: Call[] = [
  ["readGitlabConnection", (as) => readGitlabConnection(as), 200, connected],
  [
    "connectGitlab",
    (as) => connectGitlab({ token: TOKEN, projectPath: "grupo/proyecto" }, as),
    200,
    connected,
  ],
  ["disconnectGitlab", (as) => disconnectGitlab(as), 204, null],
  ["startGitlabImport", (as) => startGitlabImport(projectId, as), 201, receipt],
  ["readGitlabImport", (as) => readGitlabImport(importId, as), 200, receipt],
];

const WITH_BODY = CALLS.filter(([name]) => name !== "disconnectGitlab");

it.each(CALLS)(
  "@s37 %s asks the server for nothing when the caller already aborted",
  async (_name, call) => {
    const controller = new AbortController();
    const fetcher = stub();
    controller.abort();

    await expect(call(controller.signal)).rejects.toThrow();
    expect(fetcher).not.toHaveBeenCalled();
  },
);

it.each(CALLS)(
  "@s37 %s does not read the body of a response that landed after the abort",
  async (_name, call, status, payload) => {
    const controller = new AbortController();
    const json = vi.fn(() => Promise.resolve(payload));
    vi.stubGlobal(
      "fetch",
      vi.fn(() => {
        controller.abort();
        return Promise.resolve({ status, json });
      }),
    );

    await expect(call(controller.signal)).rejects.toThrow();
    expect(json).not.toHaveBeenCalled();
  },
);

it.each(WITH_BODY)(
  "@s37 %s decodes nothing when the abort lands while the body is being read",
  async (_name, call, status, payload) => {
    const controller = new AbortController();
    vi.stubGlobal(
      "fetch",
      vi.fn(() =>
        Promise.resolve({
          status,
          json: () => {
            controller.abort();
            return Promise.resolve(payload);
          },
        }),
      ),
    );

    await expect(call(controller.signal)).rejects.toThrow();
  },
);

/** Sin la señal en la petición, salir de la pantalla no cancela nada: se queda en vuelo. */
it("@s37 sends the caller's own signal with the read of the connection", async () => {
  const controller = new AbortController();
  const fetcher = stub(Response.json(connected));

  await readGitlabConnection(controller.signal);

  expect((fetcher.mock.calls[0][1] as RequestInit).signal).toBe(
    controller.signal,
  );
});

it("@s37 sends the caller's own signal with the read of a receipt", async () => {
  const controller = new AbortController();
  const fetcher = stub(Response.json(receipt));

  await readGitlabImport(importId, controller.signal);

  expect((fetcher.mock.calls[0][1] as RequestInit).signal).toBe(
    controller.signal,
  );
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
