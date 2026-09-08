# Destilación Gherkin — Feature 25 `webhooks`

Fecha: 8 de septiembre de 2026. Fuente normativa: `progress/proposal_webhooks.md` (el coordinador la integra tal cual en `project-spec.md` como sección 25). Contrato: `features/webhooks.feature`, 42 escenarios `@s1`–`@s42`. Dialecto: palabras clave Gherkin en inglés y prosa en español, igual que `export_data.feature` y `custom_views_fields.feature` (ningún `.feature` del repositorio usa `# language: es`).

No se han tocado `project-spec.md`, `feature_list.json` ni código. El estado de la feature sigue `pending` hasta que el coordinador integre la sección y decida el paso a `spec_ready`.

## Mapa @s → sección de la propuesta

| @s | Escenario | Sección de la propuesta |
| --- | --- | --- |
| @s1 | Crear devuelve secreto una vez y cursor en el presente | API `POST /`; Modelo (cursor inicial); DTO `endpoint` |
| @s2 | URL sólo https absoluto | API `POST /` (`url`); Errores `WEBHOOK_INVALID` |
| @s3 | Descripción y tipos campo a campo | API `POST /` (`description`, `eventTypes`); Propósito (catálogo de doce) |
| @s4 | Frontera HTTP: estructura y 4096 bytes | API (JSON cerrado, cuerpo máximo); Errores `WEBHOOK_TOO_LARGE`; Prioridad |
| @s5 | SSRF en creación | Seguridad y privacidad (guardia SSRF); Errores `WEBHOOK_URL_BLOCKED` / `WEBHOOK_URL_UNRESOLVABLE` |
| @s6 | Cupo de cinco cuenta desactivados | API (cinco endpoints, activos y desactivados); Errores `WEBHOOK_LIMIT` |
| @s7 | Carrera por la última plaza | Concurrencia (`pg_advisory_xact_lock`, un 201 y un 409) |
| @s8 | Secreto sólo en claro en la creación; cifrado con AAD | Seguridad (AES-256-GCM, AAD = id, nunca en lecturas ni logs) |
| @s9 | Sin clave válida: arranque degradado y 503 selectivo | Seguridad (clave ausente o mal formada); Errores `CONNECTORS_DISABLED` |
| @s10 | Listar propio, orden `createdAt DESC, id DESC` | API `GET /` |
| @s11 | Ajeno = inexistente en todas las rutas | API (`GET /{id}` ajeno o inexistente 404); Errores (un endpoint ajeno responde igual) |
| @s12 | Estado binario, idempotente, sin ETag | API `PUT /{id}/status` |
| @s13 | Eliminar: cascada, libera plaza, repetir 404 | API `DELETE /{id}`; Modelo (`ON DELETE CASCADE`); cupo |
| @s14 | Ping encolado, nunca síncrono; 409 según estado | API `POST /{id}/ping`; Decisión 11 |
| @s15 | Firma `t`/`v1` con vector HMAC concreto | Entrega (petición y firma); Verificación (vector conocido con `t` fijo) |
| @s16 | Cabeceras salientes, sin credenciales, respuesta descartada | Entrega (petición); Seguridad (cliente HTTP) |
| @s17 | Cuerpo del ping sintético | Entrega (`webhook.ping.v1`) |
| @s18 | Encolar sólo propios y suscritos; outbox intacta | Entrega (paso encolar); Propósito (fuente única) |
| @s19 | Cursor: creación, gracia de 5 s, orden por tupla | Entrega (encolar); Modelo (cursor); Límites |
| @s20 | Una entrega de outbox en vuelo; orden; ping intercalado | Entrega (encolar); Decisión 6; Límites (orden por endpoint) |
| @s21 | `blocked` e inválidos: sin entrega, cursor avanza | Entrega (`status <> 'blocked'`, `validationCode()` no nulo) |
| @s22 | Reinicio durante el envío: al menos una vez | Concurrencia (reclamación transaccional, al menos una vez) |
| @s23 | Dos instancias con `SKIP LOCKED` | Concurrencia (`FOR UPDATE SKIP LOCKED`); Límites |
| @s24 | Tabla de reintentos 1/5/30/120/1440 min y agotamiento | Entrega (reintentos) |
| @s25 | Clases de error, sin seguir 3xx, `httpStatus` sólo con respuesta | Entrega (`error_class`); Seguridad (cliente HTTP) |
| @s26 | Cualquier 2xx es `succeeded`; respuesta no almacenada | Entrega (éxito) |
| @s27 | Agotar desactiva con `DELIVERY_EXHAUSTED` en la misma transacción | Entrega (reintentos); Modelo (`disabled_reason`) |
| @s28 | Reactivar reanuda desde el cursor; agotada no se repite | API `PUT /status`; Decisión 9 |
| @s29 | Registro de 50, poda sólo terminales, DTO `delivery` cerrado | API `GET /{id}/deliveries`; Entrega (poda); Decisión 10 |
| @s30 | Redeliver: terminales y activo; 409/404 | API `POST .../redeliver` |
| @s31 | Reenvío con cuerpo original y firma nueva | Entrega (body idéntico, sólo `t` cambia) |
| @s32 | `app.webhooks.enabled`, ciclo de 1 s, 20 por ciclo | Entrega (`WebhookSchedule`) |
| @s33 | Sesión, Bearer, CSRF, Origin, 405 | API (sólo sesión cookie, CSRF, OriginGuard, Bearer fuera de allowlist) |
| @s34 | Prioridad de errores | Errores (Prioridad) |
| @s35 | Logs y problem+json sin URL completa, firma ni secreto | Entrega (auditoría); Seguridad |
| @s36 | Ruta `/webhooks`, estados cargando/vacío/error/lista | Interfaz (ruta, estado vacío) |
| @s37 | Formulario, doble envío, secreto una vez con Copiar | Interfaz (formulario, secreto) |
| @s38 | Secreto y peticiones no sobreviven a salida ni otra identidad | Interfaz (ocultación, guardas de identidad/abort) |
| @s39 | Acciones de la lista y estados texto + ARIA | Interfaz (lista) |
| @s40 | Panel de entregas manual con Reenviar | Interfaz (panel de entregas) |
| @s41 | Errores del servidor explicados sin reintento | Interfaz (503, respuesta incierta) |
| @s42 | Teclado, 320/768/1280/1440, 200 %, axe | Interfaz (UX30, responsive); `docs/ux-requirements.md` |

Cobertura por grupo pedido: creación @s1–@s9; gestión @s10–@s13; entrega @s14–@s23; reintentos @s24–@s28; registro y reenvío @s29–@s31; worker @s32 (+@s22, @s23); seguridad @s33–@s35; interfaz @s36–@s42.

## Revisión adversarial propia: huecos detectados y cómo se resolvieron

Lentes aplicadas: satisfacibilidad, mensurabilidad de cada `Then`, qué mutante mata cada aserción, fidelidad a la propuesta y colisión con features `done`. Ninguna resolución contradice una decisión fijada; donde la propuesta calla se eligió la convención ya vigente en el repositorio y se deja aquí anotado.

1. **Código para JSON malformado (propiedad desconocida, duplicada, tokens finales).** La propuesta exige JSON cerrado pero sólo nombra `WEBHOOK_INVALID` para campos. Resolución: `@s4` y `@s34` usan `400 MALFORMED_JSON`, código que ya emplean 23 escenarios de features cerradas para el mismo caso. La prioridad «tamaño y estructura antes que valores» de la propuesta se conserva.

2. **Id de ruta que no es un UUID canónico.** La propuesta fija 404 para ajeno o inexistente pero no dice nada de `/webhooks/no-es-uuid`. Resolución: `@s11` lo incluye en el mismo `404 WEBHOOK_NOT_FOUND` con cuerpo idéntico, porque cualquier otra respuesta (400) crearía una tercera clase distinguible y la propuesta pide que ajeno e inexistente sean indistinguibles. Ver duda abierta 1.

3. **Interacción entre ping/redeliver y la regla «una entrega de outbox en vuelo».** La propuesta dice «endpoint activo sin entrega pendiente derivada de la outbox» y «como máximo un ping pendiente», sin cruzarlas. Resolución: `@s14` fija que un ping se acepta aunque exista una entrega de outbox pendiente, y `@s20` fija que el ping se intercala sin alterar el orden E1, E2, E3. Un redeliver pendiente se trata como entrega derivada de la outbox (bloquea el encolado del siguiente evento hasta ser terminal), coherente con la decisión 6. Ver duda abierta 2.

4. **Vector de firma verificable.** La propuesta pide «firma HMAC con vector conocido y `t` fijo» pero no da el vector. Resolución: `@s15` fija secreto `whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8` (32 bytes 0x00..0x1F), un cuerpo de 209 bytes y dos instantes; las firmas se calcularon con HMAC-SHA256 sobre `t + "." + body` usando como clave los bytes UTF-8 de la cadena completa `whsec_...` (no los 32 bytes decodificados). Este detalle no está escrito en la propuesta; el vector lo fija y `docs/webhooks.md` debe decirlo igual. Ver duda abierta 3.

5. **Longitud del cifrado y AAD.** La propuesta describe `nonce(12) || AES-GCM || tag(16)` sin decir si el texto plano son los 49 bytes ASCII de `whsec_...` o los 32 bytes crudos. `@s8` afirma «al menos 60 bytes» (12 + 32 + 16) para no fijar lo que la propuesta no fija, y sí exige que descifrar con el AAD de otro endpoint falle, que es la aserción que mata el mutante que ignora el AAD.

6. **Anchos de la evidencia e2e.** La propuesta cita 320, 768 y 1440 px; la delegación pide 320, 768 y 1280. `@s42` exige los cuatro, que caen dentro de la matriz de `docs/ux-requirements.md`.

7. **Mutantes que cada grupo mata (comprobación de que ningún `Then` sólo cuenta).** `@s19` distingue `<=` de `<` en la ventana de gracia y en la tupla del cursor con instantes exactos; `@s24` fija cada plazo con valor absoluto, no «creciente»; `@s6` distingue «cuenta desactivados» con la fila 0/5; `@s27` exige la misma transacción con el caso de fallo; `@s12` distingue idempotencia conservando `disabledAt` de una fecha anterior distinta al reloj.

## Dudas abiertas para el coordinador

1. Confirmar `404 WEBHOOK_NOT_FOUND` (y no 400) para ids de ruta no canónicos. Si se prefiere 400, sólo cambia la última fila de `@s11`.
2. Confirmar que una entrega reenviada cuenta como «derivada de la outbox» y bloquea el encolado del siguiente evento hasta ser terminal. La alternativa (no bloquear) rompe la garantía de orden por endpoint que la propuesta promete.
3. Confirmar la clave HMAC: bytes UTF-8 de la cadena `whsec_...` completa (lo habitual en receptores; permite pegar el secreto tal cual). Si se decide usar los 32 bytes decodificados, hay que recalcular las dos firmas de `@s15` y `docs/webhooks.md` debe explicarlo.
4. El agotamiento de un **ping** también desactiva el endpoint con `DELIVERY_EXHAUSTED` (la propuesta trata todas las entregas igual). Se ha mantenido uniforme; señalar si el ping debe exceptuarse.
5. `@s33` asume que una credencial Bearer de la feature 24 contra `/api/v1/me/webhooks` responde `401 UNAUTHENTICATED`, como una petición sin sesión. La feature 24 vive en otra rama (`codex/integration-api`) y su allowlist aún no está en `project-spec.md`; si su filtro responde con otro código a rutas fuera de allowlist, ajustar esas dos filas.
6. La propuesta omite el valor exacto de `Location`; `@s1` fija `/api/v1/me/webhooks/<id>`.

## Decisiones del coordinador (8 de septiembre de 2026, 20:45)

1. Ids de ruta no canónicos → `404 WEBHOOK_NOT_FOUND` con cuerpo idéntico (confirmado).
2. Una entrega reenviada cuenta como derivada de la outbox y bloquea el encolado siguiente hasta ser terminal (confirmado: preserva el orden por endpoint).
3. Clave HMAC = bytes UTF-8 de la cadena completa `whsec_...` (confirmado; `docs/webhooks.md` lo documenta).
4. El agotamiento de un ping también desactiva el endpoint con `DELIVERY_EXHAUSTED` (uniforme, confirmado).
5. `@s33`: el tdd_craftsman verifica el comportamiento real del filtro Bearer de la feature 24 en su worktree (`codex/integration-api` ya incluida) y ajusta el código de respuesta de esas dos filas al que devuelva el filtro para rutas fuera de su allowlist, anotándolo en `progress/tdd_webhooks.md`.
6. `Location: /api/v1/me/webhooks/<id>` (confirmado).
