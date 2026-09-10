# Mutación de backend de la feature 29 — la primera de su historia

**96.50 %** (331/343), calculado del `mutations.xml`. 10 de septiembre de 2026.

Sobre el SHA `bb4171d7`, árbol limpio, máquina drenada.

## Es la primera vez que esta feature se mide

El bloqueante B2 del panel decía, literalmente, que **no existía ninguna medición de
mutación de backend para esta feature**. Era cierto, y por dos razones que se
arreglaron hoy:

1. El ámbito era **el único de los 34 sin `reportDir` propio**: escribía en el
   genérico `reports/pitest`, donde la siguiente campaña lo habría pisado sin dejar
   rastro.
2. Los dos intentos anteriores murieron: uno porque dos pruebas de 32 MiB a través de
   nginx no pasaron sin mutación con nueve carriles encima, y otro por memoria.

## Las tres clases que estaban fuera del ámbito y ahora sí se miden

| Clase | Mutantes | Por qué faltaba |
|---|---|---|
| `BoundedResponse` | 21 | La creó esta feature para el techo de 5 MiB del `@s25`, y al quitar el comodín de la 27 se habría quedado sin campaña ninguna |
| `GitlabProjectPath` | 18 | No estaba declarada en ningún ámbito |
| `ImportGuard` | 1 | Estaba en el ámbito de la 27 por error: sólo la invocan `ConnectGitlab` y `DisconnectGitlab` |

## Los 12 sin matar, nominalmente

| Clase | Método | Línea | Mutador | Estado |
|---|---|---|---|---|
| `BoundedResponse` | `expired` | 98 | ConditionalsBoundaryMutator | SURVIVED |
| `ConnectorCatalog` | `lambda$row$1` | 15 | NullReturnValsMutator | NO_COVERAGE |
| `GitlabConnectorController` | `delete` | 92 | VoidMethodCallMutator | SURVIVED |
| `GitlabConnectorController` | `startImport` | 105 | VoidMethodCallMutator | SURVIVED |
| `GitlabConnectorController` | `titleOf` | 268 | EmptyObjectReturnValsMutator | SURVIVED |
| `GitlabConnectorController$ConnectionResponse` | `lastActivityAt` | 291 | NullReturnValsMutator | SURVIVED |
| `HttpGitlabIssueSource` | `get` | 139 | VoidMethodCallMutator | NO_COVERAGE |
| `HttpGitlabIssueSource` | `retryAfter` | 180 | PrimitiveReturnsMutator | SURVIVED |
| `HttpGitlabIssueSource` | `retryAfter` | 181 | MathMutator | NO_COVERAGE |
| `HttpGitlabIssueSource` | `retryAfter` | 181 | PrimitiveReturnsMutator | NO_COVERAGE |
| `HttpGitlabIssueSource` | `atLeastOneSecond` | 185 | ConditionalsBoundaryMutator | SURVIVED |
| `PostgresGitlabConnectionStore` | `lambda$save$0` | 63 | EmptyObjectReturnValsMutator | SURVIVED |

## Por clase, de peor a mejor

| Clase | Puntuación | Mutantes |
|---|---|---|
| `ConnectorCatalog` | 80.0 % | 4/5 |
| `GitlabConnectorController$ConnectionResponse` | 88.9 % | 8/9 |
| `HttpGitlabIssueSource` | 90.7 % | 49/54 |
| `PostgresGitlabConnectionStore` | 92.9 % | 13/14 |
| `GitlabConnectorController` | 93.5 % | 43/46 |
| `BoundedResponse` | 95.2 % | 20/21 |
| `GitlabApiBase` | 100.0 % | 27/27 |
| `GitlabProjectPath` | 100.0 % | 18/18 |
| `GitlabConnection` | 100.0 % | 15/15 |
| `WebhookStatusSource` | 100.0 % | 14/14 |
| `GitlabConnectorController$ImportResponse` | 100.0 % | 14/14 |
| `ApiCredentialStatusSource` | 100.0 % | 11/11 |
| `GithubStatusSource` | 100.0 % | 11/11 |
| `GitlabConnectionView` | 100.0 % | 10/10 |
| `ConnectGitlab` | 100.0 % | 8/8 |
| `ExternalCalendarStatusSource` | 100.0 % | 8/8 |
| `ConnectorRow` | 100.0 % | 8/8 |
| `GitlabIssueConnections` | 100.0 % | 7/7 |
| `ReadGitlabConnection` | 100.0 % | 6/6 |
| `ConnectorCatalogController$RowResponse` | 100.0 % | 5/5 |
| `ReadConnectorCatalog` | 100.0 % | 5/5 |
| `GitlabConnectorController$ErrorResponse` | 100.0 % | 4/4 |
| `ConnectorCatalogController$ErrorResponse` | 100.0 % | 4/4 |
| `IcsCalendarStatusSource` | 100.0 % | 4/4 |
| `GitlabStatusSource` | 100.0 % | 3/3 |
| `GitlabProject` | 100.0 % | 2/2 |
| `DisconnectGitlab` | 100.0 % | 2/2 |
| `ConnectorCatalogController$CatalogResponse` | 100.0 % | 2/2 |
| `ConnectorError` | 100.0 % | 2/2 |
| `ConnectorCatalogController` | 100.0 % | 2/2 |
| `ConnectorRateLimitedException` | 100.0 % | 1/1 |
| `ImportGuard` | 100.0 % | 1/1 |
