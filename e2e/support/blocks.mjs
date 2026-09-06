import { expect } from "@playwright/test";
import { csrfHeaders } from "../../scripts/session-client.mjs";
const days = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
const taskRoute = (project, task) =>
  `/proyectos/${project.id}/tareas/${task.id}`;
export async function configure(request, minutes = 120, zoneId = "UTC") {
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

export async function openEditor(
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
