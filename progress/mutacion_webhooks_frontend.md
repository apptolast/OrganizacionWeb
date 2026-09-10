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

---

## Racimo 9 — las entregas de un webhook bajo el nombre de otro (defecto de producto)

**Causa común.** `openDeliveries` y `ping` abren el panel con
`setDeliveriesOf(endpoint.id)` **antes** de tener las filas, y no tocaban
`deliveries`. Ninguna prueba abría el panel de un **segundo** webhook, así que
el estado intermedio no lo miraba nadie.

### Defecto de producto: el panel de B enseñando las entregas de A

Entre el clic en «Ver entregas» de B y la llegada de su respuesta —y **para
siempre** si esa respuesta falla— la tabla seguía mostrando las entregas de A
mientras el panel ya decía B. Y el botón «Reenviar» de esas filas hace
`POST /api/v1/me/webhooks/B/deliveries/{id-de-una-entrega-de-A}/redeliver`:
reenviar la entrega de un webhook desde otro. Lo mismo con `ping(B)`, que
antepone su entrega nueva a las filas de A.

Escritas las tres pruebas contra la producción de partida, salieron las tres en
rojo.

**Arreglo**, en el único sitio donde se decide de quién es el panel:

```tsx
function showDeliveriesOf(endpointId: string) {
  if (deliveriesOf !== endpointId) setDeliveries([]);
  setDeliveriesOf(endpointId);
}
```

Cambiar de webhook vacía la tabla; refrescar el mismo la conserva, así que
«Actualizar» sigue comportándose igual. Rehecho el defecto a mano (quitando la
línea que vacía), caen **3 pruebas**.

Seis pruebas nuevas, tres del defecto y tres de las ramas huérfanas: el ping que
**sustituye** su propia fila sin borrar las demás (con dos filas en pantalla, que
es lo que separa «quitar la repetida» de «quitar todas»), «Actualizar» pidiendo
las entregas del webhook cuyo panel está abierto y no las del primero de la
lista, y «Actualizar» sin hacer nada cuando ese webhook ya no existe.

**Evidencia del rojo: 7 mutantes muertos.**

```
ROJO 489 filtro del ping -> current          · pinging the webhook already on screen replaces its row…
ROJO 490 predicado del filtro -> undefined   · idem
ROJO 491 d.id !== sent.id -> true            · idem
ROJO 492 d.id !== sent.id -> false           · idem
ROJO 493 d.id !== sent.id -> ===             · idem
ROJO 609 item.id === deliveriesOf -> true    · Actualizar asks for the deliveries of the webhook whose panel is open
ROJO 612 if (endpoint) -> true               · Actualizar does nothing when the webhook of the open panel is gone
ROJO DEFECTO la tabla conserva las filas del webhook anterior · 3 rojas
```

**Previsión de muertes: 7.** Acumulado acreditado: 99.

Observación anotada y **no** arreglada, por estar fuera del encargo: eliminar un
webhook no cierra su panel de entregas, así que quedan en pantalla las filas de
algo que ya no existe. El guarda `if (endpoint)` de «Actualizar» impide que eso
reviente, y cerrarlo por mi cuenta convertiría ese guarda en código muerto. Es
una decisión de contrato (@s39 no dice nada del panel), no mía.

---

## Racimo 10 — las guardas de aborto y la aserción que no podía fallar

### Defecto de método: `webhooks.test.tsx:314` afirmaba la ausencia de algo que nunca llegó

La prueba de @s38 retenía el POST **para siempre** (`new Promise(() => {})`),
desmontaba la vista y afirmaba que el secreto no estaba en el DOM. Como la
respuesta no llegaba nunca, **no había secreto que pudiera aparecer**: la
aserción no podía fallar. Es el mismo patrón cazado ya cuatro veces esta noche.

Arreglado: se guarda el `resolve`, se desmonta, y **sólo entonces** se resuelve
con un 201 y su secreto. Ahora hay algo que podría aparecer, y se afirma que no
aparece —ni en el campo ni en ningún sitio del `body`—. Tres pruebas hermanas
para la lista, para el PUT de estado y para una acción que **falla** tarde.

### Las siete puertas y sus comprobaciones de arranque

Sólo `listWebhooks` tenía prueba de señal ya abortada. Las otras seis salían a
la red con la señal muerta sin que nadie lo notara. Un caso por función, todos
afirmando que **`fetch` no se llama**. Y tres de aborto **en vuelo**: durante la
petición, durante el parseo del cuerpo, y en el `DELETE`.

Un contraejemplo que hubo que afinar: quitar el primer `throwIfAborted()` de
`json()` **no** deja pasar la respuesta, porque el segundo la caza igual. Sólo
se distingue si el servidor contesta un estado inesperado: entonces el original
avisa del abandono y el mutante propaga el `409` del servidor como si fuera un
problema de la vista actual, que es justo lo que @s38 prohíbe. El oráculo afirma
el `name` del rechazo, no sólo que hay rechazo.

**Evidencia del rojo: 9 mutantes muertos**, todos del cliente.

```
ROJO 204 throwIfAborted de json (1) -> ;   · an aborted read reports the abort, not the late error of the server
ROJO 208 throwIfAborted de json (2) -> ;   · abandons a read whose signal aborts while the body is being parsed
ROJO 215 arranque de createWebhook -> ;    · createWebhook refuses to start once the signal is already aborted
ROJO 239 arranque de setWebhookStatus -> ; · idem
ROJO 255 arranque de deleteWebhook -> ;    · idem
ROJO 258 throwIfAborted tras el DELETE -> ;· abandons a delete whose signal aborts while the request is in flight
ROJO 269 arranque de pingWebhook -> ;      · idem
ROJO 274 arranque de listWebhookDeliveries -> ; · idem
ROJO 279 arranque de redeliverWebhook -> ; · idem
```

### Hallazgo medido: las ocho guardas de aborto **de la vista** son equivalentes

Escritas las cuatro pruebas de «resuelve después de abortar» que el mapa pedía,
medí los ocho mutantes de guarda de `webhooks.tsx` uno a uno. **Los ocho siguen
vivos**, y no por falta de oráculo:

```
VIVO 365 !aborted (setItems) -> true          VIVO 415 aborted -> false (submit fallo)
VIVO 370 !aborted (setLoadFailed) -> true     VIVO 418 !aborted (setCreating) -> true
VIVO 376 !aborted (setLoading) -> true        VIVO 462 guarda de act -> true
VIVO 402 aborted -> false (submit éxito)      VIVO 467 aborted -> false (changeStatus)
```

Motivo: en esta vista **abortar y desmontar son el mismo suceso** —el único
`abort()` está en la limpieza del `useEffect`—, y React descarta por su cuenta
los `setState` sobre un componente desmontado. Ejecutar la rama de más no cambia
ni un carácter del DOM, así que **ningún oráculo sobre el DOM puede
distinguirlos**. Son equivalentes por la superficie pública del componente.

Las guardas no sobran: son la defensa correcta y el contrato de @s38 en el
cliente, donde **sí** se acreditan las nueve de arriba. Pero como puerta de
mutación, esos ocho no se pueden ganar sin exponer estado interno, y no lo voy a
hacer. Se anotan aquí para que la campaña no los persiga.

**Previsión de muertes: 9.** Acumulado acreditado: 108.

---

## Racimo 11 — las ocho celdas de la tabla de entregas, pintadas y nunca miradas

**Causa común.** La prueba de @s40 comprobaba **cuántas** filas hay y el
**estado** de cada una, y nada más. Las ocho celdas se pintaban sin oráculo: ni
el guion de los tres campos que pueden faltar, ni la unidad de la latencia, ni
la fecha recortada a diez caracteres, ni el `data-label` que cada celda lleva
para cuando la cabecera se apila fuera de la vista a 320 px (@s42).

En vez de un oráculo por celda, **una fila entera contra una tabla literal**, con
la etiqueta de columna emparejada con su contenido. Dos filas: una completa y una
agotada con los tres campos opcionales vacíos, que es donde vive el guion.

Eso ata además los ocho `data-label`: simulando un desalineamiento (`Latencia`
etiquetada como `Intento`) la prueba cae. Sin la tabla literal, ese
desalineamiento dejaba a quien navega a 320 px leyendo «Intento: 12 ms» sin que
nada se pusiera rojo.

Y la traducción de tipos a etiquetas de la lista —el **otro** sitio donde viven
las dos listas paralelas del racimo 6— con un webhook suscrito a tres tipos.

**Evidencia del rojo: 12 mutantes muertos**, más el desalineamiento simulado.

```
ROJO 617 httpStatus ?? -> &&        ROJO 623 plantilla de ms -> ""
ROJO 618 guion de httpStatus -> ""  ROJO 624 errorClass ?? -> &&
ROJO 619 latencyMs === null -> true ROJO 625 guion de errorClass -> ""
ROJO 620 latencyMs === null -> false ROJO 626 updatedAt.slice -> updatedAt
ROJO 621 latencyMs === null -> !==  ROJO 586 traducción de tipos -> undefined
ROJO 622 guion de latencia -> ""    ROJO 587 join(", ") -> ""
ROJO DEFECTO data-label de Latencia desalineado
```

**Previsión de muertes: 12.** Acumulado acreditado: 120.

---

## Racimo 12 — identidad por elemento, reinicio del formulario y los atributos de @s42

**Causa común, la misma de siempre en tres sitios distintos.** Con **un solo**
elemento en la lista, «cambiar el que toca» y «cambiarlos todos» son
indistinguibles: `map((item) => item.id === updated.id ? updated : item)` da lo
mismo que `map(() => updated)`. Igual al eliminar y al reenviar. Hacen falta dos
elementos y mirar **el otro**, el que no se tocó.

Y tres cosas que ninguna prueba miraba nunca: que la creación con éxito vacía el
formulario que acaba de enviar, que un intento nuevo borra lo que dejó el
anterior, y los atributos de los que depende @s42 (`tabIndex={-1}` del `main` y
del encabezado, `onFocus` que selecciona el secreto entero).

Ocho pruebas nuevas. **17 mutantes muertos.**

```
ROJO 471 item.id === updated.id -> true   · disabling one webhook of two leaves the other exactly as it was
ROJO 503 filtro de eliminar -> undefined  · deleting one webhook of two removes only that one…
ROJO 505 item.id !== endpoint.id -> false · idem
ROJO 507 setConfirming(null) -> ;         · idem
ROJO 525 item.id === reopened.id -> true  · redelivering one row of two leaves the other exactly as it was
ROJO 408 setUrl("") -> "Stryker"          · a successful creation empties the form it just sent
ROJO 410 setDescription("") -> "Stryker"  · idem
ROJO 412 setTypes([]) -> ["Stryker"]      · idem
ROJO 395 setFormError(null) -> ;          · each attempt clears what the previous one left on screen
ROJO 396 setUrlError(null) -> ;           · idem
ROJO 390 preventDefault() -> ;            · submitting the form never navigates away from the view
ROJO 385 setLoading(true) -> false        · Reintentar shows the loading state again…
ROJO 387 setLoadFailed(false) -> true     · idem
ROJO 543 alerta de carga fallida -> ""    · idem
ROJO 536 tabIndex del main -> +1          · the main landmark and the list heading take focus…
ROJO 584 tabIndex del h2 -> +1            · idem
ROJO 547 onFocus del secreto -> undefined · focusing the secret selects it whole…
```

Tres oráculos que la primera versión **no** discriminaba, y por qué:

- **La fila que no se toca.** Guardaba la referencia al `<tr>` *antes* de
  reenviar. Si React sustituye la fila, el oráculo lee un nodo desprendido y no
  se entera de nada. Hay que **volver a consultarla** después de la acción.
- **`setTypes([])` tras crear.** Otra vez lo del racimo 7: ninguna etiqueta casa
  con un tipo inventado, así que las casillas se ven igual. Sólo un **segundo
  envío** distingue «vacío» de «un tipo que nadie reconoce».
- **`setFormError(null)`.** El primer intento tiene que dejar un mensaje
  **general**; si falla por el campo URL, lo que se limpia es el otro estado. La
  prueba encadena tres intentos —límite, URL bloqueada, éxito— y comprueba que
  en el segundo hay **una** alerta, no dos.

**Previsión de muertes: 17.** Acumulado acreditado: **137**.

---

# Resumen

## Racimos cerrados y previsión

La previsión es **no medida**: es el recuento de mutantes que se han aplicado a
mano al fichero de producción real y han puesto la suite en rojo, con la prueba
concreta que cae anotada en cada racimo. Stryker puede diferir en los bordes.

| # | Racimo | Fichero | Previsión |
|---|---|---|---|
| 1 | El catálogo de `eventTypes`: orden, repetidos, catálogo cerrado | cliente | 12 |
| 2 | Las demás guardas de `decodeEndpoint` y la invariante de desactivado | cliente | 14 |
| 3 | `decodeDelivery`, las cuatro cláusulas de `whole()`, la deduplicación | cliente | 13 |
| 4 | Identidad, cabecera, señal y crosscheck del cambio de estado | cliente | 19 |
| 5 | Las guardas de aborto de las siete puertas | cliente | 9 |
| 6 | Las doce etiquetas de evento y su emparejamiento | vista | 6 |
| 7 | Marcar y **desmarcar** tipos; estado inicial del formulario | vista | 12 |
| 8 | `report()` reutilizado por `act()` | vista | 16 |
| 9 | La fuga de entregas entre webhooks | vista | 7 |
| 10 | Las ocho celdas de la tabla de entregas | vista | 12 |
| 11 | Identidad por elemento, reinicio del formulario, atributos de @s42 | vista | 17 |
| | **Total** | | **137** |

Partida: 432 muertos de 605 (71,40 %). Hacían falta **52** para llegar a 80 %.
Si la mitad de la previsión se confirmara, la puerta ya estaría pasada.

Pruebas: **44 → 150** en los tres ficheros de `src/webhooks*`. Verde de punta a
punta, más `connectors-catalog` (30) que también importa la vista.

## Los cuatro defectos encontrados

Tres de producto y uno de método. Los tres primeros se arreglaron **con el rojo
demostrado primero**: escritas las pruebas contra la producción de partida,
fallaron; hecho el arreglo, pasaron; rehecho el defecto a mano, volvieron a
fallar.

1. **Al fallar una acción, el usuario leía «No se ha podido crear el webhook».**
   `report()` se escribió para la creación y `act()` lo reutilizaba para las
   cinco acciones de la lista. Además, un `WEBHOOK_LIMIT` decía «Ya tienes cinco
   webhooks» al desactivar, y un `WEBHOOK_URL_BLOCKED` encendía el error del
   campo URL del formulario de creación con `aria-describedby` apuntando a un
   campo ajeno. **7 pruebas en rojo** contra la producción de partida.
   *Arreglado*: un reportero propio para las acciones y un mensaje por acción.

2. **El panel de entregas de un webhook enseñando las de otro.** El panel se
   abría antes de tener las filas y no vaciaba la tabla, así que entre el clic y
   la respuesta —y **para siempre** si esa respuesta fallaba— se veían las
   entregas del webhook anterior bajo el nombre del nuevo, y su botón «Reenviar»
   hacía `POST /webhooks/B/deliveries/{entrega-de-A}/redeliver`. **3 pruebas en
   rojo**. *Arreglado*: cambiar de webhook vacía la tabla; refrescar el mismo la
   conserva.

3. **Nada ataba `eventLabels` con `webhookEventTypes`.** Dos listas paralelas
   emparejadas por índice, y sólo el índice 3 verificado. No había error hoy,
   pero no había forma de enterarse mañana: insertar un tipo a mitad del
   catálogo haría que quien marca «Crear subtarea» se suscriba a otra cosa con
   la suite en verde. *Arreglado*: una tabla literal de las doce parejas, fuera
   de las dos listas, que las fija a las dos. Simulado el desalineamiento, caen
   **12 pruebas**; antes no caía ninguna.

4. **Dos aserciones que no podían fallar.**
   - `webhooks.test.tsx:314` afirmaba la ausencia del secreto con la respuesta
     retenida para siempre: no había secreto que pudiera aparecer. Ahora la
     respuesta llega —tarde, con su 201— y sólo entonces se afirma la ausencia.
   - En vitest 4, `rejects.toThrow("…")` **se cumple también cuando lo lanzado
     es `undefined`** en vez de un `Error`. Las ~25 aserciones de rechazo del
     cliente no comprobaban el diagnóstico, sólo que algo fallaba. Cerrado con
     un oráculo que fija el valor rechazado entero.

## Lo que no se puede ganar, y por qué

**Las ocho guardas de aborto de `webhooks.tsx` son equivalentes.** Medido, no
razonado: escritas las cuatro pruebas de «resuelve después de abortar», los ocho
mutantes siguen vivos. En esta vista abortar y desmontar son el mismo suceso
—el único `abort()` está en la limpieza del `useEffect`— y React descarta por su
cuenta los `setState` sobre un componente desmontado, así que ejecutar la rama
de más no cambia ni un carácter del DOM. Distinguirlos exigiría exponer estado
interno. Las guardas del **cliente**, que sí son observables, quedan las nueve
acreditadas.

Otros equivalentes descartados y no perseguidos: `indexOf` → `lastIndexOf` sobre
un catálogo sin duplicados; `(value as string).length === 36` cuando la expresión
regular de `uuid()` ya fija la longitud; `disabledReason !== null` → `true` y
`disabledAt !== null` → `true`, porque `includes(null)` e `instant(null)`
devuelven `false` sin lanzar.

## Cómo se acreditó

`node scripts/verificar-mutantes-webhooks.mjs [filtro]` aplica cada mutante al
fichero de producción real, ejecuta la suite del fichero, anota si cae en rojo y
**restaura**. No se ejecutó Stryker: lo mide el orquestador.

## Ficheros compartidos

Ninguno. Sólo `frontend/src/webhooks*`, `scripts/verificar-mutantes-webhooks.mjs`
y esta bitácora.

## Para el juez

El arreglo del defecto 1 introduce mensajes de error por acción que el
`.feature` **no** describe: @s39 y @s40 sólo cuentan el camino feliz y @s41 habla
sólo del POST de creación. El arreglo llena un silencio del contrato, no lo
contradice, pero si se quiere una fila explícita en @s39 para el fallo de las
acciones, ahí está el sitio.

Observación anotada y **no** arreglada por estar fuera del encargo: eliminar un
webhook no cierra su panel de entregas, así que quedan en pantalla las filas de
algo que ya no existe. El guarda `if (endpoint)` de «Actualizar» impide que eso
reviente. Cerrarlo por mi cuenta convertiría ese guarda en código muerto, y es
una decisión de contrato.

---

## Incidente: un mutante llegó a un commit, y cómo se evita

Al lanzar la batería completa **en segundo plano**, la tarea agotó su tiempo y
el proceso murió sin ejecutar el `finally` que restaura. El mutante 461
(`if (!controller.signal.aborted)` → `if (controller.signal.aborted)` en `act`)
se quedó escrito en `webhooks.tsx`, y el siguiente `git add -A` lo commiteó en
`9246699c`. Dos agravantes de Windows:

- matar el envoltorio de msys **no** alcanza al `node.exe` real, así que el
  proceso siguió mutando el fichero mientras yo intentaba arreglarlo;
- `timeout` y las tareas en segundo plano matan con SIGKILL, que **ningún**
  manejador puede interceptar.

**Arreglado**: mutante revertido, producción idéntica a `5bd6c1e5`, 150 pruebas
en verde. Y tres defensas en el script:

1. manejadores de `exit`, `SIGINT`, `SIGTERM`, `SIGHUP` y `SIGBREAK` que
   restauran los dos ficheros;
2. una comprobación final que le pregunta **a git** si producción quedó limpia y
   sale con código distinto de cero si no, en vez de fiarse del `finally`;
3. un aviso en la cabecera: **no ejecutar en segundo plano ni bajo `timeout`**.

La segunda es la que vale: la única garantía frente a un SIGKILL no es un
manejador, es comprobar el resultado contra git antes de commitear nada.

Comprobado que la defensa muerde: matando una pasada a mitad, el script avisa
`PRODUCCIÓN SUCIA` y nombra el fichero.

---

## Racimo 13 — respuesta al bloqueante B1 del panel

`progress/carriles/bloqueantes_25.md` §B1 enumera los supervivientes por línea.
Contrastada su lista con lo hecho, quedaban **dos** puntos suyos abiertos, y los
dos eran cláusulas del contrato, no código accesorio.

### 1. @s38:474-475, con **cambio de identidad**, no con desmontaje

Yo había escrito la respuesta tardía **desmontando** la vista. No es lo mismo:
al cambiar de identidad hay **una vista nueva en pantalla** que podría recibir lo
que llega tarde de la anterior. Dos pruebas más, exactamente las que pide el
panel:

- una **201** de Ana que resuelve cuando Bea ya está en pantalla: no aparece el
  `whsec_` por ninguna parte del `body`, y la lista de Bea sigue siendo la de
  Bea (el webhook de Ana no se cuela en ella);
- una **401** tardía de esa misma petición que **no** llega al observador de
  acceso (`observeAccess`), es decir que no retira la sesión posterior.

### 2. «La vista aceptaría y pintaría una URL `http://` o un secreto arbitrario»

Cierto y no cubierto: yo había puesto el oráculo en el **cliente**, que rechaza,
pero nada decía qué hace **la vista** con ese rechazo. Trece casos nuevos de
extremo a extremo: siete de listado (url no https, id con forma de uuid pero que
no lo es, tipo fuera de catálogo, tipos desordenados, `createdAt` y `updatedAt`
nulos, estado no definido), cinco de secreto y uno de entrega.

Acreditado corriendo mutantes **del cliente** contra la **suite de la vista**:

```
ROJO 57  "https://" -> ""            · refuses to paint a listed webhook with a url that is not https
ROJO 3   uuid(value) -> true         · …with an id of the right length that is not a uuid
ROJO 233 ancla ^ del secreto         · never shows a created secret when it is one with anything in front of the prefix
ROJO 229 typeof secret !== string    · …when it is one that is not a string at all
```

Los tres últimos **sobrevivían** a la primera versión de estas pruebas: hubo que
volver a afinar los contraejemplos igual que en los racimos 2 y 4 —un id de
**36** caracteres, un secreto válido con basura **delante**, y un secreto que no
es cadena pero se **lee** como el bueno—. Un contraejemplo que falla por el
motivo equivocado no acredita nada.

### Sobre «el mecanismo entero de @s38 se puede borrar sin que nada se ponga rojo»

Era verdad, y ya no lo es. Hay que separar dos cosas que B1 junta:

- **El mecanismo** —registrar cada controlador y abortarlos todos al salir— **sí
  está cubierto ahora**. Medido borrándolo de las cuatro formas posibles:

```
ROJO MECANISMO borrar el abort de la limpieza      · 3 rojas
ROJO MECANISMO limpieza -> () => undefined         · 3 rojas
ROJO MECANISMO no registrar los controladores      · 3 rojas
ROJO MECANISMO track() no crea controlador propio  · 3 rojas
```

- **Las ocho guardas `if (!aborted)`** de la vista siguen siendo
  **equivalentes**, y lo he vuelto a medir **con la prueba que el propio panel
  prescribe** (201 y 401 tras cambiar de identidad): los ocho siguen vivos, 0
  muertos. No es un oráculo que falte. En esta vista abortar y desmontar son el
  mismo suceso, y React descarta por su cuenta los `setState` sobre un
  componente desmontado: ejecutar la rama de más no cambia ni un carácter del
  DOM. Distinguirlos exigiría exponer estado interno de la vista.

Es decir: la cláusula de @s38 **sí** queda sujeta por un oráculo que puede
fallar; lo que no se puede ganar son ocho mutantes concretos. Lo dejo medido y
por escrito para que la campaña no los persiga y para que el juez decida con el
dato, no con la suposición. Si aun así se exige matarlos, la única vía honesta
es cambiar producción para que abortar sea observable sin desmontar, y eso no lo
hago por mi cuenta.

**Muertes acreditadas en este racimo: 8** (4 del mecanismo, 4 de las guardas de
decodificación vistas desde la vista; estas últimas ya contaban desde el
cliente, así que no las sumo dos veces).

**Total de pruebas: 44 → 176** en `src/webhooks*`. 195 en verde contando
`connectors-catalog`.
