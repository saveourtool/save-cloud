/**
 * Configuration for diktat static analysis
 */

package com.saveourtool.save.buildutils

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.diffplug.spotless.LineEnding
import org.cqfn.diktat.plugin.gradle.DiktatExtension
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin
import org.cqfn.diktat.plugin.gradle.DiktatJavaExecTaskBase
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*

/**
 * Applies diktat gradle plugin and configures diktat for [this] project
 */
fun Project.configureDiktat() {
    apply<DiktatGradlePlugin>()
    configure<DiktatExtension> {
        diktatConfigFile = rootProject.file("diktat-analysis.yml")
        githubActions = findProperty("diktat.githubActions")?.toString()?.toBoolean() ?: false
        inputs {
            // using `Project#path` here, because it must be unique in gradle's project hierarchy
            if (path == rootProject.path) {
                include("$rootDir/buildSrc/src/**/*.kt", "$rootDir/*.kts", "$rootDir/buildSrc/**/*.kts")
            } else {
                include("src/**/*.kt", "**/*.kts")
            }
        }
    }
    fixDiktatTasks()
}

fun Project.configureSpotless() {
    val libs = the<LibrariesForLibs>()
    val diktatVersion = libs.versions.diktat.get()
    apply<SpotlessPlugin>()
    configure<SpotlessExtension> {
        kotlin {
            diktat(diktatVersion).configFile(rootProject.file("diktat-analysis.yml"))
            target("src/**/*.kt")
        }
        kotlinGradle {
            diktat(diktatVersion).configFile(rootProject.file("diktat-analysis.yml"))

            // using `Project#path` here, because it must be unique in gradle's project hierarchy
            if (path == rootProject.path) {
                target("$rootDir/*.kts", "$rootDir/buildSrc/**/*.kts")
            } else {
                target("**/*.kts")
            }
        }
    }
}

private fun Project.fixDiktatTasks() {
    tasks.withType<DiktatJavaExecTaskBase>().configureEach {
        javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
        })
        // https://github.com/saveourtool/diktat/issues/1269
        systemProperty("user.home", rootDir.toString())
    }
}
