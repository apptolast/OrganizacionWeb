# project_states — mutación backend

PIT 1.22.0, ejecución local 28681 finalizada con salida 0: **163 de 163 mutantes eliminados (100 %)**. XML revisado: todos KILLED, ninguno SURVIVED ni NO_COVERAGE. Cobertura de líneas de las clases mutadas: 205/206 (99,51 %); la única línea no recorrida es el constructor privado vacío de ProjectStates. No se añade un test artificial para instanciar una utilidad estática.

El denominador contiene los 125 mutantes anteriores y 38 adicionales: ActiveProjectLimitException (2), ChangeProjectStatus (11), ProjectStatusChange (2), ProjectStatusChanged (8), ProjectStates (7) y ocho condiciones adicionales de OutboxMessage. No hay supervivientes que justificar.

Alcance constante: dominio y aplicación, con sus pruebas. Umbral 80 %, FRECORD desactivado para incluir constructores compactos escritos a mano. Únicamente equals/hashCode/toString generados quedan excluidos. No se atribuye este porcentaje a HTTP, JDBC, configuración ni RabbitMQ; esas fronteras se verifican mediante integración real y revisión independiente.

Comando ejecutado una vez al congelar: `gradlew.bat spotlessApply test --tests '*ProjectStates*' --tests '*ProjectStateConfigurationTest' --tests '*ChangeProjectStatusTest' --tests '*ProjectTest' --tests '*EditProject*' --tests '*ReadProjects*' --tests '*PublishOutboxTest' --tests '*RabbitBroker*' --tests '*ArchitectureTest' pitest`. Las 14 clases seleccionadas sumaron 249 casos sin fallos. Después solo se añadió y formateó el comentario sobre READ_COMMITTED; no se modificó comportamiento ni se repitió PIT.

Informes reproducibles: `backend/build/reports/pitest/mutations.xml` y `backend/build/reports/pitest/index.html`. Regresión global raíz 51375 confirmada: salida 0, lint, 328 pruebas backend sin fallos/errores/omitidos y 171 frontend verdes. El coordinador revisó también los 163 resultados KILLED del XML. CI de esta entrega pendiente; la CI anterior de edición no se contabiliza como evidencia de estados.
