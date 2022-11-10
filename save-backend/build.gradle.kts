import com.saveourtool.save.buildutils.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

import java.nio.file.Files.isDirectory
import java.nio.file.Paths

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.spring-data-configuration")
    id("com.saveourtool.save.buildutils.save-cli-configuration")
    id("com.saveourtool.save.buildutils.save-agent-configuration")
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
        jvmArgs.add("-Dbackend.fileStorage.location=\${user.home}/cnb/files")
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

dependencies {
    implementation(projects.saveCloudCommon)
    implementation(projects.authenticationService)
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
