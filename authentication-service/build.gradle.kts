@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
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
    implementation(libs.spring.jdbc)
    implementation(libs.spring.security.core)
    implementation(libs.spring.security.config)
    implementation(libs.spring.security.web)
    implementation(libs.spring.boot.autoconfigure) {
        because("This dependency contains `ConditionalOnCloudPlatform`")
    }
    implementation(libs.fabric8.kubernetes.client)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter.api)
}
