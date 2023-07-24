import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    id("com.saveourtool.save.buildutils.save-cli-configuration")
    id("com.saveourtool.save.buildutils.save-agent-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
}

kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    implementation(projects.saveOrchestratorCommon)
    implementation(libs.zip4j)
    implementation(libs.spring.cloud.starter.kubernetes.fabric8.config)
    implementation(libs.hibernate.jpa21.api)
    implementation(libs.save.plugins.warn.jvm)
    implementation(projects.authenticationService)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
    implementation(libs.save.common.jvm)
    testImplementation(projects.testUtils)
    testImplementation(libs.fabric8.kubernetes.server.mock)
}
