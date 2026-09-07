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

## Continuación completa autorizada — núcleo temporal y zona

Árbol HEAD020f7ff, sin init redundante. Publisher trabaja en otro árbol con archivos exclusivos; HTTP se delegará posteriormente por root. Se conserva TDD individual y ejecuciones focales.

- @s4 sin disponibilidad: RED c19fc1 (NoSuchElementException), mínimo UTC, GREEN 7c7c3c.
- @s4 zona fuera de catálogo: RED compilación 956ab5 exige inyectar ZoneCatalog; constructor y fixtures existentes adaptados, GREEN 633a72. El catálogo se consulta dentro del callback, no antes del futuro replay.
- @s4 zona presente en catálogo pero no resoluble: RED 4199b9 por devolver Legacy/Removed; mínimo resolución ZoneId con fallback UTC, GREEN 70b04e.
- @s13 inicio antes de0001: RED 3f9154 por excepción específica ausente; guard de rango de inicio y excepción WorkSessionTimeOutOfRangeException, GREEN a19543.
- @s13 inicio después de9999: test individual inicialmente GREEN d476b2. No se inventa RED para el límite ya implementado.
- @s13 fin fuera de9999: RED c2849d, mínimo guarda del fin calculado una vez, GREEN c6130d. Se continúa persistencia; las filas temporales heredadas aún se mapearán con refuerzos individuales antes del cierre.

### Persistencia owner/key y carrera inicial

@s14 RED funcional febf70 por ProjectCompletedException antes de replay; incidente previo 597187 por fixture completed_at posterior a updated_at, corregido sin relajar constraint. GREEN afe507: hecho previo recuperado, Clock/ZoneCatalog no invocados, una sesión/evento y ninguna preferencia.

@s15 duración distinta: RED abd8dc exige excepción de conflicto propia, GREEN 8dd5e6. Tarea distinta inicialmente GREEN 271d9c; proyecto/tarea distintos del mismo owner inicialmente GREEN fe17f4. Cada caso se añadió tras cerrar el anterior.

@s12 activa antes de completed: RED 7c6e52, GREEN 8b4cdb; excepción con sessionId propio. La consulta no usa disponibilidad como mutex.

@s17 misma key/intención: RED edfbb2 devuelve dos creaciones. Se añade UNIQUE(owner_id,request_key) en V14 del corte14 aún en desarrollo y recuperación posterior a DuplicateKeyException fuera de la transacción fallida. Incidente de edición 14ba1f (catch no insertado) corregido sin cambiar el oráculo; GREEN 6e9f7b. Barrera de fixture JDBC exige dos llamadas INSERT alcanzadas antes de dejarlas ejecutar en PostgreSQL, con timeout y executor cerrado; no se introduce barrera productiva. El ganador/replay son iguales y sólo quedan una sesión y un outbox.
Refactor de fixture de carrera GREEN 42d47b, sin cambio productivo. @s17 misma key/duración distinta inicialmente GREEN bfa49c. @s17 keys distintas entre proyectos RED 0f99dc, UNIQUE parcial owner running y resolución de activa después de key, GREEN ed457c. Las tres carreras verifican dos intentos de INSERT y un único ganador durable.

### Puertos de lectura exigidos por pruebas

ReadWorkSessionsTest: activa RED cd9b1e → GREEN 94b91c; detalle RED 524101 → GREEN 1e9f7d; ausencia de detalle RED c447b3 → GREEN 51ef19; key RED 9e0ae9 → GREEN bffd1f. Cada método nace de un caso y no se añadieron defaults ficticios. ReadWorkSessionsUseCase expone active(owner) Optional, detail(owner,id) y byRequest(owner,key) SessionStart; WorkSessionQueries retorna Optional en los tres, y la aplicación traduce ausencia identificada a WorkSessionNotFoundException. Adapter real pendiente en este punto, HTTP delegado a otro autor en árbol aislado.

### Lecturas PostgreSQL y pausa por regresión CI

Activa nominal RED 412e99 → GREEN fd87da. Detalle histórico sin outbox RED 03b436 → GREEN 7db2fd. Key histórica RED 64d8c5 → GREEN ef960a junto tres carreras tras mover los UNIQUE a migración aditiva V15 (PR7 publicó V14; se conserva su checksum). Store implementa ambos puertos sólo una vez que todos sus métodos existen, sin defaults ficticios.

Read-only observado mediante SHOW transaction_read_only/isolation durante SELECT y ausencia de cláusulas FOR SHARE/UPDATE: RED 07559e → GREEN f34a96. Plantilla de lectura propia READ_COMMITTED, sin mutar configuración por petición. Pausa requerida por root para corregir veinte fixtures CI, informe independiente progress/fix_work_session_ci_fixtures.md; regresión20/20 GREEN7c5461. Se retoma sin modificar más esos fixtures.

@s24 cierre de activa RED 2c6485 → GREEN 10e43d: catch alrededor de execute, no sólo del callback. Cierre de detalle inicialmente GREEN46939c (incluye refactor de fixture de cierre) y key inicialmente GREEN0cd75d. Fallos SQL reales con tabla renombrada/restaurada en finally: activa239313, detalle5a4888 y key478169 inicialmente GREEN, individualmente. No son mocks de ausencia.

Refuerzo de aislamiento pedido por review: configurar default_transaction_isolation de la base aislada a repeatable read y restaurarlo en finally. Lectura real falló71d8b8, plantilla explícita READ_COMMITTED GREENf5124d. El test observa SHOW dentro de la transacción, no un getter de configuración. Comando equivalente RED616524; verificación mínima en curso.

Comando READ_COMMITTED explícito GREEN c77275 tras RED616524. @s20 consulta disponibilidad ausente por fallo SQL RED0d51ac → GREEN344740, catch exterior cubre también la transacción de resolución de colisión. @s20 INSERT sesión suprimido RED1f5ff3 → GREENeced78: exige una fila, no inventa conflicto; incidente de paréntesis f91096 durante edición corregido sin alterar test. @s20 INSERT outbox suprimido RED89c25b → GREENdb6be7, mismo guard y rollback total. Escritura outbox rechazada por trigger PostgreSQL inicialmente GREEN601b32. Los triggers se retiran en finally.

Formato de fixtures CI: root integró 2e9976e con Spotless real c3942b; ninguna alteración de aserciones. Los intentos de spotlessIdeHook sólo diagnosticaron IS DIRTY y Apply SKIPPED; no se documentan como pase. No volver a editar esas tres clases para formato manual.
s22_foreignIdentityIsNotFound: inicialmente GREEN f037e9; caso individual ejecutado antes de escribir el siguiente, sin fuente productiva cambiada.
s22_absentIdentityIsNotFound: inicialmente GREEN b8a2d8; caso individual ejecutado antes de escribir el siguiente, sin fuente productiva cambiada.
s22_foreignKeyIsNotFound: inicialmente GREEN 4308bd; caso individual ejecutado antes de escribir el siguiente, sin fuente productiva cambiada.
s22_absentKeyIsNotFound: inicialmente GREEN c095e5; caso individual ejecutado antes de escribir el siguiente, sin fuente productiva cambiada.

@s20 fallo de commit PostgreSQL diferido inicialmente GREEN10f688. @s10 contexto ajeno antes de replay RED0b594a → GREENa0a2f7, las consultas de proyecto/tarea ahora distinguen ausencia comprobada de errores SQL. @s19 completar primero: proyecto9bea6f y tarea79955f inicialmente GREEN, con wait_event_type=Lock observado y futuro pendiente antes de confirmar. @s19 inicio primero: proyectob8e9e4 y tareac63788 inicialmente GREEN; writer bloqueado hasta commit y sesión recuperable/activa después de completed. @s18 propietarios independientes con misma key inicialmente GREEN67af18 junto tres carreras tras refactor del helper de owner. No mutex global ni disponibilidad creada.
@s3 Midnight: inicialmente GREEN 2cf20a; caso individual antes del siguiente, fuente productiva intacta.
@s3 SpringDst: inicialmente GREEN bf96ff; caso individual antes del siguiente, fuente productiva intacta.
@s3 AutumnDst: inicialmente GREEN 44300a; caso individual antes del siguiente, fuente productiva intacta.
@s5 idea: inicialmente GREEN 753a3b; proyecto conserva estado, sin bloques ni preferencia creada.
@s5 paused: inicialmente GREEN 2b0b37; proyecto conserva estado, sin bloques ni preferencia creada.

Integridad de proyecto/tarea: @s1 SQL directo con tarea de otro proyecto RED dda89f → GREEN 44cf73. V15 reutiliza tasks_project_identity de V8 para FK compuesta; V14 publicada permanece intacta.

@s20 outbox SQLSTATE23505 y ganador después del rollback: primer ensayo eaf538 fue un falso positivo de fixture (inyectaba también en el segundo rollback). Se corrigió a una única inyección, sin producción modificada; RED real 9bd3ad mostró WorkSessionAlreadyActiveException en lugar de StorageUnavailableException. Captura restringida exclusivamente al INSERT de sesión mediante excepción interna; GREEN c820ac incluye las tres carreras s17. Incidente de edición 683196 por apertura try ausente corregido antes del GREEN. El trigger es PostgreSQL real; ganador inicia en otra transacción una vez terminado el rollback, y la key fallida sigue ausente.

@s11 precedencia sobre captura de zona: proyecto completado y tabla disponibilidad temporalmente ausente RED bc6569 → GREEN a18abe junto casos core s11. WorkSessionContext.requireEligible centraliza la regla, usada antes de leer zona en persistencia y en el núcleo. Tarea equivalente inicialmente GREEN 9fafe4. Replay y activa conservan precedencia anterior.

Cierre del paquete core/persistencia: @s16 inicialmente GREEN41987b, misma key en creación11 y cancelación13 con recibos/proyección coherentes; el inicio14 es nuevo y preserva íntegros esos registros. No afirma que se hayan ejecutado POST11/13 en este fixture SQL.

Formato real del repositorio spotlessApply GREEN8d5277. Intento de CLI73668f rechazó --tests colocado después de spotlessCheck, sin ejecutar suite; orden corregido. Regresión focal final + spotlessCheck GREEN1e3110, XML5b65c8: StartWorkSessionTest17, ReadWorkSessionsTest4, WorkSessionPersistenceTest1 y WorkSessionStoreTest41, total63, cero fallos/errores/omitidos. No init global ni PIT.

Freeze para revisión e integración HTTP. Trazabilidad: @s1 nominal/upgrade/FK y evento; @s2/s6 duración en núcleo (forma JSON corresponde a HTTP); @s3 tres instantes medianoche/DST; @s4 fallback y fallo real de lectura diferenciado; @s5 idea/active/paused sin planificación; @s10 contexto previo a replay; @s11 elegibilidad y precedencia ante fallo de zona; @s12 activa; @s13 límites temporales; @s14–16 replay/intención/espacios de key; @s17–18 carreras reales owner/key/activa y propietarios independientes; @s19 cuatro órdenes observados por bloqueo PostgreSQL; @s20 rowcounts/SQL/outbox/commit/23505; @s21–24 consultas propias, privacidad, aislamiento y seis fallos SQL/cierre. Seguridad/negociación/query/DTO/Location y wiring se entregan por el autor HTTP. @s25 reinicio/ACK perdido y publicación real requieren smoke separado; no se atribuyen a borrar outbox en una prueba PG.

SHA256 corte: PostgresWorkSessionStore 2CAB64124114844CA1BFC934EE9E758E401A2C02436F49011A67C3AEBA8082DF; StartWorkSession A18F8E7E087FD10EFBECA9FC2974C36A7E0B17190CCF492BE1ACB1E14A98F2CE; WorkSessionContext FB9BAC4EBFA645A1F22CFDEBB7A5FB353DD514BBDF6BC0CD27E02F93D584E975; V15 27C1221CA01D4085B5763A91F3E231E1A5F320332CFA3C5255F36255694EEEB5. V14 publicada no se reescribe. Rutas productivas: application/StartWorkSession y contratos WorkSession*, ReadWorkSessions*, adapter/persistence/PostgresWorkSessionStore y migración V15. No se añaden estados15–18.
