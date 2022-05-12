import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import org.cqfn.save.buildutils.pathToSaveCliVersion
import org.cqfn.save.buildutils.readSaveCliVersion

import de.undercouch.gradle.tasks.download.Download
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm")
    id("de.undercouch.download")  // can't use `alias`, because this plugin is a transitive dependency of kotlin-gradle-plugin
    id("org.gradle.test-retry") version "1.3.2"
}

configureSpringBoot()
configureJacoco()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }
}

// if required, file can be provided manually
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
    dependsOn("processResources")
    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)

    src(KotlinClosure0(function = {
        val saveCliVersion = readSaveCliVersion()
        "https://github.com/analysis-dev/save/releases/download/v$saveCliVersion/save-$saveCliVersion-linuxX64.kexe"
    }))
    dest("$buildDir/resources/main")
    overwrite(false)
}
// since we store save-cli in resources directory, a lot of tasks start using it
// and gradle complains about missing dependency
tasks.named("jar") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named<BootJar>("bootJar") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named<BootRun>("bootRun") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named("bootJarMainClassName") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named<KotlinCompile>("compileTestKotlin") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named("test") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named("jacocoTestReport") { dependsOn(downloadSaveCliTaskProvider) }

tasks.withType<Test> {
    useJUnitPlatform()
    retry {
        failOnPassedAfterRetry.set(false)
        maxFailures.set(20)
        maxRetries.set(5)
    }
}

dependencies {
    api(projects.saveCloudCommon)
    testImplementation(projects.testUtils)
    if (DefaultNativePlatform.getCurrentOperatingSystem().isWindows) {
        logger.warn("Dependency `save-agent` is omitted on Windows because of problems with linking in cross-compilation." +
                " Task `:save-agent:linkReleaseExecutableLinuxX64` would fail without correct libcurl.so. If your changes are about " +
                "save-agent, please test them on Linux or provide a file `save-agent-distribution.jar` built on Linux."
        )
    } else {
        runtimeOnly(project(":save-agent", "distribution"))
    }
    implementation(libs.dockerJava.core)
    implementation(libs.dockerJava.transport.httpclient5)
    implementation(libs.kotlinx.serialization.json.jvm)
    implementation(libs.commons.compress)
    implementation(libs.kotlinx.datetime)
    implementation(libs.zip4j)
    implementation(libs.kubernetes.javaClient)
}

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
