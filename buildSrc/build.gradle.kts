plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

dependencies {
    implementation("org.cqfn.diktat:diktat-gradle-plugin:0.4.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.15.0")
}