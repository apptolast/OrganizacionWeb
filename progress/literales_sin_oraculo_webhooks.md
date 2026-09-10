# Los tres literales graves de la 25 sin oráculo — carril `claude/github-connector`

Fecha: 10 de septiembre de 2026. Puerto E2E asignado: 18096 (no se usa: los tres
hallazgos son de persistencia y no tocan la interfaz).

## Por qué existe este trabajo

PIT muta bytecode y **no muta literales de cadena**. La campaña de backend de la
feature 25 está en 89,88 % y aun así estos tres trozos de SQL podían cambiarse
—o borrarse— sin que ninguna prueba se moviera. El informe completo que los cazó
está en `progress/punto_ciego_literales.md`; aquí sólo se cierran los tres graves.

Los tres son de persistencia, así que las pruebas van **contra PostgreSQL real**
(Testcontainers): el problema es precisamente que los dobles se portan bien y el
SQL no está ejercido.

## Estado

| # | Literal | Prueba nueva | Estado |
|---|---|---|---|
| 1 | `AND d.next_attempt_at <= ?` (`PostgresWebhookWork.java:64`) | `WebhookWorkPersistenceTest#s24_aFailedDeliveryIsNotClaimableUntilTheClockReachesItsNextAttempt` | cerrado |
| 2 | `status='pending' AND event_type=?` (`PostgresWebhookStore.java:143-147`) | `WebhookPersistenceTest#s14_onlyItsOwnPendingPingBlocksTheNextPing` | cerrado |
| 3 | `ORDER BY occurred_at, event_id` (`PostgresWebhookOutbox.java:66`) | `WebhookOutboxPersistenceTest#s19_ofTwoEventsOfTheSameInstantTheSmallerEventIdGoesFirstAndTheOtherIsNotLost` | cerrado |

## Evidencia del rojo

Cada fila es una rotura aplicada **al SQL de producción**, con la prueba nueva
ejecutada contra ella. Sin esto no habría nada demostrado: el defecto de partida
es justamente que la suite se queda verde.

| # | Qué rompí en producción | Qué dijo el rojo |
|---|---|---|
| 1 | `AND d.next_attempt_at <= ?` → `AND (d.next_attempt_at <= ? OR TRUE)` (la cláusula deja de filtrar) | `AssertionFailedError: la entrega recién fallada no se vuelve a reclamar en la misma pasada ==> expected: <true> but was: <false>` |
| 1b | `<=` → `<` (el vencimiento exacto deja de contar) | `AssertionError: no delivery of backoff-5248…c193f4 was claimable` — y de paso caen otras 6 de la clase, porque todas las entregas nacen con `nextAttemptAt = T` y se reclaman en `T`. Ese mutante ya estaba cubierto; el de la fila 1 no lo estaba por nadie. |

| 2a | `status='pending'` → `status='succeeded'` (la guarda, invertida) | `AssertionFailedError: y para el otro webhook sí está en vuelo ==> expected: <true> but was: <false>` |
| 2b | `AND event_type=?` → `AND (event_type=? OR TRUE)` (el tipo deja de importar) | `AssertionFailedError: «una entrega de outbox pendiente»: 202, no 409 ==> expected: <false> but was: <true>` |
| 2c | `AND endpoint_id=?` → `AND (endpoint_id=? OR TRUE)` | `AssertionFailedError: el ping en vuelo de otro webhook no es el suyo ==> expected: <false> but was: <true>` |
| 2d | `WHERE owner_id=?` → `WHERE (owner_id=? OR TRUE)` | `AssertionFailedError: un webhook ajeno no existe para nadie más ==> expected: <false> but was: <true>` |

| 3a | `ORDER BY occurred_at, event_id` → `ORDER BY occurred_at` (se cae el desempate) | `AssertionFailedError: «se encola primero el de eventId menor A» (features/webhooks.feature:258) ==> expected: <[1111…, 2222…]> but was: <[2222…, 1111…]>` |
| 3b | `event_id` → `event_id DESC` (desempate invertido) | mismo mensaje que 3a, mismos valores |
| 3c | 3b **y además**, a modo de experimento, relajada la primera aserción de la prueba | `AssertionFailedError: expected: <WebhookCursor[…, eventId=1111…]> but was: <WebhookCursor[…, eventId=2222…]>` — el cursor salta al hermano equivocado |
| 3d | 3b **y además** relajadas las dos primeras aserciones | `AssertionFailedError: el hermano queda por delante del cursor, no detrás: no se pierde ==> expected: <[2222…]> but was: <[]>` — **la pérdida silenciosa, vista** |

Las filas 3c y 3d son experimentos sobre la prueba, no sobre el producto: existen
porque las tres aserciones están en cascada —si cae la primera, las otras dos no
llegan a ejecutarse— y quería comprobar que cada una discrimina por su cuenta y
ninguna es decorado. Las relajaciones se deshicieron acto seguido; la prueba
commiteada lleva las tres aserciones en su forma estricta.

Tras cada rojo se restauró el SQL y la clase volvió a verde (`WebhookWork` 8/8,
`WebhookPersistence` 9/9, `WebhookOutboxPersistence` 7/7). `git diff` sobre
`backend/src/main` quedó vacío las tres veces.

## Hallazgo 1 — la compuerta del backoff

`PostgresWebhookWork.claimNext` es lo único que separa los reintentos en el
tiempo: el dominio calcula el `nextAttemptAt` (`RetrySchedule`, @s24) pero quien
lo hace cumplir es esa cláusula. Ninguna prueba creaba jamás una entrega con
vencimiento futuro —en `WebhookWorkPersistenceTest` todas nacían con
`nextAttemptAt = T`—, así que la cláusula se podía borrar entera sin que nada se
moviera. Con ella relajada, `DispatchWebhooks.runCycle` (hasta 20 reclamaciones
por ciclo) quema los seis intentos contra el receptor caído en un solo tick y el
webhook acaba `disabled` con `DELIVERY_EXHAUSTED` de golpe, en vez de a lo largo
de 1 min + 5 min + 30 min + 2 h + 24 h.

La prueba nueva hace fallar de verdad una entrega (`recorded` con un 500, por el
camino del producto, no con un `UPDATE` a mano) y luego mira tres relojes:

- en `T`, recién fallada: **no** reclamable;
- un milisegundo antes del vencimiento: **no** reclamable;
- justo en el vencimiento: reclamable, la misma entrega y con `attempt = 1`.

El instante de vencimiento **no se escribe a mano**: se lee del propio resultado
del dominio (`failed.nextAttemptAt()`), para que la prueba no caduque si la tabla
de reintentos cambia. La única constante escrita, `T + 1 min`, es la primera fila
del Examples de @s24, y está ahí precisamente como oráculo del contrato.

## Hallazgo 2 — la guarda de «un solo ping en vuelo»

`hasPendingPing` es lo único que hace que `ManageWebhook.ping` devuelva 409
`WEBHOOK_DELIVERY_PENDING` (`ManageWebhook.java:69`). Su SQL **no se ejecutaba en
ninguna prueba**: sólo lo cubrían dos dobles, `FakeWebhookDeliveries` y el de
`ConnectorStatusSourcesTest`, y un doble se porta bien por construcción.

Las cuatro roturas de arriba (2a-2d) muestran que el predicado tiene ahora un
oráculo **por término**, no uno global, que es lo que pedía el hallazgo: cada
término decide una fila distinta del Examples de @s14.

- `status='pending'` → si dice `'succeeded'`, la guarda queda del revés: pings
  ilimitados mientras uno está en vuelo, y 409 en el primer ping después de uno
  entregado. Por eso la prueba mira las dos caras: con el ping pendiente dice sí,
  y tras marcarlo entregado dice no.
- `event_type=?` → sin él, cualquier entrega de outbox pendiente bloquearía el
  ping, y el contrato dice justo lo contrario («una entrega de outbox pendiente →
  202»).
- `endpoint_id=?` y `owner_id=?` → el aislamiento. Un ping en vuelo de otro
  webhook del mismo propietario no bloquea a éste, y un propietario ajeno no ve
  nada.

La entrega de tipo outbox se siembra por `enqueuePing`, que es el único punto de
inserción del almacén y escribe el `event_type` que se le dé tal cual: lo que se
ejerce aquí es el predicado de la consulta, no de dónde salió la fila.

## Hallazgo 3 — el desempate por `event_id`

La mitad `occurred_at` del `ORDER BY` sí tenía oráculo
(`WebhookRecoveryPersistenceTest:264`, contra la base real); el desempate no. La
línea 258 del contrato lo exige —«se encola primero el de eventId menor A»— pero
la prueba que la cubría, `EnqueueWebhookDeliveriesTest:169`, corre contra un
`FakeOutbox` que **se ordena a sí mismo**: demuestra que el caso de uso respeta
el orden que le den, no que el adaptador lo produzca.

Lo que está en juego no es estético. `enqueueFirstEligible` adelanta el cursor al
candidato que encola y `WebhookCursor.precedes` excluye para siempre lo que quede
por detrás: con el desempate roto, el hermano de identificador menor **se pierde
en silencio y de forma permanente**. La fila 3d de la tabla es exactamente eso,
observado: pedir candidatos con el cursor ya movido devuelve `[]` en vez del
hermano.

Dos decisiones del diseño de la prueba, por si alguien las toca:

- **El mayor se inserta primero**, a propósito. Sin desempate, PostgreSQL
  devuelve las filas en orden de escritura (comprobado: fila 3a), así que
  insertarlas al revés es lo que hace roja la prueba. Insertadas en orden
  «natural» habría pasado por casualidad.
- **Los dos identificadores son fijos**, `1111…` y `2222…`, y no aleatorios. Con
  UUID al azar «menor» puede significar cosas distintas en PostgreSQL (16 bytes
  sin signo) y en Java (`UUID.compareTo`, dos `long` con signo); con estos dos
  ambos coinciden y la prueba no depende de la suerte.

## Lo que espera la campaña de mutación

Nada nuevo, y conviene decirlo claro: **PIT no puede matar ninguno de estos tres
mutantes**, porque los tres viven dentro de literales de cadena y PIT muta
bytecode. Estas pruebas no suben el porcentaje; tapan agujeros que el porcentaje
no puede ver. Si alguien contrasta la campaña esperando movimiento en la cifra,
no lo va a haber, y eso es precisamente el punto del informe
`progress/punto_ciego_literales.md`.

## Ámbito y contrato

No se enmendó ningún `.feature`: los tres hallazgos son código que ya cumplía el
contrato sin que nadie lo comprobara. La producción está intacta (`git diff` de
`backend/src/main` vacío) y no se tocó ningún fichero compartido de los que lista
`REGLAS.md` §6 ni `REPARTO_NOCHE.md` §3.

Fuera de ámbito, anotado y no tocado: el hallazgo de gravedad media del mismo
informe, `ORDER BY d.next_attempt_at, d.id` en `PostgresWebhookWork.java:67` (el
FIFO por vencimiento), sigue sin oráculo. Con `DESC` el worker serviría siempre
lo más nuevo primero y las entregas viejas se quedarían sin enviar. La prueba del
hallazgo 1 no lo cubre: sólo hay una entrega reclamable a la vez.

---

## Adenda del 10 de septiembre — las dos garantías estructurales de la condición 13

Escritas aquí porque la condición 13 de `progress/cierre_25.md` pedía justamente
eso: no dejarlas implícitas, nombrando **el colaborador que no existe** en el
caso de uso. Ahora, además de escritas, están sujetas por una prueba.

| Cláusula del contrato | Colaborador ausente | Prueba que lo sujeta |
|---|---|---|
| @s5:80 «ni se abre conexión saliente» (`features/webhooks.feature`) | `CreateWebhook` no recibe `WebhookSender` | `CreateWebhookTest#s5_creatingHasNoSenderToOpenAnOutgoingConnectionWith` |
| @s14:198 «el receptor no ha recibido ninguna petición al terminar la respuesta HTTP» | `ManageWebhook` no recibe `WebhookSender` | `ManageWebhookTest#s14_thePingHasNoSenderToReachTheReceiverWithOnTheHttpThread` |

El argumento es de forma, no de conducta: el caso de uso que atiende la petición
HTTP —crear un webhook, o encolar un ping— **no tiene con qué salir a la red**,
porque el único que declara `WebhookSender` entre sus colaboradores es
`DispatchWebhooks`, y a `DispatchWebhooks` sólo lo llama `WebhookSchedule.tick()`
fuera del hilo HTTP. Un doble no puede demostrar esto: demostraría que hoy no se
llamó, no que no se pueda llamar.

Para que la aserción no pueda quedarse vacía —el defecto que la condición 12
destapó en otra prueba— cada una lleva una **aserción de control** sobre
`DispatchWebhooks`, donde el colaborador sí está. Medido en la misma ejecución:
el mismo predicado da `false` sobre `CreateWebhook` y `ManageWebhook` y `true`
sobre `DispatchWebhooks`. Si alguien inyecta un emisor en cualquiera de los dos
primeros, la prueba se pone roja; si el predicado se estropeara y dejara de
encontrar nada, el control se pone rojo.

**Lo que estas dos pruebas NO dicen:** que la respuesta HTTP no espere a nada
lento, ni que el worker envíe cuando debe. Sólo que por estos dos caminos no
sale una petición al receptor.

### La otra mitad de la condición 13, la que sí es de conducta

«No se resuelve DNS» (@s2:26) ya no es una promesa: el resolutor de
`CreateWebhookTest` lleva un contador de los hosts que se le piden, y las dos
pruebas afirman sobre esa lista. Evidencia del rojo en
`progress/cierre_25_carril.md`.
