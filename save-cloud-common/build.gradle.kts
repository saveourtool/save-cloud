plugins {
    kotlin("multiplatform")
    kotlin("kapt")
    alias(libs.plugins.kotlin.plugin.serialization)
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlin.plugin.jpa)
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
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
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
                implementation(libs.hibernate.jpa21.api)

                compileOnly("org.mapstruct:mapstruct:1.4.2.Final")
                configurations.get("kapt").dependencies.add(
                    project.dependencies.create("org.mapstruct:mapstruct-processor:1.4.2.Final")
                )
            }
        }
    }
}
