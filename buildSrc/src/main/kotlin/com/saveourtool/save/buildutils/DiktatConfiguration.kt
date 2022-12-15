/**
 * Configuration for diktat static analysis
 */

package com.saveourtool.save.buildutils

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*

/**
 * Applies spotless to [this] project and configures diktat step
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
fun Project.configureSpotless() {
    val libs = the<LibrariesForLibs>()
    val diktatVersion = libs.versions.diktat.get()
    apply<SpotlessPlugin>()
    configure<SpotlessExtension> {
        kotlin {
            diktat(diktatVersion).configFile(rootProject.file("diktat-analysis.yml"))
            target("src/**/*.kt")
            if (path == rootProject.path) {
                target("gradle/plugins/**/*.kt")
            }
        }
        kotlinGradle {
            diktat(diktatVersion).configFile(rootProject.file("diktat-analysis.yml"))

            // using `Project#path` here, because it must be unique in gradle's project hierarchy
            if (path == rootProject.path) {
                target("$rootDir/*.kts", "$rootDir/gradle/plugins/**/*.kts")
            } else {
                target("**/*.kts")
            }
        }
    }
}

