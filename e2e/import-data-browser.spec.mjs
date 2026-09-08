import { test, expect, chromium } from "@playwright/test";
import { createHash } from "node:crypto";
import { writeFile, mkdir } from "node:fs/promises";
import { resolve } from "node:path";
import AxeBuilder from "@axe-core/playwright";
import assert from "node:assert/strict";

test("import simulated API: native Chromium zoom 200 at 320 CSS pixels @s42", async ({
  baseURL,
}, testInfo) => {
  test.skip(
    testInfo.project.name !== "chromium",
    "Native extension zoom is measured in Chromium; other engines have text/reflow evidence.",
  );
  const folder = resolve(".e2e-work", "import-zoom-" + process.pid);
  const extension = resolve(folder, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    extension + "/manifest.json",
    JSON.stringify({
      manifest_version: 3,
      name: "Import zoom QA",
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
    const unexpected = await simulatedApi(page, async (route, path) => {
      expect(path).toBe("/api/v1/me/import/preview");
      await route.fulfill({ json: preview });
    });
    await page.goto("/importacion");
    await expect(
      page.getByRole("heading", { name: "Importar mis datos" }),
    ).toBeVisible();
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
    await page
      .getByLabel("Archivo JSON")
      .setInputFiles({
        name: "copia.json",
        mimeType: "application/json",
        buffer: bytes,
      });
    await page.getByRole("button", { name: "Validar archivo" }).click();
    const confirm = page.getByRole("button", { name: "Confirmar importación" });
    await expect(confirm).toBeVisible();
    await confirm.focus();
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
    await confirm.scrollIntoViewIfNeeded();
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

// Browser evidence with explicitly simulated API responses, not backend acceptance.
const owner = "ana";
const collections =
  "projects tasks taskStatusHistory availability plannedBlocks blockProjections blockChanges workSessions workSessionIntervals workSessionChanges appearance customization projectCustomFieldValues taskCustomFieldValues".split(
    " ",
  );
const counts = Object.fromEntries(collections.map((key) => [key, 0]));
const bytes = Buffer.from(
  JSON.stringify({
    format: "organizationweb-export",
    schemaVersion: 1,
    owner,
    exportedAt: "2026-09-08T10:20:30.123456Z",
    counts,
    data: Object.fromEntries(collections.map((key) => [key, []])),
  }),
);
const hash = createHash("sha256").update(bytes).digest("hex");
const preview = {
  format: "organizationweb-import-preview",
  schemaVersion: 1,
  owner,
  exportedAt: "2026-09-08T10:20:30.123456Z",
  fileSha256: hash,
  byteLength: bytes.length,
  counts,
  insertCounts: counts,
  identicalCounts: counts,
  runningSessions: [],
};

async function simulatedApi(page, handler) {
  const unexpected = [];
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (request.method() === "GET" && path === "/api/session")
      return route.fulfill({
        json: {
          authenticated: true,
          username: owner,
          csrfToken: "simulated-private-token",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (request.method() === "GET" && path === "/api/v1/me/appearance")
      return route.fulfill({
        headers: { ETag: '"appearance:unconfigured"' },
        json: {
          configured: false,
          theme: "SYSTEM",
          accentLight: "#244C3C",
          accentDark: "#B7E4C7",
          updatedAt: null,
        },
      });
    if (path.startsWith("/api/v1/me/import")) return handler(route, path);
    unexpected.push(request.method() + " " + path);
    return route.abort();
  });
  return unexpected;
}

test("import simulated API: native File, separate confirmation and usable controls at 320 @s33 @s36 @s42", async ({
  page,
}, testInfo) => {
  let writes = 0;
  const unexpected = await simulatedApi(page, async (route, path) => {
    expect(route.request().method()).toBe("POST");
    const transportBytes = route.request().postDataBuffer();
    // WebKit does not expose native File request bodies through interception.
    // Chromium/Firefox retain the transport-byte oracle; WebKit verifies File
    // selection, request method/headers, confirmation and the resulting UI.
    if (testInfo.project.name === "webkit") expect(transportBytes).toBeNull();
    else expect(transportBytes.equals(bytes)).toBe(true);
    expect(route.request().headers()["x-csrf-token"]).toBe(
      "simulated-private-token",
    );
    if (path === "/api/v1/me/import/preview")
      return route.fulfill({ json: preview });
    expect(path).toBe("/api/v1/me/import");
    writes++;
    const requestKey = route.request().headers()["idempotency-key"];
    expect(route.request().headers()["x-import-content-sha256"]).toBe(hash);
    return route.fulfill({
      json: {
        requestKey,
        fileSha256: hash,
        byteLength: bytes.length,
        recordedAt: "2026-09-08T10:30:00.000001Z",
        outcome: "NO_CHANGE",
        insertedCounts: counts,
        identicalCounts: counts,
      },
    });
  });
  await page.setViewportSize({ width: 320, height: 850 });
  await page.goto("/importacion");
  await expect(
    page.getByRole("heading", { name: "Importar mis datos" }),
  ).toBeFocused();
  await page
    .getByLabel("Archivo JSON")
    .setInputFiles({
      name: "copia.json",
      mimeType: "application/json",
      buffer: bytes,
    });
  const measures = await page
    .locator("main input, main button")
    .evaluateAll((elements) =>
      elements.map((element) => ({
        name:
          element.textContent ||
          element.getAttribute("aria-label") ||
          element.id,
        width: element.getBoundingClientRect().width,
        height: element.getBoundingClientRect().height,
      })),
    );
  await writeFile(
    testInfo.outputPath("controls.json"),
    JSON.stringify(measures, null, 2),
  );
  await page.screenshot({ path: testInfo.outputPath("selected-320.png") });
  for (const measure of measures) {
    expect(measure.width, measure.name).toBeGreaterThanOrEqual(44);
    expect(measure.height, measure.name).toBeGreaterThanOrEqual(44);
  }
  await page.getByRole("button", { name: "Validar archivo" }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("button", { name: "Confirmar importación" }),
  ).toBeVisible();
  expect(writes).toBe(0);
  await page.screenshot({
    path: testInfo.outputPath("prepared-320.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "Confirmar importación" }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("table", { name: "Resultado confirmado" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Importar mis datos" }),
  ).toBeFocused();
  expect(writes).toBe(1);
  expect(
    await page.evaluate(() =>
      sessionStorage.getItem("organizationweb.import.pending.v1"),
    ),
  ).toBeNull();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  expect(unexpected).toEqual([]);
});

test("import simulated API: keyboard cancellation, delayed response and corrected 413 @s35 @s42", async ({
  page,
}, testInfo) => {
  let held;
  let calls = 0;
  const unexpected = await simulatedApi(page, async (route, path) => {
    expect(path).toBe("/api/v1/me/import/preview");
    calls++;
    if (calls === 1) {
      held = route;
      return;
    }
    return route.fulfill({
      status: 413,
      json: {
        status: 413,
        type: "urn:organization:problem:import_too_large",
        code: "IMPORT_TOO_LARGE",
      },
    });
  });
  await page.setViewportSize({ width: 768, height: 850 });
  await page.goto("/importacion");
  await page
    .getByLabel("Archivo JSON")
    .setInputFiles({
      name: "copia.json",
      mimeType: "application/json",
      buffer: bytes,
    });
  await page.getByRole("button", { name: "Validar archivo" }).focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("status")).toHaveText("Validando archivo");
  await expect.poll(() => Boolean(held)).toBe(true);
  await page.getByRole("button", { name: "Cancelar preparación" }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("heading", { name: "Importar mis datos" }),
  ).toBeFocused();
  await held.fulfill({ json: preview });
  await expect(
    page.getByRole("button", { name: "Confirmar importación" }),
  ).toHaveCount(0);
  expect(
    await page
      .getByLabel("Archivo JSON")
      .evaluate((element) => element.files.length),
  ).toBe(0);
  await page
    .getByLabel("Archivo JSON")
    .setInputFiles({
      name: "copia.json",
      mimeType: "application/json",
      buffer: bytes,
    });
  await page.getByRole("button", { name: "Validar archivo" }).focus();
  await page.keyboard.press("Enter");
  await expect(page.getByRole("alert")).toContainText(
    "32 MiB o 100.000 registros",
  );
  await expect(
    page.getByRole("heading", { name: "Importar mis datos" }),
  ).toBeFocused();
  await page.screenshot({ path: testInfo.outputPath("limit-keyboard.png") });
  expect(calls).toBe(2);
  expect(unexpected).toEqual([]);
});

test("import simulated API: responsive states and themes @s42", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000);
  const widths = [
    320, 359, 360, 361, 390, 480, 600, 699, 700, 701, 768, 820, 1024, 1099,
    1100, 1101, 1280, 1440, 1599, 1600, 1601, 1920, 2560,
  ];
  const evidence = [];
  for (const theme of ["light", "dark"]) {
    await page.unroute("**/api/**");
    await page.emulateMedia({ colorScheme: theme });
    let phase = "hold";
    let held;
    const unexpected = await simulatedApi(page, async (route, path) => {
      if (path === "/api/v1/me/import/preview") {
        if (phase === "hold") {
          held = route;
          return;
        }
        if (phase === "error")
          return route.fulfill({
            status: 413,
            json: {
              status: 413,
              code: "IMPORT_TOO_LARGE",
              type: "urn:organization:problem:import_too_large",
            },
          });
        return route.fulfill({ json: preview });
      }
      if (path === "/api/v1/me/import") return route.abort("failed");
      assert.ok(path.startsWith("/api/v1/me/imports/by-key/"));
      return route.fulfill({
        json: {
          requestKey: path.split("/").at(-1),
          fileSha256: hash,
          byteLength: bytes.length,
          recordedAt: "2026-09-08T10:30:00.000001Z",
          outcome: "NO_CHANGE",
          insertedCounts: counts,
          identicalCounts: counts,
        },
      });
    });
    async function inspect(state) {
      for (const width of widths) {
        await page.setViewportSize({ width, height: 900 });
        const metrics = await page.evaluate(() => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          controls: [
            ...document.querySelectorAll("main input, main button"),
          ].map((el) => {
            const r = el.getBoundingClientRect();
            return {
              name: el.textContent || el.id,
              left: r.left,
              right: r.right,
              width: r.width,
              height: r.height,
            };
          }),
        }));
        evidence.push({ theme, state, ...metrics });
        assert.ok(
          metrics.scroll <= metrics.width,
          `${theme}/${state}/${width}: overflow`,
        );
        for (const control of metrics.controls) {
          assert.ok(
            control.width >= 44 && control.height >= 44,
            `${state}/${width}/${control.name}: target`,
          );
          assert.ok(
            control.left >= 0 && control.right <= metrics.width + 0.5,
            `${state}/${width}/${control.name}: bounds`,
          );
        }
        if ([320, 1280].includes(width))
          await page.screenshot({
            path: testInfo.outputPath(`${theme}-${state}-${width}.png`),
            fullPage: true,
          });
      }
      expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
    }
    await page.goto("/importacion");
    await expect(
      page.getByRole("heading", { name: "Importar mis datos" }),
    ).toBeVisible();
    await expect
      .poll(() => page.evaluate(() => document.documentElement.dataset.theme))
      .toBe(theme);
    await inspect("initial");
    await page
      .getByLabel("Archivo JSON")
      .setInputFiles({
        name: "copia-personal-漢字-é.json",
        mimeType: "application/json",
        buffer: bytes,
      });
    await page.evaluate(() => {
      let started;
      document.addEventListener(
        "click",
        () => {
          started = performance.now();
        },
        { once: true, capture: true },
      );
      const observer = new MutationObserver(() => {
        if (
          started !== undefined &&
          document.querySelector("main [role=status]")
        ) {
          window.importFeedback = performance.now() - started;
          observer.disconnect();
        }
      });
      observer.observe(document.querySelector("main"), {
        subtree: true,
        childList: true,
      });
    });
    await page.getByRole("button", { name: "Validar archivo" }).click();
    await expect(page.getByRole("status")).toHaveText("Validando archivo");
    const feedback = await page.evaluate(() => window.importFeedback);
    assert.ok(feedback >= 0 && feedback < 400, `feedback ${feedback}`);
    evidence.push({ theme, feedbackMs: feedback });
    await inspect("validating");
    await expect.poll(() => Boolean(held)).toBe(true);
    await held.fulfill({ json: preview });
    await expect(
      page.getByRole("button", { name: "Confirmar importación" }),
    ).toBeVisible();
    await inspect("prepared");
    await page.getByRole("button", { name: "Cancelar preparación" }).click();
    phase = "error";
    await page
      .getByLabel("Archivo JSON")
      .setInputFiles({
        name: "copia.json",
        mimeType: "application/json",
        buffer: bytes,
      });
    await page.getByRole("button", { name: "Validar archivo" }).click();
    await expect(page.getByRole("alert")).toContainText("32 MiB");
    await inspect("error");
    phase = "ready";
    await page
      .getByLabel("Archivo JSON")
      .setInputFiles({
        name: "copia.json",
        mimeType: "application/json",
        buffer: bytes,
      });
    await page.getByRole("button", { name: "Validar archivo" }).click();
    await page.getByRole("button", { name: "Confirmar importación" }).click();
    await expect(
      page.getByRole("button", { name: "Comprobar resultado" }),
    ).toBeVisible();
    await inspect("uncertain");
    await page.getByRole("button", { name: "Comprobar resultado" }).click();
    await expect(
      page.getByRole("table", { name: "Resultado confirmado" }),
    ).toBeVisible();
    await inspect("confirmed");
    expect(unexpected).toEqual([]);
  }
  await writeFile(
    testInfo.outputPath("matrix.json"),
    JSON.stringify(evidence, null, 2),
  );
});

test("import simulated API: voluntary focus, enlarged text and accessibility modes @s42", async ({
  page,
}, testInfo) => {
  let held;
  const unexpected = await simulatedApi(page, async (route, path) => {
    expect(path).toBe("/api/v1/me/import/preview");
    held = route;
  });
  await page.setViewportSize({ width: 320, height: 850 });
  await page.goto("/importacion");
  await page
    .getByLabel("Archivo JSON")
    .setInputFiles({
      name: "copia.json",
      mimeType: "application/json",
      buffer: bytes,
    });
  await page.getByRole("button", { name: "Validar archivo" }).focus();
  await page.keyboard.press("Enter");
  await expect.poll(() => Boolean(held)).toBe(true);
  const chosen = page.getByRole("link", { name: "Exportación" });
  await chosen.focus();
  await chosen.evaluate((el) => el.blur());
  await held.fulfill({ json: preview });
  await expect(
    page.getByRole("button", { name: "Confirmar importación" }),
  ).toBeVisible();
  expect(
    await page.evaluate(() => document.activeElement === document.body),
  ).toBe(true);
  const normalFont = await page
    .getByRole("heading", { name: "Importar mis datos" })
    .evaluate((el) => parseFloat(getComputedStyle(el).fontSize));
  await page.evaluate(() => {
    const elements = [...document.querySelectorAll("body *")];
    const sizes = elements.map((el) =>
      parseFloat(getComputedStyle(el).fontSize),
    );
    elements.forEach((el, index) => {
      el.style.fontSize = sizes[index] * 2 + "px";
    });
  });
  const enlargedFont = await page
    .getByRole("heading", { name: "Importar mis datos" })
    .evaluate((el) => parseFloat(getComputedStyle(el).fontSize));
  expect(enlargedFont).toBe(normalFont * 2);
  for (const theme of ["light", "dark"]) {
    await page.emulateMedia({ colorScheme: theme });
    for (const width of [320, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      expect(
        await page.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      ).toBe(true);
      await page.screenshot({
        path: testInfo.outputPath(`${theme}-text200-${width}.png`),
        fullPage: true,
      });
    }
    expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  }
  await page.emulateMedia({ reducedMotion: "reduce", forcedColors: "active" });
  await page.setViewportSize({ width: 768, height: 360 });
  await page.getByRole("button", { name: "Confirmar importación" }).focus();
  const modalities = await page
    .getByRole("button", { name: "Confirmar importación" })
    .evaluate((el) => {
      const box = el.getBoundingClientRect();
      const hit = document.elementFromPoint(
        box.x + box.width / 2,
        box.y + box.height / 2,
      );
      return {
        forced: matchMedia("(forced-colors: active)").matches,
        reduced: matchMedia("(prefers-reduced-motion: reduce)").matches,
        outline: getComputedStyle(el).outlineStyle,
        outlineWidth: parseFloat(getComputedStyle(el).outlineWidth),
        visible: hit === el || el.contains(hit),
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        animations: document
          .getAnimations()
          .filter((a) => a.playState === "running").length,
      };
    });
  await writeFile(
    testInfo.outputPath("modalities.json"),
    JSON.stringify({ normalFont, enlargedFont, ...modalities }, null, 2),
  );
  await page.screenshot({ path: testInfo.outputPath("forced-short.png") });
  expect(modalities.forced).toBe(true);
  expect(modalities.reduced).toBe(true);
  expect(modalities.outline).not.toBe("none");
  expect(modalities.outlineWidth).toBeGreaterThan(0);
  expect(modalities.visible).toBe(true);
  expect(modalities.animations).toBe(0);
  expect(modalities.scroll).toBeLessThanOrEqual(modalities.width);
  expect(unexpected).toEqual([]);
});
