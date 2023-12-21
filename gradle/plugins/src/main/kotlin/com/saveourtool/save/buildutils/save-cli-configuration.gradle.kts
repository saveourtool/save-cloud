/**
 * Configuration utilities for project which needs a runtime dependency to `save-cli`
 */

package com.saveourtool.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

plugins {
    kotlin("jvm")
}

val saveCliVersion: String = the<LibrariesForLibs>()
    .versions
    .save
    .cli
    .get()

dependencies {
    if (saveCliVersion.isSnapshot()) {
        addRuntimeDependency(
            "saveCliPath",
            "save-cli",
            "copySaveCli",
            "save-cli version is SNAPSHOT ($saveCliVersion)",
            this::add
        )
    }
}

val generateSaveCliVersionFileTaskProvider: TaskProvider<Task> = tasks.register("generateSaveCliVersionFile") {
    val outputDir = layout.buildDirectory.dir("generated/src")
    val versionFile = outputDir.map { it.file("generated/SaveCliVersion.kt") }

    val saveCliVersion = findProperty("saveCliVersion") ?: saveCliVersion
    // description = "Reads version of save-cli, either from project property, or from Versions, or latest"
    inputs.property("save-cli version", saveCliVersion)
    outputs.dir(outputDir)

    doFirst {
        versionFile.get().asFile
            .let { file ->
                file.parentFile.mkdirs()
                file.writeText(
                    """
                    package generated

                    internal const val SAVE_CLI_VERSION = "$saveCliVersion"

                    """.trimIndent()
                )
            }
    }
}

kotlin.sourceSets.getByName("main") {
    kotlin.srcDir(
        generateSaveCliVersionFileTaskProvider.map {
            it.outputs.files.singleFile
        }
    )
}
