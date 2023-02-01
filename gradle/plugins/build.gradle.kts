plugins {
    // Using dependencies with Kotlin 1.8 requires compatible embedded Kotlin in Gradle.
    // Gradle >= 7.6.0 (containing `kotlin-dsl` >= 2.4.0) is compatible; Gradle 7.5.x still embeds Kotlin 1.6.x.
    `kotlin-dsl` version "2.4.1"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // workaround https://github.com/gradle/gradle/issues/15383
    implementation(files(project.libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.kotlin.plugin.allopen)
    implementation(libs.download.plugin)
    implementation(libs.reckon.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(libs.gradle.plugin.spotless)
    implementation(libs.publish.gradle.plugin)
}
