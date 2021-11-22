/**
 * Configuration for project versioning
 */

package org.cqfn.save.buildutils

import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import java.net.URL

/**
 * Configures reckon plugin for [this] project, should be applied for root project only
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()
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
}

/**
 * Docker tags cannot contain `+`, so we change it.
 *
 * @return correctly formatted version
 */
fun Project.versionForDockerImages() = version.toString().replace("+", "-")

/**
 * @return version of save-cli, either from project property, or from Versions, or latest
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
fun Project.getSaveCliVersion(): String {
    val libs = the<LibrariesForLibs>()
    val saveCoreVersion = libs.versions.save.core.get()
    return if (saveCoreVersion.endsWith("SNAPSHOT")) {
        // try to get the required version of cli
        findProperty("saveCliVersion") as String? ?: run {
            // as fallback, use latest release to allow the project to build successfully
            val latestRelease = ResourceGroovyMethods.getText(
                URL("https://api.github.com/repos/diktat-static-analysis/save/releases/latest")
            )
            (groovy.json.JsonSlurper().parseText(latestRelease) as Map<String, Any>)["tag_name"].let {
                (it as String).trim('v')
            }
        }
    } else {
        saveCoreVersion
    }
}
