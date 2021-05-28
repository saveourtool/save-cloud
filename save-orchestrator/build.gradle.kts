import de.undercouch.gradle.tasks.download.Download
import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import org.codehaus.groovy.runtime.ResourceGroovyMethods

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

tasks.getByName("processResources").finalizedBy("downloadSaveCli")
tasks.register<Download>("downloadSaveCli") {
    dependsOn("processResources")
    val latestVersion: String = if (Versions.saveCore.endsWith("SNAPSHOT")) {
        // fixme: we will probably need snapshot of CLI too
        val latestRelease = ResourceGroovyMethods.getText(
            URL("https://api.github.com/repos/cqfn/save/releases/latest")
        )
        (groovy.json.JsonSlurper().parseText(latestRelease) as Map<String, Any>)["tag_name"].let {
            (it as String).trim('v')
        }
    } else {
        Versions.saveCore
    }
    src("https://github.com/cqfn/save/releases/download/v$latestVersion/save-$latestVersion-linuxX64.kexe")
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.kotlinxDatetime}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    testImplementation("com.squareup.okhttp3:okhttp:${Versions.okhttp3}")
    testImplementation("com.squareup.okhttp3:mockwebserver:${Versions.okhttp3}")
}

configureJacoco()

// todo: this logic is duplicated between agent and frontend, can be moved to a shared plugin in buildSrc
val generateVersionFileTaskProvider = tasks.register("generateVersionFile") {
    val versionsFile = File("$buildDir/generated/src/generated/Versions.kt")

    outputs.file(versionsFile)

    doFirst {
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_CORE_VERSION = "${Versions.saveCore}"
            internal const val SAVE_CLOUD_VERSION = "$version"

            """.trimIndent()
        )
    }
}
val generatedKotlinSrc = kotlin.sourceSets.create("generated") {
    kotlin.srcDir("$buildDir/generated/src")
}
kotlin.sourceSets.getByName("main").dependsOn(generatedKotlinSrc)
tasks.withType<KotlinCompile>().forEach {
    it.dependsOn(generateVersionFileTaskProvider)
}
