# Mutación backend15 — campaña original

**Veredicto: PASS del umbral.** Resultado final estricto: **523 KILLED / 525 total = 99,6190476%**, umbral80%. **0 SURVIVED, 2 NO_COVERAGE, 0 TIMED_OUT, 0 errores**. EXIT0 del arnés7a1af2; XML finalfe1dfe. No se excluyó ni reclasificó ningún mutante.

Contexto de inicio: Autorización root tras revisión A+B APPROVED e integración0dbd562; tests globales1890 Java/79 suites sin fallos/errores/skips preservados en pause_resume_global_init_xml (df574b), frontend1666 yNode32 informados por root. Único fallo de formato corregido por C y lint completo GREEN835359 antes de medir.

Comando: `node .harness/harness.mjs mutate pause_resume_session-backend`. Scope Gradle pause_resume_session, JUnit completo,4 workers, umbral80, configuración y filtros heredados sin modificación durante campaña. Informes originales backend/build/reports/pitest-pause-resume-session. Manifest anterior mutation_pause_resume_backend_before.json; el manifiesto preparatorio anterior no sustituye este freeze final.

Se separarán KILLED, SURVIVED, NO_COVERAGE, TIMED_OUT y demás errores del XML final. No se contabilizarán timeouts como kills estrictos ni se excluirán residuos para mejorar porcentaje. No replay automático ni cambios de código/tests/config.

Diagnóstico read-only solicitado por root durante ejecución: c42bde/563313 observaron CPU efectiva en tres minions durante15s, sin interpretar XML parcial como resultado. Thread.print único d66821 preservado en reports/pitest-pause-resume-session/thread-minion-{65412,65976}.txt: ya quedaban dos minions. El hilo de prueba65412 tenía2,12s de vida y esperaba respuesta Docker a KillContainerCmd durante stop;65976 tenía3,01s y esperaba LogMessageWaitStrategy en TestcontainersExtension.beforeAll. No apareció lock SQL ni deadlock Java. La muestra acredita lifecycle reciente de contenedores, no bloqueo fijo durante toda la campaña; no permite identificar el nombre JUnit de la prueba desde las pilas de extensión. No hubo cancelación, cambios de configuración ni nuevas ejecuciones.

Root integró historia Git en6b8f360 durante la campaña y verificó árboles idénticos a su padre (ca2e4b); el freeze se validará por hashes de archivos, sin atribuir cambios productivos a ese merge.

## Resultado final y conservación del freeze

Campaña única iniciada2026-09-07T01:59:53Z. Duración PIT29m42s y Gradle29m50s; captura posterior02:30:00Z. PIT examinó300 clases de prueba, ejecutó3338 pruebas para525 mutantes y reportó cobertura826/836 líneas. Estos contadores describen esta campaña; no sustituyen las1890 pruebas de init.

Los315 hashes anteriores/posteriores son idénticos (fe1dfe). Manifiestos mutation_pause_resume_backend_before.json y mutation_pause_resume_backend_after.json. No hubo cambios de fuentes/tests/config ni una segunda campaña. Los informes originales permanecen en backend/build/reports/pitest-pause-resume-session/:

- mutations.xml SHA256 F0B68A866A3BBBF29BC96284062A9EDDC2BFF2A81153654430BA9023EADC0D51.
- index.html SHA256 50A8995FAC6BAD4FA9A10FBF80A123E9C854AFAA381238DF20E08A336B2765AE.

Log completo: mutation_pause_resume_backend.log. Inventario exacto de residuos: mutation_pause_resume_backend_residuals.json, con clase/método/descriptor/línea/mutador/índice/bloque; se evita una identidad ordinal dependiente del orden XML.

## Dos residuos reales de cobertura

Ambos pertenecen a PostgresWorkSessionStore y NullReturnValsMutator:

1. lambda$commit$0, línea132, índice35, bloque3: cambia por null el retorno de readOnly.execute después de TransitionInsertCollision.
2. lambda$commit$2, línea138, índice31, bloque5: cambia por null la confirmación replayed=true tras encontrar recibo ganador con intención idéntica.

Es la salida exitosa de la recuperación defensiva tras colisión. Las pruebas actuales alcanzan allí conflicto de intención (409) y ausencia de ganador (503), pero no el retorno exitoso; el replay ordinario idéntico y la carrera same-key sí están medidos en el camino anterior protegido por el lock. Por eso son NO_COVERAGE, no kills ni equivalencias demostradas. Un eventual refuerzo debería exigir recibo histórico exacto y replayed=true después de rollback/consulta nueva, mediante una inyección de visibilidad claramente declarada; no debe fabricar dos sesiones abiertas del mismo propietario ni anticipar estados16. No se ejecutó ese refuerzo ni se cambió el denominador.

La inspección no encontró un defecto productivo nuevo en esos dos retornos: el código devuelve la confirmación esperada y conserva la comparación de intención. El hueco de oráculo queda explícito para el juez. El umbral aprobado se supera sin excluirlo; este PASS de medición no marca automáticamente feature15 done ni sustituye el dictamen final de root.
