# Errores HTTP compartidos de reprogramación

Autoría exclusiva de BlockController, ApiErrors, RescheduleErrorsApiTest y esta bitácora sobre el checkpoint aislado f0fde3e. Snapshot Java d3ffecf proporcionado por root; V13 aprobada parcialmente. E2E congelado. No se modifican Store, RescheduleController, wiring ni configuración.

Baseline init f4721d ya registrado; no se repite init sobre el snapshot parcial. Se aplican Ponytail full y Caveman lite. Pruebas MockMvc con seguridad Spring, PostgreSQL/Flyway reales y Clock fijo; no mocks de excepciones ni de Store. Un caso nuevo por ciclo. Cada rechazo comprueba problem+json, cuerpo cerrado, mensaje exacto, no-store y conservación de bloques, proyecciones, recibos, outbox y preferencias.

1. Rechazo de movimiento por presupuesto: primer foco en ejecución. Presupuesto de 30 minutos, destino de 60 minutos y bloque previo excluido del cálculo. Se espera 409 BUDGET_EXCEEDED con días actuales, no INTERNAL_ERROR.

1. RED fc52b4/1b63cd: se esperaba409 y se recibió500. Extraído únicamente el handler de BlockBudgetExceededException de BlockController a ApiErrors, con el mismo cuerpo y mensaje. GREEN en ejecución.

1. GREENc7aba9: presupuesto409 con días exactos y sin escrituras.
2. Solape con allowOverBudget=true: nuevo caso individual en ejecución; el conflicto debe conservar sólo id/projectId/taskId propios.

2. REDf25e54/d7c5a3: solape devolvía500 en vez de409. Trasladado íntegro el handler de BlockOverlapException existente; no se duplica ni cambia su selección de conflicto. GREEN en ejecución.

2. Solape GREEN150f8c.
3. Disponibilidad ausente ante proyecto completed: RED8b0092/0b636e (500 en vez de409). Trasladado el handler existente que agrupa ausencia, zona no disponible y tarea completada. No se crean ramas nuevas; las otras dos ramas se verificarán individualmente después de este GREEN.

3. Ausencia de disponibilidad GREENcbdfec, manteniendo precedencia frente a proyecto completed.
4. Zona de preferencia retirada: primer caso de preview en ejecución. La petición propone UTC válida; el fallo se debe exclusivamente a la zona de la preferencia.

4. Zona retirada: inicialmente GREENe8eb8a, sin producción nueva.
5. Tarea completed con preferencia válida: caso individual en ejecución, sin modificar handlers.

5. Primer intento falló en el fixture, antes de HTTP (5c3c9a/9fe742): tasks_completion_consistent exige completed_at. Se corrige sólo la preparación como en ScheduleBlockApiTest; no es RED del producto. Nueva ejecución en curso.

5. Con fixture válido, inicialmente GREEN399ecf/5ca5c3; no cambio de producción.
6. Preview ambiguo Europe/Madrid en otoño: nuevo caso individual en ejecución. Exige validOffsets startOffset en orden +02:00,+01:00 y error de campo exacto.

6. REDc7d379/d6100c: hora ambigua devolvía500 en vez de400. Trasladado sin alterar su lógica el handler BlockOffsetException; mantiene errors y validOffsets. GREEN en ejecución.

6. Ambigüedad GREENf25826.
7. Offset de fin incorrecto al confirmar: caso individual en ejecución. El inicio tiene un offset válido; se exige validOffsets bajo endOffset, no startOffset.

7. Offset final incorrecto: inicialmente GREENaf852c, sin producción nueva.
8. Hora local inexistente: caso en ejecución para conservar la validación global existente y distinguirla de BlockOffsetException.

8. Hora inexistente: inicialmente GREEN14e76d, sin producción nueva.
9. Privacidad: otro owner consulta un bloque cuyo presupuesto excedería el límite. Caso individual en ejecución; debe recibir sólo RESOURCE_NOT_FOUND, sin presupuesto ni identidad del bloque.

9. Owner ajeno: inicialmente GREEN49ebd2; respuesta404 cerrada, sin detalles y sin escrituras.
Refactor en GREEN: la nueva suite adopta el ciclo de PostgreSQL de las API existentes (endpoint estable mientras Spring conserva el contexto; Ryuk limpia al salir la JVM). No cambia los oráculos. Formato y dry-run GJF1.31.0 focal sobre los tres archivos Java: efa03f EXIT0. Diff e51f79 confirma sólo los cuatro handlers trasladados; parser, identificadores, cursores e idempotencia permanecen iguales.
Regresión final en ejecución: RescheduleErrorsApiTest + ScheduleBlockApiTest + ProjectApiTest. Esta última cubre los errores globales de validación, JSON, media type y fallo interno ya existentes. No se ejecuta init ni E2E.

## Entrega congelada para revisión

Regresión `8d3280`, EXIT 0. XML `481f3f`: **224 tests**, incluidos 9 de errores13, 173 de bloques11 y 42 de ProjectApiTest; cero fallos, errores u omitidos. Formato focal y dry-run `efa03f`, EXIT 0; comprobación de whitespace `481f3f` sin diagnósticos. No se modifica configuración ni se ejecuta mutación.

La producción sólo traslada cuatro handlers de BlockController a ApiErrors. Conserva status, content-type, mensajes y campos exactos; elimina sus copias locales. Los handlers de JSON ilegible e idempotencia, las firmas identifier/invalid y los cursores permanecen sin cambios. No se modifica RescheduleController, Store ni otros archivos de producción.

Mapa acotado de cobertura:

- `@s6`: ausencia de disponibilidad antes de proyecto completed y tarea completed con preferencia válida.
- Herencia de elegibilidad11: zona de preferencia retirada en preview.
- `@s8`: preview ambiguo, offset final incorrecto al confirmar y hora local inexistente; validOffsets sólo donde corresponde.
- `@s22`: presupuesto con días actuales y solape aunque se consienta exceso.
- Precedencia y privacidad generales: otro owner recibe RESOURCE_NOT_FOUND sin datos del bloque o presupuesto.
- Todos los casos comprueban no-store y conservación de bloques, proyecciones, recibos, outbox y preferencias.

Hashes SHA-256 de los tres Java congelados (`9863cd`):

- `BlockController.java`: `55A4FA277B163850D16C08770DB14DB982E2AA04C5CE5A18663E4C2B4AF6D0EA`.
- `ApiErrors.java`: `F34E369002B7AEB2678C971D44DB2D639D7487803BB5B0BF78F9B18BE523B96A`.
- `RescheduleErrorsApiTest.java`: `E5D6291CE485544975A7924F84D399FAD568D7BA482E565CA8A612C00E4DD52D`.

La entrega integra exclusivamente esos tres archivos y esta bitácora desde f0fde3e. El snapshot de producción de otros autores permanece como base aislada, no como paquete publicable. Sin commits ni push. E2E13 sigue congelado; este resultado no cierra la feature ni sustituye revisión independiente, pruebas de carreras o mutación.
