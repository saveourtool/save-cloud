import org.cqfn.save.buildutils.configureDetekt
import org.cqfn.save.buildutils.configureDiktat
import org.cqfn.save.buildutils.configureVersioning
import org.cqfn.save.buildutils.createDetektTask
import org.cqfn.save.buildutils.createDiktatTask
import org.cqfn.save.buildutils.createStackDeployTask
import org.cqfn.save.buildutils.installGitHooks

plugins {
    kotlin("jvm") version Versions.kotlin apply false
    id("com.github.ben-manes.versions") version "0.36.0"
    id("com.cdsap.talaiot.plugin.base") version "1.4.1"
    id("org.liquibase.gradle") version Versions.liquibaseGradlePlugin
}

val profile = properties.getOrDefault("profile", "dev")

val props = java.util.Properties()
val file = file("save-backend/src/main/resources/application-$profile.properties").apply { props.load(inputStream()) }

if (File("secrets").exists()) {
    file("secrets").apply { props.load(inputStream()) }
}

val databaseUrl = props.getProperty("spring.datasource.url")
var username: String
var password: String

if (profile == "prod") {
    username = props.getProperty("username")
    password = props.getProperty("password")

    fillSpringDatasource(username, password)
} else {
    username = props.getProperty("spring.datasource.username")
    password = props.getProperty("spring.datasource.password")
}

fun fillSpringDatasource(userName: String, password: String) {
    val fileLines = file.readLines()
    val newText: StringBuilder = java.lang.StringBuilder()
    fileLines.forEach {
        when {
            it.startsWith("spring.datasource.username") -> {
                newText.append("spring.datasource.username=$userName\n")
            }
            it.startsWith("spring.datasource.password") -> {
                newText.append("spring.datasource.password=$password\n")
            }
            else -> {
                newText.append("$it\n")
            }
        }
    }
    file.writeText(newText.toString())
}

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                    "changeLogFile" to "db/changelog/db.changelog-master.xml",
                    "url" to "jdbc:mysql://$databaseUrl/save_cloud",
                    "username" to username,
                    "password" to password,
                    "logLevel" to "info",
                    "contexts" to "prod"
            )
        }
    }
}

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:${Versions.liquibase}")
    liquibaseRuntime("mysql:mysql-connector-java:${Versions.mySql}")
    liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:${Versions.liquibaseHibernate5}")
}

talaiot {
    publishers {
        timelinePublisher = true
    }
}

allprojects {
    repositories {
        jcenter()
        mavenCentral()
    }
    configureDiktat()
    configureDetekt()
}

createStackDeployTask()
configureVersioning()
createDiktatTask()
createDetektTask()
installGitHooks()