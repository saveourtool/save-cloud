/**
 * Configuration utilities for spring boot projects
 */

package com.saveourtool.save.buildutils

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin
import org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.run.BootRun

/**
 * Adds necessary spring boot dependencies for [this] project
 *
 * @param withSpringDataJpa whether spring-data related dependencies should be included
 */
@Suppress("TOO_LONG_FUNCTION", "GENERIC_VARIABLE_WRONG_DECLARATION", "COMPLEX_EXPRESSION")
fun Project.configureSpringBoot(withSpringDataJpa: Boolean = false) {
    apply<SpringBootPlugin>()

    extensions.getByType<KotlinJvmProjectExtension>().jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }

    val libs = the<LibrariesForLibs>()
    dependencies {
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

        add("testImplementation", libs.spring.boot.starter.test)
        add("testImplementation", libs.mockito.kotlin)
        add("testImplementation", libs.okhttp)
        add("testImplementation", libs.okhttp.mockwebserver)
    }

    configure<SpringBootExtension> {
        buildInfo()  // configures `bootBuildInfo` task, which creates META-INF/build-info.properties file
    }

    apply<SpringGradleSubplugin>()

    if (withSpringDataJpa) {
        apply<AllOpenGradleSubplugin>()

        configure<AllOpenExtension> {
            annotation("javax.persistence.Entity")
            annotation("javax.persistence.Embeddable")
            annotation("javax.persistence.MappedSuperclass")
        }

        dependencies {
            add("implementation", libs.hibernate.core)
            add("implementation", libs.liquibase.core)
            add("implementation", libs.spring.boot.starter.data.jpa)
            add("implementation", libs.kotlin.reflect)
            add("implementation", libs.mysql.connector.java)
            add("testImplementation", libs.testcontainers)
            add("testImplementation", libs.testcontainers.mysql)
            add("testImplementation", libs.testcontainers.junit.jupiter)
        }
    }

    tasks.withType<BootRun>().configureEach {
        if (Os.isFamily(Os.FAMILY_MAC)) {
            val profiles = if (this.path.contains("save-backend")) {
                "secure,dev,mac"
            } else {
                "dev,mac"
            }
            environment["SPRING_PROFILES_ACTIVE"] = profiles
        }
    }

    tasks.named<BootBuildImage>("bootBuildImage") {
        imageName = "ghcr.io/analysis-dev/${project.name}:${project.versionForDockerImages()}"
        environment = mapOf(
            "BP_JVM_VERSION" to Versions.jdk,
            "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
            "BPE_APPEND_JAVA_TOOL_OPTIONS" to "-Dreactor.netty.pool.maxIdleTime=60000 -Dreactor.netty.pool.leasingStrategy=lifo " +
                    "-Dspring.config.additional-location=optional:file:/home/cnb/config/application.properties"
        )
        isVerboseLogging = true
        val registryPassword: String? = System.getenv("GHCR_PWD")
        isPublish = registryPassword != null
        if (isPublish) {
            docker {
                publishRegistry {
                    username = "analysis-dev"
                    password = registryPassword
                    url = "https://ghcr.io"
                }
            }
        }
    }
}
