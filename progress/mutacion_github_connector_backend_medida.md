# Mutación de backend de la feature 27 — remedida, 10 de septiembre de 2026

**95.90 %** (421/439), calculado del `mutations.xml`.

Sobre el SHA `26a5a5eb`, árbol limpio, máquina drenada.

## Por qué había que remedirla: el 93,23 % era prestado

El ámbito llevaba el comodín `adapter.connectors.*`, que arrastraba
`GitlabApiBase` y `HttpGitlabIssueSource` — **81 mutantes de la feature 29**, ya
declarados en su propio ámbito. La puntuación de esta feature se sostenía en parte
sobre pruebas de otra.

**Comprobado en el XML nuevo: 0 mutantes de clases de GitLab.** El ámbito ya sólo
mide lo que es suyo.

| | antes | ahora |
|---|---|---|
| Puntuación | 93,23 % | **95.90 %** |
| Mutantes | 502, con 81 ajenos | 439, todos propios |

**Sube quitándole 81 mutantes prestados**, que es el mejor desenlace posible: la
feature estaba mejor probada de lo que decía su número, y el número decía otra cosa
por medir lo que no era.

## Las dos condiciones del juez que se comprueban aquí

- `Slf4jConnectorAudit` salía con **cero** mutantes estando nombrado en el ámbito, y
  era causal de rechazo por la condición 7. Ahora recibe **4**, de los que
  mueren 4. La causa era el `avoidCallsTo` por defecto de PIT, arreglado hoy.
- `AesGcmSecretCipher`: **17 mutantes, 17 muertos**. El mutante de
  frontera que el veredicto exigió matar uno por uno ya no sobrevive.

## Los 18 sin matar, nominalmente

| Clase | Método | Línea | Mutador | Estado |
|---|---|---|---|---|
| `BoundedResponse` | `expired` | 98 | ConditionalsBoundaryMutator | SURVIVED |
| `ConnectorKeyRing` | `malformed` | 55 | NegateConditionalsMutator | SURVIVED |
| `GithubConnectorController` | `githubUnavailable` | 217 | NullReturnValsMutator | NO_COVERAGE |
| `GithubConnectorController` | `titleOf` | 281 | EmptyObjectReturnValsMutator | SURVIVED |
| `HttpGithubIssueSource` | `get` | 113 | VoidMethodCallMutator | NO_COVERAGE |
| `HttpGithubIssueSource` | `retryAfter` | 158 | PrimitiveReturnsMutator | SURVIVED |
| `HttpGithubIssueSource` | `atLeastOneSecond` | 163 | ConditionalsBoundaryMutator | SURVIVED |
| `IssueImportReceipt` | `isRunning` | 52 | BooleanFalseReturnValsMutator | NO_COVERAGE |
| `IssueImportReceipt` | `isRunning` | 52 | BooleanTrueReturnValsMutator | NO_COVERAGE |
| `IssueImportReceipt` | `close` | 85 | NegateConditionalsMutator | SURVIVED |
| `PostgresConnectorConnectionStore` | `lambda$save$0` | 52 | EmptyObjectReturnValsMutator | SURVIVED |
| `PostgresConnectorConnectionStore` | `lambda$invalidate$0` | 82 | EmptyObjectReturnValsMutator | SURVIVED |
| `PostgresImportedTaskCommit` | `insertTask` | 88 | VoidMethodCallMutator | SURVIVED |
| `PostgresImportedTaskCommit` | `insertEvent` | 104 | VoidMethodCallMutator | SURVIVED |
| `PostgresImportedTaskCommit` | `insertLink` | 119 | VoidMethodCallMutator | SURVIVED |
| `PostgresIssueImportReceiptStore` | `lambda$progress$0` | 115 | EmptyObjectReturnValsMutator | SURVIVED |
| `PostgresIssueImportReceiptStore` | `lambda$finish$1` | 144 | NullReturnValsMutator | NO_COVERAGE |
| `PostgresIssueImportReceiptStore` | `missing` | 229 | NullReturnValsMutator | NO_COVERAGE |

## Por clase, de peor a mejor

| Clase | Puntuación | Mutantes |
|---|---|---|
| `PostgresImportedTaskCommit` | 81.2 % | 13/16 |
| `PostgresConnectorConnectionStore` | 81.8 % | 9/11 |
| `PostgresIssueImportReceiptStore` | 87.5 % | 21/24 |
| `ConnectorKeyRing` | 91.7 % | 11/12 |
| `IssueImportReceipt` | 91.9 % | 34/37 |
| `HttpGithubIssueSource` | 93.8 % | 45/48 |
| `BoundedResponse` | 95.2 % | 20/21 |
| `GithubConnectorController` | 95.9 % | 47/49 |
| `ImportIssues` | 100.0 % | 30/30 |
| `GithubApiBase` | 100.0 % | 27/27 |
| `ConnectorConfiguration` | 100.0 % | 19/19 |
| `PersonalAccessToken` | 100.0 % | 17/17 |
| `AesGcmSecretCipher` | 100.0 % | 17/17 |
| `GithubConnectorController$ImportResponse` | 100.0 % | 15/15 |
| `ExternalIssue` | 100.0 % | 15/15 |
| `IssueSourceException` | 100.0 % | 10/10 |
| `StoredConnection` | 100.0 % | 8/8 |
| `ConnectorFailures` | 100.0 % | 7/7 |
| `ConnectGithub` | 100.0 % | 7/7 |
| `IssuePage` | 100.0 % | 7/7 |
| `GithubConnectorController$ConnectionResponse` | 100.0 % | 6/6 |
| `ConnectionView` | 100.0 % | 6/6 |
| `GithubRepository` | 100.0 % | 6/6 |
| `IssueConnection` | 100.0 % | 5/5 |
| `GithubIssueConnections` | 100.0 % | 4/4 |
| `Slf4jConnectorAudit` | 100.0 % | 4/4 |
| `ReadIssueImport` | 100.0 % | 2/2 |
| `ReadGithubConnection` | 100.0 % | 2/2 |
| `ConnectorError` | 100.0 % | 2/2 |
| `IssueImportFailedException` | 100.0 % | 2/2 |
| `ConnectorRateLimitedException` | 100.0 % | 1/1 |
| `ConnectorKeyRing$ConnectorKey` | 100.0 % | 1/1 |
| `DisconnectGithub` | 100.0 % | 1/1 |
