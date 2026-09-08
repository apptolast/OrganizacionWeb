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
