# Cobertura adicional del núcleo — schedule_block

Cesión explícita del coordinador y autor backend tras freeze del publicador. Ponytail full, Caveman lite y contrato aprobado a84e42f. Alcance: cinco tests del núcleo; producción permanece sin cambios salvo defecto confirmado y comunicado. Bitácora propia para evitar colisiones con TDD HTTP/PG. Se completan pendientes de review_schedule_block_domain.md distinguiendo casos inicialmente verdes de cambios nuevos.

## Ciclos

1. @s3: BlockRequestTest.s3_acceptsExactlyFiveHundredSupplementaryCodePoints acepta 500 caracteres suplementarios tras retirar Unicode White_Space exterior. Inicialmente GREEN, suite BlockRequestTest, salida 368659.
2. @s8: fila Madrid con 02:45 +02:00 a 02:15 +01:00 añadida a s6_s7_s8_s10_acceptsExactBoundariesAndHistoricalOffsets. Afirma duración de 30 minutos, instantes correspondientes y ambos offsets. Inicialmente GREEN, ResolvedBlockTimeTest, salida 03baf2.
3. @s11/@s28: s11_s28_projectsMidnightAndHistoricalReservationsIntoCurrentBudgetZone cubre fin exactamente a medianoche sin fila cero, proyección UTC a Madrid, cruce del ancla final de otoño y reparto de reserva histórica en dos días mediante dos solicitudes diurnas no solapadas. Las filas afirman BudgetDay completo y suma exacta; los instantes históricos no cambian. Cinco casos inicialmente GREEN, BlockBudgetTest, salida 71c460.
4. @s13/@s14: s13_s14_rejectsOverlapUsingFirstStartThenUuid mantiene el caso de empate UUID y añade un conflicto de inicio anterior con UUID superior. Dos casos inicialmente GREEN, salida 7c353f.
5. @s13/@s15: s13_s15_allowsAdjacentBlocksAtBudgetBoundaryOrWithSpecificConsent prueba contigüidad antes/después, reservas de otro proyecto propio, exceso exactamente cero sobre 120 minutos y creación con presupuesto cero/permiso explícito. Afirma el preview completo antes de crear y el éxito/intención resultante. Tres casos inicialmente GREEN, salida 8aad42.
6. @s7: s7_rechecksServerClockAfterPreviewAndAcceptsExactEquality retiene preview y precondición, luego prueba creación al mismo instante o un segundo después. En avance exige startLocal/IN_PAST; igualdad confirma tiempo/createdAt. Dos casos inicialmente GREEN, salida f70236.

No hubo RED nuevo de producción: todas las reglas existían y estos casos completan evidencia ausente. No se inventa un ciclo de implementación para estas incorporaciones. La evidencia HTTP actual ya cubre estados idea/active/paused y createdAt con microsegundos en ScheduleBlockApiTest.s2_createAtomic, y replay histórico en s21_s22_replayConfirmedIntention; no se duplicaron en el núcleo.

## Verificación y entrega

Formato global autorizado y coordinado con backend en su frontera de edición. Ejecución final: `gradlew.bat spotlessApply test --tests '*BlockRequestTest' --tests '*ResolvedBlockTimeTest' --tests '*BlockBudgetTest' --tests '*PlannedBlockTest' --tests '*PlanBlockTest' --no-daemon`, EXIT 0, salida f92fc9, 16 segundos. XML leído tras terminar: **93 pruebas, 0 fallos, 0 errores y 0 omitidas**: PlanBlockTest 24, BlockBudgetTest 16, BlockRequestTest 14, PlannedBlockTest 9 y ResolvedBlockTimeTest 30.

Sólo se modificaron cuatro archivos de pruebas del núcleo; PlannedBlockTest se ejecutó sin necesitar ampliación. Producción y contrato permanecen intactos por este autor. No PIT. La revisión de esta cobertura debe realizarla otro agente: este revisor inicial asumió autoría de los nuevos tests por cesión explícita y no se autoaprueba.
