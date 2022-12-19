/**
 * Publishing configuration file.
 */

@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
)

package com.saveourtool.save.buildutils

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

@Suppress("TOO_LONG_FUNCTION")
internal fun Project.configurePublications() {
    val dokkaJar: Jar = tasks.create<Jar>("dokkaJar") {
        group = "documentation"
        archiveClassifier.set("javadoc")
        from(tasks.findByName("dokkaHtml"))
    }
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }
        publications.withType<MavenPublication>().configureEach {
            this.artifact(dokkaJar)
            this.pom {
                name.set(project.name)
                description.set(project.description ?: project.name)
                url.set("https://github.com/saveourtool/save-cloud")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("petertrr")
                        name.set("Petr Trifanov")
                        email.set("peter.trifanov@gmail.com")
                    }
                    developer {
                        id.set("akuleshov7")
                        name.set("Andrey Kuleshov")
                        email.set("andrewkuleshov7@gmail.com")
                    }
                }
                scm {
                    url.set("https://github.com/saveourtool/save-cloud")
                    connection.set("scm:git:git://github.com/saveourtool/save-cloud.git")
                }
            }
        }
    }
}

internal fun Project.configureSigning() {
    configure<SigningExtension> {
        useInMemoryPgpKeys(property("signingKey") as String?, property("signingPassword") as String?)
        logger.lifecycle("The following publications are getting signed: ${extensions.getByType<PublishingExtension>().publications.map { it.name }}")
        sign(*extensions.getByType<PublishingExtension>().publications.toTypedArray())
    }
}

internal fun Project.configureNexusPublishing() {
    configure<NexusPublishExtension> {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(property("sonatypeUsername") as String)
                password.set(property("sonatypePassword") as String)
            }
        }
    }
}
