# create_task — TDD backend

Ponytail full y Caveman lite activos. Contrato aprobado de 35 escenarios; baseline de 384 pruebas backend y frontend final 260 confirmado por el coordinador. No se repite baseline. Agregado proyecto, tarea hija y outbox atómico; no se añade bloqueo asesor.

1. RED de compilación por Task inexistente; GREEN con identidad, normalización exterior, valores opcionales e instantes confirmados.
2. RED para títulos null, vacíos y espacios; GREEN con REQUIRED también en construcción directa.
3. RED para límites de 161/2001 puntos de código; GREEN con máximos específicos y aceptación de Unicode suplementario.
4. RED para estimación 0/1441; GREEN con intervalo cerrado 1–1440 y null opcional.
5. RED de compilación del caso de uso; GREEN con puerto transaccional, tarea hija y evento mínimo, reloj truncado a microsegundos.
6. RED para proyecto completed; GREEN al decidir dentro de la operación transaccional proporcionada por el puerto.

7. RED HTTP por ruta inexistente; GREEN con V7, POST y confirmación de tarea/outbox, ocho campos y precisión idéntica a PostgreSQL.
8. RED para tipos JSON de título, criterio y estimación; GREEN con validación sin coerción y errores por campo.
9. RED para duplicados y propiedades no admitidas; GREEN con lectura estricta del documento y lista cerrada de campos. Raíz null/array y truncamiento mantienen errores distintos.
10. RED de lectura HTTP; GREEN con puertos de consulta, lista vacía y detalle confirmado propio.
11. RED de códigos públicos para proyecto ajeno/inexistente/completed; GREEN con 404 uniforme y 409 explícito sin escrituras.
12. RED para 21 tareas; GREEN con orden descendente por fecha/id, página de 20 y continuación estable después de una creación nueva.
13. RED de 14 casos de cursor/consulta; GREEN con documento exacto vinculado al proyecto, base64url canónica, fechas PostgreSQL y errores seguros.
14. RED para UUID abreviados en las cuatro rutas; GREEN con formato completo y error en projectId/taskId.

15. RED de falso 201 cuando un trigger suprime INSERT tasks o outbox; GREEN con comprobación de una fila. Los errores SQL reales también conservan rollback y proyecto intacto.
16. RED del evento no admitido; GREEN con TaskCreated.v1 de ocho campos y límite de título 160. RED de taskId abreviado; GREEN con UUID completo. Otros defectos conservaron códigos vigentes.
17. RED del envío Rabbit por tipo no admitido; GREEN con ruta task.created.v1 y cola propia. Se comprobó JSON original, identidad y persistencia en broker real.
18. Carrera real crear/terminar: GREEN inicial de regresión en ambos órdenes. Puertos reales, dos transacciones, sincronización acotada y bloqueo de la segunda operación; terminar primero impide tarea/evento y crear primero conserva tarea pending al completar.
19. RED para identidad, estado y fechas inválidas en construcción directa; GREEN con siete invariantes y sólo pending admitido en este corte.
20. Privacidad de cinco lecturas, sesión/CSRF/origen/media, indisponibilidad de consulta y conservación de ETag: GREEN inicial de regresión con comprobaciones HTTP/PostgreSQL.
21. Revisión detectó que projectId inválido con cursor válido se clasificaba como cursor. RED reproducido; GREEN al validar una sola vez el identificador de ruta antes de decodificar.
22. Unidades de paginación y detalle: GREEN inicial de regresión para 0, 1, 20 y 21 elementos, continuidad, scope del propietario/proyecto e instantánea independiente de la lista mutable del puerto.
23. La nueva FK produjo RED en limpieza histórica de ProjectApiTest. Se amplió exclusivamente la limpieza de fixtures con TRUNCATE CASCADE para incluir tareas dependientes. Refactor de imports y formato; errores transitorios de importación corregidos antes de congelar.

## Corte entregado para revisión previa a mutación

Gradle 32919 terminó con salida 0: 106 pruebas relevantes, cero fallos y errores. Incluye TaskApiTest (65), TaskTest (17), TaskEventTest (10), CreateTaskTest (2), ReadTasksTest (5), RabbitBrokerPublisherTest (6) y creación histórica (1). El coordinador ejecuta la regresión completa por separado. No se declara todavía score de mutación, cierre ni despliegue.

## Mapa del contrato

| Escenario | Evidencia backend o responsable |
| --- | --- |
| s1 | TaskApiTest.s1_commitsTaskAndMinimalEvent; CreateTaskTest.s1_createsChildAndPrivateEventTogether; TaskTest |
| s2–s3 | TaskTest.s2_s5_s6_enforcesCodePointMaximum y s1_s3_normalizesOnlyOuterWhitespaceAndDefaults |
| s4 | TaskTest.s4_requiresTitleEvenInDirectConstruction; TaskApiTest.s4_s6_validatesJsonTypes |
| s5–s7 | TaskTest límites y estimación; TaskApiTest.s19_s23_readsOnlyConfirmedOwnTasks; validación JSON s4_s6 |
| s8 | TaskApiTest.s8_strictBody y s8_s9_s10_s24_protectsTaskResources |
| s9–s10 | TaskApiTest.s8_s9_s10_s24_protectsTaskResources |
| s11 | TaskApiTest.s11_s13_hidesUnavailableProjects y s11_privateReadsUseOneNotFoundProblem |
| s12 | TaskApiTest.s12_rejectsPartialIdentifiers y s12_pathErrorPrecedesCursorParsing |
| s13 | TaskApiTest.s13_doesNotAlterParentRepresentationOrRevision y s11_s13; CreateTaskTest.s13 |
| s14 | TaskApiTest.s14_serializesCreationAndCompletionOnSameProject, ambas confirmaciones iniciales |
| s15 | TaskApiTest.s15_rollsBackSuppressedOrFailedWrites, cuatro variantes reales |
| s16 | Integración confirmada por el coordinador: smoke con publicador habilitado, broker detenido, recuperación y retención tras reinicio, salida 0 |
| s17 | RabbitBrokerPublisherTest.task_s17_publishesTaskRouteWithoutChangingProjectRoutes; TaskEventTest.s17 |
| s18 | TaskEventTest.s18_blocksIncompatibleTaskEvents; conservación del worker histórico en regresión raíz |
| s19 | TaskApiTest.s19_s23; ReadTasksTest.s19_s20 |
| s20–s21 | TaskApiTest.s20_s21_stablePaginationWithTiesAndNewerInsert; ReadTasksTest |
| s22 | TaskApiTest.s22_rejectsInvalidCursorAndQuery, 14 casos |
| s23 | TaskApiTest.s19_s23_readsOnlyConfirmedOwnTasks; ReadTasksTest.s23 |
| s24 | Respuestas 200/401/404/503 comprobadas en consultas y seguridad; errores de cursor 400 en s22 |
| s25 | TaskApiTest.s24_s25_storageFailureNeverBecomesEmpty |
| s26–s35 | Frontend e integración; informes independientes, sin atribuir ejecución UI a pruebas backend |

24. Regresión global del coordinador: 484 pruebas, sólo tres fallos del fixture de configuración de estados al faltar puertos nuevos. Se añaden TaskCommit y TaskQueries al ApplicationContextRunner de prueba; siete casos focales verdes, sin cambios de producción ni de asserts de capacidad. Se añade la aserción no-store al escenario existente de cursor 400 antes de la verificación conjunta.

25. Revisión previa APPROVED y regresión conjunta del coordinador 74002 con salida 0: 484 pruebas backend, 331 frontend y lint verdes. PIT focal autorizado después de esa revisión; ejecución iniciada sin modificar producción.

26. Mutación global completada: 182/186, cero timeouts y cero NO_COVERAGE tras corregir lifecycle del fixture y margen medido de arranque Rabbit. Se reforzaron tres huecos de pruebas sin cambiar producción; replay separado 15/15 confirma su eliminación. Una equivalencia de normalización justificada. Informe detallado: mutation_create_task_backend.md.
27. Regresión normal final de fixtures afectados y formato: 87014 salida 0, 65 casos TaskApiTest y 8 RabbitBrokerPublisherTest, cero fallos/errores. La suite global anterior era 484; ahora hay dos casos adicionales de destino Rabbit verificados en este foco, sin atribuir una ejecución global de 486. Gradle liberado para cierre independiente.
