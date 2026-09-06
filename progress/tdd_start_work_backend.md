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
