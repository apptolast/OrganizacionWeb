# Corrección de expectativas del arnés durante incremental21

CI34173605033 falló60/61 pruebasNode: scripts/project.test.mjs línea63 esperaba tempDirName original aunque la configuración incremental autorizada ya usa refined. RED remoto conservado en customization_ci_34173605033_failed.log, revisiónroot8e686d/1f9f6c.

Delta exclusivamente tres literales de la misma prueba: tempDirName, jsonReporter.fileName y htmlReporter.fileName pasan a rutas exactas refined. No se flexibiliza ninguna comparación ni se cambian80,8workers,candidatosVitest,plugins,ignores o selectores. Las dos rutas de reporte estaban ocultas por el primer assert fallido. Producto, configuración seleccionada, testsVitest y sandbox no se editaron.

GREEN `node --test scripts/project.test.mjs`: EXIT0 3ed5a4,61/61,0fail/cancel/skip/todo. Log customization_harness_incremental_paths_green.log. Diff leído2b64cf: únicamente tres sustituciones.

SHA256 scripts/project.test.mjs:950E435795022E6DA8A275BA08B05E60C7D03C3A2E4B5A99439B1FFEFE82E68B.
Configuración sólo leída al cierre:D5E38935564067252AD2900EB7B9ABBE239269BD3B744B513E6CB8D2F14413B0.

El cambio es testNode externo a Vitest, realizado durante la campaña incremental bajo autorizaciónroot. No acredita nueva mutación ni altera sus entradas efectivas JS/tests/config. Root/C revisan antes de integrar; no commit/push del autor.