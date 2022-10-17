import com.saveourtool.save.buildutils.*

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    alias(libs.plugins.download)
    id("org.gradle.test-retry") version "1.4.1"
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
}

kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
        annotation("org.springframework.stereotype.Service")
    }
//
//    jvmToolchain {
//        this.languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
//    }

    sourceSets {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }

    }
}

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(projects.saveOrchestratorCommon)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
}


configureJacoco()
configureSpotless()
