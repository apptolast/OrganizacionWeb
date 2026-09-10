# Mutación de frontend — feature 30 (automatizaciones)

Punto de partida medido por el orquestador: **52,74 %** frente a un umbral de 80.

| Fichero | Puntuación | Supervivientes | Sin cobertura |
|---|---|---|---|
| `src/automations.tsx` | 47,36 % | 208 | 51 |
| `src/automations-api.ts` | 58,22 % | 129 | 26 |
| `src/workspace.tsx` | 100 % | 0 | 0 |

Denominador total (los cuatro ficheros del ámbito): 876 mutantes vivos o muertos,
462 muertos. Para llegar al 80 % hacen falta 701 muertos, es decir **239 muertes
nuevas** sobre los 414 que quedan.

Método de acreditación: no se ejecuta Stryker. Cada mutante que digo matar se
aplica **al fichero de producción real** con
`scripts/verificar-mutantes-automations.mjs`, se corre `vitest run src/automations`,
se comprueba que alguna prueba cae y se restaura el fichero. El veredicto de cada
uno queda en `progress/verificacion_mutantes_automations.json`.

---

## Racimo 1 — el cliente HTTP nunca comprobó su propio esquema (`automations-api.ts`)

**Causa común.** Los seis predicados de forma (`uuid`, `action`, `automation`,
`preview`, `match`, `run`) son cadenas largas de `&&`. Las pruebas existentes sólo
alimentaban **un** cuerpo válido y **un** cuerpo roto por función, de modo que
ningún conjunto individual era jamás el único falso: cambiar cualquiera de ellos
por `true` no rompía nada. Lo mismo con los tres sobres (`{ items }`,
`{ evaluatedEvents, matches }`, `{ items, nextCursor }`).

Además, **la rama `NOTIFY_WEBHOOK` no estaba ejercida en absoluto** (los 26
mutantes sin cobertura del fichero están todos ahí y en la condición del
`automation()`): el cliente jamás había aceptado ni rechazado una regla de aviso
al webhook, ni una previsualización de aviso, pese a que el contrato las publica
(@s7 del `.feature`).

**Oráculo que faltaba.** Una tabla por predicado que rompe **un solo campo cada
vez** y exige rechazo, más una tabla gemela de formas válidas que exige
aceptación. Seis pruebas nuevas:

- `@s37 refuses a rule that breaks any single part of the closed shape` — 25 filas.
- `@s37 accepts every shape that the contract does publish` — 6 filas (incluida la
  regla de webhook, la condición no nula, el criterio con texto y los minutos nulos).
- `@s37 refuses an envelope that is not exactly { items }` — 4 filas.
- `@s39 refuses a coincidence that breaks any single part of its shape` — 16 filas.
- `@s39 accepts the coincidences the contract publishes` — 4 filas.
- `@s39 refuses a simulation whose envelope or list is not the published one` — 5 filas.
- `@s41 refuses an execution that breaks any single part of its shape` — 10 filas.
- `@s41 accepts the three states and the nulls where they apply` — 2 filas.
- `@s41 refuses a page whose envelope is not exactly { items, nextCursor }` — 5 filas.

Dos filas merecen mención porque no son huecos mecánicos sino agujeros reales del
guardián de tipos:

- **Un `id` que sólo parece un UUID al convertirlo a texto** (`[uuid]`, una lista
  de un elemento) pasaba si se eliminaba la comprobación `typeof value === "string"`,
  porque `RegExp.test` coacciona su argumento. La fila `["id que sólo parece un
  UUID al convertirlo", { ...rule, id: [RULE] }]` fija que no basta con parecerlo.
- **Una acción con la forma exacta de `CREATE_TASK` pero con `type` distinto** se
  aceptaba como tarea si se relajaba la comparación del tipo. Igual con la
  previsualización. Las filas «acción con forma de CREATE_TASK y otro tipo» y
  «previsualización con forma de tarea y otro tipo» lo fijan.
- **Una regla con basura pegada delante o detrás del UUID** (`zz<uuid>`,
  `<uuid>zz`) pasaba si se caía cualquiera de las dos anclas de la expresión
  regular. Dos filas la fijan por separado.

**Previsión (no medida): 40 mutantes verificados uno a uno, todos MUEREN.**
Verificación completa en `progress/verificacion_mutantes_automations.json`
(`TOTAL: 40 mueren de 40`). El racimo cubre además, por arrastre, la mayoría de
los `ConditionalExpression` y `LogicalOperator` hermanos de esas mismas cadenas
que Stryker cuenta por separado: la previsión razonada para el racimo completo
está entre **95 y 115 mutantes** de `automations-api.ts` (los ~155 vivos menos los
de mensajes, cabeceras y abortos, que van en los racimos 2 y 3).

`git diff` sobre producción: vacío.

---

## Racimo 2 — el cliente HTTP tampoco comprobó su conversación (`automations-api.ts`)

**Causa común.** Las pruebas del cliente miraban el **valor devuelto** pero nunca
**la petición** ni **el cuerpo de error**. De ahí tres familias enteras vivas:

1. **La petición**: nadie afirmaba la URL, el método, el `Accept`, el
   `Content-Type`, el cuerpo JSON, el `If-Match` ni el `signal`. Cambiar el objeto
   de opciones entero por `{}` no rompía nada: el cliente podía dejar de propagar
   la señal de aborto y las pruebas seguían verdes.
2. **El cuerpo de error**: `failure()` recorre `body.errors` con siete guardas
   (`body`, `typeof body === "object"`, `"errors" in body`, `Array.isArray`,
   `error`, `typeof error === "object"`, `"field" in error`, tipos de `field` y
   `code`, y `length > 0`). Sólo se alimentaba **un** cuerpo bien formado, así que
   ninguna guarda era jamás la única que decidía.
3. **Los abortos**: los seis `signal.throwIfAborted()` podían borrarse sin que
   nadie se quejara, salvo el primero. Es exactamente lo que @s43 exige («la
   respuesta tardía no modifica la interfaz visible»).

**Oráculos que faltaban.** Once pruebas nuevas y una completada:

- `@s38 gives each failure of its own a name and a message for the editor`.
- `@s38 names the field for every code the contract publishes` — completada: le
  faltaban `UNKNOWN_PLACEHOLDER`, `UNKNOWN_EVENT_TYPE`, `TOO_LONG`, `REQUIRED` y
  `OUT_OF_RANGE`, y además no exigía que la promesa rechazara (si el cliente
  hubiera resuelto, el `catch` no se ejecutaba y la prueba pasaba en vacío).
- `@s38 surfaces the response itself when the error body is not the published list`
  — 12 cuerpos deformes, incluido uno con un elemento de texto que hace estallar
  al operador `in` si se relaja la guarda de tipo.
- `@s36 only reads field errors out of a 400 or a 422` — un 409, un 500 y un 503
  con lista de errores siguen siendo la respuesta cruda.
- `@s38 keeps the first message when the server repeats a field`.
- `@s37 asks for JSON and carries the caller's signal on every read`.
- `@s41 asks for the page of runs with its cursor escaped, its Accept and its signal`.
- `@s40 sends the draft as JSON and only carries If-Match when there is a version`
  — el POST de creación **no** puede llevar `If-Match`, y el de simulación tampoco.
- `@s40 returns the created rule only on a 201 that carries the closed shape`.
- `@s14 names the rule in the delete URL and surfaces a refusal`.
- `@s43 throws instead of handing back a body when the signal was cut mid-flight`
  — seis operaciones, con un `fetch` que corta la señal antes de responder.
- `@s43 never touches the network when the signal was already cut` — cinco
  operaciones; antes sólo se comprobaba la lectura.

**Previsión (no medida): los 74 mutantes verificados a mano MUEREN los 74**
(`TOTAL: 74 mueren de 74` entre las dos pasadas). Con los hermanos que Stryker
cuenta por separado en las mismas cadenas, la previsión razonada para
`automations-api.ts` es de **135 a 150 de los 155** vivos, es decir pasar de
58,22 % a ~95 %.

`git diff` sobre producción: vacío.

---

## Racimo 3 — las tablas de la vista se comprobaban por muestreo (`automations.tsx`)

**Causa común.** Cuatro tablas de traducción (los doce disparadores, los cinco
motivos de fallo, los cuatro marcadores de ejemplo y los cinco controles de
`controlIdOf`) se afirmaban con **una o dos filas** cada una. Vaciar cualquiera de
las demás no rompía nada: son 22 mutantes de cadena que sólo hacen falta cuando
alguien mira la fila concreta.

**Oráculos que faltaban.** Cinco pruebas nuevas, todas dirigidas por tabla:

- `@s37 writes each of the twelve published triggers in readable Spanish` — doce
  reglas, una por tipo de evento, cada fila con su etiqueta.
- `@s38 offers the twelve triggers in the editor, in the published order` — la
  lista completa de opciones del `select`, en orden, más el valor por omisión y la
  lista de proyectos con «Cualquiera» delante. La afirmación es sobre **la lista
  entera**, no sobre posiciones: no caduca cuando se añada un evento nuevo, falla.
- `@s39 explains every failure the simulation can foresee and repeats the code it
  does not know` — los cinco motivos publicados y uno inventado, que debe salir
  con su código tal cual.
- `@s38 replaces the four markers with their sample values and lists them verbatim`
  — los cuatro marcadores a la vez, más uno inventado que ha de quedar literal, y
  el texto exacto de la ayuda con sus comas.
- `@s38 focuses the control that owns the field the server complained about` —
  los cinco campos que el contrato nombra y uno desconocido, que cae al control
  del nombre.

**Previsión (no medida): 23 mutantes verificados, todos MUEREN**
(`TOTAL: 97 mueren de 97` acumulado con los racimos 1 y 2). Previsión razonada del
racimo, contando hermanos: **25 a 30**.

`git diff` sobre producción: vacío.
