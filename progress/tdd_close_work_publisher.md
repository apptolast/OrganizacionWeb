# Publicación16 — TDD en árbol aislado

Base contractual5cbe0b6, gate root80c94b6 comunicado desde COMMON. Propiedad: OutboxMessage, RabbitBrokerPublisher y sus tests directos. No init global duplicado; cachés existentes y Gradle daemon local768 MiB/384 MiB metaspace, cuatro workers. No copias de core ni tipos inventados: primer fixture usa JSON exacto11 del contrato, autorizado por root; el record real se comprobará cuando exista.

Cada ciclo añade un caso y ejecuta antes de cambiar producción. Logs `progress/close_publisher_cycleN_*.log`; las salidas inicialmente verdes se distinguen de RED. No campañas ni smoke en este paquete.

| Ciclo | Oráculo público | Resultado observado |
| --- | --- | --- |
| 1 | Publicar cierre running original sin notas | RED5146b5 (evento bloqueado) → mínimo tipo/esquema11 → GREENb32723 |
| 2 | Cierre paused/cero/revisión máxima y zona histórica textual | Inicialmente GREEN1a296f; no producción |
| 3 | Origen closed incompatible se bloquea INVALID_EVENT sin entrega | REDeeb843 → guarda running/paused → GREEN615c69 (3 casos) |
| 4 | Revisión no canónica | RED97bfe4 → GREEN0aa3e1 |
| 5 | Revisión fuera de BIGINT | RED24d23a → GREENd5f746 |
| 6 | Acumulado no canónico | REDd3e895 → GREENa98e10 |
| 7 | Instante con precisión inferior a microsegundo | RED481d3b → GREEN12c57e |
| 8 | Instante en año UTC0000 | REDfc4b14 → GREEN064226 |
| 9 | Fecha atribuida imposible, 30 de febrero | RED569abd → GREENbabaf0 |
| 10 | Fecha atribuida fuera de año9999 | RED754415 → GREEN613f9d |
| 11 | Fecha atribuida en año0000 | RED277f8a → GREEN70e9f7 |
| 12 | Zona atribuida vacía | RED6cf268 → GREENcde719 |
| 13 | Identidad del evento igual a sesión | RED9a3735 → GREEN1b86b9 |
| 14 | Campo de notas privado añadido al evento | Inicialmente GREENbe8c2c, esquema cerrado ya lo bloquea |
| 15 | Calendario máximo y315537897599999999µs conservados | Inicialmente GREEN5fa9e5 (15 casos) |

| 16 | Record real11 entregado en Rabbit quorum durable, bytes/ruta/props exactos | RED56cb22 (Unsupported event type) → GREEN1f3442 |
| 17 | Broker indisponible conserva payload y retry | Inicialmente GREENf2f93a |
| 18 | Confirmación perdida permite redelivery del mismo evento | Inicialmente GREEN1fb5b1 |

## Freeze de publicación

Bundle real integrado por root db8bb0e→1c4831f antes del ciclo16; no copia mutable. El test Rabbit usa WorkSessionClosed real. Regresión completa de PublishOutboxTest191 + RabbitBrokerPublisherTest16 =207 tests, cero fallos/errores/skip. Resultado final8495cb EXIT0, log `close_publisher_spotless_final.log`; XML preservados en `progress/close_publisher_final_xml`, hashes de los cuatro Java en `progress/close_publisher_freeze_hashes.json`. Diffcheck verde, sólo cuatro Java del ownership cambiados.

Los primeros intentos de Spotless con propiedad spotlessIdeHook no seleccionaron correctamente los archivos (tests verdes pero formato todavía ausente). Se corrigió con Spotless real en el árbol aislado, sin cambios fuera de los cuatro archivos, y se repitió su regresión al cambiar formato; no se atribuye formato al intento anterior. El checkpoint ya venía formateado.

@s29 acreditado en adaptadores: esquema11, origen/revisión/neto/fecha/zona, bloqueo, retry y ruta11 real conservando las diez anteriores. @s28 (HTTP durable/reinicio/retirada outbox) pendiente de integración, sin smoke ni PIT en este paquete. No se afirma exactly-once ni cierre de feature16. Los payloads conservan el texto serializado y el publicador lo entrega sin regenerarlo.
