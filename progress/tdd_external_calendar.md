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
   Corrección del 10-09-2026: cuando se escribió esta línea @s34 sólo tenía
   `ExternalCalendarIsolationTest` (ArchUnit) y una E2E *sin* suscripción; no había
   ningún MockMvc que ejecutara su Given. Lo hay desde el hallazgo 11 del dictamen:
   `ExternalCalendarTodayApiTest`.
10. Frontend — @s35…@s40; E2E; docs; registro de mutación.

## Bitácora de ciclos

### Ciclo 1 — parser iCalendar puro (@s14…@s22, @s12 malformado)

- ROJO: `IcsFeedTest` cubre instantes UTC, TZID con DST real, VALUE=DATE y
  flotantes en la zona de instantánea, DURATION, cancelados y recurrentes,
  desplegado de líneas y escapes, UID duplicado, inválidos y componentes ajenos.
  Fixtures reales en `backend/src/test/resources/ics/`.
- VERDE: `IcsFeed`, `ExternalEvent`, `IcsFeedMalformedException` (renombrados en el reasentamiento sobre `main`: `IcsCalendar` es el escritor de la feature 26).
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

Comando: `backend\gradlew.bat test --no-daemon --tests '...domain.IcsFeedTest'
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

### Ciclo 11 — caso de uso de sincronización (@s11, @s12, @s13, @s14, @s16, @s23, @s24, @s26, @s27, @s28, @s29)

- ROJO: `SyncExternalCalendarTest` con un almacén en memoria, un feed que
  registra las descargas y una auditoría que registra las líneas.
- VERDE: `SyncExternalCalendar` más `SyncOutcome`, `ExternalCalendarAudit`,
  `OutboundGuard` (interfaz funcional que ahora implementa `OutboundHostGuard`)
  y `ExternalCalendarNotConfiguredException`.
- Orden fijado por los tests: descifrar, guardia de direcciones, descarga,
  análisis. Los dos primeros fallos no emiten ninguna petición HTTP.
- El reloj se lee una sola vez: `lastAttemptAt` y `lastSyncAt` coinciden en el
  éxito. `imported` cuenta todos los VEVENT válidos, también los que quedan
  fuera de la ventana almacenada.
- Corrección de un test mal planteado: la carrera de @s26 no se reproduce
  escribiendo antes de `execute` (entonces se lee la versión nueva). El
  competidor se confirma *durante* la descarga, con un gancho en el feed.
- La zona de instantánea cae a `ZoneId.of("UTC")`, no a `ZoneOffset.UTC`, cuyo
  identificador es "Z" y no "UTC".

### Ciclo 12 — guardar, borrar y leer (@s2, @s3, @s4, @s5, @s6, @s7, @s31, @s32, @s33)

- ROJO: `SaveExternalCalendarTest` exige que guardar la misma dirección otra vez
  deje un cifrado distinto (@s3) conservando la instantánea (@s6). El puerto
  tenía `relabel(owner, label, now)` y no volvía a sellar.
- VERDE: `relabel` recibe ahora el cifrado nuevo; `SaveExternalCalendar` decide
  entre crear, resellar con otra etiqueta, reasignar dirección o no escribir
  nada. Una dirección que ya no se puede descifrar cuenta como distinta, así que
  reasigna y limpia la instantánea.
- También verde: `DeleteExternalCalendar`, `ReadExternalCalendar`,
  `ReadExternalCalendarEvents` con `ExternalEventsView`, y el rango de dominio
  `ExternalEventsRange` con `ExternalEventsRangeTest` (@s32).

### Ciclo 13 — las cinco rutas HTTP (@s1, @s2, @s4, @s7, @s8, @s9, @s10, @s28, @s31, @s32)

- ROJO: `ExternalCalendarApiTest` (37 casos) y `ExternalCalendarDisabledApiTest`
  no compilaban: no existía el controlador.
- VERDE: `ExternalCalendarController` con los DTO cerrados (dos campos en la
  suscripción, quince en `subscription`, cuatro en la lectura de eventos y cinco
  por evento), `Cache-Control: no-store` en todas, JSON estricto con campos
  desconocidos rechazados y `ConnectorsGate`.
- Puertos de entrada `ExternalCalendarUseCases.{Read,Save,Delete,Sync,ReadEvents}`
  para poder doblar los casos de uso, que son clases finales.
- `ConnectorsGate` va detrás de `AuthorizationFilter` en las dos cadenas de
  seguridad: así 401, 403 de CSRF y 403 de Origin siguen decidiendo antes que el
  503 de conectores, y el cuerpo no se llega a leer.
- Corrección nacida de un test: la guardia comparaba con `startsWith`, así que
  también habría capturado `/api/v1/me/external-calendars`. Ahora exige la ruta
  exacta o algo colgando de ella.
- Cableado en `ApplicationConfiguration` con su `ExternalCalendarWiringTest`:
  clave mal formada detiene el arranque sin revelar su valor (@s9), la política
  de direcciones solo se relaja a propósito y el feed usa los 5 s y 1 MiB del
  contrato.

### Ciclo 14 — Hoy no cambia y nadie sincroniza al arrancar (@s30, @s34)

- ROJO: `ExternalCalendarIsolationTest` no compilaba.
- VERDE: reglas ArchUnit que impiden que `ReadToday`, `PlanBlock`,
  `TodayController` y `BlockController` dependan de nada del carril, y que nada
  ajeno al carril dependa de su almacén ni de su sincronización. Así la
  respuesta de Hoy y el cálculo de solapes no pueden cambiar por tener una
  suscripción: no hay ningún camino de código que los una.
- También: el único planificador del backend no menciona el calendario externo y
  ninguna clase del carril es `ApplicationRunner` ni `CommandLineRunner`.

Punto de control del backend: los 14 ficheros de test del carril en verde,
incluidas las 37 pruebas MockMvc y la de Testcontainers.

### Ciclo 15 — cliente HTTP del navegador (@s35, @s36, @s37, @s38, @s39)

- ROJO: `external-calendar-api.test.ts` (42 casos) no encontraba el módulo.
- VERDE: `external-calendar-api.ts` con validación cerrada de la forma de cada
  respuesta (quince campos de suscripción, cinco por evento, cuatro en la
  lectura de eventos), errores tipados `ExternalCalendarValidationError`,
  `ConnectorsDisabledError` y `ExternalCalendarNotConfiguredError`, y
  `signal.throwIfAborted()` antes y después de cada petición.
- Un item sin `allDay` o con fin no posterior al inicio invalida la lectura
  entera: es lo que @s36 pide para mostrar el aviso de lectura inválida.

### Ciclo 16 — pantalla /calendario-externo (@s37, @s38, @s39, @s40)

- Aquí rompí el orden: escribí `external-calendar.tsx` antes que su test, y las
  24 pruebas pasaron a la primera. Un test que pasa a la primera no demuestra
  nada, así que lo comprobé mutando la producción: cambiar el separador del
  resumen de contadores, quitar el `abort` del desmontaje y refrescar la lista
  también tras una sincronización fallida hacen caer 3 pruebas. Las pruebas
  muerden; queda anotado el desliz de disciplina.
- Cubre: formulario de alta con "Etiqueta" y "Dirección secreta iCal" de tipo
  url y la ayuda de Google Calendar; host y cola sin dirección completa;
  contadores en la frase del contrato; aviso de truncado; mensaje accionable por
  cada código; "Guardando"/"Sincronizando" antes de esperar nada, con una sola
  petición y controles bloqueados; borrador conservado en 400, 503 y fallo de
  red; datos privados retirados en 401; confirmación explícita del borrado;
  cancelación al desmontar; orden de teclado del contrato.

### Ciclo 17 — ruta y navegación (@s37)

- ROJO: `external-calendar-route.test.tsx`, 3 de 4 casos en rojo.
- VERDE: rama `/calendario-externo` en `App.tsx`, sección "Calendario externo"
  en `Workspace` y su entrada de navegación.
- La entrada se inserta antes de "Importación" a propósito: `App.test.tsx` del
  carril de importación afirma cuáles son los dos últimos enlaces, y así ese
  test sigue verde sin tocarlo.

### Ciclo 18 — sección Calendario externo dentro de Hoy (@s35, @s36)

- ROJO: `today-external-calendar.test.tsx` (11 casos) sin módulo, y después
  `today-external-section.test.tsx` (2 casos) en rojo por no estar montada.
- VERDE: `today-external-calendar.tsx`, montada al final del cuerpo de Hoy con
  `zoneId`, `dayStartAt`, `dayEndAt` y `revision` (`serverNow`).
- Orden garantizado por test: POST /sync con onlyIfStale true y después
  GET /events con las fronteras del día. Como la sección se monta con la
  instantánea de Hoy ya pintada, la agenda y sus resúmenes están completos antes
  de que respondan.
- Comprobación de que los tests muerden: al mutar la producción caen 2 de 3
  mutaciones. La tercera —quitar el `abort` del desmontaje— sobrevivía porque
  React 19 ya no avisa de un `setState` sobre una vista desmontada. Reescribí
  el test para capturar las señales entregadas a `fetch` y exigir que todas
  queden abortadas al desmontar; ahora esa mutación también muere.
- `today.test.tsx` aísla la sección con `vi.mock`, como `App.test.tsx` hace con
  `ProjectTasks`: sus 57 pruebas cuentan peticiones de Hoy y la sección añade
  dos. La integración real la cubre `today-external-section.test.tsx`.

### Ciclo 19 — alcance de mutación (feature 28)

- ROJO: cinco pruebas nuevas en `scripts/project.test.mjs` para los objetivos
  `external_calendar-backend` y `external_calendar-frontend`, el conjunto de
  clases del alcance PIT y la configuración de Stryker.
- VERDE: `-PmutationScope=external_calendar` en `backend/build.gradle.kts` con
  las 25 clases del carril, `frontend/stryker.external-calendar.config.json`
  (umbral 80) y los dos bloques en `scripts/project.mjs`.
- El alcance nuevo se suma al final de la unión `else ->` a propósito: las
  pruebas de importación y exportación afirman tramos contiguos de esa línea y
  así siguen verdes.
- Reparación de arrastre: `App.tsx` y `workspace.tsx` movieron líneas al añadir
  la ruta, y `stryker.appearance.config.json` selecciona nodos por línea y
  columna. Se recalcularon los cuatro rangos y se actualizó su espejo en
  `scripts/project.test.mjs`; la prueba de apariencia, que ya venía roja en la
  rama, vuelve a verde.
- `node --test scripts/project.test.mjs`: 75 pasan, 2 fallan. Las dos que fallan
  son de la feature 24 (`integration_api-backend` y `-frontend`), cuyo carril no
  está integrado en esta rama; ya fallaban antes de tocar nada.

**No se ha ejecutado ninguna mutación** ni E2E: hay cinco carriles compartiendo
la máquina. Queda listo para que el `mutation_tester` lance
`bin/harness mutate external_calendar-backend` y `external_calendar-frontend`.

### Ciclo 20 — estilos, extremo a extremo y documentación (@s40)

- ROJO: una prueba nueva en `scripts/project.test.mjs` exigía que la pila de
  extremo a extremo declarase `APP_CONNECTOR_KEY` (32 bytes en base64) y dejase
  la guardia SSRF activada.
- VERDE: `docker-compose.yml` y `scripts/e2e.mjs` pasan las tres variables de
  conectores; la clave del arnés es de pruebas y
  `APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES` se queda en `false`.
- Estilos: `.external-calendar` en `styles.scss` y `.today-external-calendar` en
  `today.scss`, con `min-width: 0`, `overflow-wrap: anywhere`, controles de
  44×44 px y colapso a una columna por debajo de 520 px.
- Especificaciones de extremo a extremo escritas, **no ejecutadas**:
  `e2e/external-calendar.spec.mjs` (alta, cifrado en reposo comprobado en SQL,
  sincronización, guardia de direcciones y borrado con confirmación) y
  `e2e/external-calendar-ux-audit.spec.mjs` (matriz de anchos, axe, orden de
  teclado y texto al 200 %). Solo se han validado con `node --check` y prettier.
- `docs/external-calendar.md` documenta configuración, rutas, seguridad,
  sincronización, análisis y alcance de mutación.
- Arrastre resuelto: mi entrada de navegación desplazaba índices que afirma
  `appearance.test.tsx` (`at(-4)`). En vez de tocar el test de otro carril, la
  entrada se coloca **antes** de "Apariencia": así `at(-4)`, `at(-2)` y `at(-1)`
  siguen valiendo. Se recalculó de nuevo el rango de `workspace.tsx` en
  `stryker.appearance.config.json`.

## Mapa @s → test

| @s | Dónde |
| --- | --- |
| s1 | `ExternalCalendarApiTest.s1_*`, `ExternalCalendarPersistenceTest.s1_*`, `ReadExternalCalendarEventsTest.s1_*` |
| s2 | `ExternalCalendarApiTest.s2_*`, `ExternalCalendarPersistenceTest.s2_*`, `SaveExternalCalendarTest.s2_*`, `AesGcmSecretCipherTest.s2_*`, `ExternalCalendarWiringTest.s2_*` |
| s3 | `AesGcmSecretCipherTest.s3_*`, `SaveExternalCalendarTest.s3_*` |
| s4 | `ExternalCalendarInputTest`, `OutboundHostGuardTest.s4_*`, `SaveExternalCalendarTest.s4_*`, `ExternalCalendarApiTest.s4_*` |
| s5 | `ExternalCalendarInputTest`, `SaveExternalCalendarTest.s5_*` |
| s6 | `ExternalCalendarPersistenceTest.s6_*`, `SaveExternalCalendarTest.s6_*` |
| s7 | `ExternalCalendarPersistenceTest.s7_*`, `SaveExternalCalendarTest.s7_*`, `ExternalCalendarApiTest.s7_*` |
| s8 | `ExternalCalendarDisabledApiTest.s8_*` (7), `ExternalCalendarWiringTest.s8_*`, `external-calendar-api.test.ts` |
| s9 | `ExternalCalendarWiringTest.s9_*`, `AesGcmSecretCipherTest.s9_*` |
| s10 | `ExternalCalendarApiTest.s10_*`, `ExternalCalendarDisabledApiTest.s10_*` |
| s11 | `OutboundHostGuardTest.s11_*`, `SyncExternalCalendarTest.s11_*`, `ExternalCalendarApiTest.s11_*` |
| s12 | `HttpCalendarFeedTest.s12_*`, `SyncExternalCalendarTest.s12_*`, `ExternalCalendarPersistenceTest.s12_*` |
| s13 | `HttpCalendarFeedTest.s13_*`, `SyncExternalCalendarTest.s13_*` |
| s14 | `IcsFeedTest`, `SyncExternalCalendarTest.s14_*` |
| s15 | `IcsFeedTest` (TZID con DST real) |
| s16 | `IcsFeedTest`, `SyncExternalCalendarTest.s16_*`, `ExternalCalendarPersistenceTest.s16_*` |
| s17 | `IcsFeedTest` (DURATION y fines no posteriores) |
| s18 | `IcsFeedTest` (cancelados y recurrentes) |
| s19 | `IcsFeedTest` (desplegado y escapes) |
| s20 | `IcsFeedTest` (UID repetido) |
| s21 | `IcsFeedTest` (inválidos) |
| s22 | `IcsFeedTest` (componentes ajenos) |
| s23 | `ExternalCalendarSnapshotTest`, `SyncExternalCalendarTest.s23_*` |
| s24 | `ExternalCalendarSnapshotTest`, `SyncExternalCalendarTest.s24_*`, `ExternalCalendarPersistenceTest.s24_*` |
| s25 | `ExternalCalendarPersistenceTest.s25_*` |
| s26 | `ExternalCalendarPersistenceTest.s26_*`, `SyncExternalCalendarTest.s26_*` |
| s27 | `SyncExternalCalendarTest.s27_*` |
| s28 | `SyncExternalCalendarTest.s28_*`, `ExternalCalendarApiTest.s28_*` |
| s29 | `AesGcmSecretCipherTest.s29_*`, `SyncExternalCalendarTest.s29_*`, `SaveExternalCalendarTest.s29_*` |
| s30 | `ExternalCalendarPersistenceTest.s30_*`, `ExternalCalendarIsolationTest.s30_*` |
| s31 | `ExternalCalendarPersistenceTest.s31_*`, `ReadExternalCalendarEventsTest.s31_*`, `ExternalCalendarApiTest.s31_*`, `external-calendar.test.tsx` (ventana) |
| s32 | `ExternalEventsRangeTest`, `ExternalCalendarApiTest.s32_*` |
| s33 | `ExternalCalendarPersistenceTest.s33_*`, `ReadExternalCalendarEventsTest.s33_*`, `ExternalCalendarApiTest.s33_*` |
| s34 | `ExternalCalendarIsolationTest.s34_*`, `ExternalCalendarTodayApiTest.s34_*`, `e2e/external-calendar.spec.mjs` |
| s35 | `today-external-calendar.test.tsx`, `today-external-section.test.tsx` |
| s36 | `today-external-calendar.test.tsx`, `external-calendar-api.test.ts` |
| s37 | `external-calendar.test.tsx`, `external-calendar-route.test.tsx`, `external-calendar-api.test.ts` |
| s38 | `external-calendar.test.tsx`, `external-calendar-api.test.ts` |
| s39 | `external-calendar.test.tsx`, `external-calendar-api.test.ts` |
| s40 | `external-calendar.test.tsx` (teclado y región viva), `e2e/external-calendar-ux-audit.spec.mjs` (**5 pruebas, ejecutadas el 10-09-2026**: 14 anchos × 6 estados, axe, 4 modos × 6 estados, teclado, texto al 200 % × 6 estados), `e2e/external-calendar-native-zoom.spec.mjs` (zoom nativo al 200 %), `progress/ux_external_calendar.md` (matriz de los 30 principios) |

## Qué queda fuera de esta sesión

- **No se ha ejecutado la suite completa** de backend ni de frontend, ni PIT, ni
  Stryker, ni E2E: cinco carriles comparten la máquina. Verde comprobado con
  filtros, carril a carril.
- Las dos especificaciones de extremo a extremo son código no ejecutado.
- `node --test scripts/project.test.mjs` deja 2 fallos ajenos, de la feature 24,
  cuyo carril no está integrado en esta rama; ya fallaban antes de empezar.

### Ciclo 21 — el registro de auditoría también se prueba (@s12, @s14)

- ROJO: `ExternalCalendarAuditTest` no compilaba; el adaptador de registro era
  la única clase del alcance de mutación sin prueba propia.
- VERDE: sin cambios de producción; el test fija el formato exacto de la línea
  (`host`, `status`, `code`, `durationMs`), que el éxito registra `code=NONE` y
  que la línea nunca contiene la ruta de la dirección.

## Reasentamiento sobre `main` (a6164e4)

La rama tenía 118 commits fuera de `origin/main` porque la importación entró en
`main` por *squash* (`#28`): rebasar todo habría replicado historia ya integrada.
Se reasentaron solo los 17 commits propios del carril:
`git rebase --onto origin/main 67699cc HEAD`. Resultado: 17 commits sobre
`a6164e4`, árbol limpio.

### Conflictos y cómo se resolvieron

1. **`feature_list.json`** — `main` traía la 28 como `spec_ready` y mi commit la
   pone `in_progress`. Se conserva `in_progress`: el carril está en curso.
2. **`domain/IcsCalendar` (add/add)** — colisión real de nombres: la feature 26
   creó un **escritor** de iCalendar con ese nombre; el mío es un **analizador**.
   Se conserva el de la 26 intacto y el mío se renombra a `IcsFeed`
   (`IcsFeed.parse`), con `IcsMalformedException` → `IcsFeedMalformedException` y
   `IcsCalendarTest` → `IcsFeedTest`. Arrastres actualizados:
   `SyncExternalCalendar`, `ExternalCalendarIsolationTest`, el alcance PIT y su
   espejo en `scripts/project.test.mjs`, y `docs/external-calendar.md`.
3. **`App.tsx`** — se conservan las dos ramas: `externalCalendar` y `calendar` de
   la 26, en la cadena de `section` y en la de render.
4. **`workspace.tsx`** (dos veces) — se conservan las dos entradas de navegación.
   La mía va **antes de "Apariencia"**, y la premisa sigue siendo válida tras el
   rebase: `appearance.test.tsx` fija ahora las **cinco últimas** posiciones
   contadas desde el final (Apariencia, Exportación, Calendario, Importación, API
   para integraciones) y `App.test.tsx` las dos últimas. Insertar antes de
   "Apariencia" no mueve ninguna de ellas. Se verificó ejecutando ambos tests.
5. **`backend/build.gradle.kts`** — la unión `else ->` de `targetClasses` se
   queda con `icsCalendarClasses + externalCalendarClasses`, ambos.
6. **`SecurityConfiguration`** — sin conflicto textual, pero se revisó a mano que
   convivan las tres cosas: la cadena `@Order(0)` del feed público de la 26 (sin
   guardia de conectores, no es ruta de conector), la `@Order(1)` Bearer de la 24
   —que ahora excluye las rutas de calendario— y la `@Order(2)` de sesión; el
   `ConnectorsGate` va detrás de `AuthorizationFilter` en las dos últimas.
   `CalendarPaths.isCalendar` no solapa con `/api/v1/me/external-calendar`.
   Verde: `SecurityHeadersTest`, `ApiCredentialBearerTest`,
   `ApiCredentialBearerAdmissionTest`, `CalendarApiTest`, `CalendarWiringTest`,
   `domain.Calendar*`, `IcsCalendarTest` y `CalendarFeedUseCasesTest`.
7. **`docker-compose.yml` y `scripts/e2e.mjs`** — mi cableado de
   `APP_CONNECTOR_KEY` sobrevivió sin conflicto junto a los cambios de la 26.

### Rangos por línea recalculados

`App.tsx` y `workspace.tsx` volvieron a moverse, así que se recalcularon los
selectores de **los dos** configs afectados y sus espejos en
`scripts/project.test.mjs`:

- `stryker.appearance.config.json`: `App.tsx` 34:8-34:44, 57:20-69:36,
  90:10-131:7 y `workspace.tsx` 85:10-90:22.
- `stryker.ics-calendar.config.json`: `App.tsx` 36:8-36:42, 53:16-54:30,
  86:10-87:37 y `workspace.tsx` 97:10-102:22.

`node --test scripts/project.test.mjs`: 82 pasan, 0 fallan (los dos fallos
ajenos de la 24 desaparecen porque su carril ya está en `main`).

## Extremo a extremo ejecutado

Ambas especificaciones se ejecutaron por primera vez, una pila cada vez, con
`E2E_WEB_PORT=18096`, y las dos pilas se bajaron solas al terminar (no queda
ningún contenedor `organizationweb-e2e-*`).

- `e2e/external-calendar.spec.mjs` → **3 de 3 en verde** (9,1 s). Comprueba
  contra PostgreSQL real que la fila guardada tiene `version 0`, más de 12 bytes
  de cifrado y **cero** apariciones de la ruta secreta; que la respuesta del PUT
  trae los quince campos con `no-store` y sin el secreto; que la sincronización
  responde 200 y deja `last_attempt_at`; que la guardia rechaza `https://10.0.0.5`
  con 400 sin escribir fila; y que Hoy sigue igual sin suscripción.
- `e2e/external-calendar-ux-audit.spec.mjs` → **4 de 4 en verde** (8,8 s). Matriz
  de catorce anchos sin scroll horizontal y con controles de 44×44 px, axe sin
  violaciones en vacío, con suscripción y tras sincronizar, orden de teclado del
  contrato con foco visible, y texto al 200 % a 320 px.

Ningún defecto destapado. Al contrario que en la feature 26, la pantalla no
depende de la forma de la URL en el cliente: el navegador solo maneja `urlHost` y
`urlTail`, y quien valida el esquema es el backend, así que el modo de fallo
"cliente que exige https" no existe aquí. La ruta pasa por el proxy sin tocar
`deploy/nginx.conf`.

**Limitación conocida y aceptada**: ninguna prueba de extremo a extremo llega a
una sincronización con `lastStatus OK`, porque no hay ningún feed ICS alcanzable
desde el contenedor con la guardia SSRF activada —y activada debe quedarse—. El
feed público de la feature 26 vive en `127.0.0.1`, que la guardia bloquea con
razón. Los caminos de éxito, contadores, truncado y ventana están cubiertos por
las pruebas de unidad, de aplicación y de Testcontainers, y la descarga real
contra un `HttpServer` en loopback por `HttpCalendarFeedTest`. Cerrar ese hueco
exigiría un contenedor de feed en la pila de E2E y
`APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES=true`, que es justo lo que el contrato y
la enmienda B10 quieren evitar por defecto: queda como decisión del coordinador.

## Reasentamiento sobre `main` (a08d3be) y unificación de `SecretCipher`

### La colisión de diseño: un solo concepto, un solo puerto

Leídas las dos implementaciones, **son el mismo concepto** y se unifican. Pero el
argumento decisivo no es de gusto, sino documental: **los dos contratos aprobados
piden el mismo formato en reposo, y la 27 no lo cumplía.**

- `features/github_connector.feature`, @s1: «token_ciphertext tiene octet_length
  **12 + 14 + 16** bytes» —nonce, texto y etiqueta— y @s2: «sus **12 primeros
  bytes** difieren».
- `features/external_calendar.feature`, @s2: «url_ciphertext de **12 bytes de
  nonce más cifrado**».

La implementación de la 27 escribía `versión(1) || nonce(12) || sellado` (43
bytes para ese token) y sus pruebas afirmaban `1 + 12 + …`, contradiciendo su
propio contrato. Es exactamente la desviación de la enmienda B5 que este carril
ya corrigió en el ciclo 6. Así que no he cambiado el formato de la 27 «porque me
convenga»: lo he devuelto al que su Gherkin exige.

Sobre el riesgo de dejar filas ilegibles: el byte de versión **no era portador de
información**. El javadoc de `ConnectorKeyRing.candidatesFor` ya decía «el byte de
versión sólo ordena a las candidatas: quien decide es la etiqueta de GCM».
Quitarlo sólo elimina una pista de ordenación; la rotación de B5 sigue viva
probando las claves del llavero. Lo que sí cambia es el desplazamiento del nonce,
así que **cualquier fila `connector_connections` ya escrita quedaría ilegible**.
En este repositorio no hay datos: V25 acaba de entrar y el despliegue vive en
otro repositorio. Si el coordinador supiera de algún entorno con filas escritas,
esto exige una migración y hay que decirlo antes de integrar.

Resultado:

- **Puerto único** `application/SecretCipher` con `encrypt`, `Optional<String>
  decrypt` y `enabled()`. Gana `Optional` por lo que dijo el coordinador y porque
  obliga a decidir en el punto de llamada.
- **Adaptador único**: el de la 27, `adapter/connectors/AesGcmSecretCipher` sobre
  `ConnectorKeyRing`, que es el más completo (llavero, rotación, validación al
  arrancar). Se le quita el byte de versión y devuelve `Optional`.
- **Se borran** mi `adapter/crypto/AesGcmSecretCipher` y `ConnectorCipher` con sus
  dos tests: eran el duplicado. Sus casos valiosos ya los cubre
  `adapter/connectors/AesGcmSecretCipherTest`, al que se le añadió la afirmación
  del formato del contrato.
- **`ConnectorsGate` consulta `enabled()`**, como recomendó el coordinador, en vez
  de releer la propiedad: una sola fuente de verdad. Se inyecta como
  `ObjectProvider<SecretCipher>` —el patrón que ya usa `SecurityConfiguration`
  para la 24— porque si no, las rebanadas `@WebMvcTest` de la 27 no levantaban
  contexto. Sin cifrado cableado, el valor por defecto es «deshabilitado».
- **La 27 conserva su comportamiento**: su único punto de llamada
  (`ImportGithubIssues.collect`) hace `.orElseThrow(SecretUndecipherableException::new)`.
- `ConnectorKeyRing` pierde `version(byte[])` y el campo `version`, ya muertos, y
  su mensaje de error nombra ahora la propiedad **y** la variable de entorno,
  porque @s9 de la 28 exige que se mencione `APP_CONNECTOR_KEY`.

**Defecto latente de la 27 que deja ver la unificación, y que no arreglo por no
ser mi carril**: `SecretUndecipherableException` no se captura en ningún sitio, así
que un token ilegible tras rotar la clave sale como 500 `INTERNAL_ERROR` en vez de
un recibo fallido. Queda anotado en el código y aquí.

### Los otros ocho conflictos

`App.tsx` (tres hunks: import, constantes y cadena de render; se conservan
`githubConnector`, `integrationsIndex`, `calendar` y `externalCalendar`),
`workspace.tsx` (todas las entradas; la mía sigue antes de «Apariencia»),
`build.gradle.kts` (unión `icsCalendarClasses + githubConnectorClasses +
externalCalendarClasses`), `docker-compose.yml` y `scripts/e2e.mjs` (se conserva
`connectorKey` de la 27 —la misma clave literal que yo había elegido— y se añade
`APP_CONNECTORS_ALLOW_PRIVATE_ADDRESSES=false`), `styles.scss` (los dos bloques
`.github-connector` y `.external-calendar` completos; `theme-tokens.test.ts` en
verde), y los dos configs de Stryker con su espejo.

`ApiCredentialCompatibilityTest` ya no tenía lista negra: no había nada que quitar
para V26.

### Rangos de Stryker recalculados sobre el `App.tsx` fusionado

- `stryker.appearance.config.json`: `App.tsx` 36:8-36:44, 61:20-73:36, 98:10-139:7
  y `workspace.tsx` 85:10-90:22.
- `stryker.ics-calendar.config.json`: `App.tsx` 38:8-38:42, 57:16-58:30,
  94:10-95:37 y `workspace.tsx` 97:10-102:22.

Validados uno a uno por el oráculo de contenido de `scripts/project.test.mjs`:
**85 pasan, 0 fallan**.

### Pruebas ejecutadas

- Carril 28 completo (backend filtrado) y frontend: verde.
- **Feature 27**: `adapter.connectors.*`, `application.*Github*`,
  `GithubConnectorWiringTest`, `GithubConnectorPersistenceTest`,
  `GithubConnectorApiTest`, `github-connector.test.tsx`,
  `github-connector-routing.test.tsx` → verde.
- **Feature 24**: `adapter.ApiCredential*`, `adapter.persistence.ApiCredential*`,
  `application.*ApiCredential*`, `SecurityHeadersTest`, `integration-api.test.tsx`
  → verde.
- Feature 26: `CalendarApiTest`, `CalendarWiringTest`, `calendar.test.tsx` → verde.
- Frontend, 28 ficheros de prueba que tocan el armazón compartido: 1.031 pruebas
  en verde.
