# TDD create_project backend

Feature aprobada por el usuario; el coordinador pasó init y cambió a in_progress.
No se declara done desde este handoff.

## Ciclos reales

1. @s1 ProjectTest.s1_createsIdeaWithServerIdentityAndDates: ROJO compileTestJava (Project inexistente). Implementado record puro y factory de idea.

## Herramientas

Gradle 9.3.1 descargado de services.gradle.org, SHA256 contrastado con distribución oficial.
Spring Boot 3.5.11 confirmado disponible en Maven Central; Java25 del entorno.
POST /api/v1/projects; HTTP Basic bootstrap de entorno sin credenciales predeterminadas.
2–6. ROJO/VERDE observados individualmente: s3 recorte Unicode, s5 null/vacío, s6 121 puntos de código, s7 descripción null, s10 descripción 4001. Cada suite verde antes del siguiente test. s2/s8 y s4/s9 añadidos como regresión en verde: límites inclusivos y conservación ya implementados, sin cambios de producción.
7. @s1/@s16 CreateProjectTest: ROJO tipos inexistentes, VERDE 14 tests; puerto ProjectCommit atómico y evento sin descripción.
8. @s1/@s16 ProjectApiTest: ROJO @SpringBootConfiguration ausente, VERDE PostgreSQL17.9 Testcontainers real con HTTP201, Location, proyecto y outbox pendientes; Flyway V1 y adaptador transaccional.
9. @s5/@s21: ROJO ValidationException sin mapping, VERDE problema español estructurado y cero escrituras.
10. @s5/@s10: ROJO 8 tipos no string aceptados/coaccionados o sin código, VERDE lectura JsonNode estricta.
11. @s12: ROJO 3 propiedades desconocidas aceptadas, VERDE rechazo ownerId/status/extra.
12. @s14 ROJO JSON incompleto sin problem, VERDE MALFORMED_JSON400.
13. @s17 ROJO errores reales SQLSTATE08006 con triggers de PostgreSQL (cada tabla), VERDE503 y ambas tablas vacías; clasificación conserva excepción original al fallar rollback JDBC.
14. @s18 ROJO error SQL inesperado antes de insertar outbox, VERDE500 con correlación y cero estado parcial, sin detalles privados en respuesta.
15. @s15 ROJO contenido text/plain mapeado500 por fallback, VERDE415 específico.
16. @s13 ROJO401 sin problem JSON, VERDE401 español con WWW-Authenticate (sin charset añadido).
17. Bootstrap GET /api/session: ROJO ruta ausente, VERDE204 autenticado /401 sin credencial, sin escrituras.
18. DirectConstruction: ROJO constructor público evitaba validación, VERDE constructor canónico valida nombre/descripción e identidad/estado/fechas.
19. Origin: ROJO POST autenticado de origen ajeno aceptado, VERDE403 UNTRUSTED_ORIGIN. JSON-only y sin CORS; APP_PUBLIC_ORIGIN explícito para navegador.
20. Credenciales vacías: ROJO contraseña vacía y espacios aceptados, VERDE rechaza bootstrap vacío.
21. Precisión temporal: ROJO nanos devueltos aunque PG admite micros, VERDE truncado a micros antes de proyecto/evento.
22. ArchUnit: ROJO dependencia directa del controlador a implementación, VERDE puerto entrada CreateProjectUseCase; dominio y aplicación sin framework.
23. Regresiones añadidas sin producción: @s11 duplicados/IDs y original inmutable; @s5 ausente/null/Unicode; @s7 defaults; @s2/@s8 límites persistidos; @s6/@s10 límites rechazados sin escrituras; @s3/@s4/@s9 texto preservado. Cada ejecución verde antes del siguiente test.
24. Mutación: primera ejecución por defecto solo generó4 mutantes porque FRECORD suprimía constructor compacto escrito a mano. Se inspeccionó fuente oficial pitest-entry1.22.0 RecordFilterFactory/RecordFilter; se desactivó FRECORD y se excluyeron únicamente equals/hashCode/toString generados. Resultado33/36; tres FieldError getters sin cobertura de suite de mutación.
25. @s5 reforzado para observar field/code/message del error de negocio. Se reprodujo el mutante getter field vacío: ROJO3 casos fallaron. Se retiró el mutante y ejecutó suite completa: VERDE. PIT36/36 muertos, cero sobrevivientes/cobertura ausente.

## Trazabilidad final del backend

- @s1 → ProjectTest.s1_createsIdeaWithServerIdentityAndDates; CreateProjectTest.s1_s16_commitsProjectAndVersionedPrivateEventTogether; ProjectApiTest.s1_s16_commitsBeforeReturningCreated.
- @s2/@s8 → ProjectTest.s2_s8_preservesInclusiveCodepointLimits; ProjectApiTest.s2_s8_persistsInclusiveUnicodeLimits.
- @s3/@s4/@s9 → ProjectTest.s3_trimsOnlyUnicodeWhitespace y s4_s9_preservesUnicodeCaseAndDescriptionWhitespace; ProjectApiTest.s3_s4_s9_preservesTextInStorage.
- @s5 → ProjectTest.s5_rejectsMissingOrEmptyNames; ProjectApiTest.s5_missingNullAndUnicodeBlankAreRequired, s5_s10_rejectsNonStringTypes y s5_s21_validationUsesSpanishProblemDetails.
- @s6/@s10 → ProjectTest.s6_rejects121CodepointsAfterTrimming y s10_rejects4001DescriptionCodepoints; ProjectApiTest.s6_s10_rejectsOverLimitWithoutWrites y s5_s10_rejectsNonStringTypes.
- @s7 → ProjectTest.s7_normalizesNullDescription; ProjectApiTest.s7_optionalDescriptionNormalizesInResponseAndStorage.
- @s11 → ProjectApiTest.s11_duplicateNamesCreateDistinctUnchangedProjects.
- @s12 → ProjectApiTest.s12_rejectsUnknownFields.
- @s13 → ProjectApiTest.s13_requiresVerifiedIdentity.
- @s14 → ProjectApiTest.s14_malformedJsonProblem.
- @s15 → ProjectApiTest.s15_rejectsUnsupportedContentType.
- @s16 → ProjectApiTest.s1_s16_commitsBeforeReturningCreated y CreateProjectTest.s1_s16_commitsProjectAndVersionedPrivateEventTogether.
- @s17 → ProjectApiTest.s17_rollsBackEitherFailedWrite (triggers reales sobre ambas tablas); CreateProjectTest.s17_doesNotConfirmWhenAtomicCommitFails.
- @s18 → ProjectApiTest.s18_internalFailureRollsBackAndHidesDetails.
- @s19 → ProjectApiTest.s19_brokerAbsentDoesNotPreventPendingDurableEvent (sin broker/configuración/publicador).
- @s20 → E2E compartido e2e/create-project.spec.mjs: agente de integración confirma recarga y reinicio real del contenedor API con filas idénticas.
- @s21 → ProjectApiTest.s5_s21_validationUsesSpanishProblemDetails, s14_malformedJsonProblem, s17_rollsBackEitherFailedWrite, s18_internalFailureRollsBackAndHidesDetails.
- @s22–@s28 → frontend y E2E, fuera del ownership backend.
- Guardas adicionales → ArchitectureTest.hexagonalBoundariesAndInputPort; SecurityConfigurationTest.bootstrapRejectsBlankCredentials; ProjectApiTest.bootstrapSessionOnlyConfirmsVerifiedCredentials/rejectsCrossOriginBasicAuthenticatedWrites; ProjectTest.directConstructionCannotBypassNameInvariant/directConstructionRequiresServerMetadata; CreateProjectTest.confirmsOnlyTimestampPrecisionSupportedByStorage.

## Evidencia final propia (2026-09-05)

`gradlew.bat spotlessApply test pitest bootJar spotlessCheck --console=plain`: exit0.
64 tests: dominio15, aplicación3, HTTP+PG41, configuración4, arquitectura1. Cero fallos/omitidos.
PIT1.22.0:36/36 muertos (100%), cero supervivientes ni NO_COVERAGE; dominio/aplicación exclusivamente, constructores de record incluidos. Adaptadores cubiertos por HTTP+PostgreSQL/E2E, no por PIT.
JAR construido y formato Java verificado. Dockerfile disponible contexto backend con Java25 sin root; integración probó build/ejecución del stack.
Comandos y configuración en backend/README.md. No se hizo commit/push desde este agente. No se marca feature done: revisión y coordinación final corresponden al coordinador.

## Corrección tras judge: documento JSON completo (@s14)

26. ProjectApiTest.s14_rejectsTrailingJsonDocumentWithoutWrites envía dos objetos concatenados. ROJO real: esperaba400, recibió201 (Jackson ignoraba segundo documento). Configurado spring.jackson.deserialization.fail-on-trailing-tokens=true. VERDE400 MALFORMED_JSON y cero filas; suite completa65 tests (HTTP42), formato/JAR verdes y PIT36/36 (100%) sin supervivientes. Comando: gradlew.bat spotlessApply test pitest spotlessCheck bootJar --console=plain, exit0. Revisión final independiente pendiente del coordinador.
