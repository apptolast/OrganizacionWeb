# TDD HTTP: revisión semanal 19

## Entorno y frontera

Árbol aislado OrganizacionWeb-weekly-http, rama codex/weekly-review-http desde 4fbc370; creación autorizada por root (568019). Árboles históricos preservados. AGENTS, Ponytail full/Caveman lite e instrucciones TDD aplicados.

pnpm install --frozen-lockfile en raíz EXIT0 cc8802 y frontend EXIT0 d1895f; dependencias reutilizadas sin modificar lockfiles. Init equivalente a init.sh: node .harness/harness.mjs init, sesión 99003, log progress/weekly_http_init.log. Resultado pendiente; todavía no se escribe producción ni tests.

## Reutilización prevista

HistoryController.date ya valida representación YYYY-MM-DD, fecha real y año mínimo 1; se puede usar desde el mismo paquete HTTP. BlockController.invalid conserva errores por campo. Principal y seguridad HTTP existentes suministran owner y no exigen CSRF para GET. El adaptador mapeará once campos, siete Day de seis campos y Totals de tres; long a cadena y capacidad nullable. No Clock, agregaciones, cursor ni acceso JDBC en el controlador.

Se leyó progress/handoff_weekly_review_backend_http.md. ReadWeeklyReviewUseCase y dominio deben llegar como bundle real compilable del autor A antes de crear mocks y comenzar el primer ciclo. Propiedad C: nuevo controlador y pruebas HTTP necesarias; no wiring, aplicación, persistencia ni frontend. No E2E, puertos de usuario ni campaña de mutación durante este arranque.

## Resultado de arranque

Init EXIT1 real 5055f6. Frontend: 1897 PASS / 2 FAIL en 40 archivos. Fallos heredados: work-session-end.test.tsx:770 no encuentra «Fin acordado actual:»; work-session.test.tsx @s36:431 espera cinco llamadas y observa tres después del remount. No se atribuye todavía una causa definitiva ni se reintenta para ocultar el fallo. Root informado para revisión del fixture con autor B. Bundle 641f656 recibido como autorización, aún sin aplicar mientras se resuelve este gate. Ningún cambio de frontend ni controlador por C.

## Init resuelto y checkpoint HTTP

Fix aprobado c8a9fb5 aplicado como 669bdaa, sólo dos fixtures y su evidencia.
Init repetido una vez EXIT0 d62927, log weekly_http_init_verified.log: 1899 frontend, 47 scripts y backend verdes. Después bundle real 641f656 incorporado como 1b2b5a1; nunca mocks de tipos ficticios.

Ciclos individuales: 01 DTO vacío/principal/µs RED db07b2 (controller inexistente) → GREEN 8012dd; 02 selección explícita y valores enteros RED bdbfbd → GREEN a82bbf; 03 query desconocida RED ebb2b5 → GREEN 83e7ab; 04 parámetros duplicados RED eec958 → GREEN f3e490. En 02 el primer fixture usaba plan mayor que una semana: se corrigió a 1800000001 µs físicamente coherentes antes de GREEN; no se acredita el valor imposible como contrato. Fallo original era ignorar parámetros, no precisión.

05 fechas inválidas, 06 autenticación antes de query y 07 almacenamiento 503 fueron inicialmente GREEN por reutilización (4beb5e/bc7c0f/d50f66), sin fabricar RED. Ninguna modificación de parser, seguridad o advice heredados.

Spotless real EXIT0 3bab1e; regresión focal EXIT0 a02007. XML preservado en weekly_http_checkpoint_xml y dos hashes en weekly_http_checkpoint_hashes.json. Checkpoint parcial: s1/s2 representación y forwarding; s4 sintaxis/duplicados; s5 seguridad heredada; s23 traducción del error. No acredita cálculo semanal, catálogo/rangos de aplicación, transacciones, persistencia ni integración real. 409 espera excepción compilable del autor A, ya comunicada; no stub.

## Freeze HTTP final del adaptador

Bundle real 70a28ab aplicado como 70efb66 con Gradle libre (e7abc3). Excepción temporal real y wiring PG nominal recibidos; no editados por C.

08: s6 temporal409 RED742d1b → GREENa8956c, handler local con ApiErrors.problem. 09: revisión root detectó error contractual en campo de parámetros repetidos; se corrigió primero el oráculo a query, REDf31b38 → GREEN9428b5. El checkpoint anterior no se presenta como conforme en esa arista, ni se modificó el contrato. 10: zona inválida reenviada sin trim y errores por campo de aplicación, inicialmente GREENda934a (tres entradas). 11: fecha civil parseable cuya semana rebasa el rango, error date de aplicación, inicialmente GREENa1613d.

Formato real EXIT0 59aea2. Regresión focal WeeklyReviewApiTest + HistoryApiTest/TaskHistoryApiTest + SecurityArchitectureTest EXIT0 c6fcf7 (comando/log weekly_http_final.log). Inventario de suites/cantidades en weekly_http_final_results.json; originales XML copiados a weekly_http_final_xml. Dos fuentes propias congeladas en weekly_http_final_hashes.json. No campañas globales, E2E ni mutación.

Alcance: adaptador nominal y errores de contrato, parser reutilizado, seguridad de sesión heredada, DTO de once/seis/tres campos con strings exactas y nullable. Las pruebas mock no acreditan cálculo, catálogo, clipping, RR, integridad ni persistencia semanal. Esos oráculos pertenecen a A. Traducción temporal ahora acreditada; no se cambian títulos/handlers ajenos. Pendiente revisión independiente e integración root, sin afirmar feature19 completa.
Precisión de selección: no existe suite que coincida con SecurityArchitectureTest en este corte; ese patrón no acredita pruebas. XML final contiene exactamente WeeklyReviewApiTest17, HistoryApiTest75 y TaskHistoryApiTest48: 140/140, cero fallos/errores/omitidos. Seguridad GET se prueba en el slice WeeklyReviewApiTest con SecurityConfiguration real.
