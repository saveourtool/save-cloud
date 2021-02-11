import org.cqfn.save.buildutils.configureJacoco
import  org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.springframework.boot") version Versions.springBoot
}

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
    implementation("org.springframework.boot:spring-boot-starter-webflux:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-actuator:${Versions.springBoot}")
    implementation("org.springframework.security:spring-security-core:${Versions.springSecurity}")
    implementation("org.liquibase:liquibase-core:${Versions.liquibase}")
    implementation("org.hibernate:hibernate-core:${Versions.hibernate}")
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
    testImplementation("org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
}

configureJacoco()
