# create_project — TDD y trazabilidad consolidada

Feature única aprobada por el usuario el5 de septiembre de2026. La implementación se dividió por frontera técnica dentro del mismo contrato.

## Evidencia de ciclos

- [Backend](tdd_create_project_backend.md): dominio, aplicación, HTTP, identidad y PostgreSQL real; rojos, verdes y regresiones diferenciados.
- [Frontend](tdd_create_project_frontend.md): POST, estado, recuperación, texto seguro y accesibilidad; rojos reales y refactor.
- [Integración](tdd_create_project_integration.md): Docker/nginx/PostgreSQL/Chromium; fallos de autenticación, contraste y foco detectados y corregidos.

## Mapa del contrato

Rutas base: backend/src/test/java/com/apptolast/organization; frontend/src; e2e/create-project.spec.mjs. Los métodos citados se localizan por nombre, evitando números de línea que cambian con formato.

| Escenario | Test concreto |
| --- | --- |
| @s1 | adapter/ProjectApiTest.s1_s16_commitsBeforeReturningCreated; application/CreateProjectTest.s1_s16_commitsProjectAndVersionedPrivateEventTogether; E2E real browser saves an idea through the same-origin API |
| @s2 | adapter/ProjectApiTest.s2_s8_persistsInclusiveUnicodeLimits; domain/ProjectTest.s2_s8_preservesInclusiveCodepointLimits |
| @s3 | adapter/ProjectApiTest.s3_s4_s9_preservesTextInStorage; domain/ProjectTest.s3_trimsOnlyUnicodeWhitespace |
| @s4 | adapter/ProjectApiTest.s3_s4_s9_preservesTextInStorage; domain/ProjectTest.s4_s9_preservesUnicodeCaseAndDescriptionWhitespace |
| @s5 | adapter/ProjectApiTest.s5_s10_rejectsNonStringTypes, s5_missingNullAndUnicodeBlankAreRequired, s5_s21_validationUsesSpanishProblemDetails; domain/ProjectTest.s5_rejectsMissingOrEmptyNames |
| @s6 | adapter/ProjectApiTest.s6_s10_rejectsOverLimitWithoutWrites; domain/ProjectTest.s6_rejects121CodepointsAfterTrimming |
| @s7 | adapter/ProjectApiTest.s7_optionalDescriptionNormalizesInResponseAndStorage |
| @s8 | adapter/ProjectApiTest.s2_s8_persistsInclusiveUnicodeLimits |
| @s9 | adapter/ProjectApiTest.s3_s4_s9_preservesTextInStorage |
| @s10 | adapter/ProjectApiTest.s5_s10_rejectsNonStringTypes, s6_s10_rejectsOverLimitWithoutWrites |
| @s11 | adapter/ProjectApiTest.s11_duplicateNamesCreateDistinctUnchangedProjects |
| @s12 | adapter/ProjectApiTest.s12_rejectsUnknownFields |
| @s13 | adapter/ProjectApiTest.s13_requiresVerifiedIdentity |
| @s14 | adapter/ProjectApiTest.s14_malformedJsonProblem, s14_rejectsTrailingJsonDocumentWithoutWrites |
| @s15 | adapter/ProjectApiTest.s15_rejectsUnsupportedContentType |
| @s16 | adapter/ProjectApiTest.s1_s16_commitsBeforeReturningCreated; application/CreateProjectTest.s1_s16_commitsProjectAndVersionedPrivateEventTogether |
| @s17 | adapter/ProjectApiTest.s17_rollsBackEitherFailedWrite; application/CreateProjectTest.s17_doesNotConfirmWhenAtomicCommitFails |
| @s18 | adapter/ProjectApiTest.s18_internalFailureRollsBackAndHidesDetails |
| @s19 | adapter/ProjectApiTest.s19_brokerAbsentDoesNotPreventPendingDurableEvent; E2E project and outbox survive reload and backend restart |
| @s20 | E2E project and outbox survive reload and backend restart: ambas acciones y mismas filas de proyecto/evento |
| @s21 | adapter/ProjectApiTest.s5_s21_validationUsesSpanishProblemDetails, s14_malformedJsonProblem, s17_rollsBackEitherFailedWrite, s18_internalFailureRollsBackAndHidesDetails; App.test.tsx @s21 muestra fallo confirmado del servicio |
| @s22 | App.test.tsx @s22 confirma solo tras HTTP201 usando los datos del servidor; E2E real browser saves an idea through the same-origin API |
| @s23 | App.test.tsx @s23 bloquea clics y eventos de envío repetidos mientras guarda |
| @s24 | App.test.tsx @s24 conserva exactamente valores, asocia el error y permite corregir; E2E server validation preserves both fields and permits correction |
| @s25 | App.test.tsx @s25 informa de incertidumbre de red, conserva el formulario y no reintenta |
| @s26 | App.test.tsx @s26 representa etiquetas literalmente como texto plano; E2E server-confirmed markup remains literal text |
| @s27 | App.test.tsx @s27 ofrece campos etiquetados; E2E accessible keyboard creation cuatro viewports/reflow, sin overflow, etiquetas y foco visible |
| @s28 | App.test.tsx @s28 envía con teclado; restaura el foco de teclado perdido; no roba el foco; E2E accessible keyboard creation con teclado real, anuncio y foco |

Los tests adicionales de fronteras hexagonales, bootstrap, origen, construcción directa y precisión temporal protegen requisitos transversales de arquitectura. La reflow de200% se reproduce mediante ancho CSS720 equivalente a1440/2; no se presenta como prueba del menú nativo de zoom. Mutación y veredicto se registran en sus propios informes.
