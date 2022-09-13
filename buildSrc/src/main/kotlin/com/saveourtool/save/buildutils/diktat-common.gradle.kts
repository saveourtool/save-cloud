/**
 * Configuration for diktat static analysis
 */

package com.saveourtool.save.buildutils

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.cqfn.diktat.plugin.gradle.DiktatExtension
import org.cqfn.diktat.plugin.gradle.DiktatGradlePlugin
import org.cqfn.diktat.plugin.gradle.DiktatJavaExecTaskBase
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.*

plugins {
    id("org.cqfn.diktat.diktat-gradle-plugin")
}

diktat {
    diktatConfigFile = rootProject.file("diktat-analysis.yml")
    githubActions = findProperty("diktat.githubActions")?.toString()?.toBoolean() ?: false
    inputs {
        // using `Project#path` here, because it must be unique in gradle's project hierarchy
        if (path == rootProject.path) {
            include("buildSrc/src/**/*.kt", "*.kts", "buildSrc/**/*.kts")
            exclude("buildSrc/build/**")
        } else {
            include("src/**/*.kt", "**/*.kts")
            exclude("src/test/**/*.kt", "src/*Test/**/*.kt")
        }
    }
}

tasks.withType<DiktatJavaExecTaskBase>().configureEach {
    javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    })
}
