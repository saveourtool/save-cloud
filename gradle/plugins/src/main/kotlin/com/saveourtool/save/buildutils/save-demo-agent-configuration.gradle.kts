/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

val downloadSaveDemoAgentDistroTaskProvider: TaskProvider<Download> = tasks.register(
    name = "downloadSaveDemoAgentDistro",
    configuration = downloadTaskConfiguration(
        urlPropertyName = "saveDemoAgentDistroFileUrl",
        targetDirectory = "demoAgentDistro",
    ),
)

dependencies {
    if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        logger.warn("Dependency `save-demo-agent` is omitted on Windows because of problems with cross-compilation.")
        addProvider("runtimeOnly", downloadSaveDemoAgentDistroTaskProvider.map { it.outputs.files })
    } else {
        add("runtimeOnly", project(":save-demo-agent", "distribution"))
    }
}
