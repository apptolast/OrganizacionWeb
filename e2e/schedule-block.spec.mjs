import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import { execFileSync } from "node:child_process";

const days = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
const blocksPath = (project, task) =>
  `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
const taskRoute = (project, task) =>
  `/proyectos/${project.id}/tareas/${task.id}`;
const fields = [
  "id",
  "projectId",
  "taskId",
  "objective",
  "startAt",
  "endAt",
  "zoneId",
  "durationMinutes",
  "createdAt",
].sort();

test.beforeEach(() =>
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

async function configure(request, minutes = 120, zoneId = "UTC") {
  const current = await request.get("/api/v1/me/availability");
  expect(current.status()).toBe(200);
  const response = await request.put("/api/v1/me/availability", {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": current.headers().etag,
    },
    data: {
      zoneId,
      dailyMinutes: Object.fromEntries(days.map((day) => [day, minutes])),
    },
  });
  expect(response.status()).toBe(200);
  return response.headers().etag;
}

async function openEditor(
  page,
  project,
  task,
  objective = "Preparar un borrador revisable 🧭",
  startLocal = "2030-01-07T10:00",
  endLocal = "2030-01-07T11:00",
) {
  await page.goto(taskRoute(project, task));
  await page
    .getByRole("button", { name: "Planificar bloque", exact: true })
    .click();
  await expect(page.getByLabel("Zona del bloque", { exact: true })).toHaveValue(
    "UTC",
  );
  await page.getByLabel("Objetivo del bloque", { exact: true }).fill(objective);
  await page.getByLabel("Inicio del bloque", { exact: true }).fill(startLocal);
  await page.getByLabel("Fin del bloque", { exact: true }).fill(endLocal);
}

async function capture(page, state) {
  const engine = page.context().browser().browserType().name();
  const directory = `.e2e-work/schedule-block-real/${engine}`;
  await mkdir(directory, { recursive: true });
  const original = page.viewportSize();
  for (const width of [320, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.evaluate(() => scrollTo(0, 0));
    await page.screenshot({
      path: `${directory}/${state}-${width}.png`,
      fullPage: true,
    });
  }
  if (original) await page.setViewportSize(original);
  await writeFile(
    `${directory}/${state}.json`,
    JSON.stringify(
      {
        state,
        engine,
        url: page.url(),
        fixture: process.env.E2E_COMPOSE_PROJECT,
        widths: [320, 1440],
        capturedAt: new Date().toISOString(),
        source: "Real API and PostgreSQL; synthetic E2E data",
      },
      null,
      2,
    ),
  );
}

test("schedule_block: real creation survives reload and exact replay preserves one block and event @s1 @s2 @s21 @s24 @s26 @s38 @s39 @s44 @s46", async ({
  page,
  request,
}) => {
  await configure(request);
  const project = await create(request, "Proyecto con bloque persistido");
  const task = await saveTask(request, project.id, "Preparar un resultado", {
    estimatedMinutes: 30,
  });
  const endpoint = blocksPath(project, task);
  const empty = await request.get(endpoint);
  expect(empty.status()).toBe(200);
  expect(await empty.json()).toEqual({ items: [], nextCursor: null });
  await openEditor(page, project, task);
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("0");
  const previewResponse = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/preview`),
  );
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  const preview = await previewResponse;
  expect(preview.status()).toBe(200);
  expect(preview.headers()["cache-control"]).toContain("no-store");
  const reviewed = await preview.json();
  expect(reviewed).toMatchObject({
    zoneId: "UTC",
    budgetZoneId: "UTC",
    startAt: "2030-01-07T10:00:00Z",
    endAt: "2030-01-07T11:00:00Z",
    durationMinutes: 60,
  });
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("0");
  await expect(
    page.getByRole("region", { name: "Revisión del bloque", exact: true }),
  ).toBeVisible();
  await capture(page, "review");
  const createResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const created = await createResponse;
  expect(created.status()).toBe(201);
  const block = await created.json();
  expect(Object.keys(block).sort()).toEqual(fields);
  expect(block).toMatchObject({
    projectId: project.id,
    taskId: task.id,
    objective: reviewed.objective,
    startAt: reviewed.startAt,
    endAt: reviewed.endAt,
    zoneId: reviewed.zoneId,
    durationMinutes: reviewed.durationMinutes,
  });
  expect(created.headers().location).toBe(`${endpoint}/${block.id}`);
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  const sent = created.request();
  const headers = await sent.allHeaders();
  const key = headers["idempotency-key"];
  expect(key).toMatch(/^[0-9a-f-]{36}$/);
  expect(headers["availability-revision"]).toBe(reviewed.availabilityEtag);
  const replay = await request.post(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "Idempotency-Key": key,
      "Availability-Revision": reviewed.availabilityEtag,
    },
    data: sent.postDataJSON(),
  });
  expect(replay.status()).toBe(200);
  expect(await replay.json()).toEqual(block);
  for (const path of [
    `${endpoint}/${block.id}`,
    `${endpoint}/by-request/${key}`,
  ]) {
    const response = await request.get(path);
    expect(response.status()).toBe(200);
    expect(response.headers()["cache-control"]).toContain("no-store");
    expect(await response.json()).toEqual(block);
  }
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  const events = JSON.parse(
    sql(
      "SELECT json_agg(payload) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  );
  expect(events).toHaveLength(1);
  expect(events[0]).toMatchObject({
    blockId: block.id,
    taskId: task.id,
    aggregateId: project.id,
    durationMinutes: 60,
  });
  expect(events[0]).not.toHaveProperty("objective");
  expect(events[0]).not.toHaveProperty("requestKey");
  expect(
    sql(
      `SELECT status || ':' || estimated_minutes FROM tasks WHERE id='${task.id}'`,
    ),
  ).toBe("pending:30");
  await page.reload();
  const list = page.getByRole("list", {
    name: "Bloques planificados",
    exact: true,
  });
  await expect(list.getByRole("listitem")).toHaveCount(1);
  await expect(
    list.getByRole("heading", { name: reviewed.objective, exact: true }),
  ).toBeVisible();
  await expect(list.locator("time").first()).toHaveAttribute(
    "datetime",
    reviewed.startAt,
  );
  await capture(page, "persisted");
});

test("schedule_block: lost creation acknowledgement recovers one committed block after completing its project @s24 @s45 @s46 @s56", async ({
  page,
  request,
}) => {
  await configure(request);
  const project = await create(request, "Proyecto con confirmación perdida");
  const task = await saveTask(request, project.id, "Resultado recuperable");
  const endpoint = blocksPath(project, task);
  let writes = 0;
  let persisted;
  let key;
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    writes++;
    key = (await route.request().allHeaders())["idempotency-key"];
    const actual = await route.fetch();
    expect(actual.status()).toBe(201);
    persisted = await actual.json();
    await route.abort("failed");
  });
  await openEditor(page, project, task);
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  await expect(
    page.getByRole("region", { name: "Revisión del bloque", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const check = page.getByRole("button", {
    name: "Comprobar guardado",
    exact: true,
  });
  await expect(check).toBeEnabled();
  await expect(page.getByText("Bloque guardado", { exact: true })).toHaveCount(
    0,
  );
  await expect(
    page.getByLabel("Objetivo del bloque", { exact: true }),
  ).toBeDisabled();
  await expect(
    page.getByLabel("Objetivo del bloque", { exact: true }),
  ).toHaveValue("Preparar un borrador revisable 🧭");
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  await page
    .getByRole("button", { name: "Marcar terminado", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Reabrir en pausa", exact: true }),
  ).toBeEnabled();
  await expect(check).toBeEnabled();
  const recoveryResponse = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/by-request/${key}`),
  );
  await check.click();
  const recovered = await recoveryResponse;
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(persisted);
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Planificar bloque", exact: true }),
  ).toHaveCount(0);
  expect(writes).toBe(1);
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("1");
  await capture(page, "recovered");
  await page.reload();
  await expect(
    page
      .getByRole("list", { name: "Bloques planificados", exact: true })
      .getByRole("listitem"),
  ).toHaveCount(1);
  await expect(
    page.getByRole("button", { name: "Planificar bloque", exact: true }),
  ).toHaveCount(0);
});

test("schedule_block: zero-minute budget requires renewed consent after editing before real creation @s15 @s39 @s42 @s43 @s44", async ({
  page,
  request,
}) => {
  await configure(request, 0);
  const project = await create(request, "Descanso con decisión explícita");
  const task = await saveTask(request, project.id, "Bloque excepcional");
  const endpoint = blocksPath(project, task);
  let writes = 0;
  page.on("request", (request) => {
    if (
      new URL(request.url()).pathname === endpoint &&
      request.method() === "POST"
    )
      writes++;
  });
  await openEditor(page, project, task);
  const review = page.getByRole("button", {
    name: "Revisar bloque",
    exact: true,
  });
  const save = page.getByRole("button", {
    name: "Guardar bloque",
    exact: true,
  });
  await review.click();
  const consent = page.getByRole("checkbox");
  await expect(consent).not.toBeChecked();
  await expect(save).toBeDisabled();
  await expect(
    page.getByText("Exceso: 3600 segundos", { exact: true }),
  ).toBeVisible();
  await expect(consent).toHaveAccessibleName(
    /aunque otras reservas aumenten el exceso antes de guardar/,
  );
  await consent.check();
  await expect(save).toBeEnabled();
  await page
    .getByLabel("Objetivo del bloque", { exact: true })
    .fill("Objetivo corregido después de aceptar");
  await expect(
    page.getByRole("region", { name: "Revisión del bloque", exact: true }),
  ).toHaveCount(0);
  await expect(save).toBeDisabled();
  expect(writes).toBe(0);
  await review.click();
  await expect(consent).not.toBeChecked();
  await consent.check();
  const responsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await save.click();
  const response = await responsePromise;
  expect(response.status()).toBe(201);
  expect(response.request().postDataJSON()).toMatchObject({
    objective: "Objetivo corregido después de aceptar",
    allowOverBudget: true,
  });
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  expect(writes).toBe(1);
  expect(
    sql("SELECT count(*) FROM planned_blocks WHERE allow_over_budget"),
  ).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("1");
});

test("schedule_block: ambiguous Madrid endpoints require explicit occurrences before persisting UTC duration @s8 @s39 @s41 @s42 @s59", async ({
  page,
  request,
}) => {
  await configure(request);
  const project = await create(request, "Horario de otoño con dos ocurrencias");
  const task = await saveTask(
    request,
    project.id,
    "Elegir un horario inequívoco",
  );
  const endpoint = blocksPath(project, task);
  await openEditor(
    page,
    project,
    task,
    "Revisar durante el cambio de hora",
    "2030-10-27T02:15",
    "2030-10-27T02:45",
  );
  await page
    .getByLabel("Zona del bloque", { exact: true })
    .selectOption("Europe/Madrid");
  const review = page.getByRole("button", {
    name: "Revisar bloque",
    exact: true,
  });
  await review.click();
  const start = page.getByLabel("Ocurrencia de inicio", { exact: true });
  await expect(start).toHaveValue("");
  await expect(start).toHaveAttribute("aria-invalid", "true");
  await expect(start).toHaveAccessibleDescription(
    "Elige una de las ocurrencias de esta hora.",
  );
  await expect(start.locator("option")).toHaveText([
    "Elige una ocurrencia",
    "UTC+02:00",
    "UTC+01:00",
  ]);
  await expect(
    page.getByLabel("Ocurrencia de fin", { exact: true }),
  ).toHaveCount(0);
  await start.selectOption("+01:00");
  await review.click();
  const end = page.getByLabel("Ocurrencia de fin", { exact: true });
  await expect(end).toHaveValue("");
  await expect(end).toHaveAttribute("aria-invalid", "true");
  await expect(end.locator("option")).toHaveText([
    "Elige una ocurrencia",
    "UTC+02:00",
    "UTC+01:00",
  ]);
  await end.selectOption("+01:00");
  await review.click();
  await expect(page.getByText("30 minutos", { exact: true })).toBeVisible();
  await start.selectOption("+02:00");
  await expect(
    page.getByRole("region", { name: "Revisión del bloque", exact: true }),
  ).toHaveCount(0);
  await expect(end).toHaveValue("+01:00");
  const previewResponse = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/preview`),
  );
  await review.click();
  const preview = await previewResponse;
  expect(preview.status()).toBe(200);
  expect(await preview.json()).toMatchObject({
    startAt: "2030-10-27T00:15:00Z",
    endAt: "2030-10-27T01:45:00Z",
    startOffset: "+02:00",
    endOffset: "+01:00",
    durationMinutes: 90,
    zoneId: "Europe/Madrid",
    budgetZoneId: "UTC",
  });
  await expect(page.getByText("90 minutos", { exact: true })).toBeVisible();
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("0");
  const createResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const created = await createResponse;
  expect(created.status()).toBe(201);
  expect(created.request().postDataJSON()).toMatchObject({
    startLocal: "2030-10-27T02:15",
    endLocal: "2030-10-27T02:45",
    startOffset: "+02:00",
    endOffset: "+01:00",
  });
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  expect(
    sql("SELECT duration_minutes || ':' || zone_id FROM planned_blocks"),
  ).toBe("90:Europe/Madrid");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("1");
});

test("schedule_block: conflicting reservation after review permits correction and explicit detail lookup @s49", async ({
  page,
  request,
}) => {
  const revision = await configure(request);
  const project = await create(request, "Plan que recibe una reserva nueva");
  const task = await saveTask(
    request,
    project.id,
    "Proteger la revisión pendiente",
  );
  const otherProject = await create(request, "Otra reserva propia");
  const otherTask = await saveTask(
    request,
    otherProject.id,
    "Preparar material separado",
  );
  const endpoint = blocksPath(project, task);
  await openEditor(page, project, task);
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  const review = page.getByRole("region", {
    name: "Revisión del bloque",
    exact: true,
  });
  await expect(review).toBeVisible();
  const reserved = await request.post(blocksPath(otherProject, otherTask), {
    headers: {
      ...(await csrfHeaders(request)),
      "Idempotency-Key": crypto.randomUUID(),
      "Availability-Revision": revision,
    },
    data: {
      objective: "Reserva confirmada en otra tarea",
      startLocal: "2030-01-07T10:00",
      endLocal: "2030-01-07T11:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
  });
  expect(reserved.status()).toBe(201);
  const conflict = await reserved.json();
  const rejectedResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const rejected = await rejectedResponse;
  expect(rejected.status()).toBe(409);
  expect(await rejected.json()).toMatchObject({
    code: "BLOCK_OVERLAP",
    conflict: {
      id: conflict.id,
      projectId: otherProject.id,
      taskId: otherTask.id,
    },
  });
  await expect(review).toHaveCount(0);
  const objective = page.getByLabel("Objetivo del bloque", { exact: true });
  await expect(objective).toBeEnabled();
  await expect(objective).toHaveValue("Preparar un borrador revisable 🧭");
  await expect(
    page.getByRole("button", { name: "Guardar bloque", exact: true }),
  ).toBeDisabled();
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  await expect(
    page.getByRole("heading", { name: conflict.objective, exact: true }),
  ).toHaveCount(0);
  const lookupResponse = page.waitForResponse((response) =>
    response
      .url()
      .endsWith(`${blocksPath(otherProject, otherTask)}/${conflict.id}`),
  );
  await page
    .getByRole("button", { name: "Consultar bloque en conflicto", exact: true })
    .click();
  expect((await lookupResponse).status()).toBe(200);
  await expect(
    page.getByRole("heading", { name: conflict.objective, exact: true }),
  ).toBeVisible();
  await page
    .getByLabel("Inicio del bloque", { exact: true })
    .fill("2030-01-07T11:00");
  await page
    .getByLabel("Fin del bloque", { exact: true })
    .fill("2030-01-07T12:00");
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  await expect(review).toBeVisible();
  const createdResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const created = await createdResponse;
  expect(created.status()).toBe(201);
  expect((await created.request().allHeaders())["idempotency-key"]).not.toBe(
    (await rejected.request().allHeaders())["idempotency-key"],
  );
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("2");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("2");
});

test("schedule_block: newly consumed budget rejects stale review and requires explicit consent @s16 @s43 @s49", async ({
  page,
  request,
}) => {
  const revision = await configure(request);
  const project = await create(
    request,
    "Capacidad consumida después de revisar",
  );
  const task = await saveTask(
    request,
    project.id,
    "Revisar capacidad antes de aceptar",
  );
  const otherTask = await saveTask(
    request,
    project.id,
    "Reserva de noventa minutos",
  );
  const endpoint = blocksPath(project, task);
  await openEditor(page, project, task);
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  const review = page.getByRole("region", {
    name: "Revisión del bloque",
    exact: true,
  });
  await expect(review).toBeVisible();
  await expect(page.getByRole("checkbox")).toHaveCount(0);
  const reserved = await request.post(blocksPath(project, otherTask), {
    headers: {
      ...(await csrfHeaders(request)),
      "Idempotency-Key": crypto.randomUUID(),
      "Availability-Revision": revision,
    },
    data: {
      objective: "Consumo real de noventa minutos",
      startLocal: "2030-01-07T08:00",
      endLocal: "2030-01-07T09:30",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
  });
  expect(reserved.status()).toBe(201);
  const rejectedResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const rejected = await rejectedResponse;
  expect(rejected.status()).toBe(409);
  expect(await rejected.json()).toMatchObject({
    code: "BUDGET_EXCEEDED",
    budgetZoneId: "UTC",
    days: [
      {
        date: "2030-01-07",
        budgetMinutes: 120,
        plannedSeconds: 5400,
        requestedSeconds: 3600,
        excessSeconds: 1800,
      },
    ],
  });
  await expect(
    page.getByText(
      "El presupuesto cambió. Revisa este bloque de nuevo antes de decidir.",
      { exact: true },
    ),
  ).toBeVisible();
  await expect(review).toHaveCount(0);
  await expect(
    page.getByLabel("Objetivo del bloque", { exact: true }),
  ).toBeEnabled();
  await expect(
    page.getByRole("button", { name: "Guardar bloque", exact: true }),
  ).toBeDisabled();
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  await expect(
    review.getByText("Reservado: 5400 segundos", { exact: true }),
  ).toBeVisible();
  await expect(
    review.getByText("Exceso: 1800 segundos", { exact: true }),
  ).toBeVisible();
  const consent = page.getByRole("checkbox");
  await expect(consent).not.toBeChecked();
  await expect(
    page.getByRole("button", { name: "Guardar bloque", exact: true }),
  ).toBeDisabled();
  await consent.check();
  const createdResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const created = await createdResponse;
  expect(created.status()).toBe(201);
  expect(created.request().postDataJSON().allowOverBudget).toBe(true);
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("2");
  expect(
    sql("SELECT count(*) FROM planned_blocks WHERE allow_over_budget"),
  ).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("2");
});

test("schedule_block: lost acknowledgement recovers persisted block after real backend restart @s35", async ({
  page,
  request,
}) => {
  test.setTimeout(90_000);
  const fixture = process.env.E2E_COMPOSE_PROJECT;
  if (
    !/^organizationweb-e2e-\d+$/.test(fixture ?? "") ||
    !process.env.E2E_ENV_FILE
  )
    throw new Error("Restart requires the isolated E2E fixture");
  const compose = (...args) =>
    execFileSync(
      "docker",
      [
        "compose",
        "--env-file",
        process.env.E2E_ENV_FILE,
        "-p",
        fixture,
        "-f",
        "docker-compose.yml",
        ...args,
      ],
      { encoding: "utf8", timeout: 30_000 },
    ).trim();
  const inspect = (id) =>
    JSON.parse(
      execFileSync("docker", ["inspect", id], {
        encoding: "utf8",
        timeout: 10_000,
      }),
    )[0];
  await configure(request);
  const project = await create(
    request,
    "Confirmación conservada tras reinicio real",
  );
  const task = await saveTask(
    request,
    project.id,
    "Recuperar el mismo bloque persistido",
  );
  const endpoint = blocksPath(project, task);
  let key;
  let persisted;
  let writes = 0;
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    writes++;
    key = (await route.request().allHeaders())["idempotency-key"];
    const actual = await route.fetch();
    expect(actual.status()).toBe(201);
    persisted = await actual.json();
    await route.abort("failed");
  });
  await openEditor(page, project, task);
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  await expect(
    page.getByRole("region", { name: "Revisión del bloque", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const check = page.getByRole("button", {
    name: "Comprobar guardado",
    exact: true,
  });
  await expect(check).toBeEnabled();
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("1");
  const backendId = compose("ps", "-q", "backend");
  const databaseId = compose("ps", "-q", "postgres");
  const beforeBackend = inspect(backendId);
  const beforeDatabase = inspect(databaseId);
  compose("restart", "backend");
  await expect
    .poll(
      async () => {
        try {
          return (
            await request.get("/api/session", { timeout: 2000 })
          ).status();
        } catch {
          return 0;
        }
      },
      { timeout: 45_000, intervals: [500, 1000] },
    )
    .toBe(200);
  const afterBackend = inspect(backendId);
  const afterDatabase = inspect(databaseId);
  expect(afterBackend.State.Running).toBe(true);
  expect(afterBackend.State.StartedAt).not.toBe(beforeBackend.State.StartedAt);
  expect(afterDatabase.State.StartedAt).toBe(beforeDatabase.State.StartedAt);
  expect(afterDatabase.Mounts).toEqual(beforeDatabase.Mounts);
  const session = await (await request.get("/api/session")).json();
  if (!session.authenticated)
    await loginSession(request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
  const recoveryResponse = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/by-request/${key}`),
  );
  await check.click();
  const recovered = await recoveryResponse;
  expect(recovered.status()).toBe(200);
  expect(Object.keys(await recovered.json()).sort()).toEqual(fields);
  expect(await recovered.json()).toEqual(persisted);
  const detail = await request.get(`${endpoint}/${persisted.id}`);
  expect(detail.status()).toBe(200);
  expect(await detail.json()).toEqual(persisted);
  const list = await request.get(endpoint);
  expect(list.status()).toBe(200);
  expect(await list.json()).toEqual({ items: [persisted], nextCursor: null });
  await expect(
    page.getByText("Bloque guardado", { exact: true }),
  ).toBeVisible();
  expect(writes).toBe(1);
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("1");
  await capture(page, "restart-recovered");
  const engine = page.context().browser().browserType().name();
  await writeFile(
    `.e2e-work/schedule-block-real/${engine}/restart-proof.json`,
    JSON.stringify(
      {
        fixture,
        backendId,
        databaseId,
        beforeBackendStartedAt: beforeBackend.State.StartedAt,
        afterBackendStartedAt: afterBackend.State.StartedAt,
        databaseStartedAt: afterDatabase.State.StartedAt,
        databaseMounts: afterDatabase.Mounts.map(
          ({ Type, Name, Destination }) => ({ Type, Name, Destination }),
        ),
        sessionReauthenticated: !session.authenticated,
        writes,
        blockId: persisted.id,
        recoveredStatus: recovered.status(),
        capturedAt: new Date().toISOString(),
      },
      null,
      2,
    ),
  );
});
