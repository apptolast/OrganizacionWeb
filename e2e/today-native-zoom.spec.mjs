import { chromium } from "@playwright/test";
import { test, expect } from "./support/authenticated-test.mjs";
import { seedAgenda } from "./support/today.mjs";
import { sql } from "./support/projects.mjs";
import { loginSession } from "../scripts/session-client.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import AxeBuilder from "@axe-core/playwright";

test("today: native Chromium zoom preserves a real long agenda at 200 percent @s38", async ({
  request,
  baseURL,
}, testInfo) => {
  sql(
    "TRUNCATE block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  );
  const { project, task } = await seedAgenda(request);
  sql(
    `UPDATE projects SET name='${"Proyecto Unicode 🧭 con contexto largo ".repeat(3).trim()}' WHERE id='${project.id}'`,
  );
  const scratch = resolve(
    ".e2e-work",
    "today-native-zoom",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated Today zoom QA",
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
    await page.goto("/");
    await expect(
      page.getByRole("link", { name: task.title, exact: true }),
    ).toBeVisible();
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
    expect(narrow.scroll).toBeLessThanOrEqual(narrow.client);
    await expect(
      page.getByRole("link", { name: task.title, exact: true }),
    ).toBeVisible();
    for (const control of await page
      .locator('main button,main a,nav[aria-label="Principal"] a')
      .all()) {
      const box = await control.boundingBox();
      expect(box.width).toBeGreaterThanOrEqual(44);
      expect(box.height).toBeGreaterThanOrEqual(44);
    }
    expect(
      (
        await new AxeBuilder({ page })
          .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
          .analyze()
      ).violations,
    ).toEqual([]);
    await page.evaluate(() => scrollTo(0, 0));
    const dimensions = await page.evaluate(() => ({
      width: document.documentElement.scrollWidth,
      height: document.documentElement.scrollHeight,
    }));
    const session = await context.newCDPSession(page);
    const result = await session.send("Page.captureScreenshot", {
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
      testInfo.outputPath("today-native-zoom-200.png"),
      Buffer.from(result.data, "base64"),
    );
    await session.detach();
    await writeFile(
      testInfo.outputPath("today-native-zoom.json"),
      JSON.stringify(
        {
          baseline,
          wide,
          narrow,
          zoom,
          fixture: process.env.E2E_COMPOSE_PROJECT,
          source:
            "Real API/PG, isolated native Chromium zoom; no CSS zoom or emulated viewport",
        },
        null,
        2,
      ),
    );
  } finally {
    await context.close();
  }
});
