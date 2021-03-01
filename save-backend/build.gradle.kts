import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import  org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.liquibase.gradle") version Versions.liquibaseGradlePlugin
}

liquibase {
    activities {
        // Configuring luiquibase
        register("main") {
            arguments = mapOf(
                "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.yaml",
                "url" to "jdbc:h2:mem:testdb",
                "username" to "test",
                "password" to "test",
                "logLevel" to "info"
            )
        }
    }
}

//tasks.register("dev") {
//    // depend on the liquibase status task
//    dependsOn("update")
//}

configureSpringBoot(true)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    liquibaseRuntime("org.yaml:snakeyaml:1.15")
    liquibaseRuntime("org.liquibase:liquibase-core:${Versions.liquibase}")
    liquibaseRuntime("mysql:mysql-connector-java:${Versions.liquibaseMySQLConnector}")
    liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:${Versions.liquibaseHibernate5}")
    liquibaseRuntime(sourceSets.getByName("main").output)
    implementation(project(":save-common"))
    implementation("org.hibernate:hibernate-core:${Versions.hibernate}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${Versions.springBoot}")
    implementation("com.h2database:h2:${Versions.h2}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
}

configureJacoco()
