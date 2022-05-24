import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

plugins {
    kotlin("js")
}

rootProject.plugins.withType<NodeJsRootPlugin> {
    rootProject.the<NodeJsRootExtension>().nodeVersion = "16.13.1"
}

dependencies {
    implementation(projects.saveCloudCommon)

    implementation(enforcedPlatform(libs.kotlin.wrappers.bom))
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-legacy")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom-legacy")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-table")

    implementation(libs.save.common)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
}

kotlin {
    js(LEGACY) {
        // as for `-pre.148-kotlin-1.4.21`, react-table gives errors with IR
        browser {
            repositories {
                mavenCentral()
                maven("https://oss.sonatype.org/content/repositories/snapshots") {
                    content {
                        includeGroup("com.saveourtool.save")
                    }
                }
            }

            testTask {
                useKarma {
                    when (properties["save.profile"]) {
                        "dev" -> {
                            useChrome()
                            // useFirefox()
                        }
                        null -> useChromeHeadless()
                    }
                }
            }
        }
        binaries.executable()  // already default for LEGACY, but explicitly needed for IR
        sourceSets.all {
            languageSettings.optIn("kotlin.RequiresOptIn")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
        sourceSets["main"].dependencies {
            compileOnly(devNpm("sass", "^1.43.0"))
            compileOnly(devNpm("sass-loader", "^12.0.0"))
            compileOnly(devNpm("style-loader", "^3.3.1"))
            compileOnly(devNpm("css-loader", "^6.5.0"))
            compileOnly(devNpm("file-loader", "^6.2.0"))
            // https://getbootstrap.com/docs/4.0/getting-started/webpack/#importing-precompiled-sass
            compileOnly(devNpm("postcss-loader", "^6.2.1"))
            compileOnly(devNpm("postcss", "^8.2.13"))
            compileOnly(devNpm("autoprefixer", ">9"))
            compileOnly(devNpm("webpack-bundle-analyzer", "^4.5.0"))
            compileOnly(devNpm("mini-css-extract-plugin", "^2.6.0"))

            // web-specific dependencies
            implementation(npm("@fortawesome/fontawesome-svg-core", "^1.2.36"))
            implementation(npm("@fortawesome/free-solid-svg-icons", "5.15.3"))
            implementation(npm("@fortawesome/free-brands-svg-icons", "5.15.3"))
            implementation(npm("@fortawesome/react-fontawesome", "^0.1.16"))
            implementation(npm("jquery", "3.6.0"))
            // BS5: implementation(npm("@popperjs/core", "2.11.0"))
            implementation(npm("popper.js", "1.16.1"))
            // BS5: implementation(npm("bootstrap", "5.0.1"))
            implementation(npm("bootstrap", "^4.6.0"))
            implementation(npm("react", "^18.0.0"))
            implementation(npm("react-dom", "^18.0.0"))
            implementation(npm("react-modal", "^3.0.0"))
            implementation(npm("os-browserify", "^0.3.0"))
            implementation(npm("path-browserify", "^1.0.1"))
            implementation(npm("react-minimal-pie-chart", "^8.2.0"))
            implementation(npm("lodash.debounce", "^4.0.8"))

            // transitive dependencies with explicit version ranges required for security reasons
            compileOnly(devNpm("minimist", "^1.2.6"))
            compileOnly(devNpm("async", "^2.6.4"))
            compileOnly(devNpm("follow-redirects", "^1.14.8"))
        }
        sourceSets["test"].dependencies {
            implementation(kotlin("test-js"))
            implementation(devNpm("jsdom", "^19.0.0"))
            implementation(devNpm("global-jsdom", "^8.4.0"))
            implementation(devNpm("@testing-library/react", "^13.2.0"))
            implementation(devNpm("@testing-library/user-event", "^14.0.0"))
            implementation(devNpm("karma-mocha-reporter", "^2.0.0"))
            implementation(devNpm("istanbul-instrumenter-loader", "^3.0.1"))
            implementation(devNpm("karma-coverage-istanbul-reporter", "^3.0.3"))
            implementation(devNpm("msw", "^0.40.0"))
        }
    }
}

rootProject.plugins.withType(NodeJsRootPlugin::class.java) {
    rootProject.the<NodeJsRootExtension>().versions.apply {
        // workaround for continuous work of WebPack: (https://github.com/webpack/webpack-cli/issues/2990)
        webpackCli.version = "4.9.0"
        webpackDevServer.version = "^4.9.0"
        // override default version from KGP for security reasons
        karma.version = "^6.3.14"
        mocha.version = "9.2.0"
    }
}
// store yarn.lock in the root directory
rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension> {
    lockFileDirectory = rootProject.projectDir
}

// generate kotlin file with project version to include in web page
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    inputs.property("project version", version.toString())
    outputs.file(versionsFile)

    doFirst {
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_VERSION = "$version"

            """.trimIndent()
        )
    }
}
kotlin.sourceSets.getByName("main") {
    kotlin.srcDir("$buildDir/generated/src")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
    dependsOn(generateVersionFileTaskProvider)
    inputs.file("$buildDir/generated/src/generated/Versions.kt")
}
tasks.named<org.gradle.jvm.tasks.Jar>("kotlinSourcesJar") {
    dependsOn(generateVersionFileTaskProvider)
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>().forEach { kotlinWebpack ->
    kotlinWebpack.doFirst {
        val additionalWebpackResources = fileTree("$buildDir/processedResources/js/main/") {
            include("scss/**")
        }
        copy {
            from(additionalWebpackResources)
            into("${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}")
        }
    }
}

val distribution: Configuration by configurations.creating
val distributionJarTask by tasks.registering(Jar::class) {
    dependsOn(":save-frontend:browserDistribution")
    archiveClassifier.set("distribution")
    from("$buildDir/distributions")
    into("static")
    exclude("scss")
}
artifacts.add(distribution.name, distributionJarTask.get().archiveFile) {
    builtBy(distributionJarTask)
}

detekt {
    config.setFrom(config.plus(file("detekt.yml")))
}
