# Publicador13 — TDD en curso

Autoría limitada a OutboxMessage, RabbitBrokerPublisher, PublisherConfiguration/PublishOutbox si el comportamiento lo exige, sus pruebas directas y esta bitácora. Backend core/Store/controller/BlockChanged/migraciones pertenecen a otro autor. Sin commits, scope PIT ni mutación. Baseline init GREEN d72c00 comunicado por coordinador; no se repite simultáneamente. Ponytail full/Caveman lite y contrato13@s26–27: esquema13 cerrado, Interval4, octava ruta y zonas históricas sin TZDB.

1. Cancelación nominal: test `PublishOutboxTest.s26_changedEventPreservesOriginalEnvelope` con JSON serializado coherente, zona `Historical/Removed`, misma instancia/bytes al puerto y estado published. RED `3ce427` (1fallo, tipo no soportado), GREEN `505066` (1caso). Implementación mínima añade tipo/esquema cerrado al validador; aún falta validar contenido específico, por lo que no es entrega final.

Ventanas Gradle coordinadas con resume_backend; no formato global durante edición del otro autor.

2. Se amplía después del primer GREEN el mismo nominal a RESCHEDULED: inicialmente GREEN `fe7387` (2casos), sin fabricar RED ni tocar producción.
3. Un comportamiento de validación: revisión debe ser BIGINT positivo, nunca coaccionar string/decimal/null. Test parametrizado de seis entradas inválidas `s27_changedRevisionMustBePositiveBigint`: RED `94e397` (6fallos), GREEN `027e8f` junto a nominales (8casos). Guarda Integer/Long positivo. Jackson decodifica números pequeños como Integer y BIGINT como Long; overflow queda fuera de ambos.
4. Límites positivos1/Long.MAX_VALUE: `s26_acceptsBigintRevisionBoundaries`, inicialmente GREEN `f62a93` (2casos), sin cambio de producción.
5. Coherencia kind/after: `s27_changedKindMatchesAfter`, RED `7ed254` (4fallos), GREEN `d5e80d` junto a nominales/límites (8casos).
6. Identidades UUID y eventId distinto de changeId incluso con texto en mayúsculas: `s27_changedIdentityIsCanonicalAndEventIsDistinct`, RED `825b44` (4fallos); guarda UUID y comparación de identidad. GREEN de este foco y nominales en siguiente evidencia.

Pausa solicitada por coordinador tras ciclo6 para corregir fixtures CI en otro árbol. Pendiente real al retomar: Interval4 cerrado e instantes/duración de before/after, esquema exterior faltantes/extras y versiones, ruta Rabbit real y regresión siete rutas/retries; formato coordinado. No publicar ni aprobar este WIP como publicador terminado.

Ciclo6 cerrado GREEN 72337a (identidad y nominales/límites, 8 casos). Retoma tras corrección aislada de fixtures E2E: no se mezcló esa suite con las pruebas del publicador. Ventana siguiente reservada después de los ciclos Move y HTTP; producción publicador sigue parcial hasta validar Interval4 y octava ruta.

7. Interval4 de before/after exige objeto cerrado de cuatro campos tipados: s27_changedIntervalsHaveClosedTypedShape. Primer intento61cd99 falló compilación del fixture por captura wildcard, corregida exclusivamente en test con HashMap<Object,Object>; no cuenta como RED conductual. RED real7b357f (8fallos); guarda mínima de forma/tipos. GREEN5780d3 (8 inválidos y4 nominales/límites,12casos). Queda coherencia temporal para ciclo siguiente. Cedo Gradle a Move; no formato global.

Corrección de proceso indicada por root: los ciclos parametrizados anteriores (incluido ciclo7 con8variantes antes de implementar) no respetaron la granularidad estricta de una fila/caso observable por ciclo. Evidencia preservada, sin rehacer ni inventar RED. Desde este punto cada caso nuevo se añade y ejecuta individualmente antes del siguiente.

8. Caso individual s27_changedBeforeDurationMustMatchInstants: before de60min con duración declarada59. RED47dee8 (1fallo), implementación reutiliza constructor intrínseco ResolvedBlockTime con offsetsUTC sin resolver zoneId. GREEN8a56d6 (caso y4positivos existentes,5tests).
9. Sólo después del GREEN se añade s27_changedAfterMustContainParseableInstants, único after con startAt invalid. Inicialmente GREEN2f91a8 (1test), sin producción nueva. Valida que también after pasa por la guarda y errores de parseo se clasifican INVALID_EVENT, no se envían.

10. Caso individual RabbitBrokerPublisherTest.reschedule_s26_routesOriginalThirteenFieldsToDurableQuorumQueue: RED941f97 (Unsupported event type), mínima octava selección de ruta/cola y GREENf04699 (1test real Rabbit). Comprueba bytes originales,13campos,Interval4 antes/después, routing block.changed.v1, messageId/contentType/deliveryMode2 y redeclaración compatible durable/quorum.
11. Caso individual s27_changedEventNeverPublishesPrivateExtraField: inicialmenteGREEN406e1f, sin producción.
12. Sólo después se añade s27_changedUnsupportedVersionIsBlockedWithoutDelivery: inicialmenteGREEN57addc; blocked UNSUPPORTED_EVENT y broker nunca invocado.
13. Sólo después s27_cancelledEventRequiresExplicitNullAfter: inicialmenteGREEN8d6f76; ausencia del campo no equivale a null y se bloquea INVALID_EVENT.
Formato global Spotless8030fc EXIT0 con cesión explícita de ambos autores Java en fronteraGREEN. Regresión seis suites focales publicador/Rabbit/recuperación iniciada sesión44771, resultado pendiente.

## Entrega congelada para judge independiente

Regresión ejecutada, no inferida: `d498ea` EXIT 0, 1m8s; XML `bb2b43`: **205 tests**, cero failures/errors/skipped. Desglose: PublishOutboxTest134, RabbitBrokerPublisherTest12, RabbitBrokerFailuresTest9, PublisherConfigurationTest11, OutboxWorkTest15, OutboxRecoveryTest24. Las siete rutas y protocolo heredados se ejecutaron junto al octavo caso Rabbit real. No se ejecutó init, PIT, E2E13 ni se declara cierre de feature13.

Producción modificada sólo OutboxMessage (esquema13, identidades/revisión, kind/after e Interval4 intrínseco) y RabbitBrokerPublisher (octava selección de cola/ruta). PublishOutbox y PublisherConfiguration no requieren cambios: pipeline y configuración son comunes. Pruebas nuevas:31 invocaciones de aplicación y1 Rabbit. La política de fallo, recuperación y entrega at-least-once queda acreditada por las suites heredadas ejecutadas, sin prometer entrega única.

Mapa: @s26 nominales CANCELLED/RESCHEDULED, límites de revisión, instancia/JSON coherente al puerto y Rabbit real con bytes/metadata/colaquorumdurable; @s27 bloqueo sin envío y clasificación para forma cerrada/identidad/revisión/tipos/kind-after/instantes/duración, extra privado, versión no soportada y after explícito. Los límites intrínsecos de segundos enteros, años1–9999 y minutos1–1440 se reutilizan de ResolvedBlockTime; no se consulta ZoneCatalog/ZoneId para la zona histórica del evento.

SHA256 tras Spotless y pruebas:
- OutboxMessage.java: `2870F31213FA35AB1F8C376D1CE3194A5B04B95AF297C26B0C84FE15654703B1`.
- RabbitBrokerPublisher.java: `CB30A3B2C8A05539D5BA184EB0A1468BEDCB6B09EF837D9B16566469A1884633`.
- PublishOutboxTest.java: `FE71A667DFAEC2F9F47BB018DC2EADA865782B586C3E9F43B0529F6F5928EE7F`.
- RabbitBrokerPublisherTest.java: `DE4395972C5FE1993FAF522645AACEBBC0E5204B251395E4B159900939AC7175`.

Diffcheck bb2b43 EXIT0. Advertencia de normalización CRLF/LF de la bitácora de otro autor conservada; no se editó ese documento. Gradle se devuelve a core. Sin commits ni cambio de scope de mutación. Revisión independiente pendiente; esta autoría no se autoaprueba.
