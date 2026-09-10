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

---

## Racimo 4 — nadie miraba lo que el editor manda (`automations.tsx`)

**Causa común.** Las pruebas de la vista contaban las peticiones («exactamente
una POST») pero **jamás abrían el cuerpo**. Por eso vivían enteros `blank()`,
`draftOf()` y la mitad de `editingOf()`: el editor podía mandar el disparador
equivocado, perder el criterio, convertir los minutos en `null` o mandar
`enabled: true` sobre una regla desactivada, y la barra seguía verde.

El arnés de pruebas ni siquiera **guardaba** el cuerpo de la petición: había que
añadirlo (`calls[].body`) antes de poder afirmar nada.

**Oráculos que faltaban.** Siete pruebas nuevas:

- `@s40 sends a brand new rule exactly as the editor shows it` — primero afirma los
  cinco valores por omisión **en pantalla** y después el cuerpo JSON **entero**
  (`toEqual`, no `toMatchObject`): nombre, `enabled: true`, disparador, `condition:
  null` y la acción completa con `criterionTemplate` y `estimatedMinutes` nulos.
- `@s40 sends the trigger, the condition and the criterion the owner picked` — los
  tres `onChange` que nadie había ejercido nunca (eran mutantes **sin cobertura**).
- `@s40 gives back untouched the parts of the rule the editor does not show` —
  renombrar una regla devuelve intactos el destino, la plantilla y los 30 minutos
  estimados, que el editor **no muestra por ningún sitio**. Con dos reglas en la
  lista, para que sustituir y añadir se distingan.
- `@s40 carries the condition and the criterion of the rule it is editing`.
- `@s37 adds the new rule to the list instead of replacing it`.
- `@s37 still lets a rule be written when the projects could not be read` — desde
  el estado vacío y desde la lista. Sin esta prueba, quitar el `?.` de
  `projects[0]?.id` no rompía nada: la página **reventaba** con un `TypeError` en
  cuanto la lectura de proyectos fallaba y nadie se enteraba.
- `@s37 shows the identifier of a destination it cannot name`.

**Previsión (no medida): 37 mutantes verificados, 36 MUEREN.** El único que
sobrevive, `blank().estimatedMinutes: ""` → `"Stryker was here!"`, es **equivalente**:
no hay ningún control para los minutos estimados en el editor, y `Number("lo que
sea")` es `NaN`, que `JSON.stringify` serializa como `null`, exactamente igual que
la cadena vacía. Queda anotado, no perseguido.

Previsión razonada del racimo, contando hermanos: **55 a 65**.

`git diff` sobre producción: vacío.

---

## Racimo 5 — las reglas de aviso al webhook no existían para la vista

**DEFECTO DE PRODUCTO ENCONTRADO Y ARREGLADO.**

Los 51 mutantes sin cobertura de `automations.tsx` se concentraban en las ramas de
`editingOf()` y del renderizado que sólo se ejecutan cuando la acción de la regla
**no** es `CREATE_TASK`. Nadie había abierto nunca una regla de webhook en la
interfaz. Al escribir la primera prueba salió esto:

> **Renombrar una regla de aviso al webhook la convertía en una regla de crear
> tareas y perdía el endpoint.**

`draftOf()` componía siempre `action: { type: "CREATE_TASK", ... }` con los campos
que `editingOf()` había dejado vacíos para una regla de webhook. El cuerpo que
salía por el PUT era:

```
action: { type: "CREATE_TASK", projectId: "", titleTemplate: "",
          criterionTemplate: null, estimatedMinutes: null }
```

El `endpointId` desaparecía. Contra un servidor que valida (@s7 exige un endpoint
propio y activo) esto acaba en un 422 sobre `action.projectId`, que `controlIdOf`
no sabe situar y manda al control del nombre: el propietario ve un error absurdo
en el campo equivocado y no entiende por qué no puede renombrar su regla. Y si el
servidor fuera más laxo, la regla quedaría convertida en otra cosa.

**Rojo demostrado.** La prueba `@s37 keeps the endpoint of a webhook rule when only
its name changes` falla contra la producción de origen con:

```
-     "endpointId": "88888888-8888-4888-8888-888888888888",
+     "projectId": "",
```

**Arreglo.** Se extrae `actionOf(editing)`: este editor **sólo compone acciones
`CREATE_TASK`**, así que la acción de cualquier otra regla vuelve intacta. Es el
mismo criterio que ya seguía `toDraft()` para el interruptor, que sí conservaba la
acción; el camino de guardar era el único que la destruía.

**Queda anotado, fuera de este encargo:** el editor sigue mostrando «Título de la
tarea» y «Criterio de la tarea» vacíos al abrir una regla de webhook, y ahora los
ignora en vez de destruir la regla. Lo correcto sería no ofrecer esos controles
para una acción que no crea tareas, o mostrar el endpoint. Es un cambio de
interfaz que no está en el contrato de @s38 y no lo hago de paso.

**Oráculos nuevos.** Tres pruebas:

- `@s37 keeps the endpoint of a webhook rule when only its name changes` — más la
  fila de la lista, que dice «Webhook» como destino y no un nombre de proyecto.
- `@s40 keeps the endpoint of a webhook rule when the switch is flipped`.
- `@s39 previews a webhook notice instead of a resolved task title` — «Aviso al
  webhook» y ni rastro de «Fallaría».

Y se refuerza `@s40 carries the condition and the criterion of the rule it is
editing`: ahora **cambia** el criterio antes de guardar, para que se distinga
componer la acción de devolverla tal cual.

**Previsión (no medida): 10 mutantes verificados, los 10 MUEREN.** Previsión
razonada del racimo, contando las ramas de `editingOf` sin cobertura que quedan
ejercidas: **30 a 40**.

---

## Racimo 6 — los cinco avisos de error y las guardas de respuesta tardía

**Causa común.** De los cinco caminos de error de la vista, **cuatro no se habían
ejercido jamás**: el guardado que falla, la simulación que falla, la recarga de la
versión actual que falla y el historial que falla. Los cuatro `setNotice` eran
mutantes **sin cobertura**, y con ellos se iban los `catch` enteros y sus guardas.

La otra mitad del racimo son las guardas de @s43. Con tres escrituras compartiendo
un único `writeRequest`, cualquiera cancela a la anterior; la guarda
`X.current !== controller` del `catch` es lo que impide que la cancelada anuncie un
error que el propietario no ha provocado. Nadie la comprobaba.

**Oráculos que faltaban.** Once pruebas nuevas y una reforzada:

- `@s40 says so when the rule could not be saved, and keeps the draft` — y que **no**
  se confunda con el conflicto de versión: si la rama del 412 se abriera para
  cualquier error, saldría «Otra pestaña cambió esta regla» ante un 503.
- `@s39 says so when the simulation could not be run`.
- `@s39 pins a field error of the simulation to its control and focuses it` — la
  simulación también devuelve errores por campo (@s38) y ese camino estaba muerto.
- `@s40 says so when the current version could not be loaded`.
- `@s41 says so when the history could not be loaded`.
- `@s37 keeps offering the retry when the second read fails too` — y de paso fija
  que el estado de carga y el estado vacío no conviven con el error.
- `@s43 drops a save that another write superseded, without announcing anything`.
- `@s43 drops a simulation that a save superseded, without announcing anything`.
- `@s43 drops a switch answer that another write superseded, without announcing anything`.
- `@s40 marks the state of each switch with a class besides the text` — el contrato
  de @s37 pide el texto **además del color**; el color no se comprobaba.
- `@s40 blocks the second switch while the first one is still in the air` — el
  interruptor en vuelo queda deshabilitado y el de otra regla no lanza una segunda
  escritura.
- Reforzada `@s43 drops the history of the rule the owner just left`: esperaba a que
  la respuesta tardía llegara **antes** de mirar, y ahora exige que no anuncie nada.

**Previsión (no medida): 26 mutantes verificados, los 26 MUEREN.** Previsión
razonada del racimo, con hermanos: **35 a 45**.

**Anotado y no perseguido (fuera de encargo).** Las tres escrituras (`save`,
`simulate`, `toggle`) comparten un solo `writeRequest`, de modo que empezar una
cancela la anterior **en silencio**: pulsar «Simular» con un guardado en vuelo lo
aborta, y como el `finally` está guardado por `writeRequest.current === controller`,
`saving` se queda a `true` y **«Guardar» no vuelve a habilitarse nunca**. Lo mismo
con `busyToggle` y el interruptor. No es un hueco de oráculo sino un diseño a
revisar (un `AbortController` por operación), y cambiarlo aquí sería refactorizar
de paso.

---

## Racimo 7 — lo que la pantalla tiene que **dejar de** mostrar

**Causa común.** Todas las pruebas comprobaban lo que aparece; ninguna, lo que
tiene que **desaparecer**. Por eso vivían los `setFields({})`, `setNotice(null)`,
`setConflict(false)` y el vaciado del historial: borrarlos no rompía nada porque
nadie miraba si la queja del intento anterior seguía en pantalla.

**Oráculos que faltaban.** Cinco pruebas nuevas y dos reforzadas:

- `@s38 clears the previous complaint each time the owner saves again` — tres
  intentos encadenados: una queja por campo, un fallo sin campos (que debe **quitar**
  el `aria-invalid` anterior) y otra queja por campo (que debe **quitar** el aviso).
- `@s39 clears the previous complaint each time the simulation is run again`.
- `@s40 clears the previous complaint when the switch is flipped again` — el aviso
  de un historial que falló no puede sobrevivir al siguiente cambio de interruptor.
- `@s40 loads the current version of the very rule that clashed` — con dos reglas,
  para que buscar la que chocó se distinga de coger la primera; y además el aviso de
  conflicto se cierra y la lista queda al día, no sólo el borrador.
- `@s41 empties the previous history before the new one arrives` — al abrir el
  historial de otra regla, las filas de la anterior desaparecen **antes** de que
  llegue la respuesta, no después.
- Reforzada `@s41 loads the history in pages…`: una fila sin código de error no
  pinta un hueco vacío por él.
- Reforzada `@s40 blocks the second switch…`: la respuesta sustituye **sólo** a su
  regla; la otra sigue siendo la otra.

**Previsión (no medida): 17 mutantes verificados, los 17 MUEREN.** Previsión
razonada del racimo: **20 a 25**.

---

## Racimo 8 — al salir de la página, lo que queda en el aire

**Causa común.** @s43 dice que la respuesta tardía «no modifica la interfaz visible
ni anuncia nada», y las pruebas lo comprobaban mirando la pantalla **después** de
desmontar. Pero una pantalla desmontada no muestra nada pase lo que pase: el
oráculo no podía fallar. Borrar los cuatro `abort()` de la limpieza de desmontaje no
rompía ninguna prueba.

**Oráculo que faltaba.** Mirar **la señal**, no la pantalla. El arnés ahora guarda
el `AbortSignal` de cada petición y dos pruebas comprueban que al desmontar quedan
las cuatro abortadas:

- `@s43 cancels the reads still in the air when the page is left` — la lista y los
  proyectos.
- `@s43 cancels the write and the history still in the air when the page is left` —
  la simulación y el historial, que sólo los cancela la limpieza del
  `useLayoutEffect`.

Y dos afirmaciones más sobre el estado de error: al reintentar se anuncia la carga
y desaparece el error, y una lectura correcta no deja el error puesto.

**Previsión (no medida): 11 mutantes verificados, 10 MUEREN.** El que sobrevive,
`return () => controller.abort()` de la lectura inicial, es **redundante con el
código, no con la prueba**: la limpieza del `useLayoutEffect` ya aborta ese mismo
controlador (`live.current`), así que quitar uno de los dos no cambia nada
observable. Es código duplicado, no un hueco de oráculo; lo dejo anotado.

---

## Resumen y previsión

| Racimo | Verificados | Mueren |
|---|---|---|
| 1 — esquema del cliente | 40 | 40 |
| 2 — conversación del cliente | 34 | 34 |
| 3 — tablas de la vista | 23 | 23 |
| 4 — el cuerpo que manda el editor | 37 | 36 |
| 5 — reglas de webhook (**defecto arreglado**) | 10 | 10 |
| 6 — avisos de error y guardas tardías | 26 | 26 |
| 7 — lo que tiene que desaparecer | 17 | 17 |
| 8 — abortos al salir | 11 | 10 |
| **Total** | **198** | **196** |

Los dos que no mueren son equivalentes y están razonados arriba.

**Previsión de puntuación (no medida; la mide el orquestador).** El denominador es
876 mutantes con 462 muertos de partida (52,74 %). De los 414 vivos:

- `automations-api.ts` — los 155 caen todos en líneas atacadas por los racimos 1 y
  2, y las familias son sistemáticas (una fila de tabla por conjunto). Previsión:
  **140-150 muertos**, de 58,22 % a **~93-96 %**.
- `automations.tsx` — de los 259, quedan sin atacar unos **41**, casi todos
  equivalentes o inalcanzables: guardas del camino de éxito que sólo se alcanzan si
  una petición se sustituye **sin** abortarla (nunca ocurre: las tres escrituras
  abortan siempre a la anterior, y el cliente lanza en cuanto ve la señal cortada),
  arrays de dependencias constantes, `?.` sobre referencias que nunca son nulas, y
  el enlace a la tarea del historial de una regla de webhook, que sólo se pintaría
  con una respuesta del servidor que se contradice a sí misma. Previsión:
  **170-195 muertos**, de 47,36 % a **~82-88 %**.

**Previsión global: entre 85 % y 91 %, con el centro en ~88 %.** Por encima del
umbral de 80 con margen incluso en el escenario pesimista (84,7 %).

## Defectos de producto

1. **Arreglado.** Renombrar una regla de aviso al webhook la convertía en una regla
   de crear tareas y perdía el `endpointId` (racimo 5). Rojo demostrado, arreglo en
   `actionOf()`, `git diff` de producción limitado a ese cambio.
2. **Anotado, no arreglado** (diseño, fuera de encargo). Las tres escrituras
   comparten un `AbortController`: empezar una cancela la anterior en silencio y
   deja su bandera de ocupado (`saving`, `busyToggle`) puesta para siempre, de modo
   que «Guardar» o el interruptor quedan inutilizables hasta recargar.
3. **Anotado, no arreglado** (interfaz). El editor sigue mostrando «Título de la
   tarea» y «Criterio de la tarea» vacíos al abrir una regla de webhook; ahora los
   ignora en vez de destruir la regla, pero lo correcto sería no ofrecerlos.
4. **Anotado, fuera de ámbito.** `frontend/src/appearance.test.tsx` →
   `@s24 follows a system color-scheme change without persisting another preference`
   falló una vez en una tanda combinada y volvió a pasar tres veces seguidas
   después. No la toco, pero conviene mirarla: huele a verde por suerte de carga.

## Comprobaciones finales

- `pnpm --dir frontend exec vitest run src/automations` → **84 pruebas verdes**
  (26 del cliente, 51 de la vista, 7 de la ruta).
- `vitest run src/automations src/workspace src/App` → 190 verdes, tres veces.
- `tsc --noEmit` y `eslint` sobre los ficheros tocados, limpios.
- `git diff` de producción contra `origin/main`: sólo `actionOf()` en
  `automations.tsx` (+17 −11), que es el arreglo del defecto 1.
