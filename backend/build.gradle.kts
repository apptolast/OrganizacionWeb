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
    val authenticationOnly = scope == "authentication"
    val taskOnly = scope == "create_task"
    val splitOnly = scope == "split_task"
    val taskStatusOnly = scope == "complete_reopen_task"
    val availabilityOnly = scope == "availability"
    val scheduleBlockOnly = scope == "schedule_block"
    val core = setOf("com.apptolast.organization.domain.*", "com.apptolast.organization.application.*")
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
    targetClasses.set(when {
        scheduleBlockOnly -> scheduleBlockClasses
        availabilityOnly -> availabilityClasses
        authenticationOnly -> authenticationClasses
        taskStatusOnly -> taskStatusClasses
        splitOnly -> splitClasses
        taskOnly -> taskClasses
        else -> core + authenticationClasses + taskAdapters + taskStatusAdapters + availabilityAdapters + scheduleBlockAdapters
    })
    targetTests.set(when {
        scheduleBlockOnly -> scheduleBlockTests
        availabilityOnly -> availabilityTests + taskStatusAdapterTests + taskAdapterTests.filter { !it.contains("broker") }
        authenticationOnly -> authenticationTests
        taskStatusOnly -> taskTests + taskStatusAdapterTests
        splitOnly -> taskTests
        taskOnly -> taskTests
        else -> core + authenticationTests + taskAdapterTests + taskStatusAdapterTests + availabilityTests + scheduleBlockTests
    })
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
    threads.set(4)
}

spotless { java { googleJavaFormat("1.31.0") } }
