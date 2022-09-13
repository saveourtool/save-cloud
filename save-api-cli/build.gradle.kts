import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.detekt-common")
    id("com.saveourtool.save.buildutils.diktat-common")
}

application {
    mainClass.set("com.saveourtool.save.apicli.MainKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

dependencies {
    implementation(projects.saveApi)
    implementation(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.kotlinx.cli)
    implementation(libs.log4j)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.properties)
}
