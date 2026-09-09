import { chromium, test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { loginSession } from "../scripts/session-client.mjs";
import { sql } from "./support/projects.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";

/**
 * @s40 de `features/external_calendar.feature`: «se revisa según la matriz de
 * docs/ux-requirements.md a 320, 768, 1280 y 2560 px CSS, **zoom nativo 200 %** y texto ampliado
 * 200 %», «no hay solapes, recortes ni scroll horizontal accidental en ningún ancho», «el recorrido
 * con teclado sigue el orden Etiqueta, Dirección secreta iCal, Guardar, Sincronizar ahora, Eliminar
 * suscripción con foco visible en cada control» y «los controles miden al menos 44 por 44 px CSS».
 *
 * `external-calendar-ux-audit.spec.mjs` cubre los anchos con `setViewportSize` y el texto al 200 %
 * con `font-size`, pero **el zoom nativo que el contrato nombra no se ejecutaba en ninguna parte**:
 * `setViewportSize` cambia el tamaño de la ventana, no el zoom. La única forma de ampliar de verdad
 * es `chrome.tabs.setZoom` desde una extensión efímera. Mismo mecanismo que
 * `e2e/github-connector-native-zoom.spec.mjs`.
 *
 * Precio declarado: `headless: false` y `channel: "chromium"` hacen la prueba dependiente de un
 * entorno gráfico. A cambio, si el zoom no se aplica **falla**, no se salta.
 *
 * **Por qué 2560 no está en la lista.** Al 200 % cada píxel CSS ocupa dos de ventana, así que ver
 * 2560 px CSS exige una ventana de 5120 px más el cromo. Ninguna pantalla de desarrollo o de CI de
 * este proyecto la tiene, y el gestor de ventanas recorta la petición en silencio: la prueba mediría
 * un ancho distinto del que dice medir, que es peor que no medirlo. Los tres anchos que sí caben se
 * miden de verdad aquí; 2560 queda cubierto a 100 % por `external-calendar-ux-audit.spec.mjs`, que
 * lo recorre junto a los dos lados de cada punto de ruptura. La limitación queda escrita en
 * `progress/ux_external_calendar.md` en lugar de disimulada con un `skip`.
 */

const WIDTHS = [320, 768, 1280];
const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"];
const MIN_TARGET = 44;
// Grosor declarado por la regla `:focus-visible` de frontend/src/styles.scss.
const FOCUS_RING_MIN_WIDTH = 3;
// Cota del recorrido: la barra lateral y el enlace de salto se interponen, pero cinco paradas no
// necesitan más de cuarenta tabulaciones.
const MAX_TAB_STEPS = 40;
const SECRET = "https://calendar.google.com/calendar/ical/e2e/private-WXYZ.ics";
const ETIQUETA = "Trabajo 😀 con resumen Unicode largo";
// El orden que fija el contrato, literal.
const ORDEN = [
  "Etiqueta",
  "Dirección secreta iCal",
  "Guardar",
  "Sincronizar ahora",
  "Eliminar suscripción",
];

function forget() {
  sql("DELETE FROM external_calendar_subscriptions WHERE owner_id='e2e-user'");
}

test.beforeEach(() => forget());
test.afterEach(() => forget());

/** Mide el documento y cada control visible del formulario. Devuelve datos; juzga quien llama. */
function measure(page) {
  return page.evaluate(() => ({
    dpr: devicePixelRatio,
    client: document.documentElement.clientWidth,
    scroll: document.documentElement.scrollWidth,
    controls: [
      ...document.querySelectorAll(
        ".external-calendar button, .external-calendar input, .external-calendar a",
      ),
    ]
      .filter((element) => element.getClientRects().length)
      .map((element) => {
        const box = element.getBoundingClientRect();
        return {
          name:
            element.labels?.[0]?.textContent ||
            element.getAttribute("aria-label") ||
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
    `scroll horizontal a ${width} px CSS con zoom nativo 200 %`,
  ).toBeLessThanOrEqual(measured.client + 1);
  for (const control of measured.controls) {
    const where = `${width} px al 200 % → «${String(control.name).trim().slice(0, 40)}»`;
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
 * El recorrido se hace **con Tab**, no con `focus()`: la hoja usa `:focus-visible`, que Chromium no
 * aplica al foco programático. Se exige el anillo del producto —`3px solid var(--accent)`— y no el
 * del agente de usuario, que Chromium computa con `outline-style: auto`.
 */
async function assertTabOrderAndFocusRing(page, width) {
  await page.getByRole("link", { name: "Saltar al contenido" }).focus();
  const seen = [];
  const invisible = [];
  const intruders = [];
  for (
    let step = 0;
    step < MAX_TAB_STEPS && seen.length < ORDEN.length;
    step++
  ) {
    await page.keyboard.press("Tab");
    const stop = await page.evaluate(() => {
      const active = document.activeElement;
      if (!active) return null;
      const label = active.labels?.[0]?.textContent;
      const style = getComputedStyle(active);
      return {
        name: (label ?? active.textContent ?? "").trim(),
        insideForm: Boolean(
          active.closest(".external-calendar") && active !== document.body,
        ),
        matchesFocusVisible: active.matches(":focus-visible"),
        outlineStyle: style.outlineStyle,
        outlineWidth: parseFloat(style.outlineWidth),
        outlineColor: style.outlineColor,
      };
    });
    if (!stop) continue;
    if (!ORDEN.includes(stop.name)) {
      if (stop.insideForm && !intruders.includes(stop.name))
        intruders.push(stop.name);
      continue;
    }
    if (seen.at(-1) === stop.name) continue;
    seen.push(stop.name);
    const visible =
      stop.matchesFocusVisible &&
      stop.outlineStyle === "solid" &&
      stop.outlineWidth >= FOCUS_RING_MIN_WIDTH &&
      !stop.outlineColor.includes("transparent");
    if (!visible) invisible.push(stop);
  }
  expect(seen, `orden del recorrido a ${width} px al 200 %`).toEqual(ORDEN);
  expect(invisible, `foco no visible a ${width} px al 200 %`).toEqual([]);
  expect(intruders, `paradas intrusas a ${width} px al 200 %`).toEqual([]);
}

test("@s40 los anchos que caben al 200 % de zoom nativo: sin recorte, con el orden del contrato y el foco visible", async ({
  baseURL,
}) => {
  test.setTimeout(240_000);

  const scratch = resolve(
    ".e2e-work",
    "external-calendar-zoom",
    process.env.E2E_COMPOSE_PROJECT ?? String(process.pid),
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated external calendar zoom QA",
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

    // Se llega al estado «con suscripción» por la interfaz, que es el que tiene los cinco controles
    // del orden del contrato; el estado vacío no tiene ni Sincronizar ni Eliminar.
    await page.goto("/calendario-externo");
    await page.getByLabel("Etiqueta").fill(ETIQUETA);
    await page.getByLabel("Dirección secreta iCal").fill(SECRET);
    await page.getByRole("button", { name: "Guardar" }).click();
    await expect(page.getByText("calendar.google.com")).toBeVisible();

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

    const chrome_ = baseline.outerWidth - baseline.innerWidth;
    for (const width of WIDTHS) {
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
      await assertTabOrderAndFocusRing(page, width);

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
      JSON.stringify(evidence, null, 2),
    );
    expect(evidence).toHaveLength(WIDTHS.length);
  } finally {
    await context.close();
  }
});
