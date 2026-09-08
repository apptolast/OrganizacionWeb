# TDD backend exportación22 — autor A

Checkout aislado OrganizacionWeb-export-data, contrato aprobado cc78396. Init oficial anterior EXIT0:2706Java/113suites,2241frontend/54archivos,61Node; evidencia externa export22-environment-results.json. COMMON/V14 no tocados.

## Corte nominal de entrega HTTP

1. @s1 documento vacío: ExportJsonWriterTest.s1_emptyAccountProducesTheClosedDocumentAndExactDownloadMetadata. RED compilación por ExportJsonWriter ausente2f3c58 (export_backend_01_red.log). GREEN4889eb (export_backend_01_green.log):14colecciones/counts, JSONcerradoUTF8, ownerUnicode, timestampµs, filename y longitud real. Writer mínimo sólo soporta cuenta vacía; aún sin PG ni límite de memoria.
2. @s1/@s14 PrepareExportDataTest.s1_s14_clockIsReadOnceAfterSnapshotAndTruncatedToMicroseconds. RED tipos ausentes13531d (export_backend_02_red.log). GREENe73e35 (export_backend_02_green.log): puerto recibe owner y proveedor de instante; no llamaClock antes del callback, truncaµs. Test puro demuestra delegación; snapshot/RR real y ejecución única del callback porPG siguen pendientes.

Formato focal: primer comando52a322 falló por argumentoPowerShell sin comillas; salida conservada export_backend_nominal_format.log. Corrección de invocación, sin workaround de reglas, EXIT0fc2d80. Regresión nominal tras formato2/2 EXIT0a26046 (export_backend_nominal_final.log).

Firmas públicas compilables: ExportDataUseCase.prepare(String owner) devuelve PreparedExport; PreparedExport.filename():String, contentLength():long, writeTo(OutputStream):void throws IOException. Sin acceso a buffer/JSONni dependenciaHTTP. ExportDataQueries.prepare(owner,Supplier<Instant>) mantiene lectura temporal delegada al snapshot futuro. No bean provisional ni tipo413 inventado; se entregará con el siguiente ciclo de límite.

Freeze7archivos en export_backend_nominal_freeze.json. A congela esas fuentes para revisiónroot/handoffC. Producto completo22 NO terminado: datos14familias/PG/recibos/integridad/ownership/bytebudget/cursor/RR/relojfallido/errores/wiring pendientes. No mutación ni campañasglobales.
Precisión de trazabilidad: reloj es @s14. Nombre corregido y foco2/2 verificado en export_backend_nominal_tag_final.log; sin cambio de asserts ni producción.

Corrección de evidencia de formato: el comando fc2d80 devolvióEXIT0 pero avisó que spotlessIdeHook requería ruta absoluta, por lo que NO acredita formato aplicado. Se conserva ese log. Selector explícito de sólo7paths en backend/.gradle/export-format.init.gradle; spotlessApply y spotlessCheck EXIT0 26a6a7, export_backend_format_actual.log. Diff y contenido confirman formatoGoogle real en writer/tests, sin editar WIPHTTP. Nuevo manifiesto export_backend_formatted_freeze.json; no tests repetidos por formato.

3. @s15 límite32MiB: ExportBufferTest.s15_acceptsExactly32MiBAndRejectsTheNextByteBeforeGrowing. REDe11672 tiposausentes; GREEN944458. Refactor writer al buffer segmentado, sin copia completa, foco2/2 EXIT0019213. Formato explícito cuatropaths/Check EXIT02a3cb8. Logs export_backend_03_*.log. ExportTooLargeException real sin dependenciasHTTP queda congelada para C; resto de límites/PG siguen pendientes.

4. @s1/@s4 PostgreSQL vacío: RED 335052, GREEN 211b65. Transacción read-only/RR y snapshot antes del callback temporal; consulta no inserta preferencias/eventos. No acredita aún carrera concurrente.
5. @s2 proyectos reales: RED 08cbfd (esperados 2, obtenidos 0); GREEN 5292e9. Selección por owner, UUID ascendente, campos cerrados, Unicode sin normalizar, long textual y año 0001 con OffsetDateTime. Cursor forward-only y fetchSize16. Se acotó el conteo del fixture vacío a su owner para evitar dependencia del orden entre casos.
6. @s15 más de 100000 registros: primer intento 84aa0a falló por genérico Mockito del fixture, corregido sin producción. RED funcional f86c87: faltaba excepción al exportar 100001 proyectos. GREEN 867f4d, foco de 6 pruebas. Conteo limitado en SQL antes de transferir payload o consultar Clock. Todavía sólo proyectos; se extenderá a las otras colecciones al incorporarlas.
7. @s14 fallo de Clock en PG: RED dae894, primer intento de implementación 5fa0ca tuvo error de compilación por reemplazo textual, preservado. GREEN fa1eeb tras corregirlo: StorageUnavailableException y transacción liberada; ExportTooLargeException conserva identidad.
8. @s14 Clock anterior a año 0001: RED ba3251, GREEN 7acca2. Guarda temporal en el proveedor del caso de uso.
9. @s14 Clock posterior a año 9999: RED e61544; GREEN en export_backend_09_green.log. Logs por ciclo export_backend_NN_*.log, sin campañas globales.

10. @s2 tareas/subtareas: RED 9dc4ae; GREEN 47bb6e. IDs parent/proyecto, estado completado, campos nulos y versión textual preservados.
11. @s2 historial de estados: RED bd6bb1; GREEN d7dafb. taskVersion histórico propio, transición y timestamp exactos.
12. @s2 disponibilidad: RED 757f0b; GREEN bd7c90. Presupuesto cero, máximo diario y versión Long.MAX_VALUE; ninguna preferencia virtual.
13. @s2 apariencia: RED 0bfbc6; GREEN fcf311. Preferencia durable cerrada sin owner duplicado.
14. @s6 configuración: RED 4d1f3a; GREEN ee9bc6. PROJECT antes de TASK, orden de columnas/definiciones e inactivos preservados.
15. @s6 valores de proyecto: RED bb395d; GREEN 56d84b. IDs ordenados, cero/false/null distintos de valor ausente e inactivos preservados.
16. @s2 valores de tarea: RED 4e8b61; GREEN 8cf33a. projectId derivado de la tarea y colección vacía persistida, sin esquema inventado.
17. @s7 bloque original: el log export_backend_17_red.log conserva AssertionFailedError y BUILD FAILED; export_backend_17_green.log conserva BUILD SUCCESSFUL. Incidencia de proceso: se inició GREEN antes de consumir el EXIT del RED y ambos Gradle se solaparon. No se reconstruye un RED artificial ni se atribuye un ciclo serial correcto. Ambos procesos terminaron; root pidió verificar una vez el caso sobre fuentes actuales. Esa comprobación se incluye junto a la siguiente proyección en export_backend_18_green.log. Los identificadores de sesión ya no estaban disponibles al recuperar contexto; no se inventan códigos EXIT individuales perdidos.
18. @s7 proyección cancelada: RED EXIT 1 bea338, AssertionFailedError por colección ausente (export_backend_18_red.log). Se incorpora lectura real con versión y todos los campos de intervalo explícitamente null cuando no existen.
18. GREEN serial EXIT 0 012d77: proyección cancelada y repetición del bloque original, 2 casos. Conserva todos los logs17.
19. Sesión legada: RED EXIT 1 310308, GREEN EXIT 0 f78e9c. Metadatos null y contador persistido sin completar ni acumular hasta exportedAt.
20. Intervalos: RED EXIT 1 cec1fc, GREEN EXIT 0 09d7fa. Orden numérico 2 antes de 10, acumulado persistido y cola abierta intactos.
Precisión literal de tags: los casos de reserva/proyección pertenecen a @s8; el de sesión legada a @s11. Sus nombres iniciales usaron s7/s8 por error de lectura y se corregirán en el refactor focal. Los asserts no cambian.

21. @s5 relación sesión/proyecto ajeno: RED EXIT 1 9717dc, GREEN EXIT 0 e34810. SQL comprueba identidad del propietario y contexto de tarea sin cargar datos privados; fallo deja fila y libera transacción.
22. @s15 conteo de sesiones: RED EXIT 1 c0a64e (se consultaba Clock); GREEN EXIT 0 7a7889, ambos casos de exceso. La consulta limitada suma ahora las catorce familias antes del payload.
23. @s16 exceso UTF-8 conocido: 2100 descripciones válidas de 4000 puntos Unicode, 33 600 000 bytes de descripción sin contar envelope. RED EXIT 1 c220ea; primer GREEN falló compilando porque LIMIT era privado (82f47c), log preservado. Constante compartida dentro del paquete y GREEN EXIT 0 573e09. SQL mide contenido textual exportable conocido antes de consultar Clock o transferirlo; no infiere 413 del JSONB bruto.

24. @s16 JSONB desconocido sobredimensionado: RED EXIT 1 01fbc1; GREEN EXIT 0 691e14. Guarda SQL antes de ObjectMapper/Store y Clock, responde 503. El nombre provisional incluía @s22 erróneo; corregido a @s16 en refactor.
Refactor de corte24 EXIT 0 e22f38: Spotless aplica y comprueba ocho fuentes propias, cuatro clases de tests verdes. ObjectMapper exacto reutilizado y fetchSize1 limita el lote a una fila; tags @s8/@s11 corregidos sin cambiar asserts. Log export_backend_24_refactor.log.
25. @s5 colección PROJECT con parent ajeno: RED EXIT 1 badef9; guardia de relación previa a payload, sin incorporar ni filtrar silenciosamente la fila.

25. GREEN EXIT 0 4936ba.
26. Refuerzo solicitado en revisión de buffer: primera ejecución GREEN EXIT 0 20775d, sin cambio productivo ni RED inventado. Patrón no uniforme con offset y longitud cruza dos límites de segmento, write(int) final y writeTo coinciden con bytes de referencia; un bulk sobredimensionado no modifica tamaño ni contenido. Log export_backend_26_initial.log.

27. @s2/@s9 cambios de bloque con mapper revisado 0264b94: RED EXIT 1 d8b0b1; GREEN EXIT 0 e2c51f. Contexto/clave de fila y receipt cerrado, versión textual, offsets de intención null, texto exacto y extras excluidos.
28. @s2/@s10 cambio PAUSE: RED EXIT 1 e85839; GREEN EXIT 0 6ca3c4. Contexto SessionStart procede de SQL real, revisiones/acumulado textuales y seis campos históricos comunes. Las catorce colecciones ya tienen rama de lectura; integridad final aún en curso.

29. @s1/@s2 wiring real: RED EXIT 1 eb6598 por ExportDataUseCase ausente en ExportDataController; GREEN EXIT 0 0d8335. ApplicationConfiguration registra PostgresExportDataQueries y PrepareExportData reales; proyecto propio exportado desde Spring y outbox intacto. Log export_backend_29_green.log.

Corte nominal de 14 colecciones y wiring: NO cierre funcional. Pendientes explícitos: comprobar relaciones restantes (valores TASK, cambios de sesión respecto a su owner y bloque/cambio respecto al bloque original), integridad de metadatos/fila, snapshot con escritor concurrente, fallo tardío y límite combinado inclusivo real. Los límites de JSONB bruto son guardas de memoria/503, no demostración de contenido exportable/413. C mantiene ownership del mapper revisado. No campaña de mutación ni init global ejecutados por A.

Freeze nominal wiring: export_backend_wiring_freeze.json, nueve fuentes. Formato real y ocho suites focales: 111 tests, cero fallos/errores/skips, EXIT 0 ac618c (34 s). XML originales copiados sin modificar a backend/.gradle/export-wiring-xml para que sobrevivan a siguientes ciclos. No se atribuye cierre de los pendientes declarados arriba.
