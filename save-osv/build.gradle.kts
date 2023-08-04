plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

dependencies {
    api(projects.saveCloudCommon)
    api(libs.osv4k)
    implementation(projects.authenticationService)
    implementation(libs.spring.security.core)
}
