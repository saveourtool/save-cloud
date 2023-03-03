/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.kotlin.dsl.*
import java.io.File

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

val copySaveCliTaskProvider = tasks.register<Copy>("copySaveCli") {
    val saveCliFile = rootProject.tasks.named<Download>("downloadSaveCli")
        .map { downloadTask ->
            downloadTask.dest
        }
    val outputDir = "$buildDir/download"
    inputs.file(saveCliFile)
    outputs.dir(outputDir)

    from(saveCliFile.map { it.parentFile })
    into(outputDir)
}

dependencies {
    add("runtimeOnly",
        files(layout.buildDirectory.dir(
            copySaveCliTaskProvider.map { task ->
                task.outputs.files.singleFile.absolutePath
            }
        ))
    )
}

val generateSaveCliVersionFileTaskProvider = tasks.register("generateSaveCliVersionFile") {
    val saveCliVersion = readSaveCliVersion()
    val outputDir = File("$buildDir/generated/src")
    val versionFile = outputDir.resolve("generated/SaveCliVersion.kt")

    inputs.property("save-cli version", saveCliVersion)
    outputs.dir(outputDir)

    doFirst {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "${saveCliVersion.get()}"

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
