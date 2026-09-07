# TDD HTTP de apariencia

Contrato aprobado: features/appearance.feature, 37 escenarios numerados;
gate root f4697a6 / in_progress 0202129. C posee sólo el nuevo adaptador
HTTP y sus pruebas. A posee dominio, casos de uso, PostgreSQL y wiring;
B posee cliente/presentación. Ponytail full y Caveman lite.

## Preparación

Leído AvailabilityController como patrón de recurso privado con ETag fuerte,
Principal, query cerrada y PUT condicionado. Reutilizar ApiErrors para
ValidationException y StorageUnavailableException, sin alterar seguridad.
El DTO HTTP contendrá sólo los cinco campos de apariencia; UUID/versión
permanecen en el ETag, sin serializar propietario ni dominio directamente.

Secuencia: primer GET nominal con puerto real compilable de A; luego PUT,
query/If-Match/JSON estricto y errores, un caso por ciclo. La validación de
colores/contraste pertenece al valor de dominio real que A entregue; HTTP
comprueba forma/orden de campos y adapta la salida. No crear interfaces
duplicadas ni stubs productivos para adelantar el test.

Árbol aislado OrganizacionWeb-appearance-http desde 0202129. Instalación de
dependencias con locks intactos EXIT0 6d453a. Init31856 EXIT0 08c192, log
appearance_http_init.log; perfil JVM768MiB separado del árbol de A.
En ese corte inicial todavía no había código HTTP ni bundle de A. Los ciclos
posteriores y el freeze final se detallan debajo. No campañas ni cambios
propios en wiring.

## Ciclos HTTP

Bundle real f7e7b22, core 8df9ac8 y validadores por campo 7daa63e incorporados
por root en fronteras sin Gradle activo. AppearanceRevision(null,0) representa
unconfigured conforme al puerto real. UUID y versión configurados se mantienen.

- @s1 GET ausencia: RED bee8cf por controlador inexistente, GREEN21245b.
- @s2 GET configurado, propietario distinto y BIGINT máximo: REDac1de5,
  GREEN33becb. Exactamente cinco campos y timestamp con microsegundos.
- @s2 PUT nominal: RED41f791 (ruta sin PUT), GREEN5c4744. Principal y
  revisión unconfigured delegados; confirmación de caso de uso adaptada.
- @s13 query antes de cuerpo/precondición: REDa7e6ff. Primer pase969a1e
  reveló fixture csrf como parámetro; el cliente real y AvailabilityApiTest
  usan cabecera. Ajustado a csrf().asHeader(), GREENa8d7ea, 5 casos.

Logs individuales appearance_http_*.log preservados en progress local.
Estos slices usan mocks de puertos reales y seguridad real; no prueban
concurrencia, persistencia ni validación interna del store.

- @s12 ausencia: RED 6774d3, GREEN 1cb816. @s3 tag configurado y
  versión máxima: RED 0eb10f, GREEN e5da49.
- @s12 sintaxis de tag: RED 50b22f; 9abdce dejó un fallo por recorte de
  espacios de Spring List<String>. HttpHeaders conserva el valor original;
  GREEN bf1ba0, 20 casos. No se modifica seguridad ni semántica de dominio.
- @s14 JSON estricto: RED 7a5ec8, GREEN 3ff05f, 27 casos. Reutilizados
  FAIL_ON_READING_DUP_TREE_KEY y FAIL_ON_TRAILING_TOKENS; cuerpo vacío es 400.
- @s10 raíz y extras lexicográficos: RED 68a206, cinco errores 500 observados;
  GREEN del foco completo documentado en appearance_http_shape_green.log.
- @s10 raíz/extras GREEN 3753b1, 32 casos completos.
- @s10/@s11 campos: RED 137dd1, 14 fallos 500; GREEN 388589, 46 casos
  completos. Cada campo pasa presencia/tipo y su validador real antes de leer
  el siguiente. Dos mezclas inválidas acreditan la precedencia semántica.
  No se duplican luminancia, superficies ni contraste dentro del adaptador.

- @s6 conflicto: RED 8a8cbd (500 en vez de 412); GREEN 663931, 47 casos.
  El handler local no modifica ApiErrors ni la política de otros recursos.
- @s7/@s16 errores de lectura y escritura: inicialmente GREEN f39d29,
  reutilización real de ApiErrors. No defaults, ETag ni detalle privado.
- @s15 seguridad y negociación: inicialmente GREEN 55cd96, cinco casos.
  Query inválida no adelanta autenticación, CSRF, origen ni tipo de contenido.
- @s12 cabecera repetida idéntica: inicialmente GREEN 03a79c; no se colapsa
  una lista de dos valores en una precondición única.

## Freeze HTTP para revisión

Root incorporó el bundle final A 1560338 como 4567520 con Gradle detenido.
No se repitieron sus 70 tests. Sólo se añaden AppearanceController.java y
AppearanceApiTest.java; no cambian fuentes versionadas de A ni seguridad.

Refactor de imports en GREEN. El primer intento Spotless con glob relativo
fue SKIPPED: exige ruta absoluta. No se atribuye formato a ese intento.
Aplicaciones por archivo absoluto: 4b946e y 042653, ambas EXIT0. Regresión
posterior `spotlessJavaCheck test --tests '*AppearanceApiTest' --tests
'*AvailabilityApiTest' --no-daemon -Dorg.gradle.jvmargs=-Xmx768m`:
EXIT0 0a6e89, 27 segundos, 206 tests (55 Appearance + 151 Availability),
cero fallos, errores y omisiones. Log appearance_http_freeze.log y XML
copiados a progress/appearance_http_final_xml; manifiesto propio
progress/appearance_http_freeze.json. Fuentes quietas desde ese resultado.

## Alcance y límites

Los métodos s1/s2/s3 acreditan DTO exacto, principal y representación de
revisión; s6 adapta el conflicto del puerto. s10/s11 acreditan forma y
orden de validación, incluyendo validadores reales de ambos temas; la matriz
matemática completa pertenece a AppearanceValuesTest de A. s12/s13/s14
cubren precondiciones, query y parser estricto; s15 conserva seguridad.
s7/s16 acreditan adaptación segura de errores de almacenamiento.

@s4–9 y @s17/@s35 dependen de core/PG para concurrencia, no-op, rollback,
durabilidad y reloj. No se presentan mocks HTTP como prueba de esos hechos.
No hay aquí reinicio API, navegador, mutación ni estado final de feature.
La propuesta PIT está separada en appearance_mutation_scope_proposal.md,
sin configuración ni campaña ejecutada.
