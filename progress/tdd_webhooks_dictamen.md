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
