import org.cqfn.save.buildutils.configureSpringBoot

plugins {
    kotlin("jvm")
}

configureSpringBoot()

dependencies {
    implementation(project(":save-common"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:${Versions.jgit}")
    implementation("org.hibernate.javax.persistence:hibernate-jpa-2.1-api:${Versions.jpa}")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:${Versions.springBoot}")
    implementation("com.h2database:h2:${Versions.h2}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")
}