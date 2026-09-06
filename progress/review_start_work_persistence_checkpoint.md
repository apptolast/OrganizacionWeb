# Revisión del checkpoint PostgreSQL 14

**APPROVED parcial: commit nominal y upgrade desde V13.** Sin bloqueantes dentro del alcance declarado. No constituye aprobación de la persistencia completa, despliegue o feature14.

Leídos `PostgresWorkSessionStore.java`, `V14__work_sessions.sql`, `WorkSessionPersistenceTest.java` y la bitácora actualizada, evidencia `8b26f4`. El autor registra GREEN final `cf68d5` y XML `f6883f`, dos casos entre núcleo y persistencia. No se ejecutaron suites ni se editaron fuentes, pruebas, SQL o Git durante esta revisión. Ponytail full y Caveman lite.

## Comprobaciones

- El adaptador consulta proyecto por owner/id y después tarea por project/id, ambos con FOR SHARE. Captura la zona de la fila existente también bajo SHARE. El callback produce inicio/evento, que se insertan dentro de la misma ejecución transaccional; el resultado sólo sale después de finalizar `execute`.
- La prueba usa PostgreSQL real y Flyway, primero hasta V13. Siembra un ProjectCreated válido serializado y conserva la fila completa previa; después aplica V14 y confirma un inicio. Comprueba que la fila histórica del outbox permanece exactamente igual y que el segundo evento pertenece al agregado sesión. No sustituye el evento heredado por un JSON vacío.
- El payload nuevo coincide con los once campos del evento esperado. Los instantes persistidos de sesión coinciden con el resultado del núcleo; su precisión exacta ya se verifica en el test nominal del núcleo. Contexto, estimación y disponibilidad conservan sus filas completas.
- V14 retira exclusivamente la FK de outbox a projects; no modifica datos ni V1–V13 y conserva el NOT NULL de aggregate_id. La creación de work_sessions y ese cambio son compatibles con el upgrade observado. No se afirma aquí que estén probados rollback de migración o todas sus restricciones.

## Límites que deben conservarse en la entrega

La prueba conserva planificación **vacía**; no acredita todavía preservación de reservas preexistentes. No comprueba fallos de escritura, supresión de INSERT, COMMIT fallido, colisiones ni restricciones de negocio. El test de upgrade y el nominal son un solo caso PG, no dos pruebas independientes.

El código devuelve siempre replayed=false, no comprueba rowcounts ni traduce errores de finalización; V14 carece todavía de unicidad owner/key, activa única y FK compuesta de contexto. La bitácora declara esos puntos pendientes, junto con elegibilidad, fallback y validaciones. No se presentan como fallos inesperados de una feature terminada ni se solicita implementarlos fuera de sus ciclos TDD.

La plantilla del comando mantiene aislamiento DEFAULT: PostgreSQL usa READ_COMMITTED por defecto, suficiente para este fixture. El cierre del contrato deberá fijar o demostrar READ_COMMITTED en el entorno soportado. No hay un riesgo adicional no delimitado que bloquee este checkpoint nominal.

El corte queda identificado por SHA256 (`8077e9`):

- Store: `0D6B77B2AAA5C88DDD2D299761B9CEC837FFFAC201A3E1D839690FF481018D65`.
- V14: `29336B2F5BEDF7A30FCAABEA44DAFF75E77E8B9A30F1B799FEFCC83069B72B0A`.
- Test: `3252AB62453664910F609CDC7A223A049788DCF4F1F2557E514E8EA968938EB4`.

Cualquier evolución posterior requiere otro dictamen. La aprobación no alcanza HTTP, GET, publicación ni los cambios que el autor emprenda después del freeze.
