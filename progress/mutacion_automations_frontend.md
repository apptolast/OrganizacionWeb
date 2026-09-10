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
