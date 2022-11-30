/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.io.File
import java.util.*
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.gradle.accessors.dm.LibrariesForLibs
import java.net.URL
import java.time.Duration

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

/**
 * @return path to the file with save-cli version for current build
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
val Project.pathToSaveCliVersion get() = "${rootProject.buildDir}/save-cli.properties"

/**
 * @return version of save-cli from properties file
 */
fun Project.readSaveCliVersion(): String {
    val file = file(pathToSaveCliVersion)
    return Properties().apply { load(file.reader()) }["version"] as String
}

// FixMe: temporarily copy-pasted
fun String.isSnapshot() = endsWith("SNAPSHOT")

/**
 * @return save-cli file path to copy
 */
fun Project.getSaveCliPath(): String {
    val saveCliVersion = readSaveCliVersion()
    val saveCliPath = findProperty("saveCliPath")?.takeIf { saveCliVersion.isSnapshot() } as String?
        ?: "https://github.com/saveourtool/save-cli/releases/download/v$saveCliVersion"
    return "$saveCliPath/save-$saveCliVersion-linuxX64.kexe"
}

val libs = the<LibrariesForLibs>()
val saveCoreVersion = libs.versions.save.core.get()
tasks.register("getSaveCliVersion") {
//    description = "Reads version of save-cli, either from project property, or from Versions, or latest"
    inputs.property("save-cli version", findProperty("saveCliVersion") ?: saveCoreVersion)
    val file = file(pathToSaveCliVersion)
    outputs.file(file)
    outputs.upToDateWhen {
        // cache value of latest save-cli version for 10 minutes to keep request rate to Github reasonable
        (System.currentTimeMillis() - file.lastModified()) < Duration.ofMinutes(10).toMillis()
    }
    doFirst {
        val version = if (saveCoreVersion.isSnapshot()) {
            // try to get the required version of cli
            findProperty("saveCliVersion") as String? ?: run {
                // as fallback, use latest release to allow the project to build successfully
                val latestRelease = ResourceGroovyMethods.getText(
                    URL("https://api.github.com/repos/saveourtool/save-cli/releases/latest")
                )
                (groovy.json.JsonSlurper().parseText(latestRelease) as Map<String, Any>)["tag_name"].let {
                    (it as String).trim('v')
                }
            }
        } else {
            saveCoreVersion
        }
        file.writeText("""version=$version""")
    }
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
