# TDD cliente API — complete_reopen_task

Contrato aprobado leído, sección 9 y baseline del coordinador. Ponytail full/Caveman lite. Ownership tasks-api.ts y task-status-api.ts con sus pruebas dedicadas; UI/hooks pertenecen al autor frontend. Snapshot pactado plano: status, completedAt, updatedAt y etag; los tres campos HTTP permanecen cerrados. El coordinador revisará este código porque este agente lo escribe. Sin mutación hasta su aprobación.

Comando focal por ciclo: pnpm --dir frontend test -- task-status-api.test.ts.

1. s1: import ausente produjo RED real (suite no resolvía módulo). GET mínimo con cookies, no-store, signal y ETag produjo GREEN 1/1.
2. s14/s18: siete HTTP distintos de 200 con cuerpo válido resolvían incorrectamente; RED 7/8, guarda HTTP exacta, GREEN 8/8.
3. s34: dieciocho cuerpos incompatibles (forma/campos/estado/fecha/invariantes) producían éxito; RED 18/26, validación de tres campos y UTC hasta microsegundos, GREEN 26/26. Rechazo usa error controlado, no TypeError.
4. s34: catorce ETag inválidos/ausentes/ajenos no se rechazaban; RED 14/40, validación fuerte canónica con BIGINT sin conversión a Number, GREEN 40/40. Identidad de ruta se compara semánticamente; el tag emitido debe permanecer minúsculo.
5. Verificación positiva de fechas UTC con/sin fracción, ruta en mayúsculas y ETag BIGINT máximo: 3 casos ya verdes, total 43; no se inventa RED sobre validación correcta.
6. s2/s3: export changeTaskStatus ausente, RED 2/45; PUT mínimo reutiliza parser de snapshot, manda sólo status/If-Match y CSRF real del cliente común; GREEN 45/45. s23: respuesta válida que confirma otro estado produjo RED 1/46; comprobar intención confirmada, GREEN 46/46.
7. s10: readTaskHistory ausente, RED 1/47; GET mínimo con sesión, no-store y señal, GREEN 47/47.
8. s12/s29: cursor no se enviaba, RED 1/48; query codificada sin interpretar cursor, GREEN 48/48. Verifica veinte entradas en el orden confirmado.
9. s14/s18: siete HTTP de historia se aceptaban como vacío; RED 7/55, status exacto, GREEN 55/55.
10. s11/s29: 26 cuerpos/páginas/entradas incompatibles aceptados; RED 26/81, validación de dos campos de página, límite 20 y cuatro campos de transición con estados distintos/UUID/fecha, GREEN 81/81.
11. s9: cuatro lecturas completed fallaban realmente en task-status-compatibility.test.ts (4/4 RED). Ampliación compartida de Task/isTask a pending/completed resolvió esas cuatro y reveló el rechazo histórico de POST completed (1/55 RED en suite combinada). Guarda explícita pending sólo en createTask restauró GREEN 55/55, sin relajar creación ni DTO8.
12. Regresión adicional, ya verde sin cambiar producción: POST raíz/hijo rechaza completed; PUT conserva HTTP/error/identidad e intención, no-op permite mismo ETag; red y JSON truncado no provocan reenvío. Los errores de shape se expresan como Error controlado; JSON ilegible conserva SyntaxError, nunca TypeError por acceso indebido.

## Handoff del módulo

pnpm --dir frontend test -- task-status-api.test.ts task-status-compatibility.test.ts tasks-api.test.ts split-task-api.test.ts: **209/209 verdes**, cuatro archivos. Incluye 100 de estado/historia, seis de compatibilidad y 103 regresiones anteriores. ESLint de los cuatro archivos propios y tsc --noEmit: EXIT 0. Formato aplicado. No se ejecutó mutación ni build Docker.

Exports pactados: TaskStatus, TaskStatusSnapshot plano, TaskHistoryEntry, TaskHistoryPage, readTaskStatus(projectId,id,signal?), changeTaskStatus(projectId,id,status,etag,signal?) y readTaskHistory(projectId,id,cursor?,signal?). No paquetes nuevos, reintentos automáticos ni otro estado de sesión. Cursor y ETag se devuelven sin normalizar ni convertir versiones a Number. La comparación del tag con la ruta usa UUID semántico; su representación emitida sigue siendo canónica.

Los tests de API usan fetch controlado; no constituyen evidencia de PostgreSQL/RabbitMQ ni de recorrido navegador. Integración esperará freeze conjunto. El coordinador revisará estos archivos y autorizará mutación; no se declara aprobación independiente del código escrito por este agente.