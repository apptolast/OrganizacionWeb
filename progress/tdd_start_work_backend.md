# TDD14 — inicio de trabajo backend

Autorización explícita root c54aee6; init91757f sigue como baseline del código anterior sin repetir suite global por cambio de contrato. Ámbito backend propio; sin frontend/publisher compartido ni estados15–18. Ponytail full/Caveman lite y rutas protegidas conservados. No se declara feature terminada.

## Ciclo1 — @s1, núcleo de inicio nominal

Test individual StartWorkSessionTest.s1_recordsOneRealStartAndEventWithFixedEnd: RED de compilación real b95e3a por ausencia de tipos de inicio/puertos. Sólo después se añade el mínimo núcleo y sus records/puertos exigidos por ese test. GREEN97a7a5,1 caso.

El fake del puerto transaccional ejecuta el callback una vez y conserva la pareja inicio/evento; el caso de uso captura Clock una sola vez, trunca123456789ns a123456µs y fija25min exactos. ID de sesión generado, evento de identidad independiente, contexto proyecto/tarea y zona Europe/Madrid preservados. No se afirma PostgreSQL, HTTP201, commit real ni publicación a partir de este test unitario.

Corte: domain/SessionStart (siete campos), application/StartWorkSessionUseCase, StartWorkSession, WorkSessionStarting, WorkSessionContext, WorkSessionChange, WorkSessionConfirmation y WorkSessionStarted. El contexto tiene el estado de proyecto/tarea y zona capturada para la frontera de almacenamiento; las guardas y fallback todavía no están implementados. No hay endpoints/beans/migración ni consultas en este corte. Próximo ciclo nominal de persistencia real antes de ampliar límites.

## Ciclo 2 — @s1, persistencia nominal y migración aditiva

Un único test WorkSessionPersistenceTest.s1_commitsRealStartAndEventWithoutChangingPlanning exige el adaptador real. RED c7167a por clase PostgresWorkSessionStore ausente; mínimo adaptador y V14 después. GREEN 4ca8c9: dos casos (núcleo y PostgreSQL), sin fallos.

El adaptador usa TransactionTemplate, consulta proyecto propio y tarea en ese orden con FOR SHARE y captura la zona existente con FOR SHARE. No toma mutex de disponibilidad. Inserta el inicio y el evento en una transacción; el test lee ambos después del retorno mediante conexiones JDBC nuevas. El agregado del evento es la sesión, los once campos del payload coinciden, y proyecto/tarea/estimación/disponibilidad permanecen intactos; las tablas de planificación vacías siguen vacías. Esto acredita el commit nominal, no rollback ni carreras.

Dependencia encontrada: V1 conserva outbox_events_aggregate_id_fkey hacia proyectos, incompatible con el agregado sesión normado. Root autorizó retirar exclusivamente esa FK mediante V14, preservando aggregate_id NOT NULL, datos y demás constraints de outbox. V1–V13 no cambiaron. V14 crea work_sessions con referencias de proyecto/tarea; las consultas de contexto enlazan la tarea al proyecto propio. La integridad contextual completa y constraints de negocio siguen pendientes de sus ciclos.

Refuerzo del mismo fixture tras GREEN: migrar primero hasta V13, sembrar contexto y evento previo, aplicar V14 y verificar conservación exacta de esa fila de outbox. Inicialmente GREEN a28fa5, sin simular RED nuevo. El payload anterior se refinó a ProjectCreated real serializado, sin placeholder; formato focal de once Java propios con GJF 1.31.0 en 0333ec. Regresión final cf68d5: StartWorkSessionTest + WorkSessionPersistenceTest, dos casos GREEN, cero fallos, tras formato y fixture final.

## Primer corte congelado para revisión

Fuentes: ocho tipos del núcleo anterior, PostgresWorkSessionStore y V14__work_sessions.sql. Pruebas: StartWorkSessionTest y WorkSessionPersistenceTest. Ningún bean, endpoint o publisher compartido modificado; no suite global, PIT, Git ni metadatos. DTO SessionStart conserva siete campos y precisión de microsegundos.

Límites explícitos: aún faltan validación/rango temporal/fallback de catálogo; elegibilidad; replay owner/key y unicidad activa; captura de errores incluyendo COMMIT; rowcounts/rollback y resolución de colisiones en nueva transacción; GET/HTTP y publicación. El adaptador nominal todavía devuelve replayed=false y el núcleo requiere zona presente. No es una entrega utilizable de toda la feature. Se detiene aquí por la frontera de revisión solicitada, sin abrir otra matriz.

## Reanudación acotada — duración y elegibilidad del núcleo

Root autorizó sólo application/domain y StartWorkSessionTest desde HEAD6578c9e, baseline nominal 2/2 cf68d5. No se modifica PG, migración, HTTP, publisher ni configuración. Los casos se añadieron individualmente y cada ejecución terminó antes de escribir el siguiente caso.

- @s6, duración 0: incidente de compilación fd30e8 por usar dos argumentos en FieldError (requiere tres); se corrigió el oráculo para comprobar field/code. No se atribuye a producción. RED funcional 464879 por ausencia de rechazo; guarda de rango cerrado 1–1440 antes del puerto; GREEN 508d4c, dos casos vigentes.
- @s6, duración 1441: añadido después del GREEN anterior; inicialmente GREEN 4ff77f, sin nueva producción. Ausencia/null/tipos JSON pertenecen al futuro adaptador HTTP: la entrada del núcleo es int y este corte no afirma cubrir esas filas.
- @s2, duración 1: inicialmente GREEN 7a34f9, fin exacto de 60 segundos preservando microsegundos.
- @s2, duración 1440: inicialmente GREEN 790e6f, fin exacto al día siguiente, 86400 segundos; sin ampliación DST/catálogo.
- @s11, proyecto completed y tarea pending: el primer fixture dejó Clock sin valor y obtuvo NPE (efe0f7); un reemplazo textual no aplicado repitió ese incidente (e88e8c). Se corrigió antes de editar producción para usar un instante válido. RED funcional final ef38e7/6677c6 por no lanzar ProjectCompletedException; mínimo guard dentro del callback antes del reloj, GREEN 628bc4 con seis casos.
- @s11, proyecto active y tarea completed: RED funcional c23250; guard de tarea después del de proyecto, GREEN e2d44a con siete casos.
- @s11, ambos completed: inicialmente GREEN a99f46. El rechazo sigue siendo ProjectCompletedException y no se consulta Clock. No se fabrica un RED para esta precedencia ya presente.

Se reutilizan ValidationException/FieldError y ProjectCompletedException/TaskCompletedException sin modificar sus clases ni títulos HTTP existentes. La duración se valida antes de entrar al puerto, y la elegibilidad dentro de su callback, de modo que la futura resolución de replay del puerto pueda preceder al negocio. Esa integración futura no se acredita con estas pruebas del núcleo.

Formato focal de los dos Java propios con GJF 1.31.0 c2fa8d, retirando nombres de clase plenamente cualificados y sin cambiar otros archivos. Pendiente de registrar resultado de la regresión final del archivo completo.

Cierre congelado: regresión final e55cd8, StartWorkSessionTest 8/8 GREEN tras formato, cero fallos/errores/omitidos. Sólo StartWorkSession.java, StartWorkSessionTest.java y esta bitácora cambiaron por esta subtarea. No se ejecutaron pruebas PG ni suites globales. Se entrega a revisión sin ampliar a temporal, fallback o persistencia.

Verificación de integración solicitada por root tras freeze del núcleo: sólo WorkSessionPersistenceTest existente, sin nuevos tests ni cambios de producción. GREEN d4b176, un caso PostgreSQL; XML confirma 1/1, cero fallos/errores/omitidos. No se repitieron los ocho casos del núcleo ni suite global. Freeze conservado para revisión/commit.
