# Recibos durables de export_data

Trabajo C en export-http después del freeze HTTP aprobado 7fbb347. Sólo la clase
ExportReceiptWriter y su prueba son nuevas. A conserva SQL, propiedad, relaciones,
límites de lectura y bytes, snapshot y campos exteriores. Las dos firmas acordadas
escriben directamente al JsonGenerator acotado de A; no crean otro buffer de archivo.
Se materializa únicamente el árbol de un recibo cuyo tamaño A limita previamente.

No se rehidrata PlannedBlock: su constructor rechaza offsets de intención null
y BlockRequest normaliza objetivos. La lectura cerrada conserva los valores
históricos y emite sólo campos conocidos; los objetos desconocidos no se vuelcan.
WorkSessionState y SessionStart son records puros usados para comparar transiciones.
WorkSessionCloseNotes valida textos ya no nulos sin alterarlos; se escribe el texto
original. No se consulta TZDB ni se recalcula workDate.

## Ciclos focales

Cada fila corresponde a un test de comportamiento añadido antes del cambio,
ejecutado por gradlew.bat test --tests con su método de ExportReceiptWriterTest.
Las variantes agrupadas usan la misma acción y fixture. No representan integración SQL.

| Oráculo | RED EXIT1 | GREEN EXIT0 |
| --- | --- | --- |
| Bloque real RESCHEDULED con dos snapshots completos y long textual | 9baf18, clase ausente | 60e4dd |
| Identidad/contexto/metadatos del bloque coinciden con fila propia | 3279cb | 0df46b |
| CLOSE conserva notas, fecha civil y revisión larga | dc162f, método ausente | 9415ee |
| PAUSE/RESUME sin closure y EXTEND con detalle propio | 613aff | 9a7916 |
| Fila de sesión y los siete campos del inicio original | 7ff58e | 5a970b |
| JSON null/duplicado/trailing y tipos sin coerción | c83809 | 59fc7b |
| Kind/after y coherencia de offsets/locales/duración/precisión/rango | d93658 | 3ca2c5 |
| Revisión/estado/contador e instante de transición coherentes | 269f58 | ee5141 |
| Notas/fecha/zona de CLOSE y fórmula/rango de EXTEND | f3d8fa | f5db6d |
| CANCELLED, offsets de intención null, texto exacto y extras omitidos | — | cbc0a9 inicialmente GREEN |
| plannedMinutes no coincide mediante desbordamiento de int | 04c5f3 | 2c8496 |
| Instante inválido usa excepción traducible a503 por A | 83b58b | 63ff9f |

Los rechazos de corrupción anteriores verifican que no se ha emitido contenido
del recibo. El resultado conserva IOException para JSON inválido y
IllegalArgumentException para incoherencia; A los traduce a503 sin datos internos.
La comparación SessionStart no se limita a IDs: incluye inicio, duración, fin
original y zona de ambos snapshots. No se actualizan contadores hasta la fecha
de exportación ni se convierten offsets de intención null en offsets resueltos.

## Verificación y límites

Regresión de clase: 55/55 y spotlessCheck EXIT0 c494d6, log
export_receipt_final_confirmed.log y XML export_receipt_final.xml preservados.
Los comandos de formato anteriores se conservan: 65cde4 falló por argumento
PowerShell; c31f87/dd4fed detectaron que el selector relativo/regex no había
formateado los archivos (la clase estaba verde). El hook exige ruta absoluta.
ae8f90 aplicó formato individual a las dos rutas absolutas; c494d6 es el cierre
real, sin convertir intentos anteriores en GREEN global.

HTTP permanece congelado, verificado contra sus cinco hashes. No se ejecutó
PIT, regresión global, SQL, navegador ni despliegue. Aún requiere revisión root
y la integración de las dos colecciones de recibos por A; el presente corte no
acredita exportación de cuenta completa.
# Refuerzo posterior: identidad de creación del bloque

Tras el corte revisado 19e8ece, se contrastó `MoveBlock`: el nuevo `PlannedBlock` conserva literalmente `prior.createdAt()`. Por ello, un recibo RESCHEDULED con instantes de creación distintos es incoherente. No se impone `occurredAt >= createdAt`: `BlockState.cancel` no exige un reloj monótono y esa restricción podría rechazar historia válida.

Un único test cambia el `createdAt` de `after` un segundo y exige rechazo antes de escribir bytes. RED real e05de3 (EXIT 1, «Expecting code to raise a throwable»), preservado en `export_receipt_created_at_red.log` y `.xml`. El guard mínimo compara ambos instantes sólo cuando existe `after`. Regresión de la clase y spotlessCheck: EXIT 0 b735d3, 56/56 sin fallos, XML y log `export_receipt_created_at_green.*`. Los cinco archivos HTTP permanecen sin cambios.

La primera tentativa PIT (`export_http_receipt_pit_original.log`, EXIT 1, 3m23s) falló durante los tests sin mutación: los contextos Spring del checkout nominal carecían del bean real `ExportDataUseCase`. Root y A confirmaron la misma causa mediante XML del ciclo 29 en primaria; no se repite un diagnóstico equivalente. No existe puntuación ni universo de resultados PIT válido. Las 443 entradas originales permanecieron idénticas en `export_http_receipt_pit_before.json` y `export_http_receipt_pit_after.json`, capturadas antes de este refuerzo. No se excluyen suites ni se añade un bean ficticio; la campaña espera la integración revisada del wiring real.
