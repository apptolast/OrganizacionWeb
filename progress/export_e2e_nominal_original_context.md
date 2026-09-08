# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: export-data.spec.mjs >> export: real owner snapshot downloads original bytes twice without another GET and releases its URL @s22 @s24 @s26 @s29
- Location: e2e\export-data.spec.mjs:13:1

# Error details

```
Error: expect(locator).toHaveText(expected) failed

Locator:  getByRole('navigation', { name: 'Principal', exact: true }).getByRole('link').first()
Expected: "Hoy"
Received: "◉ Hoy"
Timeout:  5000ms

Call log:
  - Expect "toHaveText" getByRole('navigation', { name: 'Principal', exact: true }).getByRole('link').first() with timeout 5000ms
  - waiting for getByRole('navigation', { name: 'Principal', exact: true }).getByRole('link').first()
    12 × locator resolved to <a href="/">…</a>
       - unexpected value "◉ Hoy"

```

```yaml
- link "Hoy":
  - /url: /
```

# Test source

```ts
  1   | import { test, expect } from "./support/authenticated-test.mjs";
  2   | import { create, sql } from "./support/projects.mjs";
  3   | import { saveTask } from "./support/tasks.mjs";
  4   | import { randomUUID, createHash } from "node:crypto";
  5   | import { readFile, mkdir, writeFile } from "node:fs/promises";
  6   | import { resolve } from "node:path";
  7   | 
  8   | const collections =
  9   |   "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
  10  |     " ",
  11  |   );
  12  | 
  13  | test("export: real owner snapshot downloads original bytes twice without another GET and releases its URL @s22 @s24 @s26 @s29", async ({
  14  |   page,
  15  |   request,
  16  | }, testInfo) => {
  17  |   const suffix = randomUUID();
  18  |   const project = await create(
  19  |     request,
  20  |     `Exportación ${suffix}`,
  21  |     "  Datos propios: á 😀 y espacios  ",
  22  |   );
  23  |   let task;
  24  |   const foreignId = randomUUID();
  25  |   try {
  26  |     task = await saveTask(request, project.id, "Tarea exportada 😀", {
  27  |       completionCriterion: "Conservar su texto exacto",
  28  |     });
  29  |     // Explicit foreign-owner PG fixture; no foreign credentials or production data.
  30  |     sql(
  31  |       `INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at) VALUES ('${foreignId}','export-foreign-${suffix}','No pertenece a la sesión','No exportar','idea',0,'2020-01-01T00:00:00Z','2020-01-01T00:00:00Z')`,
  32  |     );
  33  |     const snapshot = () =>
  34  |       sql(
  35  |         `SELECT jsonb_build_object('project',(SELECT to_jsonb(p) FROM projects p WHERE id='${project.id}'),'task',(SELECT to_jsonb(t) FROM tasks t WHERE id='${task.id}'),'events',(SELECT jsonb_agg(jsonb_build_object('id',event_id,'payload',payload) ORDER BY event_id) FROM outbox_events WHERE aggregate_id IN ('${project.id}','${task.id}')))`,
  36  |       );
  37  |     const before = snapshot();
  38  |     const reads = [];
  39  |     const writes = [];
  40  |     page.on("request", (reading) => {
  41  |       if (new URL(reading.url()).pathname === "/api/v1/me/export")
  42  |         reads.push(reading);
  43  |       if (
  44  |         new URL(reading.url()).pathname.startsWith("/api/") &&
  45  |         reading.method() !== "GET"
  46  |       )
  47  |         writes.push(reading.method() + " " + reading.url());
  48  |     });
  49  |     await page.goto("/proyectos");
  50  |     const nav = page.getByRole("navigation", {
  51  |       name: "Principal",
  52  |       exact: true,
  53  |     });
> 54  |     await expect(nav.getByRole("link").first()).toHaveText("Hoy");
      |                                                 ^ Error: expect(locator).toHaveText(expected) failed
  55  |     await expect(nav.getByRole("link").last()).toHaveText("Exportación");
  56  |     await nav.getByRole("link", { name: "Exportación", exact: true }).click();
  57  |     await expect(page).toHaveURL(/\/exportacion$/);
  58  |     await expect(
  59  |       page.getByRole("heading", { name: "Exportar mis datos", level: 1 }),
  60  |     ).toBeFocused();
  61  |     expect(reads).toHaveLength(0);
  62  |     const received = page.waitForResponse(
  63  |       (response) => new URL(response.url()).pathname === "/api/v1/me/export",
  64  |     );
  65  |     const downloads = [];
  66  |     page.on("download", (download) => downloads.push(download));
  67  |     await page
  68  |       .getByRole("button", { name: "Preparar exportación", exact: true })
  69  |       .click();
  70  |     const response = await received;
  71  |     expect(response.status()).toBe(200);
  72  |     const original = await response.body();
  73  |     expect(response.headers()["content-type"]).toMatch(
  74  |       /^application\/json\s*;\s*charset=utf-8$/i,
  75  |     );
  76  |     expect(Number(response.headers()["content-length"])).toBe(original.length);
  77  |     const data = JSON.parse(
  78  |       new TextDecoder("utf-8", { fatal: true }).decode(original),
  79  |     );
  80  |     expect(data.owner).toBe("e2e-user");
  81  |     expect(data.schemaVersion).toBe(1);
  82  |     expect(data.format).toBe("organizationweb-export");
  83  |     expect(Object.keys(data.data).sort()).toEqual([...collections].sort());
  84  |     expect(Object.keys(data.counts).sort()).toEqual([...collections].sort());
  85  |     for (const name of collections)
  86  |       expect(data.counts[name]).toBe(data.data[name].length);
  87  |     expect(data.data.projects).toContainEqual(
  88  |       expect.objectContaining({
  89  |         id: project.id,
  90  |         name: project.name,
  91  |         description: project.description,
  92  |         version: "0",
  93  |       }),
  94  |     );
  95  |     expect(data.data.tasks).toContainEqual(
  96  |       expect.objectContaining({
  97  |         id: task.id,
  98  |         projectId: project.id,
  99  |         title: task.title,
  100 |       }),
  101 |     );
  102 |     expect(data.data.projects.some((item) => item.id === foreignId)).toBe(
  103 |       false,
  104 |     );
  105 |     expect(data.data.tasks.some((item) => item.projectId === foreignId)).toBe(
  106 |       false,
  107 |     );
  108 |     const link = page.getByRole("link", {
  109 |       name: "Descargar archivo JSON",
  110 |       exact: true,
  111 |     });
  112 |     await expect(link).toBeVisible();
  113 |     await expect(page.getByRole("status")).toHaveText("Archivo preparado");
  114 |     expect(downloads).toHaveLength(0);
  115 |     const filename = `organizationweb-export-v1-${data.exportedAt.replace(/[-:.]/g, "")}.json`;
  116 |     expect(response.headers()["content-disposition"]).toBe(
  117 |       `attachment; filename="${filename}"`,
  118 |     );
  119 |     await expect(link).toHaveAttribute("download", filename);
  120 |     const objectUrl = await link.getAttribute("href");
  121 |     expect(objectUrl).toMatch(/^blob:/);
  122 |     for (let gesture = 0; gesture < 2; gesture++) {
  123 |       const completed = page.waitForEvent("download");
  124 |       await link.click();
  125 |       const download = await completed;
  126 |       expect(download.suggestedFilename()).toBe(filename);
  127 |       expect(await download.failure()).toBeNull();
  128 |       expect(await readFile(await download.path())).toEqual(original);
  129 |     }
  130 |     expect(reads).toHaveLength(1);
  131 |     expect(downloads).toHaveLength(2);
  132 |     expect(writes).toEqual([]);
  133 |     expect(snapshot()).toBe(before);
  134 |     await nav.getByRole("link", { name: "Hoy", exact: true }).click();
  135 |     await expect(link).toHaveCount(0);
  136 |     expect(
  137 |       await page.evaluate(async (url) => {
  138 |         try {
  139 |           await fetch(url);
  140 |           return false;
  141 |         } catch {
  142 |           return true;
  143 |         }
  144 |       }, objectUrl),
  145 |     ).toBe(true);
  146 |     expect(reads).toHaveLength(1);
  147 |     const folder = resolve(
  148 |       ".e2e-work",
  149 |       "export-real",
  150 |       process.env.E2E_COMPOSE_PROJECT,
  151 |       testInfo.project.name || "chromium",
  152 |     );
  153 |     await mkdir(folder, { recursive: true });
  154 |     await writeFile(
```