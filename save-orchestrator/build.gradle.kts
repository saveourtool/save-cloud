import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("de.undercouch.download")
}

configureSpringBoot()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.getByName("processTestResources").dependsOn("downloadSaveCli")

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadSaveCli") {
    src("https://github.com/cqfn/save/releases/download/v0.1.0-alpha.2/save-0.1.0-alpha.2-linuxX64.kexe")
    dest("$buildDir/resources/main")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    api(project(":save-common"))
    runtimeOnly(project(":save-agent", "distribution"))
    implementation("org.slf4j:slf4j-api:${Versions.slf4j}")
    implementation("ch.qos.logback:logback-core:${Versions.logback}")
    implementation("com.github.docker-java:docker-java-core:${Versions.dockerJavaApi}")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:${Versions.dockerJavaApi}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:${Versions.serialization}")
    implementation("org.apache.commons:commons-compress:1.20")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    testImplementation("com.squareup.okhttp3:okhttp:${Versions.okhttp3}")
    testImplementation("com.squareup.okhttp3:mockwebserver:${Versions.okhttp3}")
}

configureJacoco()
