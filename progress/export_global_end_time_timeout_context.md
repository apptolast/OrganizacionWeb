# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: end-time-notification-ux.spec.mjs >> end_time_notification: extension controls and recovery remain accessible across widths @s43 @s44
- Location: e2e\end-time-notification-ux.spec.mjs:330:1

# Error details

```
Test timeout of 180000ms exceeded.
```

```
Error: page.setViewportSize: Target page, context or browser has been closed
```

# Page snapshot

```yaml
- generic [ref=e3]:
  - link "Saltar al contenido" [ref=e4] [cursor=pointer]:
    - /url: "#proyectos"
  - complementary [ref=e5]:
    - generic [ref=e6]:
      - generic [aria-hidden] [ref=e7]: o.
      - generic [ref=e8]: OrganizationWeb
    - paragraph [ref=e9]: TU ESPACIO
    - navigation "Principal" [ref=e10]:
      - link "Hoy" [ref=e11] [cursor=pointer]:
        - /url: /
        - generic [aria-hidden] [ref=e12]: ◉
        - text: Hoy
      - link "Proyectos" [ref=e13] [cursor=pointer]:
        - /url: /proyectos
        - generic [aria-hidden] [ref=e14]: ▦
        - text: Proyectos
      - link "Disponibilidad" [ref=e16] [cursor=pointer]:
        - /url: /disponibilidad
        - generic [aria-hidden] [ref=e17]: ◷
        - text: Disponibilidad
      - link "Historial" [ref=e18] [cursor=pointer]:
        - /url: /historial
      - link "Revisión semanal" [ref=e19] [cursor=pointer]:
        - /url: /revision-semanal
      - link "Apariencia" [ref=e20] [cursor=pointer]:
        - /url: /apariencia
      - link "Exportación" [ref=e21] [cursor=pointer]:
        - /url: /exportacion
    - generic [ref=e22]:
      - text: ↗
      - paragraph [ref=e23]:
        - text: Las grandes cosasempiezan con
        - emphasis [ref=e24]: un pequeño paso.
    - generic [ref=e26]:
      - generic [aria-hidden] [ref=e27]: P
      - generic [ref=e28]:
        - text: Espacio personal
        - generic [ref=e29]: A tu ritmo
  - generic [ref=e30]:
    - banner [ref=e31]:
      - generic [ref=e32]:
        - text: Mi espacio /
        - strong [ref=e33]: Proyectos
      - generic [ref=e34]:
        - generic [aria-hidden] [ref=e35]: ●
        - text: Un paso cada día
      - button "Cerrar sesión" [ref=e36] [cursor=pointer]
    - main [ref=e37]:
      - link "Volver al proyecto" [ref=e38] [cursor=pointer]:
        - /url: /proyectos/e56bee29-093a-4887-b97a-f45c958ed7d4
      - article [ref=e39]:
        - heading "Conservar decisiones importantes y un fin deliberado 🧭" [level=1] [ref=e40]
        - paragraph
        - link "Ver historial de esta tarea" [ref=e41] [cursor=pointer]:
          - /url: /historial?projectId=e56bee29-093a-4887-b97a-f45c958ed7d4&taskId=f5a47c80-f81c-4be7-bb3d-20aa3f02463e
        - region "Estado de la tarea" [ref=e42]:
          - heading "Estado de la tarea" [level=2] [ref=e43]
          - generic [ref=e44]: Pendiente
          - button "Completar tarea" [ref=e45] [cursor=pointer]
        - region "Historial de la tarea" [ref=e46]:
          - heading "Historial de la tarea" [level=2] [ref=e47]
          - paragraph [ref=e48]: Todavía no hay cambios de estado.
        - paragraph [ref=e49]: Sin estimación
        - region "Padre directo" [ref=e50]:
          - status [ref=e51]: Tarea principal confirmada
        - generic [ref=e52]:
          - heading "Campos personales" [level=2] [ref=e53]
          - paragraph [ref=e54]: No hay campos personales activos
        - region [ref=e55]:
          - heading "Sesión de trabajo" [level=2] [ref=e56]
          - paragraph [ref=e57]: Elige cuánto tiempo te propones trabajar. El fin previsto no cierra la sesión automáticamente ni acredita tiempo neto o una tarea completada.
          - button "Actualizar sesión activa" [ref=e58] [cursor=pointer]
          - status [ref=e59]: Sesión iniciada
          - article [ref=e60]:
            - paragraph [ref=e61]:
              - text: "Inicio:"
              - time [ref=e62]: 8 de septiembre de 2026 a las 4:10:57 UTC
            - paragraph [ref=e63]: "Duración prevista: 25 minutos"
            - paragraph [ref=e64]:
              - text: "Fin previsto:"
              - time [ref=e65]: 8 de septiembre de 2026 a las 4:35:57 UTC
            - paragraph [ref=e66]: "Zona: UTC"
            - link "Ir a la tarea de esta sesión" [ref=e67] [cursor=pointer]:
              - /url: /proyectos/e56bee29-093a-4887-b97a-f45c958ed7d4/tareas/f5a47c80-f81c-4be7-bb3d-20aa3f02463e
          - paragraph [ref=e68]: La pausa no desplaza el fin previsto de la sesión.
          - generic [ref=e69]:
            - heading "Estado de la sesión" [level=3] [ref=e70]
            - button "Actualizar estado de la sesión" [ref=e71] [cursor=pointer]
            - paragraph [ref=e72]: En curso
            - paragraph [ref=e73]: "Tiempo de trabajo hasta la actualización: 127,746057 s"
            - paragraph [ref=e74]:
              - text: "Actualizado:"
              - time [ref=e75]: 8 de septiembre de 2026 a las 4:13:05 UTC
              - text: UTC
            - button "Pausar" [ref=e76] [cursor=pointer]
            - link "Cerrar sesión de trabajo" [ref=e77] [cursor=pointer]:
              - /url: /proyectos/e56bee29-093a-4887-b97a-f45c958ed7d4/tareas/f5a47c80-f81c-4be7-bb3d-20aa3f02463e/sesiones/ac49fcc7-c3cd-4f6d-9d5a-0aa1f072ac16
          - generic [ref=e78]:
            - heading "Fin de la sesión" [active] [level=3] [ref=e79]
            - button "Actualizar fin acordado" [ref=e80] [cursor=pointer]
            - status [ref=e81]: Ampliación confirmada
            - article "Ampliación guardada" [ref=e82]:
              - paragraph [ref=e83]: 1440 minutos adicionales
              - paragraph [ref=e84]:
                - text: "Fin anterior:"
                - time [ref=e85]: 8 de septiembre de 2026 a las 4:35:57 UTC
                - text: UTC
              - paragraph [ref=e86]:
                - text: "Fin guardado en esta ampliación:"
                - time [ref=e87]: 9 de septiembre de 2026 a las 4:35:57 UTC
                - text: UTC
            - paragraph [ref=e88]:
              - text: "Fin previsto original:"
              - time [ref=e89]: 8 de septiembre de 2026 a las 4:35:57 UTC
              - text: UTC
            - paragraph [ref=e90]:
              - text: "Fin acordado actual:"
              - time [ref=e91]: 9 de septiembre de 2026 a las 4:35:57 UTC
              - text: UTC
            - link "Cerrar sesión de trabajo" [ref=e92] [cursor=pointer]:
              - /url: /proyectos/e56bee29-093a-4887-b97a-f45c958ed7d4/tareas/f5a47c80-f81c-4be7-bb3d-20aa3f02463e/sesiones/ac49fcc7-c3cd-4f6d-9d5a-0aa1f072ac16
            - button "Ampliar tiempo" [ref=e93] [cursor=pointer]
        - region "Bloques planificados" [ref=e94]:
          - heading "Bloques planificados" [level=2] [ref=e95]
          - paragraph [ref=e96]: Los bloques son tiempo planificado, no trabajo realizado.
          - paragraph [ref=e97]: Todavía no hay bloques planificados para esta tarea.
          - list "Bloques planificados"
          - button "Planificar bloque" [ref=e98] [cursor=pointer]
          - button "Ver cambios de bloques" [ref=e99] [cursor=pointer]
        - region [ref=e100]:
          - heading "Estado del proyecto" [level=2] [ref=e101]
          - paragraph [ref=e102]: Decide qué sigue para este proyecto, a tu ritmo.
          - generic [ref=e103]:
            - button "Activar" [ref=e104] [cursor=pointer]
            - button "Marcar terminado" [ref=e105] [cursor=pointer]
        - region [ref=e106]:
          - heading "Subtareas" [level=2] [ref=e107]
          - region "Personalización de vista" [ref=e108]:
            - button "Personalizar vista" [ref=e109] [cursor=pointer]
            - button "Gestionar campos personales" [ref=e111] [cursor=pointer]
          - paragraph [ref=e112]: Pasos pequeños, con un resultado claro.
          - paragraph [ref=e113]: Cada estimación es independiente; las subtareas no se suman automáticamente a la tarea principal.
          - paragraph [ref=e114]: Esta tarea todavía no tiene subtareas.
          - paragraph [ref=e115]: Terminar el proyecto no completa sus tareas pendientes.
          - generic [ref=e116]:
            - generic [ref=e117]:
              - generic [ref=e118]: Título de la tarea
              - textbox "Título de la tarea" [ref=e119]
            - generic [ref=e120]:
              - generic [ref=e121]: Criterio de finalización
              - textbox "Criterio de finalización" [ref=e122]
            - generic [ref=e123]:
              - generic [ref=e124]: Estimación en minutos
              - spinbutton "Estimación en minutos" [ref=e125]
            - paragraph [ref=e126]: La estimación no es tiempo trabajado.
            - button "Crear subtarea" [ref=e127] [cursor=pointer]
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
  12  |     "TRUNCATE project_custom_field_values, task_custom_field_values, work_session_intervals, work_session_changes, work_sessions, block_changes, block_projections, planned_blocks, availability_preferences, task_status_history, tasks, outbox_events, projects",
  13  |   ),
  14  | );
  15  | async function inspectStates(page, folder, options = {}) {
  16  |   const widths = options.widths ?? [
  17  |     320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  18  |     768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  19  |     1601, 1920, 2560,
  20  |   ];
  21  |   const evidence = [];
  22  |   async function inspect(state) {
  23  |     if (options.text200) {
  24  |       const scales = await page.evaluate(() => {
  25  |         const elements = [...document.querySelectorAll("main,main *")].filter(
  26  |           (el) => el instanceof HTMLElement,
  27  |         );
  28  |         window.workOriginalFonts ??= new WeakMap();
  29  |         for (const el of elements) {
  30  |           if (!window.workOriginalFonts.has(el))
  31  |             window.workOriginalFonts.set(el, el.style.fontSize);
  32  |           el.style.fontSize = window.workOriginalFonts.get(el);
  33  |         }
  34  |         const before = elements.map((el) =>
  35  |           parseFloat(getComputedStyle(el).fontSize),
  36  |         );
  37  |         elements.forEach((el, index) => {
  38  |           el.style.fontSize = before[index] * 2 + "px";
  39  |         });
  40  |         return elements.map((el, index) => ({
  41  |           before: before[index],
  42  |           after: parseFloat(getComputedStyle(el).fontSize),
  43  |         }));
  44  |       });
  45  |       scales.forEach((size) =>
  46  |         expect(size.after).toBeCloseTo(size.before * 2, 3),
  47  |       );
  48  |       await writeFile(
  49  |         `${folder}/${state}-font-scale.json`,
  50  |         JSON.stringify(scales, null, 2),
  51  |       );
  52  |     }
  53  | 
  54  |     for (const width of widths) {
  55  |       if (options.native)
  56  |         await expect.poll(() => page.evaluate(() => innerWidth)).toBe(width);
  57  |       else
> 58  |         await page.setViewportSize({
      |                    ^ Error: page.setViewportSize: Target page, context or browser has been closed
  59  |           width,
  60  |           height: width === 768 ? 400 : 900,
  61  |         });
  62  |       const measured = await page.evaluate(() => ({
  63  |         width: innerWidth,
  64  |         height: innerHeight,
  65  |         scroll: document.documentElement.scrollWidth,
  66  |         controls: [
  67  |           ...document.querySelectorAll(
  68  |             'nav[aria-label="Principal"] a,main button,main a,main input,main select,main textarea,header button',
  69  |           ),
  70  |         ]
  71  |           .filter((el) => el.getClientRects().length)
  72  |           .map((el) => {
  73  |             const box = el.getBoundingClientRect();
  74  |             return {
  75  |               name:
  76  |                 el.getAttribute("aria-label") ||
  77  |                 el.labels?.[0]?.textContent ||
  78  |                 el.textContent,
  79  |               x: box.x,
  80  |               y: box.y,
  81  |               width: box.width,
  82  |               height: box.height,
  83  |             };
  84  |           }),
  85  |       }));
  86  |       evidence.push({ state, ...measured });
  87  |       await writeFile(
  88  |         `${folder}/geometry.json`,
  89  |         JSON.stringify(evidence, null, 2),
  90  |       );
  91  |       expect(
  92  |         measured.scroll,
  93  |         `${state}:${width} page overflow`,
  94  |       ).toBeLessThanOrEqual(width);
  95  |       for (const box of measured.controls) {
  96  |         expect(
  97  |           box.x,
  98  |           `${state}:${width}:${box.name} left`,
  99  |         ).toBeGreaterThanOrEqual(0);
  100 |         expect(
  101 |           box.x + box.width,
  102 |           `${state}:${width}:${box.name} right`,
  103 |         ).toBeLessThanOrEqual(width + 1);
  104 |         expect(
  105 |           box.width,
  106 |           `${state}:${width}:${box.name} width`,
  107 |         ).toBeGreaterThanOrEqual(44);
  108 |         expect(
  109 |           box.height,
  110 |           `${state}:${width}:${box.name} height`,
  111 |         ).toBeGreaterThanOrEqual(44);
  112 |       }
  113 |       for (let a = 0; a < measured.controls.length; a++)
  114 |         for (let b = a + 1; b < measured.controls.length; b++) {
  115 |           const x = measured.controls[a],
  116 |             y = measured.controls[b];
  117 |           expect(
  118 |             Math.min(x.x + x.width, y.x + y.width) - Math.max(x.x, y.x) > 1 &&
  119 |               Math.min(x.y + x.height, y.y + y.height) - Math.max(x.y, y.y) > 1,
  120 |             `${state}:${width} overlap ${x.name}/${y.name}`,
  121 |           ).toBe(false);
  122 |         }
  123 |       if (width === 320 || width === 1440) {
  124 |         const skip = await page.locator(".skip-link").evaluate((el) => {
  125 |           const box = el.getBoundingClientRect();
  126 |           return {
  127 |             focused: document.activeElement === el,
  128 |             top: box.top,
  129 |             bottom: box.bottom,
  130 |             scrollY,
  131 |             viewportHeight: innerHeight,
  132 |           };
  133 |         });
  134 |         await writeFile(
  135 |           `${folder}/${state}-${width}-skiplink.json`,
  136 |           JSON.stringify(skip, null, 2),
  137 |         );
  138 |         if (!skip.focused) expect(skip.bottom).toBeLessThanOrEqual(0);
  139 |         if (options.native) {
  140 |           const viewportCapture = await page.context().newCDPSession(page);
  141 |           const viewportShot = await viewportCapture.send(
  142 |             "Page.captureScreenshot",
  143 |             { format: "png", fromSurface: true, captureBeyondViewport: false },
  144 |           );
  145 |           await writeFile(
  146 |             `${folder}/${state}-${width}-viewport.png`,
  147 |             Buffer.from(viewportShot.data, "base64"),
  148 |           );
  149 |           await viewportCapture.detach();
  150 |         } else {
  151 |           await page.screenshot({
  152 |             path: `${folder}/${state}-${width}-viewport.png`,
  153 |           });
  154 |         }
  155 |         if (options.native) {
  156 |           const dimensions = await page.evaluate(() => ({
  157 |             width: document.documentElement.scrollWidth,
  158 |             height: document.documentElement.scrollHeight,
```