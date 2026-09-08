import { test, expect } from "./support/authenticated-test.mjs";
import { create, stored, sql } from "./support/projects.mjs";

test("customization: project view and personal text survive detail reload @s29 @s31 @s32", async ({
  page,
  request,
}) => {
  const project = await create(request, "Proyecto de personalización visual");
  const before = stored(project.id);
  const schemaPath = "/api/v1/me/customization/PROJECT";
  const valuesPath = `/api/v1/projects/${project.id}/custom-fields`;
  await page.goto("/proyectos");
  await page
    .getByRole("button", { name: "Personalizar vista", exact: true })
    .click();
  const view = page.getByRole("group", {
    name: "Metadatos visibles",
    exact: true,
  });
  await view.getByRole("checkbox", { name: "Creado", exact: true }).uncheck();
  const viewResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(schemaPath) &&
      response.request().method() === "PUT",
  );
  await view
    .getByRole("button", { name: "Guardar vista", exact: true })
    .click();
  const viewSaved = await viewResponse;
  expect(viewSaved.status()).toBe(200);
  expect((await viewSaved.json()).visibleFields).toEqual([]);
  await expect(
    page.getByRole("status").filter({ hasText: /^Vista guardada$/ }),
  ).toBeVisible();
  const row = page
    .getByRole("list", { name: "Proyectos guardados" })
    .getByRole("listitem")
    .filter({
      has: page.getByRole("link", { name: project.name }),
    });
  await expect(row.getByRole("link", { name: project.name })).toBeVisible();
  await expect(row.getByText("Idea", { exact: true })).toBeVisible();
  await expect(row.locator("time")).toHaveCount(0);

  await page
    .getByRole("button", { name: "Gestionar campos personales", exact: true })
    .click();
  const definition = page.getByRole("group", {
    name: "Nuevo campo",
    exact: true,
  });
  await definition.getByLabel("Etiqueta", { exact: true }).fill("Nota privada");
  await definition
    .getByRole("combobox", { name: "Tipo", exact: true })
    .selectOption("TEXT");
  const definitionResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(`${schemaPath}/fields`) &&
      response.request().method() === "POST",
  );
  await definition
    .getByRole("button", { name: "Crear campo", exact: true })
    .click();
  const created = await definitionResponse;
  expect(created.status()).toBe(200);
  const schema = await created.json();
  expect(schema.visibleFields).toEqual([]);
  expect(schema.customFields).toHaveLength(1);
  const field = schema.customFields[0];
  expect(field).toEqual({
    id: expect.any(String),
    label: "Nota privada",
    type: "TEXT",
    active: true,
  });
  await expect(
    page.getByRole("status").filter({ hasText: /^Campo creado$/ }),
  ).toBeVisible();
  await row.getByRole("link", { name: project.name }).click();
  await expect(page).toHaveURL(`/proyectos/${project.id}`);
  const values = page.getByRole("group", {
    name: "Valores personales",
    exact: true,
  });
  await expect(values.getByLabel("Nota privada", { exact: true })).toHaveValue(
    "",
  );
  const text = "  Texto privado con espacios 🧭  ";
  await values.getByLabel("Nota privada", { exact: true }).fill(text);
  const valueResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith(valuesPath) &&
      response.request().method() === "PUT",
  );
  await values
    .getByRole("button", { name: "Guardar campos", exact: true })
    .click();
  const confirmed = await valueResponse;
  expect(confirmed.status()).toBe(200);
  const body = await confirmed.json();
  expect(body.values).toEqual([
    { fieldId: field.id, label: field.label, type: "TEXT", value: text },
  ]);
  await expect(
    page.getByRole("status").filter({ hasText: /^Campos guardados$/ }),
  ).toBeVisible();
  await page.reload();
  await expect(
    page
      .getByRole("group", { name: "Valores personales", exact: true })
      .getByLabel("Nota privada", { exact: true }),
  ).toHaveValue(text);
  const durable = await request.get(valuesPath);
  expect(durable.status()).toBe(200);
  expect(durable.headers().etag).toBe(confirmed.headers().etag);
  expect(await durable.json()).toEqual(body);
  expect(
    JSON.parse(
      sql(
        `SELECT field_values -> '${field.id}' FROM project_custom_field_values WHERE owner_id='e2e-user' AND project_id='${project.id}'`,
      ),
    ),
  ).toBe(text);
  expect(stored(project.id)).toEqual(before);
});
