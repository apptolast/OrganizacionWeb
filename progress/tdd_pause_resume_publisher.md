# Publicación15 — TDD

Propiedad C: OutboxMessage, RabbitBrokerPublisher y tests directos. Common; ventanas Gradle coordinadas con backend. Evento real WorkSessionStateChanged congelado por su autor, sin copias ni cambios al núcleo. Ponytail full/Caveman lite. No campañas globales, PIT, smoke ni Git.

1. @s26 PAUSE nominal, `pauseResume_s26_publishesTheOriginalPauseEvent`: RED053fc3 (el tipo no era admitido, el broker no recibe evento) → GREEN202db0, 1/1. Se añade tipo/esquema12 a la validación común. Fixture serializa el record real con instantes ISO y reutiliza el oráculo de entrega de la misma instancia/bytes y persistencia published. Validaciones específicas y ruta Rabbit todavía pendientes.
2. Revisión numérica incompatible, `pauseResume_s26_blocksNumericRevision`: RED433fcf (se intentaba publicar) → GREEN05f766, 1/1. Primera guarda exige String; canonicalidad/rango pendientes de sus ciclos. Freeze solicitado por root para trasladar únicamente estos adaptadores/tests/documento a árbol aislado, sin borrar WIP común.

Traslado: COPY13a87e comparó los tres SHA256 con3178a6. Al continuar ciclos en aislado, una comprobación posterior de root377b59 detectó el test ya cambiado y se detuvo sin restaurar nada. Root preservó los originales congelados en build/handoff y verificó028179 antes de restaurar common. No se integrará ese directorio. Desde ciclo3 sólo OrganizacionWeb-pause-http; Gradle daemon768, sin ventanas de core.

| Ciclo | Oráculo individual @s26 | RED | GREEN | Cambio mínimo |
|---|---|---|---|---|
|3|blocksNoncanonicalRevision (`02`)|1a2850|50fcb2|Decimal positivo canónico.|
|4|blocksRevisionAboveBigint|6828a8|af553b|Límite Long con rechazo seguro.|
|5|blocksNegativeWorkedMicroseconds|f5ac7f|2853aa|Texto decimal no negativo canónico.|
|6|blocksUnknownAction|305c39|d9a066|Sólo PAUSE/RESUME.|
|7|blocksPauseFromPaused|264a27|61b9bc|Estado de origen correspondiente.|
|8|blocksPauseToRunning|7988e4|267b88|Estado de destino correspondiente.|
|9|blocksPauseWithAnOpenInterval|da93cc|872e48|PAUSE exige runningSince null.|

Cada fila se escribió y ejecutó por separado, una prueba por foco, con fallo por intento de publicación indebida y posterior GREEN1/1. No se introdujo una matriz anticipada.

| Ciclo | Oráculo individual @s26 | RED / inicialmente GREEN | GREEN posterior |
|---|---|---|---|
|10|RESUME válido, revisión BIGINT máxima y acumulado cero|Inicialmente GREEN24a2be|Sin producción nueva.|
|11|RESUME con runningSince distinto por1µs|579e2f|e62fe6: instante UTC válido e igual a occurredAt.|
|12|occurredAt con precisión submicrosegundo|659a74|964264: patrón temporal14 reutilizado.|
|13|occurredAt en año0000|fc7003|2cec91: límite inferior explícito.|
|14|eventId igual a identidad de sesión|296086|30561c: independencia.|
|15|Campo privado extra|Inicialmente GREEN20751f|Esquema cerrado común.|
|16|Acumulado numérico|Inicialmente GREEN87cd61|Guarda de tipo existente.|
|17|Acumulado textual `01`|Inicialmente GREENaa4903|Guarda canónica existente.|
|18|RESUME sin intervalo abierto|Inicialmente GREENa9dae3|Guarda existente.|
|19|RESUME con fecha imposible2026-02-30|Inicialmente GREEN2bf3d7|Parseo real bloquea INVALID_EVENT.|
|20|Rabbit PAUSE ruta/quorum/durable/bytes12/messageId/persistencia|afb9a5, tipo no soportado en router|3e5386: décima ruta.|
|21|Rabbit RESUME y microsegundos antes1970|Inicialmente GREEN562d72 (2casos contando PAUSE)|Extracción de helper compartido con PAUSE revalidado.|
|22|Broker no disponible y reintento con payload original|Inicialmente GREENc15042 (nuevo+regresión14)|Helper de reintento parametrizado por evento; worker intacto.|
|23|Confirmación perdida, redelivery de misma identidad|Inicialmente GREEN822e7a|No promesa de exactly-once.|

Los ciclos15–19 se ejecutaron secuencialmente: se añadió una prueba, se verificó su GREEN y sólo entonces se escribió la siguiente. Los resultados inicialmente verdes no se presentan como RED ni detecciones de mutantes. El evento no incluye inicio/acumulado anterior: la validez de su suma contra intervalos corresponde al núcleo/PG, no se inventa esa comprobación en el publicador. No se consulta TZDB ni se cambian validadores11/13/14.

## Entrega congelada para revisión

Regresión36acf7 EXIT0, XML98214b:209 pruebas, cero fallos/errores/omitidas; PublishOutbox174, RabbitBrokerPublisher15, RabbitBrokerFailures9, PublisherConfiguration11. Se conservan las nueve rutas anteriores y el protocolo existente. No fue una suite global de backend ni acredita commit HTTP15: esa parte de@s26 corresponde a PG/HTTP y smoke integrado posterior. La validación de respuestas cliente@s27 sigue en su pista frontend.

Spotless real focal sobre los cuatro archivos propios: b49e80/13c090/9be8ed/e2acbc; segunda comprobación IS CLEAN19d981/f49e2a/9de498/44191a. Diffcheck2b9eb1 verde. Worker y configuración de publicación intactos, sin nueva dependencia ni cambio de core.

SHA256def967:

- domain/OutboxMessage.java:02F882D1321C256A43050242E91F15C53A3A0E8FEAEBA9DBB960BCD451F300B6
- adapter/broker/RabbitBrokerPublisher.java:AEFF92BBE454BBE92A8958D9E7FEA2C00B6686C80600B7980556F3D510CD04D5
- application/PublishOutboxTest.java:436C10D85516B1EF344E8783E2E39BA6B9E094917E9DCD1E35B841BB7377DD04
- adapter/broker/RabbitBrokerPublisherTest.java:6863ABAFBE5B7048785DD4359DDB5796396D71C5870B0CD00409C5215DB24A17

Integración permitida sólo de esos cuatro Java y esta bitácora tras judge. La base aislada contiene core parcial de otro autor: no fusionar la rama entera ni entregar build/handoff. HTTP todavía no se ha iniciado; no hay ejecución activa al freeze.
