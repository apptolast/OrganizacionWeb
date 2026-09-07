import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { restartBackend } from "./support/backend.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";

test("weekly review: explicit selection and browser Back preserve the applied date and zone @s28", async ({
  page,
}) => {
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const readings = [];
  page.on("request", (request) => {
    if (new URL(request.url()).pathname === "/api/v1/weekly-review")
      readings.push(request.url());
  });
  await page.goto("/revision-semanal?date=2020-01-08&zoneId=UTC");
  await expect(
    page.getByRole("heading", { name: "2020-01-06", exact: true }),
  ).toBeVisible();
  const beforeEdit = readings.length;
  await page
    .getByLabel("Fecha de la semana", { exact: true })
    .fill("2020-02-06");
  expect(readings).toHaveLength(beforeEdit);
  await page
    .getByRole("button", { name: "Mostrar semana", exact: true })
    .click();
  await expect(page).toHaveURL(/date=2020-02-06&zoneId=UTC$/);
  await expect(
    page.getByRole("heading", { name: "2020-02-03", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", {
      name: "Revisión semanal",
      level: 1,
      exact: true,
    }),
  ).toBeFocused();
  await page
    .getByRole("link", { name: "Semana anterior", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "2020-01-27", exact: true }),
  ).toBeVisible();
  await page.goBack();
  await expect(
    page.getByRole("heading", { name: "2020-02-03", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByLabel("Fecha de la semana", { exact: true }),
  ).toHaveValue("2020-02-06");
  await page.reload();
  await expect(
    page.getByRole("heading", { name: "2020-02-03", exact: true }),
  ).toBeVisible();
  await expect(page.getByLabel("Zona horaria", { exact: true })).toHaveValue(
    "UTC",
  );
  const current = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === "/api/v1/weekly-review" &&
      !new URL(response.url()).searchParams.has("date"),
  );
  await page.getByRole("link", { name: "Esta semana", exact: true }).click();
  const response = await current;
  expect(response.status()).toBe(200);
  const snapshot = await response.json();
  await expect(page).toHaveURL(/\/revision-semanal\?zoneId=UTC$/);
  await expect(
    page.getByRole("heading", { name: snapshot.weekStart, exact: true }),
  ).toBeVisible();
});

test("weekly review: opens seven empty days from the existing navigation @s1 @s27", async ({
  page,
}) => {
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await page.goto("/proyectos");
  const link = page
    .getByRole("navigation", { name: "Principal", exact: true })
    .getByRole("link", { name: "Revisión semanal", exact: true });
  await expect(link).toHaveAttribute("href", "/revision-semanal");
  const reading = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      new URL(response.url()).pathname === "/api/v1/weekly-review",
  );
  await link.click();
  const response = await reading;
  expect(response.status(), await response.text()).toBe(200);
  expect(response.headers()["cache-control"]).toContain("no-store");
  const snapshot = await response.json();
  expect(snapshot.zoneId).toBe("UTC");
  expect(snapshot.zoneSource).toBe("UNCONFIGURED");
  expect(snapshot.availabilityZoneId).toBeNull();
  expect(snapshot.days).toHaveLength(7);
  expect(snapshot.totals).toEqual({
    plannedMicroseconds: "0",
    workedMicroseconds: "0",
    capacityMicroseconds: null,
  });
  expect(snapshot.unquantifiedSessionCount).toBe("0");
  await expect(page).toHaveURL(/\/revision-semanal$/);
  await expect(
    page.getByRole("heading", {
      level: 1,
      name: "Revisión semanal",
      exact: true,
    }),
  ).toBeFocused();
  const days = page.getByRole("list", {
    name: "Días de la semana",
    exact: true,
  });
  await expect(days.getByRole("listitem")).toHaveCount(7);
  for (const day of snapshot.days) {
    expect(day.plannedMicroseconds).toBe("0");
    expect(day.workedMicroseconds).toBe("0");
    expect(day.capacityMicroseconds).toBeNull();
    await expect(
      days.getByRole("heading", { level: 2, name: day.date, exact: true }),
    ).toBeVisible();
  }
  await expect(page.getByText(/Disponibilidad no configurada/)).toBeVisible();
  await expect(
    page.getByRole("link", { name: "Ver planificación", exact: true }),
  ).toHaveAttribute("href", "/proyectos");
  expect(sql("SELECT count(*) FROM outbox_events")).toBe("0");
});

test("weekly review: past plan and closed work survive own outbox removal and API restart @s24 @s28", async ({
  page,
  request,
}) => {
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(request, "Semana durable");
  const task = await saveTask(
    request,
    project.id,
    "Trabajo registrado, no logro supuesto",
  );
  const availability = await configure(request, 120, "UTC");
  const taskPath = `/api/v1/projects/${project.id}/tasks/${task.id}`;
  async function post(path, data, headers = {}) {
    const response = await request.post(path, {
      headers: {
        ...(await csrfHeaders(request)),
        "Idempotency-Key": randomUUID(),
        ...headers,
      },
      data,
    });
    expect(response.status(), await response.text()).toBe(201);
    return response.json();
  }
  const block = await post(
    `${taskPath}/blocks`,
    {
      objective: "Una hora reservada",
      startLocal: "2030-01-07T09:00",
      endLocal: "2030-01-07T10:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
    { "Availability-Revision": availability },
  );
  const session = await post(`${taskPath}/work-sessions`, {
    plannedMinutes: 25,
  });
  const closed = await post(
    `/api/v1/work-sessions/${session.id}/close`,
    { progressNote: "Hecho conservado", nextStep: "Revisar" },
    { "Work-Session-Revision": `work-session-${session.id}-1` },
  );
  // The isolated historical fixture relocates the real closure consistently; the production clock is not changed.
  const receipt = JSON.parse(
    sql(`SELECT receipt FROM work_session_changes WHERE id='${closed.id}'`),
  );
  for (const state of [receipt.before, receipt.after]) {
    state.session.startedAt = "2020-01-06T09:00:00Z";
    state.session.plannedEndAt = "2020-01-06T09:25:00Z";
  }
  receipt.before.changedAt = receipt.before.runningSince =
    "2020-01-06T09:00:00Z";
  receipt.before.workedMicroseconds = 0;
  receipt.after.changedAt = receipt.occurredAt = "2020-01-06T09:30:00Z";
  receipt.after.workedMicroseconds = 1800000000;
  receipt.after.runningSince = null;
  receipt.closure.workDate = "2020-01-06";
  const receiptLiteral = JSON.stringify(receipt).replaceAll("'", "''");
  sql(
    `BEGIN; UPDATE planned_blocks SET start_local='2020-01-06T09:00',end_local='2020-01-06T10:00',start_at='2020-01-06T09:00Z',end_at='2020-01-06T10:00Z' WHERE id='${block.id}'; UPDATE work_sessions SET started_at='2020-01-06T09:00Z', planned_end_at='2020-01-06T09:25Z', effective_end_at='2020-01-06T09:25Z', changed_at='2020-01-06T09:30Z', worked_microseconds=1800000000 WHERE id='${session.id}'; UPDATE work_session_intervals SET start_at='2020-01-06T09:00Z', end_at='2020-01-06T09:30Z' WHERE session_id='${session.id}'; UPDATE work_session_changes SET occurred_at='2020-01-06T09:30Z',receipt='${receiptLiteral}'::jsonb WHERE id='${closed.id}'; COMMIT;`,
  );
  const durableRows = () =>
    sql(
      `SELECT jsonb_build_object('session',(SELECT to_jsonb(s) FROM work_sessions s WHERE id='${session.id}'),'intervals',(SELECT jsonb_agg(to_jsonb(i) ORDER BY revision) FROM work_session_intervals i WHERE session_id='${session.id}'),'receipt',(SELECT to_jsonb(c) FROM work_session_changes c WHERE id='${closed.id}'),'block',(SELECT to_jsonb(b) FROM planned_blocks b WHERE id='${block.id}'))`,
    );
  const rowsBefore = durableRows();
  const path = "/api/v1/weekly-review?date=2020-01-08&zoneId=UTC";
  const first = await request.get(path);
  expect(first.status(), await first.text()).toBe(200);
  const before = await first.json();
  expect(before.weekStart).toBe("2020-01-06");
  expect(before.totals).toEqual({
    plannedMicroseconds: "3600000000",
    workedMicroseconds: "1800000000",
    capacityMicroseconds: "50400000000",
  });
  expect(before.days[0]).toMatchObject({
    date: "2020-01-06",
    plannedMicroseconds: "3600000000",
    workedMicroseconds: "1800000000",
    capacityMicroseconds: "7200000000",
  });
  expect(
    before.days
      .slice(1)
      .every(
        (day) =>
          day.plannedMicroseconds === "0" && day.workedMicroseconds === "0",
      ),
  ).toBe(true);
  // Only transport records belonging to this fixture are removed; durable facts remain.
  sql(
    `DELETE FROM outbox_events WHERE aggregate_id IN ('${project.id}','${task.id}','${block.id}','${session.id}')`,
  );
  const transportAfterRemoval = sql("SELECT count(*) FROM outbox_events");
  await restartBackend(request);
  const second = await request.get(path);
  expect(second.status(), await second.text()).toBe(200);
  const after = await second.json();
  expect({ ...after, serverNow: before.serverNow }).toEqual(before);
  expect(after.serverNow > before.serverNow).toBe(true);
  expect(durableRows()).toBe(rowsBefore);
  expect(sql("SELECT count(*) FROM outbox_events")).toBe(transportAfterRemoval);
  await page.goto("/revision-semanal?date=2020-01-08&zoneId=UTC");
  const monday = page.getByRole("listitem").filter({
    has: page.getByRole("heading", { name: "2020-01-06", exact: true }),
  });
  await expect(
    monday.getByText("Plan vigente: 1 h", { exact: true }),
  ).toBeVisible();
  await expect(
    monday.getByText("Trabajo registrado: 30 min", { exact: true }),
  ).toBeVisible();
  await expect(page.getByText(/no indica tareas terminadas/)).toBeVisible();
  await page.reload();
  await expect(
    monday.getByText("Trabajo registrado: 30 min", { exact: true }),
  ).toBeVisible();
  expect(durableRows()).toBe(rowsBefore);
});
