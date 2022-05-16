import org.cqfn.save.buildutils.configurePublishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
    `maven-publish`
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.log4j)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.logging)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.cqfn"
            artifactId = "save-cloud-api"
            // for consistency, version chosen same, as the version of backend API
            version = "v1"
            from(components["java"])
        }
    }
}

configurePublishing()
