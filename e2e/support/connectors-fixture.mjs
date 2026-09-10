/**
 * Andamiaje compartido por las dos revisiones de @s38 —la de anchos y texto
 * (`additional-connectors-ux.spec.mjs`) y la de zoom nativo
 * (`additional-connectors-native-zoom.spec.mjs`)—: las cotas del contrato y la API
 * simulada de las dos pantallas de la feature 29.
 *
 * La API se simula desde el navegador, como en `e2e/webhooks-ux.spec.mjs`. No es una
 * comodidad: el Given de @s38 exige «GitLab connected y un recibo reciente truncated
 * true», y la pila de E2E no levanta ningún GitLab falso —`docker-compose.yml` sólo
 * declara `github-fake`, y `APP_GITLAB_API_BASE` no existe en `scripts/e2e.mjs`—, así
 * que por la interfaz real ese estado es inalcanzable. Lo que se audita es la interfaz
 * de verdad en un navegador de verdad; la aceptación del backend la cubre la suite JVM.
 */

/** Los tres anchos que nombra el escenario, ni uno más. */
export const WIDTHS = [320, 768, 1280];

/** Objetivo táctil del producto (`docs/ux-requirements.md`), no una cifra de la ley de Fitts. */
export const MIN_TARGET = 44;

export const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"];

const PROJECT_ID = "11111111-1111-4111-8111-111111111111";
const SECOND_PROJECT_ID = "22222222-2222-4222-8222-222222222222";
const IMPORT_ID = "33333333-3333-4333-8333-333333333333";

/**
 * Ruta larga y sin espacios, como las que GitLab admite de verdad. Una ruta corta no
 * demuestra nada: lo que puede desbordar a 320 px es exactamente esto.
 */
export const PROJECT_PATH =
  "plataforma-de-integraciones/servicios-compartidos/pasarela-de-eventos-internos";

export const connection = (overrides = {}) => ({
  status: "connected",
  apiBase: "https://gitlab.com/api/v4",
  projectPath: PROJECT_PATH,
  projectId: 90210,
  tokenHint: "9f4c",
  lastActivityAt: "2026-09-08T10:00:00.000000Z",
  lastError: null,
  version: 3,
  ...overrides,
});

export const DISCONNECTED = {
  status: "not_connected",
  apiBase: null,
  projectPath: null,
  projectId: null,
  tokenHint: null,
  lastActivityAt: null,
  lastError: null,
  version: null,
};

/** El recibo del Given: `truncated: true`, con su aviso de «quedaron issues sin traer». */
export const receipt = (overrides = {}) => ({
  id: IMPORT_ID,
  source: "gitlab",
  projectId: PROJECT_ID,
  projectPath: PROJECT_PATH,
  status: "completed",
  created: 128,
  skipped: 47,
  failed: 3,
  truncated: true,
  errorCode: null,
  startedAt: "2026-09-08T10:00:00.000000Z",
  finishedAt: "2026-09-08T10:02:31.500000Z",
  ...overrides,
});

/**
 * Las seis filas del catálogo, en el orden que el cliente exige. Se reparten los cuatro
 * estados y se incluye una con `lastError` y otra sin actividad: así la auditoría mide
 * todas las variantes de fila que la pantalla sabe pintar, no la más corta.
 */
export const CATALOG = [
  {
    id: "api_credentials",
    status: "connected",
    lastActivityAt: "2026-09-09T08:15:00.000000Z",
    lastError: null,
  },
  {
    id: "webhooks",
    status: "error",
    lastActivityAt: "2026-09-09T07:00:00.000000Z",
    lastError: {
      code: "DELIVERY_EXHAUSTED",
      at: "2026-09-09T07:00:00.000000Z",
    },
  },
  {
    id: "ics_calendar",
    status: "not_connected",
    lastActivityAt: null,
    lastError: null,
  },
  {
    id: "github",
    status: "disabled",
    lastActivityAt: null,
    lastError: null,
  },
  {
    id: "external_calendar",
    status: "error",
    lastActivityAt: "2026-09-09T06:30:00.000000Z",
    lastError: {
      code: "FEED_UNSUPPORTED_TYPE",
      at: "2026-09-09T06:30:00.000000Z",
    },
  },
  {
    id: "gitlab",
    status: "connected",
    lastActivityAt: "2026-09-09T09:45:00.000000Z",
    lastError: null,
  },
];

const PROJECTS = {
  items: [
    {
      id: PROJECT_ID,
      name: "Pasarela de eventos internos",
      status: "active",
      createdAt: "2026-01-02T09:00:00.000Z",
      updatedAt: "2026-09-01T09:00:00.000Z",
    },
    {
      id: SECOND_PROJECT_ID,
      name: "Migración del catálogo de integraciones a la nueva pasarela",
      status: "active",
      createdAt: "2026-01-03T09:00:00.000Z",
      updatedAt: "2026-09-02T09:00:00.000Z",
    },
  ],
  nextCursor: null,
};

const problem = (code, status) => ({
  status,
  contentType: "application/problem+json",
  body: JSON.stringify({
    type: `urn:organization:problem:${code.toLowerCase()}`,
    title: code,
    status,
    code,
  }),
});

/** Enruta cada llamada que las dos pantallas pueden hacer; `control` decide qué contestan. */
export async function simulate(page, control) {
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const method = request.method();
    if (path === "/api/session" && method === "GET")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance" && method === "GET")
      return route.fulfill({
        headers: { ETag: '"appearance:unconfigured"' },
        json: {
          configured: false,
          theme: "SYSTEM",
          accentLight: "#244C3C",
          accentDark: "#B7E4C7",
          updatedAt: null,
        },
      });
    if (path === "/api/v1/projects" && method === "GET")
      return route.fulfill({ json: PROJECTS });
    if (path === "/api/v1/me/connectors" && method === "GET") {
      if (control.catalogFails)
        return route.fulfill(problem("STORAGE_UNAVAILABLE", 503));
      return route.fulfill({ json: { connectors: CATALOG } });
    }
    if (path === "/api/v1/me/connectors/gitlab" && method === "GET")
      return route.fulfill({ json: control.connection });
    if (path === "/api/v1/me/connectors/gitlab" && method === "PUT") {
      if (control.connectDelayMs)
        await new Promise((done) => setTimeout(done, control.connectDelayMs));
      if (control.connectFails)
        return route.fulfill(problem(control.connectFails, 422));
      control.connection = connection();
      return route.fulfill({ json: control.connection });
    }
    if (path === "/api/v1/me/connectors/gitlab" && method === "DELETE") {
      control.connection = DISCONNECTED;
      return route.fulfill({ status: 204, body: "" });
    }
    if (path === "/api/v1/me/connectors/gitlab/imports" && method === "POST") {
      if (control.importDelayMs)
        await new Promise((done) => setTimeout(done, control.importDelayMs));
      return route.fulfill({ status: 201, json: receipt() });
    }
    return route.fulfill({ status: 204, body: "" });
  });
}

export function newControl() {
  return {
    catalogFails: false,
    connectDelayMs: 0,
    connectFails: null,
    connection: connection(),
    importDelayMs: 0,
  };
}
