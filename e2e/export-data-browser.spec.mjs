import { test, expect, chromium } from "@playwright/test";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { resolve } from "node:path";
import AxeBuilder from "@axe-core/playwright";

// Browser-only evidence: every API response below is simulated explicitly.
// This does not validate the server snapshot, database or authenticated HTTP.
const owner = "ana";
const names =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
    " ",
  );
const bytes = Buffer.from(
  JSON.stringify(
    {
      format: "organizationweb-export",
      schemaVersion: 1,
      exportedAt: "2026-09-08T10:20:30.123456Z",
      owner,
      data: Object.fromEntries(names.map((name) => [name, []])),
      counts: Object.fromEntries(names.map((name) => [name, 0])),
    },
    null,
    2,
  ) + "\n",
);
const filename = "organizationweb-export-v1-20260908T102030123456Z.json";

async function simulatedApi(page, exportHandler) {
  const unexpected = [];
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (request.method() === "GET" && path === "/api/session") {
      await route.fulfill({
        json: {
          authenticated: true,
          username: owner,
          csrfToken: "simulated-private-token",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    } else if (request.method() === "GET" && path === "/api/v1/me/appearance") {
      await route.fulfill({
        headers: { ETag: '"appearance:unconfigured"' },
        json: {
          configured: false,
          theme: "SYSTEM",
          accentLight: "#244C3C",
          accentDark: "#B7E4C7",
          updatedAt: null,
        },
      });
    } else if (request.method() === "GET" && path === "/api/v1/me/export") {
      await exportHandler(route);
    } else {
      unexpected.push(request.method() + " " + path);
      await route.abort();
    }
  });
  return unexpected;
}
async function fulfillArchive(route) {
  // WebKit interception drops charset from MIME type. Serve the simulated
  // bytes over local HTTP so the browser receives the actual transport header.
  const server = createServer((_request, response) => {
    response.writeHead(200, {
      "Content-Type": "application/json; charset=utf-8",
      "Content-Length": String(bytes.length),
      "Content-Disposition": 'attachment; filename="' + filename + '"',
      "Access-Control-Allow-Origin": "*",
    });
    response.end(bytes, () => server.close());
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  try {
    await route.continue({
      url: "http://127.0.0.1:" + server.address().port + "/export",
    });
  } catch (error) {
    server.close();
    throw error;
  }
}

test("export browser simulated API: system theme, reduced motion, forced colors and short landscape @s32", async ({
  page,
}, testInfo) => {
  const unexpected = await simulatedApi(page, fulfillArchive);
  await page.emulateMedia({ colorScheme: "dark", reducedMotion: "reduce" });
  await page.setViewportSize({ width: 768, height: 360 });
  await page.goto("/exportacion");
  await expect(
    page.getByRole("heading", { name: "Exportar mis datos" }),
  ).toBeVisible();
  await expect
    .poll(() => page.evaluate(() => document.documentElement.dataset.theme))
    .toBe("dark");
  await page.emulateMedia({
    colorScheme: "light",
    reducedMotion: "reduce",
    forcedColors: "active",
  });
  await expect
    .poll(() => page.evaluate(() => document.documentElement.dataset.theme))
    .toBe("light");
  const prepare = page.getByRole("button", { name: "Preparar exportación" });
  await prepare.focus();
  await page.keyboard.press("Enter");
  const download = page.getByRole("link", { name: "Descargar archivo JSON" });
  await expect(download).toBeVisible();
  await download.focus();
  const state = await download.evaluate((el) => {
    const r = el.getBoundingClientRect(),
      style = getComputedStyle(el);
    const hit = document.elementFromPoint(
      r.x + r.width / 2,
      r.y + r.height / 2,
    );
    return {
      width: innerWidth,
      scroll: document.documentElement.scrollWidth,
      forced: matchMedia("(forced-colors: active)").matches,
      reduced: matchMedia("(prefers-reduced-motion: reduce)").matches,
      outline: style.outlineStyle,
      outlineWidth: parseFloat(style.outlineWidth),
      visible: el === hit || el.contains(hit),
      animations: document
        .getAnimations()
        .filter((a) => a.playState === "running").length,
    };
  });
  await writeFile(
    testInfo.outputPath("modalities.json"),
    JSON.stringify(state, null, 2),
  );
  await page.screenshot({ path: testInfo.outputPath("forced-landscape.png") });
  expect(state.forced).toBe(true);
  expect(state.reduced).toBe(true);
  expect(state.visible).toBe(true);
  expect(state.outline).not.toBe("none");
  expect(state.outlineWidth).toBeGreaterThanOrEqual(3);
  expect(state.animations).toBe(0);
  expect(state.scroll).toBeLessThanOrEqual(state.width);
  expect(unexpected).toEqual([]);
});

test("export browser simulated API: native Chromium zoom 200 at 320 CSS pixels @s32", async ({
  baseURL,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== "chromium",
    "Native extension zoom is measured in Chromium; other engines have text/reflow evidence.",
  );
  const folder = resolve(".e2e-work", "export-zoom-" + process.pid);
  const extension = resolve(folder, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    extension + "/manifest.json",
    JSON.stringify({
      manifest_version: 3,
      name: "Export zoom QA",
      version: "1.0",
      permissions: ["tabs"],
      host_permissions: ["http://127.0.0.1/*"],
      background: { service_worker: "worker.js" },
    }),
  );
  await writeFile(
    extension + "/worker.js",
    "chrome.runtime.onInstalled.addListener(()=>{});",
  );
  await writeFile(
    testInfo.outputPath("zoom-stage.json"),
    JSON.stringify({ stage: "launch", folder }),
  );
  const context = await chromium.launchPersistentContext(
    resolve(folder, "browser"),
    {
      channel: "chromium",
      headless: false,
      viewport: null,
      baseURL,
      args: [
        "--disable-extensions-except=" + extension,
        "--load-extension=" + extension,
        "--window-size=1440,1000",
      ],
    },
  );
  try {
    const page = await context.newPage();
    const unexpected = await simulatedApi(page, fulfillArchive);
    await page.goto("/exportacion");
    await expect(
      page.getByRole("heading", { name: "Exportar mis datos" }),
    ).toBeVisible();
    await writeFile(
      testInfo.outputPath("zoom-stage.json"),
      JSON.stringify({
        stage: "page-loaded",
        folder,
        workers: context.serviceWorkers().length,
      }),
    );
    const baseline = await page.evaluate(() => ({
      dpr: devicePixelRatio,
      inner: innerWidth,
      outer: outerWidth,
    }));
    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const zoom = await worker.evaluate(
      async ({ pattern, width }) => {
        const [tab] = await chrome.tabs.query({ url: pattern });
        await chrome.tabs.setZoom(tab.id, 2);
        await chrome.windows.update(tab.windowId, { width });
        return chrome.tabs.getZoom(tab.id);
      },
      { pattern: baseURL + "/*", width: 640 + baseline.outer - baseline.inner },
    );
    expect(zoom).toBe(2);
    await expect
      .poll(() => page.evaluate(() => devicePixelRatio))
      .toBe(baseline.dpr * 2);
    await expect.poll(() => page.evaluate(() => innerWidth)).toBe(320);
    await page.getByRole("button", { name: "Preparar exportación" }).click();
    const download = page.getByRole("link", { name: "Descargar archivo JSON" });
    await expect(download).toBeVisible();
    await download.focus();
    const geometry = await page.evaluate(() => ({
      width: innerWidth,
      scroll: document.documentElement.scrollWidth,
      dpr: devicePixelRatio,
    }));
    await writeFile(
      testInfo.outputPath("zoom.json"),
      JSON.stringify({ zoom, baseline, geometry }, null, 2),
    );
    await page.bringToFront();
    await download.scrollIntoViewIfNeeded();
    await page.evaluate(
      () =>
        new Promise((resolve) =>
          requestAnimationFrame(() => requestAnimationFrame(resolve)),
        ),
    );
    await page.screenshot({ path: testInfo.outputPath("zoom200.png") });
    const cdp = await context.newCDPSession(page);
    const frame = await cdp.send("Page.captureScreenshot", {
      format: "png",
      fromSurface: false,
    });
    await writeFile(
      testInfo.outputPath("zoom200-compositor.png"),
      Buffer.from(frame.data, "base64"),
    );
    expect(geometry.scroll).toBeLessThanOrEqual(320);
    expect(unexpected).toEqual([]);
  } finally {
    await context.close();
  }
});

test("export browser simulated API: responsive states, themes and enlarged text @s32", async ({
  page,
}, testInfo) => {
  test.setTimeout(120000);
  let responseKind = "pending";
  let release;
  let gate = new Promise((resolve) => {
    release = resolve;
  });
  const unexpected = await simulatedApi(page, async (route) => {
    if (responseKind === "pending") await gate;
    if (responseKind === "success") await fulfillArchive(route);
    else
      await route.fulfill({
        status: 503,
        json: { code: "STORAGE_UNAVAILABLE" },
      });
  });
  await page.goto("/exportacion");
  await expect(
    page.getByRole("heading", { name: "Exportar mis datos" }),
  ).toBeVisible();
  const widths = [
    320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
    768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
    1601, 1920, 2560,
  ];
  const measurements = [];
  const violations = [];
  async function inspect(state) {
    for (const colorScheme of ["light", "dark"]) {
      await page.emulateMedia({ colorScheme });
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        const geometry = await page.evaluate(() => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          controls: [...document.querySelectorAll("main button,main a,nav a")]
            .filter((el) => el.getClientRects().length)
            .map((el) => {
              const r = el.getBoundingClientRect();
              return {
                name: el.textContent,
                x: r.x,
                width: r.width,
                height: r.height,
              };
            }),
        }));
        measurements.push({ state, colorScheme, ...geometry });
        expect(
          geometry.scroll,
          state + colorScheme + width,
        ).toBeLessThanOrEqual(width);
        for (const box of geometry.controls) {
          expect(box.width, box.name).toBeGreaterThanOrEqual(44);
          expect(box.height, box.name).toBeGreaterThanOrEqual(44);
          expect(box.x, box.name).toBeGreaterThanOrEqual(0);
          expect(box.x + box.width, box.name).toBeLessThanOrEqual(width + 1);
        }
        if ([320, 1440].includes(width))
          await page.screenshot({
            path: testInfo.outputPath(
              state + "-" + colorScheme + "-" + width + ".png",
            ),
          });
      }
      await page.setViewportSize({ width: 320, height: 700 });
      await page.evaluate(() => {
        const elements = [...document.querySelectorAll("body *")];
        const sizes = elements.map((el) =>
          parseFloat(getComputedStyle(el).fontSize),
        );
        elements.forEach((el, index) => {
          el.dataset.uxFont = el.style.fontSize;
          el.style.fontSize = sizes[index] * 2 + "px";
        });
      });
      const reflow = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
      }));
      measurements.push({ state, colorScheme, text200: true, ...reflow });
      await page.screenshot({
        path: testInfo.outputPath(state + "-" + colorScheme + "-text200.png"),
      });
      expect(reflow.scroll).toBeLessThanOrEqual(reflow.width);
      const axe = await new AxeBuilder({ page })
        .withTags([
          "wcag2a",
          "wcag2aa",
          "wcag21aa",
          "wcag22aa",
          "best-practice",
        ])
        .analyze();
      violations.push({ state, colorScheme, violations: axe.violations });
      expect(axe.violations).toEqual([]);
      await page.evaluate(() => {
        document.querySelectorAll("[data-ux-font]").forEach((el) => {
          el.style.fontSize = el.dataset.uxFont;
          delete el.dataset.uxFont;
        });
      });
    }
  }
  try {
    await inspect("initial");
    await page.getByRole("button", { name: "Preparar exportación" }).click();
    await expect(page.getByRole("status")).toHaveText(
      "Preparando exportación…",
    );
    await inspect("pending");
    responseKind = "error";
    release();
    await expect(page.getByRole("alert")).toBeVisible();
    await inspect("error");
    responseKind = "success";
    await page.getByRole("button", { name: "Reintentar preparación" }).click();
    await expect(
      page.getByRole("link", { name: "Descargar archivo JSON" }),
    ).toBeVisible();
    await inspect("success");
    const paints = await page.evaluate(() => ({
      download: getComputedStyle(document.querySelector("main a"))
        .backgroundColor,
      again: getComputedStyle(document.querySelector("main button"))
        .backgroundColor,
    }));
    expect(paints.download).not.toBe(paints.again);
    expect(unexpected).toEqual([]);
  } finally {
    release();
    await writeFile(
      testInfo.outputPath("matrix.json"),
      JSON.stringify({ simulatedApi: true, measurements, violations }, null, 2),
    );
  }
});

test("export browser simulated API: cancellation, 413 and voluntary focus destinations @s28 @s29 @s31", async ({
  page,
}, testInfo) => {
  let calls = 0;
  let release;
  let gate = new Promise((resolve) => {
    release = resolve;
  });
  let finished;
  let settled = new Promise((resolve) => {
    finished = resolve;
  });
  const unexpected = await simulatedApi(page, async (route) => {
    const ordinal = ++calls;
    if (ordinal === 2) {
      await route.fulfill({ status: 413, json: { code: "EXPORT_TOO_LARGE" } });
    } else {
      await gate;
      await route.fulfill({
        status: 503,
        json: { code: "STORAGE_UNAVAILABLE" },
      });
      finished();
    }
  });
  await page.goto("/exportacion");
  const heading = page.getByRole("heading", { name: "Exportar mis datos" });
  await expect(heading).toBeFocused();
  await page.keyboard.press("Tab");
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toHaveText("Preparando exportación…");
  await page.keyboard.press("Tab");
  await expect(
    page.getByRole("button", { name: "Cancelar preparación" }),
  ).toBeFocused();
  await page.keyboard.press("Enter");
  await expect(heading).toBeFocused();
  release();
  await settled;
  await expect(page.getByRole("alert")).toHaveCount(0);
  await page.keyboard.press("Tab");
  await page.keyboard.press("Enter");
  await expect(page.getByRole("alert")).toContainText(
    "100000 registros o 32 MiB",
  );
  await expect(heading).toBeFocused();
  await expect(
    page.getByRole("link", { name: "Descargar archivo JSON" }),
  ).toHaveCount(0);
  await page.screenshot({ path: testInfo.outputPath("limit.png") });
  await page.reload();
  await expect(heading).toBeFocused();
  for (const blur of [false, true]) {
    gate = new Promise((resolve) => {
      release = resolve;
    });
    settled = new Promise((resolve) => {
      finished = resolve;
    });
    await page
      .getByRole("button", {
        name: blur ? "Reintentar preparación" : "Preparar exportación",
      })
      .focus();
    await page.keyboard.press("Enter");
    await expect(page.getByRole("status")).toHaveText(
      "Preparando exportación…",
    );
    const destination = page
      .getByRole("navigation", { name: "Principal" })
      .getByRole("link", { name: "Exportación" });
    await destination.focus();
    if (blur) await destination.evaluate((element) => element.blur());
    release();
    await settled;
    await expect(page.getByRole("alert")).toContainText("No se pudo preparar");
    if (blur) await expect(page.locator("body")).toBeFocused();
    else await expect(destination).toBeFocused();
  }
  expect(calls).toBe(4);
  expect(unexpected).toEqual([]);
});

test("export browser simulated API: keyboard preparation and native byte-exact download @s23 @s24 @s26 @s31", async ({
  page,
}, testInfo) => {
  // La evidencia del transporte se queda con las cabeceras (metadatos que
  // Playwright ya tiene) y con los bytes que el navegador persiste en la
  // descarga: response.body() se los pediría a la caché del inspector de
  // Chromium, que desaloja el cuerpo ya consumido y falla de forma
  // intermitente bajo carga.
  // fulfillArchive reencamina la petición al servidor local, así que la
  // respuesta observable termina en /export en lugar de /api/v1/me/export.
  let transportHeaders = null;
  page.on("response", (response) => {
    if (new URL(response.url()).pathname.endsWith("/export"))
      transportHeaders = response.headers();
  });
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  let calls = 0;
  const unexpected = await simulatedApi(page, async (route) => {
    calls++;
    await gate;
    await fulfillArchive(route);
  });
  await page.goto("/exportacion");
  await expect(
    page.getByRole("heading", { name: "Exportar mis datos" }),
  ).toBeFocused();
  expect(calls).toBe(0);
  await page.keyboard.press("Tab");
  await expect(
    page.getByRole("button", { name: "Preparar exportación" }),
  ).toBeFocused();
  const started = Date.now();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toHaveText("Preparando exportación…");
  const feedbackMs = Date.now() - started;
  expect(feedbackMs).toBeLessThan(400);
  await expect(
    page.getByRole("link", { name: "Descargar archivo JSON" }),
  ).toHaveCount(0);
  release();
  await expect(
    page.getByRole("link", { name: "Descargar archivo JSON" }),
  ).toBeVisible();
  const focus = await page.evaluate(() => ({
    tag: document.activeElement?.tagName,
    text: document.activeElement?.textContent?.slice(0, 100),
    hasFocus: document.hasFocus(),
  }));
  await mkdir(testInfo.outputPath("evidence"), { recursive: true });
  await writeFile(
    testInfo.outputPath("evidence", "focus.json"),
    JSON.stringify({ simulatedApi: true, feedbackMs, focus }, null, 2),
  );
  await page.screenshot({
    path: testInfo.outputPath("evidence", "prepared.png"),
  });
  await expect(
    page.getByRole("button", { name: "Preparar de nuevo" }),
  ).toBeFocused();
  let delivered = null;
  for (let i = 0; i < 2; i++) {
    const downloading = page.waitForEvent("download");
    await page.getByRole("link", { name: "Descargar archivo JSON" }).click();
    const download = await downloading;
    expect(download.suggestedFilename()).toBe(filename);
    delivered = await readFile(await download.path());
    expect(delivered).toEqual(bytes);
  }
  await writeFile(
    testInfo.outputPath("transport.json"),
    JSON.stringify(
      {
        headers: transportHeaders,
        bytes: delivered.length,
        equal: delivered.equals(bytes),
      },
      null,
      2,
    ),
  );
  expect(calls).toBe(1);
  expect(unexpected).toEqual([]);
});
