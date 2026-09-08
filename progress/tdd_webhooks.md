# TDD — Feature 25 `webhooks`

Worktree `C:/Users/vhurt/ow-worktrees/webhooks`, rama `claude/webhooks`. Contrato: `features/webhooks.feature` (42 escenarios). Inicio: 8 de septiembre de 2026.

Feature en curso: 25 — webhooks. Escenarios a recorrer: @s1…@s42.

## Decisiones previas

- @s33 filas Bearer: `ApiCredentialBearerFilter` autentica la credencial y, para rutas fuera de su allowlist, responde `403 API_SCOPE_DENIED` (no 401). Por la decisión 5 del coordinador, esas dos filas se prueban con `403 API_SCOPE_DENIED` y sin tocar webhooks.
- Firma HMAC y AES-GCM viven en adaptadores (`javax.crypto` no está permitido en dominio/aplicación por ArchUnit). El dominio conserva catálogo, validación, política de direcciones y tabla de reintentos.
- Errores de aplicación: una sola `WebhookOperationException(Code)` para los códigos 4xx/5xx estables; `WebhookInvalidException(campos)` en dominio para `WEBHOOK_INVALID` con `errors[]`.

## Bitácora de ciclos

