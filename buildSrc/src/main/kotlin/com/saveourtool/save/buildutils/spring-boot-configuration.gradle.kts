/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    kotlin("kapt")
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

extensions.getByType<KotlinJvmProjectExtension>().jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val libs = the<LibrariesForLibs>()
dependencies {
    add("implementation", platform(libs.spring.boot.dependencies))
    add("implementation", libs.spring.boot.starter.webflux)
    add("implementation", libs.spring.boot.starter.actuator)
    add("implementation", libs.micrometer.registry.prometheus)  // expose prometheus metrics in actuator
    add("implementation", libs.jackson.module.kotlin)
    add("implementation", libs.slf4j.api)
    add("implementation", libs.logback.core)
    add("implementation", libs.reactor.kotlin.extensions)

    add("implementation", libs.springdoc.openapi.ui)
    add("runtimeOnly", libs.springdoc.openapi.webflux.ui)
    add("runtimeOnly", libs.springdoc.openapi.security)
    add("runtimeOnly", libs.springdoc.openapi.kotlin)
    add("implementation", libs.swagger.annotations)
    add("kapt", libs.spring.boot.configuration.processor)

    add("testImplementation", libs.spring.boot.starter.test)
    add("testImplementation", libs.mockito.kotlin)
    add("testImplementation", libs.okhttp)
    add("testImplementation", libs.okhttp.mockwebserver)
    add("testImplementation", libs.kotest.assertions.core)
}

configure<SpringBootExtension> {
    buildInfo()  // configures `bootBuildInfo` task, which creates META-INF/build-info.properties file
}

tasks.withType<BootRun>().configureEach {
    val profiles = buildString {
        append("dev")
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        when {
            os.isWindows -> append(",win")
            os.isMacOsX -> append(",mac")
        }
        if (project.path.contains("save-backend")) {
            append(",secure")
        }
    }
    environment["SPRING_PROFILES_ACTIVE"] = profiles
}

tasks.named<BootBuildImage>("bootBuildImage") {
    imageName = "ghcr.io/saveourtool/${project.name}:${project.versionForDockerImages()}"
    environment = mapOf(
        "BP_JVM_VERSION" to Versions.jdk,
        "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
        "BPE_APPEND_JAVA_TOOL_OPTIONS" to
                // Workaround for https://github.com/reactor/reactor-netty/issues/564
                "-Dreactor.netty.pool.maxIdleTime=60000 -Dreactor.netty.pool.leasingStrategy=lifo " +
                        // Override default configuration. Intended to be used on a particular environment.
                        "-Dspring.config.additional-location=optional:file:/home/cnb/config/application.properties"
    )
    isVerboseLogging = true
    System.getenv("GHCR_PWD")?.let { registryPassword ->
        isPublish = true
        docker {
            publishRegistry {
                username = "saveourtool"
                password = registryPassword
                url = "https://ghcr.io"
            }
        }
    }
}
