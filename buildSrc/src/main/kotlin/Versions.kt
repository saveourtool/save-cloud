@file:Suppress("PACKAGE_NAME_MISSING", "CONSTANT_UPPERCASE")

object Versions {
    // core dependencies
    const val jdk = "11"  // jdk version that will be used as kotlin compiler target

    const val BP_JVM_VERSION = "11"  // jvm version for spring boot image build
    const val kotlin = "1.5.0"
    const val ktor = "1.6.0"
    const val coroutines = "1.5.0"
    const val serialization = "1.1.0"
    const val kotlinxDatetime = "0.2.0"
    const val saveCore = "0.1.0-SNAPSHOT"

    // microservices
    const val springBoot = "2.5.0"
    const val reactor = "1.1.3"
    const val springSecurity = "5.5.0"
    const val slf4j = "1.7.30"
    const val logback = "1.2.3"
    const val micrometer = "1.7.0"

    // database
    const val jpa = "1.0.2"
    const val hibernate = "5.4.2.Final"
    const val liquibase = "4.3.5"
    const val mySql = "8.0.20"
    const val liquibaseGradlePlugin = "2.0.4"
    const val testcontainers = "1.15.3"

    // other JVM dependencies
    const val dockerJavaApi = "3.2.8"
    const val jgit = "5.11.1.202105131744-r"
    const val okhttp3 = "4.9.1"

    // frontend
    const val react = "17.0.2"
    const val kotlinJsWrappersSuffix = "-pre.156-kotlin-1.5.0"
    const val kotlinReact = "$react$kotlinJsWrappersSuffix"
}
