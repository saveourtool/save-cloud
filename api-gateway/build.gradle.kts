plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

kotlin {
    sourceSets {
        val commonMain by creating {
            dependencies {
                api(projects.saveCloudCommon)
            }
        }
    }
}
dependencies {
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation(libs.spring.security.core)
    implementation(projects.authenticationService)
}
