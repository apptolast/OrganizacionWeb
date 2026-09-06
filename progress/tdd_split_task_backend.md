# TDD backend: split_task

Contrato aprobado leído: 38 escenarios y 82 casos locales, más todas las entradas heredadas. Ponytail full y Caveman lite activos. Se reutilizan Task, DTO8, bloqueo del proyecto y transporte existentes; no se introduce recorrido del árbol.

Baseline compartido: init 73511 con 486 pruebas backend, 366 frontend y lint verdes; frontend final 371 y lint. No se repite la suite inicial. Create_task está publicada en db4d20b; CI 34004667683 sigue en curso. No se atribuye despliegue.

La mutación requiere revisión previa del coordinador.

## Ciclo 1: creación y evento de aplicación
RED de compilación por puerto y caso de uso ausentes; GREEN focal CreateSubtaskTest. TaskCreation comparte una interfaz sellada entre los dos eventos reales. Se conservan identidad, propietario, título normalizado y precisión de microsegundos.

## Ciclo 2: confirmación HTTP y relación
RED HTTP 404 en SubtaskApiTest.s1; GREEN tras ruta, puerto, migración V8 y transacción compartida. Comprueba DTO8, Location, relación SQL, dos tareas (padre e hijo), único evento de nueve campos e identidad temporal confirmada.

## Ciclos 3 a 5: lectura directa y paginación
Cada comportamiento comenzó con su prueba HTTP roja: GET padre inexistente en el adaptador, GET hijos inexistente y continuación ausente. Los respectivos verdes consultan raíz confirmada, sólo hijos directos (excluyendo nietos) y 21 hijos con empate temporal, cursor exacto de cuatro campos e inserción más reciente entre páginas. La lectura de padre usa un único JOIN y distingue recurso ausente de relación nula.

## Ciclos 6 y 7: validación del evento
RED de tipo no admitido y GREEN con nueve campos. La primera ejecución verde requirió corregir una codificación del emoji en la prueba, sin rebajar el límite Unicode. Otro RED parametrizado mostró cinco identidades de padre aceptadas indebidamente; GREEN valida UUID completo, tipo escalar e identidad distinta mediante UUID.equals, incluida igualdad con mayúsculas.

## Ciclo 8: transporte real
RED IllegalArgumentException para el nuevo tipo; GREEN RabbitBrokerPublisherTest.subtask_s21 publica el JSON original con identidad y persistencia a subtask.created.v1 y organization.subtask-created.v1. Se conservan los cuatro destinos anteriores. El ciclo utiliza RabbitMQ real.

## Ciclo 9: regresión de contenido y cursor heredados
Sin nueva producción: las tablas reutilizadas resultaron verdes en el endpoint de subtareas. Se ejecutan títulos positivos y negativos, espacios Unicode, criterio opcional, estimaciones, JSON duplicado/truncado/concatenado, propiedades cerradas y media type. El generador de pruebas tuvo errores de escape de cadenas Java, corregidos antes de ejecutar las aserciones. No se presentan como rojos de comportamiento.

El cursor reutiliza todas las filas de create_task s22, más precisión, claves duplicadas, texto posterior, tipos escalares, padding, zona horaria y pertenencia al padre. Todas las respuestas rechazadas conservan no-store.

## Ciclo 10: conservación, jerarquía y privacidad
Regresiones verdes con el código compartido: tres estados abiertos conservan instantáneas completas de padre/proyecto y ETag; un nieto apunta al hijo directo; la colección plana incluye las cuatro tareas y crear una raíz emite sólo el evento histórico. Los cinco casos de privacidad comparan los cuerpos completos de POST hijos, GET hijos y GET padre contra el mismo recurso inexistente.

## Ciclo 11: transacción y carrera reales
Se reutiliza el algoritmo transaccional probado: cuatro casos de trigger PostgreSQL (excepción o cero filas en tareas/outbox) revierten todo y preservan ancestros. Dos órdenes concurrentes observan la espera real del segundo escritor con plazos acotados; si gana el cierre no hay hijo, si gana creación permanece pending tras el cierre. Todos verdes sin cambios de producción.

## Ciclo 12: sesión, caché y almacenamiento
Regresiones verdes: seis combinaciones de operaciones y sesión ausente/vencida (sesión real persistida y caducada en PostgreSQL), tres controles CSRF/origen, cuatro identificadores de ruta y diez combinaciones GET padre/hijos con HTTP 200/400/401/404/503. La indisponibilidad real por tabla temporalmente renombrada nunca se convierte en relación nula ni lista vacía. Un problema de accesibilidad del tipo interno JdbcSession se resolvió usando la interfaz estándar SessionRepository en el fixture.

## Ciclo 13: evento inválido, reapertura e integridad
Diecisiete variantes del evento recorren PublishOutbox, quedan blocked con el código exacto y nunca llaman al broker. El rechazo completed conserva las lecturas y sólo una reapertura explícita permite otra creación. Tres inserciones SQL inválidas prueban FK compuesta, padre inexistente y relación consigo mismo. Verdes sobre la implementación existente; no se atribuyen nuevos rojos.

## Ciclo 14: puertos de lectura y regresión final del alcance
ReadSubtasksTest conserva propietario, proyecto, padre y posición exactos en el puerto; comprueba 0, 1, 20 y 21 filas, además de raíz y padre confirmado. El fixture de configuración registra ambos puertos nuevos sin alterar las aserciones históricas de capacidad. Foco de 13 pruebas verde. Formato aplicado. Suite normal del alcance 29329 EXIT 0 en 26 segundos: 370 pruebas, cero fallos, errores u omisiones. Incluye regresiones de dominio/aplicación, creación y lectura plana de tareas, subtareas, broker, arquitectura y configuración. Gradle liberado para el init independiente del coordinador.

## Trazabilidad backend

| Contrato split_task | Evidencia ejecutable |
| --- | --- |
| s1 | SubtaskApiTest.s1_commitsChildRelationshipAndOnlySubtaskEvent; s1_databaseEnforcesParentIntegrity; CreateSubtaskTest.s1_createsDirectChildAndOnlySubtaskEvent |
| s2, s3, s9, s10, s34 | SubtaskApiTest.s2_s3_s9_s10_s34_preservesAncestorsAndHistoricalRoots (idea, active, paused); s9_readsConfirmedRoot; ReadSubtasksTest.s9_distinguishesConfirmedRootFromParent |
| s4 | Las tres tablas de contenido descritas abajo y s4_s5_inherited_s8_strictBody |
| s5 | SubtaskApiTest.s4_s5_inherited_s8_strictBody, propiedad parentId |
| s6 | SubtaskApiTest.s6_requiresSessionIncludingPersistedExpiration: POST hijos, GET hijos y GET padre, cada uno sin sesión y con sesión JDBC vencida |
| s7 | SubtaskApiTest.s7_writeProtection: CSRF ausente, inválido y origen extranjero con CSRF válido |
| s8 | SubtaskApiTest.s8_comparesCompletePrivacyProblemsForAllThreeResources: cinco defectos, cada uno sobre las tres rutas, compara cuerpos completos |
| s11, s12, s35 | SubtaskApiTest.s11_readsOnlyDirectChildren; s12_s35_pagesStableDirectChildren; s19_s20 caso GET hijos 200; ReadSubtasksTest.s11_s35_keepsScopeAndContinuation |
| s13, s14 | SubtaskApiTest.s13_s14_inherited_s22_rejectsInvalidCursorAndQuery, tabla detallada abajo |
| s15 | SubtaskApiTest.s15_validatesPathBeforeCursor: projectId/parentId en hijos, parentId en POST e id en padre |
| s16, s36 | SubtaskApiTest.s16_s36_requiresDeliberateReopeningBeforeAnotherCreation |
| s17 | SubtaskApiTest.s17_serializesCreationAndCompletionOnSameProject, ambos órdenes reales con espera del segundo escritor |
| s18 | SubtaskApiTest.s18_rollsBackSuppressedOrFailedWrites: tasks/outbox_events por skip/fail, instantáneas de ambos ancestros |
| s19, s20 | SubtaskApiTest.s19_s20_doesNotCacheOrInventAnEmptyRelation: ambas lecturas por los cinco estados HTTP |
| s21 | RabbitBrokerPublisherTest.subtask_s21_publishesOriginalNineFieldsToOwnQuorumRoute; integración del worker y recuperación del broker asignada al agente de integración, aún sin atribuir resultado |
| s22 | SubtaskEventTest (nueve campos y cinco identidades inválidas); SubtaskPublicationTest.s22_blocksEveryIncompatibleDivisionWithoutSending (17 variantes); regresiones de los cuatro eventos anteriores en la suite del alcance |
| s23–s33, s38 | Recorridos de interfaz asignados a frontend/integración; las rutas de datos se verifican aquí, sin atribuir pruebas de navegador |
| s37 | La creación HTTP no contacta el broker y confirma la transacción; escenario completo con worker habilitado/broker detenido asignado a integración |

### Todas las entradas heredadas, ejecutadas en POST de subtareas

| Referencia create_task | Entrada concreta | Prueba nueva |
| --- | --- | --- |
| s2 | a, 160 letras a, 160 cohetes | s4_inherited_s2_s3_s5_s7_positiveContent |
| s3 | U+0020/U+00A0/U+2003 alrededor de Mi  Ae + U+0301 tarea; conserva espacios interiores y secuencia Unicode | s4_inherited_s2_s3_s5_s7_positiveContent |
| s4 | ausente, null, número, array | s4_inherited_s4_s6_validatesJsonTypes |
| s4 | cadena vacía, Unicode White_Space, 161 cohetes | s4_inheritedRemainingContentBoundaries: emptyTitle, spaceTitle, longTitle |
| s5 | ausente, null, 2000 cohetes, texto con espacios y salto | s4_inherited_s2_s3_s5_s7_positiveContent |
| s6 | criterio numérico; estimación cadena, booleano, 1.5, 0, 1441 | s4_inherited_s4_s6_validatesJsonTypes |
| s6 | criterio de 2001 cohetes | s4_inheritedRemainingContentBoundaries: longCriterion |
| s7 | estimación ausente, null, 1, 1440 | s4_inherited_s2_s3_s5_s7_positiveContent |
| s8 | truncado, dos documentos, raíz array/null, duplicado, id/ownerId/status/createdAt/desconocida | s4_s5_inherited_s8_strictBody |
| s8 | contenido text/plain | s4_inheritedRemainingContentBoundaries: media |

Todos los rechazos comprueban ausencia de nuevas tareas y eventos; los éxitos verifican los valores confirmados y un único SubtaskCreated.v1. También se prueba un entero mayor que Integer.MAX_VALUE, sin sustituir ninguna fila contractual.

### Todas las entradas heredadas de cursor, ejecutadas en GET hijos

`SubtaskApiTest.s13_s14_inherited_s22_rejectsInvalidCursorAndQuery` ejecuta: empty (vacío), garbage (codificación), missing/extra (claves), timestamp/range (fecha inválida/fuera de rango), id (UUID incompleto), foreign (otro proyecto), repeated (query repetida), limit y unknown (parámetros no admitidos). Además verifica duplicate (clave JSON duplicada), trailing (otro documento), precision (nanosegundos), parent (otro padre), flat (cursor histórico de tres campos), parentType/projectType/dateType/idType (tipos), padding y offset. La página válida usa microsegundos y cuatro claves; no se confunden claves JSON duplicadas con query repetida.

## Alcance de mutación propuesto, todavía sin ejecutar

Perfil `-PmutationScope=split_task`, informe independiente `backend/build/reports/pitest-split-task`. Incluye TaskController, PostgresTaskCommit, PostgresTaskQueries y RabbitBrokerPublisher; Task, TaskPage, TaskPosition y OutboxMessage; CreateTask/ReadTasks/TaskCreated y CreateSubtask/ReadSubtasks/SubtaskCreated. Mantiene regresión de raíces porque esos adaptadores son compartidos. Los puertos y la interfaz sellada no contienen lógica; configuración únicamente conecta los puertos. El perfil predeterminado de CI incluye también las nuevas pruebas de adaptadores y las clases de aplicación mediante su alcance existente.

No se atribuye la mutación histórica a split_task ni se ejecuta PIT antes de la revisión del coordinador. Se conservan FRECORD desactivado, umbral 80 y margen de arranque del broker ya justificado en create_task; no se amplían tiempos por esta feature.

## Mutación tras puerta aprobada

El coordinador autorizó PIT después de inspeccionar el corte y su XML normal. Sesión 76051 EXIT 0: 235/236 (99,58 %), un superviviente equivalente de normalización y cero timeouts/NO_COVERAGE. Informe y descriptor del mutante en progress/mutation_split_task_backend.md. No hubo replay ni cambios de producción/tests; Gradle queda libre. El cierre sigue pendiente del init independiente, integración y dictamen final.

## Cierre autorizado

El coordinador emitió dictamen final APPROVED y autorizó el cierre de split_task. Init global 9396: 622 backend y 462 frontend verdes; suite frontend final 475/475 verificada por el coordinador. Mutación, E2E y límites constan en progress/judge_split_task.md y progress/history.md, conservando campañas y replays separados. Feature 8 done localmente; commit y CI propios todavía pendientes. Feature 9 permanece pending documental. No cambió producción ni se ejecutaron pruebas adicionales para este cierre.
