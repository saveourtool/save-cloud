import com.saveourtool.save.buildutils.configureSigning

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.publishing-configuration")
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
    js(IR) {
        browser()
        binaries.library()
    }

    // setup native compilation
    linuxX64()
    macosX64()

    sourceSets {
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
        val commonMain by getting {
            dependencies {
                implementation(libs.save.common)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.datetime)

                implementation(libs.okio)
                implementation(libs.ktor.client.core)
            }
        }
        commonTest {
            dependencies {
                dependencies {
                    api(libs.kotlin.test)
                    api(libs.kotlinx.coroutines.test)
                    implementation(libs.kotlinx.serialization.json)
                }
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(project.dependencies.platform(libs.spring.boot.dependencies))
                implementation(libs.spring.web)
                implementation(libs.spring.webflux)
                implementation(libs.spring.boot)
                implementation(libs.spring.data.jpa)
                implementation(libs.jackson.module.kotlin)
                implementation(libs.hibernate.jpa21.api)
                api(libs.slf4j.api)
                api(libs.jetbrains.annotations)
                implementation(libs.reactor.kotlin.extensions)
                implementation(libs.commons.compress)
                implementation(libs.validation.api)
                implementation(libs.swagger.annotations)
                implementation(libs.annotation.api)
                implementation(project.dependencies.platform(libs.aws.sdk.bom))
                implementation(libs.aws.sdk.s3)
                implementation(libs.aws.sdk.netty.nio)
                implementation(libs.ktoml.core)
                implementation(libs.ktoml.file)
                api(libs.kotlinx.coroutines.reactor)
            }
        }
        val jvmTest by getting {
            tasks.withType<Test> {
                useJUnitPlatform()
            }
            dependencies {
                api(libs.assertj.core)
                api(libs.junit.jupiter.api)
                api(libs.junit.jupiter.params)
                runtimeOnly(libs.junit.jupiter.engine)
            }
        }

        val linuxX64Main by getting
        val macosX64Main by getting

        val nativeMain by creating {
            dependsOn(commonMain)
            linuxX64Main.dependsOn(this)
            macosX64Main.dependsOn(this)

            dependencies {
                implementation(libs.ktoml.core)
                implementation(libs.ktoml.file)
            }
        }
    }
}

configureSigning()
