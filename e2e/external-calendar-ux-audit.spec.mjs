import { test, expect } from "./support/authenticated-test.mjs";
import AxeBuilder from "@axe-core/playwright";
import { sql } from "./support/projects.mjs";

// Matriz de docs/ux-requirements.md: los cuatro anchos del contrato y ambos
// lados de cada punto de ruptura.
const widths = [
  320, 359, 360, 361, 599, 600, 601, 767, 768, 769, 1279, 1280, 1281, 2560,
];
const SECRET = "https://calendar.google.com/calendar/ical/e2e/private-WXYZ.ics";
// Grosor declarado por la regla `:focus-visible` de frontend/src/styles.scss.
const FOCUS_RING_MIN_WIDTH = 3;
// Cota del recorrido con teclado: la barra lateral y el enlace de salto se
// interponen, pero cinco paradas no necesitan más de cuarenta tabulaciones.
const MAX_TAB_STEPS = 40;

test.afterEach(() => {
  sql("DELETE FROM external_calendar_subscriptions WHERE owner_id='e2e-user'");
});

async function noHorizontalScroll(page) {
  const overflow = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }));
  expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth + 1);
}

async function controlsAreLargeEnough(page) {
  const boxes = await page
    .locator(".external-calendar button, .external-calendar input")
    .evaluateAll((nodes) =>
      nodes
        .filter((node) => node.getClientRects().length > 0)
        .map((node) => {
          const box = node.getBoundingClientRect();
          return {
            width: box.width,
            height: box.height,
            text: node.textContent,
          };
        }),
    );
  for (const box of boxes) {
    expect(box.height, box.text ?? "").toBeGreaterThanOrEqual(44);
    expect(box.width, box.text ?? "").toBeGreaterThanOrEqual(44);
  }
}

test("calendario externo audit: sin solapes ni scroll horizontal en toda la matriz @s40", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await expect(page.getByLabel("Etiqueta")).toBeVisible();
  for (const width of widths) {
    await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
    await noHorizontalScroll(page);
    await controlsAreLargeEnough(page);
  }
});

test("calendario externo audit: axe no encuentra violaciones en vacío y con suscripción @s40", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await expect(page.getByLabel("Etiqueta")).toBeVisible();
  const empty = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
    .analyze();
  expect(empty.violations).toEqual([]);

  await page
    .getByLabel("Etiqueta")
    .fill("Trabajo 😀 con resumen Unicode largo");
  await page.getByLabel("Dirección secreta iCal").fill(SECRET);
  await page.getByRole("button", { name: "Guardar" }).click();
  await expect(page.getByText("calendar.google.com")).toBeVisible();
  const configured = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
    .analyze();
  expect(configured.violations).toEqual([]);

  await page.getByRole("button", { name: "Sincronizar ahora" }).click();
  await expect(page.getByRole("status")).not.toHaveText("Sincronizando…");
  const afterSync = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
    .analyze();
  expect(afterSync.violations).toEqual([]);
  // axe automatiza reglas, no certifica un lector de pantalla real: la revisión
  // manual con lector sigue siendo obligatoria antes de dar la feature por buena.
  expect(afterSync.testEngine.name).toBe("axe-core");
});

test("calendario externo audit: el recorrido con teclado sigue el orden del contrato @s40", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await page.getByLabel("Etiqueta").fill("Trabajo");
  await page.getByLabel("Dirección secreta iCal").fill(SECRET);
  await page.getByRole("button", { name: "Guardar" }).click();
  await expect(page.getByText("calendar.google.com")).toBeVisible();
  await page.getByRole("link", { name: "Saltar al contenido" }).focus();
  const expected = [
    "Etiqueta",
    "Dirección secreta iCal",
    "Guardar",
    "Sincronizar ahora",
    "Eliminar suscripción",
  ];
  const seen = [];
  // Un fallo por parada, no uno global: el contrato pide foco visible «en cada
  // control», así que se mide dentro del bucle y se acumula el nombre del que
  // falle. `intruders` recoge lo que recibe el foco DENTRO del formulario sin
  // estar en el orden del contrato; fuera de él (saltar al contenido, barra
  // lateral) los intermedios son legítimos y no cuentan.
  const invisible = [];
  const intruders = [];
  for (
    let step = 0;
    step < MAX_TAB_STEPS && seen.length < expected.length;
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
    if (!expected.includes(stop.name)) {
      if (stop.insideForm && !intruders.includes(stop.name))
        intruders.push(stop.name);
      continue;
    }
    if (seen.at(-1) === stop.name) continue;
    seen.push(stop.name);
    // El anillo del producto es `:focus-visible { outline: 3px solid var(--accent) }`
    // (frontend/src/styles.scss). Se exige ese anillo y no el del agente de
    // usuario, que Chromium computa con `outline-style: auto`.
    const visible =
      stop.matchesFocusVisible &&
      stop.outlineStyle === "solid" &&
      stop.outlineWidth >= FOCUS_RING_MIN_WIDTH &&
      !stop.outlineColor.includes("transparent");
    if (!visible) invisible.push(stop);
  }
  expect(seen).toEqual(expected);
  expect(invisible).toEqual([]);
  expect(intruders).toEqual([]);
});

test("calendario externo audit: texto al 200 % no recorta la pantalla a 320 px @s40", async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 900 });
  await page.addInitScript(() => {
    document.documentElement.style.fontSize = "32px";
  });
  await page.goto("/calendario-externo");
  await expect(page.getByLabel("Etiqueta")).toBeVisible();
  await noHorizontalScroll(page);
  await controlsAreLargeEnough(page);
});
