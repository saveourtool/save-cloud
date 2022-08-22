import com.saveourtool.save.buildutils.*
import de.undercouch.gradle.tasks.download.Download
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("jvm")
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

configureSpringBoot(true)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
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

tasks.register<Exec>("cleanupDbAndStorage") {
    dependsOn(":liquibaseDropAll")
    val profile = properties.get("save.profile") as String?

    val storagePath = when (profile) {
        "win" -> "${System.getProperty("user.home")}/.save-cloud/cnb/files"
        "mac" -> "/Users/Shared/.save-cloud/cnb/files"
        else -> "/home/cnb/files"
    }

    val args = if (profile != "win") {
        arrayOf("rm", "-rf")
    } else {
        arrayOf("rmdir", "/s", "/q")
    }
    commandLine(*args, storagePath)
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveAgentDistroTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveAgentDistro") {
    enabled = findProperty("saveAgentDistroFilepath") != null
    src(KotlinClosure0(function = { findProperty("saveAgentDistroFilepath") ?: "file:\\\\" }))
    File("$buildDir/agentDistro/").mkdirs()
    dest("$buildDir/agentDistro")
    outputs.dir("$buildDir/agentDistro")
    overwrite(false)
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val downloadSaveCliTaskProvider: TaskProvider<Download> = tasks.register<Download>("downloadSaveCli") {
    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)

    src(KotlinClosure0(function = { getSaveCliPath() }))
    dest("$buildDir/download")
    outputs.file("$buildDir/download/${getSaveCliPath().substringAfterLast("/")}")
    overwrite(false)
}

dependencies {
    implementation(projects.saveCloudCommon)
    runtimeOnly(projects.saveFrontend) {
        targetConfiguration = "distribution"  // static resources packed as a jar, will be accessed from classpath
    }
    runtimeOnly(files("$buildDir/download/${getSaveCliPath().substringAfterLast("/")}"))
    if (!DefaultNativePlatform.getCurrentOperatingSystem().isLinux) {
        logger.warn("Dependency `save-agent` is omitted on Windows and Mac because of problems with linking in cross-compilation." +
                " Task `:save-agent:copyAgentDistribution` would fail without correct libcurl.so. If your changes are about " +
                "save-agent, please test them on Linux " +
                "or put the file with name like `save-agent-*-distribution.jar` built on Linux into libs subfolder."
        )
        runtimeOnly(fileTree("$buildDir/agentDistro"))
    } else {
        runtimeOnly(project(":save-agent", "distribution"))
    }
    implementation(libs.save.common.jvm)
    implementation(libs.spring.boot.starter.quartz)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.security.core)
    implementation(libs.hibernate.micrometer)
    implementation(libs.spring.cloud.starter.kubernetes.client.config)
    implementation("io.projectreactor.addons:reactor-extra")
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

// since we store save-cli in resources directory, a lot of tasks start using it
// and gradle complains about missing dependency
tasks.named("jar") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named<BootJar>("bootJar") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named<BootRun>("bootRun") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named("bootJarMainClassName") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named<KotlinCompile>("compileTestKotlin") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named("test") { dependsOn(downloadSaveCliTaskProvider) }
tasks.named("jacocoTestReport") { dependsOn(downloadSaveCliTaskProvider) }
// hack, because Gradle smh doesn't infer dependencies on tasks that produce `files(...)` dependencies
downloadSaveCliTaskProvider.configure { dependsOn(downloadSaveAgentDistroTaskProvider) }