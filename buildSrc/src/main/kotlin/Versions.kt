@file:Suppress("CONSTANT_UPPERCASE")

object Versions {
    // core dependencies
    const val jdk = "11"  // jdk version that will be used as kotlin compiler target

    const val BP_JVM_VERSION = "11"  // jvm version for spring boot image build
    const val kotlin = "1.5.31"
    const val ktor = "1.6.3"
    const val coroutines = "1.5.2"
    const val serialization = "1.2.2"
    const val kotlinxDatetime = "0.2.1"
    const val saveCore = "0.2.1"
    const val okio = "3.0.0-alpha.10"

    // microservices
    const val springBoot = "2.5.4"
    const val reactor = "1.1.4"
    const val springSecurity = "5.5.2"
    const val slf4j = "1.7.32"
    const val logback = "1.2.3"
    const val micrometer = "1.7.4"

    // database
    const val jpa = "1.0.2"
    const val hibernate = "5.4.2.Final"
    const val liquibase = "4.3.5"
    const val mySql = "8.0.20"
    const val liquibaseGradlePlugin = "2.0.4"
    const val testcontainers = "1.16.0"

    // other JVM dependencies
    const val dockerJavaApi = "3.2.12"
    const val jgit = "5.13.0.202109080827-r"
    const val okhttp3 = "4.9.1"

    // frontend
    const val react = "17.0.2"
    const val kotlinJsWrappersSuffix = "-pre.247-kotlin-1.5.31"
    const val kotlinReact = "$react$kotlinJsWrappersSuffix"
}
