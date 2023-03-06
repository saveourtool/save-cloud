/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.io.File

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

val saveCoreVersion: String = the<LibrariesForLibs>()
    .versions
    .save
    .core
    .get()

dependencies {
    val isSaveCliProvided = hasProperty("saveCliPath")
    if (isSaveCliProvided) {
        val saveCliPath = providers.gradleProperty("saveCliPath")
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
            enabled = isSaveCliProvided
            src { saveCliPath }
            dest { saveCliPath.map { "$buildDir/download/${File(it).name}" } }

            overwrite(false)
        }
        add("runtimeOnly",
            files(layout.buildDirectory.dir("$buildDir/download")).apply {
                builtBy(downloadSaveCliTaskProvider)
            }
        )
    }
}

val generateSaveCliVersionFileTaskProvider: TaskProvider<Task> = tasks.register("generateSaveCliVersionFile") {
    val outputDir = File("$buildDir/generated/src")
    val versionFile = outputDir.resolve("generated/SaveCliVersion.kt")

    val saveCliVersion = findProperty("saveCliVersion") ?: saveCoreVersion
    // description = "Reads version of save-cli, either from project property, or from Versions, or latest"
    inputs.property("save-cli version", saveCliVersion)
    outputs.dir(outputDir)

    doFirst {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "$saveCliVersion"

            """.trimIndent()
        )
    }
}

kotlin.sourceSets.getByName("main") {
    kotlin.srcDir(
        generateSaveCliVersionFileTaskProvider.map {
            it.outputs.files.singleFile
        }
    )
}
