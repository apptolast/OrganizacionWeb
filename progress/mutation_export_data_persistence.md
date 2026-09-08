# PIT original: persistencia de exportación

## Configuración y evidencia

Ejecutado en aislado `OrganizacionWeb-export-http`, HEAD 9e90e53, mediante el comando oficial `node .harness/harness.mjs mutate export_data-persistence-backend`. Los 449 paths backend/src y build.gradle.kts coincidían con primaria antes de arrancar (c4af05). Scope de nueve patrones completos aprobado, incluidos internos y ApplicationConfiguration completo; todos los candidatos JUnit, cuatro workers, umbral 80 y parámetros oficiales intactos. No se repitió el gate JUnit idéntico ni se modificó producción/tests/config durante la campaña.

Proceso 39843: **EXIT 0 e21b74**, BUILD SUCCESSFUL en 48m13s. Cobertura previa 340 segundos, sin fallos; mutación 42m17s. El motor examinó 448 clases candidatas y ejecutó 2013 pruebas durante mutación: no son el conteo del gate global. Cobertura de líneas mutadas 504/518. Se crearon seis unidades; los nueve patrones incluyen interfaces que no necesariamente generan mutantes.

Raw XML/HTML preservado completo en `export_persistence_pit_original_raw`. Log y código de salida en `export_persistence_pit_original.log` y `.exit`. Inventario de los 359 conserva clase, método, descriptor, línea, mutador, índices/bloques, descripción, estado, detected y killingTest; los IDs numéricos son ordinales documentales. `export_persistence_pit_original_residues.json` contiene los tres residuos exactos. Las **453 entradas before/after permanecen idénticas**, cero diferencias (943879).

## Resultado sin reclasificación

| Estado original | Cantidad |
| --- | ---: |
| KILLED | 356 |
| SURVIVED | 1 |
| NO_COVERAGE | 1 |
| MEMORY_ERROR | 1 |
| Total | 359 |

No hay TIMED_OUT, RUN_ERROR, NON_VIABLE ni estados pendientes. El resumen del motor dice 357 detectados porque incluye MEMORY_ERROR con detected=true. Este informe conserva el resultado bruto y calcula **356/359 = 99,164345 %** como proporción conservadora. Supera 80 sin descontar errores ni ausencia de cobertura. No suma ni mezcla la campaña HTTP/recibos anterior.

## Los tres residuos

1. **Ordinal 134, NO_COVERAGE**: PostgresExportDataQueries.lambda$projections$0, descriptor `(Lcom/fasterxml/jackson/core/JsonGenerator;Ljava/sql/ResultSet;)V`, línea 471, VoidMethodCallMutator, índice 109, bloque 21. Elimina writeNumberField de durationMinutes no-null. El nominal de proyección cancelada acredita null, pero falta el caso de proyección completa válida con duración numérica. Es una laguna concreta del oráculo de @s8, no un defecto de producción demostrado ni equivalencia. Refuerzo posible: proyección RESCHEDULED completa y comparación exacta de campos contra reserva original separada.
2. **Ordinal 247, SURVIVED**: PostgresExportDataQueries.values, descriptor `(Lcom/fasterxml/jackson/core/JsonGenerator;Ljava/lang/String;Ljava/util/Optional;)V`, línea 343, ConditionalsBoundaryMutator, índice 15, bloque 4. Cambia size > 12 por >= 12. Falta un nominal con exactamente doce pares persistidos que resuelvan definiciones propias, incluidos inactivos si corresponde. Puede rechazar el máximo válido, por tanto no es equivalente. Refuerzo posible: doce valores legítimos presentes conservados en la salida. No implica que el código original falle.
3. **Ordinal 325, MEMORY_ERROR**: ExportBuffer.write, descriptor `([BII)V`, línea 30, ConditionalsBoundaryMutator, índice 25, bloque 4. Cambia while(length > 0) por >= 0. Al alcanzar length=0 en frontera de segmento puede añadir segmentos sin avanzar size/offset/length y agotar memoria. Es explicación sustentada por el código de la mutación y el aviso original a 06:11:11, no una atribución a un test concreto: raw registra testsRun=0 y killingTest vacío. Se conserva MEMORY_ERROR, no KILLED ni timeout. No justifica aumentar heap ni ejecutar otro original para limpiar la estadística.

## Seguimiento y límites

Durante la espera se leyó una única traza `jcmd Thread.print` del minion 7600, preservada en `export_persistence_minion_diagnostic.txt`. Mostró un mutationTestThread recién iniciado esperando arranque PostgreSQL de Testcontainers y el coordinador esperando MutationTimeoutDecorator. No se canceló, alteró o relanzó la campaña; CPU progresaba y no se diagnosticó un bloqueo productivo.

Dictamen acotado: **umbral de persistencia superado conservadoramente**, con tres residuos explícitos. Los dos huecos de oráculo se comunican para decisión de root; no se persigue 100 %, no se hizo replay ni se añadió una matriz nueva. Esta campaña no acredita UX, descarga en navegador, TLS, despliegue o aceptación final. Las validaciones globales de root y E2E de B tienen evidencia propia.
