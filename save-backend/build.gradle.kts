import  org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

val kotlinVersion = "1.4.21"
val springBootVersion = "2.2.6.RELEASE"
val hibernateVersion = "5.4.2.Final"
val slf4jVersion = "1.7.30"
val compileKotlin: KotlinCompile by tasks

compileKotlin.apply {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kotlin {
    sourceSets {
        getByName("main") {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
                implementation("org.hibernate:hibernate-core:$hibernateVersion")
                implementation("org.slf4j:slf4j-api:$slf4jVersion")
            }
        }
        getByName("test") {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
            }
        }
    }
}