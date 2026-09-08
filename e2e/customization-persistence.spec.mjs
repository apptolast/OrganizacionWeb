import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { restartBackend } from "./support/backend.mjs";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";

test("customization: lost response and API restart preserve typed metadata and work facts @s11 @s26", async ({
  page,
  request,
  browser,
}) => {
  test.setTimeout(90_000);
  const project = await create(request, "Customization persistence fixture");
  const task = await saveTask(request, project.id, "Preserve work facts", {
    completionCriterion: "Keep business fields intact",
    estimatedMinutes: 25,
  });
  const facts = () =>
    sql(`SELECT jsonb_build_object(
    'project',(SELECT to_jsonb(p) FROM projects p WHERE id='${project.id}'),
    'task',(SELECT to_jsonb(t) FROM tasks t WHERE id='${task.id}'),
    'outbox',(SELECT jsonb_agg(to_jsonb(e) ORDER BY event_id) FROM outbox_events e))`);
  const before = facts();
  const schemaPath = "/api/v1/me/customization/TASK";
  let schemaResponse = await request.get(schemaPath);
  expect(schemaResponse.status()).toBe(200);
  for (const [label, type] of [
    ["Nota", "TEXT"],
    ["Cantidad", "NUMBER"],
    ["Fecha", "DATE"],
    ["Validado", "BOOLEAN"],
  ]) {
    schemaResponse = await request.post(`${schemaPath}/fields`, {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": schemaResponse.headers().etag,
      },
      data: { label, type },
    });
    expect(schemaResponse.status(), await schemaResponse.text()).toBe(200);
  }
  let schema = await schemaResponse.json();
  let schemaTag = schemaResponse.headers().etag;
  expect(schema.customFields).toHaveLength(4);
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/custom-fields`;
  const initial = await request.get(endpoint);
  expect(initial.status()).toBe(200);
  expect((await initial.json()).values.map((field) => field.value)).toEqual([
    null,
    null,
    null,
    null,
  ]);
  const values = ["  texto privado  ", 0, "2026-09-07", false];
  const intention = {
    values: schema.customFields.map((field, index) => ({
      fieldId: field.id,
      value: values[index],
    })),
  };
  const headers = {
    ...(await csrfHeaders(request)),
    "If-Match": initial.headers().etag,
    "Content-Type": "application/json",
  };
  let actualBody;
  let actualTag;
  let writes = 0;
  await page.goto("/proyectos");
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    const actual = await route.fetch();
    expect(actual.status(), await actual.text()).toBe(200);
    actualBody = await actual.json();
    actualTag = actual.headers().etag;
    await route.abort("failed");
  });
  const lost = await page.evaluate(
    async ({ endpoint, headers, intention }) => {
      try {
        await fetch(endpoint, {
          method: "PUT",
          headers,
          body: JSON.stringify(intention),
        });
        return false;
      } catch {
        return true;
      }
    },
    { endpoint, headers, intention },
  );
  expect(lost).toBe(true);
  expect(writes).toBe(1);
  expect(actualBody).toEqual({
    configured: true,
    updatedAt: expect.any(String),
    values: schema.customFields.map((field, index) => ({
      fieldId: field.id,
      label: field.label,
      type: field.type,
      value: values[index],
    })),
  });
  expect(actualTag).toMatch(
    /^"custom-values:TASK:[0-9a-f-]{36}:schema:[0-9a-f-]{36}:3:values:[0-9a-f-]{36}:0"$/,
  );
  // GET recovers durable current state; it does not prove which intention committed.
  const recovered = await request.get(endpoint);
  expect(recovered.status()).toBe(200);
  expect(await recovered.json()).toEqual(actualBody);
  expect(recovered.headers().etag).toBe(actualTag);
  const metadata = () =>
    sql(`SELECT jsonb_build_object(
    'schema',(SELECT to_jsonb(c) FROM customization_preferences c WHERE owner_id='e2e-user' AND scope='TASK'),
    'values',(SELECT to_jsonb(v) FROM task_custom_field_values v WHERE owner_id='e2e-user' AND task_id='${task.id}'))`);
  const inactiveId = schema.customFields[0].id;
  const view = await request.put(schemaPath, {
    headers: { ...(await csrfHeaders(request)), "If-Match": schemaTag },
    data: { visibleFields: [] },
  });
  expect(view.status()).toBe(200);
  const deactivated = await request.put(`${schemaPath}/fields/${inactiveId}`, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": view.headers().etag,
    },
    data: { label: "Nota", active: false },
  });
  expect(deactivated.status()).toBe(200);
  schema = await deactivated.json();
  schemaTag = deactivated.headers().etag;
  expect(schema.visibleFields).toEqual([]);
  expect(schema.customFields[0].active).toBe(false);
  const current = await request.get(endpoint);
  expect(current.status()).toBe(200);
  const visible = await current.json();
  expect(visible.values).toEqual(actualBody.values.slice(1));
  expect(current.headers().etag.split(":values:")[1]).toBe(
    actualTag.split(":values:")[1],
  );
  actualBody = visible;
  actualTag = current.headers().etag;
  expect(
    JSON.parse(
      sql(
        `SELECT field_values -> '${inactiveId}' FROM task_custom_field_values WHERE owner_id='e2e-user' AND task_id='${task.id}'`,
      ),
    ),
  ).toBe("  texto privado  ");
  const stored = metadata();
  expect(facts()).toBe(before);
  const restarted = await restartBackend(request);
  const fresh = await browser.newContext({
    baseURL: test.info().project.use.baseURL,
  });
  try {
    await loginSession(fresh.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    for (const [path, expectedBody, expectedTag] of [
      [schemaPath, schema, schemaTag],
      [endpoint, actualBody, actualTag],
    ]) {
      const response = await fresh.request.get(path);
      expect(response.status()).toBe(200);
      expect(response.headers().etag).toBe(expectedTag);
      expect(response.headers()["cache-control"]).toContain("no-store");
      expect(await response.json()).toEqual(expectedBody);
    }
    expect(metadata()).toBe(stored);
    expect(facts()).toBe(before);
    expect(writes).toBe(1);
    await test.info().attach("persistence-evidence", {
      contentType: "application/json",
      body: JSON.stringify({
        fixture: restarted.fixture,
        projectId: project.id,
        taskId: task.id,
        schemaTag,
        valuesTag: actualTag,
        lostResponseAfterReal200: true,
        writes,
        freshSession: true,
        backendRestarted:
          restarted.beforeBackend.State.StartedAt !==
          restarted.afterBackend.State.StartedAt,
        databaseUnchanged:
          restarted.beforeDatabase.State.StartedAt ===
          restarted.afterDatabase.State.StartedAt,
        metadataUnchanged: true,
        businessAndOutboxUnchanged: true,
      }),
    });
  } finally {
    await fresh.close();
  }
});
