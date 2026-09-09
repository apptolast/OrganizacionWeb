import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";

test.beforeEach(() =>
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects CASCADE",
  ),
);

test("close_work_session: pending parent lookup cannot replace closure after navigation @s39", async ({
  page,
  request,
}) => {
  const project = await create(
    request,
    "Cerrar con consulta anterior pendiente",
  );
  const task = await saveTask(
    request,
    project.id,
    "Preservar el cierre visible",
  );
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  const starting = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/work-sessions"),
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const started = await starting;
  expect(started.status()).toBe(201);
  const session = await started.json();
  let held;
  const received = new Promise((done) => {
    held = done;
  });
  let release;
  const delivery = new Promise((done) => {
    release = done;
  });
  let delivered;
  const finished = new Promise((done) => {
    delivered = done;
  });
  await page.route(
    "**/work-sessions/active",
    async (route) => {
      const actual = await route.fetch();
      expect(actual.status()).toBe(200);
      expect(await actual.json()).toEqual({ session });
      held();
      await delivery;
      await route.fulfill({ response: actual });
      delivered();
    },
    { times: 1 },
  );
  await section
    .getByRole("button", { name: "Actualizar sesión activa", exact: true })
    .click();
  await received;
  await expect(
    section
      .getByRole("status")
      .filter({ hasText: /^Consultando sesión activa$/ }),
  ).toBeVisible();
  await section
    .getByRole("heading", { name: "Estado de la sesión", exact: true })
    .locator("..")
    .getByRole("link", { name: "Cerrar sesión de trabajo", exact: true })
    .click();
  const stableUrl = `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`;
  await expect(page).toHaveURL(stableUrl);
  const note = "El cierre permanece después de la consulta anterior";
  await page
    .getByLabel("Avance anotado (opcional)", { exact: true })
    .fill(note);
  const closing = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith(`/work-sessions/${session.id}/close`),
  );
  await page
    .getByRole("button", { name: "Confirmar cierre", exact: true })
    .click();
  const closed = await closing;
  expect(closed.status()).toBe(201);
  const receipt = await closed.json();
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toBeVisible();
  release();
  await finished;
  await expect(page).toHaveURL(stableUrl);
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toBeVisible();
  await expect(page.getByText(note, { exact: true })).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Pausar", exact: true }),
  ).toHaveCount(0);
  await expect(
    page
      .getByRole("region", { name: "Sesión abierta", exact: true })
      .getByText("No hay ninguna sesión abierta.", { exact: true }),
  ).toBeVisible();
  const recovered = await request.get(
    `/api/v1/work-sessions/${session.id}/closure`,
  );
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(receipt);
});

test("close_work_session: lost confirmed response reloads closure by URL without retained key @s28 @s33", async ({
  page,
  request,
}) => {
  const project = await create(request, "Recuperar cierre sin respuesta");
  const task = await saveTask(
    request,
    project.id,
    "Conservar notas tras recarga",
  );
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  const starting = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/work-sessions"),
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const startResponse = await starting;
  expect(startResponse.status()).toBe(201);
  const session = await startResponse.json();
  expect(session.id).toMatch(/^[0-9a-f-]{36}$/i);
  await section
    .getByRole("heading", { name: "Estado de la sesión", exact: true })
    .locator("..")
    .getByRole("link", { name: "Cerrar sesión de trabajo", exact: true })
    .click();
  const stableUrl = `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`;
  await expect(page).toHaveURL(stableUrl);
  const progress = "Hecho durable aunque la respuesta se pierda";
  const nextStep = "Continuar después de recargar";
  await page
    .getByLabel("Avance anotado (opcional)", { exact: true })
    .fill(progress);
  await page
    .getByLabel("Siguiente paso (opcional)", { exact: true })
    .fill(nextStep);
  let confirmDurable;
  const durable = new Promise((done) => {
    confirmDurable = done;
  });
  let sentCount = 0;
  await page.route(`**/work-sessions/${session.id}/close`, async (route) => {
    sentCount++;
    const actual = await route.fetch();
    expect(actual.status()).toBe(201);
    const receipt = await actual.json();
    confirmDurable(receipt);
    await route.abort("connectionreset");
  });
  await page
    .getByRole("button", { name: "Confirmar cierre", exact: true })
    .click();
  const receipt = await durable;
  expect(receipt.action).toBe("CLOSE");
  expect(receipt.closure.progressNote).toBe(progress);
  await expect(
    page.getByRole("button", { name: "Comprobar cierre", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toHaveCount(0);
  let keyReadsAfterReload = 0;
  page.on("request", (sent) => {
    if (
      sent.method() === "GET" &&
      new URL(sent.url()).pathname.startsWith(
        "/api/v1/work-session-changes/by-request/",
      )
    )
      keyReadsAfterReload++;
  });
  const found = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().endsWith(`/work-sessions/${session.id}/closure`),
  );
  await page.reload();
  const recovered = await found;
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(receipt);
  await expect(page).toHaveURL(stableUrl);
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toBeVisible();
  await expect(page.getByText(progress, { exact: true })).toBeVisible();
  await expect(page.getByText(nextStep, { exact: true })).toBeVisible();
  expect(sentCount).toBe(1);
  expect(keyReadsAfterReload).toBe(0);
  expect(
    sql(
      `SELECT count(*) FROM work_session_changes WHERE session_id='${session.id}' AND action='CLOSE'`,
    ),
  ).toBe("1");
  expect(
    sql(
      `SELECT count(*) FROM work_session_intervals WHERE session_id='${session.id}'`,
    ),
  ).toBe("1");
  expect(
    sql(
      `SELECT count(*) FROM outbox_events WHERE aggregate_id='${session.id}' AND event_type='WorkSessionClosed.v1'`,
    ),
  ).toBe("1");
});

test("close_work_session: paused closes without counting rest and keeps a new active session separate @s2 @s34", async ({
  page,
  request,
}) => {
  const project = await create(request, "Cierre después de pausa");
  const task = await saveTask(request, project.id, "Retomar en otra sesión");
  const taskUrl = `/proyectos/${project.id}/tareas/${task.id}`;
  await page.goto(taskUrl);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  const starting = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/work-sessions"),
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const startResponse = await starting;
  expect(startResponse.status()).toBe(201);
  const session = await startResponse.json();
  expect(session.id).toMatch(/^[0-9a-f-]{36}$/i);
  const pausing = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith(`/work-sessions/${session.id}/pause`),
  );
  await section.getByRole("button", { name: "Pausar", exact: true }).click();
  const pauseResponse = await pausing;
  expect(pauseResponse.status()).toBe(201);
  const pause = await pauseResponse.json();
  await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
  await section
    .getByRole("heading", { name: "Estado de la sesión", exact: true })
    .locator("..")
    .getByRole("link", { name: "Cerrar sesión de trabajo", exact: true })
    .click();
  await expect(page).toHaveURL(
    `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`,
  );
  const closing = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith(`/work-sessions/${session.id}/close`),
  );
  await page
    .getByRole("button", { name: "Confirmar cierre", exact: true })
    .click();
  const closeResponse = await closing;
  expect(closeResponse.status()).toBe(201);
  const receipt = await closeResponse.json();
  expect(receipt.before).toEqual(pause.after);
  expect(receipt.after.status).toBe("closed");
  expect(receipt.after.revision).toBe("3");
  expect(receipt.after.workedMicroseconds).toBe(pause.after.workedMicroseconds);
  expect(receipt.after.session).toEqual(session);
  expect(
    sql(
      `SELECT count(*) FROM work_session_intervals WHERE session_id='${session.id}'`,
    ),
  ).toBe("1");
  expect(
    sql(
      `SELECT worked_microseconds::text FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe(pause.after.workedMicroseconds);
  await expect(
    page.getByText("Sin avance anotado", { exact: true }),
  ).toBeVisible();
  const activeRegion = page.getByRole("region", {
    name: "Sesión abierta",
    exact: true,
  });
  await expect(
    activeRegion.getByText("No hay ninguna sesión abierta.", { exact: true }),
  ).toBeVisible();
  const nextPage = await page.context().newPage();
  await nextPage.goto(taskUrl);
  const nextSection = nextPage.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await nextSection
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("15");
  const newStarting = nextPage.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/work-sessions"),
  );
  await nextSection
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const newResponse = await newStarting;
  expect(newResponse.status()).toBe(201);
  const next = await newResponse.json();
  expect(next.id).not.toBe(session.id);
  await nextPage.close();
  await activeRegion
    .getByRole("button", { name: "Consultar sesión abierta", exact: true })
    .click();
  await expect(
    activeRegion.getByText("Hay otra sesión abierta.", { exact: true }),
  ).toBeVisible();
  await expect(
    activeRegion.getByRole("link", {
      name: "Ir a la sesión abierta",
      exact: true,
    }),
  ).toHaveAttribute(
    "href",
    `/proyectos/${project.id}/tareas/${task.id}/sesiones/${next.id}`,
  );
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toBeVisible();
  const original = await request.get(
    `/api/v1/work-sessions/${session.id}/closure`,
  );
  expect(original.status()).toBe(200);
  expect(await original.json()).toEqual(receipt);
  expect(
    sql(
      `SELECT count(*) FROM work_session_changes WHERE session_id='${session.id}'`,
    ),
  ).toBe("2");
});

test("close_work_session: running closes with exact durable time and reloads by session URL @s1 @s26 @s32", async ({
  page,
  request,
}) => {
  const project = await create(request, "Cierre deliberado de trabajo");
  const task = await saveTask(
    request,
    project.id,
    "Conservar avance y siguiente paso",
  );
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  const started = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/work-sessions"),
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const startResponse = await started;
  expect(startResponse.status()).toBe(201);
  const session = await startResponse.json();
  expect(session.id).toMatch(
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  );
  const sessionUrl = `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`;
  const closeRequests = [];
  page.on("request", (sent) => {
    if (
      sent.method() === "POST" &&
      sent.url().endsWith(`/work-sessions/${session.id}/close`)
    )
      closeRequests.push(sent);
  });
  await section
    .getByRole("heading", { name: "Estado de la sesión", exact: true })
    .locator("..")
    .getByRole("link", { name: "Cerrar sesión de trabajo", exact: true })
    .click();
  await expect(page).toHaveURL(sessionUrl);
  await expect(
    page.getByRole("heading", {
      name: "Cerrar sesión de trabajo",
      exact: true,
    }),
  ).toBeVisible();
  expect(closeRequests).toHaveLength(0);
  const progress = "  Avance parcial real\nSin completar la tarea  ";
  const nextStep = "Revisar las notas mañana";
  await page
    .getByLabel("Avance anotado (opcional)", { exact: true })
    .fill(progress);
  await page
    .getByLabel("Siguiente paso (opcional)", { exact: true })
    .fill(nextStep);
  const closed = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith(`/work-sessions/${session.id}/close`),
  );
  await page
    .getByRole("button", { name: "Confirmar cierre", exact: true })
    .click();
  const closeResponse = await closed;
  expect(closeResponse.status()).toBe(201);
  const receipt = await closeResponse.json();
  expect(Object.keys(receipt).sort()).toEqual(
    [
      "id",
      "sessionId",
      "action",
      "occurredAt",
      "before",
      "after",
      "closure",
    ].sort(),
  );
  expect(receipt.action).toBe("CLOSE");
  expect(receipt.sessionId).toBe(session.id);
  expect(receipt.before.status).toBe("running");
  expect(receipt.after.status).toBe("closed");
  expect(receipt.after.revision).toBe("2");
  expect(receipt.before.session).toEqual(session);
  expect(receipt.after.session).toEqual(session);
  expect(receipt.after.runningSince).toBeNull();
  expect(receipt.after.changedAt).toBe(receipt.occurredAt);
  expect(receipt.closure.progressNote).toBe(progress);
  expect(receipt.closure.nextStep).toBe(nextStep);
  expect(closeResponse.headers().location).toBe(
    `/api/v1/work-session-changes/${receipt.id}`,
  );
  expect(closeRequests).toHaveLength(1);
  expect(closeRequests[0].postDataJSON()).toEqual({
    progressNote: progress,
    nextStep,
  });
  expect(
    sql(
      `SELECT status || ':' || revision FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe("closed:2");
  const exactIntervalTime = sql(
    `SELECT sum(extract(epoch FROM (end_at-start_at))*1000000)::bigint::text FROM work_session_intervals WHERE session_id='${session.id}'`,
  );
  expect(receipt.after.workedMicroseconds).toBe(exactIntervalTime);
  expect(
    sql(
      `SELECT (extract(epoch FROM (changed_at-started_at))*1000000)::bigint::text FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe(exactIntervalTime);
  expect(
    sql(
      `SELECT worked_microseconds::text FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe(exactIntervalTime);
  expect(
    sql(
      `SELECT count(*) FROM work_session_intervals WHERE session_id='${session.id}'`,
    ),
  ).toBe("1");
  expect(
    sql(
      `SELECT count(*) FROM work_session_changes WHERE session_id='${session.id}' AND action='CLOSE'`,
    ),
  ).toBe("1");
  expect(
    sql(
      `SELECT count(*) FROM outbox_events WHERE aggregate_id='${session.id}' AND event_type='WorkSessionClosed.v1'`,
    ),
  ).toBe("1");
  expect(sql(`SELECT status FROM tasks WHERE id='${task.id}'`)).toBe("pending");
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toBeVisible();
  await expect(
    page.locator(".closure-note").filter({ hasText: "Avance parcial real" }),
  ).toHaveJSProperty("textContent", progress);
  const recovered = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().endsWith(`/work-sessions/${session.id}/closure`),
  );
  await page.reload();
  const recoveryResponse = await recovered;
  expect(recoveryResponse.status()).toBe(200);
  expect(await recoveryResponse.json()).toEqual(receipt);
  await expect(page).toHaveURL(sessionUrl);
  await expect(
    page.getByRole("status").filter({ hasText: /^Sesión cerrada$/ }),
  ).toBeVisible();
  await expect(page.getByText(nextStep, { exact: true })).toBeVisible();
  expect(closeRequests).toHaveLength(1);
  expect(
    sql(
      `SELECT count(*) FROM work_session_changes WHERE session_id='${session.id}'`,
    ),
  ).toBe("1");
});
