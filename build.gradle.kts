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
    id("org.liquibase.gradle") version Versions.liquibaseGradlePlugin
}

val profile = if(properties["profile"] == null) "dev" else properties["profile"]
val propertyFileLines = File("save-backend/src/main/resources/application-$profile.properties").readLines()
val secretsLines = File("secrets").readLines() // TODO: specify path to file

val databaseUrl = getCredential(propertyFileLines, "database.url")
var username: String
var password: String

if (profile == "prod") {
    username = getCredential(secretsLines, "username")
    password = getCredential(secretsLines, "password")
} else {
    username = getCredential(propertyFileLines, "database.username")
    password = getCredential(propertyFileLines, "database.password")
}

fun getCredential(lines: List<String>, token: String): String =
    lines.find {
        it.startsWith(token)
    }.let {
        it?.split("=")?.get(1)?.trim()!!
    }

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                    "changeLogFile" to "db/changelog/db.changelog-master.xml",
                    "url" to "jdbc:mysql://$databaseUrl:3306/test",
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