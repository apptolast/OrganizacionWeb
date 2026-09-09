import { test, expect } from "./support/authenticated-test.mjs";
import AxeBuilder from "@axe-core/playwright";
import { sql } from "./support/projects.mjs";

// Matriz de docs/ux-requirements.md: los cuatro anchos del contrato y ambos
// lados de cada punto de ruptura.
const widths = [
  320, 359, 360, 361, 599, 600, 601, 767, 768, 769, 1279, 1280, 1281, 2560,
];
const SECRET = "https://calendar.google.com/calendar/ical/e2e/private-WXYZ.ics";
const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"];
// Grosor declarado por la regla `:focus-visible` de frontend/src/styles.scss.
const FOCUS_RING_MIN_WIDTH = 3;
// Cota del recorrido con teclado: la barra lateral y el enlace de salto se
// interponen, pero cinco paradas no necesitan más de cuarenta tabulaciones.
const MAX_TAB_STEPS = 40;
// Objetivo táctil del producto (docs/ux-requirements.md), no una cifra de la ley de Fitts.
const MIN_TARGET = 44;
// Cuántos eventos tiene la «lista larga» del contrato. Suficiente para que la página crezca varias
// veces la altura del viewport sin convertir la auditoría en una prueba de rendimiento.
const LONG_LIST = 60;

/**
 * Los estados de pantalla del Given de @s40 (`features/external_calendar.feature:542`).
 *
 * Los cinco primeros son literalmente los que el contrato enumera. El sexto —el diálogo de
 * confirmación abierto— se añade porque «Sí, eliminar» y «Cancelar» son controles que el Then de la
 * línea 546 obliga a medir a 44 × 44 px y que sólo existen dentro de `{confirming ? …}`
 * (`frontend/src/external-calendar.tsx:374`): sin este estado, tres de los siete controles de la
 * pantalla no entrarían en ninguna medición. Es una ampliación del estado «con suscripción», no una
 * sustitución de ninguna fila del contrato.
 */
const STATES = [
  "vacío",
  "con suscripción",
  "con error",
  "lista larga de resúmenes Unicode",
  "guardando",
  "confirmando la eliminación",
];

// Bytes cualesquiera de longitud válida: la fila exige `octet_length(url_ciphertext) > 12` y la
// pantalla no descifra nada, sólo pinta `url_host` y `url_tail`.
const CIPHERTEXT = "decode('000102030405060708090a0b0c0d0e0f','hex')";
// Etiqueta con emoji y acentos construida con `chr()`: el texto viaja a `psql` como argumento de
// proceso, y en Windows los caracteres fuera de ASCII se pueden estropear por el camino. Así lo que
// llega a la base es exactamente lo que se pretende.
const LABEL_SQL = "'Trabajo ' || chr(128512) || ' ' || chr(233) || chr(241)";
// Resumen Unicode largo: 120 puntos de código de emoji y acentos más una palabra sin espacios de
// 280 caracteres, que es el caso que de verdad puede desbordar una caja.
const LONG_SUMMARY_SQL =
  "repeat(chr(128512) || chr(233) || chr(241) || chr(120), 30)" +
  " || repeat('Zusammenarbeitsvereinbarungsentwurf', 8)";
const SHORT_SUMMARY_SQL = "'Reuni' || chr(243) || 'n de equipo ' || g::text";

function forget() {
  sql("DELETE FROM external_calendar_subscriptions WHERE owner_id='e2e-user'");
}

test.afterEach(() => forget());

function seedSubscription({
  status = "OK",
  error = null,
  imported = 3,
  truncated = false,
} = {}) {
  sql(
    "INSERT INTO external_calendar_subscriptions(owner_id,id,label,url_ciphertext," +
      "url_host,url_tail,version,created_at,updated_at,last_attempt_at,last_sync_at," +
      "last_status,last_error,snapshot_zone_id,imported,skipped_recurring," +
      "skipped_cancelled,skipped_invalid,truncated) VALUES ('e2e-user',gen_random_uuid()," +
      LABEL_SQL +
      "," +
      CIPHERTEXT +
      ",'calendar.google.com','.ics',0,now(),now(),now()," +
      (status === "OK" ? "now()" : "NULL") +
      ",'" +
      status +
      "'," +
      (error === null ? "NULL" : "'" + error + "'") +
      ",'Europe/Madrid'," +
      imported +
      ",1,2,0," +
      truncated +
      ")",
  );
}

function seedEvents(count, long) {
  sql(
    "INSERT INTO external_calendar_events(owner_id,uid,summary,start_at,end_at,all_day)" +
      " SELECT 'e2e-user','uid-'||g," +
      (long ? LONG_SUMMARY_SQL : SHORT_SUMMARY_SQL) +
      ", now() + (g || ' hours')::interval, now() + ((g+1) || ' hours')::interval, g = 2" +
      " FROM generate_series(1," +
      count +
      ") AS g",
  );
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_events WHERE owner_id='e2e-user'",
    ),
    "la siembra de eventos no llegó a la base",
  ).toBe(String(count));
}

/**
 * Deja la pantalla en el estado pedido y devuelve la función que lo deshace.
 *
 * Los estados con datos se siembran por SQL en vez de por la interfaz a propósito: con la guardia
 * SSRF activa no hay ningún feed iCalendar alcanzable desde el contenedor, así que una
 * sincronización real nunca termina en `lastStatus OK` y la lista de eventos jamás se pintaría. La
 * bitácora de la feature daba por eso el estado «con lista larga» por inalcanzable; no lo es:
 * `e2e/support/projects.mjs` ejecuta SQL contra el postgres de la pila, y la pantalla lee esas
 * mismas filas por `GET …/external-calendar/events`.
 */
async function enter(page, state) {
  forget();
  if (state === "con suscripción" || state === "confirmando la eliminación") {
    seedSubscription();
    seedEvents(3, false);
  } else if (state === "con error") {
    seedSubscription({
      status: "FAILED",
      error: "FEED_HTTP_ERROR",
      imported: 0,
    });
  } else if (state === "lista larga de resúmenes Unicode") {
    seedSubscription({ imported: LONG_LIST, truncated: true });
    seedEvents(LONG_LIST, true);
  }

  await page.goto("/calendario-externo");
  await expect(page.getByLabel("Etiqueta")).toBeVisible();

  if (state === "vacío") {
    await expect(
      page.getByText("Todavía no tienes ningún calendario externo."),
    ).toBeVisible();
    return async () => {};
  }
  if (state === "con suscripción") {
    await expect(page.getByText("calendar.google.com")).toBeVisible();
    await expect(page.getByRole("listitem")).toHaveCount(3);
    return async () => {};
  }
  if (state === "con error") {
    await expect(page.getByRole("alert")).toContainText(
      "El proveedor respondió",
    );
    return async () => {};
  }
  if (state === "lista larga de resúmenes Unicode") {
    await expect(page.getByRole("listitem")).toHaveCount(LONG_LIST);
    await expect(page.getByRole("note")).toBeVisible();
    return async () => {};
  }
  if (state === "confirmando la eliminación") {
    await expect(page.getByText("calendar.google.com")).toBeVisible();
    await page.getByRole("button", { name: "Eliminar suscripción" }).click();
    await expect(page.getByRole("alertdialog")).toBeVisible();
    return async () => {};
  }

  // «Guardando»: la respuesta del PUT se retiene con una promesa que sólo se libera al salir del
  // estado. Sin esto el estado dura milisegundos y no se puede medir en catorce anchos.
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  await page.route(
    (url) => url.pathname === "/api/v1/me/external-calendar",
    async (route) => {
      if (route.request().method() !== "PUT") {
        await route.fallback();
        return;
      }
      await held;
      await route.abort();
    },
  );
  await page
    .getByLabel("Etiqueta")
    .fill("Trabajo 😀 con resumen Unicode largo");
  await page.getByLabel("Dirección secreta iCal").fill(SECRET);
  await page.getByRole("button", { name: "Guardar" }).click();
  await expect(page.getByRole("status")).toHaveText("Guardando…");
  await expect(page.getByRole("button", { name: "Guardar" })).toBeDisabled();
  return async () => {
    release();
    await page.unrouteAll({ behavior: "ignoreErrors" });
  };
}

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

test("calendario externo audit: sin solapes, recortes ni scroll horizontal en los seis estados y toda la matriz @s40", async ({
  page,
}) => {
  test.setTimeout(300_000);
  const seen = [];
  for (const state of STATES) {
    const leave = await enter(page, state);
    try {
      for (const width of widths) {
        // A 768 px se fuerza una altura corta: es donde el recorte vertical es más probable, y hasta
        // ahora era justo el caso que el oráculo de sólo `scrollWidth` no podía ver.
        await page.setViewportSize({
          width,
          height: width === 768 ? 400 : 900,
        });
        const measured = await nothingBreaksAt(page, `${state} a ${width} px`);
        seen.push({ state, width, controls: measured.controls });
      }
    } finally {
      await leave();
    }
    await page.setViewportSize({ width: 1280, height: 900 });
  }
  expect(seen).toHaveLength(STATES.length * widths.length);
  // El estado con suscripción tiene más controles que el vacío: si la preparación de estados dejara
  // de funcionar, la matriz volvería a medir seis veces el formulario vacío sin avisar.
  const controlsOf = (state) =>
    seen.find((row) => row.state === state).controls;
  expect(
    controlsOf("con suscripción"),
    "el estado con suscripción no pintó Sincronizar ni Eliminar",
  ).toBeGreaterThan(controlsOf("vacío"));
  expect(
    controlsOf("confirmando la eliminación"),
    "el diálogo de confirmación no aportó sus dos botones",
  ).toBe(controlsOf("con suscripción") + 2);
});

test("calendario externo audit: axe no encuentra violaciones en vacío y con suscripción @s40", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await expect(page.getByLabel("Etiqueta")).toBeVisible();
  const empty = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
  expect(empty.violations).toEqual([]);

  await page
    .getByLabel("Etiqueta")
    .fill("Trabajo 😀 con resumen Unicode largo");
  await page.getByLabel("Dirección secreta iCal").fill(SECRET);
  await page.getByRole("button", { name: "Guardar" }).click();
  await expect(page.getByText("calendar.google.com")).toBeVisible();
  const configured = await new AxeBuilder({ page })
    .withTags(AXE_TAGS)
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
  const afterSync = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
  expect(afterSync.violations).toEqual([]);
  // axe automatiza reglas, no certifica un lector de pantalla real: la revisión
  // manual con lector sigue siendo obligatoria antes de dar la feature por buena.
  expect(afterSync.testEngine.name).toBe("axe-core");
});

/**
 * @s40 remite a la matriz de `docs/ux-requirements.md`, cuya línea 54 exige que «toda variante
 * personalizable (tema, densidad, tamaño de texto, paneles) pase sus verificaciones». Hasta hoy
 * esta pantalla sólo se auditaba en el tema por defecto, aunque la aplicación implementa
 * `prefers-color-scheme: dark` (`frontend/src/styles.scss:53`) y `prefers-reduced-motion`
 * (`:1293`), y aunque otras nueve specs del repositorio ya usan `emulateMedia`.
 *
 * Cada modo fija **las tres** preferencias: `emulateMedia` conserva las que no se nombran, y sin
 * esto `forced-colors` se cuela en la pasada de movimiento reducido y falsea la medida (lección ya
 * escrita en `e2e/ics-calendar-ux.spec.mjs:304`).
 */
test("calendario externo audit: claro, oscuro, forced-colors y movimiento reducido en los seis estados @s40", async ({
  page,
}) => {
  test.setTimeout(300_000);
  const base = {
    colorScheme: "light",
    forcedColors: "none",
    reducedMotion: "no-preference",
  };
  const modes = [
    { name: "claro", media: { ...base } },
    { name: "oscuro", media: { ...base, colorScheme: "dark" } },
    {
      name: "forced-colors",
      media: { ...base, forcedColors: "active" },
      forced: true,
    },
    {
      name: "movimiento reducido",
      media: { ...base, reducedMotion: "reduce" },
    },
  ];
  const evidence = [];
  try {
    for (const mode of modes) {
      await page.emulateMedia(mode.media);
      for (const state of STATES) {
        const leave = await enter(page, state);
        try {
          const analyzer = new AxeBuilder({ page }).withTags(AXE_TAGS);
          // Chromium pinta colores del sistema mientras axe lee los declarados: bajo colores
          // forzados la regla de contraste mide una paleta que el usuario no ve. Se omite sólo esa
          // regla y sólo en esa pasada, como en appearance-ux-audit e ics-calendar-ux.
          if (mode.forced) analyzer.disableRules(["color-contrast"]);
          const { violations } = await analyzer.analyze();
          expect(violations, `axe en ${mode.name} / ${state}`).toEqual([]);
          await nothingBreaksAt(page, `${mode.name} / ${state}`);

          const painted = await page.evaluate(() => {
            const main = document.querySelector(".external-calendar");
            const opaque = (element) => {
              for (let node = element; node; node = node.parentElement) {
                const colour = getComputedStyle(node).backgroundColor;
                if (colour && !/rgba\(0, 0, 0, 0\)|transparent/.test(colour))
                  return colour;
              }
              return getComputedStyle(document.documentElement).backgroundColor;
            };
            return {
              ink: getComputedStyle(main).color,
              canvas: opaque(main),
              forcedColors: matchMedia("(forced-colors: active)").matches,
              dark: matchMedia("(prefers-color-scheme: dark)").matches,
              reducedMotion: matchMedia("(prefers-reduced-motion: reduce)")
                .matches,
              moving: [
                ...document.querySelectorAll(
                  ".external-calendar, .external-calendar *",
                ),
              ].filter((element) => {
                const style = getComputedStyle(element);
                return (
                  parseFloat(style.transitionDuration) > 0.01 ||
                  parseFloat(style.animationDuration) > 0.01
                );
              }).length,
            };
          });
          // Legibilidad mínima comprobable por máquina: tinta y lienzo no colapsan en el mismo
          // color. Es la comprobación que en el carril de automatizaciones destapó un contraste de
          // 1,01 sobre 1 en un control que jamás se había renderizado en una corrida medida.
          expect(painted.ink, `${mode.name} / ${state}`).not.toBe(
            painted.canvas,
          );
          if (mode.name === "oscuro") expect(painted.dark).toBe(true);
          if (mode.forced) expect(painted.forcedColors).toBe(true);
          if (mode.name === "movimiento reducido")
            expect(painted.reducedMotion).toBe(true);
          evidence.push({ mode: mode.name, state, moving: painted.moving });
        } finally {
          await leave();
        }
      }
    }
  } finally {
    await page.emulateMedia({
      colorScheme: null,
      forcedColors: null,
      reducedMotion: null,
    });
  }
  expect(evidence).toHaveLength(modes.length * STATES.length);
  // Declaración honesta del alcance: la pasada de movimiento reducido es **vacua** en esta
  // pantalla. No hay ni una transición ni una animación que reducir ni con la preferencia puesta ni
  // sin ella, así que se ejecuta por conformidad con la matriz, no como evidencia de que algo se
  // haya atenuado. Si algún día se añade movimiento aquí, esta aserción caerá y habrá que medirlo
  // de verdad en vez de suponerlo.
  const moving = (name) =>
    evidence
      .filter((row) => row.mode === name)
      .reduce((a, b) => a + b.moving, 0);
  expect(moving("claro"), "hay movimiento sin la preferencia puesta").toBe(0);
  expect(moving("movimiento reducido")).toBe(0);
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

/**
 * Duplica el tamaño de letra **calculado** de cada elemento de la pantalla y comprueba que se ha
 * duplicado de verdad, como `e2e/ics-calendar-ux.spec.mjs`, `reschedule-text` y
 * `appearance-ux-audit`.
 *
 * La versión anterior de esta prueba hacía `addInitScript(() => documentElement.style.fontSize =
 * "32px")` y no comprobaba nada. Al ejecutar la auditoría por primera vez, ese valor medía **16 px**
 * en el navegador: el texto nunca se amplió y la prueba llevaba desde su nacimiento midiendo la
 * pantalla a tamaño normal con el título «texto al 200 %». Y aunque hubiera funcionado, tampoco
 * habría bastado: la hoja de estilos declara la mayoría de sus tamaños en píxeles
 * (`frontend/src/styles.scss:658`, `:671`, `:692`…), que no dependen del `font-size` de la raíz.
 * Por eso se escala elemento a elemento y se afirma el resultado.
 */
async function doubleText(page) {
  const scaled = await page.evaluate(() => {
    const elements = [
      ...document.querySelectorAll(".external-calendar, .external-calendar *"),
    ];
    const before = elements.map((element) =>
      parseFloat(getComputedStyle(element).fontSize),
    );
    elements.forEach((element, index) => {
      // Con prioridad: la hoja global declara `.quiet-note { font-size: 14px !important }`
      // (`frontend/src/styles.scss:1095`), y una declaración en línea sin `!important` pierde
      // contra ella. Sin esta prioridad el estado vacío se mediría a tamaño normal y la prueba
      // volvería a mentir en su título. La regla no es un defecto de accesibilidad —el zoom del
      // navegador sí escala esos píxeles, y eso lo mide
      // `e2e/external-calendar-native-zoom.spec.mjs`—, pero sí impide emular la ampliación
      // desde la prueba, así que se declara aquí en vez de tocar una hoja compartida.
      element.style.setProperty(
        "font-size",
        `${before[index] * 2}px`,
        "important",
      );
    });
    return elements.map((element, index) => ({
      before: before[index],
      after: parseFloat(getComputedStyle(element).fontSize),
    }));
  });
  expect(scaled.length, "no se encontró texto que ampliar").toBeGreaterThan(0);
  const stubborn = scaled.filter(
    (size) => Math.abs(size.after - size.before * 2) > 0.01,
  );
  expect(stubborn, "elementos que no llegaron al 200 %").toEqual([]);
}

test("calendario externo audit: texto al 200 % no recorta ninguno de los seis estados a 320 px @s40", async ({
  page,
}) => {
  test.setTimeout(180_000);
  await page.setViewportSize({ width: 320, height: 900 });
  for (const state of STATES) {
    const leave = await enter(page, state);
    try {
      await doubleText(page);
      await nothingBreaksAt(page, `texto al 200 % a 320 px, ${state}`);
    } finally {
      await leave();
    }
  }
});
