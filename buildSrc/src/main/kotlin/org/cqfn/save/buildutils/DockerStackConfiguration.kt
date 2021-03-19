package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.io.File
import java.io.ByteArrayOutputStream

fun Project.createStackDeployTask(profile: String) {
    tasks.register<Exec>("startLocalDockerRegistry") {
        enabled = false
        description = "Start local docker registry for spring boot images. Disabled, see comment in deployDockerStack task."
        commandLine("docker", "service", "create", "--name", "registry", "--publish", "published=6000,target=5000", "registry:2")
    }

    tasks.register("generateComposeFile") {
        description = "Set project version in docker-compose file"
        doFirst {
            val newText = file("$rootDir/docker-compose.yaml.template").readLines()
                .joinToString(System.lineSeparator()) {
                    if (profile != "dev" && it.contains("profiles:")) {
                        // `docker stack deploy` doesn't recognise `profiles` option in compose file for some reason, with docker 20.10.5, compose file 3.9
                        ""
                    } else {
                        it.replace("{{project.version}}", versionForDockerImages())
                            .replace("{{profile}}", profile)
                    }
                }
            file("$buildDir/docker-compose.yaml")
                .apply { createNewFile() }
                .writeText(newText)
        }
    }

    tasks.register<Exec>("deployDockerStack") {
        dependsOn("liquibaseUpdate")
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("generateComposeFile")
        doFirst {
            copy {
                description = "Copy configuration files from repo to actual locations"
                from("save-deploy")
                into("${System.getProperty("user.home")}/configs")
            }
        }
        description = "Deploy to docker swarm. If swarm contains more than one node, some registry for built images is requried."
        commandLine("docker", "stack", "deploy", "--compose-file", "$buildDir/docker-compose.yaml", "save")
//        doLast {
//            exec {
//                description = "Stop local docker registry"
//                commandLine("docker", "service", "rm", "registry")
//            }
//        }
    }

    tasks.register<Exec>("startMysqlDb") {
        dependsOn("generateComposeFile")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "--profile", "dev", "up", "-d", "mysql")
        errorOutput = ByteArrayOutputStream()
        if (!errorOutput.toString().contains(" is up-to-date")) {
            val waitIntervalMs = 10_000L
            logger.info("Waitnig $waitIntervalMs millis for mysql to start")
            Thread.sleep(waitIntervalMs)  // wait for mysql to start, can be manually increased when needed
        }
        finalizedBy("liquibaseUpdate")
    }

    tasks.register<Exec>("deployLocal") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("startMysqlDb")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", "orchestrator", "backend", "preprocessor")
    }
}
