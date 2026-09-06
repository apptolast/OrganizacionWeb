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
        "com.apptolast.organization.adapter.broker.*Test"
    )
    val taskTests = core + taskAdapterTests
    targetClasses.set(when {
        authenticationOnly -> authenticationClasses
        taskOnly -> taskClasses
        else -> core + authenticationClasses + taskAdapters
    })
    targetTests.set(when {
        authenticationOnly -> authenticationTests
        taskOnly -> taskTests
        else -> core + authenticationTests + taskAdapterTests
    })
    if (authenticationOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-authentication"))
    if (taskOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-create-task"))
    jvmArgs.set(setOf("-Dapi.version=1.44"))
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
