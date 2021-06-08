/**
 * Configuration for docker swarm deployment
 */

package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.io.ByteArrayOutputStream

const val MYSQL_STARTUP_DELAY_MILLIS = 10_000L

/**
 * @param profile deployment profile, used, for example, to start SQL database in dev profile only
 */
@Suppress("TOO_LONG_FUNCTION", "TOO_MANY_LINES_IN_LAMBDA")
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
                    if (profile == "dev" && it.startsWith("services:")) {
                        // `docker stack deploy` doesn't recognise `profiles` option in compose file for some reason, with docker 20.10.5, compose file 3.9
                        // so we create it here only in dev profile
                        """|$it
                           |  mysql:
                           |    image: mysql:8.0.20
                           |    ports:
                           |      - "3306:3306"
                           |    environment:
                           |      - "MYSQL_ROOT_PASSWORD=123"
                           |      - "MYSQL_DATABASE=save_cloud"
                        """.trimMargin()
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
        // doLast {
        // exec {
        // description = "Stop local docker registry"
        // commandLine("docker", "service", "rm", "registry")
        // }
        // }
    }

    tasks.register<Exec>("stopDockerStack") {
        description = "Completely stop all services in docker swarm. NOT NEEDED FOR REDEPLOYING! Use only to explicitly stop everything."
        commandLine("docker", "stack", "rm", "save")
    }

    tasks.register<Exec>("startMysqlDb") {
        dependsOn("generateComposeFile")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", "mysql")
        errorOutput = ByteArrayOutputStream()
        doLast {
            if (!errorOutput.toString().contains(" is up-to-date")) {
                logger.info("Waiting $MYSQL_STARTUP_DELAY_MILLIS millis for mysql to start")
                Thread.sleep(MYSQL_STARTUP_DELAY_MILLIS)  // wait for mysql to start, can be manually increased when needed
            }
        }
        finalizedBy("liquibaseUpdate")
    }

    tasks.register<Exec>("restartMysqlDb") {
        dependsOn("generateComposeFile")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "mysql")
        finalizedBy("startMysqlDb")
    }

    tasks.register<Exec>("deployLocal") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("startMysqlDb")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", "orchestrator", "backend", "preprocessor")
    }
}
