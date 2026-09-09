# APPROVED — corrección del modo oscuro (`ab26f53..681016b`, 8 commits)

Revisión independiente del carril `claude/darkmode` ya integrado en `main`.
Contrato: `features/appearance.feature` (feature 20, `done`) y
`progress/darkmode_audit.md`, sección 9 (auditoría medida) y sección 6
(correcciones priorizadas). Relato juzgado: `progress/tdd_darkmode.md`.

Superficie: 9 ficheros, +594/−15, **cero borrados**.

## 0. Qué ejecuté yo y qué cito del artesano

Ejecutado por mí (disciplina de recursos respetada: nada de suite completa,
mutación ni E2E):

- `pnpm vitest run src/theme-tokens.test.ts src/theme-color.test.tsx`
  → **15 pasados**, 2 ficheros.
- `pnpm vitest run src/appearance.test.tsx` → **50 pasados**.
- Reproducción del **rojo** de la guarda global contra el `today.scss` de
  `main` en `ab26f53`, ejecutando el predicado real del test (extraído del
  propio `theme-tokens.test.ts`, sin tocar el repositorio) desde el scratchpad.
- Recálculo propio de los contrastes WCAG a partir de los valores de los dos
  mixins de `styles.scss`, en los **dos** temas.
- Barrido estático de literales de color sobre las cuatro hojas `.scss`.
- Lectura de `@axe-core/playwright` para decidir si el E2E es un oráculo real.

**No verificado por mí** (y por tanto citado, no confirmado): la ejecución de
`e2e/today-dark.spec.mjs` y sus «0 violaciones axe»; `bin/harness init`, `tsc`,
`prettier`, `eslint`; el aspecto real de la barra del navegador; Firefox y
WebKit; `:hover`/`:active`; `forced-colors`; impresión.

## 1. ¿Están cerrados D1–D5? Sí, y las cifras del artesano se sostienen

Barrido estático: **no sobrevive ni un literal de color** en `today.scss`,
`history.scss`, `work-session.scss` ni en `styles.scss` fuera de los dos mixins
de apariencia. Los cuatro literales que la sección 9 listaba (`#b8c8b8` 21,
`#f0f3eb` 23, `#e0e5dc` 30 y 48, `#fff` 32 y 47) han desaparecido:
`today.scss:21,30,48` usan `var(--line)`, `:23` usa `var(--selection)` y
`:32,47` usan `var(--panel)`. Las únicas declaraciones de color literales que
quedan son tres `box-shadow` de negro puro (`styles.scss:658,1204,1494`),
neutras en los dos temas.

Contrastes recalculados por mí desde `@mixin dark-appearance`
(`--panel #1f2937`, `--selection #28394a`, `--ink #e8eee9`, `--accent #b7e4c7`):

| # | Medida de la sección 9 | Ahora (recálculo propio) | Mínimo | Coincide con la bitácora |
|---|---|---|---|---|
| D1 | `dl.today-summary` con `#ffffff` fijo | `var(--panel)` → `#1f2937` | — | sí |
| D2 | `dt`/`dd` **1,18** | **12,47** | 4,5 | sí (12,47) |
| D3 | `p.today-notice` fondo fijo, texto **1,05** | **10,05** | 4,5 | sí (10,05) |
| D4 | «Configurar disponibilidad» **1,25** | **8,42** | 4,5 | sí (8,42) |
| — | título de agenda / anillo de foco **1,41** | **10,44** | 3 | sí (10,44) |
| D5 | `theme-color` `#f8f9f5` en tema oscuro | dos metas con `media` + repintado desde `--canvas` | — | sí |

D5 comprobado también en el código: `frontend/index.html:6-15` declara las dos
metas y `appearance-state.tsx:44-51` repinta **todas** las metas
`theme-color` con el `--canvas` computado. Importante y correcto:
`paintThemeColor()` está dentro de `apply()` (`appearance-state.tsx:150`), que
se registra en el listener de `(prefers-color-scheme: dark)` y en
`visibilitychange` (`:157-158`), así que con `SYSTEM` un cambio de esquema del
sistema **sí** repinta la barra — que es justo el agujero que abriría igualar
el `content` de las dos metas. Se repinta también al desmontar (`:165`), y el
cuarto caso de `theme-color.test.tsx` lo fija (@s32).

**Las «0 violaciones axe» en las dos variantes no las he medido yo.** Lo que sí
puedo afirmar es que la causa física de las 18 violaciones de la sección 4
(fondos claros fijos) ya no existe en el código.

## 2. Falsos positivos y tema claro: intactos, sin regresión medida

Los tres falsos positivos que la sección 9 declaró intocables siguen intactos:

- `--accent` oscuro sigue siendo `#b7e4c7` (`styles.scss:39`) con
  `--on-accent: #000000` (`:40`). El único cambio dentro de los mixins es la
  adición de los cuatro `--seed-*`.
- `section` «Vista previa clara» de `/apariencia`: `appearance.tsx` **no está
  en el diff**. Sigue en `appearance.tsx:293-307` con `data-theme="light"`, que
  reaplica `@include light-appearance` (`styles.scss:50-52`). Es claro a
  propósito y lo sigue siendo.
- Bordes de los botones de acento: `styles.scss:781` conserva
  `border: 1px solid $forest` con `$forest: var(--accent)` (`:61`). Comparten
  color con su propio fondo, como decía la auditoría.

Tema claro (nadie lo había vuelto a medir; lo he medido yo sobre los tokens):

| Superficie cambiada | Antes | Ahora | Mínimo |
|---|---|---|---|
| texto del aviso de Hoy | 11,03 sobre `#f0f3eb` | **9,83** sobre `--selection #dfe8d9` | 4,5 |
| enlace «Configurar disponibilidad» | 8,61 | **7,67** | 4,5 |
| `dl.today-summary` y tarjetas de agenda | `#fff` | `--panel` = `#ffffff`, **idéntico** | — |
| bordes de Hoy | `#e0e5dc` (1,28 sobre panel) | `--line #d2dace` (1,43) | decorativo |
| `.empty-divider` | `#cad9bb` (1,48) | `--line` (1,43) | decorativo |
| arte de la semilla en claro | `#7a9863/#a8bb88/#7d9b61/#b3c59e` | **los mismos**, fijados por test | — |
| sombras | `#2b3f2410 / 07 / 08` | negro puro al 6 % / 3 % / 3 % | — |

**Sin regresión de contraste en claro**: todo lo que era texto sigue muy por
encima de AA; los dos ratios que bajan (11,03→9,83 y 8,61→7,67) siguen en AAA.
Los cambios visibles en claro se reducen a un aviso de Hoy algo más verde, dos
bordes y una línea decorativa un pelo más claros, y sombras de negro puro en
vez de negro verdoso — **exactamente** lo que prescriben las correcciones 1, 4
y 5 de la sección 6. Los verdes históricos del arte están blindados por
`theme-tokens.test.ts:96-100`, que es la guarda correcta contra el riesgo real
(«la deuda de tokens no puede rediseñar el tema claro»).

Salvedad honesta: **es contraste calculado, no inspección visual**. Nadie ha
vuelto a mirar el tema claro con ojos; yo tampoco.

## 3. La guarda nueva: muerde donde importa, y tiene agujeros conocidos

Reproduje el **rojo** ejecutando `literalColorsOutsideTokens` (el predicado
real del test) contra `git show ab26f53:frontend/src/today.scss`. Salida:

```
border: 1px solid #b8c8b8 / background: #f0f3eb / border: 1px solid #e0e5dc
background: #fff / background: #fff / border: 1px solid #e0e5dc
```

Seis literales, los mismos y en el mismo orden que documenta la bitácora
(`tdd_darkmode.md:86-93`). **El relato del rojo es cierto.**

- ¿Mordería si alguien reintrodujese `background: #fff` en `today.scss`? **Sí,
  por dos vías independientes**: `theme-tokens.test.ts:17-19` (ningún `#hex` en
  `today.scss`) y la guarda global `:151-162`.
- ¿En un fichero `.scss` nuevo? **Sí, si vive en `frontend/src/`** —
  `styleSheets()` (`:12-15`) lee el directorio en tiempo de ejecución. **No si
  vive en un subdirectorio**: `readdirSync` no es recursivo y no usa
  `withFileTypes`. Hoy no hay ninguna hoja en subcarpeta, así que no es un
  hueco activo; sí es un hueco futuro.
- La corrección de la excepción de sombra (comparar el **valor** con
  `NEUTRAL_SHADOW` en vez del texto completo de la declaración) es correcta
  para lo que buscaba: las tres sombras legítimas dejan de marcarse y los seis
  literales históricos siguen marcándose. **Pero sí abre un hueco por el otro
  lado**, y lo he verificado ejecutándolo: como el regex está anclado con `$`,
  exime la declaración entera si *termina* en una sombra neutra, de modo que
  `box-shadow: 0 0 8px #ffffff, 0 5px 18px rgb(0 0 0 / 6%)` **se cuela**. El
  test de sombras (`:164-170`) tiene el mismo ancla, así que tampoco lo caza, y
  además sólo mira `styles.scss`.

Otros huecos que probé y que se cuelan (ninguno explotado hoy):
`background-image: linear-gradient(#fff,#000)` (la propiedad no está en
`COLOR_DECLARATION`); `--brand-alt: #ffffff` declarado fuera de los mixins (las
custom properties no son propiedades de color reconocidas); nombres de color
fuera de la lista parcial de `LITERAL_COLOR` (`cornsilk`, `navy`…). Sí cazan
correctamente: `background: white`, `border-color: #e0e5dc` y hasta el
*fallback* `var(--panel, #fff)`.

Veredicto sobre la guarda: **vale**, y vale bastante — cubre exactamente la
clase de defecto que dejó Hoy ilegible y está anclada por un caso que le fija
los dientes (`:145-149`), que es lo que impide que un mutante vacíe el barrido.
Su nombre («no stylesheet declares a fixed color») promete más de lo que
cumple: escanea `frontend/src/*.scss` no recursivo, una lista cerrada de
propiedades y una lista parcial de nombres de color.

## 4. `e2e/today-dark.spec.mjs`: oráculo real, no una prueba que pasa siempre

No lo he ejecutado (E2E prohibido en esta sesión). Lo he juzgado leyéndolo y
leyendo la librería:

- **No es vacío por configuración.** `withTags(["wcag2aa"]).withRules(["color-contrast"])`
  parecía sospechoso, pero en `@axe-core/playwright/dist/index.js:180-202`
  ambos escriben el mismo campo `option.runOnly` y `withRules` va después:
  queda `{type:"rule", values:["color-contrast"]}`. **La regla se ejecuta.**
  El `withTags` previo es código muerto y engañoso (la propia librería
  documenta que no se pueden combinar), pero no anula la comprobación.
- **Los tres estados se comprueban presentes antes de medir**: el aviso
  (`:70-72`), el enlace de la tarea sembrada (`:77-78`) y el `role="alert"` con
  `/api/v1/today` interceptado a 503 (`:94-104`). Si el estado no aparece, el
  test falla antes de llamar a axe; no hay pasada silenciosa sobre una página
  vacía.
- El anillo de foco no se da por bueno por existir: mide `outlineWidth > 0` y
  el contraste del `outlineColor` contra el `backgroundColor` **real** del `li`
  vía `element.closest("li")` (`:81-92`). Es la comprobación correcta del
  hallazgo #3 y no la cubre axe.
- Se ejecuta en las dos variantes de oscuro, `DARK` con SO claro y `SYSTEM` con
  SO oscuro (`:57-60`), que era el punto de la sección 1 de la auditoría.
- Está dentro de `testDir: "./e2e"` (`playwright.config.ts`), así que entra en
  la pasada normal; los ayudantes que usa (`seedAgenda`, `sql`, `csrfHeaders`,
  `authenticated-test.mjs`) existen y son preexistentes.

Límite del oráculo, dicho sin adornos: lo que axe no puede calcular acaba en
`incomplete`, no en `violations`, y este spec sólo mira `violations`. Es la
limitación estándar de axe, y la auditoría demostró que con el código roto la
regla sí producía violaciones (18 nodos), así que la regresión se cazaría.

## 5. Feature 24: no se ha destruido nada

`git diff --name-status ab26f53..681016b` no contiene **ni una sola entrada
`D`**: 5 modificados, 4 añadidos. Comprobado además en el árbol:
`integration-api.tsx`, `integration-api-client.ts`, `integration-api-intent.ts`
y sus cinco ficheros de test (`.test` y `.mutation.test`) siguen presentes, y
`frontend/stryker.integration-api.config.json` también. En `e2e/` la única
entrada es `A e2e/today-dark.spec.mjs`. Ningún test existente desaparece.
El árbol de trabajo está limpio (`git status --porcelain` vacío): el
`darkmode-visual-check.spec.mjs` temporal no entró.

## 6. Disciplina TDD y calidad

- **¿Producción que ningún test exige?** Prácticamente no. Cada cambio de
  producción viaja con el test que lo pide en el mismo commit (17cc59a,
  868a85e, 07ec49d, 08f290e, a368397) y 52c24c3 es sólo test. Única excepción
  menor: `history.scss:17-19` añade `padding: 8px 12px` y `border-radius: 8px`,
  que ningún test exige (el test sólo fija las tres declaraciones de color).
  Está avalado por la corrección 3 de la auditoría («border-radius coherente»),
  pero son dos números mágicos sin oráculo.
- **¿Rojo→Verde→Refactor?** Los ciclos vienen colapsados por el cherry-pick
  (test y producción en el mismo commit), lo cual impide leer el rojo en la
  historia. Compensado: la bitácora transcribe el rojo y **yo lo he
  reproducido**, literal por literal.
- Nombres y estructura: bien. `APPEARANCE_MIXINS`, `COLOR_DECLARATION`,
  `LITERAL_COLOR`, `NEUTRAL_SHADOW`, `VISIBLE_ON_PANEL`, `NOT_GLARING_ON_PANEL`
  son reveladores; `literalColorsOutsideTokens` está bien extraído y
  compartido. `paintThemeColor()` es corta, con una sola razón de cambio y con
  guarda (`if (!canvas) return`).
- Duplicación: `channel`/`luminance`/`contrast` están copiados en
  `theme-tokens.test.ts:65-81` y en `today-dark.spec.mjs:31-45`. En la copia
  del E2E, `contrast()` usa `[a, b].sort()` **sin comparador**: ordena como
  cadenas. Con luminancias normales acierta, y si fallara lo haría en dirección
  segura (ratio < 1 → test rojo, nunca verde falso). Aun así es un defecto: la
  otra copia sí lleva `(a, b) => a - b`.
- Constante duplicada: `DARK_PANEL = "#1f2937"` (`theme-tokens.test.ts:54`) se
  escribe a mano en vez de leerse del mixin con el `tokenValue()` que el propio
  fichero ya tiene. Si alguien cambia `--panel`, el test mide contra un panel
  que ya no existe.
- **Trazabilidad mal etiquetada.** `today-dark.spec.mjs:61` se titula
  `@s2 @s21 @s23`. En `features/appearance.feature`, @s21 es «Editar sólo
  cambia muestra y borrador» y @s23 «Guardar confirma junto el formulario y la
  apariencia global»: **el spec no ejercita ninguno de los dos** (no abre el
  formulario ni pulsa Guardar). Lo que sí verifica es **@s34** («Variantes
  conservan legibilidad… los contrastes definidos sobre los fondos reales»,
  filas `DARK`), y @s34 **no** aparece en el título. Es una etiqueta que
  reclama cobertura que no da, en un repositorio donde esas etiquetas *son* el
  mapa escenario→test (ver `e2e/appearance-ux-audit.spec.mjs:454`). No deja
  ningún `@s` descubierto —@s21, @s23 y @s34 los cubre ese otro spec—, pero
  pudre el mapa. Lo mismo en `tdd_darkmode.md:115-120`.
- Arquitectura: todo el cambio queda en la capa de presentación; no toca
  backend, dominio, `feature_list.json` ni contratos HTTP. Correcto.

## Cobertura (auditoría → test) y escenarios

- D1 `.today-summary` `#ffffff` → `theme-tokens.test.ts:17` y `:151`; E2E agenda. **[x]**
- D2 `dt`/`dd` 1,18 → E2E agenda (axe) + recálculo 12,47. **[x]**
- D3 `.today-notice` `#f0f3eb` → mismos dos tests estáticos; E2E aviso. **[x]**
- D4 enlace 1,25 → E2E aviso (axe) + recálculo 8,42. **[x]**
- D5 `theme-color` → `theme-color.test.tsx`, 4 casos, ejecutados verdes por mí. **[x]**
- #3 anillo de foco → E2E, contraste del `outline` contra el fondo real. **[x]**
- #4 estado de error de Hoy → E2E con `/api/v1/today` a 503. **[x]**
- #6 filtros de Historial → `theme-tokens.test.ts:21-29`. **[x]**
- #7 `.empty-divider` → `:34-38`. **[x]**
- #8 arte y sombras → `:83-124` y `:164-170`. **[x]**
- Escenarios del contrato: `@s34` (filas DARK) reforzado; `@s24` y `@s32`
  intactos y cubiertos (`theme-color.test.tsx:75-85` y los listeners de
  `apply()`). Ningún `@s` de `appearance.feature` queda sin test.

## Checkpoints

- C1 arnés completo: **[ ] no evaluado** — `bin/harness init` **no se ejecutó**
  por la disciplina de recursos impuesta a esta revisión (cinco carriles vivos
  en la máquina). No está en rojo: está sin ejecutar. La puerta sigue abierta
  para el `craftsman_lead` o para CI.
- C2 estado coherente: **[x]** una sola feature `in_progress` (24); este carril
  es correctivo sobre la 20 (`done`) y no toca `feature_list.json`.
- C3 arquitectura: **[x]** sólo presentación; sin dependencias nuevas, sin
  TODOs sueltos ni logs de depuración.
- C4 verificación real: **[x]** en lo filtrado — 65 casos verdes ejecutados por
  mí (15 + 50); ningún mock del sistema de ficheros (los tests leen las hojas
  reales del disco).
- C5 sesión cerrada: **[x]** árbol limpio, bitácora escrita, el spec visual
  temporal no entró.
- C6 contrato Gherkin: **[x] con reserva** — cobertura completa, pero el
  etiquetado `@s` del E2E nuevo es incorrecto (corrección 1).
- C7 mutación: **[ ]** pendiente del `mutation_tester`; es una puerta distinta.

## Correcciones exigidas

No bloquean lo ya integrado en `main`; sí son obligatorias antes de dar el
carril por cerrado.

1. `e2e/today-dark.spec.mjs:61`: sustituir `@s2 @s21 @s23` por `@s34` (o
   `@s2 @s34` si se quiere reconocer el PUT de preparación). Alinear la tabla
   de trazabilidad de `progress/tdd_darkmode.md:115-120`. Una etiqueta que
   reclama @s21/@s23 sin ejercitarlos es cobertura falsa en el mapa.
2. `frontend/src/theme-tokens.test.ts:135` y `:169`: quitar el ancla `$` de
   `NEUTRAL_SHADOW` o, mejor, exigir que **cada capa** de la lista de
   `box-shadow` sea neutra. Hoy
   `box-shadow: 0 0 8px #ffffff, 0 5px 18px rgb(0 0 0 / 6%)` atraviesa las dos
   guardas; verificado ejecutando el predicado real.
3. `frontend/src/theme-tokens.test.ts:12-15`: hacer `styleSheets()` recursivo
   (o afirmar explícitamente que no existen `.scss` fuera de la raíz de `src`).
   Hoy una hoja en subcarpeta no se escanea y la guarda global no se enteraría.

## Recomendaciones (no exigidas)

- `today-dark.spec.mjs:43`: `[a, b].sort()` → `sort((x, y) => x - y)`, como ya
  hace `theme-tokens.test.ts:77`; y extraer la aritmética de contraste a un
  ayudante compartido en `e2e/support/` en vez de duplicarla.
- `today-dark.spec.mjs:49`: eliminar el `withTags(["wcag2aa"])` muerto, que
  `withRules` sobrescribe.
- `theme-tokens.test.ts:54`: derivar `DARK_PANEL` del mixin con `tokenValue()`
  en lugar de fijar `#1f2937` a mano.
- Ampliar `COLOR_DECLARATION` con `background-image` y con las custom
  properties declaradas fuera de los mixins; el nombre del test promete
  cubrirlas.
- Medir el tema **claro** una vez con la misma pasada visual de la auditoría:
  aquí sólo se ha demostrado por cálculo que no hay regresión de contraste.

## Fundamento del APPROVED

Los cinco defectos medidos están cerrados en el código, no sólo en la bitácora:
el rojo lo he reproducido yo, las cifras de contraste las he recalculado yo
desde los tokens y coinciden al céntimo, no queda ningún literal de color fuera
de los mixins, los tres falsos positivos siguen intactos, el tema claro no
pierde contraste en ninguna superficie tocada, el E2E es un oráculo real y no
se ha borrado nada de la feature 24. Los defectos que encuentro son de alcance
de la guarda y de etiquetado, no de producto, y ninguno deja un escenario sin
test ni deja producción sin un test que la pida.
