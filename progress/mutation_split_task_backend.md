# Mutación backend: split_task

## Puerta y alcance

El coordinador aprobó ejecutar PIT tras revisión independiente del corte y comprobación XML de 370 pruebas del alcance, sin fallos, errores ni omisiones. La producción queda congelada; Ponytail full y Caveman lite permanecen activos.

Comando: `./gradlew.bat pitest -PmutationScope=split_task`. Sesión 76051; log local ignorado `.e2e-work/split-task-pit.log`. Informe independiente esperado en `backend/build/reports/pitest-split-task/mutations.xml` y HTML del mismo directorio.

Incluye TaskController, PostgresTaskCommit, PostgresTaskQueries y RabbitBrokerPublisher; Task, TaskPage, TaskPosition y OutboxMessage; CreateTask, ReadTasks, TaskCreated, CreateSubtask, ReadSubtasks y SubtaskCreated. Los adaptadores compartidos exigen conservar pruebas históricas de raíces y de los cuatro eventos anteriores. No se presenta el denominador entero como lógica nueva de subtareas. Puertos e interfaz sellada carecen de lógica; la configuración sólo conecta dependencias.

Se mantienen PIT 1.22.0, plugin JUnit 5 1.2.3, umbral 80, exclusión de equals/hashCode/toString y FRECORD desactivado para conservar constructores escritos a mano. El margen de 15 segundos del contenedor RabbitMQ procede del diagnóstico de create_task; no se ha ampliado en esta feature. PostgreSQL usa el endpoint singleton por JVM para el contexto Spring; RabbitMQ reinicia su contenedor entre ciclos de JUnit y evita contaminación de mensajes/topología.

## Resultado

Sesión 76051 EXIT 0 en 8 minutos y 50 segundos. XML inspeccionado: **235 KILLED / 236 mutaciones (99,58 %)**, **1 SURVIVED**, **0 TIMED_OUT**, **0 NO_COVERAGE** y ningún error de ejecución. Cobertura de líneas de las clases mutadas: 410/416. PIT examinó 67 pruebas y ejecutó 1025 comprobaciones de mutantes. La cobertura inicial terminó en 19 segundos; ningún test superó 2000 ms.

CreateSubtask aporta 3 mutaciones, ReadSubtasks 2 y SubtaskCreated 9: las 14 quedaron KILLED. Las otras 222 pertenecen a clases compartidas e históricas, incluidas las nuevas ramas de cursor, relación PostgreSQL y evento; no se etiquetan todas como lógica antigua ni nueva. El denominador 236 corresponde al alcance completo declarado, sin sumar replays ni diagnósticos.

## Único superviviente equivalente

| Identidad XML | Observación y justificación |
| --- | --- |
| TaskController.string, línea 98; EmptyObjectReturnValsMutator; índice 17, bloque 4; descriptor (JsonNode,String)String | Cambia el retorno null por cadena vacía cuando falta la propiedad o su valor es null. Este helper sólo procesa title y completionCriterion en creación de raíz/subtarea. Task normaliza ambos valores antes de usar o publicar datos: title ausente/null/vacío produce el mismo REQUIRED, y completionCriterion ausente/null/vacío queda como cadena vacía. No cambia DTO, código de error, escritura ni evento. |

El mutante ejecutó 49 pruebas y sobrevivió por esa equivalencia observable. La equivalencia histórica se comprobó también en las tablas del endpoint nuevo: SubtaskApiTest.s4_inherited_s4_s6_validatesJsonTypes y s4_inherited_s2_s3_s5_s7_positiveContent, además de los tests de Task. No se añade una prueba espejo del retorno privado para perseguir el score.

No quedan supervivientes no equivalentes identificados y no hizo falta replay ni modificación de producción o tests después del corte normal. Los 370 tests del alcance ya verificados conservan su evidencia; el coordinador ejecutará el init global independiente. No se declara split_task done ni CI propia completada.

## Límites

PIT verifica mutaciones JVM de las clases indicadas. No muta por sí mismo el texto de restricciones SQL ni sustituye los casos PostgreSQL reales de integridad, privacidad, rollback y carrera. La recuperación del broker y los recorridos de navegador se verifican por separado en integración. La equivalencia se limita a los dos usos actuales del helper; si cambia su contrato, debe revisarse.
