# Revisión de escala del importador 23

Revisión de sólo lectura sobre HEAD `336b153` y contraste con el WIP de A en PRIMARY. No se ejecutaron pruebas, SQL ni mediciones nuevas. Las observaciones siguientes están presentes en el commit y continúan en el WIP leído; A confirmó que staging por fila, índices temporales y prueba inclusiva real están en su cola. No son resultados de rendimiento medido ni una aprobación de escala.

## Límites y memoria

ImportJsonReader cuenta los bytes del InputStream antes de materializar el archivo completo y rechaza el registro exterior 100001 antes de consumirlo. Conserva un JsonNode por registro; no construye una lista Java de 100000 registros. El límite estructural es 16 contenedores. Esto acredita un diseño progresivo, pero no una cota pequeña por registro: uno solo puede aproximarse a 32 MiB antes de su validación de dominio.

PostgresImportDataStore.stage serializa ese registro con ESCAPE_NON_ASCII y hace un INSERT individual. No hay batch en el corte leído. Al introducirlo, el presupuesto debe acotar también los bytes serializados, además del número de filas: la representación escapada puede ser mayor que los bytes UTF-8 del archivo. Las tres lecturas JDBC posteriores usan fetchSize(1), evitando pedir explícitamente toda la colección en una lista, con coste de intercambios que corresponde medir. No se propone reemplazarlas por una materialización global.

## Consultas prioritarias

- `validateHistoricalRelations`: por cada sesión busca y suma intervalos de import_stage filtrados por sessionId; por cada proyección busca el máximo de cambios por blockId. Son subconsultas correlacionadas sin índice temporal en el corte actual.
- `validate`: el CTE recursivo reachable recorre tareas por parentId. Una colección ancha y una cadena profunda ejercitan costes distintos; el límite de profundidad JSON no limita por sí mismo la profundidad del grafo de tareas.
- Los joins padre/hijo de import_stage usan collection más payload.id/parentId/sessionId/blockId. No hay CREATE INDEX ni ANALYZE explícito en el staging actual; el plan efectivo debe comprobarse con el conjunto grande que A prepara.
- `compare` y varios INSERT SELECT / NOT EXISTS comparan la PK durable mediante `id::text` con el campo de staging. Esa expresión difiere de la clave UUID indexada; revisar el plan y, tras validación, considerar convertir el lado staging a UUID para conservar la búsqueda por clave nativa. No afirmo un plan concreto sin EXPLAIN.

La recomendación mínima es la que A ya está preparando: prueba inclusiva real de 100000 registros y frontera de bytes, batch limitado por filas y bytes, e índices dirigidos a las relaciones observadas. Mantener el resultado original y medir el cambio. No inventar un nuevo límite de producto ni ampliar todos los escritores. Las carreras, timeouts transaccionales y atomicidad permanecen bajo sus oráculos de backend.

## Coordinación y límite del dictamen

A recibió las consultas concretas, el detalle de los casts y el presupuesto de bytes del batch. B no editará Java ni duplicará esa prueba. La revisión no acredita todavía tiempo, RSS máximo, capacidad bajo concurrencia ni aceptación a través del proxy final; esos resultados dependen del corte final y las pruebas de escala. El frontend aprobado no se repitió.
