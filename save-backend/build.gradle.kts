plugins {
    kotlin("multiplatform") version "1.4.21"
    id("org.springframework.boot") version "2.4.1"
}


repositories {
    jcenter()
}

val kotlinVersion = "1.4.21"
val springBootVersion = "2.4.1"
val springWebFluxVersion = "5.2.8.RELEASE"
val reactVersion = "17.0.0"
val kotlinReactVersion = "17.0.0-pre.134-kotlin-1.4.10"

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */

    jvm {
        repositories {
            mavenLocal()
            mavenCentral()
        }
        withJava()
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        getByName("jvmMain") {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
                implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
                implementation("io.projectreactor:reactor-core:3.4.2")
                implementation("org.reactivestreams:reactive-streams:1.0.2")
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
            }
        }
    }
}