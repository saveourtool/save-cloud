/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.io.File

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
        val target = "$buildDir/save-cli"
        val saveCliPath = providers.gradleProperty("saveCliPath")
        logger.info(
            "save-cli version is SNAPSHOT ({}), add {} as a runtime dependency",
            saveCliVersion, saveCliPath
        )
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        val copySaveCliTaskProvider: TaskProvider<Copy> = tasks.register<Copy>("copySaveCli") {
            from(saveCliPath)
            into(target) {
                duplicatesStrategy = DuplicatesStrategy.WARN
            }
            destinationDir = file(target)
        }
        add("runtimeOnly",
            files(layout.buildDirectory.dir(target)).apply {
                builtBy(copySaveCliTaskProvider)
            }
        )
    }
}

val generateSaveCliVersionFileTaskProvider: TaskProvider<Task> = tasks.register("generateSaveCliVersionFile") {
    val outputDir = File("$buildDir/generated/src")
    val versionFile = outputDir.resolve("generated/SaveCliVersion.kt")

    val saveCliVersion = findProperty("saveCliVersion") ?: saveCliVersion
    // description = "Reads version of save-cli, either from project property, or from Versions, or latest"
    inputs.property("save-cli version", saveCliVersion)
    outputs.dir(outputDir)

    doFirst {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            package generated

            internal const val SAVE_CLI_VERSION = "$saveCliVersion"

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
