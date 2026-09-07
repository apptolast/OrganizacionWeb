import { chromium } from "@playwright/test";
import { join, resolve } from "node:path";
import { loginSession } from "../scripts/session-client.mjs";
import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(() =>
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("start_work_session: native Chromium zoom preserves states at 200 percent @s28 @s32 @s33 @s40 @s41", async ({
  request,
  baseURL,
}) => {
  test.setTimeout(120_000);
  const project = await create(
    request,
    "Lectura deliberada y recuperación de una sesión con contexto suficiente",
  );
  const task = await saveTask(
    request,
    project.id,
    "Preparar notas claras y conservar las decisiones para el siguiente paso, con una revisión tranquila de la información importante 🧭",
  );
  const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`;
  const scratch = resolve(
    ".e2e-work",
    "start-work-native",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated session zoom QA",
      version: "1.0",
      permissions: ["tabs"],
      host_permissions: ["http://127.0.0.1/*"],
      background: { service_worker: "worker.js" },
    }),
  );
  await writeFile(
    join(extension, "worker.js"),
    "chrome.runtime.onInstalled.addListener(()=>{});",
  );
  const context = await chromium.launchPersistentContext(
    join(scratch, "browser"),
    {
      channel: "chromium",
      headless: false,
      viewport: null,
      baseURL,
      args: [
        `--disable-extensions-except=${extension}`,
        `--load-extension=${extension}`,
        "--window-size=1440,1000",
      ],
    },
  );
  try {
    await loginSession(context.request, {
      username: "e2e-user",
      password: "e2e-only-password",
    });
    const page = await context.newPage();
    const engine = "chromium";
    const folder = `${scratch}/evidence`;
    await mkdir(folder, { recursive: true });
    const widths = [320];
    const evidence = [];
    async function inspect(state) {
      for (const width of widths) {
        await expect.poll(() => page.evaluate(() => innerWidth)).toBe(width);
        const measured = await page.evaluate(() => ({
          width: innerWidth,
          height: innerHeight,
          scroll: document.documentElement.scrollWidth,
          controls: [
            ...document.querySelectorAll(
              'nav[aria-label="Principal"] a,main button,main a,main input,main select,header button',
            ),
          ]
            .filter((el) => el.getClientRects().length)
            .map((el) => {
              const box = el.getBoundingClientRect();
              return {
                name:
                  el.getAttribute("aria-label") ||
                  el.labels?.[0]?.textContent ||
                  el.textContent,
                x: box.x,
                y: box.y,
                width: box.width,
                height: box.height,
              };
            }),
        }));
        evidence.push({ state, ...measured });
        await writeFile(
          `${folder}/geometry.json`,
          JSON.stringify(evidence, null, 2),
        );
        expect(
          measured.scroll,
          `${state}:${width} page overflow`,
        ).toBeLessThanOrEqual(width);
        for (const box of measured.controls) {
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
        }
        for (let a = 0; a < measured.controls.length; a++)
          for (let b = a + 1; b < measured.controls.length; b++) {
            const x = measured.controls[a],
              y = measured.controls[b];
            expect(
              Math.min(x.x + x.width, y.x + y.width) - Math.max(x.x, y.x) > 1 &&
                Math.min(x.y + x.height, y.y + y.height) - Math.max(x.y, y.y) >
                  1,
              `${state}:${width} overlap ${x.name}/${y.name}`,
            ).toBe(false);
          }
        if (width === 320 || width === 1440) {
          const skip = await page.locator(".skip-link").evaluate((el) => {
            const box = el.getBoundingClientRect();
            return {
              focused: document.activeElement === el,
              top: box.top,
              bottom: box.bottom,
              scrollY,
              viewportHeight: innerHeight,
            };
          });
          await writeFile(
            `${folder}/${state}-${width}-skiplink.json`,
            JSON.stringify(skip, null, 2),
          );
          if (!skip.focused) expect(skip.bottom).toBeLessThanOrEqual(0);
          await page.evaluate(() => scrollTo(0, 0));
          await page.screenshot({
            path: `${folder}/${state}-${width}-viewport.png`,
          });
          const dimensions = await page.evaluate(() => ({
            width: document.documentElement.scrollWidth,
            height: document.documentElement.scrollHeight,
          }));
          const capture = await context.newCDPSession(page);
          const shot = await capture.send("Page.captureScreenshot", {
            format: "png",
            fromSurface: true,
            captureBeyondViewport: true,
            clip: {
              x: 0,
              y: 0,
              width: dimensions.width * 2,
              height: dimensions.height * 2,
              scale: 1,
            },
          });
          await writeFile(
            `${folder}/${state}-${width}.png`,
            Buffer.from(shot.data, "base64"),
          );
          await capture.detach();
        }
      }

      const axe = await new AxeBuilder({ page })
        .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
        .analyze();
      await writeFile(
        `${folder}/${state}-axe.json`,
        JSON.stringify(axe.violations, null, 2),
      );
      expect(axe.violations, state).toEqual([]);
    }
    let release;
    const gate = new Promise((resolve) => {
      release = resolve;
    });
    await page.route(`**${endpoint}`, async (route) => {
      const response = await route.fetch();
      expect(response.status()).toBe(201);
      await gate;
      await route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          type: "urn:organization:problem:storage_unavailable",
          title: "Almacenamiento no disponible.",
          status: 503,
          code: "STORAGE_UNAVAILABLE",
        }),
      });
    });
    await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
    const baseline = await page.evaluate(() => ({
      dpr: devicePixelRatio,
      inner: innerWidth,
      outer: outerWidth,
    }));
    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const zoom = await worker.evaluate(async () => {
      const [tab] = await chrome.tabs.query({
        url: "http://127.0.0.1:18080/*",
      });
      await chrome.tabs.setZoom(tab.id, 2);
      return chrome.tabs.getZoom(tab.id);
    });
    expect(zoom).toBe(2);
    await expect
      .poll(() => page.evaluate(() => devicePixelRatio))
      .toBe(baseline.dpr * 2);
    await worker.evaluate(
      async (width) => {
        const [tab] = await chrome.tabs.query({
          url: "http://127.0.0.1:18080/*",
        });
        await chrome.windows.update(tab.windowId, { width });
      },
      640 + baseline.outer - baseline.inner,
    );
    await expect.poll(() => page.evaluate(() => innerWidth)).toBe(320);
    await writeFile(
      `${folder}/zoom.json`,
      JSON.stringify(
        {
          zoom,
          baseline,
          current: await page.evaluate(() => ({
            dpr: devicePixelRatio,
            width: innerWidth,
          })),
        },
        null,
        2,
      ),
    );
    const section = page.getByRole("region", {
      name: "Sesión de trabajo",
      exact: true,
    });
    const input = section.getByLabel("Duración prevista (minutos)", {
      exact: true,
    });
    await expect(input).toHaveValue("");
    await inspect("absence");
    await input.fill("25");
    const start = section.getByRole("button", {
      name: "Empezar a trabajar",
      exact: true,
    });
    await start.focus();
    await start.evaluate((button) => {
      button.addEventListener(
        "keydown",
        (event) => {
          if (event.key !== "Enter") return;
          const began = performance.now();
          const section = button.closest("section");
          const observer = new MutationObserver(() => {
            if (
              [...section.querySelectorAll('[role="status"]')].some(
                (el) => el.textContent === "Iniciando sesión de trabajo",
              )
            ) {
              window.workSessionFeedbackMs = performance.now() - began;
              observer.disconnect();
            }
          });
          observer.observe(section, {
            subtree: true,
            childList: true,
            characterData: true,
          });
        },
        { once: true },
      );
    });
    try {
      await start.press("Enter");
      await expect(
        section.getByText("Iniciando sesión de trabajo", { exact: true }),
      ).toBeVisible();
      await expect(
        section.getByText("No hay una sesión de trabajo activa.", {
          exact: true,
        }),
      ).toHaveCount(0);
      const feedback = await page.evaluate(() => window.workSessionFeedbackMs);
      expect(feedback).toBeGreaterThanOrEqual(0);
      expect(feedback).toBeLessThan(400);
      await writeFile(
        `${folder}/feedback.json`,
        JSON.stringify({ milliseconds: feedback }, null, 2),
      );
      await expect(start).toBeFocused();
      await inspect("pending");
    } finally {
      release();
    }
    const check = section.getByRole("button", {
      name: "Comprobar inicio",
      exact: true,
    });
    await expect(check).toBeVisible();
    await expect(
      section.getByText("No hay una sesión de trabajo activa.", {
        exact: true,
      }),
    ).toHaveCount(0);
    await expect(input).toHaveValue("25");
    await inspect("uncertain");
    await check.focus();
    await check.press("Enter");
    await expect(
      section.getByText("Sesión iniciada", { exact: true }),
    ).toBeVisible();
    await expect(
      section.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
    ).toBeFocused();
    await inspect("confirmed");
    expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
  } finally {
    await context.close();
  }
});
