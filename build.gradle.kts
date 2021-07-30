import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.createStackDeployTask
import org.cqfn.save.buildutils.getDatabaseCredentials
import org.cqfn.save.buildutils.installGitHooks

plugins {
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
    id("org.liquibase.gradle") version Versions.liquibaseGradlePlugin
}

val profile = properties.getOrDefault("save.profile", "dev") as String
val databaseCredentials = getDatabaseCredentials(profile)

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                "changeLogFile" to "db/changelog/db.changelog-master.xml",
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
    liquibaseRuntime("org.liquibase:liquibase-core:${Versions.liquibase}")
    liquibaseRuntime("mysql:mysql-connector-java:${Versions.mySql}")
}

talaiot {
    publishers {
        timelinePublisher = true
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            content {
                includeGroup("org.cqfn.save")
            }
        }
    }
    configureDiktat()
    configureDetekt()
}

createStackDeployTask(profile)
configureVersioning()
createDiktatTask()
createDetektTask()
installGitHooks()
