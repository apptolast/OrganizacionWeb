# Revisión anticipada PIT13 — lectura parcial no disponible

2026-09-06. La única lectura intentada del archivo específico backend/build/reports/pitest-reschedule/mutations.xml falló por bloqueo de compartición: «The process cannot access the file … because it is being used by another process» (cbbdcb).

El archivo existe, pero no se pudo obtener su contenido. Los contadores0 impresos después del error son un artefacto del comando y NO evidencia de cero mutantes ni de ausencia de registros completos. No se seleccionó ni analizó ninguna entrada; no hay score, resultado definitivo ni candidato equivalente atribuido.

Se conserva el XML original intacto. No se reintentó lectura, copiado o sondeo en bucle, ni se ejecutaron suites o cambios de fuentes/tests/configuración. El análisis de las primeras15 entradas SURVIVED/NO_COVERAGE queda pendiente de un corte legible después de la campaña o de una entrega estable autorizada del autor. No se infiere fallo de PIT por este bloqueo de lectura externo.

## Única lectura compartida autorizada posterior — provisional

La lectura FileStream(Open,Read,FileShare.ReadWrite), con StreamReader/Dispose en finally, sí funcionó (c4ca9a). Snapshot tomado2026-09-06T20:55:24.8534811Z; longitud del archivo al abrir303104bytes. Se seleccionaron sólo las primeras entradas SURVIVED/NO_COVERAGE con cierre </mutation>: había3 seleccionables en ese corte, no15. Esto no es un inventario final ni acredita cuántas entradas faltaban por escribir.

Se conserva una copia derivada únicamente de esas tres entradas completas en progress/reschedule_pit_partial_review.xml, SHA256 E90CC2C5508AE95C408301E9AEE3D955F7971B56BFA9FC788BCFF2884A8FD2BF. No es copia íntegra del informe ni sustituye al original. No se volvió a leer el XML activo ni se calculó score.

### Identidades y análisis anticipado

Clase común: com.apptolast.organization.adapter.http.RescheduleController. Los índices siguientes son los índices de bytecode del XML, no ordinales de aparición; la identidad se expresa por clase/método/línea/mutador.

| Método / línea / mutador | Estado parcial y evidencia | Grupo de oráculo propuesto después del freeze |
| --- | --- | --- |
| blockStateConflict,213, org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator; index20 | SURVIVED,7testsRun. Invierte sólo la elección del título según status412; no cambia HTTPstatus/code. Los tests s6_cancelStaleRevisionReturnsConflictWithoutWriting y s6_cancelCurrentCancelledBlockIsDefinitiveConflict comprueban status/code, pero no la explicación. | Completar un oráculo existente de problema de revisión:412 debe explicar revisión más reciente, sin usar la explicación de rechazo definitivo. El título intercambiado tiene efecto público; no se declara equivalente porque el cliente clasifique por code. No hace falta una prueba por cada código409. |
| move,42, org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator; index37 | NO_COVERAGE,0testsRun. La rama If-Match ausente retorna requiredRevision. El caso s19_moveQueryPrecedesMissingRevisionAndHistoricalReplay elimina If-Match pero abandona antes por query inválida; s5_previewRequiresBlockRevisionWithoutWriting cubre otra ruta; missingAvailabilityRevision cubre línea44. | Un caso HTTP de mover con If-Match ausente y todos los demás datos válidos, exigiendo428 problem+json/PRECONDITION_REQUIRED y ausencia de escrituras. No repetir la matriz completa de headers11. El null alteraría respuesta pública; aún no se ejecutó el oráculo propuesto. |
| moveRequest,120, org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator; index60 | SURVIVED,31testsRun. Elimina Iterator.forEachRemaining, por tanto desaparece la comprobación de campos desconocidos tanto en preview como move. El caso de objective extra actual pertenece a cancel, que tiene otro bucle. Las validaciones de campos conocidos no detectan un extra agregado a un cuerpo por lo demás válido. | Un caso de movimiento con cuerpo válido más objective extra, preferiblemente ante key ya confirmada, que rechace400/UNKNOWN_FIELD antes de replay y conserve hechos. Complementa la recuperación existente y verifica una frontera compartida real; no añadir todos los posibles nombres ni fabricar un error con varios campos inválidos. |

Fuentes y tests contrastados en1cdb90/2a826e. La configuración reschedule incluye el namespace de pruebas existente; la falta de cobertura de move42 se explica por la rama concreta, no por una exclusión inferida del targetTests. Las tres propuestas son grupos de conducta pública, no objetivos de porcentaje.

No se ha demostrado un defecto en producción actual: las guardas correctas están presentes. Se han identificado posibles huecos de oráculo a partir del reporte parcial, sujetos al resultado final y revisión del coordinador. No se atribuyen equivalencias, kills futuros, cambios de denominador ni resultados de ejecución que no ocurrieron. Ningún código/test/config ni runtime fue modificado.
