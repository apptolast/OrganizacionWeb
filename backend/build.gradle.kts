plugins {
    java
    id("com.diffplug.spotless") version "7.2.1"
    id("org.springframework.boot") version "3.5.11"
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
}

group = "com.apptolast"
version = "0.1.0"
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
repositories { mavenCentral() }
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.11"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.session:spring-session-jdbc")
    implementation("com.rabbitmq:amqp-client")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.test {
    useJUnitPlatform()
    systemProperty("api.version", "1.44")
    doFirst { systemProperty("outbox.test.classpath", sourceSets.test.get().runtimeClasspath.asPath) }
}
pitest {
    pitestVersion.set("1.22.0")
    junit5PluginVersion.set("1.2.3")
    val scope = providers.gradleProperty("mutationScope").orNull
    val webhooksOnly = scope == "webhooks"
    val icsCalendarOnly = scope == "ics_calendar"
    val githubConnectorOnly = scope == "github_connector"
    val additionalConnectorsOnly = scope == "additional_connectors"
    val integrationApiOnly = scope == "integration_api"
    val integrationApiHttpOnly = scope == "integration_api_http"
    val automationsOnly = scope == "automations"
    val importReaderOnly = scope == "import_data_reader"
    val importHttpOnly = scope == "import_data_http"
    val importPersistenceOnly = scope == "import_data_persistence"
    val exportPersistenceOnly = scope == "export_data_persistence"
    val externalCalendarOnly = scope == "external_calendar"
    val appearanceOnly = scope == "appearance"
    val customizationOnly = scope == "custom_views_fields"
    val weeklyReviewOnly = scope == "weekly_review"
    val historyOnly = scope == "history"
    val authenticationOnly = scope == "authentication"
    val taskOnly = scope == "create_task"
    val splitOnly = scope == "split_task"
    val taskStatusOnly = scope == "complete_reopen_task"
    val availabilityOnly = scope == "availability"
    val scheduleBlockOnly = scope == "schedule_block"
    val todayOnly = scope == "today"
    val rescheduleOnly = scope == "reschedule"
    val pauseResumeSessionOnly = scope == "pause_resume_session"
    val closeWorkSessionOnly = scope == "close_work_session"
    val endTimeNotificationOnly = scope == "end_time_notification"
    val startWorkSessionOnly = scope == "start_work_session"
    val startWorkSessionReplayOnly = scope == "start_work_session_replay"
    val core = setOf("com.apptolast.organization.domain.*", "com.apptolast.organization.application.*")
    // Feature 29: el conector GitLab y el catalogo de conectores. No incluye las
    // clases compartidas con la 27 (ImportIssues, IssueImportReceipt), que las
    // muta el ambito github_connector: un mutante contado dos veces no informa.
    val additionalConnectorsClasses = setOf(
        "com.apptolast.organization.application.ConnectGitlab*",
        "com.apptolast.organization.application.DisconnectGitlab*",
        "com.apptolast.organization.application.ReadGitlabConnection*",
        "com.apptolast.organization.application.GitlabConnection*",
        "com.apptolast.organization.application.GitlabIssueConnections*",
        "com.apptolast.organization.application.GitlabProject*",
        "com.apptolast.organization.application.GitlabUnavailableException*",
        "com.apptolast.organization.application.ConnectorStatusSource*",
        "com.apptolast.organization.adapter.connectors.GitlabApiBase*",
        "com.apptolast.organization.adapter.connectors.HttpGitlabIssueSource*",
        "com.apptolast.organization.adapter.http.GitlabConnectorController*",
        "com.apptolast.organization.adapter.persistence.PostgresGitlabConnectionStore*")
    val githubConnectorClasses = setOf(
        "com.apptolast.organization.domain.GithubRepository*",
        "com.apptolast.organization.domain.PersonalAccessToken*",
        "com.apptolast.organization.domain.ExternalIssue*",
        "com.apptolast.organization.domain.IssueImportReceipt*",
        "com.apptolast.organization.application.ConnectGithub*",
        "com.apptolast.organization.application.ImportGithubIssues*",
        "com.apptolast.organization.application.ReadGithubConnection*",
        "com.apptolast.organization.application.DisconnectGithub*",
        "com.apptolast.organization.application.ReadIssueImport*",
        "com.apptolast.organization.application.ConnectorFailures*",
        "com.apptolast.organization.application.IssueSourceException*",
        "com.apptolast.organization.application.IssuePage*",
        "com.apptolast.organization.application.StoredConnection*",
        "com.apptolast.organization.application.ConnectionView*",
        "com.apptolast.organization.adapter.connectors.*",
        "com.apptolast.organization.adapter.http.GithubConnectorController*",
        "com.apptolast.organization.adapter.persistence.PostgresConnectorConnectionStore*",
        "com.apptolast.organization.adapter.persistence.PostgresIssueImportReceiptStore*",
        "com.apptolast.organization.adapter.persistence.PostgresImportedTaskCommit*",
        "com.apptolast.organization.adapter.logging.Slf4jConnectorAudit*",
        "com.apptolast.organization.adapter.config.ConnectorConfiguration")
    val integrationApiClasses = setOf(
        "com.apptolast.organization.domain.ApiCredential*",
        "com.apptolast.organization.application.ApiCredential*",
        "com.apptolast.organization.application.CreateApiCredential*",
        "com.apptolast.organization.application.ReadApiCredentials*",
        "com.apptolast.organization.application.RevokeApiCredential*",
        "com.apptolast.organization.application.AuthenticateApiCredential*",
        "com.apptolast.organization.application.ConsumeApiQuota*",
        "com.apptolast.organization.application.ApiQuotaAdmission*",
        "com.apptolast.organization.application.ApiRateLimitedException*",
        "com.apptolast.organization.application.ApiUnauthenticatedException*",
        "com.apptolast.organization.adapter.persistence.PostgresApiCredentialStore*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration*"
    )
    val icsCalendarClasses = setOf(
        "com.apptolast.organization.domain.IcsCalendar*",
        "com.apptolast.organization.domain.CalendarWindow*",
        "com.apptolast.organization.domain.CalendarFeedSecret*",
        "com.apptolast.organization.domain.CalendarEntry*",
        "com.apptolast.organization.domain.CalendarSnapshot*",
        "com.apptolast.organization.application.ManageCalendarFeed*",
        "com.apptolast.organization.application.RenderCalendar*",
        "com.apptolast.organization.application.CalendarFeedAddress*",
        "com.apptolast.organization.adapter.http.CalendarFeedController*",
        "com.apptolast.organization.adapter.http.PublicCalendarController*",
        "com.apptolast.organization.adapter.http.CalendarDocuments*",
        "com.apptolast.organization.adapter.http.CalendarProblems*",
        "com.apptolast.organization.adapter.http.CalendarPaths*",
        "com.apptolast.organization.adapter.persistence.PostgresCalendarStore*",
        "com.apptolast.organization.adapter.persistence.SnapshotRenderCalendar*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val integrationApiHttpClasses = setOf(
        "com.apptolast.organization.adapter.http.ApiCredentialController*",
        "com.apptolast.organization.adapter.http.ApiCredentialSessionIdResolver*",
        "com.apptolast.organization.adapter.http.ApiCredentialBearerFilter*",
        "com.apptolast.organization.adapter.http.IntegrationOpenApiController*",
        "com.apptolast.organization.adapter.config.SecurityConfiguration*"
    )
    val importReaderClasses = setOf(
        "com.apptolast.organization.adapter.persistence.ImportJsonReader*",
        "com.apptolast.organization.adapter.persistence.ImportReceiptDecoder*"
    )
    val importReaderTests = setOf(
        "com.apptolast.organization.adapter.persistence.ImportJsonReaderTest",
        "com.apptolast.organization.adapter.persistence.ImportReceiptDecoderTest"
    )
    val importHttpClasses = setOf(
        "com.apptolast.organization.adapter.http.ImportDataController*",
        "com.apptolast.organization.application.PreviewImportData*",
        "com.apptolast.organization.application.ApplyImportData*",
        "com.apptolast.organization.application.ReadImportReceipt*"
    )
    val importHttpTests = setOf(
        "com.apptolast.organization.adapter.ImportDataApiTest",
        "com.apptolast.organization.application.PreviewImportDataTest",
        "com.apptolast.organization.application.ApplyImportDataTest",
        "com.apptolast.organization.application.ReadImportReceiptTest"
    )
    val importPersistenceClasses = setOf(
        "com.apptolast.organization.adapter.persistence.PostgresImportDataStore*",
        "com.apptolast.organization.adapter.persistence.ImportRecordValidator*",
        "com.apptolast.organization.adapter.persistence.ImportCustomizationValidator*",
        "com.apptolast.organization.application.ImportCounts*"
    )
    val importPersistenceTests = setOf(
        "com.apptolast.organization.adapter.persistence.ImportPersistenceTest",
        "com.apptolast.organization.adapter.persistence.ImportCustomizationValidatorTest",
        "com.apptolast.organization.adapter.persistence.ImportConcurrencyTest",
        "com.apptolast.organization.adapter.config.ImportScaleTest",
        "com.apptolast.organization.adapter.config.ImportWiringTest"
    )
    val importAdapterTests = setOf(
        "com.apptolast.organization.adapter.Import*Test",
        "com.apptolast.organization.adapter.persistence.Import*Test",
        "com.apptolast.organization.adapter.config.Import*Test"
    )
    val authenticationClasses = setOf(
        "com.apptolast.organization.adapter.http.SessionController",
        "com.apptolast.organization.adapter.http.SessionAccessDeniedHandler",
        "com.apptolast.organization.adapter.http.SessionFailureFilter",
        "com.apptolast.organization.adapter.config.SessionCookiePolicy"
    )
    val authenticationTests = setOf(
        "com.apptolast.organization.adapter.http.Session*Test",
        "com.apptolast.organization.adapter.config.SessionCookiePolicyTest"
    )
    val taskAdapters = setOf(
        "com.apptolast.organization.adapter.http.TaskController",
        "com.apptolast.organization.adapter.persistence.PostgresTaskCommit",
        "com.apptolast.organization.adapter.persistence.PostgresTaskQueries",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher"
    )
    val taskClasses = taskAdapters + setOf(
        "com.apptolast.organization.domain.Task",
        "com.apptolast.organization.domain.TaskPage",
        "com.apptolast.organization.domain.TaskPosition",
        "com.apptolast.organization.domain.OutboxMessage",
        "com.apptolast.organization.application.CreateTask",
        "com.apptolast.organization.application.ReadTasks",
        "com.apptolast.organization.application.TaskCreated"
    )
    val taskAdapterTests = setOf(
        "com.apptolast.organization.adapter.TaskApiTest",
        "com.apptolast.organization.adapter.SubtaskApiTest",
        "com.apptolast.organization.adapter.broker.*Test"
    )
    val splitClasses = taskClasses + setOf(
        "com.apptolast.organization.application.CreateSubtask",
        "com.apptolast.organization.application.ReadSubtasks",
        "com.apptolast.organization.application.SubtaskCreated"
    )
    val taskStatusAdapters = setOf(
        "com.apptolast.organization.adapter.http.ApiErrors",
        "com.apptolast.organization.adapter.http.TaskStatusController",
        "com.apptolast.organization.adapter.http.TaskHistoryController",
        "com.apptolast.organization.adapter.persistence.PostgresTaskStatusStore",
        "com.apptolast.organization.adapter.persistence.PostgresTaskHistoryQueries"
    )
    val taskStatusClasses = taskStatusAdapters + setOf(
        "com.apptolast.organization.domain.Task",
        "com.apptolast.organization.domain.TaskSnapshot",
        "com.apptolast.organization.domain.TaskRevision",
        "com.apptolast.organization.domain.TaskHistoryEntry",
        "com.apptolast.organization.domain.TaskHistoryPosition",
        "com.apptolast.organization.domain.TaskHistoryPage",
        "com.apptolast.organization.domain.OutboxMessage",
        "com.apptolast.organization.application.ChangeTaskStatus",
        "com.apptolast.organization.application.ReadTaskStatus",
        "com.apptolast.organization.application.ReadTaskHistory",
        "com.apptolast.organization.application.TaskStatusChange",
        "com.apptolast.organization.application.TaskStatusChanged",
        "com.apptolast.organization.adapter.persistence.PostgresTaskQueries",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher"
    )
    val taskStatusAdapterTests = setOf(
        "com.apptolast.organization.adapter.TaskStatusApiTest",
        "com.apptolast.organization.adapter.TaskHistoryApiTest",
        "com.apptolast.organization.adapter.ProjectApiTest",
        "com.apptolast.organization.adapter.ReadProjectsApiTest",
        "com.apptolast.organization.adapter.EditProjectsApiTest",
        "com.apptolast.organization.adapter.ProjectStatesApiTest"
    )
    val availabilityAdapters = setOf(
        "com.apptolast.organization.adapter.http.AvailabilityController*",
        "com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore",
        "com.apptolast.organization.adapter.config.JavaTimeZoneCatalog",
        "com.apptolast.organization.adapter.http.ApiErrors"
    )
    val availabilityClasses = availabilityAdapters + setOf(
        "com.apptolast.organization.domain.Availability",
        "com.apptolast.organization.domain.AvailabilityRevision",
        "com.apptolast.organization.application.SaveAvailability",
        "com.apptolast.organization.application.ReadAvailability"
    )
    val availabilityTests = setOf(
        "com.apptolast.organization.domain.Availability*Test",
        "com.apptolast.organization.application.*AvailabilityTest",
        "com.apptolast.organization.adapter.AvailabilityApiTest",
        "com.apptolast.organization.adapter.config.JavaTimeZoneCatalogTest"
    )
    val taskTests = core + taskAdapterTests
    val scheduleBlockAdapters = setOf(
        "com.apptolast.organization.adapter.http.BlockController*",
        "com.apptolast.organization.adapter.persistence.PostgresBlockStore"
    )
    val scheduleBlockClasses = scheduleBlockAdapters + setOf(
        "com.apptolast.organization.adapter.config.ApplicationConfiguration",
        "com.apptolast.organization.domain.Block*",
        "com.apptolast.organization.domain.BudgetDay",
        "com.apptolast.organization.domain.PlannedBlock",
        "com.apptolast.organization.domain.ResolvedBlockTime",
        "com.apptolast.organization.application.Block*",
        "com.apptolast.organization.application.PlanBlock",
        "com.apptolast.organization.application.ReadBlocks",
        "com.apptolast.organization.domain.OutboxMessage",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher"
    )
    val scheduleBlockTests = setOf(
        "com.apptolast.organization.adapter.config.ProjectStateConfigurationTest",
        "com.apptolast.organization.adapter.config.ApplicationWiringTest",
        "com.apptolast.organization.domain.Block*Test",
        "com.apptolast.organization.domain.PlannedBlockTest",
        "com.apptolast.organization.domain.ResolvedBlockTimeTest",
        "com.apptolast.organization.application.PlanBlockTest",
        "com.apptolast.organization.application.PublishOutboxTest",
        "com.apptolast.organization.adapter.ScheduleBlockApiTest",
        "com.apptolast.organization.adapter.persistence.ScheduleBlockPersistenceTest",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisherTest",
        "com.apptolast.organization.adapter.broker.RabbitBrokerFailuresTest",
        "com.apptolast.organization.adapter.persistence.OutboxWorkTest",
        "com.apptolast.organization.adapter.persistence.OutboxRecoveryTest"
    )
    val todayAdapters = setOf(
        "com.apptolast.organization.adapter.http.TodayController",
        "com.apptolast.organization.adapter.persistence.PostgresTodayQueries"
    )
    val todayClasses = todayAdapters + setOf(
        "com.apptolast.organization.domain.TodayWindow*",
        "com.apptolast.organization.domain.TodayItem",
        "com.apptolast.organization.application.ReadToday",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val todayTests = setOf(
        "com.apptolast.organization.domain.TodayWindowTest",
        "com.apptolast.organization.adapter.TodayApiTest",
        "com.apptolast.organization.adapter.config.ApplicationWiringTest",
        "com.apptolast.organization.adapter.config.ProjectStateConfigurationTest"
    )
    // Feature13 includes the changed shared creation, publication and read paths.
    val rescheduleClasses = scheduleBlockClasses + setOf(
        "com.apptolast.organization.application.MoveBlock*",
        "com.apptolast.organization.application.MoveContext",
        "com.apptolast.organization.application.CancelBlock*",
        "com.apptolast.organization.application.ReadBlockChanges*",
        "com.apptolast.organization.application.PublishOutbox",
        "com.apptolast.organization.adapter.http.RescheduleController*",
        "com.apptolast.organization.adapter.http.BlockChangesController*",
        "com.apptolast.organization.adapter.http.BlockCursor*",
        "com.apptolast.organization.adapter.http.ApiErrors",
        "com.apptolast.organization.adapter.persistence.PostgresBlockChangeQueries",
        "com.apptolast.organization.adapter.persistence.PostgresTodayQueries"
    )
    // Keep all JUnit candidates: PIT chooses tests using measured coverage.
    val rescheduleTests = setOf("com.apptolast.organization.*")
    val startWorkSessionClasses = setOf(
        "com.apptolast.organization.domain.SessionStart",
        "com.apptolast.organization.domain.OutboxMessage",
        "com.apptolast.organization.application.StartWorkSession*",
        "com.apptolast.organization.application.ReadWorkSessions*",
        "com.apptolast.organization.application.WorkSession*",
        "com.apptolast.organization.application.PublishOutbox",
        "com.apptolast.organization.adapter.persistence.PostgresWorkSessionStore*",
        "com.apptolast.organization.adapter.http.WorkSessionController*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher"
    )
    val pauseResumeSessionClasses = startWorkSessionClasses + setOf(
        "com.apptolast.organization.domain.WorkSession*",
        "com.apptolast.organization.application.ChangeWorkSession*",
        "com.apptolast.organization.application.ReadWorkSessionState*",
        "com.apptolast.organization.application.ReadWorkSessionChanges*",
        "com.apptolast.organization.adapter.http.WorkSessionStateController*"
    )
    val closeWorkSessionClasses = setOf(
        "com.apptolast.organization.domain.WorkSessionCloseNotes",
        "com.apptolast.organization.domain.WorkSessionState",
        "com.apptolast.organization.domain.OutboxMessage",
        "com.apptolast.organization.application.ChangeWorkSession",
        "com.apptolast.organization.application.ReadWorkSessionState",
        "com.apptolast.organization.application.ReadWorkSessionChanges",
        "com.apptolast.organization.application.WorkSessionTransitionReceipt",
        "com.apptolast.organization.application.WorkSessionTransition",
        "com.apptolast.organization.application.WorkSessionChanging",
        "com.apptolast.organization.application.WorkSessionClosed",
        "com.apptolast.organization.application.WorkSessionClosure",
        "com.apptolast.organization.adapter.persistence.PostgresWorkSessionStore*",
        "com.apptolast.organization.adapter.http.WorkSessionStateController*",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val endTimeNotificationClasses = setOf(
        "com.apptolast.organization.application.ExtendWorkSession",
        "com.apptolast.organization.application.ReadWorkSessionEnd",
        "com.apptolast.organization.application.WorkSessionEnd",
        "com.apptolast.organization.application.WorkSessionEndSnapshot",
        "com.apptolast.organization.application.WorkSessionExtension",
        "com.apptolast.organization.application.WorkSessionExtensionTransition",
        "com.apptolast.organization.application.WorkSessionExtended",
        "com.apptolast.organization.application.WorkSessionTransitionReceipt",
        "com.apptolast.organization.application.WorkSessionTransition",
        "com.apptolast.organization.application.WorkSessionChanging",
        "com.apptolast.organization.application.ChangeWorkSession",
        "com.apptolast.organization.application.ReadWorkSessionState",
        "com.apptolast.organization.application.ReadWorkSessionChanges",
        "com.apptolast.organization.domain.WorkSessionState",
        "com.apptolast.organization.domain.OutboxMessage",
        "com.apptolast.organization.adapter.persistence.PostgresWorkSessionStore*",
        "com.apptolast.organization.adapter.http.WorkSessionStateController*",
        "com.apptolast.organization.adapter.broker.RabbitBrokerPublisher",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val exportPersistenceClasses = setOf(
        "com.apptolast.organization.adapter.persistence.PostgresExportDataQueries*",
        "com.apptolast.organization.adapter.persistence.ExportJsonWriter*",
        "com.apptolast.organization.adapter.persistence.ExportBuffer*",
        "com.apptolast.organization.application.PrepareExportData*",
        "com.apptolast.organization.application.ExportDataUseCase*",
        "com.apptolast.organization.application.ExportDataQueries*",
        "com.apptolast.organization.application.PreparedExport*",
        "com.apptolast.organization.application.ExportTooLargeException*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration*"
    )
    val externalCalendarClasses = setOf(
        "com.apptolast.organization.domain.IcsFeed*",
        "com.apptolast.organization.domain.ExternalCalendarInput*",
        "com.apptolast.organization.domain.ExternalCalendarSnapshot*",
        "com.apptolast.organization.domain.ExternalCalendarSubscription*",
        "com.apptolast.organization.domain.ExternalEvent*",
        "com.apptolast.organization.domain.ExternalEventsRange*",
        "com.apptolast.organization.domain.SyncSummary*",
        "com.apptolast.organization.application.SyncExternalCalendar*",
        "com.apptolast.organization.application.SaveExternalCalendar*",
        "com.apptolast.organization.application.ReadExternalCalendar*",
        "com.apptolast.organization.application.ReadExternalCalendarEvents*",
        "com.apptolast.organization.application.DeleteExternalCalendar*",
        "com.apptolast.organization.application.OutboundHostGuard*",
        "com.apptolast.organization.application.PublicAddressPolicy*",
        "com.apptolast.organization.application.AddressPolicy*",
        "com.apptolast.organization.application.ExternalEventsView*",
        // El cifrado vive en adapter.connectors desde que las features 27 y 28 unificaron
        // SecretCipher: el paquete adapter.crypto ya no existe y este ambito lo seguia
        // nombrando, de modo que AES-256-GCM no recibia ni un mutante y la campana lo bendecia.
        "com.apptolast.organization.adapter.connectors.AesGcmSecretCipher*",
        "com.apptolast.organization.adapter.connectors.ConnectorKeyRing*",
        "com.apptolast.organization.adapter.feed.HttpCalendarFeed*",
        "com.apptolast.organization.adapter.net.SystemHostResolver*",
        "com.apptolast.organization.adapter.http.ExternalCalendarController*",
        "com.apptolast.organization.adapter.http.ConnectorsGate*",
        "com.apptolast.organization.adapter.persistence.PostgresExternalCalendarStore*",
        "com.apptolast.organization.adapter.logging.Slf4jExternalCalendarAudit*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration*"
    )
    val exportHttpClasses = setOf(
        "com.apptolast.organization.adapter.http.ExportDataController*",
        "com.apptolast.organization.adapter.http.ExportHeadersFilter*",
        "com.apptolast.organization.adapter.persistence.ExportReceiptWriter*"
    )
    val exportAdapterTests = setOf(
        "com.apptolast.organization.application.*Export*Test",
        "com.apptolast.organization.adapter.Export*Test",
        "com.apptolast.organization.adapter.http.Export*Test",
        "com.apptolast.organization.adapter.persistence.Export*Test",
        "com.apptolast.organization.adapter.config.Export*Test"
    )
    val customizationClasses = setOf(
        "com.apptolast.organization.domain.Customization*",
        "com.apptolast.organization.domain.CustomField*",
        "com.apptolast.organization.application.ReadCustomization*",
        "com.apptolast.organization.application.SaveCustomization*",
        "com.apptolast.organization.application.CreateCustomField*",
        "com.apptolast.organization.application.UpdateCustomField*",
        "com.apptolast.organization.application.ReadCustomFieldValues*",
        "com.apptolast.organization.application.SaveCustomFieldValues*",
        "com.apptolast.organization.application.Customization*",
        "com.apptolast.organization.application.CustomFieldValues*",
        "com.apptolast.organization.adapter.persistence.PostgresCustomizationStore*",
        "com.apptolast.organization.adapter.http.CustomizationController*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val customizationAdapterTests = setOf(
        "com.apptolast.organization.adapter.CustomizationApiTest",
        "com.apptolast.organization.adapter.persistence.Customization*Test",
        "com.apptolast.organization.adapter.persistence.CustomField*Test",
        "com.apptolast.organization.adapter.config.CustomizationWiringTest",
        "com.apptolast.organization.adapter.config.ApplicationWiringTest"
    )
    val appearanceClasses = setOf(
        "com.apptolast.organization.application.ReadAppearance",
        "com.apptolast.organization.application.ReadAppearanceUseCase",
        "com.apptolast.organization.application.SaveAppearance",
        "com.apptolast.organization.application.SaveAppearanceUseCase",
        "com.apptolast.organization.application.AppearanceQueries",
        "com.apptolast.organization.application.AppearanceEditing",
        "com.apptolast.organization.application.AppearanceConflictException",
        "com.apptolast.organization.domain.Appearance*",
        "com.apptolast.organization.adapter.persistence.PostgresAppearanceStore*",
        "com.apptolast.organization.adapter.http.AppearanceController*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val appearanceAdapterTests = setOf(
        "com.apptolast.organization.adapter.AppearanceApiTest",
        "com.apptolast.organization.adapter.persistence.Appearance*Test",
        "com.apptolast.organization.adapter.config.ApplicationWiringTest"
    )
    val weeklyReviewClasses = setOf(
        "com.apptolast.organization.application.ReadWeeklyReview",
        "com.apptolast.organization.application.ReadWeeklyReviewUseCase",
        "com.apptolast.organization.application.WeeklyReviewQueries",
        "com.apptolast.organization.domain.WeeklyReview*",
        "com.apptolast.organization.adapter.persistence.PostgresWeeklyReviewQueries*",
        "com.apptolast.organization.adapter.http.WeeklyReviewController*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val historyClasses = setOf(
        "com.apptolast.organization.application.ReadHistory",
        "com.apptolast.organization.application.ReadHistoryUseCase",
        "com.apptolast.organization.application.HistoryQueries",
        "com.apptolast.organization.application.HistoryEntry",
        "com.apptolast.organization.application.HistoryFilters",
        "com.apptolast.organization.application.HistoryCursor",
        "com.apptolast.organization.application.HistoryPosition",
        "com.apptolast.organization.application.HistoryPage",
        "com.apptolast.organization.adapter.persistence.PostgresHistoryQueries*",
        "com.apptolast.organization.adapter.http.HistoryController*",
        "com.apptolast.organization.adapter.http.HistoryCursorCodec*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    val weeklyReviewAdapterTests = setOf(
        "com.apptolast.organization.adapter.WeeklyReviewApiTest",
        "com.apptolast.organization.adapter.persistence.WeeklyReview*Test",
        "com.apptolast.organization.adapter.config.ApplicationWiringTest"
    )
    val historyAdapterTests = setOf(
        "com.apptolast.organization.adapter.HistoryApiTest",
        "com.apptolast.organization.adapter.persistence.History*Test"
    )
    val automationsClasses = setOf(
        "com.apptolast.organization.domain.Automation*",
        "com.apptolast.organization.domain.CreateTaskAction*",
        "com.apptolast.organization.domain.NotifyWebhookAction*",
        "com.apptolast.organization.domain.EventProject*",
        "com.apptolast.organization.domain.EventTask*",
        "com.apptolast.organization.domain.TemplateValues*",
        "com.apptolast.organization.domain.UnknownEventTypeException*",
        "com.apptolast.organization.application.Automation*",
        "com.apptolast.organization.application.CreateAutomation*",
        "com.apptolast.organization.application.ReadAutomation*",
        "com.apptolast.organization.application.ReplaceAutomation*",
        "com.apptolast.organization.application.DeleteAutomation*",
        "com.apptolast.organization.application.SimulateAutomation*",
        "com.apptolast.organization.application.WebhookEndpoint*",
        "com.apptolast.organization.adapter.http.Automation*",
        // Los seis manejadores de error de la feature viven en ApiErrors, que quedaba fuera del
        // ambito: sus mutantes no se generaban y la campana daba por cubierto un codigo que nadie
        // habia tocado. Mismo criterio que taskStatusAdapters y availabilityAdapters.
        "com.apptolast.organization.adapter.http.ApiErrors",
        "com.apptolast.organization.adapter.persistence.PostgresAutomation*",
        "com.apptolast.organization.adapter.persistence.AutomationActionJson*",
        "com.apptolast.organization.adapter.config.ApplicationConfiguration"
    )
    // Feature 25. Deliberadamente NO incluye AddressPolicy ni PublicAddressPolicy
    // (compartidas, ya en externalCalendarClasses), ni WebhookEndpointLookup,
    // WebhookEndpointNotFoundException y NotifyWebhookAction (feature 30, ya en
    // automationsClasses), ni ApplicationConfiguration o SecurityConfiguration
    // (cableado compartido, ya mutado por otros ambitos): la logica de arranque
    // propia del carril vive en WebhookConnectorStartup, y esa si entra.
    val webhooksClasses = setOf(
        "com.apptolast.organization.domain.RetrySchedule*",
        "com.apptolast.organization.domain.WebhookAttempt*",
        "com.apptolast.organization.domain.WebhookCursor*",
        "com.apptolast.organization.domain.WebhookDelivery*",
        "com.apptolast.organization.domain.WebhookEndpoint*",
        "com.apptolast.organization.domain.WebhookIntent*",
        "com.apptolast.organization.domain.WebhookInvalidException*",
        "com.apptolast.organization.domain.WebhookPingPayload*",
        "com.apptolast.organization.application.ClaimedDelivery*",
        "com.apptolast.organization.application.CreateWebhook*",
        "com.apptolast.organization.application.DispatchWebhooks*",
        "com.apptolast.organization.application.EnqueueWebhookDeliveries*",
        "com.apptolast.organization.application.ManageWebhook*",
        "com.apptolast.organization.application.OutboxCandidate*",
        "com.apptolast.organization.application.ReadyEndpoint*",
        "com.apptolast.organization.application.WebhookAudit*",
        "com.apptolast.organization.application.WebhookCreation*",
        "com.apptolast.organization.application.WebhookDeliveries*",
        "com.apptolast.organization.application.WebhookDestinationGuard*",
        "com.apptolast.organization.application.WebhookEndpoints*",
        "com.apptolast.organization.application.WebhookOperationException*",
        "com.apptolast.organization.application.WebhookOutbox*",
        "com.apptolast.organization.application.WebhookSecrets*",
        "com.apptolast.organization.application.WebhookSender*",
        "com.apptolast.organization.application.WebhookWork*",
        "com.apptolast.organization.adapter.webhook.JdkWebhookSender*",
        "com.apptolast.organization.adapter.webhook.AesGcmWebhookSecrets*",
        "com.apptolast.organization.adapter.webhook.WebhookSignature*",
        "com.apptolast.organization.adapter.http.WebhookController*",
        "com.apptolast.organization.adapter.http.WebhookDeliveryView*",
        "com.apptolast.organization.adapter.http.WebhookEndpointView*",
        "com.apptolast.organization.adapter.persistence.PostgresWebhookStore*",
        "com.apptolast.organization.adapter.persistence.PostgresWebhookOutbox*",
        "com.apptolast.organization.adapter.persistence.PostgresWebhookWork*",
        "com.apptolast.organization.adapter.logging.Slf4jWebhookAudit*",
        "com.apptolast.organization.adapter.config.WebhookConfiguration*",
        "com.apptolast.organization.adapter.config.WebhookConnectorStartup*",
        "com.apptolast.organization.adapter.config.WebhookSchedule*"
    )
    targetClasses.set(when {
        webhooksOnly -> webhooksClasses
        automationsOnly -> automationsClasses
        icsCalendarOnly -> icsCalendarClasses
        githubConnectorOnly -> githubConnectorClasses
        additionalConnectorsOnly -> additionalConnectorsClasses
        integrationApiOnly -> integrationApiClasses
        integrationApiHttpOnly -> integrationApiHttpClasses
        importReaderOnly -> importReaderClasses
        importHttpOnly -> importHttpClasses
        importPersistenceOnly -> importPersistenceClasses
        exportPersistenceOnly -> exportPersistenceClasses
        externalCalendarOnly -> externalCalendarClasses
        customizationOnly -> customizationClasses
        appearanceOnly -> appearanceClasses
        weeklyReviewOnly -> weeklyReviewClasses
        historyOnly -> historyClasses
        endTimeNotificationOnly -> endTimeNotificationClasses
        closeWorkSessionOnly -> closeWorkSessionClasses
        pauseResumeSessionOnly -> pauseResumeSessionClasses
        startWorkSessionReplayOnly -> setOf("com.apptolast.organization.application.WorkSessionStarted")
        startWorkSessionOnly -> startWorkSessionClasses
        rescheduleOnly -> rescheduleClasses
        todayOnly -> todayClasses
        scheduleBlockOnly -> scheduleBlockClasses
        availabilityOnly -> availabilityClasses
        authenticationOnly -> authenticationClasses
        taskStatusOnly -> taskStatusClasses
        splitOnly -> splitClasses
        taskOnly -> taskClasses
        else -> core + authenticationClasses + taskAdapters + taskStatusAdapters + availabilityAdapters + scheduleBlockAdapters + todayAdapters + rescheduleClasses + startWorkSessionClasses + pauseResumeSessionClasses + closeWorkSessionClasses + endTimeNotificationClasses + historyClasses + weeklyReviewClasses + appearanceClasses + customizationClasses + exportPersistenceClasses + exportHttpClasses + importReaderClasses + importHttpClasses + importPersistenceClasses + integrationApiClasses + integrationApiHttpClasses + icsCalendarClasses + githubConnectorClasses + externalCalendarClasses + automationsClasses + webhooksClasses
    })
    targetTests.set(when {
        webhooksOnly -> setOf("com.apptolast.organization.*")
        automationsOnly -> setOf("com.apptolast.organization.*")
        icsCalendarOnly -> setOf("com.apptolast.organization.*")
        githubConnectorOnly -> setOf("com.apptolast.organization.*")
        additionalConnectorsOnly -> setOf("com.apptolast.organization.*")
        integrationApiOnly || integrationApiHttpOnly -> setOf("com.apptolast.organization.*")
        importReaderOnly -> importReaderTests
        importHttpOnly -> importHttpTests
        importPersistenceOnly -> importPersistenceTests
        exportPersistenceOnly -> setOf("com.apptolast.organization.*")
        externalCalendarOnly -> setOf("com.apptolast.organization.*")
        customizationOnly -> setOf("com.apptolast.organization.*")
        appearanceOnly -> setOf("com.apptolast.organization.*")
        weeklyReviewOnly -> setOf("com.apptolast.organization.*")
        historyOnly -> setOf("com.apptolast.organization.*")
        endTimeNotificationOnly -> setOf("com.apptolast.organization.*")
        closeWorkSessionOnly -> setOf("com.apptolast.organization.*")
        pauseResumeSessionOnly -> setOf("com.apptolast.organization.*")
        startWorkSessionReplayOnly -> setOf("com.apptolast.organization.*")
        startWorkSessionOnly -> setOf("com.apptolast.organization.*")
        rescheduleOnly -> rescheduleTests
        todayOnly -> todayTests
        scheduleBlockOnly -> scheduleBlockTests
        availabilityOnly -> availabilityTests + taskStatusAdapterTests + taskAdapterTests.filter { !it.contains("broker") }
        authenticationOnly -> authenticationTests
        taskStatusOnly -> taskTests + taskStatusAdapterTests
        splitOnly -> taskTests
        taskOnly -> taskTests
        else -> core + authenticationTests + taskAdapterTests + taskStatusAdapterTests + availabilityTests + scheduleBlockTests + todayTests + rescheduleTests + historyAdapterTests + weeklyReviewAdapterTests + appearanceAdapterTests + customizationAdapterTests + exportAdapterTests + importAdapterTests
    })
    if (webhooksOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-webhooks"))
    if (icsCalendarOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-ics-calendar"))
    if (githubConnectorOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-github-connector"))
    if (integrationApiOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-integration-api"))
    if (integrationApiHttpOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-integration-api-http"))
    if (automationsOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-automations"))
    if (importReaderOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-import-data-reader"))
    if (importHttpOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-import-data-http"))
    if (importPersistenceOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-import-data-persistence"))
    if (exportPersistenceOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-export-data-persistence"))
    if (externalCalendarOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-external-calendar"))
    if (customizationOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-custom-views-fields"))
    if (appearanceOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-appearance"))
    if (weeklyReviewOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-weekly-review"))
    if (historyOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-history"))
    if (endTimeNotificationOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-end-time-notification"))
    if (pauseResumeSessionOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-pause-resume-session"))
    if (closeWorkSessionOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-close-work-session"))
    if (rescheduleOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-reschedule"))
    if (startWorkSessionOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-start-work-session"))
    if (startWorkSessionReplayOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-start-work-session-replay"))
    if (todayOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-today"))
    if (availabilityOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-availability"))
    if (scheduleBlockOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-schedule-block"))
    if (taskStatusOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-complete-reopen-task"))
    if (authenticationOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-authentication"))
    if (splitOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-split-task"))
    if (taskOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-create-task"))
    jvmArgs.set(setOf("-Dapi.version=1.44"))
    jvmArgs.add(providers.provider {
        "-Doutbox.test.classpath=${sourceSets.test.get().runtimeClasspath.asPath}"
    })
    // Real broker tests restart a container for each JUnit lifecycle in PIT.
    // Keep their transport assertions intact while allowing measured container startup.
    if (!authenticationOnly) timeoutConstInMillis.set(15000)
    // PIT's default FRECORD also removes hand-written compact constructors.
    features.set(setOf("-FRECORD"))
    excludedMethods.set(setOf("equals", "hashCode", "toString"))
    mutationThreshold.set(80)
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    threads.set(if (integrationApiOnly || integrationApiHttpOnly) 8 else 4)
}

spotless { java { googleJavaFormat("1.31.0") } }
