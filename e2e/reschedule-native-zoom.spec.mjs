import { chromium } from "@playwright/test";
import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";
import { configure } from "./support/blocks.mjs";
import { csrfHeaders, loginSession } from "../scripts/session-client.mjs";
import { randomUUID } from "node:crypto";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import AxeBuilder from "@axe-core/playwright";

test("reschedule: native Chromium zoom preserves movement and history at 200 percent @s40", async ({
  request,
  baseURL,
}, testInfo) => {
  test.setTimeout(120_000);
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const availability = await configure(request);
  const project = await create(request, "Proyecto para zoom nativo real");
  const task = await saveTask(
    request,
    project.id,
    "Conservar información y controles al ampliar 🧭",
  );
  const objective =
    "Revisar una propuesta legible y conservar el siguiente paso 🧭";
  const created = await request.post(
    `/api/v1/projects/${project.id}/tasks/${task.id}/blocks`,
    {
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
    },
  );
  expect(created.status(), await created.text()).toBe(201);
  const scratch = resolve(
    ".e2e-work",
    "reschedule-native-zoom",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated reschedule zoom QA",
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
    await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
    await page
      .getByRole("button", { name: `Mover bloque: ${objective}`, exact: true })
      .click();
    const editor = page.getByRole("region", {
      name: "Mover bloque",
      exact: true,
    });
    await expect(
      editor.getByLabel("Inicio local", { exact: true }),
    ).toHaveValue("2030-01-07T10:00");
    const measure = () =>
      page.evaluate(() => ({
        innerWidth,
        outerWidth,
        dpr: devicePixelRatio,
        scroll: document.documentElement.scrollWidth,
        client: document.documentElement.clientWidth,
      }));
    const baseline = await measure();
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
    const wide = await measure();
    await worker.evaluate(
      async (width) => {
        const [tab] = await chrome.tabs.query({
          url: "http://127.0.0.1:18080/*",
        });
        await chrome.windows.update(tab.windowId, { width });
      },
      640 + baseline.outerWidth - baseline.innerWidth,
    );
    await expect.poll(() => page.evaluate(() => innerWidth)).toBe(320);
    const narrow = await measure();
    const states = [];
    async function inspect(state) {
      const geometry = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            'main button,main a,main input,main select,nav[aria-label="Principal"] a',
          ),
        ]
          .filter((el) => el.getClientRects().length)
          .map((el) => {
            const b = el.getBoundingClientRect();
            return {
              name:
                el.getAttribute("aria-label") ||
                el.labels?.[0]?.textContent ||
                el.textContent,
              x: b.x,
              width: b.width,
              height: b.height,
            };
          }),
      }));
      expect(geometry.scroll).toBeLessThanOrEqual(geometry.width);
      for (const box of geometry.controls) {
        expect(box.x).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width).toBeLessThanOrEqual(geometry.width + 1);
        expect(box.width).toBeGreaterThanOrEqual(44);
        expect(box.height).toBeGreaterThanOrEqual(44);
      }
      const violations = (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations;
      expect(violations).toEqual([]);
      states.push({ state, ...geometry, violations });
      await page.evaluate(() => scrollTo(0, 0));
      await page.screenshot({
        path: testInfo.outputPath(`reschedule-native-${state}-viewport.png`),
        fullPage: false,
      });
      const dimensions = await page.evaluate(() => ({
        width: document.documentElement.scrollWidth,
        height: document.documentElement.scrollHeight,
      }));
      const session = await context.newCDPSession(page);
      const shot = await session.send("Page.captureScreenshot", {
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
        testInfo.outputPath("reschedule-native-" + state + "-full.png"),
        Buffer.from(shot.data, "base64"),
      );
      await session.detach();
    }
    await inspect("move");
    await editor
      .getByLabel("Inicio local", { exact: true })
      .fill("2030-01-07T12:00");
    await editor
      .getByLabel("Fin local", { exact: true })
      .fill("2030-01-07T13:00");
    await editor
      .getByRole("button", { name: "Revisar movimiento", exact: true })
      .click();
    await expect(
      editor.getByRole("region", {
        name: "Revisión del movimiento",
        exact: true,
      }),
    ).toBeVisible();
    await inspect("review");
    await editor
      .getByRole("button", { name: "Confirmar movimiento", exact: true })
      .click();
    await expect(
      page.getByText("Cambio confirmado (hecho histórico)", { exact: true }),
    ).toBeVisible();
    await page
      .getByRole("button", {
        name: `Cancelar bloque: ${objective}`,
        exact: true,
      })
      .click();
    const cancel = page.getByRole("region", {
      name: "Cancelar bloque",
      exact: true,
    });
    await expect(
      cancel.getByRole("button", {
        name: "Confirmar cancelación del bloque",
        exact: true,
      }),
    ).toBeVisible();
    await inspect("cancel");
    await cancel
      .getByRole("button", {
        name: "Confirmar cancelación del bloque",
        exact: true,
      })
      .click();
    await expect(
      page.getByRole("heading", { name: "Bloques planificados", exact: true }),
    ).toBeFocused();
    await page
      .getByRole("button", { name: "Ver cambios de bloques", exact: true })
      .click();
    await expect(
      page
        .getByRole("list", { name: "Historial de bloques", exact: true })
        .getByRole("listitem"),
    ).toHaveCount(2);
    await inspect("history");
    await writeFile(
      testInfo.outputPath("reschedule-native-zoom.json"),
      JSON.stringify(
        {
          baseline,
          wide,
          narrow,
          zoom,
          states,
          fixture: process.env.E2E_COMPOSE_PROJECT,
          source:
            "Real API/PG and native Chromium zoom, no CSS zoom or emulated viewport",
        },
        null,
        2,
      ),
    );
  } finally {
    await context.close();
  }
});
