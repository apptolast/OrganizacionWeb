# Mutación backend — feature 11 schedule_block

**Veredicto vigente: PASS — 453/454 = 99,78 % (umbral 80 %); 1 superviviente contextual, 0 sin cobertura. Segunda medición al final; primera preservada como historial.** Rol mutation_tester; precondiciones: juez APPROVED en judge_schedule_block.md e init verde documentada allí.

Comando exacto: `node .harness/harness.mjs mutate schedule_block-backend`. Inicio 2026-09-06 11:16 Europe/Madrid, sesión 35831. Scope y runtime aprobados, threshold 80 intacto. El arnés invoca PIT con mutationScope=schedule_block; no se edita código ni tests durante medición.

Primer resultado: 25 unidades de mutación en pre-scan y 12 clases de pruebas enviadas al minion. Baseline y score pendientes. Informe previsto: backend/build/reports/pitest-schedule-block (ruta aislada del informe default).

## Seguimiento parcial (no veredicto)

Baseline válida: cobertura calculada en 45 s, ninguna prueba supera 2 s; la prueba de recuperación con proceso real tarda 1250 ms. Código medido publicado por coordinador como checkpoint 3671b94, sin cambios de código durante medición.

Primera lectura parcial completa de entradas: 207 resultados volcados (197 KILLED, 6 SURVIVED, 4 NO_COVERAGE). No es total ni puntuación final. Para evitar conflictos de acceso, otros agentes no leen mutations.xml mientras PIT escribe.

Entradas observadas pendientes de inventario final con identificador/mutador exactos:
- OutboxMessage.validationCode:109 y :110, SURVIVED, changed conditional boundary: faltan eventos válidos con durationMinutes exactamente 1 y 1440.
- OutboxMessage.validationCode:128/:129/:130, NO_COVERAGE, negated conditional; :131, NO_COVERAGE, replaced return value with empty string: rama compartida de validación taskId de eventos anteriores.
- OutboxMessage.validationCode:133/:134, SURVIVED, negated conditional: rama compartida de validación parentTaskId de subtareas.
- BlockController.cursor:152 (dos entradas), SURVIVED, changed conditional boundary: faltan cursores sintácticamente válidos en años UTC 1 y 9999.

Inspección de hilos sólo lectura 08ff12: el minion restante espera lifecycles Testcontainers en un mutationTestThread recién creado; main espera MutationTimeoutDecorator. No se observó deadlock ni se interrumpió/modificó proceso alguno. Informe final pendiente.

## Resultado de primera medición (histórico)

**Veredicto: PASS.** **Score: 414/454 = 91,19 % (umbral: 80 %).** No se excluye ningún equivalente ni se corrige el denominador.

Comando exacto ejecutado mediante el arnés: `node .harness/harness.mjs mutate schedule_block-backend`. EXIT 0, salida final fed038, fin 2026-09-06 11:59:30 Europe/Madrid. PIT: 42 min 35 s (45 s de cobertura y 41 min 49 s de análisis); Gradle: 42 min 42 s. 25 unidades, 12 clases examinadas y 2469 ejecuciones de pruebas. El checkpoint de código medido es 3671b94.

| Estado | Cantidad |
| --- | ---: |
| KILLED | 414 |
| SURVIVED | 35 |
| NO_COVERAGE | 5 |
| TIMED_OUT / NON_VIABLE / MEMORY_ERROR / RUN_ERROR / NOT_STARTED / STARTED | 0 |
| Total | 454 |

Cobertura de líneas de las clases mutadas: 725/738 (98 % informado por PIT). Test strength informado: 92 %. El score requerido se calcula sobre todos los 454 mutantes, incluyendo los cinco sin cobertura.

Informes completos conservados: `backend/build/reports/pitest-schedule-block/index.html` y `backend/build/reports/pitest-schedule-block/mutations.xml`. La lista siguiente se obtiene del XML final, ya cerrado. El índice ASM distingue mutantes que comparten línea y descripción.

PASS acredita el umbral, no ausencia de huecos. No se modificó src/main, pruebas ni límites en este rol. Los 35 supervivientes y las cinco entradas sin cobertura se entregan al artesano/juez; las propuestas de prueba son recomendaciones, no pruebas realizadas ni nuevos defectos de producción confirmados.

## Inventario final y prueba pendiente

Los nombres de mutador se presentan completos para permitir correlación exacta con PIT. No se declara equivalente ni se excluye ninguna fila; donde la observabilidad necesita análisis adicional se dice expresamente.

| Estado | Archivo:línea | Método / índice ASM | Mutador | Mutación | Prueba o análisis pendiente |
| --- | --- | --- | --- | --- | --- |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:109` | `validationCode` / `468` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | Publicar un BlockPlanned válido de exactamente 1 minuto y afirmar aceptación/evento intacto. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:110` | `validationCode` / `474` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | Publicar un BlockPlanned válido de exactamente 1440 minutos y afirmar aceptación/evento intacto. |
| NO_COVERAGE | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:128` | `validationCode` / `560` | `org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator` | negated conditional | Ejercitar TaskCreated/TaskStatusChanged con taskId inválido y exigir INVALID_EVENT sin envío. |
| NO_COVERAGE | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:129` | `validationCode` / `570` | `org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator` | negated conditional | Ejercitar taskId no string en TaskCreated/TaskStatusChanged y exigir clasificación INVALID_EVENT. |
| NO_COVERAGE | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:130` | `validationCode` / `582` | `org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator` | negated conditional | Ejercitar taskId string no UUID en TaskCreated/TaskStatusChanged y exigir INVALID_EVENT. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:133` | `validationCode` / `602` | `org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator` | negated conditional | Ejercitar SubtaskCreated con parentTaskId no string y comprobar clasificación sin envío. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:134` | `validationCode` / `615` | `org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator` | negated conditional | Ejercitar SubtaskCreated con parentTaskId string no UUID y comprobar clasificación sin envío. |
| NO_COVERAGE | `backend/src/main/java/com/apptolast/organization/domain/OutboxMessage.java:131` | `validationCode` / `587` | `org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator` | replaced return value with "" for com/apptolast/organization/domain/OutboxMessage::validationCode | Afirmar exactamente INVALID_EVENT, no cadena vacía, para taskId inválido en rutas compartidas anteriores. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/http/BlockController.java:152` | `cursor` / `189` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | GET lista con cursor contextual válido en años UTC 0001 y 9999; exigir aceptación sin VALIDATION_ERROR. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/http/BlockController.java:152` | `cursor` / `192` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | GET lista con cursor contextual válido en años UTC 0001 y 9999; exigir aceptación sin VALIDATION_ERROR. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:21` | `<init>` / `47` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setHandshakeTimeout | Simular handshake que no termina y verificar el límite configurado, sin depender sólo del broker sano. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:22` | `<init>` / `53` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setConnectionTimeout | Simular conexión pendiente y comprobar presupuesto temporal de conexión. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:23` | `<init>` / `59` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setChannelRpcTimeout | Simular RPC de canal sin respuesta y comprobar timeout configurado. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:24` | `<init>` / `65` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setShutdownTimeout | Simular cierre pendiente y comprobar límite de shutdown. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:25` | `<init>` / `71` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setAutomaticRecoveryEnabled | Comprobar que pérdida de conexión no activa recuperación automática paralela al reintento outbox. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:26` | `<init>` / `77` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setTopologyRecoveryEnabled | Comprobar ausencia de recuperación automática de topología tras reconexión. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:27` | `<init>` / `82` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::useNio | Evaluar observabilidad del modo NIO en el contrato de transporte; probar comportamiento bajo bloqueo antes de proponer equivalencia. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:28` | `<init>` / `96` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setNioParams | Saturar o controlar cola de escritura NIO y comprobar límite de encolado configurado. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:30` | `<init>` / `105` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/ConnectionFactory::setExceptionHandler | Provocar excepción de transporte con datos sensibles sintéticos y verificar que el logger no los publica. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:100` | `publish` / `397` | `org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator` | negated conditional | Contrastar InterruptedException frente a fallo ordinario: marca restaurada sólo en interrupción. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:109` | `publish` / `354,474,459,383` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to com/rabbitmq/client/Connection::abort | Comprobar cierre/abort de la conexión en éxito y error con observación explícita de recursos. |
| NO_COVERAGE | `backend/src/main/java/com/apptolast/organization/adapter/broker/RabbitBrokerPublisher.java:100` | `publish` / `399` | `org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator` | removed call to java/lang/Thread::interrupt | Inyectar InterruptedException y comprobar que publish restaura la marca de interrupción. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/BlockBudget.java:14` | `calculate` / `25` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | Presupuesto válido en año local 0001 con segundos positivos; afirmar BudgetDay completo. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/BlockBudget.java:34` | `calculate` / `187` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | Índice187 cambia seconds > 0 a >= 0. Analizar si un día de cero segundos fuera de años admitidos es alcanzable desde una petición válida; probarlo si lo es, demostrar equivalencia si no. No excluido. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/domain/BlockBudget.java:34` | `calculate` / `191` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | Presupuesto válido en año local 0001 que atraviese el guard de fin; afirmar que no rechaza el día. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:93` | `changeTaskStatus` / `8` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::changeTaskStatus | Contexto fresco que obtenga y ejercite explícitamente el bean changeTaskStatus; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:33` | `createProject` / `8` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::createProject | Contexto fresco que obtenga y ejercite explícitamente el bean createProject; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:75` | `createSubtask` / `8` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::createSubtask | Contexto fresco que obtenga y ejercite explícitamente el bean createSubtask; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:63` | `createTask` / `8` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::createTask | Contexto fresco que obtenga y ejercite explícitamente el bean createTask; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:45` | `editProject` / `8` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::editProject | Contexto fresco que obtenga y ejercite explícitamente el bean editProject; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:23` | `planBlock` / `10` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::planBlock | Contexto fresco que obtenga y ejercite explícitamente el bean planBlock; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:106` | `readAvailability` / `8` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readAvailability | Contexto fresco que obtenga y ejercite explícitamente el bean readAvailability; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:14` | `readBlocks` / `7` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readBlocks | Contexto fresco que obtenga y ejercite explícitamente el bean readBlocks; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:39` | `readProjects` / `7` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readProjects | Contexto fresco que obtenga y ejercite explícitamente el bean readProjects; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:81` | `readSubtasks` / `7` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readSubtasks | Contexto fresco que obtenga y ejercite explícitamente el bean readSubtasks; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:99` | `readTaskHistory` / `7` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readTaskHistory | Contexto fresco que obtenga y ejercite explícitamente el bean readTaskHistory; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:87` | `readTaskStatus` / `7` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readTaskStatus | Contexto fresco que obtenga y ejercite explícitamente el bean readTaskStatus; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:69` | `readTasks` / `7` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::readTasks | Contexto fresco que obtenga y ejercite explícitamente el bean readTasks; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/adapter/config/ApplicationConfiguration.java:114` | `saveAvailability` / `9` | `org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator` | replaced return value with null for com/apptolast/organization/adapter/config/ApplicationConfiguration::saveAvailability | Contexto fresco que obtenga y ejercite explícitamente el bean saveAvailability; hasNotFailed por sí solo no distingue NullBean. Revisar efecto de caché Spring en PIT; no se da por equivalente. |
| SURVIVED | `backend/src/main/java/com/apptolast/organization/application/ReadBlocks.java:15` | `list` / `24` | `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator` | changed conditional boundary | Lista con exactamente 20 bloques: afirmar 20 items y nextCursor null, frente a la página de 21 ya cubierta. |

SHA-256 del XML final: 96F761C70A650A15B31EA6C7717CCE06B251EA278E48EA4815D35B8051495C68. Las secciones de seguimiento anteriores son históricas; el dictamen y el inventario final prevalecen.

## Inicio de segunda medición (histórico)

Judge de seguimiento APPROVED y323pruebas afectadas verdes, verificados por coordinador. Vuelta a rol mutation_tester sin cambios de código/tests/config durante medición. Comando exacto: `node .harness/harness.mjs mutate schedule_block-backend`, sesión30272. Se conservan targetClasses y threshold80; sólo se amplió targetTests con ApplicationWiringTest/RabbitBrokerFailuresTest y se añadieron casos revisados. El XML inicial permanece en pitest-schedule-block-initial/mutations.xml con su hash registrado. Baseline y resultado de esta segunda medición pendientes; el PASS anterior no se presenta como su resultado.

## Resultado de segunda medición — dictamen vigente

**Veredicto: PASS. Score bruto: 453/454 = 99,78 % (umbral: 80 %).** Un superviviente, cero entradas sin cobertura y cero errores/timeouts. **No se aplica ninguna exclusión ni se altera el denominador**, tampoco por el equivalente contextual restante.

Comando exacto: `node .harness/harness.mjs mutate schedule_block-backend`, sesión30272, salida final c6bc2c, EXIT0. Inicio PIT2026-09-06 12:23:11; fin12:39:29 Europe/Madrid. Checkpoint de pruebas/config medido334f47b; producción idéntica a3671b94. Judge de seguimiento APPROVED y regresión afectada323GREEN preceden esta medición.

Baseline correcta:14clases, cobertura52s, cero pruebas de más de2s. Caso más lento ApplicationWiringTest.s1_blockPlanningBeanReachesItsPortInFreshContext,1252ms. PIT total16min17s, análisis15min24s; Gradle16min26s. Se ejecutaron2276pruebas para25unidades. Cobertura de líneas733/738 (99% informado por PIT); test strength99% informado. La comparación relevante mantiene454mutantes, sin cambio de targetClasses, operadores ni umbral.

| Estado final | Cantidad |
| --- | ---: |
| KILLED | 453 |
| SURVIVED | 1 |
| NO_COVERAGE | 0 |
| TIMED_OUT / NON_VIABLE / MEMORY_ERROR / RUN_ERROR / NOT_STARTED / STARTED | 0 |
| Total | 454 |

### Comparación comprobada con XML inicial

Se correlacionan las454identidades exactas mediante clase, método, descriptor, mutador e índices ASM. No hay entradas añadidas ni retiradas. Lectura/verificación ffa5fd y d1ed01.

| Primera medición → segunda | Cantidad |
| --- | ---: |
| KILLED → KILLED | 414 |
| SURVIVED → KILLED | 34 |
| NO_COVERAGE → KILLED | 5 |
| SURVIVED → SURVIVED | 1 |

Por tanto, las ampliaciones revisadas y la selección corregida de pruebas eliminan39mutantes adicionales. Todos los supervivientes de ApplicationConfiguration (incluidos readBlocks/planBlock), los límites observables y las ramas/garantías del publicador identificadas quedan KILLED en el XML final. Esto es medición real, no inferencia por suite verde.

### Único superviviente y clasificación

- Archivo: `backend/src/main/java/com/apptolast/organization/domain/BlockBudget.java:34`.
- Clase/método: `com.apptolast.organization.domain.BlockBudget.calculate`, índice ASM187.
- Mutador: `org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator`.
- Mutación: `seconds > 0` pasa a `seconds >= 0` en el guard de año presupuestario.
- Estado: SURVIVED después de103ejecuciones seleccionadas.
- Clasificación: equivalencia contextual aceptada independientemente por coordinador en `review_schedule_block_mutation_candidates.md`, sección «Comprobación independiente del coordinador: candidato24». El inicio local ya se valida, las fechas sólo avanzan y la primera fecha fuera de rango posible es10000-01-01. Si el bucle la alcanza, ambos guards rechazan la intersección positiva; la comprobación readonly del coordinador sobre604zonas Java25 no encontró anclas iguales/invertidas ese día que generasen una intersección cero fuera de rango.
- No falta un test de entrada contractual que distinga ambas versiones bajo esos supuestos; no se fabrica una petición inválida. Reabrir el análisis si cambian el catálogo (p. ej., zona personalizada que omita ese día), los extremos admitidos o las llamadas productivas. No se excluye del score bruto99,78%.

### Artefactos conservados

- Inicial: `backend/build/reports/pitest-schedule-block-initial/mutations.xml`, SHA-256 `96F761C70A650A15B31EA6C7717CCE06B251EA278E48EA4815D35B8051495C68`.
- Segunda/final: `backend/build/reports/pitest-schedule-block/mutations.xml`, SHA-256 `785A32A3355DC4C97FB939D660C48B8FD0C86A1599B555E4451E5116DA9C0EB0`.
- HTML final: `backend/build/reports/pitest-schedule-block/index.html`.

Durante la segunda medición no se editaron producción, pruebas ni configuración. Las secciones anteriores conservan historia de la primera ejecución; este dictamen es el vigente. No se marca feature done ni se hace commit en rol mutation_tester.
