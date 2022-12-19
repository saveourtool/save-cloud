import com.saveourtool.save.buildutils.*
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
    id("com.saveourtool.save.buildutils.code-quality-convention")
    alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
    ivy {
        setUrl("https://download.eclipse.org/tools/cdt/releases/10.3/cdt-10.3.2/plugins")
        metadataSources {
            artifact()
        }
        patternLayout {
            artifact("/[organisation].[module]_[revision].[ext]")
        }
    }
    mavenCentral()
}

kotlin {
    sourceSets {
        val commoMain by creating {
            dependencies {
                implementation(projects.saveCloudCommon)
            }
        }
    }
}

dependencies {
    implementation("org.neo4j:neo4j-ogm-bolt-driver:3.2.38")
    implementation("org.neo4j:neo4j-ogm-core:3.2.38")
    implementation(libs.spring.data.neo4j)
    api(libs.arrow.kt.core)

    implementation(libs.cpg.core) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }
    implementation(libs.cpg.python) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }
}

// This is a special hack for macOS and JEP, see: https://github.com/Fraunhofer-AISEC/cpg/pull/995/files
val os = System.getProperty("os.name")
run {
    if (os.contains("mac", ignoreCase = true)) {
        tasks.withType<BootRun> {
            environment("CPG_JEP_LIBRARY", "/opt/homebrew/lib/python3.10/site-packages/jep/libjep.jnilib")
        }
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>().configureEach {
    from("requirements.txt")
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
    environment["BPE_CPG_JEP_LIBRARY"] = "/layers/paketo-buildpacks_pip-install/packages/lib/python3.10/site-packages/jep/libjep.so"
    environment["BP_CPYTHON_VERSION"] = "3.10"
    environment["BP_JVM_TYPE"] = "JDK"
}
