import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import AxeBuilder from "@axe-core/playwright";
import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { chromium } from "@playwright/test";
import { loginSession } from "../scripts/session-client.mjs";

/**
 * @s38 completo: los siete estados de /calendario, a 320/768/1280, con texto al 200 %, con zoom
 * nativo al 200 % a 320 px y en tema claro, tema oscuro, forced-colors y movimiento reducido.
 * La revisión heurística de los treinta principios vive en progress/ux_ics_calendar.md; axe no
 * declara cumplimiento por sí solo (AGENTS.md:51).
 */

const WIDTHS = [320, 768, 1280];
const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"];
const MIN_TARGET = 44;

const minute = (offsetDays) => {
  const at = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  at.setUTCSeconds(0, 0);
  return at;
};
const utc = (at) => at.toISOString().replace(/\.\d{3}Z$/, "Z");
const local = (at) => utc(at).replace("Z", "");

function withoutFeedToken() {
  sql("DELETE FROM calendar_feed_tokens WHERE owner_id='e2e-user'");
}

async function plannedBlock(request) {
  withoutFeedToken();
  const suffix = randomUUID();
  const project = await create(request, `Calendario UX ${suffix}`, "");
  const task = await saveTask(request, project.id, `Revisión ${suffix}`, {
    completionCriterion: "Cerrar la revisión",
  });
  const start = minute(3);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  sql(
    `INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES ('${randomUUID()}','${project.id}','${task.id}','${randomUUID()}','Objetivo del bloque','${local(start)}','${local(end)}','UTC','+00:00','+00:00',false,'${utc(start)}','${utc(end)}',60,'${utc(start)}')`,
  );
}

const FIELD = { name: "Enlace de suscripción", exact: true };

/**
 * Los siete estados que nombra el escenario. «cargando» y «fallo» se provocan interceptando la
 * lectura de estado, que es la única petición que la vista emite al abrirse.
 */
async function enter(page, state) {
  if (state === "cargando") {
    await page.route("**/api/v1/me/calendar-feed", async () => {
      /* Se deja en vuelo a propósito: la vista debe anunciar el trabajo, no adivinarlo. */
    });
    await page.goto("/calendario");
    await expect(page.getByRole("status")).toHaveText("Cargando…");
    return;
  }
  if (state === "fallo") {
    await page.route("**/api/v1/me/calendar-feed", (route) =>
      route.fulfill({ status: 503, body: "" }),
    );
    await page.goto("/calendario");
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Reintentar", exact: true }),
    ).toBeVisible();
    return;
  }
  await page.goto("/calendario");
  await expect(
    page.getByRole("heading", { level: 1, name: "Calendario ICS" }),
  ).toBeVisible();
  if (state === "sin enlace") {
    await expect(
      page.getByRole("button", { name: "Crear enlace de suscripción" }),
    ).toBeVisible();
    return;
  }
  await page
    .getByRole("button", { name: "Crear enlace de suscripción" })
    .click();
  await expect(page.getByRole("textbox", FIELD)).toBeVisible();
  if (state === "enlace recién creado") return;
  if (state === "con enlace activo") {
    // Recargar: el mismo propietario con token activo, ya sin campo de url.
    await page.reload();
    await expect(
      page.getByRole("button", { name: "Regenerar enlace", exact: true }),
    ).toBeVisible();
    await expect(page.getByRole("textbox", FIELD)).toHaveCount(0);
    return;
  }
  if (state === "confirmación abierta") {
    await page
      .getByRole("button", { name: "Regenerar enlace", exact: true })
      .click();
    await expect(
      page.getByRole("group", { name: /dejará de funcionar/ }),
    ).toBeFocused();
    return;
  }
  if (state === "descarga preparada") {
    await page
      .getByRole("button", { name: "Descargar archivo .ics", exact: true })
      .click();
    await expect(page.getByText("Archivo preparado")).toBeVisible();
    return;
  }
  throw new Error(`estado desconocido: ${state}`);
}

const STATES = [
  "cargando",
  "sin enlace",
  "con enlace activo",
  "enlace recién creado",
  "confirmación abierta",
  "descarga preparada",
  "fallo",
];

/** Mide lo que el escenario nombra: desbordamiento, objetivos y recorte del campo de url. */
function geometry(page) {
  return page.evaluate((minimum) => {
    const root = document.documentElement;
    const controls = [
      ...document.querySelectorAll("main button, main a, main textarea"),
    ];
    const field = document.querySelector("main textarea");
    return {
      overflow: root.scrollWidth > root.clientWidth,
      widest: Math.max(0, ...controls.map((c) => c.getBoundingClientRect().width)),
      small: controls
        .filter((c) => {
          const box = c.getBoundingClientRect();
          return box.width < minimum || box.height < minimum;
        })
        .map((c) => `${c.tagName}:${(c.textContent || "").trim().slice(0, 24)}`),
      escaping: controls
        .filter((c) => c.getBoundingClientRect().right > root.clientWidth + 1)
        .map((c) => (c.textContent || "").trim().slice(0, 24)),
      fieldClipped: field
        ? field.scrollWidth > field.clientWidth + 1
        : false,
      fieldValue: field ? field.value : null,
    };
  }, MIN_TARGET);
}

async function audit(page, label, folder, options = {}) {
  const analyzer = new AxeBuilder({ page }).withTags(AXE_TAGS);
  // Chromium pinta colores del sistema mientras axe lee los declarados; el contraste forzado se
  // revisa a ojo y se conserva el resto de reglas (igual que appearance-ux-audit).
  if (options.forcedColors) analyzer.disableRules(["color-contrast"]);
  const { violations } = await analyzer.analyze();
  await writeFile(
    join(folder, `${label.replace(/[^a-z0-9]+/gi, "-")}-axe.json`),
    JSON.stringify(violations, null, 2),
  );
  expect(violations, `axe en ${label}`).toEqual([]);

  const measured = await geometry(page);
  expect(measured.overflow, `${label} desborda en horizontal`).toBe(false);
  expect(measured.escaping, `${label} saca controles del viewport`).toEqual([]);
  expect(measured.small, `${label} tiene objetivos menores de 44 px`).toEqual([]);
  expect(measured.fieldClipped, `${label} recorta la url`).toBe(false);
  return measured;
}

/** Duplica el tamaño de letra calculado de la vista, como reschedule-text y appearance-ux-audit. */
async function doubleText(page) {
  const scaled = await page.evaluate(() => {
    const elements = [...document.querySelectorAll("main, main *")];
    const before = elements.map((el) => parseFloat(getComputedStyle(el).fontSize));
    elements.forEach((el, index) => {
      el.style.fontSize = `${before[index] * 2}px`;
    });
    return elements.map((el, index) => ({
      before: before[index],
      after: parseFloat(getComputedStyle(el).fontSize),
    }));
  });
  expect(scaled.length).toBeGreaterThan(0);
  for (const size of scaled) expect(size.after).toBeCloseTo(size.before * 2, 3);
}

test("ics ux: los siete estados se sostienen a 320, 768 y 1280 px @s38", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180_000);
  const folder = resolve(".e2e-work", "ics-calendar-ux", "normal");
  await mkdir(folder, { recursive: true });
  const evidence = [];
  await plannedBlock(request);
  for (const state of STATES) {
    for (const width of WIDTHS) {
      // Cada medida parte de «sin enlace»: los estados que crean token no deben heredarse.
      withoutFeedToken();
      await page.setViewportSize({ width, height: 900 });
      await enter(page, state);
      const measured = await audit(page, `${state}-${width}`, folder);
      evidence.push({ state, width, ...measured });
      await page.unrouteAll({ behavior: "ignoreErrors" });
    }
  }
  expect(evidence).toHaveLength(STATES.length * WIDTHS.length);
  await writeFile(
    testInfo.outputPath("ics-calendar-ux-normal.json"),
    JSON.stringify(evidence, null, 2),
  );
});

test("ics ux: los siete estados se sostienen con el texto al 200 % @s38", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180_000);
  const folder = resolve(".e2e-work", "ics-calendar-ux", "text200");
  await mkdir(folder, { recursive: true });
  const evidence = [];
  await plannedBlock(request);
  for (const state of STATES) {
    for (const width of WIDTHS) {
      withoutFeedToken();
      await page.setViewportSize({ width, height: 900 });
      await enter(page, state);
      await doubleText(page);
      const measured = await audit(page, `${state}-${width}-text200`, folder);
      evidence.push({ state, width, ...measured });
      await page.screenshot({
        path: join(folder, `${state.replace(/ /g, "-")}-${width}.png`),
        fullPage: true,
      });
      await page.unrouteAll({ behavior: "ignoreErrors" });
    }
  }
  expect(evidence).toHaveLength(STATES.length * WIDTHS.length);
  // La url recién creada es el control de riesgo: 43 caracteres a 320 px con el texto doblado.
  const created = evidence.filter(
    (row) => row.state === "enlace recién creado" && row.width === 320,
  );
  expect(created).toHaveLength(1);
  expect(created[0].fieldValue).toMatch(
    /^https?:\/\/[^/]+\/calendar\/[A-Za-z0-9_-]{43}\.ics$/,
  );
  await writeFile(
    testInfo.outputPath("ics-calendar-ux-text200.json"),
    JSON.stringify(evidence, null, 2),
  );
});

test("ics ux: tema claro, tema oscuro, forced-colors y movimiento reducido @s38", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180_000);
  const folder = resolve(".e2e-work", "ics-calendar-ux", "modes");
  await mkdir(folder, { recursive: true });
  // Cada modo fija las tres preferencias: emulateMedia conserva las que no se nombran, y sin esto
  // forced-colors se colaba en la pasada de movimiento reducido y falseaba la medida.
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
      name: "movimiento-reducido",
      media: { ...base, reducedMotion: "reduce" },
    },
  ];
  const evidence = [];
  await plannedBlock(request);
  for (const mode of modes) {
    await page.emulateMedia(mode.media);
    for (const state of ["sin enlace", "enlace recién creado", "fallo"]) {
      withoutFeedToken();
      await page.setViewportSize({ width: 320, height: 900 });
      await enter(page, state);
      const measured = await audit(
        page,
        `${mode.name}-${state}`,
        folder,
        mode.forced ? { forcedColors: true } : {},
      );
      const painted = await page.evaluate(() => {
        const main = document.querySelector("main");
        const style = getComputedStyle(main);
        // El lienzo real: el primer ancestro que de verdad pinta, no un fondo transparente.
        const opaque = (element) => {
          for (let node = element; node; node = node.parentElement) {
            const colour = getComputedStyle(node).backgroundColor;
            if (colour && !/rgba\(0, 0, 0, 0\)|transparent/.test(colour))
              return colour;
          }
          return getComputedStyle(document.documentElement).backgroundColor;
        };
        return {
          ink: style.color,
          canvas: opaque(main),
          forcedColors: matchMedia("(forced-colors: active)").matches,
          reducedMotion: matchMedia("(prefers-reduced-motion: reduce)").matches,
          transitions: [...document.querySelectorAll("main, main *")].map((el) =>
            parseFloat(getComputedStyle(el).transitionDuration),
          ),
        };
      });
      // Legibilidad mínima comprobable por máquina: tinta y lienzo no colapsan en el mismo color.
      expect(painted.ink, `${mode.name}/${state}`).not.toBe(painted.canvas);
      if (mode.name === "forced-colors") expect(painted.forcedColors).toBe(true);
      if (mode.name === "movimiento-reducido") {
        expect(painted.reducedMotion).toBe(true);
        expect(painted.transitions.every((value) => value <= 0.01)).toBe(true);
      }
      // La operación se conserva: el control principal del estado sigue activable.
      const control =
        state === "fallo"
          ? page.getByRole("button", { name: "Reintentar", exact: true })
          : state === "sin enlace"
            ? page.getByRole("button", { name: "Crear enlace de suscripción" })
            : page.getByRole("button", { name: "Copiar enlace", exact: true });
      await expect(control).toBeEnabled();
      await control.focus();
      await expect(control).toBeFocused();
      evidence.push({ mode: mode.name, state, ...measured, ...painted });
      await page.unrouteAll({ behavior: "ignoreErrors" });
    }
  }
  await page.emulateMedia({
    colorScheme: null,
    forcedColors: null,
    reducedMotion: null,
  });
  await writeFile(
    testInfo.outputPath("ics-calendar-ux-modes.json"),
    JSON.stringify(evidence, null, 2),
  );
});

test("ics ux: el recorrido de teclado alcanza todo en orden, con foco visible y sin trampas @s38", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180_000);
  const evidence = [];
  await plannedBlock(request);
  for (const state of [
    "sin enlace",
    "enlace recién creado",
    "confirmación abierta",
  ]) {
    withoutFeedToken();
    await page.setViewportSize({ width: 320, height: 900 });
    await enter(page, state);

    const expected = await page.evaluate(() =>
      [...document.querySelectorAll("main button, main a, main textarea")].map(
        (el) => el.id || (el.textContent || "").trim().slice(0, 32),
      ),
    );
    expect(expected.length).toBeGreaterThan(0);

    await page.evaluate(() => document.body.focus());
    await page.locator("h1").focus();
    const visited = [];
    const focusStyles = [];
    let leftMain = false;
    for (let step = 0; step < expected.length + 12; step++) {
      await page.keyboard.press("Tab");
      const spot = await page.evaluate(() => {
        const el = document.activeElement;
        if (!el || el === document.body) return null;
        const inMain = Boolean(el.closest("main"));
        const style = getComputedStyle(el);
        return {
          inMain,
          key: el.id || (el.textContent || "").trim().slice(0, 32),
          tag: el.tagName,
          name:
            el.getAttribute("aria-label") ||
            (el.labels && el.labels[0] && el.labels[0].textContent.trim()) ||
            (el.textContent || "").trim(),
          outline: parseFloat(style.outlineWidth) || 0,
          outlineStyle: style.outlineStyle,
          shadow: style.boxShadow,
        };
      });
      if (!spot) {
        leftMain = true;
        break;
      }
      if (spot.inMain) {
        if (!visited.includes(spot.key)) visited.push(spot.key);
        focusStyles.push(spot);
      } else if (visited.length === expected.length) {
        leftMain = true;
        break;
      }
    }

    // Orden lógico: se recorren todos los controles de main en el orden del DOM.
    expect(visited, `orden en ${state}`).toEqual(expected);
    // Nombre accesible y foco visible en cada parada.
    for (const spot of focusStyles) {
      expect(spot.name, `nombre accesible en ${state}`).not.toBe("");
      const visible =
        (spot.outline > 0 && spot.outlineStyle !== "none") ||
        spot.shadow !== "none";
      expect(visible, `foco visible en ${state}/${spot.key}`).toBe(true);
    }
    // Sin trampa de foco: tabulando hacia delante se sale de main tras el último control...
    expect(leftMain, `trampa de foco hacia delante en ${state}`).toBe(true);
    // ...y hacia atrás desde el primero también se sale.
    await page.locator("main button, main textarea").first().focus();
    await page.keyboard.press("Shift+Tab");
    const backwards = await page.evaluate(() =>
      Boolean(document.activeElement && document.activeElement.closest("main")),
    );
    expect(backwards, `trampa de foco hacia atrás en ${state}`).toBe(false);
    evidence.push({ state, visited, leftMain, backwardsStillInMain: backwards });

    if (state === "enlace recién creado") {
      // El campo de url se selecciona entero sólo con teclado.
      const field = page.getByRole("textbox", FIELD);
      await field.focus();
      await page.keyboard.press("ControlOrMeta+a");
      const selection = await field.evaluate((el) => ({
        start: el.selectionStart,
        end: el.selectionEnd,
        length: el.value.length,
        value: el.value,
      }));
      expect(selection.start).toBe(0);
      expect(selection.end).toBe(selection.length);
      expect(selection.value).toMatch(
        /^https?:\/\/[^/]+\/calendar\/[A-Za-z0-9_-]{43}\.ics$/,
      );
    }
    await page.unrouteAll({ behavior: "ignoreErrors" });
  }
  await writeFile(
    testInfo.outputPath("ics-calendar-ux-keyboard.json"),
    JSON.stringify(evidence, null, 2),
  );
});

test("ics ux: zoom nativo de Chromium al 200 % con 320 px CSS @s38", async ({
  request,
  browserName,
}, testInfo) => {
  test.skip(browserName !== "chromium", "El zoom nativo se mide en Chromium");
  test.setTimeout(180_000);
  await plannedBlock(request);
  const baseURL = process.env.E2E_BASE_URL ?? "http://127.0.0.1:18080";
  const scratch = resolve(
    ".e2e-work",
    "ics-calendar-native-zoom",
    process.env.E2E_COMPOSE_PROJECT ?? "local",
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated calendar zoom QA",
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
        "--window-size=760,1000",
      ],
    },
  );
  try {
    await loginSession(context.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    const page = await context.newPage();
    await page.goto("/calendario");
    await page
      .getByRole("button", { name: "Crear enlace de suscripción" })
      .click();
    await expect(page.getByRole("textbox", FIELD)).toBeVisible();

    const before = await page.evaluate(() => devicePixelRatio);
    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const zoom = await worker.evaluate(async (origin) => {
      const [tab] = await chrome.tabs.query({ url: `${origin}/*` });
      await chrome.tabs.setZoom(tab.id, 2);
      return chrome.tabs.getZoom(tab.id);
    }, baseURL);
    expect(zoom).toBe(2);
    await expect.poll(() => page.evaluate(() => devicePixelRatio)).toBe(
      before * 2,
    );
    // Con el zoom al 200 % la ventana de 760 px deja unos 320 px CSS de ancho útil.
    await expect
      .poll(() => page.evaluate(() => document.documentElement.clientWidth))
      .toBeLessThanOrEqual(400);

    const measured = await geometry(page);
    expect(measured.overflow, "zoom nativo 200 % desborda").toBe(false);
    expect(measured.escaping, "zoom nativo saca controles").toEqual([]);
    expect(measured.fieldClipped, "zoom nativo recorta la url").toBe(false);
    expect(measured.small, "zoom nativo encoge objetivos").toEqual([]);
    await expect(
      page.getByRole("button", { name: "Copiar enlace", exact: true }),
    ).toBeEnabled();

    const { violations } = await new AxeBuilder({ page })
      .withTags(AXE_TAGS)
      .analyze();
    expect(violations, "axe con zoom nativo").toEqual([]);
    const shot = join(scratch, "zoom200.png");
    await page.screenshot({ path: shot, fullPage: true });
    await writeFile(
      testInfo.outputPath("ics-calendar-native-zoom.json"),
      JSON.stringify(
        {
          zoom,
          clientWidth: await page.evaluate(
            () => document.documentElement.clientWidth,
          ),
          devicePixelRatio: await page.evaluate(() => devicePixelRatio),
          measured,
          method: "chrome.tabs.setZoom real, sin viewport emulado ni zoom CSS",
          screenshot: shot,
        },
        null,
        2,
      ),
    );
  } finally {
    await context.close();
  }
});
