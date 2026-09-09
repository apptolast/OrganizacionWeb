# Calendario ICS (feature 26)

Feed iCalendar de solo lectura sobre los bloques planificados vigentes del propietario, más la
descarga del mismo documento desde la sesión web. Contrato: `features/ics_calendar.feature`
(@s1–@s38). Detalle normativo: sección «Feature 26» de `project-spec.md`.

## Direcciones

| Método | Ruta | Autenticación | Respuesta |
| --- | --- | --- | --- |
| GET, HEAD | `/calendar/{token}.ics` | ninguna: el token de la ruta es la capacidad | 200 `text/calendar; charset=utf-8` |
| GET | `/api/v1/me/calendar-feed` | sesión | 200 `{active, createdAt}` |
| POST | `/api/v1/me/calendar-feed` | sesión + CSRF + Origin | 201 `{url, createdAt}` |
| DELETE | `/api/v1/me/calendar-feed` | sesión + CSRF + Origin | 204 |
| GET | `/api/v1/me/calendar.ics` | sesión | 200 con `Content-Disposition: attachment` |

Cabeceras de todo documento y de todo error del feed: `Cache-Control: private, no-store` y
`X-Content-Type-Options: nosniff`. La cadena de seguridad del feed emite también
`Content-Security-Policy` y `Referrer-Policy: same-origin`, como las otras dos (hallazgo A8).

> Desviación conocida: el controlador fija `text/calendar; charset=utf-8`, pero Tomcat reserializa
> el tipo y entrega `text/calendar;charset=utf-8`, sin el espacio opcional que RFC 9110 permite.
> Ninguna capa de la aplicación puede evitarlo. Ver `progress/tdd_ics_calendar.md`. El feed público no emite `Content-Disposition`, `Set-Cookie`
ni `ETag`. Sólo se admiten GET y HEAD; el resto responde 405 con `Allow: GET, HEAD` **antes** de
mirar el candidato, para que un token real y uno inventado sean indistinguibles.

Errores: `application/problem+json` con `type`, `title`, `status` y `code`.
`CALENDAR_NOT_FOUND` (404, idéntico byte a byte para token malformado, desconocido, revocado,
sustituido o ruta ajena), `CALENDAR_TOO_LARGE` (413, más de 2000 eventos en la ventana),
`STORAGE_UNAVAILABLE` (503), `VALIDATION_ERROR` (400, cuerpo no vacío en POST),
`UNAUTHENTICATED` (401), `CSRF_INVALID` y `UNTRUSTED_ORIGIN` (403).

`createdAt` viaja como instante UTC con seis decimales siempre presentes
(`2026-09-08T12:00:00.000000Z`), tal y como fija el contrato.

## Token

32 octetos de `SecureRandom` en base64url sin relleno: exactamente 43 caracteres `[A-Za-z0-9_-]`.
Sólo se persiste su SHA-256 (`calendar_feed_tokens.token_hash`, `BYTEA` de 32 octetos con
`UNIQUE` y `CHECK`), en una fila por propietario (`owner_id` es la clave primaria). Generar sobre
una fila existente la sustituye con `INSERT … ON CONFLICT (owner_id) DO UPDATE`; revocar la borra.
La resolución busca por el hash completo, sin comparación parcial ni por prefijo. El token en claro
sólo existe en la respuesta del POST que lo emite: no se guarda, no se registra y no vuelve a
mostrarse.

Migración: `V24__calendar_feed_tokens.sql`. No altera ninguna tabla anterior y no escribe outbox
ni eventos.

## Contenido del documento

Bloques vigentes (`coalesce(projection.status,'planned')='planned'`) que intersecan la ventana
semiabierta `[now − 30 días, now + 365 días)`, es decir `end_at > from` y `start_at < to`. La regla
vive en `domain/CalendarWindow` y el SQL de `PostgresCalendarStore` es su espejo.

Orden fijo de propiedades del VCALENDAR, `X-WR-TIMEZONE` sólo si hay disponibilidad configurada, y
VEVENT con `UID`, `DTSTAMP`, `DTSTART`, `DTEND`, `SUMMARY`, `DESCRIPTION`, `SEQUENCE` y `URL` en
ese orden. Los eventos se ordenan por `DTSTART` y después por `UID`, de modo que dos peticiones
seguidas devuelven los mismos octetos. Escapes TEXT (`\\`, `\;`, `\,`, `\n`, CR aislado eliminado)
y plegado a 75 octetos sin partir un punto de código UTF-8 ni una secuencia de escape.

La lectura pública y la descarga se ejecutan dentro de una única transacción read-only
`REPEATABLE_READ` (`SnapshotRenderCalendar`): resolución del token, zona de disponibilidad y
bloques salen del mismo snapshot.

## Despliegue

`deploy/nginx.conf` enruta `location /calendar/` al backend con `access_log off` y con
`proxy_hide_header` de `X-Content-Type-Options`, `Content-Security-Policy` y `Referrer-Policy`: el
backend ya las emite y el `add_header` del bloque `server` las duplicaría. El `access_log off` es
obligatorio: el token viaja en la ruta y no debe aparecer en ningún registro de acceso. Esto resuelve la
pregunta abierta que dejó la propuesta de la feature 26. Cualquier proxy o CDN que se añada por
delante debe repetir la exclusión o enmascarar la ruta.

## Interfaz

Ruta `/calendario`, entrada «Calendario» en la navegación Principal justo después de
«Exportación». Estados: cargando, sin enlace, enlace activo, enlace recién creado, confirmación
inline, descarga preparada y fallo con reintento manual. La URL sólo vive en la memoria de la
vista: no se escribe en `localStorage`, `sessionStorage` ni consola, y salir de la ruta, cerrar
sesión o cambiar de identidad la retira y aborta la petición en vuelo.

## Mutación

- Backend: `node scripts/project.mjs mutate ics_calendar-backend`
  (`gradlew pitest -PmutationScope=ics_calendar`), informe en
  `backend/build/reports/pitest-ics-calendar`.
- Frontend: `node scripts/project.mjs mutate ics_calendar-frontend`
  (`stryker.ics-calendar.config.json`), informe en
  `frontend/reports/mutation-ics-calendar`.

Umbral 80 % en ambos, sin rebajas.
