# Feature 29 — puerta de mutación de frontend

Carril `additional-connectors`, noche del 9 al 10 de septiembre de 2026.
Rama `claude/additional-connectors`, sobre `origin/main` (`5b019343`).

## El punto de partida

Medición previa: **68,51 %** (216 supervivientes + 17 sin cobertura de 740), umbral 80 %.
Faltan **85 muertes**.

| Fichero | Puntuación | Vivos | Sin cobertura | Total |
|---|---|---|---|---|
| `src/gitlab-connector.tsx` | 64,6 % | 97 | 5 | 288 |
| `src/gitlab-connector-client.ts` | 66,0 % | 88 | 11 | 291 |
| `src/connectors-catalog.tsx` | 76,6 % | 17 | 1 | 77 |
| `src/connectors-catalog-client.ts` | 83,3 % | 14 | 0 | 84 |

Fuentes cruzadas: `progress/prediccion_huecos_frontend.md` (sección `29-conectores`, causas)
y `frontend/reports/mutation-additional-connectors/mutation.json` (cuenta exacta, línea y
reemplazo de cada superviviente).

## Cómo se acredita cada racimo

No se ejecuta Stryker (hay otra campaña corriendo). Cada mutante se aplica **al fichero de
producción real** con `scripts/verificar-mutantes-additional-connectors.mjs <racimo>`, se
ejecuta `pnpm --dir frontend exec vitest run src/gitlab-connector src/connectors-catalog` y se
restaura el fichero. El veredicto queda en
`progress/verificacion_mutantes_additional_connectors<racimo>.json`.

Producción **no se toca** para matar mutantes. `git diff` sobre `frontend/src/*.tsx`,
`frontend/src/*-client.ts` y `backend/` queda vacío al terminar.

---

## Racimo 1 — los 17 sin cobertura

«Sin cobertura» = ninguna prueba ejecuta esa rama. Antes de escribir el oráculo se ha leído qué
hace la rama; abajo, lo que se ha encontrado.

### 1.1 `decodeFailure` del cliente de GitLab (9 mutantes, `gitlab-connector-client.ts:112-116`)

Causa: **el estado «conectado pero roto» no se decodificaba nunca**. Las dos fixtures del
cliente (`connected`, `notConnected`) llevaban `lastError: null`, así que `decodeFailure` salía
siempre por el `return null` de la 110 y las líneas 111-117 no las ejecutaba nadie.

Comprobado contra el backend antes de escribir la fixture: `GitlabConnectionView` conserva los
cinco campos de la conexión cuando el estado es `error` y añade `lastError`
(`ErrorResponse(code, at)`), así que la fixture `broken` es la forma que el servidor devuelve
de verdad, no una inventada.

Oráculos nuevos (`gitlab-connector-client.test.ts`):

- `@s8 decodes a connection that is broken, keeping the code and the instant of the failure`
- `@s5 refuses a lastError that smuggles the provider's words in a third field`
- `@s8 refuses a lastError with no code, which would leave «Error» without a reason`
- `@s8 refuses a lastError whose instant is not one`

El de `@s5` es el que faltaba de verdad: la propiedad «`lastError` nunca lleva texto del
proveedor» estaba probada en el cliente del catálogo pero **no** en el de GitLab, que es el que
decodifica la respuesta por la que se colaría un `invalid_token: glpat-…`.

### 1.2 El recibo en curso (2 mutantes, `gitlab-connector-client.ts:167`)

Causa: las dos fixtures de recibo eran `completed`; un recibo `running` no lo decodificaba
nadie, y el contrato lo da por existente (@s27: «un recibo de gitlab en status running»; @s28:
un running abandonado pasa a `failed` con `errorCode INTERRUPTED`).

Oráculo: `@s30 reads back an import still running, with no ending and no error`.

### 1.3 «Cancelar» de la confirmación de desconexión (2 mutantes, `gitlab-connector.tsx:368`)

Causa: **ninguna prueba pulsaba «Cancelar»**. Es un camino de producto declarado por @s35 y sin
recorrer: su `onClick` podía no cerrar nada y la suite seguía verde.

Oráculo: `@s35 cancelling the confirmation closes it and disconnects nothing`, que además
afirma que no sale ningún `DELETE` — cancelar no puede desconectar.

### 1.4 El campo del token marcado por el servidor (1 mutante, `gitlab-connector.tsx:409`)

Causa: `aria-invalid` del campo `token` nunca llegaba a ser `true` en ninguna prueba; sólo se
probaba que **no** lo fuera. @s34 promete señalar «el campo afectado», y sólo se verificaba
para `projectPath`.

Oráculo: `@s34 marks the token field when the server is the one complaining about the token`.

### 1.5 El texto de reserva del catálogo (1 mutante, `connectors-catalog.tsx:67`)

Causa: ninguna fila traía un código desconocido, así que el `??` no se ejercitaba. Y de las
nueve traducciones sólo `CONNECTION_INVALID` llegaba a pintarse.

Oráculos: una paramétrica de nueve filas (código → texto exacto, y el código crudo ausente),
el caso del código desconocido, y **una prueba derivada del enum**: lee
`backend/.../domain/FeedError.java` y exige que toda constante tenga traducción. Sin ella, una
constante nueva caería en el texto genérico y el propietario de un calendario caído leería «Hay
un problema con esta integración» en vez del motivo. Sigue la regla 2 del reparto: la lista no
se escribe a mano, se deriva de lo que la hace caducar.

### 1.6 Los tres `?? ""` — supervivientes equivalentes, declarados

No se escriben oráculos para ellos porque **no hay ningún camino de ejecución que los
distinga**, y una prueba que no puede fallar es peor que ninguna:

| Mutante | Por qué es inalcanzable |
|---|---|
| `gitlab-connector.tsx:183` `tokenField.current?.value ?? ""` | `submitConnection` sólo se dispara desde el `onSubmit` del formulario que contiene el propio campo; la ref está siempre montada cuando corre. |
| `gitlab-connector.tsx:294` `connection?.projectPath ?? ""` | `replaceToken` sólo es alcanzable desde el panel, que se pinta con `connection` no nulo y `status !== "not_connected"`; para esos dos estados el decodificador exige `nonEmpty(projectPath)`. |
| `gitlab-connector.tsx:329` `connection.tokenHint ?? ""` | Mismo guardia: el panel sólo se pinta con `connected`/`error`, y el decodificador exige `nonEmpty(tokenHint)` en ambos. |

Los tres son consecuencia de que el decodificador cierre el DTO: el `?? ""` es defensa muerta.

### Acreditación del rojo (17 de 17 mueren)

`node scripts/verificar-mutantes-additional-connectors.mjs 1` →
`progress/verificacion_mutantes_additional_connectors1.json`.

| Mutante aplicado a producción | Prueba que se pone roja |
|---|---|
| `112 !exact‖!nonEmpty‖!instant → true` | @s8 decodes a connection that is broken… |
| `112 … → false` | @s5 refuses a lastError that smuggles… |
| `112 (A‖B) && C` | @s5 refuses a lastError that smuggles… |
| `112 (A‖B) → false` | @s5 refuses a lastError that smuggles… |
| `112 A && B` | @s5 refuses a lastError that smuggles… |
| `112 !exact → exact` | @s8 decodes a connection that is broken… |
| `113 !nonEmpty → nonEmpty` | @s8 decodes a connection that is broken… |
| `114 !instant → instant` | @s8 decodes a connection that is broken… |
| `116 throw → ;` | @s5 refuses a lastError that smuggles… |
| `167 errorCode !== null → true` | @s30 reads back an import still running… |
| `167 errorCode === null` | @s30 reads back an import still running… |
| `368 setConfirming(false) → true` | @s35 cancelling the confirmation… |
| `368 onClick → () => undefined` | @s35 cancelling the confirmation… |
| `409 aria-invalid token → false` | @s34 marks the token field… |
| `67 texto de reserva → ""` | @s33 falls back to a plain explanation… |
| `54 FEED_UNREACHABLE → ""` | @s33 shows FEED_UNREACHABLE as words… |
| `FeedError + FEED_NUEVO` (rotura en el backend) | @s33 translates every failure the external calendar can report |

**Previsión del racimo: 17 mutantes muertos** (14 sin cobertura + 3 que ya estaban vivos y
caen de paso: los dos de «Cancelar» cuentan como sin cobertura, y `FEED_UNREACHABLE` estaba
marcado `Timeout`). 3 declarados equivalentes.

---

## Racimo 2 — la tabla de rechazo de `decodeConnection` (31 mutantes)

Causa común: **ninguna prueba falsificaba las guardas del decodificador**. La fixture
`notConnected` traía los siete campos nulos a la vez, así que con todos los operandos en falso
cambiar cualquier `||` por `&&` daba el mismo resultado; y la fixture `connected` era siempre
válida, así que `nonEmpty`, `counter` e `identifier` sólo se ejercían por su rama verdadera.

### 2.1 La cadena `absent` (19 mutantes, líneas 130-136)

Siete `||` encadenados: siete nodos con su `ConditionalExpression`, seis operandos y seis
`LogicalOperator`. Una paramétrica de siete filas —`not_connected` con exactamente un campo no
nulo— los mata todos:

`@s8 refuses a not_connected row still dragging <campo> from the previous connection`.

Es la guarda que sostiene @s32 y @s37: una fila sin conexión que arrastre el `projectPath` o el
`tokenHint` de la conexión anterior se aceptaba hoy sin que nada se pusiera rojo. La pantalla no
lo pintaría —el panel exige `status !== "not_connected"`— pero el decodificador es la única
guarda que hay, y era un adorno.

Los 19 se generan en el verificador (`absentChainMutants()`) en vez de escribirse a mano: con
siete cláusulas, escribir 19 anclas a mano es invitar a olvidar una.

### 2.2 Las guardas de la rama con conexión (12 mutantes)

| Guarda | Oráculo nuevo | Mata |
|---|---|---|
| `nonEmpty` (106) | ruta vacía y pista vacía se rechazan | 2 |
| `counter`/`isCount` (88, 94) | `projectId` y `version` no enteros se rechazan | 3 |
| `identifier` (99, 100) | id del recibo en mayúsculas, e id con forma de UUID que no lo es | 2 |
| `lastActivityAt` (142) | conexión sin usar todavía se acepta; «ayer» se rechaza | 3 |
| `decodeFailure` (110) y lista blanca de estados (124) | ya los mata la fixture `broken` del racimo 1 | 2 |

La pista vacía tiene consecuencia visible: el panel enseñaría «••••» y nada más, una pista que
no distingue ninguna cuenta de ninguna otra.

### Equivalentes declarados en este racimo

| Mutante | Por qué no puede matarse |
|---|---|
| `88:10-35 CE typeof value === "number" → true` | `Number.isInteger(x)` sólo devuelve `true` para números, así que `true && Number.isInteger(v)` es exactamente `typeof v === "number" && Number.isInteger(v)`. |
| `101:5 value.length === 36 → true` | La expresión regular de `uuid()` está anclada (`^…$`) y sólo casa cadenas de exactamente 36 caracteres; la comprobación de longitud es redundante. |

### Acreditación del rojo (31 de 31 mueren)

`node scripts/verificar-mutantes-additional-connectors.mjs 2` →
`progress/verificacion_mutantes_additional_connectors2.json`, con la prueba que cae por cada
mutante aplicado a `frontend/src/gitlab-connector-client.ts`.

**Previsión acumulada: 46 mutantes muertos** (15 del racimo 1 + 2 de propina + 29 nuevos).

---

## Racimo 3 — la tabla de rechazo del recibo (20 mutantes)

Causa común: **las dos fixtures de recibo eran `completed` y válidas**. Los tres estados que el
contrato admite no se recorrían, y ninguna prueba cruzaba las fronteras de `finishedAt`.

| Oráculo nuevo | Qué fija del contrato | Mata |
|---|---|---|
| `@s27 refuses a running receipt that already carries an error` | sólo un recibo en curso carece de error | 3 |
| `@s27 refuses a running receipt that already has an ending` | sólo un recibo en curso carece de final | 2 |
| `@s21 accepts a failed receipt with the code that explains it` | @s21: `failed` + `errorCode` + `finishedAt` no nulo | 4 |
| `@s15 refuses a receipt whose errorCode is an empty string` | un código vacío no explica nada | 1 |
| `@s16 refuses a receipt whose truncated is not a yes or a no` | `truncated` es booleano, no «sí» | 1 |
| `@s15 refuses a receipt that ends before it starts` | el orden de los dos instantes | 4 |
| `@s15 accepts a receipt that ends in the very microsecond it started` | la frontera: `<`, no `<=` | 1 |
| `@s15 refuses a receipt carrying a thirteenth field` | los doce campos exactos de @s15 | 2 |
| `@s30 turns a receipt that is not there into the typed error…` | @s30: el 404 `IMPORT_NOT_FOUND` | 1 |
| (del racimo 1) `@s30 reads back an import still running…` | recibo en curso | +1 |

El último merece nota: `readGitlabImport` sólo se probaba con un 200. Con la guarda de estado
relajada, un 404 dejaba de convertirse en `GitlabConnectorError` y salía como «Confirmación
incompatible» — la pantalla perdería el código con el que decide qué ofrecerle al propietario.

### Acreditación del rojo (20 de 20 mueren)

`node scripts/verificar-mutantes-additional-connectors.mjs 3` →
`progress/verificacion_mutantes_additional_connectors3.json`.

**Previsión acumulada: 66 mutantes muertos.**

---

## Racimo 4 — abortos, cabeceras, el error tipado y el mapa de campos (32 mutantes)

### 4.1 Las tres paradas de aborto de cada llamada (14 mutantes)

Causa común: las cinco funciones del cliente tienen **tres paradas** de `throwIfAborted()` —antes
de pedir, con la respuesta en la mano y con el cuerpo leído— y ninguna prueba distinguía una de
otra. Bastaba con que la promesa acabara rechazando, y eso lo consigue cualquiera de las tres:
quitar la primera dejaba que la segunda rechazara igual.

Los oráculos nuevos afirman **qué no llegó a pasar** en cada parada, que es lo que el aborto
promete:

- `@s37 <llamada> asks the server for nothing when the caller already aborted` → `fetch` no se
  llamó (mata la parada 1 de las cinco).
- `@s37 <llamada> does not read the body of a response that landed after the abort` → el `json()`
  no se llamó (mata la parada 2 de las cinco).
- `@s37 <llamada> decodes nothing when the abort lands while the body is being read` (mata la
  parada 3 de las cuatro que leen cuerpo).

Y dos más para la señal: `{ signal }` vaciado dejaba la petición sin cancelar, y ninguna prueba
del cliente miraba que la señal viajara. Sin ella, salir de la pantalla no cancela nada.

### 4.2 Las cabeceras (4 mutantes)

`Content-Type: application/json` no lo afirmaba nadie en el PUT ni en el POST. Dos líneas en las
pruebas que ya leen `options.headers` para el CSRF.

### 4.3 `GitlabConnectorError`, su nombre y su mensaje (6 mutantes)

Las pruebas comprobaban `.code`, `.retryAfterSeconds`, `.importId` e `instanceof`, nunca
`.message` ni `.name`. Un error con el mensaje vacío no dice nada en un volcado ni en el log del
navegador, que es lo único que se ve cuando el error escapa de la pantalla.

### 4.4 El mapa campo → código (6 mutantes)

El único cuerpo con `errors` que se probaba traía **una** entrada bien formada: las tres
condiciones del filtro se cumplían a la vez, así que cambiar cualquier `&&` por `||` daba el
mismo resultado. Dos oráculos: dos entradas válidas marcan los dos campos, y una lista con
basura (un `null`, un `field` vacío, un `code` vacío) deja fuera exactamente esa basura.

### Equivalente declarado

`76:7 typeof entry === "object" → true`: para distinguirlo haría falta una entrada **no objeto**
con `.field` y `.code` no vacíos. El cuerpo viene de `response.json()`, y ningún valor JSON
cumple eso (una cadena o un número no tienen `.field`). No hay prueba que pueda matarlo.

### Acreditación del rojo (32 de 32 mueren)

`node scripts/verificar-mutantes-additional-connectors.mjs 4` →
`progress/verificacion_mutantes_additional_connectors4.json`. Las catorce paradas de aborto se
generan del propio código fuente (`abortMutants()`), no se escriben a mano.

**Previsión acumulada: 98 mutantes muertos** — por encima de las 85 que pide el umbral.
Los racimos siguientes son margen, y van al fichero donde la predicción marcaba defecto.
