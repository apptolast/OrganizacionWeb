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

---

# Revisión independiente parcial — disponibilidad backend

**Estado: revisión parcial favorable del núcleo. No aprueba el backend completo, el cierre de la función ni la ejecución de PIT.**

## Alcance y método

Lectura independiente de `Availability`, `AvailabilityRevision`, los puertos de consulta/escritura y catálogo, `ReadAvailability`, `SaveAvailability`, `JavaTimeZoneCatalog` y sus cuatro archivos de pruebas. Se contrastaron con `features/availability.feature` y los ciclos 1–11 de `tdd_availability_backend.md`. Ponytail full y Caveman lite aplicados: reutilización de `java.time`, estructuras estándar y puertos existentes, sin nuevas dependencias ni capas auxiliares.

REST, SQL, filtros de seguridad, serialización y concurrencia PostgreSQL siguen en TDD del autor. No se revisan como producto terminado ni se deduce su corrección del callback puro. No he ejecutado Gradle, pruebas ni mutación en esta revisión. El init compartido previo está documentado por el coordinador; no acredita el corte backend actual.

## Resultado del núcleo

- `Availability` exige identidad, propietario, zona textual, exactamente siete días, presupuestos entre 0 y 1440, versión no negativa y fechas coherentes. Copia el mapa y deriva el total; no conserva un total redundante ni consulta el catálogo al reconstruir una zona histórica.
- `SaveAvailability` valida pertenencia al catálogo antes de tocar almacenamiento, también para no-op. Dentro del callback compara identidad/revisión antes de decidir que el contenido es idéntico. La creación usa UUID propio y versión 0; la edición conserva identidad y creación, aumenta versión y usa el máximo entre reloj UTC truncado a microsegundos y fecha previa. No-op devuelve el snapshot anterior.
- `ReadAvailability` propaga el propietario al puerto y devuelve ausencia o snapshot sin resolver zonas históricas. La consulta de catálogo no consulta preferencias.
- `JavaTimeZoneCatalog` usa el conjunto exacto de `ZoneId.getAvailableZoneIds()` más UTC. No filtra aliases por longitud, abreviatura o barra; tampoco incorpora `SHORT_IDS` ni offsets libres. La aplicación devuelve orden léxico y la estructura Set evita duplicados.
- Los puertos no dependen de Spring, HTTP, JDBC ni RabbitMQ. No aparece publicación de eventos ni acceso a proyectos/tareas/historia desde el núcleo de disponibilidad.

## Pruebas y observaciones concretas

La lectura de pruebas cubre copia inmutable, reconstrucciones inválidas, creación y precisión, reloj que avanza/iguala/retrocede, no-op, conflicto anterior al no-op, zona histórica y catálogo exacto. La bitácora diferencia los RED y GREEN correspondientes; aquí no se reejecutaron.

1. **Trazabilidad menor:** `SaveAvailabilityTest.s11_checksRevisionBeforeNoOp` contiene las etiquetas `foreign` y `missingId`, pero ambas construyen el mismo caso: UUID aleatorio distinto del existente. No representa dos fronteras diferentes. Conviene conservar una sola fila o renombrar la segunda únicamente si se introduce una entrada distinta. El caso de ID nulo ya está expresado por `unconfigured`.
2. **Pendiente de evidencia final, sin fallo de producción observado:** la guarda `value == null` de cada presupuesto existe en `Availability`, pero la matriz de reconstrucción actual no incluye un mapa completo con valor null. Si ninguna prueba REST/SQL final alcanza esa condición, añadir una entrada concreta a la matriz sería suficiente; no requiere otra suite ni nueva lógica.
3. Las pruebas puras de conflicto invocan el callback con un snapshot. No demuestran bloqueo, carrera de primera inserción, rollback ni lectura consistente en PostgreSQL. Esos puntos permanecen pendientes del corte final del adaptador y sus pruebas reales, conforme al alcance solicitado.

No se identificó un bloqueo de comportamiento en la producción revisada. El formato compacto actual queda pendiente del formateador del autor antes del freeze; no se solicita un refactor ajeno al contrato.

## Puerta final pendiente

Ampliar este mismo informe tras congelación backend a REST/SQL, precedencia de errores, privacidad, ETag/cuerpo del mismo snapshot, carreras reales 200/412, supresión de INSERT/UPDATE y rechazo diferido de COMMIT con rollback. Verificar trazabilidad final de escenarios y resultados del corte conjunto. La integración de navegador y UX se documentará por separado, después del freeze conjunto autorizado.

## Ampliación de implementación tras freeze backend

**Veredicto de diseño y pruebas backend: APPROVED para pasar a la verificación coordinada.** Este dictamen amplía la revisión parcial anterior; no declara la función terminada ni ejecuta/autoriza por sí mismo PIT. El coordinador mantiene la puerta de init conjunto, mutación e integración.

Se revisaron ahora `AvailabilityController`, `PostgresAvailabilityStore`, V10, los cambios de `ApplicationConfiguration` y `ApiErrors`, las pruebas HTTP completas y el perfil PIT focal/global. No se modificó producción ni se ejecutó Gradle. La revisión contractual situada al principio de este archivo se conserva íntegra.

### Resolución de las observaciones parciales

`AvailabilityTest` incluye ahora `nullDay`. `AvailabilityRevision` rechaza revisión negativa y ausencia con versión distinta de cero mediante tres casos propios. La duplicidad de etiquetas del test puro se documenta honestamente: `AvailabilityApiTest.s11_returnsConflictWithoutChangingOwnOrForeignRows` sí diferencia una fila ajena existente de un UUID inexistente, conservando ambos propietarios. El formateador ya se aplicó.

### REST, seguridad y snapshot

El controller mantiene GET sin escrituras, DTO cerrado de cuatro campos y ETag construido desde el mismo objeto usado en el cuerpo. PUT acepta cuerpo crudo opcional para evaluar query y precondición antes del JSON. Rechaza claves duplicadas y documentos concatenados; valida raíz, extras léxicos, zona y siete días en orden. El token JSON `1.0` se rechaza como tipo no integral. La pertenencia al catálogo también se exige antes del no-op; GET conserva texto histórico sin resolverlo.

If-Match distingue ausencia de cabecera (428), formato inválido (400) e identidad/revisión obsoleta (412). No consulta preferencias por UUID de la cabecera: la búsqueda SQL usa exclusivamente el propietario de sesión. Los filtros existentes protegen las tres rutas, incluidas zonas, sin excepción pública nueva. Los tests comprueban sesión ausente/expirada, CSRF, Origin, no-store y los códigos exactos. ApiErrors añade sólo AVAILABILITY_CONFLICT y reutiliza STORAGE_UNAVAILABLE sin exponer causas SQL.

### SQL, concurrencia y rollback

V10 es aditiva: owner_id único, siete columnas NOT NULL con CHECK de rango, versión BIGINT no negativa y fechas coherentes. No referencia proyectos ni outbox. No se persiste total semanal.

El store obtiene la fila propia con FOR UPDATE y ejecuta dentro de la transacción la comparación de versión anterior al no-op. UPDATE incluye propietario, UUID y versión, y exige una fila. La primera inserción usa ON CONFLICT únicamente por propietario; después de esperar al ganador, la consulta bajo READ_COMMITTED distingue fila existente (412) de supresión sin fila (503). No hay upsert que reescriba al ganador. El resultado sólo sale del TransactionTemplate después de confirmar.

Los tests s19 y s20 retienen bloqueos PostgreSQL reales y observan dos conexiones esperando antes de liberarlos; exigen 200/412 y cuerpo/ETag de la única revisión ganadora. s21 retiene un escritor real y demuestra que el no-op anterior espera y después recibe 412. Los seis fallos de s25 cubren lectura, INSERT/UPDATE que lanzan o suprimen, y constraint trigger diferido que rechaza COMMIT. Comprueban 503, ausencia de ETag y snapshots conservados. Esta evidencia no se describe como pérdida de ACK después de commit.

### Trazabilidad y límites de evidencia

Se contrastó la tabla s1–s47 de `tdd_availability_backend.md` con métodos y matrices de entrada. Los escenarios backend s1–s27, s45 y s46 disponen de comprobaciones específicas; s28 sigue asignado al reinicio real de integración. Los escenarios UI no se dan por demostrados desde MockMvc. Las seis filas sintéticas de outbox en s18 prueban ausencia de efectos, no publicación ni validez de sus payloads.

El autor informa sesión 49498 con 194 pruebas y lint verdes; posteriormente reforzó nueve casos sin cambiar producción. La lectura independiente del XML actualmente disponible confirma **9 pruebas, 0 fallos, 0 errores y 0 omitidas** de ese último replay focal. Ese XML reemplaza el anterior: no lo presento como confirmación independiente de las 194 ni sumo ambos resultados. El init conjunto del coordinador verificará el corte completo.

### Perfil de mutación

`-PmutationScope=availability` incluye entidad/revisión, ambos casos de uso, controller y parsing, JDBC, catálogo y ApiErrors; incorpora pruebas históricas pertinentes de los handlers compartidos. El perfil global también incorpora los adaptadores y pruebas nuevos. Mantiene umbral 80 y constructores compactos mutables mediante desactivación de FRECORD; las exclusiones generales de equals/hashCode/toString no ocultan validación nueva. No se han ejecutado mutantes en esta revisión y ningún porcentaje se anticipa.

No quedan hallazgos de producción abiertos en el backend congelado. Pendientes de cierre global: init conjunto, mutación aprobada por el coordinador, reinicio con sesión/snapshot real y recorridos UI/UX sobre imagen definitiva.
