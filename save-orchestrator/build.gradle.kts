import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import org.cqfn.save.buildutils.getSaveCliVersion

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

configureSpringBoot()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

// if required, file can be provided manually
val saveCliVersion = getSaveCliVersion()
@Suppress("RUN_IN_SCRIPT")
if (!file("$buildDir/resources/main/save-$saveCliVersion-linuxX64.kexe").exists()) {
    tasks.getByName("processResources").finalizedBy("downloadSaveCli")
    tasks.register<Download>("downloadSaveCli") {
        dependsOn("processResources")
        src("https://github.com/diktat-static-analysis/save/releases/download/v$saveCliVersion/save-$saveCliVersion-linuxX64.kexe")
        dest("$buildDir/resources/main")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    api(projects.saveCloudCommon)
    runtimeOnly(project(":save-agent", "distribution"))
    implementation(libs.dockerJava.core)
    implementation(libs.dockerJava.transport.httpclient5)
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.datetime)
}

configureJacoco()

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    val saveCliVersion = getSaveCliVersion()
    inputs.property("Version of save-cli", saveCliVersion)
    inputs.property("project version", version.toString())
    outputs.file(versionsFile)

    doFirst {
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "$saveCliVersion"
            internal const val SAVE_CLOUD_VERSION = "$version"

            """.trimIndent()
        )
    }
}
kotlin.sourceSets.getByName("main") {
    kotlin.srcDir("$buildDir/generated/src")
}
tasks.withType<KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
