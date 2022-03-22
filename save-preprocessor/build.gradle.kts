import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
}

configureSpringBoot()

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
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configureJacoco()
