# Cierre del dictamen de la feature 25 (webhooks) — carril A

Worktree `C:/Users/vhurt/ow-worktrees/webhooks`, rama `claude/webhooks`, base `c5f9f93`.
`E2E_WEB_PORT=18090`.

Hallazgos que YA venían cerrados en la base y que no se tocan: 6, 7, 8, 14, 19, 20, 22, 23.

Backend siempre por clase concreta (REGLAS.md §1). Ninguna ejecución de la suite completa.

---

## Hallazgo 1 y 17 — @s25: la fila TIMEOUT no tenía oráculo (parte a)

**Cubre:** `features/webhooks.feature:327` (fila «acepta la conexión y no responde»).

**Prueba:** `JdkWebhookSenderTest.s25_aReceiverThatAcceptsTheConnectionAndNeverAnswersIsATimeout`
más el test de cableado `b1_theProductionDeadlinesAreFiveSecondsToConnectAndTenForTheWholeExchange`.

**Ciclo.**

1. ROJO 1 (no compila, Ley 2). Escribo el test contra un constructor con plazos
   inyectables que no existe. `gradlew test --tests JdkWebhookSenderTest`:
   `error: constructor JdkWebhookSender in class JdkWebhookSender cannot be applied
   to given types; required: Clock,AddressPolicy,HostResolver`.
2. VERDE. Constructor package-private con `connectDeadline`/`exchangeDeadline`; el
   público delega en las constantes de producción (5 s / 10 s). `request()` usa
   `exchangeDeadline` en vez de la constante. Accesores package-private
   `connectDeadline()` (leído del propio `HttpClient`, no de un campo) y
   `exchangeDeadline()`.
3. ROJO ACREDITADO del oráculo de comportamiento. Rompo la producción: cambio
   `.timeout(exchangeDeadline)` por `.timeout(Duration.ofSeconds(30))`. Resultado:
   `s25_aReceiverThatAcceptsTheConnectionAndNeverAnswersIsATimeout() FAILED —
   org.opentest4j.AssertionFailedError at JdkWebhookSenderTest.java:248`
   (el receptor acaba respondiendo 200 y `errorClass` es null, no "TIMEOUT").
   Restaurado.
4. ROJO ACREDITADO del oráculo de cableado. Rompo la producción: `EXCHANGE_TIMEOUT`
   de 10 s a 9 s. Resultado:
   `b1_theProductionDeadlinesAreFiveSecondsToConnectAndTenForTheWholeExchange() FAILED —
   AssertionFailedError at JdkWebhookSenderTest.java:230`. Restaurado.
5. VERDE final: `BUILD SUCCESSFUL`.

**Ficheros cambiados.**
- `backend/src/main/java/com/apptolast/organization/adapter/webhook/JdkWebhookSender.java`
- `backend/src/test/java/com/apptolast/organization/adapter/webhook/JdkWebhookSenderTest.java`
- `features/webhooks.feature:327`

**Enmienda del contrato (REGLAS.md §8).** La fila 327 decía «acepta la conexión y no
responde **en 5 s**». Los 5 s son `CONNECT_TIMEOUT`, que sobre una conexión YA
ACEPTADA no aplica: el corte real es el plazo de intercambio de la enmienda B1
(10 s, `JdkWebhookSender:31`). Ejecutar la fila tal como estaba escrita habría
fallado. La fila pasa a «acepta la conexión y no responde **dentro del plazo de
intercambio**», y el valor concreto (10 s) queda fijado por el test de cableado
`b1_theProductionDeadlines...`, que es donde debe vivir un número, no en el
Gherkin. Se alinea contrato con código sin silenciar nada.

**Coste en la suite.** El test de plazo tarda ~0,3 s: los plazos se inyectan
(`TEST_EXCHANGE_DEADLINE = 300 ms`), no se esperan los 10 s de producción.

**Queda abierto de este hallazgo:** la fila TLS (certificado no confiable). Ver abajo.
