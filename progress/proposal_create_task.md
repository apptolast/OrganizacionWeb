# Propuesta inicial — create_task (feature 7)

Preparación de producto, sin contrato aprobado ni producción. Authentication sigue siendo la única feature activa. El objetivo siguiente es guardar una unidad pequeña de trabajo dentro de un proyecto y recuperarla después de recargar la web.

## Decisiones propuestas

Una tarea pertenece a un proyecto propio. Pedir título y permitir un criterio de finalización y una estimación en minutos, ambos opcionales. El título expresa el resultado; el criterio permite decidir cuándo se ha conseguido. La estimación nunca cuenta como trabajo realizado. Crear no activa el proyecto, programa bloques ni inicia sesiones.

Propuesta de límites: título de 1 a 160 puntos de código tras recortar espacios; criterio de finalización hasta 2000 puntos de código conservando el texto; estimación null o entero de 1 a 1440 minutos. Los límites evitan datos accidentales sin imponer una duración recomendada. UI con minutos y ayuda breve; sin selector de fechas hasta el contrato de planificación.

Identidad UUID generada en servidor, propietario derivado del Principal, referencia al proyecto, estado inicial pendiente e instantes de creación/actualización. Completar y reabrir corresponden a feature 9; subtareas a feature 8. No añadir campos de progreso, prioridad, etiquetas o fechas que todavía no tengan comportamiento acordado.

Persistir tarea y TaskCreated.v1 atómicamente mediante el patrón outbox existente. Reutilizar publicación, confirmaciones y recuperación para una ruta cerrada nueva. Mantener privacidad de criterio de finalización en el evento; fijar el esquema mínimo exacto al destilar el contrato. Fallos de almacenamiento no producen una tarea parcial ni un falso éxito. Sin reintento automático de creación cuando la red deje un resultado incierto.

El roadmap no contiene una feature independiente para consultar tareas. Este corte debe incluir la lectura necesaria para ver las tareas guardadas dentro del proyecto tras recargar, con límite y paginación explícitos, sin implementar filtros o vistas avanzadas. La creación aislada con una confirmación que desaparece al recargar no cubre el objetivo del usuario.

Antes del contrato hay que decidir: política de creación dentro de un proyecto terminado; relación entre terminar un proyecto y sus tareas pendientes; campos públicos y evento exactos; orden y tamaño de página; rutas; tratamiento de concurrencia con un cambio de estado del proyecto. Se propone exigir reapertura en pausa para añadir trabajo a un proyecto terminado, sin completar tareas automáticamente ni modificar los proyectos al consultar.

## Verificación prevista

Propiedad y privacidad; límites Unicode/estimación y JSON estricto; lectura persistente; atomicidad de tarea/evento; carreras pertinentes con el estado del proyecto; publicación real del nuevo tipo; CSRF y sesión ya establecidos por feature 6. Formulario y lista con SCSS existente, teclado/foco, errores conservando borrador, matriz de treinta principios y pruebas responsive/zoom. No declarar evidencia antes de ejecutar ni iniciar este corte durante autenticación.
