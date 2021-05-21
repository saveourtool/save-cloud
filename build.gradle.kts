import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.createStackDeployTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.2"
    id("org.liquibase.gradle") version Versions.liquibaseGradlePlugin
}

val profile = properties.getOrDefault("profile", "dev") as String

val props = java.util.Properties()
val file = file("save-backend/src/main/resources/application-$profile.properties").apply { props.load(inputStream()) }

if (File("${System.getenv()["HOME"]}/secrets").exists()) {
    file("${System.getenv()["HOME"]}/secrets").apply { props.load(inputStream()) }
}

var databaseUrl: String
var username: String
var password: String

if (profile == "prod") {
    databaseUrl = props.getProperty("spring.datasource.url")
    username = props.getProperty("username")
    password = props.getProperty("password")
} else {
    databaseUrl = props.getProperty("datasource.dev.url")
    username = props.getProperty("spring.datasource.username")
    password = props.getProperty("spring.datasource.password")
}

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                    "changeLogFile" to "db/changelog/db.changelog-master.xml",
                    "url" to databaseUrl,
                    "username" to username,
                    "password" to password,
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
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {  // detekt requires kotlinx.html
            content {
                includeModule("org.jetbrains.kotlinx", "kotlinx-html-jvm")
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
