import com.saveourtool.save.buildutils.configureSpotless
import com.saveourtool.save.buildutils.pathToSaveCliVersion
import com.saveourtool.save.buildutils.readSaveCliVersion
import org.gradle.api.tasks.TaskProvider
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Versions.jdk
            }
        }
    }

    // Create a target for the host platform.
    val linuxTarget = linuxX64 {
        binaries.executable {
            entryPoint = "com.saveourtool.save.agent.main"
            baseName = "save-agent"
        }
        binaries.all {
            binaryOptions["memoryModel"] = "experimental"
            freeCompilerArgs = freeCompilerArgs + "-Xruntime-logs=gc=info"
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.save.common)
                implementation(projects.saveCloudCommon)
                implementation(libs.save.core)
                implementation(libs.save.plugins.fix)
                implementation(libs.save.reporters)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.logging)
                implementation(libs.kotlinx.serialization.properties)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
            }
        }
        val commonTest by getting

        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.apache)
                implementation(libs.commons.compress)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-engine:5.9.1")
            }
        }

        val linuxX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.curl)
                implementation(libs.kotlinx.coroutines.core.linuxx64)
            }
        }
        val linuxX64Test by getting {
            dependencies {
                implementation(libs.ktor.client.mock)
            }
        }
    }

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val linkTask: TaskProvider<KotlinNativeLink> = tasks.named<KotlinNativeLink>("linkReleaseExecutableLinuxX64")

    val copyAgentDistribution by tasks.registering(Jar::class) {
        dependsOn(linkTask)
        archiveClassifier.set("distribution")
        from(linkTask.flatMap { it.outputFile })
        from(file("$projectDir/src/linuxX64Main/resources/agent.properties"))
    }
    val distribution by configurations.creating
    artifacts.add(distribution.name, copyAgentDistribution.flatMap { it.archiveFile }) {
        builtBy(copyAgentDistribution)
    }

    // code coverage: https://github.com/JetBrains/kotlin/blob/master/kotlin-native/CODE_COVERAGE.md,
    // https://github.com/JetBrains/kotlin/blob/master/kotlin-native/samples/coverage/build.gradle.kts
    if (false) {
        // this doesn't work for 1.4.31, maybe will be fixed later
        linuxTarget.binaries.getTest("DEBUG").apply {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xlibrary-to-cover=${linuxTarget.compilations["main"].output.classesDirs.singleFile.absolutePath}")
        }
        val createCoverageReportTask by tasks.creating {
            dependsOn("${linuxTarget.name}Test")
            description = "Create coverage report"

            doLast {
                val testDebugBinary = linuxTarget.binaries.getTest("DEBUG").outputFile
                val llvmPath = "${System.getenv()["HOME"]}/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin"
                exec {
                    commandLine("$llvmPath/llvm-profdata", "merge", "$testDebugBinary.profraw", "-o", "$testDebugBinary.profdata")
                }
                exec {
                    commandLine("$llvmPath/llvm-cov", "show", testDebugBinary, "-instr-profile", "$testDebugBinary.profdata")
                }
            }
        }
        tasks.named("${linuxTarget.name}Test") {
            finalizedBy(createCoverageReportTask)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinTest> {
    testLogging.showStandardStreams = true
}
configureSpotless()

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    dependsOn(rootProject.tasks.named("getSaveCliVersion"))
    inputs.file(pathToSaveCliVersion)
    inputs.property("project version", version.toString())
    outputs.file(versionsFile)

    doFirst {
        val saveCliVersion = readSaveCliVersion()
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "$saveCliVersion"
            internal const val SAVE_CLOUD_VERSION = "$version"

            """.trimIndent()
        )
    }
}
val generatedKotlinSrc = kotlin.sourceSets.create("commonGenerated") {
    kotlin.srcDir("$buildDir/generated/src")
}
kotlin.sourceSets.getByName("commonMain").dependsOn(generatedKotlinSrc)
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    dependsOn(generateVersionFileTaskProvider)
}

/*
 * On Windows, it's impossible to link a Linux executable against
 * `io.ktor:ktor-client-curl` because `-lcurl` is not found by `ld`.
 */
tasks.named("linkDebugExecutableLinuxX64") {
    onlyIf {
        !DefaultNativePlatform.getCurrentOperatingSystem().isWindows
    }
}

tasks.named("linkReleaseExecutableLinuxX64") {
    onlyIf {
        !DefaultNativePlatform.getCurrentOperatingSystem().isWindows
    }
}

tasks.named("linkDebugTestLinuxX64") {
    onlyIf {
        !DefaultNativePlatform.getCurrentOperatingSystem().isWindows
    }
}
