import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
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
    implementation(libs.reckon.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(libs.gradle.plugin.spotless)
    implementation(libs.publish.gradle.plugin)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}
