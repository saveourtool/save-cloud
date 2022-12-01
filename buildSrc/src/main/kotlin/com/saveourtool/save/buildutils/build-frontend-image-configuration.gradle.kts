package com.saveourtool.save.buildutils

import org.gradle.kotlin.dsl.register
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

tasks.register<BootBuildImage>("buildImage") {
    inputs.property("project version", version.toString())
    inputs.file("$projectDir/nginx.conf")

    commonConfigure()

    // FixMe: task name is hardcoded here from `save-frontend/build.gradle.kts`
    archiveFile.set(tasks.named<Jar>("distributionJarTask").flatMap { it.archiveFile })
    buildpacks = listOf("paketo-buildpacks/nginx")
    environment = mapOf(
        "BP_WEB_SERVER_ROOT" to "static",
    )
}
