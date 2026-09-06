import { execFileSync } from "node:child_process";
import AxeBuilder from "@axe-core/playwright";
import { test, expect } from "./support/authenticated-test.mjs";
import { sql, create, stored } from "./support/projects.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";

const endpoint = "/api/v1/me/availability";
const emptyTag = '"availability:unconfigured"';
const labels = [
  "Lunes",
  "Martes",
  "Miércoles",
  "Jueves",
  "Viernes",
  "Sábado",
  "Domingo",
];
const days = [
  "MONDAY",
  "TUESDAY",
  "WEDNESDAY",
  "THURSDAY",
  "FRIDAY",
  "SATURDAY",
  "SUNDAY",
];
const input = (amount = 0) => ({
  zoneId: "UTC",
  dailyMinutes: Object.fromEntries(days.map((day) => [day, amount])),
});
async function read(request) {
  const response = await request.get(endpoint);
  expect(response.status()).toBe(200);
  expect(response.headers()["cache-control"]).toContain("no-store");
  return { body: await response.json(), etag: response.headers().etag };
}
async function save(request, body, etag, headers) {
  return request.put(endpoint, {
    headers: { ...(headers ?? (await csrfHeaders(request))), "If-Match": etag },
    data: body,
  });
}
const saveButton = (page) =>
  page.getByRole("button", { name: "Guardar disponibilidad", exact: true });
const field = (page, index) =>
  page.getByLabel(`${labels[index]} · minutos`, { exact: true });
async function open(page) {
  await page.goto("/disponibilidad");
  await expect(page.getByLabel("Zona horaria", { exact: true })).toBeEnabled();
  await page.getByLabel("Zona horaria", { exact: true }).selectOption("UTC");
  await expect(saveButton(page)).toBeEnabled();
}

test.beforeEach(() =>
  sql(
    "TRUNCATE planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  ),
);

test("availability: absence, saved budgets, no-op and explicit discard preserve project and outbox @s1 @s3 @s8 @s18 @s28 @s47", async ({
  page,
  request,
}) => {
  const project = await create(
    request,
    "Proyecto independiente de disponibilidad",
  );
  const before = stored(project.id);
  expect(await read(request)).toEqual({
    body: {
      configured: false,
      zoneId: null,
      dailyMinutes: null,
      updatedAt: null,
    },
    etag: emptyTag,
  });
  await open(page);
  await expect(page.getByText("Sin configurar", { exact: true })).toBeVisible();
  await expect(
    page.getByText("Los cambios sin guardar se pierden al salir", {
      exact: true,
    }),
  ).toBeVisible();
  const values = [60, 120, 30, 45, 90, 0, 0];
  for (let index = 0; index < 7; index++)
    await field(page, index).fill(String(values[index]));
  const pending = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "PUT",
  );
  await saveButton(page).click();
  expect((await pending).status()).toBe(200);
  await expect(
    page.getByRole("status").filter({ hasText: /^Disponibilidad guardada$/ }),
  ).toBeVisible();
  const confirmed = await read(request);
  expect(confirmed.body.dailyMinutes).toEqual(
    Object.fromEntries(days.map((day, index) => [day, values[index]])),
  );
  expect(confirmed.body.zoneId).toBe("UTC");
  expect(confirmed.etag).toMatch(/^"availability:[0-9a-f-]{36}:0"$/);
  const noOp = await save(
    request,
    { zoneId: "UTC", dailyMinutes: confirmed.body.dailyMinutes },
    confirmed.etag,
  );
  expect(noOp.status()).toBe(200);
  expect(noOp.headers().etag).toBe(confirmed.etag);
  expect(await noOp.json()).toEqual(confirmed.body);
  const projectName = process.env.E2E_COMPOSE_PROJECT;
  if (
    !projectName?.startsWith("organizationweb-e2e-") ||
    !process.env.E2E_ENV_FILE
  )
    throw new Error("Restart requires isolated fixture");
  execFileSync(
    "docker",
    [
      "compose",
      "--env-file",
      process.env.E2E_ENV_FILE,
      "-p",
      projectName,
      "-f",
      "docker-compose.yml",
      "restart",
      "backend",
    ],
    { stdio: "pipe", timeout: 60000 },
  );
  await expect
    .poll(
      async () => {
        try {
          return (
            await request.get("/api/session", { timeout: 2000 })
          ).status();
        } catch {
          return 0;
        }
      },
      { timeout: 60000 },
    )
    .toBe(200);
  expect((await (await request.get("/api/session")).json()).authenticated).toBe(
    true,
  );
  expect(await read(request)).toEqual(confirmed);
  await page.reload();
  for (let index = 0; index < 7; index++)
    await expect(field(page, index)).toHaveValue(String(values[index]));
  await field(page, 0).fill("777");
  await page
    .getByRole("link", { name: "Cancelar y volver a Proyectos", exact: true })
    .click();
  await expect(page).toHaveURL(/\/proyectos$/);
  expect(await read(request)).toEqual(confirmed);
  expect(stored(project.id)).toEqual(before);
  expect(sql("SELECT count(*) FROM availability_preferences")).toBe("1");
});

test("availability: concurrent first save and update have one winner; stale UI requires deliberate replacement @s19 @s20 @s33", async ({
  page,
  request,
}) => {
  const headers = await csrfHeaders(request);
  const first = await Promise.all([
    save(request, input(10), emptyTag, headers),
    save(request, input(20), emptyTag, headers),
  ]);
  expect(first.map((response) => response.status()).sort()).toEqual([200, 412]);
  const initial = await read(request);
  const next = await Promise.all([
    save(request, input(30), initial.etag, headers),
    save(request, input(40), initial.etag, headers),
  ]);
  expect(next.map((response) => response.status()).sort()).toEqual([200, 412]);
  await open(page);
  await field(page, 0).fill("99");
  const current = await read(request);
  expect((await save(request, input(50), current.etag, headers)).status()).toBe(
    200,
  );
  const conflict = page.waitForResponse(
    (response) =>
      response.url().endsWith(endpoint) &&
      response.request().method() === "PUT",
  );
  await saveButton(page).click();
  expect((await conflict).status()).toBe(412);
  await expect(field(page, 0)).toHaveValue("99");
  await expect(saveButton(page)).toBeDisabled();
  await page
    .getByRole("button", { name: "Recargar versión guardada", exact: true })
    .click();
  for (let index = 0; index < 7; index++)
    await expect(field(page, index)).toHaveValue("50");
  await expect(saveButton(page)).toBeEnabled();
  expect(sql("SELECT count(*) FROM availability_preferences")).toBe("1");
  expect(sql("SELECT count(*) FROM outbox_events")).toBe("0");
});

test("availability: valid-shaped contradictory confirmation cannot create false success or retry a write @s32 @s33 @s44", async ({
  page,
}) => {
  await open(page);
  await field(page, 0).fill("31");
  let writes = 0;
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    const actual = await route.fetch();
    expect(actual.status()).toBe(200);
    const body = await actual.json();
    body.dailyMinutes.MONDAY = 32;
    await route.fulfill({ response: actual, json: body });
  });
  await saveButton(page).click();
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(
    page.getByRole("status").filter({ hasText: /^Disponibilidad guardada$/ }),
  ).toHaveCount(0);
  await expect(field(page, 0)).toHaveValue("31");
  await expect(saveButton(page)).toBeDisabled();
  expect(writes).toBe(1);
  await page
    .getByRole("button", { name: "Recargar versión guardada", exact: true })
    .click();
  await expect(saveButton(page)).toBeEnabled();
  await expect(field(page, 0)).toHaveValue("31");
  expect(writes).toBe(1);
});

test("availability: seven valid budgets, keyboard and prompt feedback @s31 @s38 @s39 @s42 @s43", async ({
  page,
}) => {
  await open(page);
  await field(page, 0).fill("");
  await expect(
    page.getByText("Completa los siete presupuestos para calcular el total", {
      exact: true,
    }),
  ).toBeVisible();
  await field(page, 0).pressSequentially("1e");
  await expect(
    page.getByText("Completa los siete presupuestos para calcular el total", {
      exact: true,
    }),
  ).toBeVisible();
  let invalidWrites = 0;
  const countInvalidWrite = (request) => {
    if (request.url().endsWith(endpoint) && request.method() === "PUT")
      invalidWrites++;
  };
  page.on("request", countInvalidWrite);
  await saveButton(page).click();
  await expect(field(page, 0)).toHaveAttribute("aria-invalid", "true");
  await expect(field(page, 0)).toBeFocused();
  expect(
    await field(page, 0).evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  expect(invalidWrites).toBe(0);
  page.off("request", countInvalidWrite);
  await field(page, 0).fill("0");
  await page.setViewportSize({ width: 320, height: 700 });
  for (
    let attempt = 0;
    attempt < 24 &&
    !(await saveButton(page).evaluate(
      (element) => element === document.activeElement,
    ));
    attempt++
  )
    await page.keyboard.press("Tab");
  await expect(saveButton(page)).toBeFocused();
  expect(
    await saveButton(page).evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await page.evaluate(() => {
    document.querySelector("form").addEventListener(
      "submit",
      () => {
        window.availabilityStart = performance.now();
      },
      { capture: true, once: true },
    );
    const observer = new MutationObserver(() => {
      if (
        [...document.querySelectorAll('[role="status"]')].some(
          (element) => element.textContent === "Guardando disponibilidad",
        )
      ) {
        window.availabilityFeedback =
          performance.now() - window.availabilityStart;
        observer.disconnect();
      }
    });
    observer.observe(document, {
      subtree: true,
      childList: true,
      characterData: true,
    });
  });
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  let writes = 0;
  await page.route(`**${endpoint}`, async (route) => {
    if (route.request().method() !== "PUT") return route.continue();
    writes++;
    const actual = await route.fetch();
    await gate;
    await route.fulfill({ response: actual });
  });
  try {
    await page.keyboard.press("Enter");
    await expect(
      page
        .getByRole("status")
        .filter({ hasText: /^Guardando disponibilidad$/ }),
    ).toBeVisible();
    expect(await page.evaluate(() => window.availabilityFeedback)).toBeLessThan(
      400,
    );
    for (let index = 0; index < 7; index++)
      await expect(field(page, index)).toBeDisabled();
    await expect(
      page.getByLabel("Zona horaria", { exact: true }),
    ).toBeDisabled();
    await expect(saveButton(page)).toBeDisabled();
    expect(writes).toBe(1);
  } finally {
    release();
  }
  await expect(
    page.getByRole("status").filter({ hasText: /^Disponibilidad guardada$/ }),
  ).toBeVisible();
});

for (const width of [
  320, 359, 360, 361, 390, 419, 421, 480, 599, 600, 601, 699, 701, 759, 761,
  768, 820, 999, 1001, 1024, 1099, 1101, 1280, 1440, 1599, 1601, 1920, 2560,
]) {
  test(`availability: responsive controls at ${width} CSS pixels @s39`, async ({
    page,
  }) => {
    await open(page);
    await field(page, 0).fill("");
    await field(page, 0).pressSequentially("1e");
    await saveButton(page).click();
    await expect(field(page, 0)).toHaveAttribute("aria-invalid", "true");
    await field(page, 0).fill("0");
    await test.step(`${width} CSS pixels`, async () => {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      await navigationFits(page);
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      for (const element of await page
        .getByRole("main")
        .locator("a,button,input,select")
        .all()) {
        const box = await element.boundingBox();
        expect(box.width).toBeGreaterThanOrEqual(44);
        expect(box.height).toBeGreaterThanOrEqual(44);
        expect(box.x).toBeGreaterThanOrEqual(0);
        expect(box.x + box.width).toBeLessThanOrEqual(width + 1);
      }
      expect(
        (
          await new AxeBuilder({ page })
            .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
            .analyze()
        ).violations,
      ).toEqual([]);
    });
  });
}

test("availability: expired session while catalog waits removes private draft without a late restoration @s23 @s34 @s40", async ({
  page,
  request,
}) => {
  expect((await save(request, input(85), emptyTag)).status()).toBe(200);
  let release;
  const gate = new Promise((resolve) => {
    release = resolve;
  });
  let captured;
  const waiting = new Promise((resolve) => {
    captured = resolve;
  });
  await page.route(`**${endpoint}/zones`, async (route) => {
    captured();
    await gate;
    await route.continue();
  });
  try {
    await page.goto("/disponibilidad");
    await waiting;
    await expect(field(page, 0)).toHaveValue("85");
    await field(page, 0).fill("123");
    sql(
      "UPDATE spring_session SET last_access_time=0,expiry_time=0 WHERE principal_name='e2e-user'",
    );
    const rejection = page.waitForResponse((response) =>
      response.url().endsWith(`${endpoint}/zones`),
    );
    release();
    expect((await rejection).status()).toBe(401);
    await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
    await expect(page.getByLabel("Zona horaria", { exact: true })).toHaveCount(
      0,
    );
    await expect(field(page, 0)).toHaveCount(0);
    expect(sql("SELECT monday_minutes FROM availability_preferences")).toBe(
      "85",
    );
  } finally {
    release();
  }
});

async function navigationFits(page) {
  for (const nav of await page.locator("nav").all()) {
    const geometry = await nav.evaluate((element) => ({
      width: element.clientWidth,
      scroll: element.scrollWidth,
    }));
    expect(geometry.scroll).toBeLessThanOrEqual(geometry.width);
    const boxes = [];
    for (const link of await nav.getByRole("link").all()) {
      const shape = await link.evaluate((element) => {
        const range = document.createRange();
        range.selectNodeContents(element);
        const text = range.getBoundingClientRect();
        const box = element.getBoundingClientRect();
        return {
          left: box.left,
          right: box.right,
          top: box.top,
          bottom: box.bottom,
          textLeft: text.left,
          textRight: text.right,
        };
      });
      expect(shape.textLeft).toBeGreaterThanOrEqual(shape.left - 1);
      expect(shape.textRight).toBeLessThanOrEqual(shape.right + 1);
      for (const other of boxes)
        expect(
          shape.left >= other.right ||
            shape.right <= other.left ||
            shape.top >= other.bottom ||
            shape.bottom <= other.top,
        ).toBe(true);
      boxes.push(shape);
    }
  }
}

test("availability: navigation text stays inside distinct links around sidebar breakpoint @s39", async ({
  page,
}) => {
  await open(page);
  for (const width of [701, 720, 759, 760, 761]) {
    await page.setViewportSize({ width, height: 900 });
    await navigationFits(page);
  }
});

test("availability: empty server validation messages retain drafts and require explicit recovery without form navigation @s32", async ({
  page,
  request,
}) => {
  await open(page);
  let navigations = 0;
  page.on("framenavigated", (frame) => {
    if (frame === page.mainFrame()) navigations++;
  });
  for (const [index, message] of ["", "   "].entries()) {
    const previous = await read(request);
    const intended = String(61 + index);
    await field(page, 0).fill(intended);
    let writes = 0;
    await page.route(`**${endpoint}`, async (route) => {
      if (route.request().method() !== "PUT") return route.continue();
      writes++;
      if (writes > 1) return route.continue();
      // The actual backend produces a real field rejection. The intercepted
      // reply removes only its useful message, modelling a malformed 400.
      const invalid = route.request().postDataJSON();
      invalid.dailyMinutes.MONDAY = 1441;
      const rejected = await route.fetch({ postData: JSON.stringify(invalid) });
      expect(rejected.status()).toBe(400);
      const body = await rejected.json();
      expect(body.code).toBe("VALIDATION_ERROR");
      expect(body.errors[0].field).toBe("dailyMinutes.MONDAY");
      body.errors[0].message = message;
      await route.fulfill({ response: rejected, json: body });
    });
    try {
      await saveButton(page).click();
      await expect(page.getByRole("alert")).toContainText(
        "No podemos confirmar el guardado",
      );
      await expect(field(page, 0)).toHaveValue(intended);
      await expect(saveButton(page)).toBeDisabled();
      await expect(
        page
          .getByRole("status")
          .filter({ hasText: /^Disponibilidad guardada$/ }),
      ).toHaveCount(0);
      expect(writes).toBe(1);
      expect(navigations).toBe(0);
      await expect(page).toHaveURL(/\/disponibilidad$/);
      expect(await read(request)).toEqual(previous);
      await page
        .getByRole("button", { name: "Recargar versión guardada", exact: true })
        .click();
      await expect(saveButton(page)).toBeEnabled();
      await expect(field(page, 0)).toHaveValue(
        String(previous.body.dailyMinutes?.MONDAY ?? 0),
      );
      expect(writes).toBe(1);
      await page
        .getByLabel("Zona horaria", { exact: true })
        .selectOption("UTC");
      await field(page, 0).fill(intended);
      await saveButton(page).click();
      await expect(
        page
          .getByRole("status")
          .filter({ hasText: /^Disponibilidad guardada$/ }),
      ).toBeVisible();
      expect((await read(request)).body.dailyMinutes.MONDAY).toBe(
        Number(intended),
      );
      expect(writes).toBe(2);
      expect(navigations).toBe(0);
    } finally {
      await page.unroute(`**${endpoint}`);
    }
  }
});

test("availability: Enter from a daily budget restores focus after success and errors without stealing external focus @s31 @s32 @s39", async ({
  page,
}) => {
  for (const outcome of ["success", "503", "400", "external"]) {
    await open(page);
    const monday = field(page, 0);
    await monday.fill("77");
    let release, entered;
    const gate = new Promise((resolve) => {
      release = resolve;
    });
    const started = new Promise((resolve) => {
      entered = resolve;
    });
    await page.route(`**${endpoint}`, async (route) => {
      if (route.request().method() !== "PUT") return route.continue();
      entered();
      await gate;
      if (outcome === "503")
        return route.fulfill({
          status: 503,
          contentType: "application/problem+json",
          json: {
            status: 503,
            code: "STORAGE_UNAVAILABLE",
            title: "Almacenamiento no disponible",
          },
        });
      if (outcome === "400") {
        const invalid = route.request().postDataJSON();
        invalid.dailyMinutes.MONDAY = 1441;
        const actual = await route.fetch({ postData: JSON.stringify(invalid) });
        expect(actual.status()).toBe(400);
        return route.fulfill({ response: actual });
      }
      return route.continue();
    });
    try {
      await expect(monday).toBeFocused();
      await monday.press("Enter");
      await started;
      await expect(monday).toBeDisabled();
      const external = page.getByRole("button", {
        name: "Cerrar sesión",
        exact: true,
      });
      if (outcome === "external") {
        for (
          let attempt = 0;
          attempt < 24 &&
          !(await external.evaluate(
            (element) => element === document.activeElement,
          ));
          attempt++
        )
          await page.keyboard.press("Shift+Tab");
        await expect(external).toBeFocused();
      }
      release();
      if (outcome === "503")
        await expect(
          page.getByRole("button", {
            name: "Recargar versión guardada",
            exact: true,
          }),
        ).toBeVisible();
      else if (outcome === "400")
        await expect(monday).toHaveAttribute("aria-invalid", "true");
      else
        await expect(
          page
            .getByRole("status")
            .filter({ hasText: /^Disponibilidad guardada$/ }),
        ).toBeVisible();
      await expect(outcome === "external" ? external : monday).toBeFocused();
    } finally {
      release();
      await page.unrouteAll({ behavior: "wait" });
    }
  }
});
