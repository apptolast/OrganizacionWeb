import AxeBuilder from "@axe-core/playwright";
import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql, stored } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";

test.beforeEach(() =>
  sql("TRUNCATE task_status_history, tasks, outbox_events, projects"),
);
const path = (projectId, id) => `/api/v1/projects/${projectId}/tasks/${id}`;
async function state(request, projectId, id) {
  const response = await request.get(`${path(projectId, id)}/status`);
  expect(response.status()).toBe(200);
  expect(response.headers()["cache-control"]).toContain("no-store");
  const body = await response.json();
  expect(Object.keys(body).sort()).toEqual([
    "completedAt",
    "status",
    "updatedAt",
  ]);
  expect(response.headers().etag).toMatch(
    new RegExp(`^"task:${id}:(0|[1-9][0-9]*)"$`),
  );
  return { body, etag: response.headers().etag };
}
async function change(request, projectId, id, status, etag) {
  return request.put(`${path(projectId, id)}/status`, {
    headers: { ...(await csrfHeaders(request)), "If-Match": etag },
    data: { status },
  });
}
async function history(request, projectId, id, cursor) {
  const response = await request.get(
    `${path(projectId, id)}/history${cursor ? `?cursor=${encodeURIComponent(cursor)}` : ""}`,
  );
  expect(response.status()).toBe(200);
  const page = await response.json();
  expect(Object.keys(page).sort()).toEqual(["items", "nextCursor"]);
  for (const row of page.items)
    expect(Object.keys(row).sort()).toEqual([
      "fromStatus",
      "id",
      "occurredAt",
      "toStatus",
    ]);
  return page;
}

test("complete_reopen_task: confirmed completion and reopening retain history after reload and outbox cleanup @s1 @s2 @s3 @s4 @s9 @s11 @s21 @s22", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de historial de prueba");
  const task = await saveTask(request, project.id, "Resultado de prueba");
  const originalProject = stored(project.id).project;
  const initial = await state(request, project.id, task.id);
  expect(initial.body.status).toBe("pending");
  expect(initial.body.completedAt).toBeNull();
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  for (const [action, status] of [
    ["Completar tarea", "completed"],
    ["Reabrir tarea", "pending"],
    ["Completar tarea", "completed"],
  ]) {
    const responsePromise = page.waitForResponse(
      (response) =>
        response.url().endsWith(`${path(project.id, task.id)}/status`) &&
        response.request().method() === "PUT",
    );
    await page.getByRole("button", { name: action, exact: true }).click();
    const response = await responsePromise;
    expect(response.status()).toBe(200);
    const result = await response.json();
    expect(result.status).toBe(status);
    expect(result.completedAt).toBe(
      status === "completed" ? result.updatedAt : null,
    );
    await expect(
      page
        .getByRole("status")
        .filter({ hasText: /^Estado de tarea actualizado$/ }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", {
        name: status === "completed" ? "Reabrir tarea" : "Completar tarea",
        exact: true,
      }),
    ).toBeEnabled();
  }
  const confirmed = await state(request, project.id, task.id);
  const before = await history(request, project.id, task.id);
  expect(before.items.map((row) => [row.fromStatus, row.toStatus])).toEqual([
    ["pending", "completed"],
    ["completed", "pending"],
    ["pending", "completed"],
  ]);
  expect(new Set(before.items.map((row) => row.id)).size).toBe(3);
  expect(before.nextCursor).toBeNull();
  const noOp = await change(
    request,
    project.id,
    task.id,
    "completed",
    confirmed.etag,
  );
  expect(noOp.status()).toBe(200);
  expect(noOp.headers().etag).toBe(confirmed.etag);
  expect(await noOp.json()).toEqual(confirmed.body);
  expect(await history(request, project.id, task.id)).toEqual(before);
  const rows = JSON.parse(
    sql(
      `SELECT json_agg(row_to_json(e)) FROM outbox_events e WHERE event_type='TaskStatusChanged.v1' AND payload->>'taskId'='${task.id}'`,
    ),
  );
  expect(rows).toHaveLength(3);
  for (const row of rows) {
    expect(Object.keys(row.payload).sort()).toEqual([
      "aggregateId",
      "eventId",
      "fromStatus",
      "occurredAt",
      "ownerId",
      "schemaVersion",
      "taskId",
      "toStatus",
      "type",
    ]);
    expect(row.payload.aggregateId).toBe(project.id);
    expect(row.payload.taskId).toBe(task.id);
    expect(row.payload.schemaVersion).toBe(1);
    expect(row.status).toBe("pending");
  }
  sql(
    `DELETE FROM outbox_events WHERE event_type='TaskStatusChanged.v1' AND payload->>'taskId'='${task.id}'`,
  );
  expect(await history(request, project.id, task.id)).toEqual(before);
  expect(stored(project.id).project).toEqual(originalProject);
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Reabrir tarea", exact: true }),
  ).toBeEnabled();
  await expect(
    page.getByRole("heading", { name: "Historial de la tarea", exact: true }),
  ).toBeVisible();
  await expect(
    page
      .getByRole("list", { name: "Transiciones de la tarea", exact: true })
      .getByRole("listitem"),
  ).toHaveCount(3);
});

test("complete_reopen_task: twenty-one transitions keep their cursor and a concurrent writer wins only once @s5 @s12 @s13 @s16 @s29", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de concurrencia de prueba");
  const task = await saveTask(request, project.id, "Alternar un resultado");
  let current = await state(request, project.id, task.id);
  for (let index = 0; index < 21; index++) {
    const response = await change(
      request,
      project.id,
      task.id,
      index % 2 ? "pending" : "completed",
      current.etag,
    );
    expect(response.status()).toBe(200);
    current = { body: await response.json(), etag: response.headers().etag };
  }
  const first = await history(request, project.id, task.id);
  expect(first.items).toHaveLength(20);
  expect(first.nextCursor).toBeTruthy();
  const decoded = JSON.parse(
    Buffer.from(first.nextCursor, "base64url").toString("utf8"),
  );
  expect(Object.keys(decoded).sort()).toEqual([
    "projectId",
    "taskId",
    "taskVersion",
  ]);
  expect(decoded.projectId).toBe(project.id);
  expect(decoded.taskId).toBe(task.id);
  expect(decoded.taskVersion).toBe(2);
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const historyRegion = page.getByRole("region", {
    name: "Historial de la tarea",
    exact: true,
  });
  const visibleEntries = historyRegion
    .getByRole("list", { name: "Transiciones de la tarea", exact: true })
    .getByRole("listitem");
  await expect(visibleEntries).toHaveCount(20);
  const headers = { ...(await csrfHeaders(request)), "If-Match": current.etag };
  const attempts = await Promise.all(
    [1, 2].map(() =>
      request.put(`${path(project.id, task.id)}/status`, {
        headers,
        data: { status: "pending" },
      }),
    ),
  );
  expect(attempts.map((response) => response.status()).sort()).toEqual([
    200, 412,
  ]);
  const rejected = attempts.find((response) => response.status() === 412);
  expect((await rejected.json()).code).toBe("TASK_CONFLICT");
  const next = await history(request, project.id, task.id, first.nextCursor);
  expect(next.items).toHaveLength(1);
  expect(next.nextCursor).toBeNull();
  await historyRegion
    .getByRole("button", { name: "Más transiciones antiguas", exact: true })
    .click();
  await expect(visibleEntries).toHaveCount(1);
  await historyRegion
    .getByRole("button", { name: "Volver al historial reciente", exact: true })
    .click();
  await expect(visibleEntries).toHaveCount(20);
  expect(first.items.some((row) => row.id === next.items[0].id)).toBe(false);
  expect(
    sql(`SELECT count(*) FROM task_status_history WHERE task_id='${task.id}'`),
  ).toBe("22");
  expect(
    sql(
      `SELECT count(*) FROM outbox_events WHERE event_type='TaskStatusChanged.v1' AND payload->>'taskId'='${task.id}'`,
    ),
  ).toBe("22");
  const unchanged = await state(request, project.id, task.id);
  const staleNoOp = await change(
    request,
    project.id,
    task.id,
    "pending",
    current.etag,
  );
  expect(staleNoOp.status()).toBe(412);
  expect(await state(request, project.id, task.id)).toEqual(unchanged);
  const other = await saveTask(request, project.id, "Otra tarea");
  const foreignCursor = await request.get(
    `${path(project.id, other.id)}/history?cursor=${encodeURIComponent(first.nextCursor)}`,
  );
  expect(foreignCursor.status()).toBe(400);
  expect((await foreignCursor.json()).code).toBe("VALIDATION_ERROR");
});

test("complete_reopen_task: private state and history stay indistinguishable from missing resources @s14", async ({
  request,
}) => {
  const project = await create(request, "Proyecto privado de prueba");
  const task = await saveTask(
    request,
    project.id,
    "Contenido privado de prueba",
  );
  sql(`UPDATE projects SET owner_id='another-owner' WHERE id='${project.id}'`);
  const missing = "00000000-0000-4000-8000-000000000009";
  for (const route of ["status", "history"]) {
    const absent = await request.get(`${path(project.id, missing)}/${route}`);
    const hidden = await request.get(`${path(project.id, task.id)}/${route}`);
    expect(absent.status()).toBe(404);
    expect(hidden.status()).toBe(404);
    expect(await hidden.json()).toEqual(await absent.json());
  }
  const hiddenWrite = await change(
    request,
    project.id,
    task.id,
    "completed",
    `"task:${task.id}:0"`,
  );
  const absentWrite = await change(
    request,
    project.id,
    missing,
    "completed",
    `"task:${missing}:0"`,
  );
  expect(hiddenWrite.status()).toBe(404);
  expect(absentWrite.status()).toBe(404);
  expect(await hiddenWrite.json()).toEqual(await absentWrite.json());
  expect(sql("SELECT count(*) FROM task_status_history")).toBe("0");
});

test("complete_reopen_task: conflict recovery and history errors never repeat a confirmed transition @s23 @s25", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de recuperación de prueba");
  const task = await saveTask(request, project.id, "Decisión conservada");
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const control = page.getByRole("region", {
    name: "Estado de la tarea",
    exact: true,
  });
  await expect(
    control.getByRole("button", { name: "Completar tarea", exact: true }),
  ).toBeEnabled();
  await expect(
    page.getByText("Todavía no hay cambios de estado.", { exact: true }),
  ).toBeVisible();
  const original = await state(request, project.id, task.id);
  expect(
    (
      await change(request, project.id, task.id, "completed", original.etag)
    ).status(),
  ).toBe(200);
  let writes = 0;
  page.on("request", (request) => {
    if (
      request.method() === "PUT" &&
      request.url().endsWith(`${path(project.id, task.id)}/status`)
    )
      writes++;
  });
  await control
    .getByRole("button", { name: "Completar tarea", exact: true })
    .click();
  await expect(
    control.getByRole("button", {
      name: "Consultar estado vigente",
      exact: true,
    }),
  ).toBeVisible();
  expect(writes).toBe(1);
  await control
    .getByRole("button", { name: "Consultar estado vigente", exact: true })
    .click();
  await expect(
    control.getByRole("button", { name: "Reabrir tarea", exact: true }),
  ).toBeEnabled();
  expect(writes).toBe(1);
  await expect(
    page
      .getByRole("list", { name: "Transiciones de la tarea", exact: true })
      .getByRole("listitem"),
  ).toHaveCount(1);
  await page.route(`**${path(project.id, task.id)}/history`, (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/problem+json",
      body: JSON.stringify({ status: 503, code: "STORAGE_UNAVAILABLE" }),
    }),
  );
  await control
    .getByRole("button", { name: "Reabrir tarea", exact: true })
    .click();
  await expect(
    control.getByRole("button", { name: "Completar tarea", exact: true }),
  ).toBeEnabled();
  await expect(
    control
      .getByRole("status")
      .filter({ hasText: /^Estado de tarea actualizado$/ }),
  ).toBeVisible();
  const historyRegion = page.getByRole("region", {
    name: "Historial de la tarea",
    exact: true,
  });
  await expect(
    historyRegion.getByRole("button", {
      name: "Reintentar historial",
      exact: true,
    }),
  ).toBeVisible();
  expect(writes).toBe(2);
  await page.unroute(`**${path(project.id, task.id)}/history`);
  await historyRegion
    .getByRole("button", { name: "Reintentar historial", exact: true })
    .click();
  await expect(
    historyRegion
      .getByRole("list", { name: "Transiciones de la tarea", exact: true })
      .getByRole("listitem"),
  ).toHaveCount(2);
  expect(writes).toBe(2);
  expect((await state(request, project.id, task.id)).body.status).toBe(
    "pending",
  );
});

test("complete_reopen_task: controls and history reflow at breakpoint edges with measured keyboard feedback @s26", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto responsive de historial");
  const task = await saveTask(request, project.id, "W".repeat(160), {
    completionCriterion: "W".repeat(2000),
  });
  let snapshot = await state(request, project.id, task.id);
  for (const status of ["completed", "pending"]) {
    const response = await change(
      request,
      project.id,
      task.id,
      status,
      snapshot.etag,
    );
    expect(response.status()).toBe(200);
    snapshot = { body: await response.json(), etag: response.headers().etag };
  }
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const control = page.getByRole("region", {
    name: "Estado de la tarea",
    exact: true,
  });
  const complete = control.getByRole("button", {
    name: "Completar tarea",
    exact: true,
  });
  await expect(complete).toBeEnabled();
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
      for (const element of await page
        .getByRole("main")
        .locator("a,button,input,textarea")
        .all()) {
        const box = await element.boundingBox();
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
  for (
    let attempt = 0;
    attempt < 24 &&
    !(await complete.evaluate((element) => element === document.activeElement));
    attempt++
  )
    await page.keyboard.press("Tab");
  await expect(complete).toBeFocused();
  expect(
    await complete.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await control.evaluate((element) => {
    element.addEventListener(
      "click",
      () => {
        window.taskStateStarted = performance.now();
      },
      { once: true, capture: true },
    );
    const observer = new MutationObserver(() => {
      if (
        [...element.querySelectorAll('[role="status"]')].some(
          (node) => node.textContent === "Cambiando estado de la tarea",
        )
      ) {
        window.taskStateFeedback = performance.now() - window.taskStateStarted;
        observer.disconnect();
      }
    });
    observer.observe(element, {
      childList: true,
      subtree: true,
      characterData: true,
    });
  });
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  let writes = 0;
  await page.route(`**${path(project.id, task.id)}/status`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    await held;
    return route.continue();
  });
  await page.keyboard.press("Enter");
  try {
    await expect(complete).toBeDisabled();
    await expect(
      control
        .getByRole("status")
        .filter({ hasText: /^Cambiando estado de la tarea$/ }),
    ).toBeVisible();
    const feedback = await page.evaluate(() => window.taskStateFeedback);
    expect(feedback).toBeGreaterThanOrEqual(0);
    expect(feedback).toBeLessThan(400);
    console.log(`Task state feedback: ${feedback} ms`);
  } finally {
    release();
  }
  await expect(
    control.getByRole("button", { name: "Reabrir tarea", exact: true }),
  ).toBeEnabled();
  expect(writes).toBe(1);
  expect((await history(request, project.id, task.id)).items).toHaveLength(3);
});
