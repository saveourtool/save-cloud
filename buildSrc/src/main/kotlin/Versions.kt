object Versions {
    // core dependencies
    const val jdk = "11"  // jdk version that will be used as kotlin compiler target
    const val BP_JVM_VERSION = "11"  // jvm version for spring boot image build
    const val kotlin = "1.4.32"
    const val ktor = "1.5.3"
    const val coroutines = "1.4.3"
    const val serialization = "1.1.0"  // serialization is compiled by 1.4.30 since version 1.1.0 and for native ABI is different. We can update serialization only after we update kotlin.
    const val kotlinxDatetime = "0.1.1"
    const val saveCore = "0.1.0-alpha.3"

    // microservices
    const val springBoot = "2.4.4"
    const val reactor = "1.1.3"
    const val springSecurity = "5.4.5"
    const val slf4j = "1.7.30"
    const val logback = "1.2.3"
    const val micrometer = "1.6.5"
    // database
    const val jpa = "1.0.2"
    const val hibernate = "5.4.2.Final"
    const val liquibase = "4.3.2"
    const val mySql = "8.0.20"
    const val liquibaseGradlePlugin = "2.0.4"
    const val testcontainers = "1.15.2"
    // other JVM dependencies
    const val dockerJavaApi = "3.2.8"
    const val jgit = "5.11.0.202103091610-r"
    const val okhttp3 = "4.9.1"

    // frontend
    const val react = "17.0.2"
    const val kotlinJsWrappersSuffix = "-pre.153-kotlin-1.4.32"
    const val kotlinReact = "$react$kotlinJsWrappersSuffix"
}
