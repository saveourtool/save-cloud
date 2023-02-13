@file:JvmName("DownloadConfiguration")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Project
import java.net.URL
import java.nio.file.Path

/**
 * @param urlPropertyName the name of the property that holds the file URL.
 * @param targetDirectory the name of the target directory (relative to
 *   [Project.getBuildDir]).
 * @return the configuration for the [Download] task.
 */
fun Project.downloadTaskConfiguration(
    urlPropertyName: String,
    targetDirectory: String,
): Download.() -> Unit = {
    /*-
     * `hasProperty()` is not enough here, as it may return `false` even if the
     * property is set (e.g.: globally).
     */
    val fileUrlOrNull = findProperty(urlPropertyName)
    enabled = fileUrlOrNull != null
    if (!enabled) {
        logger.warn("To enable the `$name` task, set the `$urlPropertyName` property.")
    }

    val fileUrl = URL(fileUrlOrNull?.toString() ?: "file:not-found")

    src { fileUrl }
    dest {
        /*
         * Take only the last segment of potentially multi-segment server-side path.
         */
        val fileName = Path.of(fileUrl.file).fileName.toString()
        buildDir.resolve(targetDirectory).resolve(fileName).normalize()
    }
    overwrite(false)
}
