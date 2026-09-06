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

test("reschedule: DST choices and excess consent remain operable by Tab with effective touch targets @s40", async ({
  page,
  request,
}) => {
  test.setTimeout(90_000);
  const availability = await configure(request, 30);
  const project = await create(request, "Ocurrencias y presupuesto explícitos");
  const task = await saveTask(
    request,
    project.id,
    "Revisar un cambio con ambigüedad horaria 🧭",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
  const objective = "Conservar una elección explícita y revisar el presupuesto";
  const created = await request.post(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "Availability-Revision": availability,
      "Idempotency-Key": randomUUID(),
    },
    data: {
      objective,
      startLocal: "2030-01-07T10:00",
      endLocal: "2030-01-07T10:30",
      zoneId: "UTC",
      startOffset: "Z",
      endOffset: "Z",
      allowOverBudget: false,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  const original = await created.json();
  const movePath = `${endpoint}/${original.id}/reschedule`;
  const folder = `.e2e-work/reschedule-real/${page.context().browser().browserType().name()}/conditional`;
  await mkdir(folder, { recursive: true });
  const evidence = [];
  async function inspect(state) {
    for (const width of [320, 768, 1440]) {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      const geometry = await page
        .getByRole("region", { name: "Mover bloque", exact: true })
        .evaluate((panel) => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          controls: [...panel.querySelectorAll("button,input,select")]
            .filter((el) => el.getClientRects().length)
            .map((el) => {
              const effective =
                el.type === "checkbox" ? el.closest("label") : el;
              const b = effective.getBoundingClientRect(),
                inner = el.getBoundingClientRect();
              return {
                name: el.labels?.[0]?.textContent || el.textContent,
                type: el.type,
                tag: effective.tagName,
                x: b.x,
                y: b.y,
                width: b.width,
                height: b.height,
                innerWidth: inner.width,
                innerHeight: inner.height,
              };
            }),
        }));
      evidence.push({ state, ...geometry });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(evidence, null, 2),
      );
      await page.screenshot({
        path: `${folder}/${state}-${width}.png`,
        fullPage: true,
      });
      expect(geometry.scroll, `${state}:${width} overflow`).toBeLessThanOrEqual(
        width,
      );
      for (const box of geometry.controls) {
        expect(box.x).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width).toBeLessThanOrEqual(width + 1);
        expect(box.width, box.name).toBeGreaterThanOrEqual(44);
        expect(box.height, box.name).toBeGreaterThanOrEqual(44);
      }
    }
    await page.setViewportSize({ width: 320, height: 700 });
    const violations = (
      await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
        .analyze()
    ).violations;
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(violations, null, 2),
    );
    expect(violations).toEqual([]);
  }
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  await page
    .getByRole("button", { name: `Mover bloque: ${objective}`, exact: true })
    .click();
  const editor = page.getByRole("region", {
    name: "Mover bloque",
    exact: true,
  });
  await expect(editor.getByLabel("Inicio local", { exact: true })).toHaveValue(
    "2030-01-07T10:00",
  );
  await editor
    .getByLabel("Zona del movimiento", { exact: true })
    .fill("Europe/Madrid");
  await editor
    .getByLabel("Inicio local", { exact: true })
    .fill("2030-10-27T02:15");
  await editor
    .getByLabel("Fin local", { exact: true })
    .fill("2030-10-27T02:45");
  const review = editor.getByRole("button", {
    name: "Revisar movimiento",
    exact: true,
  });
  const startError = page.waitForResponse((r) =>
    r.url().endsWith(movePath + "/preview"),
  );
  await review.focus();
  await page.keyboard.press("Enter");
  expect((await startError).status()).toBe(400);
  const start = editor.getByLabel("Ocurrencia de inicio", { exact: true });
  await expect(start).toBeVisible();
  await expect(start).toHaveAttribute("aria-invalid", "true");
  await inspect("start-error");
  await review.focus();
  await page.keyboard.press("Tab");
  await expect(start).toBeFocused();
  await page.keyboard.press("Home");
  await page.keyboard.press("ArrowDown");
  await expect(start).toHaveValue("+02:00");
  await page.keyboard.press("Shift+Tab");
  await expect(review).toBeFocused();
  const endError = page.waitForResponse((r) =>
    r.url().endsWith(movePath + "/preview"),
  );
  await page.keyboard.press("Enter");
  expect((await endError).status()).toBe(400);
  const end = editor.getByLabel("Ocurrencia de fin", { exact: true });
  await expect(end).toBeVisible();
  await expect(end).toHaveAttribute("aria-invalid", "true");
  await inspect("end-error");
  await review.focus();
  await page.keyboard.press("Tab");
  await expect(start).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(end).toBeFocused();
  await page.keyboard.press("End");
  await expect(end).toHaveValue("+01:00");
  await page.keyboard.press("Shift+Tab");
  await page.keyboard.press("Shift+Tab");
  await expect(review).toBeFocused();
  const previewResponse = page.waitForResponse((r) =>
    r.url().endsWith(movePath + "/preview"),
  );
  await page.keyboard.press("Enter");
  const response = await previewResponse;
  expect(response.status()).toBe(200);
  const preview = await response.json();
  expect(preview).toMatchObject({
    startAt: "2030-10-27T00:15:00Z",
    endAt: "2030-10-27T01:45:00Z",
    durationMinutes: 90,
    days: [
      {
        date: "2030-10-27",
        budgetMinutes: 30,
        requestedSeconds: 5400,
        excessSeconds: 3600,
      },
    ],
  });
  const consent = editor.getByRole("checkbox", {
    name: "Acepto superar el presupuesto para este movimiento. Esto no permite solapes.",
    exact: true,
  });
  const confirm = editor.getByRole("button", {
    name: "Confirmar movimiento",
    exact: true,
  });
  await expect(consent).not.toBeChecked();
  await expect(confirm).toHaveAttribute("aria-disabled", "true");
  expect(sql("SELECT count(*) FROM block_changes")).toBe("0");
  await inspect("excess");
  await review.focus();
  await page.keyboard.press("Tab");
  await expect(start).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(end).toBeFocused();
  await page.keyboard.press("Tab");
  await expect(consent).toBeFocused();
  await page.keyboard.press("Space");
  await expect(consent).toBeChecked();
  await page.keyboard.press("Tab");
  await expect(confirm).toBeFocused();
  const committed = page.waitForResponse(
    (r) => r.url().endsWith(movePath) && r.request().method() === "POST",
  );
  await page.keyboard.press("Enter");
  const moved = await committed;
  expect(moved.status(), await moved.text()).toBe(201);
  expect(moved.request().postDataJSON()).toMatchObject({
    startOffset: "+02:00",
    endOffset: "+01:00",
    allowOverBudget: true,
  });
  await expect(
    page.getByText("Cambio confirmado (hecho histórico)", { exact: true }),
  ).toBeVisible();
  expect(sql("SELECT count(*) FROM block_changes")).toBe("1");
});
