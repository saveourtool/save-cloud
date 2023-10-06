import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("de.undercouch.download")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    api(projects.saveCloudCommon)
    implementation(projects.saveOrchestratorCommon)
    implementation(project(":save-agent:save-cloud-agent-api"))
    implementation(libs.save.common.jvm)
    implementation(libs.dockerJava.core)
    implementation(libs.dockerJava.transport.httpclient5)
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.datetime)
    implementation(libs.zip4j)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation(libs.fabric8.kubernetes.client)
    implementation(libs.spring.kafka)
    testImplementation(projects.testUtils)
    testImplementation(libs.fabric8.kubernetes.server.mock)
}
