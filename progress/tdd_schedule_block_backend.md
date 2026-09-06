# TDD backend — planificar bloque

Contrato aprobado a84e42f: 62 escenarios y 325 casos. Ponytail full y Caveman lite activos; arquitectura hexagonal y TDD estricto. Baseline compartido raíz 96222 EXIT 0 sin cambios de producción posteriores al corte de disponibilidad; no se repite init global. Feature activa marcada por el coordinador. No se ejecutará mutación antes de revisión.

Este autor modifica backend y esta bitácora. No cambia interfaz, estados compartidos, despliegue ni acciones bloqueadas. Cada ciclo registra resultado real y separa herencia inicialmente verde de una implementación nueva.

## Ciclos

1. RED por BlockRequest ausente; GREEN de normalización Unicode White_Space, interior intacto e intención local conservada.
2. Cuatro objetivos ausentes/vacíos/demasiado largos fallan inicialmente; GREEN de REQUIRED/TOO_LONG por puntos de código.
3. RED por resolución ausente; GREEN de UTC inequívoco con offsets null, instantes y duración exactos.
4. Tres gaps reales de Madrid/Lord Howe fallan con la resolución inicial; GREEN de NONEXISTENT_LOCAL_TIME sin desplazamiento, por extremo.
5. Dos overlaps ambiguos fallan antes de excepción/opciones; GREEN con AMBIGUOUS_OFFSET y ocurrencias ordenadas.
6. Dos offsets explícitos ajenos al conjunto válido fallan; se añade INVALID_OFFSET conservando opciones reales. Verificación focal GREEN.

7. Duración cero, negativa, superior al máximo y 60,5 minutos reales de Monrovia fallan; GREEN sin redondeo ni truncado.
8. Inicio un microsegundo anterior al reloj falla antes de guarda; GREEN de IN_PAST.
9. Una zona Java válida pero fuera del catálogo permitido se aceptaba; GREEN de INVALID_VALUE antes de resolver.

10. Dos instantes resueltos fuera del rango UTC público fallan; GREEN de OUT_OF_RANGE por extremo.
11. RED por cálculo de presupuesto ausente; GREEN de reparto UTC en dos días, 1800 segundos por día.
12. Apia 2011 omitía una fecha local: el primer cálculo exponía una fila de cero segundos. RED confirmado y GREEN omitiendo exclusivamente intersecciones no positivas.
13. El cálculo inicial ignoraba las reservas existentes. RED y GREEN sumando sólo sus intersecciones con cada día y calculando el exceso en segundos.
14. Una fecha de presupuesto anterior al año 0001 se exponía. RED y GREEN de startLocal/OUT_OF_RANGE.
15. Una fecha final de presupuesto posterior al año 9999 se exponía. RED y GREEN de endLocal/OUT_OF_RANGE. BlockBudgetTest: cinco casos verdes al cerrar este ciclo.

16. Ocho reconstrucciones de intención con fechas nulas, años fuera del rango, segundos/nanos o zona ausente fallan; GREEN con invariantes del constructor.
17. Siete reconstrucciones de tiempo incoherentes se aceptaban. RED y GREEN protegiendo identidad temporal, offsets, duración exacta y años públicos.
18. RED por puerto y aplicación de preview ausentes; GREEN de snapshot coherente con disponibilidad, duración y presupuesto, sin escritura.
19. RED de disponibilidad ausente; GREEN de AvailabilityRequiredException antes de comprobar recursos completados.
20. Tres combinaciones de recursos completados fallan inicialmente; GREEN de precedencia proyecto antes de tarea.
21. RED de zona histórica de presupuesto no resoluble; GREEN con error específico seguro, sin reemplazar la preferencia.
22. RED por creación y puerto transaccional ausentes; GREEN de bloque y evento del proyecto con identidades, tiempo y contenido coherentes.
23. Tres casos de disponibilidad/revisión fallan en creación; GREEN antes de comprobar elegibilidad.
24. Tres casos de elegibilidad fallan en creación; GREEN respetando proyecto antes de tarea.
25. El primer creador aceptaba un exceso sin consentimiento. RED y GREEN de error con zona y días actuales. PlanBlockTest: 14 casos verdes al cerrar.

26. Casey 2010 invertía las fechas locales de los extremos y devolvía días vacíos. RED y GREEN avanzando por anclas consecutivas hasta el instante final. El segundo vector, completamente dentro de la hora repetida, pasa inicialmente gracias a esa corrección; no se atribuye un segundo cambio de producción.
27. RED por conflicto sin identidad; GREEN de solape semiaabierto con selección por inicio e ID lexicográfico, coherente con UUID PostgreSQL.
28. Creación con consentimiento admitía solape. RED y GREEN conservando rechazo incondicional del solape.
29. La zona histórica desconocida en creación propagaba DateTimeException. RED y GREEN de AvailabilityZoneUnavailableException también en esa ruta.
30. Refactor en verde: una evaluación común para preview y creación conserva la precedencia y evita duplicar reglas.
31. Un reloj que avanzaba exponía createdAt de una segunda lectura. RED y GREEN capturando una sola vez, con precisión de microsegundos al persistir.
32. Nueve reconstrucciones de bloque inválidas se aceptaban. RED y GREEN de identidad, precisión de createdAt y correspondencia intención/instantes. Dos fixtures anteriores eran deliberadamente incoherentes y se corrigieron conservando sus aserciones; regresión focal verde.
33. Casos válidos de 1 y 1440 minutos, igualdad con Clock, Lord Howe, París histórico y años públicos pasan inicialmente con la implementación existente. No se presenta como nuevo RED.
34. Días de 23/25 horas y ancla interna del año 10000 pasan inicialmente con el cálculo existente.
35. Mutar las listas originales cambiaba la instantánea. RED y GREEN con copias inmutables de contexto y preview. Formato global y foco de clases Block verdes; todavía sin adaptadores ni mutación.

36. Retoma: foco *Block* real, 85 casos, 84 verdes y un único RED de ScheduleBlockApiTest.s1_previewReturnsExactSnapshotWithoutWriting por ruta ausente. GREEN de preview HTTP y adaptación PostgreSQL con bloqueos SHARE y puertos existentes. Creación aún no implementada.
37. RED de estructura HTTP (18 de 19 casos); GREEN para campos obligatorios, tipos textuales y campos desconocidos. Objective null heredaba REQUIRED; no se atribuye cambio nuevo.
38. RED de siete fechas locales no estrictas; GREEN mediante forma fija y LocalDateTime, manteniendo INVALID_FORMAT por campo. Año cero ya rechazado por dominio.
39. RED de cuatro offsets no canónicos; GREEN verificando el ID exacto de ZoneOffset y traduciendo sintaxis inválida.
40. RED de JSON sintácticamente inválido, duplicado y trailing; GREEN con lector Jackson estricto y MALFORMED_JSON. Cuerpo vacío conservaba error compartido.
41. RED de cinco raíces JSON no objeto; GREEN con validación body/INVALID_TYPE. Foco HTTP: 41 casos verdes. Pendientes: precedencia, propiedad, errores temporales extendidos, creación/lecturas, V11, concurrencia y séptima ruta. Sin revisión final ni mutación.
42. RED de query e IDs con cuerpo roto; GREEN de precedencia query, projectId y taskId antes de parsear JSON.
43. RED de cuatro contextos no propios/existentes sin disponibilidad; GREEN de RESOURCE_NOT_FOUND antes de reglas de planificación.
44. RED de creación para idea/active/paused; GREEN de V11, inserción de bloque y evento en una transacción y DTO de nueve campos/Location. Prueba comprueba identidades distintas de key, payload cerrado de doce campos, microsegundos y ausencia de cambios en proyecto/tarea/disponibilidad. Foco HTTP: 51 casos verdes.

Cesión coordinada de publicador al autor independiente resume_review: OutboxMessage, RabbitBrokerPublisher, configuración/flujo si lo exige su RED y sus pruebas. Usa bitácora tdd_schedule_block_publisher.md. Este autor conserva HTTP/PG; no ejecuta mutación. V11 añade FK a tasks, por lo que los fixtures de integración anteriores necesitan incluir planned_blocks en sus TRUNCATE; se ajustarán sin cambiar sus aserciones.
45. RED de ocho formas de headers y precedencia; GREEN con revisión obligatoria 428, sintaxis configurada cerrada, key canónica, duplicados y overflow. Revisión distinta ya heredaba 412. Foco HTTP: 60 verdes. La aserción de IDs se ajusta a UUID válidos generados por servidor; no impone desigualdad probabilística con key (aclaración del coordinador), aunque el generador los obtiene independientemente.
46. RED de siete defectos del cuerpo de creación; GREEN de permiso booleano obligatorio y esquema cerrado. Offsets null ya heredaban REQUIRED. Foco HTTP69GREEN.
47. RED de replay confirmado en ocho situaciones (pasado, completados, preferencia cambiada, revisión, catálogo retirado, zona de presupuesto irresoluble, preferencia ausente y normalización NBSP). Un fixture de tarea completed se ajustó primero a la restricción existente completed_at=updated_at; el RED se repitió con los ocho fallos de comportamiento. GREEN de lectura de intención guardada antes de disponibilidad y evaluación, sin interacciones con catálogo, DTO/Location originales y una sola outbox. Foco HTTP77GREEN. Diferente intención y recuperación/lecturas aún pendientes.
48. RED de cambio en cada uno de siete campos de la intención con misma key; GREEN de comparación exacta del BlockRequest normalizado y 409 IDEMPOTENCY_CONFLICT sin datos extra/escrituras. Foco HTTP84GREEN.
49. Hallazgo del coordinador reproducido: cuatro RED por cuerpo vacío que Spring rechazaba antes de query/IDs/headers. GREEN con RequestBody opcional y validación explícita posterior; cuerpo vacío con contexto sintáctico válido conserva MALFORMED_JSON. Foco HTTP90GREEN. Próxima frontera incluye lecturas, presupuesto/solapes almacenados, concurrencia y rollback.

El autor resume_review asume además los cinco tests de núcleo (BlockRequestTest, ResolvedBlockTimeTest, BlockBudgetTest, PlannedBlockTest, PlanBlockTest) para cerrar los huecos de trazabilidad ya revisados. No se duplican aquí esas pruebas; este autor conserva HTTP/PG. Se autoriza Spotless global backend en la frontera HTTP90GREEN, sin ediciones Java simultáneas.

## Cobertura de núcleo cedida tras freeze del publicador — 2026-09-06

El coordinador y autor backend ceden los cinco archivos de pruebas del núcleo a este autor. Se completan los pendientes de review_schedule_block_domain.md sin modificar producción salvo un RED real comunicado. Se distinguen casos inicialmente verdes de nueva implementación. El publicador permanece congelado según su bitácora propia.
50. RED de catorce lecturas detalle/by-request: propio, ausente, ajeno, contexto inexistente, otra tarea, completados y zona de disponibilidad irresoluble. GREEN de puertos de lectura, consultas con propiedad previa y BLOCK_NOT_FOUND diferenciado. Conserva DTO y no consulta catálogo ni escribe. Foco HTTP104GREEN.
51. RED de lista vacía/paginada; GREEN de 21 reservas reales distintas con empate createdAt, páginas 20/1, UUID descendente sin omisiones, DTO cerrado y cursor opaco. Foco HTTP105GREEN. Se autoriza al autor frontend el snapshot Docker para smoke creación/replay/GET; no se acredita todavía validación de cursor, errores de negocio, reservas globales, concurrencia ni rollback.
52. RED de 21 variantes de cursor/query (incluidas colecciones/contextos ajenos, JSON estricto, duplicados, años/precisión y queries de detalle/by-request). GREEN de validación previa a lectura con field cursor/query; focal HTTP126GREEN. Dos E2E reales ya informados por autor frontend: flujo base y ACK perdido recuperado tras completar proyecto; evidencia propia en sus artefactos.
53. RED de cinco errores de disponibilidad/tarea/zona; GREEN con conflictos 409 cerrados y precedencia conservada. Proyecto completed y revisión distinta ya heredaban sus respuestas correctas. Foco HTTP134GREEN.
54. RED de cuatro errores de offset ambiguo/inválido por extremo; GREEN de VALIDATION_ERROR con una entrada field/code/message y validOffsets cerrado, opciones canónicas en el orden del dominio. Foco HTTP138GREEN. Autor frontend informado para E2E DST.

Nueva cesión acordada por coordinador: resume_review posee exclusivamente PostgresBlockStore.java, V11 y nuevo ScheduleBlockPersistenceTest.java; bitácora tdd_schedule_block_persistence.md. Cierra carga de reservas propias, recheck de key, restricciones, rowcounts, fallos, snapshot y carreras. Este autor conserva HTTP/ApiErrors, puertos/lectura y ScheduleBlockApiTest. Targets PIT schedule_block configurados sin ejecutar: incluye nuevos modelos/lecturas, store/persistencia/HTTP y publicador compartido; umbral80 intacto.
55. RED de BUDGET_EXCEEDED con presupuesto0/30; GREEN de error cerrado con budgetZoneId/days actuales y cero escrituras. HTTP140GREEN también con carga de reservas del autor de persistencia.
56. RED de BLOCK_OVERLAP en preview y creación con permiso de exceso; GREEN de conflict cerrado con id/projectId/taskId propios, sin otros datos ni nuevas escrituras. HTTP142GREEN. Frontend informado para siguiente snapshot real; no mutación ejecutada.
57. RED de cinco raíces no objeto en creación; GREEN con body/INVALID_TYPE antes de acceder a campos. HTTP147GREEN. Siguiente comprobación de seguridad usa filtros existentes; no se presupone nuevo RED de producción.
58. Seguridad de cinco rutas, origen no permitido y CSRF inválido: siete casos inicialmente verdes con filtros existentes. No cambios de producción.
59. Replay con header ausente, key/cuerpo inválidos, pérdida de propiedad, sesión o CSRF: seis casos inicialmente verdes, sin bloque/evento adicional. No cambios de producción.
60. JSON ilegible de creación (duplicado, sintaxis rota, trailing y vacío): cuatro casos inicialmente verdes con handler existente. No cambios de producción. Foco acumulado esperado164HTTP; falta regresión completa tras ventanas de persistencia.
61. Misma key en otra tarea y otro intervalo: inicialmente GREEN, dos bloques/outbox y recuperación ligada a la tarea solicitada. No cambios de producción.
62. Fixtures de lectura con createdAt contrapuesto al UUID: inicialmente GREEN, prioridad temporal antes del desempate y cero escrituras. No cambios de producción.
63. Regresión HTTP completa166GREEN (sesión40098, EXIT0) con las restricciones y cambios de persistencia ya disponibles. Pendiente comprobar traducción503 tras el último ciclo de lecturas del autor PG, refactor de parseo duplicado y freeze/formato común antes del juez.

### Ciclo 64 — @s26 errores de lectura HTTP
- Tras el RED/GREEN de almacenamiento del propietario de persistencia (bitácora propia), se ejecuta el único test preparado `s26_readStorageFailureIsSafe503`: cuatro operaciones reales (preview, lista, detalle y recuperación) con tabla temporalmente inaccesible y restaurada en finally.
- GREEN inicial en esta capa: 4 casos, salida 00469b, 17 s. Confirma 503 `STORAGE_UNAVAILABLE`, problem cerrado, no-store y ausencia de detalles SQL o escrituras outbox. No cambia producción HTTP; reutiliza el manejo de almacenamiento probado por persistencia.
- Frontera GREEN para extraer la lectura JSON duplicada de ambos POST conservando la precedencia de headers y la validación específica de creación.

### Ciclo 65 — refactor y freeze HTTP
- En GREEN, ambos POST comparten `request(raw, creation)`: JSON estricto cerrado y construcción de BlockRequest. La creación conserva headers antes de leer JSON, boolean obligatorio y offsets no nulos; preview conserva sus seis campos y offsets opcionales.
- Freeze Java de persistencia confirmado por su autor antes de Spotless global. Regresión conjunta `gradlew.bat spotlessApply test --tests '*ScheduleBlockApiTest' --tests '*ScheduleBlockPersistenceTest' --no-daemon`: salida 949b02, EXIT 0, 38 s. XML comprobados en 96219f: **170 HTTP + 38 persistencia, cero fallos y cero errores**.
- HTTP, puertos/modelos de lectura y configuración PIT Java congelados para juez. ApiErrors no cambió; los nuevos handlers están en BlockController. Scope PIT incluye ReadBlocks, el store y el package real de ScheduleBlockPersistenceTest; no contiene el inexistente OutboxMessageTest. No se ejecutó mutación.
- No se encontró un documento OpenAPI/Swagger adicional en docs ni recursos main; el contrato público sigue siendo project-spec sección 11 y features/schedule_block.feature aprobados. No se inventó otra especificación.

### Mapa de trazabilidad backend para revisión independiente

Los números de métodos corresponden a ScheduleBlockApiTest; las variantes temporales y transaccionales se contrastan con las bitácoras específicas, no se presentan como ejecutadas dos veces.

| Gherkin | Evidencia concreta |
| --- | --- |
| @s1 | s1_previewReturnsExactSnapshotWithoutWriting |
| @s2 | s2_createAtomic (idea/active/paused, DTO, microsegundos, SQL y outbox) |
| @s3 | BlockRequestTest; complemento Unicode500 en tdd_schedule_block_core_coverage.md |
| @s4–@s5 | s4_invalidStructure, s4_creationStructure, s4_rootObject, s4_creationRootMustBeObject, s5_strictLocal |
| @s6–@s10 | ResolvedBlockTimeTest/PlanBlockTest; HTTP s8_closedOffsetOptions y s9_canonicalOffset; reloj/DST complementados en bitácora core |
| @s11–@s12 | BlockBudgetTest y persistencia s12 (reservas propias entre proyectos/estados, exclusión ajena) |
| @s13–@s16 | s13_s14_s16_overlapHasOnlyOwnIdentity, s15_budgetErrorHasCurrentDays; PlanBlockTest y persistencia verifican contigüidad, selección determinista, exceso y consentimiento |
| @s17–@s18 | s17_s18_businessPrecedence y s18_headers |
| @s19 | s19_queryThenIdsBeforeBody, s19_ownershipBeforeAvailability, s19_emptyBodyPreservesPrecedence, s19_allRoutesKeepSecurity, s19_s27_strictReadQueryAndCursor |
| @s20 | Respuestas/no-store en s1, s2, replay, lecturas y lista; filtros y errores en s19_allRoutesKeepSecurity |
| @s21–@s23 | s21_s22_replayConfirmedIntention, s22_keyBindsExactIntention, s22_sameKeyIsScopedToTask, s23_replayStillRequiresSecurityAndStructure |
| @s24–@s27 | s24_s26_readSavedBlock, s25_s26_listUsesStableCursor, s25_createdAtPrecedesUuidOrder, s26_readStorageFailureIsSafe503, s19_s27_strictReadQueryAndCursor |
| @s28 | BlockBudgetTest.s11_s28_projectsMidnightAndHistoricalReservationsIntoCurrentBudgetZone; historia preservada en lecturas HTTP |
| @s29–@s34 | ScheduleBlockPersistenceTest y tdd_schedule_block_persistence.md: locks PostgreSQL reales, recheck, seis órdenes, usuarios independientes, snapshot y rollback/COMMIT |
| @s35 | Recuperación HTTP por key; reinicio real y ACK perdido documentados por autor E2E (no sustituidos por mock) |
| @s36–@s37 | tdd_schedule_block_publisher.md: siete rutas y PG/Rabbit reales, clasificación, confirmación/reintento; s2_createAtomic verifica persistencia inicial |
| @s62 | s62_unreadableJson y s62_creationUnreadableJson |

Las pruebas UI @s38–@s61 y los tres motores pertenecen a la entrega frontend. Este mapa no es aprobación final ni marca feature done; queda juez independiente y puerta de mutación a través del arnés.

### Ciclo 66 — fixture de configuración tras regresión global
- Init global del coordinador (3534/3fa422) ejecuta 1338 tests backend y detecta sólo tres fallos de ProjectStateConfigurationTest para capacidad válida. XML confirma UnsatisfiedDependency por BlockQueries ausente en el contexto unitario, anterior a evaluar capacidad; también faltaban BlockPlanning/BlockCommit introducidos por feature11.
- Se añaden exclusivamente los tres mocks al fixture del contexto, conservando las aserciones de capacidad válida/inválida. Foco siete casos GREEN 69d319; SpotlessCheck posterior GREEN 852fd8. No cambio de producción.

### Ciclos 67–73 — targets cerrados de mutación por arnés
- Cambio solicitado y aprobado por coordinador tras detectar que mutate ignoraba target. Se modifica scripts/project.mjs y harness.config.json, con scripts/project.test.mjs de node:test, sin tocar motor .harness.
- 67: RED 9c5402 por ausencia de factory inyectable; GREEN 589d5c de target backend fijo `pitest --no-daemon -PmutationScope=schedule_block`. La factory createProject(runner) evita que cualquier prueba dispare mutación real, incluso en RED.
- 68: RED 281ef4 ejecutaba ambas suites; GREEN aeadfb de target frontend único, cuatro archivos completos aprobados: schedule-block-api.ts, task-blocks.tsx, task-reader.tsx, task-state.tsx. Reutiliza configuración Stryker y umbral80 existentes.
- 69: RED 2e60a1; GREEN 91b3eb de rechazo de target desconocido, con metacaracteres o fuera de mutate antes de cualquier subprocess. Los argumentos finales son constantes, no interpolación del target en shell.
- 70: default sin target o vacío inicialmente GREEN 0e6e74, conserva PIT y Stryker completos en su orden original.
- 71: RED b87938; GREEN aa9be4 del token {{target}} en configuración y main(args) que entrega exactamente task/target al ejecutor.
- 72: RED 541ae9; GREEN 90e539: test normal ejecuta node:test antes de las dos suites; lint incorpora node --check del nuevo archivo de regresión.
- 73: RED 0033ac; GREEN 65df0d: CLI rechaza argumentos extra antes de ejecutar. Formato limitado a los tres archivos del arnés; regresión final siete pruebas GREEN 15734d. No PIT ni Stryker ejecutados.
- Freeze final de fixture y arnés para nueva init/juez del coordinador. HTTP170+PG38 de ciclo65 siguen siendo última regresión conjunta de aplicación; no cambió src/main después.

### Ciclo 74 — compatibilidad del runtime PIT sin ejecutar mutación
- Hallazgo del coordinador confirmado leyendo bytecode del plugin local gradle-pitest-plugin 1.19.0-rc.3: configureTaskDefault enlaza extension.jvmArgs con childProcessJvmArgs, y PitestTask (JavaExec independiente) genera --jvmArgs desde esa lista. No incorpora el doFirst de tasks.test que configura outbox.test.classpath. La prueba OutboxRecoveryTest.s11_realProcessDeathReleasesClaimAndRetriesOriginalIdentity exige dicha propiedad.
- Con autorización, una tarea temporal de inspección en build/tmp/verify-schedule-block-pit.gradle valida únicamente configuración resuelta, sin depender de pitest/test ni lanzar procesos de pruebas. RED fd3847 por propiedad de runtime ausente.
- Ajuste mínimo: jvmArgs añade provider lazy con el mismo sourceSets.test.runtimeClasspath.asPath usado por tasks.test y conserva api.version. Scope schedule_block incluye ApplicationConfiguration completa y ProjectStateConfigurationTest, manteniendo OutboxRecoveryTest completo y threshold80.
- Inspección GREEN 643eae: PIT_CONFIG_VALID=true, 104 entradas de runtime, recoveryIncluded=true, threshold80=true. Sólo se ejecutó verifyScheduleBlockPitRuntime; **no se ejecutaron PIT, Stryker ni mutantes**. No cambia producción ni pruebas. Configuración congelada para revisión del coordinador.
