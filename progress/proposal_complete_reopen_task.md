# Propuesta inicial — completar y reabrir tareas

Preparación documental de la feature 9. Split_task sigue siendo la única implementación activa; este documento no activa código ni sustituye el contrato futuro.

El usuario necesita saber qué completó y cuándo, incluso si después reabre ese trabajo. Completar una tarea debe ser una decisión explícita, distinta de terminar un proyecto o cerrar una sesión de trabajo. No debe propagar estados automáticamente entre padre e hijos ni inventar minutos trabajados. La interfaz mostrará el estado confirmado y conservará una recuperación deliberada ante conflicto o fallo de red.

Antes de destilar el contrato deben fijarse la representación de completedAt, la precondición de versión para cambios concurrentes, los estados permitidos y la conducta al repetir una intención ya satisfecha. Reabrir no debe borrar el registro de la finalización anterior. El historial duradero necesita una decisión explícita de persistencia y retención; no se debe asumir que una cola de mensajes o una tabla de outbox operativo son por sí mismas un historial permanente del producto.

La compatibilidad debe revisar todas las representaciones que usan Task: lista plana, detalle, padre y lista de hijos. El corte anterior exige ocho campos exactos y estado inicial pending; cualquier ampliación o recurso adicional se especificará antes de cambiar clientes y pruebas. El evento nuevo tendrá esquema cerrado, identidad estable y publicación atómica con el cambio confirmado. Los eventos de creación existentes no se reescriben.

También queda detectada una necesidad del recorrido completo: corregir título, criterio o estimación después de crear una tarea. El roadmap actual no tiene una feature explícita de edición de tarea. Antes de dar por terminado el MVP debe resolverse como corte acotado, con contrato propio y control de concurrencia, sin mezclarlo silenciosamente con completar/reabrir ni con personalización avanzada.

La validación prevista incluye propiedad, versión antigua, carreras, repetición sin efectos duplicados, finalización seguida de reapertura sin pérdida de historia, independencia entre niveles, atomicidad y recuperación de la interfaz. El siguiente paso será revisar estas decisiones después del cierre de split_task.

La revisión independiente está en `review_complete_reopen_task_proposal.md`: propone una versión interna de tarea, un recurso de estado con ETag y una tabla de transiciones separada del outbox. El contrato también debe decidir expresamente si un padre completed admite nuevas subtareas mientras el proyecto permanezca abierto. Actualmente la creación sólo depende del estado del proyecto; no debe introducirse una nueva restricción del padre por accidente al ampliar los estados de Task.

El borrador documental `contract_complete_reopen_task_draft.feature` propone mantener esa independencia: un padre completed admite hijos nuevos si el proyecto está abierto, sin reabrirse automáticamente. Su primera validación con el parser Gherkin existente confirma 28 escenarios, 61 casos locales de tablas, etiquetas únicas y un When por escenario. Todavía requiere revisión y cierre de detalles; no activa implementación ni cambia el estado pending de la feature 9.

El borrador final incorpora las revisiones backend e integración: recurso de estado con ETag propio, historial independiente de retención indefinida, compatibilidad DTO8 y reloj no decreciente. Parser final del coordinador: 36 escenarios y 137 casos locales; las matrices referenciadas exigen además cada variante indicada. Los 28/61 anteriores describen exclusivamente la primera versión. Precisadas la selección HTTP 415 y las nueve ausencias de campos del evento. Esta preparación sigue sin activar implementación.
