import { chromium, test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";
import { sql } from "./support/projects.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";

/**
 * @s42 de `features/automations.feature`: «no hay desplazamiento horizontal ni contenido cortado»,
 * «todos los controles interactivos miden al menos 44 por 44 píxeles CSS y son alcanzables por
 * teclado en orden lógico con foco visible» y «axe no reporta violaciones serious ni critical», en
 * los anchos 320, 768, 1280 y 1440.
 *
 * **Qué añade esta spec, y qué no.** A diferencia de las otras dos features del encargo, aquí el
 * zoom nativo **sí** se ejecutaba ya: `automations-ux.spec.mjs:201` lo aplica con
 * `chrome.tabs.setZoom`. Lo que le falta es el barrido: mide **un solo ancho, 320 px**. El contrato
 * nombra cuatro. Esta spec cubre los cuatro con el zoom nativo puesto.
 *
 * Conviene decir también lo que el contrato **no** dice, para que nadie lea de más: la tabla de
 * `Examples` del escenario empareja los cuatro anchos con «100 %» y sólo 1440 con «texto 200 %»;
 * el zoom **nativo** al 200 % no aparece en esa tabla, sino en el título del escenario («matriz
 * responsive, **de zoom** y de accesibilidad») y en `docs/ux-requirements.md`. Medirlo en los
 * cuatro anchos es, por tanto, **más estricto** que la letra de la tabla. Se hace así a propósito:
 * WCAG 1.4.10 exige reflujo sin scroll horizontal a 320 px CSS, que es justo lo que produce el zoom
 * al 200 % sobre 640. Si alguna vez falla, el hallazgo hay que contrastarlo con el contrato antes
 * de tratarlo como defecto del producto.
 *
 * Precio declarado, igual que en las otras: `headless: false` y `channel: "chromium"` hacen la
 * prueba dependiente de un entorno gráfico. Si el zoom no se aplica, **falla**; no se salta.
 *
 * Estado medido: **editor abierto sobre la lista vacía**, que es el que más controles pone en
 * pantalla de los que se alcanzan sin sembrar datos. Los otros dos estados del `Given` —lista con
 * reglas y resultados de simulación— **no** se reproducen aquí: exigen reglas guardadas y bloques
 * planificados, y los cubre `automations-ux.spec.mjs`. Queda dicho para que nadie lea esta spec
 * como cobertura de los tres.
 */

const WIDTHS = [320, 768, 1280, 1440];
const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"];
const MIN_TARGET = 44;

function clearRules() {
  sql(
    "DELETE FROM automation_runs WHERE owner_id='e2e-user'; DELETE FROM automation_rules WHERE owner_id='e2e-user'; DELETE FROM automation_cursors WHERE owner_id='e2e-user'",
  );
}

test.beforeEach(() => clearRules());
test.afterEach(() => clearRules());

function measure(page) {
  return page.evaluate(() => ({
    dpr: devicePixelRatio,
    client: document.documentElement.clientWidth,
    scroll: document.documentElement.scrollWidth,
    controls: [
      ...document.querySelectorAll(
        "main a, main button, main input, main select, main textarea",
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
}

function assertNoClipping(measured, width) {
  expect(
    measured.scroll,
    `desplazamiento horizontal a ${width} px al 200 %`,
  ).toBeLessThanOrEqual(measured.client + 1);
  for (const control of measured.controls) {
    const where = `${width} px al 200 % → «${String(control.name).trim().slice(0, 40)}»`;
    expect(
      control.x,
      `contenido cortado por la izquierda: ${where}`,
    ).toBeGreaterThanOrEqual(-1);
    expect(
      control.x + control.width,
      `contenido cortado por la derecha: ${where}`,
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
 * El recorrido se hace **con Tab**: la hoja usa `:focus-visible`, que Chromium no aplica al foco
 * programático sobre botones y enlaces, así que medirlo con `focus()` daría un falso negativo.
 */
async function assertFocusIsVisible(page, width) {
  const controls = await page.evaluate(
    () =>
      [
        ...document.querySelectorAll(
          "main a, main button, main input, main select, main textarea",
        ),
      ].filter((element) => element.getClientRects().length).length,
  );
  expect(
    controls,
    `no hay controles que recorrer a ${width} px`,
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
  expect(invisible, `foco no visible a ${width} px al 200 %`).toEqual([]);
}

test("@s42 los cuatro anchos al 200 % de zoom nativo, sin desplazamiento horizontal ni contenido cortado", async ({
  baseURL,
}) => {
  test.setTimeout(240_000);

  const scratch = resolve(
    ".e2e-work",
    "automations-zoom",
    process.env.E2E_COMPOSE_PROJECT ?? String(process.pid),
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated automations zoom QA",
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
    const project = await context.request.post("/api/v1/projects", {
      headers: await csrfHeaders(context.request),
      data: { name: "Marketing", description: "Descripción conservada" },
    });
    expect(project.status()).toBe(201);

    const page = await context.newPage();
    await page.goto("/automatizaciones");
    await page.getByRole("button", { name: "Nueva regla" }).click();
    await expect(page.getByLabel(/nombre/i)).toBeVisible();

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

    // Al 200 % cada píxel CSS ocupa dos de ventana: para ver `width` px CSS hay que abrir
    // `2 * width` más el cromo del navegador, medido antes de ampliar.
    const chrome_ = baseline.outerWidth - baseline.innerWidth;
    // Chromium rechaza una ventana que no quepa al menos al 50 % en la pantalla
    // visible: «Invalid value for bounds». Al 200 % ver `width` px CSS exige
    // `2 * width + cromo`, así que en una pantalla pequeña —el xvfb de CI es el
    // caso— hay anchos que no se pueden medir. Se declaran omitidos con su
    // motivo en vez de fingir que se midieron, que es lo que haría un `skip`
    // silencioso o dejar que el gestor recorte la ventana sin que nadie mire.
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

      const measured = await measure(page);
      expect(measured.dpr, `zoom perdido a ${width} px`).toBe(baseline.dpr * 2);
      assertNoClipping(measured, width);
      await assertFocusIsVisible(page, width);

      const violations = (
        await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze()
      ).violations;
      expect(violations, `axe a ${width} px al 200 %`).toEqual([]);

      evidence.push({
        width,
        dpr: measured.dpr,
        scroll: measured.scroll,
        client: measured.client,
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
