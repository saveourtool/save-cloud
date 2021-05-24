plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")  // detekt requires kotlinx.html
}

dependencies {
    runtimeOnly(kotlin("gradle-plugin", "1.4.32"))
    implementation("org.springframework.boot:spring-boot-gradle-plugin:2.4.5")
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.5.3")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.17.0")
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.4.32")
}