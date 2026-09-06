import { test, expect } from "./support/authenticated-test.mjs";
import { sql } from "./support/projects.mjs";
import { seedAgenda } from "./support/today.mjs";
import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "node:fs/promises";

test.beforeEach(() =>
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("today: root reads a real empty UTC fallback without writing preferences @s1 @s3 @s19 @s21 @s32", async ({
  page,
}) => {
  const read = page.waitForResponse((response) =>
    response.url().endsWith("/api/v1/today"),
  );
  expect((await page.goto("/")).status()).toBe(200);
  const response = await read;
  expect(response.status()).toBe(200);
  expect(response.headers()["cache-control"]).toContain("no-store");
  const snapshot = await response.json();
  expect(snapshot).toMatchObject({
    zoneId: "UTC",
    zoneSource: "UNCONFIGURED",
    availabilityZoneId: null,
    budgetMinutes: null,
    plannedSeconds: 0,
    remainingSeconds: null,
    excessSeconds: null,
    currentBlockId: null,
    nextBlockId: null,
    closingAt: null,
    items: [],
  });
  await expect(
    page.getByRole("heading", { name: "Hoy", exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel(/Nombre del proyecto/)).toHaveCount(0);
  const main = page.getByRole("main");
  await expect(main).toContainText(/no hay bloques planificados/i);
  await expect(main).toContainText("UTC");
  await expect(main).toContainText(/desconocid/i);
  await expect(main.locator('a[href="/disponibilidad"]')).toBeVisible();
  await expect(main.locator('a[href="/proyectos"]')).toBeVisible();
  expect(sql("SELECT count(*) FROM availability_preferences")).toBe("0");
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("0");
  expect(sql("SELECT count(*) FROM outbox_events")).toBe("0");
});

test("today: persisted agenda shows names, capacity and current block then opens its task @s2 @s7 @s18", async ({
  page,
  request,
}, testInfo) => {
  const { project, task, block, start, end } = await seedAgenda(request);
  const eventsBefore = sql("SELECT count(*) FROM outbox_events");
  const read = page.waitForResponse((response) =>
    response.url().endsWith("/api/v1/today"),
  );
  await page.goto("/");
  const response = await read;
  expect(response.status()).toBe(200);
  const snapshot = await response.json();
  const seconds =
    (Math.min(Date.parse(end), Date.parse(snapshot.dayEndAt)) -
      Math.max(Date.parse(start), Date.parse(snapshot.dayStartAt))) /
    1000;
  expect(snapshot).toMatchObject({
    budgetMinutes: 120,
    plannedSeconds: seconds,
    remainingSeconds: 7200 - seconds,
    excessSeconds: 0,
    currentBlockId: block.id,
    nextBlockId: null,
    closingAt: end.replace(".000Z", "Z"),
    items: [
      {
        projectName: project.name,
        taskTitle: task.title,
        block: { id: block.id },
      },
    ],
  });
  const main = page.getByRole("main");
  await expect(main.getByText(project.name, { exact: true })).toBeVisible();
  await expect(
    main.getByText("En horario planificado", { exact: true }),
  ).toBeVisible();
  await expect(
    main.getByText("Preparar un borrador revisable", { exact: true }),
  ).toBeVisible();
  await expect(main.locator("b")).toHaveCount(0);
  const summary = main.locator("dl");
  await expect(
    summary.locator("dt", { hasText: "Tiempo planificado" }).locator("+ dd"),
  ).toHaveText(`${seconds / 60} min`);
  await expect(
    summary.locator("dt", { hasText: "Presupuesto del día" }).locator("+ dd"),
  ).toHaveText("120 min");
  for (const width of [320, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.screenshot({
      path: testInfo.outputPath(`today-agenda-${width}.png`),
      fullPage: true,
    });
  }
  await main.getByRole("link", { name: task.title, exact: true }).click();
  await expect(page).toHaveURL(
    new RegExp(`/proyectos/${project.id}/tareas/${task.id}$`),
  );
  await expect(
    page.getByRole("heading", { name: task.title, exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM outbox_events")).toBe(eventsBefore);
  expect(sql(`SELECT status FROM tasks WHERE id='${task.id}'`)).toBe("pending");
});

test("today: capture has its own route and logout removes the private agenda before a fresh login @s31 @s32 @s33 @s34 @s36", async ({
  page,
  request,
}) => {
  const { project, task } = await seedAgenda(request);
  await page.goto("/");
  await expect(
    page.getByRole("link", { name: task.title, exact: true }),
  ).toBeVisible();
  const navigation = page.getByRole("navigation", { name: "Principal" });
  await expect(
    navigation.getByRole("link", { name: "Hoy", exact: true }),
  ).toHaveAttribute("aria-current", "page");
  await navigation
    .getByRole("link", { name: "Proyectos", exact: true })
    .click();
  await page.getByRole("link", { name: /Crear proyecto/ }).click();
  await expect(page).toHaveURL(/\/proyectos\/nuevo$/);
  await page.getByLabel(/Nombre del proyecto/).fill("Captura desde Hoy");
  const saved = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/v1/projects") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  expect((await saved).status()).toBe(201);
  await expect(page.getByRole("status")).toContainText("Proyecto guardado");
  await navigation.getByRole("link", { name: "Hoy", exact: true }).click();
  await expect(
    page.getByRole("link", { name: task.title, exact: true }),
  ).toBeVisible();
  const logout = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/session/logout") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  expect((await logout).status()).toBe(204);
  await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
  await expect(page.getByText(project.name, { exact: true })).toHaveCount(0);
  await expect(
    page.getByRole("link", { name: task.title, exact: true }),
  ).toHaveCount(0);
  expect((await request.get("/api/v1/today")).status()).toBe(401);
  await page.getByLabel("Usuario", { exact: true }).fill("e2e-user");
  await page
    .getByLabel("Contraseña", { exact: true })
    .fill("e2e-only-password");
  const fresh = page.waitForResponse((response) =>
    response.url().endsWith("/api/v1/today"),
  );
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  expect((await fresh).status()).toBe(200);
  await expect(page).toHaveURL(/\/$/);
  await expect(
    page.getByRole("link", { name: task.title, exact: true }),
  ).toBeVisible();
});

test("today: loading, long agenda, retained error and empty states reflow with keyboard feedback @s21 @s23 @s29 @s37 @s38", async ({
  page,
  request,
}, testInfo) => {
  const { project, task } = await seedAgenda(request);
  const longName = "Proyecto Unicode 🧭 con contexto largo ".repeat(3).trim();
  sql(`UPDATE projects SET name='${longName}' WHERE id='${project.id}'`);
  let release;
  let pending = new Promise((resolve) => {
    release = resolve;
  });
  let fail = false;
  await page.route("**/api/v1/today", async (route) => {
    const response = await route.fetch();
    await pending;
    if (fail)
      await route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({ code: "STORAGE_UNAVAILABLE" }),
      });
    else await route.fulfill({ response });
  });
  const widths = [
    320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
    768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
    1601, 1920, 2560,
  ];
  const evidence = [];
  async function inspect(state) {
    for (const width of widths) {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      const measure = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            'nav[aria-label="Principal"] a,main button,main a,header button',
          ),
        ].map((element) => {
          const box = element.getBoundingClientRect();
          return {
            name: element.textContent,
            x: box.x,
            width: box.width,
            height: box.height,
          };
        }),
      }));
      expect(measure.scroll, `${state}:${width} overflow`).toBeLessThanOrEqual(
        width,
      );
      for (const box of measure.controls) {
        expect(box.x, `${state}:${width}:${box.name}`).toBeGreaterThanOrEqual(
          0,
        );
        expect(box.x + box.width).toBeLessThanOrEqual(width + 1);
        expect(box.width).toBeGreaterThanOrEqual(44);
        expect(box.height).toBeGreaterThanOrEqual(44);
      }
      evidence.push({ state, ...measure });
    }
    await page.setViewportSize({ width: 320, height: 700 });
    expect(
      (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations,
    ).toEqual([]);
    if (["agenda", "error"].includes(state)) {
      await page.screenshot({
        path: testInfo.outputPath(`today-${state}-320.png`),
        fullPage: true,
      });
    }
  }
  await page.goto("/");
  await expect(page.getByRole("status")).toHaveText("Cargando Hoy…");
  await inspect("loading");
  release();
  await expect(
    page.getByRole("link", { name: task.title, exact: true }),
  ).toBeVisible();
  await inspect("agenda");
  await page
    .getByRole("main")
    .evaluate((element) => (element.style.fontSize = "200%"));
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page
    .getByRole("main")
    .evaluate((element) => (element.style.fontSize = ""));
  const update = page.getByRole("button", { name: "Actualizar", exact: true });
  await page.getByRole("heading", { name: "Hoy", exact: true }).focus();
  await page.keyboard.press("Tab");
  await expect(update).toBeFocused();
  expect(
    await update.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  pending = new Promise((resolve) => {
    release = resolve;
  });
  await page.evaluate(() => {
    const started = performance.now();
    const observer = new MutationObserver(() => {
      if (
        document.querySelector('[role="status"]')?.textContent ===
        "Actualizando Hoy…"
      ) {
        window.todayFeedback = performance.now() - started;
        observer.disconnect();
      }
    });
    observer.observe(document.querySelector("main"), {
      subtree: true,
      childList: true,
      characterData: true,
    });
  });
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toHaveText("Actualizando Hoy…");
  const feedback = await page.evaluate(() => window.todayFeedback);
  expect(feedback).toBeGreaterThanOrEqual(0);
  expect(feedback).toBeLessThan(400);
  await inspect("updating");
  fail = true;
  release();
  await expect(page.getByRole("alert")).toContainText("Sin actualizar");
  await expect(
    page.getByRole("link", { name: task.title, exact: true }),
  ).toBeVisible();
  await inspect("error");
  await expect.soft(update).toBeFocused();
  fail = false;
  sql("DELETE FROM planned_blocks");
  await page.getByRole("button", { name: "Reintentar", exact: true }).click();
  await expect(
    page.getByText("No hay bloques planificados", { exact: true }),
  ).toBeVisible();
  await inspect("empty");
  await page
    .getByRole("main")
    .evaluate((element) => (element.style.fontSize = "200%"));
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  const evidenceFile = testInfo.outputPath("today-layout-and-feedback.json");
  await writeFile(
    evidenceFile,
    JSON.stringify(
      {
        engine: testInfo.project.use.browserName ?? "chromium",
        feedbackMs: feedback,
        evidence,
        network:
          "Real API/PG; first and update responses deliberately retained; update 503 injected",
      },
      null,
      2,
    ),
  );
  await testInfo.attach("today-layout-and-feedback", {
    path: evidenceFile,
    contentType: "application/json",
  });
});
