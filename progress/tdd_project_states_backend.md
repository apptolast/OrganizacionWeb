# project_states — TDD backend

Contrato aprobado y baseline confirmado por el coordinador. Ponytail full y Caveman lite activos. Cambios acotados a estados y capacidad; sin dependencias nuevas.

1. Modelo: prueba parametrizada de cuatro estados almacenados. RED para active/paused/completed; GREEN al permitir el conjunto cerrado. La prueba histórica de estado inválido usa ahora unknown: active es válido por el contrato nuevo, sin debilitar el rechazo de valores ajenos.
2. Caso de uso: RED por puerto/clases ausentes. GREEN en las siete transiciones permitidas, conservando identidad y texto y generando el evento exacto con ocho campos.
3. Tabla cerrada: RED por excepción ausente; GREEN para las cinco transiciones prohibidas mediante ProjectStates, reutilizable por el publicador.
4. No-op: cuatro estados devolvían transición inválida. GREEN al conservar snapshot y evento ausente, incluso por encima de capacidad.
5. Revisión: ETag obsoleto o de otro proyecto aceptaba no-op. RED y GREEN tras comparar identidad y versión dentro del callback transaccional, antes del no-op.
6. Capacidad: RED por excepción ausente; GREEN al rechazar activación cuando el conteo propio es igual o superior al límite. Se comprueban activeCount y limit observables del error.
7. HTTP/PostgreSQL: tras corregir un error de preparación del fixture, RED por endpoint ausente. GREEN con migración V5, controlador de escritura reutilizado, puerto de estados y adaptador transaccional con bloqueo asesor antes de fila/conteo. Durante GREEN se corrigió una lista de parámetros SQL heredada de edición. PUT confirma estado, ETag y evento exactos.
8. Transición HTTP inválida: RED con 500; GREEN mediante 409 INVALID_PROJECT_TRANSITION sin escrituras.
9. Última plaza: dos solicitudes HTTP concurrentes con PostgreSQL real dieron 200 y 500 por falta de mapeo. GREEN con 200/409 ACTIVE_PROJECT_LIMIT, tres activos propios y exactamente un evento. El bloqueo global transaccional impide superar la capacidad.
10. Configuración: seis de siete casos fallaron por límite fijo y ausencia de validación. GREEN con propiedad app.max-active-projects / APP_MAX_ACTIVE_PROJECTS, valor ausente 3, extremos 1 y 10 válidos, valores 0/11/abc/1.5 rechazados al arrancar.
11. Outbox StatusChanged: RED por tipo no permitido. GREEN al reconocer el esquema exacto de ocho campos y conservar las validaciones comunes.
12. Payload incompatible: RED en cinco casos que llegaban al broker; GREEN al validar tipos y reutilizar ProjectStates.allows. También se comprueban propiedades extra y ausentes. No-op, transición imposible y estados desconocidos quedan INVALID_EVENT.
13. RabbitMQ real: RED por ruta desconocida. GREEN al añadir únicamente el destino cerrado status-changed, verificando JSON original, routing key, message-id, persistencia y content-type en la cola dedicada.

Quedan regresiones relevantes de privacidad, rollback y compatibilidad, seguidas de PIT y revisión independiente. Los casos inicialmente verdes se identificarán como regresión, sin inventar ciclos RED.

14. Regresión inicialmente verde: las siete transiciones se persisten, aparecen en lista/detalle y la edición posterior de texto conserva el estado usando la misma versión.
15. Regresión inicialmente verde: los cuatro no-op conservan filas completas y ETag sin evento.
16. Regresión de frontera inicialmente verde: 19 casos de autenticación, privacidad, UUID, ETag, JSON, Origin y media type. Todos conservan datos y no-store, sin revelar estado o contenido ajeno.
17. Regresión real PostgreSQL inicialmente verde: errores y escrituras suprimidas por triggers en proyecto y outbox devuelven 503 y revierten todos los campos y eventos.
18. Regresión inicialmente verde: pausar libera una plaza propia, una activación posterior la ocupa y tres proyectos activos de otro propietario permanecen intactos y no consumen capacidad propia.
19. Regresión inicialmente verde: adoptar límite dos con tres activos devuelve el error con conteo tres/límite dos y no pausa ni modifica filas. Se ejecuta el caso de uso contra el adaptador PostgreSQL real; la configuración de arranque se verifica por separado.
20. Regresión inicialmente verde: tres órdenes de cambios de texto/estado/no-op rechazan la revisión antigua con 412 sin sobrescribir ni producir otro evento.
21. Regresión inicialmente verde: fallo interno inyectado devuelve 500 seguro con correlationId y no-store, sin SQL, secreto, datos ajenos ni escrituras.
22. Política de estados inicialmente verde: null, vacío, desconocido y mayúsculas no autorizan transiciones como origen o destino.

La premisa READ_COMMITTED del bloqueo asesor se documenta en el adaptador y README. Focalizado final y PIT en ejecución; no se atribuye resultado hasta finalizar. El coordinador confirmó SUCCESS de CI de edición 33997062229; esa ejecución pertenece a la feature anterior.

## Verificación final del autor

Gradle 28681 terminó con salida 0: 249 casos focalizados en 14 clases verdes. PIT 163/163, sin supervivientes ni falta de cobertura de mutantes; detalle del denominador y la única línea no recorrida en mutation_project_states_backend.md. Fuente congelada y Gradle liberado al coordinador. Después solo se documentó y formateó el comentario READ_COMMITTED.

| Escenarios | Evidencia |
| --- | --- |
| s1–s3 | ChangeProjectStatusTest cubre siete transiciones permitidas, cinco prohibidas y cuatro no-op. ProjectStatesApiTest confirma persistencia y errores HTTP. |
| s4 | ProjectStatesApiTest.s4_onlyOneConcurrentActivationCanTakeLastSlot: dos peticiones reales concurrentes, última plaza y un único evento. |
| s5–s6 | ProjectStatesApiTest.s5_s6_pauseReleasesOnlyOwnersCapacityForAnotherActivation. |
| s7 | ProjectStateConfigurationTest.s7_capacityIsValidatedAtStartup: siete configuraciones y límite observable del caso de uso creado por Spring. |
| s8 | ProjectStatesApiTest.s8_lowerLimitDoesNotPauseExistingProjects y ChangeProjectStatusTest.s4_s8_capacityCountsCurrentOwnerAndRejectsAtOrAboveLimit. |
| s9 | ProjectStatesApiTest.s9_textAndStateShareOneRevision y prueba de revisión antes de no-op en aplicación. |
| s10 | ProjectStatesApiTest.s10_boundaryFailuresCannotWriteOrExposePrivateState: 19 casos de frontera. |
| s11 | ProjectStatesApiTest.s1_s11_updatesStateAndPersistsOneExactEvent confirma transacción/payload. Smoke independiente con broker realmente detenido y recuperación confirmado por integración, salida 0. |
| s12 | ProjectStatesApiTest.s12_failedOrSuppressedWriteRollsBackAllState: cuatro fallos reales de PostgreSQL. |
| s13 | PublishOutboxTest.states_s13_publishesExactStatusEnvelope y states_s13_blocksIncompatibleStatusPayload; RabbitBrokerPublisherTest.states_s13_routesStatusChangedToDedicatedQueue. Regresión completa de ambas rutas anteriores incluida en el focalizado. |
| s14 | ProjectStatesApiTest.s1_s14_transitionsRemainReadableAndTextEditingPreservesState, ProjectTest.states_s14_acceptsAllDefinedStoredStates. Interfaz a cargo de frontend. |
| s15–s16 | Frontend, integración y revisión UX independientes; no se atribuyen a las pruebas backend. |
| s17 | ProjectStatesApiTest.s17_internalFailureIsSafeAndPrivate y no-store en la matriz de fronteras. |

Sin nuevas dependencias, consumidores, tareas, preferencias individuales ni despliegue en el servidor del usuario. El cierre requiere revisión y evidencia conjunta del coordinador.
Regresión completa del coordinador 51375: salida 0, lint correcto, 328 pruebas backend y 171 frontend verdes. XML backend sin fallos, errores ni omitidos. PIT 163 KILLED comprobado independientemente. Integración, mutación frontend y juez conjunto pendientes.
Cierre local autorizado por el coordinador: juez conjunto APPROVED, 22 E2E, Firefox/WebKit y smoke correctos. Frontend final 176 pruebas y lint verdes; Stryker 284/312, replay separado 14/14 y equivalencias documentadas. Feature 5 done; commit/push y CI propios pendientes. Authentication spec_ready, sin producción.
