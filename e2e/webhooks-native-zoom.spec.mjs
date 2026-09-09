import { chromium, test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";

/**
 * @s42 de `features/webhooks.feature`: «se recorre la vista con teclado en 320, 768, 1280 y 1440 px
 * CSS, con texto al 200 % y **zoom al 200 %**», y «ningún ancho presenta scroll horizontal ni
 * recorte de la URL, del secreto ni de la tabla de entregas».
 *
 * `webhooks-ux.spec.mjs` ya cubre los anchos y el texto al 200 %, pero **el zoom nativo no se
 * ejecutaba en ninguna parte**: `setViewportSize` cambia el tamaño de la ventana, no el zoom, y
 * `deviceScaleFactor` cambia la densidad de píxeles. Ninguno de los dos reproduce el reflujo que
 * este escenario nombra. La única forma de ampliar de verdad es `chrome.tabs.setZoom` desde una
 * extensión efímera, que es lo que hace esta spec. Mismo mecanismo que
 * `e2e/github-connector-native-zoom.spec.mjs` y `e2e/reschedule-native-zoom.spec.mjs`.
 *
 * Precio declarado, igual que en las otras dos: `headless: false` y `channel: "chromium"` hacen la
 * prueba dependiente de un entorno gráfico. Es el coste de medir el zoom de verdad; a cambio, si el
 * zoom no se aplica la prueba **falla**, no se salta.
 *
 * La API se simula porque los siete estados de /webhooks se alcanzan así de forma determinista
 * (mismo criterio que `webhooks-ux.spec.mjs`). Lo que aquí se mide es el navegador real: reflujo,
 * geometría y foco bajo zoom nativo.
 */

// Los cuatro anchos que nombra @s42.
const WIDTHS = [320, 768, 1280, 1440];
const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"];
// Objetivo táctil mínimo de @s42.
const MIN_TARGET = 44;
const ENDPOINT_ID = "12345678-1234-4234-8234-123456789abc";
const DELIVERY_ID = "33333333-3333-4333-8333-333333333333";
const SECOND_ID = "22222222-2222-4222-8222-222222222222";
// Un secreto real es largo: es justamente lo que el contrato teme que se recorte.
const SECRET = `whsec_${"A".repeat(43)}`;
// Y una URL larga, por la misma razón: «ni recorte de la URL».
const URL_LARGA =
  "https://integraciones.example.com/hooks/organizacion/entrada-principal";

const endpoint = () => ({
  id: ENDPOINT_ID,
  url: URL_LARGA,
  description: "Notificar a mi panel de integraciones",
  eventTypes: ["TaskCreated.v1", "TaskStatusChanged.v1"],
  status: "active",
  disabledReason: null,
  disabledAt: null,
  createdAt: "2026-09-08T10:00:00.000000Z",
  updatedAt: "2026-09-08T10:00:00.000000Z",
});

const delivery = (overrides = {}) => ({
  id: DELIVERY_ID,
  eventId: DELIVERY_ID,
  eventType: "TaskCreated.v1",
  status: "succeeded",
  attempt: 1,
  httpStatus: 200,
  latencyMs: 12,
  errorClass: null,
  nextAttemptAt: null,
  createdAt: "2026-09-08T10:00:00.000000Z",
  updatedAt: "2026-09-08T10:00:00.000000Z",
  ...overrides,
});

/** Responde a cada llamada que la vista puede hacer; `control.items` decide qué contesta la lista. */
async function simulate(context, control) {
  await context.route("**/api/**", async (route) => {
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
    if (path === "/api/v1/me/webhooks" && method === "GET")
      return route.fulfill({ json: { items: control.items } });
    if (path === "/api/v1/me/webhooks" && method === "POST")
      return route.fulfill({
        status: 201,
        json: { endpoint: endpoint(), secret: SECRET },
      });
    if (path.endsWith("/deliveries") && method === "GET")
      return route.fulfill({
        json: {
          items: [
            delivery(),
            delivery({
              id: SECOND_ID,
              eventId: SECOND_ID,
              status: "exhausted",
              attempt: 6,
              httpStatus: 500,
              errorClass: "HTTP_ERROR",
              latencyMs: 4210,
            }),
          ],
        },
      });
    return route.fulfill({ status: 204, body: "" });
  });
}

/**
 * Mide la geometría del documento y de cada control visible. Devuelve datos; quien juzga es
 * `assertNoClipping`, para que el mensaje de fallo pueda nombrar el estado y el control.
 */
function measure(page) {
  return page.evaluate(() => {
    const target = (element) =>
      element instanceof HTMLInputElement && element.type === "checkbox"
        ? (element.closest("label") ?? element)
        : element;
    return {
      dpr: devicePixelRatio,
      client: document.documentElement.clientWidth,
      scroll: document.documentElement.scrollWidth,
      controls: [
        ...document.querySelectorAll(
          "main button, main a, main input, main select, main textarea",
        ),
      ]
        .filter((element) => element.getClientRects().length)
        .map((element) => {
          const box = target(element).getBoundingClientRect();
          return {
            name:
              element.getAttribute("aria-label") ||
              element.labels?.[0]?.textContent ||
              element.textContent ||
              element.id,
            x: box.x,
            width: box.width,
            height: box.height,
          };
        }),
    };
  });
}

function assertNoClipping(measured, state, width) {
  // Sin desplazamiento horizontal del documento.
  expect(
    measured.scroll,
    `desplazamiento horizontal en «${state}» a ${width} px al 200 %`,
  ).toBeLessThanOrEqual(measured.client + 1);
  // Sin recorte: ningún control se sale por ninguno de los dos lados.
  for (const control of measured.controls) {
    const where = `«${state}» a ${width} px al 200 % → «${String(control.name).trim().slice(0, 40)}»`;
    expect(
      control.x,
      `recortado por la izquierda: ${where}`,
    ).toBeGreaterThanOrEqual(-1);
    expect(
      control.x + control.width,
      `recortado por la derecha: ${where}`,
    ).toBeLessThanOrEqual(measured.client + 1);
    expect(control.width, `ancho mínimo: ${where}`).toBeGreaterThanOrEqual(
      MIN_TARGET,
    );
    expect(control.height, `alto mínimo: ${where}`).toBeGreaterThanOrEqual(
      MIN_TARGET,
    );
  }
}

/**
 * «Foco visible» se comprueba recorriendo **con Tab**, no con `focus()`: la hoja usa
 * `:focus-visible`, que Chromium no aplica al foco programático sobre botones y enlaces. Medirlo de
 * la otra forma daría un falso negativo.
 */
async function assertFocusIsVisible(page, state) {
  const controls = await page.evaluate(
    () =>
      [
        ...document.querySelectorAll(
          "main button, main a, main input, main select, main textarea",
        ),
      ].filter((element) => element.getClientRects().length).length,
  );
  expect(
    controls,
    `«${state}» no tiene controles que recorrer`,
  ).toBeGreaterThan(0);
  await page.evaluate(() => document.querySelector("main h1")?.focus());

  const invisible = [];
  for (let step = 0; step < controls; step++) {
    await page.keyboard.press("Tab");
    const ring = await page.evaluate(() => {
      const active = document.activeElement;
      if (!active || !active.closest("main")) return null;
      const style = getComputedStyle(active);
      return {
        name:
          active.getAttribute("aria-label") ||
          active.textContent?.trim() ||
          active.id,
        matchesFocusVisible: active.matches(":focus-visible"),
        outlineStyle: style.outlineStyle,
        outlineWidth: parseFloat(style.outlineWidth),
        outlineColor: style.outlineColor,
      };
    });
    if (!ring) continue;
    const visible =
      ring.matchesFocusVisible &&
      ring.outlineStyle !== "none" &&
      ring.outlineWidth >= 1 &&
      !ring.outlineColor.includes("transparent");
    if (!visible) invisible.push(ring);
  }
  expect(invisible, `foco no visible en «${state}» al 200 %`).toEqual([]);
}

test("@s42 los cuatro anchos al 200 % de zoom nativo, sin recorte de la URL, del secreto ni de la tabla de entregas", async ({
  baseURL,
}) => {
  // Dos estados por cuatro anchos, cada uno con su recorrido de teclado y su pasada de axe.
  test.setTimeout(240_000);

  const scratch = resolve(
    ".e2e-work",
    "webhooks-zoom",
    process.env.E2E_COMPOSE_PROJECT ?? String(process.pid),
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated webhooks zoom QA",
      version: "1.0",
      permissions: ["tabs"],
      host_permissions: ["http://127.0.0.1/*"],
      background: { service_worker: "worker.js" },
    }),
  );
  await writeFile(
    join(extension, "worker.js"),
    "chrome.runtime.onInstalled.addListener(()=>{});",
  );

  const context = await chromium.launchPersistentContext(
    join(scratch, "browser"),
    {
      channel: "chromium",
      headless: false,
      viewport: null,
      baseURL,
      args: [
        `--disable-extensions-except=${extension}`,
        `--load-extension=${extension}`,
        "--window-size=1440,1000",
      ],
    },
  );

  const evidence = [];
  try {
    const control = { items: [] };
    await simulate(context, control);
    const page = await context.newPage();
    const view = page.getByRole("main");

    // Estado «secreto visible»: es el que enseña el secreto de 49 caracteres, uno de los tres
    // elementos que el contrato prohíbe recortar.
    await page.goto("/webhooks");
    await expect(
      view.getByRole("heading", { level: 1, name: "Webhooks" }),
    ).toBeVisible();
    await view
      .getByRole("textbox", { name: "URL", exact: true })
      .fill(URL_LARGA);
    await view
      .getByRole("checkbox", { name: "Seleccionar todos", exact: true })
      .check();
    await view
      .getByRole("button", { name: "Crear webhook", exact: true })
      .click();
    await expect(view.getByLabel("Secreto", { exact: true })).toHaveValue(
      SECRET,
    );

    const baseline = await page.evaluate(() => ({
      innerWidth,
      outerWidth,
      dpr: devicePixelRatio,
    }));

    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const target = `${baseURL}/*`;
    const zoom = await worker.evaluate(async (url) => {
      const [tab] = await chrome.tabs.query({ url });
      await chrome.tabs.setZoom(tab.id, 2);
      return chrome.tabs.getZoom(tab.id);
    }, target);
    expect(zoom, "el zoom nativo no llegó al 200 %").toBe(2);
    await expect
      .poll(() => page.evaluate(() => devicePixelRatio))
      .toBe(baseline.dpr * 2);

    // Al 200 % cada píxel CSS ocupa dos de ventana, así que para ver `width` px CSS hay que abrir
    // `2 * width` más el cromo del navegador, que se mide antes de ampliar.
    const chrome_ = baseline.outerWidth - baseline.innerWidth;
    // Chromium rechaza una ventana que no quepa al menos al 50 % en la pantalla visible
    // («Invalid value for bounds»), y al 200 % ver `width` px CSS exige `2 * width + cromo`.
    // En una pantalla pequeña —el xvfb de CI— hay anchos que no se pueden medir: se declaran
    // omitidos con su motivo en vez de fingir que se midieron.
    const available = await page.evaluate(() => screen.availWidth);
    const fits = WIDTHS.filter((width) => width * 2 + chrome_ <= available);
    const skipped = WIDTHS.filter((width) => !fits.includes(width));
    expect(
      fits,
      `ningún ancho del contrato cabe al 200 % en una pantalla de ${available} px`,
    ).not.toHaveLength(0);

    for (const state of ["secreto", "entregas"]) {
      if (state === "entregas") {
        // Se cierra el secreto y se abre el panel de entregas: la tabla es el tercer elemento que
        // el contrato prohíbe recortar.
        control.items = [endpoint()];
        await view.getByRole("button", { name: "Cerrar", exact: true }).click();
        await expect(view.getByText(URL_LARGA)).toBeVisible();
        await view
          .getByRole("button", { name: "Ver entregas", exact: true })
          .click();
        await expect(view.getByRole("table")).toBeVisible();
      }

      for (const width of fits) {
        await worker.evaluate(
          async ({ url, outer }) => {
            const [tab] = await chrome.tabs.query({ url });
            await chrome.windows.update(tab.windowId, { width: outer });
          },
          { url: target, outer: width * 2 + chrome_ },
        );
        await expect
          .poll(() => page.evaluate(() => innerWidth), {
            message: `la ventana no llegó a ${width} px CSS al 200 %`,
          })
          .toBe(width);

        const measured = await measure(page);
        expect(measured.dpr, `zoom perdido en «${state}» a ${width} px`).toBe(
          baseline.dpr * 2,
        );
        assertNoClipping(measured, state, width);
        await assertFocusIsVisible(page, `${state} a ${width} px`);

        const violations = (
          await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze()
        ).violations;
        expect(violations, `axe en «${state}» a ${width} px al 200 %`).toEqual(
          [],
        );

        evidence.push({
          state,
          width,
          dpr: measured.dpr,
          scroll: measured.scroll,
          client: measured.client,
          controls: measured.controls.length,
          violations: violations.length,
        });
        await page.screenshot({
          path: join(scratch, `zoom200-${state}-${width}.png`),
          fullPage: true,
        });
      }
    }

    await writeFile(
      join(scratch, "evidence.json"),
      JSON.stringify(
        { pantalla: available, medidos: evidence, omitidos: skipped },
        null,
        2,
      ),
    );
    expect(evidence).toHaveLength(2 * fits.length);
  } finally {
    await context.close();
  }
});
