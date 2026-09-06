# Revisión frontend — completar y reabrir tareas

**APPROVED para iniciar mutación del frontend. No autoriza cerrar la feature completa.**

Revisados TaskState, TaskHistory, cliente API, cambios de montaje/listas, SCSS, adaptación de la suite histórica y mapa de los 36 escenarios. El coordinador repitió la suite completa sobre la fuente congelada: **625/625 pruebas en 17 archivos**, salida 0, 7,02 segundos. Lint y build finales verdes informados por el autor. Las pruebas de navegador y la integración backend siguen pendientes.

La representación del estado usa un único snapshot/ETag. Los cambios requieren confirmación compatible con la intención; una respuesta incierta ofrece una consulta deliberada. Estado e historial mantienen errores independientes. El historial se actualiza después de PUT o de una consulta deliberada confirmada, sin duplicar la consulta inicial. El hallazgo de historia vacía tras conflicto fue reproducido RED y corregido GREEN; el coordinador leyó la prueba que espera el vacío inicial y comprueba una sola escritura y la transición posterior.

Las lecturas y escrituras abortadas no revocan la sesión vigente al recibir un 401 tardío. Las pruebas usan App/SessionGate reales, incluyendo navegación, logout y montaje StrictMode; no dependen sólo de inspeccionar setters. Se conservan las pruebas históricas de subtareas mediante respuestas a los endpoints nuevos, sin sustituir sus componentes. Las listas muestran el estado de cada DTO; crear sigue exigiendo pending.

Las fechas conservan datetime e indican UTC. Recuperación y paginación restituyen foco local sólo cuando no existe otro destino elegido. SCSS incorpora controles de al menos 44 px, texto adaptable y botones de ancho completo en móvil; su comportamiento visual real todavía requiere matriz/axe/zoom y revisión de capturas.

Alcance de Stryker contrastado con las líneas finales: completos task-state.tsx, task-history.tsx y task-status-api.ts; tasks-api.ts líneas 87 y 115; project-tasks.tsx 103–105; montaje task-reader.tsx 107–111. El perfil global conserva sus archivos anteriores y añade los tres módulos nuevos. Umbral 80 y concurrencia 2 sin recortes de lógica nueva. Registrar resultados originales, supervivientes y cualquier replay por separado. La revisión de equivalencias debe tener en cuenta que revision en TaskState ahora participa en una comparación mayor que cero.
