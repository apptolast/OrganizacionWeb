import { afterEach, expect, it, vi } from "vitest";
import { setCsrfToken, observeAccess } from "./api-client";
import {
  ConnectorError,
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
  projectId,
  repository: "octocat/Hello-World",
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
