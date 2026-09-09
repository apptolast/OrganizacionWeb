import assert from "node:assert/strict";
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
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects CASCADE",
  ),
);

test("pause_resume_session: explicit start pause resume and reload preserve historical end @s4 @s29 @s35", async ({
  page,
  request,
}) => {
  const project = await create(request, "Trabajo con pausas deliberadas");
  const task = await saveTask(request, project.id, "Leer y retomar las notas");
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
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
  const startResponse = await started;
  expect(startResponse.status()).toBe(201);
  const session = await startResponse.json();
  await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  const paused = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/pause"),
  );
  await section.getByRole("button", { name: "Pausar", exact: true }).click();
  const pauseResponse = await paused;
  expect(pauseResponse.status()).toBe(201);
  const pause = await pauseResponse.json();
  expect(pause.after.status).toBe("paused");
  await expect(
    section.getByText("Pausa confirmada", { exact: true }),
  ).toBeVisible();
  await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
  await expect(
    section.getByRole("button", { name: "Pausar", exact: true }),
  ).toHaveCount(0);
  const resumed = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().endsWith("/resume"),
  );
  await section.getByRole("button", { name: "Reanudar", exact: true }).click();
  const resumeResponse = await resumed;
  expect(resumeResponse.status()).toBe(201);
  const resume = await resumeResponse.json();
  expect(resume.after.status).toBe("running");
  expect(resume.after.workedMicroseconds).toBe(pause.after.workedMicroseconds);
  expect(resume.after.session).toEqual(session);
  await expect(
    section.getByText("Reanudación confirmada", { exact: true }),
  ).toBeVisible();
  await page.reload();
  await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  await expect(
    section
      .getByRole("paragraph")
      .filter({ hasText: /^Fin previsto:/ })
      .locator(`time[datetime="${session.plannedEndAt}"]`),
  ).toBeVisible();
  await expect(
    section.getByRole("button", { name: "Empezar a trabajar", exact: true }),
  ).toHaveCount(0);
  expect(sql("SELECT status || ':' || revision FROM work_sessions")).toBe(
    "running:3",
  );
  expect(sql("SELECT count(*) FROM work_session_changes")).toBe("2");
  expect(sql("SELECT count(*) FROM work_session_intervals")).toBe("1");
});

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
      assert.ok(measured.scroll <= width, `${state}:${width} page overflow`);
      for (const box of measured.controls) {
        assert.ok(box.x >= 0, `${state}:${width}:${box.name} left`);
        assert.ok(
          box.x + box.width <= width + 1,
          `${state}:${width}:${box.name} right`,
        );
        assert.ok(box.width >= 44, `${state}:${width}:${box.name} width`);
        assert.ok(box.height >= 44, `${state}:${width}:${box.name} height`);
      }
      for (let a = 0; a < measured.controls.length; a++)
        for (let b = a + 1; b < measured.controls.length; b++) {
          const x = measured.controls[a],
            y = measured.controls[b];
          assert.equal(
            Math.min(x.x + x.width, y.x + y.width) - Math.max(x.x, y.x) > 1 &&
              Math.min(x.y + x.height, y.y + y.height) - Math.max(x.y, y.y) > 1,
            false,
            `${state}:${width} overlap ${x.name}/${y.name}`,
          );
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

test("pause_resume_session: responsive running pending uncertain paused and query error @s29 @s31 @s32 @s37 @s38", async ({
  page,
  request,
}) => {
  test.setTimeout(180000);
  const project = await create(
    request,
    "Pausas claras, decisiones explícitas y recuperación sin perder el trabajo realizado",
  );
  const task = await saveTask(
    request,
    project.id,
    "Preparar notas comprensibles para retomar el trabajo y revisar con calma la información importante 🧭",
  );
  const engine = page.context().browser().browserType().name();
  const folder = `.e2e-work/pause-resume-real/${engine}/ux`;
  await mkdir(folder, { recursive: true });
  const inspect = await inspectStates(page, folder);
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  await inspect("running");
  let release;
  const gate = new Promise((done) => {
    release = done;
  });
  await page.route("**/work-sessions/*/pause", async (route) => {
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
  const pause = section.getByRole("button", { name: "Pausar", exact: true });
  await pause.focus();
  await pause.evaluate((button) =>
    button.addEventListener(
      "keydown",
      (event) => {
        if (event.key !== "Enter") return;
        const started = performance.now();
        const observer = new MutationObserver(() => {
          if (
            [
              ...button.closest("section").querySelectorAll('[role="status"]'),
            ].some((el) => el.textContent === "Procesando cambio de sesión")
          ) {
            window.pauseFeedbackMs = performance.now() - started;
            observer.disconnect();
          }
        });
        observer.observe(button.closest("section"), {
          subtree: true,
          childList: true,
          characterData: true,
        });
      },
      { once: true },
    ),
  );
  try {
    await pause.press("Enter");
    await expect(
      section.getByText("Procesando cambio de sesión", { exact: true }),
    ).toBeVisible();
    await expect(pause).toBeFocused();
    const feedback = await page.evaluate(() => window.pauseFeedbackMs);
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
    section.getByText("No podemos confirmar el cambio", { exact: true }),
  ).toBeVisible();
  await expect(
    section.getByRole("heading", { name: "Estado de la sesión", exact: true }),
  ).toBeFocused();
  await inspect("uncertain");
  await section
    .getByRole("button", { name: "Comprobar cambio", exact: true })
    .press("Enter");
  await expect(
    section.getByText("Pausa confirmada", { exact: true }),
  ).toBeVisible();
  await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
  await inspect("paused");
  await page.route("**/work-sessions/*/state", (route) =>
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
  await section
    .getByRole("button", {
      name: "Actualizar estado de la sesión",
      exact: true,
    })
    .click();
  await expect(
    section.getByText("No podemos consultar el estado de la sesión", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    section.getByText("Pausa confirmada", { exact: true }),
  ).toBeVisible();
  await inspect("query-error");
  expect(sql("SELECT count(*) FROM work_session_changes")).toBe("1");
});

test("pause_resume_session: text200 running pending uncertain paused and query error @s29 @s31 @s32 @s37 @s38", async ({
  page,
  request,
}) => {
  test.setTimeout(180000);
  const project = await create(
    request,
    "Pausas claras, decisiones explícitas y recuperación sin perder el trabajo realizado",
  );
  const task = await saveTask(
    request,
    project.id,
    "Preparar notas comprensibles para retomar el trabajo y revisar con calma la información importante 🧭",
  );
  const engine = page.context().browser().browserType().name();
  const folder = `.e2e-work/pause-resume-real/${engine}/text200`;
  await mkdir(folder, { recursive: true });
  const inspect = await inspectStates(page, folder, {
    widths: [320, 768, 1440],
    text200: true,
  });
  await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  const section = page.getByRole("region", {
    name: "Sesión de trabajo",
    exact: true,
  });
  await section
    .getByLabel("Duración prevista (minutos)", { exact: true })
    .fill("25");
  await section
    .getByRole("button", { name: "Empezar a trabajar", exact: true })
    .click();
  await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  await inspect("running");
  let release;
  const gate = new Promise((done) => {
    release = done;
  });
  await page.route("**/work-sessions/*/pause", async (route) => {
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
  const pause = section.getByRole("button", { name: "Pausar", exact: true });
  await pause.focus();
  await pause.evaluate((button) =>
    button.addEventListener(
      "keydown",
      (event) => {
        if (event.key !== "Enter") return;
        const started = performance.now();
        const observer = new MutationObserver(() => {
          if (
            [
              ...button.closest("section").querySelectorAll('[role="status"]'),
            ].some((el) => el.textContent === "Procesando cambio de sesión")
          ) {
            window.pauseFeedbackMs = performance.now() - started;
            observer.disconnect();
          }
        });
        observer.observe(button.closest("section"), {
          subtree: true,
          childList: true,
          characterData: true,
        });
      },
      { once: true },
    ),
  );
  try {
    await pause.press("Enter");
    await expect(
      section.getByText("Procesando cambio de sesión", { exact: true }),
    ).toBeVisible();
    await expect(pause).toBeFocused();
    const feedback = await page.evaluate(() => window.pauseFeedbackMs);
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
    section.getByText("No podemos confirmar el cambio", { exact: true }),
  ).toBeVisible();
  await expect(
    section.getByRole("heading", { name: "Estado de la sesión", exact: true }),
  ).toBeFocused();
  await inspect("uncertain");
  await section
    .getByRole("button", { name: "Comprobar cambio", exact: true })
    .press("Enter");
  await expect(
    section.getByText("Pausa confirmada", { exact: true }),
  ).toBeVisible();
  await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
  await inspect("paused");
  await page.route("**/work-sessions/*/state", (route) =>
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
  await section
    .getByRole("button", {
      name: "Actualizar estado de la sesión",
      exact: true,
    })
    .click();
  await expect(
    section.getByText("No podemos consultar el estado de la sesión", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(
    section.getByText("Pausa confirmada", { exact: true }),
  ).toBeVisible();
  await inspect("query-error");
  expect(sql("SELECT count(*) FROM work_session_changes")).toBe("1");
});

test("pause_resume_session: native zoom200 states @s38", async ({
  request,
  baseURL,
}) => {
  test.setTimeout(180000);
  const project = await create(
    request,
    "Pausas claras, decisiones explícitas y recuperación sin perder el trabajo realizado",
  );
  const task = await saveTask(
    request,
    project.id,
    "Preparar notas comprensibles para retomar el trabajo y revisar con calma la información importante 🧭",
  );
  const scratch = resolve(
    ".e2e-work",
    "pause-resume-native",
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

    const inspect = await inspectStates(page, folder, {
      widths: [320],
      native: true,
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
    await section
      .getByLabel("Duración prevista (minutos)", { exact: true })
      .fill("25");
    await section
      .getByRole("button", { name: "Empezar a trabajar", exact: true })
      .click();
    await expect(section.getByText("En curso", { exact: true })).toBeVisible();
    await inspect("running");
    let release;
    const gate = new Promise((done) => {
      release = done;
    });
    await page.route("**/work-sessions/*/pause", async (route) => {
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
    const pause = section.getByRole("button", { name: "Pausar", exact: true });
    await pause.focus();
    await pause.evaluate((button) =>
      button.addEventListener(
        "keydown",
        (event) => {
          if (event.key !== "Enter") return;
          const started = performance.now();
          const observer = new MutationObserver(() => {
            if (
              [
                ...button
                  .closest("section")
                  .querySelectorAll('[role="status"]'),
              ].some((el) => el.textContent === "Procesando cambio de sesión")
            ) {
              window.pauseFeedbackMs = performance.now() - started;
              observer.disconnect();
            }
          });
          observer.observe(button.closest("section"), {
            subtree: true,
            childList: true,
            characterData: true,
          });
        },
        { once: true },
      ),
    );
    try {
      await pause.press("Enter");
      await expect(
        section.getByText("Procesando cambio de sesión", { exact: true }),
      ).toBeVisible();
      await expect(pause).toBeFocused();
      const feedback = await page.evaluate(() => window.pauseFeedbackMs);
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
      section.getByText("No podemos confirmar el cambio", { exact: true }),
    ).toBeVisible();
    await expect(
      section.getByRole("heading", {
        name: "Estado de la sesión",
        exact: true,
      }),
    ).toBeFocused();
    await inspect("uncertain");
    await section
      .getByRole("button", { name: "Comprobar cambio", exact: true })
      .press("Enter");
    await expect(
      section.getByText("Pausa confirmada", { exact: true }),
    ).toBeVisible();
    await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
    await inspect("paused");
    await page.route("**/work-sessions/*/state", (route) =>
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
    await section
      .getByRole("button", {
        name: "Actualizar estado de la sesión",
        exact: true,
      })
      .click();
    await expect(
      section.getByText("No podemos consultar el estado de la sesión", {
        exact: true,
      }),
    ).toBeVisible();
    await expect(
      section.getByText("Pausa confirmada", { exact: true }),
    ).toBeVisible();
    await inspect("query-error");
    expect(sql("SELECT count(*) FROM work_session_changes")).toBe("1");
  } finally {
    await context.close();
  }
});
