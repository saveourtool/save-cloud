import org.cqfn.save.buildutils.getSaveCliVersion

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    // Create a target for the host platform.
    val hostTarget = linuxX64 {
        binaries.executable {
            entryPoint = "org.cqfn.save.agent.main"
            baseName = "save-agent"
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
        val linuxX64Main by getting {
            dependencies {
                implementation(projects.saveCloudCommon)
                implementation(libs.save.core)
                implementation(libs.save.plugins.fix)
                implementation(libs.save.reporters)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.curl)
                implementation(libs.ktor.client.serialization)
//                implementation("io.ktor:ktor-client-encoding:${Versions.ktor}")
                implementation(libs.kotlinx.serialization.properties)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
            }
        }
        val linuxX64Test by getting {
            dependencies {
                implementation(libs.ktor.client.mock)
            }
        }
    }

    val distribution by configurations.creating
    val copyAgentDistribution by tasks.registering(Jar::class) {
        dependsOn("linkReleaseExecutableLinuxX64")
        archiveClassifier.set("distribution")
        from(file("$buildDir/bin/linuxX64/releaseExecutable")) {
            include("*")
        }
        from(file("$projectDir/src/linuxX64Main/resources/agent.properties"))
    }
    artifacts.add(distribution.name, file("$buildDir/libs/${project.name}-${project.version}-distribution.jar")) {
        builtBy(copyAgentDistribution)
    }

    // code coverage: https://github.com/JetBrains/kotlin/blob/master/kotlin-native/CODE_COVERAGE.md,
    // https://github.com/JetBrains/kotlin/blob/master/kotlin-native/samples/coverage/build.gradle.kts
    if (false) {
        // this doesn't work for 1.4.31, maybe will be fixed later
        hostTarget.binaries.getTest("DEBUG").apply {
            freeCompilerArgs = freeCompilerArgs + listOf("-Xlibrary-to-cover=${hostTarget.compilations["main"].output.classesDirs.singleFile.absolutePath}")
        }
        val createCoverageReportTask by tasks.creating {
            dependsOn("${hostTarget.name}Test")
            description = "Create coverage report"

            doLast {
                val testDebugBinary = hostTarget.binaries.getTest("DEBUG").outputFile
                val llvmPath = "${System.getenv()["HOME"]}/.konan/dependencies/clang-llvm-8.0.0-linux-x86-64/bin"
                exec {
                    commandLine("$llvmPath/llvm-profdata", "merge", "$testDebugBinary.profraw", "-o", "$testDebugBinary.profdata")
                }
                exec {
                    commandLine("$llvmPath/llvm-cov", "show", testDebugBinary, "-instr-profile", "$testDebugBinary.profdata")
                }
            }
        }
        tasks.getByName("${hostTarget.name}Test").finalizedBy(createCoverageReportTask)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinTest> {
    testLogging.showStandardStreams = true
}

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    val saveCliVersion = getSaveCliVersion()
    inputs.property("Version of save-cli", saveCliVersion)
    inputs.property("project version", version.toString())
    outputs.file(versionsFile)

    doFirst {
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
kotlin.sourceSets.getByName("linuxX64Main").dependsOn(generatedKotlinSrc)
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    dependsOn(generateVersionFileTaskProvider)
}
