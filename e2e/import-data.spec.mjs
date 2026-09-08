import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { randomUUID, createHash } from "node:crypto";
import { writeFile } from "node:fs/promises";

test("import: own durable project preview and deliberate atomic addition with no added outbox events @s33 @s36 @s37", async ({
  page,
  request,
}, testInfo) => {
  const project = await create(
    request,
    "Importación existente " + randomUUID(),
    "  Texto durable propio 😀  ",
  );
  const addedId = randomUUID();
  let requestKey;
  try {
    const exported = await request.get("/api/v1/me/export");
    expect(exported.status()).toBe(200);
    const archive = await exported.json();
    expect(archive.owner).toBe("e2e-user");
    const durable = archive.data.projects.find(
      (item) => item.id === project.id,
    );
    expect(durable).toBeDefined();
    // Explicit portable-file preparation from a real exported durable record.
    // Existing records outside this file are intentionally left untouched.
    for (const name of Object.keys(archive.data)) {
      archive.data[name] = [];
      archive.counts[name] = 0;
    }
    const added = {
      ...durable,
      id: addedId,
      name: "Importado íntegro " + addedId,
    };
    archive.data.projects = [durable, added];
    archive.counts.projects = 2;
    const bytes = Buffer.from(JSON.stringify(archive), "utf8");
    const hash = createHash("sha256").update(bytes).digest("hex");
    const snapshot = () =>
      sql(
        `SELECT jsonb_build_object('project',(SELECT to_jsonb(p) FROM projects p WHERE id='${project.id}'),'events',(SELECT jsonb_agg(jsonb_build_object('id',event_id,'payload',payload) ORDER BY event_id) FROM outbox_events WHERE aggregate_id IN ('${project.id}','${addedId}')))`,
      );
    const before = snapshot();
    const calls = [];
    page.on("request", (call) => {
      if (new URL(call.url()).pathname.startsWith("/api/v1/me/import"))
        calls.push(call);
      if (
        new URL(call.url()).pathname === "/api/v1/me/import" &&
        call.method() === "POST"
      )
        requestKey = call.headers()["idempotency-key"];
    });
    await page.goto("/proyectos");
    const nav = page.getByRole("navigation", {
      name: "Principal",
      exact: true,
    });
    await expect(nav.getByRole("link").first()).toHaveAccessibleName("Hoy");
    await expect(nav.getByRole("link").nth(-2)).toHaveAccessibleName(
      "Importación",
    );
    await expect(nav.getByRole("link").last()).toHaveAccessibleName(
      "API para integraciones",
    );
    await nav.getByRole("link", { name: "Importación", exact: true }).click();
    await expect(
      page.getByRole("heading", { name: "Importar mis datos", level: 1 }),
    ).toBeFocused();
    expect(calls).toHaveLength(0);
    await page.getByLabel("Archivo JSON").setInputFiles({
      name: "copia-propia-é.json",
      mimeType: "application/json",
      buffer: bytes,
    });
    expect(calls).toHaveLength(0);
    const validating = page.waitForResponse(
      (response) =>
        new URL(response.url()).pathname === "/api/v1/me/import/preview",
    );
    await page
      .getByRole("button", { name: "Validar archivo", exact: true })
      .click();
    const previewResponse = await validating;
    expect(previewResponse.status()).toBe(200);
    const preview = await previewResponse.json();
    expect(preview.fileSha256).toBe(hash);
    expect(preview.byteLength).toBe(bytes.length);
    expect(preview.counts.projects).toBe(2);
    expect(preview.insertCounts.projects).toBe(1);
    expect(preview.identicalCounts.projects).toBe(1);
    expect(calls).toHaveLength(1);
    // Backend SHA256 and byteLength above verify the original File received.
    // DevTools does not expose this non-intercepted native File request body.
    expect(snapshot()).toBe(before);
    expect(sql(`SELECT count(*) FROM projects WHERE id='${addedId}'`)).toBe(
      "0",
    );
    const confirming = page.waitForResponse(
      (response) => new URL(response.url()).pathname === "/api/v1/me/import",
    );
    await page
      .getByRole("button", { name: "Confirmar importación", exact: true })
      .click();
    const response = await confirming;
    expect(response.status()).toBe(200);
    const receipt = await response.json();
    requestKey = receipt.requestKey;
    expect(requestKey).toMatch(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/,
    );
    expect(receipt.outcome).toBe("IMPORTED");
    expect(receipt.fileSha256).toBe(hash);
    expect(receipt.byteLength).toBe(bytes.length);
    expect(receipt.insertedCounts.projects).toBe(1);
    expect(receipt.identicalCounts.projects).toBe(1);
    await expect(
      page.getByRole("table", { name: "Resultado confirmado" }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Importar mis datos" }),
    ).toBeFocused();
    expect(calls).toHaveLength(2);
    expect(calls[1].headers()["idempotency-key"]).toBe(requestKey);
    expect(calls[1].headers()["x-import-content-sha256"]).toBe(hash);
    expect(
      await page.evaluate(() =>
        sessionStorage.getItem("organizationweb.import.pending.v1"),
      ),
    ).toBeNull();
    expect(snapshot()).toBe(before);
    expect(
      JSON.parse(
        sql(
          `SELECT jsonb_build_object('id',id,'owner',owner_id,'name',name,'description',description,'version',version::text) FROM projects WHERE id='${addedId}'`,
        ),
      ),
    ).toEqual({
      id: addedId,
      owner: "e2e-user",
      name: added.name,
      description: added.description,
      version: added.version,
    });
    const recovered = await request.get(
      "/api/v1/me/imports/by-key/" + requestKey,
    );
    expect(recovered.status()).toBe(200);
    expect(await recovered.json()).toEqual(receipt);
    expect(
      sql(
        `SELECT count(*) FROM import_receipts WHERE owner_id='e2e-user' AND request_key='${requestKey}'`,
      ),
    ).toBe("1");
    await writeFile(
      testInfo.outputPath("import-real.json"),
      JSON.stringify(
        {
          originalProject: project.id,
          addedProject: addedId,
          fileSha256: hash,
          byteLength: bytes.length,
          preview,
          receipt,
          existingAndEventsUnchanged: true,
        },
        null,
        2,
      ),
    );
  } finally {
    if (requestKey && /^[0-9a-f-]{36}$/.test(requestKey))
      sql(
        `DELETE FROM import_receipts WHERE owner_id='e2e-user' AND request_key='${requestKey}'`,
      );
    // Guarded helper permits SQL only in our ephemeral E2E stack.
    sql(
      `DELETE FROM outbox_events WHERE aggregate_id='${project.id}'; DELETE FROM projects WHERE id IN ('${project.id}','${addedId}') AND owner_id='e2e-user'`,
    );
  }
});
