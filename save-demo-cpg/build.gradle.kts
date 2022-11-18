import com.saveourtool.save.buildutils.*

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
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
    implementation(libs.spring.data.neo4j)
    implementation(libs.cpg.core) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }
    implementation(libs.cpg.language.python) {
        exclude("org.apache.logging.log4j", "log4j-slf4j2-impl")
    }
}

configureJacoco()
configureSpotless()
