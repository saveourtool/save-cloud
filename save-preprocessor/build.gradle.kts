import org.cqfn.save.buildutils.configureJacoco
import org.cqfn.save.buildutils.configureSpringBoot

plugins {
    kotlin("jvm")
}

configureSpringBoot()

dependencies {
    implementation(project(":save-cloud-common"))
    implementation("org.cqfn.save:save-common-jvm:${Versions.saveCore}")
    implementation("org.cqfn.save:save-core-jvm:${Versions.saveCore}")
    implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    testImplementation("com.squareup.okhttp3:okhttp:${Versions.okhttp3}")
    testImplementation("com.squareup.okhttp3:mockwebserver:${Versions.okhttp3}")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = Versions.jdk
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configureJacoco()
