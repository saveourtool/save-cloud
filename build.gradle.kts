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

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                    "changeLogFile" to "db/changelog/db.changelog-master.xml",
                    "url" to "jdbc:mysql://172.17.0.2:3306/test",
                    "username" to "root",
                    "password" to "123",
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