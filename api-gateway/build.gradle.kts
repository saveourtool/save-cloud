import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpotless
import org.cqfn.save.buildutils.configureSpringBoot

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

configureSpringBoot()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
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
