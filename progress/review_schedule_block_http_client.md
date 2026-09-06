# Contraste incremental HTTP / cliente de bloques

Estado: revisión acotada IN_PROGRESS, no freeze ni veredicto final de backend. Solicitada por coordinador durante autoría de GETs y excepciones de negocio. No se modificó backend ni cliente API aprobado. Ponytail full y Caveman lite.

## Alcance leído

BlockController, ApiErrors, BlockRequest, ResolvedBlockTime, BlockOffsetException, BudgetDay y PlanBlock; contraste con readBlockError, isPreview, isBlock, validación de instantes/offsets/días en schedule-block-api.ts. Se leyeron las aserciones s1_previewReturnsExactSnapshotWithoutWriting y validación estructural de ScheduleBlockApiTest. La ejecución de 90 HTTP pertenece al autor backend; esta revisión fue de lectura y no se atribuye una ejecución independiente de esos tests.

## Formas estables compatibles

- PreviewResponse declara exactamente los diez campos esperados. Su prueba HTTP comprueba objetivo normalizado, zonas, instantes UTC, offsets Z, duración, ETag configurado y día de cinco campos. Coincide con el parser del cliente y su fixture de intención.
- BlockResponse declara los nueve campos cerrados; UUID, objetivo/zoneId almacenados e instantes conservados coinciden con el cliente. PlanBlock trunca createdAt a microsegundos, dentro de las 1–6 cifras fraccionarias aceptadas. startAt/endAt tienen segundos enteros en ResolvedBlockTime, incluidas zonas históricas con desfase de segundos. La duración corresponde a la diferencia de instantes.
- BudgetDay serializa fecha local y cuatro cantidades; presupuesto 0–1440, requestedSeconds positivo y exceso calculado son los invariantes exigidos por el cliente. La enumeración de fechas del dominio sigue revisión independiente; no se declara aprobada por coincidir el DTO.
- ApiErrors.problem usa exactamente type/title/status/code y Locale.ROOT para construir el URN. ValidationException añade errors con FieldError field/code/message. Los códigos estructurales emitidos por BlockController y BlockRequest figuran en fieldCodes del cliente, incluidos encabezados/consulta como nombres de campo no asociados a controles.
- PRECONDITION_REQUIRED 428, AVAILABILITY_CONFLICT 412, RESOURCE_NOT_FOUND 404, PROJECT_COMPLETED 409, IDEMPOTENCY_CONFLICT 409, STORAGE_UNAVAILABLE 503 y MALFORMED_JSON 400 tienen formas reconocidas. Las respuestas internas 500 con correlationId no son interpretadas como rechazo definitivo: el cliente las deja desconocidas y la UI mantiene incertidumbre de escritura.
- Availability-Revision emitida por preview y aceptada por create comparte UUID canónico y versión BIGINT no negativa. Idempotency-Key nace como crypto.randomUUID y cumple el patrón canónico de create. Location construida desde IDs de contexto y bloque coincide con la validación de createBlock.

## Tramos todavía en autoría, no defectos finales

BlockOffsetException no tenía handler específico en el corte leído y heredaba el genérico 500. El autor backend ya había anunciado pendientes las excepciones de negocio ampliadas. Se le comunicó la forma exacta que requiere el cliente para ese siguiente ciclo: 400 VALIDATION_ERROR, errors con una entrada y validOffsets con sólo el extremo afectado; offsets canónicos mediante getId, ordenados por instante resultante ascendente (desfase descendente), al menos dos para AMBIGUOUS_OFFSET y uno para INVALID_OFFSET. El DTO no permite enviar ZoneOffset como objeto estructurado ni añadir campos de otro extremo.

GET lista/detalle/by-request y errores de solape/presupuesto/elegibilidad/disponibilidad siguen en trabajo del autor. No se presentan ausencias temporales como hallazgos de entrega final. No se detectó un desajuste concreto entre las formas estables leídas y el cliente aprobado. La evidencia integrada decisiva seguirá siendo el E2E real cuando esos endpoints estén disponibles.

## Verificación integrada posterior

Los tramos anteriormente pendientes ya tienen evidencia real incremental: GET lista inicial/detalle/by-request y replay pasan; DST ambiguo de ambos extremos activa los selectores UI y permite persistir offsets explícitos (Chromium/Firefox/WebKit). BLOCK_OVERLAP real posterior al preview se interpreta como rechazo definitivo y el lookup explícito devuelve el bloque de otra tarea propia. No se encontró desajuste DTO/cliente en esos recorridos; versiones y resultados exactos en tdd_schedule_block_integration.md. BUDGET_EXCEEDED posterior a reserva está en la siguiente ejecución, no se da por probado por la mera presencia del handler.
BUDGET_EXCEEDED también verificado con API/PG real: snapshot147HTTP, suite6/6 PASS, DTO día120/5400/3600/1800 interpretado y recuperación mediante nueva revisión/consentimiento confirmada. No se detectó desajuste de forma.
