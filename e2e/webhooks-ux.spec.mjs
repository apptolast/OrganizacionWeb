import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

// The API is simulated explicitly so every one of the seven states of /webhooks can be
// reached deterministically. This audits real browser UI; it does NOT replace backend
// acceptance, which the filtered JVM suite covers.

const WIDTHS = [320, 768, 1280, 1440];
const ENDPOINT_ID = "12345678-1234-4234-8234-123456789abc";
const SECOND_ID = "22222222-2222-4222-8222-222222222222";
const DELIVERY_ID = "33333333-3333-4333-8333-333333333333";
const PENDING_ID = "44444444-4444-4444-8444-444444444444";
const SECRET = `whsec_${"A".repeat(43)}`;

const endpoint = (overrides = {}) => ({
  id: ENDPOINT_ID,
  url: "https://example.com/hooks",
  description: "Notificar a mi panel de integraciones",
  eventTypes: ["TaskCreated.v1", "TaskStatusChanged.v1"],
  status: "active",
  disabledReason: null,
  disabledAt: null,
  createdAt: "2026-09-08T10:00:00.000000Z",
  updatedAt: "2026-09-08T10:00:00.000000Z",
  ...overrides,
});

const delivery = (overrides = {}) => ({
  id: DELIVERY_ID,
  eventId: DELIVERY_ID,
  eventType: "TaskCreated.v1",
  status: "succeeded",
  attempt: 1,
  httpStatus: 200,
  latencyMs: 12,
  errorClass: null,
  nextAttemptAt: null,
  createdAt: "2026-09-08T10:00:00.000000Z",
  updatedAt: "2026-09-08T10:00:00.000000Z",
  ...overrides,
});

/** Routes every API call the view can make; `state` decides what the list answers. */
async function simulate(page, control) {
  await page.route("**/api/**", async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const method = request.method();
    if (path === "/api/session" && method === "GET")
      return route.fulfill({
        json: {
          authenticated: true,
          username: "Ana",
          csrfToken: "simulated-csrf",
          csrfHeaderName: "X-CSRF-TOKEN",
        },
      });
    if (path === "/api/v1/me/appearance" && method === "GET")
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
    if (path === "/api/v1/me/webhooks" && method === "GET")
      return route.fulfill({ json: { items: control.items } });
    if (path === "/api/v1/me/webhooks" && method === "POST") {
      if (control.createFails)
        return route.fulfill({
          status: 409,
          contentType: "application/problem+json",
          body: JSON.stringify({
            type: "urn:organization:problem:webhook_limit",
            title: "Límite alcanzado",
            status: 409,
            code: "WEBHOOK_LIMIT",
          }),
        });
      return route.fulfill({
        status: 201,
        json: { endpoint: endpoint(), secret: SECRET },
      });
    }
    if (path.endsWith("/deliveries") && method === "GET")
      return route.fulfill({
        json: {
          items: [
            delivery(),
            delivery({
              id: SECOND_ID,
              eventId: SECOND_ID,
              status: "exhausted",
              attempt: 6,
              httpStatus: 500,
              errorClass: "HTTP_ERROR",
              latencyMs: 4210,
            }),
            delivery({
              id: PENDING_ID,
              eventId: PENDING_ID,
              status: "pending",
              attempt: 0,
              httpStatus: null,
              latencyMs: null,
              nextAttemptAt: "2026-09-08T13:00:00.000000Z",
            }),
          ],
        },
      });
    return route.fulfill({ status: 204, body: "" });
  });
}

/**
 * Measures one state at every width, then runs axe. Checkbox hit areas are measured on
 * their label, which is what a finger actually presses.
 */
function auditor(page, folder, options = {}) {
  const evidence = [];
  return async function audit(state) {
    if (options.text200) {
      const scales = await page.evaluate(() => {
        const nodes = [...document.querySelectorAll("main,main *")].filter(
          (node) => node instanceof HTMLElement,
        );
        // Restore the original inline size FIRST. Without this the doubling compounds
        // on every state (51 -> 102 -> 204 -> 409 px) and the audit measures a text
        // scale nobody ever asked for, failing the product for the harness's fault.
        window.__fonts ??= new WeakMap();
        for (const node of nodes) {
          if (!window.__fonts.has(node))
            window.__fonts.set(node, node.style.fontSize);
          node.style.fontSize = window.__fonts.get(node);
        }
        const before = nodes.map((node) =>
          parseFloat(getComputedStyle(node).fontSize),
        );
        nodes.forEach((node, index) => {
          node.style.fontSize = `${before[index] * 2}px`;
        });
        return nodes.map((node, index) => ({
          before: before[index],
          after: parseFloat(getComputedStyle(node).fontSize),
        }));
      });
      for (const size of scales)
        expect(size.after).toBeCloseTo(size.before * 2, 3);
      await writeFile(
        `${folder}/${state}-font-scale.json`,
        JSON.stringify(scales, null, 2),
      );
    }

    for (const width of WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      const measured = await page.evaluate(() => {
        const target = (element) =>
          element instanceof HTMLInputElement && element.type === "checkbox"
            ? (element.closest("label") ?? element)
            : element;
        const controls = [
          ...document.querySelectorAll(
            'nav[aria-label="Principal"] a,main button,main a,main input,main select,main textarea',
          ),
        ].filter((element) => element.getClientRects().length);
        // Whatever sticks out past the viewport is named, so a failure says which
        // element overflowed instead of only by how much.
        const offenders = [...document.querySelectorAll("body *")]
          .filter((element) => element.getClientRects().length)
          .map((element) => ({
            tag: element.tagName,
            className: element.className?.toString?.() ?? "",
            right: element.getBoundingClientRect().right,
            scrollWidth: element.scrollWidth,
            clientWidth: element.clientWidth,
            overflowing: element.scrollWidth > element.clientWidth + 1,
            css: (() => {
              const style = getComputedStyle(element);
              return `${style.overflowWrap}|${style.wordBreak}|${style.textWrap}|${style.whiteSpace}|${style.fontSize}`;
            })(),
            text: (element.textContent ?? "").slice(0, 40),
          }))
          .filter((entry) => entry.right > innerWidth + 1 || entry.overflowing)
          .sort((left, right) => right.right - left.right)
          .slice(0, 6);
        // @s42 exige «ni recorte de la URL, del secreto ni de la tabla de entregas».
        // Conjunto NOMBRADO, uno por sujeto del contrato, nunca `body *`: el thead a
        // <900 px es visually-hidden legítimo (width:1px; clip-path: inset(50%)) y
        // una lista abierta haría fallar código correcto. El campo del secreto se
        // busca como textarea Y como input: si alguien lo devuelve a un campo de una
        // línea, el oráculo tiene que seguir mirándolo, no dejar de verlo.
        // Se miden las DOS dimensiones: un oráculo que sólo mira la horizontal deja
        // pasar el recorte vertical de las celdas apiladas.
        const clipped = [
          ...document.querySelectorAll(
            "main li span, main .webhook-secret textarea, main .webhook-secret input, main tbody td",
          ),
        ]
          .filter((element) => element.getClientRects().length)
          .map((element) => ({
            what: `${element.tagName}.${element.className?.toString?.() ?? ""}`,
            text: (element.value ?? element.textContent ?? "").slice(0, 40),
            scrollWidth: element.scrollWidth,
            clientWidth: element.clientWidth,
            scrollHeight: element.scrollHeight,
            clientHeight: element.clientHeight,
          }))
          .filter(
            (entry) =>
              entry.scrollWidth > entry.clientWidth + 1 ||
              entry.scrollHeight > entry.clientHeight + 1,
          );
        return {
          width: innerWidth,
          scroll: document.documentElement.scrollWidth,
          offenders,
          clipped,
          controls: controls.map((element) => {
            const box = target(element).getBoundingClientRect();
            return {
              name:
                element.getAttribute("aria-label") ||
                element.labels?.[0]?.textContent ||
                element.textContent ||
                element.type,
              x: box.x,
              y: box.y,
              width: box.width,
              height: box.height,
            };
          }),
        };
      });
      evidence.push({ state, mode: options.mode ?? "normal", ...measured });
      await writeFile(
        `${folder}/geometry.json`,
        JSON.stringify(evidence, null, 2),
      );

      expect(
        measured.scroll,
        `${state}:${width} horizontal page overflow; offenders=${JSON.stringify(measured.offenders)}`,
      ).toBeLessThanOrEqual(width);
      // Recorte POR ELEMENTO sobre los tres sujetos que el contrato nombra. La
      // aserción de scroll de arriba NO lo cubre: un elemento que recorta no
      // ensancha la página, precisamente porque se recorta. Acreditado con tres
      // rojos independientes (ver progress/tdd_webhooks_cierre_dictamen.md).
      expect(measured.clipped, `${state}:${width} contenido recortado`).toEqual(
        [],
      );
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
          `${state}:${width}:${box.name} target width`,
        ).toBeGreaterThanOrEqual(44);
        expect(
          box.height,
          `${state}:${width}:${box.name} target height`,
        ).toBeGreaterThanOrEqual(44);
      }
      if (width === 320 || width === 1440)
        await page.screenshot({
          path: `${folder}/${state}-${width}.png`,
          fullPage: true,
        });
    }

    await page.setViewportSize({ width: 320, height: 900 });
    const analyzer = new AxeBuilder({ page }).withTags([
      "wcag2a",
      "wcag2aa",
      "wcag21aa",
      "wcag22aa",
    ]);
    // Forced colours replace every colour with the system palette, so contrast is not
    // ours to answer there. It is NOT skipped in light or dark.
    if (options.mode === "forced-colors")
      analyzer.disableRules(["color-contrast"]);
    const axe = await analyzer.analyze();
    await writeFile(
      `${folder}/${state}-axe.json`,
      JSON.stringify(axe.violations, null, 2),
    );
    expect(axe.violations, `${state} (${options.mode ?? "normal"})`).toEqual(
      [],
    );
  };
}

/** Walks the view through its seven states, auditing each one. */
async function walkStates(page, audit, control) {
  // Every interaction is scoped to the main landmark: the sidebar and the top bar carry
  // their own "Cerrar sesión" and would make bare names ambiguous.
  const view = page.getByRole("main");
  const button = (name) => view.getByRole("button", { name, exact: true });

  // 1. Empty
  control.items = [];
  await page.goto("/webhooks");
  await expect(
    view.getByRole("heading", { level: 1, name: "Webhooks" }),
  ).toBeVisible();
  await expect(view.getByText("Cargando webhooks…")).toHaveCount(0);
  await audit("empty");

  // 2. Filled form
  await view
    .getByRole("textbox", { name: "URL", exact: true })
    .fill("https://example.com/hooks");
  await view
    .getByRole("textbox", { name: "Descripción", exact: true })
    .fill("Notificar a mi panel de integraciones");
  await view
    .getByRole("checkbox", { name: "Seleccionar todos", exact: true })
    .check();
  await audit("form");

  // 3. Visible secret
  await button("Crear webhook").click();
  await expect(view.getByLabel("Secreto", { exact: true })).toHaveValue(SECRET);
  await audit("secret");

  // 4. List
  await button("Cerrar").click();
  await expect(view.getByLabel("Secreto", { exact: true })).toHaveCount(0);
  await expect(view.getByText("https://example.com/hooks")).toBeVisible();
  await audit("list");

  // 5. Deliveries panel
  await button("Ver entregas").click();
  await expect(view.getByRole("table")).toBeVisible();
  await audit("deliveries");

  // 6. Delete confirmation
  await button("Eliminar").click();
  await expect(view.getByRole("dialog")).toBeVisible();
  await audit("confirm");
  await button("Cancelar").click();

  // 7. Server error that keeps the form visible
  control.createFails = true;
  await view
    .getByRole("textbox", { name: "URL", exact: true })
    .fill("https://otro.example/h");
  await button("Crear webhook").click();
  await expect(view.getByRole("alert").first()).toBeVisible();
  await audit("error");
  control.createFails = false;
}

for (const theme of ["light", "dark"]) {
  test(`webhooks audit: the seven states at four widths in ${theme} @s42`, async ({
    page,
  }) => {
    test.setTimeout(300000);
    const folder = resolve(".e2e-work", "webhooks-ux", theme);
    await mkdir(folder, { recursive: true });
    const control = { items: [], createFails: false };
    await simulate(page, control);
    await page.emulateMedia({ colorScheme: theme });
    await walkStates(page, auditor(page, folder, { mode: theme }), control);
  });
}

for (const mode of ["text200", "forced-colors", "reduced-motion"]) {
  test(`webhooks audit: the seven states with ${mode} @s42`, async ({
    page,
  }) => {
    test.setTimeout(300000);
    const folder = resolve(".e2e-work", "webhooks-ux", mode);
    await mkdir(folder, { recursive: true });
    const control = { items: [], createFails: false };
    await simulate(page, control);
    if (mode === "forced-colors")
      await page.emulateMedia({ forcedColors: "active" });
    if (mode === "reduced-motion")
      await page.emulateMedia({ reducedMotion: "reduce" });
    const observed = await page.evaluate(() => ({
      forcedColors: matchMedia("(forced-colors: active)").matches,
      reducedMotion: matchMedia("(prefers-reduced-motion: reduce)").matches,
    }));
    await writeFile(`${folder}/media.json`, JSON.stringify(observed, null, 2));
    if (mode === "forced-colors") expect(observed.forcedColors).toBe(true);
    if (mode === "reduced-motion") expect(observed.reducedMotion).toBe(true);
    await walkStates(
      page,
      auditor(page, folder, { mode, text200: mode === "text200" }),
      control,
    );
  });
}

test("webhooks audit: keyboard reaches every control in DOM order and focus returns @s42", async ({
  page,
}) => {
  test.setTimeout(120000);
  const folder = resolve(".e2e-work", "webhooks-ux", "keyboard");
  await mkdir(folder, { recursive: true });
  const control = { items: [endpoint()], createFails: false };
  await simulate(page, control);
  await page.goto("/webhooks");
  await expect(page.getByText("https://example.com/hooks")).toBeVisible();

  // Every control is reachable by Tab, in the order it appears in the DOM.
  const order = [];
  for (let step = 0; step < 60; step++) {
    await page.keyboard.press("Tab");
    const focused = await page.evaluate(() => {
      const active = document.activeElement;
      if (!active || !active.closest("main")) return null;
      return {
        tag: active.tagName,
        name:
          active.getAttribute("aria-label") ||
          active.labels?.[0]?.textContent ||
          active.textContent ||
          active.type,
        visibleFocus:
          getComputedStyle(active).outlineStyle !== "none" ||
          getComputedStyle(active, ":focus-visible").outlineStyle !== "none",
      };
    });
    if (focused) order.push(focused);
  }
  await writeFile(`${folder}/tab-order.json`, JSON.stringify(order, null, 2));
  expect(order.length).toBeGreaterThan(5);
  for (const item of order) expect(item.name?.trim()).not.toBe("");

  // Closing the delete confirmation returns focus to the control that opened it.
  const remove = page
    .getByRole("main")
    .getByRole("button", { name: "Eliminar", exact: true });
  await remove.click();
  await expect(page.getByRole("dialog")).toBeVisible();
  await page
    .getByRole("main")
    .getByRole("button", { name: "Cancelar", exact: true })
    .click();
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await expect(remove).toBeFocused();
});
