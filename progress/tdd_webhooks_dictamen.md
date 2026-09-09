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

Commit: `12ca10d`.

---

## Hallazgo 19 (MEDIA) — la poda de `@s29` contaba 50, no comprobaba cuáles

**Fichero:** `backend/src/test/java/com/apptolast/organization/adapter/persistence/WebhookWorkPersistenceTest.java`,
método `s29_onlyTheFiftyMostRecentTerminalsSurviveAndPendingOnesAreNeverPruned`.
**Cláusulas:** `features/webhooks.feature:367-368` — «quedan persistidas
exactamente 50 terminales, **las de mayor updatedAt**, y las 2 pendientes» y
«items … ordenados por updatedAt DESC y después id DESC».

**Problema.** El oráculo era de cardinalidad: `assertEquals(52, log.size())` y
`assertEquals(50, …filter("succeeded").count())`. Una poda que borrase cinco al
azar —o que conservase las cinco más antiguas— pasaba igual.

**Prueba nueva.** Se acumulan los ids de las 55 terminales en orden de registro
(`recorded`, índice 0 el `updatedAt` menor) y se sustituye el conteo por
identidad **y** orden:

```java
assertEquals(recorded.subList(5, 55).reversed(), survivors,
    "the fifty terminals of greatest updatedAt, newest first");
assertTrue(java.util.Collections.disjoint(recorded.subList(0, 5), survivors),
    "the five oldest terminals are the ones pruned");
```

**ROJO 1 — se conservan las 50 más antiguas** (`PostgresWebhookWork.java:131`,
`ORDER BY updated_at DESC, id DESC` → `ASC, id ASC` dentro del `prune`):

```
WebhookWorkPersistenceTest > s29_onlyTheFiftyMostRecentTerminals…() FAILED
AssertionFailedError: the fifty terminals of greatest updatedAt, newest first
  expected: <[7dbae1c8-…, 9c8b614b-…, …]>  (índices 54→5)
  but was:  <[e9a7fe69-…, …, 1460a8a3-…]>  (índices 49→0: se perdieron las cinco más recientes)
```

**ROJO 2 — poda de cinco al azar** (mismo sitio, `ORDER BY random() LIMIT ?`):

```
WebhookWorkPersistenceTest > s29_onlyTheFiftyMostRecentTerminals…() FAILED
(falla la aserción «the fifty terminals of greatest updatedAt, newest first»)
```

Los dos mutantes dejaban intactos `52 == log.size()` y `50 == count`, es decir
sobrevivían al oráculo anterior.

**ROJO 3 — el listado se sirve al revés** (`PostgresWebhookStore.java:123`,
`ORDER BY updated_at DESC, id DESC` → `ASC, id ASC` en `list`). También rojo:
la misma aserción fija ahora el orden del listado, que hasta hoy no tenía
oráculo en ninguna prueba (`WebhookPersistenceTest:197` compara una lista de un
solo elemento).

**VERDE.** Los tres mutantes retirados; `git status backend/` sólo muestra
modificado el fichero de test. Clase completa 6/6 verde
(`build/test-results/test/TEST-…WebhookWorkPersistenceTest.xml`:
`tests="6" skipped="0" failures="0" errors="0"`), `spotlessApply` sin cambios
pendientes. Nunca se lanzó la suite entera: siempre `--tests` sobre esta clase.

Commit: `422b68e`.

---

## Hallazgo 23 (MEDIA) — la cifra de unitarios de la matriz UX no era reproducible

**Fichero:** `progress/ux_webhooks.md:5`. No hay ciclo rojo-verde: es un defecto
documental, no un hueco de oráculo. La disciplina aquí es **medir**, no razonar.

**Medición real** (no conteo de declaraciones):

```
pnpm --dir frontend exec vitest run src/webhooks.test.tsx \
  src/webhooks-route.test.tsx src/webhooks-client.test.ts
→ Tests 44 passed (44)
   webhooks.test.tsx 23 · webhooks-client.test.ts 18 · webhooks-route.test.tsx 3
```

(el desglose por fichero se extrajo del `--reporter=json` de esa misma corrida).

**De dónde no salían los 58.** En `99f1e94`, el commit que escribió la matriz,
los tres ficheros declaraban 22 + 18 + 3 = 43 `it`, o 44 casos contando el
`it.each` de dos filas de `webhooks.test.tsx`. Ningún otro fichero de pruebas
del frontend contiene pruebas de webhooks (`integration-api.test.tsx` lo
menciona sólo en un comentario), así que **no** se trata de haber incluido
ficheros ajenos: la cifra nunca fue reproducible. No hay cobertura perdida; los
dos tests de `@s42` que añadió ese mismo commit están contados.

**Arreglo.** `progress/ux_webhooks.md:5` pasa a declarar 44 con desglose por
fichero y con el comando exacto que lo reproduce, para que la cifra sea
auditable sin ejecutar nada. Los 23 de `webhooks.test.tsx` incluyen las pruebas
reforzadas en esta sesión: se han endurecido oráculos, no añadido casos, así que
la cifra no cambia por mi trabajo.
