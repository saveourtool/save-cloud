

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("com.saveourtool.save.buildutils.code-quality-convention")
}

kotlin {
    val nativeTarget = when (System.getProperty("os.name")) {
        "Mac OS X" -> macosX64("native")
        "Linux" -> linuxX64("native")
        else -> throw GradleException("Host OS is not supported.")
    }

    nativeTarget.apply {
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

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation(libs.save.common)
                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
            }
        }

        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
