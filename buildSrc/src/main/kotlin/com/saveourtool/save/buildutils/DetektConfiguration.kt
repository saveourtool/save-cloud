/**
 * Configuration for detekt static analysis
 */

package com.saveourtool.save.buildutils

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Configure Detekt for a single project
 */
@Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
fun Project.configureDetekt() {
    apply<DetektPlugin>()
    configure<DetektExtension> {
        config = rootProject.files("detekt.yml")
        basePath = rootDir.canonicalPath
        buildUponDefaultConfig = true
    }
    if (path == rootProject.path) {
        tasks.register("mergeDetektReports", ReportMergeTask::class) {
            output.set(buildDir.resolve("detekt-sarif-reports/detekt-merged.sarif"))
        }
    }
    val reportMerge: TaskProvider<ReportMergeTask> = rootProject.tasks.named<ReportMergeTask>("mergeDetektReports") {
        input.from(
            tasks.withType<Detekt>().map { it.sarifReportFile }
        )
        shouldRunAfter(tasks.withType<Detekt>())
    }
    tasks.withType<Detekt>().configureEach {
        reports.sarif.required.set(true)
        finalizedBy(reportMerge)
    }
}

/**
 * Register a unified detekt task
 */
fun Project.createDetektTask() {
    tasks.register("detektAll") {
        allprojects {
            this@register.dependsOn(tasks.withType<Detekt>())
        }
    }
}
