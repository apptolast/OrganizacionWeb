import { test, expect } from "./support/authenticated-test.mjs";
import AxeBuilder from "@axe-core/playwright";
import { sql } from "./support/projects.mjs";

// Escrito para @s42 y el recorrido de @s37-@s41. NO ejecutado en el carril de
// automatizaciones por la contencion de recursos: hay cinco carriles compartiendo
// la maquina y el arnes E2E levanta una pila docker completa. Debe ejecutarlo el
// coordinador con turno exclusivo antes del cierre.

const widths = [320, 768, 1280, 1440];

test.afterEach(() => {
  sql("DELETE FROM automation_runs WHERE owner_id='e2e-user'");
  sql("DELETE FROM automation_rules WHERE owner_id='e2e-user'");
  sql("DELETE FROM automation_cursors WHERE owner_id='e2e-user'");
});

test("automatizaciones: la pagina se abre desde la navegacion y lista sus reglas @s37", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Automatizaciones" }).click();
  await expect(page).toHaveURL(/\/automatizaciones$/);
  await expect(
    page.getByRole("heading", { level: 1, name: "Automatizaciones" }),
  ).toBeVisible();
});

test("automatizaciones: simular no guarda y guardar deja la regla en la lista @s39 @s40", async ({
  page,
}) => {
  await page.goto("/automatizaciones");
  await page.getByRole("button", { name: "Nueva regla" }).click();
  await page.getByLabel(/nombre/i).fill("Seguimiento");
  await page.getByRole("button", { name: "Simular" }).click();
  await expect(
    page.getByRole("status", { name: "Resultado de la simulación" }),
  ).toBeVisible();
  expect(
    sql("SELECT count(*) FROM automation_rules WHERE owner_id='e2e-user'"),
  ).toBe("0");
  await page.getByRole("button", { name: "Guardar" }).click();
  await expect(page.getByText("Seguimiento")).toBeVisible();
  expect(
    sql("SELECT count(*) FROM automation_rules WHERE owner_id='e2e-user'"),
  ).toBe("1");
});

for (const width of widths)
  test(`automatizaciones: sin desplazamiento horizontal ni violaciones axe a ${width} px @s42`, async ({
    page,
  }) => {
    await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
    await page.goto("/automatizaciones");
    await page.getByRole("button", { name: "Nueva regla" }).click();
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
    ).toBe(true);
    for (const control of await page.getByRole("button").all()) {
      const box = await control.boundingBox();
      if (box) {
        expect(box.width).toBeGreaterThanOrEqual(44);
        expect(box.height).toBeGreaterThanOrEqual(44);
      }
    }
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
      .analyze();
    expect(
      results.violations.filter((violation) =>
        ["serious", "critical"].includes(violation.impact),
      ),
    ).toEqual([]);
  });

test("automatizaciones: el texto al 200 % no corta contenido a 1440 px @s42", async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/automatizaciones");
  await page.addStyleTag({ content: "html { font-size: 200% !important; }" });
  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
});
