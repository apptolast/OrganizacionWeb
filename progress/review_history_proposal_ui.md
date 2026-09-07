# Revisión UI/cliente de la propuesta18

**APPROVED para normativa/Gherkin** tras delta puntual ratificado. Dictamen inicial y observaciones conservados como trazabilidad. Corte leído: proposal_history.md SHA FB11F6BC8006B0C4571E302CB0FAEAF76F829FAE28C8F07128052889B83EF1D0, verificado11c758. Sólo lectura; no código/tests/campañas ni cambios a la propuesta.

## R1 — Correspondencia de la página con la consulta

La representación exige coherencia id/occurredAt/contexto entre envoltorio y details, y el servidor ordena por occurredAt/sourceRank/id. No declara que el cliente compruebe las filas contra projectId/taskId/category/from/to aplicados, ni orden estricto y ausencia de duplicados(type,id).

Un resultado200 con DTOs internamente válidos de otra consulta podría aparecer bajo el contexto seleccionado. También podría mostrar duplicado/orden incorrecto sin rechazarlo. El patrón existente readBlockChanges13 (reschedule-api.ts) ya comprueba contexto, unicidad y orden de página; no requiere una abstracción nueva.

Cambio mínimo solicitado: declarar rechazo integral del cliente si cualquier fila contradice filtros normalizados, el orden total o unicidad(type,id); usar comparación temporal exactaµs y UUID canónico. Mantener nextCursor opaco para navegador, sin decodificar owner/upper. No solicitar una matriz combinatoria; Gherkin puede referenciar las fronteras con ejemplos concretos.

## Precisiones pequeñas del mismo DTO

- projectName/taskTitle son etiquetas actuales: explicitar strings no vacíos, sin null, reutilizando la semántica de text del cliente Today cuando corresponda. No inventar nombres históricos ni límites nuevos distintos de los productores.
- Nombrar explícitamente BLOCK_PLANNED occurredAt=details.createdAt; es distinto de startAt futuro. Para TASK_STATUS_CHANGED, details4 no incluye contexto: el envoltorio/contexto autorizado aporta projectId/taskId, no se le añaden campos al DTO9.

Estas precisiones completan la frontera de representación, sin añadir tipos, rutas o funciones.

## Resto del alcance revisado

- Cinco familias con details discriminados reutilizables y cerrado; identidad(type,id) evita colisiones entre tablas. CLOSE mantiene notas/workDate/closeZoneId, EXTEND no suma tiempo trabajado. Sin agregados, backfill desde outbox ni falso estado actual.
- FechasUTC inclusivas0001–9999, instante del hecho distinto de día atribuido; no se necesita TZDB cliente ni fecha10000. Orden porµs/sourceRank/id explícitamente no causal en empates. Revisiones, si se muestran para aclarar un empate, pertenecen al detalle del hecho y no son una métrica de progreso.
- Cursor vinculado a filtros/owner, keyset y límites de commits tardíos descritos. Una página por URL y navegación existente, sin append ni nuevas rutas de bloque. Contextos vacíos usan etiqueta genérica y enlace, sin nombres inventados.
- Aplicación explícita de filtros, estados de error/vacío/carga distintos, retiro ante401/404 y descarte de respuestas antiguas antes del observador. Reintento conserva consulta y no muta dominio. Foco condicionado al iniciador y 30criteriosUX con evidencia futura, sin afirmar pruebas realizadas.
- No nuevas peticiones por fila ni montaje de TaskHistory/RescheduleHistory dentro de18. La reutilización propuesta conserva sus DTO/patrones y evita N+1.

C y root recibieron R1 y precisiones. El delta descrito abajo los resuelve sin ampliar18. Backend/concurrencia/persistencia los revisa root por separado.

## Ratificación del delta

Propuesta F207B83746AF453EC1B6B09C75BB7BBFAFB735C06121D96033886CDF9C8BE478 verificada d40cec. Se revisaron sólo los cambios acordados:

- R1 cerrado: coincidencia con filtros projectId/taskId/category/from/to, orden total estricto y unicidad(type,id); cualquier incompatibilidad rechaza la página completa.
- Precisiones DTO cerradas: etiquetas actuales strings no vacíos; BLOCK_PLANNED occurredAt=createdAt; TaskHistory4 obtiene contexto del envoltorio/joins y no gana campos nuevos.
- Descubrimiento completo: enlaces desde detalles existentes de proyecto/tarea y desde filas; no exige encontrar un hecho previo ni escribir IDs. Sin catálogo ni rutas adicionales.
- Códigos concretos: taskId sin projectId es400 VALIDATION_ERROR/taskId/INVALID_VALUE; from>to es400/to/INVALID_VALUE. Cursor sintácticamente inválido precede a404 de contexto; con sintaxis válida,404 de contexto precede al vínculo owner/filtros y upper>=after. Estas precedencias permiten una recuperación UI inequívoca sin filtrar propiedad.

No quedan bloqueos UI/cliente en este corte. Aprobación limitada a propuesta: no acredita implementación, Gherkin, pruebas ni gates de18. No se ejecutaron suites ni se modificó la propuesta.