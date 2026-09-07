import { chromium } from "@playwright/test";
import { join, resolve } from "node:path";
import { loginSession } from "../scripts/session-client.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import AxeBuilder from "@axe-core/playwright";
import { test, expect } from "./support/authenticated-test.mjs";
import { create, sql } from "./support/projects.mjs";
import { saveTask } from "./support/tasks.mjs";

test.beforeEach(() =>
  sql(
    "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

async function inspectStates(page, folder, options = {}) {
  const widths = options.widths ?? [
    320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
    768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
    1601, 1920, 2560,
  ];
  const evidence = [];
  async function inspect(state) {
    if (options.text200) {
      const scales = await page.evaluate(() => {
        const elements = [...document.querySelectorAll("main,main *")].filter(
          (el) => el instanceof HTMLElement,
        );
        window.workOriginalFonts ??= new WeakMap();
        for (const el of elements) {
          if (!window.workOriginalFonts.has(el))
            window.workOriginalFonts.set(el, el.style.fontSize);
          el.style.fontSize = window.workOriginalFonts.get(el);
        }
        const before = elements.map((el) =>
          parseFloat(getComputedStyle(el).fontSize),
        );
        elements.forEach((el, index) => {
          el.style.fontSize = before[index] * 2 + "px";
        });
        return elements.map((el, index) => ({
          before: before[index],
          after: parseFloat(getComputedStyle(el).fontSize),
        }));
      });
      scales.forEach((size) =>
        expect(size.after).toBeCloseTo(size.before * 2, 3),
      );
      await writeFile(
        `${folder}/${state}-font-scale.json`,
        JSON.stringify(scales, null, 2),
      );
    }

    for (const width of widths) {
      if (options.native)
        await expect.poll(() => page.evaluate(() => innerWidth)).toBe(width);
      else
        await page.setViewportSize({
          width,
          height: width === 768 ? 400 : 900,
        });
      const measured = await page.evaluate(() => ({
        width: innerWidth,
        height: innerHeight,
        scroll: document.documentElement.scrollWidth,
        controls: [
          ...document.querySelectorAll(
            'nav[aria-label="Principal"] a,main button,main a,main input,main select,main textarea,header button',
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
              Math.min(x.y + x.height, y.y + y.height) - Math.max(x.y, y.y) > 1,
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
        if (options.native) {
          const viewportCapture = await page.context().newCDPSession(page);
          const viewportShot = await viewportCapture.send(
            "Page.captureScreenshot",
            { format: "png", fromSurface: true, captureBeyondViewport: false },
          );
          await writeFile(
            `${folder}/${state}-${width}-viewport.png`,
            Buffer.from(viewportShot.data, "base64"),
          );
          await viewportCapture.detach();
        } else {
          await page.screenshot({
            path: `${folder}/${state}-${width}-viewport.png`,
          });
        }
        if (options.native) {
          const dimensions = await page.evaluate(() => ({
            width: document.documentElement.scrollWidth,
            height: document.documentElement.scrollHeight,
          }));
          const capture = await page.context().newCDPSession(page);
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
        } else {
          await page.screenshot({
            path: `${folder}/${state}-${width}.png`,
            fullPage: true,
          });
        }
      }
    }
    if (!options.native)
      await page.setViewportSize({ width: 320, height: 700 });
    const axe = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
      .analyze();
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(axe.violations, null, 2),
    );
    expect(axe.violations, state).toEqual([]);
  }

  return inspect;
}

async function closeJourney(page, request, options = {}) {
  test.setTimeout(180000);
  const project = await create(
    request,
    "Cerrar con calma y conservar el trabajo realizado",
  );
  const task = await saveTask(
    request,
    project.id,
    "Revisar decisiones importantes y dejar notas claras para continuar mañana 🧭",
  );
  const engine = page.context().browser().browserType().name();
  const folder =
    options.folder ??
    `.e2e-work/close-work-real/${engine}/${options.mode ?? "ux"}`;
  await mkdir(folder, { recursive: true });
  const inspect = await inspectStates(page, folder, options);
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  if (options.prepare) await options.prepare(page, folder);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  const started = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/work-sessions"),
  );
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  const initial = await started;
  expect(initial.status()).toBe(201);
  const session = await initial.json();
  await section
    .getByRole("heading", { name: "Estado de la sesión", exact: true })
    .locator("..")
    .getByRole("link", { name: "Cerrar sesión de trabajo", exact: true })
    .click();
  await expect(page).toHaveURL(
    `/proyectos/${project.id}/tareas/${task.id}/sesiones/${session.id}`,
  );
  const note =
    "  Avance parcial 🧭\n" +
    "Decisiones explicadas y trabajo pendiente sin prisa. ".repeat(28) +
    "\n  ";
  const next =
    "Siguiente paso: " + "Continuar con las notas guardadas. ".repeat(35);
  expect([...note].length).toBeLessThanOrEqual(2000);
  expect([...next].length).toBeLessThanOrEqual(2000);
  await page
    .getByLabel("Avance anotado (opcional)", { exact: true })
    .fill(note);
  await page
    .getByLabel("Siguiente paso (opcional)", { exact: true })
    .fill(next);
  await inspect("form");
  let release;
  const gate = new Promise((done) => {
    release = done;
  });
  await page.route(`**/work-sessions/${session.id}/close`, async (route) => {
    const actual = await route.fetch();
    expect(actual.status()).toBe(201);
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
  const confirm = page.getByRole("button", {
    name: "Confirmar cierre",
    exact: true,
  });
  await confirm.focus();
  await confirm.evaluate((button) =>
    button.addEventListener(
      "keydown",
      (event) => {
        if (event.key !== "Enter") return;
        const started = performance.now();
        const observer = new MutationObserver(() => {
          if (
            [...document.querySelectorAll('[role="status"]')].some(
              (el) => el.textContent === "Cerrando sesión de trabajo",
            )
          ) {
            window.closeFeedbackMs = performance.now() - started;
            observer.disconnect();
          }
        });
        observer.observe(button.closest("main"), {
          subtree: true,
          childList: true,
          characterData: true,
        });
      },
      { once: true },
    ),
  );
  try {
    await confirm.press("Enter");
    await expect(
      page.getByText("Cerrando sesión de trabajo", { exact: true }),
    ).toBeVisible();
    await expect(confirm).toBeFocused();
    const feedback = await page.evaluate(() => window.closeFeedbackMs);
    expect(feedback).toBeGreaterThanOrEqual(0);
    expect(feedback).toBeLessThan(400);
    await writeFile(
      `${folder}/feedback.json`,
      JSON.stringify({ engine, feedbackMs: feedback }, null, 2),
    );
    await inspect("pending");
  } finally {
    release();
  }
  await expect(
    page.getByText("No podemos confirmar el cierre.", { exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
  ).toBeFocused();
  await inspect("uncertain");
  const check = page.getByRole("button", {
    name: "Comprobar cierre",
    exact: true,
  });
  await check.press("Enter");
  await expect(page.getByText("Sesión cerrada", { exact: true })).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
  ).toBeFocused();
  const notes = page.locator(".closure-note");
  expect(await notes.nth(0).textContent()).toBe(note);
  expect(await notes.nth(1).textContent()).toBe(next);
  expect(
    await notes.nth(0).evaluate((el) => getComputedStyle(el).whiteSpace),
  ).toBe("pre-wrap");
  await inspect("closed");
  await page.route(`**/work-sessions/${session.id}/closure`, (route) =>
    route.fulfill({
      status: 503,
      contentType: "application/problem+json",
      body: JSON.stringify({
        type: "urn:organization:problem:storage_unavailable",
        title: "Almacenamiento no disponible.",
        status: 503,
        code: "STORAGE_UNAVAILABLE",
      }),
    }),
  );
  await page.reload();
  await expect(
    page.getByText("No se ha podido consultar la sesión de trabajo.", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Reintentar lectura", exact: true }),
  ).toBeVisible();
  await inspect("read-failure");
  expect(
    sql("SELECT count(*) FROM work_session_changes WHERE action='CLOSE'"),
  ).toBe("1");
}

test("close_work_session: responsive form pending uncertain closed and read failure @s32 @s35 @s40 @s41", async ({
  page,
  request,
}) => {
  await closeJourney(page, request);
});

test("close_work_session: text200 form pending uncertain closed and read failure @s41", async ({
  page,
  request,
}) => {
  await closeJourney(page, request, {
    mode: "text200",
    widths: [320, 768, 1440],
    text200: true,
  });
});

test("close_work_session: native zoom200 form pending uncertain closed and read failure @s41", async ({
  request,
  baseURL,
}) => {
  test.setTimeout(180000);
  const scratch = resolve(
    ".e2e-work",
    "close-work-native",
    process.env.E2E_COMPOSE_PROJECT,
  );
  const extension = join(scratch, "extension");
  await mkdir(extension, { recursive: true });
  await writeFile(
    join(extension, "manifest.json"),
    JSON.stringify({
      manifest_version: 3,
      name: "OrganizationWeb isolated closure zoom QA",
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
    await closeJourney(page, request, {
      folder: join(scratch, "evidence"),
      widths: [320],
      native: true,
      prepare: async (page, folder) => {
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
          join(folder, "zoom.json"),
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
      },
    });
  } finally {
    await context.close();
  }
});
