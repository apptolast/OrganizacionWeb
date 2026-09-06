# TDD frontend: crear tareas

Contrato aprobado: `features/create_task.feature`, sección 7 de `project-spec.md`. Ponytail full y Caveman lite aplicados. Baseline recibido: 384 pruebas backend y 260 frontend; no se repite init.

## Ciclos observados

1. s29: RED por ausencia de región Tareas. GREEN con carga independiente y acciones del proyecto disponibles.
2. s19/s29: RED por ausencia del estado vacío y campos. GREEN con formulario nativo y colección vacía explícita.
3. s20/s23: RED por ausencia de lista confirmada. GREEN con título, criterio, estado pendiente y estimación diferenciada de tiempo trabajado.
4. s25/s29, dos casos: RED con HTTP 503 y fallo de red; GREEN con error independiente y recuperación GET deliberada, sin falso vacío.

5. s26: RED sin envío ni confirmación. GREEN con POST 201 y consulta posterior de la colección.
6. s27: RED sin manejo de errores 400/503/red. GREEN con borrador exacto, mensajes locales y ningún reenvío automático.
7. s28: RED con doble envío y sin espera. GREEN con bloqueo y estado Guardando tarea.
8. s1: 18 respuestas inválidas aceptadas en RED; GREEN al validar los ocho campos, identidad del proyecto, estado, tipos, límites Unicode y fechas.
9. s20: nueve colecciones malformadas aceptadas en RED; GREEN con validación de envoltura, tamaño y cursor.
10. s30: RED mostraba creación para completed. GREEN conserva lista y recupera formulario tras reapertura real en la composición App.
11. s21/s35: RED sin paginación. GREEN con cursor opaco, páginas separadas y borrador conservado.
12. s33: cinco casos RED de validación sin error ni foco. GREEN con límites por puntos de código y estimación entera, mensajes asociados y foco en primer campo.
13. s31: RED sin AbortSignal de lectura. GREEN aborta al navegar y descarta respuesta tardía.
14. s32: RED sin AbortSignal de escritura. GREEN con SessionGate real: salir cancela POST y un HTTP 401 tardío no revoca la sesión vigente.

15. s26: RED perdía la tarea confirmada tras GET fallido desde página antigua. GREEN conserva respuesta 201 en tarjeta y consulta la primera página, sin falso fracaso de escritura.
16. s14: RED sin recuperación de PROJECT_COMPLETED. GREEN conserva borrador, bloquea nuevo POST y revisa GET sólo al pulsar; actualiza proyecto y ETag juntos y permite reabrir.
17. s7: RED confundía validity.badInput con estimación opcional vacía. GREEN bloquea la entrada numérica incompleta y enfoca el campo.
18. s27/s33: RED sin errores del servidor asociados. GREEN reconoce campos permitidos y enfoca el primero, sin mostrar mensajes privados.
19. Refactor después de GREEN: estado y transporte trasladados a use-project-tasks; paginación y recarga limpian errores. La extracción produjo un error de sintaxis temporal, corregido antes de continuar; suite focal verde. Una aserción esperaba un nodo transitorio entre tarjeta y lista; ahora espera la fila vigente.
20. s33: RED perdía foco al desaparecer el botón de página. GREEN devuelve foco al encabezado sin desplazar un campo que el usuario haya elegido.
21. s1: RED aceptaba fechas iniciales distintas. GREEN exige createdAt = updatedAt en el POST confirmado.
22. Regresiones añadidas ya verdes: Unicode suplementario y texto literal, 401 vigente con SessionGate real, recuperación CSRF sin reenvío, revisión GET fallida con borrador, recuperación de página fallida, límites 1/1440/null y página completa de veinte registros.

## Corte para revisión

Lint verde; suite completa: **331 pruebas en 11 archivos**, incluida la base histórica de 260 y 71 nuevas. Build TypeScript/Vite verde. Primer recorrido real de creación/lectura de integración verde; matriz final y dispositivos siguen pendientes. Las suites históricas aíslan únicamente el nuevo componente, con autorización del coordinador; esta suite cubre composición real con App y SessionGate.

## Trazabilidad de interfaz

| Escenario | Evidencia frontend |
| --- | --- |
| s26 | POST confirmado, lista posterior y tarjeta persistente si GET falla. |
| s27 | 400/503/red conservan valores exactos; errores de campos y mensajes locales; no POST automático. |
| s28 | Estado de espera inmediato, botón bloqueado y segundo submit ignorado. |
| s29 | Carga/vacío/error independientes; acciones de proyecto disponibles; recuperación de página. |
| s30 | Lista visible en completed; creación ausente; reapertura recupera formulario y borrador. |
| s31 | Navegación aborta GET y descarta respuesta antigua; separación por proyecto. |
| s32 | SessionGate real retira lista/borrador ante 401; POST antiguo abortado no invalida otra vista. |
| s33 | Campos con labels, errores asociados, foco inicial, paginación y recuperación; no robo de foco. |
| s34 | SCSS adaptable y texto seguro; mediciones reales a cargo de integración, no se afirma cobertura física. |
| s35 | Cursor opaco, páginas separadas y retorno a recientes sin POST; recarga real verificada inicialmente por integración. |

s1–s25 se comprueban donde son observables en cliente (DTO, Unicode, HTTP, CSRF y confirmación); transacciones, propiedad, eventos y carreras SQL pertenecen a backend/integración y no se simulan como evidencia del navegador.

Mutación: archivos completos tasks-api.ts, task-validation.ts, use-project-tasks.ts y project-tasks.tsx. Incorporados al alcance predeterminado y a stryker.create-task.config.json. Revisión previa APPROVED e init conjunto 74002 verdes; coordinador autorizó la campaña de 504 mutantes, actualmente en ejecución, sin excluir ramas ni bajar el umbral 80.

Integración comunicó posteriormente 32/32 recorridos finales, 2/2 recorridos en otros motores, zoom real y smoke verdes. La evidencia y los límites físicos se mantienen en sus informes independientes; no sustituyen el resultado de mutación pendiente.

## Revisión de mutación y carrera corregida

La primera campaña detectó 402 de 504 mutantes (79,76 %, EXIT 1). Se añadieron pruebas de los huecos observables, conservando el informe original y clasificando cada superviviente.

Al revisar el contador de recarga se reprodujo una carrera real: GET inicial fallido, POST pendiente, reintento GET completado y después POST 201. La actualización capturada `setRevision(revision + 1)` coincidía con el contador ya actualizado por el reintento y dejaba la lista cargando sin nuevo GET. RED observado; GREEN al cambiar únicamente a actualización funcional `setRevision((value) => value + 1)` en use-project-tasks.ts:81.

Lint, 366 pruebas y build verdes tras ese cambio. El coordinador volvió a revisar y ejecutó init 73511 (486 backend y 366 frontend); integración reprodujo el caso real sobre la nueva imagen, 1/1 verde. Una prueba adicional de ambas operaciones en el mismo ciclo produjo inicialmente un fallo de fixture por reutilizar un Response consumido; corregido el fixture a una respuesta nueva por petición. Suite posterior: **367 pruebas y lint verdes**, sin más producción modificada.

Segunda campaña autorizada con cuatro archivos completos. El modo incremental leyó los 504 mutantes originales pero Stryker decidió reutilizar **0 de 505** resultados actuales por sus diferencias de fuente/pruebas; ejecuta los 505. El resultado final figura a continuación; no se infiere sumando campañas.

## Cierre del autor

Segunda campaña completa: **480/505 = 95,05 %**, EXIT 0; replay focal posterior: **16/16**, EXIT 0, ambos resultados separados. Se detectan los últimos huecos de tarjeta paginada, status GET y cancelación del contador dentro de un lote React. Esta última prueba usa confirmación y acción DOM pública antes del commit; no se afirma una reproducción de ese mutante en navegador ni un nuevo defecto de producción.

Suite final: **371 pruebas en 12 archivos**, lint verde. Build y prueba de carrera real sobre la única línea de producción corregida ya estaban verdes; después sólo se añadieron pruebas y documentación. Los 102 supervivientes iniciales y los restantes finales están clasificados en mutation_create_task_frontend.md. Dictamen independiente final APPROVED. Fuentes, pruebas, configuraciones y bitácoras del frontend quedan liberadas al coordinador para commit; no se modifican aquí metadatos globales ni se inicia otra feature.
