import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.save-demo-agent-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.kotlin.plugin.serialization)
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

val diktatVersion: String = libs.versions.diktat.get()

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)

    implementation(libs.spring.cloud.starter.kubernetes.client.config)

    api(libs.ktor.client.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.fabric8.kubernetes.client)
}
