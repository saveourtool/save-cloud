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

dependencies {
    //implementation(libs.okhttp)
    implementation(libs.slf4j.api)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.curl)
    implementation(libs.ktor.client.serialization)
}
