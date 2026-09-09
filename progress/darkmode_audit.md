# Auditoría del modo oscuro — 8 de septiembre de 2026

Auditoría de solo lectura (sin correcciones) del tema oscuro de toda la aplicación, con evidencia visual (capturas) y numérica (estilos computados, ratios de contraste y axe-core `color-contrast`, etiqueta `wcag2aa`).

## 1. Método

- Pila E2E aislada: `E2E_WEB_PORT=18090 pnpm test:e2e e2e/darkmode-audit.spec.mjs` (docker compose propio, Chromium de Playwright 1.63, sesión iniciada por el fixture `authenticated-test.mjs`). La pila quedó bajada al terminar (`docker ps` sin contenedores `organizationweb-e2e-*`) y el spec temporal se borró.
- Datos reales sembrados por API: 1 proyecto, 2 tareas + 1 subtarea, 3 bloques (uno replanificado y cancelado, uno en 2030-01-08 para la revisión semanal, uno movido al reloj del servidor para Hoy), 1 sesión cerrada (pausa/reanudación/ampliación/cierre) y 1 sesión en curso que después se pausó. Segunda pasada con la base vacía (sin disponibilidad, sin proyectos) y con `/api/v1/today` forzado a 503.
- Dos variantes de oscuro, ambas ejecutadas completas:
  - `pref`: `PUT /api/v1/me/appearance` con `{ theme: "DARK", accentLight: "#244C3C", accentDark: "#B7E4C7" }` (OS emulado en claro).
  - `system`: `theme: "SYSTEM"` + `page.emulateMedia({ colorScheme: "dark" })`.
  - Resultado: `data-theme="dark"`, `color-scheme: dark` e idénticos hallazgos en las dos variantes. Todo lo que sigue aplica a ambas.
- Rutas × anchos 320 / 768 / 1280 px, captura `fullPage`: `/`, `/proyectos`, `/proyectos/nuevo`, `/proyectos/:id/editar`, `/proyectos/:p/tareas/:t` (tarea con historial; tarea con sesión en curso; misma tarea con sesión pausada; misma tarea con el editor "Planificar bloque" abierto), `/proyectos/:p/tareas/:t/sesiones/:s` (en curso, pausada y cerrada), `/disponibilidad`, `/revision-semanal?date=2030-01-08&zoneId=UTC`, `/historial`, `/apariencia`, `/exportacion`, `/importacion`, `/no-existe` (404) y la pantalla de login sin sesión (más el estado de error de credenciales).
- Escáner en página (JS): recorre todos los elementos visibles, lee `getComputedStyle` (fondo, color, borde, sombra, `::placeholder`), calcula el fondo efectivo subiendo por los ancestros (con mezcla alfa) y marca: fondo con luminancia > 0,6 sobre lienzo oscuro (excluyendo el acento y la "Vista previa clara" de `/apariencia`), texto oscuro sobre fondo oscuro, y contraste < 4,5:1 (3:1 para texto grande).
- axe `color-contrast` en cada ruta a 320 y 1280 px en las dos variantes.
- Capturas y JSON crudos en `C:/Users/vhurt/AppData/Local/Temp/claude/C--Users-vhurt-OneDrive-Escritorio-Proyectos-OrganizacionWeb/e2cf4c76-4bd7-40b1-b0c9-86f12e28c92e/scratchpad/darkmode/` (`<ruta>-<ancho>-<variante>.png`, `report-pref.json`, `report-system.json`, `report-run2.json`). No se copiaron al repositorio.

## 2. Resumen

El modo oscuro está bien resuelto en toda la aplicación salvo en **una pantalla: Hoy (`/`)**. Ahí los cuatro paneles de `today.scss` usan colores claros fijos y el texto (que sí usa el token `--ink` claro) queda casi invisible: paneles blancos con texto blanco. Es el único sitio donde axe reporta violaciones de contraste en oscuro (18 nodos únicos). El resto de rutas (13 vistas × 3 anchos × 2 variantes) dan 0 hallazgos del escáner y 0 violaciones axe.

Totales: **9 hallazgos — 4 altos, 2 medios, 3 bajos.**

## 3. Tabla ruta × hallazgo

| # | Ruta | Selector | Archivo:línea | Sev. | Evidencia (captura · valores) |
|---|------|----------|---------------|------|-------------------------------|
| 1 | `/` | `dl.today-summary` (resumen del día) | `frontend/src/today.scss:30-32` (`border: 1px solid #e0e5dc; background: #fff`) | **Alta** | `hoy-1280-pref.png`, `hoy-320-pref.png`, `hoy-notice-1280-pref.png`. Fondo computado `rgb(255,255,255)`; texto `--ink` `#e8eee9` sobre `#ffffff` = **1,18:1** (axe 1,17:1, exige 4,5:1). 10 nodos axe (`dt`/`dd`). Ilegible: el panel se ve blanco vacío. |
| 2 | `/` | `.today-agenda li` (tarjetas de la agenda) | `frontend/src/today.scss:47-48` (`background: #fff; border: 1px solid #e0e5dc`) | **Alta** | `hoy-1280-pref.png`, `hoy-320-pref.png`. Párrafos `#e8eee9` sobre `#ffffff` = **1,18:1** (4 nodos axe). Enlace `h2 > a` con `--accent` `#b7e4c7` sobre `#ffffff` = **1,41:1** (texto grande, exige 3:1; 1 nodo axe). |
| 3 | `/` | `a` dentro de `.today-agenda li` con foco | `frontend/src/styles.scss:429-431` (`:focus-visible { outline: 3px solid var(--accent) }`) sobre el fondo de #2 | **Alta** | `focus-1280-pref.png`. Anillo de foco `#b7e4c7` sobre `#ffffff` = **1,41:1**: el indicador de foco del teclado es prácticamente invisible en la tarjeta. Consecuencia directa de #2; en el resto de la app el anillo da 12,62:1 sobre `--canvas`. |
| 4 | `/` | `.today-notice` y `[role="alert"]` (aviso de disponibilidad no configurada; error de carga con botón Reintentar) | `frontend/src/today.scss:19-24` (`border: 1px solid #b8c8b8; background: #f0f3eb`) | **Alta** | `hoy-notice-1280-pref.png`, `hoy-notice-320-pref.png`, `hoy-alert-1280-pref.png`. Fondo `rgb(240,243,235)`; texto `#e8eee9` sobre `#f0f3eb` = **1,05:1** (axe 1,04:1); enlace "Configurar disponibilidad" `#b7e4c7` sobre `#f0f3eb` = **1,25:1**. 3 nodos axe. El botón "Reintentar" (acento sobre acento) sí es legible. El mensaje de error de Hoy es invisible en oscuro. |
| 5 | Todas | `<meta name="theme-color">` | `frontend/index.html:6` (`content="#f8f9f5"`, sin variante `media`) | Media | `report-*.json` → `themeColorMeta: "#f8f9f5"` con `data-theme="dark"` en todas las rutas. La barra del navegador/PWA en móvil se pinta clara sobre una app oscura. El efecto visual no es capturable en headless; se verificó solo el valor en el DOM. |
| 6 | `/historial` | `select#history-category`, `input[type="date"]` (Desde/Hasta) | `frontend/src/history.scss:11-16` (solo tamaños; sin fondo/borde/color) | Media | `historial-1280-pref.png`, `report-run2.json` → `bg: rgb(59,59,59); border: rgb(133,133,133); color: rgb(255,255,255)`. Son los valores por defecto del agente de usuario con `color-scheme: dark`, no los tokens (`--editable` `#182232`, `--control-border` `#98a99d`). Legible (11:1) pero inconsistente: en `/revision-semanal` y `/disponibilidad` los `select` sí usan `var(--editable)` (`styles.scss` ~380, ~1784) y se ven azul oscuro. |
| 7 | `/proyectos/nuevo` | `.empty-divider` | `frontend/src/styles.scss:891` (`background: #cad9bb`) | Baja | `proyecto-nuevo-1280-pref.png`. Línea decorativa `#cad9bb` (luminancia 0,658) sobre `--panel` `#1f2937`: destaca demasiado frente a `--line` `#4b5563` usado en los demás separadores. Sin impacto en legibilidad. |
| 8 | `/proyectos/nuevo` y tarjetas | Seed art `.seed-stem/.seed-leaf/.seed-soil` y sombras `.form-card`, `.project-row`, `.session-card` | `styles.scss:856, 867, 877, 886` (verdes fijos) y `646, 1192, 1482` (sombras `#2b3f2410`, `#2b3f2407`, `#23392f08`) | Baja | `proyecto-nuevo-1280-pref.png`, `login-1280-system.png`. Los verdes fijos se ven correctos sobre el panel oscuro (ratios 4,5–8:1). Las sombras son oscuras con alfa 3–6 %: en oscuro desaparecen (no hay sombra clara ni halo; el escáner no marcó ninguna `light-shadow`). Sin defecto visible; solo deuda de tokens. |
| 9 | Login (sin sesión) | `main.session-screen` | `styles.scss:1470-1482`, mecanismo en `appearance-state.tsx:128-141` | Baja | `login-oslight-1280.png`, `report-run2b.json` → `dataTheme: null; bg: rgb(248,249,245)`. Con preferencia guardada `DARK` y OS en claro, el login se pinta claro y la app cambia a oscuro tras entrar (la preferencia solo se conoce con sesión). Con OS en oscuro el login es oscuro y correcto (`login-1280-system.png`, `login-error-1280-system.png`). Limitación de diseño, no error. |

## 4. Violaciones axe `color-contrast` (tema oscuro)

Únicamente en `/`, idénticas a 320 y 1280 px y en las dos variantes. Cero violaciones en las 14 rutas restantes (incluidos login, 404, editor de bloques abierto y sesión pausada/cerrada).

| Selector axe | Texto | fg / bg | Ratio | Exigido |
|---|---|---|---|---|
| `dl.today-summary dt:nth-child(1,3,5,7,9)` | Tiempo planificado · Presupuesto del día · Presupuesto sin reservar · Exceso planificado · Cierre previsto | `#e8eee9` / `#ffffff` | 1,17 | 4,5 |
| `dl.today-summary dd:nth-child(2,4,6,8,10)` | 60 min · 120 min · 60 min · 0 min · fecha (o "Desconocido" / "Sin bloques" sin disponibilidad) | `#e8eee9` / `#ffffff` | 1,17 | 4,5 |
| `.today-agenda li > p:nth-child(1,3,4,5)` | nombre del proyecto · "En horario planificado" · objetivo · intervalo horario | `#e8eee9` / `#ffffff` | 1,17 | 4,5 |
| `.today-agenda li h2 > a` | título de la tarea (20,8 px negrita) | `#b7e4c7` / `#ffffff` | 1,40 | 3 |
| `p.today-notice` | "Disponibilidad no configurada. Mostramos UTC; capacidad desconocida." | `#e8eee9` / `#f0f3eb` | 1,04 | 4,5 |
| `.today-notice > a[href$="disponibilidad"]` | "Configurar disponibilidad" | `#b7e4c7` / `#f0f3eb` | 1,25 | 4,5 |
| `.today p[role="alert"]` | "No se pudo actualizar Hoy." | `#e8eee9` / `#f0f3eb` | 1,04 | 4,5 |

Total: 18 nodos únicos (15 con agenda + 2 del aviso + 1 del error).

## 5. Lo que se vio en las capturas

- **Hoy a 1280 y 320** (`hoy-1280-pref.png`, `hoy-320-pref.png`): cabecera, botón "Actualizar" y párrafos correctos; debajo, dos grandes rectángulos blancos en los que solo se intuye el texto (blanco sobre blanco) y el título de la tarea en verde menta pálido casi invisible. A 320 px ocupan casi toda la pantalla. El emoji del nombre del proyecto es lo único que se distingue dentro del panel.
- **Hoy sin disponibilidad / con error** (`hoy-notice-*`, `hoy-alert-1280-pref.png`): panel verdoso muy claro; el mensaje no se lee; el botón "Reintentar" sí (acento sobre acento).
- **Foco por teclado en Hoy** (`focus-1280-pref.png`): el anillo de foco menta se pierde sobre la tarjeta blanca.
- **Tarea con sesión en curso / pausada, sesión de trabajo, editor de bloques** (`tarea-sesion-running-1280-pref.png`, `tarea-bloque-editor-1280-pref.png`, `sesion-paused-1280-pref.png`): todo coherente. Paneles `--panel`, inputs `--editable` con borde visible, botones de acento con texto negro, `time` y estados legibles, `datetime-local` con icono de calendario claro (funciona `color-scheme: dark`).
- **Historial** (`historial-1280-pref.png`, `historial-320-pref.png`): legible; el `select` de categoría y los dos `date` se ven gris neutro (estilo nativo) en lugar del azul oscuro de los demás formularios (#6).
- **Revisión semanal, Disponibilidad, Apariencia, Exportación, Importación, 404**: sin defectos. En Apariencia la "Vista previa clara" es blanca a propósito; los `radio` y `color` nativos se ven en oscuro. En Importación el `input[type=file]` nativo ("Choose File") es oscuro con borde claro.
- **Login** (`login-1280-system.png`, `login-error-1280-system.png`): tarjeta oscura sobre lienzo oscuro, inputs oscuros, botón de acento, mensaje de error legible. Con OS claro y preferencia DARK el login es claro (#9).
- **No observado**: paneles blancos fuera de Hoy, iconos invisibles, scrollbars claras (computed `color-scheme: dark` en `html`, `body` y `main`; las capturas `fullPage` no muestran barras de scroll, así que la barra real no se verificó visualmente), placeholders con contraste insuficiente, texto oscuro sobre fondo oscuro, sombras claras.

## 6. Correcciones priorizadas

1. **`frontend/src/today.scss` (cierra #1–#4, alta).** Sustituir los seis colores fijos por tokens:
   - línea 23 `background: #f0f3eb` → `background: var(--selection)` (o `var(--sidebar)` si se quiere más apagado);
   - línea 21 `border: 1px solid #b8c8b8` → `border: 1px solid var(--line)`;
   - líneas 32 y 47 `background: #fff` → `background: var(--panel)`;
   - líneas 30 y 48 `border: 1px solid #e0e5dc` → `border: 1px solid var(--line)`.
   Con `--panel` `#1f2937`, `--ink` `#e8eee9` da 12,5:1 y el acento `#b7e4c7` queda igual que en el resto de paneles (axe ya lo aprueba en ellos). Al desaparecer el fondo blanco se recupera también el anillo de foco (#3). Añadir a `e2e/today.spec.mjs` una comprobación axe `color-contrast` de `/` con `data-theme="dark"` en tres estados (agenda, aviso, error) para que no vuelva a ocurrir: es la única página que hoy no la tiene.
2. **`frontend/index.html:6` (#5, media).** Declarar dos metas con `media`: `<meta name="theme-color" content="#f8f9f5" media="(prefers-color-scheme: light)">` y `<meta name="theme-color" content="#111827" media="(prefers-color-scheme: dark)">`. Como la preferencia de la app puede contradecir al OS, actualizar además `content` desde `appearance-state.tsx` en el mismo efecto que fija `dataset.theme` (líneas 137-141), leyendo `getComputedStyle(document.documentElement).getPropertyValue("--canvas")`.
3. **`frontend/src/history.scss:11-16` (#6, media).** Añadir a `input, select` el mismo aspecto que usa `.weekly-review select` en `styles.scss`: `background: var(--editable); color: var(--ink); border: 1px solid var(--control-border); border-radius` coherente. Alternativa: mover ese bloque a una regla global de `styles.scss` para `select, input[type="date"], input[type="datetime-local"]` y eliminar duplicados.
4. **`frontend/src/styles.scss:891` (#7, baja).** `.empty-divider { background: var(--line) }`.
5. **`styles.scss:856-886` y `646, 1192, 1482` (#8, baja, opcional).** Seed art: definir `--seed-stem`, `--seed-leaf`, `--seed-leaf-alt`, `--seed-soil` en los dos mixins de apariencia y usarlos. Sombras: `box-shadow: 0 5px 18px rgb(0 0 0 / 6%)` (y equivalentes) para que también aporten profundidad en oscuro sin depender de un verde claro.
6. **Login (#9, baja, opcional).** Guardar el último tema aplicado en `localStorage` al resolverlo en `appearance-state.tsx` y leerlo en `session-gate.tsx` antes de autenticar para fijar `data-theme` provisional; la preferencia del servidor sigue mandando tras el login.

## 7. No verificado

- Firefox y WebKit (solo Chromium).
- Aspecto real de la barra de scroll, del selector de fecha nativo abierto y de la barra del navegador (`theme-color`): solo se comprobaron los valores computados.
- Estados `:hover` y `:active`; modo `forced-colors`; impresión.
- `/integraciones` (rama `codex/integration-api`, no está en `main`).
- Zoom nativo 200 % y texto 200 % en oscuro (ya cubiertos por otros specs en claro y oscuro para Apariencia; no repetidos aquí).

## 9. Confirmación medida sobre `main` — 9 de septiembre de 2026

Reejecución independiente contra `main` en `ac7be85`, pila real levantada con
`docker compose` en el puerto 18099, Chromium por Playwright a 1440×900 con
`colorScheme: dark` y el tema «oscuro» fijado por la pantalla de Apariencia
(`data-theme="dark"` verificado en las diez rutas). Escáner propio: recorre
todo el DOM visible y calcula el contraste WCAG sobre el fondo **efectivo
compuesto**, apilando capas translúcidas hasta la primera opaca.

Rutas: `/`, `/proyectos`, `/proyectos/nuevo`, `/disponibilidad`,
`/apariencia`, `/historial`, `/revision-semanal`, `/exportacion`,
`/importacion`, `/integraciones/api`.

**73 hallazgos brutos, 14 distintos.** Confirma punto por punto la sección 2:
el único fallo real de producto sigue siendo «Hoy», y se le suma la etiqueta
`theme-color`.

| # | Dónde | Medida | Origen |
| - | ----- | ------ | ------ |
| D1 | `dl.today-summary` | fondo `#ffffff` fijo dentro de página oscura | `frontend/src/today.scss:32` |
| D2 | `dt` y `dd` del resumen | contraste **1,18** (mínimo 4,5): `rgb(232,238,233)` sobre `rgb(255,255,255)` | consecuencia de D1 |
| D3 | `p.today-notice` | fondo `#f0f3eb` fijo; su texto queda en **1,05** | `frontend/src/today.scss:23` |
| D4 | enlace «Configurar disponibilidad» | contraste **1,25** sobre ese `#f0f3eb` | consecuencia de D3 |
| D5 | `<meta name="theme-color">` | vale `#f8f9f5` (lienzo **claro**) en las diez rutas con tema oscuro | `frontend/index.html` |

Colores claros fijos que quedan en `today.scss` de `main`: `#b8c8b8` (21),
`#f0f3eb` (23), `#e0e5dc` (30 y 48) y `#fff` (32 y 47).

Falsos positivos de esta heurística, que no deben tocarse: los elementos con
fondo `rgb(183,228,199)` son `--accent` del tema oscuro con `--on-accent:#000`
(`button`, `a.skip-link`, `span.brand-mark`, `span.nav-dot`, `a.primary-link`);
el `section` «Vista previa clara» de `/apariencia` es claro a propósito; y los
bordes con ratio 1 en botones de acento comparten color con su propio fondo.

**Lo importante: los arreglos existen en `claude/darkmode` y no están en
`main`.** Por eso el usuario sigue viendo el fallo. Esa rama salió de un punto
anterior a la feature 24 y su diferencia contra `main` borra todo el frontend
de la API para integraciones, así que debe integrarse por rebase o cherry-pick,
nunca por merge directo.

Capturas de las diez rutas y `darkmode-report.json` con las 73 entradas, en el
scratchpad de la sesión (`dm-out/`). No se copian al repositorio.

## 10. Verificación posterior al arreglo — 9 de septiembre de 2026

Mismo método de la sección 9, misma pila y mismo escáner, sobre `main` en
`aa6ec86` (ya con los ocho commits del carril del modo oscuro integrados).

**Fallos de contraste: 0** (antes 12). `theme-color` pasa de `#f8f9f5` a
`#111827`, que es el `--canvas` del tema oscuro, en las diez rutas. El total
baja de 73 a 59 hallazgos brutos, y los 59 que quedan son exactamente la lista
de falsos positivos declarada en la sección 9: los elementos con fondo
`rgb(183,228,199)` (`--accent`, con `--on-accent: #000`) y el `section` «Vista
previa clara» de `/apariencia`, que es claro a propósito.

D1, D2, D3, D4 y D5 quedan cerrados. La puerta del `judge` es aparte y se
tramita en `progress/judge_darkmode.md`.
