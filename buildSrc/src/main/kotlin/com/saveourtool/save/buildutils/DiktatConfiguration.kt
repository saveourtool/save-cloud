/**
 * Configuration for diktat static analysis
 */

package com.saveourtool.save.buildutils

import com.saveourtool.diktat.plugin.gradle.DiktatExtension
import com.saveourtool.diktat.plugin.gradle.DiktatGradlePlugin
import com.saveourtool.diktat.plugin.gradle.DiktatJavaExecTaskBase
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

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

private fun Project.fixDiktatTasks() {
    tasks.withType<DiktatJavaExecTaskBase>().configureEach {
        javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
        })
        // https://github.com/saveourtool/diktat/issues/1269
        systemProperty("user.home", rootDir.toString())
    }
}
