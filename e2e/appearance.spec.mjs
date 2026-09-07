import { test, expect } from "./support/authenticated-test.mjs";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import { csrfHeaders } from "../scripts/session-client.mjs";
import { sql } from "./support/projects.mjs";

test.afterEach(() => {
  sql("DELETE FROM appearance_preferences WHERE owner_id = 'e2e-user'");
});

test("appearance: explicit dark save paints the application and survives reload @s20 @s23", async ({
  page,
  request,
}) => {
  const current = await request.get("/api/v1/me/appearance");
  expect(current.status()).toBe(200);
  const prepared = await request.put("/api/v1/me/appearance", {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": current.headers().etag,
    },
    data: {
      theme: "SYSTEM",
      accentLight: "#244C3C",
      accentDark: "#B7E4C7",
    },
  });
  expect(prepared.status()).toBe(200);
  await page.setViewportSize({ width: 1440, height: 960 });
  await page.goto("/apariencia");
  await expect(page.getByRole("radio", { name: "Sistema" })).toBeChecked();
  await page.getByRole("radio", { name: "Oscuro" }).check();
  const saved = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/v1/me/appearance") &&
      response.request().method() === "PUT",
  );
  await page.getByRole("button", { name: "Guardar apariencia" }).click();
  expect((await saved).status()).toBe(200);
  await expect(
    page.getByText("Apariencia guardada.", { exact: true }),
  ).toBeVisible();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
  const colors = await page.evaluate(() => ({
    canvas: getComputedStyle(document.documentElement).backgroundColor,
    text: getComputedStyle(document.querySelector("main")).color,
    sidebar: getComputedStyle(document.querySelector(".sidebar"))
      .backgroundColor,
  }));
  const folder = resolve(
    ".e2e-work",
    "appearance-real",
    process.env.E2E_COMPOSE_PROJECT,
    "nominal",
  );
  await mkdir(folder, { recursive: true });
  await writeFile(
    resolve(folder, "dark-colors.json"),
    JSON.stringify(colors, null, 2),
  );
  await page.screenshot({
    path: resolve(folder, "dark-1440.png"),
    fullPage: true,
  });
  expect(colors.canvas).toBe("rgb(17, 24, 39)");
  expect(colors.sidebar).toBe("rgb(11, 18, 32)");
  await page.reload();
  await expect(page.getByRole("radio", { name: "Oscuro" })).toBeChecked();
  await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
});
