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
