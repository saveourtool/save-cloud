import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

kotlin {

//    configure {
//        binaries {
//            executable {
//                entryPoint = "org.cqfn.save.api.main"
//                baseName = "save-api"
//            }
//        }
//    }


    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.slf4j.api)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.serialization)
    implementation(libs.kotlinx.serialization.properties)
}
