# Alcance PIT de importación23

Preparación aislada; no se ejecutó ninguna campaña. Tres focos separados con clases completas (incluidos internos por `*`), umbral80, cuatro workers y opciones PIT heredadas sin alterar mutadores, timeout ni exclusiones.

| Target del arnés | Scope Gradle | Patrones completos | Candidatos JUnit |
| --- | --- | --- | --- |
| import_data-reader-backend | import_data_reader | ImportJsonReader*, ImportReceiptDecoder* | ImportJsonReaderTest, ImportReceiptDecoderTest |
| import_data-http-backend | import_data_http | ImportDataController*, PreviewImportData*, ApplyImportData*, ReadImportReceipt* | ImportDataApiTest, PreviewImportDataTest, ApplyImportDataTest, ReadImportReceiptTest |
| import_data-persistence-backend | import_data_persistence | PostgresImportDataStore*, ImportRecordValidator*, ImportCounts* | ImportPersistenceTest, ImportWiringTest |

Reportes independientes `backend/build/reports/pitest-import-data-reader`, `-http` y `-persistence`. Ningún método/clase propio se excluye para aumentar score. Los tres focos usan los candidatos dedicados autorizados por root; no ejecutan ImportSocketTest por cada mutante. La configuración Nginx conserva sus oráculos socket reales y no se acredita con PIT. Las exclusiones históricas equals/hashCode/toString y FRECORD permanecen exactamente como antes.

El default conserva sus clases y tests previos y añade la unión de los nueve patrones. `core` ya incluye toda application/domain (también puertos/errores); se añaden candidatos adapter.Import*Test, adapter.persistence.Import*Test y adapter.config.Import*Test. Por tanto el default sí conserva candidatos de socket. No se cambia el foco frontend de B.

TDD: dispatcher RED0e3eae → GREEN752e58; selección/clases/default RED081914 → GREEN1a4882. Rechazo de tareas/argumentos inyectados inicialmente GREEN932352. Arnés final70/70 EXIT0 7843ac; Prettier verde45a697 y diffcheck limpio.

DSL real inspeccionada con tarea temporal externa de sólo lectura, sin ejecutar PIT: tres scopes EXIT0, logs import_pit_reader_inspect.log, import_pit_http_inspect.log e import_pit_persistence_inspect.log. Valores resueltos coinciden con clases, tests, reportdirs, workers4 y umbral80. Script operativo fuera Git: deployment-preparation/import23-pit-inspect.init.gradle.

Persistencia no está lista para campaña en este checkout: A continúa su freeze de Store/ImportRecordValidator y falta trasladar ese corte. El nuevo ImportCustomizationValidator solicitado por root aún no existe; su inclusión se revisará al disponer de la fuente real, sin declarar ahora cobertura por un patrón inexistente. Reader/HTTP esperan autorización de campaña y huellas de entradas. No integrar soporte en PRIMARY mientras B conserve su freeze de Stryker.
