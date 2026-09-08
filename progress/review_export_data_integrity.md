# Revisión acotada de integridad de exportación

Lectura de primaria `OrganizacionWeb-export-data`, exclusivamente `PostgresExportDataQueries.java`, su diff actual, sección 22, @s8–s11 y restricciones publicadas necesarias para comprobar relaciones. No se leyó COMMON ni se ejecutaron pruebas. Fuente observada SHA256 `46C9798027700EAA645FBDD8449FC887E48C152B3A94E054EDB4845D99758F9C`; WIP de A, no dictamen del freeze futuro.

## Hallazgo contractual concreto

`sessions()` y `intervals()` serializan `revision` mediante `getString` sin validar su signo. V16 declara ambos BIGINT sin CHECK no negativo; las migraciones posteriores leídas tampoco añaden ese CHECK. Por tanto, una fila durable con `revision=-1` puede producir un 200 con `"-1"`, contrario a la representación no negativa de BIGINT de sección 22. No hace falta quitar restricciones ni cambiar relaciones para construir el caso. Comunicado a A y root para un refuerzo acotado; esta revisión no lo ejecutó ni modificó producción. La corrección no debe exigir una revisión positiva nueva: el contrato exportable define no negatividad.

## Comprobaciones favorables y alcance

- El diff añade el vínculo owner de valores TASK con su tarea/proyecto y la coherencia owner entre recibo y sesión. El resto del contexto de tareas, subtareas, historial y bloques está respaldado por FKs compuestas publicadas y joins al proyecto propio; no se encontró una referencia ajena adicional demostrable en esa lectura.
- @s8: reserva original y proyección se leen por separado; no se fabrica la proyección ausente. @s9–s10: las filas delegan al mapper revisado con metadata exterior y SessionStart inmutable real; no se serializa JSONB genérico. Los offsets de intención y resolución se conservan separados. No se consulta TZDB.
- @s11: se conservan nulls históricos de changedAt/runningSince/effectiveEndAt/lastDecisionAt. No se calcula trabajo hasta el reloj de exportación, no se inventan recibos ni se modifican sesiones. La revisión negativa anterior es independiente de estos nulls legítimos.
- El nuevo formateador de instantes rechaza años ampliados y conserva seis microdecimales de TIMESTAMPTZ. Las horas locales siguen protegidas por CHECKs publicados de años y minutos. No se impone monotonicidad del reloj contra createdAt.
- Proyectos y tareas pueden usar lote 64: sus límites de texto están impuestos en SQL. Intervalos tienen ancho escalar fijo y lote 64. Los recibos/JSONB conservan lote 1; el rechazo temprano de JSONB bruto excesivo es 503 y no se confunde con 413 de bytes exportables demostrados.

## WIP conocido, sin duplicar ciclos

A está cerrando el presupuesto anterior a materialización de columnas TEXT sin límites SQL, como objective/zonas/offsets persistidos. El corte leído aún permite `getString` de esas columnas antes de la guarda del buffer. Se comunicó como pendiente de su trabajo en curso, no como una nueva matriz ni como exigencia de normalizar zona histórica. Falta revisar su cierre para aprobar memoria acotada global.

Dictamen acotado: sin otro defecto demostrado en @s8–s11 y sus relaciones durante esta lectura. Permanecen la revisión negativa concreta y el presupuesto escalar en desarrollo. No sustituye pruebas, mutación de persistencia, revisión del freeze final ni aceptación de exportación completa.

## Segunda lectura tras ciclos 39–43

Corte leído antes del refactor de formato: `0DB0D392960D2FB99388ED056911F67F487D195ADD643231F6F7792DE9B3FE28`. A continúa cambiando esa fuente; esta revisión no lo congela.

El hallazgo de revisiones negativas queda resuelto: guardas `< 0` antes de escribir cada sesión/intervalo y dos pruebas PostgreSQL insertan -1 sin retirar CHECKs, exigen StorageUnavailableException y comprueban que la fila conserva -1. No se impone positividad ni se toca la metadata null legada. La validación de AppearanceValues comprueba la preferencia sin normalizar su salida; el test de contraste inválido exige rechazo y conserva la fila.

`scalarBatch` consulta el máximo por owner en el mismo snapshot antes de materializar, limita a 64 y usa `1048576 / (2 * bytes + 4096)`, con mínimo 1. El máximo está acotado a 32 MiB antes de esa aritmética. Para filas grandes, el mínimo 1 conserva un límite de una fila, no promete que esa fila quepa en 1 MiB. Reservas/proyecciones suman todos sus TEXT variables; proyectos/tareas siguen sus CHECKs y los intervalos son de ancho fijo. Esa composición es coherente, salvo las omisiones siguientes.

Pasada única de columnas TEXT de las consultas frente a migraciones publicadas:

| Grupo | Resultado |
| --- | --- |
| Proyectos y tareas | Nombre/descripcion/título/criterio acotados por CHECK, estados enumerados. |
| Historial de tareas, proyecciones y cambios de bloque | Estados/kind enumerados por CHECK. |
| Apariencia y configuración | Theme/accent/scope tienen CHECK; contenido JSONB limitado antes de lectura y validado por adaptadores. |
| Reservas, proyecciones y disponibilidad | Objective/zonas/offsets ya entran en las sumas/guardas de tamaño; no se solicita reinterpretación TZDB. |
| Sesiones | `zone_id` presupuestado; `status` TEXT sin CHECK todavía omitido del presupuesto y del enum en el corte leído. |
| Recibos de sesión | `receipt` presupuestado; `action` TEXT exterior sin CHECK aún omitido del presupuesto. El mapper rechaza acción desconocida, pero después de materializar ese String. |
| Propietario | Se usa como criterio, no se repite en las filas exportadas. No se amplía esta revisión a la configuración de credenciales. |

Dos correcciones concretas comunicadas a A/root: incluir bytes de `work_sessions.status` y `work_session_changes.action` en la guarda previa existente; rechazar status distinto de running/paused/closed. Un status corto `alien` actualmente puede producir 200, sin depender del tamaño ni de nulls históricos. No se pide reconstrucción del estado.

Precisión adicional pendiente de contraste con root: los offsets externos de plannedBlocks/blockProjections carecen de CHECK sintáctico y se escriben directamente. `start_offset='bogus'` es alcanzable con restricciones publicadas intactas. Se propuso validar únicamente sintaxis de ZoneOffset sin TZDB, conservando nulls de proyección; no ejecutar ni añadir nuevas reglas hasta su decisión. Es independiente del mapper de recibos, que ya valida esos offsets.

Aprobación parcial de revisiones, guardas y aritmética leídas; pendientes sólo esos campos concretos y su cierre por A. Esta segunda lectura reemplaza el pendiente previo genérico de TEXT por las omisiones identificadas y retira el hallazgo negativo ya resuelto.
