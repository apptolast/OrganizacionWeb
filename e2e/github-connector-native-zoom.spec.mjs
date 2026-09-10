import { chromium } from "@playwright/test";
import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { loginSession } from "../scripts/session-client.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import AxeBuilder from "@axe-core/playwright";

/**
 * @s42 «en los anchos 320, 768 y 1440 px CSS con zoom nativo 200 por ciento», y «a 320 px con zoom
 * 200 por ciento no hay desplazamiento horizontal ni contenido recortado».
 *
 * El zoom nativo de Chromium se aplica con `chrome.tabs.setZoom` desde una extensión efímera, que
 * es la única forma de ampliar de verdad: `deviceScaleFactor` cambia la densidad, no el zoom, y no
 * reproduce el reflujo que este escenario quiere medir. Mismo mecanismo que
 * `e2e/reschedule-native-zoom.spec.mjs`.
 *
 * **Precio declarado del zoom nativo.** Al 200 % cada píxel CSS ocupa dos de ventana, así que ver
 * `w` px CSS exige una ventana de `2w` más el cromo del navegador. Los anchos que no quepan en la
 * pantalla no se pueden medir aquí —Chromium rechaza la petición— y se declaran omitidos en
 * `evidence.json` junto al ancho de pantalla que los descartó. Los tres anchos del contrato quedan
 * cubiertos a 100 % por `e2e/github-connector.spec.mjs`, que los recorre enteros con axe y con la
 * geometría de todos los controles. La limitación se escribe, no se disimula con un `skip`.
 */

const OWNER = "e2e-user";
const TOKEN = "ghp_token_de_pruebas";
const REPOSITORY = "octocat/hello-world";
const WIDTHS = [320, 768, 1440];

/**
 * Vacía en cascada, no por lista ordenada a mano: el orden enumerado caducó en
 * cuanto `work_sessions` pasó a referenciar `tasks`. Mismo criterio y mismo
 * motivo que en `e2e/github-connector.spec.mjs`.
 */
function forget() {
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, task_status_history, tasks, outbox_events, projects, connector_connections CASCADE",
  );
}

test.afterEach(() => forget());

test("@s42 los anchos que caben al 200 % de zoom nativo, sin recorte ni desplazamiento", async ({
  request,
  baseURL,
}) => {
  test.setTimeout(180_000);
  forget();
  await create(request, "Proyecto destino");

  const scratch = resolve(
    ".e2e-work",
    "github-connector-zoom",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated github connector zoom QA",
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
    await loginSession(context.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    const page = await context.newPage();

    // Se conecta e importa por la interfaz: así el barrido recorre el estado con más controles
    // (selector, botones, resumen y enlace) y no una pantalla casi vacía.
    await page.goto("/integraciones/github");
    await page.getByLabel(/repositorio/i).fill(REPOSITORY);
    await page.getByLabel(/token/i).fill(TOKEN);
    await page.getByRole("button", { name: "Conectar" }).click();
    await expect(page.getByText("Conectada")).toBeVisible();
    await page
      .getByRole("button", { name: "Importar issues abiertas" })
      .click();
    await expect(
      page
        .getByRole("region", { name: "Resultado de la importación" })
        .getByText("Creadas 3"),
    ).toBeVisible();

    const measure = () =>
      page.evaluate(() => ({
        innerWidth,
        outerWidth,
        dpr: devicePixelRatio,
      }));
    const baseline = await measure();

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
    // Chromium rechaza una ventana que no quepa al menos al 50 % en la pantalla visible («Invalid
    // value for bounds»), y al 200 % ver `width` px CSS exige `2 * width + cromo`. En una pantalla
    // pequeña —el xvfb de CI, 1280 px de ancho por omisión— hay anchos que no se pueden medir: se
    // declaran omitidos con su motivo en vez de fingir que se midieron. Mismo criterio que
    // `external-calendar-native-zoom`, `automations-native-zoom` y `webhooks-native-zoom`.
    const available = await page.evaluate(() => screen.availWidth);
    const fits = WIDTHS.filter((width) => width * 2 + chrome_ <= available);
    const skipped = WIDTHS.filter((width) => !fits.includes(width));
    expect(
      fits,
      `ningún ancho del contrato cabe al 200 % en una pantalla de ${available} px`,
    ).not.toHaveLength(0);
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

      const measured = await page.evaluate(() => ({
        width: document.documentElement.clientWidth,
        scroll: document.documentElement.scrollWidth,
        dpr: devicePixelRatio,
        controls: [
          ...document.querySelectorAll(
            "main button, main a, main select, main input",
          ),
        ]
          .filter((element) => element.getClientRects().length)
          .map((element) => {
            const box = element.getBoundingClientRect();
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
      }));

      expect(measured.dpr, `zoom perdido a ${width} px`).toBe(baseline.dpr * 2);
      // Sin desplazamiento horizontal.
      expect(
        measured.scroll,
        `desplazamiento horizontal a ${width} px al 200 %`,
      ).toBeLessThanOrEqual(measured.width + 1);
      // Sin contenido recortado: ningún control se sale por ninguno de los dos lados.
      for (const control of measured.controls) {
        const where = `${width} px al 200 % → «${String(control.name).trim().slice(0, 40)}»`;
        expect(
          control.x,
          `recortado por la izquierda: ${where}`,
        ).toBeGreaterThanOrEqual(-1);
        expect(
          control.x + control.width,
          `recortado por la derecha: ${where}`,
        ).toBeLessThanOrEqual(measured.width + 1);
        expect(control.width, `ancho mínimo: ${where}`).toBeGreaterThanOrEqual(
          44,
        );
        expect(control.height, `alto mínimo: ${where}`).toBeGreaterThanOrEqual(
          44,
        );
      }

      const violations = (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations;
      expect(violations, `axe a ${width} px al 200 %`).toEqual([]);

      evidence.push({
        width,
        dpr: measured.dpr,
        scroll: measured.scroll,
        client: measured.width,
        controls: measured.controls.length,
        violations: violations.length,
      });
      await page.screenshot({
        path: join(scratch, `zoom200-${width}.png`),
        fullPage: true,
      });
    }
    await writeFile(
      join(scratch, "evidence.json"),
      JSON.stringify(
        { pantalla: available, medidos: evidence, omitidos: skipped },
        null,
        2,
      ),
    );
    expect(evidence).toHaveLength(fits.length);
  } finally {
    await context.close();
  }
});
