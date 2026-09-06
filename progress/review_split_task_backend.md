# Revisión backend del borrador split_task

Revisión exclusivamente documental de progress/proposal_split_task.md y progress/contract_split_task_draft.feature. Ponytail full y Caveman lite activos. Create_task sigue pendiente de cierre formal; no se activa ni implementa feature 8.

## Compatibilidad y persistencia

El diseño mantiene el DTO de ocho campos y la colección plana existente. Añadir hijos no altera fechas, estado ni ETag del proyecto o del padre. El recurso de relación distingue raíz confirmada de fallo de lectura, y la lista de hijos evita descargar todo el árbol.

Se recomienda una migración V8 aditiva con parent_id nullable en tasks. Las filas anteriores y las creaciones por la ruta vigente permanecen raíz. Una restricción UNIQUE(project_id,id) permite una clave externa compuesta (project_id,parent_id) que referencia tasks(project_id,id). Así PostgreSQL impide relaciones entre proyectos incluso si falla una comprobación de aplicación. Añadir CHECK(parent_id <> id) conserva la distinción entre hijo y padre; el valor null de una raíz sigue permitido. No introducir borrado en cascada de jerarquías en este corte.

Un índice (project_id,parent_id,created_at DESC,id DESC) respalda la colección de hijos directos. El índice plano actual se conserva. No se requieren consultas recursivas ni un registro de ancestros: sólo se crean hijos nuevos, sin movimientos o reasignación. La API no acepta parentId en el cuerpo.

La creación bloquea primero el proyecto, después verifica que el padre pertenece a ese mismo proyecto dentro de la transacción. Reutiliza el orden y READ_COMMITTED del corte de tareas, sin adquirir el bloqueo asesor global de capacidad. Relación, contenido y evento se confirman juntos y ambas escrituras deben afectar exactamente una fila. La carrera con completar mantiene las dos consecuencias ya acordadas: completar primero rechaza la creación; crear primero permite conservar el hijo pendiente después del cierre.

## Identidad y publicación

SubtaskCreated.v1 con nueve campos exactos conserva aggregateId como proyecto e identifica por separado taskId y parentTaskId. La validación del publicador debe rechazar taskId igual a parentTaskId aunque ambos sean UUID completos: ese payload contradice una creación de hijo nuevo. El borrador s22 necesita esa fila explícita. El título mantiene 160 puntos de código; criterio y estimación no se publican. No se emite además TaskCreated.v1 por la misma creación.

Se mantiene la semántica de entrega al menos una vez y la identidad del evento, sin prometer orden entre eventos distintos. Las cuatro rutas previas permanecen cerradas y compatibles. La relación debe poder consultarse antes de publicar en RabbitMQ.

## Ajustes del contrato antes de activarlo

Los escenarios s10, s12, s16, s21 y s24 contienen dos When en la versión revisada. Deben separarse o trasladar la preparación al Given, preservando cada comportamiento observable, conforme a docs/gherkin.md. Las referencias a tablas previas deben conservar trazabilidad por entrada y ejecutar la ruta nueva; no basta citar pruebas históricas de otra ruta.

La validación de projectId y parentId precede a decodificar el cursor. Su documento exacto debe contener projectId, parentTaskId, createdAt e id y quedar vinculado a ambos identificadores. Un cursor de la colección plana no se acepta como cursor de hijos.

No se observaron otros bloqueos de diseño en el alcance propuesto. Este informe no declara aprobado el contrato ni anticipa resultados de pruebas futuras. El coordinador aplica los ajustes y decide la activación después del cierre de feature 7.

## Validación del borrador corregido

El coordinador corrigió el borrador manteniendo @split_task y @draft. Se volvió a parsear con las dependencias ya disponibles en work/spec-validation: @cucumber/gherkin y @cucumber/messages, sin instalar paquetes. Resultado: 38 escenarios, 82 casos expandidos por las tablas locales, tags únicos y exactamente un When por escenario. Las referencias a tablas heredadas requieren además sus entradas concretas al implementar; el recuento 82 no pretende expandir referencias textuales a otro archivo.

Los cinco hallazgos de múltiples When quedan resueltos mediante la separación de comportamientos en s34–s38 y la revisión de los originales. s22 incorpora explícitamente taskId igual a parentTaskId como INVALID_EVENT, resolviendo el hallazgo de identidad. El parseo confirma sintaxis y estructura; no sustituye pruebas de comportamiento ni activa el contrato.

No se modificaron el borrador, dependencias, metadatos de feature, pruebas ni producción durante esta validación. La recomendación de integridad compuesta y orden de bloqueo sigue vigente para la implementación futura.
