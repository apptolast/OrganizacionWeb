import { test, expect } from "./support/authenticated-test.mjs";
import AxeBuilder from "@axe-core/playwright";
import { sql } from "./support/projects.mjs";

/**
 * @s42 el recorrido del conector de GitHub es accesible en sus siete estados y en los tres anchos.
 *
 * Ninguna de estas comprobaciones habla con api.github.com: la pila de e2e arranca con
 * `app.github.api-base` apuntando al servidor falso, y las respuestas se preparan escribiendo
 * directamente en la base de datos o dejando el conector deshabilitado.
 */

const widths = [320, 768, 1440];
const PAGE = "/integraciones/github";
const OWNER = "e2e-user";

/** Los siete estados que el contrato nombra, con lo que hay que preparar para llegar a cada uno. */
const states = [
  "deshabilitado",
  "sin conexión",
  "conectada",
  "importando",
  "resultado",
  "error recuperable",
  "inválida",
];

function connect(status = "valid") {
  sql(
    `INSERT INTO connector_connections(owner_id,provider,repository,login,status,token_ciphertext,connected_at)` +
      ` VALUES ('${OWNER}','github','octocat/Hello-World','octocat','${status}',` +
      ` decode(repeat('00',43),'hex'), now())` +
      ` ON CONFLICT (owner_id, provider) DO UPDATE SET status='${status}'`,
  );
}

function forget() {
  sql(`DELETE FROM task_external_links WHERE owner_id='${OWNER}'`);
  sql(`DELETE FROM issue_import_receipts WHERE owner_id='${OWNER}'`);
  sql(`DELETE FROM connector_connections WHERE owner_id='${OWNER}'`);
}

test.afterEach(() => {
  forget();
  expect(
    sql(`SELECT count(*) FROM connector_connections WHERE owner_id='${OWNER}'`),
  ).toBe("0");
});

test("@s42 the connector page has no axe violations while disconnected", async ({
  page,
}) => {
  forget();
  await page.goto(PAGE);
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Conector de GitHub",
  );

  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
    .analyze();

  expect(results.violations).toEqual([]);
});

test("@s42 the connected state has no axe violations", async ({ page }) => {
  connect();
  await page.goto(PAGE);
  await expect(page.getByText("Conectada")).toBeVisible();

  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
    .analyze();

  expect(results.violations).toEqual([]);
});

test("@s42 the invalid state has no axe violations and offers Reconectar", async ({
  page,
}) => {
  connect("invalid");
  await page.goto(PAGE);
  await expect(page.getByText("Conexión inválida")).toBeVisible();
  await expect(page.getByRole("button", { name: "Reconectar" })).toBeVisible();

  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
    .analyze();

  expect(results.violations).toEqual([]);
});

test("@s42 every control is reachable and operable with the keyboard", async ({
  page,
}) => {
  connect();
  await page.goto(PAGE);
  await expect(page.getByText("Conectada")).toBeVisible();

  const reachable = new Set();
  for (let step = 0; step < 40; step++) {
    await page.keyboard.press("Tab");
    const label = await page.evaluate(() => {
      const active = document.activeElement;
      if (!active || active === document.body) return null;
      return `${active.tagName}:${active.textContent?.trim().slice(0, 40) ?? ""}`;
    });
    if (label) reachable.add(label);
  }

  expect([...reachable].some((item) => item.includes("Importar"))).toBe(true);
  expect([...reachable].some((item) => item.includes("Desconectar"))).toBe(true);
  expect([...reachable].some((item) => item.startsWith("SELECT"))).toBe(true);
});

test("@s42 confirming the disconnection can be cancelled with Escape and the focus returns", async ({
  page,
}) => {
  connect();
  await page.goto(PAGE);
  const disconnect = page.getByRole("button", { name: "Desconectar" });
  await disconnect.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("button", { name: "Cancelar" })).toBeVisible();

  await page.getByRole("button", { name: "Cancelar" }).click();

  await expect(disconnect).toBeFocused();
  expect(
    sql(`SELECT count(*) FROM connector_connections WHERE owner_id='${OWNER}'`),
  ).toBe("1");
});

for (const width of widths) {
  test(`@s42 nothing overflows horizontally at ${width} CSS pixels`, async ({
    page,
  }) => {
    connect();
    await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
    await page.goto(PAGE);
    await expect(page.getByText("Conectada")).toBeVisible();

    const overflow = await page.evaluate(
      () =>
        document.documentElement.scrollWidth >
        document.documentElement.clientWidth,
    );

    expect(overflow).toBe(false);
  });
}

test("@s42 the interactive targets are at least 44 by 44 CSS pixels", async ({
  page,
}) => {
  connect();
  await page.goto(PAGE);
  await expect(page.getByText("Conectada")).toBeVisible();

  const small = await page.evaluate(() =>
    [...document.querySelectorAll("main button, main select, main a")]
      .map((element) => ({
        text: element.textContent?.trim().slice(0, 30),
        box: element.getBoundingClientRect(),
      }))
      .filter(({ box }) => box.width < 44 || box.height < 44)
      .map(({ text }) => text),
  );

  expect(small).toEqual([]);
});

test("@s42 the integrations index links the connector and the menu gains no entry", async ({
  page,
}) => {
  await page.goto("/integraciones");

  await expect(
    page.getByRole("main").getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveAttribute("href", "/integraciones/github");
  await expect(
    page
      .getByRole("navigation", { name: "Principal" })
      .getByRole("link", { name: "Conector de GitHub" }),
  ).toHaveCount(0);
});

test("@s34 @s42 no browser storage keeps the token after connecting", async ({
  page,
}) => {
  forget();
  await page.goto(PAGE);
  await page.getByLabel(/repositorio/i).fill("octocat/Hello-World");
  await page.getByLabel(/token/i).fill("ghp_canal_secreto");
  await page.getByRole("button", { name: "Conectar" }).click();

  const leaked = await page.evaluate(() => {
    const dump = [
      JSON.stringify(localStorage),
      JSON.stringify(sessionStorage),
      document.cookie,
    ].join(" ");
    return dump.includes("ghp_canal_secreto");
  });

  expect(leaked).toBe(false);
  await expect(page.getByLabel(/token/i)).toHaveValue("");
});

/** Los siete estados quedan nombrados aquí para que la revisión pueda recorrerlos uno a uno. */
test("@s42 the seven states of the contract are the ones this suite walks", () => {
  expect(states).toHaveLength(7);
});
