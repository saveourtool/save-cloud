import com.saveourtool.save.buildutils.*

import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    alias(libs.plugins.download)
    id("org.gradle.test-retry") version "1.4.1"
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
}

configureJacoco()
configureSpotless()

dependencies {
    implementation(projects.saveOrchestratorCommon)
//    implementation(libs.zip4j)
//    implementation(libs.spring.cloud.starter.kubernetes.client.config)
//    implementation(libs.hibernate.jpa21.api)
//    implementation(libs.save.plugins.warn.jvm)
//    testImplementation(projects.testUtils)
//    testImplementation(libs.fabric8.kubernetes.server.mock)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
}

