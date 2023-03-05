/**
 * Configuration utilities for projects which needs a generated file with SAVE_CLOUD_VERSION
 */

package com.saveourtool.save.buildutils

import java.io.File

tasks.register("generateSaveCloudVersionFile") {
    val outputDir = File("$buildDir/generated/src")
    val versionFile = outputDir.resolve("generated/SaveCloudVersion.kt")
    inputs.property("project version", version.toString())
    outputs.dir(outputDir)

    doFirst {
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            package generated

            internal const val SAVE_CLOUD_VERSION = "$version"

            """.trimIndent()
        )
    }
}
