# Mutación de backend de la feature 25 — remedida, 10 de septiembre de 2026

**92.81 %** (400/431), calculado del `mutations.xml`, no leído de un HTML.

Sobre el SHA `95cf64bf`, árbol limpio, máquina drenada. 18 min 42 s.

## Cierra el bloqueante B2: el informe anterior era irreproducible

El informe que sostenía el «89,88 %» describía un árbol que **no existe en ningún
commit**. Las cuatro comprobaciones que lo delataban, ahora todas en orden:

| Comprobación | Antes | Ahora |
|---|---|---|
| Línea de `elapsedMillis` en el XML | 237 — no coincide con ningún commit | **239** — coincide con el fuente |
| `s25_latencyMsComesFromTheInjectedTicker` como `killingTest` | **0 menciones**, existiendo el test | **3 menciones** |
| `hasPendingPing` | `NO_COVERAGE`, invocándolo ocho veces | **4 menciones**, cubierto |
| Árbol durante la campaña | desconocido | limpio, sin un cambio de fuente |

## Cierra el bloqueante B3: el adaptador de bitácora ya recibe mutantes

`Slf4jWebhookAudit` salía con **cero** mutantes estando nombrado en su ámbito, y se
explicaba como «una propiedad de los mutadores». La causa real era el `avoidCallsTo`
**por defecto** de PIT, que suprime las llamadas a `org.slf4j`: la clase es puro
logging, así que no quedaba nada que mutar.

Quitado `org.slf4j` de esa lista, la clase recibe **3 mutantes** y
`WebhookSchedule` otros **4**. El arreglo funciona, o sea que no era una
propiedad inevitable de la herramienta. Cierra la misma condición en las features 27,
28 y 30, que la tenían idéntica.

## La cifra SUBE, y esta vez es legítimo

| | antes | ahora |
|---|---|---|
| Puntuación | 89,88 % (irreproducible) | **92.81 %** |
| Mutantes | 425 | 431 |

Sube porque el carril mató supervivientes de verdad y porque una clase dejó de salir
sin cobertura. **No sube por relajar nada**: el ámbito no se tocó, y de hecho se le
añadieron mutantes al dejar de eximir la bitácora.

## Los 31 sin matar, nominalmente

| Clase | Método | Línea | Mutador | Estado |
|---|---|---|---|---|
| `AesGcmWebhookSecrets` | `decrypt` | 110 | ConditionalsBoundaryMutator | SURVIVED |
| `AnchoredConnection` | `literal` | 48 | ConditionalsBoundaryMutator | SURVIVED |
| `AnchoredConnection` | `authority` | 57 | ConditionalsBoundaryMutator | SURVIVED |
| `AnchoredConnection` | `isAddressLiteral` | 76 | ConditionalsBoundaryMutator | SURVIVED |
| `JdkWebhookSender` | `send` | 174 | NullReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `send` | 176 | VoidMethodCallMutator | NO_COVERAGE |
| `JdkWebhookSender` | `send` | 177 | NullReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 183 | NegateConditionalsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 184 | NegateConditionalsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 184 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 185 | NegateConditionalsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 185 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 186 | NegateConditionalsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 186 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender` | `classify` | 188 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `PostgresWebhookStore` | `enqueuePing` | 158 | NullReturnValsMutator | SURVIVED |
| `PostgresWebhookWork` | `instant` | 207 | NullReturnValsMutator | SURVIVED |
| `PostgresWebhookWork` | `number` | 212 | NegateConditionalsMutator | SURVIVED |
| `PostgresWebhookWork` | `number` | 212 | EmptyObjectReturnValsMutator | SURVIVED |
| `WebhookAttempt` | `classify` | 51 | ConditionalsBoundaryMutator | SURVIVED |
| `WebhookConfiguration` | `webhookSchedule` | 18 | NullReturnValsMutator | SURVIVED |
| `WebhookController` | `text` | 81 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `WebhookController` | `allowedOn` | 235 | NegateConditionalsMutator | SURVIVED |
| `WebhookController` | `allowedOn` | 235 | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `WebhookController` | `allowedOn` | 236 | NegateConditionalsMutator | SURVIVED |
| `WebhookController` | `allowedOn` | 236 | EmptyObjectReturnValsMutator | SURVIVED |
| `WebhookIntent` | `validUrl` | 47 | BooleanTrueReturnValsMutator | NO_COVERAGE |
| `WebhookIntent` | `validUrl` | 53 | ConditionalsBoundaryMutator | SURVIVED |
| `WebhookIntent` | `validUrl` | 53 | ConditionalsBoundaryMutator | SURVIVED |
| `WebhookPingPayload` | `escape` | 49 | ConditionalsBoundaryMutator | SURVIVED |
| `WebhookSchedule` | `guarded` | 38 | VoidMethodCallMutator | SURVIVED |

## Por clase, de peor a mejor

| Clase | Puntuación | Mutantes |
|---|---|---|
| `WebhookConfiguration` | 0.0 % | 0/1 |
| `JdkWebhookSender` | 63.3 % | 19/30 |
| `WebhookSchedule` | 75.0 % | 3/4 |
| `PostgresWebhookWork` | 80.0 % | 12/15 |
| `AnchoredConnection` | 85.7 % | 18/21 |
| `WebhookPingPayload` | 87.5 % | 7/8 |
| `WebhookController` | 88.9 % | 40/45 |
| `WebhookIntent` | 90.9 % | 30/33 |
| `WebhookAttempt` | 95.0 % | 19/20 |
| `AesGcmWebhookSecrets` | 96.0 % | 24/25 |
| `PostgresWebhookStore` | 96.3 % | 26/27 |
| `WebhookDelivery` | 100.0 % | 24/24 |
| `ManageWebhook` | 100.0 % | 20/20 |
| `WebhookEndpoint` | 100.0 % | 20/20 |
| `EnqueueWebhookDeliveries` | 100.0 % | 15/15 |
| `PostgresWebhookOutbox` | 100.0 % | 14/14 |
| `WebhookDeliveryView` | 100.0 % | 12/12 |
| `WebhookEndpointView` | 100.0 % | 12/12 |
| `OutboxCandidate` | 100.0 % | 12/12 |
| `DispatchWebhooks` | 100.0 % | 10/10 |
| `WebhookDestinationGuard` | 100.0 % | 7/7 |
| `CreateWebhook` | 100.0 % | 6/6 |
| `ClaimedDelivery` | 100.0 % | 5/5 |
| `PostgresWebhookWork$LeasedRow` | 100.0 % | 5/5 |
| `WebhookController$Failure` | 100.0 % | 4/4 |
| `WebhookCursor` | 100.0 % | 4/4 |
| `RetrySchedule` | 100.0 % | 4/4 |
| `WebhookController$CreationRequest` | 100.0 % | 3/3 |
| `Slf4jWebhookAudit` | 100.0 % | 3/3 |
| `ReadyEndpoint` | 100.0 % | 3/3 |
| `WebhookConnectorStartup` | 100.0 % | 3/3 |
| `WebhookSignature` | 100.0 % | 3/3 |
| `WebhookCreation` | 100.0 % | 2/2 |
| `WebhookController$CreationView` | 100.0 % | 2/2 |
| `AesGcmWebhookSecrets$VersionedKey` | 100.0 % | 2/2 |
| `WebhookInvalidException` | 100.0 % | 2/2 |
| `WebhookController$DeliveryView` | 100.0 % | 1/1 |
| `WebhookOperationException` | 100.0 % | 1/1 |
| `WebhookController$EndpointList` | 100.0 % | 1/1 |
| `WebhookController$DeliveryList` | 100.0 % | 1/1 |
| `WebhookSecrets$1` | 100.0 % | 1/1 |
