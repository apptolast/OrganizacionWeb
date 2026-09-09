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
