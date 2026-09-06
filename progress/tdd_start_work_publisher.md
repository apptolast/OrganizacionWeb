# TDD de publicación14

Árbol aislado OrganizacionWeb-session-publisher, base autorizada 020f7ff. Se reutiliza el baseline validado; no init global duplicado. Propiedad: OutboxMessage, RabbitBrokerPublisher y pruebas de publicación. Sin cambios al evento core, configuración, Git, metadatos o árbol común. Ponytail full y Caveman lite.

Contrato @s26–27: once campos cerrados, agregado sesión, duración real y microsegundos, zona histórica, novena ruta y protocolo de publicación existente. Un caso por ciclo; los casos inicialmente verdes se registran sin fabricar RED.

Comando focal habitual desde backend: `.\gradlew.bat test --tests '*PublishOutboxTest.workSession*' --no-daemon --max-workers=2`. JDK local25.0.1. La primera ejecución prepara el build del árbol nuevo; sin copiar fuentes o limpiar caches globales.

## Ciclos observados

1. @s26 nominal con zona histórica: RED 8984c9 (tipo no admitido, no entrega); mínimo registro del tipo/esquema y salida nominal. GREEN 4aec74, 1/1. Se entrega la misma instancia con JSON coherente; sin tocar worker ni catálogo.

2. @s27 fin desviado 1µs: RED 5d72a4 por entrega indebida; comparación exacta Instant con suma real. GREEN 67e985, 2/2.
3. Duración cero con fin coherente: RED fc73e7; rango1–1440 antes de suma. GREEN 3e6657, 3/3.

4. Precisión submicrosegundo con extremos coherentes antes de1970: RED bb77a1; guarda nanos múltiplo1000. GREEN23b8ed,4/4.
5. Año0000 con fin en0001: RED1b2b74; mínimo límite inferior del inicio. GREENbb21cf,5/5.

6. Fin fuera9999: RED5c47d9; límite superior antes de igualdad temporal. GREEN5e65a9,6/6.
7. Identidad de proyecto abreviada: REDc49ebd; patrón UUID de contexto reutilizado para project/task. GREEN7e85fe,7/7. Desde aquí daemon local autorizado por root; sin cambios al arnés.
8. Zona en blanco: RED84c2ea; texto no vacío sin catálogo. GREEN9697d5,8/8.
9. EventId igual al agregado sesión: REDf6d86c; independencia de identidad.

9. GREEN66f7fb,9/9.
10. Texto con9decimales equivalente: RED1dab94; forma UTC hasta6decimales específica14, sin tocarResolvedBlockTime. GREEN56fceb,10/10.
11. Positivo1min año0001 conµs: inicialmente GREEN165a8d,11/11. Extraído oráculo compartido de entrega.
12. Positivo1440min hasta últimoµs de9999: inicialmente GREEN554fa3,12/12.

13. 1441min coherentes: inicialmente GREEN575e84,13/13.
14. Campo privado extra: inicialmente GREENfdcdde,14/14.
15. Duración string: inicialmente GREEN34b4be,15/15.
16. TaskId array: inicialmente GREEN7aedb2,16/16.
17. Broker unavailable seguido de entrega: inicialmente GREEN1bb96f,17/17. Misma identidad/JSON, retry1 y published2; protocolo heredado sin cambios.

18. Confirm timeout seguido de entrega: inicialmente GREEN142484,18/18. Misma identidad y JSON; no promesa exactamente una vez.
19. Ruta Rabbit real: REDfc0159 por Unsupported event type; novena rama/cola/routing. GREENc9a2e0,1/1 Testcontainers Rabbit. Verifica bytes exactos, once campos, messageId, persistent2 y JSON, declaración quorum durable compatible. Fecha previa1970 y zona histórica sin catálogo. Sin worker/config nuevos.

## Entrega congelada para revisión independiente

Formato focal GJF 1.31.0: `2b6cb4`. Regresión posterior `2249c5` EXIT0, XML `2626ab`: **185 pruebas, cero fallos, errores u omitidos**. Desglose: PublishOutboxTest 152, RabbitBrokerPublisherTest 13, RabbitBrokerFailuresTest 9 y PublisherConfigurationTest 11. Incluye los 18 casos unitarios nuevos y una integración Rabbit nueva; las demás pruebas conservan los ocho destinos y el protocolo heredado.

Comando final desde backend: `.\gradlew.bat test --tests '*PublishOutboxTest' --tests '*RabbitBrokerPublisherTest' --tests '*RabbitBrokerFailuresTest' --tests '*PublisherConfigurationTest' --daemon --max-workers=2`. Sin E2E, smoke, PIT ni suite global. No se detuvieron daemons ajenos ni se limpiaron caches.

Mapa de alcance: @s26 se acredita por entrega de la misma instancia/JSON y por el test Rabbit de once campos, routing, identidad, tipo de contenido y persistencia sobre quorum durable. @s27 queda cubierto por rechazo blocked sin llamar al broker, fin exacto, límites de tiempo/duración, contexto y tipos, zona histórica y reintentos con identidad estable ante BROKER_UNAVAILABLE/CONFIRM_TIMEOUT. Los dos últimos son pruebas del puerto de entrega; las pruebas heredadas de fallos ejercitan el adaptador real. Este paquete no demuestra por sí solo el commit HTTP/PG de inicio ni un reinicio completo del servicio14. Esos recorridos se integrarán con el core.

Sólo se entrega producción en OutboxMessage y RabbitBrokerPublisher, sus dos suites modificadas y esta bitácora. PublishOutbox, PublisherConfiguration, evento core, validadores11/13 y sus contratos permanecen intactos. No se declara aprobación propia ni cierre de feature; se espera judge y mutación integrada.

SHA256 del freeze:

- OutboxMessage.java: `05F04E4617B81123BA1BEF64C1B20BC91071347C3DD5F2989D2F10D264DE6677`.
- RabbitBrokerPublisher.java: `3A47D01079E3CD28661FA550AC769F931C3DF8D3F1386786A248528329787F65`.
- PublishOutboxTest.java: `07A0C191F4BE45821D81B6222DB3B6825C2E8C223D999A4127E79D7E40A6CFFA`.
- RabbitBrokerPublisherTest.java: `E458E9DC74EBDC92388CC96FCFE82BD29881891AD7BB44EAD38E5F54CBEFDD50`.
