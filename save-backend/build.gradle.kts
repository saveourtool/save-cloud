import org.cqfn.save.buildutils.configureJacoco
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
val dockerJavaApiVersion = "3.2.7"

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("org.hibernate:hibernate-core:$hibernateVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-core:$logbackVersion")
    implementation("com.github.docker-java:docker-java-core:$dockerJavaApiVersion")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:$dockerJavaApiVersion")
    implementation("org.apache.commons:commons-compress:1.20")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

configureJacoco()
