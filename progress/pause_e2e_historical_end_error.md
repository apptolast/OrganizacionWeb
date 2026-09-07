# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: pause-resume-session.spec.mjs >> pause_resume_session: explicit start pause resume and reload preserve historical end @s4 @s29 @s35
- Location: e2e\pause-resume-session.spec.mjs:16:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:17:58.954015Z"]')
Expected: visible
Error: strict mode violation: getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:17:58.954015Z"]') resolved to 2 elements:
    1) <time datetime="2026-09-07T08:17:58.954015Z">7 de septiembre de 2026 a las 8:17:58 UTC</time> aka getByRole('paragraph').filter({ hasText: 'Fin previsto: 7 de septiembre' }).getByRole('time')
    2) <time datetime="2026-09-07T08:17:58.954015Z">7 de septiembre de 2026 a las 8:17:58 UTC</time> aka getByRole('paragraph').filter({ hasText: 'Fin previsto original: 7 de' }).getByRole('time')

Call log:
  - Expect "toBeVisible" getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:17:58.954015Z"]') with timeout 5000ms
  - waiting for getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:17:58.954015Z"]')

```

# Page snapshot

```yaml
- generic [ref=f1e3]:
  - link "Saltar al contenido" [ref=f1e4] [cursor=pointer]:
    - /url: "#proyectos"
  - complementary [ref=f1e5]:
    - generic [ref=f1e6]:
      - generic [aria-hidden] [ref=f1e7]: o.
      - generic [ref=f1e8]: OrganizationWeb
    - paragraph [ref=f1e9]: TU ESPACIO
    - navigation "Principal" [ref=f1e10]:
      - link "Hoy" [ref=f1e11] [cursor=pointer]:
        - /url: /
        - generic [aria-hidden] [ref=f1e12]: ◉
        - text: Hoy
      - link "Proyectos" [ref=f1e13] [cursor=pointer]:
        - /url: /proyectos
        - generic [aria-hidden] [ref=f1e14]: ▦
        - text: Proyectos
      - link "Disponibilidad" [ref=f1e16] [cursor=pointer]:
        - /url: /disponibilidad
        - generic [aria-hidden] [ref=f1e17]: ◷
        - text: Disponibilidad
    - generic [ref=f1e18]:
      - text: ↗
      - paragraph [ref=f1e19]:
        - text: Las grandes cosasempiezan con
        - emphasis [ref=f1e20]: un pequeño paso.
    - generic [ref=f1e22]:
      - generic [aria-hidden] [ref=f1e23]: P
      - generic [ref=f1e24]:
        - text: Espacio personal
        - generic [ref=f1e25]: A tu ritmo
  - generic [ref=f1e26]:
    - banner [ref=f1e27]:
      - generic [ref=f1e28]:
        - text: Mi espacio /
        - strong [ref=f1e29]: Proyectos
      - generic [ref=f1e30]:
        - generic [aria-hidden] [ref=f1e31]: ●
        - text: Un paso cada día
      - button "Cerrar sesión" [ref=f1e32] [cursor=pointer]
    - main [ref=f1e33]:
      - link "Volver al proyecto" [ref=f1e34] [cursor=pointer]:
        - /url: /proyectos/171c51cc-520d-4f2d-b70e-897305ce4551
      - article [ref=f1e35]:
        - heading "Leer y retomar las notas" [active] [level=1] [ref=f1e36]
        - paragraph
        - region "Estado de la tarea" [ref=f1e37]:
          - heading "Estado de la tarea" [level=2] [ref=f1e38]
          - generic [ref=f1e39]: Pendiente
          - button "Completar tarea" [ref=f1e40] [cursor=pointer]
        - region "Historial de la tarea" [ref=f1e41]:
          - heading "Historial de la tarea" [level=2] [ref=f1e42]
          - paragraph [ref=f1e43]: Todavía no hay cambios de estado.
        - paragraph [ref=f1e44]: Sin estimación
        - region "Padre directo" [ref=f1e45]:
          - status [ref=f1e46]: Tarea principal confirmada
        - region [ref=f1e47]:
          - heading "Sesión de trabajo" [level=2] [ref=f1e48]
          - paragraph [ref=f1e49]: Elige cuánto tiempo te propones trabajar. El fin previsto no cierra la sesión automáticamente ni acredita tiempo neto o una tarea completada.
          - button "Actualizar sesión activa" [ref=f1e50] [cursor=pointer]
          - article [ref=f1e51]:
            - paragraph [ref=f1e52]:
              - text: "Inicio:"
              - time [ref=f1e53]: 7 de septiembre de 2026 a las 7:52:58 UTC
            - paragraph [ref=f1e54]: "Duración prevista: 25 minutos"
            - paragraph [ref=f1e55]:
              - text: "Fin previsto:"
              - time [ref=f1e56]: 7 de septiembre de 2026 a las 8:17:58 UTC
            - paragraph [ref=f1e57]: "Zona: UTC"
            - link "Ir a la tarea de esta sesión" [ref=f1e58] [cursor=pointer]:
              - /url: /proyectos/171c51cc-520d-4f2d-b70e-897305ce4551/tareas/9998b723-b991-4321-9405-8e5f6d0adea8
          - paragraph [ref=f1e59]: La pausa no desplaza el fin previsto de la sesión.
          - generic [ref=f1e60]:
            - heading "Estado de la sesión" [level=3] [ref=f1e61]
            - button "Actualizar estado de la sesión" [ref=f1e62] [cursor=pointer]
            - paragraph [ref=f1e63]: En curso
            - paragraph [ref=f1e64]: "Tiempo de trabajo hasta la actualización: 0,228191 s"
            - paragraph [ref=f1e65]:
              - text: "Actualizado:"
              - time [ref=f1e66]: 7 de septiembre de 2026 a las 7:52:59 UTC
              - text: UTC
            - button "Pausar" [ref=f1e67] [cursor=pointer]
            - link "Cerrar sesión de trabajo" [ref=f1e68] [cursor=pointer]:
              - /url: /proyectos/171c51cc-520d-4f2d-b70e-897305ce4551/tareas/9998b723-b991-4321-9405-8e5f6d0adea8/sesiones/862e7d09-2992-4c93-bc74-af5f6257352f
          - generic [ref=f1e69]:
            - heading "Fin de la sesión" [level=3] [ref=f1e70]
            - button "Actualizar fin acordado" [ref=f1e71] [cursor=pointer]
            - paragraph [ref=f1e72]:
              - text: "Fin previsto original:"
              - time [ref=f1e73]: 7 de septiembre de 2026 a las 8:17:58 UTC
              - text: UTC
            - link "Cerrar sesión de trabajo" [ref=f1e74] [cursor=pointer]:
              - /url: /proyectos/171c51cc-520d-4f2d-b70e-897305ce4551/tareas/9998b723-b991-4321-9405-8e5f6d0adea8/sesiones/862e7d09-2992-4c93-bc74-af5f6257352f
            - button "Ampliar tiempo" [ref=f1e75] [cursor=pointer]
        - region "Bloques planificados" [ref=f1e76]:
          - heading "Bloques planificados" [level=2] [ref=f1e77]
          - paragraph [ref=f1e78]: Los bloques son tiempo planificado, no trabajo realizado.
          - paragraph [ref=f1e79]: Todavía no hay bloques planificados para esta tarea.
          - list "Bloques planificados"
          - button "Planificar bloque" [ref=f1e80] [cursor=pointer]
          - button "Ver cambios de bloques" [ref=f1e81] [cursor=pointer]
        - region [ref=f1e82]:
          - heading "Estado del proyecto" [level=2] [ref=f1e83]
          - paragraph [ref=f1e84]: Decide qué sigue para este proyecto, a tu ritmo.
          - generic [ref=f1e85]:
            - button "Activar" [ref=f1e86] [cursor=pointer]
            - button "Marcar terminado" [ref=f1e87] [cursor=pointer]
        - region [ref=f1e88]:
          - heading "Subtareas" [level=2] [ref=f1e89]
          - paragraph [ref=f1e90]: Pasos pequeños, con un resultado claro.
          - paragraph [ref=f1e91]: Cada estimación es independiente; las subtareas no se suman automáticamente a la tarea principal.
          - paragraph [ref=f1e92]: Esta tarea todavía no tiene subtareas.
          - paragraph [ref=f1e93]: Terminar el proyecto no completa sus tareas pendientes.
          - generic [ref=f1e94]:
            - generic [ref=f1e95]:
              - generic [ref=f1e96]: Título de la tarea
              - textbox "Título de la tarea" [ref=f1e97]
            - generic [ref=f1e98]:
              - generic [ref=f1e99]: Criterio de finalización
              - textbox "Criterio de finalización" [ref=f1e100]
            - generic [ref=f1e101]:
              - generic [ref=f1e102]: Estimación en minutos
              - spinbutton "Estimación en minutos" [ref=f1e103]
            - paragraph [ref=f1e104]: La estimación no es tiempo trabajado.
            - button "Crear subtarea" [ref=f1e105] [cursor=pointer]
```

# Test source

```ts
  1   | import { chromium } from "@playwright/test";
  2   | import { join, resolve } from "node:path";
  3   | import { loginSession } from "../scripts/session-client.mjs";
  4   | import { mkdir, writeFile } from "node:fs/promises";
  5   | import AxeBuilder from "@axe-core/playwright";
  6   | import { test, expect } from "./support/authenticated-test.mjs";
  7   | import { create, sql } from "./support/projects.mjs";
  8   | import { saveTask } from "./support/tasks.mjs";
  9   | 
  10  | test.beforeEach(() =>
  11  |   sql(
  12  |     "TRUNCATE work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  13  |   ),
  14  | );
  15  | 
  16  | test("pause_resume_session: explicit start pause resume and reload preserve historical end @s4 @s29 @s35", async ({
  17  |   page,
  18  |   request,
  19  | }) => {
  20  |   const project = await create(request, "Trabajo con pausas deliberadas");
  21  |   const task = await saveTask(request, project.id, "Leer y retomar las notas");
  22  |   await page.goto(`/proyectos/${project.id}/tareas/${task.id}`);
  23  |   const section = page.getByRole("region", {
  24  |     name: "Sesión de trabajo",
  25  |     exact: true,
  26  |   });
  27  |   await section
  28  |     .getByLabel("Duración prevista (minutos)", { exact: true })
  29  |     .fill("25");
  30  |   const started = page.waitForResponse(
  31  |     (response) =>
  32  |       response.request().method() === "POST" &&
  33  |       response.url().endsWith("/work-sessions"),
  34  |   );
  35  |   await section
  36  |     .getByRole("button", { name: "Empezar a trabajar", exact: true })
  37  |     .click();
  38  |   const startResponse = await started;
  39  |   expect(startResponse.status()).toBe(201);
  40  |   const session = await startResponse.json();
  41  |   await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  42  |   const paused = page.waitForResponse(
  43  |     (response) =>
  44  |       response.request().method() === "POST" &&
  45  |       response.url().endsWith("/pause"),
  46  |   );
  47  |   await section.getByRole("button", { name: "Pausar", exact: true }).click();
  48  |   const pauseResponse = await paused;
  49  |   expect(pauseResponse.status()).toBe(201);
  50  |   const pause = await pauseResponse.json();
  51  |   expect(pause.after.status).toBe("paused");
  52  |   await expect(
  53  |     section.getByText("Pausa confirmada", { exact: true }),
  54  |   ).toBeVisible();
  55  |   await expect(section.getByText("En pausa", { exact: true })).toBeVisible();
  56  |   await expect(
  57  |     section.getByRole("button", { name: "Pausar", exact: true }),
  58  |   ).toHaveCount(0);
  59  |   const resumed = page.waitForResponse(
  60  |     (response) =>
  61  |       response.request().method() === "POST" &&
  62  |       response.url().endsWith("/resume"),
  63  |   );
  64  |   await section.getByRole("button", { name: "Reanudar", exact: true }).click();
  65  |   const resumeResponse = await resumed;
  66  |   expect(resumeResponse.status()).toBe(201);
  67  |   const resume = await resumeResponse.json();
  68  |   expect(resume.after.status).toBe("running");
  69  |   expect(resume.after.workedMicroseconds).toBe(pause.after.workedMicroseconds);
  70  |   expect(resume.after.session).toEqual(session);
  71  |   await expect(
  72  |     section.getByText("Reanudación confirmada", { exact: true }),
  73  |   ).toBeVisible();
  74  |   await page.reload();
  75  |   await expect(section.getByText("En curso", { exact: true })).toBeVisible();
  76  |   await expect(
  77  |     section.locator(`time[datetime="${session.plannedEndAt}"]`),
> 78  |   ).toBeVisible();
      |     ^ Error: expect(locator).toBeVisible() failed
  79  |   await expect(
  80  |     section.getByRole("button", { name: "Empezar a trabajar", exact: true }),
  81  |   ).toHaveCount(0);
  82  |   expect(sql("SELECT status || ':' || revision FROM work_sessions")).toBe(
  83  |     "running:3",
  84  |   );
  85  |   expect(sql("SELECT count(*) FROM work_session_changes")).toBe("2");
  86  |   expect(sql("SELECT count(*) FROM work_session_intervals")).toBe("1");
  87  | });
  88  | 
  89  | async function inspectStates(page, folder, options = {}) {
  90  |   const widths = options.widths ?? [
  91  |     320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  92  |     768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  93  |     1601, 1920, 2560,
  94  |   ];
  95  |   const evidence = [];
  96  |   async function inspect(state) {
  97  |     if (options.text200) {
  98  |       const scales = await page.evaluate(() => {
  99  |         const elements = [...document.querySelectorAll("main,main *")].filter(
  100 |           (el) => el instanceof HTMLElement,
  101 |         );
  102 |         window.workOriginalFonts ??= new WeakMap();
  103 |         for (const el of elements) {
  104 |           if (!window.workOriginalFonts.has(el))
  105 |             window.workOriginalFonts.set(el, el.style.fontSize);
  106 |           el.style.fontSize = window.workOriginalFonts.get(el);
  107 |         }
  108 |         const before = elements.map((el) =>
  109 |           parseFloat(getComputedStyle(el).fontSize),
  110 |         );
  111 |         elements.forEach((el, index) => {
  112 |           el.style.fontSize = before[index] * 2 + "px";
  113 |         });
  114 |         return elements.map((el, index) => ({
  115 |           before: before[index],
  116 |           after: parseFloat(getComputedStyle(el).fontSize),
  117 |         }));
  118 |       });
  119 |       scales.forEach((size) =>
  120 |         expect(size.after).toBeCloseTo(size.before * 2, 3),
  121 |       );
  122 |       await writeFile(
  123 |         `${folder}/${state}-font-scale.json`,
  124 |         JSON.stringify(scales, null, 2),
  125 |       );
  126 |     }
  127 | 
  128 |     for (const width of widths) {
  129 |       if (options.native)
  130 |         await expect.poll(() => page.evaluate(() => innerWidth)).toBe(width);
  131 |       else
  132 |         await page.setViewportSize({
  133 |           width,
  134 |           height: width === 768 ? 400 : 900,
  135 |         });
  136 |       const measured = await page.evaluate(() => ({
  137 |         width: innerWidth,
  138 |         height: innerHeight,
  139 |         scroll: document.documentElement.scrollWidth,
  140 |         controls: [
  141 |           ...document.querySelectorAll(
  142 |             'nav[aria-label="Principal"] a,main button,main a,main input,main select,header button',
  143 |           ),
  144 |         ]
  145 |           .filter((el) => el.getClientRects().length)
  146 |           .map((el) => {
  147 |             const box = el.getBoundingClientRect();
  148 |             return {
  149 |               name:
  150 |                 el.getAttribute("aria-label") ||
  151 |                 el.labels?.[0]?.textContent ||
  152 |                 el.textContent,
  153 |               x: box.x,
  154 |               y: box.y,
  155 |               width: box.width,
  156 |               height: box.height,
  157 |             };
  158 |           }),
  159 |       }));
  160 |       evidence.push({ state, ...measured });
  161 |       await writeFile(
  162 |         `${folder}/geometry.json`,
  163 |         JSON.stringify(evidence, null, 2),
  164 |       );
  165 |       expect(
  166 |         measured.scroll,
  167 |         `${state}:${width} page overflow`,
  168 |       ).toBeLessThanOrEqual(width);
  169 |       for (const box of measured.controls) {
  170 |         expect(
  171 |           box.x,
  172 |           `${state}:${width}:${box.name} left`,
  173 |         ).toBeGreaterThanOrEqual(0);
  174 |         expect(
  175 |           box.x + box.width,
  176 |           `${state}:${width}:${box.name} right`,
  177 |         ).toBeLessThanOrEqual(width + 1);
  178 |         expect(
```