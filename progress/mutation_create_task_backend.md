# Mutación backend de create_task

Ponytail full y Caveman lite activos. La revisión previa y la regresión conjunta se aprobaron antes de ejecutar mutación. Producción congelada; el umbral permanece en 80 %.

## Alcance

El perfil `-PmutationScope=create_task` incluye Task, TaskPage, TaskPosition, CreateTask, ReadTasks, TaskCreated, TaskController, PostgresTaskCommit, PostgresTaskQueries, OutboxMessage y RabbitBrokerPublisher. Los adaptadores se ejercitan con TaskApiTest/PostgreSQL real y las pruebas Rabbit existentes; dominio/aplicación mantienen pruebas unitarias. El informe incluye partes históricas compartidas de OutboxMessage/Rabbit; su denominador no se presenta íntegramente como código nuevo. Puertos, excepciones y configuración sin lógica nueva no se confunden con comportamiento cubierto por mutación. Los dos handlers nuevos tienen verificaciones HTTP de estado/código.

## Diagnóstico del ciclo de vida de pruebas

La primera ejecución se interrumpió sin score aceptado al aparecer timeouts. Un diagnóstico limitado a TaskController identificó el mutante lambda$list$0, índice 6, NegateConditionalsMutator. La extensión JUnit reiniciaba PostgreSQL entre iteraciones mientras Spring reutilizaba el contexto: puerto inicial 40306 y siguiente 47178; Hikari informó conexiones cerradas antes del timeout aproximado de cuatro segundos. Este resultado no acredita detección de un defecto de producción.

Se corrigió sólo TaskApiTest siguiendo el [patrón oficial de contenedor singleton](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers): arranque estático una vez por JVM y limpieza Ryuk al salir. Todos los asserts y la limpieza de datos por prueba permanecen. El foco de 14 casos volvió a verde. El diagnóstico posterior conservó un puerto por JVM (6805 para cobertura y 43393 para mutación) y Docker inspect confirmó eliminados ambos contenedores. No se amplió el timeout.

El diagnóstico limitado produjo 46/48, cero timeouts. Es evidencia de la corrección del entorno, no el score de la feature; no se suma al perfil completo. Logs locales ignorados: `.e2e-work/task-pit-diagnostic.log` y `.e2e-work/task-pit-diagnostic-fixed.log`.

## Margen de arranque del broker

El perfil completo reveló otra causa: RabbitBrokerPublisher.publish, índice 105, NegateConditionalsMutator sobre la selección de ruta. El log aislado muestra que el contenedor Rabbit tardó 3,64 segundos y PIT venció aproximadamente a los cuatro segundos antes de llegar al assert. Se mantiene un broker nuevo por ciclo para que mensajes o topología de un mutante no contaminen otro.

Sólo el margen constante de PIT se fija en 15000 ms para los perfiles que ejecutan broker real. El perfil authentication conserva su configuración anterior. No se modifica ningún límite de transporte de producción ni los asserts temporales. La ampliación permite arranque/limpieza del contenedor medidos; los timeouts posteriores, si aparecen, requieren revisión independiente. Log local: `.e2e-work/task-pit-broker-diagnostic.log`.

## Resultado final

El perfil completo terminó con salida 0 en 461 segundos: 182/186 mutantes eliminados (97,85 %), cero TIMED_OUT y cero NO_COVERAGE. Cobertura de líneas: 316/320. XML: `backend/build/reports/pitest-create-task/mutations.xml`. Este resultado permanece como denominador global observado.

Se detectaron tres huecos reales de pruebas, sin defecto nuevo de producción: falta de continuación con microsegundos válidos, host Rabbit configurado distinto del predeterminado y vhost distinto del predeterminado. Se refuerza el caso existente de paginación y se añaden dos casos de destino inaccesible: el publicador debe fallar de forma clasificada y no usar silenciosamente localhost o /. Los tres casos pasan con producción intacta.

| Superviviente inicial | Identificador | Tratamiento |
| --- | --- | --- |
| TaskController.decode: resto cambiado por multiplicación | línea 156, índice 160, MathMutator | Hueco real: cursor con microsegundos válidos; eliminado en replay |
| RabbitBrokerPublisher constructor: quitar setHost | línea 16, índice 17, VoidMethodCallMutator | Hueco real: destino configurado inaccesible; eliminado en replay |
| RabbitBrokerPublisher constructor: quitar setVirtualHost | línea 20, índice 41, VoidMethodCallMutator | Hueco real: vhost configurado inaccesible; eliminado en replay |
| TaskController.string: null sustituido por cadena vacía | línea 71, índice 17, EmptyObjectReturnValsMutator | Equivalente: title ausente/null y vacío producen REQUIRED; criterio ausente/null y vacío se normalizan a la misma cadena. No existe salida observable diferente en el contrato actual. |

El replay se limita a MATH y VOID_METHOD_CALLS de decode y el constructor Rabbit. No reemplaza el perfil global ni se suman denominadores. El informe separado será `backend/build/reports/pitest-create-task-replay/mutations.xml`. Ningún timeout se acepta como detección sin clasificar su causa.

### Replay final

El replay terminó con salida 0: 15/15 KILLED, cero timeouts y cero NO_COVERAGE, en 334 segundos. Incluye los tres IDs de huecos reales del perfil global; los doce mutantes adicionales de la misma selección también fueron eliminados. El XML separado permite verificar cada killingTest. El resultado global sigue siendo 182/186, con tres huecos reforzados y una equivalencia justificada; no se inventa un global recalculado.

Para reproducir el replay se aplica un init script a Gradle que configura targetClasses con TaskController y RabbitBrokerPublisher, targetTests con TaskApiTest y adapter.broker.*Test, mutators con MATH y VOID_METHOD_CALLS, y excluye los métodos equals, hashCode, toString, publish, create, string, minutes, malformed, list, detail, identifier y lambda*. reportDir se dirige a build/reports/pitest-create-task-replay. Se conserva -PmutationScope=create_task y sus límites de ejecución. El script ejecutado está en .e2e-work/task-pit-replay.gradle; esta descripción conserva su configuración aunque el directorio de trabajo sea ignorado.
