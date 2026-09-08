# Revisión del borrador import23

Corte leído: import23-spec-proposal.md SHA
004C2D89A00CE99136FF2ADBC4F838F1A3CD0431DDE412B5CB0AF92BE77D5B8B.
Dictamen: diseño coherente, pendiente aplicar precisiones de root y fijar
lista física antes de normativa. No código, Gherkin ni pruebas ejecutadas.

## Profundidad: conforme

Máximo estructural v1 observado: siete contenedores. Raíz1, data2, array3,
registro4, receipt5, before/after6 y session7 en recibos de sesión; en bloques,
request/time ocupa el nivel7. closure/extension son objetos del nivel6 sin
otros contenedores. Definiciones/values personales llegan a6 y sus valores
son escalares o null. Fuente: ExportReceiptWriter.session/state/block y
sección22 del checkout limpio export22-closure42639f7. Límite16 admite todo
v1; no requiere ampliar formato ni permitir valores personales arbitrarios.

## Locks: conforme con alcance explícito

EXCLUSIVE impide writers ordinarios y SELECT FOR UPDATE/SHARE, pero deja
lecturas simples. Permanece hasta finalizar la transacción. No equivale a
ACCESS EXCLUSIVE ni aislamiento por propietario. Fuente primaria:
[PostgreSQL17, modos de bloqueo](https://www.postgresql.org/docs/17/explicit-locking.html).

Orden físico propuesto, fijo e independiente de los datos del archivo:

```text
appearance_preferences
availability_preferences
block_changes
block_projections
customization_preferences
planned_blocks
project_custom_field_values
projects
task_custom_field_values
task_status_history
tasks
work_session_changes
work_session_intervals
work_sessions
```

Añadir en posición fija la nueva tabla de recibos de importación; su nombre
físico aún debe fijarlo B/A. Por ejemplo, import_receipts al principio, si
ése es el nombre finalmente aprobado. No usar nombres de colecciones camelCase
como tablas ni incluir outbox_events, flyway_schema_history o Spring Session.
El orden de inserción es distinto: padres antes de dependientes y subtareas
tras sus padres. El orden de locks no exige orden de UUID de inserción.

Una lista LOCK TABLE adquiere locks uno a uno en el orden escrito. Importadores
entre sí comparten orden; writers anteriores aún pueden producir deadlock.
El fallo debe revertir y liberar también los locks adquiridos parcialmente.
Leer el destino para el plan definitivo sólo después de adquirir todos bajo
READ COMMITTED. Fuente:
[PostgreSQL17, LOCK](https://www.postgresql.org/docs/17/sql-lock.html).

## Precisiones pendientes, ya enviadas por root

1. Sustituir committedAt por recordedAt: el reloj dentro de la transacción
   no certifica el instante real del commit.
2. Retirar plazo global30s. SET LOCAL lock_timeout2s y statement_timeout10s
   son límites por adquisición/sentencia, no total de transacción ni SLA de
   respuesta bajo proxy15s. No relajar proxy. Cancelación y rollback deben
   ser reales; la recepción/parsing tampoco quedan acotados temporalmente por
   esos ajustes SQL. Fuente:
   [PostgreSQL17, timeouts](https://www.postgresql.org/docs/17/runtime-config-client.html).
3. Fijar counts=insertCounts+identicalCounts por colección; runningSessions
   identifica sólo filas running del archivo, máximo1 tras integridad. El
   destino puede contener otra fila abierta y generar409. No listar sesiones
   ajenas al archivo ni inventar runningSince de una fila legada.
4. Fijar recuperación tras reload: campos mínimos en sessionStorage ligados
   al owner, sin File/bytes/nombre ni payload. Reupload exige mismos bytes/hash
   y requestKey;404 mientras hay petición pendiente no acredita rollback.
5. Fijar nombre/posición de la tabla operativa de recibos en la lista anterior.

No nuevo bloqueo adicional de formato detectado. Owner exacto y cero eventos
nuevos ratificados son coherentes. La frase «intervalos y acumulados según las
reglas durables existentes» no debe convertirse durante implementación en
reconstruir historia completa o prohibir nulls legados válidos. La validación
puede ser estricta sin inventar invariantes temporales nuevas.

La propuesta sigue siendo una revisión documental, no evidencia de rendimiento,
locks efectivos o recuperación ya implementada. Esos oráculos corresponden al
TDD posterior contra PostgreSQL y writers reales.

## Ratificación de corrección concurrente señalada por root

**APPROVED para el diseño corregido.** La aprobación anterior de EXCLUSIVE
solo era incompleta: root identificó una ventana real de pérdida de datos en
PostgresCustomizationStore:54–64 y104–126. El writer toma advisory, hace SELECT
simple, valida la revisión y luego UPSERT sin CAS; EXCLUSIVE permite esa lectura
y podría intercalarse antes del UPSERT. No se considera equivalente ni inocuo.

Corrección mínima ratificada: en la transacción de confirmación, con timeouts
locales ya activos, adquirir primero pg_advisory_xact_lock de
hashtextextended('customization:' + owner + ':PROJECT',0), luego el mismo patrón
TASK; sólo después los quince EXCLUSIVE y lectura definitiva del destino.
Exactamente el mismo texto/hash y locks de transacción, no mutex Java o hash
alternativo. No tomarlos después de los locks de tabla.

Si customization ya leyó, conserva el advisory hasta su commit/rollback: import
espera sin tener tablas bloqueadas y luego observa su resultado. Si import posee
ambos advisory, los cambios de configuración y valores esperan antes de leer
schema/previous. Tras import vuelven a validar la revisión nueva. changeValues
hace requireOwned antes del advisory, pero sólo comprueba identidad/contexto;
import no actualiza ni borra esos padres y las lecturas de schema/values siguen
después del lock compartido. No queda esa ventana de UPSERT antiguo.

Pasada agrupada de otros writers publicados:

- ProjectEditing/ProjectStatusEditing usan findForUpdate, y CAS por versión;
  el count de activos se obtiene después del lock de proyecto. PostgresProjectQueries:49
  confirma FOR UPDATE, no un SELECT simple oculto en el helper.
- TaskCommit bloquea proyecto FOR UPDATE antes de validar/insertar; TaskStatusStore
  bloquea tarea FOR NO KEY UPDATE antes del cambio y CAS/historial/outbox.
- Appearance/Availability leen FOR UPDATE antes de operation. Altas usan
  ON CONFLICT DO NOTHING y comprueban el resultado, no sobrescritura por UPSERT.
- BlockStore bloquea contexto FOR SHARE y disponibilidad/bloque antes de calcular
  mutación; relee replay/state después de los locks. WorkSessionStore bloquea
  contexto FOR SHARE al iniciar y sesión FOR UPDATE al cambiar/extender.
- ProjectCommit sólo inserta identidad nueva; no aplica una actualización
  derivada de estado previo leído sin protección.

No se encontró otra lectura simple seguida de UPSERT sobrescribiente equivalente
en los escritores inspeccionados. Lecturas simples de replay pueden retornar un
resultado ya durable, sin escritura. La protección se apoya tanto en los locks
compartidos existentes como en el carácter insert-only/no-op de import.

Quedan posibles deadlocks entre tablas de writers anteriores y el orden fijo de
import. Ningún camino debe convertirlos en éxito parcial; abortar toda la
transacción libera advisory y tablas. Orden PROJECT/TASK uniforme evita inversión
entre importadores. Colisión del hash sólo añade contención, no elimina exclusión.
Los ensayos propuestos con writer real ya leído y writer iniciado durante import
son necesarios para acreditar esta solución; esta lectura no los sustituye.

B comunicó corte corregido de spec5251914B y propuestaACB339F7. Esta ratificación
aprueba el mecanismo descrito; la revisión final de normativa sigue en root.
No se inició Gherkin ni se modificó producto.

## Ratificación del ajuste mínimo de proxy23

**APPROVED como diseño pendiente de prueba real.** deploy/nginx.conf publicado
mantiene el máximo de cuerpo por defecto1m y buffering de petición activado.
Las dos locations exactas /api/v1/me/import y /api/v1/me/import/preview pueden
usar client_max_body_size0, proxy_request_buffering off y proxy_http_version1.1.
El límite de33.554.432 bytes queda aplicado por la API al recibir, antes de
materializar más datos, contando también chunked sin Content-Length.

El valor0 desactiva sólo el chequeo de tamaño Nginx en esas locations. No
significa que la API acepte cuerpos ilimitados. Con bufferingoff, Nginx envía
el cuerpo conforme llega; chunked requiere proxyHTTP1.1 explícito para no
volver al buffering. Fuente primaria:
[Nginx client_max_body_size](https://nginx.org/en/docs/http/ngx_http_core_module.html#client_max_body_size)
y [proxy_request_buffering](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_request_buffering).

Precisión de implementación importante: las dos locations exactas son hermanas
de location/api/, no heredan sus directivas. Conservar explícitamente en ellas
proxy_pass al upstream existente sin reescritura de URI, Host/X-Forwarded-Proto/
X-Forwarded-For, proxy_read_timeout15s y proxy_next_upstreamoff. No cambiar DNS,
headers de seguridad del server, otras rutas, mounts ni tmpfs. Tampoco confundir
request_buffering con buffering de respuestas.

No imponer32m en Nginx: emitiría413 antes de los filtros API y posiblemente HTML.
Autenticación previa y errores JSON requieren que la ruta alcance esos filtros.
Esto se refiere a HTTP válido: errores de framing/transporte no se convierten
en promesa de problem+json generado por la aplicación.

Oráculos mínimos con Nginx real y configuración runtime actual, para ambas rutas:

1. Archivo v1 válido de exactamente33.554.432 bytes pasa autenticación/CSRF y
   llega íntegro a preview/aplicar según caso, con respuesta contractual. No
   completar el tamaño con un campo de negocio inválido; whitespace JSON válido
   o datos válidos acotados evitan confundir error de esquema con tamaño.
2. Byte adicional devuelve413 IMPORT_TOO_LARGE problem+json, sin filas/recibo
   parcial. Mantener separado un caso con Content-Length conocido y uno chunked
   sin él; ambos acreditan límite de bytes reales, no sólo comparar cabecera.
3. Petición anónima válida de tamaño grande obtiene401 UNAUTHENTICATED de API,
   sin preparar/staging de negocio ni413HTML previo. Acreditar rechazo temprano
   sin necesitar completar carga, con un emisor capaz de observar la respuesta.
   CSRF/Origin se conservan en una comprobación autenticada representativa.
4. Durante carga chunked, observar que upstream recibe antes de terminar emisor;
   no basta éxito final para demostrar bufferingoff. Ejecución con readonly y
   tmpfs16MiB existentes debe admitir32MiB sin archivo temporal completo ni
   ampliar mounts. Comprobar recursos propios durante la carga, no sólo después
   de que un temporal pudiera haberse eliminado.
5. Una ruta API ajena conserva su guardia de tamaño anterior; comprobar también
   un path próximo no exacto. DNS/reinicio, headers,15s y no-next-upstream no
   cambian; los tests contractuales existentes conservan estos oráculos.

No afirmar tiempo total garantizado ni memoria cero: hay buffers de transporte.
El API debe dejar de procesar al exceder límite y cerrar recursos; conservar
incertidumbre si la red corta después de una confirmación. Son oráculos futuros,
no pruebas ejecutadas ni permiso para modificar proxy global.
