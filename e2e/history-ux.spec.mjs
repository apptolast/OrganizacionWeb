import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";

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
  }
  async function keyboard(locator) {
    for (let i = 0; i < 120; i++) {
      await page.keyboard.press("Tab");
      if (await locator.evaluate((el) => el === document.activeElement)) {
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
    throw new Error("Control not reachable by Tab");
  }
  await page.goto(`/historial?projectId=${project.id}`);
  await expect(
    page.getByRole("list", { name: "Hechos del historial" }),
  ).toBeVisible();
  await keyboard(page.locator("main summary"));
  await page.keyboard.press("Enter");
  await expect(
    page.getByText("Estado registrado:", { exact: false }),
  ).toBeVisible();
  await inspect("details");
  const clearContext = page.getByRole("link", {
    name: "Quitar filtro de contexto",
    exact: true,
  });
  await keyboard(clearContext);
  await page.keyboard.press("Enter");
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
  await page.keyboard.press("Enter");
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
  await page.keyboard.press("Enter");
  await expect(
    page.getByText("No hay hechos con estos filtros.", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
  ).toBeFocused();
  await inspect("empty");
  const clear = page.getByRole("link", {
    name: "Limpiar filtros",
    exact: true,
  });
  await keyboard(clear);
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("list", { name: "Hechos del historial" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
  ).toBeFocused();
  await inspect("list");
});
