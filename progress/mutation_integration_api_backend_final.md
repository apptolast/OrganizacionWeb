# Mutación backend de integración API 24

Campaña oficial completa sobre `360c7636a624a7ff884c30e962bedd19126a988f`, mediante `node .harness/harness.mjs mutate integration_api-backend`. EXIT 0. No replay ni reclasificación.

Resultado estricto: **180 KILLED / 188 totales = 95,7446808511 %**, frente al umbral del 80 %. Se conservan 7 SURVIVED y 1 NO_COVERAGE en el denominador; TIMED_OUT, NON_VIABLE, MEMORY_ERROR, RUN_ERROR, NOT_STARTED y STARTED son cero.

| Clase con residuos         | KILLED | SURVIVED | NO_COVERAGE |
| -------------------------- | -----: | -------: | ----------: |
| PostgresApiCredentialStore |     59 |        5 |           0 |
| ApiCredentialIntent        |     20 |        1 |           0 |
| CreateApiCredential        |      4 |        1 |           0 |
| ApplicationConfiguration   |     64 |        0 |           1 |

Las otras once clases generadas sólo tienen KILLED. El inventario original conserva método, descriptor, línea, mutador, índice, bloque y descripción de cada residuo.

Controles efectivos observados: 12 patrones de clases completos, todos los candidatos JUnit (`com.apptolast.organization.*`), 8 workers, timeout constante de 15000 ms, umbral 80, mutadores por defecto, `-FRECORD` y exclusiones existentes de equals/hashCode/toString. PIT 1.22.0. La ejecución examinó 541 candidatos, creó 15 unidades y ejecutó 1667 pruebas durante la mutación. Cobertura: 547 s; mutación: 725 s; PIT total: 1273 s. Estos tiempos son resultados medidos, sin afirmar mejora de velocidad.

Los 1039 archivos del manifiesto permanecieron idénticos antes y después. V14 se excluyó antes de toda lectura/hash. Checkout limpio al terminar; proceso principal finalizado. No se detuvieron recursos ajenos. El scope HTTP y los cambios posteriores del recurso OpenAPI se revisan en su campaña independiente.

El baseline oficial previo pasó: Node 72/72, Java 3446 pruebas en 149 suites y frontend 2492 pruebas en 65 suites; todos sin fallos. Se conservaron por separado los intentos anteriores fallidos y sus reparaciones autorizadas de expectativas heredadas.

Evidencia portátil versionada: `progress/integration24_core_pit_originals.zip` contiene los 29 artefactos originales del freeze y el propio freeze. `progress/integration24_core_pit_portable_manifest.json` registra tamaño y SHA-256 de cada entrada. Los originales también se conservan en `work/deployment-preparation`. Root verificó 29/29 originales, las 30 entradas del ZIP y contó directamente el XML; aprobación de esta campaña, sin declarar despliegue ni cierre de la feature.

La comparación Java/Gradle contra959ad66 sólo cambia IntegrationOpenApiTest, revisado junto al recurso final y cubierto por el gate HTTP independiente y CI final. Las clases mutadas de núcleo permanecen idénticas.
