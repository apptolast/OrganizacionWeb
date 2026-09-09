# Corrección del modo oscuro — bitácora TDD

Rama `claude/darkmode` (worktree `C:/Users/vhurt/ow-worktrees/darkmode`).
Contrato de partida: `progress/darkmode_audit.md`, sección 6 «Correcciones
priorizadas». Puertos E2E propios: 18096 (primera sesión), 18091 (cierre).

**Estado: verde y lista para integrarse en `main` por PR.**

## 1. Rebase sobre `main` (lo primero, y lo que faltaba)

Las correcciones existían en la rama pero **no en `main`**, así que el usuario
seguía viendo Hoy roto. La rama salía de `6be9761`, anterior a la integración
de la feature 24 (API para integraciones): un merge habría **borrado**
`integration-api.tsx`, `integration-api-client.ts`, sus tests y su
configuración de Stryker (más de 3.000 líneas ya aprobadas por el juez).

Por eso **cherry-pick, no merge**. `main` estaba en `ac7be85` (confirmado
contra `origin/main` tras `git fetch`). Etiqueta de seguridad del estado
previo: `darkmode-backup` (apunta al antiguo `f5a16cd`).

| Original | Nuevo | Commit |
|---|---|---|
| `02d4bd8` | `0dc5a2a` | fix(today): pintar Hoy con tokens del tema |
| `9c998a1` | `7795e11` | fix(appearance): theme-color sigue al lienzo |
| `c4c2aa5` | `66f9959` | fix(history): filtros con tokens editables |
| `cb0ea07` | `b8ec804` | fix(appearance): separador con `--line` |
| `a5c78bd` | `e1e25c7` | fix(appearance): arte decorativo y sombras |
| `407e8b6` | `76edd6c` | docs: bitácora TDD (parcial) |

**Descartados a propósito:**

- `ec0a17e` + `f5a16cd`: el segundo revierte al primero, se anulan. No se
  arrastran. La corrección 6 de la auditoría (el login recuerda el último tema)
  queda **fuera de esta rama**; era opcional y de severidad baja.
- `6fb370f` (versionar `progress/darkmode_audit.md`): `main` ya trae ese
  archivo con contenido **idéntico** (`git diff` vacío). El cherry-pick habría
  quedado vacío.

**Conflictos:** ninguno manual. Dos auto-merges limpios en `styles.scss`
(`b8ec804` y `e1e25c7`), porque la feature 24 no toca ese archivo.

**Segundo reasentamiento.** Durante la sesión `main` avanzó dos commits
(`803dfe2` arnés y `ab26f53` docs, que tocan `.claude/settings.json`,
`.gitignore`, `progress/current.md` y `progress/darkmode_audit.md`). La rama se
volvió a rebasar sobre `ab26f53` sin un solo conflicto: no comparte ningún
archivo con esos dos commits. Hashes finales `17cc59a`…`df3d2af`. Después del
rebase la suite filtrada se reejecutó (122 casos verdes) y las comprobaciones
de no destrucción se repitieron sobre la nueva base.

**Verificación de que no se destruyó nada** (la comprobación que exigía el
riesgo, hecha después del rebase):

- `git diff --diff-filter=D --name-only main claude/darkmode` → **vacío**: la
  rama no borra ni un solo archivo respecto a `main`.
- `git diff --stat main claude/darkmode` → **9 archivos**, +396/−15. Nada de
  la feature 24 aparece.
- `frontend/src/integration-api.tsx`, `integration-api-client.ts`,
  `integration-api-intent.ts` y sus seis tests siguen en el árbol.
- El árbol `e2e/` de la rama es el de `main` **más** `today-dark.spec.mjs`;
  ningún spec desaparece.

## 2. Ciclos Rojo-Verde-Refactor

| # | Hallazgo | Test rojo | Cambio | Commit |
|---|---|---|---|---|
| 1 | D1–D4 (#1–#4): paneles de Hoy con blancos fijos, texto a 1,04–1,41:1 | `theme-tokens.test.ts` (sin `#hex` en `today.scss`) + `e2e/today-dark.spec.mjs` (axe `color-contrast`) | `today.scss` pasa a `--panel`, `--line`, `--selection` | `0dc5a2a` |
| 2 | D5 (#5): `theme-color` claro con la app oscura | `theme-color.test.tsx` (metas con `media` y repintado con `--canvas`) | dos metas en `index.html` + `paintThemeColor()` en `appearance-state.tsx` | `7795e11` |
| 3 | #6: filtros de Historial con grises nativos | `theme-tokens.test.ts` (tokens editables en `history.scss`) | `history.scss` acota `--editable/--ink/--control-border` a `.history` | `66f9959` |
| 4 | #7: `.empty-divider` con verde claro fijo | `theme-tokens.test.ts` (bloque `.empty-divider`) | `background: var(--line)` | `b8ec804` |
| 5 | #8: arte decorativo con verdes fijos y sombras verdosas | 6 casos nuevos en `theme-tokens.test.ts` | `--seed-stem/-leaf/-leaf-alt/-soil` en los dos mixins; sombras `rgb(0 0 0 / x%)` | `e1e25c7` |
| 6 | Regla de fondo: ninguna hoja SCSS puede pintar con un literal | 2 casos nuevos en `theme-tokens.test.ts` | — (guarda de regresión; el código ya cumplía) | `41f161d` |

### Ciclo 6 en detalle (el de esta sesión)

La regla exigida es más amplia que lo que vigilaban los tests: solo cubrían
`today.scss` entero y **regiones concretas** de `styles.scss`. Una hoja nueva
podía repetir exactamente el blanco fijo que dejó Hoy ilegible.

**ROJO.** Escrita la guarda `no stylesheet declares a fixed color outside the
appearance mixins`: recorre todas las `.scss` de `frontend/src`, quita los dos
mixins de apariencia (el único sitio donde un color puede ser literal, porque
allí *es* la definición del token) y exige `var(--token)` en cualquier
declaración de color. Para verla fallar se reinyectó el `today.scss` de `main`
(el estado que el usuario ve roto). Salida del rojo, exacta:

```
today.scss -> border: 1px solid #b8c8b8
today.scss -> background: #f0f3eb
today.scss -> border: 1px solid #e0e5dc
today.scss -> background: #fff
today.scss -> background: #fff
today.scss -> border: 1px solid #e0e5dc
```

El rojo destapó además un defecto **de la propia guarda**: las tres sombras
`rgb(0 0 0 / x%)` salían marcadas porque la excepción de sombra neutra se
comprobaba contra `"box-shadow: …"` en vez de contra el valor. Corregido antes
del verde; tras corregirlo el rojo señala los seis literales históricos y nada
más.

**VERDE.** Restaurado `today.scss` a su versión con tokens: 15 casos verdes en
`theme-tokens.test.ts` + `theme-color.test.tsx`.

**REFACTOR.** El predicado se extrajo a `literalColorsOutsideTokens(source)`,
compartido por la guarda global y por un caso que le fija los dientes sobre el
defecto real (`.today-summary { background: #fff }`); sin ese segundo caso, un
mutante que vaciara el barrido quedaría verde. Sin números mágicos:
`APPEARANCE_MIXINS`, `COLOR_DECLARATION`, `LITERAL_COLOR`, `NEUTRAL_SHADOW`.

## 3. Trazabilidad escenario → test

| Origen | Test |
|---|---|
| D1 `.today-summary` con `#ffffff` | `theme-tokens.test.ts`: «today.scss paints notice, summary and agenda cards only with theme tokens» + «no stylesheet declares a fixed color…»; `e2e/today-dark.spec.mjs` (axe, estado agenda) |
| D2 `dt`/`dd` a 1,18 | `e2e/today-dark.spec.mjs` (axe `color-contrast`, estado agenda) @s34 |
| D3 `.today-notice` con `#f0f3eb` | `theme-tokens.test.ts` (mismos dos casos); `e2e/today-dark.spec.mjs` (axe, estado notice) @s34 |
| D4 enlace «Configurar disponibilidad» a 1,25 | `e2e/today-dark.spec.mjs` (axe, estado notice) @s34 |
| D5 `theme-color` claro en tema oscuro | `theme-color.test.tsx`: los 4 casos (metas con `media`, repintado en DARK, en LIGHT y al salir de la sesión) @s32 |
| Anillo de foco invisible (#3) | `e2e/today-dark.spec.mjs`: contraste del `outline` contra el fondo real de la tarjeta ≥ 3 @s34 |
| Estado de error de Hoy (#4) | `e2e/today-dark.spec.mjs` (axe, estado error, `/api/v1/today` a 503) @s34 |
| #6, #7, #8 | `theme-tokens.test.ts`: casos de `history.scss`, `.empty-divider`, seed art y sombras |

## 4. Medida después del rebase (D1–D5)

Contrastes WCAG calculados sobre los tokens reales del mixin
`dark-appearance` de esta rama (`--panel #1f2937`, `--selection #28394a`,
`--ink #e8eee9`, `--accent #b7e4c7`, `--canvas #111827`):

| # | Qué | Antes | Ahora | Mínimo |
|---|---|---|---|---|
| D1 | fondo de `dl.today-summary` | `#ffffff` fijo | `var(--panel)` → `#1f2937` | — |
| D2 | `dt`/`dd` (`--ink` sobre `--panel`) | **1,18** | **12,47** | 4,5 |
| D3 | fondo de `p.today-notice` | `#f0f3eb` fijo | `var(--selection)` → `#28394a` | — |
| D3 | texto del aviso y del error | **1,05** | **10,05** | 4,5 |
| D4 | enlace «Configurar disponibilidad» | **1,25** | **8,42** | 4,5 |
| — | título de la agenda / anillo de foco (acento sobre panel) | **1,41** | **10,44** | 3 |
| D5 | `<meta name="theme-color">` | `#f8f9f5` con tema oscuro | sigue a `--canvas` → `#111827` | — |

**Los cinco resueltos.** Ningún literal de color sobrevive en las cuatro hojas
SCSS fuera de los mixins: solo las tres sombras de negro puro, neutras en los
dos temas.

## 5. Verificación ejecutada

Todo filtrado, respetando la disciplina de recursos (nunca la suite completa,
nunca mutación).

- `pnpm vitest run src/theme-tokens.test.ts src/theme-color.test.tsx` → **15
  pasados**.
- `pnpm vitest run src/appearance.test.tsx src/today.test.tsx
  src/appearance-api.test.ts` → **147 pasados** (nada se rompió al reasentar
  `appearance-state.tsx` sobre la base con la feature 24).
- `npx tsc --noEmit` → sin errores.
- `npx prettier --check` y `npx eslint` sobre el archivo tocado → limpios.
- `E2E_WEB_PORT=18091 pnpm test:e2e e2e/today-dark.spec.mjs` → **2 pasados**
  (9,5 s): las dos variantes de oscuro (preferencia `DARK` con SO claro, y
  `SYSTEM` con SO oscuro) dan **cero violaciones axe `color-contrast`** en los
  tres estados de Hoy (aviso, agenda, error 503) y el anillo de foco es
  visible. Pila propia, bajada al terminar (`docker ps` sin contenedores
  `organizationweb-e2e-55908-*`).

## 6. Riesgos y límites

- **Riesgo de integración: bajo.** La rama es `main` (`ab26f53`) + 8 commits,
  9 archivos, cero borrados y cero commits por detrás de `main`. No toca backend, ni `feature_list.json`, ni nada de la
  feature 24. Los únicos archivos compartidos con otros carriles son
  `frontend/src/styles.scss` (+26/−9, todo dentro de los mixins, `.seed-art`,
  `.empty-divider` y tres `box-shadow`) y `frontend/index.html` (dos metas).
- **No verificado aquí:** Firefox y WebKit; el aspecto real de la barra del
  navegador con `theme-color` (solo el valor en el DOM); `:hover`/`:active`;
  `forced-colors`; impresión.
- **Fuera de alcance por decisión:** corrección 6 de la auditoría (el login
  recuerda el último tema en `localStorage`, severidad baja). Se intentó y se
  revirtió en la rama anterior; no se arrastra.
- **No marcado `done`.** Falta el `judge` y el `mutation_tester`; esa puerta la
  abre el `craftsman_lead`.
- **Sin versionar:** `e2e/darkmode-visual-check.spec.mjs` sigue en el worktree
  como archivo sin seguimiento. Es la pasada visual temporal de la sesión
  anterior; **no debe entrar en el PR**. La guarda permanente es
  `e2e/today-dark.spec.mjs` más `theme-tokens.test.ts`.

## 7. Cierre: las tres correcciones exigidas por el juez

Dictamen `progress/judge_darkmode.md` (**APPROVED** con tres correcciones
obligatorias). Los ocho commits ya estaban en `main`, así que estas van encima
de `main` (`ac9e9a5`), no sobre la rama vieja.

### 7.1 Etiquetas `@s` falsas (exigida 1)

`e2e/today-dark.spec.mjs:61` se titulaba `@s2 @s21 @s23`. En
`features/appearance.feature`, @s21 es «Editar sólo cambia muestra y borrador»
y @s23 «Guardar confirma junto el formulario y la apariencia global»: el spec
**no abre el formulario ni pulsa Guardar**. Lo que sí verifica es **@s34**
(«Variantes conservan legibilidad… los contrastes definidos sobre los fondos
reales», filas `DARK`).

Se deja **`@s34` a secas**, no `@s2 @s34`. El juez permitía reconocer el PUT de
preparación como @s2, pero `preferTheme()` solo comprueba que el PUT devuelve
200: no verifica los colores canónicos, ni `updatedAt`, ni el UUID y la versión
del ETag, ni que quede exactamente una fila. Reclamar @s2 sería exactamente el
mismo tipo de cobertura falsa que la corrección venía a quitar. La tabla de
trazabilidad (sección 3) queda alineada: las seis filas que apuntan a ese spec
dicen @s34.

Sin ciclo rojo: es una etiqueta, no comportamiento. Commit `0aa8c33`.

### 7.2 Sombras: el ancla `$` dejaba pasar una capa clara (exigida 2, la grave)

**ROJO.** Con el predicado anterior, la guarda global devolvía **`[]`** para el
caso exacto del juez:

```
box-shadow: 0 0 8px #ffffff, 0 5px 18px rgb(0 0 0 / 6%)
```

`NEUTRAL_SHADOW = /rgb\(0 0 0 \/ \d+%\)$/` eximía la **declaración entera** si
*terminaba* en una capa neutra, así que una capa blanca colada delante
atravesaba las dos guardas. Tres casos nuevos lo fijan: el barrido sobre la
declaración completa y `isNeutralShadow` con la capa clara delante y detrás.

**VERDE.** `isNeutralShadow(value)` parte el valor en capas por las comas que
no están dentro de paréntesis (`SHADOW_LAYER_SEPARATOR`) y exige que **ninguna
capa** conserve un literal tras quitarle el negro puro. Además la exención solo
se aplica ya a propiedades de sombra (`SHADOW_PROPERTY`), no a cualquier valor
que acabe en negro translúcido. El test de sombras de `styles.scss` pasa a usar
el mismo ayudante, así que las dos guardas se cierran de una vez.

Commit `d5a496e`. 13 casos verdes.

### 7.3 El barrido no entraba en subcarpetas (exigida 3)

**ROJO.** `styleSheets()` usaba `readdirSync` sin `withFileTypes` y sin
recursión. Un test sobre un directorio temporal con `top.scss`,
`partials/mid.scss` y `partials/deep/low.scss` recibía las cuatro hojas de la
raíz de `src` en vez de las tres anidadas: la función ni siquiera miraba la
raíz que se le pedía.

**VERDE.** Recorrido recursivo con `withFileTypes`, devolviendo rutas relativas
a la raíz con barras normales (para que el mensaje de fallo sea legible en
Windows). El `.md` del directorio de prueba se ignora.

**Comprobación de punta a punta**, más fuerte que el test unitario: se creó
`frontend/src/probe/nested-probe.scss` con los dos defectos a la vez y la
guarda global lo señaló por ambos:

```
probe/nested-probe.scss -> background: #fff
probe/nested-probe.scss -> box-shadow: 0 0 8px #ffffff, 0 5px 18px rgb(0 0 0 / 6%)
```

La sonda se retiró después; `frontend/src` vuelve a tener sus cuatro hojas.
Commit `1595895`. 14 casos verdes.

### 7.4 Verificación del cierre

- `pnpm vitest run src/theme-tokens.test.ts` → **14 pasados** (11 antes de las
  correcciones).
- `npx tsc --noEmit`, `npx prettier --check`, `npx eslint` sobre los ficheros
  tocados → limpios.
- E2E no reejecutado: el único cambio en `today-dark.spec.mjs` es el texto del
  título del test, sin efecto sobre lo que ejercita. Cinco carriles vivos en la
  máquina; no se levantó pila.

### 7.5 Recomendaciones del juez que **no** se aplican aquí

Deliberadamente fuera de alcance, para que el conjunto revisado sea exactamente
el exigido. Quedan anotadas para el `craftsman_lead`:

- `today-dark.spec.mjs:43`: `[a, b].sort()` sin comparador (ordena como
  cadenas). El propio juez constata que, si fallara, fallaría en dirección
  segura (ratio < 1 → rojo, nunca verde falso). Cambiarlo obliga a reejecutar
  el E2E para no cambiar el oráculo a ciegas.
- `today-dark.spec.mjs:49`: `withTags(["wcag2aa"])` muerto, sobrescrito por
  `withRules`.
- `theme-tokens.test.ts`: derivar `DARK_PANEL` del mixin con `tokenValue()`.
- Ampliar `COLOR_DECLARATION` con `background-image` y con las custom
  properties declaradas fuera de los mixins.
- Medir el tema **claro** con una pasada visual (aquí solo hay cálculo).
