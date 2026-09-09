import { createServer } from "node:http";

/**
 * Servicio falso de GitHub para la pila de E2E (project-spec.md, feature 27).
 *
 * Comparte el espacio de red del backend (`network_mode: service:backend` en el compose), así que
 * el backend lo alcanza como `http://127.0.0.1:9000`, un host de loopback. Eso importa: la lista
 * blanca de `GithubApiBase` sólo admite api.github.com o loopback, y no se relaja para las
 * pruebas. Nada sale nunca hacia api.github.com.
 *
 * No hay canal de control: **la conducta depende del nombre del repositorio**, que es lo único que
 * la prueba escribe en la interfaz. Así cada recorrido es determinista y no hay estado compartido
 * entre pruebas que pueda filtrarse de una a otra.
 */

const PORT = Number(process.env.FAKE_GITHUB_PORT ?? 9000);
const LOGIN = "octocat";

/** Cada repositorio conocido describe qué debe contestar el falso. */
const REPOSITORIES = {
  "hello-world": { issues: 3, pullRequests: 1 },
  // Cien issues abiertas más una segunda página: ejercita la paginación y `truncated`.
  muchas: { issues: 201 },
  // Una issue con el título en blanco: el recibo termina con `failed 1`.
  "con-fallo": { issues: 2, blankTitleAt: 2 },
  vacio: { issues: 0 },
  limitado: { issuesStatus: 429, retryAfter: 90 },
  rechazado: { repositoryStatus: 401 },
  perdido: { repositoryStatus: 404 },
  caido: { issuesStatus: 500 },
};

function describe(name) {
  return REPOSITORIES[String(name).toLowerCase()] ?? null;
}

function issuesOf(spec, page) {
  const total = spec.issues ?? 0;
  const perPage = 100;
  const from = (page - 1) * perPage;
  const count = Math.max(0, Math.min(perPage, total - from));
  const items = [];
  for (let index = 0; index < count; index++) {
    const number = from + index + 1;
    items.push({
      id: 1000 + number,
      title:
        spec.blankTitleAt === number
          ? "   "
          : `Issue de prueba número ${number}`,
      body: `Cuerpo de la issue ${number}.\r\nSegunda línea.`,
      html_url: `https://github.com/octocat/Hello-World/issues/${number}`,
    });
  }
  if (page === 1 && spec.pullRequests)
    for (let index = 0; index < spec.pullRequests; index++)
      items.push({
        id: 9000 + index,
        title: "Esto es un pull request y debe descartarse",
        html_url: "https://github.com/octocat/Hello-World/pull/1",
        pull_request: { url: "https://api.github.com/pulls/1" },
      });
  const more = total > from + count;
  return { items, more };
}

function send(response, status, body, headers = {}) {
  const payload = JSON.stringify(body);
  response.writeHead(status, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(payload),
    ...headers,
  });
  response.end(payload);
}

const server = createServer((request, response) => {
  const url = new URL(request.url, "http://127.0.0.1");
  const path = url.pathname;

  if (path === "/user") return send(response, 200, { login: LOGIN });

  const repository = /^\/repos\/([^/]+)\/([^/]+)$/.exec(path);
  if (repository) {
    const spec = describe(repository[2]);
    if (!spec) return send(response, 404, { message: "Not Found" });
    if (spec.repositoryStatus && spec.repositoryStatus !== 200)
      return send(response, spec.repositoryStatus, { message: "Rejected" });
    return send(response, 200, {
      full_name: `${LOGIN}/${repository[2]}`,
      private: false,
    });
  }

  const issues = /^\/repos\/([^/]+)\/([^/]+)\/issues$/.exec(path);
  if (issues) {
    const spec = describe(issues[2]);
    if (!spec) return send(response, 404, { message: "Not Found" });
    if (spec.issuesStatus === 429)
      return send(
        response,
        429,
        { message: "Rate limited" },
        {
          "Retry-After": String(spec.retryAfter ?? 60),
        },
      );
    if (spec.issuesStatus && spec.issuesStatus !== 200)
      return send(response, spec.issuesStatus, { message: "Boom" });
    const page = Number(url.searchParams.get("page") ?? "1");
    const { items, more } = issuesOf(spec, page);
    const link = more
      ? {
          Link: `<http://127.0.0.1:${PORT}${path}?page=${page + 1}>; rel="next"`,
        }
      : {};
    return send(response, 200, items, link);
  }

  send(response, 404, { message: "Not Found" });
});

server.listen(PORT, "0.0.0.0", () => {
  process.stdout.write(`fake-github escuchando en ${PORT}\n`);
});
