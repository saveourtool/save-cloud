plugins {
    application
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.kotlin.plugin.serialization)
}

application {
    mainClass.set("com.saveourtool.save.apicli.MainKt")
}

dependencies {
    implementation(projects.saveApi)
    implementation(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.kotlinx.cli)
    implementation(libs.log4j)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.properties)
}
