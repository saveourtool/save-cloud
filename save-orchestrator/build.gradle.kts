import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import org.cqfn.save.buildutils.pathToSaveCliVersion
import org.cqfn.save.buildutils.readSaveCliVersion

import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.nio.file.Files

plugins {
    kotlin("jvm")
    id("de.undercouch.download")  // can't use `alias`, because this plugin is a transitive dependency of kotlin-gradle-plugin
}

configureSpringBoot()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

// if required, file can be provided manually
val downloadSaveCliTaskProvider = tasks.register<Download>("downloadSaveCli") {
    dependsOn("processResources")
    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)

    src(KotlinClosure0(function = {
        val saveCliVersion = readSaveCliVersion()
        "https://github.com/analysis-dev/save/releases/download/v$saveCliVersion/save-$saveCliVersion-linuxX64.kexe"
    }))
    dest("$buildDir/tmp")
    overwrite(false)

    doFirst { Files.createDirectories(dest.toPath()) }
    doLast { copy { from(outputFiles); into("$buildDir/resources/test") } }
}
tasks.named<BootJar>("bootJar") {
    // include save-cli into BootJar archive
    dependsOn(downloadSaveCliTaskProvider)
    from(downloadSaveCliTaskProvider.map { it.outputFiles }) {
        into("BOOT-INF/classes")
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
    implementation(libs.zip4j)
}

configureJacoco()

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider: TaskProvider<Task> = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)
    inputs.property("project version", version.toString())
    outputs.file(versionsFile)

    doFirst {
        val saveCliVersion = readSaveCliVersion()
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

tasks.withType<Test> {
    testLogging.showStandardStreams = true
}
