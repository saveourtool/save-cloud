plugins {
    // Using dependencies with Kotlin 1.8 requires compatible embedded Kotlin in Gradle.
    // Gradle >= 7.6.0 (containing `kotlin-dsl` >= 2.4.0) is compatible; Gradle 7.5.x still embeds Kotlin 1.6.x.
    `kotlin-dsl` version "2.4.1"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    // workaround https://github.com/gradle/gradle/issues/15383
    implementation(files(project.libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.kotlin.gradle.plugin) {
        because("Add plugin on plugin classpath here to automatically set its version for the whole build")
    }
    implementation(libs.spring.boot.gradle.plugin)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}
