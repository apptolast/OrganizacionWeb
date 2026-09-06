import { test, expect } from "./support/authenticated-test.mjs";
test("real browser saves an idea through the same-origin API @s1 @s22", async ({
  page,
}) => {
  expect((await page.goto("/proyectos/nuevo")).status()).toBe(200);
  await page.getByLabel(/Nombre del proyecto/).fill("  Zenit Digital  ");
  await page.getByLabel(/Descripción/).fill("Preparar la web");
  const responsePromise = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/v1/projects") && r.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  const response = await responsePromise;
  expect(response.status()).toBe(201);
  const project = await response.json();
  expect(project.name).toBe("Zenit Digital");
  expect(project.status).toBe("idea");
  await expect(page.getByRole("status")).toContainText("Proyecto guardado");
  await expect(page.getByText(project.id, { exact: true })).toBeVisible();
});

import { execFileSync } from "node:child_process";
function compose(...args) {
  if (!process.env.E2E_COMPOSE_PROJECT || !process.env.E2E_ENV_FILE)
    throw new Error("Use pnpm test:e2e for isolated database verification");
  return execFileSync(
    "docker",
    [
      "compose",
      "--env-file",
      process.env.E2E_ENV_FILE,
      "-p",
      process.env.E2E_COMPOSE_PROJECT,
      "-f",
      "docker-compose.yml",
      ...args,
    ],
    { encoding: "utf8", timeout: 60_000 },
  );
}
function stored(id) {
  expect(id).toMatch(/^[0-9a-f-]{36}$/i);
  return JSON.parse(
    compose(
      "exec",
      "-T",
      "postgres",
      "psql",
      "-U",
      "e2e_user",
      "-d",
      "organization",
      "-tA",
      "-v",
      "ON_ERROR_STOP=1",
      "-c",
      `SELECT json_build_object('project', row_to_json(p), 'event', row_to_json(e)) FROM projects p JOIN outbox_events e ON e.aggregate_id=p.id WHERE p.id='${id}'`,
    ),
  );
}
test("project and outbox survive reload and backend restart @s16 @s19 @s20", async ({
  page,
  request,
}) => {
  test.setTimeout(90_000);
  expect((await page.goto("/proyectos/nuevo")).status()).toBe(200);
  await page.getByLabel(/Nombre del proyecto/).fill("Persistencia");
  await page.getByLabel(/Descripción/).fill("Contenido privado");
  const confirmed = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/v1/projects") && r.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  const response = await confirmed;
  expect(response.status()).toBe(201);
  const project = await response.json();
  const before = stored(project.id);
  expect(before.project).toMatchObject({
    id: project.id,
    owner_id: "e2e-user",
    name: "Persistencia",
    description: "Contenido privado",
    status: "idea",
  });
  expect(before.event).toMatchObject({
    aggregate_id: project.id,
    owner_id: "e2e-user",
    event_type: "ProjectCreated.v1",
    status: "pending",
    schema_version: 1,
  });
  expect(before.event.payload.name).toBe("Persistencia");
  expect(before.event.payload).not.toHaveProperty("description");
  await page.reload();
  expect(stored(project.id)).toEqual(before);
  compose("restart", "backend");
  await expect
    .poll(
      async () => {
        try {
          return (await request.get("/api/session")).status();
        } catch {
          return 0;
        }
      },
      { timeout: 60_000 },
    )
    .toBe(200);
  expect((await (await request.get("/api/session")).json()).authenticated).toBe(
    true,
  );
  await page.goto(`/proyectos/${project.id}`);
  await expect(
    page.getByRole("heading", { name: "Persistencia", exact: true }),
  ).toBeVisible();
  expect(stored(project.id)).toEqual(before);
});

test("server-confirmed markup remains literal text @s26", async ({ page }) => {
  expect((await page.goto("/proyectos/nuevo")).status()).toBe(200);
  const dialogs = [];
  page.on("dialog", (dialog) => {
    dialogs.push(dialog.message());
    dialog.dismiss();
  });
  await page.getByLabel(/Nombre del proyecto/).fill("<b>Idea</b>");
  await page.getByLabel(/Descripción/).fill("<script>alert(1)</script>");
  const confirmed = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/v1/projects") && r.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  expect((await confirmed).status()).toBe(201);
  const result = page.getByRole("complementary", {
    name: "Resultado de creación",
  });
  await expect(result.getByText("<b>Idea</b>", { exact: true })).toBeVisible();
  await expect(
    result.getByText("<script>alert(1)</script>", { exact: true }),
  ).toBeVisible();
  await expect(result.locator("b, script")).toHaveCount(0);
  expect(dialogs).toEqual([]);
});

test("server validation preserves both fields and permits correction @s24", async ({
  page,
}) => {
  expect((await page.goto("/proyectos/nuevo")).status()).toBe(200);
  const description = "🚀".repeat(4001);
  await page.getByLabel(/Nombre del proyecto/).fill("Idea");
  await page.getByLabel(/Descripción/).fill(description);
  const rejected = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/v1/projects") && r.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  expect((await rejected).status()).toBe(400);
  await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue("Idea");
  await expect(page.getByLabel(/Descripción/)).toHaveValue(description);
  await expect(page.getByLabel(/Descripción/)).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  await expect(page.locator("#description-error")).not.toBeEmpty();
  await expect(page.getByRole("status")).not.toContainText("Proyecto guardado");
  await page.getByLabel(/Descripción/).fill("Una sección cada día");
  const accepted = page.waitForResponse(
    (r) =>
      r.url().endsWith("/api/v1/projects") && r.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  expect((await accepted).status()).toBe(201);
  await expect(page.getByRole("status")).toContainText("Proyecto guardado");
});

import AxeBuilder from "@axe-core/playwright";
for (const [width, zoom] of [
  [320, 1],
  [768, 1],
  [1440, 1],
  [1440, 2],
]) {
  test(`accessible keyboard creation at ${width}px and ${zoom * 100}% reflow @s27 @s28`, async ({
    page,
  }, testInfo) => {
    // Browser zoom halves the CSS viewport at 200%; reproduce that reflow width.
    await page.setViewportSize({ width: width / zoom, height: 900 / zoom });
    expect((await page.goto("/proyectos/nuevo")).status()).toBe(200);
    const skipLink = page.getByRole("link", { name: /Saltar al contenido/ });
    expect(
      await skipLink.evaluate(
        (element) => element.getBoundingClientRect().bottom,
      ),
    ).toBeLessThanOrEqual(0);
    // The session gate focuses the destination heading after access is checked.
    // Reach the skip link by keyboard from that meaningful initial focus.
    for (
      let step = 0;
      step < 12 &&
      !(await skipLink.evaluate(
        (element) => element === document.activeElement,
      ));
      step++
    )
      await page.keyboard.press("Shift+Tab");
    await expect(skipLink).toBeFocused();
    expect(
      await skipLink.evaluate((element) => element.getBoundingClientRect().top),
    ).toBeGreaterThanOrEqual(0);
    const name = page.getByLabel(/Nombre del proyecto/);
    const description = page.getByLabel(/Descripción/);
    const submit = page.getByRole("button", {
      name: "Crear proyecto",
      exact: true,
    });
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
    ).toBe(true);
    expect(
      (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations,
    ).toEqual([]);
    for (
      let step = 0;
      step < 10 &&
      !(await name.evaluate((element) => element === document.activeElement));
      step++
    ) {
      await page.keyboard.press("Tab");
    }
    await expect(name).toBeFocused();
    expect(
      await name.evaluate((element) =>
        parseFloat(getComputedStyle(element).outlineWidth),
      ),
    ).toBeGreaterThan(0);
    await page.keyboard.type("Zenit Digital (prueba)");
    await page.keyboard.press("Tab");
    await expect(description).toBeFocused();
    await page.keyboard.type("Preparar una seccion cada dia.");
    await page.keyboard.press("Tab");
    await expect(submit).toBeFocused();
    const confirmed = page.waitForResponse(
      (r) =>
        r.url().endsWith("/api/v1/projects") && r.request().method() === "POST",
    );
    await page.keyboard.press("Enter");
    expect((await confirmed).status()).toBe(201);
    await expect(page.getByRole("status")).toContainText("Proyecto guardado");
    await expect(submit).toBeFocused();
    expect(
      await submit.evaluate((element) =>
        parseFloat(getComputedStyle(element).outlineWidth),
      ),
    ).toBeGreaterThan(0);
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
    ).toBe(true);
    expect(
      (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations,
    ).toEqual([]);
    if (zoom === 1 && [320, 1440].includes(width)) {
      await page.screenshot({
        path: testInfo.outputPath(
          `organizationweb-${width === 320 ? "mobile" : "desktop"}.png`,
        ),
        fullPage: true,
      });
    }
  });
}
