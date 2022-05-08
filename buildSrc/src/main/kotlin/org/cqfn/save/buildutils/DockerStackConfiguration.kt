/**
 * Configuration for docker swarm deployment
 */

package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

const val MYSQL_STARTUP_DELAY_MILLIS = 30_000L

/**
 * @param profile deployment profile, used, for example, to start SQL database in dev profile only
 */
@OptIn(ExperimentalStdlibApi::class)
@Suppress("TOO_LONG_FUNCTION", "TOO_MANY_LINES_IN_LAMBDA")
fun Project.createStackDeployTask(profile: String) {
    tasks.register("generateComposeFile") {
        description = "Set project version in docker-compose file"
        val templateFile = "$rootDir/docker-compose.yaml"
        val composeFile = "$buildDir/docker-compose.yaml"
        val envFile = "$buildDir/.env"
        inputs.file(templateFile)
        inputs.property("project version", version.toString())
        inputs.property("profile", profile)
        outputs.file(composeFile)
        doFirst {
            val newText = file(templateFile).readLines()
                .joinToString(System.lineSeparator()) {
                    if (profile == "dev" && it.startsWith("services:")) {
                        // `docker stack deploy` doesn't recognise `profiles` option in compose file for some reason, with docker 20.10.5, compose file 3.9
                        // so we create it here only in dev profile
                        """|$it
                           |  mysql:
                           |    image: mysql:8.0.28-oracle
                           |    container_name: mysql
                           |    ports:
                           |      - "3306:3306"
                           |    environment:
                           |      - "MYSQL_ROOT_PASSWORD=123"
                           |      - "MYSQL_DATABASE=save_cloud"
                        """.trimMargin()
                    } else if (profile == "dev" && it.trim().startsWith("logging:")) {
                        ""
                    } else {
                        it
                    }
                }
            file(composeFile)
                .apply { createNewFile() }
                .writeText(newText)
        }

        doLast {
            // https://docs.docker.com/compose/environment-variables/#the-env-file
            file(envFile).writeText(
                """
                    TAG=${versionForDockerImages()}
                    PROFILE=$profile
                """.trimIndent()
            )
        }
    }

    tasks.register<Exec>("deployDockerStack") {
        dependsOn("liquibaseUpdate")
        dependsOn("generateComposeFile")
        subprojects {
            // in case bootBuildImage tasks are also requested, they should run before deployment task
            tasks.withType<BootBuildImage>().configureEach {
                this@register.shouldRunAfter(this)
            }
        }

        val configsDir = Paths.get("${System.getProperty("user.home")}/configs")
        val useOverride = (properties.getOrDefault("useOverride", "true") as String).toBoolean()
        val composeOverride = File("$configsDir/docker-compose.override.yaml")
        if (useOverride && !composeOverride.exists()) {
            logger.warn("`useOverride` option is set to true, but can't use override configuration, because ${composeOverride.canonicalPath} doesn't exist")
        } else {
            logger.info("Using override configuration from ${composeOverride.canonicalPath}")
        }
        doFirst {
            copy {
                description = "Copy configuration files from repo to actual locations"
                from("save-deploy")
                into(configsDir)
            }
            // create directories for optional property files
            Files.createDirectories(configsDir.resolve("backend"))
            Files.createDirectories(configsDir.resolve("gateway"))
            Files.createDirectories(configsDir.resolve("orchestrator"))
            Files.createDirectories(configsDir.resolve("preprocessor"))
        }
        description = "Deploy to docker swarm. If swarm contains more than one node, some registry for built images is required."
        // this command puts env variables into compose file
        val composeCmd = "docker-compose -f ${rootProject.buildDir}/docker-compose.yaml --env-file ${rootProject.buildDir}/.env config"
        val stackCmd = "docker stack deploy --compose-file -" +
            if (useOverride && composeOverride.exists()) {
                " --compose-file ${composeOverride.canonicalPath}"
            } else {
                ""
            } +
                " save"
        commandLine("bash", "-c", "$composeCmd | $stackCmd")
    }

    tasks.register("buildAndDeployDockerStack") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("deployDockerStack")
    }

    tasks.register<Exec>("stopDockerStack") {
        description = "Completely stop all services in docker swarm. NOT NEEDED FOR REDEPLOYING! Use only to explicitly stop everything."
        commandLine("docker", "stack", "rm", "save")
    }

    // in case you are running it on MAC, first do the following: docker pull --platform linux/x86_64 mysql
    tasks.register<Exec>("startMysqlDb") {
        dependsOn("generateComposeFile")
        println("Running the follwoing command: [docker-compose --file $buildDir/docker-compose.yaml up -d mysql]")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", "mysql")
        errorOutput = ByteArrayOutputStream()
        doLast {
            if (!errorOutput.toString().contains(" is up-to-date")) {
                logger.lifecycle("Waiting $MYSQL_STARTUP_DELAY_MILLIS millis for mysql to start")
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

    tasks.register<Exec>("buildAndDeployComponent") {
        description = "Build and deploy a single component of save-cloud. Component name should be provided via `-Psave.component=<name> " +
                "and it should be a name of one of gradle subprojects."
        val componentName = findProperty("save.component") as String?
        requireNotNull(componentName) { "Component name should be provided for `deployComponent` task" }
        require(componentName in allprojects.map { it.name }) { "Component name should be one of gradle subproject names" }
        val buildTask = project(componentName).tasks.named<BootBuildImage>("bootBuildImage")
        dependsOn(buildTask)
        val serviceName = when (componentName) {
            "save-backend", "save-orchestrator", "save-preprocessor" -> "save_${componentName.substringAfter("save-")}"
            "api-gateway" -> "save_gateway"
            else -> error("Wrong component name $componentName")
        }
        commandLine("docker", "service", "update", "--image", buildTask.get().imageName, serviceName)
    }
}
