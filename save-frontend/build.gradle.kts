plugins {
    kotlin("js")
}

kotlin {
    js(LEGACY) {  // as of kotlin 1.4.21, IR compilation leads to errors in React in production webpack
        browser {
            repositories {
                jcenter()
                maven("https://kotlin.bintray.com/js-externals")
            }
        }
        binaries.executable()  // already default for LEGACY, but explicitly needed for IR
        sourceSets["main"].dependencies {
            implementation(project(":save-common"))

            // devDependencies for webpack
            compileOnly(devNpm("node-sass", "*"))
            compileOnly(devNpm("sass-loader", "10.1.1"))  // todo: there is some issue with newer versions
            compileOnly(devNpm("style-loader", "*"))
            compileOnly(devNpm("css-loader", "*"))
            compileOnly(devNpm("url-loader", "*"))
            compileOnly(devNpm("file-loader", "*"))

            // web-specific dependencies
            // todo: bootstrap and jquery.easing need jquery, but if it's loaded from webpack, they can't use it
            //  and for some reason neither can be loaded from webpack. So they reside in html, bootstrap is here for scss.
            compileOnly(npm("bootstrap", "4.5.3"))
            compileOnly(npm("@fortawesome/fontawesome-free", "5.15.1"))  // needed to copy fonts to resources, not needed in runtime
            compileOnly("kotlin.js.externals:kotlin-js-jquery:3.2.0-0")  // todo: use react instead of jquery
            implementation("org.jetbrains:kotlin-react:${Versions.kotlinReact}")
            implementation("org.jetbrains:kotlin-react-dom:${Versions.kotlinReact}")
            implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-pre.142-kotlin-1.4.21")
            implementation("org.jetbrains:kotlin-react-table:7.6.3-pre.143-kotlin-1.4.21")
            implementation(npm("react", Versions.react))
            implementation(npm("react-dom", Versions.react))

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")
        }
        sourceSets["test"].dependencies {
            implementation(kotlin("test-js"))
        }
    }
}

// generate kotlin file with project version to include in web page
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

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
val generatedKotlinSrc = kotlin.sourceSets.create("jsGenerated") {
    kotlin.srcDir("$buildDir/generated/src")
}
kotlin.sourceSets.getByName("main").dependsOn(generatedKotlinSrc)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}

val copyWebfontsTaskProvider = tasks.register("copyWebfonts", Copy::class) {
    // add fontawesome font into the build
    dependsOn(rootProject.tasks.getByName("kotlinNpmInstall"))  // to have dependencies downloaded
    from("$rootDir/build/js/node_modules/@fortawesome/fontawesome-free/webfonts")
    into("$buildDir/processedResources/js/main/webfonts")
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>().forEach {
    it.dependsOn(copyWebfontsTaskProvider)
    it.doFirst {
        val additionalWebpackResources = fileTree("$buildDir/processedResources/js/main/") {
            include("scss/**")
            include("webfonts/**")
        }
        copy {
            from(additionalWebpackResources)
            into("${rootProject.buildDir}/js/packages/save-${project.name}")
        }
    }
    it.doLast {
        // remove resources that have been bundled into frontend.js
        file("${it.destinationDirectory}/scss").deleteRecursively()
        file("${it.destinationDirectory}/webfonts").deleteRecursively()
    }
}

detekt {
    config.setFrom(config.plus(file("detekt.yml")))
}