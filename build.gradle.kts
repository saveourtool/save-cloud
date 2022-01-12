import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.createStackDeployTask
import org.cqfn.save.buildutils.getDatabaseCredentials
import org.cqfn.save.buildutils.installGitHooks

plugins {
    alias(libs.plugins.versions.plugin)
    alias(libs.plugins.talaiot.base)
    alias(libs.plugins.liquibase.gradle)
}

val profile = properties.getOrDefault("save.profile", "dev") as String
val databaseCredentials = getDatabaseCredentials(profile)

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                "changeLogFile" to "db/db.changelog-master.xml",
                "url" to databaseCredentials.databaseUrl,
                "username" to databaseCredentials.username,
                "password" to databaseCredentials.password,
                "logLevel" to "info",
                "contexts" to when (profile) {
                    "prod" -> "prod"
                    "dev" -> "dev"
                    else -> throw GradleException("Profile $profile not configured to map on a particular liquibase context")
                }
            )
        }
    }
}

dependencies {
    liquibaseRuntime(libs.liquibase.core)
    liquibaseRuntime(libs.mysql.connector.java)
    liquibaseRuntime(libs.picocli)
    liquibaseRuntime(libs.snakeyaml)
}

talaiot {
    publishers {
        timelinePublisher = true
    }
}

allprojects {
    configureDiktat()
    configureDetekt()
    configurations.all {
        // if SNAPSHOT dependencies are used, refresh them periodically
        resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.MINUTES)
    }
}

createStackDeployTask(profile)
configureVersioning()
createDiktatTask()
createDetektTask()
installGitHooks()
