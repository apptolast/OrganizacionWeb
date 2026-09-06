import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(() =>
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("reschedule: main panel states retain readable layout with text at 200 percent @s40", async ({
  page,
  request,
}) => {
  test.setTimeout(120_000);
  const availability = await configure(request);
  const project = await create(
    request,
    "Proyecto de revisión visual y accesibilidad",
  );
  const task = await saveTask(
    request,
    project.id,
    "Preparar una propuesta legible 🧭 para revisar en equipo",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
  const objective =
    "Revisar una propuesta con información suficiente, conservar las decisiones y preparar el siguiente paso 🧭";
  const created = await request.post(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "Availability-Revision": availability,
      "Idempotency-Key": randomUUID(),
    },
    data: {
      objective,
      startLocal: "2030-01-07T10:00",
      endLocal: "2030-01-07T11:00",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  const widths = [320, 768, 1440];
  const engine = page.context().browser().browserType().name();
  const folder = `.e2e-work/reschedule-real/${engine}/text200`;
  await mkdir(folder, { recursive: true });
  const evidence = [];
  async function inspect(state) {
    const fontScale = await page.evaluate(() => {
      const elements = [...document.querySelectorAll("main,main *")];
      window.originalFontStyles ??= new WeakMap();
      for (const el of elements) {
        if (!window.originalFontStyles.has(el))
          window.originalFontStyles.set(el, el.style.fontSize);
        el.style.fontSize = window.originalFontStyles.get(el);
      }
      const sizes = elements.map((el) =>
        parseFloat(getComputedStyle(el).fontSize),
      );
      elements.forEach((el, i) => {
        el.style.fontSize = sizes[i] * 2 + "px";
      });
      return elements.map((el, i) => ({
        before: sizes[i],
        after: parseFloat(getComputedStyle(el).fontSize),
      }));
    });
    for (const size of fontScale)
      expect(size.after).toBeCloseTo(size.before * 2, 3);

    await page.setViewportSize({ width: 320, height: 700 });
    await page.screenshot({
      path: `${folder}/${state}-320.png`,
      fullPage: true,
    });
    for (const width of widths) {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      const measure = await page.evaluate(() => ({
        width: innerWidth,
        height: innerHeight,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            'nav[aria-label="Principal"] a,main button,main a,main input,main select,header button',
          ),
        ]
          .filter((element) => element.getClientRects().length)
          .map((element) => {
            const box = element.getBoundingClientRect(),
              style = getComputedStyle(element);
            return {
              name:
                element.getAttribute("aria-label") ||
                element.labels?.[0]?.textContent ||
                element.textContent,
              tag: element.tagName,
              x: box.x,
              y: box.y,
              width: box.width,
              height: box.height,
              borderStyle: style.borderTopStyle,
              borderWidth: parseFloat(style.borderTopWidth),
            };
          }),
      }));
      evidence.push({ state, ...measure });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(evidence, null, 2),
      );
      expect(measure.scroll, `${state}:${width} overflow`).toBeLessThanOrEqual(
        width,
      );
      for (const box of measure.controls) {
        expect(
          box.x,
          `${state}:${width}:${box.name} left`,
        ).toBeGreaterThanOrEqual(0);
        expect(
          box.x + box.width,
          `${state}:${width}:${box.name} right`,
        ).toBeLessThanOrEqual(width + 1);
        expect(
          box.width,
          `${state}:${width}:${box.name} width`,
        ).toBeGreaterThanOrEqual(44);
        expect(
          box.height,
          `${state}:${width}:${box.name} height`,
        ).toBeGreaterThanOrEqual(44);
        if (box.tag === "INPUT" || box.tag === "SELECT") {
          expect(
            box.borderStyle,
            `${state}:${width}:${box.name} border`,
          ).not.toBe("none");
          expect(box.borderWidth).toBeGreaterThan(0);
        }
      }
      for (let a = 0; a < measure.controls.length; a++)
        for (let b = a + 1; b < measure.controls.length; b++) {
          const x = measure.controls[a],
            y = measure.controls[b];
          const intersectionWidth =
            Math.min(x.x + x.width, y.x + y.width) - Math.max(x.x, y.x);
          const intersectionHeight =
            Math.min(x.y + x.height, y.y + y.height) - Math.max(x.y, y.y);
          expect(
            intersectionWidth > 1 && intersectionHeight > 1,
            `${state}:${width} overlap ${x.name}/${y.name}`,
          ).toBe(false);
        }
    }
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.screenshot({
      path: `${folder}/${state}-1440.png`,
      fullPage: true,
    });
    await page.setViewportSize({ width: 320, height: 700 });
    await page.evaluate(() => scrollTo(0, 0));
    const skip = await page.locator(".skip-link").evaluate((el) => {
      const rect = el.getBoundingClientRect();
      return {
        focused: document.activeElement === el,
        x: rect.x,
        y: rect.y,
        width: rect.width,
        height: rect.height,
        bottom: rect.bottom,
        viewportHeight: innerHeight,
      };
    });
    expect(skip.focused).toBe(false);
    expect(skip.bottom).toBeLessThanOrEqual(0);
    await writeFile(
      folder + "/" + state + "-skiplink.json",
      JSON.stringify(skip, null, 2),
    );
    await page.screenshot({
      path: folder + "/" + state + "-viewport.png",
      fullPage: false,
    });
    const axe = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
      .analyze();
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(axe.violations, null, 2),
    );
    expect(axe.violations, state).toEqual([]);
  }
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const mover = page.getByRole("button", {
    name: `Mover bloque: ${objective}`,
    exact: true,
  });
  await mover.focus();
  await page.keyboard.press("Enter");
  const editor = page.getByRole("region", {
    name: "Mover bloque",
    exact: true,
  });
  await expect(editor.getByLabel("Inicio local", { exact: true })).toHaveValue(
    "2030-01-07T10:00",
  );
  await inspect("move");
  await editor
    .getByLabel("Inicio local", { exact: true })
    .fill("2030-01-07T12:00");
  await editor
    .getByLabel("Fin local", { exact: true })
    .fill("2030-01-07T13:00");
  const review = editor.getByRole("button", {
    name: "Revisar movimiento",
    exact: true,
  });
  await review.focus();
  await page.keyboard.press("Enter");
  await expect(
    editor.getByRole("region", {
      name: "Revisión del movimiento",
      exact: true,
    }),
  ).toBeVisible();
  await expect(review).toBeFocused();
  await inspect("review");
  const confirm = editor.getByRole("button", {
    name: "Confirmar movimiento",
    exact: true,
  });
  await confirm.focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByText("Cambio confirmado (hecho histórico)", { exact: true }),
  ).toBeVisible();
  const cancel = page.getByRole("button", {
    name: `Cancelar bloque: ${objective}`,
    exact: true,
  });
  await cancel.focus();
  await page.keyboard.press("Enter");
  const panel = page.getByRole("region", {
    name: "Cancelar bloque",
    exact: true,
  });
  const cancelConfirm = panel.getByRole("button", {
    name: "Confirmar cancelación del bloque",
    exact: true,
  });
  await expect(cancelConfirm).toBeVisible();
  await inspect("cancel");
  await cancelConfirm.focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("heading", { name: "Bloques planificados", exact: true }),
  ).toBeFocused();
  const history = page.getByRole("button", {
    name: "Ver cambios de bloques",
    exact: true,
  });
  await history.focus();
  await page.keyboard.press("Enter");
  await expect(
    page
      .getByRole("list", { name: "Historial de bloques", exact: true })
      .getByRole("listitem"),
  ).toHaveCount(2);
  await inspect("history");
  expect(sql("SELECT count(*) FROM block_changes")).toBe("2");
});
