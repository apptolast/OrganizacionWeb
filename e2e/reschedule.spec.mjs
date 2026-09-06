import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure, openEditor } from "./support/blocks.mjs";

test.beforeEach(() =>
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("reschedule: moving and cancelling through UI preserves original creation and historical receipts @s2 @s3 @s11 @s12 @s16 @s26 @s36 @s38 @s39", async ({
  page,
  request,
}) => {
  await configure(request);
  const project = await create(request, "Proyecto con reserva reprogramada");
  const task = await saveTask(
    request,
    project.id,
    "Resultado que sigue pendiente",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
  const objective = "Preparar un borrador revisable 🧭";
  await openEditor(page, project, task, objective);
  const creationPreview = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/preview`),
  );
  await page
    .getByRole("button", { name: "Revisar bloque", exact: true })
    .click();
  expect((await creationPreview).status()).toBe(200);
  const creationResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Guardar bloque", exact: true })
    .click();
  const created = await creationResponse;
  expect(created.status()).toBe(201);
  const original = await created.json();
  const creationKey = created.request().headers()["idempotency-key"];
  const statePath = `${endpoint}/${original.id}/state`;
  const movePreviewPath = `${endpoint}/${original.id}/reschedule/preview`;
  const movePath = `${endpoint}/${original.id}/reschedule`;

  await page
    .getByRole("button", { name: `Mover bloque: ${objective}`, exact: true })
    .click();
  const editor = page.getByRole("region", {
    name: "Mover bloque",
    exact: true,
  });
  await expect(editor.getByLabel("Inicio local", { exact: true })).toHaveValue(
    "2030-01-07T10:00",
  );
  await editor
    .getByLabel("Inicio local", { exact: true })
    .fill("2030-01-07T12:00");
  await editor
    .getByLabel("Fin local", { exact: true })
    .fill("2030-01-07T13:00");
  const previewResponse = page.waitForResponse((response) =>
    response.url().endsWith(movePreviewPath),
  );
  await editor
    .getByRole("button", { name: "Revisar movimiento", exact: true })
    .click();
  const preview = await previewResponse;
  expect(preview.status(), await preview.text()).toBe(200);
  await expect(
    editor.getByRole("region", {
      name: "Revisión del movimiento",
      exact: true,
    }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM block_changes")).toBe("0");
  const movedResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(movePath) &&
      response.request().method() === "POST",
  );
  await editor
    .getByRole("button", { name: "Confirmar movimiento", exact: true })
    .click();
  const moved = await movedResponse;
  expect(moved.status(), await moved.text()).toBe(201);
  const movement = await moved.json();
  expect(Object.keys(movement).sort()).toEqual(
    [
      "id",
      "blockId",
      "kind",
      "revision",
      "occurredAt",
      "before",
      "after",
    ].sort(),
  );
  expect(movement).toMatchObject({
    blockId: original.id,
    kind: "RESCHEDULED",
    revision: `"block:${original.id}:2"`,
    before: original,
    after: {
      ...original,
      startAt: "2030-01-07T12:00:00Z",
      endAt: "2030-01-07T13:00:00Z",
    },
  });
  expect(moved.headers().location).toBe(`${endpoint}/changes/${movement.id}`);
  await expect(
    page.getByText("Cambio confirmado (hecho histórico)", { exact: true }),
  ).toBeVisible();

  await page
    .getByRole("button", { name: `Cancelar bloque: ${objective}`, exact: true })
    .click();
  const cancellation = page.getByRole("region", {
    name: "Cancelar bloque",
    exact: true,
  });
  const cancelledResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(`${endpoint}/${original.id}/cancel`) &&
      response.request().method() === "POST",
  );
  await cancellation
    .getByRole("button", {
      name: "Confirmar cancelación del bloque",
      exact: true,
    })
    .click();
  const cancelled = await cancelledResponse;
  expect(cancelled.status(), await cancelled.text()).toBe(201);
  const receipt = await cancelled.json();
  expect(receipt).toMatchObject({
    blockId: original.id,
    kind: "CANCELLED",
    revision: `"block:${original.id}:3"`,
    before: movement.after,
    after: null,
  });
  expect(receipt.id).not.toBe(movement.id);
  expect(cancelled.headers().location).toBe(
    `${endpoint}/changes/${receipt.id}`,
  );
  await expect(
    page.getByRole("heading", { name: "Bloques planificados", exact: true }),
  ).toBeFocused();
  await expect(
    page
      .getByRole("list", { name: "Bloques planificados", exact: true })
      .getByRole("listitem"),
  ).toHaveCount(0);
  await expect(
    page.getByRole("region", { name: "Estado actual del bloque", exact: true }),
  ).toContainText("Cancelado");
  const originalResponse = await request.get(
    `${endpoint}/by-request/${creationKey}`,
  );
  expect(originalResponse.status()).toBe(200);
  expect(await originalResponse.json()).toEqual(original);

  let stateQueries = 0;
  page.on("request", (sent) => {
    if (sent.url().endsWith(statePath)) stateQueries += 1;
  });
  const historyResponse = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/changes`),
  );
  await page
    .getByRole("button", { name: "Ver cambios de bloques", exact: true })
    .click();
  const history = await historyResponse;
  expect(history.status()).toBe(200);
  expect(await history.json()).toEqual({
    items: [receipt, movement],
    nextCursor: null,
  });
  const entries = page
    .getByRole("list", { name: "Historial de bloques", exact: true })
    .getByRole("listitem");
  await expect(entries).toHaveCount(2);
  expect(stateQueries).toBe(0);
  const historicalMovement = entries.filter({
    has: page.getByRole("heading", { name: "Movimiento", exact: true }),
  });
  await historicalMovement
    .getByRole("button", { name: "Consultar estado actual", exact: true })
    .click();
  await expect(
    historicalMovement.getByRole("region", {
      name: "Estado actual del bloque",
      exact: true,
    }),
  ).toContainText("Cancelado");
  expect(stateQueries).toBe(1);
  expect(sql("SELECT count(*) FROM planned_blocks")).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM block_projections WHERE status='cancelled' AND version=3",
    ),
  ).toBe("1");
  expect(sql("SELECT count(*) FROM block_changes")).toBe("2");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockChanged.v1'",
    ),
  ).toBe("2");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockPlanned.v1'",
    ),
  ).toBe("1");
  expect(sql(`SELECT status FROM tasks WHERE id='${task.id}'`)).toBe("pending");
});
