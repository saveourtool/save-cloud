/**
 * Configuration for project versioning
 */

package com.saveourtool.save.buildutils

import org.ajoberstar.grgit.gradle.GrgitServiceExtension
import org.ajoberstar.grgit.gradle.GrgitServicePlugin
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import java.util.Properties

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
 * Image reference must be in the form '[domainHost:port/][path/]name[:tag][@digest]', with 'path' and 'name' containing
 * only [a-z0-9][.][_][-].
 *
 * @return correctly formatted version
 */
fun Project.versionForDockerImages(): String =
        (project.findProperty("build.dockerTag") as String? ?: version.toString())
            .replace(Regex("[^._\\-a-zA-Z0-9]"), "-")

private fun String.isSnapshot() = endsWith("SNAPSHOT")
