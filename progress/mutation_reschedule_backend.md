# PIT13 — campaña inicial finalizada

Autorización coordinador tras init1806 EXIT0/91757f; backend1617, frontend1498, scripts22. Fuentes/tests/config congelados, sin cambios para matar mutantes.

Comando: `node .harness/harness.mjs mutate reschedule-backend`. Sesión98852, inicio UTC 09/06/2026 20:37:41. Snapshot before: 265 archivos en `reschedule_backend_pit_before.json` (backend/src/main, src/test y4config/scripts explícitos). Log íntegro `reschedule_backend_pit_initial.log`; salida prevista `backend/build/reports/pitest-reschedule`.

Threads4, umbral80, filtros/mutadores heredados. Pre-scan33ea7d:49unidades de prueba de mutación y247clases enviadas al minion. No son49mutantes ni247tests ejecutados; baseline/cantidad real aún pendientes. Advertencias Mockito/ByteBuddy registradas, sin fallo atribuido todavía.

Estado histórico inicial: RUNNING; se preserva el seguimiento sin atribuir resultados anticipados. El resultado final se registra abajo.

Seguimiento30ff50: cobertura calculada en127s y49unidades. Baseline no reporta tests fallidos; la mutación comenzó22:39:56. Referencia de sourcefreeze publicada por root1c467e5 (soporte0f355e0), sin cambio de hashes frente a before.

Muestra de actividad1f5846 a289s desde inicio: cuatro Java recientes iniciados22:39:56/22:40:45/22:42:01/22:42:02, CPU acumulada93,41/20,28/47,31/36,23s. Se observa renovación de procesos; no se infiere porcentaje completado. Sin resultado final, sin cancelación/reintento.

## Resultado final — PASS del umbral, residuos sujetos a judge

Arnés EXIT0 `338999`. PIT publica758mutantes/750Killed; score bruto exacto750/758 = **98,944591%**, umbral80 (reporter redondea99%). Sin ajustes por equivalencia o cobertura. **3Survived,5NoCoverage,0TimedOut,0NonViable,0MemoryError,0RunError,0NotStarted,0Started.** El resultado no transforma errores/timeouts en detección: no hubo ninguno.

PIT:1294segundos/21m34, incluyendo127s de cobertura y19m26 de análisis. Gradle:21m41. Se examinaron247clases de prueba según reporter y se ejecutaron3253tests durante análisis (4,29 por mutante); no equivalen a3253casos únicos. Cobertura de líneas de clases mutadas1356/1371 (99%). Advertencias Mockito/ByteBuddy y sugerencia comercial opcional Arcmutate preservadas en log, sin error de ejecución ni nueva dependencia.

Antes/después:265archivos de backend/src/main, backend/src/test y4archivos explícitos de soporte/configuración. `reschedule_backend_pit_before.json` y `reschedule_backend_pit_after.json` confirman **0mismatch** (`d9e1e9`). Root verificó también XML312a6b. Freeze de campaña liberado inmediatamente al terminar proceso; no se editó producción/tests para mutantes ni se ejecutó replay.

### Ocho resultados residuales, sin reclasificación del bruto

Los números son ordinales1-based del XML original, no IDs estables entre campañas. Identidad completa (clase, método, descriptor, mutador, índices y bloques) en `reschedule_backend_pit_pending.json`; los campos indexes/blocks se extrajeron como texto XML, evitando serializar objetos XmlElement vacíos. Lectura65427c.

| Ordinal | Estado original | Ubicación / mutación | Efecto que debe valorar el judge independiente |
| --- | --- | --- | --- |
|252|SURVIVED|RescheduleController.blockStateConflict:213, NegateConditionals|Invierte selección de título según status412. Status/code no cambian, pero el texto puede describir equivocadamente revisión frente a estado; no se descarta sólo por ser texto.|
|275|NO_COVERAGE|RescheduleController.move:42, NullReturnVals|requiredRevision por If-Match ausente devolvería null. Falta cobertura de esa rama HTTP en esta campaña; no es un helper fuera de ruta ni se declara equivalente.|
|288|SURVIVED|RescheduleController.moveRequest:120, VoidMethodCall|Elimina Iterator.forEachRemaining que rechaza campos desconocidos. Candidato a oráculo de estructura cerrada antes de negocio, sin añadir matriz de casos innecesaria.|
|464|SURVIVED|BlockBudget.calculate:34, ConditionalsBoundary|Cambia frontera de seconds>0 en guardia de rango anual. Contrastar invariantes/precedencia y evidencia histórica de11 antes de declarar equivalencia o pedir otro test.|
|674|NO_COVERAGE|BlockMoveRequest.endLocal:6, NullReturnVals|Accessor generado no observado por tests seleccionados. withObjective utiliza campos directamente; revisar uso real/serialización antes de fabricar test espejo.|
|675|NO_COVERAGE|BlockMoveRequest.endOffset:6, NullReturnVals|Mismo análisis de getter endOffset, identidad individual preservada.|
|676|NO_COVERAGE|BlockMoveRequest.startLocal:6, NullReturnVals|Mismo análisis de getter startLocal, identidad individual preservada.|
|677|NO_COVERAGE|BlockMoveRequest.startOffset:6, NullReturnVals|Mismo análisis de getter startOffset, identidad individual preservada.|

Esta tabla identifica efecto/pregunta; no acepta equivalencias ni ordena cambios de autor. Root asignó el dictamen de los8resultados a otro juez. Cumplir umbral no sustituye esa revisión.

### Artefactos conservados

- XML original `backend/build/reports/pitest-reschedule/mutations.xml`: SHA256 `ee62629b1c1f1d58e1ccf3fea68d60600c49a54baf9f38530e1d7c58e244ebd6`.
- HTML original `backend/build/reports/pitest-reschedule/index.html` y páginas asociadas: índice SHA256 `2041408b9b2d170f1200cb8acc871461e831394a5fb325c61e68391e3f482e92`.
- Log íntegro `progress/reschedule_backend_pit_initial.log`: SHA256 `68fc2ed91c7213884332168060e49501c9c6b4f4aad7307beaa597a3b92c11ed`.
- Scope integrado54fuentes/23patrones y hashes en `reschedule_backend_mutation_scope_integrated.json`. El inventario real generó758mutantes; no se confundieron interfaces/records con mutantes previstos.

La espera larga del último minion se inspeccionó sin cancelarlo: a968s sólo60544 seguía vivo; dos muestras jcmd47bfc4/c20add a~810/849s de vida del proceso mostraron main en MutationTimeoutDecorator/FutureTask con objetos distintos y CPUcreciente. La campaña terminó correctamente y confirmó0timeouts; el nombre de esa clase de PIT nunca se contó como timeout. Los dos dumps temporales propios se retiran después de conservar aquí el diagnóstico, no los reportes originales ni rutas protegidas.

Sin Git/commit/push, cambios a fuentes/tests/configuración, limpieza de ancestros ni nuevas campañas. PASS corresponde únicamente a la medición y umbral de esta campaña; cierre de feature13 pertenece al coordinador tras judge.

Corrección de referencia del ordinal275 tras contraste rootdb1d5a: línea42 corresponde a If-Match ausente; Availability-Revision está en44 y ya tiene caso. El mensaje preliminar del autor confundió esas dos guardas vecinas; XML/JSON original nunca cambiaron. Tabla corregida, sin test/replay nuevo.
