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
| D2 `dt`/`dd` a 1,18 | `e2e/today-dark.spec.mjs` (axe `color-contrast`, estado agenda) @s2 |
| D3 `.today-notice` con `#f0f3eb` | `theme-tokens.test.ts` (mismos dos casos); `e2e/today-dark.spec.mjs` (axe, estado notice) @s21 |
| D4 enlace «Configurar disponibilidad» a 1,25 | `e2e/today-dark.spec.mjs` (axe, estado notice) |
| D5 `theme-color` claro en tema oscuro | `theme-color.test.tsx`: los 4 casos (metas con `media`, repintado en DARK, en LIGHT y al salir de la sesión) @s32 |
| Anillo de foco invisible (#3) | `e2e/today-dark.spec.mjs`: contraste del `outline` contra el fondo real de la tarjeta ≥ 3 |
| Estado de error de Hoy (#4) | `e2e/today-dark.spec.mjs` (axe, estado error, `/api/v1/today` a 503) @s23 |
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

- **Riesgo de integración: bajo.** La rama es `main` + 7 commits, 9 archivos,
  cero borrados. No toca backend, ni `feature_list.json`, ni nada de la
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
