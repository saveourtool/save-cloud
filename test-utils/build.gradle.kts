import com.saveourtool.save.buildutils.configureSpotless

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
}

kotlin {
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(Versions.jdk))
    }
}

dependencies {
    implementation(libs.okhttp.mockwebserver)
    implementation(libs.okhttp)
    implementation(libs.slf4j.api)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
}

configureSpotless()
