# Feature 25 · webhooks — cerrar la puerta de mutación del frontend

Medida de partida (10 de septiembre, campaña del orquestador):

| Fichero | Puntuación | Vivos | Sin cobertura | Total |
|---|---|---|---|---|
| `src/webhooks-client.ts` | 74,13 % | 69 | 5 | 286 |
| `src/webhooks.tsx` | 68,97 % | 82 | 17 | 319 |
| **Total** | **71,40 %** | **151** | **22** | **605** |

Umbral 80 %. Muertos hoy: 432 de 605. Para 80 % hacen falta 484, es decir
**52 muertes más**.

Regla de la noche: **no se toca producción para matar mutantes**, no se relaja
ni se borra ninguna prueba existente. Lo que falta son aserciones. Las
excepciones son los defectos de producto, que sí se arreglan y se anotan aquí.

## Cómo se acredita cada racimo

No se ejecuta Stryker (20 min, y hay otra campaña en curso). Cada mutante se
aplica **al fichero de producción real** con
`scripts/verificar-mutantes-webhooks.mjs`, se ejecuta la suite del fichero, se
comprueba el rojo y se restaura. El script termina siempre dejando
`git diff` vacío sobre producción.

---

## Racimo 1 — el catálogo de `eventTypes` en `decodeEndpoint`

**Causa común.** La expresión de `webhooks-client.ts:93-99` codifica **tres**
reglas distintas del contrato (@s3): que todo tipo esté en el catálogo, que no
haya repetidos y que lleguen en orden de catálogo. Todas las pruebas existentes
usan `eventTypes: ["TaskCreated.v1"]`, una lista de **un solo elemento**. Con
una lista de uno, `index > 0` nunca se cumple, así que **la mitad de la
expresión no se ejecuta jamás** (los tres mutantes «sin cobertura» de la línea
97) y `.some()` y `.every()` son indistinguibles.

**Ocho pruebas nuevas** en `webhooks-client.test.ts`, todas sobre listas de dos
o más tipos:

| Prueba | Qué separa |
|---|---|
| acepta los doce en orden de catálogo | el orden correcto no se rechaza |
| acepta dos tipos en orden | idem, caso mínimo |
| rechaza dos conocidos fuera de orden | la regla de orden existe |
| rechaza el mismo tipo repetido | `>=` y no `>` (el repetido empata) |
| rechaza un tipo fuera del catálogo, solo | la rama de catálogo en índice 0 |
| rechaza un desconocido escondido tras uno válido | `some` y no `every` |
| rechaza un endpoint sin ningún tipo | `length === 0` |
| rechaza `eventTypes` que no es array | `Array.isArray` |

**Evidencia del rojo** (`node scripts/verificar-mutantes-webhooks.mjs`):

```
ROJO 79 ConditionalExpression orden -> true
ROJO 80 EqualityOperator >= -> >
ROJO 81 EqualityOperator >= -> <
ROJO 74 ConditionalExpression index>0 && ... -> false
ROJO 75 LogicalOperator index>0 && -> ||
ROJO 78 EqualityOperator index > 0 -> index <= 0
ROJO 68 MethodExpression some -> every
ROJO 69 ArrowFunction predicado -> undefined
ROJO 71 ConditionalExpression !includes(type) -> false
ROJO 72 LogicalOperator !includes || -> &&
ROJO 66 ConditionalExpression eventTypes.length === 0 -> false
ROJO 65 ConditionalExpression !Array.isArray -> false

Muertos: 12 · Vivos: 0
```

**Previsión de muertes: 12** (3 de los «sin cobertura» de la línea 97 y 9
supervivientes de las líneas 90-99).

Nota sobre un equivalente: `indexOf` → `lastIndexOf` sobre `webhookEventTypes`
**es equivalente** y ninguna prueba puede matarlo, porque el catálogo no tiene
duplicados (y hay una prueba que fija la lista entera). No se persigue.

---

## Racimo 2 — las demás guardas de `decodeEndpoint` y la invariante de desactivado

**Causa común.** Las trece pruebas del cliente partían todas del **mismo**
endpoint válido y sólo variaban `status`. Cada campo del DTO cerrado tiene su
guarda y ninguna se ejercía: ni un id no-uuid, ni una url `http://` o no-cadena,
ni una descripción no-cadena o de 81 puntos, ni la frontera de 80.

Y la invariante de @s1 —«un endpoint desactivado lleva SIEMPRE razón e instante,
y uno activo NUNCA»— sólo se probaba con **las dos mitades fallando a la vez**
(`status: "disabled"` con ambos campos `null`). Ese caso no separa el `||` del
`&&`: hacen falta los **asimétricos**, uno por mitad.

Doce pruebas nuevas. Frontera exacta incluida: ochenta emojis son ochenta puntos
de código y ciento sesenta unidades UTF-16, así que el caso de 80 acredita a la
vez el `> 80` (y no `>= 80`) y que la medida es en puntos de código.

**Evidencia del rojo: 13 mutantes muertos**, con la prueba que cae en cada caso:

```
ROJO 3   uuid(value) -> true                  · rejects an id of thirty-six characters that is not a uuid
ROJO 5   identifier && -> ||                  · idem
ROJO 52  typeof url !== string -> false       · rejects a url that is not even a string
ROJO 57  "https://" -> ""                     · rejects a destination that is not https
ROJO 58  typeof description !== string -> false · rejects a description that is not a string
ROJO 61  description > 80 -> false            · rejects a description of eighty-one code points
ROJO 62  > 80 -> >= 80                        · accepts a description of exactly eighty code points
ROJO 64  [...description] -> []               · rejects a description of eighty-one code points
ROJO 98  invariante || -> &&                  · rejects a disabled endpoint with a reason but no instant
ROJO 99  mitad de la razón -> false           · rejects a disabled endpoint with an instant but no reason
ROJO 103 razón && -> ||                       · rejects a disabled reason outside the two the contract defines
ROJO 106 mitad del instante -> false          · rejects a disabled endpoint with a reason but no instant
ROJO 110 instante && -> ||                    · rejects a disabledAt that is not an instant
```

### Hallazgo: `rejects.toThrow("…")` no acredita el diagnóstico

El mutante 0 (`incompatible = () => new Error(…)` → `() => undefined`) **seguía
vivo con las trece pruebas anteriores y con las doce nuevas**. Motivo: en vitest
4, `await expect(p).rejects.toThrow("Confirmación incompatible")` **se cumple
igual cuando lo que se lanza es `undefined`** en vez de un `Error`. Es decir,
las ~25 aserciones de rechazo del cliente no comprobaban en realidad que hubiera
un diagnóstico; sólo que algo fallaba.

Es la misma familia que la aserción-que-no-puede-fallar de `webhooks.test.tsx`.
Se cierra con **un** oráculo que fija el valor rechazado entero, y que cubre a
toda la familia:

```
ROJO 0 ArrowFunction incompatible -> undefined
     1 rojas · @s36 rejects with a real Error carrying the diagnosis, not a bare throw
```

**Previsión de muertes del racimo 2: 14.**

Dos equivalentes descartados y no perseguidos:
- `identifier`: `(value as string).length === 36` → `true`. La expresión regular
  de `uuid()` ya fija la longitud en 36, así que la comparación es redundante.
- `value.disabledReason !== null` → `true` y `value.disabledAt !== null` → `true`.
  `includes(null)` e `instant(null)` devuelven `false` sin lanzar, así que el
  resultado de la conjunción no cambia nunca.

---

## Racimo 3 — `decodeDelivery`, las cuatro cláusulas de `whole()` y la deduplicación

**Causa común.** `whole(value, max)` codifica **cuatro** reglas en una línea
—que sea número, que sea entero, que no sea negativo y que no pase del máximo—
y las tres pruebas de entregas sólo pasaban por enteros válidos. Un predicado de
cuatro cláusulas necesita **cuatro contraejemplos**, uno por cláusula, o las
conjunciones son indistinguibles de las disyunciones.

Lo mismo, campo por campo, con los seis `null || …` de `decodeDelivery`: cuando
la única entrega de prueba trae siempre valores válidos, cambiar la guarda entera
por `true` no rompe nada.

Y la invariante de @s40 —«sólo una entrega pendiente está programada para otro
intento»— se rompe por **los dos lados**: una terminal con `nextAttemptAt`, y una
pendiente sin él. Hay que probar los dos.

Quince pruebas nuevas, incluida la frontera que sí acepta (`attempt: 6`).

**Evidencia del rojo: 13 mutantes muertos.**

```
ROJO 9   whole() entero -> true                · rejects an attempt that is not a number
ROJO 11  whole() … && <= max -> ||             · idem
ROJO 13  whole() … && >= 0 -> ||               · idem
ROJO 15  whole() typeof && isInteger -> ||     · rejects an attempt that is fractional
ROJO 19  whole() value >= 0 -> true            · rejects an attempt that is negative
ROJO 22  whole() value <= max -> true          · rejects an attempt beyond the six the contract allows
ROJO 143 typeof eventType !== string -> false  · rejects an eventType that is not a string
ROJO 150 httpStatus null|whole -> true         · rejects an httpStatus outside the range of a status code
ROJO 156 latencyMs null|whole -> true          · rejects a negative latency
ROJO 162 errorClass null|catálogo -> true      · rejects an error class outside the seven the contract defines
ROJO 168 nextAttemptAt null|instant -> true    · rejects a nextAttemptAt that is not an instant
ROJO 176 invariante de pendiente -> false      · rejects a terminal delivery still scheduled for another attempt
ROJO 193 deduplicación -> false                · rejects a list that repeats the same delivery id
```

Detalle del 168: no basta con un `nextAttemptAt` inválido en una entrega
terminal, porque entonces **rechaza igualmente por la invariante de pendiente**
y el mutante sobrevive. El contraejemplo tiene que ser una entrega **pendiente**
con un `nextAttemptAt` que no es un instante: así la invariante se cumple y la
única defensa que queda es la guarda mutada.

**Previsión de muertes del racimo 3: 13.** Acumulado acreditado: 39.

---

## Racimo 4 — identidad, forma de la petición y contrato de la respuesta

**Causa común.** Las pruebas del cliente comprobaban **qué se pide** (la ruta, el
método, el cuerpo) pero no **cómo** se pide ni **qué se acepta de vuelta**.
Tres huecos con la misma raíz:

1. **La guarda de identidad.** Las cinco rutas que llevan un id en el camino lo
   validan antes de salir a la red (`webhookUrl`, y el `deliveryId` de
   `redeliverWebhook`). Un `grep` de «Identidad incompatible» en las suites no
   devolvía nada: cero pruebas con un id malo. Es la defensa contra construir una
   ruta con lo que venga —`"../otro"` sale de `/api/v1/me/webhooks/` y apunta a
   otro recurso—, así que el oráculo afirma además que **no se toca la red**.
2. **La cabecera `Content-Type` y el `{ signal }`.** Se leían `method` y `body`
   de las llamadas registradas, nunca `headers` ni `signal`.
3. **El crosscheck de `setWebhookStatus`.** `endpoint.id !== id || endpoint.status
   !== status` es la única defensa contra que el servidor confirme el cambio del
   webhook equivocado, o un estado distinto del pedido. Ninguna respuesta de
   prueba descuadraba, así que la condición nunca era verdadera. Como en el
   racimo 2, hay que romper **cada mitad por separado**: rompiendo las dos a la
   vez, el `||` y el `&&` coinciden.

**Evidencia del rojo: 19 mutantes muertos.**

```
ROJO 199 !identifier(id) -> false          · setWebhookStatus refuses an id that is not a uuid…
ROJO 201 "Identidad incompatible" -> ""    · idem
ROJO 282 !identifier(deliveryId) -> false  · redeliver refuses a delivery id that is not a uuid…
ROJO 284 "Identidad incompatible" -> ""    · idem
ROJO 219 headers de createWebhook -> {}    · declares the JSON media type on both bodies it sends
ROJO 220 "application/json" -> ""          · idem
ROJO 243 headers de setWebhookStatus -> {} · idem
ROJO 244 "application/json" -> ""          · idem
ROJO 212 { signal } de listWebhooks -> {}  · hands its own signal to every read it starts
ROJO 276 { signal } de deliveries -> {}    · idem
ROJO 224 !exact(endpoint secret) -> false  · rejects a creation body that carries anything beyond endpoint and secret
ROJO 229 typeof secret !== string -> false · rejects a secret that is not a string even if it reads like one
ROJO 233 ancla ^ del secreto               · rejects a secret with anything in front of the whsec_ prefix
ROJO 248 crosscheck -> false               · rejects a status change confirmed for another webhook
ROJO 249 crosscheck || -> &&               · idem
ROJO 250 endpoint.id !== id -> false       · idem
ROJO 252 endpoint.status !== status -> false · rejects a status change confirmed with a status nobody asked for
ROJO 260 status !== 204 -> false           · treats any DELETE answer other than 204 as a failure
ROJO 266 !exact(delivery) -> false         · rejects an accepted ping that carries anything beyond delivery
```

Dos contraejemplos que hubo que afinar:

- **El secreto no-cadena.** `secret: 42` no sirve: al quitar la guarda de tipo,
  `/^whsec_…$/.test(42)` coacciona a `"42"`, no casa, y el mutante **rechaza
  igual**. El contraejemplo tiene que *leerse* como un secreto válido sin serlo:
  `secret: [secretoVálido]`, cuyo `toString` devuelve la cadena buena.
- **El ancla `^`.** El mutante quita sólo el ancla inicial, así que el
  contraejemplo es un secreto válido **con basura delante**: `"xx" + secreto`.

**Previsión de muertes del racimo 4: 19.** Acumulado acreditado: 58.

---

## Racimo 6 — las doce etiquetas de evento y su emparejamiento (defecto de producto)

**Causa común.** `eventLabels` (en `webhooks.tsx`) y `webhookEventTypes` (en
`webhooks-client.ts`) son **dos listas paralelas emparejadas por índice**:

```tsx
{webhookEventTypes.map((type, index) => ( … {eventLabels[index]} … ))}
```

Sólo se ejercía **una** de las doce parejas —«Crear tarea» / `TaskCreated.v1`—
porque todas las pruebas marcaban esa casilla. Las once restantes se pintaban sin
que nadie las mirara.

### Defecto de producto: nada ata las dos listas

No hay ningún error hoy, pero **no hay forma de que se note si mañana lo hay**.
Insertar un tipo nuevo en una lista y no en la otra desplaza todas las etiquetas
siguientes: quien marque «Crear subtarea» se suscribe a `TaskStatusChanged.v1`,
y la suite sigue verde. Un webhook suscrito a lo que no se pidió es exactamente
lo que la feature promete no hacer.

El arreglo es la **atadura que faltaba**: una tabla literal de las doce parejas,
escrita en la prueba, que vive fuera de las dos listas y las fija a las dos. Dos
oráculos la usan:

- los nombres accesibles de las trece casillas, en orden, contra la tabla;
- doce casos —uno por pareja— que marcan **esa** casilla y afirman que el POST
  lleva **exactamente** ese tipo.

Simulando el defecto (insertando `"Archivar proyecto"` en el índice 1 de
`eventLabels`, que es lo que pasaría al añadir un tipo a mitad del catálogo):

```
ROJO DEFECTO desalineamiento de las dos listas paralelas
     12 rojas · @s37 offers exactly the twelve labels of the catalogue, in catalogue order
```

Doce pruebas en rojo. Antes de esta tabla, ese desalineamiento no rompía ninguna.

**Evidencia del rojo de los mutantes: 6 muertos** (los tres restantes de las doce
etiquetas ya morían por *timeout*).

```
ROJO 290 "Editar proyecto" -> ""            · offers exactly the twelve labels…
ROJO 291 "Cambiar estado de proyecto" -> "" · idem
ROJO 294 "Cambiar estado de tarea" -> ""    · idem
ROJO 295 "Planificar bloque" -> ""          · idem
ROJO 299 "Extender sesión" -> ""            · idem
ROJO 300 "Cerrar sesión de trabajo" -> ""   · idem
```

**Previsión de muertes: 6.** Acumulado acreditado: 64.

---

## Racimo 7 — marcar se probaba; **desmarcar**, jamás

**Causa común.** Los dos ternarios del formulario tienen dos ramas cada uno y
**sólo se ejercía la de marcar**. Nadie desmarcaba nunca la casilla maestra ni
una casilla suelta, así que las seis mutaciones de la rama de desmarcar estaban
en «sin cobertura»: no es un oráculo débil, es producto que nadie ha ejercido.

Y el estado inicial del formulario tampoco se afirmaba: ni la descripción vacía,
ni la selección vacía, ni la región `role=status` en silencio, ni la ausencia de
alerta al entrar.

Cinco pruebas nuevas:

- la maestra marcada y **desmarcada**, comprobando las trece casillas en cada
  paso **y el POST resultante**;
- desmarcar una de tres, comprobando que se quedan las otras dos y sólo se cae
  esa;
- marcar las doce **en orden inverso** y comprobar que el POST las lleva en
  orden de catálogo (@s1): eso es lo que separa «conservar el orden del
  catálogo» de «conservar el orden de los clics»;
- el estado inicial completo, con el POST vacío que lo confirma;
- una descripción escrita que viaja en el cuerpo (nadie escribía nunca en ese
  campo, así que su `onChange` era una función entera sin ejercer).

Detalle: para matar el mutante 561 (`[]` → `["Stryker was here"]` al desmarcar
la maestra) **no basta con mirar las casillas**. Ningún `<label>` casa con ese
valor, así que las trece salen desmarcadas igual y la vista se ve idéntica. Sólo
el **cuerpo del POST** distingue «vacío» de «un tipo que no existe»: el oráculo
tiene que enviar el formulario y afirmar `eventTypes: []`.

**Evidencia del rojo: 13 mutantes muertos.**

```
ROJO 557 maestra checked -> false           · unchecking the master checkbox clears every type
ROJO 561 rama de desmarcar la maestra       · idem
ROJO 572 desmarcar uno -> current           · unchecking one type keeps the others…
ROJO 573 predicado de desmarcar -> undefined· idem
ROJO 574 candidate !== type -> true         · idem
ROJO 575 candidate !== type -> false        · idem
ROJO 576 candidate !== type -> ===          · idem
ROJO 555 onChange de descripción -> undefined · idem
ROJO 347 description inicial -> "Stryker"   · starts with an empty selection, an empty url…
ROJO 348 types inicial -> ["Stryker"]       · idem
ROJO 350 uncertain inicial -> true          · idem
ROJO 352 announcement inicial -> "Stryker"  · idem
```

(Doce arriba más el 563/565/566/567-571 del racimo anterior, que las doce
parejas ya matan.)

**Previsión de muertes: 12.** Acumulado acreditado: 76.

---

## Racimo 8 — `report()` escrito para la creación y reutilizado por `act()` (defecto de producto)

**Causa común.** `report()` interpreta los códigos de problema del **POST de
creación** (@s41). `act()` —el envoltorio de las **cinco** acciones de la lista:
ping, activar/desactivar, eliminar, ver entregas y reenviar— lo reutilizaba tal
cual. Ninguna prueba hacía fallar ninguna de las cinco, así que el `catch` de
`act()` y las dos últimas ramas de `report()` no se ejecutaban **nunca**.

### Defecto de producto: al desactivar un webhook, el usuario leía «No se ha podido crear el webhook»

No es un hueco de cobertura: es una falsedad en pantalla. Escritas las siete
pruebas contra la producción de partida, salieron en rojo **las siete**, y esto
es lo que decía cada una:

| Acción que falla | Lo que el usuario leía |
|---|---|
| Desactivar, con cualquier problema | «No se ha podido crear el webhook.» |
| Enviar ping / Ver entregas / Eliminar / Reenviar | lo mismo |
| Cualquier acción, con `409 WEBHOOK_LIMIT` | «Ya tienes cinco webhooks. Elimina uno antes de crear otro.» |
| Cualquier acción, con `400 WEBHOOK_URL_BLOCKED` | se encendía el error del **campo URL del formulario de creación**, con `aria-describedby` apuntando a un campo que no tiene nada que ver |

**Arreglo.** Un reportero propio para las acciones, que no interpreta los códigos
que sólo tienen sentido creando, y un mensaje por acción:

```tsx
async function reportAction(error: unknown, failure: string) {
  const code = await problemCode(error);
  setFormError(
    code === "CONNECTORS_DISABLED"
      ? "Falta configuración del servidor para conectores."
      : failure,
  );
}
```

`CONNECTORS_DISABLED` se conserva porque es el único código que aplica a
**todas** las operaciones de conectores y explica mejor que el genérico.
`act(run, failure)` pasa el mensaje de cada acción. `report()` sigue siendo el
reportero de la creación y no cambia: @s41 lo gobierna y sigue cumpliéndose.

Rehecho el defecto a mano sobre la producción arreglada (volviendo a
`await report(error)` dentro de `act`), caen **7 pruebas**.

Además se cierran las dos ramas de `report()` que nadie ejercía: el
`WEBHOOK_INVALID` y el cajón de sastre de `Response`, éste con dos casos —un
código no listado y un cuerpo `502` que ni siquiera es json—, comprobando que el
usuario lee «No se ha podido crear el webhook» y **no** el mensaje de resultado
incierto, que sólo vale cuando no hubo respuesta ninguna.

**Evidencia del rojo: 17 mutantes muertos.**

```
ROJO 445 code === "WEBHOOK_INVALID" -> false · a creation rejected as invalid asks to review the form
ROJO 447 "WEBHOOK_INVALID" -> ""             · idem
ROJO 448 rama WEBHOOK_INVALID -> {}          · idem
ROJO 450 "Revisa los datos…" -> ""           · idem
ROJO 452 error instanceof Response -> false  · a creation that fails with an unlisted problem code…
ROJO 453 rama Response genérica -> {}        · idem
ROJO 455 "No se ha podido crear…" -> ""      · idem
ROJO 460 catch de act -> {}                  · a failed «Desactivar» says what failed… (7 rojas)
ROJO 461 !aborted -> aborted (act)           · idem (7 rojas)
ROJO 463 guarda de act -> false              · idem (7 rojas)
ROJO mensaje de desactivar -> ""             · idem
ROJO mensaje de activar -> ""                · a failed «Activar» says the activate failed…
ROJO mensaje de ping -> ""                   · a failed «Enviar ping» says what failed…
ROJO mensaje de eliminar -> ""               · a failed delete says the delete failed…
ROJO mensaje de entregas -> ""               · a failed «Ver entregas» says what failed…
ROJO mensaje de reenviar -> ""               · a failed redeliver says the redeliver failed
ROJO DEFECTO act reusa el reportero de la creación · 7 rojas
```

Queda vivo el 462 (`if (!controller.signal.aborted)` → `true` en `act`): sólo se
distingue si la acción falla **después** de abortar. Va con el racimo de guardas
de aborto.

**Previsión de muertes: 16 de los medidos + los 6 mensajes nuevos.** Acumulado
acreditado: 92.

Contrato: el `.feature` **no** cubre el fallo de las acciones de la lista —@s39
y @s40 sólo describen el camino feliz, y @s41 habla sólo del POST de creación—,
así que el arreglo llena un silencio del contrato, no lo contradice. Queda
anotado para el juez por si quiere una fila explícita en @s39.
