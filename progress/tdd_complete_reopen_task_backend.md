# TDD backend: completar y reabrir tareas

Contrato aprobado d65bba5 leído completo: 36 escenarios, 137 casos locales y todas las variantes referidas. Única feature activa: complete_reopen_task. Ponytail full y Caveman lite activos; arquitectura hexagonal y TDD estricto conservados.

Baseline compartido: init 9396, 622 backend/462 frontend y lint verdes; frontend final 475/475 verificado por el coordinador. No se repite el init antes de implementar. Split_task publicada en 3675c36, CI 34007601179 en curso. Mutación pendiente de revisión previa.

Decisiones aplicables: DTO8 conserva campos y amplía status; recurso /status con snapshot y ETag task propio; historia independiente; FOR NO KEY UPDATE OF t en transacción y carreras reales en ambos órdenes. Ninguna llamada al broker dentro del commit.

## Ciclo 1: compatibilidad del estado en DTO8
RED TaskStateTest por constructor que rechazaba completed; GREEN tras admitir el segundo estado conservando todas las demás invariantes. La entrada histórica de estado rechazado se actualiza de completed a active, porque el contrato 9 amplía explícitamente completed. Ambas suites de dominio focales verdes; no se eliminan aserciones de identidad, contenido ni fechas.

## Ciclos 2 a 6: intención, versión e instante
Cada comportamiento comienza con una prueba focal. Ciclo 2: RED de tipos/puertos ausentes; GREEN de completar con una versión y evento privado. Ciclo 3: RED de completedAt conservado al reabrir; GREEN lo limpia. Ciclo 4: RED en ambos no-op; GREEN conserva el mismo snapshot y no genera evento. Ciclo 5: RED de conflicto inexistente; GREEN compara identidad/versión antes del no-op. Ciclo 6: los dos casos de reloj que retrocede fallan; los dos de mismo microsegundo ya estaban verdes. Se aplica el máximo entre reloj truncado y updatedAt previo, manteniendo versión como orden.

## Ciclos 7 a 11: snapshot y HTTP de estado
Ciclo 7: cinco reconstrucciones contradictorias fallan antes de proteger TaskSnapshot; después pasan. Ciclo 8: GET de tarea heredada devuelve 404 antes del adaptador y V9; pasa con DTO3 y ETag obtenidos del mismo SELECT. Ciclo 9: PUT devuelve 405 antes de implementar el commit; pasa comprobando tarea, historia y evento persistidos con el mismo instante, identidad de historia/evento y proyecto intacto. Ciclo 10: cuatro casos de ruta inválida fallan antes de validar los identificadores; pasan con precedencia sobre precondición y JSON. Ciclo 11: doce variantes de If-Match fallan con el parser permisivo; pasan con formato canónico, identidad exacta, límite BIGINT y ausencia diferenciada mediante 428. Resultado focal de este último ciclo: BUILD SUCCESSFUL, 12 casos, sin escrituras.

## Ciclo 12: JSON de estado estricto
Las 20 variantes de s7 fallan inicialmente. El parser exige documento completo, detecta claves repetidas, valida forma y tipos y escoge la primera clave desconocida en orden léxico antes de validar status. GREEN de los 20 casos con versión, historia y outbox intactos.

## Ciclos 13 a 19: concurrencia lógica y fronteras heredadas
Ciclo 13: el conflicto HTTP daba 500; se añade el mapeo TASK_CONFLICT y queda verde el 412 sin escrituras incluso con intención satisfecha. Ciclo 14: reapertura y ambos no-op sobre PostgreSQL pasan con implementación existente; el primer intento tropezó con el bean de historial en incorporación paralela, resuelto al registrar el servicio acordado. No se atribuye ese fallo a comportamiento del producto.
Ciclo 15: seis casos reales de excepción o supresión mediante triggers sobre tareas, historia y outbox pasan con las guardas atómicas implementadas en el ciclo 9. Ciclo 16: ocho combinaciones de estado de proyecto e intención pasan; se comprueba DTO8 en lista, detalle, padre e hijos, y ausencia de cambios sobre proyecto o descendiente. Ciclo 17: seis variantes de privacidad pasan antes de evaluar versión; cuatro variantes de sesión ausente o expirada mediante JDBC también pasan. Ciclo 18: CSRF ausente/inválido pasan; la aserción inicial de origen usó un código ajeno al contrato histórico, se corrige a UNTRUSTED_ORIGIN sin cambiar producción y pasan los tres casos. Ciclo 19: indisponibilidad real de tabla en GET/PUT devuelve 503 seguro sin escrituras; ambos pasan con traducción de almacenamiento existente. Estas comprobaciones verdes de comportamiento reutilizado no se presentan como nuevos RED funcionales.

## Ciclos 20 a 25: carreras, reloj y publicación
Ciclo 20: dos peticiones HTTP reales de MockMvc con la misma revisión esperan un bloqueo PostgreSQL y terminan en 200/412; versión, historia y outbox quedan en uno. Ciclo 21: las carreras crear hijo/completar padre pasan en ambos órdenes con callbacks de prueba que mantienen los bloqueos reales antes de liberar al primero; cada operación confirma su evento y no modifica el proyecto. Ciclo 22: crear un hijo pendiente bajo tarea completada en proyecto abierto pasa y conserva la tarea padre. Ciclo 23: cuatro variantes de reloj igual o anterior pasan sobre PostgreSQL con igualdad exacta entre snapshot, historia y payload.
Ciclo 24: ambos eventos TaskStatusChanged inicialmente resultan UNSUPPORTED_EVENT; se amplía la allowlist y pasan. Ocho transiciones desconocidas, incompatibles por tipo o sin cambio fallan después; se añade validación cerrada y pasan. Otras 17 variantes, incluidas las nueve claves ausentes, quedan bloqueadas sin envío gracias a validación compartida existente, con UNSUPPORTED_EVENT reservado a tipo/versión. Ciclo 25: RabbitMQ real rechaza inicialmente la sexta ruta; tras admitirla, la prueba detecta una clave de enrutamiento incorrecta. Se corrige y queda verde, verificando JSON original, messageId, persistencia y ruta task.status-changed.v1.

## Verificación de corte antes de formato
TaskStatusApiTest completo termina verde, 79 casos, sin fallos. Incluye las cabeceras no-store de cada respuesta GET/PUT ejercitada y 415. El historial tiene bitácora separada en tdd_complete_reopen_task_history.md; su autor ejecuta un build y caché aislados para evitar colisiones de informes. No se ha ejecutado PIT: el perfil complete_reopen_task se prepara para revisión, conserva umbral 80 y añade las clases nuevas de adaptadores al perfil CI predeterminado.

## Trazabilidad del contrato 9
La numeración corresponde a los tags de complete_reopen_task.feature. Las filas referidas se ejecutan como casos parametrizados, no como un único ejemplo representativo.

| Escenarios | Evidencia backend |
| --- | --- |
| s1 | TaskStatusApiTest.s1_readsLegacyPendingSnapshotAndTaskEtag |
| s2 | ChangeTaskStatusTest.s2 y TaskStatusApiTest.s2; snapshot SQL, historia y evento exactos |
| s3–s4 | ChangeTaskStatusTest.s3/s4 y TaskStatusApiTest.s3_s4, ambos estados |
| s5 | ChangeTaskStatusTest.s5 y TaskStatusApiTest.s5; conflicto antes de no-op |
| s6 | TaskStatusApiTest.s6, 12 precondiciones |
| s7 | TaskStatusApiTest.s7, 20 documentos y 415 separado |
| s8–s9 | TaskStatusApiTest.s8_s9, ocho combinaciones y las cuatro vistas DTO8 |
| s10 | TaskHistoryApiTest.s10 y ReadTaskHistoryTest.s10_s29 |
| s11 | TaskStatusApiTest.s11; tres cambios reales e historia conservada tras eliminar outbox |
| s12–s13 | TaskHistoryApiTest.s12_s29/s13; 20 filas, inserción concurrente y todas las variantes de cursor referidas |
| s14 | TaskStatusApiTest.s14 y TaskHistoryApiTest.s14; tres operaciones, privacidad y sesiones ausentes/expiradas mediante JDBC |
| s15 | TaskStatusApiTest.s15, tres fronteras CSRF/origen |
| s16 | TaskStatusApiTest.s16; dos PUT concurrentes y una única confirmación |
| s17 | TaskStatusApiTest.s17; tres escrituras por dos modos de fallo PostgreSQL |
| s18 | TaskStatusApiTest.s18 y TaskHistoryApiTest.s18; 503, nunca ausencia confirmada |
| s19–s20 | RabbitBrokerPublisherTest.taskStatus_s20 verifica broker real, JSON y sexta ruta. Caída/recuperación con aplicación completa corresponde al smoke de integración, pendiente de su informe |
| s21–s26 | Interfaz, incertidumbre, solicitudes obsoletas y UX: responsabilidad de frontend/integración; no se atribuyen a estas pruebas backend |
| s27 | TaskStatusApiTest.s27; hijo pendiente bajo padre completado en proyecto abierto |
| s28 | TaskStatusApiTest.s28; ambos órdenes de bloqueo PostgreSQL, con ambos callbacks activos antes de liberar al primero |
| s29 | ReadTaskHistoryTest.s10_s29 y TaskHistoryApiTest.s12_s29 |
| s30 | TaskStatusApiTest.s30 y TaskHistoryApiTest.s30; seis rutas/campos antes de entradas secundarias |
| s31 | Cabecera no-store comprobada en cada estado GET/PUT ejercitado y en TaskHistoryApiTest; 19 combinaciones de contrato |
| s32 | TaskStatusEventTest.s32 y PublishOutboxTest.taskStatus_s32; nueve campos ausentes individualmente, estados/tipos e identidad inválidos, sin envío |
| s33 | ChangeTaskStatusTest.s33 y TaskStatusApiTest.s33, cuatro variantes de reloj sobre PostgreSQL |
| s34–s36 | Respuestas GET incoherentes/obsoletas y retirada UI ante 401/404: frontend/integración |

## Verificación de alcance
Sesión 64210: spotlessApply y pruebas del alcance, regresiones de creación/subtareas/publicación, configuración y arquitectura: EXIT 0. XML sumado independientemente: 427 pruebas, cero fallos, errores u omisiones. No sustituye el init global del coordinador. Se añadió después la comprobación explícita s11 de retención, sin cambiar producción; su resultado se registra aparte. Perfil PIT preparado en build.gradle.kts y pendiente de autorización de ejecución. Tipos DTO y puertos sin ramas no se presentan como lógica cubierta; el perfil incluye Task/TaskSnapshot, aplicación, adaptadores HTTP/JDBC nuevos, queries compartidas, OutboxMessage y RabbitBrokerPublisher.

Retención s11 adicional: sesión 17387 EXIT 0, 1/1, con formato. Producción congelada y Gradle liberado. La verificación 427 y este caso adicional se registran separados porque no hubo una nueva suite conjunta posterior.

## Init independiente y preparación de mutación
El coordinador ejecutó init 58990: EXIT 0, XML 798 pruebas backend sin fallos, errores u omisiones; 625 frontend y lint verdes. Después autorizó un cambio exclusivamente de ciclo de vida en ProjectApiTest, ReadProjectsApiTest, EditProjectsApiTest y ProjectStatesApiTest: singleton PostgreSQL por JVM, misma URL durante iteraciones PIT, limpieza final de Ryuk y TRUNCATE explícito de las cuatro tablas. Se conservan todos los asserts. Estas suites ahora forman parte del perfil para cubrir ApiErrors completo, incluido el nuevo 412; sus métodos históricos no se excluyen para mejorar puntuación. La nueva ejecución focal se registra separada del init.

Sesión focal 94651: spotlessApply y las cuatro suites históricas, EXIT 0. XML: 163 pruebas, cero fallos, errores u omisiones. Producción intacta. Se solicita actualización del judge antes de ejecutar PIT.

PIT autorizado final: sesión 11298 EXIT 0, XML 270 KILLED de 270, sin otros estados; original conservado y no hay replays. Informe en mutation_complete_reopen_task_backend.md. Producción sigue congelada y Gradle queda libre.
