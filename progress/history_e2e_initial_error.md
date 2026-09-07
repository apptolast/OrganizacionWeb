# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: history.spec.mjs >> history: workspace opens an empty real history page @s28
- Location: e2e\history.spec.mjs:4:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('navigation', { name: 'Principal', exact: true }).getByRole('link', { name: 'Historial', exact: true })
Expected: visible
Timeout: 5000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" getByRole('navigation', { name: 'Principal', exact: true }).getByRole('link', { name: 'Historial', exact: true }) with timeout 5000ms
  - waiting for getByRole('navigation', { name: 'Principal', exact: true }).getByRole('link', { name: 'Historial', exact: true })

```

```yaml
- link "Saltar al contenido":
  - /url: "#proyectos"
- complementary:
  - text: OrganizationWeb
  - paragraph: TU ESPACIO
  - navigation "Principal":
    - link "Hoy":
      - /url: /
    - link "Proyectos":
      - /url: /proyectos
    - link "Disponibilidad":
      - /url: /disponibilidad
  - paragraph:
    - text: Las grandes cosas empiezan con
    - emphasis: un pequeño paso.
  - text: Espacio personal A tu ritmo
- banner:
  - text: Mi espacio
  - strong: Proyectos
  - text: Un paso cada día
  - button "Cerrar sesión"
- main:
  - paragraph: TUS IDEAS, CON PERSPECTIVA.
  - heading "Proyectos" [level=1]
  - paragraph: Vuelve a lo que quieres construir. Un proyecto, un pequeño paso.
  - heading "Todavía no tienes proyectos" [level=2]
  - paragraph: Dale un nombre a esa idea que quieres guardar. Podrás volver a ella cuando quieras.
  - link "Crear proyecto":
    - /url: /proyectos/nuevo
  - text: Un lugar para lo que quieres construir. Con calma. Con intención.
```

# Test source

```ts
  1  | import { test, expect } from "./support/authenticated-test.mjs";
  2  | import { sql } from "./support/projects.mjs";
  3  | 
  4  | test("history: workspace opens an empty real history page @s28", async ({
  5  |   page,
  6  | }) => {
  7  |   sql(
  8  |     "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  9  |   );
  10 |   await page.goto("/proyectos");
  11 |   const link = page
  12 |     .getByRole("navigation", { name: "Principal", exact: true })
  13 |     .getByRole("link", { name: "Historial", exact: true });
> 14 |   await expect(link).toBeVisible();
     |                      ^ Error: expect(locator).toBeVisible() failed
  15 |   await expect(link).toHaveAttribute("href", "/historial");
  16 |   const reading = page.waitForResponse(
  17 |     (response) =>
  18 |       response.request().method() === "GET" &&
  19 |       new URL(response.url()).pathname === "/api/v1/history",
  20 |   );
  21 |   await link.click();
  22 |   await expect(page).toHaveURL(/\/historial$/);
  23 |   await expect(
  24 |     page.getByRole("heading", { name: "Historial", exact: true, level: 1 }),
  25 |   ).toBeVisible();
  26 |   const response = await reading;
  27 |   expect(response.status()).toBe(200);
  28 |   expect(response.headers()["cache-control"]).toContain("no-store");
  29 |   expect(await response.json()).toEqual({ items: [], nextCursor: null });
  30 |   expect(sql("SELECT count(*) FROM outbox_events")).toBe("0");
  31 | });
  32 | 
```