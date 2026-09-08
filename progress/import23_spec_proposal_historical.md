# Propuesta 23: importar una exportación propia JSON v1

Corte para revisión de root y C. Sólo propuesta: no modifica la especificación normativa, los estados, el producto ni las pruebas. Fuente funcional: sección 22 de `work/OrganizacionWeb-export-data/project-spec.md` y dominio/migraciones de ese checkout limpio. No se ha usado COMMON ni su V14 protegida para este diseño.

## Decisión y utilidad

Se propone **fusión conservadora integral, sin remapear identificadores ni propietarios**. Permite incorporar una copia a una cuenta que ya contiene otros proyectos y repetir una importación sin duplicarla. Inserta registros ausentes; un registro ya presente sólo es aceptable si es igual en todos sus datos durables tipados. Un conflicto impide toda la operación. No hay selección parcial, sobrescritura, combinación de preferencias ni decisiones silenciosas por campo.

Comparación de alternativas:

| Alternativa | Utilidad y coste | Decisión 23 |
| --- | --- | --- |
| Restaurar sólo cuenta vacía | Sencilla, pero impide recuperar proyectos en una cuenta que ya se usa | No restringir a esta alternativa; queda incluida como caso de fusión |
| Reemplazar todo | Recupera una imagen antigua, pero elimina cambios e historia actuales y complica sesiones, publicaciones y carreras | Fuera de alcance |
| Fusionar remapeando UUID y claves | Facilita copiar entre cuentas, pero transforma referencias, recibos e identidades históricas | Fuera de alcance |
| Fusionar sin sobrescribir | Útil para incorporar datos ausentes y repetir copias; los datos divergentes necesitan una decisión posterior | Propuesta elegida |

El `owner` del archivo debe coincidir exactamente con el principal autenticado. No existe importación entre propietarios en 23. La interfaz explica que una copia antigua no revierte cambios actuales: si un mismo registro ha cambiado, habrá conflicto y no se importará nada. El usuario no necesita una cuenta vacía, pero sí una copia compatible.

## Archivo y validación

Único formato: el JSON UTF-8 `organizationweb-export`, `schemaVersion: 1`, definido por 22. Se conservan sus catorce colecciones, campos cerrados, recibos anidados completos, valores inactivos, nulos legados, precisión de microsegundos y BIGINT decimales textuales. No CSV, ZIP, importador genérico, conectores ni reinterpretación de proyecciones HTTP incompletas.

Límites inclusivos: 33.554.432 bytes y 100.000 registros exteriores, iguales a exportación. El cliente comprueba `File.size` antes de enviar; el servidor cuenta bytes realmente recibidos y corta al superar el límite, aunque no haya Content-Length. No se confía en el nombre, extensión o tipo declarado por el archivo. Se rechazan UTF-8 inválido, BOM, contenido posterior al único JSON, claves duplicadas, campos desconocidos, formato/versión distintos y counts discordantes.

Profundidad máxima propuesta: **16 contenedores JSON**, contando el objeto raíz como 1. El v1 es cerrado: el recorrido de recibo de bloque `data → blockChanges[] → receipt → before → request/time` y los recibos de sesión son de profundidad fija; no existen valores personales objeto/array arbitrarios. El margen hasta 16 admite ese esquema sin permitir anidamiento no acotado. Se contrastaron WorkSessionTransitionReceipt, WorkSessionClosure y WorkSessionExtension del checkout limpio: el máximo estructural validado es siete: raíz/data/array/registro/receipt/before o after/session para sesiones, o request/time para bloques; closure/extension quedan en seis. No existe recursión. El límite16 queda por encima de esos caminos válidos y no abre campos nuevos.

No materializar un árbol completo de 100.000 registros antes de validar el tamaño. Lectura incremental con contador y parser acotado; validación por registro y preparación privada en almacenamiento temporal de PostgreSQL o presupuesto equivalente demostrado. La preparación admite comprobaciones de identidad y relaciones sin mantener todos los registros completos en el heap. Se descarta al terminar o fallar. No se almacena el archivo como respaldo, no se publica y no se registra su contenido en logs.

La validación de importación es más fuerte que reconocer el envelope de exportación: comprueba referencias, identidades, claves alternativas, enums, límites tipados, padres sin ciclos y del mismo proyecto, coherencia entre recibos y sus filas, proyecciones y revisiones, intervalos y acumulados según las reglas durables existentes. Las catorce colecciones deben ser autocontenidas conforme a 22; no completar referencias ausentes usando casualmente datos del destino.

La rehidratación conserva hechos históricos. No vuelve a ejecutar comandos de creación, no exige que un hecho antiguo ocurra hoy, no recalcula offsets históricos con la TZDB actual y no inventa cierres ni tiempo conocido para datos legados. A la vez, el conjunto resultante debe cumplir las restricciones vigentes de integridad: cuota de proyectos activos, como máximo una sesión abierta por propietario, unicidad y referencias. No se introduce una prohibición nueva de solapamientos históricos que el dominio actual no imponga.

## Igualdad, conflictos e identidad

La igualdad es por **registro durable completo y tipado**, no por bytes JSON: el orden de las propiedades o los espacios del documento no hacen diferentes dos registros. Se admite cualquier orden de los registros exteriores y de los valores indexados por identidad; el orden de serialización de exportación no es una precondición de importación. Los duplicados siguen rechazándose. Sí importan las cadenas de negocio exactas, nulos, versiones, fechas, todos los campos de recibos y el orden de las listas cuya semántica es ordenada (`visibleFields` y definiciones). No se normalizan nombres ni notas. Los valores personales se comparan por fieldId y valor tipado, preservando ausencia frente a null, 0 y false.

Se comprueban tanto UUID como claves únicas alternativas del esquema: propietario/ámbito en preferencias, propietario/requestKey, tarea/requestKey y bloque/versión, además de las restantes restricciones publicadas. Dos registros con UUID distintos y la misma clave alternativa no son una oportunidad de remapeo: son conflicto. Una colisión con datos de otro propietario sólo produce el error genérico; no expone su existencia detallada, nombres, IDs adicionales ni valores.

Availability, appearance y cada ámbito de customization son recursos completos. Si existe el recurso, debe coincidir también su identidad y revisión; no se mezclan arrays de definiciones ni se sustituyen preferencias actuales por las del archivo. Las definiciones inactivas y sus valores se conservan. Los datos actuales no mencionados por la copia permanecen intactos.

Una sesión importada conserva status, runningSince, intervalos y demás tiempos originales. Si está RUNNING, la vista previa advierte que continuará figurando en curso desde el instante histórico y que el cómputo actual puede incluir el tiempo transcurrido desde entonces. No se pausa silenciosamente ni se transforma ese periodo en intervalos inventados. Si el destino ya tiene otra sesión abierta, el conjunto es incompatible y no se escribe nada.

## Protocolo propuesto

Todas las rutas son privadas, autenticadas, sin caché. Los POST siguen CSRF y comprobación de origen ya existentes. No se modifica el flujo global de autenticación. Los dos POST reciben el archivo directamente como JSON, sin multipart ni un envelope que obligue a volver a serializarlo. No admiten query. Se acepta Content-Type application/json con charset ausente o UTF-8 sin distinguir mayúsculas; otra codificación o Content-Encoding distinto de identity se rechaza con 415 según el flujo HTTP existente. No hay negociación nueva de Accept. Sólo los métodos indicados; HEAD no sustituye POST ni devuelve una vista previa. Métodos no admitidos siguen 405 del framework. El GET de recibo no admite query ni cuerpo (400 IMPORT_INVALID_REQUEST).

El proxy requiere dos locations exactas, `location = /api/v1/me/import/preview` y `location = /api/v1/me/import`, para los POST de vista previa y aplicación: `client_max_body_size 0`, `proxy_request_buffering off` y `proxy_http_version 1.1` explícitos. Las rutas exactas siguen delegando autenticación y métodos a la API. No se aplica la excepción a prefijos, al GET de recibos ni a otras rutas. El límite efectivo de 33.554.432 bytes lo impone la API autenticada contando el cuerpo antes de materializarlo, también con transferencia chunked sin Content-Length. El cero de Nginx elimina su límite anticipado sólo allí; no elimina el límite de la aplicación.

La configuración actual hereda 1 MiB y buffering de petición, que no permiten prometer el archivo de 32 MiB sobre el cache tmpfs de 16 MiB. Desactivar buffering evita que el proxy necesite almacenar el cuerpo completo antes de pasarlo al backend; no se promete ausencia de todo buffer de transporte. Se preservan timeout de 15 s, `proxy_next_upstream` desactivado, cabeceras, DNS y resto de parámetros existentes. Las otras rutas mantienen su límite actual de 1 MiB. Referencias: [client_max_body_size](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size) y [proxy_request_buffering](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_request_buffering).

La aceptación local debe atravesar el Nginx real: archivo válido de 32 MiB aceptado, 32 MiB + 1 byte rechazado por API con 413, y ambas fronteras con cuerpo chunked sin Content-Length; verificar además que otra ruta conserva el límite previo. Seguridad se decide antes de consumir el archivo mediante la aplicación, sin un 413 anticipado de tamaño del proxy en esas dos rutas. No se elevan timeouts ni se modifica proxy fuera de ese alcance para obtener verde.
### Vista previa

`POST /api/v1/me/import/preview` valida y compara una instantánea consistente del destino. No escribe datos de negocio ni reserva recursos para la futura confirmación.

200 cerrado:

```text
{ format: "organizationweb-import-preview", schemaVersion: 1,
  fileSha256, byteLength, owner, exportedAt,
  counts, insertCounts, identicalCounts,
  runningSessions: [{ sessionId, runningSince }] }
```

Los tres objetos de counts tienen exactamente las catorce claves de 22, con enteros no negativos, y por colección `counts = insertCounts + identicalCounts`. `fileSha256` son 64 caracteres hexadecimales minúsculos sobre los bytes originales; `byteLength` es el tamaño exacto. `runningSessions` contiene sólo las sesiones RUNNING que se van a insertar, nunca las idénticas ya presentes. Tiene cero o un elemento, pues antes se valida que el conjunto resultante tenga como máximo una sesión abierta; cada elemento tiene exactamente sessionId y runningSince. `runningSince` conserva el nullable legado permitido, sin inventar una fecha. Un conflicto produce 409, no una vista previa confirmable. Esta primera versión anuncia el conflicto integral sin ofrecer resolución parcial ni un catálogo de valores actuales.

### Confirmación

`POST /api/v1/me/import` vuelve a recibir **los mismos bytes** y requiere `Idempotency-Key` UUID canónico y `X-Import-Content-SHA256` igual al hash mostrado. El cliente conserva el File y el hash de su propia vista previa; cambiar de archivo retira esa vista y su confirmación. El servidor comprueba el hash, revalida el formato y vuelve a comparar el estado vigente. El hash vincula archivo e intención; no convierte la vista previa en autorización ni evita la validación completa.

200 cerrado, tanto primera confirmación como repetición resuelta:

```text
{ requestKey, fileSha256, byteLength, recordedAt,
  outcome: "IMPORTED" | "NO_CHANGE", insertedCounts, identicalCounts }
```

`recordedAt` es el instante capturado del reloj inyectado al construir el recibo dentro de la transacción, antes del commit; no afirma medir el instante del commit. Se representa en UTC con seis cifras de microsegundos; los counts tienen las catorce claves. IMPORTED exige al menos una fila insertada; NO_CHANGE significa todas las filas idénticas y ninguna inserción. Los counts de confirmación son los reales: otra operación pudo añadir filas idénticas después de la vista previa. Cualquier diferencia incompatible nueva causa 409 y rollback completo.

`GET /api/v1/me/imports/by-key/{requestKey}` obtiene ese mismo recibo para el propietario autenticado, o 404 genérico. Permite resolver una respuesta perdida sin reenviar automáticamente la importación. Un 404 mientras una petición anterior sigue en curso no demuestra que nunca vaya a confirmar; la UI conserva la incertidumbre y sólo permite una recuperación deliberada con la misma clave y los mismos bytes.

El recibo de importación es metadato operativo de idempotencia, no una decimoquinta colección de JSON v1. Restricción única `(owner, requestKey)`: misma clave y mismos bytes devuelve el recibo original aunque después cambie el workspace; misma clave con archivo distinto devuelve 409. Su persistencia es atómica con las inserciones. Nunca se genera una clave nueva para ocultar una confirmación incierta.

### Errores y prioridad

Se reutiliza `application/problem+json`; título/detalle seguros en español y código estable, sin contenido del archivo ni información ajena. Propuesta de códigos propios mínimos:

| HTTP | Código | Significado |
| --- | --- | --- |
| 400 | IMPORT_INVALID_FILE | Formato, estructura, tipos, integridad interna o propietario incompatible |
| 400 | IMPORT_INVALID_REQUEST | Cabeceras de confirmación o requestKey mal formados |
| 413 | IMPORT_TOO_LARGE | Bytes o número de registros supera el límite |
| 409 | IMPORT_CONFLICT | El conjunto completo no se puede incorporar al destino |
| 409 | IMPORT_KEY_REUSED | Clave propia ya confirmada con otros bytes |
| 412 | IMPORT_FILE_CHANGED | Hash declarado distinto de los bytes recibidos |
| 404 | IMPORT_NOT_FOUND | No hay recibo accesible por esa clave |
| 503 | STORAGE_UNAVAILABLE | Persistencia, bloqueo agotado, deadlock o fallo recuperable de preparación/aplicación |

La seguridad conserva sus respuestas actuales: 401 UNAUTHENTICATED, CSRF/origen y métodos/tipos no admitidos según los filtros existentes; fallo previo de persistencia de sesión 503 SESSION_UNAVAILABLE. No se homogeneizan esas respuestas globales.

Prioridad de la aplicación: seguridad existente; método y tipo de contenido; query/cuerpo no admitidos y sintaxis de ruta/cabeceras; lectura acotada y JSON; hash de confirmación; validación completa interna/owner; recibo idempotente; comparación y restricciones del destino; escritura. Si la lectura alcanza un límite antes de poder terminar la validación, devuelve 413 sin afirmar que el resto era válido. Un recibo confirmado no se devuelve ante archivo inválido o hash discordante. Los fallos de almacenamiento se mantienen 503, no se presentan como conflictos de usuario.

## Atomicidad y concurrencia real

La vista previa es una lectura consistente, no vinculante. La aplicación prepara y valida el archivo **antes** de retener bloqueos de negocio. En una transacción READ COMMITTED activa primero `lock_timeout=2s` y `statement_timeout=10s`. Antes de cualquier lock de tabla adquiere los mismos advisory locks transaccionales que usan `PostgresCustomizationStore.change` y `changeValues`: `pg_advisory_xact_lock(hashtextextended('customization:' + owner + ':PROJECT', 0))` y después el equivalente `:TASK`, siempre PROJECT antes de TASK. La concatenación descrita se pasa como parámetro SQL, no mediante SQL interpolado. No es un lock privado entre importadores: coordina con los escritores existentes de configuración y valores del mismo propietario. Después adquiere bloqueos de tabla `EXCLUSIVE`, en una única lista fija de las catorce tablas durables y el recibo operativo, y sólo entonces lee el destino para el plan definitivo. Orden fijo de adquisición: appearance_preferences, availability_preferences, block_changes, block_projections, customization_preferences, import_receipts (nueva tabla operativa), planned_blocks, project_custom_field_values, projects, task_custom_field_values, task_status_history, tasks, work_session_changes, work_session_intervals, work_sessions. Este orden de locks no es el orden de inserción por dependencias. Las inserciones se ordenan por dependencias y el recibo confirma en esa misma transacción.

EXCLUSIVE bloquea las escrituras ordinarias y los SELECT FOR UPDATE de writers existentes; permite SELECT simple. Por eso los locks de tabla solos son insuficientes para customization: un escritor puede haber leído ausencia o versión, esperar en su UPSERT y sobrescribir después con una validación anterior a la importación. Los dos advisory locks compartidos cierran esa ventana sin modificar a todos los escritores. La elección se apoya en los modos de bloqueo documentados por [PostgreSQL](https://www.postgresql.org/docs/current/explicit-locking.html). La lista usa las tablas físicas del esquema publicado; no se bloquean sesiones de autenticación ni outbox para leer el archivo.

La transacción de aplicación configura `lock_timeout=2s` y `statement_timeout=10s`, sin reintento automático. lock_timeout se aplica a cada adquisición y statement_timeout a cada sentencia, no a toda la transacción. La lista se adquiere tabla por tabla en ese orden, no de forma atómica; los locks parciales se liberan al abortar. No son un SLA global de reloj: varias sentencias y la transmisión tienen su propio tiempo. Un aborto debe hacer rollback y liberar recursos. Nginx sólo recibe las excepciones de tamaño y buffering descritas para las dos rutas; no se eleva su timeout de 15 segundos; una respuesta de red perdida o agotada puede ser incierta y se resuelve mediante el recibo.

El coste explícito es detener brevemente escrituras de otros propietarios en esas tablas. No se oculta esa limitación ni se afirma aislamiento por owner. Las transacciones existentes pueden tener un orden distinto: un deadlock sigue siendo posible, pero debe acabar en aborto recuperable sin datos parciales. Las pruebas futuras deben enfrentar importación con writers reales de proyectos/tareas, reservas, sesiones y preferencias, no sólo con otro importador. No basta con demostrar un advisory lock entre imports. Se exigen dos carreras concretas de personalización, con barreras observables: (1) un escritor de configuración o valores ya leyó ausencia/versión y mantiene su advisory lock cuando empieza importación; ésta espera, o aborta 503 íntegramente al agotar el bloqueo, y tras adquirirlo compara el estado ya confirmado, devolviendo 409 integral si es incompatible; (2) importación mantiene los locks compartidos y se inicia un escritor, que no puede leer/validar para luego hacer UPSERT hasta su liberación. Tras la espera el escritor revalida el estado vigente conforme a su contrato. Ningún resultado puede perder un cambio confirmado, sobrescribir con una lectura obsoleta ni dejar recibo o filas parciales.

Rollback completo ante cualquier error: no quedan filas parciales, recibos falsos ni valores personales huérfanos. La desconexión del navegador después de enviar no equivale a rollback: puede haber commit y respuesta perdida, resuelta por el recibo.

## Eventos y publicación

No se borran, reescriben ni republican eventos de outbox existentes. Los registros importados conservan sus recibos históricos, pero no se ejecutan de nuevo ProjectCreated, BlockPlanned o cambios de sesión por cada fila. La justificación es semántica, **no una FK vigente**: la migración V14 publicada retiró aquella FK de outbox hacia projects.

Decisión mínima propuesta para 23: la restauración duradera se acredita por el recibo de importación y **no produce eventos de negocio históricos ni un nuevo tipo de evento de publicación**. No existen consumidores actuales que deban reconstruirse mediante esa republicación; las vistas leen persistencia. Si la revisión de arquitectura exige un evento nuevo de importación, debe aprobarse como hecho distinto con contrato y decodificación propios antes de Gherkin, no añadirse incidentalmente durante implementación.

## Recorrido y privacidad de la interfaz

Ruta `/importacion`, encabezado «Importar mis datos», navegación después de Exportación sin desplazar Hoy. Control nativo de archivo y botón «Validar archivo»; seleccionar no envía nada. Ayuda: JSON v1 propio, copia privada, incorporación sin sobrescribir, incompatibilidades impiden toda la operación.

La vista previa muestra propietario, fecha original de exportación, tamaño, cantidades por colección que se añadirán y que ya coinciden, y aviso explícito de sesiones en curso. No presenta reservas como trabajo realizado ni suma logros históricos. «Confirmar importación» es un segundo gesto explícito, con consecuencia visible; cancelar antes de confirmar descarta la preparación local sin escribir.

Durante confirmación no se edita la intención ni se dispara otra petición. Ante respuesta incierta, conservar clave/hash e indicar «Comprobar resultado»; no asegurar que Cancelar o cerrar la pestaña deshace una petición enviada. No guardar bytes, contenido, nombres del archivo o datos privados en localStorage, URLs, logs o analítica. Para recuperar después de recargar la pestaña se guarda únicamente `{ owner, requestKey, fileSha256 }` en una entrada propia de sessionStorage `organizationweb.import.pending.v1`, justo antes de enviar la confirmación. No contiene archivo, nombre, bytes ni vista previa. Se valida su forma al leer y se elimina si es inválida, no coincide con la sesión, cambia la identidad, hay logout o se obtiene un recibo confirmado. Se conserva ante incertidumbre y 404; no permite reenviar sin volver a seleccionar bytes con el mismo hash. No hay localStorage, GET global ni proveedor nuevo. Si sessionStorage no está disponible, no se envía la confirmación: se explica el requisito de recuperación sin degradarlo silenciosamente.

Cambio de sesión o salida retira datos visibles y aborta lectura, con guardas de generación antes y después de awaits: un 401 o JSON tardío de otra identidad no altera la sesión nueva. Después de commit se refrescan deliberadamente snapshots afectados para que los datos importados aparezcan; no se reinician borradores ajenos ni se aplican preferencias en una vista previa.

Controles de al menos 44 px, teclado, foco visible y anuncios de progreso/error/resultado conforme a UX existente. Retorno de foco cuando desaparezca el iniciador, sin robarlo si el usuario lo movió; comprobar el blur real de controles disabled en navegador. Estados preparados y errores no dependen sólo del color. Reutilizar SCSS, controles nativos y apiRequest; ninguna dependencia nueva.

## Aceptación y límites de este corte

Toda confirmación de aceptación se ejecutará exclusivamente en PostgreSQL y cuentas locales efímeras. En live sólo se autoriza vista previa sin escrituras; no se borran datos productivos para preparar una importación. La aceptación debe acreditar copia propia útil, no-op, conflicto integral, colisiones privadas, recibos e intervalos completos, sesión RUNNING preservada con aviso, límites de recursos, carrera con writer existente y commit con respuesta perdida.

Pendientes de revisión concreta antes de Gherkin: confirmar la lista física y orden de locks, y ratificar las respuestas y la recuperación descritas. Root ha ratificado cero evento nuevo, recordedAt previo al commit y límites por bloqueo/sentencia, sin SLA global nuevo. Son decisiones técnicas de este contrato, sin preguntas opcionales al usuario y sin prometer implementación ya validada.
