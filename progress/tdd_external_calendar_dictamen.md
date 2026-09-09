# TDD — cierre de tres hallazgos del dictamen final sobre la feature 28 (calendario externo)

Fuente: `progress/dictamen_final_25_28_30.md`, hallazgos 15 (bloqueante), 16 y 17
de la sección `## Feature 28-external_calendar`.

Los tres son huecos de oráculo: la producción de esos tres puntos es correcta y
no se toca. Por eso el ciclo rojo se provoca **inyectando a mano el defecto que
la prueba debería detectar**, comprobando que la prueba nueva se pone roja,
restaurando y volviendo a verde. Sin esa comprobación solo se demuestra que la
prueba pasa, no que sirve.

Base: `c187023` (`main`).

## Mapa @s -> prueba

| Hallazgo | Cláusula del contrato | Prueba |
| --- | --- | --- |
| 15 | `features/external_calendar.feature` @s39 fila 3 (cancelación al salir) | `frontend/src/external-calendar.test.tsx` — «@s39 cancela la petición en curso al desmontar la vista» |
| 16 | @s36 / guardián `external-calendar-api.ts:197` | `frontend/src/external-calendar-api.test.ts` — «no valida la respuesta de una lectura ya cancelada» |
| 17 | @s40 «...con foco visible en cada control» | `e2e/external-calendar-ux-audit.spec.mjs` — «el recorrido con teclado sigue el orden del contrato @s40» |

## Hallazgo 15 — la prueba de cancelación al desmontar no comprobaba ninguna cancelación

**Qué había.** `external-calendar.test.tsx` montaba, desmontaba y afirmaba
`document.body.textContent === ""`, cierto tras cualquier desmontaje. El
`return () => controller.abort()` de `external-calendar.tsx:160` no lo miraba
nadie.

**Qué hay ahora.** Se calca el patrón ya vigente en
`frontend/src/today-external-calendar.test.tsx:248-276`: un `fetch` doblado
local que retiene la respuesta con una promesa liberada a mano y acumula en un
array las `options.signal` que recibe. Con la vista montada se exige
`signals.length >= 1` y `signals.every((s) => !s.aborted)`; se desmonta y se
exige `signals.every((s) => s.aborted)`; solo entonces se libera la respuesta
tardía (un cuerpo con suscripción real) y se comprueba que no repinta:
`queryByText("calendar.google.com")` ausente y cuerpo vacío.

**ROJO demostrado.** Defecto inyectado en `frontend/src/external-calendar.tsx`:
`return () => controller.abort();` sustituido por `return () => {};`.

```
FAIL  src/external-calendar.test.tsx > @s39 cancela la petición en curso al desmontar la vista
AssertionError: expected false to be true
 ❯ src/external-calendar.test.tsx:393:53
   expect(signals.every((signal) => signal.aborted)).toBe(true);
```

La antigua aserción `document.body.textContent === ""` seguía pasando con el
defecto puesto: es exactamente el hueco que el dictamen describe.

**VERDE tras restaurar.** Restaurado el `abort`:
`pnpm --dir frontend exec vitest run src/external-calendar.test.tsx` →
`Test Files 1 passed (1) / Tests 24 passed (24)`.

**Refactor.** Ninguno necesario; la prueba queda en una sola función corta y sin
duplicación con las vecinas (el `fetch` doblado local no puede compartirse con
el de `beforeEach` porque ese resuelve al instante y aquí hace falta retener).

Commit: `3ea8644`.

## Hallazgo 16 — la prueba de «no validar una lectura ya cancelada» pasaba por el motivo equivocado

**Qué había.** El `fetch` doblado abortaba y devolvía
`{configured: true, subscription: null}`, cuerpo que `snapshotOf` ya declara
inválido. La aserción era un `rejects.toThrow()` pelado: rechazaba, sí, pero
podía estar rechazando por la validación de forma y no por el aborto. Borrar
`external-calendar-api.ts:197` (`signal?.throwIfAborted()` posterior a
`apiRequest`) dejaba la prueba verde.

**Qué hay ahora.** El cuerpo devuelto es **válido**
(`{configured: false, subscription: null}`), de modo que la validación de forma
ya no puede enmascarar nada, y el rechazo se captura y se afirma en concreto:

- `expect(rejection).toBe(controller.signal.reason)` — es exactamente el motivo
  del aborto, no otro error cualquiera;
- `expect(rejection).toMatchObject({ name: "AbortError" })`;
- `expect(rejection).not.toMatchObject({ message: "Respuesta de calendario externo inválida." })`
  — afirmación explícita de que **no** se lanzó el error de validación.

**ROJO demostrado (dos veces).** Defecto inyectado en
`frontend/src/external-calendar-api.ts`: borrado el `signal?.throwIfAborted();`
posterior a `apiRequest` (el mutante que el dictamen nombra).

1. Con el cuerpo válido de la prueba nueva, la lectura ya no rechaza:

```
- Expected: [Error: This operation was aborted]
+ Received: null
 ❯ src/external-calendar-api.test.ts:342:23
   expect(rejection).toBe(controller.signal.reason);
```

2. Con el mismo mutante y el cuerpo **inválido** de la versión anterior —es
   decir, «que la validación falle en vez del aborto»— el rechazo existe pero es
   el equivocado, y la prueba nueva lo distingue:

```
- "message": "This operation was aborted"
+ Error { "message": "Respuesta de calendario externo inválida." }
 ❯ src/external-calendar-api.test.ts:342:23
```

Esa segunda ejecución es la prueba de que el hueco denunciado era real: con el
oráculo antiguo, ese mismo rechazo por validación bastaba para pasar.

**VERDE tras restaurar.** `git diff` sobre `external-calendar-api.ts` vacío
(producción idéntica a `main`) y
`pnpm --dir frontend exec vitest run src/external-calendar-api.test.ts` →
`Test Files 1 passed (1) / Tests 42 passed (42)`.

**Refactor.** Ninguno; tres aserciones, sin duplicación ni números mágicos.
Fuera de alcance por decisión explícita del dictamen (arreglo 2 del hallazgo 16,
marcado como opcional): no se añade la prueba de componente sobre la vía de
éxito de `external-calendar.tsx:144-145` / `:128-129`.

Commit: `39c04a4`.

## Hallazgo 17 — el foco visible se comprobaba en un solo control, no «en cada control»

**Qué había.** El bucle salía en cuanto `seen` reunía los cinco nombres y la
única medición se hacía después, sobre el elemento activo en ese instante:
«Eliminar suscripción». Etiqueta, Dirección secreta iCal, Guardar y Sincronizar
ahora no se medían nunca. El predicado era `outlineWidth !== "0px"`, que ni
siquiera distingue el anillo del producto del que pinta el agente de usuario.

**Qué hay ahora.** La medición ocurre **dentro** del bucle, en cada una de las
cinco paradas reconocidas, con el predicado del precedente del repositorio
(`e2e/github-connector.spec.mjs:293-338`) más la parte específica del producto:

```
stop.matchesFocusVisible &&
stop.outlineStyle === "solid" &&
stop.outlineWidth >= FOCUS_RING_MIN_WIDTH &&
!stop.outlineColor.includes("transparent")
```

Exigir `outlineStyle === "solid"` y 3 px es lo que declara la regla
`:focus-visible` de `frontend/src/styles.scss:621-624`; el anillo de Chromium
computa `outline-style: auto`, así que borrar esa regla global —el mutante que
el dictamen señala como superviviente— ya no pasa desapercibido.

Los fallos se acumulan en `invisible` con el nombre y las cuatro métricas de la
parada, y se afirma `expect(invisible).toEqual([])`. Además se acumulan en
`intruders` los nombres **no esperados** que reciben el foco dentro de
`.external-calendar`, con `expect(intruders).toEqual([])`: así se afirma que no
se cuela ningún foco intermedio dentro del formulario. Los intermedios de fuera
del formulario (saltar al contenido, barra lateral) se siguen filtrando, que es
lo correcto según el propio dictamen: el contrato pide orden, no contigüidad.
Los dos números del bucle dejan de ser mágicos (`FOCUS_RING_MIN_WIDTH`,
`MAX_TAB_STEPS`).

**ROJO demostrado.** Defecto inyectado en `frontend/src/styles.scss`: se apaga
el anillo del **primer** control del recorrido, justo el que el oráculo antiguo
no miraba nunca.

```scss
#external-calendar-label:focus-visible {
  outline: none;
}
```

`E2E_WEB_PORT=18096 node scripts/e2e.mjs e2e/external-calendar-ux-audit.spec.mjs -g "recorrido con teclado"`:

```
> 159 |   expect(invisible).toEqual([]);
+   Object {
+     "insideForm": true,
+     "matchesFocusVisible": true,
+     "name": "Etiqueta",
+     "outlineColor": "rgb(35, 57, 47)",
+     "outlineStyle": "none",
+     "outlineWidth": 3,
+   },
1 failed › calendario externo audit: el recorrido con teclado sigue el orden del contrato @s40
```

El mensaje nombra el control que falla, que era el objetivo del acumulador.

**VERDE tras restaurar.** Revertido `frontend/src/styles.scss` a HEAD (`git
status` limpio para ese fichero) y repetida la misma orden:

```
✓ e2e\external-calendar-ux-audit.spec.mjs:94:1 › calendario externo audit: el recorrido con teclado sigue el orden del contrato @s40 (1.2s)
2 passed (7.8s)
```

(El segundo verde es `github-connector.spec.mjs:261`, que el `-g` también
alcanza; se deja constancia para que nadie lo lea como una repetición.)

**Refactor.** El cuerpo del bucle queda en tres decisiones cortas (intruso,
repetición, medición) y prettier reformatea el `for`. Una sola pila E2E en el
puerto 18096 por ejecución, bajada por el `finally` de `scripts/e2e.mjs` en
ambas (rojo y verde): no quedan contenedores huérfanos.

## Estado de cierre

Los tres hallazgos quedan cerrados con rojo demostrado antes del verde
(cuatro ejecuciones rojas en total: una en el 15, dos en el 16, una en el 17).
Producción sin cambios: `git diff` contra la base es vacío para
`frontend/src/external-calendar.tsx`, `frontend/src/external-calendar-api.ts` y
`frontend/src/styles.scss`.

No se marca nada `done`: falta el veredicto del juez y la mutación.

Fuera de alcance (registrado por el lead, no tocado aquí):

- La guardia contra SSRF descarta las direcciones que acaba de validar y vuelve
  a conectar por nombre: la enmienda B3 no está implementada. Bloqueante de
  seguridad aparte.
- `HttpCalendarFeed` no tiene plazo para leer el **cuerpo**
  (`HttpRequest.timeout` no lo cubre con `BodyHandlers.ofInputStream`); un
  proveedor que se calle a mitad cuelga la descarga. Demostrado en
  `progress/tdd_feed_size_abort.md` sección 5; exige contrato nuevo.
