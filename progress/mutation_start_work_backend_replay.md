# Replay backend14 — record del evento

Estado final: PASS, 11 KILLED de 11 mutantes (100 % bruto), EXIT 0. Autorización del coordinador para selector cerrado y replay tras refuerzo del oráculo. El informe original mutation_start_work_backend.md conserva347mutantes y score bruto340/347, sin cambios.

Refuerzo previo: WorkSessionPersistenceTest compara el JSON durable con once valores independientes, inicialmente GREENd8ed64; formato/check81027f. Caso de calendario imposible del otro autor PublishOutboxTest inicialmente GREEN231773 y153/153c54dab, congelado antes de snapshot. No cambia producción.

Selector: start_work_session-backend-replay despacha exclusivamente pitest -PmutationScope=start_work_session_replay. TargetClasses es sólo com.apptolast.organization.application.WorkSessionStarted completo; candidatosJUnit com.apptolast.organization.* intactos, cuatro workers y umbral80. Destino backend/build/reports/pitest-start-work-session-replay. Test del plan CLI RED55412c → GREENc99bff;30/30regresión scripts y formato bf07fd. Gradle --dry-run e84f3a evaluó configuración, no ejecutó PIT ni pruebas. No modificación del motor .harness ni exclusiones.

Review independiente aprobado antes de capturar hashes y lanzar la única ejecución por arnés. No se mezclará su denominador con347originales ni se atribuirá un kill a los residuales fuera de este record.

Inicio autorizado tras review APPROVED 07f35d y checkpoint 431abda. Arnés iniciado en 6ee512 (sesión 44456): snapshot previo de 289 archivos, una unidad de mutación y 273 clases candidatas enviadas a cobertura. Resultados aún pendientes; no se modifica el informe original.

## Resultado verificado

Ejecución única finalizada en f4bb5c: EXIT 0, 8 min 38 s de Gradle y 8 min 31 s de PIT. Baseline de cobertura completado en 124 segundos (882ffa), 273 clases candidatas; análisis de mutación 6 min 27 s, 185 ejecuciones de pruebas. XML final ac7a8f: 11 KILLED, 0 SURVIVED, 0 NO_COVERAGE, 0 TIMED_OUT y 0 errores restantes. Score bruto propio 11/11 = 100 %, umbral 80 sin cambios.

Las once identidades del record coinciden una a una con el original por clase, método, descriptor, línea, mutador e índice. Los cinco supervivientes originales ownerId, plannedMinutes, projectId, taskId y zoneId pasan efectivamente a KILLED; el killingTest en los cinco es WorkSessionPersistenceTest.s1_commitsRealStartAndEventWithoutChangingPlanning. El JSON esperado independiente corrige el acoplamiento del oráculo sin modificar producción. Inventario y correspondencia completos: mutation_start_work_backend_replay_inventory.json y mutation_start_work_backend_replay_mapping.json.

Verificación ac7a8f: 289/289 hashes antes/después idénticos, registrados en mutation_start_work_backend_replay_before.json y mutation_start_work_backend_replay_after.json. Corte de referencia 431abda; ningún archivo medido modificado durante la campaña.

SHA256 del XML replay: 9F944969A89581FD5EB36ADAB96ED028CD1327B327112371FF6DF9C47A787A09. HTML replay: 4033BF4C500D38D733D2880F817D4063C68197530FE46A49D6DE6857E0D5775D. Ambos están en backend/build/reports/pitest-start-work-session-replay. Log íntegro: mutation_start_work_backend_replay.log.

El XML original conserva SHA256 167AEB9D9461E8E5AECABC6D73D56105FCE892B539BB0670397BEF03DACF445F y su HTML 27DFF88E6215FD22008DC354600BB6964DC4C12D4F2DBB5AD1F3FD163568CB56. Su resultado permanece 340 KILLED / 347, cinco supervivientes, un NO_COVERAGE y un TIMED_OUT. Este replay no vuelve a medir OutboxMessage ni RabbitBrokerPublisher y no les atribuye kills. El test calendario verde es evidencia funcional adicional, no reclasificación de aquel NO_COVERAGE. No se mezclan denominadores ni se requiere otra campaña para perseguir 100 % global.

Dictamen de medición: objetivo acotado cumplido; no se observa residual accionable en el record. Entrega congelada al coordinador para incorporar al cierre contractual que administra por separado.
