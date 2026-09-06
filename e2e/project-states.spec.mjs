import { loginSession, csrfHeaders } from "../scripts/session-client.mjs";
import { test, expect } from "./support/authenticated-test.mjs";
import { sql, create, stored } from "./support/projects.mjs";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(() => sql("TRUNCATE planned_blocks, task_status_history, tasks, outbox_events, projects"));

async function changeStatus(request, id, status, etag) {
  const tag =
    etag ?? (await request.get(`/api/v1/projects/${id}`)).headers().etag;
  return request.put(`/api/v1/projects/${id}/status`, {
    headers: { ...(await csrfHeaders(request)), "If-Match": tag },
    data: { status },
  });
}

test("project_states: explicit transitions persist and remain readable and editable @s1 @s3 @s5 @s11 @s14 @s15", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de prueba de estados");
  await page.goto(`/proyectos/${project.id}`);
  await expect(page.getByText("Idea", { exact: true })).toBeVisible();
  for (const [action, status, label] of [
    ["Activar", "active", "Activo"],
    ["Pausar", "paused", "Pausado"],
    ["Retomar", "active", "Activo"],
    ["Marcar terminado", "completed", "Terminado"],
    ["Reabrir en pausa", "paused", "Pausado"],
  ]) {
    const before = stored(project.id);
    const responsePromise = page.waitForResponse(
      (response) =>
        response.request().method() === "PUT" &&
        response.url().endsWith("/status"),
    );
    if (action === "Activar") {
      await page.evaluate(() => {
        document.querySelector(".status-actions").addEventListener(
          "click",
          () => {
            window.stateStartedAt = performance.now();
          },
          { capture: true, once: true },
        );
        new MutationObserver(() => {
          if (
            document.querySelector('[role="status"]')?.textContent ===
              "Cambiando estado" &&
            window.stateFeedbackMs === undefined
          )
            window.stateFeedbackMs = performance.now() - window.stateStartedAt;
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
      await page.route(
        `**/api/v1/projects/${project.id}/status`,
        async (route) => {
          attempts++;
          const actual = await route.fetch();
          await gate;
          await route.fulfill({ response: actual });
        },
      );
      try {
        await page
          .getByRole("button", { name: action, exact: true })
          .dblclick();
        await expect(
          page
            .getByRole("region", { name: "Estado del proyecto", exact: true })
            .getByRole("status"),
        ).toHaveText("Cambiando estado");
        await expect(page.getByText("Idea", { exact: true })).toBeVisible();
        for (const button of await page
          .getByRole("region", { name: "Estado del proyecto" })
          .getByRole("button")
          .all())
          await expect(button).toBeDisabled();
        const feedback = await page.evaluate(() => window.stateFeedbackMs);
        expect(feedback).toBeGreaterThanOrEqual(0);
        expect(feedback).toBeLessThan(400);
        console.info(`State feedback: ${Math.round(feedback)} ms`);
      } finally {
        release();
      }
      await responsePromise;
      expect(attempts).toBe(1);
      await page.unroute(`**/api/v1/projects/${project.id}/status`);
    } else
      await page.getByRole("button", { name: action, exact: true }).click();
    const response = await responsePromise;
    expect(response.status()).toBe(200);
    expect(response.headers()["cache-control"]).toContain("no-store");
    const confirmed = await response.json();
    expect(Object.keys(confirmed)).toHaveLength(7);
    expect(confirmed).toMatchObject({
      id: project.id,
      ownerId: project.ownerId,
      name: project.name,
      description: project.description,
      createdAt: project.createdAt,
      status,
    });
    await expect(
      page
        .getByRole("region", { name: "Estado del proyecto", exact: true })
        .getByRole("status"),
    ).toHaveText("Estado actualizado");
    await expect(page.getByText(label, { exact: true })).toBeVisible();
    const after = stored(project.id);
    expect(after.project.version).toBe(before.project.version + 1);
    const previousIds = new Set(before.events.map((event) => event.event_id));
    const added = after.events.filter(
      (event) => !previousIds.has(event.event_id),
    );
    expect(added).toHaveLength(1);
    expect(added[0]).toMatchObject({
      event_type: "ProjectStatusChanged.v1",
      status: "pending",
      owner_id: project.ownerId,
      aggregate_id: project.id,
    });
    expect(added[0].payload).toEqual({
      eventId: added[0].event_id,
      aggregateId: project.id,
      ownerId: project.ownerId,
      occurredAt: confirmed.updatedAt,
      schemaVersion: 1,
      type: "ProjectStatusChanged.v1",
      fromStatus: before.project.status,
      toStatus: status,
    });
    await page.goto("/proyectos");
    await expect(
      page.getByRole("link", { name: project.name, exact: true }),
    ).toBeVisible();
    await expect(page.getByText(label, { exact: true })).toBeVisible();
    await page.getByRole("link", { name: project.name, exact: true }).click();
    await expect(page.getByText(label, { exact: true })).toBeVisible();
  }
  const beforeNoop = stored(project.id);
  expect((await changeStatus(request, project.id, "paused")).status()).toBe(
    200,
  );
  expect(stored(project.id)).toEqual(beforeNoop);
  await page
    .getByRole("link", { name: "Editar proyecto", exact: true })
    .click();
  await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue(
    project.name,
  );
  await page
    .getByLabel(/Descripción/)
    .fill("Texto editado mientras está pausado");
  await page
    .getByRole("button", { name: "Guardar cambios", exact: true })
    .click();
  await expect(page.getByRole("status")).toHaveText("Proyecto actualizado");
  expect(stored(project.id).project.status).toBe("paused");
  await page.getByRole("link", { name: "Cancelar", exact: true }).click();
  await expect(page.getByText("Pausado", { exact: true })).toBeVisible();
  await page.reload();
  await expect(
    page.getByText("Texto editado mientras está pausado", { exact: true }),
  ).toBeVisible();
});

test("project_states: concurrent requests cannot overfill the last active slot @s4 @s5 @s15", async ({
  page,
  request,
}) => {
  const projects = [];
  for (let index = 0; index < 4; index++)
    projects.push(await create(request, `Proyecto de capacidad ${index + 1}`));
  for (const project of projects.slice(0, 2))
    expect((await changeStatus(request, project.id, "active")).status()).toBe(
      200,
    );
  const candidates = projects.slice(2);
  const tags = await Promise.all(
    candidates.map(
      async (project) =>
        (await request.get(`/api/v1/projects/${project.id}`)).headers().etag,
    ),
  );
  const countBefore = Number(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='ProjectStatusChanged.v1'",
    ),
  );
  const responses = await Promise.all(
    candidates.map((project, index) =>
      changeStatus(request, project.id, "active", tags[index]),
    ),
  );
  expect(responses.map((response) => response.status()).sort()).toEqual([
    200, 409,
  ]);
  const loser =
    candidates[responses.findIndex((response) => response.status() === 409)];
  const winner =
    candidates[responses.findIndex((response) => response.status() === 200)];
  const rejection = await responses
    .find((response) => response.status() === 409)
    .json();
  expect(rejection).toMatchObject({
    code: "ACTIVE_PROJECT_LIMIT",
    activeCount: 3,
    limit: 3,
  });
  expect(
    Number(
      sql(
        "SELECT count(*) FROM projects WHERE owner_id='e2e-user' AND status='active'",
      ),
    ),
  ).toBe(3);
  expect(
    Number(
      sql(
        "SELECT count(*) FROM outbox_events WHERE event_type='ProjectStatusChanged.v1'",
      ),
    ),
  ).toBe(countBefore + 1);
  expect(stored(loser.id).project.status).toBe("idea");
  const beforeRejectedClick = sql(
    "SELECT json_agg(row_to_json(p) ORDER BY id) FROM projects p",
  );
  await page.goto(`/proyectos/${loser.id}`);
  await page.getByRole("button", { name: "Activar", exact: true }).click();
  await expect(page.getByRole("alert")).toContainText("3");
  await expect(
    page.getByRole("link", { name: "Elegir qué pausar", exact: true }),
  ).toBeVisible();
  expect(
    sql("SELECT json_agg(row_to_json(p) ORDER BY id) FROM projects p"),
  ).toBe(beforeRejectedClick);
  await page
    .getByRole("link", { name: "Elegir qué pausar", exact: true })
    .click();
  await page.getByRole("link", { name: winner.name, exact: true }).click();
  await page.getByRole("button", { name: "Pausar", exact: true }).click();
  await expect(
    page
      .getByRole("region", { name: "Estado del proyecto", exact: true })
      .getByRole("status"),
  ).toHaveText("Estado actualizado");
  await page.goto(`/proyectos/${loser.id}`);
  await page.getByRole("button", { name: "Activar", exact: true }).click();
  await expect(
    page
      .getByRole("region", { name: "Estado del proyecto", exact: true })
      .getByRole("status"),
  ).toHaveText("Estado actualizado");
  expect(stored(loser.id).project.status).toBe("active");
  expect(stored(winner.id).project.status).toBe("paused");
  expect(
    Number(
      sql(
        "SELECT count(*) FROM projects WHERE owner_id='e2e-user' AND status='active'",
      ),
    ),
  ).toBe(3);
});

test("project_states: text and status share a version and require deliberate conflict recovery @s9 @s15", async ({
  page,
  context,
  request,
}) => {
  const project = await create(request, "Proyecto con dos pestañas");
  const editor = await context.newPage();
  try {
    await page.goto(`/proyectos/${project.id}`);
    await expect(
      page.getByRole("button", { name: "Activar", exact: true }),
    ).toBeVisible();
    await editor.goto(`/proyectos/${project.id}/editar`);
    await expect(editor.getByLabel(/Nombre del proyecto/)).toHaveValue(
      project.name,
    );
    await editor.getByLabel(/Nombre del proyecto/).fill("Mi borrador de texto");
    await page.getByRole("button", { name: "Activar", exact: true }).click();
    await expect(
      page
        .getByRole("region", { name: "Estado del proyecto", exact: true })
        .getByRole("status"),
    ).toHaveText("Estado actualizado");
    const active = stored(project.id);
    const conflict = editor.waitForResponse(
      (response) => response.request().method() === "PUT",
    );
    await editor
      .getByRole("button", { name: "Guardar cambios", exact: true })
      .click();
    expect((await conflict).status()).toBe(412);
    await expect(editor.getByRole("alert")).toBeVisible();
    await expect(editor.getByLabel(/Nombre del proyecto/)).toHaveValue(
      "Mi borrador de texto",
    );
    expect(stored(project.id)).toEqual(active);
    await editor
      .getByRole("button", { name: "Recargar versión guardada", exact: true })
      .click();
    await expect(editor.getByLabel(/Nombre del proyecto/)).toHaveValue(
      project.name,
    );
    await editor
      .getByLabel(/Nombre del proyecto/)
      .fill("Texto confirmado tras revisar");
    await editor
      .getByRole("button", { name: "Guardar cambios", exact: true })
      .click();
    await expect(editor.getByRole("status")).toHaveText("Proyecto actualizado");
    const edited = stored(project.id);
    const staleStatus = page.waitForResponse(
      (response) => response.request().method() === "PUT",
    );
    await page.getByRole("button", { name: "Pausar", exact: true }).click();
    expect((await staleStatus).status()).toBe(412);
    await expect(page.getByRole("alert")).toBeVisible();
    expect(stored(project.id)).toEqual(edited);
    await page
      .getByRole("button", { name: "Recargar versión guardada", exact: true })
      .click();
    await expect(
      page.getByRole("heading", {
        name: "Texto confirmado tras revisar",
        exact: true,
      }),
    ).toBeVisible();
    await page.getByRole("button", { name: "Pausar", exact: true }).click();
    await expect(
      page
        .getByRole("region", { name: "Estado del proyecto", exact: true })
        .getByRole("status"),
    ).toHaveText("Estado actualizado");
    expect(stored(project.id).project).toMatchObject({
      status: "paused",
      name: "Texto confirmado tras revisar",
    });
  } finally {
    await editor.close();
  }
});

test("project_states: controls reflow at breakpoint edges and retain keyboard and touch access @s16", async ({
  page,
  request,
  browser,
}, testInfo) => {
  test.setTimeout(120_000);
  const project = await create(
    request,
    "Proyecto " + "W".repeat(111),
    "W".repeat(4000),
  );
  expect((await changeStatus(request, project.id, "active")).status()).toBe(
    200,
  );
  for (const width of [
    320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099,
    1101, 1280, 1440, 1599, 1601, 1920, 2560,
  ]) {
    await test.step(`${width} CSS pixels`, async () => {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      await page.goto(`/proyectos/${project.id}`);
      await expect(page.getByText("Activo", { exact: true })).toBeVisible();
      const control = page.getByRole("region", { name: "Estado del proyecto" });
      await expect(control).toBeVisible();
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      for (const button of await control.getByRole("button").all()) {
        const box = await button.boundingBox();
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
    });
  }
  await page.setViewportSize({ width: 320, height: 700 });
  await page.goto(`/proyectos/${project.id}`);
  const pause = page.getByRole("button", { name: "Pausar", exact: true });
  await expect(pause).toBeVisible();
  for (
    let index = 0;
    index < 15 &&
    !(await pause.evaluate((element) => element === document.activeElement));
    index++
  )
    await page.keyboard.press("Tab");
  await expect(pause).toBeFocused();
  expect(
    await pause.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await page.keyboard.press("Enter");
  await expect(
    page
      .getByRole("region", { name: "Estado del proyecto", exact: true })
      .getByRole("status"),
  ).toHaveText("Estado actualizado");
  await expect(
    page.getByRole("heading", { name: "Estado del proyecto", exact: true }),
  ).toBeFocused();
  for (const [action, label] of [
    ["Marcar terminado", "Terminado"],
    ["Reabrir en pausa", "Pausado"],
  ]) {
    await page.getByRole("button", { name: action, exact: true }).click();
    await expect(page.getByText(label, { exact: true })).toBeVisible();
    expect(
      (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations,
    ).toEqual([]);
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
    ).toBe(true);
  }
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
    await touch.goto(`/proyectos/${project.id}`);
    await touch.getByRole("button", { name: "Retomar", exact: true }).tap();
    await expect(touch.getByText("Activo", { exact: true })).toBeVisible();
    expect(
      await touch.evaluate(() => navigator.maxTouchPoints),
    ).toBeGreaterThan(0);
  } finally {
    await touchContext.close();
  }
});
