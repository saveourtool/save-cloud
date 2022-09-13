import com.saveourtool.save.buildutils.configureJacoco
import com.saveourtool.save.buildutils.configureSpotless

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("com.saveourtool.save.buildutils.detekt-common")
    id("com.saveourtool.save.buildutils.diktat-common")
}

dependencies {
    implementation(projects.saveCloudCommon)
    testImplementation(projects.testUtils)
    implementation(libs.save.common.jvm)
    implementation(libs.save.core.jvm)
    implementation(libs.save.plugins.warn.jvm)
    implementation(libs.save.plugins.fix.jvm)
    implementation(libs.save.plugins.fixAndWarn.jvm)
    implementation(libs.jgit)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.ktoml.file)
    implementation(libs.ktoml.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.compress)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configureJacoco()
configureSpotless()
