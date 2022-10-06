import com.saveourtool.save.buildutils.*

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    alias(libs.plugins.download)
    id("org.gradle.test-retry") version "1.4.1"
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
}

kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
    }
}

configureJacoco()
configureSpotless()

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
    //implementation(libs.spring.boot.starter.security)
    //implementation(libs.spring.security.core)
}

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider: TaskProvider<Task> = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)
    outputs.file(versionsFile)

    doFirst {
        val saveCliVersion = readSaveCliVersion()
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "$saveCliVersion"

            """.trimIndent()
        )
    }
}
kotlin.sourceSets.getByName("main") {
    kotlin.srcDir("$buildDir/generated/src")
}
tasks.withType<KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
