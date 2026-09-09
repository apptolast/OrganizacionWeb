import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { restartBackend } from "./support/backend.mjs";
import { randomUUID } from "node:crypto";

const minute = (offsetDays) => {
  const at = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  at.setUTCSeconds(0, 0);
  return at;
};
const utc = (at) => at.toISOString().replace(/\.\d{3}Z$/, "Z");
const local = (at) => utc(at).replace("Z", "");

/**
 * The shared fixture does not know about the feed table, so each test starts from «sin enlace»
 * explicitly instead of inheriting the token of whichever test ran before it.
 */
function withoutFeedToken() {
  sql("DELETE FROM calendar_feed_tokens WHERE owner_id='e2e-user'");
}

async function plannedBlock(request) {
  withoutFeedToken();
  const suffix = randomUUID();
  const project = await create(request, `Calendario ${suffix}`, "");
  const task = await saveTask(request, project.id, `Revisión ${suffix}`, {
    completionCriterion: "Cerrar la revisión",
  });
  const id = randomUUID();
  const start = minute(3);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  sql(
    `INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES ('${id}','${project.id}','${task.id}','${randomUUID()}','Objetivo del bloque','${local(start)}','${local(end)}','UTC','+00:00','+00:00',false,'${utc(start)}','${utc(end)}',60,'${utc(start)}')`,
  );
  return { project, task, id, start, end };
}

/** The url is only ever shown once, so every step reads it from the field on screen. */
async function createLink(page) {
  await page.goto("/calendario");
  await expect(
    page.getByRole("heading", { level: 1, name: "Calendario ICS" }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Crear enlace de suscripción" })
    .click();
  const field = page.getByLabel("Enlace de suscripción", { exact: true });
  await expect(field).toBeVisible();
  return (await field.textContent()).trim();
}

test("ics: an anonymous client reads the feed of a planned block and loses it when it is revoked @s11 @s12 @s15 @s16 @s27 @s32 @s35", async ({
  page,
  request,
  browser,
}) => {
  const block = await plannedBlock(request);
  const url = await createLink(page);
  const path = new URL(url).pathname;
  const anonymous = await browser.newContext();
  try {
    const feed = await anonymous.request.get(path);
    expect(feed.status()).toBe(200);
    // El controlador fija «text/calendar; charset=utf-8»; Tomcat reserializa el tipo y suprime el
    // espacio opcional tras el «;» (OWS que RFC 9110 permite). Ver la desviación en
    // progress/tdd_ics_calendar.md; ninguna capa de la aplicación puede evitar esa normalización.
    expect(feed.headers()["content-type"].replace("; ", ";")).toBe(
      "text/calendar;charset=utf-8",
    );
    expect(feed.headers()["cache-control"]).toBe("private, no-store");
    // Una sola copia de cada una: el backend las emite y el proxy no las duplica.
    expect(feed.headers()["x-content-type-options"]).toBe("nosniff");
    expect(feed.headers()["referrer-policy"]).toBe("same-origin");
    expect(feed.headers()["content-security-policy"]).toContain(
      "frame-ancestors 'none'",
    );
    expect(feed.headers()["content-security-policy"]).not.toContain(
      "frame-ancestors 'none', ",
    );
    expect(feed.headers()["content-disposition"]).toBeUndefined();
    expect(feed.headers()["set-cookie"]).toBeUndefined();
    const document = await feed.text();
    expect(document.startsWith("BEGIN:VCALENDAR\r\n")).toBe(true);
    expect(document.endsWith("END:VCALENDAR\r\n")).toBe(true);
    expect(document).toContain(`UID:${block.id}@`);
    expect(document).toContain("SEQUENCE:1");
    expect(Number(feed.headers()["content-length"])).toBe(
      Buffer.byteLength(document, "utf8"),
    );

    for (const method of ["post", "put", "delete", "patch"]) {
      const refused = await anonymous.request[method](path);
      expect(refused.status()).toBe(405);
      expect(refused.headers()["allow"]).toBe("GET, HEAD");
    }
    for (const wrong of [
      path.slice(0, -5) + ".ics",
      path.replace(/.$/, ""),
      path + "?x=1",
      "/calendar/",
    ]) {
      const missing = await anonymous.request.get(wrong);
      expect(missing.status()).toBe(404);
      expect(await missing.json()).toMatchObject({
        code: "CALENDAR_NOT_FOUND",
      });
    }

    await page
      .getByRole("button", { name: "Revocar enlace", exact: true })
      .click();
    await page
      .getByRole("button", { name: "Confirmar revocación", exact: true })
      .click();
    await expect(
      page.getByRole("button", { name: "Crear enlace de suscripción" }),
    ).toBeVisible();
    const revoked = await anonymous.request.get(path);
    expect(revoked.status()).toBe(404);
  } finally {
    await anonymous.close();
  }
});

test("ics: regenerating invalidates the previous address and a restart keeps the new one @s4 @s29 @s35", async ({
  page,
  request,
  browser,
}) => {
  await plannedBlock(request);
  const first = await createLink(page);
  await page
    .getByRole("button", { name: "Regenerar enlace", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Confirmar regeneración", exact: true })
    .click();
  // Aserción web-first: el campo se repuebla cuando llega el 201, no en el clic.
  const field = page.getByLabel("Enlace de suscripción", { exact: true });
  await expect(field).not.toHaveText(first);
  const second = (await field.textContent()).trim();

  const anonymous = await browser.newContext();
  try {
    expect((await anonymous.request.get(new URL(first).pathname)).status()).toBe(
      404,
    );
    const before = await anonymous.request.get(new URL(second).pathname);
    expect(before.status()).toBe(200);
    const bytes = await before.text();

    await restartBackend(request);

    const after = await anonymous.request.get(new URL(second).pathname);
    expect(after.status()).toBe(200);
    expect(await after.text()).toBe(bytes);
  } finally {
    await anonymous.close();
  }
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Regenerar enlace", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByLabel("Enlace de suscripción", { exact: true }),
  ).toHaveCount(0);
});

test("ics: the session download offers the same document as an attachment @s17 @s36", async ({
  page,
  request,
}) => {
  const block = await plannedBlock(request);
  await page.goto("/calendario");
  await page
    .getByRole("button", { name: "Descargar archivo .ics", exact: true })
    .click();
  await expect(page.getByText("Archivo preparado")).toBeVisible();
  const link = page.getByRole("link", { name: /organizationweb-bloques\.ics/ });
  await expect(link).toHaveAttribute("download", "organizationweb-bloques.ics");

  const direct = await request.get("/api/v1/me/calendar.ics");
  expect(direct.status()).toBe(200);
  expect(direct.headers()["content-disposition"]).toBe(
    'attachment; filename="organizationweb-bloques.ics"',
  );
  expect(await direct.text()).toContain(`UID:${block.id}@`);
});
