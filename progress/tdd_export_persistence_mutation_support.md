# Soporte PIT de persistencia de exportación

Selector fijo `export_data-persistence-backend` del dispatcher: invoca exclusivamente `pitest --no-daemon -PmutationScope=export_data_persistence`. No acepta flags inyectados, sufijos ni tareas distintas de mutate. No se ejecutó ninguna campaña.

Alcance confirmado con A: clases completas `PostgresExportDataQueries*`, `ExportJsonWriter*`, `ExportBuffer*`, `PrepareExportData*`, puertos `ExportDataUseCase*`, `ExportDataQueries*`, `PreparedExport*`, `ExportTooLargeException*` y `ApplicationConfiguration*`. Incluye internos y wiring completo, sin recortar métodos o guardas. Los puertos y la excepción se conservan aunque el motor no genere mutantes para alguna clase. Los candidatos de pruebas son todos `com.apptolast.organization.*`.

Las tres clases C `ExportDataController*`, `ExportHeadersFilter*` y `ExportReceiptWriter*` quedan fuera de este selector positivo disjunto; ya tienen una campaña propia revisada. El default conserva íntegramente la unión previa y añade persistencia y las tres clases C. Añade los patrones de tests de aplicación/exportación, HTTP, persistencia y wiring; éstos incluyen `ExportSocketTest`, `ExportWiringTest`, `ExportDataHttpPersistenceTest` y el mapper. No se modifica la configuración de mutadores, exclusiones históricas, timeout, umbral 80 ni cuatro workers. Reporte separado `reports/pitest-export-data-persistence`.

TDD del arnés, un ciclo cada vez:

- Dispatcher: RED dc4124 (target desconocido), GREEN 7c465e tras añadir sólo selector y llamada fija.
- Alcance/default/candidatos: RED cdbae1 (scope ausente), GREEN e23613 con las familias completas y default ampliado.
- Rechazos anticipados: inicialmente GREEN d0eba1, sin llamadas al runner.

Formato de ambos scripts y regresión íntegra del arnés: 64/64, EXIT 0 8f8ada. Evaluación real del DSL mediante `gradlew help -PmutationScope=export_data_persistence`: EXIT 0 f2fc23, sin ejecutar test/PIT. Diffcheck limpio. No implica que el backend final de A ni una campaña de persistencia estén aprobados.

Leído únicamente como referencia el dispatcher de B en commit 4666401, que añade `export_data-frontend`. No se copia ese branch ni su configuración a este aislado. La modificación C se inserta junto a comandos backend y al final de la allowlist, para conservar el hunk frontend de B al integrar. Root debe unir ambos cambios en su ventana después de Stryker. No hay transferencia a primaria ni cambios de producto.
