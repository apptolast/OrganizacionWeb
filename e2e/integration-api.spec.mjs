import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { randomUUID } from "node:crypto";

test("integration API: create once, use scoped Bearer, hide secret and revoke with real persistence @s33 @s37 @s40 @s42", async ({
  page,
  request,
}) => {
  const project = await create(request, "Integración real " + randomUUID());
  let credentialId;
  try {
    await page.goto("/integraciones/api");
    await expect(
      page.getByRole("heading", { name: "Credenciales para integraciones" }),
    ).toBeFocused();
    const name = "Lectura propia " + randomUUID();
    await page.getByRole("textbox", { name: "Nombre", exact: true }).fill(name);
    await page
      .getByRole("checkbox", { name: "Leer proyectos", exact: true })
      .check();
    const creating = page.waitForResponse(
      (response) =>
        response.request().method() === "PUT" &&
        /\/api\/v1\/me\/api-credentials\/[a-f0-9-]{36}$/.test(
          new URL(response.url()).pathname,
        ),
    );
    await page.getByRole("button", { name: "Crear", exact: true }).click();
    const response = await creating;
    expect(response.status()).toBe(201);
    expect(response.headers()["cache-control"]).toContain("no-store");
    const created = await response.json();
    expect(created.credential.id).toMatch(
      /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/,
    );
    credentialId = created.credential.id;
    const input = page.getByLabel("Secreto de la credencial");
    await expect(input).toBeVisible();
    const secret = await input.inputValue();
    await page.getByRole("button", { name: "Cerrar secreto" }).click();
    expect(secret === created.secret).toBe(true);
    expect(
      sql(
        `SELECT count(*) FROM api_credentials WHERE id='${credentialId}' AND owner_id='e2e-user' AND octet_length(verifier)=32`,
      ),
    ).toBe("1");
    const own = await request.get(`/api/v1/projects/${project.id}`, {
      headers: { Authorization: `Bearer ${secret}` },
    });
    expect(own.status()).toBe(200);
    expect((await own.json()).id).toBe(project.id);
    expect(own.headers()["set-cookie"]).toBeUndefined();
    const forbidden = await request.post("/api/v1/projects", {
      headers: { Authorization: `Bearer ${secret}` },
      data: { name: "No crear", description: "No crear" },
    });
    expect(forbidden.status()).toBe(403);
    expect((await forbidden.json()).code).toBe("API_SCOPE_DENIED");
    const metadata = await request.get(
      `/api/v1/me/api-credentials/${credentialId}`,
    );
    expect(metadata.status()).toBe(200);
    expect(Object.keys(await metadata.json()).sort()).toEqual(
      ["id", "name", "scopes", "createdAt", "expiresAt", "revokedAt"].sort(),
    );
    await page.reload();
    await expect(
      page.getByRole("button", { name: "Revocar " + name, exact: true }),
    ).toBeVisible();
    await expect(page.getByLabel("Secreto de la credencial")).toHaveCount(0);
    await page
      .getByRole("button", { name: "Revocar " + name, exact: true })
      .click();
    const revoking = page.waitForResponse(
      (value) =>
        new URL(value.url()).pathname ===
        `/api/v1/me/api-credentials/${credentialId}/revocation`,
    );
    await page.getByRole("button", { name: "Confirmar revocación" }).click();
    expect((await revoking).status()).toBe(200);
    await expect(page.getByText("Revocada", { exact: true })).toBeVisible();
    const retired = await request.get(`/api/v1/projects/${project.id}`, {
      headers: { Authorization: `Bearer ${secret}` },
    });
    expect(retired.status()).toBe(401);
    expect((await retired.json()).code).toBe("API_UNAUTHENTICATED");
    expect(retired.headers()["set-cookie"]).toBeUndefined();
    expect(
      sql(
        `SELECT count(*) FROM api_credentials WHERE id='${credentialId}' AND revoked_at IS NOT NULL`,
      ),
    ).toBe("1");
  } finally {
    await page.goto("about:blank").catch(() => {});
    if (credentialId)
      sql(
        `DELETE FROM api_credential_quotas WHERE credential_id='${credentialId}'; DELETE FROM api_credentials WHERE id='${credentialId}' AND owner_id='e2e-user'; DELETE FROM api_owner_quotas WHERE owner_id='e2e-user'`,
      );
    sql(
      `DELETE FROM outbox_events WHERE aggregate_id='${project.id}'; DELETE FROM projects WHERE id='${project.id}' AND owner_id='e2e-user'`,
    );
  }
});
