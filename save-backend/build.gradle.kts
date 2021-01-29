plugins {
    kotlin("multiplatform") version "1.4.10"
}


repositories {
    jcenter()
    mavenCentral()
}

val kotlinVersion = "1.4.10"
val springBootVersion = "2.2.6.RELEASE"
val hibernateVersion = "5.4.2.Final"
val loggerVersion = "1.7.30"

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
                implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
                implementation("org.hibernate:hibernate-core:$hibernateVersion")
                implementation("org.slf4j:slf4j-api:$loggerVersion")
            }
        }
        getByName("jvmTest") {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
            }
        }
    }
}