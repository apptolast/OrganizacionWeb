# HTTP16 — TDD aislado

Contrato3E45F260…44285 y gate80c94b6. Árbol close-http, bundle real db8bb0e integrado por root como1c4831f. Únicamente controller y nueva CloseWorkSessionApiTest; puertos mock reales, ninguna infraestructura PG simulada como integración. Publicador congelado y aprobado por root por separado.

| Ciclo | Oráculo | Resultado |
| --- | --- | --- |
| 1 | POST nominal CLOSE7/closure4, Location y precisión, delegación íntegra | RED163382 (ruta ausente) → GREEN013b1f:1nuevo+48heredados15 |
| 2 | Tipo numérico de nota rechazado antes de aplicación | REDb35e44 (llegaba al mock sin resultado) → GREENa46553:2 |
| 3 | Replay200 conserva recibo y Location | Inicialmente GREEN5badf1 |
| 4 | Query antes de UUID/cuerpo inválidos | Inicialmente GREEN78aba0 |
| 5 | Falta de revisión antes de JSON | Inicialmente GREEN8ba888 |
| 6 | Sintaxis de token antes de JSON | Inicialmente GREENe66df1 |
| 7 | JSON concatenado antes de validar notas | Inicialmente GREEN10f3bd |
| 8 | Campo de sistema desconocido | Inicialmente GREENabc4e9 |
| 9 | Anonimato antes de notas | Inicialmente GREEN04ac2f |
| 10 | CSRF inválido antes de notas | Fixture incorrecto0f22aa combinó CSRF válido del helper con otro procesador;400 en lugar de403. Fixture sin token como patrón heredado → GREENe8b9d6 sin producción; no RED funcional |
| 11 | K sirve cierre histórico completo sin Location | Inicialmente GREENfce135 |
| 12 | Ausencia/null normalizados antes de aplicación | Inicialmente GREENa7e61f tras snapshot real Notes16f61c |
| 13 | Unicode2000 y whitespace preservados | Inicialmente GREENa82108 |
| 14 | NUL decodificado antes de propiedad/replay | Inicialmente GREENc39f8b |
| 15 | Surrogate aislado en siguiente paso | Inicialmente GREENfe2893 |
| 16 | Exceso2001 puntos de código | Inicialmente GREEN8648f6 |
| 17 | Token completo conserva identidad, problema412 cerrado | Inicialmente GREEN9a3e2e |
| 18 | Closed conserva State6 y snapshot3/token de revisión | Inicialmente GREEN1afdb6 |
| 19 | Intención distinta devuelve conflicto cerrado15 | Inicialmente GREEN66c650 |
| 20 | Error temporal conserva título15 | Inicialmente GREEN4bce03 |
| 21 | GET closure devuelve recibo completo, sin Location ni consulta de state | REDa8d962 (ruta ausente) → GREENcc36f5 |
| 22 | Query de closure precede al UUID inválido | Inicialmente GREEN039661 |
| 23 | Propiedad ajena representada por404 de sesión | Inicialmente GREEN169d02 |
| 24 | Sesión propia abierta representada por404 de cambio | Inicialmente GREEN93ba63 |
| 25 | Fallo de lectura devuelve503 cerrado, sin ausencia ni SQL privado | Inicialmente GREEN9e6fb4 |

Dependencia Notes copiada por root con SHA C1C9B239CDCCEB7B55815D3DF40EDEA37CCCB3E7D3A2F94F351BAAB2834D9994. Snapshot de A: excluirlo de entrega HTTP. Sus tests de validación son integración de constructor real con HTTP, no una segunda implementación.

Checkpoint20: Spotless real y regresión117 (20 nuevos +48 API15 +49 API14), EXIT0 0360f4. Logs `close_http_cycleN_*.log`, daemon768/384 y cuatro workers. Falta incorporar el bundle compilable de consulta closure de A; no copiar sólo el método abstracto dejando su implementación anterior incompleta. No se duplica su lógica. Se mantiene ReceiptResponse6 para PAUSE/RESUME y CloseReceiptResponse7 independiente, sin closure:null en DTO15. Paquete en curso, sin gate global ni evidencia PG de estos mocks.

## Entrega HTTP congelada

Root integró el bundle real de consulta2b1d7d8→3082ccd antes del ciclo21, con siete fuentes y sus tests dependientes; Notes pasó a estar versionado con idéntico contenido. Esos archivos de A no forman parte del diff HTTP. No se introdujeron defaults ni stubs para compilar la interfaz nueva.

Resultado final5744e9 EXIT0: Spotless y122 tests (25 nuevos,48 API15,49 API14), cero fallos/errores/skip verificados en681f93. Log `progress/close_http_final.log`; XML preservados en `progress/close_http_final_xml`. Hashes propios en `progress/close_http_freeze_hashes.json`: controller7EED9E97…AE2F82E, test7D5DB07F…20324F7. Diffcheck verde; sólo controller y nuevo test de producto pendientes de integrar, más esta bitácora/manifest. Los XML son evidencia local preservada, no parte necesaria del commit. Publicador aprobado e integrado previamente por root, sin cambios posteriores.

Mapa de alcance: @s1/4–6/12/16 acreditan representación/delegación de POST, normalización y precedencias de entrada; @s7 conserva el mapping temporal; @s24 el DTO de estado terminal; @s26–27 la consulta y errores de cierre. @s28 aporta aquí sólo el formato de recuperación por key y sesión. Los48 casos15 y49 de14 conservan problemas, seguridad y DTO anteriores. Las demás reglas temporales, propiedad efectiva, replay, lectura RR, fallos de commit, carreras y recuperación tras reinicio requieren pruebas reales de A/integración; los mocks no las acreditan. No se afirma E2E, smoke16, PIT ni cierre de feature.
