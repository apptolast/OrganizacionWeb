# Revisión de propuesta — disponibilidad

Revisión documental con Ponytail full y Caveman lite. Feature 10 sigue pending; no se modifica producción, contrato aprobado ni metadatos. Leídas la propuesta y su recomendación backend.

## Dictamen

Favorable al corte de una zona horaria y siete presupuestos diarios, sin ventanas ni evento personal. Un presupuesto de 90 minutos no afirma que una franja concreta esté libre. Cero permite descanso, incluso los siete días; no debe presentarse como incumplimiento. No añadir calendarios, motor de recurrencia ni sumar disponibilidad a tiempo trabajado.

Hay una precisión necesaria en la propuesta backend: sustituir «sin aliases abreviados» por «pertenencia exacta al catálogo publicado, sin aplicar SHORT_IDS ni aceptar offsets introducidos libremente». `getAvailableZoneIds()` no garantiza excluir identificadores históricos o abreviados del catálogo. No inventar un filtro por longitud o por presencia de `/`. La documentación distingue el catálogo disponible de los mapas adicionales de aliases y de los offsets; no equivale a una promesa de nombres canónicos únicos. [ZoneId, Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZoneId.html#getAvailableZoneIds()).

## Contrato mínimo recomendado

- `GET /api/v1/me/availability/zones`: sesión requerida, HTTP 200 con `{items: string[]}` ordenado, sin duplicados; conjunto backend de IDs disponibles más `UTC`. Se conserva el identificador exacto, sin normalizarlo a otro alias. Lista no-store; fallo recuperable y sin fallback silencioso.
- `GET /api/v1/me/availability`: HTTP 200 con exactamente `{configured, zoneId, dailyMinutes, updatedAt}` y ETag fuerte. Ausencia: `false` y tres null, ETag literal `"availability:unconfigured"`; no INSERT al leer. Configurada: `true`, ID permitido, mapa completo y fecha UTC confirmada. UUID/version permanecen internos salvo ETag opaco `"availability:<uuid>:<version>"`. No hacen falta ownerId ni un segundo total semanal en la respuesta.
- `PUT /api/v1/me/availability`: reemplaza exactamente `{zoneId, dailyMinutes}` con If-Match. dailyMinutes contiene sólo MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY. Cada valor es número JSON entero entre 0 y 1440; sin redondeo, coerción de texto, null ni claves desconocidas. Zona textual de pertenencia exacta; no trim ni conversión de caja ocultos. Total semanal derivado, entre 0 y 10080.
- Guardado confirmado devuelve HTTP 200 con la representación de GET y ETag del mismo snapshot. Primer guardado inserta una fila propia; siguientes incrementan versión sólo ante cambio real. No-op con revisión vigente conserva contenido, fecha y versión. Revisión anterior devuelve 412 aunque los valores coincidan con los actuales.
- If-Match ausente: 428 PRECONDITION_REQUIRED. Sintaxis no admitida, lista de tags, tag débil o versión fuera de BIGINT: 400 VALIDATION_ERROR en If-Match. Tag válido que ya no representa la fila propia (incluido literal inicial después de configurar): 412 AVAILABILITY_CONFLICT. No buscar otras filas por el UUID del tag; la identidad siempre viene de la sesión.
- Sin sesión: 401; Origin/CSRF siguen el contrato actual; contenido no JSON: 415. JSON vacío, truncado, duplicado o concatenado: 400 MALFORMED_JSON. Ausencias/null de campos obligatorios: REQUIRED; tipo incorrecto: INVALID_TYPE; minutos fuera de rango: OUT_OF_RANGE; zona fuera de catálogo: INVALID_VALUE; extra: UNKNOWN_FIELD. Usar paths de campo como `dailyMinutes.MONDAY`. Al enumerar varios errores, fijar orden zoneId y luego lunes a domingo; extras en orden léxico. No mezclar varios defectos en una fila de Examples salvo casos explícitos de precedencia.
- La validación de precondición precede al cuerpo dentro del handler; filtros de sesión/CSRF/origen y negociación de contenido mantienen la precedencia heredada. Persistencia inaccesible: 503 STORAGE_UNAVAILABLE, nunca `configured:false` inventado. Respuestas nuevas no-store y errores sanitizados.

## Concurrencia y conservación

Una fila por owner_id con UNIQUE. La creación simultánea desde ausencia debe producir un 200 y un 412, sin duplicados. Traducir únicamente la colisión esperada de esa unicidad a conflicto; otros fallos SQL siguen siendo 503. Para edición, comparar ETag y contenido dentro de la misma transacción protegida por bloqueo de la fila propia o UPDATE condicionado equivalente. No-op también comprueba revisión. No serializar usuarios distintos con un bloqueo global.

Un guardado fallido no altera fila ni fecha/revisión; una respuesta perdida puede haber confirmado. El cliente conserva el borrador y ofrece consultar la versión guardada, sin reenviar PUT automáticamente. La consulta tras conflicto no reemplaza el borrador de forma implícita: mostrar versión guardada para comparar y permitir «Usar versión guardada» o «Guardar mi borrador» con la nueva revisión. La segunda opción es una escritura deliberada; otro conflicto sigue siendo posible. No agregar fusión por día ni last-write-wins oculto.

Cambiar zona o presupuesto no desplaza instantes ya planificados ni cambia historia, tareas, estimaciones o eventos existentes. Sin evento nuevo en este corte: ninguna fila de outbox ni proyecto ficticio. Los seis esquemas y rutas previos permanecen como están.

## UX mínima verificable

Una sección «Disponibilidad» con selector nativo «Zona horaria» alimentado por el catálogo backend y siete campos numéricos en orden lunes–domingo, etiquetados «Lunes · minutos», etc. Mostrar «0 permite descansar» y «Presupuesto diario; no reserva un horario». El total semanal puede resumirse como cálculo del borrador, rotulado así; no es una obligación ni progreso ganado.

Antes de configurar, mostrar «Sin configurar» aunque se sugiera la zona del navegador y se rellenen siete ceros como borrador inicial. La sugerencia sólo se usa si pertenece al catálogo; si no, mantener la selección vacía. No consultar Intl para decidir qué acepta la API. Si no carga el catálogo, conservar el borrador y ofrecer reintentar; no asumir UTC ni habilitar un guardado inválido.

Separar snapshot confirmado y borrador en memoria. Cambiar un campo muestra «Cambios sin guardar»; sólo PUT válido muestra «Disponibilidad guardada». Mantener campos ante 400/412/503/red y dirigir el foco al campo inválido. No confundir input numérico incompleto como `1e` con cero. No persistir preferencias/borradores privados en localStorage. Al abandonar la vista, el borrador no pasa a ser guardado; cualquier descarte debe ser explícito en el recorrido que lo provoque. Evitar una infraestructura nueva de autosave o navegación para este formulario.

Errores de autenticación retiran datos privados; respuestas tardías tras ruta/logout no restauran preferencias anteriores. Durante guardado hay un solo envío y feedback accesible menor de 400 ms, sin robar otro foco. Verificar selector por teclado/touch, campos y controles de al menos 44 × 44, 22 anchos existentes, zoom real 200 % y 320 CSS. La matriz de treinta principios debe declarar pendiente planificación con inicio/fin: esta pantalla aún no verifica Parkinson ni una sesión de concentración.

Si una zona previamente guardada desapareciese del catálogo tras actualizar el runtime, GET debe conservar su valor para no fingir otra preferencia; el formulario lo identifica como no disponible y exige una selección soportada antes de guardar. No requiere resolver cambios DST aquí: las horas ambiguas/inexistentes pertenecen al futuro bloque con fecha y zona.

Estas decisiones quedan propuestas para que el coordinador cierre el contrato. No se activa feature 10 ni se solicita ampliar el alcance de feature 9.
