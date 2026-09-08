import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";

test("customization: task zero false and date survive reversible field deactivation @s7 @s32", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de valores tipados");
  const task = await saveTask(request, project.id, "Tarea con campos propios");
  const facts = () =>
    sql(`SELECT jsonb_build_object(
    'project',(SELECT to_jsonb(p) FROM projects p WHERE id='${project.id}'),
    'task',(SELECT to_jsonb(t) FROM tasks t WHERE id='${task.id}'),
    'outbox',(SELECT jsonb_agg(to_jsonb(e) ORDER BY event_id) FROM outbox_events e))`);
  const before = facts();
  const schemaPath = "/api/v1/me/customization/TASK";
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/custom-fields`;
  const taskLink = () =>
    page
      .getByRole("list", { name: "Tareas guardadas", exact: true })
      .getByRole("link", { name: task.title });
  await page.goto(`/proyectos/${project.id}`);
  await page
    .getByRole("button", { name: "Gestionar campos personales", exact: true })
    .click();
  let schema;
  for (const [label, type] of [
    ["Cantidad", "NUMBER"],
    ["Revisado", "BOOLEAN"],
    ["Fecha personal", "DATE"],
  ]) {
    const form = page.getByRole("group", { name: "Nuevo campo", exact: true });
    await form.getByLabel("Etiqueta", { exact: true }).fill(label);
    await form
      .getByRole("combobox", { name: "Tipo", exact: true })
      .selectOption(type);
    const response = page.waitForResponse(
      (r) =>
        r.url().endsWith(`${schemaPath}/fields`) &&
        r.request().method() === "POST",
    );
    await form
      .getByRole("button", { name: "Crear campo", exact: true })
      .click();
    const confirmed = await response;
    expect(confirmed.status()).toBe(200);
    schema = await confirmed.json();
  }
  expect(schema.customFields.map(({ label, type }) => [label, type])).toEqual([
    ["Cantidad", "NUMBER"],
    ["Revisado", "BOOLEAN"],
    ["Fecha personal", "DATE"],
  ]);
  const numberId = schema.customFields[0].id;
  await taskLink().click();
  const values = page.getByRole("group", {
    name: "Valores personales",
    exact: true,
  });
  await values.getByLabel("Cantidad", { exact: true }).fill("0");
  await values
    .getByRole("combobox", { name: "Revisado", exact: true })
    .selectOption("false");
  await values.getByLabel("Fecha personal", { exact: true }).fill("2026-09-08");
  const saving = page.waitForResponse(
    (r) => r.url().endsWith(endpoint) && r.request().method() === "PUT",
  );
  await values
    .getByRole("button", { name: "Guardar campos", exact: true })
    .click();
  const confirmed = await saving;
  expect(confirmed.status()).toBe(200);
  const body = await confirmed.json();
  expect(body.values.map(({ value }) => value)).toEqual([
    0,
    false,
    "2026-09-08",
  ]);
  const originalTag = confirmed.headers().etag;
  const row = () =>
    JSON.parse(
      sql(
        `SELECT to_jsonb(v) FROM task_custom_field_values v WHERE owner_id='e2e-user' AND task_id='${task.id}'`,
      ),
    );
  const originalRow = row();
  await expect(
    page.getByRole("status").filter({ hasText: /^Campos guardados$/ }),
  ).toBeVisible();

  for (const active of [false, true]) {
    await page
      .getByRole("link", { name: "Volver al proyecto", exact: true })
      .click();
    await page
      .getByRole("button", { name: "Gestionar campos personales", exact: true })
      .click();
    await page
      .getByRole("button", { name: "Editar Cantidad", exact: true })
      .click();
    const field = page.getByRole("group", {
      name: "Editar campo",
      exact: true,
    });
    await field
      .getByRole("checkbox", { name: "Activo", exact: true })
      .setChecked(active);
    const changing = page.waitForResponse(
      (r) =>
        r.url().endsWith(`${schemaPath}/fields/${numberId}`) &&
        r.request().method() === "PUT",
    );
    await field
      .getByRole("button", { name: "Guardar campo", exact: true })
      .click();
    const changed = await changing;
    expect(changed.status()).toBe(200);
    const currentSchema = await changed.json();
    expect(currentSchema.customFields[0]).toEqual({
      ...schema.customFields[0],
      active,
    });
    expect(row()).toEqual(originalRow);
    await taskLink().click();
    if (active)
      await expect(values.getByLabel("Cantidad", { exact: true })).toHaveValue(
        "0",
      );
    else
      await expect(values.getByLabel("Cantidad", { exact: true })).toHaveCount(
        0,
      );
    await expect(
      values.getByRole("combobox", { name: "Revisado", exact: true }),
    ).toHaveValue("false");
    await expect(
      values.getByLabel("Fecha personal", { exact: true }),
    ).toHaveValue("2026-09-08");
    const durable = await request.get(endpoint);
    expect(durable.status()).toBe(200);
    expect((await durable.json()).values).toEqual(
      active ? body.values : body.values.slice(1),
    );
    expect(durable.headers().etag.split(":values:")[1]).toBe(
      originalTag.split(":values:")[1],
    );
  }
  await page.reload();
  await expect(values.getByLabel("Cantidad", { exact: true })).toHaveValue("0");
  await expect(
    values.getByRole("combobox", { name: "Revisado", exact: true }),
  ).toHaveValue("false");
  await expect(
    values.getByLabel("Fecha personal", { exact: true }),
  ).toHaveValue("2026-09-08");
  expect(row()).toEqual(originalRow);
  expect(facts()).toBe(before);
});
