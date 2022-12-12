/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

plugins {
    kotlin("plugin.allopen")
}

configure<AllOpenExtension> {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.Embeddable")
    annotation("javax.persistence.MappedSuperclass")
}

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val libs = the<LibrariesForLibs>()
dependencies {
    add("implementation", platform(libs.spring.boot.dependencies))
    add("implementation", libs.hibernate.core)
    add("implementation", libs.liquibase.core)
    add("implementation", libs.spring.boot.starter.data.jpa)
    add("implementation", libs.kotlin.reflect)
    add("implementation", libs.mysql.connector.java)
    add("testImplementation", libs.testcontainers)
    add("testImplementation", libs.testcontainers.mysql)
    add("testImplementation", libs.testcontainers.junit.jupiter)
}
