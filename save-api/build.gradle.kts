import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = Versions.jdk
//        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
//    }
//}

kotlin {
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
