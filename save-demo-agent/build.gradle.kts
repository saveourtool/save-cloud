import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

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
        val macosX64Main by getting
        val linuxX64Main by getting

        val nativeMain by creating {
            macosX64Main.dependsOn(this)
            linuxX64Main.dependsOn(this)

            dependencies {
                implementation(projects.saveCloudCommon)
                implementation(libs.save.common)
                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.ktor.serialization.kotlinx.json)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)

                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)
            }
        }

        val macosX64Test by getting
        val linuxX64Test by getting

        val nativeTest by creating {
            macosX64Test.dependsOn(this)
            linuxX64Test.dependsOn(this)
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val linkTask: TaskProvider<KotlinNativeLink> = tasks.named<KotlinNativeLink>("linkReleaseExecutableLinuxX64")

    val copyDemoAgentDistribution by tasks.registering(Jar::class) {
        dependsOn(linkTask)
        archiveClassifier.set("distribution")
        from(linkTask.flatMap { it.outputFile })
        from(file("$projectDir/src/nativeMain/resources/agent.toml"))
    }
    val distribution by configurations.creating
    artifacts.add(distribution.name, copyDemoAgentDistribution.flatMap { it.archiveFile }) {
        builtBy(copyDemoAgentDistribution)
    }
}
