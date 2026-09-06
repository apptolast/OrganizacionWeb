import { test, expect } from "@playwright/test";
import { create, sql } from "./support/projects.mjs";
import { loginSession, csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";

test.beforeEach(() => sql("TRUNCATE tasks, outbox_events, projects"));

test("authentication: recovering CSRF never repeats a rejected write @s9 @s17", async ({
  page,
  context,
}) => {
  await loginSession(context.request, {
    username: "e2e-user",
    password: "e2e-only-password",
  });
  await page.goto("/");
  await page
    .getByLabel(/Nombre del proyecto/)
    .fill("Proyecto tras recuperación deliberada");
  let attempts = 0;
  await page.route("**/api/v1/projects", (route) => {
    if (route.request().method() !== "POST") return route.continue();
    attempts++;
    if (attempts === 1)
      return route.continue({
        headers: {
          ...route.request().headers(),
          "x-csrf-token": "invalid-test-token",
        },
      });
    return route.continue();
  });
  const denied = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/v1/projects") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  expect((await denied).status()).toBe(403);
  expect(sql("SELECT count(*) FROM projects")).toBe("0");
  await page
    .getByRole("button", { name: "Recuperar acceso", exact: true })
    .click();
  await expect(
    page.getByRole("button", { name: "Recuperar acceso", exact: true }),
  ).toBeHidden();
  expect(attempts).toBe(1);
  expect(sql("SELECT count(*) FROM projects")).toBe("0");
  await expect(page.getByLabel(/Nombre del proyecto/)).toHaveValue(
    "Proyecto tras recuperación deliberada",
  );
  await page
    .getByRole("button", { name: "Crear proyecto", exact: true })
    .click();
  await expect(page.getByRole("status")).toHaveText("Proyecto guardado.");
  expect(attempts).toBe(2);
  expect(sql("SELECT count(*) FROM projects")).toBe("1");
});

test("authentication: access and errors reflow at breakpoint edges with keyboard and touch @s14 @s19", async ({
  page,
  browser,
}, testInfo) => {
  test.setTimeout(120_000);
  await page.goto("/");
  await page.getByLabel("Usuario", { exact: true }).fill("e2e-user");
  await page
    .getByLabel("Contraseña", { exact: true })
    .fill("incorrect-test-password");
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  await expect(page.getByRole("alert")).toBeVisible();
  for (const width of [
    320, 359, 360, 361, 390, 480, 599, 600, 601, 699, 701, 768, 820, 1024, 1099,
    1101, 1280, 1440, 1599, 1601, 1920, 2560,
  ]) {
    await test.step(`${width} CSS pixels`, async () => {
      await page.setViewportSize({ width, height: width === 768 ? 400 : 900 });
      expect(
        await page.evaluate(
          () =>
            document.documentElement.scrollWidth <=
            document.documentElement.clientWidth,
        ),
      ).toBe(true);
      for (const control of await page.locator("input,button").all()) {
        const box = await control.boundingBox();
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
  }
  await page.setViewportSize({ width: 320, height: 700 });
  const password = page.getByLabel("Contraseña", { exact: true });
  await expect(password).toHaveAttribute("autocomplete", "current-password");
  await expect(page.getByLabel("Usuario", { exact: true })).toHaveAttribute(
    "autocomplete",
    "username",
  );
  await password.fill("e2e-only-password");
  await page.keyboard.press("Tab");
  const submit = page.getByRole("button", {
    name: "Iniciar sesión",
    exact: true,
  });
  await expect(submit).toBeFocused();
  expect(
    await submit.evaluate((element) =>
      parseFloat(getComputedStyle(element).outlineWidth),
    ),
  ).toBeGreaterThan(0);
  await page.keyboard.press("Enter");
  await expect(page.getByLabel(/Nombre del proyecto/)).toBeVisible();
  expect(
    await page.evaluate(() => document.activeElement !== document.body),
  ).toBe(true);
  await page
    .getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
  expect(
    await page.evaluate(() => document.activeElement !== document.body),
  ).toBe(true);
  const touchContext = await browser.newContext({
    baseURL: testInfo.project.use.baseURL,
    viewport: { width: 390, height: 844 },
    hasTouch: true,
  });
  try {
    const touch = await touchContext.newPage();
    await touch.goto("/");
    await touch.getByLabel("Usuario", { exact: true }).fill("e2e-user");
    await touch
      .getByLabel("Contraseña", { exact: true })
      .fill("e2e-only-password");
    await touch
      .getByRole("button", { name: "Iniciar sesión", exact: true })
      .tap();
    await expect(touch.getByLabel(/Nombre del proyecto/)).toBeVisible();
    await touch
      .getByRole("button", { name: "Cerrar sesión", exact: true })
      .tap();
    await expect(touch.getByLabel("Usuario", { exact: true })).toBeVisible();
  } finally {
    await touchContext.close();
  }
});

test("authentication: public access screen rejects bad credentials then opens the original local route @s1 @s2 @s3 @s4 @s13 @s14 @s16", async ({
  page,
  browserName,
}) => {
  const bootstrap = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/session") &&
      response.request().method() === "GET",
  );
  expect((await page.goto("/proyectos")).status()).toBe(200);
  const cookieHeader = await (await bootstrap).headerValue("set-cookie");
  expect(/;\s*SameSite=Lax(?:;|$)/i.test(cookieHeader ?? "")).toBe(true);
  const user = page.getByLabel("Usuario", { exact: true });
  const password = page.getByLabel("Contraseña", { exact: true });
  await expect(user).toBeVisible();
  await expect(password).toBeVisible();
  await expect(
    page.getByRole("list", { name: "Proyectos guardados" }),
  ).toHaveCount(0);
  const anonymous = await page.request.get("/api/session");
  expect(anonymous.status()).toBe(200);
  const session = await anonymous.json();
  expect(Object.keys(session).sort()).toEqual([
    "authenticated",
    "csrfHeaderName",
    "csrfToken",
    "username",
  ]);
  expect(session).toMatchObject({
    authenticated: false,
    username: null,
    csrfHeaderName: "X-CSRF-TOKEN",
  });
  expect(
    typeof session.csrfToken === "string" && session.csrfToken.length > 0,
  ).toBe(true);
  expect(anonymous.headers()["cache-control"]).toContain("no-store");
  const cookieBefore = (await page.context().cookies()).find(
    (cookie) => cookie.name === "SESSION",
  );
  expect(!!cookieBefore).toBe(true);
  expect(cookieBefore).toMatchObject({
    path: "/api",
    httpOnly: true,
  });
  // Windows WebKit reports None for the real Lax cookie. Keep the server
  // header assertion above; do not claim browser enforcement in that port.
  if (browserName !== "webkit" || process.platform !== "win32")
    expect(cookieBefore.sameSite).toBe("Lax");
  const basicOnly = await page.request.get("/api/v1/projects", {
    headers: {
      Authorization:
        "Basic " + Buffer.from("e2e-user:e2e-only-password").toString("base64"),
    },
  });
  expect(basicOnly.status()).toBe(401);
  expect(basicOnly.headers()["www-authenticate"]).toBeUndefined();
  await user.fill("e2e-user");
  await password.fill("incorrect-test-password");
  let release;
  const waiting = new Promise((resolve) => {
    release = resolve;
  });
  let loginAttempts = 0;
  await page.route("**/api/session", async (route) => {
    if (route.request().method() !== "POST") return route.continue();
    loginAttempts++;
    await waiting;
    return route.continue();
  });
  await page.evaluate(() => {
    window.loginFeedback = null;
    let started;
    document.addEventListener(
      "click",
      () => {
        started = performance.now();
      },
      { once: true },
    );
    const observer = new MutationObserver(() => {
      if (
        started !== undefined &&
        [...document.querySelectorAll('[role="status"]')].some(
          (element) => element.textContent === "Comprobando credenciales",
        )
      ) {
        window.loginFeedback = performance.now() - started;
        observer.disconnect();
      }
    });
    observer.observe(document, { childList: true, subtree: true });
  });
  const rejected = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/session") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  await expect(page.getByRole("status")).toHaveText("Comprobando credenciales");
  await expect(
    page.getByRole("button", { name: "Iniciar sesión", exact: true }),
  ).toBeDisabled();
  const feedback = await page.evaluate(() => window.loginFeedback);
  expect(feedback).not.toBeNull();
  expect(feedback).toBeLessThan(400);
  console.info(`Login feedback: ${Math.round(feedback)} ms`);
  expect(loginAttempts).toBe(1);
  release();
  expect((await rejected).status()).toBe(401);
  await expect(page.getByRole("alert")).toBeVisible();
  await expect(password).toHaveValue("");
  await page.unroute("**/api/session");
  await password.fill("e2e-only-password");
  const accepted = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/session") &&
      response.request().method() === "POST",
  );
  await page
    .getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  expect((await accepted).status()).toBe(204);
  await expect(
    page.getByRole("heading", { name: "Proyectos", exact: true }),
  ).toBeVisible();
  await expect(page).toHaveURL(/\/proyectos$/);
  const cookieAfter = (await page.context().cookies()).find(
    (cookie) => cookie.name === "SESSION",
  );
  expect(!!cookieAfter).toBe(true);
  expect(cookieAfter.value === cookieBefore.value).toBe(false);
  expect((await page.request.get("/api/session")).status()).toBe(200);
  expect(
    await page.evaluate(() => ({
      local: localStorage.length,
      session: sessionStorage.length,
    })),
  ).toEqual({ local: 0, session: 0 });
});

test("authentication: logout clears two tabs and rejects the previous cookie @s7 @s8 @s15", async ({
  page,
  context,
}, testInfo) => {
  await loginSession(context.request, {
    username: "e2e-user",
    password: "e2e-only-password",
  });
  const project = await create(context.request, "Proyecto privado de sesión");
  await page.goto(`/proyectos/${project.id}`);
  await expect(
    page.getByRole("heading", { name: project.name, exact: true }),
  ).toBeVisible();
  await context.request.get("/api/session/logout");
  expect(
    (await (await context.request.get("/api/session")).json()).authenticated,
  ).toBe(true);
  const second = await context.newPage();
  try {
    await second.goto(`/proyectos/${project.id}/editar`);
    await second
      .getByLabel(/Nombre del proyecto/)
      .fill("Borrador privado sin guardar");
    const oldCookie = (await context.cookies()).find(
      (cookie) => cookie.name === "SESSION",
    );
    expect(!!oldCookie).toBe(true);
    const response = page.waitForResponse(
      (response) =>
        response.url().endsWith("/api/session/logout") &&
        response.request().method() === "POST",
    );
    await page
      .getByRole("button", { name: "Cerrar sesión", exact: true })
      .click();
    expect((await response).status()).toBe(204);
    for (const tab of [page, second]) {
      await expect(tab.getByLabel("Usuario", { exact: true })).toBeVisible();
      await expect(tab.getByText(project.name, { exact: true })).toHaveCount(0);
      await expect(tab.getByLabel(/Nombre del proyecto/)).toHaveCount(0);
    }
    const origin = testInfo.project.use.baseURL;
    const headers = { Cookie: `SESSION=${oldCookie.value}` };
    expect((await fetch(`${origin}/api/v1/projects`, { headers })).status).toBe(
      401,
    );
    expect(
      (
        await fetch(`${origin}/api/v1/projects`, {
          method: "POST",
          headers: { ...headers, "Content-Type": "application/json" },
          body: JSON.stringify({ name: "No debe guardarse", description: "" }),
        })
      ).status,
    ).toBe(401);
    expect(sql("SELECT count(*) FROM projects")).toBe("1");
  } finally {
    await second.close();
  }
});

test("authentication: invalid CSRF and expired persisted session cannot write @s6 @s9 @s10 @s11", async ({
  page,
  context,
}) => {
  await loginSession(context.request, {
    username: "e2e-user",
    password: "e2e-only-password",
  });
  await page.goto("/");
  await expect(page.getByLabel(/Nombre del proyecto/)).toBeVisible();
  for (const headers of [{}, { "X-CSRF-TOKEN": "invalid-test-token" }]) {
    const response = await context.request.post("/api/v1/projects", {
      headers,
      data: { name: "No debe guardarse", description: "" },
    });
    expect(response.status()).toBe(403);
    expect((await response.json()).code).toBe("CSRF_INVALID");
  }
  const foreign = await context.request.post("/api/v1/projects", {
    headers: {
      ...(await csrfHeaders(context.request)),
      Origin: "https://untrusted.invalid",
    },
    data: { name: "No debe guardarse", description: "" },
  });
  expect(foreign.status()).toBe(403);
  expect((await foreign.json()).code).toBe("UNTRUSTED_ORIGIN");
  // Move the real persisted timestamps beyond the configured inactivity boundary;
  // no clock mock and no thirty-minute sleep are necessary.
  sql(
    "UPDATE spring_session SET last_access_time=last_access_time-1860000, expiry_time=expiry_time-1860000 WHERE principal_name='e2e-user'",
  );
  const expired = await context.request.post("/api/v1/projects", {
    headers: { "X-CSRF-TOKEN": "invalid-test-token" },
    data: { name: "No debe guardarse", description: "" },
  });
  expect(expired.status()).toBe(401);
  expect((await expired.json()).code).toBe("UNAUTHENTICATED");
  await page.reload();
  await expect(page.getByLabel("Usuario", { exact: true })).toBeVisible();
  await expect(page.getByLabel(/Nombre del proyecto/)).toHaveCount(0);
  expect(sql("SELECT count(*) FROM projects")).toBe("0");
});
