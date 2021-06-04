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
    val os = org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem()
    val hostTarget = when {
        os.isLinux -> linuxX64()
        os.isWindows -> mingwX64()
        os.isMacOsX -> macosX64()
        else -> throw GradleException("Host OS '${os.name}' is not supported in Kotlin/Native $project.")
    }

    sourceSets {
        sourceSets.all {
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.hibernate.javax.persistence:hibernate-jpa-2.1-api:${Versions.jpa}")
            }
        }
    }
}
