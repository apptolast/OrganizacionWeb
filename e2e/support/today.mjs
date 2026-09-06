import { expect } from "@playwright/test";
import { create, sql } from "./projects.mjs";
import { saveTask } from "./tasks.mjs";
import { csrfHeaders } from "../../scripts/session-client.mjs";

export async function seedAgenda(request) {
  const preference = await request.get("/api/v1/me/availability");
  expect(preference.status()).toBe(200);
  const configured = await request.put("/api/v1/me/availability", {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": preference.headers().etag,
    },
    data: {
      zoneId: "UTC",
      dailyMinutes: Object.fromEntries(
        [
          "MONDAY",
          "TUESDAY",
          "WEDNESDAY",
          "THURSDAY",
          "FRIDAY",
          "SATURDAY",
          "SUNDAY",
        ].map((day) => [day, 120]),
      ),
    },
  });
  expect(configured.status()).toBe(200);
  const project = await create(request, "Proyecto <b>literal</b> 🧭");
  const task = await saveTask(
    request,
    project.id,
    "Resultado verificable sin confundir reserva con trabajo",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
  const reserved = await request.post(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "Idempotency-Key": crypto.randomUUID(),
      "Availability-Revision": configured.headers().etag,
    },
    data: {
      objective: "Preparar un borrador revisable",
      startLocal: "2090-01-07T10:00",
      endLocal: "2090-01-07T11:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
  });
  expect(reserved.status()).toBe(201);
  const block = await reserved.json();
  expect(block.id).toMatch(/^[0-9a-f-]{36}$/i);
  // Place the real persisted reservation around the server clock without changing
  // the application clock or waiting for its future schedule. This is fixture setup.
  const current = await request.get("/api/v1/today");
  expect(current.status()).toBe(200);
  const now = Date.parse((await current.json()).serverNow);
  const start = new Date(
    Math.floor(now / 60_000) * 60_000 - 30 * 60_000,
  ).toISOString();
  const end = new Date(Date.parse(start) + 60 * 60_000).toISOString();
  sql(
    `UPDATE planned_blocks SET start_at='${start}',end_at='${end}',start_local='${start.slice(0, 16)}',end_local='${end.slice(0, 16)}' WHERE id='${block.id}'`,
  );
  return { project, task, block, start, end };
}
