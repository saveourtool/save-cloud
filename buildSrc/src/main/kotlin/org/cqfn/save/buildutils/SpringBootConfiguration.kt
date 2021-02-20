package org.cqfn.save.buildutils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.springframework.boot.gradle.dsl.SpringBootExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

fun Project.configureSpringBoot() {
    apply<SpringBootPlugin>()

    dependencies {
        add("implementation", "org.springframework.boot:spring-boot-starter-webflux:${Versions.springBoot}")
        add("implementation", "org.springframework.boot:spring-boot-starter-actuator:${Versions.springBoot}")
        add("implementation", "io.micrometer:micrometer-registry-prometheus:${Versions.micrometer}")  // expose prometheus metrics in actuator
        add("implementation", "org.springframework.security:spring-security-core:${Versions.springSecurity}")
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test:${Versions.springBoot}")
    }

    configure<SpringBootExtension> {
        buildInfo()  // configures `bootBuildInfo` task, which creates META-INF/build-info.properties file
    }

    tasks.named<BootBuildImage>("bootBuildImage") {
        dependsOn(rootProject.tasks.getByName("startLocalDockerRegistry"))
        // `host.docker.internal` for win 10?
        imageName = "127.0.0.1:6000/${project.name}:${project.version}"
        environment = mapOf("BP_JVM_VERSION" to Versions.BP_JVM_VERSION)
        isPublish = false
    }
}
