/**
 * Configuration for project versioning
 */

package com.saveourtool.save.buildutils

import org.ajoberstar.reckon.core.Scope
import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/**
 * Configures reckon plugin for [this] project, should be applied for root project only
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    // should be provided in the gradle.properties
    val isDevelopmentVersion = hasProperty("save.profile") && property("save.profile") == "dev"
    configure<ReckonExtension> {
        setDefaultInferredScope(Scope.MINOR.name)
        setScopeCalc(calcScopeFromProp())
        if (isDevelopmentVersion) {
            // this should be used during local development most of the time, so that constantly changing version
            // on a dirty git tree doesn't cause other task updates
            snapshots()
            setStageCalc(calcStageFromProp())
        } else {
            stages("alpha", "rc", "final")
            setStageCalc(calcStageFromProp())
        }
    }

    val status = FileRepositoryBuilder()
        .findGitDir(project.rootDir)
        .setup()
        .let(::FileRepository)
        .let(::Git)
        .status()
        .call()

    if (!status.isClean) {
        logger.warn("git tree is not clean; " +
                "Untracked files: ${status.untracked}, uncommitted changes: ${status.uncommittedChanges}"
        )
    }
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
