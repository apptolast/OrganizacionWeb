import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql, stored } from "./support/projects.mjs";
import { saveTask, taskFields } from "./support/tasks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";
import { request as anonymousRequest } from "@playwright/test";

test.beforeEach(() => sql("TRUNCATE tasks,outbox_events,projects"));
const resource = (projectId, taskId) =>
  `/api/v1/projects/${projectId}/tasks/${taskId}`;
const screen = (projectId, taskId) =>
  `/proyectos/${projectId}/tareas/${taskId}`;

test("split_task: nested children preserve parent, project and flat task contract @s1 @s2 @s3 @s9 @s10 @s11 @s23 @s34", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de pasos pequeños");
  const root = await saveTask(request, project.id, "Portada de prueba", {
    completionCriterion: "Portada completa",
    estimatedMinutes: 60,
  });
  const before = stored(project.id).project;
  const etag = (await request.get(`/api/v1/projects/${project.id}`)).headers()
    .etag;
  const child = await saveTask(
    request,
    project.id,
    "Título 🚀 <b>literal</b>",
    {
      completionCriterion: "Texto sintético privado",
      estimatedMinutes: 20,
    },
    root.id,
  );
  const grandchild = await saveTask(
    request,
    project.id,
    "Revisar una frase",
    {
      estimatedMinutes: 5,
    },
    child.id,
  );
  for (const [task, parent] of [
    [root, null],
    [child, root],
    [grandchild, child],
  ]) {
    const relation = await request.get(
      `${resource(project.id, task.id)}/parent`,
    );
    expect(relation.status()).toBe(200);
    expect(relation.headers()["cache-control"]).toContain("no-store");
    expect(await relation.json()).toEqual({ parent });
    const detail = await request.get(resource(project.id, task.id));
    expect(await detail.json()).toEqual(task);
  }
  const rootsChildren = await request.get(
    `${resource(project.id, root.id)}/subtasks`,
  );
  expect(await rootsChildren.json()).toEqual({
    items: [child],
    nextCursor: null,
  });
  const leafChildren = await request.get(
    `${resource(project.id, grandchild.id)}/subtasks`,
  );
  expect(await leafChildren.json()).toEqual({ items: [], nextCursor: null });
  const flat = await (
    await request.get(`/api/v1/projects/${project.id}/tasks`)
  ).json();
  expect(flat.items.map((task) => task.id).sort()).toEqual(
    [root.id, child.id, grandchild.id].sort(),
  );
  for (const task of flat.items)
    expect(Object.keys(task).sort()).toEqual(taskFields);
  expect(stored(project.id).project).toEqual(before);
  expect(
    (await request.get(`/api/v1/projects/${project.id}`)).headers().etag,
  ).toBe(etag);
  const events = stored(project.id).events;
  expect(
    events.filter((event) => event.event_type === "TaskCreated.v1"),
  ).toHaveLength(1);
  const splits = events.filter(
    (event) => event.event_type === "SubtaskCreated.v1",
  );
  expect(splits).toHaveLength(2);
  for (const [task, parent] of [
    [child, root],
    [grandchild, child],
  ]) {
    const event = splits.find((item) => item.payload.taskId === task.id);
    expect(Object.keys(event.payload).sort()).toEqual(
      [
        "eventId",
        "aggregateId",
        "ownerId",
        "occurredAt",
        "schemaVersion",
        "type",
        "taskId",
        "parentTaskId",
        "title",
      ].sort(),
    );
    expect(event.payload).toMatchObject({
      aggregateId: project.id,
      taskId: task.id,
      parentTaskId: parent.id,
      title: task.title,
      type: "SubtaskCreated.v1",
      schemaVersion: 1,
    });
    expect(event.status).toBe("pending");
  }
  await page.goto(`/proyectos/${project.id}`);
  await page.getByRole("link", { name: root.title, exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`${screen(project.id, root.id)}$`));
  await page.getByRole("link", { name: child.title, exact: true }).click();
  await expect(
    page.getByRole("heading", { level: 1, name: child.title, exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: grandchild.title, exact: true }).click();
  await page.reload();
  await expect(
    page.getByRole("heading", {
      level: 1,
      name: grandchild.title,
      exact: true,
    }),
  ).toBeVisible();
  await page.locator(`a[href="${screen(project.id, child.id)}"]`).click();
  await expect(
    page.getByRole("heading", { level: 1, name: child.title, exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM tasks")).toBe("3");
});

test("split_task: 21 direct children keep their cursor context and exclude newer siblings @s12 @s14 @s35", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto con pasos paginados");
  const parent = await saveTask(request, project.id, "Trabajo divisible");
  const children = [];
  for (let index = 1; index <= 21; index++)
    children.push(
      await saveTask(
        request,
        project.id,
        `Paso ${String(index).padStart(2, "0")}`,
        {},
        parent.id,
      ),
    );
  const ids = children.map((task) => `'${task.id}'`).join(",");
  sql(
    `UPDATE tasks SET created_at='2026-09-05T12:00:00.123456Z',updated_at='2026-09-05T12:00:00.123456Z' WHERE id IN (${ids})`,
  );
  const expected = children
    .map((task) => task.id)
    .sort()
    .reverse();
  const path = `${resource(project.id, parent.id)}/subtasks`;
  const firstResponse = await request.get(path);
  expect(firstResponse.status()).toBe(200);
  const first = await firstResponse.json();
  expect(first.items.map((task) => task.id)).toEqual(expected.slice(0, 20));
  expect(first.nextCursor).toBeTruthy();
  const cursorData = JSON.parse(
    Buffer.from(first.nextCursor, "base64url").toString(),
  );
  expect(Object.keys(cursorData).sort()).toEqual(
    ["projectId", "parentTaskId", "createdAt", "id"].sort(),
  );
  expect(cursorData).toMatchObject({
    projectId: project.id,
    parentTaskId: parent.id,
  });
  await page.goto(screen(project.id, parent.id));
  const region = page.getByRole("region", { name: "Subtareas", exact: true });
  const list = region.getByRole("list", { name: "Subtareas guardadas" });
  await expect(list.getByRole("listitem")).toHaveCount(20);
  const newer = await saveTask(
    request,
    project.id,
    "Paso posterior",
    {},
    parent.id,
  );
  const second = await (
    await request.get(`${path}?cursor=${encodeURIComponent(first.nextCursor)}`)
  ).json();
  expect(second.items.map((task) => task.id)).toEqual(expected.slice(20));
  expect(second.nextCursor).toBeNull();
  expect(second.items.some((task) => task.id === newer.id)).toBe(false);
  await region
    .getByRole("button", { name: "Más subtareas antiguas", exact: true })
    .click();
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await expect(list.getByRole("link")).toHaveAttribute(
    "href",
    screen(project.id, expected[20]),
  );
  await region
    .getByRole("button", { name: "Volver a subtareas recientes", exact: true })
    .click();
  await expect(list.getByRole("listitem")).toHaveCount(20);
  await expect(list.getByRole("link").first()).toHaveText(newer.title);
  const other = await saveTask(request, project.id, "Otro padre");
  for (const url of [
    `${resource(project.id, other.id)}/subtasks?cursor=${encodeURIComponent(first.nextCursor)}`,
    `/api/v1/projects/${project.id}/tasks?cursor=${encodeURIComponent(first.nextCursor)}`,
  ]) {
    const response = await request.get(url);
    expect(response.status()).toBe(400);
    expect((await response.json()).code).toBe("VALIDATION_ERROR");
  }
});

test("split_task: private relations and children have indistinguishable failures @s6 @s7 @s8 @s19", async ({
  request,
}) => {
  const own = await create(request, "Proyecto propio de prueba");
  const other = await create(request, "Proyecto ajeno de prueba");
  const foreign = await saveTask(
    request,
    other.id,
    "Contenido privado sintético",
  );
  sql(`UPDATE projects SET owner_id='another-owner' WHERE id='${other.id}'`);
  const missing = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa";
  const absent = await request.get(`${resource(own.id, missing)}/parent`);
  expect(absent.status()).toBe(404);
  const body = await absent.json();
  for (const [projectId, taskId] of [
    [own.id, missing],
    [other.id, foreign.id],
    [own.id, foreign.id],
    [missing, missing],
  ]) {
    const path = resource(projectId, taskId);
    for (const operation of ["parent", "subtasks", "create"]) {
      const response =
        operation === "create"
          ? await request.post(`${path}/subtasks`, {
              headers: await csrfHeaders(request),
              data: { title: "No debe guardarse" },
            })
          : await request.get(`${path}/${operation}`);
      expect(response.status()).toBe(404);
      expect(response.headers()["cache-control"]).toContain("no-store");
      expect(await response.json()).toEqual(body);
    }
  }
  expect(sql("SELECT count(*) FROM tasks")).toBe("1");
  const parent = await saveTask(
    request,
    own.id,
    "Padre para fronteras de acceso",
  );
  const ownPath = resource(own.id, parent.id);
  const anonymous = await anonymousRequest.newContext({
    baseURL: test.info().project.use.baseURL,
  });
  try {
    for (const operation of ["parent", "subtasks", "create"]) {
      const response =
        operation === "create"
          ? await anonymous.post(`${ownPath}/subtasks`, {
              data: { title: "No autorizado" },
            })
          : await anonymous.get(`${ownPath}/${operation}`);
      expect(response.status()).toBe(401);
      expect(response.headers()["www-authenticate"]).toBeUndefined();
    }
  } finally {
    await anonymous.dispose();
  }
  for (const headers of [
    {},
    { "X-CSRF-TOKEN": "invalid" },
    { ...(await csrfHeaders(request)), Origin: "https://foreign.invalid" },
  ]) {
    const response = await request.post(`${ownPath}/subtasks`, {
      headers,
      data: { title: "No autorizado" },
    });
    expect(response.status()).toBe(403);
    expect((await response.json()).code).toBe(
      headers.Origin ? "UNTRUSTED_ORIGIN" : "CSRF_INVALID",
    );
  }
  expect(sql("SELECT count(*) FROM tasks")).toBe("2");
});

test("split_task: relation and child retries preserve drafts and refresh after a pending save @s24 @s25 @s26 @s29 @s38", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de recuperación");
  const parent = await saveTask(
    request,
    project.id,
    "Tarea con pasos recuperables",
  );
  const path = `${resource(project.id, parent.id)}/subtasks`;
  const unavailable = {
    status: 503,
    contentType: "application/problem+json",
    body: JSON.stringify({ status: 503, code: "STORAGE_UNAVAILABLE" }),
  };
  await page.route(`**${resource(project.id, parent.id)}/parent`, (route) =>
    route.fulfill(unavailable),
  );
  let reads = 0;
  await page.route(`**${path}`, (route) => {
    if (route.request().method() === "GET" && ++reads === 1)
      return route.fulfill(unavailable);
    return route.continue();
  });
  await page.goto(screen(project.id, parent.id));
  await expect(
    page.getByRole("button", { name: "Reintentar relación", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByText("Tarea principal confirmada", { exact: true }),
  ).toHaveCount(0);
  await page.unroute(`**${resource(project.id, parent.id)}/parent`);
  await page
    .getByRole("button", { name: "Reintentar relación", exact: true })
    .click();
  await expect(
    page.getByText("Tarea principal confirmada", { exact: true }),
  ).toBeVisible();
  const region = page.getByRole("region", { name: "Subtareas", exact: true });
  const title = region.getByLabel("Título de la tarea", { exact: true });
  const criterion = region.getByLabel("Criterio de finalización", {
    exact: true,
  });
  const minutes = region.getByLabel("Estimación en minutos", { exact: true });
  const submit = region.getByRole("button", {
    name: "Crear subtarea",
    exact: true,
  });
  for (const status of [400, 503, 0]) {
    await title.fill("Paso conservado");
    await criterion.fill("Resultado conservado");
    await minutes.fill("15");
    let attempts = 0;
    const failure = async (route) => {
      if (route.request().method() !== "POST") return route.fallback();
      attempts++;
      if (!status) return route.abort("failed");
      return route.fulfill({
        status,
        contentType: "application/problem+json",
        body: JSON.stringify({
          status,
          code: status === 400 ? "VALIDATION_ERROR" : "STORAGE_UNAVAILABLE",
          errors: status === 400 ? [{ field: "title", code: "REQUIRED" }] : [],
        }),
      });
    };
    await page.route(`**${path}`, failure);
    await submit.click();
    await expect(submit).toBeEnabled();
    await expect.poll(() => attempts).toBe(1);
    await expect(title).toHaveValue("Paso conservado");
    await expect(criterion).toHaveValue("Resultado conservado");
    await expect(minutes).toHaveValue("15");
    expect(sql("SELECT count(*) FROM tasks")).toBe("1");
    await page.unroute(`**${path}`, failure);
  }
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  let posts = 0;
  await page.route(`**${path}`, async (route) => {
    if (route.request().method() !== "POST") return route.fallback();
    posts++;
    await held;
    return route.continue();
  });
  await submit.click();
  try {
    await expect(
      region.getByRole("status").filter({ hasText: /^Guardando subtarea$/ }),
    ).toBeVisible();
    await expect(submit).toBeDisabled();
    await region
      .getByRole("button", { name: "Reintentar subtareas", exact: true })
      .click();
    await expect.poll(() => reads).toBe(2);
    await expect(
      region.getByRole("status").filter({ hasText: /Cargando/ }),
    ).toHaveCount(0);
    expect(sql("SELECT count(*) FROM tasks")).toBe("1");
  } finally {
    release();
  }
  await expect(
    region.getByRole("status").filter({ hasText: /^Subtarea guardada$/ }),
  ).toBeVisible();
  await expect(
    region
      .getByRole("list", { name: "Subtareas guardadas" })
      .getByRole("link", { name: "Paso conservado", exact: true }),
  ).toBeVisible();
  expect(reads).toBe(3);
  expect(posts).toBe(1);
  await page.reload();
  await expect(
    region
      .getByRole("list", { name: "Subtareas guardadas" })
      .getByRole("link", { name: "Paso conservado", exact: true }),
  ).toBeVisible();
  expect(posts).toBe(1);
});

test("split_task: long child content reflows with accessible keyboard feedback @s29 @s32 @s33", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto responsive de prueba");
  const ancestor = await saveTask(request, project.id, "P");
  const parent = await saveTask(
    request,
    project.id,
    "W".repeat(160),
    {},
    ancestor.id,
  );
  await saveTask(request, project.id, "A", {}, parent.id);
  await saveTask(
    request,
    project.id,
    "W".repeat(160),
    { completionCriterion: "W".repeat(2000), estimatedMinutes: 1440 },
    parent.id,
  );
  await page.goto(screen(project.id.toUpperCase(), parent.id.toUpperCase()));
  const region = page.getByRole("region", { name: "Subtareas", exact: true });
  await expect(
    region.getByRole("list", { name: "Subtareas guardadas" }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "P", exact: true }),
  ).toBeVisible();
  await expect(
    region.getByRole("link", { name: "A", exact: true }),
  ).toBeVisible();
  await expect(
    page.locator(`a[href="/proyectos/${project.id}"]`),
  ).toBeVisible();
  const title = region.getByLabel("Título de la tarea", { exact: true });
  const submit = region.getByRole("button", {
    name: "Crear subtarea",
    exact: true,
  });
  await submit.click();
  await expect(title).toBeFocused();
  await expect(title).toHaveAttribute("aria-invalid", "true");
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
      for (const control of await page
        .getByRole("main")
        .locator("a,input,textarea,button")
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
  await title.fill("Paso por teclado");
  await page.keyboard.press("Tab");
  await expect(
    region.getByLabel("Criterio de finalización", { exact: true }),
  ).toBeFocused();
  await page.keyboard.press("Tab");
  const estimate = region.getByLabel("Estimación en minutos", { exact: true });
  await expect(estimate).toBeFocused();
  await page.keyboard.type("1e");
  expect(await estimate.evaluate((element) => element.validity.badInput)).toBe(
    true,
  );
  await page.keyboard.press("Tab");
  await page.keyboard.press("Enter");
  await expect(estimate).toBeFocused();
  expect(sql("SELECT count(*) FROM tasks")).toBe("4");
  await estimate.fill("5");
  await page.keyboard.press("Tab");
  await expect(submit).toBeFocused();
  await region.evaluate((element) => {
    element.querySelector("form").addEventListener(
      "submit",
      () => {
        window.subtaskStart = performance.now();
      },
      { capture: true, once: true },
    );
    const observer = new MutationObserver(() => {
      if (
        [...element.querySelectorAll('[role="status"]')].some(
          (status) => status.textContent === "Guardando subtarea",
        )
      ) {
        window.subtaskFeedback = performance.now() - window.subtaskStart;
        observer.disconnect();
      }
    });
    observer.observe(element, {
      subtree: true,
      childList: true,
      characterData: true,
    });
  });
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  await page.route(
    `**${resource(project.id, parent.id)}/subtasks`,
    async (route) => {
      if (route.request().method() !== "POST") return route.continue();
      await held;
      return route.continue();
    },
  );
  await page.keyboard.press("Enter");
  try {
    await expect(submit).toBeDisabled();
    await expect(
      region.getByRole("status").filter({ hasText: /^Guardando subtarea$/ }),
    ).toBeVisible();
    const elapsed = await page.evaluate(() => window.subtaskFeedback);
    expect(elapsed).toBeGreaterThanOrEqual(0);
    expect(elapsed).toBeLessThan(400);
    console.info(`Subtask save feedback: ${Math.round(elapsed)} ms`);
  } finally {
    release();
  }
  await expect(
    region
      .getByRole("list", { name: "Subtareas guardadas" })
      .getByRole("link", { name: "Paso por teclado", exact: true }),
  ).toBeVisible();
});
