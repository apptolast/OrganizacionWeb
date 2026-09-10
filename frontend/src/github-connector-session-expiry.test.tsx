import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { observeAccess, setCsrfToken } from "./api-client";
import { GithubConnector } from "./github-connector";

/**
 * @s41, cuarta fila: «la sesión vence en mitad de la importación → veo la pantalla de acceso sin
 * datos privados y ninguna petición se reintenta». Ninguna prueba del conector la ejercía: los tres
 * 401 de la pantalla llevan código GITHUB_TOKEN_REJECTED, que es otra cosa —el token de GitHub, no
 * la sesión—, y la fila descansaba entera en el mecanismo global de api-client.
 *
 * <p>Aquí se mide ese mecanismo desde el conector: un 401 de sesión durante la importación avisa a
 * quien gobierna el acceso —que es quien pinta la pantalla de acceso y desmonta lo privado— y la
 * petición no se repite.
 */

const projectId = "11111111-2222-4333-8444-555555555555";

const connection = {
  repository: "octocat/Hello-World",
  login: "octocat",
  status: "valid",
  connectedAt: "2026-09-09T10:00:00.123456Z",
  lastImport: null,
};

const projects = {
  items: [
    {
      id: projectId,
      name: "Primero",
      status: "idea",
      createdAt: "2026-09-01T08:00:00.000000Z",
      updatedAt: "2026-09-01T08:00:00.000000Z",
    },
  ],
  nextCursor: null,
};

const IMPORTS = "/api/v1/me/connectors/github/imports";

type Route = (options: RequestInit) => Response | Promise<Response>;
let routes: Record<string, Route>;
let calls: { url: string; method: string }[];
let lostAccess: (401 | 403)[];

function serve(url: string, method: string, route: Route) {
  routes[`${method} ${url}`] = route;
}

function callsTo(url: string, method: string) {
  return calls.filter(
    (call) => call.url.split("?")[0] === url && call.method === method,
  );
}

beforeEach(() => {
  calls = [];
  routes = {};
  lostAccess = [];
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
  observeAccess((status) => lostAccess.push(status));
});

afterEach(() => {
  vi.unstubAllGlobals();
  setCsrfToken();
  observeAccess();
});

async function open() {
  render(<GithubConnector owner="owner" />);
  await screen.findByRole("heading", { level: 1, name: "Conector de GitHub" });
}

it("@s41 an expired session in the middle of the import hands over to the access screen", async () => {
  serve(IMPORTS, "POST", () => new Response(null, { status: 401 }));
  await open();
  await screen.findByText("octocat/Hello-World");

  await userEvent.click(
    screen.getByRole("button", { name: "Importar issues abiertas" }),
  );

  expect(lostAccess).toEqual([401]);
});

it("@s41 an expired session in the middle of the import retries nothing", async () => {
  serve(IMPORTS, "POST", () => new Response(null, { status: 401 }));
  await open();
  await screen.findByText("octocat/Hello-World");

  await userEvent.click(
    screen.getByRole("button", { name: "Importar issues abiertas" }),
  );

  expect(callsTo(IMPORTS, "POST")).toHaveLength(1);
  expect(callsTo(`${IMPORTS}/undefined`, "GET")).toHaveLength(0);
  expect(document.body.innerHTML).not.toContain("csrf-own-session");
});
