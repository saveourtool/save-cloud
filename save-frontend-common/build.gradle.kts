import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    kotlin("multiplatform")
    id("com.saveourtool.save.buildutils.build-frontend-image-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    id("com.saveourtool.save.buildutils.save-cloud-version-file-configuration")
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    js(IR) {
        browser {
            testTask(Action {
                useKarma {
                    when (properties["save.profile"]) {
                        "dev" -> {
                            useChrome()
                            // useFirefox()
                        }

                        null -> useChromeHeadless()
                    }
                }
            })
            commonWebpackConfig(Action {
                cssSupport {
                    enabled.set(true)
                }
            })
        }
        // kotlin-wrapper migrates to commonjs and missed @JsNonModule annotations
        // https://github.com/JetBrains/kotlin-wrappers/issues/1935
        useCommonJs()
        binaries.executable()  // already default for LEGACY, but explicitly needed for IR
        sourceSets.all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.js.ExperimentalJsExport")
            }
        }
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(projects.saveCloudCommon)

                implementation(project.dependencies.platform(libs.kotlin.wrappers.bom))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-tanstack-react-table")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui-icons")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-mui")
                implementation("io.github.petertrr:kotlin-multiplatform-diff-js:0.4.0")

                implementation(libs.save.common)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.http)

                compileOnly(devNpm("sass", "^1.43.0"))
                compileOnly(devNpm("sass-loader", "^12.0.0"))
                compileOnly(devNpm("style-loader", "^3.3.1"))
                compileOnly(devNpm("css-loader", "^6.5.0"))
                compileOnly(devNpm("file-loader", "^6.2.0"))
                // https://getbootstrap.com/docs/4.0/getting-started/webpack/#importing-precompiled-sass
                compileOnly(devNpm("postcss-loader", "^6.2.1"))
                compileOnly(devNpm("postcss", "^8.2.13"))
                // See https://stackoverflow.com/a/72828500; newer versions are supported only for Bootstrap 5.2+
                compileOnly(devNpm("autoprefixer", "10.4.5"))
                compileOnly(devNpm("webpack-bundle-analyzer", "^4.5.0"))
                compileOnly(devNpm("mini-css-extract-plugin", "^2.6.0"))
                compileOnly(devNpm("html-webpack-plugin", "^5.5.0"))

                // web-specific dependencies
                implementation(npm("@fortawesome/fontawesome-svg-core", "^1.2.36"))
                implementation(npm("@fortawesome/free-solid-svg-icons", "5.15.3"))
                implementation(npm("@fortawesome/free-brands-svg-icons", "5.15.3"))
                implementation(npm("@fortawesome/react-fontawesome", "^0.1.16"))
                implementation(npm("devicon", "^2.15.1"))
                implementation(npm("animate.css", "^4.1.1"))
                implementation(npm("react-scroll-motion", "^0.3.0"))
                implementation(npm("react-spinners", "0.13.0"))
                implementation(npm("react-tsparticles", "1.42.1"))
                implementation(npm("tsparticles", "2.1.3"))
                implementation(npm("jquery", "3.6.0"))
                // BS5: implementation(npm("@popperjs/core", "2.11.0"))
                implementation(npm("popper.js", "1.16.1"))
                // BS5: implementation(npm("bootstrap", "5.0.1"))
                implementation(npm("react-calendar", "^3.8.0"))
                implementation(npm("bootstrap", "^4.6.0"))
                implementation(npm("react", "^18.0.0"))
                implementation(npm("react-dom", "^18.0.0"))
                implementation(npm("react-modal", "^3.0.0"))
                implementation(npm("os-browserify", "^0.3.0"))
                implementation(npm("path-browserify", "^1.0.1"))
                implementation(npm("react-minimal-pie-chart", "^8.2.0"))
                implementation(npm("lodash.debounce", "^4.0.8"))
                implementation(npm("react-markdown", "^8.0.3"))
                implementation(npm("rehype-highlight", "^5.0.2"))
                implementation(npm("react-ace", "^10.1.0"))
                implementation(npm("react-avatar-image-cropper", "^1.4.2"))
                implementation(npm("react-circle", "^1.1.1"))
                implementation(npm("react-diff-viewer-continued", "^3.2.6"))
                implementation(npm("react-json-view", "^1.21.3"))
                implementation(npm("multi-range-slider-react", "^2.0.5"))
                // react-sigma
                implementation(npm("@react-sigma/core", "^3.1.0"))
                implementation(npm("sigma", "^2.4.0"))
                implementation(npm("graphology", "^0.25.1"))
                implementation(npm("graphology-layout", "^0.6.1"))
                implementation(npm("graphology-layout-forceatlas2", "^0.10.1"))
                implementation(npm("@react-sigma/layout-core", "^3.1.0"))
                implementation(npm("@react-sigma/layout-random", "^3.1.0"))
                implementation(npm("@react-sigma/layout-circular", "^3.1.0"))
                implementation(npm("@react-sigma/layout-forceatlas2", "^3.1.0"))
                implementation(npm("react-graph-viz-engine", "^0.1.0"))
                implementation(npm("cytoscape", "^3.25.0"))
                // translation
                implementation(npm("i18next", "^23.4.5"))
                implementation(npm("react-i18next", "^13.2.0"))
                implementation(npm("i18next-http-backend", "^2.2.2"))
                implementation(npm("js-cookie", "^3.0.5"))
                // transitive dependencies with explicit version ranges required for security reasons
                compileOnly(devNpm("minimist", "^1.2.6"))
                compileOnly(devNpm("async", "^2.6.4"))
                compileOnly(devNpm("follow-redirects", "^1.14.8"))
            }
        }
        val jsTest by getting {
            dependencies {
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
}

kotlin.sourceSets.getByName("jsMain") {
    kotlin.srcDir(
        tasks.named("generateSaveCloudVersionFile").map {
            it.outputs.files.singleFile
        }
    )
}
detekt {
    config.setFrom(config.plus(file("detekt.yml")))
}
