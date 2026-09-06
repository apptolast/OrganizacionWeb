# TDD publicador — schedule_block

Contrato aprobado a84e42f, @s36/@s37. Ponytail full y Caveman lite. Cesión explícita del coordinador y exclusividad acordada con autor backend para OutboxMessage, RabbitBrokerPublisher y pruebas del publicador. Baseline init compartida; Gradle coordinado, sin PIT, cambios build.gradle ni commits.

## Ciclos

1. RED: PublishOutboxTest.block_s36_s37_publishesHistoricalZoneWithoutCatalogResolution falla (1/1, salida 090238) porque BlockPlanned.v1 está sin soporte. GREEN mínimo: reconocer tipo y esquema cerrado de doce campos, preservando validación común y publicación existente. El fixture conserva Historical/Removed sin consultar catálogo.

2. RED de validación específica: 27 casos, 12 fallan (3bcd3c); los otros 15 ya quedan cubiertos por esquema cerrado/versión existentes. GREEN PublishOutboxTest completo (fc2017): validar IDs completos, zoneId textual no blanco, timestamps analizables, duración Integer 1–1440 y correspondencia exacta con Duration. Sin consulta a ZoneId/catálogo.
3. RED RabbitMQ real: block_s36_routesOriginalTwelveFieldsToDurableQuorumQueue falla con Unsupported event type (bdf5fc). Se añade séptimo destino block.planned.v1 / organization.block-planned.v1 conservando las seis ramas anteriores y sus garantías de confirmación.

GREEN del ciclo 3: RabbitBrokerPublisherTest completo, salida 243053, broker real. La prueba nueva vuelve a declarar la cola con durable=true y x-queue-type=quorum, comprueba routing, JSON original de doce campos, messageId, deliveryMode=2 y contentType. Las seis rutas anteriores permanecen cubiertas por la misma ejecución.

4. Integración PG+Rabbit: 20 fallos iniciales en fixtures TRUNCATE tras nueva FK de planned_blocks (ffba2f), antes de ejecutar reglas. Corrección autorizada sólo de aislamiento de OutboxWorkTest/OutboxRecoveryTest incorporando planned_blocks. GREEN 20 casos (a6401d): 19 variantes de @s37 con status blocked, clasificación exacta, attempts sin incremento, payload/ID conservados y cola real vacía; una regresión previa de reintento. No se atribuye un cambio nuevo de producción a este tramo.
5. Revisión del coordinador detecta extremos fraccionarios o fuera de años públicos con duración coherente. RED 3/30 (aad168) para fracciones simultáneas de microsegundo, año 0000 y +10000. GREEN PublishOutboxTest (dd6815) reutilizando invariantes de ResolvedBlockTime sin llamar resolve ni ZoneId: precisión de segundos, años UTC públicos y duración coherente. Los offsets UTC sólo representan los instantes del evento; la zona histórica original permanece intacta.

6. Recuperación específica con PostgreSQL y RabbitMQ: block_s37_retriesOriginalHistoricalEventAfterUnconfirmedRealDelivery, 2 casos inicialmente GREEN (c62261), sin producción nueva. El broker confirma realmente; un spy transforma esa confirmación en nack o timeout a nivel cliente. Se comprueban pending/código/attempts/payload/ID, una entrega real previa y otra tras reintento deliberado, y published con dos intentos. La zona Historical/Removed se publica conservada. No se afirma que RabbitMQ generase espontáneamente el nack/timeout: el fallo está inyectado después de la aceptación real.

## Freeze para revisión independiente

Formato global Spotless autorizado por backend y coordinador durante frontera GREEN sin ediciones concurrentes. Ejecución final: `gradlew.bat spotlessApply test --tests '*PublishOutboxTest' --tests '*RabbitBrokerPublisherTest' --tests '*RabbitBrokerFailuresTest' --tests '*OutboxRecoveryTest' --tests '*OutboxWorkTest' --tests '*PublisherConfigurationTest' --no-daemon`, EXIT 0, salida 10879e, 43 segundos. XML leído tras terminar: **164 pruebas, 0 fallos, 0 errores, 0 omitidas**.

- PublishOutboxTest: 94.
- RabbitBrokerPublisherTest: 11.
- RabbitBrokerFailuresTest: 9.
- PublisherConfigurationTest: 11.
- OutboxRecoveryTest: 24.
- OutboxWorkTest: 15.

Cambios funcionales sólo OutboxMessage y RabbitBrokerPublisher; tests tocados PublishOutboxTest, RabbitBrokerPublisherTest, OutboxRecoveryTest y fixture TRUNCATE de OutboxWorkTest. PublisherConfiguration, PublishOutbox y PostgresOutboxWork reutilizados sin cambios funcionales. Los fixtures de recuperación siembran directamente una outbox para aislar publicación; la correspondencia con el bloque confirmado y atomicidad de creación pertenece a las pruebas HTTP/PG del autor backend.

Hashes SHA256 del corte:

- OutboxMessage.java: 9E17D13449F4AB35765189258791898E391BD4501258C63C36FDCBD059F79C4B
- RabbitBrokerPublisher.java: 6C6B38CC94825861426FF88261AA9314BCFF3860F3F2715F9E24457D5A50059B
- PublishOutboxTest.java: D6F86322FA4129CF8B5031CDD8D1E2EFEF40429093D82A98CD6720AD8F30CF01
- RabbitBrokerPublisherTest.java: DA2DFE9934C9E73D4E02540FEDECC34A7A143AC5787072C03589907F0E5C1304
- OutboxRecoveryTest.java: B292165003CA0482E93DF0B25B70A651D4B74245826678061AE333935884A293
- OutboxWorkTest.java: 8094A847BCCB48A2C9E900A642EE48940F069B187398EE419362CA8CE9D6C319

Trazabilidad: @s36 usa block_s36_routesOriginalTwelveFieldsToDurableQuorumQueue y la regresión de seis rutas. @s37 usa block_s36_s37_publishesHistoricalZoneWithoutCatalogResolution, block_s37_blocksInvalidEnvelopeWithoutPublishing, block_s37_persistsInvalidClassificationWithoutSendingToRealRabbit y block_s37_retriesOriginalHistoricalEventAfterUnconfirmedRealDelivery. Las garantías previas de proceso/rollback se mantienen mediante OutboxRecoveryTest completo. No PIT ni aprobación final de feature.
