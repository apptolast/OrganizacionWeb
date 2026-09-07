import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { chromium } from "@playwright/test";
import { loginSession } from "../scripts/session-client.mjs";

const widths = [
  320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  1601, 1920, 2560,
];

test.afterEach(() => {
  sql("DELETE FROM appearance_preferences WHERE owner_id='e2e-user'");
});

test("customization UX: system dark reduced motion and forced colors retain personal controls @s40", async ({
  page,
  request,
}, testInfo) => {
  const project = await create(request, "Proyecto de modalidades del sistema");
  const path = "/api/v1/me/customization/PROJECT";
  const current = await request.get(path);
  const created = await request.post(`${path}/fields`, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": current.headers().etag,
    },
    data: { label: "Revisado", type: "BOOLEAN" },
  });
  expect(created.status()).toBe(200);
  const folder = `.e2e-work/customization-real/${testInfo.project.name}/media`;
  await mkdir(folder, { recursive: true });
  await page.emulateMedia({ colorScheme: "dark" });
  await page.goto(`/proyectos/${project.id}`);
  const select = page.getByRole("combobox", { name: "Revisado", exact: true });
  await expect(select).toBeVisible();
  await select.selectOption("false");
  for (const mode of ["system-dark", "reduced-motion", "forced-colors"]) {
    if (mode === "reduced-motion")
      await page.emulateMedia({ reducedMotion: "reduce" });
    if (mode === "forced-colors")
      await page.emulateMedia({ forcedColors: "active" });
    for (const width of [320, 1440]) {
      await page.setViewportSize({ width, height: 900 });
      const measure = await page.evaluate(() => ({
        width: innerWidth,
        scroll: document.documentElement.scrollWidth,
        dark: matchMedia("(prefers-color-scheme: dark)").matches,
        reduced: matchMedia("(prefers-reduced-motion: reduce)").matches,
        forced: matchMedia("(forced-colors: active)").matches,
        controls: [
          ...document.querySelectorAll(
            ".customization button,.custom-fields button,.custom-fields select",
          ),
        ]
          .filter((el) => el.getClientRects().length)
          .map((el) => {
            const r = el.getBoundingClientRect();
            const s = getComputedStyle(el);
            return {
              name: el.textContent || el.id,
              x: r.x,
              width: r.width,
              height: r.height,
              color: s.color,
              background: s.backgroundColor,
              border: s.borderColor,
            };
          }),
      }));
      await writeFile(
        `${folder}/${mode}-${width}.json`,
        JSON.stringify(measure, null, 2),
      );
      await page.screenshot({
        path: `${folder}/${mode}-${width}.png`,
        fullPage: true,
      });
      expect(measure.dark).toBe(true);
      if (mode !== "system-dark") expect(measure.reduced).toBe(true);
      if (mode === "forced-colors") expect(measure.forced).toBe(true);
      expect(measure.scroll).toBeLessThanOrEqual(width);
      for (const box of measure.controls) {
        expect(box.x, box.name).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width, box.name).toBeLessThanOrEqual(width + 1);
        expect(box.width, box.name).toBeGreaterThanOrEqual(44);
        expect(box.height, box.name).toBeGreaterThanOrEqual(44);
      }
    }
    await expect(select).toHaveValue("false");
    const axe = (
      await new AxeBuilder({ page })
        .withTags([
          "wcag2a",
          "wcag2aa",
          "wcag21aa",
          "wcag22aa",
          "best-practice",
        ])
        .analyze()
    ).violations;
    await writeFile(`${folder}/${mode}-axe.json`, JSON.stringify(axe, null, 2));
    expect(axe).toEqual([]);
    await page.bringToFront();
  }
});

test("customization UX: native zoom200 keeps personal fields at320 CSS pixels @s40", async ({
  request,
  baseURL,
}) => {
  const project = await create(request, "Proyecto de zoom nativo");
  const path = "/api/v1/me/customization/PROJECT";
  const current = await request.get(path);
  const created = await request.post(`${path}/fields`, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": current.headers().etag,
    },
    data: { label: "Nota personal con Unicode 🧭", type: "TEXT" },
  });
  expect(created.status()).toBe(200);
  const folder = resolve(
    ".e2e-work",
    "customization-real",
    "native-zoom",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = resolve(folder, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    resolve(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "Customization isolated zoom QA",
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
    await page.goto(`/proyectos/${project.id}`);
    const note = page.getByRole("textbox", {
      name: "Nota personal con Unicode 🧭",
      exact: true,
    });
    await expect(note).toBeVisible();
    await note.fill("Contenido personal ampliado sin perder controles.");
    const baseline = await page.evaluate(() => ({
      dpr: devicePixelRatio,
      inner: innerWidth,
      outer: outerWidth,
    }));
    const worker =
      context.serviceWorkers()[0] ??
      (await context.waitForEvent("serviceworker"));
    const pattern = `${baseURL}/*`;
    const zoom = await worker.evaluate(async (pattern) => {
      const [tab] = await chrome.tabs.query({ url: pattern });
      await chrome.tabs.setZoom(tab.id, 2);
      return chrome.tabs.getZoom(tab.id);
    }, pattern);
    expect(zoom).toBe(2);
    await expect
      .poll(() => page.evaluate(() => devicePixelRatio))
      .toBe(baseline.dpr * 2);
    await worker.evaluate(
      async ({ pattern, width }) => {
        const [tab] = await chrome.tabs.query({ url: pattern });
        await chrome.windows.update(tab.windowId, { width });
      },
      { pattern, width: 640 + baseline.outer - baseline.inner },
    );
    await expect.poll(() => page.evaluate(() => innerWidth)).toBe(320);
    await note.scrollIntoViewIfNeeded();
    const geometry = await page.evaluate(() => ({
      width: innerWidth,
      scroll: document.documentElement.scrollWidth,
      dpr: devicePixelRatio,
      controls: [
        ...document.querySelectorAll(
          ".customization button,.custom-fields button,.custom-fields textarea",
        ),
      ]
        .filter((el) => el.getClientRects().length)
        .map((el) => {
          const r = el.getBoundingClientRect();
          return {
            name: el.textContent || el.id,
            x: r.x,
            width: r.width,
            height: r.height,
          };
        }),
    }));
    await writeFile(
      resolve(folder, "zoom.json"),
      JSON.stringify({ zoom, baseline, geometry }, null, 2),
    );
    expect(geometry.scroll).toBeLessThanOrEqual(320);
    for (const box of geometry.controls) {
      expect(box.x, box.name).toBeGreaterThanOrEqual(0);
      expect(box.x + box.width, box.name).toBeLessThanOrEqual(321);
      expect(box.width, box.name).toBeGreaterThanOrEqual(44);
      expect(box.height, box.name).toBeGreaterThanOrEqual(44);
    }
    const cdp = await context.newCDPSession(page);
    const viewport = await cdp.send("Page.captureScreenshot", {
      format: "png",
      fromSurface: false,
    });
    await writeFile(
      resolve(folder, "native-zoom200-viewport.png"),
      Buffer.from(viewport.data, "base64"),
    );
    await cdp.detach();
    const axe = (
      await new AxeBuilder({ page })
        .withTags([
          "wcag2a",
          "wcag2aa",
          "wcag21aa",
          "wcag22aa",
          "best-practice",
        ])
        .analyze()
    ).violations;
    await writeFile(resolve(folder, "axe.json"), JSON.stringify(axe, null, 2));
    expect(axe).toEqual([]);
  } finally {
    await context.close();
  }
});

test("customization UX: keyboard confirmation and manual recovery restore logical focus @s35 @s36 @s39", async ({
  page,
  request,
}, testInfo) => {
  const project = await create(request, "Proyecto de recuperación por teclado");
  const path = "/api/v1/me/customization/PROJECT";
  const empty = await request.get(path);
  const fieldResponse = await request.post(`${path}/fields`, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": empty.headers().etag,
    },
    data: { label: "Nota", type: "TEXT" },
  });
  expect(fieldResponse.status()).toBe(200);
  const folder = `.e2e-work/customization-real/${testInfo.project.name}/keyboard`;
  await mkdir(folder, { recursive: true });
  const trace = [];
  async function tabTo(target) {
    for (let i = 0; i < 100; i++) {
      if (await target.evaluate((el) => el === document.activeElement)) return;
      const reverse = await target.evaluate((el) =>
        Boolean(
          document.activeElement?.compareDocumentPosition(el) &
          Node.DOCUMENT_POSITION_PRECEDING,
        ),
      );
      await page.keyboard.press(reverse ? "Shift+Tab" : "Tab");
      trace.push(
        await page.evaluate(
          () =>
            document.activeElement?.textContent || document.activeElement?.id,
        ),
      );
    }
    throw new Error("Keyboard target not reached");
  }
  let release;
  const held = new Promise((resolve) => {
    release = resolve;
  });
  await page.route(`**${path}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    await held;
    await route.fulfill({ response });
  });
  await page.goto("/proyectos");
  await page.setViewportSize({ width: 320, height: 900 });
  const customize = page.getByRole("button", {
    name: "Personalizar vista",
    exact: true,
  });
  await expect(customize).toBeVisible();
  await tabTo(customize);
  await page.keyboard.press("Enter");
  const check = page.getByRole("checkbox", {
    name: "Actualizado",
    exact: true,
  });
  await tabTo(check);
  await page.keyboard.press("Space");
  const save = page.getByRole("button", { name: "Guardar vista", exact: true });
  await tabTo(save);
  await page.evaluate(() => {
    window.customizationFeedback = { start: performance.now(), shown: null };
    const observer = new MutationObserver(() => {
      if (document.body.textContent.includes("Guardando personalización")) {
        window.customizationFeedback.shown = performance.now();
        observer.disconnect();
      }
    });
    observer.observe(document.body, {
      subtree: true,
      childList: true,
      characterData: true,
    });
  });
  await page.keyboard.press("Enter");
  await expect(
    page.getByRole("status").filter({ hasText: /^Guardando personalización$/ }),
  ).toBeVisible();
  const timing = await page.evaluate(() => window.customizationFeedback);
  expect(timing.shown).not.toBeNull();
  expect(timing.shown - timing.start).toBeLessThan(400);
  await expect(save).toBeDisabled();
  release();
  await expect(
    page.getByRole("status").filter({ hasText: /^Vista guardada$/ }),
  ).toBeVisible();
  const region = page.getByRole("region", {
    name: "Personalización de vista",
    exact: true,
  });
  await expect(region).toBeFocused();
  await page.unroute(`**${path}`);
  const link = page
    .getByRole("list", { name: "Proyectos guardados" })
    .getByRole("link", { name: project.name });
  if (
    process.platform === "win32" &&
    page.context().browser().browserType().name() === "webkit"
  ) {
    testInfo.annotations.push({
      type: "limitation",
      description:
        "WebKit Windows: project link opened by click; link Tab navigation is not accredited. Remaining controls and recovery use keyboard.",
    });
    await link.click();
  } else {
    await tabTo(link);
    await page.keyboard.press("Enter");
  }
  const note = page.getByRole("textbox", { name: "Nota", exact: true });
  await expect(note).toBeVisible();
  await tabTo(note);
  await page.keyboard.type("Borrador conservado");
  const endpoint = `/api/v1/projects/${project.id}/custom-fields`;
  let writes = 0;
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    const response = await route.fetch();
    expect(response.status()).toBe(200);
    await route.abort("failed");
  });
  const saveValues = page.getByRole("button", {
    name: "Guardar campos",
    exact: true,
  });
  await tabTo(saveValues);
  await page.keyboard.press("Enter");
  const recover = page.getByRole("button", {
    name: "Recargar guardado",
    exact: true,
  });
  await expect(recover).toBeVisible();
  await tabTo(recover);
  await page.evaluate(() => {
    window.recoveryFocus = [];
    for (const kind of ["focusin", "focusout", "click", "keydown"])
      document.addEventListener(
        kind,
        (event) =>
          window.recoveryFocus.push({
            kind,
            target: event.target.tagName,
            text: event.target.textContent?.slice(0, 80),
            disabled: event.target.disabled,
            connected: event.target.isConnected,
            related: event.relatedTarget?.tagName,
            active: document.activeElement?.tagName,
          }),
        true,
      );
  });
  await page.keyboard.press("Enter");
  await expect(recover).toHaveCount(0);
  await writeFile(
    `${folder}/recovery-diagnostic.json`,
    JSON.stringify(
      await page.evaluate(() => ({
        events: window.recoveryFocus,
        active: document.activeElement?.outerHTML?.slice(0, 400),
        hasFocus: document.hasFocus(),
      })),
      null,
      2,
    ),
  );
  await page.screenshot({ path: `${folder}/recovery-diagnostic.png` });
  await expect(
    page.getByRole("heading", { name: "Campos personales", exact: true }),
  ).toBeFocused();
  await expect(note).toHaveValue("Borrador conservado");
  await expect(
    page.getByRole("status").filter({ hasText: /^Campos guardados$/ }),
  ).toHaveCount(0);
  expect(writes).toBe(1);
  await page.screenshot({ path: `${folder}/recovery-focus-viewport.png` });
  const focus = await page
    .getByRole("heading", { name: "Campos personales", exact: true })
    .evaluate((el) => {
      const r = el.getBoundingClientRect();
      const css = getComputedStyle(el);
      return {
        top: r.top,
        bottom: r.bottom,
        viewport: innerHeight,
        outlineStyle: css.outlineStyle,
        outlineWidth: css.outlineWidth,
      };
    });
  await writeFile(
    `${folder}/evidence.json`,
    JSON.stringify(
      { timing, trace, focus, writes, upstream200BeforeTransportFailure: true },
      null,
      2,
    ),
  );
  expect(focus.top).toBeGreaterThanOrEqual(0);
  expect(focus.bottom).toBeLessThanOrEqual(focus.viewport);
  expect(focus.outlineStyle).not.toBe("none");
});

test("customization UX: twelve Unicode fields preserve both themes at text200 @s40", async ({
  page,
  request,
}, testInfo) => {
  const project = await create(request, "Proyecto de doce campos Unicode 🧭");
  const path = "/api/v1/me/customization/PROJECT";
  let schema = await request.get(path);
  expect(schema.status()).toBe(200);
  for (let index = 1; index <= 12; index++) {
    schema = await request.post(`${path}/fields`, {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": schema.headers().etag,
      },
      data: {
        label: `Campo ${index} con contexto personal y Unicode 🧭`,
        type: "TEXT",
      },
    });
    expect(schema.status()).toBe(200);
  }
  const definitions = (await schema.json()).customFields;
  const endpoint = `/api/v1/projects/${project.id}/custom-fields`;
  const initial = await request.get(endpoint);
  expect(initial.status()).toBe(200);
  const text = "  Anotación personal con contexto y Unicode 🧭.\n".repeat(8);
  const saved = await request.put(endpoint, {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": initial.headers().etag,
    },
    data: {
      values: definitions.map((field) => ({ fieldId: field.id, value: text })),
    },
  });
  expect(saved.status()).toBe(200);
  const folder = `.e2e-work/customization-real/${testInfo.project.name}/text200`;
  await mkdir(folder, { recursive: true });
  for (const theme of ["LIGHT", "DARK"]) {
    const appearance = await request.get("/api/v1/me/appearance");
    expect(appearance.status()).toBe(200);
    const changed = await request.put("/api/v1/me/appearance", {
      headers: {
        ...(await csrfHeaders(request)),
        "If-Match": appearance.headers().etag,
      },
      data: { theme, accentLight: "#244C3C", accentDark: "#B7E4C7" },
    });
    expect(changed.status()).toBe(200);
    for (const state of ["definitions", "values"]) {
      await page.goto(
        state === "definitions" ? "/proyectos" : `/proyectos/${project.id}`,
      );
      await expect(page.locator("[data-theme]")).toHaveAttribute(
        "data-theme",
        theme.toLowerCase(),
      );
      if (state === "definitions") {
        await page
          .getByRole("button", {
            name: "Gestionar campos personales",
            exact: true,
          })
          .click();
        await expect(
          page.getByRole("button", { name: /^Editar Campo / }),
        ).toHaveCount(12);
      } else {
        await expect(
          page
            .getByRole("group", { name: "Valores personales", exact: true })
            .getByRole("textbox"),
        ).toHaveCount(12);
        await expect(
          page.getByLabel(definitions[0].label, { exact: true }),
        ).toHaveValue(text);
      }
      const primary = page.getByRole("button", {
        name: state === "definitions" ? "Crear campo" : "Guardar campos",
        exact: true,
      });
      expect(
        await primary.evaluate((el) =>
          el.matches("fieldset > button:last-child"),
        ),
      ).toBe(true);
      const fonts = await page.evaluate(() => {
        const elements = [
          ...document.querySelectorAll("main,main *,nav,nav *"),
        ].filter((el) => el instanceof HTMLElement);
        const sizes = elements.map((el) =>
          parseFloat(getComputedStyle(el).fontSize),
        );
        elements.forEach((el, index) => {
          el.style.fontSize = `${sizes[index] * 2}px`;
        });
        return elements.map((el, index) => ({
          before: sizes[index],
          after: parseFloat(getComputedStyle(el).fontSize),
        }));
      });
      expect(
        fonts.every((font) => Math.abs(font.after - font.before * 2) < 0.01),
      ).toBe(true);
      const measures = [];
      for (const width of [320, 768, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        const measure = await page.evaluate(() => ({
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          controls: [
            ...document.querySelectorAll(
              ".customization button,.customization input,.customization select,.custom-fields button,.custom-fields textarea",
            ),
          ]
            .filter((el) => el.getClientRects().length)
            .map((el) => {
              const r = el.getBoundingClientRect();
              return {
                name: el.getAttribute("aria-label") || el.textContent || el.id,
                x: r.x,
                width: r.width,
                height: r.height,
              };
            }),
        }));
        measures.push(measure);
        await writeFile(
          `${folder}/${theme}-${state}.json`,
          JSON.stringify({ fonts, measures }, null, 2),
        );
        expect(measure.scroll).toBeLessThanOrEqual(width);
        for (const box of measure.controls) {
          expect(box.x, box.name).toBeGreaterThanOrEqual(0);
          expect(box.x + box.width, box.name).toBeLessThanOrEqual(width + 1);
          expect(box.width, box.name).toBeGreaterThanOrEqual(44);
          expect(box.height, box.name).toBeGreaterThanOrEqual(44);
        }
        if (width === 320)
          await page.screenshot({
            path: `${folder}/${theme}-${state}-320.png`,
            fullPage: true,
          });
      }
      const axe = (
        await new AxeBuilder({ page })
          .withTags([
            "wcag2a",
            "wcag2aa",
            "wcag21aa",
            "wcag22aa",
            "best-practice",
          ])
          .analyze()
      ).violations;
      await writeFile(
        `${folder}/${theme}-${state}-axe.json`,
        JSON.stringify(axe, null, 2),
      );
      expect(axe).toEqual([]);
      await page.bringToFront();
      await page.evaluate(() => window.scrollTo(0, 0));
      const skip = await page.evaluate(() => {
        const link = document.querySelector(".skip-link");
        const r = link.getBoundingClientRect();
        return {
          top: getComputedStyle(link).top,
          rectTop: r.top,
          rectBottom: r.bottom,
          focused: document.activeElement === link,
          activeElement: document.activeElement?.tagName,
          scrollY,
        };
      });
      await writeFile(
        `${folder}/${theme}-${state}-skip.json`,
        JSON.stringify(skip, null, 2),
      );
      await page.screenshot({
        path: `${folder}/${theme}-${state}-viewport.png`,
      });
      expect(skip.focused).toBe(false);
      expect(skip.scrollY).toBe(0);
      expect(skip.rectBottom).toBeLessThanOrEqual(0);
    }
  }
});

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
