import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

/**
 * @s38 de `features/additional_connectors.feature`: las dos pantallas de la feature 29
 * —el catálogo en `/conectores` y el conector de GitLab en `/conectores/gitlab`— a 320,
 * 768 y 1280 px CSS, con texto al 200 %, objetivos de 44 × 44, recorrido de teclado con
 * foco visible en cada parada y axe en los tres anchos.
 *
 * La API se simula desde el navegador, como en `e2e/webhooks-ux.spec.mjs`. No es una
 * comodidad: el Given exige «GitLab connected y un recibo reciente truncated true», y la
 * pila de E2E no levanta ningún GitLab falso (`docker-compose.yml` sólo declara
 * `github-fake`, y `APP_GITLAB_API_BASE` no existe en `scripts/e2e.mjs`), así que por la
 * interfaz real ese estado es inalcanzable. Lo que se audita aquí es la interfaz de
 * verdad en un navegador de verdad; la aceptación del backend la cubre la suite JVM.
 *
 * El zoom NATIVO no se mide aquí —redimensionar el viewport no es ampliar—: vive en
 * `e2e/additional-connectors-native-zoom.spec.mjs`.
 */

/** Nombre de fichero a partir del estado, sin acentos ni espacios. */
const slug = (state) =>
  state
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/gi, "-");

/** Los tres anchos que nombra el escenario, ni uno más. */
const WIDTHS = [320, 768, 1280];

/** Objetivo táctil del producto (`docs/ux-requirements.md`), no una cifra de la ley de Fitts. */
const MIN_TARGET = 44;

/** Grosor del anillo que declara `:focus-visible` en `frontend/src/styles.scss:625`. */
const FOCUS_RING_MIN_WIDTH = 3;

/** Cota del recorrido: la barra lateral y el enlace de salto se interponen antes del contenido. */
const MAX_TAB_STEPS = 140;

const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"];

const PROJECT_ID = "11111111-1111-4111-8111-111111111111";
const SECOND_PROJECT_ID = "22222222-2222-4222-8222-222222222222";
const IMPORT_ID = "33333333-3333-4333-8333-333333333333";

/**
 * Ruta larga y sin espacios, como las que GitLab admite de verdad. Una ruta corta no
 * demuestra nada: lo que puede desbordar a 320 px es exactamente esto.
 */
const PROJECT_PATH =
  "plataforma-de-integraciones/servicios-compartidos/pasarela-de-eventos-internos";

const connection = (overrides = {}) => ({
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

const DISCONNECTED = {
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
const receipt = (overrides = {}) => ({
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
const CATALOG = [
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
async function simulate(page, control) {
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

/**
 * Mide un estado en los tres anchos y pasa axe en cada uno.
 *
 * Los objetivos táctiles se miden sobre la etiqueta cuando el control es una casilla,
 * que es lo que un dedo pulsa de verdad.
 */
function auditor(page, folder, options = {}) {
  const evidence = [];
  return async function audit(state) {
    if (options.text200) {
      const scales = await page.evaluate(() => {
        const nodes = [...document.querySelectorAll("main,main *")].filter(
          (node) => node instanceof HTMLElement,
        );
        // Se restaura el tamaño en línea original ANTES de doblar. Sin esto el factor
        // se compone estado tras estado y la auditoría acabaría midiendo un texto al
        // 800 % que nadie pidió, suspendiendo al producto por culpa del arnés.
        window.__fonts ??= new WeakMap();
        for (const node of nodes) {
          if (!window.__fonts.has(node))
            window.__fonts.set(node, node.style.fontSize);
          node.style.fontSize = window.__fonts.get(node);
        }
        const before = nodes.map((node) =>
          parseFloat(getComputedStyle(node).fontSize),
        );
        nodes.forEach((node, index) => {
          node.style.fontSize = `${before[index] * 2}px`;
        });
        return nodes.map((node, index) => ({
          before: before[index],
          after: parseFloat(getComputedStyle(node).fontSize),
        }));
      });
      // Que el navegador haya aplicado de verdad el doble; si no, lo que sigue no mide
      // el texto al 200 % sino el texto normal con otro nombre.
      for (const size of scales)
        expect(size.after).toBeCloseTo(size.before * 2, 3);
      await writeFile(
        `${folder}/${slug(state)}-font-scale.json`,
        JSON.stringify(scales, null, 2),
      );
    }

    for (const width of WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      const measured = await page.evaluate(() => {
        const target = (element) =>
          element instanceof HTMLInputElement && element.type === "checkbox"
            ? (element.closest("label") ?? element)
            : element;
        const controls = [
          ...document.querySelectorAll(
            'nav[aria-label="Principal"] a,main button,main a,main input,main select,main textarea',
          ),
        ].filter((element) => element.getClientRects().length);
        // Lo que asome por fuera del viewport se nombra: así un fallo dice QUÉ
        // desbordó, no sólo por cuánto.
        const offenders = [...document.querySelectorAll("body *")]
          .filter((element) => element.getClientRects().length)
          .map((element) => ({
            tag: element.tagName,
            className: element.className?.toString?.() ?? "",
            right: element.getBoundingClientRect().right,
            scrollWidth: element.scrollWidth,
            clientWidth: element.clientWidth,
            // Un bloque puede no asomar por el borde y aun asi desbordar por dentro: es
            // lo que hace un titulo de una sola palabra a 320 px con el texto al 200 %.
            // Sin esta rama el mensaje de fallo salia con la lista de culpables vacia.
            overflowing: element.scrollWidth > element.clientWidth + 1,
            text: (element.textContent ?? "").slice(0, 40),
          }))
          .filter((entry) => entry.right > innerWidth + 1 || entry.overflowing)
          .sort((left, right) => right.right - left.right)
          .slice(0, 6);
        // «ni acciones o cifras del recibo recortadas». Conjunto NOMBRADO, uno por
        // sujeto del contrato: las acciones (botones y enlaces), las cifras y los
        // valores de las dos listas de definición —el recibo y la conexión— y el
        // texto de cada fila del catálogo. Nunca `body *`: hay contenido oculto a
        // propósito (`.skip-link`) que haría fallar código correcto.
        // INPUT y SELECT quedan fuera porque Chromium les impone `overflow: clip`
        // en su propia hoja: su recorte no lo decide este producto. Su geometría sí
        // se mide, en `controls`.
        const clipped = [
          ...document.querySelectorAll(
            "main button, main a, main dd, main dt, main li h2, main li span, main section p, main li p",
          ),
        ]
          .filter((element) => element.getClientRects().length)
          .map((element) => ({
            what: `${element.tagName}.${element.className?.toString?.() ?? ""}`,
            text: (element.textContent ?? "").slice(0, 40),
            scrollWidth: element.scrollWidth,
            clientWidth: element.clientWidth,
            scrollHeight: element.scrollHeight,
            clientHeight: element.clientHeight,
          }))
          .filter(
            (entry) =>
              entry.scrollWidth > entry.clientWidth + 1 ||
              entry.scrollHeight > entry.clientHeight + 1,
          );
        return {
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          offenders,
          clipped,
          controls: controls.map((element) => {
            const box = target(element).getBoundingClientRect();
            return {
              name: (
                element.getAttribute("aria-label") ||
                element.labels?.[0]?.textContent ||
                element.textContent ||
                element.type ||
                ""
              )
                .trim()
                .slice(0, 48),
              x: box.x,
              y: box.y,
              width: box.width,
              height: box.height,
            };
          }),
        };
      });
      evidence.push({ state, mode: options.mode ?? "normal", ...measured });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(evidence, null, 2),
      );

      const where = `${state} @ ${width}px (${options.mode ?? "normal"})`;
      expect(
        measured.scroll,
        `${where}: desbordamiento horizontal; culpables=${JSON.stringify(measured.offenders)}`,
      ).toBeLessThanOrEqual(width);
      // El recorte POR ELEMENTO no lo cubre la aserción de arriba: un elemento que
      // recorta no ensancha la página, precisamente porque se recorta.
      expect(measured.clipped, `${where}: contenido recortado`).toEqual([]);
      for (const box of measured.controls) {
        expect(
          box.x,
          `${where}: «${box.name}» sale por la izquierda`,
        ).toBeGreaterThanOrEqual(0);
        expect(
          box.x + box.width,
          `${where}: «${box.name}» sale por la derecha`,
        ).toBeLessThanOrEqual(width + 1);
        expect(
          box.width,
          `${where}: «${box.name}» ancho del objetivo`,
        ).toBeGreaterThanOrEqual(MIN_TARGET);
        expect(
          box.height,
          `${where}: «${box.name}» alto del objetivo`,
        ).toBeGreaterThanOrEqual(MIN_TARGET);
      }

      // axe EN LOS TRES ANCHOS, que es lo que pide el escenario: una pasada única a
      // 320 px no dice nada del reflujo de 768 ni de 1280.
      const analyzer = new AxeBuilder({ page }).withTags(AXE_TAGS);
      // En `forced-colors` el sistema operativo sustituye toda la paleta, así que el
      // contraste no es nuestro para responder. No se desactiva en claro ni en oscuro.
      if (options.mode === "forced-colors")
        analyzer.disableRules(["color-contrast"]);
      const axe = await analyzer.analyze();
      await writeFile(
        `${folder}/${slug(state)}-${width}-axe.json`,
        JSON.stringify(axe.violations, null, 2),
      );
      expect(axe.violations, `${where}: axe`).toEqual([]);

      if (width === 320 || width === 1280)
        await page.screenshot({
          path: `${folder}/${slug(state)}-${width}.png`,
          fullPage: true,
        });
    }
  };
}

/** Los dos estados de `/conectores`: el catálogo completo y el catálogo ilegible. */
async function walkCatalog(page, audit, control) {
  const view = page.getByRole("main");

  control.catalogFails = false;
  await page.goto("/conectores");
  await expect(
    view.getByRole("heading", { level: 1, name: "Conectores" }),
  ).toBeVisible();
  await expect(view.getByRole("list", { name: "Conectores" })).toBeVisible();
  await expect(view.getByRole("listitem")).toHaveCount(CATALOG.length);
  await audit("catálogo");

  control.catalogFails = true;
  await page.goto("/conectores");
  await expect(view.getByRole("alert")).toHaveText(
    "No se pudo consultar el estado. Inténtalo más tarde",
  );
  await audit("catálogo ilegible");
  control.catalogFails = false;
}

/** Los cinco estados de `/conectores/gitlab`, empezando por el Given del escenario. */
async function walkGitlab(page, audit, control) {
  const view = page.getByRole("main");
  const button = (name) => view.getByRole("button", { name, exact: true });

  control.connection = connection();
  await page.goto("/conectores/gitlab");
  await expect(
    view.getByRole("heading", { level: 1, name: "Conector de GitLab" }),
  ).toBeVisible();
  await expect(view.getByRole("region", { name: "Conexión" })).toBeVisible();
  await audit("conectado");

  // El Given completo: recibo reciente con `truncated: true`.
  await button("Importar issues").click();
  const result = view.getByRole("region", {
    name: "Resultado de la importación",
  });
  await expect(result).toBeVisible();
  await expect(result.getByText("128")).toBeVisible();
  await expect(result.getByText("Sí")).toBeVisible();
  await audit("recibo truncado");

  await button("Desconectar").click();
  await expect(
    view.getByRole("group", { name: "Confirmar desconexión" }),
  ).toBeVisible();
  await audit("confirmando la desconexion");

  await button("Confirmar desconexión").click();
  await expect(view.getByLabel("Ruta del proyecto")).toBeVisible();
  await audit("desconectado");

  control.connectFails = "VALIDATION_ERROR";
  await view.getByLabel("Ruta del proyecto").fill("grupo/proyecto");
  await view.getByLabel("Token de acceso personal").fill("glpat-xxxxxxxxxxxx");
  await button("Conectar").click();
  await expect(view.getByRole("alert")).toHaveText(
    "Revisa la ruta del proyecto y el token",
  );
  await audit("error al conectar");
  control.connectFails = null;
}

function newControl() {
  return {
    catalogFails: false,
    connectDelayMs: 0,
    connectFails: null,
    connection: connection(),
    importDelayMs: 0,
  };
}

for (const mode of ["light", "dark", "text200"]) {
  test(`conectores: catálogo y GitLab en los tres anchos con ${mode} @s38`, async ({
    page,
  }) => {
    test.setTimeout(360_000);
    const folder = resolve(".e2e-work", "additional-connectors-ux", mode);
    await mkdir(folder, { recursive: true });
    const control = newControl();
    await simulate(page, control);
    if (mode !== "text200") await page.emulateMedia({ colorScheme: mode });
    const audit = auditor(page, folder, {
      mode,
      text200: mode === "text200",
    });
    await walkCatalog(page, audit, control);
    await walkGitlab(page, audit, control);
  });
}

/**
 * Los controles del contenido, leídos del documento y en orden del DOM. Nunca una lista
 * escrita a mano: así el oráculo compara el recorrido de teclado contra el orden del DOM
 * —que es lo que pide el escenario— y no contra la opinión de quien escribió la prueba.
 *
 * Se devuelven DOS listas a propósito. Si sólo se derivara la de los tabulables, poner
 * `tabindex="-1"` a un botón lo sacaría a la vez de la expectativa y del recorrido, y la
 * prueba seguiría verde con un control inalcanzable: el agujero exacto que @s38 prohíbe.
 */
function controlsOf(page) {
  return page.evaluate(() => {
    const named = (element) =>
      (element.labels?.[0]?.textContent ?? element.textContent ?? "").trim();
    const visible = [
      ...document.querySelectorAll(
        "main a, main button, main input, main select, main textarea",
      ),
    ].filter((element) => element.getClientRects().length && !element.disabled);
    return {
      visible: visible.map(named),
      reachable: visible.filter((element) => element.tabIndex >= 0).map(named),
    };
  });
}

function currentStop(page) {
  return page.evaluate(() => {
    const active = document.activeElement;
    if (!active || active === document.body) return null;
    const style = getComputedStyle(active);
    return {
      name: (
        active.labels?.[0]?.textContent ??
        active.textContent ??
        ""
      ).trim(),
      insideMain: Boolean(active.closest("main")),
      // `matches(":focus-visible")` es la pregunta correcta.
      // `getComputedStyle(el, ":focus-visible")` NO lo es: esa API espera un
      // pseudo-ELEMENTO y con una pseudo-clase devuelve el estilo del elemento sin
      // más, de modo que la comparación sale siempre verdadera. Sería un no-op.
      matchesFocusVisible: active.matches(":focus-visible"),
      outlineStyle: style.outlineStyle,
      outlineWidth: parseFloat(style.outlineWidth),
      outlineColor: style.outlineColor,
    };
  });
}

/**
 * Se exige el anillo del PRODUCTO y no el del agente de usuario, que Chromium computa con
 * `outline-style: auto` y que un `outline: none` del producto no apagaría.
 */
function ringIsVisible(stop) {
  return (
    stop.matchesFocusVisible &&
    stop.outlineStyle === "solid" &&
    stop.outlineWidth >= FOCUS_RING_MIN_WIDTH &&
    !stop.outlineColor.includes("transparent")
  );
}

async function walk(page, key, expected) {
  const seen = [];
  const invisible = [];
  for (let step = 0; step < MAX_TAB_STEPS && seen.length < expected.length;) {
    await page.keyboard.press(key);
    step += 1;
    const stop = await currentStop(page);
    if (!stop || !stop.insideMain) continue;
    if (!expected.includes(stop.name) || seen.at(-1) === stop.name) continue;
    seen.push(stop.name);
    // Una medida por parada, no una al final: el escenario pide foco visible en CADA
    // control, así que un anillo apagado en el primero tiene que doler.
    if (!ringIsVisible(stop)) invisible.push(stop);
  }
  return { seen, invisible };
}

for (const screen of ["catálogo", "gitlab"]) {
  test(`conectores: el teclado recorre ${screen} en el orden del DOM, ida y vuelta, con foco visible en cada parada @s38`, async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const folder = resolve(".e2e-work", "additional-connectors-ux", "keyboard");
    await mkdir(folder, { recursive: true });
    const control = newControl();
    await simulate(page, control);
    await page.setViewportSize({ width: 1280, height: 1000 });
    const view = page.getByRole("main");

    if (screen === "catálogo") {
      await page.goto("/conectores");
      await expect(view.getByRole("listitem")).toHaveCount(CATALOG.length);
    } else {
      await page.goto("/conectores/gitlab");
      await expect(
        view.getByRole("region", { name: "Conexión" }),
      ).toBeVisible();
      // Con el recibo a la vista, que es el estado del Given y el que más controles tiene.
      await view.getByRole("button", { name: "Importar issues" }).click();
      await expect(
        view.getByRole("region", { name: "Resultado de la importación" }),
      ).toBeVisible();
    }

    const controls = await controlsOf(page);
    await writeFile(
      `${folder}/${slug(screen)}-tab-order.json`,
      JSON.stringify(controls, null, 2),
    );
    // Ningún control visible se ha sacado de la secuencia con un tabindex negativo.
    expect(controls.reachable).toEqual(controls.visible);
    const expected = controls.visible;
    // Comparar por nombre sólo dice la verdad si los nombres distinguen las paradas:
    // con un duplicado, dos controles distintos serían el mismo para el oráculo.
    expect(new Set(expected).size).toBe(expected.length);
    // Todo control alcanzable necesita nombre accesible, o la parada no se puede anunciar.
    for (const name of expected) expect(name).not.toBe("");

    // El punto de partida es el contenido, que es donde deja al usuario el enlace de
    // salto; `main` lleva `tabIndex={-1}` justamente para poder recibirlo.
    await page.evaluate(() => document.querySelector("main").focus());
    const forward = await walk(page, "Tab", expected);
    expect(forward.seen).toEqual(expected);
    expect(forward.invisible).toEqual([]);

    // Y de vuelta, sin trampa de foco y en el orden inverso exacto. Tras la ida el foco
    // queda aparcado en el ÚLTIMO control, así que el primer Shift+Tab ya salta al
    // penúltimo: la vuelta es el inverso ROTADO una posición. Se afirma esa rotación
    // exacta y no un «contiene los mismos»: una trampa de foco la rompe igual.
    const reversed = [...expected].reverse();
    const back = [...reversed.slice(1), reversed[0]];
    const backwards = await walk(page, "Shift+Tab", back);
    expect(backwards.seen).toEqual(back);
    expect(backwards.invisible).toEqual([]);
  });
}

/** Dónde está el foco ahora mismo, descrito por lo que el escenario permite. */
function focusPlace(page) {
  return page.evaluate(() => {
    const active = document.activeElement;
    if (!active || active === document.body) return "body";
    if (active.tagName === "H1") return "h1";
    if (active.getAttribute("role") === "status") return "aviso";
    if (active.getAttribute("aria-label") === "Resultado de la importación")
      return "aviso";
    return `${active.tagName}«${(active.textContent ?? "").trim().slice(0, 30)}»`;
  });
}

test("conectores: en cada cambio de estado el foco pasa al h1 o al aviso de resultado @s38", async ({
  page,
}) => {
  test.setTimeout(120_000);
  const folder = resolve(".e2e-work", "additional-connectors-ux", "focus");
  await mkdir(folder, { recursive: true });
  const control = newControl();
  await simulate(page, control);
  await page.setViewportSize({ width: 1280, height: 1000 });
  const view = page.getByRole("main");
  const places = {};

  // El escenario nombra cuatro cambios de estado: conectado, importando, recibo y
  // desconectado. Se recorren los cuatro en el orden en que un propietario los vive.
  control.connection = DISCONNECTED;
  await page.goto("/conectores/gitlab");
  await expect(view.getByLabel("Ruta del proyecto")).toBeVisible();
  await view.getByLabel("Ruta del proyecto").fill(PROJECT_PATH);
  await view.getByLabel("Token de acceso personal").fill("glpat-xxxxxxxxxxxx");
  // «Guardando» no está entre los cuatro estados que el paréntesis del escenario enumera,
  // pero es un cambio de estado de la misma pantalla y sufre el mismo defecto que
  // «importando»: el botón pulsado queda `disabled` y el foco se cae al body. Se mide
  // aquí, y se declara como ampliación en progress/a11y_conectores_29.md.
  control.connectDelayMs = 1500;
  await view.getByRole("button", { name: "Conectar", exact: true }).click();
  await expect(view.getByRole("status")).toHaveText("Guardando…");
  places.guardando = await focusPlace(page);
  await expect(view.getByRole("region", { name: "Conexión" })).toBeVisible();
  places.conectado = await focusPlace(page);
  control.connectDelayMs = 0;

  // «Importando»: la respuesta se retrasa a propósito para poder mirar el estado
  // intermedio. Sin el retraso el recibo llegaría antes de la medición y el estado que
  // el contrato nombra no se auditaría nunca.
  control.importDelayMs = 1500;
  await view.getByRole("button", { name: "Importar issues" }).click();
  await expect(view.getByRole("status")).toHaveText(
    "Importando issues. Esto puede tardar un poco…",
  );
  places.importando = await focusPlace(page);

  await expect(
    view.getByRole("region", { name: "Resultado de la importación" }),
  ).toBeVisible();
  places.recibo = await focusPlace(page);
  control.importDelayMs = 0;

  await view.getByRole("button", { name: "Desconectar", exact: true }).click();
  await view
    .getByRole("button", { name: "Confirmar desconexión", exact: true })
    .click();
  await expect(view.getByLabel("Ruta del proyecto")).toBeVisible();
  places.desconectado = await focusPlace(page);

  await writeFile(`${folder}/places.json`, JSON.stringify(places, null, 2));
  expect(places).toEqual({
    guardando: "aviso",
    conectado: "h1",
    importando: "aviso",
    recibo: "aviso",
    desconectado: "h1",
  });
});
