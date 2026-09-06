import { loginSession } from "../scripts/session-client.mjs";
import { test, expect } from "./support/authenticated-test.mjs";
import { sql, create } from "./support/projects.mjs";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(() => sql("TRUNCATE tasks, outbox_events, projects"));

test("read_projects: real creation survives list/detail navigation and fresh reload @s14 @s20 @s30", async ({
  page,
}) => {
  const name = "<b>Zenit 🚀</b>";
  const description =
    "<script>window.projectMarkupExecuted=true</script> — contenido privado";
  await page.goto("/");
  await page.getByLabel(/Nombre del proyecto/).fill(name);
  await page.getByLabel(/Descripción/).fill(description);
  const confirmed = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/v1/projects") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  const saved = await confirmed;
  expect(saved.status()).toBe(201);
  const project = await saved.json();
  await page.goto("/proyectos");
  await expect(
    page.getByRole("heading", { name: "Proyectos", exact: true }),
  ).toBeVisible();
  const items = page.getByRole("list", { name: "Proyectos guardados" });
  await items.getByRole("link", { name, exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/proyectos/${project.id}$`));
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  await expect(page.getByText(description, { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name, exact: true })).toBeVisible();
  await expect(page.getByText(description, { exact: true })).toBeVisible();
  expect(
    await page.evaluate(() => window.projectMarkupExecuted),
  ).toBeUndefined();
  expect(await page.locator("main script, main b").count()).toBe(0);
  expect(
    (
      await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
        .analyze()
    ).violations,
  ).toEqual([]);
  await page
    .getByRole("link", { name: "Volver a proyectos", exact: true })
    .click();
  await expect(page).toHaveURL(/\/proyectos$/);
  await expect(items.getByRole("link", { name, exact: true })).toBeVisible();
});

test("read_projects: touch navigation works without hover @s26", async ({
  browser,
  request,
}) => {
  const project = await create(request, "Proyecto táctil");
  const context = await browser.newContext({
    baseURL: process.env.E2E_BASE_URL ?? "http://127.0.0.1:18080",
    viewport: { width: 390, height: 700 },
    hasTouch: true,
  });
  try {
    await loginSession(context.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    const page = await context.newPage();
    await page.goto(
      `${process.env.E2E_BASE_URL ?? "http://127.0.0.1:18080"}/proyectos`,
    );
    expect(await page.evaluate(() => navigator.maxTouchPoints)).toBeGreaterThan(
      0,
    );
    await page.getByRole("link", { name: project.name, exact: true }).tap();
    await expect(
      page.getByRole("heading", { name: project.name, exact: true }),
    ).toBeVisible();
    await page
      .getByRole("link", { name: "Volver a proyectos", exact: true })
      .tap();
    await expect(
      page.getByRole("link", { name: project.name, exact: true }),
    ).toBeVisible();
  } finally {
    await context.close();
  }
});

test("read_projects: 21 real projects paginate without repeats after a newer creation @s4 @s5 @s6 @s13 @s18 @s19", async ({
  page,
  request,
}) => {
  for (let index = 0; index < 21; index++)
    await create(request, `Paginación ${index}`);
  sql(
    "UPDATE projects SET created_at='2026-09-05T12:00:00Z',updated_at='2026-09-05T12:00:00Z'",
  );
  const expected = sql(
    "SELECT id FROM projects ORDER BY created_at DESC,id DESC",
  ).split(/\r?\n/);
  const first = await (await request.get("/api/v1/projects")).json();
  expect(first.items.map((item) => item.id)).toEqual(expected.slice(0, 20));
  expect(first.nextCursor).toBeTruthy();
  await page.goto("/proyectos");
  const links = page
    .getByRole("list", { name: "Proyectos guardados" })
    .getByRole("link");
  await expect(links).toHaveCount(20);
  expect(
    await links.evaluateAll((elements) =>
      elements.map((element) => element.getAttribute("href").split("/").at(-1)),
    ),
  ).toEqual(expected.slice(0, 20));
  await create(request, "Creado después de la primera página");
  const snapshot = sql(
    "SELECT json_build_object('projects',(SELECT json_agg(p ORDER BY id) FROM projects p),'events',(SELECT json_agg(e ORDER BY event_id) FROM outbox_events e))",
  );
  await page.getByRole("link", { name: "Más antiguos", exact: true }).click();
  await expect(page).toHaveURL(
    new RegExp(`/proyectos\\?cursor=${first.nextCursor}$`),
  );
  await expect(links).toHaveCount(1);
  await expect(links).toHaveAttribute("href", `/proyectos/${expected[20]}`);
  await expect(
    page.getByRole("heading", { name: "Proyectos", exact: true }),
  ).toBeFocused();
  await expect(
    page.getByRole("link", { name: "Más antiguos", exact: true }),
  ).toHaveCount(0);
  await page.reload();
  await expect(links).toHaveCount(1);
  await page
    .getByRole("link", { name: "Volver al inicio", exact: true })
    .click();
  await expect(page).toHaveURL(/\/proyectos$/);
  await expect(links.first()).toHaveAccessibleName(
    "Creado después de la primera página",
  );
  expect(
    sql(
      "SELECT json_build_object('projects',(SELECT json_agg(p ORDER BY id) FROM projects p),'events',(SELECT json_agg(e ORDER BY event_id) FROM outbox_events e))",
    ),
  ).toBe(snapshot);
});

test("read_projects: empty guidance and private queries do not expose another owner @s1 @s2 @s3 @s10 @s15 @s21 @s29", async ({
  page,
  request,
  baseURL,
}) => {
  await page.goto("/proyectos");
  await expect(
    page.getByText("Todavía no tienes proyectos", { exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Crear proyecto", exact: true }).click();
  await expect(page).toHaveURL(/\/$/);
  const own = await create(request, "Proyecto propio");
  const other = await create(request, "Nombre ajeno que no debe filtrarse");
  expect(other.id).toMatch(/^[0-9a-f-]{36}$/i);
  sql(
    `UPDATE projects SET owner_id='another-test-owner' WHERE id='${other.id}'`,
  );
  for (const path of ["/api/v1/projects", `/api/v1/projects/${own.id}`]) {
    // Node fetch has no Playwright context defaults or cached HTTP credentials.
    const response = await fetch(new URL(path, baseURL));
    expect(response.status).toBe(401);
    const body = await response.text();
    expect(body).toContain("UNAUTHENTICATED");
    expect(body).not.toContain("Proyecto propio");
  }
  const listing = await request.get("/api/v1/projects");
  expect(listing.headers()["cache-control"]).toContain("no-store");
  const body = await listing.json();
  expect(body.items.map((item) => item.id)).toEqual([own.id]);
  expect(Object.keys(body.items[0]).sort()).toEqual(
    ["id", "name", "status", "createdAt", "updatedAt"].sort(),
  );
  const detail = await request.get(`/api/v1/projects/${own.id}`);
  expect(detail.headers()["cache-control"]).toContain("no-store");
  const missing = await request.get(
    "/api/v1/projects/00000000-0000-0000-0000-000000000000",
  );
  const foreign = await request.get(`/api/v1/projects/${other.id}`);
  expect(missing.status()).toBe(404);
  expect(foreign.status()).toBe(404);
  const hidden = await foreign.json();
  const absent = await missing.json();
  expect(hidden.code).toBe("PROJECT_NOT_FOUND");
  expect(hidden).toEqual(absent);
  expect(JSON.stringify(hidden)).not.toContain(other.name);
  await page.goto(`/proyectos/${own.id}`);
  await expect(
    page.getByRole("heading", { name: own.name, exact: true }),
  ).toBeVisible();
  await page.goto(`/proyectos/${other.id}`);
  await expect(
    page.getByText("Proyecto no encontrado", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText(own.name, { exact: true })).toHaveCount(0);
  expect(
    await page.evaluate(() => ({
      local: localStorage.length,
      session: sessionStorage.length,
    })),
  ).toEqual({ local: 0, session: 0 });
});

test("read_projects: honest loading, recoverable errors and expired authentication @s16 @s17 @s27 @s31 @s32", async ({
  page,
  request,
}) => {
  const project = await create(request, "Lectura recuperable");
  for (const detail of [false, true]) {
    const projectContent = page.getByRole(detail ? "heading" : "link", {
      name: project.name,
      exact: true,
    });
    const path = detail ? `/proyectos/${project.id}` : "/proyectos";
    const api = detail
      ? `**/api/v1/projects/${project.id}`
      : "**/api/v1/projects";
    const loading = detail ? "Cargando proyecto" : "Cargando proyectos";
    await page.addInitScript(() => {
      window.loadingShownAt = null;
      new MutationObserver(() => {
        if (
          window.loadingShownAt === null &&
          Array.from(document.querySelectorAll('[role="status"]')).some(
            (element) => /^Cargando proyecto/.test(element.textContent),
          )
        )
          window.loadingShownAt = performance.now();
      }).observe(document, { childList: true, subtree: true });
    });
    let release;
    const blocked = new Promise((resolve) => {
      release = resolve;
    });
    await page.route(api, async (route) => {
      await blocked;
      await route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          status: 503,
          code: "STORAGE_UNAVAILABLE",
          message: "No disponible temporalmente",
        }),
      });
    });
    await page.goto(path);
    await expect(page.getByRole("status")).toHaveText(loading);
    const feedbackMs = await page.evaluate(() => window.loadingShownAt);
    expect(feedbackMs).not.toBeNull();
    expect(feedbackMs).toBeLessThan(400);
    console.info(
      `${detail ? "detail" : "list"} loading feedback: ${Math.round(feedbackMs)} ms from navigation`,
    );
    await expect(
      page.getByText("Todavía no tienes proyectos", { exact: true }),
    ).toHaveCount(0);
    release();
    await expect(page.getByRole("alert")).toBeVisible();
    await expect(projectContent).toHaveCount(0);
    expect(
      (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations,
    ).toEqual([]);
    await page.unroute(api);
    await page.getByRole("button", { name: "Reintentar", exact: true }).click();
    await expect(projectContent).toBeVisible();
    await expect(page.getByRole("alert")).toHaveCount(0);
    sql("DELETE FROM spring_session WHERE principal_name='e2e-user'");
    await page.reload();
    await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
    await expect(projectContent).toHaveCount(0);
    await loginSession(request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
  }
});

test("read_projects: long text reflows across twelve widths with accessible keyboard and touch targets @s23 @s24 @s26", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(120_000);
  const project = await create(
    request,
    "Proyecto " + "W".repeat(111),
    "W".repeat(4000),
  );
  for (const width of [
    320, 360, 390, 480, 600, 768, 820, 1024, 1280, 1440, 1920, 2560,
  ]) {
    await test.step(`${width} CSS pixels: list and detail`, async () => {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      for (const detail of [false, true]) {
        await page.goto(detail ? `/proyectos/${project.id}` : "/proyectos");
        const target = detail
          ? page.getByRole("heading", { name: project.name, exact: true })
          : page.getByRole("link", { name: project.name, exact: true });
        await expect(target).toBeVisible();
        expect(
          await page.evaluate(
            () =>
              document.documentElement.scrollWidth <=
              document.documentElement.clientWidth,
          ),
        ).toBe(true);
        const controls = page.locator("main a, main button");
        for (const control of await controls.all()) {
          const box = await control.boundingBox();
          expect(box.width).toBeGreaterThanOrEqual(44);
          expect(box.height).toBeGreaterThanOrEqual(44);
          expect(box.x).toBeGreaterThanOrEqual(0);
          expect(box.x + box.width).toBeLessThanOrEqual(width + 1);
        }
        expect(
          (
            await new AxeBuilder({ page })
              .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
              .analyze()
          ).violations,
        ).toEqual([]);
        if ([320, 1440].includes(width))
          await page.screenshot({
            path: testInfo.outputPath(
              `read-${detail ? "detail" : "list"}-${width}.png`,
            ),
            fullPage: true,
          });
      }
    });
  }
  await page.setViewportSize({ width: 320, height: 700 });
  await page.goto("/proyectos");
  const projectLink = page.getByRole("link", {
    name: project.name,
    exact: true,
  });
  for (
    let index = 0;
    index < 12 &&
    !(await projectLink.evaluate(
      (element) => element === document.activeElement,
    ));
    index++
  )
    await page.keyboard.press("Tab");
  await expect(projectLink).toBeFocused();
  expect(
    await projectLink.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(new RegExp(`/proyectos/${project.id}$`));
  const back = page.getByRole("link", {
    name: "Volver a proyectos",
    exact: true,
  });
  await expect(
    page.getByRole("heading", { name: project.name, exact: true }),
  ).toBeFocused();
  await page.keyboard.press("Shift+Tab");
  await expect(back).toBeFocused();
  expect(
    await back.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await page.keyboard.press("Enter");
  await expect(page).toHaveURL(/\/proyectos$/);
});
