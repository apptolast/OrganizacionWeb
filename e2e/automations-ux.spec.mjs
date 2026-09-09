import { test, expect } from "./support/authenticated-test.mjs";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";
import { chromium } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { create, sql } from "./support/projects.mjs";

// Auditoría UX de /automatizaciones. Cubre los siete estados que exige
// docs/ux-requirements.md: los cuatro anchos, texto al 200 %, zoom nativo, los
// dos temas, forced-colors y prefers-reduced-motion. La matriz de principios y
// sus límites viven en progress/ux_automations.md; axe por sí solo no declara
// cumplimiento (AGENTS.md, «Referencia obligatoria de interfaces»).

const WIDTHS = [320, 768, 1280, 1440];
const AXE_TAGS = ["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"];

function clearRules() {
  sql(
    "DELETE FROM automation_runs WHERE owner_id='e2e-user'; DELETE FROM automation_rules WHERE owner_id='e2e-user'; DELETE FROM automation_cursors WHERE owner_id='e2e-user'",
  );
}

test.beforeEach(() => clearRules());
test.afterEach(() => clearRules());

// Nombre largo a proposito: a 320 px el riesgo real de esta pantalla es el
// reflujo del `li` con cinco hijos en flex-wrap, y sin reglas no hay ni un `li`.
const LONG = "Seguimiento de las tareas creadas en Marketing durante 2027";

async function seedRule(request, project, name, enabled) {
  const response = await request.post("/api/v1/me/automations", {
    headers: await csrfHeaders(request),
    data: {
      name,
      enabled,
      trigger: { eventType: "TaskCreated.v1" },
      condition: null,
      action: {
        type: "CREATE_TASK",
        projectId: project,
        titleTemplate: "Revisar {{task.title}} en {{project.name}}",
        criterionTemplate: "{{event.type}} a las {{occurredAt}}",
        estimatedMinutes: 30,
      },
    },
  });
  expect(response.status()).toBe(201);
}

/**
 * El Given de @s42 (features/automations.feature:548) exige lista, editor
 * abierto y resultados de simulacion visibles. Medir tras `Nueva regla` sobre
 * cero reglas audita el estado vacio, que es lo que hacia esta suite: axe nunca
 * veia el <ul aria-label="Reglas"> ni el <ul aria-label="Coincidencias">.
 */
async function openDenseScreen(page, request, project) {
  await clearRules();
  await seedRule(request, project, LONG, true);
  await seedRule(request, project, "Pausada", false);
  await page.goto("/automatizaciones");
  await expect(page.getByRole("list", { name: "Reglas" })).toBeVisible();
  await expect(page.getByRole("switch", { name: /Pausada/ })).toBeVisible();
  await page.getByRole("button", { name: `Editar ${LONG}` }).click();
  await expect(page.getByLabel(/nombre/i)).toBeVisible();
  await page.getByRole("button", { name: "Simular" }).click();
  await expect(
    page.getByRole("status", { name: "Resultado de la simulación" }),
  ).toBeVisible();
}

async function geometry(page) {
  return page.evaluate(() => ({
    width: innerWidth,
    scroll: document.documentElement.scrollWidth,
    client: document.documentElement.clientWidth,
    forced: matchMedia("(forced-colors: active)").matches,
    reduced: matchMedia("(prefers-reduced-motion: reduce)").matches,
    dark: matchMedia("(prefers-color-scheme: dark)").matches,
    theme: document.documentElement.dataset.theme ?? null,
    // Recorte por ELEMENTO y en LOS DOS EJES. El oraculo anterior solo miraba
    // documentElement.scrollWidth, que no crece cuando el contenido se corta
    // dentro de una caja: es la regresion que documenta ics-calendar-ux.
    // Los controles de formulario nativos se exceptuan a proposito: Chromium
    // les aplica overflow:clip en su hoja de agente y su valor sigue siendo
    // alcanzable con el cursor e integro en el arbol de accesibilidad. Queda
    // escrito aqui y en progress/ux_automations.md, no silenciado.
    clipped: [...document.querySelectorAll("main, main *")]
      .filter((element) => element.getClientRects().length)
      .filter(
        (element) => !["INPUT", "SELECT", "TEXTAREA"].includes(element.tagName),
      )
      .filter((element) => {
        const style = getComputedStyle(element);
        return (
          (style.overflowX !== "visible" &&
            element.scrollWidth > element.clientWidth + 1) ||
          (style.overflowY !== "visible" &&
            element.scrollHeight > element.clientHeight + 1)
        );
      })
      .map((element) => ({
        tag: element.tagName.toLowerCase(),
        label: element.getAttribute("aria-label") ?? "",
        text: (element.textContent ?? "").trim().slice(0, 60),
        scrollWidth: element.scrollWidth,
        clientWidth: element.clientWidth,
        scrollHeight: element.scrollHeight,
        clientHeight: element.clientHeight,
      })),
    controls: [
      ...document.querySelectorAll("main a,main button,main input,main select"),
    ]
      .filter((element) => element.getClientRects().length)
      .map((element) => {
        const box = element.getBoundingClientRect();
        return { width: box.width, height: box.height };
      }),
  }));
}

function assertUsable(observed) {
  expect(observed.scroll).toBeLessThanOrEqual(observed.client);
  expect(
    observed.clipped,
    "la pantalla recorta contenido (ancho o alto)",
  ).toEqual([]);
  for (const control of observed.controls) {
    expect(control.width).toBeGreaterThanOrEqual(44);
    expect(control.height).toBeGreaterThanOrEqual(44);
  }
}

test("automatizaciones UX: los cuatro anchos en tema claro y oscuro @s42", async ({
  page,
  request,
}, info) => {
  const { id: project } = await create(request, "Marketing");
  const observations = [];
  for (const colorScheme of ["light", "dark"]) {
    await page.emulateMedia({ colorScheme });
    for (const width of WIDTHS) {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      await openDenseScreen(page, request, project);
      const observed = await geometry(page);
      expect(observed.dark).toBe(colorScheme === "dark");
      assertUsable(observed);
      observations.push({ colorScheme, width, ...observed });
      await page.screenshot({
        path: info.outputPath(`${colorScheme}-${width}.png`),
        fullPage: true,
      });
      expect(
        (await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze())
          .violations,
      ).toEqual([]);
    }
  }
  await writeFile(
    info.outputPath("widths.json"),
    JSON.stringify(observations, null, 2),
  );
});

test("automatizaciones UX: texto al 200 % en los cuatro anchos y los dos temas @s42", async ({
  page,
  request,
}, info) => {
  const { id: project } = await create(request, "Marketing");
  const observations = [];
  for (const colorScheme of ["light", "dark"]) {
    await page.emulateMedia({ colorScheme });
    for (const width of WIDTHS) {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      await openDenseScreen(page, request, project);
      // Zoom de texto, no de disposición: se dobla el tamaño calculado de cada
      // elemento por su atributo style. Una hoja inline la bloquea la CSP.
      const scaled = await page.evaluate(() => {
        const elements = [...document.querySelectorAll("main,main *")].filter(
          (element) => element instanceof HTMLElement,
        );
        const before = elements.map((element) =>
          parseFloat(getComputedStyle(element).fontSize),
        );
        elements.forEach((element, index) => {
          element.style.fontSize = before[index] * 2 + "px";
        });
        const after = elements.map((element) =>
          parseFloat(getComputedStyle(element).fontSize),
        );
        return before.map((size, index) => ({
          before: size,
          after: after[index],
        }));
      });
      expect(scaled.length).toBeGreaterThan(0);
      for (const { before, after } of scaled)
        expect(after).toBeCloseTo(before * 2, 1);
      const observed = await geometry(page);
      assertUsable(observed);
      observations.push({ colorScheme, width, ...observed });
      await page.screenshot({
        path: info.outputPath(`text200-${colorScheme}-${width}.png`),
        fullPage: true,
      });
      expect(
        (await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze())
          .violations,
      ).toEqual([]);
    }
  }
  await writeFile(
    info.outputPath("text200.json"),
    JSON.stringify(observations, null, 2),
  );
});

test("automatizaciones UX: colores forzados y movimiento reducido @s42", async ({
  page,
  request,
}, info) => {
  const { id: project } = await create(request, "Marketing");
  await page.emulateMedia({ reducedMotion: "reduce", forcedColors: "active" });
  await page.setViewportSize({ width: 320, height: 1000 });
  await openDenseScreen(page, request, project);
  const observed = await geometry(page);
  expect(observed.forced).toBe(true);
  expect(observed.reduced).toBe(true);
  assertUsable(observed);

  // El foco sigue siendo alcanzable y visible con colores del sistema.
  const save = page.getByRole("button", { name: "Guardar" });
  await save.focus();
  await expect(save).toBeFocused();

  // Ninguna animación queda activa con movimiento reducido.
  const animated = await page.evaluate(
    () =>
      [...document.querySelectorAll("main,main *")].filter((element) => {
        const style = getComputedStyle(element);
        return (
          (style.animationName !== "none" &&
            parseFloat(style.animationDuration) > 0) ||
          parseFloat(style.transitionDuration) > 0
        );
      }).length,
  );
  expect(animated).toBe(0);

  await page.screenshot({
    path: info.outputPath("forced-colors.png"),
    fullPage: true,
  });
  // color-contrast no se evalúa con colores nativos forzados: los decide el
  // sistema operativo, no la hoja de estilos.
  expect(
    (
      await new AxeBuilder({ page })
        .withTags(AXE_TAGS)
        .disableRules(["color-contrast"])
        .analyze()
    ).violations,
  ).toEqual([]);
  await writeFile(
    info.outputPath("modalities.json"),
    JSON.stringify(
      {
        ...observed,
        animatedElements: animated,
        forcedColorsContrast:
          "Colores nativos; la regla de contraste de axe no se evalúa en este modo.",
      },
      null,
      2,
    ),
  );
});

test("automatizaciones UX: zoom nativo de Chromium al 200 % sobre 320 px CSS @s42", async ({
  baseURL,
}, info) => {
  // La configuración por defecto no declara proyectos y corre Chromium: sólo se
  // salta cuando hay un proyecto con nombre y no es Chromium (Firefox/WebKit).
  test.skip(
    info.project.name !== "" && info.project.name !== "chromium",
    "El zoom nativo se mide en Chromium; el texto al 200 % cubre los tres motores.",
  );
  test.setTimeout(120_000);
  const folder = resolve(".e2e-work", "automations-zoom-" + process.pid);
  const extension = resolve(folder, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    resolve(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "Automations isolated zoom QA",
      version: "1.0",
      permissions: ["tabs"],
      host_permissions: ["http://127.0.0.1/*"],
      background: { service_worker: "worker.js" },
    }),
  );
  await writeFile(
    resolve(extension, "worker.js"),
    "chrome.runtime.onInstalled.addListener(()=>{});",
  );
  const context = await chromium.launchPersistentContext(
    resolve(folder, "browser"),
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
      dpr: devicePixelRatio,
      inner: innerWidth,
      outer: outerWidth,
    }));
    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const pattern = new URL("/*", baseURL).toString();
    const zoom = await worker.evaluate(async (url) => {
      const [tab] = await chrome.tabs.query({ url });
      await chrome.tabs.setZoom(tab.id, 2);
      return chrome.tabs.getZoom(tab.id);
    }, pattern);
    expect(zoom).toBe(2);
    await expect
      .poll(() => page.evaluate(() => devicePixelRatio))
      .toBe(baseline.dpr * 2);
    await worker.evaluate(
      async ({ url, width }) => {
        const [tab] = await chrome.tabs.query({ url });
        await chrome.windows.update(tab.windowId, { width });
      },
      { url: pattern, width: 640 + baseline.outer - baseline.inner },
    );
    await expect.poll(() => page.evaluate(() => innerWidth)).toBe(320);

    const observed = await geometry(page);
    assertUsable(observed);
    await page.screenshot({
      path: info.outputPath("zoom200-compositor.png"),
      fullPage: false,
    });
    await writeFile(
      info.outputPath("native-zoom.json"),
      JSON.stringify({ baseline, zoom, ...observed }, null, 2),
    );
    expect(
      (await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze()).violations,
    ).toEqual([]);
  } finally {
    await context.close();
  }
});
