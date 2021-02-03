import  org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val kotlinVersion = "1.4.21"
val springBootVersion = "2.4.2"
val springSecurityVersion = "5.4.2"
val hibernateVersion = "5.4.2.Final"
val liquibaseVersion = "4.2.2"
val slf4jVersion = "1.7.30"
val logbackVersion = "1.2.3"
val compileKotlin: KotlinCompile by tasks

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
        implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
        implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
        implementation("org.liquibase:liquibase-core:$liquibaseVersion")
        implementation("org.hibernate:hibernate-core:$hibernateVersion")
        implementation("org.slf4j:slf4j-api:$slf4jVersion")
        implementation("ch.qos.logback:logback-core:$logbackVersion")
        testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    }
}