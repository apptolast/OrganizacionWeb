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

test("reschedule: retained preview announces promptly and keeps keyboard focus through failure and retry @s40", async ({
  page,
  request,
}) => {
  const availability = await configure(request);
  const project = await create(request, "Respuesta temprana y espera honesta");
  const task = await saveTask(
    request,
    project.id,
    "Reintentar sin perder el borrador",
  );
  const objective = "Revisar un cambio con respuesta pendiente";
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`;
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
  const original = await created.json();
  const path = `${endpoint}/${original.id}/reschedule/preview`;
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  let calls = 0;
  await page.route(`**${path}`, async (route) => {
    calls++;
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    if (calls === 1) {
      await gate;
      await route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "about:blank",
          title: "Almacenamiento no disponible.",
          status: 503,
          code: "STORAGE_UNAVAILABLE",
        }),
      });
    } else await route.fulfill({ response });
  });
  const folder = `.e2e-work/reschedule-real/${page.context().browser().browserType().name()}/feedback`;
  await mkdir(folder, { recursive: true });
  await page.setViewportSize({ width: 320, height: 700 });
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
  await review.evaluate((button) => {
    button.addEventListener(
      "keydown",
      (event) => {
        if (event.key !== "Enter") return;
        const started = performance.now();
        const observer = new MutationObserver(() => {
          if (
            [
              ...button.closest("form").querySelectorAll('[role="status"]'),
            ].some((el) => el.textContent === "Revisando movimiento")
          ) {
            window.rescheduleFeedbackMs = performance.now() - started;
            observer.disconnect();
          }
        });
        observer.observe(button.closest("form"), {
          subtree: true,
          childList: true,
          characterData: true,
        });
      },
      { once: true },
    );
  });
  try {
    await page.keyboard.press("Enter");
    await expect(editor.getByRole("status")).toHaveText("Revisando movimiento");
    const feedback = await page.evaluate(() => window.rescheduleFeedbackMs);
    expect(feedback).toBeGreaterThanOrEqual(0);
    expect(feedback).toBeLessThan(400);
    await expect(review).toBeFocused();
    await expect(review).toHaveAttribute("aria-disabled", "true");
    await page.keyboard.press("Enter");
    await expect.poll(() => calls).toBe(1);
    expect(sql("SELECT count(*) FROM block_changes")).toBe("0");
    const states = [];
    async function inspect(state) {
      const geometry = await editor.evaluate((panel) => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        controls: [...panel.querySelectorAll("button,input,select")].map(
          (el) => {
            const b = el.getBoundingClientRect();
            return { width: b.width, height: b.height, x: b.x };
          },
        ),
      }));
      expect(geometry.scroll).toBeLessThanOrEqual(geometry.width);
      for (const b of geometry.controls) {
        expect(b.width).toBeGreaterThanOrEqual(44);
        expect(b.height).toBeGreaterThanOrEqual(44);
      }
      const violations = (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations;
      expect(violations).toEqual([]);
      states.push({ state, geometry, violations });
      await page.screenshot({
        path: `${folder}/${state}-320.png`,
        fullPage: true,
      });
    }
    await inspect("pending");
    release();
    await expect(editor.getByRole("alert")).toContainText(
      "No se pudo revisar el movimiento.",
    );
    await expect(review).toBeFocused();
    await expect(review).toHaveAttribute("aria-disabled", "false");
    await expect(
      editor.getByLabel("Inicio local", { exact: true }),
    ).toHaveValue("2030-01-07T12:00");
    await inspect("error");
    await page.keyboard.press("Enter");
    await expect(
      editor.getByRole("region", {
        name: "Revisión del movimiento",
        exact: true,
      }),
    ).toBeVisible();
    await expect(editor.getByRole("alert")).toHaveCount(0);
    await expect(review).toBeFocused();
    expect(calls).toBe(2);
    expect(sql("SELECT count(*) FROM block_changes")).toBe("0");
    await writeFile(
      `${folder}/evidence.json`,
      JSON.stringify(
        {
          feedbackMs: feedback,
          calls,
          states,
          network:
            "Real preview response retained; first response replaced by controlled503, retry actual200",
          fixture: process.env.E2E_COMPOSE_PROJECT,
        },
        null,
        2,
      ),
    );
  } finally {
    release();
  }
});
