import { test, expect, chromium } from "@playwright/test";
import assert from "node:assert/strict";
import AxeBuilder from "@axe-core/playwright";
import { writeFile, mkdir } from "node:fs/promises";
import { resolve } from "node:path";

// Explicit API simulation. This exercises native browser UI, not backend acceptance.
test("integration simulated API: keyboard creation, transient secret and responsive readability @s33 @s37 @s41", async ({
  page,
}, info) => {
  test.setTimeout(120000);
  const unexpected = [];
  let writes = 0;
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    if (path === "/api/session" && request.method() === "GET")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance" && request.method() === "GET")
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
    if (path === "/api/v1/me/api-credentials" && request.method() === "GET")
      return route.fulfill({ json: { items: [], nextCursor: null } });
    if (
      /^\/api\/v1\/me\/api-credentials\/[a-f0-9-]{36}$/.test(path) &&
      request.method() === "PUT"
    ) {
      writes++;
      const id = path.split("/").at(-1);
      const input = request.postDataJSON();
      expect(request.headers()["x-csrf-token"]).toBe("simulated-csrf");
      expect(input).toEqual({
        name: "Automatización personal",
        scopes: ["projects:read"],
        expiresInDays: 30,
      });
      return route.fulfill({
        status: 201,
        json: {
          credential: {
            id,
            name: input.name,
            scopes: input.scopes,
            createdAt: "2026-09-08T10:00:00Z",
            expiresAt: "2026-10-08T10:00:00Z",
            revokedAt: null,
          },
          secret: `owp_${id}.${"A".repeat(43)}`,
        },
      });
    }
    unexpected.push(request.method() + " " + path);
    return route.abort();
  });
  await page.goto("/integraciones/api");
  const heading = page.getByRole("heading", {
    name: "Credenciales para integraciones",
  });
  await expect(heading).toBeFocused();
  await page
    .getByRole("textbox", { name: "Nombre", exact: true })
    .fill("Automatización personal");
  await page
    .getByRole("checkbox", { name: "Leer proyectos", exact: true })
    .check();
  const create = page.getByRole("button", { name: "Crear", exact: true });
  await create.focus();
  await page.keyboard.press("Enter");
  await expect(page.getByLabel("Secreto de la credencial")).toBeVisible();
  const measurements = [];
  for (const colorScheme of ["light", "dark"]) {
    await page.emulateMedia({ colorScheme });
    for (const width of [320, 768, 1280]) {
      await page.setViewportSize({ width, height: 1000 });
      const geometry = await page
        .locator(".integration-api")
        .evaluate((element) => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          boxes: [
            ...element.querySelectorAll(
              "button,select,input:not([type=checkbox]),label:has(input[type=checkbox])",
            ),
          ].map((node) => {
            const box = node.getBoundingClientRect();
            return {
              text: node.textContent?.trim(),
              x: box.x,
              y: box.y,
              width: box.width,
              height: box.height,
            };
          }),
        }));
      assert.ok(geometry.scroll <= width, "no page overflow");
      for (const box of geometry.boxes) {
        assert.ok(box.width >= 44, "44px target width");
        assert.ok(box.height >= 44, "44px target height");
        assert.ok(
          box.x >= 0 && box.x + box.width <= width + 1,
          "control within viewport width",
        );
      }
      measurements.push({ colorScheme, ...geometry });
      await page.screenshot({
        path: info.outputPath(`${colorScheme}-prepared-${width}.png`),
        fullPage: true,
      });
    }
    const axe = await new AxeBuilder({ page }).analyze();
    expect(axe.violations).toEqual([]);
  }
  const close = page.getByRole("button", { name: "Cerrar secreto" });
  await close.focus();
  await page.keyboard.press("Enter");
  await expect(heading).toBeFocused();
  await expect(page.getByLabel("Secreto de la credencial")).toHaveCount(0);
  expect(writes).toBe(1);
  expect(unexpected).toEqual([]);
  await writeFile(
    info.outputPath("geometry.json"),
    JSON.stringify(measurements, null, 2),
  );
});

test("integration simulated API: manual creation and revocation recovery retain identity and physical focus @s35 @s36 @s40", async ({
  page,
}) => {
  let id;
  let creates = 0;
  let revocations = 0;
  let exists = false;
  const calls = [];
  const credential = () => ({
    id,
    name: "Recuperación propia",
    scopes: ["tasks:read"],
    createdAt: "2026-09-08T10:00:00Z",
    expiresAt: "2026-10-08T10:00:00Z",
    revokedAt: null,
  });
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    calls.push(request.method() + " " + path);
    if (path === "/api/session")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance")
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
    if (path === "/api/v1/me/api-credentials")
      return route.fulfill({ json: { items: [], nextCursor: null } });
    if (path.endsWith("/revocation")) {
      expect(path).toBe(`/api/v1/me/api-credentials/${id}/revocation`);
      expect(request.method()).toBe("PUT");
      revocations++;
      return revocations === 1
        ? route.fulfill({ status: 503, body: "" })
        : route.fulfill({
            json: { ...credential(), revokedAt: "2026-09-08T12:00:00Z" },
          });
    }
    if (request.method() === "PUT") {
      const nextId = path.split("/").at(-1);
      if (id) expect(nextId).toBe(id);
      else id = nextId;
      creates++;
      expect(request.postDataJSON()).toEqual({
        name: "Recuperación propia",
        scopes: ["tasks:read"],
        expiresInDays: 30,
      });
      if (creates === 1) return route.fulfill({ status: 503, body: "" });
      exists = true;
      return route.fulfill({
        json: { credential: credential(), secret: null },
      });
    }
    expect(path).toBe(`/api/v1/me/api-credentials/${id}`);
    return exists
      ? route.fulfill({ json: credential() })
      : route.fulfill({ status: 404, body: "" });
  });
  await page.goto("/integraciones/api");
  await page
    .getByRole("textbox", { name: "Nombre", exact: true })
    .fill("Recuperación propia");
  await page
    .getByRole("checkbox", { name: "Leer tareas", exact: true })
    .check();
  const heading = page.getByRole("heading", {
    name: "Credenciales para integraciones",
  });
  await page.getByRole("button", { name: "Crear", exact: true }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByText(/No podemos confirmar la creación/),
  ).toBeVisible();
  await expect(heading).toBeFocused();
  await page.getByRole("button", { name: "Comprobar creación" }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByText(/Todavía no se encuentra la credencial/),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Reenviar el mismo intento", exact: true })
    .focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByText(
      "La credencial existe, pero su secreto no se puede recuperar.",
    ),
  ).toBeVisible();
  await expect(heading).toBeFocused();
  expect(creates).toBe(2);
  await expect(page.getByLabel("Secreto de la credencial")).toHaveCount(0);
  await page
    .getByRole("button", { name: "Revocar Recuperación propia" })
    .click();
  await page.getByRole("button", { name: "Confirmar revocación" }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByText(/No se puede confirmar la revocación/),
  ).toBeVisible();
  await page.getByRole("button", { name: "Comprobar revocación" }).focus();
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("button", { name: "Confirmar revocación" }),
  ).toBeEnabled();
  await expect(heading).toBeFocused();
  expect(revocations).toBe(1);
  await page.getByRole("button", { name: "Confirmar revocación" }).focus();
  await page.keyboard.press("Enter");
  await expect(page.getByText("Revocada", { exact: true })).toBeVisible();
  await expect(heading).toBeFocused();
  expect(revocations).toBe(2);
  expect(
    calls.filter((call) => call === "GET /api/v1/me/api-credentials"),
  ).toHaveLength(1);
});

test("integration simulated API: text 200 percent and existing media modes @s41", async ({
  page,
}, info) => {
  test.setTimeout(120000);
  const observations = [];
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/session")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance")
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
    expect(path).toBe("/api/v1/me/api-credentials");
    expect(route.request().method()).toBe("GET");
    return route.fulfill({
      json: {
        items: [
          {
            id: "12345678-1234-4234-8234-123456789abc",
            name: "Á".repeat(80),
            scopes: [
              "projects:read",
              "projects:write",
              "tasks:read",
              "tasks:write",
              "agenda:read",
              "history:read",
            ],
            createdAt: "2026-09-08T10:00:00Z",
            expiresAt: "2026-10-08T10:00:00Z",
            revokedAt: null,
          },
        ],
        nextCursor: null,
      },
    });
  });
  await page.goto("/integraciones/api");
  await expect(page.getByRole("button", { name: /^Revocar Á/ })).toBeVisible();
  const before = await page
    .locator(".integration-api h1")
    .evaluate((element) => parseFloat(getComputedStyle(element).fontSize));
  await page.evaluate(() => {
    const nodes = [...document.body.querySelectorAll("*")];
    const sizes = nodes.map((element) =>
      parseFloat(getComputedStyle(element).fontSize),
    );
    nodes.forEach(
      (element, index) => (element.style.fontSize = sizes[index] * 2 + "px"),
    );
  });
  expect(
    await page
      .locator(".integration-api h1")
      .evaluate((element) => parseFloat(getComputedStyle(element).fontSize)),
  ).toBe(before * 2);
  for (const colorScheme of ["light", "dark"]) {
    await page.emulateMedia({ colorScheme });
    for (const width of [320, 768, 1280]) {
      await page.setViewportSize({ width, height: 1000 });
      const geometry = await page
        .locator(".integration-api")
        .evaluate((element) => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
          offending: [...document.body.querySelectorAll("*")]
            .map((node) => {
              const box = node.getBoundingClientRect();
              return {
                tag: node.tagName,
                className: String(node.className),
                right: box.right,
                width: box.width,
              };
            })
            .filter((box) => box.width > 0 && box.right > innerWidth + 0.5)
            .slice(0, 10),
          controls: [
            ...element.querySelectorAll(
              "button,select,input:not([type=checkbox]),label:has(input[type=checkbox])",
            ),
          ].map((node) => {
            const r = node.getBoundingClientRect();
            return { x: r.x, width: r.width, height: r.height };
          }),
        }));
      assert.ok(
        geometry.scroll <= width,
        `text200 page fits: ${JSON.stringify({
          expectedWidth: width,
          innerWidth: geometry.width,
          clientWidth: geometry.clientWidth,
          scroll: geometry.scroll,
          offending: geometry.offending,
        })}`,
      );
      for (const r of geometry.controls) {
        assert.ok(r.width >= 44 && r.height >= 44, "text200 target");
        assert.ok(
          r.x >= 0 && r.x + r.width <= width + 1,
          "text200 control fits",
        );
      }
      observations.push({ colorScheme, ...geometry });
      if (width !== 768)
        await page.screenshot({
          path: info.outputPath(`${colorScheme}-text200-${width}.png`),
          fullPage: true,
        });
    }
    expect((await new AxeBuilder({ page }).analyze()).violations).toEqual([]);
  }
  await page.emulateMedia({ reducedMotion: "reduce", forcedColors: "active" });
  await page.setViewportSize({ width: 320, height: 1000 });
  const mode = await page.evaluate(() => ({
    forced: matchMedia("(forced-colors: active)").matches,
    reduced: matchMedia("(prefers-reduced-motion: reduce)").matches,
    scroll: document.documentElement.scrollWidth,
    width: innerWidth,
  }));
  expect(mode.forced).toBe(true);
  expect(mode.reduced).toBe(true);
  assert.ok(mode.scroll <= mode.width);
  const button = page.getByRole("button", { name: /^Revocar Á/ });
  await button.focus();
  await expect(button).toBeFocused();
  expect(
    (await new AxeBuilder({ page }).disableRules(["color-contrast"]).analyze())
      .violations,
  ).toEqual([]);
  await page.screenshot({
    path: info.outputPath("forced-colors.png"),
    fullPage: true,
  });
  await writeFile(
    info.outputPath("modalities.json"),
    JSON.stringify(
      {
        observations,
        mode,
        forcedColorsContrast:
          "Native colors; axe contrast rule not evaluated in this mode.",
      },
      null,
      2,
    ),
  );
});

test("integration simulated API: native Chromium zoom 200 @s41", async ({
  baseURL,
}, info) => {
  test.skip(
    info.project.name !== "chromium",
    "Native extension zoom is measured in Chromium; text 200 and reflow cover all three engines.",
  );
  const folder = resolve(".e2e-work", "integration-zoom-" + process.pid);
  const extension = resolve(folder, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    resolve(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "Integration zoom QA",
      version: "1.0",
      permissions: ["tabs"],
      host_permissions: ["http://127.0.0.1/*"],
      background: { service_worker: "worker.js" },
    }),
  );
  await writeFile(
    resolve(extension, "worker.js"),
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
    await page.route("**/api/**", async (route) => {
      const path = new URL(route.request().url()).pathname;
      if (path === "/api/session")
        return route.fulfill({
          json: {
            authenticated: true,
            username: "Ana",
            csrfToken: "simulated-csrf",
            csrfHeaderName: "X-CSRF-TOKEN",
          },
        });
      if (path === "/api/v1/me/appearance")
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
      expect(path).toBe("/api/v1/me/api-credentials");
      return route.fulfill({ json: { items: [], nextCursor: null } });
    });
    await page.goto("/integraciones/api");
    await expect(
      page.getByRole("heading", { name: "Credenciales para integraciones" }),
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
      .getByRole("textbox", { name: "Nombre", exact: true })
      .fill("Integración propia");
    await page
      .getByRole("checkbox", { name: "Leer proyectos", exact: true })
      .check();
    const button = page.getByRole("button", { name: "Crear", exact: true });
    await button.focus();
    await button.scrollIntoViewIfNeeded();
    await page.bringToFront();
    await page.evaluate(
      () =>
        new Promise((resolve) =>
          requestAnimationFrame(() => requestAnimationFrame(resolve)),
        ),
    );
    const geometry = await page.evaluate(() => ({
      width: innerWidth,
      scroll: document.documentElement.scrollWidth,
      dpr: devicePixelRatio,
    }));
    assert.ok(geometry.scroll <= geometry.width);
    await page.screenshot({ path: info.outputPath("zoom200.png") });
    const cdp = await context.newCDPSession(page);
    const frame = await cdp.send("Page.captureScreenshot", {
      format: "png",
      fromSurface: false,
    });
    await writeFile(
      info.outputPath("zoom200-compositor.png"),
      Buffer.from(frame.data, "base64"),
    );
    await writeFile(
      info.outputPath("zoom.json"),
      JSON.stringify({ zoom, baseline, geometry }, null, 2),
    );
  } finally {
    await context.close();
  }
});

test("integration simulated API: busy feedback precedes a retained response without announcing success @s41", async ({
  page,
}, info) => {
  let retained;
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/session")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance")
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
    if (route.request().method() === "GET")
      return route.fulfill({ json: { items: [], nextCursor: null } });
    retained = route;
  });
  await page.goto("/integraciones/api");
  await page
    .getByRole("textbox", { name: "Nombre", exact: true })
    .fill("Medición de respuesta");
  await page
    .getByRole("checkbox", { name: "Leer proyectos", exact: true })
    .check();
  await page
    .getByRole("button", { name: "Crear", exact: true })
    .evaluate((button) => {
      button.addEventListener(
        "click",
        () => {
          const began = performance.now();
          const observer = new MutationObserver(() => {
            if (
              document.body.textContent.includes(
                "Consultando o guardando la credencial…",
              )
            ) {
              window.integrationFeedback = performance.now() - began;
              observer.disconnect();
            }
          });
          observer.observe(document.body, {
            subtree: true,
            childList: true,
            characterData: true,
          });
        },
        { once: true, capture: true },
      );
    });
  await page.getByRole("button", { name: "Crear", exact: true }).click();
  await expect(
    page.getByText("Consultando o guardando la credencial…", { exact: true }),
  ).toBeVisible();
  const elapsed = await page.evaluate(() => window.integrationFeedback);
  assert.ok(
    Number.isFinite(elapsed) && elapsed < 400,
    "feedback under 400ms before response",
  );
  await expect(page.getByLabel("Secreto de la credencial")).toHaveCount(0);
  await expect.poll(() => Boolean(retained)).toBe(true);
  await retained.fulfill({ status: 503, body: "" });
  await expect(
    page.getByText(/No podemos confirmar la creación/),
  ).toBeVisible();
  await writeFile(
    info.outputPath("feedback.json"),
    JSON.stringify(
      { elapsed, threshold: 400, responseRetained: true },
      null,
      2,
    ),
  );
});

test("integration simulated API: wide layouts at 1920 and 2560 @s41", async ({
  page,
}, info) => {
  const measurements = [];
  await page.route("**/api/**", async (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === "/api/session")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance")
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
    expect(path).toBe("/api/v1/me/api-credentials");
    return route.fulfill({
      json: {
        items: [
          {
            id: "12345678-1234-4234-8234-123456789abc",
            name: "Automatización personal",
            scopes: ["projects:read"],
            createdAt: "2026-09-08T10:00:00Z",
            expiresAt: "2026-10-08T10:00:00Z",
            revokedAt: null,
          },
        ],
        nextCursor: null,
      },
    });
  });
  await page.goto("/integraciones/api");
  await expect(
    page.getByRole("button", { name: "Revocar Automatización personal" }),
  ).toBeVisible();
  for (const colorScheme of ["light", "dark"]) {
    await page.emulateMedia({ colorScheme });
    for (const width of [1920, 2560]) {
      await page.setViewportSize({ width, height: 1080 });
      const geometry = await page
        .locator(".integration-api")
        .evaluate((element) => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          mainWidth: element.getBoundingClientRect().width,
          boxes: [
            ...element.querySelectorAll(
              "button,select,input:not([type=checkbox]),label:has(input[type=checkbox])",
            ),
          ].map((node) => {
            const r = node.getBoundingClientRect();
            return { x: r.x, width: r.width, height: r.height };
          }),
        }));
      assert.ok(geometry.scroll <= width);
      assert.ok(geometry.mainWidth <= 880);
      for (const r of geometry.boxes) {
        assert.ok(r.width >= 44 && r.height >= 44);
        assert.ok(r.x >= 0 && r.x + r.width <= width + 1);
      }
      measurements.push({ colorScheme, ...geometry });
      await page.screenshot({
        path: info.outputPath(`${colorScheme}-${width}.png`),
        fullPage: true,
      });
    }
  }
  await writeFile(
    info.outputPath("wide.json"),
    JSON.stringify(measurements, null, 2),
  );
});
