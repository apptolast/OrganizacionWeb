# Replay dirigido PIT18 — resultado final

EXIT 0 real 3a2a66; única medición autorizada, 5 min 30 s. 17 mutantes: 15 KILLED y 2 SURVIVED; cero NO_COVERAGE, timeout o errores. Score del subconjunto 15/17 = 88,2352941%, sin alterar umbral80. No sustituye el gate global original 263/284 = 92,6056338% ni borra sus 19S/2NC.

XML/HTML/log preservados en progress/history_pit_replay_final. XML SHA AF8A632A85DF718097364B88420536D4F21CFF5A42B5CBCA5FB87BB89F407F49. Inventario completo17 en history_pit_replay_inventory.json; cada entrada indica si corresponde a objetivo original. Los 376 inputs de before/after permanecen idénticos (aa8e2b), incluido init. No modificaciones de producción/tests/config durante medición.

Se encontraron exactamente las seis firmas originales, sin ausencias ni ambigüedad; todas KILLED por su nuevo oráculo público:

| Clase/método/línea/mutador/índice | Test que mata |
| --- | --- |
| ReadHistory.list:15 ConditionalsBoundaryMutator [28] | s6_exactlyTwentyFactsHaveNoNextCursor |
| PostgresHistoryQueries.lambda$compare$1:215 EmptyObjectReturnValsMutator [6] | s17_afterUuidCannotExceedUpperAtEqualTimeAndFamily |
| PostgresHistoryQueries.sessionContext:229 BooleanTrueReturnValsMutator [44] | s23_coherentSnapshotContextCannotOverrideDurableTask |
| PostgresHistoryQueries.validTransition:286 MathMutator [221] | s1_runningCloseRetainsItsPositiveFinalInterval |
| PostgresHistoryQueries.validTransition:258 BooleanTrueReturnValsMutator [74] | s23_extensionPreviousEndCannotPrecedeOriginalPlannedEnd |
| PostgresHistoryQueries.validTransition:300 BooleanTrueReturnValsMutator [286] | s23_unknownDurableActionAndReceiptRemainUnavailable |

Once extras explícitos: nueve KILLED, dos SURVIVED. Los supervivientes son validTransition:256 [54] y :257 [60], ConditionalsBoundaryMutator, límites legítimos additionalMinutes1/1440 ya inventariados en original. No son nuevos defectos de producto confirmados ni equivalentes; no se solicitaron oráculos nuevos ni otra campaña por porcentaje. Los nueve extras muertos conservan clase/método/línea/índice y killingTest en el inventario completo.

Conclusión: los seis refuerzos muerden las seis mutaciones previstas. Medición dirigida satisfecha con límites conservados; no se afirma100% global ni se recalcula una campaña original ficticia sumando resultados. Revisión final root pendiente; no implementación adicional.
