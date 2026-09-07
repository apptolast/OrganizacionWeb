import { test, expect } from "./support/authenticated-test.mjs";
import { sql } from "./support/projects.mjs";
import { configure } from "./support/blocks.mjs";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { chromium } from "@playwright/test";
import { join, resolve } from "node:path";
import { loginSession } from "../scripts/session-client.mjs";

async function inspectEnlargedWeek(page, folder, state) {
  const geometry = await page.evaluate(() => ({
    width: innerWidth,
    scroll: document.documentElement.scrollWidth,
    controls: [
      ...document.querySelectorAll("main a,main button,main input,main select"),
    ]
      .filter((el) => el.getClientRects().length)
      .map((el) => {
        const r = el.getBoundingClientRect();
        return {
          name: el.textContent,
          x: r.x,
          width: r.width,
          height: r.height,
        };
      }),
  }));
  await writeFile(
    `${folder}/${state}-geometry.json`,
    JSON.stringify(geometry, null, 2),
  );
  await page.screenshot({ path: `${folder}/${state}.png`, fullPage: true });
  await page.screenshot({ path: `${folder}/${state}-viewport.png` });
  expect(geometry.scroll).toBeLessThanOrEqual(geometry.width);
  for (const box of geometry.controls) {
    expect(box.x, box.name).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width, box.name).toBeLessThanOrEqual(geometry.width + 1);
    expect(box.height, box.name).toBeGreaterThanOrEqual(44);
    expect(box.width, box.name).toBeGreaterThanOrEqual(44);
  }
  const violations = (
    await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
      .analyze()
  ).violations;
  await writeFile(
    `${folder}/${state}-axe.json`,
    JSON.stringify(violations, null, 2),
  );
  expect(violations).toEqual([]);
}

test("weekly review: nativeZoom200 preserves weekly reading at 320 CSS pixels @s34", async ({
  request,
  baseURL,
}) => {
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await configure(request, 0, "UTC");
  const scratch = resolve(
    ".e2e-work",
    "weekly-review-native",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  const folder = join(scratch, "evidence");
  await mkdir(extension, { recursive: true });
  await mkdir(folder, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated weekly review zoom QA",
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
    await page.goto("/revision-semanal?date=2020-01-08&zoneId=UTC");
    await expect(
      page.getByRole("list", { name: "Días de la semana", exact: true }),
    ).toBeVisible();
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
    await inspectEnlargedWeek(page, folder, "week");
    await expect(
      page.getByText(
        "Sin tiempo presupuestado actualmente: descanso planificado actual.",
      ),
    ).toHaveCount(7);
  } finally {
    await context.close();
  }
});

test("weekly review: native selection and seven days remain usable across widths @s34", async ({
  page,
}, testInfo) => {
  const folder = `.e2e-work/weekly-review-real/${page.context().browser().browserType().name()}/ux`;
  await mkdir(folder, { recursive: true });
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await page.goto("/revision-semanal?date=2020-01-08&zoneId=UTC");
  await expect(
    page.getByRole("list", { name: "Días de la semana", exact: true }),
  ).toBeVisible();
  const measurements = [];
  for (const width of [
    320, 360, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701, 768, 820,
    999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1920, 2560,
  ]) {
    await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
    const measurement = await page.evaluate(() => ({
      width: innerWidth,
      height: innerHeight,
      scroll: document.documentElement.scrollWidth,
      controls: [
        ...document.querySelectorAll(
          'nav[aria-label="Principal"] a,main a,main button,main input,main select',
        ),
      ]
        .filter((el) => el.getClientRects().length)
        .map((el) => {
          const r = el.getBoundingClientRect();
          return {
            name: el.labels?.[0]?.textContent || el.textContent,
            x: r.x,
            y: r.y,
            width: r.width,
            height: r.height,
          };
        }),
    }));
    measurements.push(measurement);
    await writeFile(
      `${folder}/geometry.json`,
      JSON.stringify(measurements, null, 2),
    );
    if (width === 320 || width === 1440)
      await page.screenshot({
        path: `${folder}/week-${width}.png`,
        fullPage: true,
      });
    expect(measurement.scroll, `overflow ${width}`).toBeLessThanOrEqual(width);
    for (const box of measurement.controls) {
      expect(box.x, `${width}:${box.name}:left`).toBeGreaterThanOrEqual(0);
      expect(
        box.x + box.width,
        `${width}:${box.name}:right`,
      ).toBeLessThanOrEqual(width + 1);
      expect(box.width, `${width}:${box.name}:width`).toBeGreaterThanOrEqual(
        44,
      );
      expect(box.height, `${width}:${box.name}:height`).toBeGreaterThanOrEqual(
        44,
      );
    }
    for (let i = 0; i < measurement.controls.length; i++)
      for (let j = i + 1; j < measurement.controls.length; j++) {
        const a = measurement.controls[i],
          b = measurement.controls[j];
        expect(
          Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x) > 1 &&
            Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y) > 1,
          `overlap ${a.name}/${b.name}`,
        ).toBe(false);
      }
  }
  await page.setViewportSize({ width: 320, height: 700 });
  const violations = (
    await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
      .analyze()
  ).violations;
  await writeFile(`${folder}/axe.json`, JSON.stringify(violations, null, 2));
  expect(violations).toEqual([]);
  await page
    .getByRole("heading", { name: "Revisión semanal", level: 1, exact: true })
    .focus();
  await page.keyboard.press("Tab");
  await expect(
    page.getByLabel("Fecha de la semana", { exact: true }),
  ).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  testInfo.annotations.push({
    type: "scope",
    description:
      "Nueva revisión semanal y quinto enlace: geometría, axe y entrada por teclado; no dispositivos físicos ni evaluación psicológica universal.",
  });
});

test("weekly review: text200 preserves dates controls and neutral weekly facts @s34", async ({
  page,
  request,
}) => {
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await configure(request, 0, "UTC");
  const folder = `.e2e-work/weekly-review-real/${page.context().browser().browserType().name()}/text200`;
  await mkdir(folder, { recursive: true });
  await page.goto("/revision-semanal?date=2020-01-08&zoneId=UTC");
  await expect(
    page.getByRole("list", { name: "Días de la semana", exact: true }),
  ).toBeVisible();
  const fonts = await page.evaluate(() => {
    const elements = [...document.querySelectorAll("main,main *")].filter(
      (el) => el instanceof HTMLElement,
    );
    const sizes = elements.map((el) =>
      parseFloat(getComputedStyle(el).fontSize),
    );
    elements.forEach((el, index) => {
      el.style.fontSize = sizes[index] * 2 + "px";
    });
    return elements.map((el, index) => ({
      before: sizes[index],
      after: parseFloat(getComputedStyle(el).fontSize),
    }));
  });
  for (const font of fonts) expect(font.after).toBeCloseTo(font.before * 2, 3);
  await writeFile(`${folder}/fonts.json`, JSON.stringify(fonts, null, 2));
  for (const width of [320, 768, 1440]) {
    await page.setViewportSize({ width, height: 700 });
    await inspectEnlargedWeek(page, folder, `week-${width}`);
  }
  await expect(
    page.getByText(
      "Sin tiempo presupuestado actualmente: descanso planificado actual.",
    ),
  ).toHaveCount(7);
});

test("weekly review: refresh announces waiting and recovers without losing the selected week @s29 @s30 @s33 @s34", async ({
  page,
  request,
}) => {
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await configure(request, 0, "UTC");
  const folder = `.e2e-work/weekly-review-real/${page.context().browser().browserType().name()}/recovery`;
  await mkdir(folder, { recursive: true });
  await page.goto("/revision-semanal?date=2020-01-08&zoneId=UTC");
  await expect(
    page.getByText(
      "Sin tiempo presupuestado actualmente: descanso planificado actual.",
    ),
  ).toHaveCount(7);
  const measurements = [];
  async function inspect(state) {
    for (const width of [320, 768, 1440]) {
      await page.setViewportSize({ width, height: 700 });
      const geometry = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            "main a,main button,main input,main select",
          ),
        ]
          .filter((el) => el.getClientRects().length)
          .map((el) => {
            const r = el.getBoundingClientRect();
            return { label: el.textContent, width: r.width, height: r.height };
          }),
      }));
      measurements.push({ state, ...geometry });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(measurements, null, 2),
      );
      await page.screenshot({
        path: `${folder}/${state}-${width}.png`,
        fullPage: true,
      });
      expect(geometry.scroll).toBeLessThanOrEqual(width);
      for (const box of geometry.controls) {
        expect(box.width, box.label).toBeGreaterThanOrEqual(44);
        expect(box.height, box.label).toBeGreaterThanOrEqual(44);
      }
    }
    await page.setViewportSize({ width: 320, height: 700 });
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
      `${folder}/${state}-axe.json`,
      JSON.stringify(violations, null, 2),
    );
    expect(violations).toEqual([]);
  }
  await inspect("rest-plan");
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  let started;
  const arrived = new Promise((resolve) => {
    started = resolve;
  });
  await page.route(
    "**/api/v1/weekly-review?**",
    async (route) => {
      started();
      await held;
      await route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          status: 503,
          code: "STORAGE_UNAVAILABLE",
        }),
      });
    },
    { times: 1 },
  );
  const update = page.getByRole("button", { name: "Actualizar", exact: true });
  await update.focus();
  await page.evaluate(() => {
    const status = document.querySelector('[role="status"]');
    document.activeElement.addEventListener(
      "keydown",
      () => {
        const start = performance.now();
        const observer = new MutationObserver(() => {
          if (status.textContent.includes("Actualizando")) {
            window.weeklyFeedback = performance.now() - start;
            observer.disconnect();
          }
        });
        observer.observe(status, {
          childList: true,
          subtree: true,
          characterData: true,
        });
      },
      { once: true },
    );
  });
  await page.keyboard.press("Enter");
  await arrived;
  await expect(page.getByRole("status")).toHaveText(
    "Actualizando revisión semanal…",
  );
  await expect(page.getByText(/Datos anteriores/)).toBeVisible();
  const elapsed = await page.evaluate(() => window.weeklyFeedback);
  expect(elapsed).toBeLessThan(400);
  await writeFile(
    `${folder}/feedback.json`,
    JSON.stringify({ elapsedMs: elapsed, responseReleased: false }, null, 2),
  );
  await inspect("pending");
  release();
  await expect(page.getByRole("alert")).toContainText(
    "No se pudo consultar la revisión semanal.",
  );
  await expect(
    page.getByLabel("Fecha de la semana", { exact: true }),
  ).toHaveValue("2020-01-08");
  await expect(update).toBeFocused();
  await inspect("error");
  await page.getByRole("button", { name: "Reintentar", exact: true }).focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("alert")).toHaveCount(0);
  await expect(page.getByText(/Datos anteriores/)).toHaveCount(0);
  await expect(
    page.getByRole("heading", {
      name: "Revisión semanal",
      level: 1,
      exact: true,
    }),
  ).toBeFocused();
  await inspect("recovered");
});
