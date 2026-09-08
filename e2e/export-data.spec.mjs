import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { randomUUID, createHash } from "node:crypto";
import { readFile, mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const collections =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
    " ",
  );

test("export: real owner snapshot downloads original bytes twice without another GET and releases its URL @s22 @s24 @s26 @s29", async ({
  page,
  request,
}, testInfo) => {
  const suffix = randomUUID();
  const project = await create(
    request,
    `Exportación ${suffix}`,
    "  Datos propios: á 😀 y espacios  ",
  );
  let task;
  const foreignId = randomUUID();
  try {
    task = await saveTask(request, project.id, "Tarea exportada 😀", {
      completionCriterion: "Conservar su texto exacto",
    });
    // Explicit foreign-owner PG fixture; no foreign credentials or production data.
    sql(
      `INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES ('${foreignId}','export-foreign-${suffix}','No pertenece a la sesión','No exportar','idea',0,'2020-01-01T00:00:00Z','2020-01-01T00:00:00Z')`,
    );
    const snapshot = () =>
      sql(
        `SELECT jsonb_build_object('project',(SELECT to_jsonb(p) FROM projects p WHERE id='${project.id}'),'task',(SELECT to_jsonb(t) FROM tasks t WHERE id='${task.id}'),'events',(SELECT jsonb_agg(jsonb_build_object('id',event_id,'payload',payload) ORDER BY event_id) FROM outbox_events WHERE aggregate_id IN ('${project.id}','${task.id}')))`,
      );
    const before = snapshot();
    const reads = [];
    const writes = [];
    page.on("request", (reading) => {
      if (new URL(reading.url()).pathname === "/api/v1/me/export")
        reads.push(reading);
      if (
        new URL(reading.url()).pathname.startsWith("/api/") &&
        reading.method() !== "GET"
      )
        writes.push(reading.method() + " " + reading.url());
    });
    await page.goto("/proyectos");
    const nav = page.getByRole("navigation", {
      name: "Principal",
      exact: true,
    });
    await expect(nav.getByRole("link").first()).toHaveAccessibleName("Hoy");
    await expect(nav.getByRole("link").nth(-3)).toHaveAccessibleName(
      "Exportación",
    );
    await expect(nav.getByRole("link").nth(-2)).toHaveAccessibleName(
      "Importación",
    );
    await expect(nav.getByRole("link").last()).toHaveAccessibleName(
      "API para integraciones",
    );
    await nav.getByRole("link", { name: "Exportación", exact: true }).click();
    await expect(page).toHaveURL(/\/exportacion$/);
    await expect(
      page.getByRole("heading", { name: "Exportar mis datos", level: 1 }),
    ).toBeFocused();
    expect(reads).toHaveLength(0);
    const received = page.waitForResponse(
      (response) => new URL(response.url()).pathname === "/api/v1/me/export",
    );
    const downloads = [];
    page.on("download", (download) => downloads.push(download));
    await page
      .getByRole("button", { name: "Preparar exportación", exact: true })
      .click();
    const response = await received;
    expect(response.status()).toBe(200);
    const original = await response.body();
    expect(response.headers()["content-type"]).toMatch(
      /^application\/json\s*;\s*charset=utf-8$/i,
    );
    expect(Number(response.headers()["content-length"])).toBe(original.length);
    const data = JSON.parse(
      new TextDecoder("utf-8", { fatal: true }).decode(original),
    );
    expect(data.owner).toBe("e2e-user");
    expect(data.schemaVersion).toBe(1);
    expect(data.format).toBe("organizationweb-export");
    expect(Object.keys(data.data).sort()).toEqual([...collections].sort());
    expect(Object.keys(data.counts).sort()).toEqual([...collections].sort());
    for (const name of collections)
      expect(data.counts[name]).toBe(data.data[name].length);
    expect(data.data.projects).toContainEqual(
      expect.objectContaining({
        id: project.id,
        name: project.name,
        description: project.description,
        version: "0",
      }),
    );
    expect(data.data.tasks).toContainEqual(
      expect.objectContaining({
        id: task.id,
        projectId: project.id,
        title: task.title,
      }),
    );
    expect(data.data.projects.some((item) => item.id === foreignId)).toBe(
      false,
    );
    expect(data.data.tasks.some((item) => item.projectId === foreignId)).toBe(
      false,
    );
    const link = page.getByRole("link", {
      name: "Descargar archivo JSON",
      exact: true,
    });
    await expect(link).toBeVisible();
    await expect(page.getByRole("status")).toHaveText("Archivo preparado");
    expect(downloads).toHaveLength(0);
    const filename = `organizationweb-export-v1-${data.exportedAt.replace(/[-:.]/g, "")}.json`;
    expect(response.headers()["content-disposition"]).toBe(
      `attachment; filename="${filename}"`,
    );
    await expect(link).toHaveAttribute("download", filename);
    const objectUrl = await link.getAttribute("href");
    expect(objectUrl).toMatch(/^blob:/);
    for (let gesture = 0; gesture < 2; gesture++) {
      const completed = page.waitForEvent("download");
      await link.click();
      const download = await completed;
      expect(download.suggestedFilename()).toBe(filename);
      expect(await download.failure()).toBeNull();
      expect(await readFile(await download.path())).toEqual(original);
    }
    expect(reads).toHaveLength(1);
    expect(downloads).toHaveLength(2);
    expect(writes).toEqual([]);
    expect(snapshot()).toBe(before);
    await nav.getByRole("link", { name: "Hoy", exact: true }).click();
    await expect(link).toHaveCount(0);
    expect(
      await page.evaluate(async (url) => {
        try {
          await fetch(url);
          return false;
        } catch {
          return true;
        }
      }, objectUrl),
    ).toBe(true);
    expect(reads).toHaveLength(1);
    const folder = resolve(
      ".e2e-work",
      "export-real",
      process.env.E2E_COMPOSE_PROJECT,
      testInfo.project.name || "chromium",
    );
    await mkdir(folder, { recursive: true });
    await writeFile(
      resolve(folder, "download.json"),
      JSON.stringify(
        {
          owner: data.owner,
          projectId: project.id,
          taskId: task.id,
          excludedProjectId: foreignId,
          filename,
          length: original.length,
          sha256: createHash("sha256").update(original).digest("hex"),
          downloadCount: downloads.length,
          exportRequests: reads.length,
          databaseUnchanged: true,
          objectUrlRevoked: true,
        },
        null,
        2,
      ),
    );
  } finally {
    // Only rows created by this test, inside the guarded ephemeral stack.
    if (task)
      sql(
        `DELETE FROM task_status_history WHERE task_id='${task.id}'; DELETE FROM outbox_events WHERE aggregate_id='${task.id}'; DELETE FROM tasks WHERE id='${task.id}' AND project_id='${project.id}'`,
      );
    sql(
      `DELETE FROM outbox_events WHERE aggregate_id='${project.id}'; DELETE FROM projects WHERE id='${project.id}' AND owner_id='e2e-user'; DELETE FROM projects WHERE id='${foreignId}' AND owner_id='export-foreign-${suffix}'`,
    );
  }
});
