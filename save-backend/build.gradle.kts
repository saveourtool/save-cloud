import com.saveourtool.save.buildutils.*
import de.undercouch.gradle.tasks.download.Download
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

import java.nio.file.Files.isDirectory
import java.nio.file.Paths

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    // this plugin will generate generateOpenApiDocs task
    // running this task, it will write the OpenAPI spec into a backend-api-docs.json file in save-backend dir.
    id("org.springdoc.openapi-gradle-plugin") version "1.4.0"
}

openApi {
    apiDocsUrl.set("http://localhost:5800/internal/v3/api-docs/latest")
    outputDir.set(file(projectDir))
    outputFileName.set("backend-api-docs.json")
    waitTimeInSeconds.set(120)

    customBootRun {
        jvmArgs.add("-Dbackend.fileStorage.location=\${HOME}/cnb/files")
    }
}

tasks.named("processTestResources") {
    dependsOn("copyLiquibase")
}

tasks.register<Copy>("copyLiquibase") {
    from("$rootDir/db")
    into("$buildDir/resources/test/db")
}

tasks.register<Exec>("cleanupDbAndStorage") {
    dependsOn(":liquibaseDropAll")
    val profile = properties["save.profile"] as String?

    val userHome = System.getProperty("user.home")
    val isWindows = DefaultNativePlatform.getCurrentOperatingSystem().isWindows

    val storagePath = when (profile) {
        "win" -> "$userHome/.save-cloud/cnb/files"
        "mac" -> "/Users/Shared/.save-cloud/cnb/files"
        else -> when {
            isWindows -> "$userHome/.save-cloud/cnb/files"
            else -> "/home/cnb/files"
        }
    }.let(Paths::get)

    /*
     * No idea why we should rely on running external commands in order to
     * delete a directory, since this can be done in a platform-independent way.
     */
    val args = if (profile != "win" && !isWindows) {
        arrayOf("rm", "-rf")
    } else if (isDirectory(storagePath)) {
        /*
         * cmd.exe will set a non-zero exit status if the directory doesn't exist.
         */
        arrayOf("cmd", "/c", "rmdir", "/s", "/q")
    } else {
        /*
         * Run a dummy command.
         */
        arrayOf("cmd", "/c", "echo")
    }
    commandLine(*args, storagePath)
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveAgentDistroTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveAgentDistro") {
    enabled = findProperty("saveAgentDistroFilepath") != null

    src(KotlinClosure0(function = { findProperty("saveAgentDistroFilepath") ?: "file:\\\\" }))
    dest("$buildDir/agentDistro")
    outputs.dir("$buildDir/agentDistro")
    overwrite(false)
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
    dependsOn(":getSaveCliVersion")
    inputs.file(pathToSaveCliVersion)

    src(KotlinClosure0(function = { getSaveCliPath() }))
    dest("$buildDir/download")
    outputs.dir("$buildDir/download")
    overwrite(false)
}

dependencies {
    implementation(projects.saveCloudCommon)
    runtimeOnly(
        files(layout.buildDirectory.dir("$buildDir/download")).apply {
            builtBy(downloadSaveCliTaskProvider)
        }
    )
    if (!DefaultNativePlatform.getCurrentOperatingSystem().isLinux) {
        logger.warn("Dependency `save-agent` is omitted on Windows and Mac because of problems with linking in cross-compilation." +
                " Task `:save-agent:copyAgentDistribution` would fail without correct libcurl.so. If your changes are about " +
                "save-agent, please test them on Linux " +
                "or put the file with name like `save-agent-*-distribution.jar` built on Linux into libs subfolder."
        )
        runtimeOnly(fileTree("$buildDir/agentDistro").apply {
            builtBy(downloadSaveAgentDistroTaskProvider)
        })
    } else {
        runtimeOnly(project(":save-agent", "distribution"))
    }
    implementation(libs.save.common.jvm)
    implementation(libs.spring.boot.starter.quartz)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
    implementation(libs.hibernate.micrometer)
    implementation(libs.spring.cloud.starter.kubernetes.fabric8.config)
    implementation(libs.reactor.extra)
    implementation(libs.commons.io)
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
configureSpotless()

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
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
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
