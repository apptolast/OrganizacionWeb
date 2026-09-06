# Propuesta inicial — split_task (feature 8)

Preparación documental. Create_task sigue siendo la única implementación activa. La siguiente necesidad del usuario es dividir trabajo demasiado grande en resultados pequeños y verificables, manteniendo su relación con la tarea original.

## Comportamiento propuesto

Añadir subtareas a una tarea propia, con título, criterio de finalización y estimación propios. La división no elimina ni completa la tarea original y no registra tiempo trabajado. El proyecto conserva sus restricciones: añadir trabajo a un proyecto terminado requiere reapertura explícita. La creación de cada subtarea y su evento deben confirmarse juntos; un fallo no borra el borrador ni declara éxito.

La relación debe quedar guardada y ser consultable tras recargar. La interfaz presentará el contexto del padre y permitirá acceder a las subtareas con teclado y en móvil; se prefieren listas y controles nativos a un árbol interactivo especializado. No cargar todas las ramas de un proyecto para mostrar una tarea. El límite y orden de cada colección deben ser explícitos.

La estimación de un padre no se sumará automáticamente a las de sus hijos. En este corte no existe todavía un total de tiempo planificado: cada estimación se muestra como tal. Al introducir totales habrá que declarar qué nivel cuenta y evitar duplicar trabajo. Completar o reabrir tareas mantiene su feature independiente y no se convierte en efecto implícito de dividirlas.

## Decisiones antes del contrato

Fijar si se admiten varios niveles desde este corte; cómo se exponen parentId y la colección de raíces/subtareas en la API; qué muestra la lista de proyecto; qué ocurre con la estimación original; y el esquema mínimo del evento. Crear sólo hijos nuevos evita necesitar movimientos, detección de ciclos por reubicación o borrado de jerarquías en esta feature. Esos comportamientos no se incorporarán sin contrato.

La persistencia debe impedir relaciones entre proyectos o propietarios distintos. Reutilizar la frontera transaccional del proyecto y un orden de bloqueo único, sin ampliar el bloqueo global de capacidad. La lectura de la relación no debe depender de que RabbitMQ haya publicado el evento. Mantener entrega al menos una vez, identidad y privacidad del contenido opcional.

## Evidencia prevista

Propiedad y relación padre/proyecto; creación y evento atómicos; límites de campos; recarga y navegación por ramas; fallo de red y sesión; doble envío y respuestas tardías; conservación de la tarea original y de las estimaciones sin sumas ficticias; publicación real; regresión de creación/lectura de tareas y matriz UX completa. Esta propuesta no activa producción ni representa funcionalidad terminada.

## Revisión de compatibilidad

La revisión independiente propone conservar los ocho campos de tarea y la lista existente del proyecto, que seguirá devolviendo todas las tareas. Una colección explícita /projects/{projectId}/tasks/{parentId}/subtasks permite crear y consultar hijos sin cambiar silenciosamente la semántica anterior. Cada página contendría veinte elementos, ordenados por createdAt e id descendentes, con cursor vinculado a proyecto y padre. Consultar un padre desde una tarea aislada podría resolverse mediante un recurso de relación separado; sólo se añadirá si el recorrido de interfaz lo requiere.

El corte de un nivel reduce complejidad, pero la petición original contempla dividir trabajo en piezas cada vez menores. Antes del contrato se comparará esa limitación con varios niveles creados siempre como hijos nuevos. En ambos casos quedan excluidos movimientos y reasignaciones: no hay razón para introducir ciclos con entidades nuevas ni un editor de árboles completo. No se cambia aún el contrato de create_task ni se inicia implementación de split_task.

## Dirección propuesta para la conversación técnica del siguiente corte

Para responder a la división progresiva del trabajo, se propone permitir varios niveles mediante creación exclusiva de hijos nuevos. La interfaz abriría una tarea en una vista propia, con enlace al proyecto, enlace al padre directo cuando exista y lista paginada de hijos. No necesita descargar un árbol completo ni calcular toda la cadena de ancestros. Cada hijo podrá abrirse con el mismo recorrido. La lista actual del proyecto conservará todas las tareas y ofrecerá navegación explícita hacia su detalle.

La API puede conservar el DTO de ocho campos y exponer la relación mediante un recurso separado del detalle, además de POST/GET de subtareas. La creación actual en el proyecto seguirá produciendo tareas raíz. Antes de aprobar el contrato debe fijarse la forma exacta del recurso del padre, la Location de creación, el cursor vinculado a la colección de hijos y la integridad compuesta proyecto/padre en PostgreSQL. El padre debe existir dentro del mismo proyecto propio al confirmar; una referencia ajena o inexistente tendrá el mismo error público.

Se propone un evento específico SubtaskCreated.v1 con identidad del proyecto, de la tarea nueva y de su padre, sin criterio ni estimación. Esto evita cambiar silenciosamente TaskCreated.v1 o emitir dos eventos para una sola intención. El nombre y esquema exactos requieren revisión backend al cerrar create_task. La estimación del padre se conserva, sin sumas ni propagación de completado; los estados de tareas permanecen en su feature siguiente.

Estas decisiones son preparación revisable, no un contrato activo. La prioridad sigue siendo terminar la validación de create_task.

## Borrador revisado por integración

El borrador revisado contiene 38 escenarios con tags únicos s1–s38 en `progress/contract_split_task_draft.feature` y sigue marcado `@draft`. Varios niveles creados exclusivamente como hijos nuevos mantienen DTO8 y no requieren un endpoint de árbol. Las reglas de contenido remiten de forma ejecutable a todas las entradas de create_task s2–s8 sobre la ruta nueva; las fronteras de sesión, rollback, cursor y recuperación se identifican por separado. Se precisan la ruta web `/proyectos/{projectId}/tareas/{id}`, el recurso del padre con `{parent:null}` o `{parent:<DTO8>}`, la recuperación deliberada de PROJECT_COMPLETED y el evento por `subtask.created.v1` a `organization.subtask-created.v1`. El cursor propuesto contiene exactamente projectId, parentTaskId, createdAt e id, revisado por backend antes de aprobar. No se activa producción, tests ni metadatos de feature 8; create_task sigue siendo la única implementación activa.
