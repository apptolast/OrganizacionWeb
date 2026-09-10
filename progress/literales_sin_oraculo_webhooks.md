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
| 2 | `status='pending' AND event_type=?` (`PostgresWebhookStore.java:143-147`) | `WebhookPersistenceTest#s14_...` | pendiente |
| 3 | `ORDER BY occurred_at, event_id` (`PostgresWebhookOutbox.java:66`) | `WebhookOutboxPersistenceTest#s19_...` | pendiente |

## Evidencia del rojo

Cada fila es una rotura aplicada **al SQL de producción**, con la prueba nueva
ejecutada contra ella. Sin esto no habría nada demostrado: el defecto de partida
es justamente que la suite se queda verde.

| # | Qué rompí en producción | Qué dijo el rojo |
|---|---|---|
| 1 | `AND d.next_attempt_at <= ?` → `AND (d.next_attempt_at <= ? OR TRUE)` (la cláusula deja de filtrar) | `AssertionFailedError: la entrega recién fallada no se vuelve a reclamar en la misma pasada ==> expected: <true> but was: <false>` |
| 1b | `<=` → `<` (el vencimiento exacto deja de contar) | `AssertionError: no delivery of backoff-5248…c193f4 was claimable` — y de paso caen otras 6 de la clase, porque todas las entregas nacen con `nextAttemptAt = T` y se reclaman en `T`. Ese mutante ya estaba cubierto; el de la fila 1 no lo estaba por nadie. |

Tras cada rojo se restauró el SQL y la clase volvió a verde (8/8). `git diff`
sobre `backend/src/main` quedó vacío.

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
