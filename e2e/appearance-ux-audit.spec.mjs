import { test, expect } from "./support/authenticated-test.mjs";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";
import { chromium } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { randomUUID } from "node:crypto";

const widths = [
  320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  1601, 1920, 2560,
];

test.afterEach(() => {
  sql("DELETE FROM appearance_preferences WHERE owner_id='e2e-user'");
  expect(
    sql(
      "SELECT count(*) FROM appearance_preferences WHERE owner_id='e2e-user'",
    ),
  ).toBe("0");
});

test("appearance audit: nativeZoom200 preserves both previews at 320 CSS pixels @s34", async ({
  baseURL,
}) => {
  const folder = resolve(
    ".e2e-work",
    "appearance-audit",
    process.env.E2E_COMPOSE_PROJECT,
    "native-zoom",
  );
  const extension = resolve(folder, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    resolve(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "Appearance isolated zoom QA",
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
    const prior = await context.request.get("/api/v1/me/appearance");
    const saved = await context.request.put("/api/v1/me/appearance", {
      headers: {
        ...(await csrfHeaders(context.request)),
        "If-Match": prior.headers().etag,
      },
      data: { theme: "DARK", accentLight: "#0000FF", accentDark: "#00FFFF" },
    });
    expect(saved.status()).toBe(200);
    const page = await context.newPage();
    await page.goto("/apariencia");
    await expect(
      page.getByRole("radio", { name: "Oscuro", exact: true }),
    ).toBeChecked();
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
    const geometry = await page.evaluate(() => ({
      width: innerWidth,
      dpr: devicePixelRatio,
      scroll: document.documentElement.scrollWidth,
      controls: [...document.querySelectorAll("main a,main button,main input")]
        .filter((el) => el.getClientRects().length)
        .map((el) => {
          const target = el.matches('[type="radio"]')
            ? el.closest("label")
            : el;
          const r = target.getBoundingClientRect();
          return {
            name: target.textContent || el.id,
            x: r.x,
            width: r.width,
            height: r.height,
          };
        }),
    }));
    await writeFile(
      resolve(folder, "zoom.json"),
      JSON.stringify({ zoom, baseline, geometry }, null, 2),
    );
    const cdp = await context.newCDPSession(page);
    const viewport = await cdp.send("Page.captureScreenshot", {
      format: "png",
      fromSurface: false,
    });
    await writeFile(
      resolve(folder, "zoom200-native-viewport.png"),
      Buffer.from(viewport.data, "base64"),
    );
    await cdp.detach();
    expect(geometry.scroll).toBeLessThanOrEqual(320);
    for (const box of geometry.controls) {
      expect(box.x, box.name).toBeGreaterThanOrEqual(0);
      expect(box.x + box.width, box.name).toBeLessThanOrEqual(321);
      expect(box.width, box.name).toBeGreaterThanOrEqual(44);
      expect(box.height, box.name).toBeGreaterThanOrEqual(44);
    }
    const violations = (
      await new AxeBuilder({ page })
        .withTags([
          "wcag2a",
          "wcag2aa",
          "wcag21aa",
          "wcag22aa",
          "best-practice",
        ])
        .analyze()
    ).violations;
    await writeFile(
      resolve(folder, "axe.json"),
      JSON.stringify(violations, null, 2),
    );
    expect(violations).toEqual([]);
  } finally {
    await context.close();
  }
});

test("appearance audit: system theme reduced motion and forced colors preserve controls @s24 @s34", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(90000);
  const prior = await request.get("/api/v1/me/appearance");
  const saved = await request.put("/api/v1/me/appearance", {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": prior.headers().etag,
    },
    data: { theme: "SYSTEM", accentLight: "#0000FF", accentDark: "#00FFFF" },
  });
  expect(saved.status()).toBe(200);
  let writes = 0;
  page.on("request", (req) => {
    if (req.method() === "PUT" && req.url().endsWith("/api/v1/me/appearance"))
      writes++;
  });
  await page.emulateMedia({ colorScheme: "light" });
  await page.goto("/apariencia");
  await expect(
    page.getByRole("radio", { name: "Sistema", exact: true }),
  ).toBeChecked();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
  await page.emulateMedia({ colorScheme: "dark" });
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  const folder = resolve(
    ".e2e-work",
    "appearance-audit",
    process.env.E2E_COMPOSE_PROJECT,
    page.context().browser().browserType().name(),
    "media",
  );
  await mkdir(folder, { recursive: true });
  const rows = [];
  for (const mode of ["system-dark", "reduced-motion", "forced-colors"]) {
    if (mode === "reduced-motion")
      await page.emulateMedia({ reducedMotion: "reduce" });
    if (mode === "forced-colors")
      await page.emulateMedia({ forcedColors: "active" });
    for (const width of [320, 1440]) {
      await page.setViewportSize({ width, height: 700 });
      const geometry = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        forcedColors: matchMedia("(forced-colors: active)").matches,
        reducedMotion: matchMedia("(prefers-reduced-motion: reduce)").matches,
        transitions: [...document.querySelectorAll("main *")].flatMap((el) =>
          getComputedStyle(el)
            .transitionDuration.split(",")
            .map(Number.parseFloat),
        ),
        canvas: getComputedStyle(document.documentElement).backgroundColor,
        ink: getComputedStyle(document.querySelector("main")).color,
      }));
      rows.push({ mode, ...geometry });
      await writeFile(
        resolve(folder, "media.json"),
        JSON.stringify(rows, null, 2),
      );
      await page.screenshot({
        path: resolve(folder, `${mode}-${width}.png`),
        fullPage: true,
      });
      expect(geometry.scroll).toBeLessThanOrEqual(width);
      if (mode === "reduced-motion") {
        expect(geometry.reducedMotion).toBe(true);
        expect(geometry.transitions.every((value) => value <= 0.01)).toBe(true);
      }
      if (mode === "forced-colors") {
        expect(geometry.forcedColors).toBe(true);
        expect(geometry.canvas).not.toBe(geometry.ink);
      }
    }
    const analyzer = new AxeBuilder({ page });
    // Chromium paints system colors while axe reads authored preview colors.
    // Forced contrast is reviewed visually; retain all normal contrast checks.
    if (mode === "forced-colors") analyzer.disableRules(["color-contrast"]);
    const violations = (
      await analyzer
        .withTags([
          "wcag2a",
          "wcag2aa",
          "wcag21aa",
          "wcag22aa",
          "best-practice",
        ])
        .analyze()
    ).violations;
    await writeFile(
      resolve(folder, `${mode}-axe.json`),
      JSON.stringify(violations, null, 2),
    );
    expect(violations).toEqual([]);
    await page.bringToFront();
  }
  expect(writes).toBe(0);
  await expect(
    page.getByRole("radio", { name: "Sistema", exact: true }),
  ).toBeChecked();
});

test("appearance audit: text200 preserves appearance session history and weekly reading @s34", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180000);
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await configure(request, 120, "UTC");
  const project = await create(
    request,
    "Lectura con colores propios y contexto 🧭",
  );
  const task = await saveTask(
    request,
    project.id,
    "Continuar con una tarea de título largo sin perder el contexto al cambiar la apariencia y ampliar el texto",
  );
  const started = await request.post(
    `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`,
    {
      headers: {
        ...(await csrfHeaders(request)),
        "Idempotency-Key": randomUUID(),
      },
      data: { plannedMinutes: 25 },
    },
  );
  expect(started.status()).toBe(201);
  const session = await started.json();
  const routes = [
    ["appearance", "/apariencia", "Apariencia"],
    [
      "session",
      `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`,
      "Sesión de trabajo",
    ],
    ["history", "/historial", "Historial"],
    ["week", "/revision-semanal?zoneId=UTC", "Revisión semanal"],
  ];
  const folder = resolve(
    ".e2e-work",
    "appearance-audit",
    process.env.E2E_COMPOSE_PROJECT,
    page.context().browser().browserType().name(),
    "text200",
  );
  await mkdir(folder, { recursive: true });
  const evidence = [];
  for (const theme of ["LIGHT", "DARK"]) {
    const prior = await request.get("/api/v1/me/appearance");
    const saved = await request.put("/api/v1/me/appearance", {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": prior.headers().etag,
      },
      data: { theme, accentLight: "#0000FF", accentDark: "#00FFFF" },
    });
    expect(saved.status()).toBe(200);
    for (const [name, url, heading] of routes) {
      await page.setViewportSize({ width: 320, height: 700 });
      await page.goto(url);
      await expect(
        page.getByRole("heading", { level: 1, name: heading, exact: true }),
      ).toBeVisible();
      await expect(page.locator("html")).toHaveAttribute(
        "data-theme",
        theme.toLowerCase(),
      );
      if (name === "appearance")
        await expect(
          page.getByLabel("Color de acento claro", { exact: true }),
        ).toHaveValue("#0000FF");
      if (name === "session")
        await expect(
          page.getByRole("heading", {
            name: "Cerrar sesión de trabajo",
            exact: true,
          }),
        ).toBeVisible();
      if (name === "history")
        await expect(
          page.getByRole("button", { name: "Aplicar filtros", exact: true }),
        ).toBeVisible();
      if (name === "week")
        await expect(
          page.getByRole("list", { name: "Días de la semana", exact: true }),
        ).toBeVisible();
      const fonts = await page.evaluate(() => {
        const elements = [
          ...document.querySelectorAll("main,main *,nav,nav *"),
        ].filter((el) => el instanceof HTMLElement);
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
      expect(
        fonts.every((font) => Math.abs(font.after - font.before * 2) < 0.01),
      ).toBe(true);
      await writeFile(
        resolve(folder, `${theme}-${name}-fonts.json`),
        JSON.stringify(fonts, null, 2),
      );
      for (const width of [320, 768, 1440]) {
        await page.setViewportSize({ width, height: 700 });
        const geometry = await page.evaluate(() => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          controls: [
            ...document.querySelectorAll(
              "main a,main button,main input,main select,nav a",
            ),
          ]
            .filter((el) => el.getClientRects().length)
            .map((el) => {
              const target = el.matches('input[type="radio"]')
                ? el.closest("label")
                : el;
              const r = target.getBoundingClientRect();
              return {
                name: el.labels?.[0]?.textContent || el.textContent,
                x: r.x,
                width: r.width,
                height: r.height,
              };
            }),
        }));
        evidence.push({ theme, name, ...geometry });
        await writeFile(
          resolve(folder, "geometry.json"),
          JSON.stringify(evidence, null, 2),
        );
        await page.screenshot({
          path: resolve(folder, `${theme}-${name}-${width}.png`),
          fullPage: true,
        });
        expect(
          geometry.scroll,
          `${theme}/${name}/${width} overflow`,
        ).toBeLessThanOrEqual(width);
        for (const box of geometry.controls) {
          expect(box.x, box.name).toBeGreaterThanOrEqual(0);
          expect(box.x + box.width, box.name).toBeLessThanOrEqual(width + 1);
          expect(box.height, box.name).toBeGreaterThanOrEqual(44);
          expect(box.width, box.name).toBeGreaterThanOrEqual(44);
        }
      }
      const violations = (
        await new AxeBuilder({ page })
          .withTags([
            "wcag2a",
            "wcag2aa",
            "wcag21aa",
            "wcag22aa",
            "best-practice",
          ])
          .analyze()
      ).violations;
      await writeFile(
        resolve(folder, `${theme}-${name}-axe.json`),
        JSON.stringify(violations, null, 2),
      );
      expect(violations).toEqual([]);
      await page.bringToFront();
    }
  }
});

test("appearance audit: keyboard save keeps focus and announces pending before confirmation @s21 @s23 @s34", async ({
  page,
  request,
}, testInfo) => {
  const prior = await request.get("/api/v1/me/appearance");
  const seeded = await request.put("/api/v1/me/appearance", {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": prior.headers().etag,
    },
    data: { theme: "LIGHT", accentLight: "#244C3C", accentDark: "#B7E4C7" },
  });
  expect(seeded.status()).toBe(200);
  await page.setViewportSize({ width: 320, height: 700 });
  await page.goto("/apariencia");
  await page.getByRole("radio", { name: "Oscuro", exact: true }).check();
  await page
    .getByLabel("Color de acento claro", { exact: true })
    .fill("#0000FF");
  await page
    .getByLabel("Color de acento oscuro", { exact: true })
    .fill("#00FFFF");
  await page.keyboard.press("Tab");
  const save = page.getByRole("button", {
    name: "Guardar apariencia",
    exact: true,
  });
  await expect(save).toBeFocused();
  const focus = await save.evaluate((el) => {
    const r = el.getBoundingClientRect(),
      style = getComputedStyle(el);
    const top = document.elementFromPoint(
      r.x + r.width / 2,
      r.y + r.height / 2,
    );
    return {
      x: r.x,
      y: r.y,
      width: r.width,
      height: r.height,
      viewportHeight: innerHeight,
      visible: el === top || el.contains(top),
      outlineWidth: parseFloat(style.outlineWidth),
      outlineStyle: style.outlineStyle,
      outlineOffset: parseFloat(style.outlineOffset),
    };
  });
  const folder = resolve(
    ".e2e-work",
    "appearance-audit",
    process.env.E2E_COMPOSE_PROJECT,
    page.context().browser().browserType().name(),
  );
  await mkdir(folder, { recursive: true });
  await writeFile(
    resolve(folder, "keyboard-focus.json"),
    JSON.stringify(focus, null, 2),
  );
  await page.screenshot({ path: resolve(folder, "keyboard-save-320.png") });
  expect(focus.y).toBeGreaterThanOrEqual(0);
  expect(focus.y + focus.height).toBeLessThanOrEqual(focus.viewportHeight);
  expect(focus.visible).toBe(true);
  expect(focus.outlineWidth).toBeGreaterThanOrEqual(2);
  expect(focus.outlineStyle).not.toBe("none");
  expect(focus.outlineOffset).toBeGreaterThanOrEqual(0);
  let release;
  const held = new Promise((resolveHeld) => {
    release = resolveHeld;
  });
  let writes = 0;
  await page.route("**/api/v1/me/appearance", async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    await held;
    await route.continue();
  });
  try {
    await page.evaluate(() => {
      window.appearanceAuditStart = performance.now();
      const observer = new MutationObserver(() => {
        if (document.querySelector('form[aria-busy="true"]')) {
          window.appearanceAuditFeedback =
            performance.now() - window.appearanceAuditStart;
          observer.disconnect();
        }
      });
      observer.observe(document.querySelector("main"), {
        subtree: true,
        attributes: true,
        childList: true,
      });
    });
    await page.keyboard.press("Enter");
    await expect(page.getByRole("status")).toHaveText("Guardando apariencia…");
    const feedbackMs = await page.evaluate(
      () => window.appearanceAuditFeedback,
    );
    await writeFile(
      resolve(folder, "feedback.json"),
      JSON.stringify({ feedbackMs }, null, 2),
    );
    expect(feedbackMs).toBeLessThan(400);
    await expect(page.locator("html")).toHaveAttribute("data-theme", "light");
    await expect(save).toBeFocused();
    await expect.poll(() => writes).toBe(1);
    await page.keyboard.press("Enter");
    expect(writes).toBe(1);
    const confirmed = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/v1/me/appearance") &&
        response.request().method() === "PUT",
    );
    release();
    expect((await confirmed).status()).toBe(200);
    await expect(page.getByRole("status")).toHaveText("Apariencia guardada.");
    await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
    await expect(save).toBeFocused();
  } finally {
    release();
  }
});

test("appearance audit: both themes retain geometry and accessible errors @s27 @s34", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180000);
  const folder = resolve(
    ".e2e-work",
    "appearance-audit",
    process.env.E2E_COMPOSE_PROJECT,
    page.context().browser().browserType().name(),
  );
  await mkdir(folder, { recursive: true });
  const evidence = [];
  for (const theme of ["LIGHT", "DARK"]) {
    const prior = await request.get("/api/v1/me/appearance");
    expect(prior.status()).toBe(200);
    const saved = await request.put("/api/v1/me/appearance", {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": prior.headers().etag,
      },
      data: { theme, accentLight: "#0000FF", accentDark: "#00FFFF" },
    });
    expect(saved.status()).toBe(200);
    await page.goto("/apariencia");
    await expect(page.locator("html")).toHaveAttribute(
      "data-theme",
      theme.toLowerCase(),
    );
    await expect(
      page.getByLabel("Color de acento claro", { exact: true }),
    ).toHaveValue("#0000FF");
    for (const state of ["form", "error"]) {
      if (state === "error") {
        await page
          .getByLabel("Color de acento claro", { exact: true })
          .fill("#FFFFFF");
        await expect(page.getByRole("alert")).toContainText("contraste");
      }
      for (const width of widths) {
        await page.setViewportSize({
          width,
          height: width === 768 ? 400 : 900,
        });
        const measure = await page.evaluate(() => {
          const elements = [
            ...document.querySelectorAll(
              'nav[aria-label="Principal"] a,main a,main button,main input,main select',
            ),
          ].filter((el) => el.getClientRects().length);
          return {
            width: innerWidth,
            scroll: document.documentElement.scrollWidth,
            controls: elements.map((el) => {
              const target = el.matches('input[type="radio"]')
                ? el.closest("label")
                : el;
              const box = target.getBoundingClientRect();
              return {
                name:
                  el.getAttribute("aria-label") ||
                  el.labels?.[0]?.textContent ||
                  el.textContent,
                x: box.x,
                y: box.y,
                width: box.width,
                height: box.height,
              };
            }),
          };
        });
        evidence.push({ theme, state, ...measure });
        await writeFile(
          resolve(folder, "geometry.json"),
          JSON.stringify(evidence, null, 2),
        );
        if (width === 320 || width === 1440)
          await page.screenshot({
            path: resolve(folder, `${theme}-${state}-${width}.png`),
            fullPage: true,
          });
        expect(
          measure.scroll,
          `${theme}/${state}/${width} overflow`,
        ).toBeLessThanOrEqual(width);
        for (const box of measure.controls) {
          const label = `${theme}/${state}/${width}/${box.name}`;
          expect(box.x, `${label} left`).toBeGreaterThanOrEqual(0);
          expect(box.x + box.width, `${label} right`).toBeLessThanOrEqual(
            width + 1,
          );
          expect(box.width, `${label} width`).toBeGreaterThanOrEqual(44);
          expect(box.height, `${label} height`).toBeGreaterThanOrEqual(44);
        }
      }
      const violations = (
        await new AxeBuilder({ page })
          .withTags([
            "wcag2a",
            "wcag2aa",
            "wcag21aa",
            "wcag22aa",
            "best-practice",
          ])
          .analyze()
      ).violations;
      await writeFile(
        resolve(folder, `${theme}-${state}-axe.json`),
        JSON.stringify(violations, null, 2),
      );
      expect(violations).toEqual([]);
      await page.bringToFront();
    }
  }
});
