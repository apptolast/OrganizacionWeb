# publish_outbox — recuperación mediante muerte real de proceso

## Autorización y alcance

El coordinador y backend delegaron OutboxRecoveryTest y PublisherCrashProcess, más la propiedad test-only outbox.test.classpath. No se añadió interruptor de fallo ni hook a producción. Dos escenarios s11 ejecutan un proceso Java hijo real con PostgreSQL17.9 y RabbitMQ4.3.5 propios de Testcontainers. Un decorador exclusivamente de test señala una barrera antes de enviar o después de una confirmación real, manteniendo la transacción de claim abierta. El padre inspecciona la cola, mata únicamente ese hijo con destroyForcibly y reintenta con otro caso de uso real.

## Evidencia inicial

- Primera compilación detectó imports ambiguos Container y resolución genérica ambigua AssertJ/TransactionTemplate. Corregidos en el fixture; no se presenta este fallo como defecto de producción ni como RED de comportamiento.
- Segundo intento compila. Ejecución real en curso. El comportamiento podría ser verde desde el primer intento funcional porque el adaptador y la transacción existentes ya satisfacen el contrato; no se fabricará un rojo ni se modificará producción sin evidencia.
- Aislamiento Gradle: `.e2e-work/recovery.init.gradle` incluye sólo estas dos clases de test, compila toda producción y utiliza build/cache exclusivos. El build normal no tiene filtros persistentes y debe descubrir conjuntamente estos dos casos al final.
- Archivos temporales bajo `.e2e-work/recovery-*`, argumento classpath mediante @argfile para Windows, secretos sintéticos por entorno, salida del hijo a fichero local. Teardown mata el hijo propio si sigue vivo y borra sólo tres ficheros conocidos y su directorio; no hace borrados recursivos ni afecta procesos existentes.

## Resultados funcionales

- s11 BEFORE y AFTER: primer intento funcional verde (2 casos, Gradle exit0 en 30s). BEFORE conserva cero copias antes de matar y termina con una; AFTER demuestra una copia real aceptada antes de matar y termina con dos copias del mismo eventId y JSON original.
- En ambos casos se demuestra claim retenido con SELECT FOR UPDATE SKIP LOCKED vacío antes de matar, row completo pending sin cambios, muerte no exit0 del proceso hijo y liberación posterior de lock comprobada con NOWAIT. El nuevo caso de uso real publica y deja attempts=1, sin contabilizar el intento interrumpido.
- Se añadieron esperas acotadas de5s para desconexión PostgreSQL y requeue RabbitMQ, sin ocultar excepciones SQL inesperadas.
- s12 adicional usa RabbitMQ real y trigger PostgreSQL que rechaza UPDATE después de aceptación: mensaje confirmado permanece en cola, fila original intacta, ningún audit published, workerError STORAGE_UNAVAILABLE. Tras retirar únicamente el trigger de fixture, otro ciclo publica y quedan dos copias con misma identidad/JSON; attempts termina en1.
- Suite final de recuperación: 3 casos verdes, exit0 en35s. No se cambió producción para estos casos. Backend realizará formato global; coordinador verificará la suite normal completa para que classpath del hijo y descubrimiento conjunto también queden comprobados.
