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
