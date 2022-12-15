import com.saveourtool.save.buildutils.*

plugins {
    id("com.saveourtool.save.buildutils.versioning-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.talaiot.base)
    alias(libs.plugins.liquibase.gradle)
    java
}

val profile = properties.getOrDefault("save.profile", "dev") as String

liquibase {
    activities {
        val commonArguments = mapOf(
            "logLevel" to "info",
            "contexts" to when (profile) {
                "prod" -> "prod"
                "dev" -> "dev"
                else -> throw GradleException("Profile $profile not configured to map on a particular liquibase context")
            }
        )
        // Configuring liquibase
        register("main") {
            arguments = mapOf("changeLogFile" to "db/db.changelog-master.xml") +
                    getBackendDatabaseCredentials(profile).toLiquibaseArguments() +
                    commonArguments
        }
        register("sandbox") {
            arguments = mapOf(
                "changeLogFile" to "save-sandbox/db/db.changelog-sandbox.xml",
                "liquibaseSchemaName" to "save_sandbox",
                "defaultSchemaName" to "save_sandbox",
            ) +
                    getSandboxDatabaseCredentials(profile).toLiquibaseArguments() +
                    commonArguments
        }
    }
}

dependencies {
    liquibaseRuntime(libs.liquibase.core)
    liquibaseRuntime(libs.mysql.connector.java)
    liquibaseRuntime(libs.picocli)
}

tasks.withType<org.liquibase.gradle.LiquibaseTask>().configureEach {
    @Suppress("MAGIC_NUMBER")
    this.javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
        // liquibase-core 4.7.0 and liquibase-gradle 2.1.1 fails on Java >= 13 on Windows; works on Mac
        languageVersion.set(JavaLanguageVersion.of(11))
    })
}

talaiot {
    publishers {
        timelinePublisher = true
    }
}

allprojects {
    configurations.all {
        // if SNAPSHOT dependencies are used, refresh them periodically
        resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.MINUTES)
        resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
    }
}
configureSpotless()

createStackDeployTask(profile)
configurePublishing()
installGitHooks()
