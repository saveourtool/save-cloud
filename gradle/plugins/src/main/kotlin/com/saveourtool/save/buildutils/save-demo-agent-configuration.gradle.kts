/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.nio.file.Path

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveDemoAgentDistroTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveDemoAgentDistro") {
    enabled = findProperty("saveDemoAgentDistroFilepath") != null

    val saveDemoAgentDistroFilepath = findProperty("saveDemoAgentDistroFilepath")?.toString() ?: "file://not-found"
    src { saveDemoAgentDistroFilepath }
    dest { "$buildDir/demoAgentDistro/${Path.of(saveDemoAgentDistroFilepath).fileName}" }
    overwrite(false)
}

dependencies {
    if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        logger.warn("Dependency `save-demo-agent` is omitted on Windows because of problems with cross-compilation.")
        addProvider("runtimeOnly", downloadSaveDemoAgentDistroTaskProvider.map { it.outputs.files })
    } else {
        add("runtimeOnly", project(":save-demo-agent", "distribution"))
    }
}
