# Revisión backend del borrador de disponibilidad

Estado: revisión documental favorable con dos precisiones necesarias antes de aprobar el contrato. Se leyó progress/contract_availability_draft.feature, 40 escenarios y 190 casos declarados, la propuesta y el roadmap. No se implementa feature 10 ni se modifica el cierre 9. Ponytail full y Caveman lite activos.

## Persistencia y concurrencia

El modelo encaja en una fila de preferencias con owner_id único, UUID propio, zone_id, siete columnas de minutos con CHECK 0–1440, versión BIGINT no negativa y fechas UTC. No se necesita tabla de ventanas ni un campo total. La fila propia debe resolverse siempre desde sesión. Las siete columnas reflejan el conjunto fijo de días y permiten restricciones SQL sin un mapa JSONB débil.

La ausencia explícita y el ETag availability:unconfigured son coherentes si GET no escribe. Primer PUT: INSERT con versión 0, propietario único y confirmación de una fila. La carrera s19 exige distinguir dos resultados que comparten el conteo cero: conflicto por una fila que otra petición acaba de crear (412) y supresión del INSERT por un trigger sin fila propia resultante (503 de s25). Una opción mínima es INSERT ON CONFLICT (owner_id) DO NOTHING y, si afecta cero, consultar la fila propia después de que PostgreSQL haya resuelto el conflicto: si existe, 412; si no existe, 503. No devolver 412 indiscriminadamente ante cualquier cero ni usar un upsert que sobrescriba al ganador.

Para fila existente, SELECT propio FOR UPDATE dentro de la transacción, comparación completa de identidad/versión antes del no-op y UPDATE por owner_id/id/version con guarda de una fila. Bajo READ_COMMITTED, la petición que esperó observa el commit anterior. No hay interacción con filas de proyecto ni motivo para introducir bloqueo asesor. El snapshot devuelto determina a la vez cuerpo y ETag, después del commit. La forma de los tags y los 412 de s11 evitan revelar si una identidad ajena existe.

## Precisiones de contrato

1. s25 debe especificar qué significa fallo real de commit. Para exigir 503 y fila idéntica, usar un rechazo PostgreSQL antes de confirmar, por ejemplo un constraint trigger diferido que falla al ejecutar COMMIT. Una desconexión después de que el servidor haya confirmado puede dejar un resultado desconocido; no permite prometer que la fila sigue intacta. Esto no pide otra función: acota la evidencia del rollback real y conserva la recuperación deliberada ante pérdida de conexión de s32.
2. s30 admite una zona guardada que ya no figure en el catálogo. La reconstrucción de una fila no puede exigir pertenencia al catálogo actual ni invocar ZoneId.of sobre el valor histórico para poder mostrarlo. Conviene explicitar que GET conserva el texto previamente guardado, mientras PUT exige pertenencia al catálogo vigente incluso en una intención equivalente. Las invariantes estructurales de la entidad son independientes de esta autorización de escritura. Así la UI puede mostrar el ID no disponible sin que GET dé 500 o lo sustituya silenciosamente.

El catálogo propuesto respeta la decisión del coordinador: conjunto backend más UTC, ordenado y sin filtros de aliases. Es correcto conservar CET si está disponible y no ampliar SHORT_IDS ni aceptar offsets libres. Una sugerencia del navegador sigue siendo un borrador. Los límites 0–1440 son presupuestos diarios abstractos; los días de 23/25 horas y la resolución de horas locales se decidirán al reservar bloques, no al guardar estas preferencias.

## Frontera de eventos y compatibilidad

s18 conserva el diseño mínimo aprobado para el borrador: no hay consumidor ni requisito causal para un evento de preferencias. La outbox actual tiene FK a projects; la tabla de disponibilidad no debe referenciarla, inventar un proyecto ni relajar la FK. No añadir evento personal en esta implementación. Las seis rutas actuales y su retención permanecen independientes.

Los filtros y ApiErrors existentes permiten reutilizar 401, CSRF_INVALID, UNTRUSTED_ORIGIN, MALFORMED_JSON, VALIDATION_ERROR, 415, 428 y STORAGE_UNAVAILABLE. AVAILABILITY_CONFLICT añade un 412 propio. El rechazo de query antes de If-Match requiere leer los parámetros explícitamente en el nuevo controller; no puede delegarse a una cabecera Spring obligatoria que falle antes del handler. PUT debe recibir el cuerpo crudo opcional y parsearlo tras precondiciones, como el recurso de estado de tarea.

El borrador mantiene separadas ausencia, error, borrador y confirmación, y cubre guardado concurrente, no-op, fechas y lectura tras reinicio. Las comprobaciones de propietario y no-store no requieren autenticación adicional ni nuevas rutas de usuario. El dictamen aquí es de viabilidad y coherencia documental; no acredita tests ejecutados ni aprobación final del contrato.
