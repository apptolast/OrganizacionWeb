# TDD del lector JSON de importación 23

Corte aislado posterior a ff7233e. Autoría limitada a ImportJsonReader y su prueba; no modifica el store, los modelos, los beans, HTTP ni Nginx. Se conserva Header y la lectura de dos argumentos para preview. El nuevo overload read(input, consumer, expectedSha256) compara el hash al EOF antes de validar el sobre. El consumidor sólo prepara filas; su validación de negocio pertenece a A.

## Evidencia por ciclo

Los archivos import_reader_NN_red/green.log y sus .exit conservan cada ejecución individual. Las variantes inicialmente verdes no se presentan como fallos.

| Ciclo | Comportamiento | Evidencia |
| --- | --- | --- |
| 01 | Overload: hash antes de counts discordantes | RED de compilación; GREEN |
| 02 | Campo exterior desconocido diferido | RED → GREEN |
| 03 | Fecha inválida después del hash | RED → GREEN |
| 04 | Counts con estructura inválida | 2 RED y 1 inicialmente GREEN; 3 GREEN |
| 05 | Format y schemaVersion obligatorios | 2 RED → 2 GREEN |
| 06 | Fecha inválida con hash correcto produce error de archivo | RED → GREEN |
| 07 | Tipos de metadata sin coerción | 2 RED y 2 inicialmente GREEN; 4 GREEN |
| 08 | Forma de data/colección después del hash | 3 RED → 3 GREEN |
| 09 | Las catorce colecciones, sin preparar desconocidas | 2 RED → 2 GREEN |
| 10 | Decimal ultrafino exacto para validación posterior | RED → GREEN |
| 11 | Exponente no representable después del hash | RED corregido → GREEN |
| 12 | Recuperación numérica conserva hermanos, sintaxis y duplicados | 4 inicialmente GREEN |
| 13 | Profundidad inclusiva 16 aun al recuperar fila | 1 RED y 1 inicialmente GREEN; 2 GREEN |
| 14 | Defaults Jackson de nombre/número/string no anticipan hash | 3 RED → 3 GREEN |
| 15 | Total compartido e inclusivo de 100.000 registros | 1 RED y 1 inicialmente GREEN; 2 GREEN |
| 16 | Fecha UTC6, rango y rechazo de normalización de segundo intercalar | 5 RED → 5 GREEN |
| 17 | Años extremos 0001 y 9999 válidos | 2 inicialmente GREEN |
| 18 | Cero exacto con exponente fuera de BigDecimal | 3 RED → 3 GREEN |
| 19 | SchemaVersion/counts aceptan enteros 1.0 y 1e0 | 2 RED → 2 GREEN |
| 20 | JSON sintáctico con raíz equivocada valida hash primero | RED → GREEN |
| 21 | Entero de 1.101 dígitos no se convierte ni prepara como BigInteger | RED → GREEN |
| 22 | Fracción larga no se redondea ni materializa arbitrariamente | RED → GREEN |
| 23 | Signo, ceros y cancelación exacta de exponentes largos | 4 RED → 4 GREEN |

El primer ensayo 11 usó 1e2147483648, que sí es representable con escala Integer.MIN_VALUE. La inferencia inicial de Infinity fue incorrecta: JShell verificó DecimalNode. Sus logs originales y el intento de configuración NaN se conservan como diagnóstico fallido, no como defecto de producto. El RED válido usa 1e2147483649; también se verifica el exponente negativo. No quedó ninguna política NaN añadida.

## Decisiones y límites

Jackson conserva la gramática, UTF-8 estricto, claves duplicadas y profundidad. Los límites internos de tokens se ajustan a 32 MiB; el contador real corta en el byte adicional. El consumidor recibe sólo nombres ASCII de las catorce colecciones. Desconocidos se consumen y se invalidan después del hash, sin pasar nombres arbitrarios a JDBC.

Los números se materializan exactamente. NumberFormatException se captura únicamente alrededor de readTree, fuera del consumidor; se consume hasta el contexto padre exacto de la fila y se comprueba el hash antes de devolver 400. JSON malformado, duplicados y profundidad no se ocultan durante esa recuperación. El cero se reconoce en la mantisa de un token ya validado, sin expandir exponentes.

Para tokens de más de 1.000 caracteres, la conversión arbitraria se evita antes de invocar BigInteger/BigDecimal. Los enteros léxicos tan largos no pueden ser valores durables válidos. En decimales se recorre la mantisa una vez, reteniendo como máximo 19 dígitos significativos (superset de long), y se acota el exponente respecto del máximo archivo. Se admiten ceros y enteros equivalentes largos; fracciones no cero o valores que no caben en ese superset se invalidan después del hash. No se cambia ninguna cadena de negocio. Los oráculos usan tokens modestos de más de 1.000 caracteres y cuentan filas preparadas; no se ejecutó un ataque de millones de dígitos. La ausencia de conversión grande se acredita además por la guarda anterior a la llamada nativa, no mediante una supuesta medición de CPU universal.

Estos tests de fila prueban framing y representación para staging, no su validez completa como proyecto o valor personal; ésta sigue siendo responsabilidad del store. No acreditan catorce colecciones importadas en PostgreSQL ni concurrencia, proxy o UI.

## Gate final

64/64 pruebas de ImportJsonReaderTest, cero fallos/errores/omitidas: import_reader_formatted_final.log y import_reader_final_xml.xml. EXIT 0, tool 9e016a. Spotless Java completo EXIT 0, tool 6e5e6c; git diff --check limpio. No campaña de mutación ni suite global ejecutada.

El primer selector relativo de Spotless fue rechazado y quedó SKIPPED; el log import_reader_final.log no acredita formato. Los dos hooks absolutos aplicaron formato y el posterior spotlessJavaCheck normal verificó el resultado. Se preservan todos los originales. El freeze enumera las huellas de este corte para revisión e integración de A.
