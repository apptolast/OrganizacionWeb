import { test, expect } from "./support/authenticated-test.mjs";
import AxeBuilder from "@axe-core/playwright";
import { create, sql } from "./support/projects.mjs";

// Cubre @s37, @s39, @s40 y la matriz de @s42 sobre la pila real.

const widths = [320, 768, 1280, 1440];

function clearRules() {
  sql(
    "DELETE FROM automation_runs WHERE owner_id='e2e-user'; DELETE FROM automation_rules WHERE owner_id='e2e-user'; DELETE FROM automation_cursors WHERE owner_id='e2e-user'",
  );
}

test.beforeEach(() => clearRules());
test.afterEach(() => clearRules());

test("automatizaciones: la pagina se abre desde la navegacion @s37", async ({
  page,
}) => {
  await page.goto("/");
  await page.getByRole("link", { name: "Automatizaciones" }).click();
  await expect(page).toHaveURL(/\/automatizaciones$/);
  await expect(
    page.getByRole("heading", { level: 1, name: "Automatizaciones" }),
  ).toBeVisible();
  await expect(
    page.getByText(/todavía no tienes ninguna regla/i),
  ).toBeVisible();
});

test("automatizaciones: simular no guarda y guardar deja la regla en la lista @s39 @s40", async ({
  page,
  request,
}) => {
  await create(request, "Marketing");
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
  await expect(page.getByRole("switch", { name: /Seguimiento/ })).toBeVisible();
  expect(
    sql("SELECT count(*) FROM automation_rules WHERE owner_id='e2e-user'"),
  ).toBe("1");
});

for (const width of widths)
  test(`automatizaciones: sin desplazamiento horizontal ni violaciones axe a ${width} px @s42`, async ({
    page,
    request,
  }) => {
    await create(request, "Marketing");
    await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
    await page.goto("/automatizaciones");
    await page.getByRole("button", { name: "Nueva regla" }).click();
    await expect(page.getByLabel(/nombre/i)).toBeVisible();

    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
    ).toBe(true);

    for (const control of await page
      .getByRole("button")
      .or(page.getByRole("switch"))
      .all()) {
      if (!(await control.isVisible())) continue;
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
  request,
}) => {
  await create(request, "Marketing");
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto("/automatizaciones");
  await page.getByRole("button", { name: "Nueva regla" }).click();
  await expect(page.getByLabel(/nombre/i)).toBeVisible();

  // Zoom de texto, no de disposición: se dobla el tamaño calculado de cada
  // elemento con su atributo style, como el resto de auditorías del repositorio.
  // Una hoja inline la bloquearía la CSP de la feature 24.
  await page.evaluate(() => {
    const elements = [...document.querySelectorAll("main,main *")].filter(
      (element) => element instanceof HTMLElement,
    );
    const sizes = elements.map((element) =>
      parseFloat(getComputedStyle(element).fontSize),
    );
    elements.forEach((element, index) => {
      element.style.fontSize = sizes[index] * 2 + "px";
    });
  });

  expect(
    await page.evaluate(
      () =>
        document.documentElement.scrollWidth <=
        document.documentElement.clientWidth,
    ),
  ).toBe(true);
});
