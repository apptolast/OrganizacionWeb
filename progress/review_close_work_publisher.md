# Revisión independiente del publicador16

APPROVED para integración de este paquete, sin cerrar16. Root580f2f/de42f3 revisó el delta de cuatro Java, los oráculos y los XML preservados:191 casos PublishOutbox y16 Rabbit,207 en total, sin fallos, errores ni omisiones. La bitácora distingue18 ciclos individuales y sus resultados inicialmente verdes. No se repiten suites desde esta revisión.

El evento Closed11 tiene esquema cerrado sin notas, identidades coherentes, origen running/paused, revisión BIGINT y acumulado decimal canónicos, instante UTC en microsegundos, fecha válida0001–9999 y zona textual no vacía. La zona histórica no se resuelve de nuevo ni se recalcula la atribución. Los validadores de eventos anteriores conservan su recorrido. La nueva ruta y cola se añaden a las diez existentes sin otro worker.

La prueba Rabbit serializa el WorkSessionClosed real, entrega sus bytes originales a la cola quorum durable y contrasta ruta, messageId, deliveryMode2 y application/json. Los tests de retry conservan el payload ante broker no disponible y confirmación perdida; no afirman entrega única. Los casos inválidos cambian el dato pertinente manteniendo coherentes payload e identidad exterior, por lo que no dependen de un fallo ajeno al oráculo.

Pendientes de la feature: commit PostgreSQL de cierre, HTTP/UI, smoke con pérdida de ACK y reinicio, trazabilidad integrada y mutación. Este paquete no demuestra por sí solo el recorrido durable @s28 ni despliegue. Se integra sólo su propiedad; el checkpoint nominal de core usado como dependencia no se vuelve a fusionar.
