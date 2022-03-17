plugins {
    kotlin("multiplatform")
}

kotlin {
    // Create a target for the host platform.
    val hostTarget = linuxX64 {
        binaries.executable {
            entryPoint = "org.cqfn.save.api.main"
            baseName = "save-api"
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
        val linuxX64Main by getting {
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.curl)
                implementation(libs.ktor.client.serialization)
            }
        }
    }
}
