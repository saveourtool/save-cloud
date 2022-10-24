import com.saveourtool.save.buildutils.*

plugins {
    alias(libs.plugins.download)
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
}

kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
        annotation("org.springframework.stereotype.Service")
    }

    sourceSets {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
}

configureJacoco()
configureSpotless()
