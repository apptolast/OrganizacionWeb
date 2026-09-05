# publish_outbox — adaptador RabbitMQ

Responsabilidad delegada por coordinador y backend: adapter/broker/**, pruebas correspondientes y TopologyMismatchException cuando el test la requiera. No incluye el caso de uso, PostgreSQL ni scheduler.

## Ciclo1

- Test s1_confirmsPersistentOriginalJsonAndMetadataOnRealBroker: rojo compileTestJava por RabbitBrokerPublisher y dependencia amqp-client ausentes. Un primer intento previo usó carpeta duplicada y no descubrió tests; se corrigió ubicación y se obtuvo el rojo válido. No se atribuye evidencia de comportamiento al intento sin tests.
- Se añade com.rabbitmq:amqp-client con versión gestionada por BOM Spring Boot3.5.11 (5.25.0, verificación previa del backend).
- Implementación mínima declara exchange direct durable, cola quorum durable y binding; publica el JSON original con mandatory, delivery_mode2, message-id y content-type; espera confirm antes de ACCEPTED.
- Primera ejecución real en curso contra rabbitmq4.3.5-management-alpine. El test lee mensaje mediante AMQP únicamente en su broker efímero.
- Manejo completo de indisponibilidad, devolución, NACK, timeout, incompatibilidad y cierre acotado se incorporarán mediante ciclos siguientes. No es versión final ni se declara verde por compilar.

## Ciclos 1–3: evidencia de ejecución

- Ciclo 1 verde: RabbitBrokerPublisherTest confirma mensaje real con JSON original y propiedades. La espera inicial por puerto abierto provocó EOF durante arranque del broker; se corrigió la condición del contenedor a `Server startup complete`, sin alterar producción por un problema de readiness.
- Una ejecución concurrente compartió el directorio Gradle y falló por fichero binario de informe ausente. Las ejecuciones de este adaptador usan `gradlew -p backend -I .e2e-work/broker.init.gradle test --tests '*adapter.broker.*' --no-daemon`, donde el initfile cambia buildDirectory a `.e2e-work/broker-build` y limita únicamente sourceSets.test al paquete broker. Compila todas las fuentes de producción. No modifica Gradle persistente ni CI; la verificación final normal descubrirá conjuntamente todos los tests.
- Ciclo 2: s5_unavailableBrokerReturnsRetryCode rojo con IllegalStateException causada por ConnectException en puerto cerrado; verde tras devolver BROKER_UNAVAILABLE. Sin credenciales reales ni llamadas a servicios existentes.
- Ciclo 3: s5_negativeConfirmIsNotAcceptance rojo (ACCEPTED en lugar de BROKER_NACK); se cambia la espera para observar el booleano del confirm. La primera sustitución textual no aplicó y el rojo se mantuvo; cambio aplicado después con parche explícito.
- Ciclo 3 verde: tres tests completos (incluido broker real), Gradle exit0.
- El aislamiento inicial también precisó `--project-cache-dir ../.e2e-work/broker-gradle-cache` para separar el historial de limpieza de outputs. Un intento intermedio falló por ClassNotFound y se descartó como evidencia funcional.
- Ciclo 4: s5_confirmTimeoutKeepsRetryClassification rojo con BROKER_UNAVAILABLE frente a CONFIRM_TIMEOUT; verde tras capturar TimeoutException sólo alrededor de la espera de confirm. Tres tests de fallos, exit0.
- Ciclo 5: s6_realMandatoryReturnWinsOverPositiveConfirm rojo ACCEPTED frente a UNROUTABLE. Es RabbitMQ real: un spy elimina el binding justo antes del basicPublish real; la devolución y el ACK proceden del servidor. Tras registrar ReturnCallback y dar prioridad a devolución, los cinco tests pasan (exit0). El test también detectará quitar mandatory porque sin él RabbitMQ descarta silenciosamente y confirma.
- Ciclo 6: TCP real acepta conexión pero nunca responde handshake. Rojo por timeout preemptivo de 3s; verde tras handshakeTimeout1s. El socket de prueba se cierra siempre.
- Ciclo 7: test de límites del transporte rojo por recuperación automática no desactivada. Configuración mínima explicita conexión/RPC/shutdown1s, NIO con enqueue de escritura1s y desactiva las dos recuperaciones. Cierre cambia a abort(1000) en finally: no espera close RPC sin límite ni convierte confirm válido en fallo de cierre. Se prueba que nunca llama close() en canal/conexión.
- Ciclo 7 verde: siete tests pasan, incluido el TCP real sin handshake.
- Ciclo 8: s21_incompatibleQueueIsPreservedAndReported crea cola classic durable con mensaje previo en broker real. Rojo porque no lanza error de incompatibilidad; verde al identificar cierre AMQP406 y lanzar TopologyMismatchException("topology_mismatch") sin transportar causa sensible. La cola y el mensaje siguen intactos. Ninguna cola se borra desde producción; limpieza sólo en fixture propio.
- Ciclo 9: s23_clientExceptionDoesNotExposeSensitiveDetails rojo porque el handler predeterminado imprimía URI con credenciales sintéticas, owner y name. Verde con StrictExceptionHandler que suprime únicamente log(raw exception); mantiene el comportamiento de manejo de fallos. La aplicación sigue registrando resultados clasificados y el log operacional propio del contenedor RabbitMQ queda fuera del audit de aplicación.
- Ciclo 10: devolución conocida seguida de timeout rojo CONFIRM_TIMEOUT; se da precedencia a UNROUTABLE también en ese camino. No altera la condición de reintento, mejora fidelidad del código registrado.
- Ciclo 10 verde, exit0.
- Ciclo 11: cancelación InterruptedException rojo por flag perdido; verde al restaurar Thread.currentThread().interrupt(), manteniendo el resultado transitorio y cierre acotado.
- Regresión adicional s5_timedOutChannelIsAbortedBeforeFreshAttempt ya verde: primer canal tiene timeout y un ACK tardío preparado; segundo canal sin confirm conserva timeout, no consulta el canal anterior y verifica abort antes de crear el siguiente. Es prueba de aislamiento determinista, no se presenta como crash real.
- Suite completa del adaptador verde, exit0 en 37s. La mutación semántica siguiente se ejecuta sobre copia aislada de todas las fuentes de producción y sólo tests de broker, con cache/build separados; nunca se modifica el adaptador compartido durante ejecución concurrente de backend.

## Mutación semántica focalizada del adaptador

Copia aislada `.e2e-work/broker-mutation-src`, initfile propio, cache y build propios. Baseline de los 12 tests exit0. Cada mutante compila y falla por una aserción funcional, no por infraestructura:

| Mutante | Test que lo elimina | Resultado |
| --- | --- | --- |
| basicPublish mandatory=false | s6_realMandatoryReturnWinsOverPositiveConfirm | ACCEPTED incorrecto, exit1 |
| ignorar devolución y aceptar confirm | s6_realMandatoryReturnWinsOverPositiveConfirm | ACCEPTED incorrecto, exit1 |
| NACK clasificado ACCEPTED | s5_negativeConfirmIsNotAcceptance | aserción BROKER_NACK, exit1 |
| timeout clasificado ACCEPTED | s5_confirmTimeoutKeepsRetryClassification | aserción CONFIRM_TIMEOUT, exit1 |

Los cuatro mutantes fueron eliminados. No es una puntuación PIT global ni cobertura de todo el adaptador: complementa PIT de dominio/aplicación con cuatro decisiones críticas de transporte. La copia se restauró en finally y SHA256 de producción compartida permaneció idéntico. Suite completa compartida del adaptador reejecutándose tras este experimento; la verificación final del coordinador debe usar el build normal conjunto.

Cierre del adaptador: suite completa compartida de 12 tests tras mutantes exit0 (29s). Revisión independiente solicitó probar explícitamente que el ACK positivo real había ocurrido, además del resultado UNROUTABLE; se capturó el booleano devuelto por waitForConfirms real mediante spy y se añadió aserción true. La prueba real reforzada pasó exit0 (26s). No fue un cambio de producción. Backend coordina formato global y el coordinador realizará la suite conjunta final sin los initfiles de aislamiento.
