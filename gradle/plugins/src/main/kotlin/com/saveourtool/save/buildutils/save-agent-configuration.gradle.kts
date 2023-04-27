/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.TaskProvider
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

val downloadSaveAgentDistroTaskProvider: TaskProvider<Download> = tasks.register(
    name = "downloadSaveAgentDistro",
    configuration = downloadTaskConfiguration(
        urlPropertyName = "saveAgentDistroFilepath",
        targetDirectory = "agentDistro",
    ),
)

dependencies {
    if (System.getenv("SKIP_SAVE_AGENT_DEPENDENCY") != null) {
        logger.info("Dependency `save-agent` is omitted on CI")
    } else if (!DefaultNativePlatform.getCurrentOperatingSystem().isLinux) {
        logger.warn("Dependency `save-agent` is omitted on Windows and Mac because of problems with linking in cross-compilation." +
                " Task `:save-agent:copyAgentDistribution` would fail without correct libcurl.so. If your changes are about " +
                "save-agent, please test them on Linux " +
                "or put the file with name like `save-agent-*-distribution.jar` built on Linux into libs subfolder."
        )
        addProvider("runtimeOnly", downloadSaveAgentDistroTaskProvider.map { it.outputs.files })
    } else {
        add("runtimeOnly", project(":save-agent", "distribution"))
    }
}
