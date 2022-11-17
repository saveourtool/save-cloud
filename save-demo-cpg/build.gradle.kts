import com.saveourtool.save.buildutils.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

import java.nio.file.Files.isDirectory
import java.nio.file.Paths

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    id("com.saveourtool.save.buildutils.save-cli-configuration")
    id("com.saveourtool.save.buildutils.save-agent-configuration")
}

dependencies {
    implementation(libs.spring.boot.starter.quartz)
    implementation(libs.spring.security.core)
    implementation(libs.hibernate.micrometer)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation(libs.reactor.extra)
    testImplementation(libs.spring.security.test)
    testImplementation(projects.testUtils)
}

configureJacoco()
configureSpotless()
