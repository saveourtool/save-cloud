import com.saveourtool.save.buildutils.*

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    kotlin("plugin.allopen")
}

configureJacoco()
configureSpotless()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

val diktatVersion: String = libs.versions.diktat.get()

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(libs.zip4j)
    implementation(libs.save.common.jvm)

    implementation(libs.ktlint.core)
    implementation(libs.ktlint.rulesets.standard)
    implementation("org.cqfn.diktat:diktat-common:$diktatVersion") {
        exclude(group = "org.apache.logging.log4j")
    }
    implementation("org.cqfn.diktat:diktat-rules:$diktatVersion") {
        exclude(group = "org.apache.logging.log4j")
    }
}
