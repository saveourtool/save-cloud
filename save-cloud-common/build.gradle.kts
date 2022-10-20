import com.saveourtool.save.buildutils.configurePublishing
import com.saveourtool.save.buildutils.configureSpotless

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
    `maven-publish`
}
kotlin {
    allOpen {
        annotation("javax.persistence.Entity")
        annotation("org.springframework.stereotype.Service")
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Versions.jdk
            }
        }
    }
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
    js(BOTH).browser()

    // setup native compilation
    linuxX64()

    sourceSets {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        commonMain {
            dependencies {
                implementation(libs.save.common)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.datetime)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project.dependencies.platform(libs.spring.boot.dependencies))
                implementation(libs.spring.security.core)
                implementation(libs.spring.security.web)
                implementation(libs.spring.security.config)
                implementation(libs.spring.web)
                implementation(libs.spring.webflux)
                implementation(libs.spring.boot)
                implementation(libs.spring.boot.autoconfigure)
                implementation(libs.spring.data.jpa)
                implementation(libs.jackson.module.kotlin)
                implementation(libs.hibernate.jpa21.api)
                api(libs.slf4j.api)
                implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
                implementation(libs.commons.compress)
                implementation(libs.validation.api)
                implementation(libs.swagger.annotations)
                implementation(libs.fabric8.kubernetes.client)
            }
        }
        val jvmTest by getting {
            tasks.withType<Test> {
                useJUnitPlatform()
            }
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

configureSpotless()
configurePublishing()
