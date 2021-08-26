plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version Versions.kotlin
    kotlin("plugin.allopen")
    kotlin("plugin.jpa") version Versions.kotlin
}
kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
        annotation("org.springframework.stereotype.Service")
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Versions.jdk
            }
        }
    }
    js(BOTH).browser()

    // setup native compilation
    linuxX64()

    sourceSets {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Versions.serialization}")
                api("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.hibernate.javax.persistence:hibernate-jpa-2.1-api:${Versions.jpa}")
            }
        }
    }
}
