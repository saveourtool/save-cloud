/**
 * Utilities for spring-boot's `BootBuildImage` Gradle task and generally to configure paketo-buildpacks-based builds in Gradle.
 */

package com.saveourtool.save.buildutils

import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

@Suppress("CUSTOM_GETTERS_SETTERS")
internal val isRelease get() = System.getenv("GHCR_PWD") != null

/**
 * Sane default for task of type [BootBuildImage] for any module.
 * Sets image name and configures Docker Registries.
 */
fun BootBuildImage.commonConfigure() {
    imageName = "ghcr.io/saveourtool/${project.name}:${project.versionForDockerImages()}"
    isVerboseLogging = true
    docker {
        host = project.findProperty("build.docker.host") as? String?
        isTlsVerify = (project.findProperty("build.docker.tls-verify") as? String?)?.toBoolean() ?: false
        certPath = project.findProperty("build.docker.cert-path") as? String?
    }
    System.getenv("GHCR_PWD")?.let { registryPassword ->
        isPublish = true
        docker {
            publishRegistry {
                username = "saveourtool"
                password = registryPassword
                url = "https://ghcr.io"
            }
        }
    }
}
