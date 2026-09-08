# Destilación de ics_calendar (feature 26)

Contrato `features/ics_calendar.feature` redactado el 8 de septiembre de 2026 desde
`progress/proposal_ics_calendar.md` como única fuente normativa. No se ha tocado
`project-spec.md`, `feature_list.json`, `src/` ni tests. Ponytail full y Caveman lite
aplicados; el contrato conserva prosa normal. 38 escenarios `@s1`–`@s38`, tags
consecutivos, un único `When` por escenario, 70 filas de Examples por conteo léxico.

Los dos documentos iCalendar de ejemplo (`@s12`, 714 octetos, y `@s13`, 158 octetos) se
generaron con un escritor de referencia en el scratchpad y se compararon línea a línea
con el docstring del contrato; el objetivo del bloque B se eligió para que los dos
octetos de «ó» caigan exactamente en la frontera de 75 octetos de `DESCRIPTION`, de modo
que la primera línea física mide 74 y no 75. El desplegado y desescapado devuelven el
objetivo persistido de 147 puntos de código. El generador no forma parte del producto.

## Mapa @s → sección de la propuesta

| @s | Sección de `proposal_ics_calendar.md` | Comportamiento |
| --- | --- | --- |
| s1 | Modelo y persistencia; API | POST genera, 201 `{url, createdAt}`, 43 caracteres base64url, fila con SHA-256 de 32 octetos |
| s2 | API (`GET /calendar-feed`) | Estado sin token: `active false`, `createdAt null`, sin escrituras |
| s3 | API; Seguridad y privacidad | Estado con token nunca expone token ni hash |
| s4 | Modelo (`ON CONFLICT`); Concurrencia | Regenerar sustituye e invalida el anterior desde el commit |
| s5 | API (`DELETE`) | Revocar borra, 204 idempotente, 404 posterior |
| s6 | API (JSON estricto) | Cuerpo no vacío en POST: 400 `VALIDATION_ERROR` sin tocar el token |
| s7 | API (sin sesión); Propósito y frontera (Bearer) | 401 `UNAUTHENTICATED` en gestión y descarga, Bearer no sustituye sesión |
| s8 | API (CSRF, OriginGuard, `permitAll`) | 403 `CSRF_INVALID` / `UNTRUSTED_ORIGIN` en POST y DELETE; feed sin CSRF |
| s9 | Concurrencia, idempotencia | Dos POST concurrentes: 201 ambos, una sola fila, una sola url válida |
| s10 | API (503 sin reintento) | Colisión de `token_hash`: 503 `STORAGE_UNAVAILABLE`, token anterior intacto |
| s11 | API y recurso público; Seguridad (logs) | 200 con cabeceras exactas, HEAD sin cuerpo, sin escrituras, sin token en logs |
| s12 | Formato iCalendar | Documento byte a byte: PRODID, VERSION, CALSCALE, METHOD, VEVENT completo, escapes, plegado, CRLF |
| s13 | Formato iCalendar (calendario vacío) | VCALENDAR sin componentes, 158 octetos, no 404/204 |
| s14 | Formato iCalendar (`X-WR-TIMEZONE`) | Presencia condicionada a disponibilidad, sin alterar instantes |
| s15 | API y recurso público (404 indistinguible) | Mismo 404 para malformado, desconocido, revocado, sustituido, ruta ajena y query |
| s16 | API y recurso público (405) | Solo GET y HEAD; `Allow: GET, HEAD` idéntico exista o no el token |
| s17 | API (`GET /api/v1/me/calendar.ics`) | Descarga con sesión, mismos octetos, `Content-Disposition`, con o sin token |
| s18 | Contenido (ventana semiabierta) | `end_at > now − 30d` y `start_at < now + 365d` con instantes concretos |
| s19 | Modelo (bloque vigente) | Cancelado excluido, reserva original intacta |
| s20 | Modelo (proyección); Formato (`SEQUENCE`, `DTSTAMP`) | Movido publica intervalo vigente, `SEQUENCE:2`, `DTSTAMP` de `updated_at` |
| s21 | Decisiones (`SUMMARY` = título de tarea) | No existe caso «proyecto»; estado de tarea/proyecto no excluye |
| s22 | Formato (UTC con `Z`) | DST Europe/Madrid 24/25 de octubre de 2026: misma hora local, UTC distinto |
| s23 | Formato (escape TEXT) | `\` `;` `,` LF, CRLF, CR aislado, `:` y comillas sin escapar |
| s24 | Formato (plegado 75 octetos) | Frontera UTF-8 de 2 y 4 octetos, escape atómico, línea de exactamente 75 |
| s25 | Formato (orden de eventos) | `DTSTART` asc, `UID` asc, salida determinista |
| s26 | Contenido (límite 2000/2001) | 413 `CALENDAR_TOO_LARGE` sin cuerpo parcial; cancelados no cuentan |
| s27 | Seguridad y privacidad | Aislamiento por propietario vía proyectos |
| s28 | Concurrencia (snapshot único) | Lectura íntegra de una sola versión |
| s29 | Concurrencia (reinicio) | El token sobrevive al reinicio del backend |
| s30 | API (503) | Almacenamiento caído: 503 sin documento parcial en feed, descarga y estado |
| s31 | Interfaz (ruta, navegación, estados) | Apertura con solo GET; cargando, sin enlace, activo, fallo |
| s32 | Interfaz (enlace recién creado) | URL una sola vez, aviso de secreto, doble clic = una petición |
| s33 | Interfaz (portapapeles) | `writeText` con la url exacta; sin portapapeles no se finge éxito |
| s34 | Interfaz (confirmación inline) | Regenerar y revocar abren confirmación sin enviar petición |
| s35 | Interfaz (confirmación); foco | Cancelar no envía; confirmar envía una sola; foco al h1 solo si procede |
| s36 | Interfaz (descarga reutilizando feature 22) | Validación de `Content-Type`, `Content-Length`, BEGIN/END + CRLF, 413, red, 401 |
| s37 | Interfaz (abandono, logout, identidad) | Aborta, descarta tardías, nada en storage ni consola |
| s38 | Interfaz (UX) | Teclado, 320/768/1280, texto 200 %, zoom 200 %, 44 px, axe, temas, movimiento reducido |

## Decisión sobre la pregunta abierta (registro de accesos del proxy)

Se adopta la opción más conservadora: el despliegue **excluye por completo** las rutas
bajo `/calendar/` de los access logs del proxy, en lugar de enmascararlas. Enmascarar
depende de una expresión regular que puede fallar con variantes de la ruta (sin `.ics`,
con query, con codificación por porcentaje); excluir la ruta entera no tiene ese modo de
fallo. Exponer el feed en producción queda condicionado a esa exclusión. La parte que sí
es exigible a la aplicación queda en el contrato: `@s11`, `@s15` y `@s30` afirman que los
logs de aplicación no contienen el token ni la ruta `/calendar/`. La exclusión en el
proxy pertenece a la infraestructura y debe anotarse en la documentación de despliegue;
no se puede verificar desde la suite del repositorio.

## Huecos detectados y resueltos

1. **`\n` en `SUMMARY`.** El encargo pedía `SUMMARY` escapado con `,` `;` `\n`, pero
   `SUMMARY` es el título de la tarea y un título no contiene saltos de línea. El ejemplo
   byte a byte pone `;` y `,` en `SUMMARY` y `\n`, `\`, `;` y `,` en `DESCRIPTION`
   (objetivo del bloque, que sí admite saltos). Los cuatro escapes quedan cubiertos en un
   solo documento verificable (`@s12`) y en la tabla de `@s23`.
2. **«Tarea vs proyecto» en `SUMMARY`.** La propuesta descarta el caso «proyecto» porque
   el esquema no admite bloques sin tarea. `@s21` fija `SUMMARY` = título de la tarea
   siempre, incluso con tarea o proyecto `completed` o proyecto `paused`, porque el único
   filtro de vigencia de la propuesta es la proyección `cancelled`. Ver duda 1.
3. **Presupuesto de la línea de continuación.** La propuesta dice «líneas de más de 75
   octetos se pliegan con CRLF seguido de un espacio» pero no fija si el espacio cuenta
   dentro de los 75. El contrato lo fija: toda línea física mide como máximo 75 octetos
   sin CRLF, incluido el espacio inicial de las continuaciones (74 octetos de carga).
   La alternativa cambiaría los 714 octetos de `@s12`; hay que decidirla antes del TDD.
4. **Escape en la frontera de plegado.** La propuesta prohíbe partir «un escape»; el
   contrato lo hace medible en `@s24` (la barra invertida de `\,` en el octeto 75 obliga
   a cortar en 74) y añade la línea de exactamente 75 octetos que no se pliega.
5. **405 como oráculo de existencia.** La propuesta fija 405 con `Allow: GET, HEAD` pero
   no dice si la respuesta puede variar según exista el token. `@s16` exige respuesta
   idéntica para token válido y token nunca emitido, coherente con el 404 indistinguible.
6. **HEAD y `Content-Length`.** `@s11` fija que HEAD devuelve `Content-Length: 714` con
   cuerpo vacío, para que los clientes que sondean con HEAD no vean un valor distinto.

## Dudas para el coordinador

1. `@s21`: la inclusión de bloques de tareas o proyectos `completed` se deduce de la
   definición de bloque vigente de la propuesta (solo la proyección `cancelled` excluye) y
   coincide con Hoy (sección 14 de `project-spec.md`, «conserva bloques de tareas y
   proyectos completed»). Confirmar que es la intención y no un olvido de la propuesta.
2. `@s6`: «cuerpo vacío» se interpreta como cero octetos. El frontend debe enviar el POST
   sin cuerpo; conviene fijar si envía `Content-Type: application/json` con cuerpo vacío
   (el backend no debe fallar al parsear) o sin `Content-Type`. Las filas `{}`, `null`,
   `""` y `[]` se rechazan por ser JSON no vacío.
3. `@s15`: no se exige medición de tiempo constante; se exige que la única clave de
   búsqueda sea el SHA-256 completo. Si el `judge` quiere una aserción temporal, habría
   que añadirla como prueba estadística fuera de la suite rápida.
4. `@s7`: la feature 24 (Bearer) no tiene contrato aún. El paso «aunque incluya
   Authorization Bearer con cualquier valor» debe seguir cierto cuando 24 aterrice: no
   se añade scope de calendario.
5. `@s26`: 2001 bloques en Testcontainers es costoso; se sugiere cubrir el umbral en el
   caso de uso con un almacén falso (`LIMIT 2001`) y dejar una sola prueba de integración
   con el umbral real, o marcarla como lenta.
6. `@s14`: la fila con `America/Bogota` supone que el catálogo de zonas de disponibilidad
   la admite; si no, sustituir por otra zona del catálogo sin cambiar la aserción.
7. Los textos de interfaz («Crear enlace de suscripción», «Copiar enlace», «Enlace
   copiado», «Creando enlace…», «Confirmar regeneración», «Confirmar revocación»,
   «Archivo preparado», «Reintentar») se toman de la propuesta o se proponen aquí; el
   humano puede cambiarlos en la puerta sin alterar el comportamiento.

## Decisiones del coordinador (8 de septiembre de 2026, 21:00)

1. @s21: los bloques vigentes de tareas o proyectos `completed` se incluyen (coherente con Hoy); sólo `cancelled` excluye. Confirmado.
2. @s6: el frontend envía el POST sin cuerpo y sin `Content-Type`; el backend acepta cero octetos y rechaza cualquier JSON no vacío.
3. Registro del proxy: se adopta la exclusión completa de `/calendar/` en los access logs; se anota como requisito de despliegue en `docs/`, no verificable desde la suite.
