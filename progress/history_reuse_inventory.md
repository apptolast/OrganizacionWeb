# Inventario de reutilización para preparar Historial18

Lectura de root el 7 de septiembre mientras los tres agentes implementan17. No es contrato aprobado ni inicio de implementación18. Su alcance deberá pasar especificación y Gherkin después de17.

La especificación general ya exige hechos propios de sesiones, finalizaciones y replanificaciones. Hay fuentes durables distintas del outbox que permiten evitar un segundo registro de hechos y un consumidor nuevo sólo para esta lectura:

- task_status_history (V9): identificador, proyecto/tarea, versión, estado anterior/nuevo e instante. El propietario se obtiene mediante projects; las reaperturas permanecen. PostgresTaskHistoryQueries y TaskHistory ya resuelven una consulta local de tarea, pero su cursor por versión no sirve como cursor global.
- block_changes (V12): identificador, proyecto/tarea/bloque, tipo, versión, instante y recibo JSONB. La planificación inicial y los cambios son hechos distintos; no se debe inventar un cambio a partir de la proyección actual.
- work_sessions y work_session_changes (V14/V16): inicio durable y recibos de decisiones, con owner_id y fecha. El cierre16 conserva notas, tiempo real y atribución de día en su recibo; no debe recalcularse a partir del estado de tarea o del tiempo previsto.17 añade una decisión EXTEND sin incrementar trabajo real.
- La URL de detalle de sesión ya existe en App. TaskHistory y RescheduleHistory aportan patrones de carga, paginación, recuperación y controles; reutilizar comportamiento comprobado sin asumir que sus DTO o cursores son intercambiables.

Decisiones que el contrato18 deberá fijar: tipos exactos que aparecen; filtros permitidos y semántica de fechas/zona; orden total estable entre colecciones; cursor opaco vinculado a filtros; límite y continuación; identidad propia en cada rama; comportamiento ante inserciones entre páginas; tratamiento de fuentes incompletas o error de almacenamiento. Una unión de lecturas no debe multiplicar un mismo cierre ni sumar ampliaciones como tiempo trabajado.

La lectura no necesita modificar las tablas históricas ni emitir un evento por consultar. Si se requieren índices, se justificarán con la consulta real y una migración aditiva. Las estadísticas semanales, exportaciones y edición retrospectiva tienen contratos posteriores y no se incorporan por inferencia. Los casos de paginación y privacidad deben probarse contra PostgreSQL real; la UI deberá conservar los criterios responsive y de recuperación ya vigentes.
