/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.*

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

val saveCoreVersion = the<LibrariesForLibs>()
    .versions
    .save
    .core
    .get()

dependencies {
    findProperty("saveCliPath")?.let { saveCliPathProperty ->
        val saveCliPath = saveCliPathProperty as String
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
            src { saveCliPath }
            dest { "$buildDir/download/${File(saveCliPath).name}" }

            overwrite(false)
        }
        add("runtimeOnly",
            files(layout.buildDirectory.dir("$buildDir/download")).apply {
                builtBy(downloadSaveCliTaskProvider)
            }
        )
    }
}

val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    val saveCliVersion = findProperty("saveCliVersion") ?: saveCoreVersion
    // description = "Reads version of save-cli, either from project property, or from Versions, or latest"
    inputs.property("save-cli version", saveCliVersion)
    outputs.file(versionsFile)

    doFirst {
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
