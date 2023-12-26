plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.kotlin.plugin.serialization)
}

dependencies {
    implementation(projects.authenticationService)
    api(projects.saveCloudCommon)
    api(libs.cosv4k)
    implementation(libs.spring.security.core)
    implementation(libs.spring.data.jpa)
    implementation(libs.hibernate.jpa21.api)
}
