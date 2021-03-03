plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
    gradlePluginPortal()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.4.2")
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.4.1")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.4.21")
}