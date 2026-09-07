import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { restartBackend } from "./support/backend.mjs";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";

test("appearance: lost confirmation survives API restart and a new browser without changing work facts @s8 @s9", async ({
  page,
  request,
  browser,
}) => {
  test.setTimeout(90_000);
  const endpoint = "/api/v1/me/appearance";
  const project = await create(request, "Appearance persistence fixture");
  const task = await saveTask(request, project.id, "Keep work facts intact");
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
      objective: "Preserved reservation",
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
  await post(
    `/api/v1/work-sessions/${session.id}/close`,
    { progressNote: "Preserved note", nextStep: "Review" },
    { "Work-Session-Revision": `work-session-${session.id}-1` },
  );
  const facts = () =>
    sql(`SELECT jsonb_build_object(
    'project',(SELECT to_jsonb(p) FROM projects p WHERE id='${project.id}'),
    'task',(SELECT to_jsonb(t) FROM tasks t WHERE id='${task.id}'),
    'block',(SELECT to_jsonb(b) FROM planned_blocks b WHERE id='${block.id}'),
    'session',(SELECT to_jsonb(s) FROM work_sessions s WHERE id='${session.id}'),
    'intervals',(SELECT jsonb_agg(to_jsonb(i) ORDER BY revision) FROM work_session_intervals i WHERE session_id='${session.id}'),
    'changes',(SELECT jsonb_agg(to_jsonb(c) ORDER BY id) FROM work_session_changes c WHERE session_id='${session.id}'),
    'availability',(SELECT to_jsonb(a) FROM availability_preferences a WHERE owner_id='e2e-user'),
    'outbox',(SELECT jsonb_agg(to_jsonb(e) ORDER BY event_id) FROM outbox_events e))`);
  const before = facts();
  const historyPath = `/api/v1/history?projectId=${project.id}`;
  const historical = await request.get(historyPath);
  expect(historical.status()).toBe(200);
  const history = await historical.json();
  const initial = await request.get(endpoint);
  expect(initial.status()).toBe(200);
  expect(await initial.json()).toEqual({
    configured: false,
    theme: "SYSTEM",
    accentLight: "#244C3C",
    accentDark: "#B7E4C7",
    updatedAt: null,
  });
  expect(initial.headers().etag).toBe('"appearance:unconfigured"');
  const headers = {
    ...(await csrfHeaders(request)),
    "If-Match": initial.headers().etag,
    "Content-Type": "application/json",
  };
  const intention = {
    theme: "DARK",
    accentLight: "#0000FF",
    accentDark: "#00FFFF",
  };
  let confirmed;
  let revision;
  let writes = 0;
  await page.goto("/proyectos");
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    const actual = await route.fetch();
    expect(actual.status(), await actual.text()).toBe(200);
    confirmed = await actual.json();
    revision = actual.headers().etag;
    await route.abort("failed");
  });
  try {
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
    expect(confirmed).toEqual({
      ...intention,
      configured: true,
      updatedAt: expect.any(String),
    });
    expect(revision).toMatch(/^"appearance:[0-9a-f-]{36}:0"$/);
    const row = sql(
      "SELECT to_jsonb(a) FROM appearance_preferences a WHERE owner_id='e2e-user'",
    );
    expect(facts()).toBe(before);
    await restartBackend(request);
    const fresh = await browser.newContext({
      baseURL: "http://127.0.0.1:18080",
    });
    try {
      await loginSession(fresh.request, {
        username: "e2e-user",
        password: "e2e-only-password",
      });
      const recovered = await fresh.request.get(endpoint);
      expect(recovered.status()).toBe(200);
      expect(recovered.headers().etag).toBe(revision);
      expect(recovered.headers()["cache-control"]).toContain("no-store");
      expect(await recovered.json()).toEqual(confirmed);
      expect(await (await fresh.request.get(historyPath)).json()).toEqual(
        history,
      );
      expect(
        sql(
          "SELECT to_jsonb(a) FROM appearance_preferences a WHERE owner_id='e2e-user'",
        ),
      ).toBe(row);
      expect(facts()).toBe(before);
      expect(writes).toBe(1);
    } finally {
      await fresh.close();
    }
  } finally {
    // Only this fixture's preference is removed; the runner owns its work facts and volume.
    if (revision) {
      const id = revision.split(":")[1];
      expect(id).toMatch(/^[0-9a-f-]{36}$/);
      sql(
        `DELETE FROM appearance_preferences WHERE id='${id}' AND owner_id='e2e-user'`,
      );
    }
  }
});
