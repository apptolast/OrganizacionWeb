# TDD backend import_data23 — A

Contrato42 escenarios aprobado en5feebd7. Baseline install/init23 verde (2846 Java/123,2295 frontend/56,66Node). A posee aplicación/decoder/PG/V21/wiring y pruebas propias; C HTTP aislado, B frontend. No campañas globales ni PIT por ciclo.

## Ciclo1 — puerto de preview (@s1/@s18, nominal parcial)

RED9dfef0: PreviewImportDataTest no compila por cinco tipos ausentes. GREENa065c9: una prueba pura confirma que la identidad/stream llegan una vez al puerto de consulta y que el resultado preparado vacío vuelve intacto sin consumir el stream en la orquestación. El fixture del puerto no es un archivo JSON ni su hash calculado: no acredita parsing, límites, integridad, snapshot PG ni ausencia de escrituras reales. Durante compilación se corrigió el tamaño literal del fixture de21 a20; no se atribuye un RED de negocio a esa corrección.

Tipos reales: ImportDataUseCase.preview(String, InputStream), ImportDataQueries misma firma, PreviewImportData, ImportPreview (incluido RunningSession), ImportCounts cerrado14campos. No métodos apply/receipt futuros ni bean provisional. El caller HTTP cierra su stream; decoder/adaptador A contará bytes y verificará UTF8/duplicados/hash, sin @RequestBody materializado.

El primer comando Spotless767321 devolvió0 pero avisó que el selector requería path absoluto: no seleccionó fuentes y NO acredita formato. Se conserva log. Selector de seis paths exactos en backend/.gradle/import-format.init.gradle, ejecución realfb6f66 aplica/verifica GoogleJavaFormat. No cambios productivos de22.

Logs originales externos en work/deployment-preparation/import23-cycle01-*.log. XML y manifiesto de este corte preservados en progress/import_reading_checkpoint/. Siguiente ciclo: decoder de archivo vacío real, y después fronteras de archivo e integración PG.

## Mapa de alcance

@s1/@s18: orquestación nominal anterior, pendientes persistencia/apply/snapshot. @s2–@s17 y @s19–@s27: pendientes backend. @s28–@s42: fronteras HTTP/proxy/UI y aceptación compartidas según contrato; no se consideran cubiertas por el primer test.

## Ciclos2–4 — decoder nominal y errores reales

Ciclo2 @s1/@s18: REDcf2afd clase ausente; GREEN6cd4e0, exportación vacía real producida por ExportJsonWriter, SHA/tamaño incluyen whitespace final, propietario/fecha exactos y stream del caller no cerrado (override observable). El parser recorre tokens sin árbol global. data aún se salta: NO acredita registros no vacíos ni esquema íntegro.

Ciclo3 @s16/@s17: REDdc7276 por excepción ausente; GREENcc3376 (dos casos). Stream sintético ilimitado de espacios exige ImportTooLargeException y consumo exactamente33554433bytes, sin almacenar ese contenido. El wrapper limita cada read a presupuesto restante más el primer byte de exceso.

Ciclo4 @s8: RED1c45fb por error ausente; GREEN247447 (tres casos). Campo exterior desconocido exige ImportInvalidFileException con mensaje seguro sin payload. Formato real de cuatro paths e6204d. Esas dos excepciones se entregan para HTTP; validación restante, callbacks de filas, PG y beans siguen pendientes. No se presenta este checkpoint como parser completo. Logs externos import23-cycle02/03/04-* preservados.
