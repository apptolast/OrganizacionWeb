import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";
import { restartBackend } from "./support/backend.mjs";
import { mkdir, writeFile } from "node:fs/promises";

test("history: workspace opens an empty real history page @s28", async ({
  page,
}) => {
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  await page.goto("/proyectos");
  const link = page
    .getByRole("navigation", { name: "Principal", exact: true })
    .getByRole("link", { name: "Historial", exact: true });
  await expect(link).toBeVisible();
  await expect(link).toHaveAttribute("href", "/historial");
  const reading = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      new URL(response.url()).pathname === "/api/v1/history",
  );
  await link.click();
  await expect(page).toHaveURL(/\/historial$/);
  await expect(
    page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
  ).toBeVisible();
  const response = await reading;
  expect(response.status()).toBe(200);
  expect(response.headers()["cache-control"]).toContain("no-store");
  expect(await response.json()).toEqual({ items: [], nextCursor: null });
  await expect(
    page.getByText("Todavía no hay hechos en tu historial.", { exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM outbox_events")).toBe("0");
});

test("history: five durable sources keep their original details after later changes @s1 @s2 @s32 @s33", async ({
  page,
  request,
}) => {
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(request, "Hechos durables de trabajo");
  const task = await saveTask(request, project.id, "Conservar lo ocurrido");
  const taskPath = `/api/v1/projects/${project.id}/tasks/${task.id}`;
  const availability = await configure(request);
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
  const original = await post(
    `${taskPath}/blocks`,
    {
      objective: "Reserva original",
      startLocal: "2030-01-07T10:00",
      endLocal: "2030-01-07T11:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
    { "Availability-Revision": availability },
  );
  const moved = await post(
    `${taskPath}/blocks/${original.id}/reschedule`,
    {
      startLocal: "2030-01-07T12:00",
      endLocal: "2030-01-07T13:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
    {
      "If-Match": `"block:${original.id}:1"`,
      "Availability-Revision": availability,
    },
  );
  const cancelled = await post(
    `${taskPath}/blocks/${original.id}/cancel`,
    {},
    { "If-Match": moved.revision },
  );
  const session = await post(`${taskPath}/work-sessions`, {
    plannedMinutes: 25,
  });
  const receipts = [];
  let revision = "1";
  const note = "  Avance conservado\n<script>no ejecutar</script> 🙂";
  for (const [action, data] of [
    ["pause", {}],
    ["resume", {}],
    ["extend", { additionalMinutes: 15 }],
    ["close", { progressNote: note, nextStep: " Revisar mañana " }],
  ]) {
    const receipt = await post(
      `/api/v1/work-sessions/${session.id}/${action}`,
      data,
      {
        "Work-Session-Revision": `work-session-${session.id}-${revision}`,
      },
    );
    revision = receipt.after.revision;
    receipts.push(receipt);
  }
  let status = await request.get(`${taskPath}/status`);
  expect(status.status()).toBe(200);
  for (const target of ["completed", "pending"]) {
    status = await request.put(`${taskPath}/status`, {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": status.headers().etag,
      },
      data: { status: target },
    });
    expect(status.status(), await status.text()).toBe(200);
  }
  const projectState = await request.get(`/api/v1/projects/${project.id}`);
  expect(projectState.status()).toBe(200);
  const completedProject = await request.put(
    `/api/v1/projects/${project.id}/status`,
    {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": projectState.headers().etag,
      },
      data: { status: "completed" },
    },
  );
  expect(completedProject.status(), await completedProject.text()).toBe(200);
  const local = await request.get(`${taskPath}/history`);
  expect(local.status()).toBe(200);
  const localHistory = (await local.json()).items;
  expect(localHistory).toHaveLength(2);
  const requests = [];
  page.on("request", (request) => {
    const path = new URL(request.url()).pathname;
    if (path.startsWith("/api/v1/"))
      requests.push({ method: request.method(), path });
  });
  const reading = page.waitForResponse(
    (r) => new URL(r.url()).pathname === "/api/v1/history",
  );
  const appearanceReading = page.waitForResponse(
    (r) => new URL(r.url()).pathname === "/api/v1/me/appearance",
  );
  await page.goto("/historial");
  expect((await appearanceReading).status()).toBe(200);
  const response = await reading;
  expect(response.status(), await response.text()).toBe(200);
  const history = await response.json();
  expect(history.items).toHaveLength(10);
  expect(history.nextCursor).toBeNull();
  expect(new Set(history.items.map((item) => item.type))).toEqual(
    new Set([
      "BLOCK_PLANNED",
      "BLOCK_CHANGED",
      "TASK_STATUS_CHANGED",
      "SESSION_STARTED",
      "SESSION_CHANGED",
    ]),
  );
  const detail = (type, id) =>
    history.items.find((item) => item.type === type && item.id === id)?.details;
  expect(detail("BLOCK_PLANNED", original.id)).toEqual(original);
  expect(detail("SESSION_STARTED", session.id)).toEqual(session);
  for (const receipt of [moved, cancelled])
    expect(detail("BLOCK_CHANGED", receipt.id)).toEqual(receipt);
  for (const receipt of receipts)
    expect(detail("SESSION_CHANGED", receipt.id)).toEqual(receipt);
  for (const item of localHistory)
    expect(detail("TASK_STATUS_CHANGED", item.id)).toEqual(item);
  const list = page.getByRole("list", {
    name: "Hechos del historial",
    exact: true,
  });
  await expect(list.getByRole("listitem")).toHaveCount(10);
  for (const label of [
    "Tiempo reservado",
    "Reserva replanificada",
    "Reserva cancelada",
    "Sesión iniciada",
    "Sesión pausada",
    "Sesión reanudada",
    "Tiempo ampliado",
    "Sesión cerrada",
    "Tarea completada",
    "Tarea reabierta",
  ]) {
    await expect(
      list.getByRole("heading", { name: label, exact: true }),
    ).toBeVisible();
  }
  const closed = list.getByRole("listitem").filter({
    has: page.getByRole("heading", { name: "Sesión cerrada", exact: true }),
  });
  await closed.getByText("Ver detalles del hecho", { exact: true }).click();
  const storedNote = closed
    .locator(".history-note")
    .filter({ hasText: "<script>no ejecutar</script>" });
  await expect(storedNote).toBeVisible();
  expect(await storedNote.textContent()).toBe(note);
  await expect(
    closed.getByText(`Día atribuido: ${receipts.at(-1).closure.workDate}`, {
      exact: true,
    }),
  ).toBeVisible();
  await expect(closed.locator("script")).toHaveCount(0);
  const planned = list.getByRole("listitem").filter({
    has: page.getByRole("heading", { name: "Tiempo reservado", exact: true }),
  });
  await planned.getByText("Ver detalles del hecho", { exact: true }).click();
  await expect(
    planned.getByText("Una reserva no acredita tiempo trabajado.", {
      exact: true,
    }),
  ).toBeVisible();
  expect([...requests].sort((a, b) => a.path.localeCompare(b.path))).toEqual([
    { method: "GET", path: "/api/v1/history" },
    { method: "GET", path: "/api/v1/me/appearance" },
  ]);
  expect(
    sql(
      "SELECT (SELECT count(*) FROM planned_blocks) + (SELECT count(*) FROM block_changes) + (SELECT count(*) FROM task_status_history) + (SELECT count(*) FROM work_sessions) + (SELECT count(*) FROM work_session_changes)",
    ),
  ).toBe("10");
  await closed
    .getByRole("link", { name: "Ver sesión de trabajo", exact: true })
    .click();
  await expect(page).toHaveURL(
    `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`,
  );
  await expect(
    page.getByRole("heading", {
      name: "Sesión de trabajo",
      level: 1,
      exact: true,
    }),
  ).toBeVisible();
});

test("history: pagination filters and reload survive API restart without fixture outbox @s14 @s24 @s29 @s30 @s31", async ({
  page,
  request,
}) => {
  test.setTimeout(120000);
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(request, "Veintiún hechos verificables");
  const task = await saveTask(request, project.id, "Paginar hechos propios");
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/status`;
  let state = await request.get(endpoint);
  expect(state.status()).toBe(200);
  for (let index = 0; index < 21; index++) {
    state = await request.put(endpoint, {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": state.headers().etag,
      },
      data: { status: index % 2 === 0 ? "completed" : "pending" },
    });
    expect(state.status(), await state.text()).toBe(200);
  }
  async function readAfter(action) {
    const response = page.waitForResponse(
      (r) =>
        r.request().method() === "GET" &&
        new URL(r.url()).pathname === "/api/v1/history",
    );
    await action();
    const result = await response;
    expect(result.status(), await result.text()).toBe(200);
    return result.json();
  }
  const initialUrl = "/historial?category=task-status";
  const recent = await readAfter(() => page.goto(initialUrl));
  expect(recent.items).toHaveLength(20);
  expect(typeof recent.nextCursor).toBe("string");
  const list = page.getByRole("list", {
    name: "Hechos del historial",
    exact: true,
  });
  await expect(list.getByRole("listitem")).toHaveCount(20);
  const older = await readAfter(() =>
    page.getByRole("link", { name: "Más antiguos", exact: true }).click(),
  );
  expect(older.items).toHaveLength(1);
  expect(older.nextCursor).toBeNull();
  await expect(list.getByRole("listitem")).toHaveCount(1);
  const olderUrl = page.url();
  expect(new URL(olderUrl).searchParams.get("cursor")).toBe(recent.nextCursor);
  expect(new URL(olderUrl).searchParams.get("category")).toBe("task-status");
  const keys = [...recent.items, ...older.items].map(
    (item) => `${item.type}:${item.id}`,
  );
  expect(new Set(keys).size).toBe(21);
  expect(await readAfter(() => page.reload())).toEqual(older);
  expect(await readAfter(() => page.goBack())).toEqual(recent);
  await expect(page).toHaveURL(initialUrl);
  expect(await readAfter(() => page.goForward())).toEqual(older);
  const outboxFilter = `event_type='TaskStatusChanged.v1' AND payload->>'taskId'='${task.id}'`;
  expect(sql(`SELECT count(*) FROM outbox_events WHERE ${outboxFilter}`)).toBe(
    "21",
  );
  sql(`DELETE FROM outbox_events WHERE ${outboxFilter}`);
  const proof = await restartBackend(request);
  expect(await readAfter(() => page.reload())).toEqual(older);
  await expect(page).toHaveURL(olderUrl);
  expect(sql(`SELECT count(*) FROM outbox_events WHERE ${outboxFilter}`)).toBe(
    "0",
  );
  expect(
    await readAfter(() =>
      page
        .getByRole("link", { name: "Volver a recientes", exact: true })
        .click(),
    ),
  ).toEqual(recent);
  await expect(page).toHaveURL(initialUrl);
  const day = recent.items[0].occurredAt.slice(0, 10);
  await page.getByLabel("Categoría", { exact: true }).selectOption("planning");
  await page.getByLabel("Desde (UTC)", { exact: true }).fill(day);
  await page.getByLabel("Hasta (UTC)", { exact: true }).fill(day);
  const filtered = await readAfter(() =>
    page.getByRole("button", { name: "Aplicar filtros", exact: true }).click(),
  );
  expect(filtered).toEqual({ items: [], nextCursor: null });
  await expect(
    page.getByText("No hay hechos con estos filtros.", { exact: true }),
  ).toBeVisible();
  const query = new URL(page.url()).searchParams;
  expect([...query.entries()].sort()).toEqual([
    ["category", "planning"],
    ["from", day],
    ["to", day],
  ]);
  const unfiltered = await readAfter(() =>
    page.getByRole("link", { name: "Limpiar filtros", exact: true }).click(),
  );
  await expect(page).toHaveURL("/historial");
  expect(unfiltered.items).toEqual(recent.items);
  await expect(list.getByRole("listitem")).toHaveCount(20);
  expect(
    sql(`SELECT count(*) FROM task_status_history WHERE task_id='${task.id}'`),
  ).toBe("21");
  await mkdir(".e2e-work/history-real", { recursive: true });
  await writeFile(
    ".e2e-work/history-real/pagination.json",
    JSON.stringify(
      {
        recent,
        older,
        keys,
        filtered,
        restart: {
          backendId: proof.backendId,
          databaseId: proof.databaseId,
          beforeBackend: proof.beforeBackend.State.StartedAt,
          afterBackend: proof.afterBackend.State.StartedAt,
          beforeDatabase: proof.beforeDatabase.State.StartedAt,
          afterDatabase: proof.afterDatabase.State.StartedAt,
        },
      },
      null,
      2,
    ),
  );
});

test("history: current errors withdraw private notes and retry only the requested read @s34 @s35 @s37", async ({
  page,
  request,
  context,
}) => {
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const project = await create(request, "Contexto privado del historial");
  const task = await saveTask(
    request,
    project.id,
    "Notas privadas conservadas",
  );
  await configure(request);
  const headers = await csrfHeaders(request);
  const start = await request.post(
    `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`,
    {
      headers: { ...headers, "Idempotency-Key": randomUUID() },
      data: { plannedMinutes: 25 },
    },
  );
  expect(start.status(), await start.text()).toBe(201);
  const session = await start.json();
  const note =
    "Nota privada de cierre: no debe persistir bajo una respuesta de error";
  const close = await request.post(
    `/api/v1/work-sessions/${session.id}/close`,
    {
      headers: {
        ...headers,
        "Idempotency-Key": randomUUID(),
        "Work-Session-Revision": `work-session-${session.id}-1`,
      },
      data: { progressNote: note },
    },
  );
  expect(close.status(), await close.text()).toBe(201);
  const path = `/historial?projectId=${project.id}&category=sessions`;
  await page.goto(path);
  const list = page.getByRole("list", { name: "Hechos del historial" });
  await expect(list.getByRole("listitem")).toHaveCount(2);
  const closed = list.getByRole("listitem").filter({
    has: page.getByRole("heading", { name: "Sesión cerrada", exact: true }),
  });
  await closed.locator("summary").click();
  await expect(page.getByText(note, { exact: true })).toBeVisible();
  const durableBefore = sql(
    "SELECT (SELECT count(*) FROM work_sessions) || ':' || (SELECT count(*) FROM work_session_changes) || ':' || (SELECT count(*) FROM outbox_events)",
  );
  const reads = [];
  page.on("request", (req) => {
    if (new URL(req.url()).pathname === "/api/v1/history")
      reads.push({ method: req.method(), query: new URL(req.url()).search });
  });
  await page.route(
    "**/api/v1/history?**",
    (route) =>
      route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Unavailable",
          status: 503,
          code: "STORAGE_UNAVAILABLE",
        }),
      }),
    { times: 1 },
  );
  await page.getByLabel("Categoría", { exact: true }).selectOption("planning");
  await page
    .getByRole("button", { name: "Aplicar filtros", exact: true })
    .click();
  await expect(page.getByRole("alert")).toHaveText(
    "No hemos podido consultar el historial.",
  );
  await expect(list).toHaveCount(0);
  await expect(page.getByText(note, { exact: true })).toHaveCount(0);
  const failedUrl = page.url();
  await page
    .getByRole("button", { name: "Reintentar consulta", exact: true })
    .click();
  await expect(
    page.getByText("No hay hechos con estos filtros.", { exact: true }),
  ).toBeVisible();
  expect(page.url()).toBe(failedUrl);
  expect(reads).toHaveLength(2);
  expect(reads[0]).toEqual(reads[1]);
  expect(reads[0].method).toBe("GET");
  await page.goto(`/historial?projectId=${randomUUID()}&category=sessions`);
  await expect(page.getByRole("alert")).toHaveText(
    "Este proyecto o tarea no está disponible para tu cuenta.",
  );
  await expect(list).toHaveCount(0);
  await page
    .getByRole("link", { name: "Quitar filtro de contexto", exact: true })
    .click();
  await expect(list.getByRole("listitem")).toHaveCount(2);
  await context.clearCookies();
  const revoked = page.waitForResponse(
    (response) =>
      new URL(response.url()).pathname === "/api/v1/history" &&
      response.status() === 401,
  );
  await page
    .getByLabel("Categoría", { exact: true })
    .selectOption("task-status");
  await page
    .getByRole("button", { name: "Aplicar filtros", exact: true })
    .click();
  await expect(list).toHaveCount(0);
  await expect(page.getByText(note, { exact: true })).toHaveCount(0);
  expect((await revoked).status()).toBe(401);
  await expect(
    page.getByRole("button", { name: "Iniciar sesión", exact: true }),
  ).toBeVisible();
  expect(
    sql(
      "SELECT (SELECT count(*) FROM work_sessions) || ':' || (SELECT count(*) FROM work_session_changes) || ':' || (SELECT count(*) FROM outbox_events)",
    ),
  ).toBe(durableBefore);
});
