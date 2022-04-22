import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createStackDeployTask
import org.cqfn.save.buildutils.getDatabaseCredentials
import org.cqfn.save.buildutils.installGitHooks
import org.cqfn.save.buildutils.registerSaveCliVersionCheckTask
import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    alias(libs.plugins.talaiot.base)
    alias(libs.plugins.liquibase.gradle)
    java
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
}

tasks.withType<org.liquibase.gradle.LiquibaseTask>().configureEach {
    this.javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
        // liquibase-core 4.7.0 and liquibase-gradle 2.1.1 fails on Java >= 13
        val javaVersion = if (Os.isFamily(Os.FAMILY_MAC)) { 17 } else { 11 }
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    })
}

talaiot {
    publishers {
        timelinePublisher = true
    }
}

allprojects {
    configureDetekt()
    configurations.all {
        // if SNAPSHOT dependencies are used, refresh them periodically
        resolutionStrategy.cacheDynamicVersionsFor(10, TimeUnit.MINUTES)
        resolutionStrategy.cacheChangingModulesFor(10, TimeUnit.MINUTES)
    }
}
allprojects {
    configureDiktat()
}

createStackDeployTask(profile)
configureVersioning()
createDetektTask()
installGitHooks()
registerSaveCliVersionCheckTask()
