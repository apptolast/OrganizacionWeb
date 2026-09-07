# Refuerzos de fronteras PIT19 y propuesta de replay

Tres oráculos autorizados por root, añadidos uno a uno únicamente a ReadWeeklyReviewTest. No producción ni cambios de scope global. El contraste rojo contra los mutantes está pendiente de replay; no se ha fabricado un RED original.

- `s6_yearOneClockAndSelectedWeekRemainPubliclyValid`: inicialmente GREEN030847; Clock y fecha0001-01-01 UTC, semana1–7 y extremo8. Distingue Window.summarize línea31 Boundary índice21.
- `s6_lastCompleteWeekAndClockInYear9999RemainPubliclyValid`: inicialmente GREEN1a5326; Clock y fecha9999-12-20 UTC, semana20–26 y extremo27. Distingue Read.get líneas25/26 Boundary índices16/27 y Window.summarize líneas31/35 índices25/62.
- `s6_utcClockOutsideRangeCannotBeHiddenByARepresentableLocalDate`: inicialmente GREEN86590b; Clock0000-12-31T23:30Z con zonaEtc/GMT-14 y fecha explícita2026-09-07 exige WeeklyReviewTimeOutOfRangeException. Distingue Window.summarize línea29 VoidMethodCall índice5; no confunde el UTC inválido con el año local ni con la semana seleccionada.

Comando focal por caso: `gradlew.bat -p backend test --daemon --max-workers=4 --tests '*ReadWeeklyReviewTest.<método>'`. Cada ejecución EXIT0. Primer intento de formato26f91b pasó ruta relativa a spotlessIdeHook; avisó que requería absoluta y no formateó (suite14verde, no evidencia de formato). Corregido a ruta absoluta883225: formato aplicado y regresión14/14 EXIT0, recompiló el test. Comprobación posterior d42c3b IS CLEAN/EXIT0, XML14/0/0/0 preservado en weekly_review_reinforcement_tests.xml. No se repitió suite global.

Freeze test SHA **A3E228DA1FC5F5FF8840AEC19C8C2AE6011583B2D83096105E5A181AC3F50BED**.30eb52 comparó388 entradas originales: sólo cambió este test,49 líneas añadidas; fuentes/config intactas.

## Replay propuesto, no iniciado

Init separado, derivado del patrón ya existente history_pit_replay.init.gradle: targetClasses exactamente ReadWeeklyReview y WeeklyReviewWindow (sin comodín); mutators exactamente CONDITIONALS_BOUNDARY y VOID_METHOD_CALLS. No se necesitan exclusiones adicionales de métodos: sobre el original selecciona nueve mutantes, seis objetivosSURVIVED y tres extras previamenteKILLED. `weekly_review_pit_replay_expected.json` enumera los nueve con clase/método/descriptor/línea/mutador/índice/bloque; no supone el resultado del replay.

Comando propuesto: `gradlew.bat pitest --no-daemon -PmutationScope=weekly_review --init-script ../progress/weekly_review_pit_replay.init.gradle`. Se conservan candidatos JUnit completos, cuatro workers, umbral80, timeout15000 y filtros heredados. Reporte separado reports/pitest-weekly-review-replay; preservar before/after/log/EXIT/XML/HTML independientes. Contrastar las seis firmas originales y todos los extras; no sustituir178/190 original por una suma sintética ni excluir6NC del denominador global.

La configuración dedicada y ejecución requieren revisión/señal de root. No se ejecutó segunda campaña.
