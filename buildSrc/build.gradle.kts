plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val kotlinVersion = "1.5.31"
dependencies {
    runtimeOnly(kotlin("gradle-plugin", kotlinVersion))
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.5.4")
    implementation("org.cqfn.diktat:diktat-gradle-plugin:1.0.0-rc.3")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.17.1")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation(kotlin("allopen", kotlinVersion))
}
