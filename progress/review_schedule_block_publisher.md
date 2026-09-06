# Revisión del publicador de bloques

Veredicto del corte: APPROVED para el publicador, sin habilitar mutación ni cerrar feature 11. Contrato a84e42f, escenarios @s36/@s37. La revisión final de toda la funcionalidad y la ejecución global del arnés siguen pendientes.

El coordinador revisó los dos archivos funcionales y las cuatro pruebas modificadas indicadas en `tdd_schedule_block_publisher.md`; sus seis hashes SHA256 coinciden con el freeze (comprobación ce392c). No se modificó código ni pruebas durante esta revisión.

El evento conserva exactamente doce campos, sin objetivo ni key. La validación distingue envoltura no soportada de payload inválido, comprueba identidad, segundos enteros, rango de años y duración y permite conservar una zona histórica sin resolverla contra el catálogo. El hallazgo de extremos fraccionarios o fuera de rango quedó reproducido y corregido mediante invariantes del dominio. La séptima ruta y cola se añaden sin cambiar los seis destinos anteriores.

Las pruebas de RabbitMQ comprueban bytes originales, routing, messageId, persistencia y declaración quorum durable. Las pruebas con PostgreSQL conservan payload/identidad al bloquear eventos inválidos y al reintentar. El fallo de confirmación se inyecta después de una aceptación real; demuestra entrega repetida posible, no un nack espontáneo del broker ni entrega exactamente una vez. La outbox se siembra directamente para aislar este tramo: la creación atómica de bloque y evento se verifica por separado en HTTP/PG.

Evidencia del autor: ejecución final 10879e, Spotless y seis suites, 164 pruebas sin fallos, errores ni omisiones. El coordinador verificó código, aserciones y hashes; no presenta esta ejecución como repetición independiente. La verificación global posterior deberá incluir este corte y los cambios restantes antes de mutación.
