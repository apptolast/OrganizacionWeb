import { test, expect } from "./support/authenticated-test.mjs";
import { create } from "./support/projects.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";

const widths = [
  320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  1601, 1920, 2560,
];

test("customization UX: view definitions and typed values preserve geometry and accessible structure @s40", async ({
  page,
  request,
}, testInfo) => {
  const project = await create(
    request,
    "Proyecto de campos personales accesibles",
  );
  const path = "/api/v1/me/customization/PROJECT";
  let current = await request.get(path);
  expect(current.status()).toBe(200);
  for (const [label, type] of [
    ["Nota con contexto personal", "TEXT"],
    ["Cantidad", "NUMBER"],
    ["Revisado", "BOOLEAN"],
    ["Fecha personal", "DATE"],
  ]) {
    current = await request.post(`${path}/fields`, {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": current.headers().etag,
      },
      data: { label, type },
    });
    expect(current.status()).toBe(200);
  }
  const folder = `.e2e-work/customization-real/${testInfo.project.name}/ux`;
  await mkdir(folder, { recursive: true });
  const evidence = [];
  async function inspect(state) {
    for (const width of widths) {
      await page.setViewportSize({ width, height: 900 });
      const geometry = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            ".customization button,.customization input,.customization select,.custom-fields button,.custom-fields input,.custom-fields textarea,.custom-fields select",
          ),
        ]
          .filter((el) => el.getClientRects().length)
          .map((el) => {
            const target = el.matches('input[type="checkbox"]')
              ? el.labels[0]
              : el;
            const r = target.getBoundingClientRect();
            return {
              target: target.tagName,
              glyphWidth: el.getBoundingClientRect().width,
              name:
                el.getAttribute("aria-label") ||
                el.labels?.[0]?.textContent ||
                el.textContent,
              x: r.x,
              y: r.y,
              width: r.width,
              height: r.height,
            };
          }),
      }));
      evidence.push({ state, ...geometry });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(evidence, null, 2),
      );
      if (width === 320 || width === 1440)
        await page.screenshot({
          path: `${folder}/${state}-${width}.png`,
          fullPage: true,
        });
      expect(geometry.scroll, `${state}:${width} overflow`).toBeLessThanOrEqual(
        width,
      );
      expect(geometry.controls.length).toBeGreaterThan(0);
      for (const box of geometry.controls) {
        expect(
          box.width,
          `${state}:${width}:${box.name} width`,
        ).toBeGreaterThanOrEqual(44);
        expect(
          box.height,
          `${state}:${width}:${box.name} height`,
        ).toBeGreaterThanOrEqual(44);
        expect(
          box.x,
          `${state}:${width}:${box.name} left`,
        ).toBeGreaterThanOrEqual(0);
        expect(
          box.x + box.width,
          `${state}:${width}:${box.name} right`,
        ).toBeLessThanOrEqual(width + 1);
      }
      for (let i = 0; i < geometry.controls.length; i++)
        for (let j = i + 1; j < geometry.controls.length; j++) {
          const a = geometry.controls[i],
            b = geometry.controls[j];
          expect(
            Math.min(a.x + a.width, b.x + b.width) - Math.max(a.x, b.x) > 1 &&
              Math.min(a.y + a.height, b.y + b.height) - Math.max(a.y, b.y) > 1,
            `${state}:${width} overlap ${a.name}/${b.name}`,
          ).toBe(false);
        }
    }
    await page.setViewportSize({ width: 320, height: 900 });
    const axe = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa", "best-practice"])
      .analyze();
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(axe.violations, null, 2),
    );
    expect(axe.violations).toEqual([]);
    await page.bringToFront();
  }
  await page.goto("/proyectos");
  await page
    .getByRole("button", { name: "Personalizar vista", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Gestionar campos personales", exact: true })
    .click();
  await page
    .getByRole("group", { name: "Nuevo campo", exact: true })
    .getByLabel("Etiqueta", { exact: true })
    .fill("Etiqueta personal larga con contexto Unicode 🧭");
  await page.setViewportSize({ width: 320, height: 900 });
  const checkbox = page.getByRole("checkbox", { name: "Creado", exact: true });
  const clickable = checkbox.locator("..");
  const box = await clickable.boundingBox();
  expect(box).not.toBeNull();
  await clickable.click({ position: { x: box.width - 4, y: box.height / 2 } });
  await expect(checkbox).not.toBeChecked();
  await clickable.click({ position: { x: box.width - 4, y: box.height / 2 } });
  await expect(checkbox).toBeChecked();
  await inspect("view-definitions");
  await page
    .getByRole("list", { name: "Proyectos guardados" })
    .getByRole("link", { name: project.name })
    .click();
  const values = page.getByRole("group", {
    name: "Valores personales",
    exact: true,
  });
  await values
    .getByLabel("Nota con contexto personal", { exact: true })
    .fill(
      "Una nota personal larga que conserva el contexto y los espacios. 🧭",
    );
  await values.getByLabel("Cantidad", { exact: true }).fill("0");
  await values
    .getByRole("combobox", { name: "Revisado", exact: true })
    .selectOption("false");
  await values.getByLabel("Fecha personal", { exact: true }).fill("2026-09-08");
  await inspect("typed-values");
});
