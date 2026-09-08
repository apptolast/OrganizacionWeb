# Recorrido TASK UI21

Único caso complementario autorizado tras el nominal PROJECT: `e2e/customization-task-ui.spec.mjs`, SHA256 `714BAD0847CD4326D13A3C22E51E82C7E38D94523AFBB6D22B464064C61DD557`. Inicialmente GREEN, sin inventar RED ni cambiar producto.

Se crean tres definiciones por UI desde detalle de proyecto; el detalle de tarea guarda NUMBER0, BOOLEANfalse y DATE2026-09-08 mediante controles reales. Desactivar/reactivar Cantidad confirma schema200, retira/restaura el control y conserva fila SQL completa de valores, componente values del ETag, otros tipos y fechas. Recarga final conserva los tres valores. Proyecto/tarea y outbox completos siguen iguales al snapshot anterior. Los waitForResponse observan upstream real; no hay respuestas simuladas ni confirmaciones fabricadas.

Comando: node scripts/e2e.mjs e2e/customization-task-ui.spec.mjs. EXIT0 5962df,1/1 PASS,6.7s caso/8.9s Playwright. Stack organizationweb-e2e-60808, puerto18080. Lifecycle retiró contenedores/red/volumen propios.577 inputs antes/después iguales (43f62f); log customization_task_ui_initial.log, exit y manifiestos independientes. Formato/node --check verdes.

Corte: HEAD2a4edb6, mismo snapshot parcial UI14 previamente revisado y backend nominal anterior al final1cd88e7. No incluye correcciones posteriores de recuperación GETschema ni foco/validaciones finales de B. Acredita únicamente partes nominales @s7/@s32, no UX completa, todos los tipos ni recuperación incierta. No se repitió PROJECT ni campaña global.
