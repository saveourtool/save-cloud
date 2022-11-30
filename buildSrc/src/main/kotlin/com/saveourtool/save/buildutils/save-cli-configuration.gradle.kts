/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.*
plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
    dependsOn(":getSaveCliVersion")

    src { getSaveCliPath() }
    dest { "$buildDir/download/${File(getSaveCliPath()).name}" }

    overwrite(false)
}

dependencies {
    add("runtimeOnly",
        files(layout.buildDirectory.dir("$buildDir/download")).apply {
            builtBy(downloadSaveCliTaskProvider)
        }
    )
}

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in gradle/plugins
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)
    inputs.property("project version", version.toString())
    outputs.file(versionsFile)

    doFirst {
        val saveCliVersion = readSaveCliVersion()
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "$saveCliVersion"
            internal const val SAVE_CLOUD_VERSION = "$version"

            """.trimIndent()
        )
    }
}

kotlin.sourceSets.getByName("main") {
    kotlin.srcDir("$buildDir/generated/src")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
