/**
 * Configuration for project versioning
 */

package com.saveourtool.save.buildutils

import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import java.net.URL
import java.time.Duration
import java.util.Properties

/**
 * @return path to the file with save-cli version for current build
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
val Project.pathToSaveCliVersion get() = "${rootProject.buildDir}/save-cli.properties"

/**
 * Configures reckon plugin for [this] project, should be applied for root project only
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()
    apply<GrgitServicePlugin>()
    val grgitProvider = project.extensions.getByType<GrgitServiceExtension>()
        .service
        .map { it.grgit }

    // should be provided in the gradle.properties
    val isDevelopmentVersion = hasProperty("save.profile") && property("save.profile") == "dev"
    configure<ReckonExtension> {
        scopeFromProp()
        if (isDevelopmentVersion) {
            // this should be used during local development most of the time, so that constantly changing version
            // on a dirty git tree doesn't cause other task updates
            snapshotFromProp()
        } else {
            stageFromProp("alpha", "rc", "final")
        }
    }

    val grgit = grgitProvider.get()
    val status = grgit.repository.jgit.status()
        .call()
    if (!status.isClean) {
        logger.warn("git tree is not clean; " +
                "Untracked files: ${status.untracked}, uncommitted changes: ${status.uncommittedChanges}"
        )
    }
}

/**
 * Docker tags cannot contain `+`, so we change it.
 *
 * @return correctly formatted version
 */
fun Project.versionForDockerImages() = version.toString().replace("+", "-")

/**
 * Register task that reads version of save-cli, either from project property, or from Versions, or latest
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION", "MAGIC_NUMBER")
fun Project.registerSaveCliVersionCheckTask() {
    val libs = the<LibrariesForLibs>()
    val saveCoreVersion = libs.versions.save.core.get()
    tasks.register("getSaveCliVersion") {
        val file = file(pathToSaveCliVersion)
        outputs.file(file)
        outputs.upToDateWhen {
            // cache value of latest save-cli version for 10 minutes to keep request rate to Github reasonable
            (System.currentTimeMillis() - file.lastModified()) < Duration.ofMinutes(10).toMillis()
        }
        doFirst {
            val version = if (saveCoreVersion.endsWith("SNAPSHOT")) {
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
}

/**
 * @return version of save-cli from properties file
 */
fun Project.readSaveCliVersion(): String {
    val file = file(pathToSaveCliVersion)
    return Properties().apply { load(file.reader()) }["version"] as String
}

/**
 * @return save-cli file path to copy
 */
fun Project.getSaveCliPath(): String {
    val saveCliVersion = readSaveCliVersion()
    val saveCliPath = findProperty("saveCliPath") as String? ?: "https://github.com/saveourtool/save-cli/releases/download/v$saveCliVersion"
    return "$saveCliPath/save-$saveCliVersion-linuxX64.kexe"
}
