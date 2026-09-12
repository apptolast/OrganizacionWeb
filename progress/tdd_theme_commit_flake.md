# Carril: flake del tema en la prueba en seco de Stryker (`theme_commit_flake`)

Rama `codex/mutation-shards` (PR #30). Alcance: `frontend/src/appearance-state.tsx`
(un hook) y una prueba de regresión en `frontend/src/theme-color.test.tsx`.

## 1. Síntoma

Run de GitHub Actions `34610657835`, job «Stryker 10/12»: Stryker 10.0.0 aborta
en la prueba en seco inicial con

```text
One or more tests failed in the initial test run: expected '' to be 'dark' // Object.is equality
There were failed tests in the initial test run.
```

Los otros 11 trozos pasaron la **misma** prueba en seco (misma suite, misma
configuración salvo `mutate` y `thresholds.break`). «Application CI» pasa en
`main`. Es intermitente, no sistemático.

`''` delata la aserción: es `document.documentElement.style.colorScheme` sin
asignar (`dataset.theme` sin asignar da `undefined`).

## 2. Diagnóstico

`AppearanceProvider` aplicaba el tema al documento (`data-theme`,
`color-scheme`, `--accent`, `theme-color`) en un `useEffect` sobre `snapshot`.
Con React 19, un commit de prioridad normal (la respuesta del `fetch` resuelve
fuera de `act`) agenda los efectos pasivos para una **tarea posterior** del
scheduler (`setImmediate` en Node). El DOM del commit ya muestra la apariencia
confirmada —el formulario, el radio marcado— y el documento sigue sin tema
hasta esa tarea.

Las pruebas hacen `await screen.findByRole(...)` y luego una aserción
**síncrona** sobre el tema (p. ej. `appearance.test.tsx:1285`, `@s24 follows a
system color-scheme change…`). `findBy*` resuelve por el `MutationObserver` del
commit y el `asyncWrapper` de Testing Library espera un `setTimeout(0)`. Si el
bucle de eventos tarda ≥ 1 ms en volver a la fase de temporizadores —código
instrumentado por Stryker, máquina cargada— el temporizador gana a la tarea de
efectos pasivos y la aserción ve `''`. Es una carrera entre dos macrotareas,
de ahí que falle uno de cada muchos.

El defecto no está en el oráculo: es real en el producto. Entre ese commit y
el efecto pasivo el navegador puede pintar un fotograma con el formulario
nuevo y el tema viejo. El oráculo lo exige bien («cuando se ve la apariencia,
el documento la lleva»); lo que no cumplía era el producto.

## 3. Reproducción

### Determinista (ROJO)

Nueva prueba en `theme-color.test.tsx`: un `MutationObserver` captura
`data-theme`, `color-scheme` y `theme-color` en el mismo instante en que el
commit muestra la apariencia confirmada. Antes del arreglo falla siempre, con
DARK y con LIGHT:

```text
× the document already wears 'DARK' in the same commit that shows it
× the document already wears 'LIGHT' in the same commit that shows it
AssertionError: expected { theme: undefined, …(2) } to deeply equal { theme: 'dark', …(2) }
```

### Estadística (la intermitencia de CI)

`appearance.test.tsx`, `theme-color.test.tsx` e `import-data.test.tsx`
lanzados en bucle con orden barajado (`--sequence.shuffle`, semilla distinta
por ejecución) y 4 ejecuciones en paralelo en una máquina de 8 núcleos, sobre
el árbol de HEAD (`5d3a2b3`) y sobre el arreglado, en las mismas condiciones:

<!-- markdownlint-disable MD013 -->

| Árbol | Ejecuciones | Fallidas | Firma de los fallos |
| --- | --- | --- | --- |
| HEAD `5d3a2b3` (antes) | 48 | 4 | las 4 `expected '' to be 'dark'` en `appearance.test.tsx:1285` (`@s24 follows a system color-scheme change…`) |
| Arreglado, tanda 1 | 48 | 0 | — |
| Arreglado, tanda 2 | 48 | 0 | — |

<!-- markdownlint-enable MD013 -->

Los cuatro fallos de antes tienen **exactamente** la firma de CI y ninguno es un
tiempo agotado. Con la tasa de antes (≈ 1 de 12), 96 ejecuciones seguidas en
verde tendrían una probabilidad de (11/12)^96 ≈ 0,02 %.

Un primer intento con 8 ejecuciones en paralelo sobre 8 núcleos saturó la
máquina: además de la misma firma (`appearance.test.tsx:1285`) salieron
tiempos agotados de 5 s y de `waitFor` (1 s) propios de la sobrecarga, que no
son esta carrera. Por eso la medida buena es la de 4 en paralelo.

## 4. Arreglo (VERDE)

`useEffect` → `useLayoutEffect` en el efecto que aplica el tema. Los efectos de
layout corren de forma síncrona dentro del mismo commit, antes de que el
navegador pinte y antes del `MutationObserver`. La limpieza (volver al tema
del sistema al salir de la sesión) también pasa a ser síncrona, y
`leaving the session returns theme-color to the system canvas (@s32)` sigue
en verde. No se toca ni se afloja ninguna prueba existente, ni la
configuración de Stryker, ni los umbrales.

## 5. Evidencia

- `vitest run src/theme-color.test.tsx` antes del arreglo: 2 fallos
  deterministas (la prueba nueva, DARK y LIGHT); después, 10 de 10 en verde.
- `vitest run` de todo el frontend después del arreglo: 79 ficheros, 2963
  pruebas en verde. `tsc --noEmit`, `eslint .` y `prettier --check .` limpios.
- `stryker run stryker.config.json --dryRunOnly` después del arreglo:
  «Initial test run succeeded. Ran 2949 tests».
- La misma prueba en seco con la configuración del trozo 10/12, generada con
  `frontendPartition(base, 12)[9]` y `shardStrykerConfig` de
  `scripts/mutation-shards.mjs` (`customization-api.ts`, `project-reader.tsx`,
  `task-history.tsx`, `use-project-tasks.ts`): «Initial test run succeeded.
  Ran 1148 tests». El fichero generado no se versiona.
- Una prueba en seco local de antes del arreglo falló por un tiempo agotado de
  5 s en `@s41`, pero corrió a la vez que el bucle saturado de 8 en paralelo:
  queda descartada como evidencia, en ningún sentido.
- Sin verificar: la tasa real en los runners de GitHub; eso lo dirá el próximo
  `workflow_dispatch` de calibración.
