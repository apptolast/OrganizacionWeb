# PIT de persistencia de importación: original completo

**Gate estricto superado: 316 KILLED / 330 totales = 95,7575757576 %.** EXIT 0 `afd411`. El motor cuenta 319 detectados incluyendo tres TIMED_OUT; este informe no los suma a KILLED ni los retira del denominador. Estados originales: 316 KILLED, 10 SURVIVED, 1 NO_COVERAGE, 3 TIMED_OUT; cero otros estados.

Candidato `bb0064c`, comando oficial `node .harness/harness.mjs mutate import_data-persistence-backend`. Cuatro clases completas (PostgresImportDataStore, ImportRecordValidator, ImportCustomizationValidator e ImportCounts), cinco candidatos (persistencia, validador, concurrencia, escala y wiring), cuatro workers y umbral 80. Sin exclusiones nuevas, modificaciones o relanzamientos. Baseline válida en 142 segundos; Gradle terminó en 2 h 21 min 10 s. PIT ejecutó 4427 pruebas; no confundirlas con métodos distintos de JUnit.

`import_persistence_pit_original/inputs_before.json` y `inputs_after.json`: 512 entradas idénticas, cero diferencias. XML y HTML completos preservados en `report/`; `run.log` y `run.exit` originales. `inventory.json` conserva las 330 firmas con clase, método, descriptor, línea, mutador, índices, bloques, descripción y estado; `residues.json` conserva los 14 no KILLED sin reclasificación. Comprobación `8183dd`.

| Clase | KILLED | Total |
| --- | ---: | ---: |
| ImportCustomizationValidator | 28 | 29 |
| ImportRecordValidator | 98 | 108 |
| PostgresImportDataStore | 162 | 164 |
| ImportCounts | 28 | 29 |

## Lectura acotada de residuos

- Los dos VoidMethodCall supervivientes de ImportRecordValidator.row, líneas 149/169 (índices 942/1041), eliminan `blockTime` para bloque/proyección. Son huecos de sensibilidad a validación temporal aislada: las comparaciones con recibos no sustituyen todos esos oráculos. La fuente original sí verifica duración y coherencia de locales, offsets e instantes; no se ha demostrado un defecto de producción ni se solicita una nueva campaña para subir porcentaje.
- Siete ConditionalsBoundary supervivientes corresponden a años locales 1/9999, objetivo de 500 puntos, minutos de sesión 1/1440 y duración de bloque 0/1440. Son límites sin detección en estos mutantes, no equivalencias declaradas ni fronteras eliminadas del contrato.
- ImportCustomizationValidator.values línea 96, IncrementsMutator índice 160: el índice sólo se pasa al validador de errores y éstos se traducen después a ImportInvalidFileException genérica. Es candidato de equivalencia observable de esta frontera; permanece SURVIVED en el resultado original.
- PostgresImportDataStore.lambda$stage$0 línea 111, VoidMethodCall índice 77: NO_COVERAGE al vaciado inmediato por tamaño de una fila. Los límites y lotes siguen en producción; este camino no queda acreditado por la campaña.
- TIMED_OUT originales: Store.lambda$stage$0 línea 102 NegateConditionals índice 11; RecordValidator.row línea 106 VoidMethodCall índice 674; ImportCounts.workSessionIntervals línea 3 PrimitiveReturns índice 5. Se preservan tal cual y no acreditan por sí solos detección funcional.

## Incidencias operativas y separación

Tres minions salieron TIMED_OUT a las 12:53:37–39 y PIT los sustituyó automáticamente dentro de esta misma campaña. Una captura de stack mostró Flyway.lock; la posterior inspección acotada de PostgreSQL propio no encontró bloqueadores ni advisory locks retenidos y observó contenedores de fixture rotando cada pocos segundos. No se demuestra contención persistente ni se atribuyen esos timeouts a un defecto de producto. No se canceló ni alteró el ensayo.

La alternativa de ocho workers/unidades de 32 tiene evidencias y resultado independientes. No se suma, mezcla o sustituye ninguna firma de este original. Este gate no acredita por sí solo aceptación live, UI humana, rollback o cierre global de la feature.
