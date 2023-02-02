import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

kotlin {
    val configureNative: Action<KotlinNativeTargetWithHostTests> = Action {
        binaries {
            all {
                binaryOptions["memoryModel"] = "experimental"
                freeCompilerArgs = freeCompilerArgs + "-Xruntime-logs=gc=info"
            }
            executable {
                entryPoint = "com.saveourtool.save.demo.agent.main"
                baseName = "save-demo-agent"
            }
        }
    }
    macosX64(configureNative)
    linuxX64(configureNative)

    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation(libs.save.common)
                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.kotlinx.serialization.properties)

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
            }
        }
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }

        val nativeTest by creating {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val macosX64Test by getting {
            dependsOn(nativeTest)
        }
        val linuxX64Test by getting {
            dependsOn(nativeTest)
        }
    }
}
