/**
 * Configuration utilities for projects which needs a generated file with SAVE_CLOUD_VERSION
 */

package com.saveourtool.save.buildutils

import java.io.File

tasks.register("generateSaveCloudVersionFile") {
    val outputDir = File("$buildDir/generated/src")
    val versionsFile = outputDir.resolve("generated/SaveCloudVersion.kt")
    inputs.property("project version", version.toString())
    outputs.dir("$buildDir/generated/src")

    doFirst {
        versionsFile.parentFile.mkdirs()
        versionsFile.writeText(
            """
            package generated

            internal const val SAVE_CLOUD_VERSION = "$version"

            """.trimIndent()
        )
    }
}
