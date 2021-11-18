/**
 * Configuration utilities for spring boot projects
 */

package org.cqfn.save.buildutils

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin
import org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

/**
 * Adds necessary spring boot dependencies for [this] project
 *
 * @param withSpringDataJpa whether spring-data related dependencies should be included
 */
@Suppress("TOO_LONG_FUNCTION", "GENERIC_VARIABLE_WRONG_DECLARATION", "COMPLEX_EXPRESSION")
fun Project.configureSpringBoot(withSpringDataJpa: Boolean = false) {
    apply<SpringBootPlugin>()

    val libs = the<LibrariesForLibs>()
    dependencies {
        // FixMe: this is mostly all we need for spring security #314 :)
        // add("implementation", "org.springframework.boot:spring-boot-starter-security:${Versions.springBoot}")
        add("implementation", libs.spring.boot.starter.webflux)
        add("implementation", libs.spring.boot.starter.actuator)
        add("implementation", libs.micrometer.registry.prometheus)  // expose prometheus metrics in actuator
        add("implementation", libs.spring.security.core)
        add("implementation", libs.jackson.module.kotlin)
        add("implementation", libs.slf4j.api)
        add("implementation", libs.logback.core)
        add("implementation", libs.reactor.kotlin.extensions)
        add("testImplementation", libs.spring.boot.starter.test)
        add("testImplementation", libs.mockito.kotlin)
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
            add("testImplementation", libs.okhttp)
            add("testImplementation", libs.okhttp.mockwebserver)
        }
    }

    tasks.named<BootBuildImage>("bootBuildImage") {
        imageName = "ghcr.io/diktat-static-analysis/${project.name}:${project.versionForDockerImages()}"
        environment = mapOf(
            "BP_JVM_VERSION" to Versions.BP_JVM_VERSION,
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
                    username = "diktat-static-analysis"
                    password = registryPassword
                    url = "https://ghcr.io"
                }
            }
        }
    }
}
