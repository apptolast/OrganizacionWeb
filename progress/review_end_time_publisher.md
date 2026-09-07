# Review del publicador de ampliaciones

APPROVED para el paquete de publicación 17, no para la funcionalidad completa.

Root revisó el validador y los oráculos del primer corte (f75feb/01d136), después el adaptador Rabbit y su prueba real (0b1047). Los cuatro hashes coinciden con end_pub_freeze_hashes.json. La nueva ruta conserva los once destinos anteriores; el test recibe los bytes originales, messageId, routing y entrega persistente, y redeclara la cola durable quorum. El esquema de once campos valida identidad independiente, revisión BIGINT canónica, rango de minutos, estado abierto y fórmula temporal exacta. No pretende comprobar plannedEndAt ausente del evento.

Evidencia del autor: Spotless y 226 casos (209 publicador y 17 Rabbit), EXIT0 7ff7c5; XML preservados localmente, ciclos y límites en tdd_end_time_publisher.md. Pruebas de indisponibilidad y confirmación perdida reutilizan el protocolo existente; no equivalen al smoke integrado pendiente. HTTP, transacción PostgreSQL, mutación y validación final quedan pendientes.
