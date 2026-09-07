import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { configure } from "./support/blocks.mjs";
import { randomUUID } from "node:crypto";
import { chromium } from "@playwright/test";
import { join, resolve } from "node:path";
import { loginSession } from "../scripts/session-client.mjs";

const widths = [
  320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  1601, 1920, 2560,
];
test("history: filters details and read recovery preserve geometry and keyboard focus @s34 @s38 @s39", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180000);
  const folder = `.e2e-work/history-real/${testInfo.project.name}/ux`;
  const pointerOnly =
    page.context().browser().browserType().name() === "webkit";
  if (pointerOnly)
    testInfo.annotations.push({
      type: "limitation",
      description:
        "WebKit Windows: geometry/axe via click; link keyboard navigation is not accredited.",
    });
  await mkdir(folder, { recursive: true });
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(
    request,
    "Revisar hechos y contexto sin perder decisiones 🧭",
  );
  const task = await saveTask(
    request,
    project.id,
    "Una tarea con título largo y Unicode para conservar una lectura comprensible en el historial",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/status`;
  const status = await request.get(endpoint);
  const completed = await request.put(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": status.headers().etag,
    },
    data: { status: "completed" },
  });
  expect(completed.status()).toBe(200);
  const evidence = [];
  async function inspect(state) {
    for (const width of widths) {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      const measure = await page.evaluate(() => ({
        width: innerWidth,
        height: innerHeight,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            'nav[aria-label="Principal"] a,main a,main button,main input,main select,main summary',
          ),
        ]
          .filter((el) => el.getClientRects().length)
          .map((el) => {
            const r = el.getBoundingClientRect();
            return {
              name:
                el.getAttribute("aria-label") ||
                el.labels?.[0]?.textContent ||
                el.textContent,
              x: r.x,
              y: r.y,
              width: r.width,
              height: r.height,
            };
          }),
      }));
      evidence.push({ state, ...measure });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(evidence, null, 2),
      );
      if (width === 320 || width === 1440)
        await page.screenshot({
          path: `${folder}/${state}-${width}.png`,
          fullPage: true,
        });
      expect(measure.scroll, `${state} overflow ${width}`).toBeLessThanOrEqual(
        width,
      );
      for (const box of measure.controls) {
        expect(
          box.x,
          `${state}:${width}:${box.name} left`,
        ).toBeGreaterThanOrEqual(0);
        expect(
          box.x + box.width,
          `${state}:${width}:${box.name} right`,
        ).toBeLessThanOrEqual(width + 1);
        expect(
          box.width,
          `${state}:${width}:${box.name} width`,
        ).toBeGreaterThanOrEqual(44);
        expect(
          box.height,
          `${state}:${width}:${box.name} height`,
        ).toBeGreaterThanOrEqual(44);
      }
      for (let i = 0; i < measure.controls.length; i++)
        for (let j = i + 1; j < measure.controls.length; j++) {
          const a = measure.controls[i],
            b = measure.controls[j];
          expect(
            Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x) > 1 &&
              Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y) > 1,
            `${state} overlap ${a.name}/${b.name}`,
          ).toBe(false);
        }
    }
    await page.setViewportSize({ width: 320, height: 700 });
    const axe = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
      .analyze();
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(axe.violations, null, 2),
    );
    expect(axe.violations).toEqual([]);
    await page.bringToFront();
  }
  async function keyboard(locator) {
    if (pointerOnly) return;
    const trail = [];
    const key = await locator.evaluate((el) =>
      document.activeElement?.compareDocumentPosition(el) &
      Node.DOCUMENT_POSITION_PRECEDING
        ? "Shift+Tab"
        : "Tab",
    );
    for (let i = 0; i < 120; i++) {
      await page.keyboard.press(key);
      await page.evaluate(() => new Promise(requestAnimationFrame));
      trail.push(
        await page.evaluate(() => ({
          tag: document.activeElement?.tagName,
          text: document.activeElement?.textContent?.slice(0, 100),
          scrollY,
        })),
      );
      if (await locator.evaluate((el) => el === document.activeElement)) {
        await expect
          .poll(() =>
            locator.evaluate(
              (el) => el.getBoundingClientRect().bottom <= innerHeight + 1,
            ),
          )
          .toBe(true);
        const focus = await locator.evaluate((el) => {
          const r = el.getBoundingClientRect(),
            s = getComputedStyle(el);
          return {
            top: r.top,
            bottom: r.bottom,
            height: innerHeight,
            outline: s.outlineStyle,
            outlineWidth: s.outlineWidth,
            shadow: s.boxShadow,
          };
        });
        expect(focus.top).toBeGreaterThanOrEqual(0);
        expect(focus.bottom).toBeLessThanOrEqual(focus.height);
        expect(
          (focus.outline !== "none" && parseFloat(focus.outlineWidth) > 0) ||
            focus.shadow !== "none",
        ).toBe(true);
        return;
      }
    }
    await writeFile(
      `${folder}/unreached-focus.json`,
      JSON.stringify(trail, null, 2),
    );
    throw new Error("Control not reachable by Tab");
  }
  async function activateControl(locator) {
    if (pointerOnly) await locator.click();
    else await page.keyboard.press("Enter");
  }
  await page.goto(`/historial?projectId=${project.id}`);
  await expect(
    page.getByRole("list", { name: "Hechos del historial" }),
  ).toBeVisible();
  await keyboard(page.locator("main summary"));
  await activateControl(page.locator("main summary"));
  await expect(
    page.getByText("Estado registrado:", { exact: false }),
  ).toBeVisible();
  await inspect("details");
  const clearContext = page.getByRole("link", {
    name: "Quitar filtro de contexto",
    exact: true,
  });
  await keyboard(clearContext);
  await activateControl(clearContext);
  await expect(page).toHaveURL(/\/historial$/);
  const apply = page.getByRole("button", {
    name: "Aplicar filtros",
    exact: true,
  });
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  await page.route(
    "**/api/v1/history?**",
    async (route) => {
      await held;
      await route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Unavailable",
          status: 503,
          code: "STORAGE_UNAVAILABLE",
        }),
      });
    },
    { times: 1 },
  );
  await page.getByLabel("Categoría", { exact: true }).selectOption("planning");
  await keyboard(apply);
  const began = Date.now();
  await activateControl(apply);
  await expect(page.getByRole("status")).toHaveText("Consultando historial…", {
    timeout: 400,
  });
  const feedback = Date.now() - began;
  await writeFile(
    `${folder}/feedback.json`,
    JSON.stringify({ milliseconds: feedback }, null, 2),
  );
  expect(feedback).toBeLessThan(400);
  await inspect("pending");
  release();
  await expect(page.getByRole("alert")).toBeVisible();
  await inspect("error");
  const retry = page.getByRole("button", {
    name: "Reintentar consulta",
    exact: true,
  });
  await keyboard(retry);
  await activateControl(retry);
  await expect(
    page.getByText("No hay hechos con estos filtros.", { exact: true }),
  ).toBeVisible();
  if (!pointerOnly)
    await expect(
      page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
    ).toBeFocused();
  await inspect("empty");
  const clear = page.getByRole("link", {
    name: "Limpiar filtros",
    exact: true,
  });
  await keyboard(clear);
  await activateControl(clear);
  await expect(
    page.getByRole("list", { name: "Hechos del historial" }),
  ).toBeVisible();
  if (!pointerOnly)
    await expect(
      page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
    ).toBeFocused();
  await inspect("list");
});

test("history: keyboard pagination and a superseded read preserve the current page @s31 @s36 @s38 @s39", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(120000);
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(
    request,
    "Navegación por teclado sin perder contexto",
  );
  const task = await saveTask(
    request,
    project.id,
    "Una página vigente tras una consulta anterior",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/status`;
  let status = await request.get(endpoint);
  for (let i = 0; i < 21; i++) {
    status = await request.put(endpoint, {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": status.headers().etag,
      },
      data: { status: i % 2 === 0 ? "completed" : "pending" },
    });
    expect(status.status()).toBe(200);
  }
  const folder = `.e2e-work/history-real/${testInfo.project.name}/keyboard`;
  await mkdir(folder, { recursive: true });
  const focus = [];
  async function activate(locator, label) {
    for (let i = 0; i < 220; i++) {
      await page.keyboard.press("Tab");
      await page.evaluate(() => new Promise(requestAnimationFrame));
      if (await locator.evaluate((el) => el === document.activeElement)) {
        await expect
          .poll(() =>
            locator.evaluate(
              (el) => el.getBoundingClientRect().bottom <= innerHeight + 1,
            ),
          )
          .toBe(true);
        const box = await locator.evaluate((el) => {
          const r = el.getBoundingClientRect(),
            s = getComputedStyle(el);
          return {
            top: r.top,
            bottom: r.bottom,
            viewport: innerHeight,
            outline: s.outlineStyle,
            width: s.outlineWidth,
            shadow: s.boxShadow,
          };
        });
        focus.push({ label, ...box });
        await writeFile(`${folder}/focus.json`, JSON.stringify(focus, null, 2));
        expect(box.top).toBeGreaterThanOrEqual(0);
        expect(box.bottom).toBeLessThanOrEqual(box.viewport + 1);
        expect(
          (box.outline !== "none" && parseFloat(box.width) > 0) ||
            box.shadow !== "none",
        ).toBe(true);
        await page.keyboard.press("Enter");
        return;
      }
    }
    throw new Error(`Tab did not reach ${label}`);
  }
  await page.setViewportSize({ width: 320, height: 700 });
  await page.goto("/historial?category=task-status");
  const list = page.getByRole("list", { name: "Hechos del historial" });
  await expect(list.getByRole("listitem")).toHaveCount(20);
  await activate(
    page.getByRole("link", { name: "Más antiguos", exact: true }),
    "older",
  );
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await expect(
    page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
  ).toBeFocused();
  await activate(
    page.getByRole("link", {
      name: "Ver historial de esta tarea",
      exact: true,
    }),
    "task history",
  );
  await expect(
    page.getByRole("link", { name: "Esta tarea", exact: true }),
  ).toBeVisible();
  await activate(
    page.getByRole("link", { name: "Quitar filtro de contexto", exact: true }),
    "remove context",
  );
  await expect(page).toHaveURL(/\/historial$/);
  await activate(
    page.getByRole("link", { name: "Más antiguos", exact: true }),
    "older again",
  );
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await activate(
    page.getByRole("link", { name: "Volver a recientes", exact: true }),
    "recent",
  );
  await expect(list.getByRole("listitem")).toHaveCount(20);
  let release, started, finished;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  const requested = new Promise((resolve) => {
    started = resolve;
  });
  const delivered = new Promise((resolve) => {
    finished = resolve;
  });
  await page.route(
    "**/api/v1/history?category=planning",
    async (route) => {
      started();
      await held;
      try {
        await route.fulfill({
          status: 401,
          contentType: "application/problem+json",
          body: JSON.stringify({
            type: "about:blank",
            title: "Unauthorized",
            status: 401,
            code: "UNAUTHENTICATED",
          }),
        });
      } finally {
        finished();
      }
    },
    { times: 1 },
  );
  await page.getByLabel("Categoría", { exact: true }).selectOption("planning");
  await activate(
    page.getByRole("button", { name: "Aplicar filtros", exact: true }),
    "apply held",
  );
  await requested;
  await expect(page.getByRole("status")).toHaveText("Consultando historial…");
  await activate(
    page.getByRole("link", { name: "Limpiar filtros", exact: true }),
    "clear pending",
  );
  await expect(list.getByRole("listitem")).toHaveCount(20);
  release();
  await delivered;
  await expect(list.getByRole("listitem")).toHaveCount(20);
  expect((await request.get("/api/session")).status()).toBe(200);
  await expect(
    page.getByRole("button", { name: "Iniciar sesión", exact: true }),
  ).toHaveCount(0);
  await writeFile(`${folder}/focus.json`, JSON.stringify(focus, null, 2));
  await page.screenshot({ path: `${folder}/current-320.png` });
});

async function largeReading(page, request, folder, options) {
  test.setTimeout(180000);
  await mkdir(folder, { recursive: true });
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(
    request,
    "Notas históricas legibles con ampliación de texto",
  );
  const task = await saveTask(
    request,
    project.id,
    "Revisar el siguiente paso sin perder espacios ni Unicode 🧭",
  );
  await configure(request);
  const headers = await csrfHeaders(request);
  const start = await request.post(
    `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`,
    {
      headers: { ...headers, "Idempotency-Key": randomUUID() },
      data: { plannedMinutes: 25 },
    },
  );
  expect(start.status()).toBe(201);
  const session = await start.json();
  const note =
    "  Nota larga conservada: progreso real y siguiente paso 🙂\n".repeat(25);
  const close = await request.post(
    `/api/v1/work-sessions/${session.id}/close`,
    {
      headers: {
        ...headers,
        "Idempotency-Key": randomUUID(),
        "Work-Session-Revision": `work-session-${session.id}-1`,
      },
      data: {
        progressNote: note,
        nextStep: "Continuar cuando convenga, sin perder el contexto.",
      },
    },
  );
  expect(close.status(), await close.text()).toBe(201);
  await page.goto("/historial");
  const row = page.getByRole("listitem").filter({
    has: page.getByRole("heading", { name: "Sesión cerrada", exact: true }),
  });
  await row.locator("summary").click();
  expect(
    await row
      .locator(".history-note")
      .filter({ hasText: "Nota larga conservada" })
      .textContent(),
  ).toBe(note);
  if (options.prepare) await options.prepare(page, folder);
  const geometry = [];
  async function measure(state) {
    if (options.text200) {
      const fonts = await page.evaluate(() => {
        const elements = [...document.querySelectorAll("main,main *")].filter(
          (el) => el instanceof HTMLElement,
        );
        window.historyOriginalFonts ??= new WeakMap();
        for (const el of elements) {
          if (!window.historyOriginalFonts.has(el))
            window.historyOriginalFonts.set(el, el.style.fontSize);
          el.style.fontSize = window.historyOriginalFonts.get(el);
        }
        const sizes = elements.map((el) =>
          parseFloat(getComputedStyle(el).fontSize),
        );
        elements.forEach((el, i) => {
          el.style.fontSize = sizes[i] * 2 + "px";
        });
        return elements.map((el, i) => ({
          before: sizes[i],
          after: parseFloat(getComputedStyle(el).fontSize),
        }));
      });
      for (const font of fonts)
        expect(font.after).toBeCloseTo(font.before * 2, 3);
      await writeFile(
        `${folder}/${state}-fonts.json`,
        JSON.stringify(fonts, null, 2),
      );
    }
    for (const width of options.native ? [320] : [320, 768, 1440]) {
      if (!options.native)
        await page.setViewportSize({
          width,
          height: width === 768 ? 400 : 900,
        });
      const value = await page.evaluate(() => ({
        width: innerWidth,
        height: innerHeight,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            "main a,main input,main select,main button,main summary",
          ),
        ]
          .filter((el) => el.getClientRects().length)
          .map((el) => {
            const r = el.getBoundingClientRect();
            return {
              name: el.textContent || el.labels?.[0]?.textContent,
              x: r.x,
              width: r.width,
              height: r.height,
            };
          }),
      }));
      geometry.push({ state, ...value });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(geometry, null, 2),
      );
      if (options.native) {
        const capture = await page.context().newCDPSession(page);
        const viewport = await capture.send("Page.captureScreenshot", {
          format: "png",
          fromSurface: true,
          captureBeyondViewport: false,
        });
        await writeFile(
          `${folder}/${state}-${width}-viewport.png`,
          Buffer.from(viewport.data, "base64"),
        );
        const dimensions = await page.evaluate(() => ({
          width: document.documentElement.scrollWidth,
          height: document.documentElement.scrollHeight,
        }));
        const full = await capture.send("Page.captureScreenshot", {
          format: "png",
          fromSurface: true,
          captureBeyondViewport: true,
          clip: {
            x: 0,
            y: 0,
            width: dimensions.width * 2,
            height: dimensions.height * 2,
            scale: 1,
          },
        });
        await writeFile(
          `${folder}/${state}-${width}.png`,
          Buffer.from(full.data, "base64"),
        );
        await capture.detach();
      } else {
        await page.screenshot({
          path: `${folder}/${state}-${width}.png`,
          fullPage: true,
        });
      }
      expect(value.width).toBe(width);
      expect(value.scroll).toBeLessThanOrEqual(width);
      for (const control of value.controls) {
        expect(control.x, control.name).toBeGreaterThanOrEqual(0);
        expect(control.x + control.width, control.name).toBeLessThanOrEqual(
          width + 1,
        );
        expect(control.width, control.name).toBeGreaterThanOrEqual(44);
        expect(control.height, control.name).toBeGreaterThanOrEqual(44);
      }
    }
    const axe = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
      .analyze();
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(axe.violations, null, 2),
    );
    expect(axe.violations).toEqual([]);
  }
  await measure("notes");
  await page.route(
    "**/api/v1/history?**",
    (route) =>
      route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Unavailable",
          status: 503,
          code: "STORAGE_UNAVAILABLE",
        }),
      }),
    { times: 1 },
  );
  await page.getByLabel("Categoría", { exact: true }).selectOption("planning");
  await page
    .getByRole("button", { name: "Aplicar filtros", exact: true })
    .click();
  await expect(page.getByRole("alert")).toBeVisible();
  await measure("error");
  await page
    .getByRole("button", { name: "Reintentar consulta", exact: true })
    .click();
  await expect(
    page.getByText("No hay hechos con estos filtros.", { exact: true }),
  ).toBeVisible();
  await measure("empty");
}

test("history: text200 retains long notes and read recovery @s39", async ({
  page,
  request,
}, testInfo) =>
  largeReading(
    page,
    request,
    `.e2e-work/history-real/${testInfo.project.name}/text200`,
    { text200: true },
  ));

test("history: nativeZoom200 preserves historical notes and recovery @s39", async ({
  request,
  baseURL,
}) => {
  test.setTimeout(180000);
  const scratch = resolve(
    ".e2e-work",
    "history-native",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated history zoom QA",
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
  try {
    await loginSession(context.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    const page = await context.newPage();
    await largeReading(page, request, join(scratch, "evidence"), {
      native: true,
      prepare: async (page, folder) => {
        const baseline = await page.evaluate(() => ({
          dpr: devicePixelRatio,
          inner: innerWidth,
          outer: outerWidth,
        }));
        const worker =
          context.serviceWorkers()[0] ??
          (await context.waitForEvent("serviceworker"));
        const zoom = await worker.evaluate(async () => {
          const [tab] = await chrome.tabs.query({
            url: "http://127.0.0.1:18080/*",
          });
          await chrome.tabs.setZoom(tab.id, 2);
          return chrome.tabs.getZoom(tab.id);
        });
        expect(zoom).toBe(2);
        await expect
          .poll(() => page.evaluate(() => devicePixelRatio))
          .toBe(baseline.dpr * 2);
        await worker.evaluate(
          async (width) => {
            const [tab] = await chrome.tabs.query({
              url: "http://127.0.0.1:18080/*",
            });
            await chrome.windows.update(tab.windowId, { width });
          },
          640 + baseline.outer - baseline.inner,
        );
        await expect.poll(() => page.evaluate(() => innerWidth)).toBe(320);
        await writeFile(
          join(folder, "zoom.json"),
          JSON.stringify(
            {
              zoom,
              baseline,
              current: await page.evaluate(() => ({
                dpr: devicePixelRatio,
                width: innerWidth,
                height: innerHeight,
              })),
            },
            null,
            2,
          ),
        );
      },
    });
  } finally {
    await context.close();
  }
});
