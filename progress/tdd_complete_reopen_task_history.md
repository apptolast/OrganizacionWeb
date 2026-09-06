# TDD backend de lectura de historial — complete_reopen_task

Delegación explícita del coordinador. Ownership: dominio de historia/posición/página, TaskHistoryQueries, ReadTaskHistoryUseCase/ReadTaskHistory, PostgresTaskHistoryQueries y TaskHistoryController, pruebas propias. Backend principal mantiene V9, configuración y cambios de estado. El coordinador revisará este código; no se atribuye revisión independiente al autor. Ponytail full/Caveman lite.

1. s10/s29: primera ejecución de herramienta apuntó por error a backend/backend y no creó el test; No tests found no cuenta como RED de contrato. Corregida la ruta, el test real de 0/1/20/21 filas falló por seis símbolos ausentes. Se añadieron tipos y servicio mínimo; GREEN 4/4, sesión 72837 EXIT 0. Verifica propietario/proyecto/tarea/cursor reenviados, continuación por última versión con huecos y copia defensiva de lista. Gradle liberado al backend principal.

Preparado siguiente test HTTP de historial vacío con PostgreSQL real, sin ejecutarlo aún. El bean de aplicación lo registrará el backend principal al existir el adaptador. No se modifica V9 ni configuración por este agente.
2. s10 HTTP vacío: RED real en sesión 10584, HTTP 500 en lugar de 200 al faltar la ruta. Adaptador/controlador mínimos y bean registrado por el agente backend; GREEN sesión 13372 EXIT 0. Las ejecuciones siguientes usan `-I ../.e2e-work/history.init.gradle --project-cache-dir ../.e2e-work/history-gradle-cache`: salida `.e2e-work/history-build`, caché distinta y sólo tres clases de pruebas propias; toda producción se compila. La suite normal/CI no tiene exclusiones.

3. s10/s11: población de tres transiciones con versiones 1/3/5 y fecha idéntica dio RED (0 filas frente a 3), sesión 8745. Consulta PostgreSQL y DTO HTTP explícito de cuatro campos: GREEN 2/2, sesión 99713. La prueba elimina el outbox antes de leer; el historial no depende de esa tabla operativa.

4. s14/s18: RED 5 fallos, sesión 14388 (recursos inexistentes devolvían 200; tabla inaccesible daba 500). Comprobación de propietario/tarea y traducción DataAccessException a STORAGE_UNAVAILABLE: GREEN 7/7, sesión 19618. Cuatro fronteras de privacidad comparan cuerpo 404 completo; caída de tabla vuelve a restaurarse en finally.

5. s12/s29: RED por nextCursor nulo en primera página de 21, sesión 46316. Cursor de tres campos y continuación SQL por versión estrictamente menor: GREEN 8/8, sesión 82987. Prueba versiones con huecos, inserción posterior y ruta con UUID en mayúsculas compatible con cursor canónico.

6. s13/s30: RED 35 casos en sesión 10653, con 8 anteriores verdes. Validación de ruta antes de cursor, consulta cerrada, base64url canónica, JSON estricto y versión BIGINT positiva: GREEN 43/43, sesión 46641. Incluye las tres claves ausentes/duplicadas y las cinco raíces no objeto por separado. Sin cambios en filtros globales.

7. Verificación adicional sin cambiar producción: sesión ausente y expiración persistida real, límites positivos 1/Long.MAX_VALUE y lectura de reapertura con otra tarea coexistente. Todos verdes a la primera, sin inventar RED: sesión 93240 EXIT 0. Total propio: 48 HTTP y 4 de aplicación (52). Pendiente formato y revisión del coordinador; no es aprobación global.

8. Formato exclusivo de archivos propios y repetición completa: sesión 63690 EXIT 0, 48 HTTP + 4 aplicación, cero fallos/errores/omisiones. Un intento previo del init temporal falló por salto de línea ausente; se corrigió sin modificar configuración productiva. Fuente y tests liberados al agente backend para formato/suite conjunta. No se ejecutó PIT de este bloque antes de revisión.

## Trazabilidad de lectura delegada

| Contrato | Evidencia propia |
| --- | --- |
| s10 | Vacío confirmado; tres transiciones con cuatro campos exactos; dirección de reapertura y aislamiento de otra tarea. |
| s11 | Historial leído después de eliminar outbox; persistencia operativa y reinicio se verificarán en integración, no se atribuyen a esta prueba. |
| s12, s29 | Página de 20 entre 21, versión con huecos, cursor de tres claves, continuación tras inserción nueva; servicio 0/1/20/21 y copia defensiva. |
| s13 | 33 variantes adversas: contexto, base64, JSON y todas las claves/raíces exigidas, tipos, límites y consultas; límites positivos 1 y Long.MAX_VALUE. |
| s14 | Sesión ausente/expirada JDBC y cuatro recursos ocultos; cuerpo 404 idéntico al inexistente. La transición PUT pertenece al backend principal. |
| s18 | Tabla de historia temporalmente inaccesible devuelve 503, nunca lista vacía; finally restaura la tabla. Atomicidad de escritura pertenece al backend principal. |
| s30 | projectId/id mal formados preceden al cursor mal formado; ruta canónica y UUID mayúsculos válidos. |
| s31 | GET history 200/400/401/404/503 con no-store. GET/PUT status se verifican en la otra delegación. |

Informes de esta corrida: `.e2e-work/history-build/test-results/test/TEST-com.apptolast.organization.adapter.TaskHistoryApiTest.xml` y `TEST-com.apptolast.organization.application.ReadTaskHistoryTest.xml`. La revisión independiente corresponde al coordinador, dado que este agente escribió API frontend y lectura backend.
