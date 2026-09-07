import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";

test.beforeEach(() =>
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

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
