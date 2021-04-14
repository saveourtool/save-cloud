import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

configureSpringBoot()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

repositories {
    maven(url = "https://kotlin.bintray.com/kotlinx/") // it is used for datetime. In future updates it will be jcenter()
}

dependencies {
    api(project(":save-common"))
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

val copyAgentResourcesTask by tasks.registering(Copy::class) {
    dependsOn(":save-agent:linkReleaseExecutableAgent")
    dependsOn("processResources")
    // fixme: properly share artifact as per https://docs.gradle.org/current/userguide/cross_project_publications.html#cross_project_publications
    from(file("${rootProject.project(":save-agent").buildDir}/bin/agent/releaseExecutable").listFiles()!!.single())
    from(file("${rootProject.project(":save-agent").projectDir}/src/nativeMain/resources/agent.properties"))
    into("$buildDir/resources/main")
}
tasks.getByName("processResources").finalizedBy(copyAgentResourcesTask)

configureJacoco()
