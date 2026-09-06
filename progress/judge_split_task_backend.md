# Revisión backend — split_task

**Diseño, cobertura y pruebas backend: APPROVED para la puerta previa de mutación. Cierre global pendiente.** Revisión independiente por integration_craftsman, que no escribió producción ni tests backend de este corte. Este agente sí escribió parte del cliente API frontend y E2E; no se atribuye independencia sobre esos archivos. Su revisión corresponde al coordinador/frontend. Ponytail full y Caveman lite activos.

## Corte inspeccionado

Contrato aprobado de 38 escenarios, sección 8 de project-spec.md, bitácora de 13 ciclos, CreateSubtask/ReadSubtasks y puertos, eventos sellados, TaskController, PostgresTaskCommit/Queries, V8, OutboxMessage y ruta RabbitBrokerPublisher. Se leyeron SubtaskApiTest, CreateSubtaskTest, SubtaskEventTest y las comprobaciones de transporte y bloqueo de eventos incompatibles.

La creación reutiliza validación de Task y confirma padre/hijo dentro del mismo proyecto. V8 agrega parent_id nullable, unicidad compuesta, FK compuesta y rechazo de autorrelación. No incorpora borrado en cascada, movimientos ni consultas de ancestros. DTO8 permanece intacto y la colección plana no filtra hijos.

La transacción bloquea el proyecto propio, comprueba que el padre pertenece a él y decide completed bajo ese bloqueo. La tarea contiene la relación en la misma inserción, seguida de un único SubtaskCreated; ambas escrituras exigen exactamente una fila. No modifica padre/proyecto ni toma bloqueo global de capacidad. Las cuatro pruebas con triggers PostgreSQL cubren excepción/cero filas en tasks/outbox y verifican ausencia de hijo/evento y snapshots intactos.

GET padre usa un JOIN único del hijo autorizado y LEFT JOIN del padre. Recurso inexistente produce cero filas y 404; raíz propia produce una fila con padre nulo. La FK evita atribuir a una referencia colgante una raíz válida. GET hijos valida primero el padre propio y consulta sólo project_id/parent_id con corte descendente por fecha/id, máximo 21 para devolver 20. No descarga el árbol completo.

El cursor reutiliza validación cerrada, base64url canónica, precisión de microsegundos y rango PostgreSQL; agrega parentTaskId al contexto. Los UUID de ruta se validan antes del cursor. Los cursores generados son canónicos aunque la ruta use mayúsculas; no se confunde el cursor plano con el de hijos.

SubtaskCreated tiene nueve campos exactos. La validación comprueba parentTaskId escalar/completo y distinto de taskId mediante UUID.equals, incluida caja distinta. Tipo/versión desconocidos se bloquean como UNSUPPORTED_EVENT y contenido incompatible como INVALID_EVENT. La ruta/cola nueva conserva mandatory, persistencia, confirmaciones y límites de transporte existentes; no cambia los cuatro contratos anteriores.

## Cobertura observada en las fuentes de pruebas

- s1–s5: POST real con Location/relación/evento; tablas positivas Unicode y opcionales, JSON y tipos heredados ejecutados efectivamente sobre `/subtasks`, no sólo referenciados en prosa. Los rechazos comprueban cero hijos/eventos.
- s6–s8, s15, s19–s20: seis combinaciones de sesión ausente/vencida, tres de CSRF/origen, cinco recursos privados comprobados en los tres endpoints, cuatro errores de path y diez respuestas no-store. Expiración usa sesión persistida; indisponibilidad renombra temporalmente tasks y no devuelve vacío/null ficticio.
- s9–s14, s34–s35: raíz confirmada, padre de hijo/nieto, sólo hijos directos, colección plana conservada, 21 hijos con empates y microsegundos, inserción posterior y cursor cerrado vinculado al padre. Hay fixtures específicos para parámetros repetidos, claves duplicadas, campos, tipos, precisión, padding y pertenencia.
- s16–s18, s36: completed impide POST pero permite lectura; reapertura por API no crea automáticamente otro hijo. Dos órdenes concurrentes mantienen la transacción del primer escritor y verifican espera del segundo, resultado y conteo de hijos/eventos. Cuatro triggers revierten escrituras parciales. Tres inserciones SQL inválidas prueban integridad del padre.
- s21–s22, s37: dominio y aplicación validan esquema/identidad y bloqueo sin llamada al broker; Rabbit real prueba recepción por ruta nueva. Broker detenido junto con API/worker será evidencia del smoke de integración, todavía pendiente.
- s23–s33 y s38: corresponden a cliente/UI/integración, fuera de la aprobación independiente de backend. Sus resultados no se presuponen por estar verde el código de servidor.

## Hallazgos y límites

No se detecta un hallazgo bloqueante en el corte congelado. El autor completó la trazabilidad explícita, incluidas todas las filas heredadas ejecutadas en las nuevas rutas, y comunicó salida 0 de la sesión 29329. Se inspeccionaron independientemente los XML: **370 pruebas, cero fallos, errores y omisiones**. Incluyen 105 de SubtaskApiTest, una de CreateSubtaskTest, seis de ReadSubtasksTest, 17 de SubtaskPublicationTest, seis de SubtaskEventTest y nueve de RabbitBrokerPublisherTest, además de regresión compartida.

Este agente no ejecutó Gradle ni construyó imágenes durante la revisión. El dictamen permite al coordinador considerar superada la revisión previa de backend; la decisión de iniciar mutación sigue siendo suya. No se afirma ningún score de mutación aún.

La aprobación final queda condicionada al corte liberado, resultados verificados, E2E/smoke del nuevo evento y revisión de mutación después de la puerta del coordinador. No se declara completada la feature ni se reutilizan números históricos como score del corte 8.

## Dictamen final del backend congelado

APPROVED en el alcance independiente backend. Se verificó directamente mutations.xml: 236 mutaciones, 235 KILLED y un SURVIVED, sin timeouts ni falta de cobertura. La identidad del superviviente es TaskController.string, línea 98, EmptyObjectReturnValsMutator, índice 17/bloque 4: null por cadena vacía; ambas rutas normalizan título/criterio de manera observable equivalente. Coincide con mutation_split_task_backend.md, sin reclasificar mutantes de otros informes.

Evidencia de integración sobre la imagen congelada: cinco E2E nuevos verdes (niveles, DTO8, proyecto intacto, cursor de 21 hijos, privacidad y recuperación); el conjunto tuvo 37/38 por una espera de foco histórica del test, corregida y repetida 1/1 sin tocar producción. Firefox/WebKit 2/2. pnpm test:publisher EXIT 0 confirmó SubtaskCreated con broker detenido/recuperado y retención real después del reinicio, conservando las cuatro rutas anteriores. Detalles y límites en tdd_split_task_integration.md y ux_split_task.md.

No queda hallazgo backend abierto en esta revisión. La aprobación no sustituye init global, revisión de frontend/API/E2E por otro autor ni decisión de publicación del coordinador. No se ejecutó Gradle host por este agente.
