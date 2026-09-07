# Revisión de propuesta16

**APPROVED de la propuesta final16**, SHA256 `77D864520E4A9CD48EBAE10D5F9E49F2319013D1A6EE564E1026535B129CA5C6`, tras relectura aa22c3. El bloqueo inicial sobre representabilidad del texto queda resuelto; se conserva debajo su historia. No código, tests, Gherkin ni cambios de propuesta. Ponytail full/Caveman lite.

## Precisión bloqueante antes de aprobar

“Strings de hasta2000 puntos de código Unicode” todavía admite, en la frontera JSON/Java, U+0000 escapado y unidades sustitutas UTF-16 aisladas. No deben prometer confirmación201 para una nota que el recibo JSONB no puede conservar. Añadir exclusivamente a progressNote/nextStep de16:

> Después de parsear JSON y antes de propiedad/replay, las notas normalizadas deben ser texto Unicode válido, sin U+0000 ni sustitutos UTF-16 altos o bajos aislados. Esos valores producen400 VALIDATION_ERROR, con el campo correspondiente y código INVALID_VALUE. Los pares válidos que representan un carácter suplementario se conservan y cuentan como un punto de código. Ausencia/null siguen normalizándose a vacío; no se recortan espacios ni se elimina silenciosamente ningún carácter.

Esta regla no cambia la precedencia de sintaxis: un carácter de control literal que ya invalida JSON conserva MALFORMED_JSON; un escape JSON válido que decodifica al valor prohibido llega a validación de campo. Tampoco modifica retroactivamente14/15 ni sus notas/criterios de otras features. No hace falta una nueva excepción, código de error o parser.

## Resto de decisiones revisadas

- PAUSE/RESUME conservan recibo6; CLOSE es recibo7 con closure4. Estado6 sólo amplía status aclosed, preserva SessionStart7 y congela el neto. Snapshot sigue con serverNow válido15 y cabecera de negocio, sin ETag fuerte engañoso.
- GETclosure por sessionId recupera el mismo recibo durable, con propiedad antes de distinguir sesión abierta y read-only/RR sin reloj. No introduce colección18 ni infiere notas desde el estado. La rutaUI estable permite recuperar trasactive=null y verifica project/task/session antes de mostrar.
- Normalización de notas forma parte de intención antes del replay. Propiedad e identidad del token preceden al replay; intención histórica precede a revisión/estado. El caso cerrarA/iniciarB vuelve comprobable owner/key entre sesiones sin permitir dos abiertas. Replay de15 trasclosed conserva el hecho original; keynueva sobreclosed rechaza.
- Cierre running suma sólo el último tramo; paused no suma descanso; microsegundo cero y precisión completa se conservan. El lock y la transacción liberan plaza sólo al commit y no alteran proyecto/tarea/planes. No se impone un ganador artificial a inicio concurrente.
- workDate es día local del cierre en zona histórica; fallbackUTC sólo ante zona irresoluble queda persistido. Zona válida que produce fecha fuera de0001–9999 rechaza, no activa fallback. No equivale a repartir minutos por día ni agrega18/19.
- EventoClosed11 separado mantiene StateChanged12 y no filtra notas al broker. Recuperación C/K y por sesión no depende de outbox. La UI mantiene recibo frente a nueva activa e incertidumbre frente a estado terminal, sin atribuir una key perdida por inferencia.

Corrección editorial menor al incorporar norma: donde dice “409 STATE_CONFLICT”, usar el código completo `WORK_SESSION_STATE_CONFLICT` ya fijado en el párrafo anterior. No es un nuevo error ni un cambio de comportamiento.

Tras incorporar la regla de texto y mantener esas formas exactas, no identifico otro bloqueo contractual. No solicito ampliar matrices, avisos17, historial18 ni infraestructura. Las pruebas y los gates se definirán después de la aprobación normativa.

## Revisión del delta final

La propuesta final rechaza NUL decodificado y sustitutos aislados como400 INVALID_VALUE por campo, conserva pares válidos y conteo2000, y distingue sintaxisJSON previa sin retroactividad14/15. También usa el código completo WORK_SESSION_STATE_CONFLICT. Las tres precisiones UX son coherentes: URL de sesión establecida antes del POST permite recuperación tras pérdida de ACK; workDate persistido no se recalcula ni invalida por diferencias de Intl del cliente;412 conserva borrador pero sólo permite nueva confirmación manual con key/revisión nuevas después de un GET que confirme sesión abierta. No añaden recuperación por inferencia, POST automático ni historial18. Queda lista para promoción normativa por root; no se redactó Gherkin en este subtask.
