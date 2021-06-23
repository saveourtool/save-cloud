/**
 * Configuration for project versioning
 */

package org.cqfn.save.buildutils

import org.ajoberstar.reckon.gradle.ReckonExtension
import org.ajoberstar.reckon.gradle.ReckonPlugin
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import java.net.URL

/**
 * Configures reckon plugin for [this] project, should be applied for root project only
 */
fun Project.configureVersioning() {
    apply<ReckonPlugin>()

    configure<ReckonExtension> {
        scopeFromProp()
        stageFromProp("alpha", "final")  // use -Preckon.stage=final for release; otherwise version string will be based on commit hash
    }
}

/**
 * Docker tags cannot contain `+`, so we change it.
 */
fun Project.versionForDockerImages() = version.toString().replace("+", "-")

/**
 * @return version of save-cli, either from project property, or from Versions, or latest
 */
fun Project.getSaveCliVersion(): String = if (Versions.saveCore.endsWith("SNAPSHOT")) {
    // try to get the required version of cli
    findProperty("saveCliVersion") as String? ?: run {
        // as fallback, use latest release to allow the project to build successfully
        val latestRelease = ResourceGroovyMethods.getText(
            URL("https://api.github.com/repos/cqfn/save/releases/latest")
        )
        (groovy.json.JsonSlurper().parseText(latestRelease) as Map<String, Any>)["tag_name"].let {
            (it as String).trim('v')
        }
    }
} else {
    Versions.saveCore
}
