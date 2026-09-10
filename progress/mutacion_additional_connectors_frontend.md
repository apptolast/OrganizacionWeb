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

---

## Racimo 5 — la pantalla de GitLab (20 mutantes)

Aquí es donde la predicción marcaba defecto, y donde lo había. Cinco hallazgos de producto que
la suite no podía delatar:

### 5.1 Cualquier fallo de lectura acusaba a la instalación (2 mutantes, líneas 116-117)

Sólo @s29 hacía fallar el GET de la conexión, y con `CONNECTORS_DISABLED`. Con la guarda
relajada, **cualquier** lectura fallida —un 503 pasajero, un cuerpo ilegible— pintaba «Falta
configuración del servidor para usar los conectores» y escondía el formulario: el propietario
sale a molestar a quien administra la instalación por algo que se arregla reintentando.

Oráculo: `@s36 a passing read failure does not accuse the server of missing configuration`.

### 5.2 El panel se pintaba sin conexión (2 mutantes, línea 319)

Con `not_connected` el objeto **no** es nulo, así que `showPanel` ya vale `true` y lo único que
impide pintar el panel es el `!==`. La única aserción que podría pillarlo,
`queryByText("Conectado")`, compara texto exacto y «No conectado» no es «Conectado». Con el
mutante, un propietario sin conexión recibía el panel entero: «No conectado», proyecto vacío,
«••••» e identificador vacío, junto al formulario.

Oráculo: `@s34 a connection that does not exist yet paints no panel to act on`.

### 5.3 El panel viejo sobrevivía al reemplazo del token (3 mutantes, líneas 299 y 319)

Nadie afirmaba la exclusión mutua en esa dirección: mientras se reemplaza el token, el panel
seguía ofreciendo «Importar issues» y «Desconectar» sobre una conexión que está a punto de
cambiar.

Oráculo: `@s35 while the token is being replaced the old panel offers nothing`.

### 5.4 La región viva no callaba (1 mutante, línea 443)

Se afirmaba que contiene «Guardando…» y «Importando», nunca que esté **vacía** cuando no hay
nada en vuelo. Una pantalla que anuncia «Importando issues…» de forma permanente pasaba la
suite entera. Es la misma familia del defecto de la región `aria-live` de esta noche, mirado por
el otro lado.

Oráculo: `@s35 the live region says nothing while nothing is travelling`.

### 5.5 Lo elegido y lo enviado no estaban atados (5 mutantes, líneas 74, 166, 207, 338)

`user.selectOptions` no aparecía en todo el fichero y no había un solo `JSON.parse` del cuerpo
del POST: el servidor doble respondía el recibo se mandara lo que se mandara. La pantalla podía
importar siempre al primer proyecto, o mandar un identificador basura, sin que nada fallara.
Y con la lista de proyectos vacía, el botón «Importar issues» sigue habilitado: sólo la guarda
`!selected` impide un POST con destino vacío, y nadie la ejercía.

Oráculos: `@s35 imports into the project chosen in the selector`, `@s35 imports into the first
open project when the owner chooses none` y `@s35 with no project to import into, the button
sends nothing`. El primero, de paso, es el único que consulta el selector por su etiqueta
(`getByLabelText("Proyecto de destino")`), que ata el `id`/`htmlFor` del control.

### 5.6 El foco robado en cada repintado (3 mutantes, líneas 141, 146, 151)

Las tres banderas se apagan justo antes de mover el foco. Dejarlas encendidas devuelve el foco
al mismo sitio en **cada** render: quien navegue con teclado no podría ni recorrer el selector
de proyectos. Las tres pruebas @s38 que había miran dónde está el foco justo después del cambio
de estado, y ahí las dos versiones coinciden.

Oráculos: tres pruebas que, tras el cambio de estado que mueve el foco, provocan **otro** render
que no debe moverlo y afirman que el foco sigue donde el usuario lo dejó.

### 5.7 El orden de tabulación (3 mutantes, líneas 303, 304, 461)

`tabIndex={-1}` → `+1` mete el `<main>`, el `<h1>` y el recibo delante de todo lo demás. La
prueba no enumera los tres elementos: recoge todo lo que declara `tabindex` y exige que ninguno
sea positivo, así que un contenedor nuevo queda cubierto sin tocarla.

### Equivalentes declarados en este racimo

| Mutante | Por qué no puede matarse |
|---|---|
| `116:9 error instanceof GitlabConnectorError → true` | Para distinguirlo haría falta un error que no sea `GitlabConnectorError` y que además lleve `code === "CONNECTORS_DISABLED"`. Lo que `readGitlabConnection` puede lanzar es el error tipado, `Error("Confirmación incompatible")` o un `TypeError`/`AbortError` de red: ninguno lleva ese `code`. |
| `299` los cuatro mutantes de `!disabled` y `!loading` | `disabled` sólo se enciende en el mismo `catch` que hace `setConnection(null)`, y `loading` sólo es cierto antes de la primera lectura, cuando `connection` todavía es `null`. En los dos casos `Boolean(connection)` ya vale `false`, así que las dos cláusulas son redundantes para `showPanel`. |
| `94, 142, 147, 152` los cuatro `?.focus()` | Las cuatro referencias están montadas cuando su efecto corre: `heading` siempre; `replaceButton` porque `focusReplace` sólo se enciende desde `startImport`, alcanzable únicamente desde el panel; `receiptBox` porque `focusReceipt` se enciende con el recibo, que se pinta en el mismo render. |

### Acreditación del rojo (20 de 20 mueren)

`node scripts/verificar-mutantes-additional-connectors.mjs 5` →
`progress/verificacion_mutantes_additional_connectors5.json`.

**Previsión acumulada: 118 mutantes muertos.**

---

## Racimo 6 — el catálogo y su cliente (19 mutantes)

### 6.1 El aviso vacío (2 mutantes, `connectors-catalog.tsx:106`)

Los dos oráculos @s7 hacían `await screen.findByRole("alert")` y nada más: un
`<p role="alert"></p>` vacío los satisfacía. Es la familia exacta del defecto de la región
`aria-live` de esta noche: un rol de alerta sin texto no anuncia nada, y quien use un lector se
queda delante de un párrafo en blanco sin saber qué ha pasado.

Oráculos: el texto exacto con `STORAGE_UNAVAILABLE` y el genérico cuando el fallo no trae
código.

### 6.2 La limpieza del efecto (2 mutantes, líneas 115-117)

Nadie afirmaba que salir del catálogo abortara su petición: la prueba @s37 desmonta y comprueba
que no hay filas ni ruido, y las dos cosas se cumplen igual con la limpieza vacía. Con ella
vacía, la petición se queda en vuelo hasta que responda.

Oráculo: `@s37 leaving the catalogue aborts the read it had in flight`.

### 6.3 El separador del marcador (1 mutante, línea 146)

`{" "}` vaciado pega el glifo al estado: «●Conectado». Se afirma el texto del párrafo entero.

### 6.4 El orden de tabulación del catálogo (2 mutantes, líneas 122-123)

La misma prueba derivada que en la pantalla de GitLab.

### 6.5 El cliente del catálogo (12 mutantes)

| Oráculo nuevo | Mata |
|---|---|
| el mensaje de `CatalogError` en sus dos ramas | 5 |
| un `lastError` cuyo `code` no es una cadena | 1 |
| una actividad que no es un instante | 1 |
| un catálogo con un séptimo conector | 1 |
| las tres paradas de aborto de la lectura, cada una con lo que promete que no pasará | 3 |
| la señal viaja en la petición | 1 |

### Acreditación del rojo (19 de 19 mueren)

`node scripts/verificar-mutantes-additional-connectors.mjs 6` →
`progress/verificacion_mutantes_additional_connectors6.json`.

**Previsión acumulada: 137 mutantes muertos.**

---

## Racimo 7 — lo que quedaba de la pantalla, con sus defectos (12 mutantes)

| Oráculo nuevo | Defecto que tapaba |
|---|---|
| `@s34 shows neither the form nor the panel until the connection is known` | con `loading` en falso, un propietario ya conectado vería parpadear el formulario «Conectar»: una invitación a reescribir el token que ya tiene |
| `@s35 offers no destination when the list of projects cannot be read` | un destino inventado en el selector cuando la lista no se puede leer |
| `@s37 leaving the screen cancels the read of the projects as well` | la petición de proyectos no pasa por `pending.current`: sólo la limpieza de su efecto la cancela, y nadie lo afirmaba |
| `@s34 does not send the token twice when the form is submitted again in flight` | el botón se deshabilita, pero un envío por teclado no pasa por el botón: doble intro, token enviado dos veces |
| `@s34 sends exactly the token and the path that were typed` | la pantalla podía mandar el token vacío y todo seguía verde |
| `@s36 a new import clears the failure / the result of the previous one` | el recibo viejo o el aviso viejo sobre una importación nueva |
| `@s36 names the seconds of a rate limit…` (+1 aserción) | un límite de peticiones marcaba la conexión como «Error»: un límite no es un token roto |
| `@s35 keeps the connection when the disconnection fails` (+2 aserciones) | la confirmación se quedaba abierta tapando el aviso que explica el fallo |
| `@s35 the disconnection leaves neither the receipt nor the path…` | la ruta del proyecto anterior seguía escrita en el formulario que se reabre — el residuo que @s32 y @s37 prohíben |

Verificación: 12 de 13 mueren. El que sobrevive es `focusReplace = useRef(false) → true`, y con
él sus dos hermanos (`focusReceipt`, `focusHeading`): **equivalentes**, porque el efecto de foco
corre por primera vez con `loading` todavía en cierto, cuando el único elemento montado es el
`<h1>` —que ya tiene el foco por el `useLayoutEffect`— y las otras dos referencias son nulas. La
bandera se consume en ese primer paso, antes de que su botón exista.

## Racimo 8 — los avisos que no dicen nada y los restos de estado (5 mutantes)

Misma familia que el defecto de la noche: un `role="alert"` que aparece sin decir nada.

- El texto de reserva de `describeFailure` no lo afirmaba nadie (la prueba de fallo de red lo
  alcanzaba sin mirarlo).
- Los textos de `STORAGE_UNAVAILABLE` y `VALIDATION_ERROR` se disparaban sin afirmarse: marcar
  un campo en rojo sin una frase que diga qué revisar no explica nada.
- `disabled` arrancando en cierto acusaba a la instalación antes de saber nada.
- Una relectura que falla dejaba de limpiar la conexión que ya no puede confirmar.
- «Actualizar estado» y la desconexión no limpiaban el aviso de lo que falló antes.

Y dos roturas a mano más, para acreditar que dos oráculos nuevos **pueden** fallar: quitar
`heading.current?.focus()` pone roja la prueba de foco de apertura de cada pantalla, y vaciar el
`aria-label="Conexión"` pone roja la prueba del panel — que ahora lo consulta por su nombre
accesible, de modo que las aserciones de ausencia no puedan pasar por el motivo equivocado.

---

## Recuento y previsión

| Racimo | Mutantes muertos |
|---|---|
| 1 · los 17 sin cobertura | 15 |
| 1 · de propina (`110`, `124`, verificados en el racimo 2) | 2 |
| 2 · tabla de rechazo de `decodeConnection` | 29 |
| 3 · tabla de rechazo del recibo | 20 |
| 4 · abortos, cabeceras, error tipado y mapa de campos | 32 |
| 5 · la pantalla de GitLab | 20 |
| 6 · el catálogo y su cliente | 19 |
| 7 · lo que quedaba de la pantalla | 12 |
| 8 · los avisos y los restos de estado | 5 |
| **Total** | **154** |

Hacían falta **85**. La previsión queda en **(507 + 154) / 740 = 661 / 740 ≈ 89 %**, con 69
muertes de margen sobre el umbral.

Por fichero, previsto:

| Fichero | Antes | Muertes nuevas | Previsión |
|---|---|---|---|
| `gitlab-connector-client.ts` | 66,0 % | 94 | ≈ 98 % (quedan los 5 equivalentes declarados) |
| `connectors-catalog-client.ts` | 83,3 % | 12 | ≈ 98 % |
| `connectors-catalog.tsx` | 76,6 % | 7 | ≈ 86 % |
| `gitlab-connector.tsx` | 64,6 % | 40 | ≈ 78 % |

`gitlab-connector.tsx` se queda por debajo del 80 **por sí solo**, y no por falta de oráculos:
**29 de sus supervivientes son el racimo de la escritura tardía** (`live(controller)` y
`pending.current === controller`, líneas 103-272). Bajo React 19 un `setState` sobre un árbol
desmontado es un no-op silencioso, y **no existe ningún camino en el que una petición se aborte
con la pantalla viva**, porque `pending.current` se sobrescribe sin abortar la anterior. Sin
cambiar producción no hay prueba que pueda distinguirlos, y el encargo prohíbe cambiarla. Quedan
declarados aquí, con su causa, como pide la sección 7 del dictamen. El umbral que mide la puerta
es el agregado de la feature, que queda en el 89 %.

## Defectos anotados y fuera de alcance

`pending.current = controller` se asigna **sin abortar la anterior** (líneas 107, 180, 212, 253).
Dos operaciones solapadas escriben las dos, y gana la que llega última, no la que el propietario
pidió última: una importación lenta puede repintar «Creadas/Omitidas/Fallidas» sobre una pantalla
ya desconectada. Arreglarlo es un cambio de producción —abortar la petición anterior al empezar
una nueva— y de paso haría matables esos 29 mutantes. Queda anotado para quien decida el
contrato; aquí no se toca.

Segundo, menor: `readGitlabImport` está exportada y **no la llama nadie en producción**, sólo su
prueba. Una importación que el servidor devuelva en `running` no tiene hoy forma de completarse
en pantalla: el componente `Receipt` pinta cuatro cifras y nunca el estado, así que un recibo en
curso se presenta idéntico a una importación terminada sin resultados.

## Cómo reproducir la acreditación

```
node scripts/verificar-mutantes-additional-connectors.mjs <racimo>   # 1..8
pnpm --dir frontend exec vitest run src/gitlab-connector src/connectors-catalog
```

179 pruebas verdes (eran 83). `git diff` sobre los cuatro ficheros de producción y sobre
`backend/` queda vacío: no se ha tocado producción para matar un solo mutante.

**Aviso sobre el verificador**: sólo ejecuta las cinco suites de la feature, así que un
«SOBREVIVE» suyo no prueba que el mutante sobreviva a la campaña completa —otra suite puede
matarlo—. Un «MUERE», en cambio, es concluyente.
