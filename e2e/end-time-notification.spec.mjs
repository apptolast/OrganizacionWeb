import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";

test.beforeEach(() =>
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("end_time_notification: real one-minute deadline is checked before notice and extension @s28 @s30 @s35", async ({
  page,
  request,
}) => {
  test.setTimeout(120000);
  const project = await create(request, "Respetar el minuto elegido");
  const task = await saveTask(
    request,
    project.id,
    "Comprobar el fin real del servidor",
  );
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("1");
  const starting = page.waitForResponse(
    (r) =>
      r.request().method() === "POST" && r.url().endsWith("/work-sessions"),
  );
  const firstEnd = page.waitForResponse(
    (r) => r.request().method() === "GET" && r.url().endsWith("/end-time"),
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const started = await starting;
  expect(started.status()).toBe(201);
  const session = await started.json();
  const initialResponse = await firstEnd;
  expect(initialResponse.status()).toBe(200);
  const initial = await initialResponse.json();
  expect(initial.state.session).toEqual(session);
  expect(initial.effectiveEndAt).toBe(session.plannedEndAt);
  expect(
    sql(
      `SELECT '${initial.serverNow}'::timestamptz < '${initial.effectiveEndAt}'::timestamptz`,
    ),
  ).toBe("t");
  await expect(
    section.getByText("Ha llegado el fin acordado", { exact: true }),
  ).toHaveCount(0);
  const counts = () =>
    sql(
      "SELECT (SELECT count(*) FROM work_session_changes) || ':' || (SELECT count(*) FROM work_session_intervals) || ':' || (SELECT count(*) FROM outbox_events WHERE event_type='WorkSessionExtended.v1')",
    );
  expect(counts()).toBe("0:0:0");
  const focus = page
    .getByRole("navigation", { name: "Principal", exact: true })
    .getByRole("link", { name: "Proyectos", exact: true });
  await focus.focus();
  const deadlineResponse = await page.waitForResponse(
    (r) =>
      r.request().method() === "GET" &&
      r.url().endsWith(`/work-sessions/${session.id}/end-time`),
    { timeout: 90000 },
  );
  expect(deadlineResponse.status()).toBe(200);
  const deadline = await deadlineResponse.json();
  expect(deadline.state).toEqual(initial.state);
  expect(deadline.effectiveEndAt).toBe(initial.effectiveEndAt);
  expect(
    sql(
      `SELECT '${deadline.serverNow}'::timestamptz >= '${deadline.effectiveEndAt}'::timestamptz`,
    ),
  ).toBe("t");
  await expect(
    section.getByText("Ha llegado el fin acordado", { exact: true }),
  ).toBeVisible();
  await expect(focus).toBeFocused();
  expect(counts()).toBe("0:0:0");
  await section
    .getByRole("button", { name: "Ampliar tiempo", exact: true })
    .click();
  await section.getByLabel("Minutos adicionales", { exact: true }).fill("1");
  const posting = page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      r.url().endsWith(`/work-sessions/${session.id}/extend`),
  );
  const refreshing = page.waitForResponse(
    (r) =>
      r.request().method() === "GET" &&
      r.url().endsWith(`/work-sessions/${session.id}/end-time`),
  );
  await section
    .getByRole("button", { name: "Confirmar ampliación", exact: true })
    .click();
  const posted = await posting;
  expect(posted.status()).toBe(201);
  const receipt = await posted.json();
  expect(receipt.extension.additionalMinutes).toBe(1);
  expect(receipt.extension.previousEndAt).toBe(session.plannedEndAt);
  expect(
    sql(
      `SELECT '${receipt.extension.effectiveEndAt}'::timestamptz = greatest('${receipt.extension.previousEndAt}'::timestamptz,'${receipt.occurredAt}'::timestamptz) + interval '1 minute'`,
    ),
  ).toBe("t");
  const refreshed = await refreshing;
  expect(refreshed.status()).toBe(200);
  const current = await refreshed.json();
  expect(current.state.revision).toBe("2");
  expect(current.effectiveEndAt).toBe(receipt.extension.effectiveEndAt);
  expect(
    sql(
      `SELECT '${current.serverNow}'::timestamptz < '${current.effectiveEndAt}'::timestamptz`,
    ),
  ).toBe("t");
  await expect(
    section.getByText("Ha llegado el fin acordado", { exact: true }),
  ).toHaveCount(0);
  expect(counts()).toBe("1:0:1");
});

test("end_time_notification: explicit extension from task persists in session detail @s1 @s28 @s29", async ({
  page,
  request,
}) => {
  const project = await create(request, "Ampliar el tiempo deliberadamente");
  const task = await saveTask(
    request,
    project.id,
    "Conservar el fin acordado al navegar",
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
  expect(session.id).toMatch(/^[0-9a-f-]{36}$/i);
  await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  await section
    .getByRole("button", { name: "Ampliar tiempo", exact: true })
    .click();
  const quantity = section.getByLabel("Minutos adicionales", { exact: true });
  await expect(quantity).toHaveValue("");
  expect(sql("SELECT count(*) FROM work_session_changes")).toBe("0");
  await quantity.fill("15");
  const extending = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith(`/work-sessions/${session.id}/extend`),
  );
  await section
    .getByRole("button", { name: "Confirmar ampliación", exact: true })
    .click();
  const extended = await extending;
  expect(extended.status()).toBe(201);
  expect(extended.request().postDataJSON()).toEqual({ additionalMinutes: 15 });
  const receipt = await extended.json();
  expect(Object.keys(receipt).sort()).toEqual([
    "action",
    "after",
    "before",
    "extension",
    "id",
    "occurredAt",
    "sessionId",
  ]);
  expect(receipt.action).toBe("EXTEND");
  expect(receipt.sessionId).toBe(session.id);
  expect(receipt.before.session).toEqual(session);
  expect(receipt.after).toEqual({ ...receipt.before, revision: "2" });
  expect(receipt.extension.additionalMinutes).toBe(15);
  expect(receipt.extension.previousEndAt).toBe(session.plannedEndAt);
  expect(extended.headers().location).toBe(
    `/api/v1/work-session-changes/${receipt.id}`,
  );
  expect(
    sql(
      `SELECT effective_end_at = greatest('${receipt.extension.previousEndAt}'::timestamptz,'${receipt.occurredAt}'::timestamptz) + interval '15 minutes' FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe("t");
  expect(
    sql(
      `SELECT effective_end_at = '${receipt.extension.effectiveEndAt}'::timestamptz AND planned_end_at = '${session.plannedEndAt}'::timestamptz AND revision=2 AND worked_microseconds=0 FROM work_sessions WHERE id='${session.id}'`,
    ),
  ).toBe("t");
  expect(sql("SELECT count(*) FROM work_session_intervals")).toBe("0");
  await expect(
    section.getByText("Ampliación confirmada", { exact: true }),
  ).toBeVisible();
  await section
    .locator("section")
    .filter({
      has: page.getByRole("heading", { name: "Fin de la sesión", exact: true }),
    })
    .getByRole("link", { name: "Cerrar sesión de trabajo", exact: true })
    .click();
  await expect(page).toHaveURL(
    `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`,
  );
  await expect(
    page.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
  ).toBeVisible();
  await expect(
    page.locator(`time[datetime="${receipt.extension.effectiveEndAt}"]`),
  ).toBeVisible();
  const end = await request.get(`/api/v1/work-sessions/${session.id}/end-time`);
  expect(end.status()).toBe(200);
  expect(end.headers()["work-session-revision"]).toBe(
    `work-session-${session.id}-2`,
  );
  const snapshot = await end.json();
  expect(snapshot.effectiveEndAt).toBe(receipt.extension.effectiveEndAt);
  expect(snapshot.state).toEqual(receipt.after);
  const recovered = await request.get(
    `/api/v1/work-session-changes/${receipt.id}`,
  );
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(receipt);
  expect(sql("SELECT count(*) FROM work_session_changes")).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='WorkSessionExtended.v1'",
    ),
  ).toBe("1");
});

test("end_time_notification: lost extension ACK recovers by key before pause and reload @s36 @s37 @s39", async ({
  page,
  request,
}) => {
  const project = await create(request, "Recuperar ampliación confirmada");
  const task = await saveTask(
    request,
    project.id,
    "Pausar conservando el fin recuperado",
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
  await section
    .getByRole("button", { name: "Ampliar tiempo", exact: true })
    .click();
  await section.getByLabel("Minutos adicionales", { exact: true }).fill("7");
  let resolveReceipt;
  const durable = new Promise((resolve) => {
    resolveReceipt = resolve;
  });
  let sentCount = 0;
  let key;
  await page.route(`**/work-sessions/${session.id}/extend`, async (route) => {
    sentCount++;
    key = route.request().headers()["idempotency-key"];
    const actual = await route.fetch();
    expect(actual.status()).toBe(201);
    resolveReceipt(await actual.json());
    await route.abort("connectionreset");
  });
  await section
    .getByRole("button", { name: "Confirmar ampliación", exact: true })
    .click();
  const receipt = await durable;
  expect(receipt.extension.additionalMinutes).toBe(7);
  await expect(
    section.getByText("No podemos confirmar la ampliación.", { exact: true }),
  ).toBeVisible();
  await expect(
    section.getByRole("button", { name: "Reenviar ampliación", exact: true }),
  ).toHaveCount(0);
  const found = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().endsWith(`/work-session-changes/by-request/${key}`),
  );
  await section
    .getByRole("button", { name: "Comprobar ampliación", exact: true })
    .press("Enter");
  const recovered = await found;
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(receipt);
  await expect(
    section.getByText("Ampliación confirmada", { exact: true }),
  ).toBeVisible();
  expect(sentCount).toBe(1);
  const pausing = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith(`/work-sessions/${session.id}/pause`),
  );
  await section.getByRole("button", { name: "Pausar", exact: true }).click();
  const paused = await pausing;
  expect(paused.status()).toBe(201);
  expect((await paused.json()).after.revision).toBe("3");
  await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
  await page.goto(
    `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`,
  );
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
  ).toBeVisible();
  await expect(
    page.locator(`time[datetime="${receipt.extension.effectiveEndAt}"]`),
  ).toBeVisible();
  const end = await request.get(`/api/v1/work-sessions/${session.id}/end-time`);
  expect(end.status()).toBe(200);
  const snapshot = await end.json();
  expect(snapshot.state.status).toBe("paused");
  expect(snapshot.effectiveEndAt).toBe(receipt.extension.effectiveEndAt);
  const historical = await request.get(
    `/api/v1/work-session-changes/${receipt.id}`,
  );
  expect(await historical.json()).toEqual(receipt);
  expect(
    sql(
      `SELECT count(*) FROM work_session_changes WHERE session_id='${session.id}'`,
    ),
  ).toBe("2");
  expect(
    sql(
      `SELECT count(*) FROM outbox_events WHERE aggregate_id='${session.id}' AND event_type='WorkSessionExtended.v1'`,
    ),
  ).toBe("1");
  expect(sentCount).toBe(1);
});
