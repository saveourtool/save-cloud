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

dependencies {
    implementation(projects.saveCloudCommon)
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

configureJacoco()
configureSpotless()

// This is a special hack for macOS and JEP, see: https://github.com/Fraunhofer-AISEC/cpg/pull/995/files
val os = System.getProperty("os.name")
run {
    if (os.contains("mac", ignoreCase = true)) {
        tasks.withType<BootRun> {
            environment("CPG_JEP_LIBRARY", "/opt/homebrew/lib/python3.10/site-packages/jep/libjep.jnilib")
        }
    }
}
