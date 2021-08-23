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
        src("https://github.com/cqfn/save/releases/download/v$saveCliVersion/save-$saveCliVersion-linuxX64.kexe")
        dest("$buildDir/resources/main")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    api(project(":save-cloud-common"))
    runtimeOnly(project(":save-agent", "distribution"))
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
    implementation("com.github.docker-java:docker-java-core:${Versions.dockerJavaApi}")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:${Versions.dockerJavaApi}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${Versions.serialization}")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    testImplementation("com.squareup.okhttp3:okhttp:${Versions.okhttp3}")
    testImplementation("com.squareup.okhttp3:mockwebserver:${Versions.okhttp3}")
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
val generatedKotlinSrc = kotlin.sourceSets.create("generated") {
    kotlin.srcDir("$buildDir/generated/src")
}
kotlin.sourceSets.getByName("main").dependsOn(generatedKotlinSrc)
tasks.withType<KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
