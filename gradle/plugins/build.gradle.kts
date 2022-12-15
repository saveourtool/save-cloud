plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // workaround https://github.com/gradle/gradle/issues/15383
    implementation(files(project.libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.kotlin.plugin.allopen)
    implementation(libs.gradle.plugin.spotless)
    implementation(libs.download.plugin)
    implementation(libs.reckon.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.diktat.gradle.plugin)
    implementation(libs.gradle.plugin.spotless)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}
