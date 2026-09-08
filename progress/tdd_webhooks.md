# TDD — Feature 25 `webhooks`

Worktree `C:/Users/vhurt/ow-worktrees/webhooks`, rama `claude/webhooks`. Contrato: `features/webhooks.feature` (42 escenarios). Inicio: 8 de septiembre de 2026.

Feature en curso: 25 — webhooks. Escenarios a recorrer: @s1…@s42.

## Decisiones previas

- @s33 filas Bearer: `ApiCredentialBearerFilter` autentica la credencial y, para rutas fuera de su allowlist, responde `403 API_SCOPE_DENIED` (no 401). Por la decisión 5 del coordinador, esas dos filas se prueban con `403 API_SCOPE_DENIED` y sin tocar webhooks.
- Firma HMAC y AES-GCM viven en adaptadores (`javax.crypto` no está permitido en dominio/aplicación por ArchUnit). El dominio conserva catálogo, validación, política de direcciones y tabla de reintentos.
- Errores de aplicación: una sola `WebhookOperationException(Code)` para los códigos 4xx/5xx estables; `WebhookInvalidException(campos)` en dominio para `WEBHOOK_INVALID` con `errors[]`.

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

