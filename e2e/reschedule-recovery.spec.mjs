import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { restartBackend } from "./support/backend.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";

test.beforeEach(() =>
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("reschedule: lost movement ACK survives real restart and later cancellation without outbox retention @s25", async ({
  page,
  request,
}) => {
  test.setTimeout(90_000);
  const availability = await configure(request);
  const project = await create(
    request,
    "Recibo histórico después del reinicio",
  );
  const task = await saveTask(
    request,
    project.id,
    "Conservar intención y confirmación",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
  const created = await request.post(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "Availability-Revision": availability,
      "Idempotency-Key": randomUUID(),
    },
    data: {
      objective: "Recuperar movimiento confirmado",
      startLocal: "2030-01-07T10:00",
      endLocal: "2030-01-07T11:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  const original = await created.json();
  const movePath = `${endpoint}/${original.id}/reschedule`;
  let key, persisted;
  let writes = 0;
  await page.route(`**${movePath}`, async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    writes += 1;
    key = (await route.request().allHeaders())["idempotency-key"];
    const actual = await route.fetch();
    expect(actual.status(), await actual.text()).toBe(201);
    persisted = await actual.json();
    await route.abort("failed");
  });
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  await page
    .getByRole("button", {
      name: `Mover bloque: ${original.objective}`,
      exact: true,
    })
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
  await editor
    .getByRole("button", { name: "Revisar movimiento", exact: true })
    .click();
  await expect(
    editor.getByRole("region", {
      name: "Revisión del movimiento",
      exact: true,
    }),
  ).toBeVisible();
  await editor
    .getByRole("button", { name: "Confirmar movimiento", exact: true })
    .click();
  const check = page.getByRole("button", {
    name: "Comprobar cambio",
    exact: true,
  });
  await expect(check).toBeEnabled();
  expect(persisted).toMatchObject({
    kind: "RESCHEDULED",
    revision: `"block:${original.id}:2"`,
    before: original,
    after: {
      ...original,
      startAt: "2030-01-07T12:00:00Z",
      endAt: "2030-01-07T13:00:00Z",
    },
  });
  expect(sql("SELECT count(*) FROM block_changes")).toBe("1");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockChanged.v1'",
    ),
  ).toBe("1");
  const proof = await restartBackend(request);
  const cancelled = await request.post(`${endpoint}/${original.id}/cancel`, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": persisted.revision,
      "Idempotency-Key": randomUUID(),
    },
    data: {},
  });
  expect(cancelled.status(), await cancelled.text()).toBe(201);
  const later = await cancelled.json();
  expect(later).toMatchObject({
    kind: "CANCELLED",
    revision: `"block:${original.id}:3"`,
    before: persisted.after,
    after: null,
  });
  expect(sql("SELECT count(*) FROM block_changes")).toBe("2");
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockChanged.v1'",
    ),
  ).toBe("2");
  // Isolated fixture: model outbox retention without touching durable receipts/projections.
  sql("DELETE FROM outbox_events WHERE event_type='BlockChanged.v1'");
  const stable = sql(
    "SELECT json_build_object('changes',(SELECT json_agg(c ORDER BY id) FROM block_changes c),'projections',(SELECT json_agg(p ORDER BY block_id) FROM block_projections p),'events',(SELECT json_agg(e ORDER BY event_id) FROM outbox_events e))",
  );
  const recoveredResponse = page.waitForResponse((response) =>
    response.url().endsWith(`${endpoint}/changes/by-request/${key}`),
  );
  await check.click();
  const recovered = await recoveredResponse;
  expect(recovered.status()).toBe(200);
  expect(Object.keys(await recovered.json()).sort()).toEqual(
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
  expect(await recovered.json()).toEqual(persisted);
  const byId = await request.get(`${endpoint}/changes/${persisted.id}`);
  expect(byId.status()).toBe(200);
  expect(await byId.json()).toEqual(persisted);
  await expect(
    page.getByText("Cambio confirmado (hecho histórico)", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Estado actual del bloque", exact: true }),
  ).toContainText("Cancelado");
  expect(writes).toBe(1);
  expect(
    sql(
      "SELECT json_build_object('changes',(SELECT json_agg(c ORDER BY id) FROM block_changes c),'projections',(SELECT json_agg(p ORDER BY block_id) FROM block_projections p),'events',(SELECT json_agg(e ORDER BY event_id) FROM outbox_events e))",
    ),
  ).toBe(stable);
  expect(
    sql(
      "SELECT count(*) FROM outbox_events WHERE event_type='BlockChanged.v1'",
    ),
  ).toBe("0");
  const engine = page.context().browser().browserType().name();
  const folder = `.e2e-work/reschedule-real/${engine}`;
  await mkdir(folder, { recursive: true });
  await writeFile(
    `${folder}/restart-proof.json`,
    JSON.stringify(
      {
        fixture: proof.fixture,
        backendId: proof.backendId,
        databaseId: proof.databaseId,
        beforeBackendStartedAt: proof.beforeBackend.State.StartedAt,
        afterBackendStartedAt: proof.afterBackend.State.StartedAt,
        databaseStartedAt: proof.afterDatabase.State.StartedAt,
        databaseMounts: proof.afterDatabase.Mounts.map(
          ({ Type, Name, Destination }) => ({ Type, Name, Destination }),
        ),
        sessionReauthenticated: !proof.session.authenticated,
        writes,
        key,
        historicalReceipt: persisted,
        laterReceipt: later,
        recovered: await recovered.json(),
      },
      null,
      2,
    ),
  );
});
