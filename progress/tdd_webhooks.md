# TDD — Feature 25 `webhooks`

Worktree `C:/Users/vhurt/ow-worktrees/webhooks`, rama `claude/webhooks`. Contrato: `features/webhooks.feature` (42 escenarios). Inicio: 8 de septiembre de 2026.

Feature en curso: 25 — webhooks. Escenarios a recorrer: @s1…@s42.

## Decisiones previas

- @s33 filas Bearer: `ApiCredentialBearerFilter` autentica la credencial y, para rutas fuera de su allowlist, responde `403 API_SCOPE_DENIED` (no 401). Por la decisión 5 del coordinador, esas dos filas se prueban con `403 API_SCOPE_DENIED` y sin tocar webhooks.
- Firma HMAC y AES-GCM viven en adaptadores (`javax.crypto` no está permitido en dominio/aplicación por ArchUnit). El dominio conserva catálogo, validación, política de direcciones y tabla de reintentos.
- Errores de aplicación: una sola `WebhookOperationException(Code)` para los códigos 4xx/5xx estables; `WebhookInvalidException(campos)` en dominio para `WEBHOOK_INVALID` con `errors[]`.

## Enmiendas del contrato (revisión de seguridad, aprobadas por el coordinador)

Origen: `progress/security_review_connectors.md` (9 de septiembre de 2026),
hallazgos B1 a B6. El coordinador actualiza `project-spec.md` y
`features/webhooks.feature` en `main`; aquí implemento ya el comportamiento
corregido. Donde la enmienda contradice el `.feature` que tengo en el worktree,
manda la enmienda.

- **B1 (alta) — el envío sale de la transacción.** El intento HTTP deja de
  ejecutarse dentro de la transacción de reclamación: se reclama con una columna
  de arrendamiento sobre la entrega, se envía fuera y se registra el resultado en
  una transacción corta posterior. El plazo es del intercambio completo
  (`HttpRequest.timeout`), no solo de lectura, y el cuerpo de respuesta se aborta
  pasados 64 KiB con un `BodySubscriber` propio. Se resuelve la contradicción del
  texto original: **5 s de conexión y 10 s de plazo total**. Afecta a @s25
  (la fila «acepta la conexión y no responde en 5 s» pasa a medir el plazo total),
  @s22, @s23 y a la migración `V23__webhooks.sql`, que gana la columna de
  arrendamiento.
- **B2 (media) — política de direcciones compartida y completa.** `AddressPolicy`
  pasa a `application/` porque las features 27 y 28 la reutilizan. Añade
  `0.0.0.0/8`, `192.0.0.0/24`, `192.88.99.0/24`, `198.18.0.0/15`, `240.0.0.0/4`,
  `255.255.255.255`, `64:ff9b::/96` y `2002::/16`, y normaliza antes las formas
  mapeada, compatible, 6to4 y NAT64. Amplía @s5 y @s25.
- **B3 (media) — sin rebinding entre comprobación y uso.** Se resuelve el nombre
  una vez, se validan todas las direcciones devueltas y se conecta contra la IP
  literal ya validada, conservando el nombre original en la cabecera `Host` y en
  SNI. Deja de ser un límite aceptado. Afecta al entregador y a @s25.
- **B4 (media) — deduplicación sobre material firmado.** Solo documentación:
  `docs/webhooks.md` debe decir que se verifica la firma primero, que se
  deduplica por el `eventId` que viaja dentro del cuerpo firmado (la cabecera
  `X-OrganizationWeb-Event-Id` es solo una pista), que se rechaza si `now` menos
  `t` supera 300 s y que la comparación de la firma es en tiempo constante.
- **B5 (media) — rotación de clave.** El texto cifrado se prefija con un byte de
  versión de clave; se leen `APP_CONNECTOR_KEY` y `APP_CONNECTOR_KEY_PREVIOUS`.
  Política unificada para los tres conectores: **fallo rápido al arrancar si la
  clave está mal formada, modo degradado con 503 `CONNECTORS_DISABLED` solo si
  está ausente**. Esto cambia @s9: la fila «base64 de 31 bytes» y la fila «texto
  no base64» dejan de ser arranque degradado y pasan a ser fallo de arranque; el
  503 queda para la clave ausente.
- **B6 (baja) — AAD atado al propietario.** El dato adicional autenticado pasa a
  ser `ownerId + "|" + endpointId`, no solo el id. Cambia la firma de
  `WebhookSecrets` y la última línea de @s8.

## Bitácora de ciclos

### Ciclo 1 — dominio de intención, direcciones, reintentos, firma y cifrado (commit `4cff2a2`)

- ROJO/VERDE por partes: `WebhookIntentTest` (16), `BlockedAddressesTest` (28),
  `RetryScheduleTest` (6), `WebhookSignatureTest` (1), `AesGcmWebhookSecretsTest` (6).
- Cubre `@s2 @s3 @s4 @s5 @s8 @s9 @s15 @s26`.

### Ciclo 2 — alta de endpoint y ciclo de vida del estado

- ROJO: `WebhookEndpointTest` (4 tests, `@s12 @s27 @s28`) exige `withStatus`
  binario e idempotente y `disabledByExhaustion` que respeta un `disabled`
  anterior. `CreateWebhookTest` (4 tests, `@s1 @s5 @s34`) exige secreto de un
  solo uso `whsec_` + 43 base64url, alta `active` sellada con el reloj, guardia
  de destino con resolución DNS y el orden fijo de errores
  valores → clave → destino → cuota.
- VERDE: `WebhookEndpoint` (record de dominio), `CreateWebhook` +
  `CreateWebhookUseCase` + `WebhookCreation` (con `toString` que redacta el
  secreto), puertos `WebhookEndpoints` / `WebhookSecrets`, guardia
  `WebhookDestinationGuard` y `WebhookOperationException(Code)`.
- REFACTOR: reutilizo `CustomizationTime.capture(clock)` como el resto de casos
  de uso en lugar de truncar el instante a mano.
- Comando: `backend\gradlew.bat test --no-daemon --tests '*Webhook*' --tests
  '*BlockedAddresses*' --tests '*RetrySchedule*'` → 65 tests, 0 fallos.
  ArchUnit verde (`--tests '*Arch*'`).

#### Nota de recuperación (sesión cortada por cuota)

La sesión anterior se cortó con un `ManageWebhookTest` a medio escribir en el
árbol. Al revisarlo aparecen escapes corrompidos por haberlo escrito con
heredoc: la aserción de `@s17` contenía un escape Unicode espurio (u0001 con barra invertida) dentro del literal
del cuerpo JSON. Aparto ese test, confirmo el verde del ciclo 2 y lo commiteo
antes de rehacer el ciclo de `ManageWebhook` con Write/Edit.


### Ciclo 3 — recuperación de `ManageWebhook` (commit `18107ad`)

- **Estado de partida**: cinco ficheros sin añadir a git. **No compilaban**:
  `ManageWebhook` declaraba una clase interna `Code` que ocultaba el enum
  `WebhookOperationException.Code`, así que cada `operation(Code.X)` no
  convertía tipos (6 errores de compilación).
- VERDE de compilación: elimino la clase interna y uso importación estática.
- ROJO real: `s12_aStatusOutsideActiveOrDisabledIsAFieldErrorBeforeAnyLookup`
  falla con `NullPointerException` — `List.of(...).contains(null)` lanza en vez
  de rechazar el campo `status`.
- VERDE mínimo: comprobar `status == null` antes de consultar la lista.
- 12 tests verdes. Cubre `@s9 @s11 @s12 @s13 @s14 @s17 @s30`.

### Ciclo 4 — alta por HTTP (commit `8815113`)

- ROJO: `WebhookApiTest.s1_...` no compila (no existe `WebhookController`).
- VERDE: `WebhookController.create` con lectura estricta del cuerpo
  (duplicados y tokens finales), 201 + `Location` + `no-store`, cuerpo de
  exactamente `endpoint` y `secret`. `WebhookEndpointView` cierra el DTO a los
  nueve campos y formatea instantes con `appendInstant(6)`.
- Cubre `@s1`.

### Ciclo 5 — resto de la superficie HTTP (commit `7e6acfe`)

- ROJO: 16 tests nuevos fallan (listar, find, status, delete, ping,
  deliveries, redeliver y el mapa de códigos).
- VERDE: rutas restantes + `WebhookDeliveryView` (once campos) + un único
  manejador que traduce cada `Code` a su par (HTTP, `problem+json`). Un id de
  ruta que no es UUID canónico responde 404 `WEBHOOK_NOT_FOUND` **sin** tocar
  el caso de uso, para no servir de oráculo de existencia.
- 17 tests verdes en `WebhookApiTest`; ArchUnit verde.
- Cubre `@s10 @s11 @s12 @s13 @s14 @s29 @s30 @s34` (parte HTTP).

### Pendiente al cierre de esta sesión

- Persistencia: migración `V23__webhooks.sql` + `PostgresWebhookEndpoints` /
  `PostgresWebhookDeliveries` (`@s6 @s7 @s10 @s13 @s29`).
- Frontera HTTP fina: 413/415/query (`@s4`), seguridad de sesión (`@s33`),
  cableado en `ApplicationConfiguration` + `WebhookWiringTest`.
- Despachador: worker de encolado y envío (`@s15 @s16 @s18`–`@s28 @s31 @s32`).
- UI de webhooks (`@s36`–`@s42`).

### Ciclo 6 — persistencia y migración V23 (commit `b825e9b`)

- ROJO: `WebhookPersistenceTest` no compila (no existe `PostgresWebhookStore`).
- VERDE: `V23__webhooks.sql` (`webhook_endpoints`, `webhook_deliveries` con
  cascada, `leased_until` de la enmienda B1 y CHECK que ata `status` con
  `disabled_reason`/`disabled_at`) + `PostgresWebhookStore` con los dos puertos.
  El cupo de cinco se cuenta bajo `pg_advisory_xact_lock` por propietario.
- REFACTOR en verde: quito `pruneTerminal` y su constante (ningún test los
  pedía todavía, Ley 3) y reduzco `save` a privado.
- 7 tests verdes. Cubre `@s1`(cursor) `@s6 @s7 @s8 @s10 @s11 @s13`.

### Ciclo 7 — cableado (commit `c2757f0`)

- ROJO: `WebhookWiringTest` con `NoSuchBeanDefinitionException`.
- VERDE: beans en `ApplicationConfiguration`. Clave ausente → `DISABLED`;
  clave mal formada → fallo al arrancar (enmienda B5).
- 2 tests verdes. Cubre `@s1 @s5 @s8` sobre el esquema real.

### Ciclo 8 — frontera HTTP (commit `523e72a`)

- ROJO: 4 fallos (413 a 4097 bytes, 415 con `text/plain`, 400 ante query, 405
  en PATCH que la advice genérica convertía en **500**).
- VERDE: `consumes=application/json`, corte a 4096 bytes antes de parsear,
  rechazo de la query y **mapeo de PATCH como ruta**, no como manejador de
  excepción: el dispatcher lanza el 405 antes de resolver el controlador, así
  que un `@ExceptionHandler` local nunca lo ve.
- Nota: las filas de sesión de `@s33` (401, CSRF, Origin) ya pasaban con
  `SecurityConfiguration`; quedan como red de regresión, no como logro nuevo.
- 28 tests verdes. Cubre `@s4 @s33 @s34`(frontera).

### Ciclo 9 — clasificación del intento (commit `807ee0a`)

- ROJO: `WebhookAttemptTest` no compila.
- VERDE: `WebhookAttempt` (2xx éxito, 3xx `REDIRECT`, resto `HTTP_ERROR`,
  transporte sin código, latencia no negativa) y `WebhookDelivery.recorded`
  que pliega el intento con `RetrySchedule`.
- Cubre `@s24` completo y la clasificación de `@s25` y `@s26`.

### Ciclo 10 — despachador (commit `930c896`)

- ROJO: `DispatchWebhooksTest` no compila.
- VERDE: `WebhookWork` / `WebhookSender` / `ClaimedDelivery` /
  `DispatchWebhooks`. Reclama arrendando, **envía fuera de la transacción**
  (B1), liquida en escritura corta, corta el ciclo a 20 entregas y arrastra el
  endpoint a `DELIVERY_EXHAUSTED` en el mismo `record`.
- 6 tests verdes. Cubre `@s16`(paso de datos) `@s20 @s24 @s27 @s32`.

## Mapa @s → test (al cierre de la sesión)

| Escenario | Test |
| --- | --- |
| @s1 | `WebhookApiTest.s1_creation…`, `WebhookPersistenceTest.s1_insertingSeals…`, `WebhookWiringTest.s1_s8_…` |
| @s2 @s3 | `WebhookIntentTest` |
| @s4 | `WebhookApiTest.s4_…` (5 tests) |
| @s5 | `AddressPolicyTest`, `CreateWebhookTest`, `WebhookWiringTest.s5_…` |
| @s6 @s7 | `WebhookPersistenceTest.s6_…`, `.s7_…` |
| @s8 | `AesGcmWebhookSecretsTest`, `WebhookPersistenceTest.s8_…`, `WebhookWiringTest.s1_s8_…` |
| @s9 | `ManageWebhookTest.s9_…`, `CreateWebhookTest` |
| @s10 @s11 | `WebhookApiTest.s10_…/s11_…`, `WebhookPersistenceTest.s10_…/s11_…` |
| @s12 | `WebhookEndpointTest`, `ManageWebhookTest.s12_…`, `WebhookApiTest.s12_…` |
| @s13 | `ManageWebhookTest.s13_…`, `WebhookApiTest.s13_…`, `WebhookPersistenceTest.s13_…` |
| @s14 @s17 | `ManageWebhookTest.s14_…`, `WebhookPingPayloadTest`, `WebhookApiTest.s14_…` |
| @s15 | `WebhookSignatureTest` (vector exacto) |
| @s16 | `DispatchWebhooksTest.s16_…` (paso de datos; **falta** el adaptador HTTP real) |
| @s20 @s27 @s32 | `DispatchWebhooksTest` |
| @s24 @s25 @s26 | `WebhookAttemptTest` (clasificación y tabla) |
| @s29 @s30 | `ManageWebhookTest.s30_…`, `WebhookApiTest.s29_…/s30_…` |
| @s33 @s34 | `WebhookApiTest.s33_…/s11_s34_…` |

## Pendiente real al cierre (NO cubierto por ningún test)

1. **Adaptador HTTP saliente** (`WebhookSender` real): cabeceras exactas de
   `@s16`, firma sobre el cuerpo de `@s15` en el envío, sin seguir
   redirecciones, plazo total de 10 s y conexión de 5 s (B1), corte del cuerpo
   de respuesta a 64 KiB, y conexión contra la IP ya validada conservando
   `Host`/SNI (B3). Falta también la clasificación real de `@s25`
   (TIMEOUT/CONNECTION/TLS/DNS/BLOCKED_ADDRESS) contra un receptor de prueba.
2. **Reclamación en Postgres** (`PostgresWebhookWork`): `@s22` (reinicio a
   media entrega), `@s23` (dos instancias sin bloquearse) y la poda de `@s29`
   (50 terminales + todas las pendientes).
3. **Encolado desde la outbox**: `@s18 @s19 @s21 @s28` (cursor, ventana de
   gracia de 5 s, orden por tupla, eventos bloqueados/inválidos).
4. **Programación del worker**: `@s32` con `app.webhooks.enabled`.
5. **Auditoría** `@s35`.
6. **UI completa** `@s36`–`@s42`.

### Ciclo 11 — adaptador de envío saliente (commit `f99adaa`)

- ROJO: `JdkWebhookSenderTest` no compila.
- VERDE: `JdkWebhookSender` con las cabeceras exactas, firma sobre el cuerpo y
  el instante del envío, `Redirect.NEVER`, 5 s de conexión y 10 s de
  intercambio (B1), cuerpo de respuesta descartado y comprobación de todas las
  direcciones resueltas antes de conectar (B3).
- 11 tests verdes contra un receptor real. Cubre `@s15 @s16 @s25 @s26`.

### Ciclo 12 — reclamación arrendada (commit `994d5eb`)

- ROJO: `WebhookWorkPersistenceTest` no compila.
- VERDE: `PostgresWebhookWork` con `FOR UPDATE ... SKIP LOCKED` + arrendamiento
  de 5 min; `record` liquida y, si agota, desactiva el endpoint en la misma
  transacción, podando a 50 terminales.
- 6 tests verdes. Cubre `@s22 @s23 @s27`(persistencia) `@s29`(poda).

### Ciclo 13 — encolado desde la outbox (commit `f31aa46`)

- ROJO: no compila; después falla la fila de `@s19` con `occurredAt` igual al
  instante de creación.
- **BUG REAL**: `UUID.compareTo` es **con signo**, así que todo id cuyo primer
  bit está a 1 (la mitad de los uuid v4) quedaba por *debajo* del uuid nulo y
  no se encolaba nunca en el primer instante del endpoint. PostgreSQL ordena
  uuid como bytes **sin signo**. `WebhookCursor.compareUnsigned` alinea dominio
  y base de datos. Este fallo habría perdido eventos en silencio.
- VERDE: `EnqueueWebhookDeliveries` con ventana de gracia de 5 s, una entrega
  por endpoint y ciclo, `blocked` sin auditar y `UNSUPPORTED_EVENT` /
  `INVALID_EVENT` auditados avanzando el cursor.
- También corrijo dos tests **míos** que reclamaban la primera entrega global y
  se rompían al compartir contenedor con el test hermano.
- Cubre `@s18 @s19 @s20 @s21`.

### Ciclo 14 — lectura real de la outbox (commit `04b73b2`)

- ROJO: no compila; después falla la aserción del payload.
- VERDE: `PostgresWebhookOutbox`. `readyEndpoints` excluye los endpoints con
  una entrega de outbox en vuelo pero **no** los que solo tienen un ping.
  La validez del payload reutiliza `OutboxMessage.validationCode`, así que
  webhooks y publicador coinciden. Ninguna sentencia escribe en `outbox_events`.
- Dos correcciones en **mis fixtures**: `ProjectCreated.v1` exige un conjunto
  exacto de claves (sobraba `status`) y `jsonb` normaliza el espaciado, así que
  «el payload de la fila» es lo que `jsonb` renderiza.
- Cubre `@s18 @s20 @s21` en persistencia.

### Ciclo 15 — worker programado (commit `f0d378e`)

- ROJO: `WebhookScheduleTest` no compila.
- VERDE: `WebhookSchedule` (encola y después despacha; ninguna mitad propaga,
  porque un `@Scheduled` que lanza detiene toda la programación) y
  `WebhookConfiguration` tras `app.webhooks.enabled=true`.
- **Además** quedan cableados `PostgresWebhookWork`, `PostgresWebhookOutbox`,
  `JdkWebhookSender`, `DispatchWebhooks` y `EnqueueWebhookDeliveries`, que
  existían sin bean: hasta este commit el worker no podía funcionar.
- Cubre `@s32`(la puerta del flag) y el orden encolar-despachar.

## Estado al cierre de la sesión

**231 tests verdes** en el carril (`*Webhook* *Dispatch* *Enqueue*
*RetrySchedule* *AddressPolicy*`), ArchUnit verde. Nunca se lanzó la suite
completa, ni pitest, ni E2E, conforme a la disciplina de recursos.

### Lo que NO está hecho (honesto)

- **UI completa `@s36`–`@s42`**: no se ha escrito ni una línea de frontend.
  Es el bloque más grande que queda.
- `@s28` reactivación de extremo a extremo (las piezas existen —
  `withStatus`, cursor conservado, `readyEndpoints`— pero falta el test que
  recorre el escenario entero).
- `@s31` firma nueva sobre el cuerpo original de una entrega reenviada: el
  `body` no se toca al reencolar y la firma se calcula en el envío, así que
  debería cumplirse, **pero no hay test que lo demuestre**.
- `@s35` auditoría: hay redacción en `ClaimedDelivery.toString`,
  `WebhookCreation.toString` y `WebhookSchedule`, pero falta el test que
  compruebe que los logs no contienen `?token=`, `whsec_` ni `v1=`.
- `@s9` el `audit workerError CONFIGURATION_ERROR` al arrancar sin clave.
- `@s2 @s3` están cubiertos en dominio (`WebhookIntentTest`) pero no hay un
  test HTTP que recorra las tablas de ejemplos por la ruta real.
- `@s22` fila «muere después de que el receptor respondiera 200» (2 copias):
  cubierta la recuperación por arrendamiento, no el conteo de copias.

## Sesión 2 — reasentamiento sobre main y cierre de la feature

### Reasentamiento (commit `6ac9537`)

`git rebase` sobre `main` intentaba reproducir **124 commits** propios (el
carril forkó en 4c7d558) y chocaba ya en el segundo con docs compartidos. Uso
**merge**: mismo objetivo, una sola pasada de conflictos, sin reescribir 124
commits. El hash que me dieron, `c0e22a3`, es la cabeza de `main` **local**, 6
commits por delante de `origin/main` (aa6ec86); rebasé sobre él, que es lo que
describía el coordinador.

Ocho conflictos: los de la feature 24 (`SecurityConfiguration`, tres
`ApiCredential*Test`) y `progress/current.md` se resuelven con la versión de
`main`, que es la integrada y aprobada; `project-spec.md` incorpora la sección
de enmiendas B1–B6; en `feature_list.json` la 24 conserva el valor de main (no
es mi carril) y la 25 sigue `in_progress`.

### Ciclo 16 — auditoría (commit `81f8491`) — `@s35`

Puerto `WebhookAudit` + `Slf4jWebhookAudit`. La garantía es **estructural**: el
puerto solo acepta `UUID` y códigos cortos, así que **no existe parámetro** por
el que una URL, un secreto, una firma o un cuerpo puedan llegar al log. Un test
lo comprueba por reflexión sobre la firma del puerto. Las filas `blocked` no se
auditan; `INVALID_EVENT` y `UNSUPPORTED_EVENT` sí.

### Ciclo 17 — arranque degradado (commit `089b002`) — `@s9`

`DispatchWebhooks` corta el ciclo **antes de reclamar** si no hay clave, y
`WebhookConnectorStartup` audita `CONFIGURATION_ERROR` **una sola vez** al
arrancar (`compareAndSet`), no una vez por ciclo.

### Ciclo 18 — reactivación y reenvío (commit `14a1911`) — `@s28 @s31`

ROJO por **mi fixture**: creaba la entrega a las 12:00 y drenaba a las 10:00,
así que nunca vencía. Corregido el fixture, **sin tocar producción**, que era
justo lo que había que demostrar: el cursor no retrocede al reactivar, D1 sigue
`exhausted` y no se reenvía, y el reenvío conserva el cuerpo byte a byte con
firma nueva (`t=1788872400` frente a `t=1788861600`) sin añadir fila.

### Ciclos 19–22 — la UI (`7f7a229`, `11d2193`, `fd04f30`, `897711b`)

- **Cliente** `webhooks-client.ts`: decodificación estricta con `exact(...)`
  contra los DTO cerrados; rechaza cualquier `secret` o `body` filtrado.
- **Vista** `webhooks.tsx`: estados de `@s36`, formulario y secreto de un solo
  uso de `@s37`, aborto e identidad de `@s38`, acciones de `@s39`, panel de
  entregas manual de `@s40` y errores de `@s41`.
- **Ruta y navegación**: `/webhooks` y entrada justo tras «API para
  integraciones».
- **Estilo**: `webhooks.scss` solo con tokens, para pasar la guarda global que
  main trajo con el modo oscuro.

**Verificación de que los tests muerden.** Inyecté dos mutantes en la vista. El
de abortar al desmontar murió; el del guard de doble envío **sobrevivió**: mi
test solo probaba que el botón deshabilitado frena el segundo clic. Un
formulario también se envía con Enter o `requestSubmit`, que eso no cubre.
Añadí un test por `requestSubmit` que ahora mata ese mutante.

### Daños colaterales corregidos (no relajados)

Colocar «Webhooks» donde manda `@s36` rompió **cinco** aserciones ajenas que
daban por hecha la POSICIÓN de los enlaces (`integration-api` @s41,
`App` @s33, `appearance` @s20, `export-data` @s22) y la guarda de esquema
aditivo de `ApiCredentialCompatibility` @s42. Todas reescritas **conservando su
intención** —orden relativo por nombre, y las tablas de V23 excluidas junto a
las de V22— en vez de debilitarlas o de renunciar a la posición del contrato.
Al hacerlo detecté que una aserción mía era un falso positivo: `indexOf` daba
−1 por un marcador decorativo y `−1 < índice` pasa siempre.

## Estado final

- Backend: **472 tests verdes** (webhooks + feature 24 + ArchUnit).
- Frontend: **42 tests** propios de webhooks; **246** en los siete ficheros que
  tocan la navegación. `tsc` y `eslint` limpios.
- Nunca se lanzó la suite completa, ni `pitest`, ni E2E.

### Lo único que queda fuera de mi alcance

`@s42` está cubierto en lo verificable en jsdom (nombres accesibles, foco de
vuelta tras cerrar el panel del secreto y la confirmación, `aria-live`,
objetivos de 44 px y tabla con desplazamiento propio en la hoja de estilo).
**La parte de axe, los cuatro anchos, el zoom al 200 % y ambos temas es E2E**, y
la disciplina de esta sesión me prohíbe lanzarlo. Queda para la puerta del
coordinador; no lo doy por probado.
