# Feature 30 — automatizaciones, segunda pasada (carril C)

Cierre de los hallazgos del dictamen `progress/carriles/dictamen_f30.md`.
Worktree `C:/Users/vhurt/ow-worktrees/automations`, rama `claude/automations`,
`E2E_WEB_PORT=18094`.

## Alcance acordado en esta sesión

El coordinador retiró el bloqueante 1 (ejecutor de reglas) del encargo por
plazo: es trabajo de horas y a medias no vale. En su lugar queda escrito el
inventario preciso de los nueve escenarios sin oráculo (sección final), para
que quien lo retome empiece con el mapa hecho.

---

## Hallazgo 2 [BLOQUEANTE] — el interruptor no ataba el If-Match vivo ni el instante del cambio

**Contrato:** `features/automations.feature:534` (@s40, fila del interruptor).

**Qué faltaba.** El doble de fetch de `frontend/src/automations.test.tsx:50`
sólo guardaba `{url, method}`: las cabeceras de la petición se tiraban. Y la
ruta PUT se declaraba sin `delay`, así que entre el clic y la respuesta no
existía ningún instante en el que afirmar nada.

**Ciclo.**

1. ROJO por mutación (no se puede escribir el test "antes" de un producto que
   ya existe, así que el rojo se acredita rompiendo la producción):
   - Mutante A — `frontend/src/automations.tsx:320`, `rule.version` sustituido
     por el literal `1`. Con las pruebas nuevas:
     `AssertionError: expected '"1"' to be '"2"'`, 2 tests fallan.
     Con las pruebas anteriores el mutante sobrevivía (era el superviviente que
     el dictamen predecía).
   - Mutante B — interruptor optimista: `setRules` marcando el cambio en el
     clic, antes del `await replaceAutomation`. Con las pruebas nuevas los dos
     tests del interruptor fallan con `Received element is not checked:`.
     Con las anteriores sobrevivía, porque sólo observaban el estado final.
2. VERDE: producción restaurada, 15/15 en `src/automations.test.tsx`.

**Cambios (sólo pruebas).**
- `calls` pasa a `{url, method, headers: Headers}[]` y el doble hace
  `new Headers(options.headers)`.
- `@s40 flips the switch…`: la ruta PUT lleva promesa retenida; antes de
  liberarla se afirma `toBeChecked()` y «Activa»; después, «Inactiva»; se
  afirma `If-Match: "2"` en el primer PUT y, tras un segundo accionamiento con
  su propia respuesta 200, `If-Match: "3"` en el segundo — la versión que el
  servidor acaba de devolver.
- `@s40 puts the switch back…`: misma promesa retenida, con la afirmación de
  que el interruptor sigue marcado durante el vuelo, de modo que el optimismo
  con revert también muere. Añade el `If-Match: "2"` de esa petición.

**Estado: cerrado.**

---

## Hallazgo 6 [ALTA] — el aislamiento por identidad (`key={owner}`) no tenía oráculo

**Contrato:** `features/automations.feature:561-571` (@s43, «no queda ningún
dato de reglas ni simulaciones en memoria de otra identidad»).

**Qué faltaba.** `frontend/src/automations.tsx:147-149` monta
`<AutomationsWorkspace key={owner} />`: el prop `owner` no se usa para nada
más, existe sólo para forzar el remontaje al cambiar de identidad. Ninguno de
los tests cambiaba nunca de identidad, así que borrar la `key` dejaba la suite
verde al 100 %. Es la única feature del proyecto sin el patrón
`view.rerender(<X owner="otro" />)` que ya usan github-connector,
integration-api, webhooks, import-data y calendar.

**Ciclo.**

1. ROJO por mutación: borrada la `key={owner}` de `automations.tsx:148`.
   `@s43 keeps no rule nor simulation of the identity that just left` falla con
   `TestingLibraryElementError: Unable to find role="switch" and name /pausada/i`
   — sin remontaje no se vuelve a pedir la lista y sigue en pantalla la de Ana.
   Antes del test, ese mutante sobrevivía entero.
2. VERDE: producción restaurada, 16/16.

**Cambio (sólo pruebas).** Test nuevo en `frontend/src/automations.test.tsx`:
monta con `owner="ana"` con la regla «Seguimiento», abre el editor, simula y
espera el `role="status"` de la simulación; entonces
`view.rerender(<Automations owner="bruno" />)` con una segunda respuesta de
`/api/v1/me/automations` que devuelve otra regla; afirma que ni la regla ni el
título de la coincidencia ni el editor de Ana siguen en pantalla, que la lista
se ha vuelto a pedir (2 GET) y que no queda nada en `localStorage` ni en
`sessionStorage`.

**Estado: cerrado.**

---

## Hallazgo 11 [MEDIA] — el ámbito de mutación dejaba fuera la integración con el armazón

**Qué faltaba.** `frontend/stryker.automations.config.json` mutaba sólo
`src/automations-api.ts` y `src/automations.tsx`. La feature también añadió
producción en `src/App.tsx` (predicado de ruta, rama del ternario `section`,
rama de render) y en `src/workspace.tsx` (el `RouteLink` con su
`aria-current`), y ninguna configuración invocable las mutaba.
`docs/mutation-testing.md` exige el umbral sobre las líneas nuevas o tocadas.

**Ciclo.**

1. ROJO: añadida al guardarraíl `automations Stryker configuration mutates only
   the feature files` de `scripts/project.test.mjs` la lista de seis entradas y
   la validación por contenido de los cuatro rangos.
   `node --test scripts/project.test.mjs` → `not ok 88 … Expected values to be
   strictly deep-equal`, con los cuatro rangos ausentes de la configuración.
2. VERDE: los cuatro rangos añadidos a la configuración. 94/94.

**Rangos, con la convención de `stryker.ics-calendar.config.json`** (columna
inicial 0-indexada, columna final excluyente, sin el punto y coma final):
`src/App.tsx:47:8-47:51`, `src/App.tsx:55:8-56:30`, `src/App.tsx:84:7-85:40`,
`src/workspace.tsx:128:10-133:22`. El guardarraíl los recorta del fichero real
y comprueba que empiezan por `automations = route`, `automations`,
`automations && username` y `<RouteLink`, y que contienen
`/automatizaciones`, `Automatizaciones`, `<Automations owner={username} />` y
`/automatizaciones`. Así un desplazamiento de `App.tsx` rompe la prueba en vez
de mutar en silencio otra pantalla.

**Fichero compartido tocado** (REGLAS.md §6): `scripts/project.test.mjs`, sólo
dentro del `test(...)` de automations. Punto de conflicto probable en la
integración.

**Deuda de lote anotada, fuera de mi ámbito:** `stryker.external-calendar`,
`stryker.github-connector` y la feature 25 (sin configuración ni destino de
mutación frontend) tienen la misma omisión.

**Estado: cerrado.** No se ejecuta la campaña: el coordinador lo prohibió por
plazo y carga de máquina.
