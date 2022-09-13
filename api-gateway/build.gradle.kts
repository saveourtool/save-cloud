import com.saveourtool.save.buildutils.configureJacoco
import com.saveourtool.save.buildutils.configureSpotless

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("com.saveourtool.save.buildutils.detekt-common")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    api(projects.saveCloudCommon)
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation(libs.spring.security.core)
}

configureJacoco()
configureSpotless()
