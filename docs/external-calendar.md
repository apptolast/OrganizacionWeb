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
  - **Reenlace de nombres (DNS rebinding): cerrado, no aceptado.** La enmienda B3 de
    `project-spec.md:2492` dejó de admitirlo como límite. `HttpCalendarFeed` resuelve el nombre una
    vez, exige que **todas** las direcciones devueltas pasen la política y **conecta contra la
    dirección literal ya validada**, conservando el nombre original en la cabecera `Host` y en el
    `SNIHostName` de TLS. Como el cliente HTTP recibe una dirección y no un nombre, no hay segunda
    resolución y por tanto no hay ventana entre la comprobación y el uso. Las tres piezas del
    anclaje viven en `adapter/net/AnchoredConnection`, compartidas con el emisor de webhooks de la
    feature 25, que ancla igual: es una sola decisión y tiene un solo sitio.
  - **El TLS del camino anclado está probado**, que es el punto que puede romper: anclar a una
    dirección suele tirar abajo la verificación del certificado. `HttpCalendarFeedTest` lo mide en
    los dos sentidos contra un `HttpsServer` con el fixture
    `src/test/resources/tls/anchored-receiver.p12` —autofirmado, con `SAN` de tipo `dNSName` y
    **ninguna** de tipo `iPAddress`, que es lo que lo convierte en oráculo—: un certificado válido
    para el nombre se acepta aunque la conexión vaya a `127.0.0.1`, el **mismo** certificado se
    rechaza si el nombre pedido es otro, y uno que nadie avala se sigue rechazando. Los tres dan
    `FEED_UNREACHABLE` al fallar, porque el conjunto cerrado de códigos de la feature no tiene una
    clase propia para TLS.
  - Lo que el anclaje cuesta, escrito y no escondido: la aplicación necesita
    `jdk.httpclient.allowRestrictedHeaders=host` (se activa sola, añadiéndose a lo que hubiera
    declarado el despliegue); la descarga habla **HTTP/1.1**, porque sobre HTTP/2 la autoridad la
    fija la URI y el proveedor no vería el nombre; y si el nombre resuelve a varias direcciones se
    usa la primera y no se reintenta con las demás. Esto último es pérdida de tolerancia a fallos,
    no de seguridad: todas estaban validadas.
  - La política de egreso de `deploy/EGRESS.md` sigue siendo obligatoria, pero como **defensa en
    profundidad**: la contención del reenlace es de la aplicación.
- **Descarga.** Redirecciones deshabilitadas, `Accept: text/calendar`, sin cookie ni `Authorization`,
  **plazo total de 5 s para el intercambio completo** —conexión, cabeceras y lectura del cuerpo—,
  aborto al superar 1 MiB, y solo 200 con `Content-Type` `text/*`. El plazo del cuerpo no lo da
  `HttpRequest.timeout`: con `BodyHandlers.ofInputStream()` ese temporizador se cancela al llegar
  las cabeceras, así que la clase fija un instante límite propio y cierra el cuerpo al vencerlo.
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
