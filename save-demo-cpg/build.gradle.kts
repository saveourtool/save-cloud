import com.saveourtool.save.buildutils.*

plugins {
    id("com.saveourtool.save.buildutils.kotlin-jvm-configuration")
    id("com.saveourtool.save.buildutils.spring-boot-app-configuration")
}

dependencies {
    implementation(libs.spring.data.neo4j)
    implementation(libs.reactor.extra)
}

configureJacoco()
configureSpotless()
