import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql, stored } from "./support/projects.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";

const fields = [
  "id",
  "projectId",
  "title",
  "completionCriterion",
  "estimatedMinutes",
  "status",
  "createdAt",
  "updatedAt",
].sort();
test.beforeEach(() => sql("TRUNCATE tasks,outbox_events,projects"));
async function createTask(request, projectId, title, extra = {}) {
  const response = await request.post(`/api/v1/projects/${projectId}/tasks`, {
    headers: await csrfHeaders(request),
    data: { title, ...extra },
  });
  expect(response.status()).toBe(201);
  const task = await response.json();
  expect(Object.keys(task).sort()).toEqual(fields);
  expect(response.headers().location).toBe(
    `/api/v1/projects/${projectId}/tasks/${task.id}`,
  );
  return task;
}

test("create_task: confirmed task survives reload without changing project or exposing private event fields @s1 @s7 @s13 @s17 @s23 @s26 @s35", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto con tareas de prueba");
  const before = stored(project.id).project;
  const projectResponse = await request.get(`/api/v1/projects/${project.id}`);
  const etag = projectResponse.headers().etag;
  await page.goto(`/proyectos/${project.id}`);
  const region = page.getByRole("region", { name: "Tareas", exact: true });
  const title = "Preparar portada 🚀 <script>literal</script>";
  const criterion =
    "Primera sección terminada.\nCriterio privado: conservar espacios  y saltos.";
  await region.getByLabel("Título de la tarea", { exact: true }).fill(title);
  await region
    .getByLabel("Criterio de finalización", { exact: true })
    .fill(criterion);
  await region.getByLabel("Estimación en minutos", { exact: true }).fill("25");
  const confirmed = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/api/v1/projects/${project.id}/tasks`) &&
      response.request().method() === "POST",
  );
  await region
    .getByRole("button", { name: "Crear tarea", exact: true })
    .click();
  const response = await confirmed;
  expect(response.status()).toBe(201);
  const task = await response.json();
  expect(Object.keys(task).sort()).toEqual(fields);
  expect(task).toMatchObject({
    projectId: project.id,
    title,
    completionCriterion: criterion,
    estimatedMinutes: 25,
    status: "pending",
  });
  expect(task.createdAt).toBe(task.updatedAt);
  await expect(
    region.getByRole("status").filter({ hasText: /^Tarea guardada$/ }),
  ).toBeVisible();
  await expect(
    region
      .getByRole("list", { name: "Tareas guardadas" })
      .getByText(title, { exact: true }),
  ).toBeVisible();
  await expect(
    region.getByText("Estimación: 25 min", { exact: true }),
  ).toBeVisible();
  await expect(
    region.getByText("La estimación no es tiempo trabajado"),
  ).toBeVisible();
  expect(stored(project.id).project).toEqual(before);
  expect(
    (await request.get(`/api/v1/projects/${project.id}`)).headers().etag,
  ).toBe(etag);
  const event = stored(project.id).events.find(
    (event) => event.event_type === "TaskCreated.v1",
  );
  expect(Object.keys(event.payload).sort()).toEqual(
    [
      "eventId",
      "aggregateId",
      "ownerId",
      "occurredAt",
      "schemaVersion",
      "type",
      "taskId",
      "title",
    ].sort(),
  );
  expect(event.payload).toMatchObject({
    aggregateId: project.id,
    taskId: task.id,
    title,
    type: "TaskCreated.v1",
    schemaVersion: 1,
    ownerId: "e2e-user",
  });
  const detail = await request.get(response.headers().location);
  expect(detail.status()).toBe(200);
  expect(await detail.json()).toEqual(task);
  expect(detail.headers()["cache-control"]).toContain("no-store");
  await page.reload();
  await expect(
    region
      .getByRole("list", { name: "Tareas guardadas" })
      .getByText(title, { exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM tasks")).toBe("1");
});

test("create_task: completing a project preserves pending tasks and requires deliberate reopening @s13 @s30", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto con trabajo pendiente");
  const title = "Tarea que sigue pendiente";
  const task = await createTask(request, project.id, title);
  await page.goto(`/proyectos/${project.id}`);
  const region = page.getByRole("region", { name: "Tareas", exact: true });
  await expect(region.getByText(title, { exact: true })).toBeVisible();
  await page
    .getByRole("button", { name: "Marcar terminado", exact: true })
    .click();
  await expect(page.getByText("Terminado", { exact: true })).toBeVisible();
  await expect(
    region.getByRole("button", { name: "Crear tarea", exact: true }),
  ).toHaveCount(0);
  await expect(
    region
      .getByRole("list", { name: "Tareas guardadas" })
      .getByText(title, { exact: true }),
  ).toBeVisible();
  expect(
    (
      await request.get(`/api/v1/projects/${project.id}/tasks/${task.id}`)
    ).status(),
  ).toBe(200);
  expect(sql("SELECT status FROM tasks")).toBe("pending");
  await page
    .getByRole("button", { name: "Reabrir en pausa", exact: true })
    .click();
  await expect(
    region.getByRole("button", { name: "Crear tarea", exact: true }),
  ).toBeVisible();
});

test("create_task: 21 persisted tasks paginate without repeats after a newer task @s19 @s20 @s21 @s22 @s24 @s35", async ({
  page,
  request,
}) => {
  const project = await create(request, "Paginación de tareas");
  const path = `/api/v1/projects/${project.id}/tasks`;
  const empty = await request.get(path);
  expect(await empty.json()).toEqual({ items: [], nextCursor: null });
  for (let index = 0; index < 21; index++)
    await createTask(request, project.id, `Tarea ${index}`);
  sql(
    "UPDATE tasks SET created_at='2026-09-05T12:00:00Z',updated_at='2026-09-05T12:00:00Z'",
  );
  const expected = sql(
    "SELECT title FROM tasks ORDER BY created_at DESC,id DESC",
  ).split(/\r?\n/);
  const firstResponse = await request.get(path);
  expect(firstResponse.headers()["cache-control"]).toContain("no-store");
  const first = await firstResponse.json();
  expect(first.items.map((item) => item.title)).toEqual(expected.slice(0, 20));
  expect(first.nextCursor).toBeTruthy();
  await page.goto(`/proyectos/${project.id}`);
  const region = page.getByRole("region", { name: "Tareas", exact: true });
  const items = region
    .getByRole("list", { name: "Tareas guardadas" })
    .getByRole("listitem");
  await expect(items).toHaveCount(20);
  await page.reload();
  await expect(items).toHaveCount(20);
  for (let index = 0; index < 20; index++)
    await expect(items.nth(index)).toContainText(expected[index]);
  await createTask(request, project.id, "Tarea más reciente");
  const continuation = page.waitForResponse(
    (response) =>
      response.url().includes(`${path}?`) &&
      response.request().method() === "GET",
  );
  await region
    .getByRole("button", { name: "Más tareas antiguas", exact: true })
    .or(region.getByRole("link", { name: "Más tareas antiguas", exact: true }))
    .click();
  const next = await continuation;
  expect(next.status()).toBe(200);
  const rest = await next.json();
  expect(rest.items.map((item) => item.title)).toEqual(expected.slice(20));
  expect(rest.nextCursor).toBeNull();
  await expect(items).toHaveCount(1);
  await expect(items.first()).toContainText(expected[20]);
  await region
    .getByRole("button", { name: "Volver a tareas recientes", exact: true })
    .or(
      region.getByRole("link", {
        name: "Volver a tareas recientes",
        exact: true,
      }),
    )
    .click();
  await expect(items).toHaveCount(20);
  await expect(items.first()).toContainText("Tarea más reciente");
  expect(sql("SELECT count(*) FROM tasks")).toBe("22");
  const other = await create(request, "Otro proyecto");
  const foreignCursor = await request.get(
    `/api/v1/projects/${other.id}/tasks?cursor=${first.nextCursor}`,
  );
  expect(foreignCursor.status()).toBe(400);
  expect((await foreignCursor.json()).code).toBe("VALIDATION_ERROR");
});

test("create_task: private resources stay indistinguishable and errors preserve a deliberate draft @s9 @s10 @s11 @s24 @s27 @s29 @s32", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto privado de tareas");
  const foreign = await create(request, "Proyecto ajeno de prueba");
  const task = await createTask(request, foreign.id, "Tarea privada ajena");
  sql(`UPDATE projects SET owner_id='other-owner' WHERE id='${foreign.id}'`);
  const missing = "00000000-0000-0000-0000-000000000000";
  const absent = await request.get(`/api/v1/projects/${missing}/tasks`);
  expect(absent.status()).toBe(404);
  const problem = await absent.json();
  for (const path of [
    `/api/v1/projects/${foreign.id}/tasks`,
    `/api/v1/projects/${foreign.id}/tasks/${task.id}`,
    `/api/v1/projects/${project.id}/tasks/${task.id}`,
    `/api/v1/projects/${project.id}/tasks/${missing}`,
  ]) {
    const response = await request.get(path);
    expect(response.status()).toBe(404);
    expect(await response.json()).toEqual(problem);
    expect(response.headers()["cache-control"]).toContain("no-store");
  }
  for (const projectId of [foreign.id, missing]) {
    const response = await request.post(`/api/v1/projects/${projectId}/tasks`, {
      headers: await csrfHeaders(request),
      data: { title: "No debe guardarse" },
    });
    expect(response.status()).toBe(404);
    expect(await response.json()).toEqual(problem);
  }
  const path = `**/api/v1/projects/${project.id}/tasks`;
  await page.route(path, (route) =>
    route.request().method() === "GET"
      ? route.fulfill({
          status: 503,
          contentType: "application/problem+json",
          body: JSON.stringify({
            status: 503,
            code: "STORAGE_UNAVAILABLE",
            title: "No disponible",
          }),
        })
      : route.continue(),
  );
  await page.goto(`/proyectos/${project.id}`);
  const region = page.getByRole("region", { name: "Tareas", exact: true });
  await expect(region.getByRole("alert")).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Activar", exact: true }),
  ).toBeVisible();
  await page.unroute(path);
  await region
    .getByRole("button", { name: "Reintentar tareas", exact: true })
    .click();
  await expect(region.getByRole("alert")).toHaveCount(0);
  for (const status of [400, 503, 0]) {
    await region
      .getByLabel("Título de la tarea", { exact: true })
      .fill("Borrador de tarea sin confirmar");
    await region
      .getByLabel("Criterio de finalización", { exact: true })
      .fill("Criterio conservado");
    await region
      .getByLabel("Estimación en minutos", { exact: true })
      .fill("15");
    let attempts = 0;
    await page.route(path, (route) => {
      if (route.request().method() !== "POST") return route.continue();
      attempts++;
      if (status === 0) return route.abort("failed");
      return route.fulfill({
        status,
        contentType: "application/problem+json",
        body: JSON.stringify({
          status,
          code: status === 400 ? "VALIDATION_ERROR" : "STORAGE_UNAVAILABLE",
          title: "No se confirmó la tarea",
          errors:
            status === 400
              ? [
                  {
                    field: "title",
                    code: "REQUIRED",
                    message: "Revisa el título",
                  },
                ]
              : [],
        }),
      });
    });
    await region
      .getByRole("button", { name: "Crear tarea", exact: true })
      .click();
    await expect(region.getByRole("alert").first()).toBeVisible();
    await expect(
      region.getByLabel("Título de la tarea", { exact: true }),
    ).toHaveValue("Borrador de tarea sin confirmar");
    await expect(
      region.getByLabel("Criterio de finalización", { exact: true }),
    ).toHaveValue("Criterio conservado");
    await expect(
      region.getByLabel("Estimación en minutos", { exact: true }),
    ).toHaveValue("15");
    expect(attempts).toBe(1);
    expect(
      sql(`SELECT count(*) FROM tasks WHERE project_id='${project.id}'`),
    ).toBe("0");
    await page.unroute(path);
  }
  sql("DELETE FROM spring_session WHERE principal_name='e2e-user'");
  await region
    .getByRole("button", { name: "Crear tarea", exact: true })
    .click();
  await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
  await expect(region).toHaveCount(0);
  expect(
    sql(`SELECT count(*) FROM tasks WHERE project_id='${project.id}'`),
  ).toBe("0");
});

test("create_task: retry completed during pending save still refreshes confirmed tasks @s26 @s27 @s29", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de concurrencia de lectura");
  const path = `/api/v1/projects/${project.id}/tasks`;
  let reads = 0;
  let posts = 0;
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  await page.route(`**${path}`, async (route) => {
    if (route.request().method() === "POST") {
      posts++;
      await held;
      return route.continue();
    }
    if (route.request().method() === "GET" && ++reads === 1)
      return route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({ status: 503, code: "STORAGE_UNAVAILABLE" }),
      });
    return route.continue();
  });
  await page.goto(`/proyectos/${project.id}`);
  const region = page.getByRole("region", { name: "Tareas", exact: true });
  await expect(region.getByRole("alert")).toHaveText(
    "No hemos podido cargar las tareas.",
  );
  await region
    .getByLabel("Título de la tarea", { exact: true })
    .fill("Tarea tras reintento concurrente");
  await region
    .getByRole("button", { name: "Crear tarea", exact: true })
    .click();
  try {
    await expect.poll(() => posts).toBe(1);
    await expect(region.getByRole("status")).toHaveText("Guardando tarea");
    const retried = page.waitForResponse(
      (response) =>
        response.url().endsWith(path) && response.request().method() === "GET",
    );
    await region
      .getByRole("button", { name: "Reintentar tareas", exact: true })
      .click();
    const empty = await retried;
    expect(empty.status()).toBe(200);
    expect(await empty.json()).toEqual({ items: [], nextCursor: null });
    await expect(
      region.getByText("Todavía no hay tareas en este proyecto.", {
        exact: true,
      }),
    ).toBeVisible();
    expect(reads).toBe(2);
    expect(sql("SELECT count(*) FROM tasks")).toBe("0");
  } finally {
    release();
  }
  await expect(
    region.getByRole("status").filter({ hasText: /^Tarea guardada$/ }),
  ).toBeVisible();
  await expect(
    region
      .getByRole("list", { name: "Tareas guardadas" })
      .getByText("Tarea tras reintento concurrente", { exact: true }),
  ).toBeVisible();
  await expect(
    region.getByText("Cargando tareas", { exact: true }),
  ).toHaveCount(0);
  expect(reads).toBe(3);
  expect(posts).toBe(1);
  expect(sql("SELECT count(*) FROM tasks")).toBe("1");
});

test("create_task: long tasks and recoverable form remain accessible at breakpoint edges @s28 @s33 @s34", async ({
  page,
  request,
}) => {
  test.setTimeout(120000);
  const project = await create(request, "Proyecto para revisar tareas");
  const title = "W".repeat(160);
  await createTask(request, project.id, title, {
    completionCriterion: "W".repeat(2000),
    estimatedMinutes: 1440,
  });
  await page.goto(`/proyectos/${project.id}`);
  const region = page.getByRole("region", { name: "Tareas", exact: true });
  await expect(
    region
      .getByRole("list", { name: "Tareas guardadas" })
      .getByText(title, { exact: true }),
  ).toBeVisible();
  const input = region.getByLabel("Título de la tarea", { exact: true });
  const submit = region.getByRole("button", {
    name: "Crear tarea",
    exact: true,
  });
  await submit.click();
  await expect(input).toBeFocused();
  await expect(input).toHaveAttribute("aria-invalid", "true");
  for (const width of [
    320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099,
    1101, 1280, 1440, 1599, 1601, 1920, 2560,
  ]) {
    await test.step(`${width} CSS pixels`, async () => {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      for (const control of await region
        .locator("input,textarea,button")
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
    });
  }
  await page.setViewportSize({ width: 320, height: 700 });
  await input.fill("Tarea por teclado");
  await page.keyboard.press("Tab");
  await expect(
    region.getByLabel("Criterio de finalización", { exact: true }),
  ).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(
    region.getByLabel("Estimación en minutos", { exact: true }),
  ).toBeFocused();
  const estimate = region.getByLabel("Estimación en minutos", { exact: true });
  let invalidPosts = 0;
  const observeInvalid = (request) => {
    if (
      request.method() === "POST" &&
      request.url().endsWith(`/api/v1/projects/${project.id}/tasks`)
    )
      invalidPosts++;
  };
  page.on("request", observeInvalid);
  await page.keyboard.type("1e");
  expect(await estimate.evaluate((element) => element.validity.badInput)).toBe(
    true,
  );
  await page.keyboard.press("Tab");
  await expect(submit).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(estimate).toBeFocused();
  expect(invalidPosts).toBe(0);
  expect(sql("SELECT count(*) FROM tasks")).toBe("1");
  page.off("request", observeInvalid);
  await estimate.fill("15");
  await page.keyboard.press("Tab");
  await expect(submit).toBeFocused();
  expect(
    await submit.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  let release;
  const waiting = new Promise((resolve) => {
    release = resolve;
  });
  let attempts = 0;
  await page.route(`**/api/v1/projects/${project.id}/tasks`, async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    attempts++;
    await waiting;
    return route.continue();
  });
  await region.evaluate((element) => {
    element.querySelector("form").addEventListener(
      "submit",
      () => {
        window.taskStartedAt = performance.now();
      },
      { capture: true, once: true },
    );
    const observer = new MutationObserver(() => {
      if (
        [...element.querySelectorAll('[role="status"]')].some(
          (status) => status.textContent === "Guardando tarea",
        )
      ) {
        window.taskFeedbackMs = performance.now() - window.taskStartedAt;
        observer.disconnect();
      }
    });
    observer.observe(element, {
      subtree: true,
      childList: true,
      characterData: true,
    });
  });
  await page.keyboard.press("Enter");
  await expect(submit).toBeDisabled();
  await expect(region.getByRole("status")).toContainText(/Guardando|Creando/);
  const feedbackMs = await page.evaluate(() => window.taskFeedbackMs);
  expect(feedbackMs).toBeGreaterThanOrEqual(0);
  expect(feedbackMs).toBeLessThan(400);
  console.info(`Task save feedback: ${Math.round(feedbackMs)} ms`);
  expect(attempts).toBe(1);
  release();
  await expect(
    region.getByRole("status").filter({ hasText: /^Tarea guardada$/ }),
  ).toBeVisible();
  await expect(
    region
      .getByRole("list", { name: "Tareas guardadas" })
      .getByText("Tarea por teclado", { exact: true }),
  ).toBeVisible();
  expect(attempts).toBe(1);
  expect(sql("SELECT count(*) FROM tasks")).toBe("2");
});
