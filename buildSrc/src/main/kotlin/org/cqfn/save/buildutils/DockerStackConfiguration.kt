package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

fun Project.createStackDeployTask() {
    tasks.register<Exec>("startLocalDockerRegistry") {
        description = "Start local docker registry for spring boot images"
        commandLine("docker", "service", "create", "--name", "registry", "--publish", "published=6000,target=5000", "registry:2")
    }

    tasks.register<Exec>("deployDockerStack") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        doFirst {
            copy {
                description = "Copy configuration files from repo to actual locations"
                from("save-deploy")
                into("${System.getProperty("user.home")}/configs")
            }
        }
        description = "Deploy docker stack to docker swarm"
        commandLine("docker", "stack", "deploy", "--compose-file", "docker-compose.yaml", "save")
        doLast {
            exec {
                description = "Stop local docker registry"
                commandLine("docker", "service", "rm", "registry")
            }
        }
    }
}
