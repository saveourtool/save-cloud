/**
 * Configuration for docker swarm deployment
 */

package com.saveourtool.save.buildutils

import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

const val MYSQL_STARTUP_DELAY_MILLIS = 30_000L
const val KAFKA_STARTUP_DELAY_MILLIS = 5_000L

/**
 * @param profile deployment profile, used, for example, to start SQL database in dev profile only
 */
@OptIn(ExperimentalStdlibApi::class)
@Suppress(
    "TOO_LONG_FUNCTION",
    "TOO_MANY_LINES_IN_LAMBDA",
    "AVOID_NULL_CHECKS",
    "GENERIC_VARIABLE_WRONG_DECLARATION"
)
fun Project.createStackDeployTask(profile: String) {
    tasks.register("generateComposeFile") {
        description = "Set project version in docker-compose file"
        val templateFile = "$rootDir/docker-compose.yaml"
        val composeFile = file("$buildDir/docker-compose.yaml")
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
                           |    command: ["--log_bin_trust_function_creators=1"]
                           |  zookeeper:
                           |    image: confluentinc/cp-zookeeper:latest
                           |    environment:
                           |      ZOOKEEPER_CLIENT_PORT: 2181
                           |      ZOOKEEPER_TICK_TIME: 2000
                           |    ports:
                           |      - 22181:2181
                           |  
                           |  kafka:
                           |    image: confluentinc/cp-kafka:latest
                           |    depends_on:
                           |      - zookeeper
                           |    ports:
                           |      - 29092:29092
                           |    environment:
                           |      KAFKA_BROKER_ID: 1
                           |      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
                           |      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
                           |      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
                           |      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
                           |      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
                           |  
                           |${declareDexService().prependIndent("  ")}
                           """.trimMargin()
                    } else if (profile == "dev" && it.trim().startsWith("logging:")) {
                        ""
                    } else {
                        it
                    }
                }
            composeFile
                .apply { createNewFile() }
                .writeText(newText)
        }

        doLast {
            val defaultVersionOrProperty: (propertyName: String) -> String = { propertyName ->
                // Image tag can be specified explicitly for a particular service,
                // or specified explicitly for the whole app,
                // or be inferred based on the project.version
                findProperty(propertyName) as String?
                    ?: findProperty("dockerTag") as String?
                    ?: versionForDockerImages()
            }
            val defaultDockerNetworkOrProperty =
                    // When deploying to Docker Swarm, the network name cannot be determined automatically.
                    // The network name is assigned on swarm creation and hence should be passed as a property to Gradle.
                    // In Docker Compose mode, however, network name defaults to project directoy name (unless
                    // overridden explicitly).
                    findProperty("dockerNetwork") as String?
                    // https://docs.docker.com/compose/networking/
                        ?: "${composeFile.parentFile.name}_default"
            // https://docs.docker.com/compose/environment-variables/#the-env-file
            file(envFile).writeText(
                """
                    BACKEND_TAG=${defaultVersionOrProperty("backend.dockerTag")}
                    FRONTEND_TAG=${defaultVersionOrProperty("frontend.dockerTag")}
                    GATEWAY_TAG=${defaultVersionOrProperty("gateway.dockerTag")}
                    ORCHESTRATOR_TAG=${defaultVersionOrProperty("orchestrator.dockerTag")}
                    SANDBOX_TAG=${defaultVersionOrProperty("sandbox.dockerTag")}
                    PREPROCESSOR_TAG=${defaultVersionOrProperty("preprocessor.dockerTag")}
                    PROFILE=$profile
                    DOCKER_NETWORK_NAME=$defaultDockerNetworkOrProperty
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
            Files.createDirectories(configsDir.resolve("sandbox"))
            Files.createDirectories(configsDir.resolve("preprocessor"))
        }
        description =
                "Deploy to docker swarm. If swarm contains more than one node, some registry for built images is required."
        // this command puts env variables into compose file
        val composeCmd =
                "docker-compose -f ${rootProject.buildDir}/docker-compose.yaml --env-file ${rootProject.buildDir}/.env config"
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
        description =
                "Completely stop all services in docker swarm. NOT NEEDED FOR REDEPLOYING! Use only to explicitly stop everything."
        commandLine("docker", "stack", "rm", "save")
    }

    // in case you are running it on MAC, first do the following: docker pull --platform linux/x86_64 mysql
    tasks.register<Exec>("startMysqlDbService") {
        dependsOn("generateComposeFile")
        doFirst {
            logger.lifecycle("Running the following command: [docker-compose --file $buildDir/docker-compose.yaml up -d mysql]")
        }
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", "mysql")
        errorOutput = ByteArrayOutputStream()
        doLast {
            logger.lifecycle("Waiting $MYSQL_STARTUP_DELAY_MILLIS millis for mysql to start")
            Thread.sleep(MYSQL_STARTUP_DELAY_MILLIS)  // wait for mysql to start, can be manually increased when needed
        }
    }
    tasks.named("liquibaseUpdate") {
        mustRunAfter("startMysqlDbService")
    }
    tasks.register("startMysqlDb") {
        dependsOn("liquibaseUpdate")
        dependsOn("startMysqlDbService")
    }

    tasks.register<Exec>("startKafka") {
        dependsOn("generateComposeFile")
        doFirst {
            logger.lifecycle("Running the following command: [docker-compose --file $buildDir/docker-compose.yaml up -d kafka]")
        }
        errorOutput = ByteArrayOutputStream()
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "up", "-d", "kafka")
        doLast {
            logger.lifecycle("Waiting $KAFKA_STARTUP_DELAY_MILLIS millis for kafka to start")
            Thread.sleep(KAFKA_STARTUP_DELAY_MILLIS)  // wait for kafka to start, can be manually increased when needed
        }
    }

    tasks.register<Exec>("restartMysqlDb") {
        dependsOn("generateComposeFile")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "mysql")
        finalizedBy("startMysqlDb")
    }

    tasks.register<Exec>("restartKafka") {
        dependsOn("generateComposeFile")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "kafka")
        commandLine("docker-compose", "--file", "$buildDir/docker-compose.yaml", "rm", "--force", "zookeeper")
        finalizedBy("startKafka")
    }

    tasks.register<Exec>("deployLocal") {
        dependsOn(subprojects.flatMap { it.tasks.withType<BootBuildImage>() })
        dependsOn("startMysqlDb")
        commandLine(
            "docker-compose",
            "--file",
            "$buildDir/docker-compose.yaml",
            "up",
            "-d",
            "orchestrator",
            "sandbox",
            "backend",
            "frontend",
            "preprocessor"
        )
    }

    val componentName = findProperty("save.component") as String?
    if (componentName != null) {
        tasks.register<Exec>("buildAndDeployComponent") {
            description =
                    "Build and deploy a single component of save-cloud. Component name should be provided via `-Psave.component=<name> " +
                            "and it should be a name of one of gradle subprojects. If component name is `save-backend`, then `save-frontend` will be built too" +
                            " and bundled into save-backend image."
            require(componentName in allprojects.map { it.name }) { "Component name should be one of gradle subproject names, but was [$componentName]" }
            val buildTask: TaskProvider<BootBuildImage> =
                    project(componentName).tasks.named<BootBuildImage>("bootBuildImage")
            dependsOn(buildTask)
            val serviceName = when (componentName) {
                "save-backend", "save-frontend", "save-orchestrator", "save-sandbox", "save-preprocessor" -> "save_${componentName.substringAfter("save-")}"
                "api-gateway" -> "save_gateway"
                else -> error("Wrong component name $componentName")
            }
            commandLine("docker", "service", "update", "--image", buildTask.get().imageName, serviceName)
        }
    }
}

private fun Project.declareDexService() =
        """
            |dex:
            |  image: ghcr.io/dexidp/dex:latest-distroless
            |  ports:
            |    - "5556:5556"
            |  volumes:
            |    - $rootDir/save-deploy/dex.dev.yaml:/etc/dex/config.docker.yaml
        """.trimMargin()
