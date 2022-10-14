import com.saveourtool.save.buildutils.*

import de.undercouch.gradle.tasks.download.Download
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
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


@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveAgentDistroTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveAgentDistro") {
    enabled = findProperty("saveAgentDistroFilepath") != null

    src(KotlinClosure0(function = { findProperty("saveAgentDistroFilepath") ?: "file:\\\\" }))
    dest("$buildDir/agentDistro")
    outputs.dir("$buildDir/agentDistro")
    overwrite(false)
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
    dependsOn(":getSaveCliVersion")
    inputs.file(pathToSaveCliVersion)

    src(KotlinClosure0(function = { getSaveCliPath() }))
    dest("$buildDir/download")
    outputs.dir("$buildDir/download")
    overwrite(false)
}

dependencies {
    implementation(projects.saveOrchestratorCommon)
    runtimeOnly(
        files(layout.buildDirectory.dir("$buildDir/download")).apply {
            builtBy(downloadSaveCliTaskProvider)
        }
    )
    if (!DefaultNativePlatform.getCurrentOperatingSystem().isLinux) {
        logger.warn("Dependency `save-agent` is omitted on Windows and Mac because of problems with linking in cross-compilation." +
                " Task `:save-agent:copyAgentDistribution` would fail without correct libcurl.so. If your changes are about " +
                "save-agent, please test them on Linux " +
                "or put the file with name like `save-agent-*-distribution.jar` built on Linux into libs subfolder."
        )
        runtimeOnly(files("$buildDir/agentDistro").apply {
            builtBy(downloadSaveAgentDistroTaskProvider)
        })
    } else {
        runtimeOnly(project(":save-agent", "distribution"))
    }
    implementation(libs.zip4j)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation(libs.hibernate.jpa21.api)
    implementation(libs.save.plugins.warn.jvm)
    testImplementation(projects.testUtils)
    testImplementation(libs.fabric8.kubernetes.server.mock)
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
