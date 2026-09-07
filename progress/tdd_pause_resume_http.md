# HTTP15 — TDD aislado

Propiedad C: nuevo WorkSessionStateController y WorkSessionStateApiTest. Sin Store, núcleo, configuración ni interfaces inventadas. Base514c996 compilable parcial de A; no fusionar rama entera. Ponytail full/Caveman lite. Gradle daemon768 en árbol aislado, sin globals.

Snapshot autorizado e858e6: copia exacta common→aislado de cinco dependencias reales, NO versionar ni integrar:

- application/ReadWorkSessionStateUseCase.java:1140EDF0362FCD9B69BA76574601CF4087CAEB02EDC31C3CC61A564E81596E48
- application/WorkSessionSnapshot.java:9C8E6521047227353EDBA4ABFBF909004A45F15BDA7CF848B48327849CB4B00E
- application/ReadWorkSessionChangesUseCase.java:379A640D94A88DB21186561A2E0214FE17345CD2E13CFFAA7594781725B5944F
- application/WorkSessionChangeNotFoundException.java:69B4164D92373453928333D16D7CAA743AC33DF121D48AF3FFC990FFBFC836E2
- domain/WorkSessionTransitionException.java:D8D35FFC186B5BE67ABA02201696A8CEB6EC27E09425D2CE92594B154B677A74

1. @s2 `s2_pauseReturnsClosedReceiptWithDecimalStrings`: REDe30ebe (controller inexistente) → GREEN485946,1/1. DTO de recibo6 y estados6 con revisión/acumulado String, conserva DTO7; Location y no-store. Token completo enviado al puerto real. Parser nominal aún sin guardas: se incorporan mediante sus ciclos, no se declara contrato completo.

| Ciclo | Oráculo individual | RED | GREEN |
|---|---|---|---|
|2|Resume envía acción/revisión propia, recibo6 exacto|073c9d|d31a9c|
|3|Snapshot3 con decimal String y cabecera propia, sin ETag/Location|b09254|388a1b|
|4|Recibo por id sin consultar estado actual|6237fa|83bef6|
|5|Recibo por key sin consultar estado actual|0ffca0|ffe986|
|6|Query repetida en pause impide negocio|89916e|d44005|
|7|Query state precede id inválido|d73f18|a155f4|
|8|Query recibo precede id inválido|d2e227|eb611b|
|9|Query byRequest precede key inválida|ab7579|678dc7|
|10|Revisión ausente precede JSON concatenado,428 cerrado|793bd9|a0e1d6|
|11|Key inválida precede revisión ausente|a3f200|99382a|
|12|Token entre comillas es400, no500|c75e3b|8691fe|
|13|Overflow de revisión es400, no500|e81c32|bcb5fb|

Cada foco ejecutó una nueva prueba individual. Se añadieron únicamente las rutas/guardas necesarias; seguridad real de WebMvcTest permanece activa, casos de uso simulados. No acredita persistencia, reloj o orden transaccional del core.

| Ciclo | Oráculo individual | RED | GREEN |
|---|---|---|---|
|14|Key ausente, campo REQUIRED|7b992b|a807a8|
|15|Key repetida, INVALID_VALUE|ac2763|347c16|
|16|JSON concatenado MALFORMED_JSON|9c1e2a|b5d3cb|
|17|Campo extra primero lexical|b27cd7|ef6ade|
|18|Raíz array INVALID_TYPE|6369f0|c614b1|
|19|Body ausente MALFORMED_JSON|4715fb|60b0d1|
|20|JSON duplicado precede campo extra|92a3d8|73a72f|
|21|Sesión ausente404 cerrado|dcace6|33a53a|
|22|Recibo ausente404 cerrado|4b8f31|b9d325|
|23|Token de otra sesión se conserva en puerto; traduce412|a305f0|20f0b2|
|24|Estado incompatible409 cerrado|f9ab33|cff989|
|25|Revisión agotada409 cerrado|2f412e|fa635b|
|26|Reloj de lectura fuera de rango409 con título15|dd5a70|0df1b2|
|27|Conflicto de key con título de cambio15|550c81|6a27da|

Los handlers son locales al nuevo controller para conservar exactamente14. El caso23 demuestra transporte del UUID y revisión completos hacia el puerto; el orden de propiedad/identidad/replay dentro de la transacción pertenece al paquete PG de A, no se afirma probado aquí.

## Casos adicionales, inicialmente verdes

Cada caso se añadió y ejecutó antes del siguiente, sin producción nueva ni matriz anticipada:

- 28–34, revisión0/signo/ceros iniciales/basura final/espacio/lista/repetición: bb8179,ccb096,cd38a8,c7e9a5,ecc69a,8314c3,69e3a4.
- 35–38, query de resume antes de headers ausentes; anónimo; CSRF; origen ajeno:617b2a,0fd4c1,26748b,43a5d8.
- 39–42,503 cerrado de state/recibo/key/comando, sin exponer causa SQL:ec85d3,edb410,8a5fd9,815c75. Traducción heredada, no simulación de commit PG.
- 43–44, replay200 con recibo/Location originales y revisión BIGINT máxima sin redondeo:7042fd,0a3137.
- 45–48,404 de propiedad antes de identidad token,404 key de cambio,415 antes query y JSON null: e7e954,13ec7c,629e99,eab685.

El contexto real de seguridad es el del proyecto; sólo se simulan los tres puertos de aplicación. El paquete no altera títulos de inicio14 ni traduce recibos históricos como estado actual. No se ejecuta PIT ni se utiliza el selector14 como gate15.

## Freeze HTTP para revisión independiente

Regresión8ce75d EXIT0:48 HTTP15 +49 HTTP14 =97, cero fallos/errores/omitidas (XMLa0498b). Log local preservado en progress/pause_resume_http_regression.log. XML copiados antes de otro foco a backend/build/reports/pause-resume-http-freeze:

- TEST-com.apptolast.organization.adapter.WorkSessionStateApiTest.xml:A5453F4FE6CCBF2E266D096D642DEB771ABCFF262899611E1CADEB2897647F5D
- TEST-com.apptolast.organization.adapter.WorkSessionApiTest.xml:FBD6FEFE2F4A2AF4F16EEE78DFD233CD41C3421B5F3B81B0AD15A860171BC5C9

Spotless real focal1e2fcb/3e41b3, segunda pasada IS CLEAN997f53/b41265. Diffchecka0498b verde. SHA256 propios:

- adapter/http/WorkSessionStateController.java:666B108F70F9BFFB1A8ED3EBCBDC9EA014621ADFCE5C4A2DD3E339B64D51DC63
- adapter/WorkSessionStateApiTest.java:7931BDFFE4E4FBE4A0E1941A38D4C00AD1C70431B8E41BBD4C034D4FF8778FC7

Mapa de alcance: @s1–3 representación y rutas; @s10–12 sintaxis/precedencia HTTP y seguridad; @s13–17 traducción de problemas y replay; @s21/23/24 traducción de fallos; @s25 rutas de recuperación histórica. Las carreras, intervalos, reloj y rollback reales corresponden al núcleo/PG; no se atribuyen a mocks ni se declara feature15 cerrada. Wiring y smoke/E2E se harán tras integración aprobada.

Integrar sólo los dos Java nuevos y esta bitácora. Los cinco snapshots enumerados al principio, la base parcial y artefactos build/log no son parte de ese diff. Publicación queda en su commit selectivo1233e0a separado. Ningún proceso de pruebas queda activo al freeze.
