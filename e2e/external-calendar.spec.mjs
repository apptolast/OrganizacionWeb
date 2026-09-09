import { test, expect } from "./support/authenticated-test.mjs";
import { sql } from "./support/projects.mjs";

const SECRET = "https://calendar.google.com/calendar/ical/e2e/private-WXYZ.ics";

test.afterEach(() => {
  sql("DELETE FROM external_calendar_subscriptions WHERE owner_id='e2e-user'");
});

test("calendario externo: alta, cifrado en reposo, sincronización y borrado @s2 @s37 @s38 @s39", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await expect(
    page.getByRole("heading", { level: 1, name: "Calendario externo" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Sincronizar ahora" }),
  ).toHaveCount(0);

  await page.getByLabel("Etiqueta").fill("Trabajo");
  await page.getByLabel("Dirección secreta iCal").fill(SECRET);
  const saving = page.waitForResponse(
    (response) =>
      response.request().method() === "PUT" &&
      new URL(response.url()).pathname === "/api/v1/me/external-calendar",
  );
  await page.getByRole("button", { name: "Guardar" }).click();
  const saved = await saving;
  expect(saved.status()).toBe(200);
  expect(saved.headers()["cache-control"]).toContain("no-store");
  const body = await saved.json();
  expect(Object.keys(body.subscription)).toHaveLength(15);
  expect(body.subscription.urlHost).toBe("calendar.google.com");
  expect(body.subscription.urlTail).toBe(".ics");
  expect(JSON.stringify(body)).not.toContain("private-WXYZ");

  // @s2: la fila guarda nonce de 12 bytes más el sellado y jamás la URL en claro.
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_subscriptions WHERE owner_id='e2e-user'" +
        " AND version=0 AND octet_length(url_ciphertext) > 12" +
        " AND position('private-WXYZ' in encode(url_ciphertext,'escape')) = 0",
    ),
  ).toBe("1");
  await expect(page.getByLabel("Dirección secreta iCal")).toHaveValue("");
  await expect(page.getByText("calendar.google.com")).toBeVisible();
  expect(await page.content()).not.toContain("private-WXYZ");

  // @s38: sincronizar contra un proveedor que no existe deja constancia del fallo
  // sin perder la suscripción, y nunca escribe la ruta secreta en la respuesta.
  const syncing = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      new URL(response.url()).pathname === "/api/v1/me/external-calendar/sync",
  );
  await page.getByRole("button", { name: "Sincronizar ahora" }).click();
  const synced = await syncing;
  expect(synced.status()).toBe(200);
  const outcome = await synced.json();
  expect(outcome.performed).toBe(true);
  expect(JSON.stringify(outcome)).not.toContain("private-WXYZ");
  expect(["OK", "FAILED"]).toContain(outcome.subscription.lastStatus);
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_subscriptions" +
        " WHERE owner_id='e2e-user' AND last_attempt_at IS NOT NULL",
    ),
  ).toBe("1");

  // @s39: eliminar exige confirmación y arrastra la instantánea.
  await page.getByRole("button", { name: "Eliminar suscripción" }).click();
  await page.getByRole("button", { name: "Cancelar" }).click();
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_subscriptions WHERE owner_id='e2e-user'",
    ),
  ).toBe("1");
  const deleting = page.waitForResponse(
    (response) => response.request().method() === "DELETE",
  );
  await page.getByRole("button", { name: "Eliminar suscripción" }).click();
  await page.getByRole("button", { name: "Sí, eliminar" }).click();
  expect((await deleting).status()).toBe(204);
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_subscriptions WHERE owner_id='e2e-user'",
    ),
  ).toBe("0");
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_events WHERE owner_id='e2e-user'",
    ),
  ).toBe("0");
  await expect(page.getByLabel("Etiqueta")).toHaveValue("");
});

test("calendario externo: la guardia de direcciones rechaza redes internas @s4 @s11", async ({
  page,
}) => {
  await page.goto("/calendario-externo");
  await page.getByLabel("Etiqueta").fill("Interno");
  await page
    .getByLabel("Dirección secreta iCal")
    .fill("https://10.0.0.5/privado.ics");
  const rejecting = page.waitForResponse(
    (response) => response.request().method() === "PUT",
  );
  await page.getByRole("button", { name: "Guardar" }).click();
  const rejected = await rejecting;
  expect(rejected.status()).toBe(400);
  const problem = await rejected.json();
  expect(problem.code).toBe("VALIDATION_ERROR");
  expect(problem.errors[0].field).toBe("url");
  await expect(page.getByLabel("Dirección secreta iCal")).toHaveValue(
    "https://10.0.0.5/privado.ics",
  );
  expect(
    sql(
      "SELECT count(*) FROM external_calendar_subscriptions WHERE owner_id='e2e-user'",
    ),
  ).toBe("0");
});

test("calendario externo: Hoy sigue mostrando su agenda sin suscripción @s34 @s36", async ({
  page,
}) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Calendario externo" }),
  ).toHaveCount(0);
  await expect(page.getByRole("alert")).toHaveCount(0);
});
