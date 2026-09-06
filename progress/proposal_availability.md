# Preparación documental — disponibilidad

Preparación previa al contrato de feature 10. Complete_reopen_task se cerró y publicó en e1afc11; el contrato definitivo está aprobado en `features/availability.feature`, con dictamen en `review_availability_contract.md`. El borrador histórico conserva 47 escenarios, 237 casos expandidos y un When por escenario. Estas decisiones del coordinador sustituyen las alternativas de comparación/fusión y descarte global consideradas en revisiones anteriores; los resultados de implementación se registrarán aparte.

## Alcance mínimo acordado para el borrador

Una zona y siete presupuestos diarios, sin ventanas de comienzo/fin. Cada día ISO de MONDAY a SUNDAY contiene un entero JSON de 0 a 1440 minutos; los siete ceros son válidos y permiten descanso sin penalización. Un presupuesto no asegura que una hora concreta esté libre, no reserva bloques y no acredita trabajo realizado. No hay autosave, calendario externo, recurrencias ni conectores nuevos.

El backend publica un catálogo ordenado y sin duplicados de `getAvailableZoneIds()` más UTC. Se valida pertenencia exacta al escribir, conservando el ID seleccionado. No filtrar aliases históricos por abreviación o falta de barra, no añadir SHORT_IDS ni aceptar offsets libres. La JVM y el navegador no tienen por qué compartir catálogo; Intl no decide qué admite la API. [ZoneId, Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZoneId.html#getAvailableZoneIds()). Una sugerencia del navegador sólo rellena borrador si está admitida; nunca se presenta como preferencia persistida.

La rehidratación de una fila conserva la zona histórica como texto, aunque haya desaparecido del catálogo; no necesita resolverla mediante ZoneId ni consultar el catálogo para devolver GET. La interfaz la muestra como no disponible y exige escoger una zona actual antes de guardar. PUT valida pertenencia incluso para una intención idéntica a la fila anterior.

## API y persistencia

`GET /api/v1/me/availability` devuelve exactamente configured, zoneId, dailyMinutes y updatedAt. Ausencia: false y tres null, HTTP 200 y ETag fuerte literal `"availability:unconfigured"`; leer no escribe. Configurada: true, zona textual, siete presupuestos y fecha UTC, con ETag `"availability:<uuid>:<version>"` obtenido del mismo snapshot. `GET /api/v1/me/availability/zones` devuelve exactamente items. Todos requieren sesión, rechazan query params y mantienen no-store.

PUT reemplaza exactamente zoneId/dailyMinutes con If-Match. Primer INSERT usa UUID propio, versión 0 y createdAt=updatedAt del reloj truncado a microsegundos. Updates reales suman uno y usan máximo entre reloj truncado y updatedAt previo; no-op vigente conserva todo. La revisión se comprueba antes del no-op. La fila se resuelve desde sesión, nunca por un propietario enviado ni mediante búsquedas de UUID ajenos.

Una fila por owner_id único, siete columnas con CHECK de rango, versión BIGINT no negativa y fechas UTC. La carrera inicial produce 200/412: INSERT que no inserta por existir la fila propia es conflicto, pero supresión de INSERT sin fila resultante es 503. No usar upsert que sobrescriba al ganador. La edición compara snapshot/revisión dentro de transacción con bloqueo de fila propia o mecanismo equivalente; otros usuarios no se serializan globalmente.

If-Match ausente produce 428; sintaxis ambigua, débil o fuera de BIGINT produce 400; revisión válida que no corresponde al snapshot propio produce 412 AVAILABILITY_CONFLICT. JSON estricto distingue sintaxis, forma, claves, tipos y rango; `1.0` es token no integral y recibe INVALID_TYPE, aunque un campo numérico de la UI pueda convertir una entrada válida a un número entero antes de serializar. No confundir `1e` incompleto con cero. El borrador fija el orden de errores y variantes independientes.

Fallo SQL produce 503 sin éxito falso. La prueba de COMMIT usa un constraint trigger diferido que rechaza antes de confirmar, por lo que puede comprobar rollback. Pérdida de respuesta después del commit es resultado incierto; no se promete deshacerlo y se exige consultar antes de otra escritura.

## Formulario y recuperación

Ruta exacta `/disponibilidad`, sin query ni sufijos, incluida en retorno autenticado. Selector nativo de zona y siete campos lunes–domingo. Mantener snapshot con ETag, borrador textual y catálogo por separado. Mostrar permanentemente «Los cambios sin guardar se pierden al salir». «Cancelar y volver a Proyectos» comunica descarte y lleva a `/proyectos`. La garantía de descarte explícito cubre las acciones del formulario; no se añade guardia global de navegación ni beforeunload. Logout o pérdida de sesión retiran inmediatamente datos privados.

La ausencia confirmada puede mostrar sugerencia y siete ceros como borrador «Sin configurar». Puede guardarse sin forzar una edición previa. El total semanal se calcula sólo con siete enteros válidos; en otro caso se muestra «Completa los siete presupuestos para calcular el total». No sumar parcialmente ni convertir vacío en cero. Un reintento del catálogo no sobrescribe ediciones ya hechas.

Campos y Guardar quedan bloqueados durante PUT o recuperación. Sólo una respuesta configurada válida, con ETag válido y zona/minutos idénticos a la intención enviada, confirma «Disponibilidad guardada». GET inicial inválido, ETag incoherente o respuesta mal formada no inventan ausencia ni habilitan Guardar. Errores de campo 400 conservan borrador y permiten corregir. Tras 412, 503, red o confirmación inválida se exige «Recargar versión guardada» antes de otra escritura.

La recarga explica el descarte y sólo un GET válido reemplaza todo el formulario y ETag; no compara, fusiona ni reenvía el borrador anterior. Si falla, conserva borrador y permite reintentar GET. Las tres peticiones (preferencia, catálogo y PUT) usan sesión/CSRF/cancelación existentes. Éxitos y rechazos antiguos no restauran datos ni revocan acceso vigente desde callbacks obsoletos.

Controles de al menos 44 × 44, feedback accesible antes de 400 ms, 22 anchos existentes, zoom nativo 200 % a 320 CSS y matriz de treinta principios corresponden a la futura implementación y revisión real. No se atribuye una prueba física o cumplimiento universal a este documento.

## EDA y límites temporales

No se emite evento de disponibilidad en este corte: no hay consumidor ni requisito causal aprobado. La FK del outbox a projects permanece íntegra y no se inventa un proyecto personal. Las seis rutas existentes no cambian. Cuando sea necesario un hecho personal, deberá aprobarse su propio contrato y persistencia.

Cambiar zona o presupuestos no mueve instantes planificados ni reescribe tareas, historia o tiempo trabajado. Resolución de horas inexistentes/repetidas, días de 23/25 horas y capacidad excedida por reservas pertenecen al futuro contrato de bloques. La disponibilidad actual sigue siendo un presupuesto abstracto.
