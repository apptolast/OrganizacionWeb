# TDD — Feature 24 (API para integraciones): marca a 320 px con texto al 200 %

Fecha: 8 de septiembre de 2026. Rama `codex/integration-api` (PR #29), worktree
`C:/Users/vhurt/ow-worktrees/integration-api`.

## Fallo de partida

CI 34253701367 fallaba en un único E2E:
`e2e/integration-api-browser.spec.mjs:272 "integration simulated API: text 200 percent and existing media modes @s41"`.

```
AssertionError: text200 page fits: {"expectedWidth":320,"innerWidth":320,"clientWidth":320,"scroll":357,
"offending":[{"tag":"DIV","className":"brand","right":357.40625,"width":345.40625},
{"tag":"SPAN","className":"","right":357.40625,"width":311.40625},
{"tag":"SPAN","className":"brand-light","right":357.40625,"width":72.1875}]}
```

El oráculo del spec duplica `font-size` de todos los elementos, fija el viewport a
320/768/1280 px y exige `document.documentElement.scrollWidth <= width`.

## Causa raíz

`frontend/src/styles.scss` declaraba `.brand { white-space: nowrap; }`. La marca
"OrganizationWeb" (`div.brand` > `span[translate=no]` > `span.brand-light`) no
tiene espacios, así que a 320 px (regla `@media (max-width: 360px)` → 12 px) y
texto al 200 % (24 px) el texto en negrita queda en una sola línea. Con las
fuentes de Linux de CI (más anchas que Segoe UI) mide 311 px; sumando la marca
`o.` (29 px) y el hueco (5 px), `.brand` alcanza 345 px y desborda el viewport.
En Windows, con Segoe UI, el mismo texto mide 266 px y cabe por poco, de ahí que
el fallo solo apareciera en CI. El problema es la prohibición de partir, no una
fuente concreta.

## Ciclo rojo → verde

### Rojo (reproducción local, independiente de la fuente del sistema)

El E2E rojo ya existía. Para reproducirlo en Windows sin depender de las
fuentes de CI, levanté `vite` (`pnpm --dir frontend exec vite --host 127.0.0.1
--port 5173`) y ejecuté un script Playwright en el scratchpad que:

1. Simula `/api/session`, `/api/v1/me/appearance` y `/api/v1/me/api-credentials`
   con `page.route`, igual que el spec.
2. Inyecta `.brand{font-family:<fuente> !important}` para forzar métricas más
   anchas.
3. Aplica el mismo doblado de `font-size` que el spec, pone el viewport a
   320 px y mide `scrollWidth` y el rectángulo de `.brand`.

Resultado en `/integraciones/api` **antes** del cambio:

| Fuente forzada | ancho `.brand` | `scrollWidth` | Resultado |
| --- | --- | --- | --- |
| Verdana | 343.5 px | 355 | OVERFLOW (reproduce el 345/357 de CI) |
| Arial Black | 338.8 px | 351 | OVERFLOW |
| serif (Times) | 278.4 px | 320 | cabe |
| Segoe UI (Windows por defecto) | 300.0 px | 320 | cabe |

### Verde (cambio mínimo en SCSS)

`frontend/src/styles.scss`:

```scss
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;            // nuevo: el bloque puede encoger dentro del sidebar flex
  font-size: 17px;
  font-weight: 650;
  letter-spacing: -0.65px;
  overflow-wrap: anywhere; // sustituye a white-space: nowrap
}
.brand-light {
  display: inline-block;   // nuevo: punto de corte preferente antes de "Web"
  font-weight: 400;
}
```

- Se elimina `white-space: nowrap`, que era lo que impedía cualquier reflujo.
- `display: inline-block` en `.brand-light` crea una oportunidad de salto de
  línea entre "Organization" y "Web" (los inline atómicos se tratan como
  ideogramas a efectos de partición), de modo que el corte normal es
  "Organization / Web" y no a mitad de palabra.
- `overflow-wrap: anywhere` es la red de seguridad si ni "Organization" cabe;
  a diferencia de `break-word`, `anywhere` sí reduce el `min-content`, por lo que
  el hijo flex puede encoger de verdad.
- `min-width: 0` permite que `.brand` encoja como hijo flex de `.sidebar`.

No se oculta ni recorta contenido, no se cambian colores (contraste intacto),
no se tocan tamaños de controles (los objetivos de 44×44 no dependen de la
marca) y a tamaño normal la marca sigue en una línea: el cambio solo actúa
cuando no cabe.

Resultado **después** del cambio, mismo script:

| Fuente forzada | ancho `.brand` | alto `.brand` | `scrollWidth` | Resultado |
| --- | --- | --- | --- | --- |
| Verdana | 296 px | 82 px (2 líneas) | 320 | cabe |
| Arial Black | 296 px | 96 px (2 líneas) | 320 | cabe |
| serif (Times) | 278.4 px | 38 px (1 línea) | 320 | cabe |
| Segoe UI | 296 px | 92 px (2 líneas) | 320 | cabe |

### Refactor

Ninguno necesario: 3 líneas añadidas, 1 sustituida, sin duplicación.

### Test unitario

No se añade test Vitest/RTL: jsdom no calcula layout ni carga `styles.scss`, y
una aserción sobre texto CSS sería un oráculo frágil que no demuestra el
comportamiento. El test que cubre @s41 sigue siendo el E2E existente
(`integration-api-browser.spec.mjs:272`), que ahora pasa.

## Archivos tocados

- `frontend/src/styles.scss` (reglas `.brand` y `.brand-light`).
- `progress/tdd_integration_api_text200.md` (este documento).

## Comandos ejecutados y resultados exactos

| Comando | Resultado |
| --- | --- |
| `pnpm --dir frontend test` | `Test Files 67 passed (67)`, `Tests 2507 passed (2507)`, 51.14 s |
| `pnpm --dir frontend exec tsc --noEmit` | exit 0 |
| `pnpm --dir frontend exec prettier --check src/styles.scss` | `All matched files use Prettier code style!` |
| `pnpm test:e2e e2e/integration-api-browser.spec.mjs` | `5 passed (12.7s)`, `1 skipped` (el test "native Chromium zoom 200" se salta cuando el proyecto Playwright no se llama `chromium`, condición preexistente del spec); `:272 text 200 percent ... @s41` en verde (5.1 s) |
| `pnpm test:e2e e2e/appearance-ux-audit.spec.mjs e2e/authentication.spec.mjs` | `10 passed (2.0m)` (5 + 5) |

La pila de E2E se levantó con Docker Compose en el puerto 18080 (proyecto
`organizationweb-e2e-<pid>`) y se destruyó al terminar.

## Límites

- La verificación con fuentes anchas se hizo en Windows contra Chromium
  forzando `font-family` (Verdana reproduce casi exactamente los 345 px de CI);
  no se ejecutó en el contenedor Linux de CI. La CI 34253701367 debe repetirse
  para confirmar.
- El script de reproducción vive en el scratchpad de la sesión, no en el
  repositorio: es una prueba manual, no un test permanente.
- En la pantalla de acceso (`/`), el mismo script mide `scrollWidth` 321 a
  320 px con texto al 200 % **antes y después** del cambio, con `.brand`
  terminando en 300 px; el desbordamiento de 1 px procede de otro elemento y
  queda fuera de esta corrección. `e2e/authentication.spec.mjs` pasa porque su
  comprobación de reflujo no dobla el texto.
