# Calendario externo (feature 28)

Suscripción de **solo lectura** a un calendario iCalendar externo. La aplicación descarga y analiza;
nunca escribe en el proveedor. Contrato normativo: `features/external_calendar.feature`.

## Configuración

| Variable                                  | Obligatoria | Efecto                                                                 |
| ----------------------------------------- | ----------- | ---------------------------------------------------------------------- |
| `APP_CONNECTOR_KEY`                       | sí          | 32 bytes en base64. Sin ella las cinco rutas responden 503 `CONNECTORS_DISABLED`. Mal formada, el arranque falla sin revelar su valor. |
| `APP_CONNECTOR_KEY_PREVIOUS`              | no          | Clave anterior admitida solo para descifrar, durante una rotación.      |
| `APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES`  | no          | `false` por defecto. **Solo** para pruebas de extremo a extremo: desactiva la guardia SSRF. |

Generar una clave: `openssl rand -base64 32`.

## Rutas privadas

Todas bajo `/api/v1/me/external-calendar`, con sesión, `OriginGuard`, CSRF en escrituras, JSON
estricto, `Cache-Control: no-store` y errores `application/problem+json`.

| Método y ruta                     | Respuesta                                                        |
| --------------------------------- | ---------------------------------------------------------------- |
| `GET /`                           | `{configured, subscription}`; `subscription` es null o los quince campos. |
| `PUT /`                           | `{label, url}` exactos; devuelve `{configured: true, subscription}`. |
| `DELETE /`                        | 204 sin cuerpo, idempotente.                                      |
| `POST /sync`                      | `{onlyIfStale}` exacto; devuelve `{performed, subscription}`.      |
| `GET /events?from&to`             | `{configured, lastSyncAt, lastStatus, items}`; rango en UTC, semiabierto, máximo dieciséis días. |

La dirección completa no aparece en ninguna respuesta, log ni error: solo `urlHost` y `urlTail`
(los cuatro últimos caracteres).

## Seguridad

- **SSRF.** Suscribirse a una URL ajena es SSRF por diseño. Se valida la sintaxis (https, sin
  userinfo, sin fragmento, host de nombre y nunca IP literal), y después se resuelve el nombre y se
  exige que **todas** sus direcciones sean públicas: `application/OutboundHostGuard` sobre
  `application/PublicAddressPolicy`, con rangos CIDR explícitos para IPv4 e IPv6 (incluidos
  `100.64.0.0/10`, `fc00::/7`, 6to4 y NAT64, y la desnormalización de IPv4 mapeada). La comprobación
  se repite en **cada** sincronización, no solo al guardar.
  - *Riesgo residual aceptado*: entre la comprobación y la conexión el DNS puede cambiar (rebinding).
    Se mitiga repitiendo la comprobación y limitando lo que se puede hacer con la respuesta: solo se
    lee, con 200, tipo textual, 1 MiB y 5 s.
- **Descarga.** Redirecciones deshabilitadas, `Accept: text/calendar`, sin cookie ni `Authorization`,
  plazo de 5 s, aborto al superar 1 MiB, y solo 200 con `Content-Type` `text/*`.
- **Secreto en reposo.** AES-256-GCM, nonce aleatorio de 12 bytes por escritura y `owner_id` como
  dato adicional autenticado. Formato almacenado: `nonce || sellado`. Una clave que ya no descifra se
  comunica como `SECRET_UNREADABLE` y pide volver a pegar la dirección.

## Sincronización

No hay planificador en segundo plano: la disparan Hoy (con `onlyIfStale`) y el botón manual. El
umbral de frescura son 15 minutos medidos sobre `lastAttemptAt`, para no insistir contra un feed
caído en cada carga de Hoy. La descarga ocurre fuera de toda transacción y se confirma condicionada
a la `version` leída antes de empezar: la sincronización perdedora se descarta con `performed: false`
y no se reintenta.

Códigos cerrados de fallo: `FEED_REJECTED`, `FEED_UNREACHABLE`, `FEED_HTTP_ERROR`, `FEED_TOO_LARGE`,
`FEED_UNSUPPORTED_TYPE`, `FEED_MALFORMED`, `SECRET_UNREADABLE`. Un fallo es 200 con
`lastStatus: FAILED` y la instantánea anterior intacta; nunca una lista parcial.

## Análisis del iCalendar

Analizador propio (`domain/IcsFeed`), sin dependencias nuevas. Solo `VEVENT`; `UID` y `DTSTART`
obligatorios; `Z` es UTC; `TZID` debe pertenecer al catálogo de zonas; flotantes y `VALUE=DATE` se
resuelven en la zona de instantánea (la de disponibilidad si es resoluble, si no UTC), guardada en
`snapshotZoneId`. `RRULE`, `RDATE` y `RECURRENCE-ID` se cuentan en `skippedRecurring` y no se
expanden. Un `UID` repetido conserva el primero. Ventana almacenada: intersección semiabierta con
`[syncAt - 24 h, syncAt + 336 h)`, orden `startAt, uid`, máximo 500 y `truncated`.

## Hoy no cambia

El DTO de Hoy es idéntico con y sin suscripción, y los eventos externos no participan en el cálculo
de solapes. Lo garantiza `ExternalCalendarIsolationTest` por arquitectura: ninguna clase de Hoy ni de
la planificación puede depender del carril. La sección "Calendario externo" se compone en el
frontend, tras la instantánea de Hoy.

## Mutación

`bin/harness mutate external_calendar-backend` (PIT, alcance `external_calendar`) y
`bin/harness mutate external_calendar-frontend` (Stryker,
`frontend/stryker.external-calendar.config.json`). Umbral 80 % en ambos.
