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
| 3 | `ORDER BY occurred_at, event_id` (`PostgresWebhookOutbox.java:66`) | `WebhookOutboxPersistenceTest#s19_...` | pendiente |

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

Tras cada rojo se restauró el SQL y la clase volvió a verde (`WebhookWork` 8/8,
`WebhookPersistence` 9/9). `git diff` sobre `backend/src/main` quedó vacío.

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
