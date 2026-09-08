# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: pause-resume-session.spec.mjs >> pause_resume_session: responsive running pending uncertain paused and query error @s29 @s31 @s32 @s37 @s38
- Location: e2e\pause-resume-session.spec.mjs:278:1

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
        - /url: /proyectos/0d1013ce-8119-446f-9e2a-f8f189c73098
      - article [ref=e39]:
        - heading "Preparar notas comprensibles para retomar el trabajo y revisar con calma la información importante 🧭" [level=1] [ref=e40]
        - paragraph
        - link "Ver historial de esta tarea" [ref=e41] [cursor=pointer]:
          - /url: /historial?projectId=0d1013ce-8119-446f-9e2a-f8f189c73098&taskId=f16cceef-ff0b-400d-b8e8-b0501ecc3856
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
              - time [ref=e62]: 8 de septiembre de 2026 a las 4:42:26 UTC
            - paragraph [ref=e63]: "Duración prevista: 25 minutos"
            - paragraph [ref=e64]:
              - text: "Fin previsto:"
              - time [ref=e65]: 8 de septiembre de 2026 a las 5:07:26 UTC
            - paragraph [ref=e66]: "Zona: UTC"
            - link "Ir a la tarea de esta sesión" [ref=e67] [cursor=pointer]:
              - /url: /proyectos/0d1013ce-8119-446f-9e2a-f8f189c73098/tareas/f16cceef-ff0b-400d-b8e8-b0501ecc3856
          - paragraph [ref=e68]: La pausa no desplaza el fin previsto de la sesión.
          - generic [ref=e69]:
            - heading "Estado de la sesión" [level=3] [ref=e70]
            - button "Actualizar estado de la sesión" [active] [ref=e71] [cursor=pointer]
            - status [ref=e72]: Pausa confirmada
            - alert [ref=e73]: No podemos consultar el estado de la sesión
            - button "Reintentar consulta" [ref=e74] [cursor=pointer]
          - generic [ref=e75]:
            - heading "Fin de la sesión" [level=3] [ref=e76]
            - button "Actualizar fin acordado" [ref=e77] [cursor=pointer]
            - paragraph [ref=e78]:
              - text: "Fin previsto original:"
              - time [ref=e79]: 8 de septiembre de 2026 a las 5:07:26 UTC
              - text: UTC
            - link "Cerrar sesión de trabajo" [ref=e80] [cursor=pointer]:
              - /url: /proyectos/0d1013ce-8119-446f-9e2a-f8f189c73098/tareas/f16cceef-ff0b-400d-b8e8-b0501ecc3856/sesiones/43039b1d-0f82-4f25-b0c7-1d630f78f9d7
            - button "Ampliar tiempo" [ref=e81] [cursor=pointer]
        - region "Bloques planificados" [ref=e82]:
          - heading "Bloques planificados" [level=2] [ref=e83]
          - paragraph [ref=e84]: Los bloques son tiempo planificado, no trabajo realizado.
          - paragraph [ref=e85]: Todavía no hay bloques planificados para esta tarea.
          - list "Bloques planificados"
          - button "Planificar bloque" [ref=e86] [cursor=pointer]
          - button "Ver cambios de bloques" [ref=e87] [cursor=pointer]
        - region [ref=e88]:
          - heading "Estado del proyecto" [level=2] [ref=e89]
          - paragraph [ref=e90]: Decide qué sigue para este proyecto, a tu ritmo.
          - generic [ref=e91]:
            - button "Activar" [ref=e92] [cursor=pointer]
            - button "Marcar terminado" [ref=e93] [cursor=pointer]
        - region [ref=e94]:
          - heading "Subtareas" [level=2] [ref=e95]
          - region "Personalización de vista" [ref=e96]:
            - button "Personalizar vista" [ref=e97] [cursor=pointer]
            - button "Gestionar campos personales" [ref=e99] [cursor=pointer]
          - paragraph [ref=e100]: Pasos pequeños, con un resultado claro.
          - paragraph [ref=e101]: Cada estimación es independiente; las subtareas no se suman automáticamente a la tarea principal.
          - paragraph [ref=e102]: Esta tarea todavía no tiene subtareas.
          - paragraph [ref=e103]: Terminar el proyecto no completa sus tareas pendientes.
          - generic [ref=e104]:
            - generic [ref=e105]:
              - generic [ref=e106]: Título de la tarea
              - textbox "Título de la tarea" [ref=e107]
            - generic [ref=e108]:
              - generic [ref=e109]: Criterio de finalización
              - textbox "Criterio de finalización" [ref=e110]
            - generic [ref=e111]:
              - generic [ref=e112]: Estimación en minutos
              - spinbutton "Estimación en minutos" [ref=e113]
            - paragraph [ref=e114]: La estimación no es tiempo trabajado.
            - button "Crear subtarea" [ref=e115] [cursor=pointer]
```

# Test source

```ts
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
  77  |     section
  78  |       .getByRole("paragraph")
  79  |       .filter({ hasText: /^Fin previsto:/ })
  80  |       .locator(`time[datetime="${session.plannedEndAt}"]`),
  81  |   ).toBeVisible();
  82  |   await expect(
  83  |     section.getByRole("button", { name: "Empezar a trabajar", exact: true }),
  84  |   ).toHaveCount(0);
  85  |   expect(sql("SELECT status || ':' || revision FROM work_sessions")).toBe(
  86  |     "running:3",
  87  |   );
  88  |   expect(sql("SELECT count(*) FROM work_session_changes")).toBe("2");
  89  |   expect(sql("SELECT count(*) FROM work_session_intervals")).toBe("1");
  90  | });
  91  | 
  92  | async function inspectStates(page, folder, options = {}) {
  93  |   const widths = options.widths ?? [
  94  |     320, 359, 360, 361, 390, 419, 420, 421, 480, 599, 600, 601, 699, 700, 701,
  95  |     768, 820, 999, 1000, 1001, 1024, 1099, 1100, 1101, 1280, 1440, 1599, 1600,
  96  |     1601, 1920, 2560,
  97  |   ];
  98  |   const evidence = [];
  99  |   async function inspect(state) {
  100 |     if (options.text200) {
  101 |       const scales = await page.evaluate(() => {
  102 |         const elements = [...document.querySelectorAll("main,main *")].filter(
  103 |           (el) => el instanceof HTMLElement,
  104 |         );
  105 |         window.workOriginalFonts ??= new WeakMap();
  106 |         for (const el of elements) {
  107 |           if (!window.workOriginalFonts.has(el))
  108 |             window.workOriginalFonts.set(el, el.style.fontSize);
  109 |           el.style.fontSize = window.workOriginalFonts.get(el);
  110 |         }
  111 |         const before = elements.map((el) =>
  112 |           parseFloat(getComputedStyle(el).fontSize),
  113 |         );
  114 |         elements.forEach((el, index) => {
  115 |           el.style.fontSize = before[index] * 2 + "px";
  116 |         });
  117 |         return elements.map((el, index) => ({
  118 |           before: before[index],
  119 |           after: parseFloat(getComputedStyle(el).fontSize),
  120 |         }));
  121 |       });
  122 |       scales.forEach((size) =>
  123 |         expect(size.after).toBeCloseTo(size.before * 2, 3),
  124 |       );
  125 |       await writeFile(
  126 |         `${folder}/${state}-font-scale.json`,
  127 |         JSON.stringify(scales, null, 2),
  128 |       );
  129 |     }
  130 | 
  131 |     for (const width of widths) {
  132 |       if (options.native)
  133 |         await expect.poll(() => page.evaluate(() => innerWidth)).toBe(width);
  134 |       else
> 135 |         await page.setViewportSize({
      |                    ^ Error: page.setViewportSize: Target page, context or browser has been closed
  136 |           width,
  137 |           height: width === 768 ? 400 : 900,
  138 |         });
  139 |       const measured = await page.evaluate(() => ({
  140 |         width: innerWidth,
  141 |         height: innerHeight,
  142 |         scroll: document.documentElement.scrollWidth,
  143 |         controls: [
  144 |           ...document.querySelectorAll(
  145 |             'nav[aria-label="Principal"] a,main button,main a,main input,main select,header button',
  146 |           ),
  147 |         ]
  148 |           .filter((el) => el.getClientRects().length)
  149 |           .map((el) => {
  150 |             const box = el.getBoundingClientRect();
  151 |             return {
  152 |               name:
  153 |                 el.getAttribute("aria-label") ||
  154 |                 el.labels?.[0]?.textContent ||
  155 |                 el.textContent,
  156 |               x: box.x,
  157 |               y: box.y,
  158 |               width: box.width,
  159 |               height: box.height,
  160 |             };
  161 |           }),
  162 |       }));
  163 |       evidence.push({ state, ...measured });
  164 |       await writeFile(
  165 |         `${folder}/geometry.json`,
  166 |         JSON.stringify(evidence, null, 2),
  167 |       );
  168 |       expect(
  169 |         measured.scroll,
  170 |         `${state}:${width} page overflow`,
  171 |       ).toBeLessThanOrEqual(width);
  172 |       for (const box of measured.controls) {
  173 |         expect(
  174 |           box.x,
  175 |           `${state}:${width}:${box.name} left`,
  176 |         ).toBeGreaterThanOrEqual(0);
  177 |         expect(
  178 |           box.x + box.width,
  179 |           `${state}:${width}:${box.name} right`,
  180 |         ).toBeLessThanOrEqual(width + 1);
  181 |         expect(
  182 |           box.width,
  183 |           `${state}:${width}:${box.name} width`,
  184 |         ).toBeGreaterThanOrEqual(44);
  185 |         expect(
  186 |           box.height,
  187 |           `${state}:${width}:${box.name} height`,
  188 |         ).toBeGreaterThanOrEqual(44);
  189 |       }
  190 |       for (let a = 0; a < measured.controls.length; a++)
  191 |         for (let b = a + 1; b < measured.controls.length; b++) {
  192 |           const x = measured.controls[a],
  193 |             y = measured.controls[b];
  194 |           expect(
  195 |             Math.min(x.x + x.width, y.x + y.width) - Math.max(x.x, y.x) > 1 &&
  196 |               Math.min(x.y + x.height, y.y + y.height) - Math.max(x.y, y.y) > 1,
  197 |             `${state}:${width} overlap ${x.name}/${y.name}`,
  198 |           ).toBe(false);
  199 |         }
  200 |       if (width === 320 || width === 1440) {
  201 |         const skip = await page.locator(".skip-link").evaluate((el) => {
  202 |           const box = el.getBoundingClientRect();
  203 |           return {
  204 |             focused: document.activeElement === el,
  205 |             top: box.top,
  206 |             bottom: box.bottom,
  207 |             scrollY,
  208 |             viewportHeight: innerHeight,
  209 |           };
  210 |         });
  211 |         await writeFile(
  212 |           `${folder}/${state}-${width}-skiplink.json`,
  213 |           JSON.stringify(skip, null, 2),
  214 |         );
  215 |         if (!skip.focused) expect(skip.bottom).toBeLessThanOrEqual(0);
  216 |         if (options.native) {
  217 |           const viewportCapture = await page.context().newCDPSession(page);
  218 |           const viewportShot = await viewportCapture.send(
  219 |             "Page.captureScreenshot",
  220 |             { format: "png", fromSurface: true, captureBeyondViewport: false },
  221 |           );
  222 |           await writeFile(
  223 |             `${folder}/${state}-${width}-viewport.png`,
  224 |             Buffer.from(viewportShot.data, "base64"),
  225 |           );
  226 |           await viewportCapture.detach();
  227 |         } else {
  228 |           await page.screenshot({
  229 |             path: `${folder}/${state}-${width}-viewport.png`,
  230 |           });
  231 |         }
  232 |         if (options.native) {
  233 |           const dimensions = await page.evaluate(() => ({
  234 |             width: document.documentElement.scrollWidth,
  235 |             height: document.documentElement.scrollHeight,
```