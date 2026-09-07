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
