# Mutación backend18 — campaña original final

Gate numérico SUPERADO; revisión de residuos pendiente de decisión root. Única campaña por bin/harness.ps1 mutate history-backend, sesión 68186, EXIT 0 real f7a179. Gradle 21 min 41 s. No replay ni cambios productivos o de tests.

XML original: progress/history_pit_final/mutations.xml, SHA256 D8E242C11763F095BDEAA23167184E4C77C441D3D430E74D285F0E08397610D5. Se preservaron HTML, XML y log completo en ese directorio. Inventario de todos los residuos: history_pit_residuals.json; identidad por clase/método/línea/mutador, nunca ordinal provisional.

284 mutantes: 263 KILLED, 19 SURVIVED, 2 NO_COVERAGE; cero TIMED_OUT, RUN_ERROR, MEMORY_ERROR y NON_VIABLE u otros estados. Score estricto 263/284 = 92,6056338%, superior al 80 configurado. No se excluyó ni reclasificó ningún residual. PIT informó 409/419 líneas y 1690 ejecuciones de tests: son métricas de campaña, no escenarios Gherkin ni tests únicos.

Los 375 inputs de history_pit_before_hashes.json permanecen idénticos en history_pit_after_hashes.json (verificación 662c6b). Before SHA319030010F70B1241E9EE1C5F8B7F4F2CE78C3721388532F6F571BFF811490A2. Alcance y filtros explícitos en history_pit_preflight.md y history_pit_scope_check.json. Todos los residuos salvo uno pertenecen a PostgresHistoryQueries; no supervivientes en HTTP/cursor/wiring.

## Residuos y oráculos diferenciables

En la tabla, BooleanTrue, Empty, Boundary, Void y Math designan respectivamente BooleanTrueReturnValsMutator, EmptyObjectReturnValsMutator, ConditionalsBoundaryMutator, VoidMethodCallMutator y MathMutator. Los nombres completos y detalles están en el inventario JSON.

| Clase/método/línea/mutador | Estado y lectura |
| --- | --- |
| ReadHistory.list:15 Boundary | 1 S. >20 pasa a >=20. Falta exactamente 20 entradas: devuelve cursor espurio. Oráculo de aplicación suficiente, sin otro PG. Prioridad alta por paginación visible. |
| PostgresHistoryQueries.lambda$compare$1:215 Empty | 1 S. Ignora UUID al vincular upper/after. Los empates SQL sí están probados, pero no upper/after misma fecha y familia con UUID invertidas. Un rechazo de cursor PG aislado cubriría esta frontera. |
| sessionContext:229 BooleanTrue | 1 S. El caso actual altera sólo before; igualdad de after esperado también lo rechaza y oculta la pérdida de control contextual. Cambiar project/task conjuntamente en before y after, conservando sessionId y metadata durable, a otro contexto produce oráculo independiente de privacidad. Prioridad alta. |
| validTransition:286 Math | 1 S. Resta último intervalo en CLOSE. Los recibos de cierre seleccionados son paused, suma cero. Leer CLOSE running real con tramo positivo verifica aceptación legítima; no requiere corrupción ni otra matriz de estado. |
| validTransition:258 BooleanTrue | 1 NC. La salida temprana de extensión inválida no se ejecutó. Recibo con previousEndAt anterior al plannedEndAt y effectiveEndAt ajustado coherentemente distingue el guard; forma/after restantes válidos. No equivalente demostrado. |
| validTransition:300 BooleanTrue | 1 NC. Acción desconocida. V16 define action TEXT NOT NULL sin CHECK de catálogo: corrupción conjunta de action durable y receipt.action puede alcanzar esa salida sin que sourceMatches la sustituya. Oráculo viable de corrupción controlada, no operación soportada ni migración nueva. |
| instantInRange:362 BooleanTrue | 1 S. Guard de rango/precisión en snapshots JSON. Extremos de SESSION_STARTED construido por SQL no ejecutan el mismo guard. Puede alterarse precisión de los instantes del start en ambos snapshots coherentemente; no basta otro test de columna PG. |
| validBefore:338 Boundary | 1 S. revision >0 cambia a >=0. Metadata expected_revision tampoco tiene CHECK positivo; caso corrupto revision0/after1 coherente sería diferenciable. |
| validBefore:342/343 Boundary | 2 S. Falta aceptación de recibo con inicio de 1/1440 minutos; pruebas de duración de otras features no atraviesan este lector. Límite de cobertura local, sin defecto de producto observado. |
| validClosure:371/372 Boundary | 2 S. Falta aceptación de workDate persistido en años 0001/9999 en recibo CLOSE. No proponer cálculo TZDB: sólo forma persistida. |
| validTransition:256/257 Boundary | 2 S. Extensiones legítimas de 1/1440 minutos no seleccionadas por historial. Cobertura de comandos17 no cubre estos guards del lector. |
| validTransition:251/279 Void requireClose | 2 S. validBefore ya exige running/paused, pero requireClose también rechaza Long.MAX_VALUE. No declarar equivalencia: el límite de revisión no está duplicado; un recibo corrupto con overflow puede distinguirlo. Baja prioridad frente a privacidad/página. |
| validTransition:252/280/304 Void requireTime | 3 S. Falta rechazo de occurredAt anterior a changedAt en extensión/cierre/P-R seleccionados con resto coherente. Guard productivo presente; ausencia de oráculo local, no bug demostrado. |
| validTransition:264 Void requireTime(end) | 1 S. Falta extensión con fin calculado fuera del rango público. La fórmula puede coincidir en JSON aunque end esté fuera de rango; guard no demostrado equivalente. |
| validTransition:303 Void requireTransition | 1 S. before running para RESUME con after running coherente distingue estado anterior incompatible; validBefore no impide ese estado inicial. No equivalente. |

Total tabla: 19 S y 2 NC. No se concluye que los restantes guards sean inalcanzables ni que los records heredados hayan quedado certificados por pruebas de comandos.

Propuesta acotada a revisión: priorizar página terminal20, vínculo UUID, corrupción contextual conjunta y lectura CLOSE running. Son cuatro conductas públicas distintas y oráculos pequeños; no implementar ni medir de nuevo hasta decisión root. Las dos salidas NC son además alcanzables mediante corrupción controlada y quedan expresamente inventariadas. El producto actual conserva las guardas; la campaña revela carencias de oráculo, sin nuevo fallo funcional confirmado en fuente original. No perseguir 100% ni repetir todas las matrices históricas.
