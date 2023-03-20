import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.springframework.boot.gradle.tasks.run.BootRun

@Suppress("DSL_SCOPE_VIOLATION", "RUN_IN_SCRIPT")  // https://github.com/gradle/gradle/issues/22797
plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/11.0/cdt-11.0.0/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
    mavenCentral()
}
val jepArchive by configurations.creating

@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
val resolveJep: TaskProvider<Copy> = tasks.register<Copy>("resolveJep") {
    destinationDir = file("$buildDir/distros/jep-distro")
    from(tarTree(jepArchive.singleFile))
}

dependencies {
    implementation(projects.saveCloudCommon)
    api(libs.arrow.kt.core)

    implementation(libs.cpg.core) {
        // we use logback
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.apache.logging.log4j", "log4j-core")
        // we don't migrate to slf4j 2.x yet
        exclude("org.slf4j", "slf4j-api")
        // FIXME: neo4j changed encryption by default, need to migrate
        exclude("org.neo4j", "neo4j-ogm-core")
    }
    implementation(libs.cpg.python) {
        // we use logback
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.apache.logging.log4j", "log4j-core")
        // we don't migrate to slf4j 2.x yet
        exclude("org.slf4j", "slf4j-api")
        // FIXME: neo4j changed encryption by default, need to migrate
        exclude("org.neo4j", "neo4j-ogm-core")
    }
    implementation(libs.neo4j.ogm.bolt.driver) {
        // we use logback
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
        exclude("org.apache.logging.log4j", "log4j-core")
        // we don't migrate to slf4j 2.x yet
        exclude("org.slf4j", "slf4j-api")
    }

    jepArchive("com.icemachined:jep-distro:4.1.1@tgz")
    runtimeOnly(fileTree("$buildDir/distros/jep-distro").apply {
        builtBy(resolveJep)
    })
}

// This is a special hack for macOS and JEP, see: https://github.com/Fraunhofer-AISEC/cpg/pull/995/files
run {
    val jepLibraryFile =
            with(DefaultNativePlatform.getCurrentOperatingSystem()) {
                when {
                    isMacOsX -> "libjep.jnilib"
                    isWindows -> "jep.dll"
                    isLinux -> "libjep.so"
                    else -> throw Exception("Unsupported operating system: ${System.getProperty("os.name")}")
                }
            }

    tasks.withType<BootRun> {
        environment("CPG_JEP_LIBRARY", "$buildDir/distros/jep-distro/jep/$jepLibraryFile")
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>().configureEach {
    from("requirements.txt")
    from("$buildDir/distros")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>().configureEach {
    builder = "ghcr.io/saveourtool/builder:base-plus-gcc"
    buildpacks(
        listOf(
            "paketo-buildpacks/java",
            "paketo-buildpacks/python",
            "paketo-buildpacks/pip",
        )
    )
    environment["BPE_CPG_JEP_LIBRARY"] = "jep-distro/jep/libjep.so"
    environment["BP_CPYTHON_VERSION"] = "3.10"
    environment["BP_JVM_TYPE"] = "JDK"
}
