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
// Objetivo táctil del producto (docs/ux-requirements.md), no una cifra de la ley de Fitts.
const MIN_TARGET = 44;

test.afterEach(() => {
  sql("DELETE FROM external_calendar_subscriptions WHERE owner_id='e2e-user'");
});

/**
 * Mide las tres cosas que el Then de @s40 nombra —«no hay solapes, recortes ni scroll horizontal
 * accidental»— y no sólo la tercera.
 *
 * El oráculo anterior comparaba `document.documentElement.scrollWidth` con `clientWidth` y nada
 * más: era ciego al recorte vertical y a los solapes, que es exactamente el fallo que en la feature
 * hermana dejó pasar un textarea con 282 px de contenido en 153 visibles
 * (`progress/ux_ics_calendar.md`). Aquí se mide, como en `e2e/ics-calendar-ux.spec.mjs`:
 *
 * - **desbordamiento** de la página, a lo ancho;
 * - **recorte por elemento y en los dos ejes**: un elemento recorta cuando su `overflow` en ese eje
 *   no es `visible` y su contenido no cabe. Con `overflow: visible` el contenido se sale a la vista
 *   pero no se pierde, y el desbordamiento de la página ya se comprueba aparte;
 * - **solapes** entre los controles y las regiones con nombre, por pares de rectángulos;
 * - **objetivos** de 44 × 44 px CSS, y que ningún control se salga del viewport.
 *
 * Excepción documentada, no lista blanca silenciosa: `INPUT`, `SELECT` y `TEXTAREA` **siempre**
 * tienen `scrollWidth > clientWidth` cuando su valor es más largo que la caja, y eso es su
 * comportamiento correcto —el usuario recorre el valor con el cursor, no se le pierde texto—. Si se
 * contaran como recorte, la pantalla daría falso positivo con cualquier dirección iCal larga, que
 * es justo el dato que este formulario recibe. Se excluyen del recorte y **no** de las demás
 * medidas: siguen entrando en objetivos, en solapes y en escape del viewport.
 */
function geometry(page) {
  return page.evaluate((minimum) => {
    const root = document.documentElement;
    const describe = (element) =>
      `${element.tagName}${element.id ? "#" + element.id : ""}:${(
        element.getAttribute("aria-label") ||
        element.labels?.[0]?.textContent ||
        element.textContent ||
        ""
      )
        .trim()
        .slice(0, 28)}`;
    const controls = [
      ...document.querySelectorAll(
        ".external-calendar button, .external-calendar input, .external-calendar a",
      ),
    ].filter((node) => node.getClientRects().length > 0);
    // Los pares se comparan entre controles y entre las cajas con nombre: dos hermanos que se pisan
    // son un solape real; un botón dentro de su propia región, no.
    const boxed = [
      ...controls,
      ...document.querySelectorAll(
        ".external-calendar .field, .external-calendar .form-footer > *, .external-calendar dt, .external-calendar dd, .external-calendar li",
      ),
    ].filter((node) => node.getClientRects().length > 0);
    const overlaps = [];
    for (let i = 0; i < boxed.length; i++)
      for (let j = i + 1; j < boxed.length; j++) {
        const a = boxed[i];
        const b = boxed[j];
        if (a.contains(b) || b.contains(a)) continue;
        const one = a.getBoundingClientRect();
        const two = b.getBoundingClientRect();
        const horizontal =
          Math.min(one.right, two.right) - Math.max(one.left, two.left);
        const vertical =
          Math.min(one.bottom, two.bottom) - Math.max(one.top, two.top);
        // Un píxel de tolerancia: los bordes adyacentes redondean.
        if (horizontal > 1 && vertical > 1)
          overlaps.push(`${describe(a)} ⨯ ${describe(b)}`);
      }
    return {
      overflow: root.scrollWidth > root.clientWidth + 1,
      small: controls
        .filter((node) => {
          const box = node.getBoundingClientRect();
          return box.width < minimum || box.height < minimum;
        })
        .map(describe),
      escaping: controls
        .filter((node) => {
          const box = node.getBoundingClientRect();
          return box.right > root.clientWidth + 1 || box.left < -1;
        })
        .map(describe),
      clipped: [
        ...document.querySelectorAll(
          ".external-calendar, .external-calendar *",
        ),
      ]
        .filter(
          (node) => !["INPUT", "SELECT", "TEXTAREA"].includes(node.tagName),
        )
        .map((element) => {
          const style = getComputedStyle(element);
          const horizontal =
            style.overflowX !== "visible" &&
            element.scrollWidth > element.clientWidth + 1;
          const vertical =
            style.overflowY !== "visible" &&
            element.scrollHeight > element.clientHeight + 1;
          if (!horizontal && !vertical) return null;
          return `${describe(element)} [${horizontal ? "ancho" : "alto"} ${
            horizontal ? element.scrollWidth : element.scrollHeight
          } en ${horizontal ? element.clientWidth : element.clientHeight}]`;
        })
        .filter(Boolean),
      overlaps,
      controls: controls.length,
    };
  }, MIN_TARGET);
}

async function nothingBreaksAt(page, label) {
  const measured = await geometry(page);
  expect(
    measured.controls,
    `${label} no encontró ningún control`,
  ).toBeGreaterThan(0);
  expect(
    measured.overflow,
    `${label}: desplazamiento horizontal de la página`,
  ).toBe(false);
  expect(measured.escaping, `${label}: controles fuera del viewport`).toEqual(
    [],
  );
  expect(
    measured.small,
    `${label}: objetivos menores de ${MIN_TARGET} px`,
  ).toEqual([]);
  expect(
    measured.clipped,
    `${label}: contenido recortado (ancho o alto)`,
  ).toEqual([]);
  expect(measured.overlaps, `${label}: cajas que se pisan`).toEqual([]);
  return measured;
}

test("calendario externo audit: sin solapes, recortes ni scroll horizontal en toda la matriz @s40", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await expect(page.getByLabel("Etiqueta")).toBeVisible();
  for (const width of widths) {
    // A 768 px se fuerza una altura corta: es donde el recorte vertical es más probable, y hasta
    // ahora era justo el caso que el oráculo de sólo `scrollWidth` no podía ver.
    await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
    await nothingBreaksAt(page, `vacío a ${width} px`);
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
  // Espera POSITIVA, no negativa. La versión anterior era `not.toHaveText("Sincronizando…")`, que
  // en Playwright pasa de inmediato si el anuncio no aparece jamás: no esperaba nada y no probaba
  // nada. Aquí se exige que el anuncio aparezca y sólo después que ceda al resultado; el plazo largo
  // es el del contrato, cinco segundos de descarga más el viaje.
  await expect(page.getByRole("status")).toHaveText("Sincronizando…");
  await expect(page.getByRole("status")).toHaveText(
    /^Sincroniza(do\.|ción fallida\.)$/,
    { timeout: 20_000 },
  );
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
  await nothingBreaksAt(page, "texto al 200 % a 320 px");
});
