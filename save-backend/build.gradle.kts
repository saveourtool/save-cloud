import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.springdoc.openapi-gradle-plugin") version "1.3.4"
}

openApi {
    apiDocsUrl.set("http://localhost:5000/internal/v3/api-docs")
    outputFileName.set("swagger-api-docs.json")
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

configureSpringBoot(true)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.getByName("processTestResources").dependsOn("copyLiquibase")

tasks.register<Copy>("copyLiquibase") {
    from("$rootDir/db")
    into("$buildDir/resources/test/db")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(projects.saveCloudCommon)
    runtimeOnly(project(":save-frontend", "distribution"))  // static resources packed as a jar, will be accessed from classpath
    implementation(libs.save.common.jvm)
    implementation(libs.spring.boot.starter.quartz)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
    implementation(libs.hibernate.micrometer)
    implementation(libs.springfox.boot.starter)
    implementation(libs.springfox.swagger.ui)
    //implementation(libs.spring.fox.swagger)
    testImplementation(libs.spring.security.test)
    testImplementation(projects.testUtils)
}

configureJacoco()
tasks.withType<Test> {
    extensions.configure(JacocoTaskExtension::class) {
        // this file is only used in dev profile for debugging, no need to calculate test coverage
        excludes = listOf("**/CorsFilter.kt")
    }
}
