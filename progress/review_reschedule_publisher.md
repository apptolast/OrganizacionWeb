# Revisión del publicador de cambios

Dictamen: APPROVED para los cuatro archivos congelados en tdd_reschedule_publisher.md. Pendiente mutación y recorrido completo desde un cambio persistido; no cierra la funcionalidad 13.

Root revisó diferencias de OutboxMessage y RabbitBrokerPublisher, validación intrínseca de ResolvedBlockTime, contrato @s26–27 y oráculo Rabbit real (0c4f92, 1592cd, e32a3f). El esquema cerrado conserva trece campos, revisión BIGINT positiva, identidad propia de evento, coherencia kind/after e intervalos de cuatro campos. La validación histórica no consulta TZDB. La octava ruta no cambia las siete selecciones previas ni el mecanismo de confirmación y recuperación.

La prueba Rabbit comprueba bytes originales, contenido, metadata y redeclaración compatible con cola durable quorum. Root comprobó en XML 0ef577 los 205 casos aprobados de las seis suites, cero fallos, errores u omisiones; coincide con ejecución d498ea y registro bb2b43 del autor. Esto incluye las regresiones de fallo y recuperación del protocolo existente, sin afirmar entrega única.

No se identifican cambios requeridos en este corte. Se conserva explícita la desviación de granularidad TDD inicial documentada por el autor; aprobación del código y pruebas no equivale a certificar un proceso individual que no ocurrió. La integración con Store, correspondencia durable entre recibo/proyección/evento y validación de mutación se revisarán por separado.
