# TDD frontend: dividir tareas

Contrato aprobado: `features/split_task.feature`, sección 8 de `project-spec.md`.
Ponytail full y Caveman lite leídos y aplicados. Base heredada: 371 pruebas frontend, lint y build verdes; no se repite el inicio completo.

1. `@s23`: enlace desde la lista plana. RED: no existía enlace con el título. GREEN: `RouteLink` conserva la ruta del proyecto y la tarea.
2. `@s23`: detalle por URL directa. RED: la ruta se interpretaba como detalle de proyecto. GREEN: lector de tarea y GET validado; 2 pruebas focalizadas verdes.

Trabajo en curso. Aún no hay corte integrable ni autorización de mutación. Integración posee la matriz UX y requiere congelación conjunta antes de reconstruir.
3. Detalle fallido: RED sin aviso y rechazo sin manejar; GREEN con aviso y reintento explícito (3 pruebas).
4. Relación raíz: RED sin GET; GREEN tras respuesta parent:null, sin afirmación anticipada.
5. Padre directo: RED mostraba raíz; GREEN con enlace al padre confirmado.
6. Relación fallida: RED sin recuperación; GREEN conservando detalle y reintento que confirma raíz (6 pruebas).
7. Hijos directos: RED por sección ausente; integración API y composición en curso. El módulo tasks-api.ts y sus pruebas específicas se ceden exclusivamente a integration_craftsman; UI y hooks siguen bajo este autor.


8. Creación de subtarea: RED sin confirmación específica; GREEN con POST dirigido al padre y respuesta visible, sin alterar estimación del padre.
9. Reapertura: RED sin acción en el contexto; GREEN reutilizando ProjectStatusControl y el ETag recibido.
10. Paginación: RED sin nombres de controles de subtareas; GREEN con cursor opaco y recuperación de recientes tras error, sin bloquear acciones del proyecto.
11. Respuestas antiguas de detalle: RED en éxito y rechazo bajo StrictMode; GREEN con guardas de cancelación antes de actualizar.
12. Relación antigua: RED en éxito y rechazo; GREEN conservando la relación vigente.
13. Guardado y recarga: RED sin espera específica; GREEN con una única escritura, confirmación preservada tras GET 503 y enlace a la tarea confirmada.
14. Recurso no disponible: RED con mensaje genérico; GREEN distingue 404 sin mostrar relación ni hijos.
15. Acceso por ruta directa: el primer doble usó una ruta de login incorrecta y se corrigió. RED real tras login redirigía a creación; GREEN admite únicamente la nueva ruta privada con dos UUID y sin consulta.
16. Conflicto del estado del proyecto: RED perdía el borrador al desmontar; GREEN recarga sólo el contexto del proyecto y conserva el formulario.
17. Fallo al recargar proyecto: RED ocultaba borrador y no permitía recuperación local; GREEN con error independiente, reintento y controles de estado bloqueados durante la consulta.
18. Foco de entrada: RED dejaba foco en body; GREEN enfoca h1 sólo si el usuario no eligió otro elemento y mantiene destino del enlace de salto con tabindex=-1.
19. Vacío de hijos: RED afirmaba que el proyecto no tenía tareas; GREEN distingue subtareas vacías y explica estimaciones independientes.
20. Espera de hijos: RED usaba el texto genérico; GREEN anuncia la carga específica sin bloquear proyecto ni formulario.

Regresiones de composición ya verdes al añadirlas, sin atribuir un RED inexistente: borrador exacto en 400/409/503/red (4 casos), revisión de PROJECT_COMPLETED y reapertura con ETag actualizado, 401 con SessionGate real, recuperación CSRF sin reenvío, navegación A/B con GET/POST pendientes, cierre y nueva sesión con 401 tardío de GET/POST, navegación multinivel por enlaces y validación/foco de controles reutilizados. Los dobles sustituyen fetch, no el cliente de sesión ni los hooks. La prueba de badInput usa la interfaz DOM de validez; integración comprueba navegador real.

Estado al primer corte: 35 pruebas de UI propias verdes; API delegada reporta 100 pruebas nuevas e históricas verdes, lint focal y tsc. Build de producción del autor verde. Suite frontend completa y lint global en ejecución. Ninguna mutación iniciada.

## Trazabilidad de frontend

- s23: detalle directo, relación, paginación, carga/errores independientes, enlaces y sesión al volver a la ruta.
- s24 y s38: error de relación no significa raíz; reintento confirma parent:null.
- s25: creación real en el cliente, representación y explicación de estimaciones independientes.
- s26 y s36: conservación de los tres campos, revisión deliberada, reapertura y ETag; recuperación de conflicto de estado sin desmontar el borrador.
- s27: 401 desmonta vista privada mediante SessionGate.
- s28 y s29: confirmación preservada tras error de GET y exclusión de doble envío.
- s30 y s31: solicitudes antiguas, navegación, cierre y nueva sesión.
- s32: etiquetas nativas, foco, validación y badInput. La evidencia de teclado y áreas táctiles reales pertenece a integración.
- s33: SCSS existente ampliado para enlaces de 44 × 44 y contexto de tarea. Matriz y zoom pendientes de integración; no se afirma prueba en dispositivos físicos.
- Validación y transporte de s1–s22, s34–s37: ver tdd_split_task_api.md y pruebas backend/integración según responsabilidad. Esta UI no agrega árbol completo ni campos al DTO.

## Corte congelado para revisión

Suite completa: 455/455 pruebas, 14 archivos, EXIT 0. Lint y build de producción verdes. La primera ejecución completa encontró una aserción que retenía el enlace de la tarjeta temporal mientras React lo reemplazaba por el enlace de la lista; se corrigió sólo la prueba para consultar el elemento vigente dentro de waitFor. La segunda suite completa pasó. No se cambió producción para esa corrección.

El coordinador aprobó el alcance de mutación y la revisión previa, pero pidió esperar su señal de carga antes de ejecutar Stryker. Integración verifica el corte congelado. La funcionalidad no está cerrada.

21. UUID de ruta en mayúsculas: hallazgo comunicado al coordinador y corrección autorizada antes de mutación. Se amplió el recorrido de conflicto/reapertura existente para usar UUID en mayúsculas. RED: el contexto del proyecto fallaba por comparación histórica estricta. GREEN: TaskReader usa projectId/id canónicos del DTO confirmado para contexto, relación, hijos y enlaces. La revisión explícita del 409 utiliza el mismo contexto canónico y el ETag actualizado. No se modificó readProjects histórico.

Congelación actualizada: lint, build y suite completa 455/455 en 14 archivos nuevamente verdes. El número de pruebas no cambia porque se amplió un caso existente. Perfiles de mutación preparados, sin ejecución hasta señal de carga del coordinador.

22. Recuperación tras pérdida de acceso: RED mostraba de nuevo la tarea retirada al limpiar el error 404. GREEN retira task/snapshot al reintentar, muestra carga y no conserva borrador privado; un GET 503 posterior sigue siendo error y un nuevo GET válido restaura detalle con foco. La recuperación independiente 412/503 del contexto del proyecto conserva su borrador.
23. Foco de relación: RED tras desmontar el botón de reintento; GREEN restaura la región local si el usuario no movió foco y anuncia el resultado confirmado. Dos casos distinguen foco abandonado y foco elegido.
24. Foco de recarga del proyecto: RED dejaba body al desmontar el control; GREEN restaura encabezado de tarea sólo si no existe foco elegido. Dos casos cubren ambas situaciones.
25. Regresión de contexto antiguo: éxito y rechazo tardíos del proyecto de una tarea abandonada no modifican el borrador actual ni muestran errores antiguos; ambos casos ya verdes con las guardas existentes.

Freeze agrupado: 462/462 pruebas frontend, 14 archivos, lint y build verdes. Son 42 casos UI de división de tareas y 49 nuevos casos API sobre la base histórica de 371. Ninguna ejecución Stryker iniciada aún. Los perfiles aprobados conservan su alcance tras estos cambios porque TaskReader/TaskParent se incluyen completos.


## Mutación y entrega

Campaña completa: 558/601 detectados, 92,85 %, EXIT 0; 41 Survived y 2 NoCoverage, sin timeout/error. Duración 15 minutos y 20 segundos. Se preservó el JSON original.

Se añadieron 13 ejemplos de prueba, sin cambiar producción: errores controlados de API, rutas parciales, privacidad del GET del proyecto, snapshot/carga durante recuperación, foco, estimación nula, segundo reintento y 401 global de una petición antigua. Un doble de GET aún no había registrado su función de resolución en el primer intento de suite; se corrigió esperando ese registro. Suite normal posterior: 475/475, 14 archivos, lint verde. El coordinador repitió independientemente la suite normal y confirmó 475/475. El build y la integración del código congelado siguen válidos; no se repitieron por cambios sólo de pruebas.

Replay focal: 56/58 detectados, 96,55 %, EXIT 0; dos supervivientes, sin NoCoverage/timeout/error, en 2 minutos y 2 segundos. La comparación exacta de fuente, archivo, ubicación, mutador y replacement confirma 24 originales ahora detectados. Quedan 12 equivalentes y 7 variantes permitidas, aceptadas por revisión independiente. Se conserva la puntuación global original; no se suman scores. Detalles y mapeo en `progress/mutation_split_task_frontend.md`.

Coordinador: JSON y emparejamientos verificados, dictamen final APPROVED. Archivos frontend e informes liberados para commit. No se modifica lifecycle ni se inicia la siguiente feature. Ponytail full y Caveman lite se mantuvieron activos, sin dependencias ni abstracciones nuevas.
