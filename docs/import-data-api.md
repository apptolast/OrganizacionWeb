# API de importación de datos v1

Describe el contrato implementado y desplegado del producto 4c74e183. La funcionalidad 23 está publicada; evidencia y límites en progress/judge_import_data.md. Complementa la sección 23 de project-spec.md y la exportación JSON v1 de la sección 22.

## Transporte y acceso

Se utiliza la sesión web autenticada existente. GET /api/session proporciona el token CSRF; los POST requieren la cookie de sesión y X-CSRF-TOKEN válido, además de la política de origen existente. GET de recibo no requiere CSRF. No se envían credenciales dentro del archivo. El propietario efectivo procede de la sesión y el campo owner del archivo debe coincidir con él.

Los dos POST reciben el archivo como cuerpo JSON bruto, no multipart ni envoltorio base64. Content-Type: application/json, con charset UTF-8 opcional; Content-Encoding ausente o identity. Solicitar Accept: application/json. No se admiten query strings. La ruta de recibo recibe UUID canónico minúsculo, sin cuerpo ni query.

El archivo es la exportación propia obtenida de GET /api/v1/me/export. Límites inclusivos: 33.554.432 bytes UTF-8 y 100.000 registros sumando las 14 colecciones exteriores. El servidor cuenta los bytes recibidos también sin Content-Length. Rechaza BOM, UTF-8 inválido, JSON concatenado o truncado, claves duplicadas, propiedades desconocidas y profundidad superior a 16 contenedores. Se validan formato/versión, tipos, counts, propiedad e integridad de referencias y recibos. No es un importador de JSON arbitrario ni normaliza cadenas de negocio.

Las claves de recuentos son exactamente: projects, tasks, taskStatusHistory, availability, plannedBlocks, blockProjections, blockChanges, workSessions, workSessionIntervals, workSessionChanges, appearance, customization, projectCustomFieldValues y taskCustomFieldValues. Sus valores son enteros no negativos. Los recibos operativos de importación no forman parte del archivo exportable.

## Previsualización

POST /api/v1/me/import/preview valida el archivo contra una instantánea consistente. No añade datos de negocio, eventos ni recibos, y no reserva el estado del destino para una confirmación posterior. Responde 200 con objeto cerrado:

```text
{ format: "organizationweb-import-preview", schemaVersion: 1,
  fileSha256, byteLength, owner, exportedAt,
  counts, insertCounts, identicalCounts,
  runningSessions: [{ sessionId, runningSince }] }
```

fileSha256 es SHA-256 hexadecimal minúsculo sobre los bytes originales; byteLength es su tamaño exacto. Por colección, counts = insertCounts + identicalCounts. runningSessions contiene únicamente sesiones RUNNING nuevas, cero o una; runningSince puede ser null por compatibilidad histórica. Un conflicto devuelve 409 y no produce una previsualización confirmable.

## Confirmación e idempotencia

POST /api/v1/me/import recibe otra vez los mismos bytes, junto con exactamente una cabecera Idempotency-Key (UUID canónico minúsculo) y una X-Import-Content-SHA256 (64 caracteres [0-9a-f]). Calcular el hash sin reserializar el JSON. Cambiar los bytes exige nueva validación de la intención. La previsualización no reemplaza la autorización ni la validación del servidor: al confirmar se revalidan archivo y destino actual.

La fusión es integral y conservadora: inserta ausentes, conserva registros completos tipadamente idénticos y rechaza diferencias incompatibles; no sobrescribe ni elimina datos. Escrituras y recibo se confirman atómicamente, sin emitir eventos nuevos. Respuesta 200 cerrada:

```text
{ requestKey, fileSha256, byteLength, recordedAt,
  outcome: "IMPORTED" | "NO_CHANGE", insertedCounts, identicalCounts }
```

Los counts son los realmente confirmados y pueden diferir de los de preview si aparecieron filas idénticas entretanto. IMPORTED implica alguna inserción; NO_CHANGE implica ninguna y también crea un recibo operativo. recordedAt se captura dentro de la transacción antes de commit: no es una medición del instante de commit. Se representa en UTC con seis cifras de microsegundos.

La identidad idempotente es (propietario, requestKey). Repetir la clave con los mismos bytes válidos devuelve el recibo existente sin volver a insertar. Con otros bytes devuelve 409 IMPORT_KEY_REUSED. El servidor comprueba sintaxis/límites, hash y validez interna antes de resolver el recibo; disponer de un recibo no permite enviar un archivo inválido.

## Recuperación de respuesta incierta

GET /api/v1/me/imports/by-key/{requestKey} devuelve 200 con el mismo objeto de recibo accesible al propietario autenticado, o 404 genérico. Tras perder la respuesta de confirmación, conservar clave/hash y consultar esta ruta antes de cualquier reenvío; nunca fabricar una nueva clave para reintentar automáticamente. Un 404 mientras el POST sigue en curso no demuestra que no vaya a confirmar. La recuperación deliberada utiliza la misma clave y exactamente los mismos bytes. Error de red, cancelación del cliente, respuesta ilegible o 503 no acreditan ausencia de commit.

## Errores públicos

Los errores de importación se responden como application/problem+json.

| HTTP | code | Significado |
| --- | --- | --- |
| 400 | IMPORT_INVALID_FILE | Formato, tipos, integridad interna o propietario incompatible. |
| 400 | IMPORT_INVALID_REQUEST | Query/cuerpo no admitidos, UUID o cabeceras de confirmación inválidos. |
| 404 | IMPORT_NOT_FOUND | No existe recibo accesible para la clave. |
| 409 | IMPORT_CONFLICT | El conjunto completo no se puede incorporar al destino. |
| 409 | IMPORT_KEY_REUSED | Clave propia ya confirmada con otros bytes. |
| 412 | IMPORT_FILE_CHANGED | El hash declarado no corresponde a los bytes recibidos. |
| 413 | IMPORT_TOO_LARGE | Superado el límite de bytes o registros. |
| 503 | STORAGE_UNAVAILABLE | Fallo de almacenamiento/preparación, contención agotada o deadlock. |

Se conservan los errores transversales: 401 UNAUTHENTICATED, 403 CSRF_INVALID para sesión autenticada con CSRF inválido, 503 SESSION_UNAVAILABLE, y negociación de método/tipo/respuesta según la política HTTP existente (405/415/406). No reinterpretar errores de almacenamiento como conflictos del archivo. Las rutas POST sólo admiten POST; los métodos rechazados anuncian Allow: POST. El recibo se consulta con GET.

Orden relevante: seguridad, transporte/ruta/cabeceras, lectura JSON acotada, hash declarado, validación interna/propiedad, recibo idempotente, comparación con destino y escritura. Un exceso encontrado durante lectura puede devolver 413 antes de conocer la validez del resto. No hay garantía pública de duración total ni cambio de timeout por esta funcionalidad.
