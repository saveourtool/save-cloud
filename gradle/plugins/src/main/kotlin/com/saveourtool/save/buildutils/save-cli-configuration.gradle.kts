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

/**
 * @return version of save-cli from properties file
 */
fun Project.readSaveCliVersion(): String {
    val file = file(pathToSaveCliVersion)
    return Properties().apply { load(file.reader()) }["version"] as String
}

dependencies {
    findProperty("saveCliPath")?.let { saveCliPathProperty ->
        val saveCliPath = saveCliPathProperty as String
        val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
            dependsOn(":getSaveCliVersion")

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
    kotlin.srcDir(generateVersionFileTaskProvider.map { _ -> "$buildDir/generated/src" })
}
