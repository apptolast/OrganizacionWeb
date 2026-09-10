import { chromium, test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import {
  AXE_TAGS,
  CATALOG,
  MIN_TARGET,
  WIDTHS,
  simulate,
  newControl,
} from "./support/connectors-fixture.mjs";

/**
 * @s38, la mitad que la revisión de anchos NO puede medir: «con zoom nativo 200 %».
 *
 * Estrechar el viewport no es ampliar. El zoom nativo de Chromium se aplica con
 * `chrome.tabs.setZoom` desde una extensión efímera, que es la única forma de ampliar de
 * verdad: `deviceScaleFactor` cambia la densidad, no el zoom, y no reproduce el reflujo
 * que este escenario quiere medir. Mismo mecanismo que
 * `e2e/github-connector-native-zoom.spec.mjs`.
 *
 * La API se simula igual que en la otra mitad, por el motivo que documenta
 * `e2e/support/connectors-fixture.mjs`.
 */

/** Dónde vive cada pantalla y qué hay que ver antes de medirla. */
const SCREENS = [
  { name: "catalogo", route: "/conectores" },
  { name: "gitlab", route: "/conectores/gitlab" },
];

/**
 * Abre un Chromium real con la extensión que sabe ampliar, y devuelve el contexto, la
 * página y el trabajador de la extensión.
 */
async function openZoomableBrowser(scratch, baseURL) {
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated connectors zoom QA",
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
  return context;
}

test("@s38 las dos pantallas en los anchos que caben, al 200 % de zoom nativo", async ({
  baseURL,
}) => {
  test.setTimeout(300_000);
  const scratch = resolve(
    ".e2e-work",
    "additional-connectors-zoom",
    process.env.E2E_COMPOSE_PROJECT ?? "local",
  );
  await mkdir(scratch, { recursive: true });

  const context = await openZoomableBrowser(scratch, baseURL);
  const evidence = [];
  try {
    const control = newControl();
    // El enrutado va en el CONTEXTO, no en la página: la persistente abre su pestaña
    // inicial antes de que exista ninguna `page` a la que enganchar la simulación.
    await simulate(context, control);
    const page = await context.newPage();

    const baseline = await page.evaluate(() => ({
      innerWidth,
      outerWidth,
      dpr: devicePixelRatio,
    }));

    // Al 200 % cada pixel CSS ocupa dos de ventana, asi que para ver `width` px CSS hay
    // que abrir `2 * width` mas el cromo del navegador, medido antes de ampliar.
    const chromeWidth = baseline.outerWidth - baseline.innerWidth;
    // En una pantalla pequena -el xvfb de CI, 1280 px de ancho por omision- hay anchos que
    // NO se pueden medir: `chrome.windows.update` rechaza los limites que no caben al menos
    // al 50 % en la pantalla visible, con «Invalid value for bounds». Se declaran omitidos
    // con su motivo en vez de fingir que se midieron. Mismo criterio y mismo idioma que
    // `github-connector-native-zoom`, `external-calendar-native-zoom`,
    // `automations-native-zoom` y `webhooks-native-zoom`.
    const available = await page.evaluate(() => screen.availWidth);
    const fits = WIDTHS.filter((width) => width * 2 + chromeWidth <= available);
    const skipped = WIDTHS.filter((width) => !fits.includes(width));
    expect(
      fits,
      `ningun ancho del contrato cabe al 200 % en una pantalla de ${available} px`,
    ).not.toHaveLength(0);

    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const target = `${baseURL}/*`;

    for (const screen of SCREENS) {
      await page.goto(screen.route);
      const view = page.getByRole("main");
      if (screen.name === "catalogo") {
        await expect(view.getByRole("listitem")).toHaveCount(CATALOG.length);
      } else {
        // Se mide el estado del Given —conectado y con el recibo truncado a la vista—,
        // que es además el que más controles y más cifras tiene.
        await expect(
          view.getByRole("region", { name: "Conexión" }),
        ).toBeVisible();
        await view.getByRole("button", { name: "Importar issues" }).click();
        await expect(
          view.getByRole("region", { name: "Resultado de la importación" }),
        ).toBeVisible();
      }

      const zoom = await worker.evaluate(async (url) => {
        const [tab] = await chrome.tabs.query({ url });
        await chrome.tabs.setZoom(tab.id, 2);
        return chrome.tabs.getZoom(tab.id);
      }, target);
      expect(zoom, `${screen.name}: el zoom nativo no llegó al 200 %`).toBe(2);
      await expect
        .poll(() => page.evaluate(() => devicePixelRatio))
        .toBe(baseline.dpr * 2);

      for (const width of fits) {
        await worker.evaluate(
          async ({ url, outer }) => {
            const [tab] = await chrome.tabs.query({ url });
            await chrome.windows.update(tab.windowId, { width: outer });
          },
          { url: target, outer: width * 2 + chromeWidth },
        );
        await expect
          .poll(() => page.evaluate(() => innerWidth), {
            message: `${screen.name}: la ventana no llegó a ${width} px CSS al 200 %`,
          })
          .toBe(width);

        const measured = await page.evaluate(() => ({
          width: document.documentElement.clientWidth,
          scroll: document.documentElement.scrollWidth,
          dpr: devicePixelRatio,
          // Mismo conjunto nombrado que la revisión de anchos: las acciones, las cifras
          // del recibo y los valores de la conexión. Ni INPUT ni SELECT, a los que
          // Chromium impone `overflow: clip` desde su propia hoja de agente.
          clipped: [
            ...document.querySelectorAll(
              "main button, main a, main dd, main dt, main li h2, main li span, main section p, main li p",
            ),
          ]
            .filter((element) => element.getClientRects().length)
            .map((element) => ({
              what: element.tagName,
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
            ),
          controls: [
            ...document.querySelectorAll(
              "main button, main a, main select, main input",
            ),
          ]
            .filter((element) => element.getClientRects().length)
            .map((element) => {
              const box = element.getBoundingClientRect();
              return {
                name: (
                  element.getAttribute("aria-label") ||
                  element.labels?.[0]?.textContent ||
                  element.textContent ||
                  element.id ||
                  ""
                )
                  .trim()
                  .slice(0, 40),
                x: box.x,
                width: box.width,
                height: box.height,
              };
            }),
        }));

        const where = `${screen.name} @ ${width} px CSS al 200 %`;
        // El zoom sigue puesto: sin esta comprobación lo demás mediría la pantalla sin
        // ampliar y pasaría por lo que no es.
        expect(measured.dpr, `${where}: zoom perdido`).toBe(baseline.dpr * 2);
        expect(
          measured.scroll,
          `${where}: desplazamiento horizontal`,
        ).toBeLessThanOrEqual(measured.width + 1);
        expect(measured.clipped, `${where}: contenido recortado`).toEqual([]);
        for (const box of measured.controls) {
          expect(
            box.x,
            `${where}: «${box.name}» sale por la izquierda`,
          ).toBeGreaterThanOrEqual(-1);
          expect(
            box.x + box.width,
            `${where}: «${box.name}» sale por la derecha`,
          ).toBeLessThanOrEqual(measured.width + 1);
          expect(
            box.width,
            `${where}: «${box.name}» ancho del objetivo`,
          ).toBeGreaterThanOrEqual(MIN_TARGET);
          expect(
            box.height,
            `${where}: «${box.name}» alto del objetivo`,
          ).toBeGreaterThanOrEqual(MIN_TARGET);
        }

        const violations = (
          await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze()
        ).violations;
        await writeFile(
          join(scratch, `${screen.name}-${width}-axe.json`),
          JSON.stringify(violations, null, 2),
        );
        expect(violations, `${where}: axe`).toEqual([]);

        evidence.push({
          screen: screen.name,
          width,
          dpr: measured.dpr,
          scroll: measured.scroll,
          client: measured.width,
          controls: measured.controls.length,
          violations: violations.length,
        });
        await page.screenshot({
          path: join(scratch, `${screen.name}-zoom200-${width}.png`),
          fullPage: true,
        });
      }
    }
    await writeFile(
      join(scratch, "evidence.json"),
      JSON.stringify(
        { pantalla: available, omitidos: skipped, evidence },
        null,
        2,
      ),
    );
    // Que se haya tomado una medida por pantalla y por ancho MEDIBLE, sin saltarse ninguna
    // vuelta. Los omitidos quedan escritos arriba: lo que no vale es que falte una de las
    // que si cabian.
    expect(evidence).toHaveLength(SCREENS.length * fits.length);
  } finally {
    await context.close();
  }
});
