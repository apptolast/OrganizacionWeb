import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";

test.beforeEach(() =>
  sql(
    "TRUNCATE work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("start_work_session: another task's active session links to its real context and logout removes it @s29 @s39", async ({
  page,
  request,
}) => {
  const project = await create(request, "Contexto de trabajo propio");
  const original = await saveTask(request, project.id, "Trabajo que ya empezó");
  const other = await saveTask(
    request,
    project.id,
    "Otra tarea todavía pendiente",
  );
  const response = await request.post(
    `/api/v1/projects/${project.id}/tasks/${original.id}/work-sessions`,
    {
      headers: {
        ...(await csrfHeaders(request)),
        "Idempotency-Key": randomUUID(),
      },
      data: { plannedMinutes: 25 },
    },
  );
  expect(response.status(), await response.text()).toBe(201);
  const session = await response.json();
  let posts = 0;
  page.on("request", (request) => {
    if (request.method() === "POST" && request.url().endsWith("/work-sessions"))
      posts++;
  });
  await page.goto(`/proyectos/${project.id}/tareas/${other.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await expect(
    section.getByText("Duración prevista: 25 minutos", { exact: true }),
  ).toBeVisible();
  await expect(
    section.locator(`time[datetime="${session.plannedEndAt}"]`),
  ).toBeVisible();
  await expect(
    section.getByRole("button", { name: "Empezar a trabajar", exact: true }),
  ).toHaveCount(0);
  const link = section.getByRole("link", {
    name: "Ir a la tarea de esta sesión",
    exact: true,
  });
  await expect(link).toHaveAttribute(
    "href",
    `/proyectos/${project.id}/tareas/${original.id}`,
  );
  await link.click();
  await expect(
    page.getByRole("heading", { name: original.title, exact: true }),
  ).toBeVisible();
  await expect(
    section.getByText("Duración prevista: 25 minutos", { exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
  await expect(section).toHaveCount(0);
  expect(posts).toBe(0);
  expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
});

test("start_work_session: lost acknowledgement is recovered by the original key without another POST @s30 @s32 @s33 @s40", async ({
  page,
  request,
}) => {
  const project = await create(request, "Recuperación sin repetir el inicio");
  const task = await saveTask(
    request,
    project.id,
    "Conservar la intención original",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`;
  let stored;
  let key;
  let posts = 0;
  await page.route(`**${endpoint}`, async (route) => {
    posts++;
    key = route.request().headers()["idempotency-key"];
    const response = await route.fetch();
    expect(response.status()).toBe(201);
    stored = await response.json();
    await route.abort("failed");
  });
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  const duration = section.getByLabel("Duración prevista (minutos)", {
    exact: true,
  });
  await duration.fill("25");
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const check = section.getByRole("button", {
    name: "Comprobar inicio",
    exact: true,
  });
  await expect(check).toBeVisible();
  await expect(duration).toHaveValue("25");
  await expect(duration).toHaveAttribute("readonly", "");
  await expect(
    section.getByText("Sesión iniciada", { exact: true }),
  ).toHaveCount(0);
  await expect(
    section.getByRole("button", {
      name: "Reenviar el mismo inicio",
      exact: true,
    }),
  ).toHaveCount(0);
  expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
  const recovery = page.waitForResponse((response) =>
    response.url().endsWith(`/api/v1/work-sessions/by-request/${key}`),
  );
  await check.focus();
  await check.press("Enter");
  expect(await (await recovery).json()).toEqual(stored);
  await expect(
    section.getByText("Sesión iniciada", { exact: true }),
  ).toBeVisible();
  await expect(
    section.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
  ).toBeFocused();
  expect(posts).toBe(1);
  expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
});

test("start_work_session: explicit start persists and reload discovers the original active session @s1 @s22 @s23 @s28 @s29 @s36 @s37", async ({
  page,
  request,
}) => {
  const project = await create(request, "Una sesión deliberada");
  const task = await saveTask(request, project.id, "Leer y preparar notas");
  const route = `/proyectos/${project.id}/tareas/${task.id}`;
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`;
  const writes = [];
  page.on("request", (request) => {
    if (request.method() === "POST" && request.url().endsWith(endpoint))
      writes.push(request);
  });
  await page.goto(route);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await expect(
    section.getByText("No hay una sesión de trabajo activa.", { exact: true }),
  ).toBeVisible();
  const duration = section.getByLabel("Duración prevista (minutos)", {
    exact: true,
  });
  await expect(duration).toHaveValue("");
  expect(writes).toHaveLength(0);
  expect(sql("SELECT count(*) FROM work_sessions")).toBe("0");
  await duration.fill("25");
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const response = await responsePromise;
  expect(response.status(), await response.text()).toBe(201);
  const session = await response.json();
  expect(Object.keys(session).sort()).toEqual(
    [
      "id",
      "projectId",
      "taskId",
      "startedAt",
      "plannedMinutes",
      "plannedEndAt",
      "zoneId",
    ].sort(),
  );
  expect(session).toMatchObject({
    projectId: project.id,
    taskId: task.id,
    plannedMinutes: 25,
    zoneId: "UTC",
  });
  expect(session.id).toMatch(/^[0-9a-f-]{36}$/);
  expect(response.headers().location).toBe(
    `/api/v1/work-sessions/${session.id}`,
  );
  await expect(
    section.getByText("Sesión iniciada", { exact: true }),
  ).toBeVisible();
  await expect(
    section.getByText("Duración prevista: 25 minutos", { exact: true }),
  ).toBeVisible();
  expect(
    sql(
      `SELECT planned_minutes || ':' || status || ':' || extract(epoch FROM (planned_end_at-started_at)) FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe("25:running:1500.000000");
  const key = response.request().headers()["idempotency-key"];
  const recovered = await request.get(
    `/api/v1/work-sessions/by-request/${key}`,
  );
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(session);
  const detail = await request.get(`/api/v1/work-sessions/${session.id}`);
  expect(detail.status()).toBe(200);
  expect(await detail.json()).toEqual(session);

  const activePromise = page.waitForResponse((response) =>
    response.url().endsWith("/api/v1/work-sessions/active"),
  );
  await page.reload();
  expect(await (await activePromise).json()).toEqual({ session });
  await expect(
    section.getByText("Duración prevista: 25 minutos", { exact: true }),
  ).toBeVisible();
  await expect(
    section.locator(`time[datetime="${session.plannedEndAt}"]`),
  ).toBeVisible();
  await expect(
    section.getByRole("button", { name: "Empezar a trabajar", exact: true }),
  ).toHaveCount(0);
  await expect(
    section.getByRole("link", {
      name: "Ir a la tarea de esta sesión",
      exact: true,
    }),
  ).toHaveAttribute("href", route);
  expect(writes).toHaveLength(1);
  expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
});
