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
