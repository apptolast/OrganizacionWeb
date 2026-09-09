# TDD — cierre de cinco hallazgos del dictamen final (feature 25, webhooks)

Base: `main` en `c187023`. Dictamen: `progress/dictamen_final_25_28_30.md`
(hallazgos 14, 19, 20, 22 y 23 de la sección «Feature 25-webhooks»).

Los cuatro primeros son huecos de oráculo, no defectos de producto: la
implementación ya es correcta. El rojo se demuestra **inyectando a mano el
defecto que la prueba nueva debe cazar**, comprobando el fallo, restaurando y
comprobando el verde. Cada ciclo deja constancia del mutante concreto, del
mensaje de fallo y de la restauración.

Comandos usados (nunca la suite completa):

- Frontend: `pnpm --dir frontend exec vitest run src/webhooks.test.tsx`
- Backend: `backend/gradlew.bat test --no-daemon -p backend --tests "com.apptolast.organization.adapter.persistence.WebhookWorkPersistenceTest"`

Línea base antes de tocar nada: `webhooks.test.tsx` 23/23 verde.

---

## Hallazgo 14 (ALTA) — `@s38 starts clean for another identity` no montaba la presencia

**Fichero:** `frontend/src/webhooks.test.tsx`
**Escenario:** `@s38`, fila «cambia la identidad de acceso»
(`features/webhooks.feature:465-476`).

**Problema.** La prueba hacía `stubApi([endpoint()])`, renderizaba con `Ana`,
`rerender` con `Bea` y afirmaba `queryByDisplayValue(secret)` ausente. El
secreto sólo se pinta tras un 201 y la prueba nunca creaba ningún webhook: la
aserción ya era cierta **antes** del `rerender`. Una prueba que no puede
fallar.

**ROJO (mutante inyectado a mano).** `frontend/src/webhooks.tsx:50`,
`key={owner}` → `key={owner.slice(0, 0)}` (conserva el prop, así que ni lint ni
TypeScript lo enmascaran; elimina el remonte por identidad). Con la prueba
antigua la suite quedaba verde: ese mutante sobrevivía.

**Prueba nueva** — `@s38 shows the secret for one identity and starts clean for
another`, en dos tiempos:

1. Presencia: stub que sirve listas distintas por montaje (`[]` para Ana,
   `[Hook de Bea]` para Bea) y un 201 con `secret`. Se rellena el formulario,
   se crea el webhook y se espera a `getByDisplayValue(secret)` visible, más
   `Mi hook` y su URL en la lista de Ana.
2. Ausencia: `rerender(<Webhooks owner="Bea" />)`, y sólo entonces se exige que
   el secreto, `Mi hook` y `https://example.com/hooks` hayan desaparecido y que
   aparezca `Hook de Bea`.

**Rojo observado** con el mutante puesto:

```
FAIL src/webhooks.test.tsx > @s38 shows the secret for one identity and starts clean for another
expected document not to contain element, found <input readonly value="whsec_AAA…" /> instead
  webhooks.test.tsx:338  expect(screen.queryByDisplayValue(secret)).not.toBeInTheDocument();
Tests  1 failed | 22 passed (23)
```

**VERDE.** Restaurado `key={owner}` en `webhooks.tsx:50` (única línea tocada de
producción, y devuelta a su estado original): `webhooks.test.tsx` 23/23 verde.
No se ha cambiado nada de `src/` de forma permanente: el defecto era del
oráculo, no del producto.

**Fuera de alcance, deliberadamente:** las cláusulas (b) «la petición pendiente
se aborta al cambiar de identidad» y (c) «un 401 tardío no retira una sesión
posterior» que el verificador anota en el mismo hallazgo. No estaban en el
encargo de este ciclo y no son «minutos»; quedan anotadas aquí como deuda
abierta de `@s38`.

Commit: `e9eab30`.

---

## Hallazgo 22 (MEDIA) — `@s37`: «no se copia sin activar Copiar» sin oráculo

**Fichero:** `frontend/src/webhooks.test.tsx`, prueba `@s37 sends one POST with
the twelve types and shows the secret once`.
**Cláusula:** `features/webhooks.feature:461` — «no se escribe whsec_x en
localStorage, sessionStorage ni en la URL **y no se copia sin activar Copiar**».
De los cuatro canales, el portapapeles —el único con condición— no se observaba
en ningún punto del carril (ni en `webhooks-client.test.ts`, ni en
`webhooks-route.test.tsx`, ni en el E2E). La única aserción sobre el botón era
que se ve.

**Prueba nueva** (cuatro líneas dentro de la prueba que ya existía, siguiendo el
patrón vigente en `integration-api.mutation.test.tsx:86-119` y
`calendar.test.tsx:233-258`):

- `const write = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();`
- con el panel del secreto ya visible, `expect(write).not.toHaveBeenCalled()`;
- `await user.click(copy)`;
- `expect(write).toHaveBeenCalledExactlyOnceWith(secret)`.

**ROJO 1 — botón decorativo** (`webhooks.tsx:293`,
`onClick={() => void navigator.clipboard?.writeText(secret)}` →
`onClick={() => void secret}`). Es el mutante que dejaría al usuario sin el
secreto irrecuperable:

```
FAIL @s37 sends one POST with the twelve types and shows the secret once
AssertionError: expected "writeText" to be called once with arguments: [ Array(1) ]
Number of calls: 0
  webhooks.test.tsx:233  expect(write).toHaveBeenCalledExactlyOnceWith(secret);
Tests  1 failed | 22 passed (23)
```

**ROJO 2 — copia sin gesto** (añadido a `WebhookPanel`:
`useEffect(() => { if (secret) void navigator.clipboard?.writeText(secret); }, [secret])`).
Es la violación de privacidad literal de la cláusula:

```
FAIL @s37 sends one POST with the twelve types and shows the secret once
AssertionError: expected "writeText" to not be called at all, but actually been called 1 times
  1st writeText call: [ "whsec_AAAA…" ]
  webhooks.test.tsx:229  expect(write).not.toHaveBeenCalled();
Tests  1 failed | 22 passed (23)
```

Ambos mutantes dejaban verde la suite antes de este ciclo.

**VERDE.** Retirados los dos mutantes; `git diff frontend/src/webhooks.tsx`
vacío contra `HEAD` y `webhooks.test.tsx` 23/23 verde.

Commit: `b2c4c16`.

---

## Hallazgo 20 (MEDIA) — `@s40`: se contaban botones en vez de mirar las filas

**Fichero:** `frontend/src/webhooks.test.tsx`, prueba `@s40 opens the deliveries
panel on demand, without polling, and redelivers terminal rows`.
**Cláusulas:** `features/webhooks.feature:496-498` — «sólo las filas succeeded y
exhausted tienen botón Reenviar y la fila pending no» y «se envía un POST
redeliver y la fila pasa a Pendiente con intento 0».

**Problema.** `expect(getAllByRole("button", { name: "Reenviar" })).toHaveLength(2)`
mide un total, no la asociación fila-botón; y la tercera cláusula (efecto
visible del reenvío) no tenía ninguna aserción: la prueba terminaba en un
`waitFor` sobre «alguna URL acaba en /redeliver».

**Prueba nueva.** Se desestructuran las filas
(`const [, succeededRow, exhaustedRow, pendingRow] = getAllByRole("row")`), se
confirma el estado de cada una («Entregada», «Agotada», «Pendiente») y luego:

- `within(succeededRow).getByRole("button", { name: "Reenviar" })` y lo mismo
  en `exhaustedRow`;
- `within(pendingRow).queryByRole("button", { name: "Reenviar" })` a `null`;
- el clic se hace sobre el botón **de la fila succeeded** obtenido con `within`;
- se exige una única llamada `/redeliver`, con URL exacta
  `/api/v1/me/webhooks/${id}/deliveries/${deliveryId}/redeliver` y método POST;
- tras la respuesta, `within(succeededRow)` muestra «Pendiente», la celda de
  intento vale `0` y ya no dice «Entregada».

**ROJO 1 — botón en la fila equivocada** (`webhooks.tsx:503`,
`row.status !== "pending"` → `row.status !== "succeeded"`; siguen siendo dos
botones, pero en exhausted y **pending**, justo lo que el contrato prohíbe):

```
FAIL @s40 opens the deliveries panel…
TestingLibraryElementError: Unable to find an accessible element with the role "button" and name "Reenviar"
```

Con la prueba antigua este mutante sobrevivía: el conteo seguía dando 2 y el
clic sobre `[0]` seguía disparando una URL acabada en `/redeliver`.

**ROJO 2 — se reenvía la entrega equivocada** (`webhooks.tsx:506`,
`redeliver(deliveriesOf, row)` → `redeliver(deliveriesOf, deliveries[1])`):

```
AssertionError: expected '/api/v1/…/deliveries/33333333-…/redeliver' …
Expected: "…/deliveries/33333333-3333-4333-8333-333333333333/redeliver"
Received: "…/deliveries/22222222-2222-4222-8222-222222222222/redeliver"
```

**ROJO 3 — la fila no se actualiza** (`webhooks.tsx:244-246`,
`current.map((item) => (item.id === reopened.id ? reopened : item))` →
`current.map((item) => item)`):

```
TestingLibraryElementError: Unable to find an element with the text: Pendiente
```

**VERDE.** Los tres mutantes retirados uno a uno; `git diff
frontend/src/webhooks.tsx` vacío contra `HEAD`. `webhooks.test.tsx` 23/23 verde,
`tsc --noEmit`, `eslint` y `prettier --check` limpios.
