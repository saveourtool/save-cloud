plugins {
    kotlin("multiplatform")
    //alias(libs.plugins.kotlin.plugin.serialization)
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
                implementation(projects.saveCloudCommon)
//                implementation(file("$rootDir/save-cloud-common/build/libs/save-cloud-common-linuxX64-$version.jar"))
                implementation(libs.slf4j.api)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.curl)
                implementation(libs.ktor.client.serialization)
            }
        }
    }
}
