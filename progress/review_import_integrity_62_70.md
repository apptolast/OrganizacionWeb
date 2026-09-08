# Revisión independiente de integridad de importación

**Corte:** e23568fa9aaa761a16ca8004774fe2aa1d2cd08e, ciclos 62–70. Lectura de los objetos Git fijados, no del trabajo posterior de escala. **Dictamen parcial final: APPROVED**, tras el cierre EXTEND congelado descrito al final. No es un dictamen global de la feature.

## Evidencia y alcance aprobado

- Freeze `import_integrity_final_freeze.json`: SHA256 `486391e81e1634882016cd9b503c96013ab225c2eac339ad3babe2024e39d859`. Root verificó los cuatro blobs Java y los XML originales: 130 casos de persistencia y uno de wiring, cero fallos, errores u omitidos; EXIT 0 `32c804`. La diferencia EOL de la bitácora no es un fallo de pruebas; se conserva el original externo.
- Las nuevas restricciones de tareas, revisiones, proyecciones y fin efectivo coinciden con los CHECK durables de V9, V13 y V18. Se conservan las proyecciones históricas con todos los campos temporales nulos; no se reconstruye TZDB ni se normalizan offsets.
- Los oráculos de coherencia comparan el objetivo y creación del bloque, la proyección con su último recibo y los cinco campos de estado de sesión con el último `after`. El caso de proyección completa distinta del recibo tiene un RED real. El fallback de una cancelación con tiempos nulos conserva la planificación original.
- Cuotas de activos y unicidad de sesión abierta se calculan sobre la unión del destino y las filas ausentes del archivo. Las diez variantes de claves alternativas internas se rechazan antes de escribir. Las advertencias RUNNING incluyen únicamente nuevas sesiones y conservan `runningSince` legado nulo.
- Los siete casos de recibos operativos corruptos exigen 503 y preservan físicamente el recibo: forma y tipos de contadores, límites, resultado y fecha. No se devuelve una confirmación fabricada.
- El oráculo de rollback comprueba, dentro de la misma transacción, que se han insertado las 14 colecciones antes del fallo SQL al insertar el recibo. Después compara todas las filas y sus identificadores físicos, outbox y ausencia del recibo. Es un fallo comprobable con rollback, no una pérdida indeterminada de respuesta de COMMIT.
- La bitácora conserva RED y GREEN reales, incluido el intento de edición sin efecto y el log intermedio que seguía rojo; el caso de rollback ampliado se registra inicialmente GREEN.

## Único punto pendiente

`PostgresImportDataStore.java:721` compara el último recibo de sesión con estado, revisión, cambio, trabajo acumulado e inicio de ejecución. No contrasta `effectiveEndAt` con el último recibo EXTEND ni `lastDecisionAt` con la decisión correspondiente. El writer real `PostgresWorkSessionStore.extend` escribe ambos valores desde `extension.effectiveEndAt` y `occurredAt`.

Contraejemplo propuesto: partir de un recibo EXTEND válido y cambiar el fin efectivo vigente por otro instante todavía posterior o igual al fin planificado. La guarda escalar actual sigue satisfecha y los cinco campos comparados no cambian. Es un hueco identificado por lectura, **todavía sin reproducción ejecutada por esta revisión**. A confirmó que lo contrastará mediante un recibo EXTEND real después de su ejecución de escala. Si una transición posterior a EXTEND conserva ese fin, debe tomarse la última extensión pertinente, no suponer que el último recibo global contiene la extensión. Los nulos legados no deben rechazarse por una invariancia nueva.

No se han editado fuentes o tests de A ni ejecutado pruebas en esta revisión. No se atribuye rendimiento, cobertura global ni PIT final. Al cerrar el punto anterior bastará revisar su delta y evidencia focal; no se solicita otra matriz histórica.

## Preparación disjunta de PIT

El nuevo `adapter.config.ImportScaleTest` no figuraba en los candidatos dedicados de persistencia. Se preparó una adición en `backend/build.gradle.kts` y su literal correspondiente en `scripts/project.test.mjs`, conservando las cuatro clases completas y todos los candidatos previos. Preparación sin ejecución mientras A mantenía su foco activo. Tras autorización de root, `node --test scripts/project.test.mjs` terminó EXIT 0, 70/70, sin fallos ni omitidos (evidencia `eb4a24`). Se ejecutó únicamente Node; no Gradle ni PIT.

## Cierre puntual de EXTEND

El hueco se reprodujo realmente: RED `cd00c9`, archivo alterado devuelto como conflicto 409 en vez de archivo inválido 400. La corrección previa del ObjectMapper del fixture y la posterior compilación detenida por una clave duplicada en un test de escala se conservan como incidencias del arnés, no defectos de producto.

Revisadas únicamente las dos condiciones nuevas de `validateHistoricalRelations` en la copia externa `import23-extend-PostgresImportDataStore.java`: `lastDecisionAt` no nulo coincide con `occurredAt` del último recibo; el fin vigente, con fallback histórico al plan, coincide con la última extensión por revisión. Esta última consulta selecciona EXTEND independientemente de una transición posterior. Los nulos legados de última decisión siguen admitidos y no se reconstruye TZDB.

El test usa `ExtendWorkSession` y `PostgresWorkSessionStore` reales sobre una sesión pausada, exporta sus datos y acredita primero la réplica idéntica válida. Después altera por separado el fin efectivo (todavía posterior al plan) y la última decisión; exige `ImportInvalidFileException` y filas intactas. GREEN `ef2f0d`: XML con seis casos, cero fallos, errores u omitidos (dos del hallazgo, integral y tres de advertencias RUNNING).

Verificación independiente `54a612`: tres de tres hashes del freeze externo `import23-extend-freeze.json` coinciden:

- Store: `8f13380ab01852a7764697ab4c9eab2bb1937c5c2bfd8b80ed466cdb76dbc3ca`.
- Test: `e8cb7e82b349cca82fb91f516cdc75b196e6a4ae2d952e052dd143fb388b6bfa`.
- XML: `6c6d449ffd63c9e91a200f6171962f603e6c108c0bcea745adc2309ea66c2585`.

Queda cerrado el único hallazgo de esta revisión. La aprobación no abarca el batching incluido en la copia, la cadena profunda posterior, otros WIP, rendimiento ni PIT final. No se han ejecutado pruebas adicionales ni editado producto.
