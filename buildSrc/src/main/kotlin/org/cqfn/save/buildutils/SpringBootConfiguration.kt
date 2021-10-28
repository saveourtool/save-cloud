/**
 * Configuration utilities for spring boot projects
 */

package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
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
@Suppress("TOO_LONG_FUNCTION")
fun Project.configureSpringBoot(withSpringDataJpa: Boolean = false) {
    apply<SpringBootPlugin>()

    dependencies {
        // FixMe: this is mostly all we need for spring security #314 :)
        // add("implementation", "org.springframework.boot:spring-boot-starter-security:${Versions.springBoot}")
        add("implementation", "org.springframework.boot:spring-boot-starter-webflux:${Versions.springBoot}")
        add("implementation", "org.springframework.boot:spring-boot-starter-actuator:${Versions.springBoot}")
        add("implementation", "io.micrometer:micrometer-registry-prometheus:${Versions.micrometer}")  // expose prometheus metrics in actuator
        add("implementation", "org.springframework.security:spring-security-core:${Versions.springSecurity}")
        add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin:2.12.3")
        add("implementation", "org.slf4j:slf4j-api:${Versions.slf4j}")
        add("implementation", "ch.qos.logback:logback-core:${Versions.logback}")
        add("implementation", "io.projectreactor.kotlin:reactor-kotlin-extensions:${Versions.reactor}")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
        add("testImplementation", "org.mockito.kotlin:mockito-kotlin:3.2.0")
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
            add("implementation", "org.hibernate:hibernate-core:${Versions.hibernate}")
            add("implementation", "org.liquibase:liquibase-core:${Versions.liquibase}")
            add("implementation", "org.springframework.boot:spring-boot-starter-data-jpa:${Versions.springBoot}")
            add("implementation", "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
            add("implementation", "mysql:mysql-connector-java:${Versions.mySql}")
            add("testImplementation", "org.testcontainers:testcontainers:${Versions.testcontainers}")
            add("testImplementation", "org.testcontainers:mysql:${Versions.testcontainers}")
            add("testImplementation", "org.testcontainers:junit-jupiter:${Versions.testcontainers}")
        }
    }

    tasks.named<BootBuildImage>("bootBuildImage") {
        imageName = "ghcr.io/diktat-static-analysis/${project.name}:${project.versionForDockerImages()}"
        environment = mapOf(
            "BP_JVM_VERSION" to Versions.BP_JVM_VERSION,
            "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
            "BPE_APPEND_JAVA_TOOL_OPTIONS" to "-Dreactor.netty.pool.maxIdleTime=60000 -Dreactor.netty.pool.leasingStrategy=lifo"
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
