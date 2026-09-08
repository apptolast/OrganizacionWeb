# PIT HTTP de integración 24: original

Campaña única oficial `node .harness/harness.mjs mutate integration_api-http-backend`, checkout c6121aa, EXIT 0. PIT 16m18, cobertura 11m24 y mutación 4m53; Gradle BUILD SUCCESSFUL 16m35. Baseline 541 clases, siete unidades. XML completo: 118 mutantes, 114 KILLED, 3 SURVIVED y 1 NO_COVERAGE. TIMED_OUT, MEMORY_ERROR, RUN_ERROR, NON_VIABLE, STARTED y NOT_STARTED: cero. Conservador KILLED/total = 96,61016949 %, superior al 80 % sin reclasificar estados.

Cinco patrones completos: ApiCredentialController, ApiCredentialSessionIdResolver, ApiCredentialBearerFilter, IntegrationOpenApiController y SecurityConfiguration. Todos los JUnit candidatos; ocho workers, timeoutConst15000 y controles existentes intactos. A verificó independientemente los argumentos del Java vivo y las 535 huellas. Before/after contienen 535 entradas y cero diferencias; V14 se excluyó antes de leer o calcular huellas. Backend, Gradle y runner equivalían a PRIMARY; únicamente cinco referencias AST históricas del test Node diferían por no copiar frontend.

Arranque escalonado tras baseline de A y autorización root: últimas muestras CPU73/30/58 %, memoria Windows libre17,44–17,83GiB. Coincidió inicialmente con mutación núcleo y frontend; núcleo finalizó durante este baseline. Los límites no se alteraron para compensar carga. Slowest baseline ImportScaleTest cadena32,954s; no se presenta como fallo ni se excluye su candidatura.

## Residuos revisados

- ApiCredentialController.revoke:37, find:51 y list:66: sobreviven las retiradas de `acceptable(http)`. La guardia existe en los tres caminos, pero la suite no demuestra individualmente su rechazo previo al puerto ante Accept incompatible. Son huecos de oráculo, no equivalencias demostradas ni defecto de la fuente actual. La creación sí tiene oráculo de negociación previo al puerto.
- ApiCredentialController.lambda$acceptable$1:90: NO_COVERAGE al reemplazar el comparador de especificidad por cero. Falta oráculo con varios tipos compatibles de distinta especificidad/calidad; la implementación usa MediaType y mantiene la precedencia descrita. No se inventa muerte ni equivalencia.

No se encontró defecto productivo confirmado en los cuatro residuos. No se persigue100 % ni se repite campaña. Resultado no acredita por sí solo todo OpenAPI: sus siete pruebas focales y validación oficial schema-base siguen siendo evidencia separada, igual que UX, E2E, rollback y aceptación real.

Originales: log, EXIT, mutations.xml y archivo ZIP con todos los informes HTML/XML del directorio oficial. Se preservan copias externas byte a byte antes de cualquier normalización Git. No se mezclan resultados con PIT núcleo o Stryker.
