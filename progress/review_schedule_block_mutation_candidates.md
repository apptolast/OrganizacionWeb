# Revisión final de candidatos de mutación schedule_block

Fecha: 2026-09-06. Revisor resume_review, Ponytail full / Caveman lite. Revisión readonly; ninguna prueba, Gradle, modificación de producción/tests ni nueva medición. No autoapruebo la cobertura de núcleo/publicador/persistencia de mi autoría.

Historia: la lectura preliminar durante PIT falló por sharing-lock. Se conservó el freeze sin reintentar. Root confirmó después cierre EXIT0 y autorizó leer el informe final. XML ahora legible y completo: backend/build/reports/pitest-schedule-block/mutations.xml, 404086 bytes, LastWriteTimeUtc 2026-09-06T09:59:30.839105Z. El inventario siguiente contiene sus 35 SURVIVED y 5 NO_COVERAGE; no recalcula score ni concede aprobación de feature.

S = SURVIVED; NC = NO_COVERAGE. Las líneas corresponden a src/main/java/com/apptolast/organization. Los índices desambiguadores son los de PIT. Un superviviente indica que esta medición no distingue el cambio; no prueba por sí solo un defecto de producción ni ausencia de pruebas en todo el repositorio.

## Inventario de los 40 no detectados

| # | Clase, línea y mutación | Clasificación y prueba mínima / razón |
| --- | --- | --- |
| 1 | OutboxMessage:109, límite inferior, S | Hueco observable: evento válido de exactamente 1 minuto pasa a INVALID_EVENT. PublishOutbox con instantes separados 60s, duración1: envío y resultado publicado, identidad conservada. |
| 2 | OutboxMessage:110, límite superior, S | Hueco observable: evento válido de exactamente1440 minutos se rechaza. Variante del anterior con86400s/1440; no basta probar1441 inválido. |
| 3 | OutboxMessage:128, niega tipo parentTaskId, NC | Rama heredada SubtaskCreated: envelope cerrado válido con parent UUID distinto debe publicarse; mismo envelope con parent numérico debe bloquearse INVALID_EVENT sin envío. No equivalente. |
| 4 | OutboxMessage:129, niega regex parent, NC | Rama heredada: parent UUID canónico distinto aceptado y parent abreviado rechazado. No equivalente. |
| 5 | OutboxMessage:130, niega igualdad parent/task, NC | Rama heredada: parent distinto válido frente a parent igual a task inválido. No equivalente. |
| 6 | OutboxMessage:131, retorna cadena vacía, NC | Cambia clasificación del parent inválido. Afirmar exactamente INVALID_EVENT y ausencia de envío, no sólo que haya error. No equivalente. |
| 7 | OutboxMessage:133, niega tipo fromStatus, S | TaskStatusChanged válido pending→completed pasa a inválido. Añadir publicación positiva del envelope válido; contraste fromStatus numérico bloqueado. Los negativos actuales no sustituyen la aceptación positiva. |
| 8 | OutboxMessage:134, niega tipo toStatus, S | Mismo test positivo distingue este cambio; variante toStatus numérico acredita rechazo seguro. No equivalente. |
| 9 | BlockController:152 índice189, año inferior, S | Cursor canónico de la colección/contexto con createdAt 0001-01-01T00:00:00Z debe aceptar GET200. El mutante lo rechaza400. No requiere crear bloque en año1: el cursor es una posición, no una prueba de existencia. |
| 10 | BlockController:152 índice192, año superior, S | Igual con9999-12-31T23:59:59Z y respuesta200 (lista vacía posible); distinguir de10000 inválido. No equivalente. |
| 11 | RabbitBrokerPublisher:21, elimina handshake timeout, S | Límite operativo heredado observable. Ya existe RabbitBrokerFailuresTest.s5_tcpPeerThatNeverHandshakesIsBounded; suite ausente de scheduleBlockTests. Incluirla antes de duplicar prueba. |
| 12 | RabbitBrokerPublisher:22, elimina connection timeout, S | Configuración heredada de tiempo acotado; s5_transportAndCleanupHaveFiniteBoundsWithoutRecovery ya verifica1000ms. Hueco del alcance de medición, no equivalencia. |
| 13 | RabbitBrokerPublisher:23, elimina RPC timeout, S | Misma suite existente verifica1000ms. Evitar test espejo nuevo. |
| 14 | RabbitBrokerPublisher:24, elimina shutdown timeout, S | Misma suite verifica1000ms para cierre acotado. No equivalente. |
| 15 | RabbitBrokerPublisher:25, elimina automatic recovery false, S | Podría recuperar conexiones fuera del ciclo de reintento propietario. Suite existente verifica false. No equivalente. |
| 16 | RabbitBrokerPublisher:26, elimina topology recovery false, S | Candidata a equivalencia contextual con automaticRecovery=false: recuperación de topología no se activa sin recuperación automática. No certificar sin comprobar implementación/version de cliente. Suite heredada verifica explícitamente false; mantener primero cobertura existente. |
| 17 | RabbitBrokerPublisher:27, elimina useNio, S | Cambia transporte y garantía de cola de escritura acotada por NIO. Suite existente verifica NIO; no demostrar equivalencia por éxito del smoke. |
| 18 | RabbitBrokerPublisher:28, elimina NioParams, S | Cambia espera de encolado de escritura. Suite existente afirma1000ms; incluirla. |
| 19 | RabbitBrokerPublisher:30, elimina exception handler, S | Riesgo observable de registrar detalles sensibles. Ya existe s23_clientExceptionDoesNotExposeSensitiveDetails con canarios. Incluir suite, no duplicar. |
| 20 | RabbitBrokerPublisher:100, niega instanceof InterruptedException, S | Invierte conservación de señal de cancelación. s5_shutdownCancellationPreservesInterruptFlag ya fuerza InterruptedException y afirma flag; falta en targetTests. |
| 21 | RabbitBrokerPublisher:109, elimina abort, S | Fuga/reutilización de conexión del intento anterior. s5_timedOutChannelIsAbortedBeforeFreshAttempt verifica abort antes de segundo canal; suite excluida del foco. |
| 22 | RabbitBrokerPublisher:100, elimina interrupt, NC | Pierde cancelación; mismo test heredado del #20. No equivalente, ni hace falta nuevo caso. |
| 23 | BlockBudget:14 índice25, año<1→<=1, S | Hueco válido año0001: calculate en UTC0001-01-01T12:00–12:01 debe devolver un BudgetDay de60s, sin excepción. El test de desbordamiento a año0 no cubre esta aceptación. |
| 24 | BlockBudget:34 índice187, seconds>0→>=0, S | Candidata a equivalencia para entrada contractual: la condición sólo cambia con seconds=0 y año fuera de rango. El año inicial fuera de rango ya falla en línea14; fechas siguientes con avance y fin exclusivo no aportan día fuera de rango con intersección cero. Fechas omitidas por cambios de zona en años dentro del rango tampoco activan error. Requiere mantener esta precondición; no inventar entrada inválida sólo para matar. |
| 25 | BlockBudget:34 índice191, año<1→<=1, S | No equivalente: el mismo intervalo válido año1 del #23 supera línea14 original y falla aquí bajo este mutante. Una sola nueva prueba distingue ambos individualmente. |
| 26 | ApplicationConfiguration:93 changeTaskStatus→null, S | Bean heredado observable ausente; obtener ChangeTaskStatusUseCase del contexto y ejecutar cambio contra puerto controlado. No equivalente. |
| 27 | ApplicationConfiguration:33 createProject→null, S | Bean heredado ausente; obtener CreateProject y ejecutar creación con puerto controlado. No equivalente. |
| 28 | ApplicationConfiguration:75 createSubtask→null, S | Bean heredado ausente; resolver caso de uso y crear subtarea con puerto controlado. No equivalente. |
| 29 | ApplicationConfiguration:63 createTask→null, S | Bean heredado ausente; resolver y ejecutar creación. No equivalente. |
| 30 | ApplicationConfiguration:45 editProject→null, S | Bean heredado ausente; resolver y editar mediante puerto controlado. No equivalente. |
| 31 | ApplicationConfiguration:23 planBlock→null, S | Wiring propio feature11: ApplicationContextRunner fresco debe resolver PlanBlockUseCase y ejecutar preview válido con reloj/catálogo/puerto controlados. No equivalente. |
| 32 | ApplicationConfiguration:106 readAvailability→null, S | Bean heredado ausente; resolver y consultar disponibilidad controlada. No equivalente. |
| 33 | ApplicationConfiguration:14 readBlocks→null, S | Wiring feature11: resolver ReadBlocksUseCase en contexto fresco y listar resultado conocido del BlockQueries. No equivalente. |
| 34 | ApplicationConfiguration:39 readProjects→null, S | Bean heredado ausente; resolver y listar contra puerto controlado. No equivalente. |
| 35 | ApplicationConfiguration:81 readSubtasks→null, S | Bean heredado ausente; resolver y leer hijos controlados. No equivalente. |
| 36 | ApplicationConfiguration:99 readTaskHistory→null, S | Bean heredado ausente; resolver y leer historial controlado. No equivalente. |
| 37 | ApplicationConfiguration:87 readTaskStatus→null, S | Bean heredado ausente; resolver y consultar estado controlado. No equivalente. |
| 38 | ApplicationConfiguration:69 readTasks→null, S | Bean heredado ausente; resolver y listar tareas controladas. No equivalente. |
| 39 | ApplicationConfiguration:114 saveAvailability→null, S | Bean heredado ausente; resolver y guardar preferencia controlada. No equivalente. |
| 40 | ReadBlocks:15, size>20→>=20, S | Página terminal con exactamente20 elementos obtiene cursor fantasma. Test del caso de uso con20 filas: items20 y nextCursor null. Puede ser HTTP/PG con20 filas;21→20+cursor existente no distingue esta frontera. |

## Prioridad y alcance de los cambios propuestos

1. Añadir sólo aceptación positiva1/1440 al publicador, año0001 al presupuesto, cursor0001/9999 y página terminal20. Son diferencias públicas concretas de feature11. Conservación de zona histórica sigue sin consultar catálogo.
2. Incorporar RabbitBrokerFailuresTest existente al alcance de una medición posterior coordinada. Sus pruebas de interrupción, abort, handshake, límites y privacidad ya existen; los12 mutantes de Rabbit no justifican12 tests nuevos. Esta revisión no ha ejecutado esa suite ni afirma nuevos kills.
3. Cubrir ramas heredadas SubtaskCreated con una matriz pequeña: parent UUID distinto válido, tipo incorrecto, formato abreviado e igualdad. Afirmar código exacto y cero envíos para inválidos. Para TaskStatusChanged, añadir una publicación positiva y tipos incorrectos. Es conservación de las seis rutas previas, no ampliar feature11 funcionalmente.
4. Probar wiring en contexto fresco. ProjectStateConfigurationTest carga ApplicationConfiguration pero sólo consume ChangeProjectStatusUseCase; sus hasNotFailed no detectan necesariamente que otros @Bean hayan devuelto null. ScheduleBlockApiTest usa SpringBootTest y caché de contexto: la supervivencia no demuestra que el arranque real acepte estos nulos. Dos pruebas de wiring con operación real contra puertos controlados cubren lo nuevo; los doce beans restantes necesitan cobertura/regresión heredada o decisión explícita de alcance, no calificarlos de equivalentes ni excluirlos para mejorar el número.
5. Revisar las dos candidatas de equivalencia (#16 y #24) independientemente antes de adjudicarlas. #24 tiene argumento sobre entradas contractuales; #16 necesita corroborar el comportamiento del cliente Rabbit fijado. No he cambiado exclusiones ni aprobado equivalencias definitivas.

Conclusión de revisión: hay huecos observables y desajustes de selección de pruebas; el EXIT0 de PIT no sustituye cerrarlos o justificar su alcance. Esta lista es una propuesta para autoría TDD y revisión posterior independiente, no aprobación de mis cambios previos.

## Comprobación independiente del coordinador: candidato 24

Lectura de PlanBlock.evaluate, ResolvedBlockTime y BlockBudget: la única llamada productiva entrega extremos resueltos con duración positiva de hasta 1440 minutos y años UTC 0001–9999. BlockBudget rechaza primero una fecha local inicial fuera de 0001–9999. La fecha del bucle sólo avanza. Por tanto, una fecha fuera de rango alcanzable posteriormente empieza en 10000-01-01; no puede retroceder al año cero.

En esa fecha, el inicio original es anterior a dayStart. El bucle sólo entra si dayStart < end. Si dayEnd > dayStart, ambos candidatos de min(end, dayEnd) son posteriores a dayStart, por lo que seconds > 0. El cambio > 0 a >= 0 no altera entonces el guard. Un día omitido podría invalidar ese argumento; se comprobó expresamente el primer día del año 10000 para las 604 zonas del runtime Java 25.0.1+8-LTS-27: ninguna tiene anclas iguales ni invertidas (jshell readonly, e99b42, EXIT 0). Si se alcanzara una fecha fuera de rango con intersección positiva, ambas versiones lanzarían antes de avanzar de nuevo.

El coordinador acepta equivalencia contextual de BlockBudget:34 índice187 con este catálogo y los extremos contractuales. No se introduce exclusión ni se cambia el 91,19 % medido. Revisar el argumento si cambia el catálogo a una zona personalizada que omita ese día, o si se amplían extremos/llamadas productivas. Los otros dos límites de BlockBudget siguen siendo huecos observables que debe cubrir el autor.

## Revisión incremental del coordinador: pruebas del publicador

Diff completo revisado en 0851bc contra OutboxMessage.validationCode: nueve casos netos nuevos, sin cambios de producción. Los positivos de BlockPlanned aceptan 1, 60 y 1440 minutos con extremos coherentes y conservan la zona histórica. SubtaskCreated distingue parent válido, tipo incorrecto, UUID abreviado e identidad igual a task. TaskStatusChanged incorpora aceptación positiva y rechaza tipos numéricos en ambos estados. Los negativos comprueban INVALID_EVENT exacto y ausencia de publicación; los positivos conservan la instancia original y el resultado persistido de publicación.

El primer borrador reutilizaba JSON de un fixture antiguo aunque cambiaba su mapa. Se corrigió a serialización del payload en los fixtures modificados, utilizando ObjectMapper ya instalado. La revisión acepta diseño y aserciones. Los ciclos focales inicialmente verdes constan en tdd_schedule_block_publisher.md; no son un RED ni una medición de mutantes eliminados. Pendientes formato, regresión integrada y repetición de mutación. Esta aprobación parcial no cierra la feature.

## Revisión independiente del seguimiento (corte preliminar)

Root asignó revisión de autoría backend, excluyendo PublishOutboxTest de mi autoría. Leídos s10_acceptsBudgetInFirstPublicYear, s27_acceptsCursorAtPublicYearBoundaries y s25_exactlyTwentyBlocksIsTerminalPage: aserciones observables correctas para los mutantes identificados, sin hallazgos. ApplicationWiringTest crea contexto fresco y consume caso de uso; exige excepción PortReached del puerto correcto, de modo que un bean ausente no satisface el test. El corte inicial incluía lectura/planificación de bloques y tres lecturas heredadas; autor seguía ampliándolo. Scope Gradle todavía pendiente en ese corte. Dictamen final condicionado al freeze/formato y evidencia de regresión del autor; no se ejecutaron pruebas por el revisor.

## Dictamen del seguimiento backend tras freeze

APPROVED diseño y aserciones de los cambios de autor backend. Leído corte final formateado de ApplicationWiringTest:14 casos consumen2 beans de bloques,6 lecturas y6 escrituras heredadas en contextos frescos; cada invocación exige PortReached del puerto específico. Bean nulo o excepción de resolución no cumple esa aserción. No pretende validar reglas del caso de uso, cubiertas por sus suites; aquí acredita composición observable. Revisados también límites de año0001 en presupuesto, años0001/9999 de cursor y terminal20. Sin hallazgos concretos.

Diff build.gradle.kts añade sólo ApplicationWiringTest y RabbitBrokerFailuresTest a scheduleBlockTests, además de retirar dos líneas vacías finales. No reduce targetClasses, operadores ni umbral. Reutiliza pruebas Rabbit existentes en vez de duplicarlas.

Backend entregó freeze tras Spotless y regresión323 GREEN (6f086d/813b55). Verifiqué XML existentes, timestamp UTC2026-09-06T10:21:04: HTTP173, Budget17, Wiring14, ProjectStateConfiguration7, PublishOutbox103, RabbitFailures9; todos0 failures/errors/skipped. Esta es lectura de evidencia del autor, no ejecución nueva. PublishOutbox103 sólo se registra como resultado: su diseño/aserciones fue aprobado por root, no por mí. No se concluyen nuevos kills ni equivalencias; pendiente segunda medición.
