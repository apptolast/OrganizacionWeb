# Revisión independiente del publicador de inicio

**APPROVED para integrar el paquete de publicación14.** Root revisó producción y pruebas del autor aislado; no es autoaprobación ni cierre de la feature completa. Ponytail full y Caveman lite.

Lecturas ea4aee/fecda8: el nuevo tipo exige once campos, identidad de evento independiente de sesión, contexto UUID, duración entera 1–1440, instantes UTC con precisión de microsegundos y suma exacta dentro de años 0001–9999. La zona histórica se valida como texto sin consultar el catálogo. No se reutiliza ResolvedBlockTime, que exige segundos enteros y habría rechazado sesiones válidas.

La ruta nueva es work-session.started.v1 y su cola organization.work-session-started.v1. Se conserva el exchange, quorum durable, persistencia, messageId y publisher confirms existentes. Las ocho rutas anteriores mantienen sus ramas; no se modifica worker, configuración ni almacenamiento. Los rechazos de payload y los reintentos pasan por el protocolo existente.

El nuevo test Rabbit comprueba declaración compatible de cola quorum durable, cuerpo JSON byte a byte, once campos, routing, messageId, deliveryMode y contentType. Los casos de indisponibilidad y confirm incierto prueban el puerto con identidad estable; la regresión heredada ejercita fallos del adaptador real. No se atribuye a esas pruebas un reinicio de API/PG14 ni entrega exactamente una vez.

Root verificó los cuatro XML en 62c0ab: 152 PublishOutbox, 13 RabbitBrokerPublisher, 9 RabbitBrokerFailures y 11 PublisherConfiguration; total185, cero fallos, errores u omitidos. Los cuatro hashes coinciden con el freeze registrado en tdd_start_work_publisher.md y git diff --check pasó. No se repitió la suite durante la revisión.

Se conservan los ciclos inicialmente verdes de la bitácora; no se infla su evidencia RED. No se encontró un defecto bloqueante en este paquete. La mutación se realizará sobre el código integrado y el cierre14 requiere todavía core/HTTP/UI, revisión y gates finales propios. Esta aprobación no autoriza declarar terminado ni desplegado el MVP.
