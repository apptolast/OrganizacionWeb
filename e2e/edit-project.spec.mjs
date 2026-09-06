import { loginSession, csrfHeaders } from "../scripts/session-client.mjs";
import { test, expect } from "./support/authenticated-test.mjs";
import { sql, create, stored } from "./support/projects.mjs";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(() => sql("TRUNCATE outbox_events, projects"));

test("edit_project: real update persists its event and unchanged save writes nothing @s1 @s3 @s7 @s12 @s17 @s21", async ({
  page,
  request,
}) => {
  const original = await create(
    request,
    "Proyecto inicial",
    "Descripción original",
  );
  const before = stored(original.id);
  const initial = await request.get(`/api/v1/projects/${original.id}`);
  const etag = initial.headers().etag;
  expect(etag).toBeTruthy();
  await page.goto(`/proyectos/${original.id}`);
  await page
    .getByRole("link", { name: "Editar proyecto", exact: true })
    .click();
  await expect(page).toHaveURL(new RegExp(`/proyectos/${original.id}/editar$`));
  const name = page.getByLabel(/Nombre del proyecto/);
  const description = page.getByLabel(/Descripción/);
  await expect(name).toHaveValue(original.name);
  await expect(description).toHaveValue(original.description);
  await name.fill("  <b>Zenit mejorado 🚀</b>  ");
  const text =
    "<script>window.updatedMarkup=true</script>\nUna sección cada día.";
  await description.fill(text);
  await page.evaluate(() => {
    document.querySelector("form").addEventListener(
      "submit",
      () => {
        window.editStartedAt = performance.now();
      },
      { capture: true, once: true },
    );
    new MutationObserver(() => {
      if (
        document.querySelector('[role="status"]')?.textContent ===
          "Guardando cambios" &&
        window.editFeedbackMs === undefined
      )
        window.editFeedbackMs = performance.now() - window.editStartedAt;
    }).observe(document, {
      subtree: true,
      childList: true,
      characterData: true,
    });
  });
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  let attempts = 0;
  await page.route(`**/api/v1/projects/${original.id}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    attempts++;
    const actual = await route.fetch();
    await gate;
    await route.fulfill({ response: actual });
  });
  const confirmed = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/api/v1/projects/${original.id}`) &&
      response.request().method() === "PUT",
  );
  await page
    .getByRole("button", { name: "Guardar cambios", exact: true })
    .click();
  try {
    await expect(page.getByRole("status")).toHaveText("Guardando cambios");
    await expect(
      page.getByRole("button", { name: "Guardar cambios", exact: true }),
    ).toBeDisabled();
    const feedbackMs = await page.evaluate(() => window.editFeedbackMs);
    expect(feedbackMs).toBeGreaterThanOrEqual(0);
    expect(feedbackMs).toBeLessThan(400);
    console.info(`Edit save feedback: ${Math.round(feedbackMs)} ms`);
    await page.locator("form").evaluate((form) => form.requestSubmit());
  } finally {
    release();
  }
  const response = await confirmed;
  expect(response.status()).toBe(200);
  expect(response.request().headers()["if-match"]).toBe(etag);
  expect(response.headers()["cache-control"]).toContain("no-store");
  expect(response.headers().etag).not.toBe(etag);
  expect(attempts).toBe(1);
  await page.unroute(`**/api/v1/projects/${original.id}`);
  const updated = await response.json();
  expect(Object.keys(updated)).toHaveLength(7);
  expect(updated).toMatchObject({
    id: original.id,
    ownerId: original.ownerId,
    createdAt: original.createdAt,
    status: original.status,
    name: "<b>Zenit mejorado 🚀</b>",
    description: text,
  });
  await expect(page.getByRole("status")).toContainText("Proyecto actualizado");
  const after = stored(original.id);
  expect(after.project.version).toBe(before.project.version + 1);
  expect(after.events).toHaveLength(2);
  expect(
    after.events.find((event) => event.event_type === "ProjectCreated.v1"),
  ).toEqual(before.events[0]);
  const event = after.events.find(
    (event) => event.event_type === "ProjectUpdated.v1",
  );
  expect(event).toMatchObject({
    aggregate_id: original.id,
    owner_id: original.ownerId,
    status: "pending",
    schema_version: 1,
  });
  expect(Object.keys(event.payload).sort()).toEqual(
    [
      "eventId",
      "aggregateId",
      "ownerId",
      "occurredAt",
      "schemaVersion",
      "name",
      "type",
    ].sort(),
  );
  expect(event.payload).toMatchObject({
    aggregateId: original.id,
    ownerId: original.ownerId,
    name: updated.name,
    type: "ProjectUpdated.v1",
    occurredAt: updated.updatedAt,
  });
  const unchanged = await request.put(`/api/v1/projects/${original.id}`, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": response.headers().etag,
    },
    data: { name: updated.name, description: updated.description },
  });
  expect(unchanged.status()).toBe(200);
  expect(unchanged.headers().etag).toBe(response.headers().etag);
  expect(stored(original.id)).toEqual(after);
  await page.reload();
  await expect(name).toHaveValue(updated.name);
  await expect(description).toHaveValue(text);
  expect(await page.evaluate(() => window.updatedMarkup)).toBeUndefined();
  expect(
    await page.evaluate(() => ({
      local: localStorage.length,
      session: sessionStorage.length,
    })),
  ).toEqual({ local: 0, session: 0 });
  await page.getByRole("link", { name: "Cancelar", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: updated.name, exact: true }),
  ).toBeVisible();
});

test("edit_project: two tabs preserve a rejected draft until deliberate reload @s2 @s19", async ({
  page,
  context,
  request,
}) => {
  const original = await create(
    request,
    "Proyecto compartido",
    "Versión inicial",
  );
  const second = await context.newPage();
  try {
    for (const tab of [page, second]) {
      await tab.goto(`/proyectos/${original.id}/editar`);
      await expect(tab.getByLabel(/Nombre del proyecto/)).toHaveValue(
        original.name,
      );
    }
    await page
      .getByLabel(/Nombre del proyecto/)
      .fill("Guardado desde primera pestaña");
    const firstResponse = page.waitForResponse(
      (response) => response.request().method() === "PUT",
    );
    await page
      .getByRole("button", { name: "Guardar cambios", exact: true })
      .click();
    const accepted = await firstResponse;
    expect(accepted.status()).toBe(200);
    const afterFirst = stored(original.id);
    const attempts = [];
    second.on("request", (request) => {
      if (request.method() === "PUT") attempts.push(request);
    });
    await second
      .getByLabel(/Nombre del proyecto/)
      .fill("Mi borrador pendiente");
    await second.getByLabel(/Descripción/).fill("No perder este texto");
    const conflictResponse = second.waitForResponse(
      (response) => response.request().method() === "PUT",
    );
    await second
      .getByRole("button", { name: "Guardar cambios", exact: true })
      .click();
    const conflict = await conflictResponse;
    expect(conflict.status()).toBe(412);
    expect(conflict.request().headers()["if-match"]).toBe(
      accepted.request().headers()["if-match"],
    );
    await expect(second.getByRole("alert")).toBeVisible();
    await expect(second.getByLabel(/Nombre del proyecto/)).toHaveValue(
      "Mi borrador pendiente",
    );
    await expect(second.getByLabel(/Descripción/)).toHaveValue(
      "No perder este texto",
    );
    expect(stored(original.id)).toEqual(afterFirst);
    expect(attempts).toHaveLength(1);
    await second
      .getByRole("button", { name: "Recargar versión guardada", exact: true })
      .click();
    await expect(second.getByLabel(/Nombre del proyecto/)).toHaveValue(
      "Guardado desde primera pestaña",
    );
    await expect(second.getByLabel(/Descripción/)).toHaveValue(
      original.description,
    );
    expect(attempts).toHaveLength(1);
    await second
      .getByLabel(/Nombre del proyecto/)
      .fill("Guardado después de revisar");
    const retryResponse = second.waitForResponse(
      (response) => response.request().method() === "PUT",
    );
    await second
      .getByRole("button", { name: "Guardar cambios", exact: true })
      .click();
    const retry = await retryResponse;
    expect(retry.status()).toBe(200);
    expect(retry.request().headers()["if-match"]).toBe(accepted.headers().etag);
    await expect(second.getByRole("status")).toContainText(
      "Proyecto actualizado",
    );
    expect(stored(original.id).project.name).toBe(
      "Guardado después de revisar",
    );
    expect(attempts).toHaveLength(2);
  } finally {
    await second.close();
  }
});

test("edit_project: failed saves preserve drafts and loss of access clears them @s18 @s20 @s24", async ({
  page,
  request,
}) => {
  const project = await create(request, "Datos privados de prueba");
  const before = stored(project.id);
  const api = `**/api/v1/projects/${project.id}`;
  for (const status of [400, 503, 500, 0, 401, 404]) {
    if (status === 404)
      await loginSession(request, {
        username: "e2e-user",
        password: "e2e-only-password",
      });
    await page.goto(`/proyectos/${project.id}/editar`);
    await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue(
      project.name,
    );
    await page.getByLabel(/Nombre del proyecto/).fill("Borrador sin confirmar");
    await page
      .getByLabel(/Descripción/)
      .fill("Conservar contenido hasta decidir");
    if (status === 401)
      sql("DELETE FROM spring_session WHERE principal_name='e2e-user'");
    await page.route(api, (route) => {
      if (route.request().method() !== "PUT") return route.continue();
      if (status === 0) return route.abort("failed");
      return route.fulfill({
        status,
        contentType: "application/problem+json",
        body: JSON.stringify({
          status,
          code: status === 400 ? "VALIDATION_ERROR" : "TEST_FAILURE",
          title: "Solicitud no confirmada",
          errors:
            status === 400
              ? [{ field: "name", message: "Revisa el nombre" }]
              : [],
        }),
      });
    });
    await page
      .getByRole("button", { name: "Guardar cambios", exact: true })
      .click();
    if (status !== 401) await expect(page.getByRole("alert")).toBeVisible();
    await expect(
      page.getByText("Proyecto actualizado", { exact: true }),
    ).toHaveCount(0);
    if (status === 401 || status === 404) {
      if (status === 401)
        await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
      else
        await expect(
          page.getByRole("heading", {
            name: "Proyecto no encontrado",
            exact: true,
          }),
        ).toBeVisible();
      await expect(page.getByLabel(/Nombre del proyecto/)).toHaveCount(0);
      await expect(
        page.getByText("Borrador sin confirmar", { exact: true }),
      ).toHaveCount(0);
    } else {
      await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue(
        "Borrador sin confirmar",
      );
      await expect(page.getByLabel(/Descripción/)).toHaveValue(
        "Conservar contenido hasta decidir",
      );
      await expect(
        page.getByRole("button", { name: "Guardar cambios", exact: true }),
      ).toBeEnabled();
      if (status === 400)
        await expect(page.getByLabel(/Nombre del proyecto/)).toHaveAttribute(
          "aria-invalid",
          "true",
        );
    }
    expect(stored(project.id)).toEqual(before);
    await page.unroute(api);
  }
  await page.goto(`/proyectos/${project.id}/editar`);
  await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue(
    project.name,
  );
  await page
    .getByLabel(/Nombre del proyecto/)
    .fill("Recuperado con respuesta real");
  await page
    .getByRole("button", { name: "Guardar cambios", exact: true })
    .click();
  await expect(page.getByRole("status")).toHaveText("Proyecto actualizado");
  expect(stored(project.id).project.name).toBe("Recuperado con respuesta real");
});

test("edit_project: accessible form reflows across widths and breakpoint edges and supports keyboard and touch @s22", async ({
  page,
  request,
  browser,
}, testInfo) => {
  test.setTimeout(90_000);
  const project = await create(
    request,
    "Proyecto " + "W".repeat(111),
    "W".repeat(4000),
  );
  for (const width of [
    320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099,
    1101, 1280, 1440, 1599, 1601, 1920, 2560,
  ]) {
    await test.step(`${width} CSS pixels`, async () => {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      await page.goto(`/proyectos/${project.id}/editar`);
      await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue(
        project.name,
      );
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      for (const control of await page
        .locator("main input, main textarea, main button, main a")
        .all()) {
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
          path: testInfo.outputPath(`edit-${width}.png`),
          fullPage: true,
        });
    });
  }
  await page.setViewportSize({ width: 320, height: 700 });
  await page.goto(`/proyectos/${project.id}/editar`);
  const name = page.getByLabel(/Nombre del proyecto/);
  await expect(name).toHaveValue(project.name);
  for (
    let index = 0;
    index < 12 &&
    !(await name.evaluate((element) => element === document.activeElement));
    index++
  )
    await page.keyboard.press("Tab");
  await expect(name).toBeFocused();
  expect(
    await name.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await page.keyboard.press("ControlOrMeta+A");
  await page.keyboard.type("Editado con teclado");
  await page.keyboard.press("Tab");
  await expect(page.getByLabel(/Descripción/)).toBeFocused();
  await page.keyboard.press("Tab");
  const save = page.getByRole("button", {
    name: "Guardar cambios",
    exact: true,
  });
  await expect(save).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toHaveText("Proyecto actualizado");
  await expect(save).toBeFocused();
  const touchContext = await browser.newContext({
    baseURL: testInfo.project.use.baseURL,
    viewport: { width: 390, height: 844 },
    hasTouch: true,
  });
  try {
    await loginSession(touchContext.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    const touch = await touchContext.newPage();
    await touch.goto(`/proyectos/${project.id}/editar`);
    await expect(touch.getByLabel(/Nombre del proyecto/)).toHaveValue(
      "Editado con teclado",
    );
    expect(
      await touch.evaluate(() => navigator.maxTouchPoints),
    ).toBeGreaterThan(0);
    await touch.getByRole("link", { name: "Cancelar", exact: true }).tap();
    await expect(
      touch.getByRole("heading", { name: "Editado con teclado", exact: true }),
    ).toBeVisible();
  } finally {
    await touchContext.close();
  }
});
