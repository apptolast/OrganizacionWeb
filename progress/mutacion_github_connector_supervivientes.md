# Feature 27 (conector GitHub) — caza de supervivientes de mutación

Punto de partida medido por el coordinador: **69,44 %** (umbral 80). 409
muertos, **175 supervivientes**, 5 sin cobertura. Por fichero:
`github-connector.tsx` 66,86 % (108 supervivientes, 5 sin cobertura),
`github-connector-client.ts` 73,17 % (66), `integrations-index.tsx` 50 % (1).

Informe leído:
`frontend/reports/mutation-github-connector/mutation.json`.

Regla respetada en todo momento: **no se toca producción** y no se relaja
ninguna prueba existente. Lo que faltaba eran aserciones.

## Racimos atacados, en `github-connector-client.ts`

### Racimo 1 — disciplina de cancelación (13 supervivientes)

Líneas 137, 141, 149, 156, 159, 164, 169, 177, 184, 187, 195, 197 y 200, todas
`[CallExpression] signal.throwIfAborted(); -> ;`.

Diagnóstico: las cinco operaciones exportadas consultan la señal **tres veces**
—antes de tocar la red, al volver la respuesta y tras leer el cuerpo— y la
suite sólo tenía un oráculo, `@s41 stops as soon as the caller aborts`, que
cubría la **primera** llamada de `readGithubConnection`. Las otras doce podían
borrarse sin que nada fallara.

Oráculo añadido: una tabla de las cinco operaciones y, por cada una, tres
pruebas:

1. señal ya abortada → rechaza y `fetch` **no** se llama;
2. el aborto llega en vuelo (el propio doble de `fetch` aborta antes de
   resolver) → rechaza, con `fetch` llamado exactamente una vez;
3. el aborto llega mientras se lee el cuerpo (un `Response` cuyo `json()`
   aborta) → rechaza. No aplica a `disconnectGithub`, que no decodifica.

**Previsión: mueren los 13.**

### Racimo 2 — opciones de la petición (6 supervivientes)

Líneas 136, 153, 181 y 196: `[ObjectLiteral] { signal } -> {}`,
`{ "Content-Type": "application/json" } -> {}` y el `[StringLiteral]` del
propio tipo de contenido.

Diagnóstico: nadie miraba el segundo argumento de `fetch`.

Oráculo añadido: se afirma que cada una de las cinco operaciones entrega **su**
`signal` al transporte (identidad, no sólo presencia) y que las dos escrituras
con cuerpo declaran `Content-Type: application/json`.

**Previsión: mueren los 6.**

### Racimo 3 — `ConnectorError` y sus contadores (≈14 supervivientes)

Líneas 47, 48, 49 (`EqualityOperator`, `ConditionalExpression` a `true` y a
`false`, y tres `StringLiteral` a `""`), 59 (los 6 de `counter`: los dos
`LogicalOperator`, los `ConditionalExpression` y el `value >= 0`) y 126/128
(`ArrowFunction` del `catch` y el `LogicalOperator`/`ConditionalExpression` del
cuerpo ilegible).

Diagnóstico: se comprobaba que el error se lanzaba, no **cómo se normalizaba**.

Oráculo añadido:

- un problema sin `code` utilizable (`{}`, `{code: 7}`, `{code: null}`) da
  `code` **y** `message` iguales a `"CONNECTOR_ERROR"`; con `code` presente,
  ambos lo conservan;
- los cuatro contadores (`retryAfterSeconds`, `created`, `skipped`, `failed`)
  se recorren con `0`, `12`, `-1`, `1.5`, `"3"` y `null`, afirmando que sólo
  sobreviven los enteros no negativos y que `0` **se conserva** (es el caso que
  mata el `value >= 0` invertido y el `ConditionalExpression` a `true`);
- el `importId` del problema se recorre con uuid canónico, uuid en mayúsculas,
  cadena no-uuid y número;
- un cuerpo ilegible (`"no es json"` con `Content-Type` de problema) sigue
  produciendo el error tipado con `code` por defecto y contadores nulos, que es
  lo que mata el `catch(() => null)` y el ternario de la línea 128.

**Previsión: mueren entre 12 y 14** (los dos `StringLiteral` a `""` de la línea
47 son equivalentes entre sí; si alguno resultara equivalente de verdad,
quedaría vivo).

Hallazgo de producción anotado, **no** corregido (fuera de encargo y sin
cambiar producción): `ConnectorError` filtra `importId` con `uuid()`, mientras
que `decodeReceipt` usa `identifier()`, que además exige minúsculas y longitud
36. Por eso un `importId` en mayúsculas **sí** se conserva en el error y **no**
en el recibo. La prueba se escribió contra la conducta real, no contra la
supuesta; si la asimetría no es deliberada, es un defecto para otro dictamen.

## Total previsto

**31–33 mutantes muertos** de los 66 de `github-connector-client.ts`, que
pasaría de 73,17 % a del orden de 86–88 %. Sobre el total de la feature, de
69,44 % a aproximadamente **74–75 %**: **todavía por debajo del umbral 80**,
porque los 108 supervivientes de `github-connector.tsx` siguen intactos.

## Lo que queda, por orden de rentabilidad

1. `github-connector.tsx`, 108 supervivientes y **5 sin cobertura** (líneas 54
   y 265-267, que ninguna prueba ejecuta: ahí hay una rama entera sin visitar).
   Racimos más densos por línea: 271 (5), 93 (4), 106 (4), 137 (4), 208 (3),
   241 (3), 419 (3), 110/160/214/416/474/478 (2 cada una).
2. `github-connector-client.ts`, los ~35 restantes: los validadores
   `identifier` (66-68), `decodeReceipt` (77-91), `isCount` (98), `nonEmpty`
   (101-102) y `decodeConnection` (108-110, con dos mutantes de la expresión
   regular del repositorio). Se atacan con una tabla de cuerpos malformados,
   uno por cláusula, comprobando que cada uno rechaza con «Confirmación
   incompatible».
3. `integrations-index.tsx`, 1 superviviente en la línea 9.

**No se relanzó Stryker**: la campaña tarda unos 17 minutos y no cabía en el
plazo. Las cifras de arriba son previsiones para contrastar contra la próxima
medición, no medidas.

## Verificación hecha

`pnpm --dir frontend exec vitest run src/github-connector-client.test.ts`:
**44/44 en verde** con la producción intacta.

Comprobación manual de que el racimo 1 y el 2 discriminan de verdad: quitando a
mano el segundo `throwIfAborted()` de `readGithubConnection`, el tercero de
`readGithubImport` y el `{ signal }` de `disconnectGithub`, **fallan 4
pruebas**; restaurada la producción, 39/39 en verde.
