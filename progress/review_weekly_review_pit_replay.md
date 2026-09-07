# Replay dirigido PIT19

**APPROVED con límites:9/9 KILLED,100% del replay**, EXIT0 real9aa5e4. Seis firmas objetivo SURVIVED originales pasan aKILLED y los tres extras yaKILLED siguenKILLED. Cero supervivientes, NO_COVERAGE, timeout o errores. El original178/190(93,6842105%,6S+6NC) permanece intacto y separado: no se atribuye100% a toda19.

Configuración aprobada root7dcfae, dos clases y dos mutadores, sin exclusiones añadidas; candidatosJUnit/4workers/80/timeout heredados. Inicio cee1b6; tiempo5m29sGradle/5m18sPIT, cobertura4m52s y mutación25s. No hubo cambios a producción/tests/config durante el replay.389 hashes idénticos antes/después, SHA303404AAD2699875D4CABABEC248C95CFFBA98EFC6A2810428C0237A4FDF0FFF.

XML/HTML preservados en weekly_review_pit_replay_final. XML SHA56A185A97FD9903A748A3CB392526039C70E0B0A78D53446CC9475EC62C77BFA. Log/EXIT originales y manifest de todos los artefactos en weekly_review_pit_replay_artifacts_manifest.json. weekly_review_pit_replay_inventory.json compara descriptor, clase, método, línea, mutador e índices con weekly_review_pit_replay_expected.json: nueve coincidencias exactas, ninguna firma faltante ni extra inesperada.

## Correspondencia

Boundary significa ConditionalsBoundaryMutator; Void significa VoidMethodCallMutator. Las clases están bajo com.apptolast.organization.

| Clase/método | Línea/índice | Mutador | Original | Replay/oráculo |
| --- | --- | --- | --- | --- |
| domain.WeeklyReviewWindow.summarize |31/21|Boundary|SURVIVED|KILLED, año1 válido|
| domain.WeeklyReviewWindow.summarize |31/25|Boundary|SURVIVED|KILLED, año9999 válido|
| domain.WeeklyReviewWindow.summarize |35/62|Boundary|SURVIVED|KILLED, año9999 válido|
| domain.WeeklyReviewWindow.summarize |29/5|Void|SURVIVED|KILLED, UTC inválido con fecha local válida|
| application.ReadWeeklyReview.get |25/16|Boundary|SURVIVED|KILLED, año9999 válido|
| application.ReadWeeklyReview.get |26/27|Boundary|SURVIVED|KILLED, año9999 válido|
| domain.WeeklyReviewWindow.summarize |35/56|Boundary|KILLED|KILLED, año1 válido (extra)|
| domain.WeeklyReviewWindow.summarize |67/96|Void|KILLED|KILLED, fronteraUTC inferior heredada (extra)|
| application.ReadWeeklyReview.get |24/10|Boundary|KILLED|KILLED, fronteraUTC inferior heredada (extra)|

Los tres oráculos fueron inicialmenteGREEN en original y ahora tienen contraste rojo real frente a las seis mutaciones, según killingTest del XML. No hubo defectos de producción ni recampaña global. Los seis accessors internos NC originales quedan como límite documentado, sin pruebas artificiales ni reclasificación. Gatebackend acreditado; no decide cierreUI/UX/CI/despliegue. Gradle liberado aroot para initfinal.
