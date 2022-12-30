/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File
plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveAgentDistroTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveAgentDistro") {
    enabled = findProperty("saveAgentDistroFilepath") != null

    val saveAgentDistroFilepath = findProperty("saveAgentDistroFilepath")?.toString() ?: "file:\\\\not-found"
    src { saveAgentDistroFilepath }
    dest { "$buildDir/agentDistro/${File(saveAgentDistroFilepath).name}" }

    overwrite(false)
}

dependencies {
    if (!DefaultNativePlatform.getCurrentOperatingSystem().isLinux) {
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
