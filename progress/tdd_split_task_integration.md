# split_task — integración

Leídos el contrato aprobado de 38 escenarios y la sección 8 de project-spec.md. Ponytail full y Caveman lite activos. Este agente mantiene E2E, smoke del publicador y evidencia de integración; no escribe producción backend. Por delegación explícita también implementó el módulo API frontend, cuya revisión independiente corresponde al coordinador; véase tdd_split_task_api.md. Se espera freeze explícito de ambos autores antes de construir imágenes.

## Preparación

`pnpm exec playwright test --list e2e/split-task.spec.mjs` descubre cinco recorridos nuevos: niveles y relación padre con DTO8/proyecto intactos; 21 hijos con paginación por padre y alta posterior; privacidad, sesión y CSRF/origen; recuperación de relación y lista durante POST; matriz de 22 anchos con teclado y feedback medido. Sólo se ha comprobado descubrimiento/sintaxis. No hay ejecución funcional ni RED/GREEN atribuido a este corte todavía.

Frontend confirmó labels: región Subtareas, campos de tarea reutilizados, Crear subtarea, Guardando subtarea, Subtarea guardada, lista Subtareas guardadas, Más subtareas antiguas, Volver a subtareas recientes y Reintentar subtareas. Relación distingue Tarea principal confirmada de error con Reintentar relación. El backend confirmó tasks.parent_id nullable y FK compuesta (project_id, parent_id).

El helper de tareas conserva exactamente los ocho campos y Location al crear raíces o hijos. El fixture usa sesiones reales y PostgreSQL; no añade CSRF global al BrowserContext. La matriz HTTP completa heredada sigue correspondiendo a tests backend, no se duplica cada entrada en navegador. Las pruebas de error de red y recuperación inyectan respuestas concretas, mientras persistencia, privacidad, paginación y confirmación usan API real.

El smoke conserva Created, Updated, StatusChanged y TaskCreated. Se prepara creación SubtaskCreated durante la misma caída Rabbit existente, selección por proyecto y taskId, esquema de nueve campos, parentTaskId distinto, recepción original en su cola quorum y retención tras el mismo reinicio con backend detenido. No se repite una matriz de crash sin cambio de transporte. No se ha ejecutado todavía el smoke del corte 8.

No se leen, eliminan ni reutilizan los dos archivos temporales previamente bloqueados. Los próximos runs utilizarán fixtures nuevos del runner estándar y limpieza acotada propia.

La preparación de la matriz incluye enlaces reales con títulos de un carácter (hijo A y padre P), además de todos los controles del main y el enlace al proyecto. El recorrido usa UUID en mayúsculas para comprobar el contexto canónico. La ausencia de raíz ante error y su recuperación usan el texto exacto Tarea principal confirmada. Estas comprobaciones aún esperan ejecución sobre el corte final congelado.

## Primer corte congelado ejecutado

Fixture nuevo organizationweb-e2e-18756, web DDEzlNbV. pnpm test:e2e ejecutó 38 casos: 37 verdes y un fallo histórico de teclado en read_projects (2,7 minutos). Los cinco recorridos split_task pasaron; matriz de 22 anchos, enlaces de un carácter y todos los controles del main sin desbordamiento, objetivos de al menos 44 CSS y axe sin violaciones en sus reglas. Feedback Guardando subtarea: 1 ms antes de liberar POST retenido. No se presenta 38/38 como resultado.

El fallo histórico buscaba Volver a proyectos con hasta 12 Tab sin esperar el foco del encabezado después de navegar. Se reemplaza por espera explícita del encabezado enfocado y Shift+Tab al enlace anterior, sin foco/click programático. Repetición acotada sobre las mismas imágenes en fixture nuevo, pendiente de resultado. El runner anterior limpió únicamente sus contenedores, red, volumen y scratch propios.

## Replay y evidencia visual

El replay read_projects pasó 1/1 en 17,5 segundos sobre las mismas imágenes, fixture nuevo 59252. Capturas y zoom nativo completos: zoom 2, innerWidth 320, scroll/client 312, enlaces/controles >=44 y subtarea creada por UI real. El fixture se limpió. La selección inicial de motores no descubrió tests por argumentos con espacios; sólo se corrigió el selector del helper. Nuevo fixture 18372 ejecutó Firefox/WebKit: 2/2 en 13,1 segundos, con jerarquía, DTO plano, navegación y recarga reales. Limpieza propia completada. La matriz y límites están en ux_split_task.md.

## Publicación y cierre de integración

pnpm test:publisher terminó EXIT 0 (sesión 32635). Las cinco rutas conservaron sus comprobaciones reales. SubtaskCreated: POST HTTP 201 con worker habilitado y Rabbit detenido; fila pendiente con reintento y parentTaskId/taskId distintos; aggregateId del proyecto; JSON original de nueve campos recibido tras recuperar Rabbit, mensaje persistente y cola quorum durable. Tras el mismo reinicio de Rabbit con backend detenido, se comprobó de nuevo el mensaje por eventId/payload de subtarea, además de la ruta histórica TaskCreated. No se añadió otro reinicio ni matriz de crash. El finally completó limpieza de los recursos propios.

Resultado final local: cinco E2E nuevos verdes dentro de 37/38 originales, replay del único fallo histórico 1/1, dos motores 2/2 y smoke completo EXIT 0. No hay hallazgo de producción abierto por integración. Los dos temporales bloqueados anteriores permanecen sin tocar. El coordinador mantiene la decisión global, el init y los commits.
