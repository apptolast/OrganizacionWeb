# TDD — external_calendar (feature 28)

Feature en curso: 28 — external_calendar. Worktree `C:/Users/vhurt/ow-worktrees/external-calendar`, rama `claude/external-calendar`.
Contrato: `features/external_calendar.feature` (@s1…@s40). Ponytail full / Caveman lite.

## Orden de trabajo

1. Parser iCalendar puro (dominio) — @s14, @s15, @s16, @s17, @s18, @s19, @s20, @s21, @s22.
2. Ventana y truncamiento (dominio) — @s23, @s24.
3. Validación de etiqueta y URL (dominio) — @s4, @s5.
4. Guardia de direcciones — @s4 (DNS), @s11.
5. Cifrado AES-GCM — @s2, @s3, @s9, @s29.
6. Persistencia V26 con Testcontainers — @s2, @s3, @s6, @s7, @s25, @s26, @s30, @s31, @s33.
7. Casos de uso — @s6, @s11, @s12, @s14, @s16, @s26, @s27, @s28, @s29.
8. Cliente HTTP del feed — @s12, @s13.
9. HTTP MockMvc — @s1, @s2, @s4, @s8, @s10, @s28, @s31, @s32; Hoy intacto — @s34.
10. Frontend — @s35…@s40; E2E; docs; registro de mutación.

## Bitácora de ciclos

### Ciclo 1 — parser iCalendar puro (@s14…@s22, @s12 malformado)

- ROJO: `IcsCalendarTest` cubre instantes UTC, TZID con DST real, VALUE=DATE y
  flotantes en la zona de instantánea, DURATION, cancelados y recurrentes,
  desplegado de líneas y escapes, UID duplicado, inválidos y componentes ajenos.
  Fixtures reales en `backend/src/test/resources/ics/`.
- VERDE: `IcsCalendar`, `ExternalEvent`, `IcsMalformedException`.
- Detalle de escapes: `\n`/`\N` → salto de línea, `\\` → barra, `\,` y `\;` →
  literales. El desplegado quita `CRLF`/`LF` seguidos de espacio o tabulador.

### Ciclo 2 — ventana y truncamiento (@s23, @s24)

- ROJO: `ExternalCalendarSnapshotTest` (intersección semiabierta, orden
  `startAt, uid`, corte en 500 con `truncated`).
- VERDE: `ExternalCalendarSnapshot`.

### Ciclo 3 — validación de etiqueta y URL (@s4 sintáctico, @s5)

- ROJO: `ExternalCalendarInputTest`.
- VERDE: `ExternalCalendarInput`.
- Corrección: el test del límite de 2048 usaba `url.substring(1)`, que rompía el
  esquema `https` y hacía fallar el caso frontera. Se separó en dos tests:
  2049 → `TOO_LONG` y 2048 exactos → aceptado con `urlTail` "WXYZ".

Comando: `backend\gradlew.bat test --no-daemon --tests '...domain.IcsCalendarTest'
--tests '...domain.ExternalCalendar*'` → 77 tests, 0 fallos.

### Ciclo 4 — direcciones prohibidas (@s4, @s11)

- ROJO: `BlockedAddressesTest` con 28 casos, incluidas las fronteras
  172.15.255.255 / 172.32.0.1 y 100.63.255.255 / 100.128.0.1, y las formas
  mapeadas `::ffff:10.0.0.1` frente a `::ffff:93.184.216.34`.
- VERDE: `BlockedAddresses.contains` desmapea IPv4-en-IPv6 y bloquea bucle
  local, enlace local, sitio local, no especificada, difusión múltiple,
  única local `fc00::/7` y espacio compartido `100.64.0.0/10`.
- Nota: `Inet6Address.isSiteLocalAddress` de Java solo cubre `fec0::/10`, por
  eso `fc00::/7` se comprueba aparte.

### Ciclo 5 — cifrado AES-256-GCM de la dirección (@s2, @s3, @s9, @s29)

- ROJO: `SecretUrlCipherTest`.
- VERDE: `SecretUrlCipher`: nonce de 12 bytes por escritura, propietario como
  AAD, formato almacenado `nonce || sellado`. Descifrar con otra clave, otro
  propietario, un cifrado truncado o con un bit cambiado devuelve vacío
  (que la aplicación traducirá a `SECRET_UNREADABLE`).
- La clave mal formada lanza `IllegalStateException` mencionando
  `APP_CONNECTOR_KEY` y nunca su valor.


### Ciclo 6 — el formato almacenado vuelve al contrato (@s2, @s3, @s29)

- ROJO: `s2_theStoredBytesStartWithTheNonceItself` exige que `HEADER_LENGTH`
  sea el propio nonce de 12 bytes. Fallaba con la cabecera de 13 bytes que
  dejó el commit de resguardo.
- VERDE: `SecretUrlCipher` almacena `nonce || sellado`, sin byte de versión.
- Decisión de contrato: el `.feature` (línea 45, @s2) y la propuesta
  ("nonce de 12 bytes seguido del cifrado") fijan el formato. La enmienda B5
  se conserva en lo que no contradice el contrato: sigue admitiéndose una
  clave anterior opcional, pero se prueba a ciegas y decide la etiqueta GCM,
  no un byte de versión. Con una sola clave configurada, @s29 se cumple.

### Ciclo 7 — guardia de direcciones con resolución DNS (@s4, @s11)

- ROJO: `OutboundHostGuardTest` con una zona DNS falsa inyectada por el puerto
  `HostResolver`: público permitido, sin direcciones `UNRESOLVABLE`, cada
  familia prohibida `BLOCKED`, y una sola prohibida entre públicas bloquea el
  host entero. Un resolutor que revienta también es `UNRESOLVABLE`, nunca
  permitido.
- VERDE: `HostResolver` y `OutboundHostGuard`.
- Riesgo residual anotado en el javadoc: rebinding DNS entre la comprobación y
  la conexión. Se mitiga repitiendo la comprobación en cada sincronización.

### Ciclo 8 — la criptografía sale de la capa de aplicación

- ROJO: `ArchitectureTest` fallaba con 15 violaciones. El commit de resguardo
  había dejado `SecretUrlCipher` en `application` dependiendo de
  `javax.crypto`, que no entra en `java..`.
- VERDE: puerto `application/SecretCipher` (encrypt/decrypt con el propietario
  como dato autenticado) y adaptador `adapter/crypto/AesGcmSecretCipher` con
  su test movido al mismo paquete.

### Ciclo 9 — descarga del feed (@s12, @s13)

- ROJO: `HttpCalendarFeedTest` contra un `com.sun.net.httpserver.HttpServer` en
  loopback: una sola petición GET con Accept text/calendar y sin cookie ni
  Authorization; 301/302/303/307/308 sin seguir y sin tocar el destino;
  cualquier estado distinto de 200; conexión rechazada; cabeceras que no llegan
  a tiempo; 1 MiB exacto aceptado; 1 MiB + 1 byte rechazado; cuerpo infinito
  abortado antes de 2 MiB; tipos textuales aceptados y no textuales rechazados.
- VERDE: `domain/FeedError` (códigos cerrados), puerto `application/CalendarFeed`
  con el resultado sellado `FeedFetch`, y `adapter/feed/HttpCalendarFeed`.
- El plazo de 5 s se inyecta para poder probar el vencimiento en 300 ms; un test
  aparte fija `HttpCalendarFeed.TIMEOUT` en 5 s y el cableado usa esa constante.

### Ciclo 10 — persistencia V26 (@s1, @s2, @s6, @s7, @s12, @s16, @s24, @s25, @s26, @s30, @s31, @s33)

- ROJO: `ExternalCalendarPersistenceTest` (Testcontainers) no compilaba: no
  existían `SyncStatus`, `SyncSummary`, `ExternalCalendarSubscription`,
  `StoredSubscription`, `ExternalCalendarStore` ni el adaptador.
- VERDE: migración `V26__external_calendar.sql` con las dos tablas de la
  propuesta y `PostgresExternalCalendarStore`.
- Decisiones que fija el test: `create` deja version 0 y todo a null o cero;
  `relabel` sube versión y conserva instantánea; `rebind` sube versión, borra
  eventos y reinicia contadores conservando la id; `commitSuccess` y
  `commitFailure` van condicionadas a la versión leída antes de descargar y
  devuelven vacío cuando pierden la carrera; el borrado arrastra la instantánea
  por clave ajena en cascada y es idempotente.
- El orden de `events` usa `uid COLLATE "C"` para que coincida con el orden de
  `String.compareTo` que aplica el dominio al truncar a 500.
