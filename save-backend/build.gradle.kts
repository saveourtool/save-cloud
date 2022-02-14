import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
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
    testImplementation(libs.spring.security.test)
}

configureJacoco()
tasks.withType<Test> {
    extensions.configure(JacocoTaskExtension::class) {
        // this file is only used in dev profile for debugging, no need to calculate test coverage
        excludes = listOf("**/CorsFilter.kt")
    }
}
