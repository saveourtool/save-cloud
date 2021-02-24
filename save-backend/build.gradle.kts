import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import  org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.jpa") version Versions.kotlin
}

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
    implementation(project(":save-common"))
    implementation("org.liquibase:liquibase-core:${Versions.liquibase}")
    implementation("org.hibernate:hibernate-core:${Versions.hibernate}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${Versions.springBoot}")
    implementation("com.h2database:h2:${Versions.h2}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
}

configureJacoco()
