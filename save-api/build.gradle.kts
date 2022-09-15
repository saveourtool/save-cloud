import com.saveourtool.save.buildutils.configurePublishing

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    alias(libs.plugins.kotlin.plugin.serialization)
    `maven-publish`
}

kotlin {
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

java {
    withSourcesJar()
}

dependencies {
    api(projects.saveCloudCommon)
    implementation(libs.save.common.jvm)
    implementation(libs.log4j)
    implementation(libs.log4j.slf4j.impl)
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.logging)
    api(libs.arrow.kt.core)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.saveourtool.save"
            artifactId = "save-cloud-api"
            version = version
            from(components["java"])
        }
    }
}

configurePublishing()
