# Handoff backend 24

Corte de creación durable compilable. `CreateApiCredentialUseCase.create(String owner, UUID id, String name, List<String> scopes, int expiresInDays)` devuelve `ApiCredentialCreation(credential, secret)`. `ApiCredential` contiene exactamente id, name, scopes, createdAt, expiresAt, revokedAt. El secreto no-null señala la primera creación; el replay posterior devolverá null. Hay bean de creación real y adaptador PostgreSQL con V22.

C controla JSON cerrado, tamaño, header y UUID canónico HTTP; A controla valores de intención, persistencia y errores de aplicación. `ApiCredentialCommit.create(owner,id,intent,Supplier<ApiCredentialIssuance>)` invoca el supplier tras idempotencia, usa su createdAt único para contar cupo y devuelve después de commit; el supplier produce metadata, secreto transitorio y verificador. Nunca almacenar ni registrar el secreto. El corte prueba emisión, replay, colisiones, cupo, carreras y fallos de INSERT/COMMIT; otros casos siguen pendientes.

Frontera Bearer acordada para siguientes ciclos, aún sin código: authenticate(token) devuelve acceso id/owner/scopes sin cuota; consume(access) revalida propietario habilitado, revocación y caducidad bajo lock y consume ambos límites. HTTP intercala Origin y allowlist antes de consume. OpenAPI usa sólo authenticate. No usar sesión JDBC ni cachear autorizaciones.

A posee dominio/aplicación/persistencia/V22/wiring; C posee HTTP/seguridad/OpenAPI en aislado. Ningún cambio de build/scopes hasta coordinación root. Fixture PG de integración reutiliza un contenedor y migración por JVM con limpieza de sus datos por test; no arranque por mutante ni omisión de oráculos.

Errores reales: domain.ApiCredentialInvalidException.errors() List<FieldError>; application.ApiCredentialConflictException, ApiCredentialLimitException y StorageUnavailableException. HTTP traduce códigos24 sin duplicar reglas de intención.


Corte de gestión: `ReadApiCredentialsUseCase.find(owner, UUID)` devuelve Optional<ApiCredential>; `list(owner, String cursor)` devuelve ApiCredentialPage(items, nextCursor), cursor null inicial. `RevokeApiCredentialUseCase.revoke(owner, UUID)` devuelve Optional<ApiCredential>. Empty equivale a 404 tanto ausente como ajena. Beans reales incluidos. El cursor es opaco y se valida en backend, error ApiCredentialInvalidException con field cursor. Revocación conserva primera fecha incluso anterior a creación por retroceso del Clock; no exigir orden nuevo en DTO HTTP/UI.


Corte Bearer nominal compilable: `AuthenticateApiCredentialUseCase.authenticate(String token)` devuelve `ApiCredentialAccess(UUID id,String owner,List<String> scopes)`. `ConsumeApiQuotaUseCase.consume(ApiCredentialAccess)` devuelve void. `ApiUnauthenticatedException` es el rechazo uniforme; `ApiRateLimitedException.retryAfterSeconds()` devuelve int; `StorageUnavailableException` conserva 503. Beans reales disponibles. Authenticate no consume cuota. Consume revalida owner habilitado, scopes, revocación y caducidad después de los locks owner/id; usa Clock dentro de la frontera. Predicate de propietario consulta UserDetailsService bootstrap, sin Spring Session JDBC. La selección de canal y el orden Origin/allowlist siguen siendo de C. OpenAPI sólo authenticate.
