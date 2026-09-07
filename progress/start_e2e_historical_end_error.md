# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: start-work-session.spec.mjs >> start_work_session: explicit start persists and reload discovers the original active session @s1 @s22 @s23 @s28 @s29 @s36 @s37
- Location: e2e\start-work-session.spec.mjs:146:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:23:17.356863Z"]')
Expected: visible
Error: strict mode violation: getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:23:17.356863Z"]') resolved to 2 elements:
    1) <time datetime="2026-09-07T08:23:17.356863Z">7 de septiembre de 2026 a las 8:23:17 UTC</time> aka getByRole('paragraph').filter({ hasText: 'Fin previsto: 7 de septiembre' }).getByRole('time')
    2) <time datetime="2026-09-07T08:23:17.356863Z">7 de septiembre de 2026 a las 8:23:17 UTC</time> aka getByRole('paragraph').filter({ hasText: 'Fin previsto original: 7 de' }).getByRole('time')

Call log:
  - Expect "toBeVisible" getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:23:17.356863Z"]') with timeout 5000ms
  - waiting for getByRole('region', { name: 'Sesión de trabajo', exact: true }).locator('time[datetime="2026-09-07T08:23:17.356863Z"]')

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
        - /url: /proyectos/39fbca50-d3e7-4578-a354-cf9d012a7e89
      - article [ref=f1e35]:
        - heading "Leer y preparar notas" [active] [level=1] [ref=f1e36]
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
              - time [ref=f1e53]: 7 de septiembre de 2026 a las 7:58:17 UTC
            - paragraph [ref=f1e54]: "Duración prevista: 25 minutos"
            - paragraph [ref=f1e55]:
              - text: "Fin previsto:"
              - time [ref=f1e56]: 7 de septiembre de 2026 a las 8:23:17 UTC
            - paragraph [ref=f1e57]: "Zona: UTC"
            - link "Ir a la tarea de esta sesión" [ref=f1e58] [cursor=pointer]:
              - /url: /proyectos/39fbca50-d3e7-4578-a354-cf9d012a7e89/tareas/44a3f02d-1ec1-464a-8593-56e74c8fcc0e
          - paragraph [ref=f1e59]: La pausa no desplaza el fin previsto de la sesión.
          - generic [ref=f1e60]:
            - heading "Estado de la sesión" [level=3] [ref=f1e61]
            - button "Actualizar estado de la sesión" [ref=f1e62] [cursor=pointer]
            - paragraph [ref=f1e63]: En curso
            - paragraph [ref=f1e64]: "Tiempo de trabajo hasta la actualización: 0,562497 s"
            - paragraph [ref=f1e65]:
              - text: "Actualizado:"
              - time [ref=f1e66]: 7 de septiembre de 2026 a las 7:58:17 UTC
              - text: UTC
            - button "Pausar" [ref=f1e67] [cursor=pointer]
            - link "Cerrar sesión de trabajo" [ref=f1e68] [cursor=pointer]:
              - /url: /proyectos/39fbca50-d3e7-4578-a354-cf9d012a7e89/tareas/44a3f02d-1ec1-464a-8593-56e74c8fcc0e/sesiones/525f992f-0233-441c-b681-b3b09f7a00de
          - generic [ref=f1e69]:
            - heading "Fin de la sesión" [level=3] [ref=f1e70]
            - button "Actualizar fin acordado" [ref=f1e71] [cursor=pointer]
            - paragraph [ref=f1e72]:
              - text: "Fin previsto original:"
              - time [ref=f1e73]: 7 de septiembre de 2026 a las 8:23:17 UTC
              - text: UTC
            - link "Cerrar sesión de trabajo" [ref=f1e74] [cursor=pointer]:
              - /url: /proyectos/39fbca50-d3e7-4578-a354-cf9d012a7e89/tareas/44a3f02d-1ec1-464a-8593-56e74c8fcc0e/sesiones/525f992f-0233-441c-b681-b3b09f7a00de
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
  137 |     section.getByText("Sesión iniciada", { exact: true }),
  138 |   ).toBeVisible();
  139 |   await expect(
  140 |     section.getByRole("heading", { name: "Sesión de trabajo", exact: true }),
  141 |   ).toBeFocused();
  142 |   expect(posts).toBe(1);
  143 |   expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
  144 | });
  145 | 
  146 | test("start_work_session: explicit start persists and reload discovers the original active session @s1 @s22 @s23 @s28 @s29 @s36 @s37", async ({
  147 |   page,
  148 |   request,
  149 | }) => {
  150 |   const project = await create(request, "Una sesión deliberada");
  151 |   const task = await saveTask(request, project.id, "Leer y preparar notas");
  152 |   const route = `/proyectos/${project.id}/tareas/${task.id}`;
  153 |   const endpoint = `/api/v1/projects/${project.id}/tasks/${task.id}/work-sessions`;
  154 |   const writes = [];
  155 |   page.on("request", (request) => {
  156 |     if (request.method() === "POST" && request.url().endsWith(endpoint))
  157 |       writes.push(request);
  158 |   });
  159 |   await page.goto(route);
  160 |   const section = page.getByRole("region", {
  161 |     name: "Sesión de trabajo",
  162 |     exact: true,
  163 |   });
  164 |   await expect(
  165 |     section.getByText("No hay una sesión de trabajo activa.", { exact: true }),
  166 |   ).toBeVisible();
  167 |   const duration = section.getByLabel("Duración prevista (minutos)", {
  168 |     exact: true,
  169 |   });
  170 |   await expect(duration).toHaveValue("");
  171 |   expect(writes).toHaveLength(0);
  172 |   expect(sql("SELECT count(*) FROM work_sessions")).toBe("0");
  173 |   await duration.fill("25");
  174 |   const responsePromise = page.waitForResponse(
  175 |     (response) =>
  176 |       response.url().endsWith(endpoint) &&
  177 |       response.request().method() === "POST",
  178 |   );
  179 |   await section
  180 |     .getByRole("button", { name: "Empezar a trabajar", exact: true })
  181 |     .click();
  182 |   const response = await responsePromise;
  183 |   expect(response.status(), await response.text()).toBe(201);
  184 |   const session = await response.json();
  185 |   expect(Object.keys(session).sort()).toEqual(
  186 |     [
  187 |       "id",
  188 |       "projectId",
  189 |       "taskId",
  190 |       "startedAt",
  191 |       "plannedMinutes",
  192 |       "plannedEndAt",
  193 |       "zoneId",
  194 |     ].sort(),
  195 |   );
  196 |   expect(session).toMatchObject({
  197 |     projectId: project.id,
  198 |     taskId: task.id,
  199 |     plannedMinutes: 25,
  200 |     zoneId: "UTC",
  201 |   });
  202 |   expect(session.id).toMatch(/^[0-9a-f-]{36}$/);
  203 |   expect(response.headers().location).toBe(
  204 |     `/api/v1/work-sessions/${session.id}`,
  205 |   );
  206 |   await expect(
  207 |     section.getByText("Sesión iniciada", { exact: true }),
  208 |   ).toBeVisible();
  209 |   await expect(
  210 |     section.getByText("Duración prevista: 25 minutos", { exact: true }),
  211 |   ).toBeVisible();
  212 |   expect(
  213 |     sql(
  214 |       `SELECT planned_minutes || ':' || status || ':' || extract(epoch FROM (planned_end_at-started_at)) FROM work_sessions WHERE id='${session.id}'`,
  215 |     ),
  216 |   ).toBe("25:running:1500.000000");
  217 |   const key = response.request().headers()["idempotency-key"];
  218 |   const recovered = await request.get(
  219 |     `/api/v1/work-sessions/by-request/${key}`,
  220 |   );
  221 |   expect(recovered.status()).toBe(200);
  222 |   expect(await recovered.json()).toEqual(session);
  223 |   const detail = await request.get(`/api/v1/work-sessions/${session.id}`);
  224 |   expect(detail.status()).toBe(200);
  225 |   expect(await detail.json()).toEqual(session);
  226 | 
  227 |   const activePromise = page.waitForResponse((response) =>
  228 |     response.url().endsWith("/api/v1/work-sessions/active"),
  229 |   );
  230 |   await page.reload();
  231 |   expect(await (await activePromise).json()).toEqual({ session });
  232 |   await expect(
  233 |     section.getByText("Duración prevista: 25 minutos", { exact: true }),
  234 |   ).toBeVisible();
  235 |   await expect(
  236 |     section.locator(`time[datetime="${session.plannedEndAt}"]`),
> 237 |   ).toBeVisible();
      |     ^ Error: expect(locator).toBeVisible() failed
  238 |   await expect(
  239 |     section.getByRole("button", { name: "Empezar a trabajar", exact: true }),
  240 |   ).toHaveCount(0);
  241 |   await expect(
  242 |     section.getByRole("link", {
  243 |       name: "Ir a la tarea de esta sesión",
  244 |       exact: true,
  245 |     }),
  246 |   ).toHaveAttribute("href", route);
  247 |   expect(writes).toHaveLength(1);
  248 |   expect(sql("SELECT count(*) FROM work_sessions")).toBe("1");
  249 | });
  250 | 
```