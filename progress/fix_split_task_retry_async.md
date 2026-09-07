# Sincronización del fixture de reintento de tarea

RED real del init integrado6643 (`9dc3e6`), preservado en `weekly_review_closure_init.log`: 1/1.965 falla con `finish is not a function` en el caso «retira snapshot anterior hasta confirmar proyecto después de un reintento de acceso». La ejecución aislada del original fue inicialmente GREEN (`2aa765`, 1/1); se registra la fluctuación sin fabricar un scheduler ni aumentar timeout.

La causa está en la precondición del fixture. Reintentar tarea limpia task/snapshot y vuelve a leer la tarea. Al resolver GETtask aparece su encabezado con «Consultando proyecto». El efecto dependiente de task inicia después GETproject, y sólo su override asigna `finish`. Esperar el encabezado y el estado de carga no garantiza que ese efecto haya entrado ya en el mock.

Cambio mínimo autorizado: una línea `await waitFor(() => expect(finish).toBeTypeOf("function"))` inmediatamente antes de entregar la respuesta. Mantiene todas las aserciones anteriores: snapshot/acciones retirados, alert ausente y borrador vacío tras confirmar proyecto. Reutiliza el patrón existente en esta misma suite, con timeout predeterminado. No cambia producción, mocks de otras rutas ni otras pruebas.

Suite focal52/52 GREEN `689c9f`. Regresión frontend completa y lint en curso al redactar este apartado; evidencias finales se añaden abajo. Manifiesto previo `weekly_review_split_retry_before.json`,141 entradas. La campaña Stryker original ya había terminado y preservado su after idéntico antes de este cambio de test heredado; no se reescribe su evidencia ni se relanza automáticamente.

Resultado final: frontend **1.965/1.965 en 42 archivos**, ESLint y Prettier completos GREEN, EXIT0. Logs `weekly_review_split_retry_global.log` (25,47 s de suite) y `weekly_review_split_retry_lint.log`. Comparación `25c57f`: 141 entradas, cambia exclusivamente `frontend/src/split-task.test.tsx`, SHA256 `FB6D0343BE86253C0ADBDF3E0945B6C5C5FDF3AF4F98200D5EF58C74AD885704`. Diff de una línea; fuentes productivas intactas. Before/after preservados en los dos manifiestos de este ajuste. Congelado para revisión del coordinador; sin campañas adicionales.
