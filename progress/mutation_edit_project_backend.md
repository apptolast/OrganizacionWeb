# edit_project — mutación backend

Ejecución local final: 6 de septiembre de 2026, Gradle 99273, salida 0. PIT 1.22.0 eliminó **125 de 125 mutantes (100 %)**, con **150 de 150 líneas cubiertas** en las clases mutadas. El XML no contiene SURVIVED, NO_COVERAGE ni otros resultados pendientes.

El denominador incluye los 103 mutantes del núcleo anterior y 22 adicionales: EditProject (8), ProjectChange (2), ProjectUpdated (7), ProjectRevision (2), ProjectSnapshot (2) y una condición adicional de la lista permitida en OutboxMessage (1). Los getters de records utilizados por el comportamiento permanecen incluidos. No hay supervivientes que justificar.

Se mantiene el alcance `com.apptolast.organization.domain.*` y `com.apptolast.organization.application.*`; las pruebas utilizadas por PIT pertenecen a esas mismas capas. El filtro FRECORD continúa desactivado para no ocultar constructores compactos escritos a mano. Solo se excluyen equals, hashCode y toString generados por el compilador. Umbral configurado: 80 %.

Los adaptadores HTTP, PostgreSQL y RabbitMQ están fuera del denominador PIT. Su evidencia procede de pruebas significativas con PostgreSQL y RabbitMQ reales, además de la revisión independiente y los recorridos E2E. No se atribuye cobertura de mutación del transporte a este porcentaje.

Informe reproducible: `backend/build/reports/pitest/mutations.xml` y `backend/build/reports/pitest/index.html`. Comando ejecutado una vez tras congelar el código: `gradlew.bat spotlessApply test --tests '*EditProjectTest' --tests '*EditProjectsApiTest' --tests '*ReadProjectsTest' --tests '*ReadProjectsApiTest' --tests '*PublishOutboxTest' --tests '*RabbitBroker*' --tests '*ArchitectureTest' pitest`. Las ocho clases seleccionadas sumaron 143 casos sin fallos ni errores. La regresión global, CI y cierre corresponden al coordinador y se registrarán con su resultado real.
