/**
 * Configuration for project versioning
 */

package com.saveourtool.save.buildutils

import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import java.util.*

/**
 * @return path to the file with save-cli version for current build
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
internal val Project.pathToSaveCliVersion get() = "${rootProject.buildDir}/save-cli.properties"

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
 * @return save-cli version for current build
 */
@Suppress("CUSTOM_GETTERS_SETTERS")
internal fun Project.readSaveCliVersion(): Provider<String> = rootProject.tasks.named("getSaveCliVersion")
    .map { getSaveCliVersionTask ->
        val file = file(getSaveCliVersionTask.outputs.files.singleFile)
        Properties().apply { load(file.reader()) }["version"] as String
    }

/**
 * @return true if this string denotes a snapshot version
 */
internal fun String.isSnapshot() = endsWith("SNAPSHOT")

/**
 * Image reference must be in the form '[domainHost:port/][path/]name[:tag][@digest]', with 'path' and 'name' containing
 * only [a-z0-9][.][_][-].
 * FixMe: temporarily copy-pasted in here and in gradle/plugins
 *
 * @return correctly formatted version
 */
internal fun Project.versionForDockerImages(): String =
        (project.findProperty("build.dockerTag") as String? ?: version.toString())
            .replace(Regex("[^._\\-a-zA-Z0-9]"), "-")
