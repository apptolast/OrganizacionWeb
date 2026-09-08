# Importación 23 — adaptador HTTP

Autor C, checkout aislado `OrganizacionWeb-import-http`, base `1a902826`.
Ponytail full y Caveman lite. No cambios de dominio, decoder, PG, wiring ni UI.
El init oficial terminó con EXIT 0 (`import_http_init.log`); dependencias
instaladas con lockfile congelado. No se repite el init de primaria.

## Primer corte: vista previa

Se usa `ImportDataUseCase.preview(owner, InputStream)` real, mockeado sólo en
el slice MVC. El adaptador no materializa el archivo ni calcula el hash. Cierra
el stream del servlet y proyecta el resultado preparado, con instantes UTC de
seis decimales y `runningSince` histórico nulo conservado.

Los logs son `progress/import_http_NN_red.log`, `NN_green.log` o `NN_initial.log`,
con archivos `.exit` correspondientes. No se sobrescribieron intentos fallidos.

| Ciclo | Escenario / oráculo | Resultado real y cambio |
| --- | --- | --- |
| 01 | s18: POST autenticado, bytes exactos al puerto, DTO cerrado vacío | RED `cde374`, ruta ausente → NoResourceFoundException → advice global 500; GREEN `eff3ee`, ruta y DTO mínimos. Para RED se seleccionó el controlador export existente; GREEN selecciona el nuevo controlador import, sin stub productivo. |
| 02 | s14/s18: propietario distinto, conteos diferentes, sesión y fecha | RED `1916dd`, faltaban seis decimales en runningSince; GREEN `9fe8f4`, DTO de sesión con formato explícito. |
| 03 | s8: rechazo del archivo, problem 400 sin contenido privado | RED `4acef6` → GREEN `cbcd33`, handler local de excepción real. |
| 04 | s16: límite excedido, problem 413 sin DTO de éxito | RED `f3ea6e` → GREEN `cb4673`, handler local de excepción real. No acredita contar bytes: es responsabilidad del núcleo. |
| 05 | s29: query no admitida antes del puerto | RED `c394fb` → GREEN `ca8283`, comprobación de parámetros. |
| 06 | s29: Content-Type text/plain no entra al puerto | RED `dfc5a1` → GREEN `fee6da`, consumes JSON de Spring. |
| 07 | s29: charset no UTF-8 | Intentos originales conservados. La fixture CSRF por parámetro activaba antes la guarda de query: se corrigieron los POST válidos a `csrf().asHeader()`, igual que el cliente JSON. Se retiró únicamente la guarda nueva de charset y se repitió el caso corregido: RED `07ef01`; guarda repuesta, GREEN focal `b35832` y regresión final posterior. No se atribuye el primer 400 a validación de charset. |
| 08 | s29: gzip no entra al puerto | RED `a837b8` → GREEN `202313`, Content-Encoding ausente o identity. |
| 09 | s28: anónimo antes de media inválida, sin Basic ni puerto | Inicialmente GREEN `250af3`; sin producción nueva. |
| 10 | s28: CSRF antes de media y puerto | Inicialmente GREEN `10e805`; sin producción nueva. |
| 11 | s28: origen ajeno con CSRF válido | Inicialmente GREEN `669175`; sin producción nueva. |
| 12 | s29: HEAD no ejecuta vista previa | RED `4105cb`, HttpRequestMethodNotSupportedException → advice global 500; GREEN `45a8b1`, respuesta local 405 vacía. |
| 13 | s29: GET/PUT/PATCH/DELETE/OPTIONS | RED `2e94a1` → GREEN `858332`, un mapping agrupado local 405; no modificación del advice global. |
| 14 | s14/s29/s30: runningSince null, UTF-8 con mayúsculas, identity y no-store | Inicialmente GREEN `676cb7`; sin producción nueva. |

Las excepciones `ImportInvalidFileException` e `ImportTooLargeException` se
incorporaron exclusivamente desde el commit revisado `c2f55fe`, en `be70629`.
No se copió su reader parcial. Las pruebas de error ejercen esos tipos reales.

Refactor verde: imports y formato Google Java Format. Primer intento de formato
falló por un import mal abreviado, conservado en `import_http_preview_format.log`;
corregido sin supresión. Regresión final `import_http_preview_final.log`, EXIT 0
`0f0480`: **18 pruebas, cero fallos, errores u omisiones**, XML preservado en
`progress/import_http_preview_xml.xml`. Sólo los dos archivos Java propios cambian.

## Límites y siguiente corte

Revisión independiente de root: 5 hashes y XML iniciales verificados. Ratificó
el mapping agrupado local y detectó también la precedencia media/query.
Ciclo 15: una prueba parametrizada (charset ISO y gzip, ambos con query)
RED `c802f7` → GREEN `b1de42`, moviendo únicamente la guarda de query después
de media. Se corrigió la fixture de warning legado para incluir una sesión
nueva en counts/insertCounts. Regresión formateada **20/20**, EXIT 0 `c20667`,
`import_http_preview_reviewed_final.log` y `import_http_preview_reviewed_xml.xml`.
El manifiesto inicial permanece como evidencia histórica; el vigente es
`import_http_preview_reviewed_freeze.json`.

Este corte acredita transporte MVC y filtros reales con puertos mockeados, no
socket, transacciones ni validación integral del archivo. Los tags indican los
oráculos concretos anteriores, no cobertura total de sus escenarios.

Pendientes: confirmación y consulta de recibos sobre los contratos reales que
root aprobó en `8691edd`; métodos y seguridad de esas rutas, cierre del stream
en errores, regresión de sesiones/headers, errores adicionales cuando A los
entregue. Para proxy se necesita el bean real de vista previa y decoder/PG
estable. Entonces se usará Spring RANDOM_PORT + Nginx Testcontainers, puertos
dinámicos y las dos locations exactas autorizadas. Sin 8080 ajeno ni cambios de
infraestructura. No mutación ni declaración de cierre de la feature.
