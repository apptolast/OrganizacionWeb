# Cierre del alcance PIT de persistencia de importación23

Delta aislado posterior a las campañas reader/HTTP aprobadas: se añade el patrón completo ImportCustomizationValidator* a importPersistenceClasses y los candidatos dedicados ImportCustomizationValidatorTest e ImportConcurrencyTest. Se conservan PostgresImportDataStore*, ImportRecordValidator*, ImportCounts*, ImportPersistenceTest e ImportWiringTest. Default hereda la unión y los candidatos Import*Test existentes. No cambian reader/HTTP, umbral80, workers4, mutadores ni exclusiones.

TDD de selección exacta: RED51321f (omisión detectada), GREENcd9a6d; arnés final70/70 EXIT0 196d86 y Prettier. DSL Gradle real inspeccionada sin ejecutar PIT, EXIT0 cc1a6d: cuatro patrones y cuatro candidatos resueltos, reporte dedicado intacto. Diff de producto/arnés únicamente3+3 líneas.

No se ejecuta campaña de persistencia: espera congelación final de A y revisión. La prueba de concurrencia está bajo revisión independiente y no debe trasladarse parcialmente. Reader/HTTP originales conservan sus entradas/resultados, sin nueva ejecución ni sumas de puntuaciones.
