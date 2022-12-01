package com.saveourtool.save.buildutils

import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

/**
 * Sane default for task of type [BootBuildImage] for any module.
 * Sets image name and configures Docker Registries.
 */
fun BootBuildImage.commonConfigure() {
    imageName = "ghcr.io/saveourtool/${project.name}:${project.versionForDockerImages()}"
    isVerboseLogging = true
    docker {
        host = project.findProperty("build.docker.host") as? String?
        setTlsVerify((project.findProperty("build.docker.tls-verify") as? String?)?.toBoolean() ?: false)
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
