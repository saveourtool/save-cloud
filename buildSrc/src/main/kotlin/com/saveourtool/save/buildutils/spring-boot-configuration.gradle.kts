/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("kapt")
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
