# Mutación de backend — feature 25, webhooks

**92.81 %** (400/431), recomputado del `mutations.xml` contando
`KILLED` + `TIMED_OUT` sobre el total. **No** leído del entero del HTML.

Este fichero existe con **este nombre exacto** porque lo exige por nombre el punto 1 de
«Cambios requeridos» de `progress/judge_webhooks_cierre.md`. Es la **bitácora canónica**
de la puerta de backend de esta feature; el acta detallada de la campaña, con las cuatro
comprobaciones de reproducibilidad, está en `progress/mutacion_webhooks_backend_medida.md`
y el trabajo de cierre de sus bloqueantes en `progress/mutation_webhooks.md`. No hay dos
verdades: esos dos cuelgan de éste.

## Qué cerró esta medición

El informe anterior publicaba **89,88 %** y describía un árbol que **no existe en ningún
commit**: situaba `elapsedMillis` en la línea 237, y no está en la 237 en ninguna versión
commiteada. Además el test que la condición 5 del dictamen obligó a escribir **no aparecía
ni una vez** como `killingTest` pese a existir. Los dos hechos están contrastados en el
acta.

## Los 31 sin matar, nominalmente

| Clase                  | Método             | Línea | Mutador                      | Estado      |
| ---------------------- | ------------------ | ----- | ---------------------------- | ----------- |
| `AesGcmWebhookSecrets` | `decrypt`          | 110   | ConditionalsBoundaryMutator  | SURVIVED    |
| `AnchoredConnection`   | `literal`          | 48    | ConditionalsBoundaryMutator  | SURVIVED    |
| `AnchoredConnection`   | `authority`        | 57    | ConditionalsBoundaryMutator  | SURVIVED    |
| `AnchoredConnection`   | `isAddressLiteral` | 76    | ConditionalsBoundaryMutator  | SURVIVED    |
| `JdkWebhookSender`     | `send`             | 174   | NullReturnValsMutator        | NO_COVERAGE |
| `JdkWebhookSender`     | `send`             | 176   | VoidMethodCallMutator        | NO_COVERAGE |
| `JdkWebhookSender`     | `send`             | 177   | NullReturnValsMutator        | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 183   | NegateConditionalsMutator    | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 184   | NegateConditionalsMutator    | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 184   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 185   | NegateConditionalsMutator    | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 185   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 186   | NegateConditionalsMutator    | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 186   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `JdkWebhookSender`     | `classify`         | 188   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `PostgresWebhookStore` | `enqueuePing`      | 158   | NullReturnValsMutator        | SURVIVED    |
| `PostgresWebhookWork`  | `instant`          | 207   | NullReturnValsMutator        | SURVIVED    |
| `PostgresWebhookWork`  | `number`           | 212   | NegateConditionalsMutator    | SURVIVED    |
| `PostgresWebhookWork`  | `number`           | 212   | EmptyObjectReturnValsMutator | SURVIVED    |
| `WebhookAttempt`       | `classify`         | 51    | ConditionalsBoundaryMutator  | SURVIVED    |
| `WebhookConfiguration` | `webhookSchedule`  | 18    | NullReturnValsMutator        | SURVIVED    |
| `WebhookController`    | `text`             | 81    | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `WebhookController`    | `allowedOn`        | 235   | NegateConditionalsMutator    | SURVIVED    |
| `WebhookController`    | `allowedOn`        | 235   | EmptyObjectReturnValsMutator | NO_COVERAGE |
| `WebhookController`    | `allowedOn`        | 236   | NegateConditionalsMutator    | SURVIVED    |
| `WebhookController`    | `allowedOn`        | 236   | EmptyObjectReturnValsMutator | SURVIVED    |
| `WebhookIntent`        | `validUrl`         | 47    | BooleanTrueReturnValsMutator | NO_COVERAGE |
| `WebhookIntent`        | `validUrl`         | 53    | ConditionalsBoundaryMutator  | SURVIVED    |
| `WebhookIntent`        | `validUrl`         | 53    | ConditionalsBoundaryMutator  | SURVIVED    |
| `WebhookPingPayload`   | `escape`           | 49    | ConditionalsBoundaryMutator  | SURVIVED    |
| `WebhookSchedule`      | `guarded`          | 38    | VoidMethodCallMutator        | SURVIVED    |
