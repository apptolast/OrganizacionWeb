# Backend architecture brief (backend/, Spring Boot 3.5.11, Java 25, Gradle Kotlin DSL)

Generado el 8 de septiembre de 2026 por un agente Explore para orientar a los implementadores de 24–30. Verificar rutas antes de usarlas.

## 1. Package layout — backend/src/main/java/com/apptolast/organization
- `domain/` — pure records/value objects + invariants (e.g. `Project`, `Task`, `Customization`, `OutboxMessage`). ArchUnit: may depend only on `java..` and `..domain..`.
- `application/` — use cases (`CreateTask`, `PrepareExportData`), input ports (`*UseCase` interfaces), output ports (`*Queries`/`*Commit`/`*Editing`/`*Store`-shaped interfaces), domain events (`TaskCreated`) and app exceptions. May depend only on `java..`, `..domain..`, `..application..`.
- `adapter/http/` — `@RestController`s + filters (`OriginGuard`, `ExportHeadersFilter`, `SessionFailureFilter`) + `ApiErrors` (RFC7807 `application/problem+json`).
- `adapter/persistence/` — `Postgres*` JdbcTemplate implementations of the output ports.
- `adapter/broker/` — `RabbitBrokerPublisher` (raw `com.rabbitmq:amqp-client`).
- `adapter/config/` — `ApplicationConfiguration` (all bean wiring, no `@Component` scanning of use cases), `SecurityConfiguration`, `PublisherConfiguration`, `PublisherSchedule`, `SessionCookiePolicy`, `JavaTimeZoneCatalog`.
- `adapter/logging/` — `Slf4jPublicationAudit`.
- Entry point: `OrganizationApplication.java`. Convention: classes are `final`, constructor injection, no field annotations in application/domain.

### Template A — `custom_views_fields` (write feature, full stack)
- domain: `domain/Customization.java`, `CustomizationRevision.java`, `CustomizationScope.java`, `CustomizationView.java`, `CustomFieldDefinition.java`, `CustomFieldLabel.java`, `CustomFieldType.java`, `CustomFieldValues.java`, `CustomFieldValuesRevision.java`
- ports: `application/CustomizationQueries.java`, `CustomizationEditing.java`, `CustomFieldValuesQueries.java`, `CustomFieldValuesEditing.java` (+ input ports `ReadCustomizationUseCase.java`, `SaveCustomizationViewUseCase.java`, `CreateCustomFieldUseCase.java`, `UpdateCustomFieldUseCase.java`, `ReadCustomFieldValuesUseCase.java`, `SaveCustomFieldValuesUseCase.java`)
- use cases: `application/ReadCustomization.java`, `SaveCustomizationView.java`, `CreateCustomField.java`, `UpdateCustomField.java`, `ReadCustomFieldValues.java`, `SaveCustomFieldValues.java`; error `CustomizationConflictException.java`
- controller: `adapter/http/CustomizationController.java` (`/api/v1/me/customization/{scope}`, `.../fields/{fieldId}`, `/api/v1/projects/{projectId}/custom-fields`)
- persistence: `adapter/persistence/PostgresCustomizationStore.java`
- migration: `src/main/resources/db/migration/V20__customization.sql`
- wiring: beans in `adapter/config/ApplicationConfiguration.java` (lines ~11-53)

### Template B — `export_data` (read-only streaming feature)
- ports: `application/ExportDataUseCase.java` (in), `application/ExportDataQueries.java` (out); result `application/PreparedExport.java`; error `ExportTooLargeException.java`
- use case: `application/PrepareExportData.java` (injects `ExportDataQueries` + `java.time.Clock`)
- controller: `adapter/http/ExportDataController.java` (`GET /api/v1/me/export`), filter `adapter/http/ExportHeadersFilter.java`
- persistence: `adapter/persistence/PostgresExportDataQueries.java` (+ `ExportJsonWriter.java`, `ExportBuffer.java`, `ExportReceiptWriter.java`)
- migration: none (read-only over existing tables)
- wiring: `ApplicationConfiguration.java` lines ~298-308

## 2. Auth / CSRF / adding an endpoint
- Session auth (form login, JDBC-backed sessions via `spring-session-jdbc`, table from `V6__jdbc_sessions.sql`, timeout 30m). La feature 24 (rama `codex/integration-api`) añade un canal Bearer stateless con credenciales personales con scopes: ver `docs/integration-api.md` en esa rama.
- Current user id = **`java.security.Principal` parameter on the controller method**, `principal.getName()` is the `ownerId` string passed as the first argument to every use case. No custom `@CurrentUser` annotation exists.
- Config: `adapter/config/SecurityConfiguration.java`. Single in-memory user from `app.auth.username` / `app.auth.password` (bcrypt-encoded at startup). `GET /api/session` is `permitAll`, **everything else `.anyRequest().authenticated()`** — a new endpoint is authenticated by default, no per-path change needed unless it must be public.
- Login `POST /api/session` (204), logout `POST /api/session/logout` (204). 401/403 bodies produced by `ApiErrors.problem(...)`; `SessionAccessDeniedHandler`.
- CSRF: Spring default (`csrf(Customizer.withDefaults())`), token fetched from `GET /api/session` → `csrfToken` + `csrfHeaderName` (`adapter/http/SessionController.java`). Plus `adapter/http/OriginGuard.java` registered before `CsrfFilter`: non-GET/HEAD/OPTIONS with an `Origin` != `app.public-origin` → 403 `UNTRUSTED_ORIGIN`. No CORS.
- New endpoint checklist: controller in `adapter/http`, use case bean in `ApplicationConfiguration`, `Principal` first param, errors via `ApiErrors`, `@WebMvcTest` API test.

## 3. Persistence
- **JdbcTemplate only** (`spring-boot-starter-jdbc`); no JPA/Hibernate anywhere. Adapters take `JdbcTemplate` + `PlatformTransactionManager`, wrapping work in `TransactionTemplate` (read paths set read-only + `ISOLATION_REPEATABLE_READ`).
- Flyway, `src/main/resources/db/migration/V<n>__snake_case.sql`. Latest en main = **V21__import_receipts.sql**; la feature 24 usa **V22__api_credentials.sql**. Reserva para 25–30: V23 webhooks, V24 ics_calendar, V25 github_connector, V26 external_calendar, V27 additional_connectors, V28 automations.
- Ownership: every table carries `owner_id TEXT NOT NULL`; per-owner uniqueness via `UNIQUE (owner_id, <scope/entity>)` or composite PK `(owner_id, request_key)`; optimistic locking via `version BIGINT NOT NULL CHECK (version >= 0)` + `updated_at TIMESTAMPTZ NOT NULL`; ids are `UUID PRIMARY KEY`; JSON columns `JSONB` with `jsonb_typeof` CHECKs.
- Outbox: table `outbox_events(event_id, aggregate_id, owner_id, event_type, schema_version, occurred_at, payload JSONB, status DEFAULT 'pending')` (`V1__projects_and_outbox.sql`, publication columns in `V2__outbox_publication.sql`).
- Emitting an event: the use case never touches the outbox directly. It calls an output port whose method takes a callback returning both the aggregate and the event, e.g. `application/TaskCommit.java`: `Task save(String ownerId, UUID projectId, Function<String, TaskCreation> operation)` where `TaskCreation(Task task, TaskCreationEvent event)`. The adapter (`adapter/persistence/PostgresTaskCommit.java`) inserts the row and `INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)` in the same transaction.
- Event payload shape (validated by `domain/OutboxMessage.validationCode()`): always `eventId, aggregateId, ownerId, occurredAt, schemaVersion(=1), type` plus per-type fields; `type` is `"<Name>.v1"` and must be whitelisted in `OutboxMessage` or publication fails with `UNSUPPORTED_EVENT`/`INVALID_EVENT`.
- Draining port: `application/OutboxWork.java` → `Optional<PublicationAttempt> processNext(Instant now, Set<UUID> excluded, Function<OutboxMessage, PublicationAttempt> operation)`, implemented by `adapter/persistence/PostgresOutboxWork.java`, driven by `application/PublishOutbox.java`.

## 4. Testing
- No shared base class. Integration tests declare their own container inline: `@Testcontainers` on the class + `@Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine")`, then `Flyway.configure().dataSource(...).load().migrate()` in `@BeforeAll`, and a `DriverManagerDataSource` + `JdbcTemplate` + `DataSourceTransactionManager` built by hand. Docker required; no env vars needed.
- API tests: **MockMvc** via `@WebMvcTest(controllers = X.class, properties = {"app.auth.username=owner","app.auth.password=test-only-secret","app.public-origin=https://organization.example"})` + `@Import(SecurityConfiguration.class)`, collaborators as `@MockitoBean`, requests with `.with(user("owner"))` and `.with(csrf().asHeader())`. No WebTestClient.
- Naming: `<Feature>ApiTest` (adapter/), `<Feature>PersistenceTest` (adapter/persistence/), `<Feature>WiringTest` (adapter/config/), plain `<Class>Test` for domain/application; test methods often prefixed with a scenario id (`s17_...`). Layering guarded by `src/test/java/com/apptolast/organization/ArchitectureTest.java` (ArchUnit).
- `tasks.test` sets `systemProperty("api.version","1.44")` and `outbox.test.classpath`.

## 5. PIT mutation scoping (`backend/build.gradle.kts`, `pitest { }` block)
- Invoked as `./gradlew -PmutationScope=<name> pitest`; read via `providers.gradleProperty("mutationScope")`. Existing scopes include `create_task`, `split_task`, `complete_reopen_task`, `authentication`, `availability`, `schedule_block`, `today`, `reschedule`, `start_work_session`, `pause_resume_session`, `close_work_session`, `end_time_notification`, `history`, `weekly_review`, `appearance`, `custom_views_fields`, `export_data_persistence`, `import_data_reader`, `import_data_http`, `import_data_persistence`. Threshold 80%, `-FRECORD`, 4 threads.
- To add a scope `my_feature`, add four pieces:
```kotlin
val myFeatureOnly = scope == "my_feature"                       // 1. flag
val myFeatureClasses = setOf(                                    // 2. class globs
    "com.apptolast.organization.domain.MyThing*",
    "com.apptolast.organization.application.MyUseCase*",
    "com.apptolast.organization.adapter.http.MyController*",
    "com.apptolast.organization.adapter.persistence.PostgresMyStore*",
    "com.apptolast.organization.adapter.config.ApplicationConfiguration")
targetClasses.set(when { myFeatureOnly -> myFeatureClasses; /* existing branches */ ... })
targetTests.set(when { myFeatureOnly -> setOf("com.apptolast.organization.*"); ... })  // 3. let PIT pick by coverage
if (myFeatureOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-my-feature"))  // 4. report dir
```
  Also append `myFeatureClasses` to the `else ->` union in `targetClasses` (and the matching test set in `targetTests`) so CI's default profile keeps the new adapters.

## 6. Config & runtime
- `src/main/resources/application.properties` (no application.yml). Everything is env-substituted: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_AUTH_USERNAME`, `APP_AUTH_PASSWORD`, `APP_PUBLIC_ORIGIN`, `APP_MAX_ACTIVE_PROJECTS` (default 3), `OUTBOX_PUBLISHER_ENABLED` (default false), `RABBITMQ_HOST/PORT/USERNAME/PASSWORD/VHOST`. Property names are `app.*`; env names are `APP_*`/`DB_*`/`RABBITMQ_*`. Read in code with `@Value("${app.…}")`.
- Hardening: `server.error.include-message=never`, `include-stacktrace=never`, `spring.jackson.deserialization.fail-on-trailing-tokens=true`, `spring.session.jdbc.initialize-schema=never`, Hikari connection-timeout 3000ms.
- Scheduler: `adapter/config/PublisherConfiguration.java` is `@ConditionalOnProperty("app.publisher.enabled"=true)` + `@EnableScheduling`; `adapter/config/PublisherSchedule.java#tick()` is `@Scheduled(fixedDelay = 1000, initialDelay = 1000)` and calls `PublishOutboxUseCase.runCycle()`. Misconfigured broker credentials yield a no-op schedule plus `audit.workerError("CONFIGURATION_ERROR")`.

Not found: no `application.yml`, no JPA entities/repositories, no custom current-user annotation, no shared abstract test base class.
