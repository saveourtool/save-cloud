object Versions {
    const val jdk = "11"  // jdk version that will be used as kotlin compiler target
    const val BP_JVM_VERSION = "11"  // jvm version for spring boot image build
    const val kotlin = "1.4.31"
    const val springBoot = "2.4.3"
    const val springSecurity = "5.4.5"
    const val hibernate = "5.4.2.Final"
    const val liquibase = "4.3.1"
    const val slf4j = "1.7.30"
    const val logback = "1.2.3"
    const val dockerJavaApi = "3.2.7"
    const val ktor = "1.5.2"
    const val serialization = "1.1.0"  // serialization is compiled by 1.4.30 since version 1.1.0 and for native ABI is different. We can update serialization only after we update kotlin.
    const val micrometer = "1.6.4"
    const val mySql = "8.0.20"
    const val jpa = "1.0.2.Final"
    const val liquibaseGradlePlugin = "2.0.4"
    const val testcontainers = "1.15.2"
}
