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
