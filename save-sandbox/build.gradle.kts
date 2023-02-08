import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    id("com.saveourtool.save.buildutils.save-cli-configuration")
    id("com.saveourtool.save.buildutils.save-agent-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("org.gradle.test-retry") version "1.5.1"
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
}

kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.withType<Test> {
    retry {
        // There once were flaky tests in sandbox, but it seems like they became stable.
        // Settings can be restored or removed, as required.
        failOnPassedAfterRetry.set(false)
        maxFailures.set(5)
        maxRetries.set(1)
    }
}

dependencies {
    implementation(projects.saveOrchestratorCommon)
    implementation(libs.zip4j)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation(libs.hibernate.jpa21.api)
    implementation(libs.save.plugins.warn.jvm)
    testImplementation(projects.testUtils)
    testImplementation(libs.fabric8.kubernetes.server.mock)
    implementation(projects.authenticationService)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
    implementation(libs.save.common.jvm)
}
