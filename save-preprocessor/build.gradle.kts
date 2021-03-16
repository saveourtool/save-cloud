import org.cqfn.save.buildutils.configureSpringBoot

plugins {
    kotlin("jvm")
}

configureSpringBoot()

dependencies {
    implementation(project(":save-common"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}