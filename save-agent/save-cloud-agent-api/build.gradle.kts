import com.saveourtool.save.buildutils.configureSigning

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.publishing-configuration")
}
kotlin {
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
        useCommonJs()
    }

    // setup native compilation
    linuxX64()
    macosX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.save.common)
                api(project(":save-agent:save-cloud-common-api"))
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)

                implementation(libs.okio)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
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
        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                api(libs.slf4j.api)
                api(libs.jetbrains.annotations)
                implementation(libs.commons.compress)
                api(libs.kotlinx.coroutines.reactor)
            }
        }
        @Suppress("UNUSED_VARIABLE")
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
    }
}

configureSigning()
