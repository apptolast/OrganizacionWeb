# TDD HTTP — vistas y campos personales

Árbol aislado `OrganizacionWeb-customization-http`, base contractual `c91bb8b`.
Ownership: adaptadores HTTP y sus tests; aplicación, dominio, persistencia y wiring pertenecen a A.
La base coincide con init21 verde comunicado por root; se verifica compilación y ejecución focal en este árbol sin repetir frontend.

## Ciclo 1 — @s1, defaults PROJECT

Test: `CustomizationApiTest.s1_projectDefaultsHaveAnUnconfiguredTagWithoutCsrf`.
Comando desde backend: `./gradlew.bat test --tests com.apptolast.organization.adapter.CustomizationApiTest --console=plain`.

RED real: sesión 91767, salida recuperada `cba049`, EXIT 1. Compilación completa correcta; un test ejecutado y fallido por `Status expected:<200> but was:<404>`.
Log: `progress/customization_http_first_red.log`; XML en `backend/build/test-results/test/TEST-com.apptolast.organization.adapter.CustomizationApiTest.xml`.
El oráculo exige cuerpo exacto de defaults, ETag fuerte propio y no-store, autenticado sin CSRF para GET.

La selección MVC está temporalmente sin controladores porque la ruta aún no existe. Se seleccionará el controlador real al implementarlo, conservando este oráculo. No hay modelos ni puertos ficticios.
A confirmó `ReadCustomizationUseCase.get(String owner, CustomizationScope scope) -> Optional<Customization>`, con enum PROJECT/TASK. Se espera el bundle compilable con record completo antes del GREEN del adaptador. Este test no acredita ausencia de escrituras PostgreSQL.

GREEN ciclo 1: bundle real `72ff667` integrado por root; controlador con puerto real y slice seleccionado explícitamente. Sesión 8316, EXIT 0 `33c8a7`, un test verde. Log `customization_http_s1_green.log`. Implementación mínima inicial, aún sin proyectar filas configuradas.

## Ciclo 2 — @s1 TASK

`s1_taskDefaultsUseTheAuthenticatedOwnerAndTaskScope` usa otro principal y exige defaults TASK exactos. RED sesión 5257 EXIT 1 `6c5982` por ruta ausente; GREEN sesión 30362 EXIT 0 `85f915`, dos tests verdes. Logs `customization_http_task_red.log` y `customization_http_task_green.log`. Ruta compartida con selección del ámbito real.

## Ciclo 3 — representación configurada

`s2_configuredResponseKeepsInactiveDefinitionsAndHidesInternalIdentity`: RED 32188 EXIT 1 `4cea01` por ETag unconfigured; GREEN 11133 EXIT 0 `40b784`, 3/3. Comprueba cuerpo cerrado, orden visible, definición inactiva, fecha µs y versión BIGINT máxima exacta en cabecera, sin owner/id/version del agregado en el cuerpo. Logs `customization_http_configured_red.log` y `customization_http_configured_green.log`.

## Ciclo 4 — @s23 query

`s23_queryIsRejectedBeforeReadingPrivateConfiguration`: RED 17915 EXIT 1 `76f0c2` (aceptaba query con 200). El oráculo exige problema 400 query INVALID_VALUE y ninguna llamada al puerto. Se añade validación previa a lectura; resultado GREEN se registra al recuperar salida.

GREEN ciclo 4: 47120 EXIT 0 `cca22b`, 4/4 (`customization_http_query_green.log`).

## Ciclo 5 — @s23 scope exacto

`s23_scopeIsCaseSensitiveAndHasItsOwnValidationProblem`: RED 28188 EXIT 1 `92eab0`, la conversión automática rechazaba el enum sin el problema contractual. Se cambia a análisis explícito que conserva `scope INVALID_VALUE`; GREEN 9325 EXIT 0 `c5666c`, 5/5. Logs `customization_http_scope_red.log` y `customization_http_scope_green.log`.

## Ciclo 6 — @s23 seguridad reutilizada

`s23_authenticationPrecedesQueryValidation`: inicialmente GREEN, sesión 9035 EXIT 0 `f532e9`. Una petición anónima con query inválida obtiene 401 problem+json/no-store sin invocar lectura. No modificación productiva para fabricar RED. Log `customization_http_auth_initial.log`.

## Ciclo 7 — @s20 almacenamiento reutilizado

`s20_storageFailureDoesNotPublishDefaultsOrPrivateDetails`: primer intento 67925 EXIT 1 `985242` falló compilación por mi referencia al paquete domain en lugar de application; no es un RED de comportamiento. Corregido sólo el test, ejecución 95893 EXIT 0 `010825`: inicialmente GREEN por ApiErrors heredado, 503 sin defaults/tag ni detalle interno. Logs inicial y corregido preservados.

## Checkpoint GET congelado

Siete casos, XML 7/7 sin fallos ni skips. Regresión inicial `aa943f` EXIT 0; ese comando NO acreditó formato porque spotlessIdeHook rechazó rutas relativas. Los dos hooks absolutos posteriores sólo informaron IS DIRTY, sin aplicar. Se usó un init script temporal externo que limita el target de Spotless exclusivamente a los dos Java propios: formato real y regresión 7/7 EXIT 0 `b2f6f6`, log `customization_http_get_formatted.log`. No se cambiaron configuración versionada, clases de A ni fuentes históricas.

Este corte acredita GET de configuración/DTO/validación query y scope/seguridad/503 mediante mocks del puerto real. No acredita PG, escrituras ni lectura de valores. Los tipos de A siguen intactos. Para el futuro parser de valores: lector JSON local BigDecimal exacto, preservar List/índices; errors values[i].fieldId y values[i].value, conjunto values INVALID_VALUE. No cambiar el mapper transversal.

## Ciclo 8 — @s41 primera PUT de defaults (abierto)

`s41_firstPutOfProjectDefaultsCreatesConfiguredRepresentation`: RED real `c950ae` EXIT 1, una prueba ejecutada. Petición autorizada con CSRF y If-Match unconfigured, body visibleFields [createdAt]; el oráculo exige 200/configured true/versión 0/fecha/ETag/no-store. Ruta PUT todavía ausente: respuesta real 500 frente a 200 (no el 405 esperado inicialmente), comprobada en XML `a72901`. Log `customization_http_put_first_red.log` preservado.

Se espera bundle real SaveCustomizationViewUseCase/CustomizationRevision de A para GREEN y verificación exacta de la delegación. No se añade implementación provisional ni modelo duplicado. El checkpoint GET anterior permanece versionado en 61fd4b0; su manifiesto describe ese corte, no este nuevo test RED.

GREEN ciclo 8 tras bundle real `4d0a565`: `b7fa28` EXIT 0, 8/8. Se añade mock del puerto real y verificación exacta owner/PROJECT/revisión ausente/lista; respuesta delegada, sin escribir PG. Implementación todavía mínima, las guardas se completan por ciclos siguientes.

## Ciclos de PUT vista en curso

| Caso individual | RED real | GREEN / regresión | Logs (prefijo customization_http_) |
| --- | --- | --- | --- |
| @s2 TASK con revisión 9007199254740993 y orden exacto | 8a9cec EXIT1 | 060261 EXIT0, 9/9 | put_revision_red/green.log |
| @s24 falta If-Match antes de JSON malformado | d3a073 EXIT1 | 072a9e EXIT0, 10/10 | header_missing_red/green.log |
| @s24 tag de TASK contra PROJECT antes del body | 4e8051 EXIT1 | 4aa3bb EXIT0, 11/11 | header_scope_red/green.log |
| @s24 versión 01 no canónica antes del body | 09e829 EXIT1 | 2e59c5 EXIT0, 12/12 | header_canonical_red/green.log |

Los ciclos incorporan lectura real de If-Match, familia/ámbito y gramática canónica. Aún falta completar límites/repetición de cabecera y shape/precedencia del PUT; no es un freeze completo de escritura.

| Caso individual posterior | RED real | GREEN | Logs (prefijo customization_http_) |
| --- | --- | --- | --- |
| versión superior a BIGINT | 9b30bb EXIT1 | e3cd6c EXIT0, 13/13 | header_overflow_red/green.log |
| If-Match repetido idéntico | cc2fb3 EXIT1 | 912ada EXIT0, 14/14 | header_repeated_red/green.log |
| query de PUT antes de cabecera ausente | fa5566 EXIT1 | salida posterior registrada | put_query_red/green.log |

| Caso individual posterior | RED real | GREEN | Logs (prefijo customization_http_) |
| --- | --- | --- | --- |
| query PUT (salida recuperada) | fa5566 | 6ec19a EXIT0, 15/15 | put_query_red/green.log |
| scope PUT antes de cabecera | c0dad7 EXIT1 | 769638 EXIT0, 16/16 | put_scope_red/green.log |
| JSON duplicado | 3a9108 EXIT1 | c49be2 EXIT0, 17/17 | json_duplicate_red/green.log |
| JSON concatenado | inicialmente GREEN | c0c932 EXIT0 focal | json_trailing_red.log (nombre conservado; no fue RED) |
| extras léxicos antes de requerido | ae8ed1 EXIT1 | 4f38b0 EXIT0, 19/19 | json_extra_red/green.log |
| visibleFields ausente | b09722 EXIT1 | salida recuperada a continuación | json_required_red/green.log |

| Caso individual posterior | RED real | GREEN | Logs (prefijo customization_http_) |
| --- | --- | --- | --- |
| visibleFields ausente, salida recuperada | b09722 | 70c83e EXIT0, 20/20 | json_required_red/green.log |
| visibleFields null | 80d0af EXIT1 | f573e4 EXIT0, 21/21 | json_null_red/green.log |
| raíz array | db9f45 EXIT1 | 71c972 EXIT0, 22/22 | json_root_red/green.log |

| Caso individual posterior | RED real | GREEN | Logs (prefijo customization_http_) |
| --- | --- | --- | --- |
| visibleFields no array | 9e8fff EXIT1 | 629380 EXIT0, 23/23 | json_array_red/green.log |
| elemento no texto por índice | 211cfb EXIT1 | 5c2711 EXIT0, 24/24 | json_item_red/green.log |
| elemento null requerido | 385942 EXIT1 | 7765fd EXIT0, 25/25 | json_item_null_red/green.log |
| cabecera ausente antes de body ausente | 65abeb EXIT1 | b5e26b EXIT0, 26/26 | absent_precedence_red/green.log |
| body ausente con cabecera válida | 157b9d EXIT1 | resultado posterior | absent_body_red/green.log |

| Caso individual posterior | RED real | GREEN | Logs (prefijo customization_http_) |
| --- | --- | --- | --- |
| body ausente con cabecera válida, salida recuperada | 157b9d | 566ead EXIT0, 27/27 | absent_body_red/green.log |
| duplicados de visibleFields antes de delegación | d90892 EXIT1 | 043fed EXIT0, 28/28 | view_duplicate_red/green.log |
| conflicto real del puerto | fe50ab EXIT1 | 198acb EXIT0, 29/29 | conflict_red/green.log |
| body blanco | 1461fe EXIT1 | f4f7ec EXIT0, 30/30 | blank_red/green.log |

Bundle `eb7d2b4` aporta CustomizationView/CustomFieldLabel; `9db2289` aporta puertos Create/Update, todos de A e intactos. El PUT usa CustomizationView para reglas de scope/lista/duplicados, no réplica local. La incorporación de eb7d2b4 coincidió con recuperación del resultado 566ead; el siguiente foco d90892 y todas regresiones posteriores recompilaron ese corte real. No se atribuye identidad completa de inputs al run anterior.

Refactor en GREEN: GET y PUT comparten serialización de configuración y análisis de scope. Formato limitado a los dos Java propios con init temporal externo; regresión final del checkpoint en customization_http_view_checkpoint.log. Este paquete contiene GET+PUT vista, 30 casos MVC. No implementa aún POST/PUT de definiciones ni GET/PUT valores; sin PG/beansSave, carreras, no-op persistente ni durabilidad acreditados por estos mocks. El primer manifiesto GET conserva sus hashes históricos.

## Definiciones — siguiente paquete (abierto)

GET+PUT vista versionado por root en `692c386`; los manifiestos previos conservan su corte. Se reutilizan puertos reales Create/Update de `9db2289`, sin beans ni persistencia supuestos. Se extraen análisis de cabecera/cuerpo y respuestas de errores a helpers locales porque POST y PUT los comparten.

| Caso individual | RED | GREEN focal | Logs prefijo customization_http_ |
| --- | --- | --- | --- |
| @s3 alta TEXT/UUID servidor/label canónica | d3adda EXIT1 | c8c0f4 EXIT0 | create_red/green.log |
| @s25 label requerida antes de type | b50af2 EXIT1 | d13eb9 EXIT0 | label_required_red/green.log |
| @s25 label numérica antes de type | 3663f9 EXIT1 | 8dd5d5 EXIT0 | label_type_red/green.log |
| @s25 type requerido después de label válido | 8680f4 EXIT1 | a449a4 EXIT0 | field_type_required_red/green.log |
| @s25 type desconocido | 6152de EXIT1 | bf93f4 EXIT0 | field_type_enum_red/green.log |
| @s4 label sólo Unicode White_Space antes de type ausente | inicialmente GREEN | 011637 EXIT0 | label_domain_initial.log |

Los focos posteriores a 692c386 ejecutan sólo el test nuevo; regresión de paquete al cerrar. El oráculo del alta verifica delegación exacta owner/scope/revisión/label normalizada/type y respuesta completa sin Location, no acredita generación de UUID ni transacción PG dentro del mock.

| Caso individual de actualización | RED | GREEN focal | Logs prefijo customization_http_ |
| --- | --- | --- | --- |
| @s7 desactivar definición UUID con letras mayúsculas de ruta | b78ccb EXIT1 | 2dc6ef EXIT0 | update_red/green.log |
| @s23 UUID abreviado antes de cabecera requerida | eab197 EXIT1 | bbfee2 EXIT0 | update_uuid_red/green.log |
| @s25 active requerido | 4f9287 EXIT1 | cb72f7 EXIT0 | active_required_red/green.log |
| @s25 active string sin coerción | 09c44b EXIT1 | 8bd5ce EXIT0 | active_type_red/green.log |
| @s17 ResourceNotFoundException privada | inicialmente GREEN | 84c6c2 EXIT0 | update_notfound_initial.log |

El 404 usa ApiErrors existente y no publica etiqueta/definiciones/tag; propiedad real y precedencia respecto a conflicto pertenecen a A. El constructor ahora requiere Create y Update reales, además de Read/Save; sólo mocks de interfaces auténticas en MVC, ningún bean productivo provisional.

Regresión intermedia de definiciones: 41/41, formato real y EXIT0 `214cbb`. Después se añadió `s25_updateCannotChangeTheDefinitionType`, inicialmente GREEN `15cc3e`, sin producción: rechaza type UNKNOWN_FIELD antes de delegar.

### Corrección acotada de precedencia del PUT ya versionado

La extracción de todos los tipos antes de validar la lista permitía que [estimatedMinutes,7] en PROJECT informase el segundo elemento antes del enum inválido del primero. Root ratificó que @s25 exige orden por índice y el error de enum mantiene campo visibleFields. Nuevo test `s25_firstInvalidVisibleFieldPrecedesLaterTypeFailure`: RED `5ecd4d` EXIT1, GREEN `2e26d8` EXIT0. Logs `customization_http_view_index_order_red.log` y `customization_http_view_index_order_green.log`. Reutiliza CustomizationView sobre cada prefijo; hay como máximo cuatro elementos válidos en TASK antes de rechazar otro, sin duplicar reglas ni cambiar dominio. Es un delta posterior a 692c386; los resultados originales quedan intactos.

## Freeze de definiciones

Regresión final: `41ce14` EXIT0, 43/43 sin fallos/errores/skips y formato focal real. Log `customization_http_definitions_final.log`; XML `backend/build/test-results/test/TEST-com.apptolast.organization.adapter.CustomizationApiTest.xml`.

Incluye GET, PUT vista, POST definición y PUT definición con shared parser de cabecera/body, label puro y errores compartidos. Se preservan orden de validación, revisión exacta, campos cerrados y ausencia de datos ajenos en404. Los tests de alta/actualización utilizan mocks de puertos reales: PG, límites12/labelúnico bajo lock, concurrencia, rollback y durabilidad siguen en evidencia de A. No GET/PUT de valores implementado en este corte. Constructor requiere cuatro puertos, ningún bean añadido por C.

## Seguridad y negociación de configuración (paquete abierto)

Reparación E2E separada versionada por root `7e4ccaa`; no se ejecutó runner. Nuevos oráculos sólo en CustomizationApiTest, con focos separados por comportamiento:

| Comportamiento en PUT vista / POST campo / PUT campo | Resultado inicial | Log |
| --- | --- | --- |
| anónimo con CSRF, query/body inválidos:401 y ninguna llamada | 3/3 GREEN224c5a EXIT0 | customization_http_security_anonymous_initial.log |
| autenticado sin CSRF:403 antes de input | 3/3 GREEN85e944 EXIT0 | customization_http_security_csrf_initial.log |
| origen ajeno aun con CSRF:403 UNTRUSTED_ORIGIN | 3/3 GREEN7c45b2 EXIT0 | customization_http_security_origin_initial.log |
| Content-Type text/plain:415 previo a delegación | 3/3 GREEN3a961f EXIT0 | customization_http_security_content_initial.log |

No producción modificada para estos doce ejemplos. GET sin CSRF y autenticación antes de query ya estaban cubiertos por el checkpoint43.

### Hallazgo Accept anterior al comando

Nuevo `s23_unacceptableViewResponseNeverExecutesTheWrite`: RED real15cd96 EXIT1. Con body válido y Accept application/xml, verifyNoInteractions detecta llamada SaveCustomizationViewUseCase antes del fallo de representación; XML comprobado3b384b. No es suficiente mapear el error de serialización a406 después del comando.

Lectura local del bytecode Spring6.2.16 efectivo (2575ab): ProducesRequestCondition compara compatibilidad y parámetros, pero no excluye calidad0 por sí solo. Propuesta enviada a root: negociación mediante HeaderContentNegotiationStrategy/MediaType de Spring, selección por especificidad sin q y calidad efectiva, guarda local21 antes de delegación. Ningún parser manual ni cambio del manejador global. Implementación pendiente de revisión de esa composición; mantener original RED.

### Corrección local Accept ratificada por root

Guardia en los cuatro métodos de CustomizationController antes de query y puertos; los filtros de sesión/CSRF/origen permanecen anteriores. Usa HeaderContentNegotiationStrategy para parsear y ordenar Accept y MediaType de Spring para compatibilidad/especificidad. Q se elimina sólo al comparar especificidad; se conserva la calidad del rango elegido. En empates de especificidad se conserva estable el orden que Spring resolvió, incluido su orden de calidad. Sin parser manual ni cambios ApiErrors/filtros compartidos. HttpMediaTypeNotAcceptableException de la estrategia se traduce localmente a406 NOT_ACCEPTABLE; no se capturan excepciones de negocio en esa guarda.

| Oráculo posterior | Resultado | Log |
| --- | --- | --- |
| PUT con Accept XML no invoca Save | GREEN b60b4c EXIT0 tras RED15cd96 | customization_http_accept_green.log |
| q0 JSON frente wildcard; JSON positivo específico; wildcard positivo; q0 application/* frente */* | 4/4 inicialmente GREEN cbc196 EXIT0 | customization_http_accept_quality_initial.log |
| GET/POST/PUTfield Accept XML antes de query | 3/3 inicialmente GREEN38534e EXIT0 | customization_http_accept_routes_initial.log |
| Accept malformado:401 anónimo/406 autenticado | 2/2 inicialmente GREEN f8adfc EXIT0 | customization_http_accept_malformed_initial.log |
| GET sin CSRF, Content-Type text/plain y origen ajeno:200 sin habilitar CORS | inicialmente GREEN2b03e7 EXIT0 | customization_http_get_negotiation_initial.log |
| Tres nominales de escritura reforzados con origen confiable, Accept JSON y Content-Type JSON UTF-8 | 3/3 inicialmente GREENbd5501 EXIT0 | customization_http_trusted_json_initial.log |

La ausencia de Access-Control-Allow-Origin no acredita lectura desde un navegador de otro origen. La guarda sólo aplica a rutas21 de este controlador; contratos1–20 no se modificaron. Bundle de lectura de valores b72e6ad incorporado por root durante la ventana sin Gradle; todavía no se usa en estos tests.

Freeze seguridad/negociación: regresión del paquete 66/66 sin fallos/errores/skips, formato focal real, EXIT0 `507b84`. Log `customization_http_security_final.log`. Manifest `customization_http_security_freeze.json`; originales43 y REDAccept conservados. No PG, SQL, E2E ni campañas ejecutados. Pendiente revisión root e integración antes de cobertura real de valores.

### Lectura HTTP de valores con puertos reales

Tras commit 0747869, se usa ReadCustomFieldValuesUseCase del bundle b72e6ad. Las dos rutas se incorporan al mismo controlador para reutilizar negociación, validación de query y errores locales. El parser UUID se extrae y reutiliza también para fieldId; no cambia la gramática anterior. No se modifica wiring ni se inventa un bean. El nuevo argumento del constructor requiere el bean real de A antes de integrar en Spring completo.

| Ciclo individual | Evidencia | Log en progress |
| --- | --- | --- |
| PROJECT ausente: cuerpo cerrado, ETag compuesto y no-store | RED c982b8 EXIT1 por ruta ausente; GREEN 30aac5 EXIT0 | customization_values_get_red.log / customization_values_get_green.log |
| TASK: UUID de entrada con letras mayúsculas, cuatro tipos, espacios, fecha y dos revisiones long exactas | RED 36071b EXIT1 por ruta ausente; GREEN b89128 EXIT0 | customization_values_task_red.log / customization_values_task_green.log |
| Definición activa null sin fila de valores no implica configured | Inicialmente GREEN 015f3c EXIT0 | customization_values_null_initial.log |
| Query antes de UUID y projectId antes de taskId; UUID abreviado rechazado | 5/5 inicialmente GREEN 802ab0 EXIT0 | customization_values_path_initial.log |
| Ambas rutas: auth antes de Accept y q0 específico antes de query | 4/4 inicialmente GREEN 771e6c EXIT0 | customization_values_security_initial.log |
| Ambas rutas: delegación de propiedad404 y almacenamiento503, sin DTO privado ni ETag | Error de compilación del fixture 7dfc79 (faltaba Throwable del constructor real); corregido el fixture, 4/4 inicialmente GREEN bb4f80 EXIT0. No se atribuye RED funcional | customization_values_errors_initial.log / customization_values_errors_green.log |
| Cero definiciones activas con fila de valores conservada | Inicialmente GREEN c2c463 EXIT0 | customization_values_empty_initial.log |

El DTO público contiene exactamente configured, values y updatedAt. Los IDs/revisiones internos de esquema y fila sólo componen un ETag fuerte; las entradas públicas mantienen fieldId, label, type y value. La lista se proyecta en el orden del puerto, sin consultas por campo ni Clock. Estos slices verifican delegación owner/contexto y traducción de errores; no acreditan joins, filtrado de inactivos, integridad ni snapshot PostgreSQL. PUT valores permanece pendiente del puerto real.

Freeze: formato focal de los dos Java y regresión de CustomizationApiTest, 83/83 sin fallos, errores ni skips; EXIT0 43a02a. Log customization_values_get_final.log y XML backend/build/test-results/test/TEST-com.apptolast.organization.adapter.CustomizationApiTest.xml. Manifest customization_values_get_freeze.json. No suite global, E2E, SQL, mutación ni cambios en modelos/configuración.
