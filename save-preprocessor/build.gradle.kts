import org.cqfn.save.buildutils.configureSpringBoot

plugins {
    kotlin("jvm")
}

configureSpringBoot()

dependencies {
    implementation(project(":save-common"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.4.31")
}

tasks.withType<Test> {
    useJUnitPlatform()
}