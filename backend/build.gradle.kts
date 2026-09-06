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
    val authenticationOnly = providers.gradleProperty("mutationScope").orNull == "authentication"
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
    targetClasses.set(if (authenticationOnly) authenticationClasses else core + authenticationClasses)
    targetTests.set(if (authenticationOnly) authenticationTests else core + authenticationTests)
    if (authenticationOnly) reportDir.set(layout.buildDirectory.dir("reports/pitest-authentication"))
    // PIT's default FRECORD also removes hand-written compact constructors.
    features.set(setOf("-FRECORD"))
    excludedMethods.set(setOf("equals", "hashCode", "toString"))
    mutationThreshold.set(80)
    outputFormats.set(setOf("HTML", "XML"))
    timestampedReports.set(false)
    threads.set(4)
}

spotless { java { googleJavaFormat("1.31.0") } }
