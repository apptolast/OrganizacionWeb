import { test, expect } from "./support/authenticated-test.mjs";
import { sql } from "./support/projects.mjs";
import { seedAgenda } from "./support/today.mjs";
import { csrfHeaders } from "../scripts/session-client.mjs";
import AxeBuilder from "@axe-core/playwright";

const MINIMUM_FOCUS_RING_CONTRAST = 3;

test.beforeEach(() =>
  sql(
    "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects CASCADE",
  ),
);
test.afterEach(() =>
  sql("DELETE FROM appearance_preferences WHERE owner_id = 'e2e-user'"),
);

async function preferTheme(request, theme) {
  const current = await request.get("/api/v1/me/appearance");
  expect(current.status()).toBe(200);
  const saved = await request.put("/api/v1/me/appearance", {
    headers: {
      ...(await csrfHeaders(request)),
      "If-Match": current.headers().etag,
    },
    data: { theme, accentLight: "#244C3C", accentDark: "#B7E4C7" },
  });
  expect(saved.status()).toBe(200);
}

function channel(value) {
  const c = value / 255;
  return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
}
function luminance(color) {
  const [r, g, b] = color
    .match(/[\d.]+/g)
    .slice(0, 3)
    .map(Number);
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}
function contrast(first, second) {
  const [dark, light] = [luminance(first), luminance(second)].sort();
  return (light + 0.05) / (dark + 0.05);
}

async function contrastViolations(page, state) {
  const { violations } = await new AxeBuilder({ page })
    .withTags(["wcag2aa"])
    .withRules(["color-contrast"])
    .analyze();
  return violations.flatMap((violation) =>
    violation.nodes.map((node) => `${state}: ${node.target.join(" ")}`),
  );
}

for (const variant of [
  { name: "DARK preference", theme: "DARK", scheme: "light" },
  { name: "SYSTEM with dark OS", theme: "SYSTEM", scheme: "dark" },
]) {
  test(`today dark (${variant.name}): notice, agenda, focus ring and load error keep AA contrast @s34`, async ({
    page,
    request,
  }) => {
    await preferTheme(request, variant.theme);
    await page.emulateMedia({ colorScheme: variant.scheme });
    await page.setViewportSize({ width: 1280, height: 900 });
    await page.goto("/");
    await expect(page.locator("html")).toHaveAttribute("data-theme", "dark");
    await expect(page.locator(".today-notice")).toContainText(
      "Disponibilidad no configurada",
    );
    expect(await contrastViolations(page, "notice")).toEqual([]);

    const { task } = await seedAgenda(request);
    await page.reload();
    const link = page.getByRole("link", { name: task.title, exact: true });
    await expect(link).toBeVisible();
    expect(await contrastViolations(page, "agenda")).toEqual([]);
    await link.focus();
    const ring = await link.evaluate((element) => {
      const style = getComputedStyle(element);
      return {
        width: parseFloat(style.outlineWidth),
        color: style.outlineColor,
        card: getComputedStyle(element.closest("li")).backgroundColor,
      };
    });
    expect(ring.width).toBeGreaterThan(0);
    expect(contrast(ring.color, ring.card)).toBeGreaterThanOrEqual(
      MINIMUM_FOCUS_RING_CONTRAST,
    );

    await page.route("**/api/v1/today", (route) =>
      route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({ code: "STORAGE_UNAVAILABLE" }),
      }),
    );
    await page.reload();
    await expect(page.getByRole("alert")).toContainText(
      "No se pudo actualizar Hoy.",
    );
    expect(await contrastViolations(page, "error")).toEqual([]);
  });
}
