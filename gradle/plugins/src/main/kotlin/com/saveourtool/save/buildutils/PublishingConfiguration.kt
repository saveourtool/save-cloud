/**
 * Publishing configuration file.
 */

@file:Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_ON_FUNCTION",
)

package com.saveourtool.save.buildutils

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutput.Style.Failure
import org.gradle.internal.logging.text.StyledTextOutput.Style.Success
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension

private fun Project.styledOut(logCategory: String): StyledTextOutput =
        serviceOf<StyledTextOutputFactory>().create(logCategory)

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
            /*
             * The content of this section will get executed only if
             * a particular module has a `publishing {}` section.
             */
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

/**
 * Enables signing of the artifacts if the `signingKey` project property is set.
 *
 * Should be explicitly called after each custom `publishing {}` section.
 */
fun Project.configureSigning() {
    if (hasProperty("signingKey")) {
        configure<SigningExtension> {
            useInMemoryPgpKeys(property("signingKey") as String?, property("signingPassword") as String?)
            val publications = extensions.getByType<PublishingExtension>().publications
            val publicationCount = publications.size
            val message = "The following $publicationCount publication(s) are getting signed: ${publications.map(Named::getName)}"
            val style = when (publicationCount) {
                0 -> Failure
                else -> Success
            }
            styledOut(logCategory = "signing").style(style).println(message)
            sign(*publications.toTypedArray())
        }
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
