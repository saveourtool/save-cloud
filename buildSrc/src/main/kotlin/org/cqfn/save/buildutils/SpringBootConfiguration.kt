package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension

fun Project.configureSpringBoot(withSpringDataJpa: Boolean = false) {
    apply<SpringBootPlugin>()

    dependencies {
        add("implementation", "org.springframework.boot:spring-boot-starter-webflux:${Versions.springBoot}")
        add("implementation", "org.springframework.boot:spring-boot-starter-actuator:${Versions.springBoot}")
        add("implementation", "io.micrometer:micrometer-registry-prometheus:${Versions.micrometer}")  // expose prometheus metrics in actuator
        add("implementation", "org.springframework.security:spring-security-core:${Versions.springSecurity}")
        add("implementation", "org.slf4j:slf4j-api:${Versions.slf4j}")
        add("implementation", "ch.qos.logback:logback-core:${Versions.logback}")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
    }

    configure<SpringBootExtension> {
        buildInfo()  // configures `bootBuildInfo` task, which creates META-INF/build-info.properties file
    }

    if (withSpringDataJpa) {
        apply<AllOpenGradleSubplugin>()

        configure<AllOpenExtension>() {
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
        dependsOn(rootProject.tasks.getByName("startLocalDockerRegistry"))
        // `host.docker.internal` for win 10?
        imageName = "127.0.0.1:6000/${project.name}:${project.versionForDockerImages()}"
        environment = mapOf("BP_JVM_VERSION" to Versions.BP_JVM_VERSION)
        isPublish = false
    }
}
