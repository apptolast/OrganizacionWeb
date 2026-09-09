# Mutación — corrección de modo oscuro (theme-color) — ámbito appearance-frontend

**Veredicto:** PASS
**Score:** killed/total = 696/778 = 89,4602 % (umbral: 80 %)

Sub-score del fichero que contiene el cambio, sin ocultarlo:
`frontend/src/appearance-state.tsx` = 121/153 = **79,08 %**, por debajo del 80 %.
El umbral de `harness.config.json` (`mutation.threshold: 0.8`) y el `break: 80`
de `frontend/stryker.appearance.config.json` son **globales de campaña**, y la
campaña los supera. Se deja constancia explícita de que el fichero, aislado,
queda 0,92 puntos por debajo: el veredicto PASS se apoya en el umbral tal como
está declarado, no en una reinterpretación favorable.
La superficie nueva del modo oscuro, aislada (líneas 43-53, 150 y 165), es
**8/9 = 88,89 %**.

## Ejecución

| Campo | Valor |
| --- | --- |
| Comando exacto | `node scripts/project.mjs mutate appearance-frontend` |
| Despacho real | `pnpm --dir frontend exec stryker run stryker.appearance.config.json` (`scripts/project.mjs:82-91`) |
| Checkout | `c0e22a35815c5c9fb1c304d275d7d17e46fee9e6`, rama `main`, árbol limpio antes y después |
| Ventana | inicio 2026-09-09 12:47:32, fin 13:04:58 (hora local). Stryker reporta 17 min 19 s de mutación |
| Código de salida | **0** |
| Stryker | `@stryker-mutator/core` y `vitest-runner` ^10.0.0; Node v22.15.0 |
| Concurrency | **8** (valor de la config). `scripts/project.mjs` **no** admite fijar workers: invoca Stryker con una lista de argumentos fija y no lee `--concurrency` ni variables de entorno. Bajar a 4 exigía editar `frontend/stryker.appearance.config.json`, es decir, alterar la configuración de la campaña, así que corrí con el valor por defecto. Queda anotado |
| Dry run | 1038 tests en verde, 2 min 28 s. `coverageAnalysis: perTest`, 20,97 tests por mutante de media |
| Configuración | `frontend/stryker.appearance.config.json`, SHA256 `15c45a328a21588821bd005b2adc4da49cb7d0dec4b5e9b2a37853e297df474c`. Sin exclusiones añadidas por mí |

Comprobación previa exigida: el bloque `mutate` de la configuración incluye
`src/appearance-state.tsx` (línea 8), el único fichero de producción del arreglo
que Stryker puede mutar. La campaña sí juzga el cambio.

## Estados — denominador completo, sin reclasificar

| Fuente | Total | Killed | Survived | NoCoverage | Timeout | RuntimeError | Score |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| appearance-api.ts | 262 | 245 | 17 | 0 | 0 | 0 | 93,51 % |
| appearance-state.tsx | 153 | 121 | 24 | 8 | 0 | 0 | 79,08 % |
| appearance.tsx | 213 | 182 | 31 | 0 | 0 | 0 | 85,45 % |
| App.tsx | 37 | 37 | 0 | 0 | 0 | 0 | 100 % |
| session-gate.tsx | 17 | 17 | 0 | 0 | 0 | 0 | 100 % |
| use-session.ts | 91 | 89 | 2 | 0 | 0 | 0 | 97,80 % |
| workspace.tsx | 5 | 5 | 0 | 0 | 0 | 0 | 100 % |
| **Total** | **778** | **696** | **74** | **8** | **0** | **0** | **89,46 %** |

Los 8 NoCoverage **se cuentan en el denominador** y no se restan. Ningún mutante
ha cambiado de estado ni se ha excluido.

## Comparación con la campaña histórica (`progress/mutation_appearance_frontend.md`)

Aquella midió 747 mutantes, 637 K / 102 S / 8 NC = 85,27 %. Ahora son 778 =
89,46 %. El denominador sube en **31** y no por un cambio de alcance: mismas 7
fuentes y mismos 9 selectores.

- `appearance-state.tsx`: 137 → 153 (+16). Nueve son la superficie del modo
  oscuro (`868a85e`); los otros siete vienen de `refreshAfterImport`, añadido
  por `3f4c3ef` (importar copias, #27) el 2026-09-08, posterior a aquella campaña.
- `use-session.ts`: 76 → 91 (+15), por el crecimiento del propio fichero en
  `0277c50` (#29).
- Las cinco fuentes restantes: total idéntico mutante a mutante.
- La configuración cambió de SHA256 (`5CA42254…` → `15c45a32…`), pero el diff
  contra `7c1bf80` sólo **reancla rangos de línea** de `App.tsx`,
  `workspace.tsx`, `session-gate.tsx` y `use-session.ts` tras crecer esos
  ficheros. No se añadió ni se quitó ninguna fuente.
- La subida de score (85,27 → 89,46) se explica por refuerzos de pruebas
  incorporados entre ambas fechas: supervivientes 102 → 74.

## Superficie del modo oscuro: los 9 mutantes, uno a uno

Fichero: `frontend/src/appearance-state.tsx`.

| ID | Estado | Línea:col | Mutador | Sustitución |
| --- | --- | --- | --- | --- |
| 321 | Killed | 44:28 | BlockStatement | cuerpo de `paintThemeColor` → `{}` |
| 322 | **Survived** | 45:18–47:12 | MethodExpression | se elimina la llamada a `.trim()` |
| 323 | Killed | 46:23 | StringLiteral | `"--canvas"` → `""` |
| 324 | Killed | 48:7 | BooleanLiteral | `!canvas` → `canvas` |
| 325 | Killed | 48:7 | ConditionalExpression | guarda → `true` |
| 326 | Killed | 48:7 | ConditionalExpression | guarda → `false` |
| 327 | Killed | 50:5 | StringLiteral | selector `meta[name=theme-color]` → `""` |
| 438 | Killed | 150:7 | CallExpression | `paintThemeColor();` al aplicar el tema → `;` |
| 459 | Killed | 165:7 | CallExpression | `paintThemeColor();` en la limpieza → `;` |

Las dos llamadas que constituyen el arreglo (aplicación y desmontaje) mueren, y
mueren también el selector de la meta, el nombre de la variable y ambas ramas de
la guarda. `theme-color.test.tsx` muerde donde tiene que morder.

### Único superviviente del cambio: mutante 322

`frontend/src/appearance-state.tsx:45-47`, mutador **MethodExpression**:

    getComputedStyle(document.documentElement).getPropertyValue("--canvas").trim()
      →  getComputedStyle(document.documentElement).getPropertyValue("--canvas")

Cubierto por 96 tests y aun así vivo. **No lo declaro equivalente sin más, y
tampoco lo declaro hueco que el `tdd_craftsman` pueda cerrar**: es **inmatable
con el runner actual**. Comprobación directa con el jsdom del proyecto:

    :root{--canvas: #f8f9f5 }   →   getPropertyValue("--canvas") === "#f8f9f5"

jsdom devuelve ya el valor recortado, así que ninguna prueba unitaria en este
entorno puede distinguir `.trim()` de su ausencia, se autoren los SCSS con
espacios o sin ellos. La diferencia sólo podría observarse en un navegador real
que preserve el espacio inicial del valor computado de la custom property, y
allí el efecto sería que `meta.content` valiese `" #f8f9f5"` en vez de
`"#f8f9f5"`.

Consecuencia práctica: **no pido test nuevo** para 322 en vitest (sería un test
que no puede fallar). Si se quiere cerrar el flanco, el sitio es el gate de
navegador/E2E, no éste. Dejo explícito que **no he verificado** el
comportamiento del navegador real; el `.trim()` queda como defensa razonable y
no demostrada innecesaria. No se resta del denominador.

## Los otros 31 residuos de `appearance-state.tsx` (preexistentes al modo oscuro)

Ninguno cae en las líneas 43-53, 150 ni 165. Los enumero igualmente porque el
fichero es el que toca la feature, y sin declarar equivalencias en bloque:

| ID | Estado | Línea:col | Mutador | Sustitución |
| --- | --- | --- | --- | --- |
| 307 | Survived | 28:58–41:2 | ObjectLiteral | contexto por defecto → `{}` |
| 308 | Survived | 29:11 | BooleanLiteral | `failed: false` → `true` |
| 309 | Survived | 30:12 | BooleanLiteral | `reading: true` → `false` |
| 310 | Survived | 31:14 | BooleanLiteral | `uncertain: false` → `true` |
| 311 | NoCoverage | 32:23–34:4 | BlockStatement | `reload` por defecto → `{}` |
| 313 | NoCoverage | 33:21 | StringLiteral | mensaje "Apariencia no disponible" → `""` |
| 314 | NoCoverage | 35:35–37:4 | BlockStatement | `refreshAfterImport` por defecto → `{}` |
| 316 | NoCoverage | 36:21 | StringLiteral | mensaje "Apariencia no disponible" → `""` |
| 317 | NoCoverage | 38:21–40:4 | BlockStatement | `save` por defecto → `{}` |
| 319 | NoCoverage | 39:21 | StringLiteral | mensaje "Consulta la apariencia antes de guardar" → `""` |
| 331 | Survived | 58:46 | BooleanLiteral | `useState(false)` de `uncertain` → `true` |
| 343 | Survived | 71:11 | ConditionalExpression | `document.activeElement === document.body` → `true` |
| 348 | Survived | 73:13 | ConditionalExpression | `if (heading)` → `true` |
| 351 | Survived | 74:30 | UnaryOperator | `tabIndex = -1` → `+1` |
| 355 | Survived | 81:9 | ConditionalExpression | guarda completa de `save` → `false` |
| 356 | Survived | 81:9 | LogicalOperator | reagrupa la guarda de `save` con `&&` sobre `uncertain` |
| 357 | Survived | 81:9 | ConditionalExpression | subguarda `!snapshot`/`writeRequest.current` → `false` |
| 358 | Survived | 81:9 | LogicalOperator | `!snapshot` con `&&` en lugar de `\|\|` |
| 361 | NoCoverage | 82:23 | StringLiteral | mensaje de la guarda de `save` → `""` |
| 364 | Survived | 92:7 | CallExpression | `controller.signal.throwIfAborted();` → `;` |
| 376 | Survived | 104:11 | ConditionalExpression | `writeRequest.current === controller` → `true` |
| 383 | Survived | 107:56 | ArrayDeclaration | deps `[]` → `["Stryker was here"]` |
| 386 | Survived | 109:9 | ConditionalExpression | `if (readRequest.current)` → `false` |
| 387 | NoCoverage | 110:39 | StringLiteral | mensaje "Consulta de apariencia en curso" → `""` |
| 389 | Survived | 115:9 | CallExpression | `controller.signal.throwIfAborted();` → `;` |
| 395 | Survived | 121:13 | ConditionalExpression | `!controller.signal.aborted` → `true` |
| 406 | Survived | 130:6 | ArrayDeclaration | deps de `load` → `["Stryker was here"]` |
| 417 | Survived | 139:7 | ConditionalExpression | `snapshot.theme === "SYSTEM"` → `true` |
| 432 | Survived | 145:10 | ConditionalExpression | `snapshot.theme === "SYSTEM" && media?.matches` → `true` |
| 465 | Survived | 174:6 | ArrayDeclaration | deps `[load]` → `[]` |
| 472 | Survived | 187:15 | StringLiteral | mensaje de `refreshAfterImport` → `""` |

Notas de lectura, sin reclasificar nada:

- **417 y 432** viven en el mismo efecto que pinta el tema y gobiernan la rama
  SYSTEM. No son equivalentes: falta una prueba con `theme: "SYSTEM"` que fije
  `matchMedia` y distinga oscuro de claro según la preferencia del sistema. Es
  el hueco más cercano al modo oscuro y el que priorizaría para el
  `tdd_craftsman`.
- **307-310, 311-319, 331**: defensas del contexto por defecto sin Provider y
  estados iniciales; la revisión histórica
  (`review_appearance_stryker_ui_residues.md`) ya los inventarió como huecos de
  oráculo, no como defectos.
- **343/348/351** (devolución de foco al `h1`) y
  **355-358/361/376/386/387/395** (guardas de concurrencia y mensajes internos)
  son límites de oráculo ya documentados en aquella revisión.
- **383/406/465** mutan arrays de dependencias de `useEffect`/`useCallback`:
  sobreviven porque la suite no fuerza el re-montaje que los distinguiría. No
  afirmo equivalencia.

**No excluyo ningún mutante por la vía de la equivalencia.** El 89,46 % es el
número crudo sobre 778.

## Qué NO he verificado

- **SCSS y HTML**: `frontend/src/styles.scss`, `today.scss`, `history.scss` y
  `frontend/index.html` están fuera del universo de Stryker. La única prueba que
  cubre las metas del HTML es una lectura textual del fichero (primer caso de
  `theme-color.test.tsx`), y esa prueba no es mutable aquí. Contraste, geometría
  y color renderizado siguen sin gate de mutación.
- **Navegador real**: todo corre en jsdom. Ni la barra del navegador, ni la PWA,
  ni el valor computado de `--canvas` con espacios en un motor real (mutante 322).
- **Backend**: no lancé `pitest` ni ninguna campaña de backend. Cero
  contenedores PostgreSQL levantados.
- **Suite completa y E2E**: no ejecutadas. Lo único que corrió es el conjunto que
  Stryker usa (1038 tests del dry run del ámbito de apariencia), que no es la
  regresión completa del proyecto.
- **Otros ámbitos de mutación** (`integration_api`, `import_data`, `export_data`,
  `custom_views_fields`, …): fuera de esta campaña; conservan sus gates propios.

## Integridad

No he editado producción, tests, configuración de Stryker ni `feature_list.json`.
Árbol de trabajo limpio antes y después de la campaña.

Evidencia durable:

- `progress/darkmode_appearance_stryker/mutation.json` — informe JSON íntegro,
  SHA256 `895dd3123f0bdcb2663f053e3a174a9f1350ef40bd79f145f0bcb2c0481ffe8d`
  (copia byte a byte de `frontend/reports/mutation-appearance/mutation.json`).
- `progress/darkmode_appearance_stryker/run.log` — log completo sin códigos
  ANSI, 1938 líneas, SHA256
  `d1cf5799099e4a197fdc042a7f682161332e3373b5f0cabf928c8866c850e525`.
- Informe HTML en `frontend/reports/mutation-appearance/mutation.html`.
