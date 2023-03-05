/**
 * Configuration task to download save-cli once per a project
 */

package com.saveourtool.save.buildutils

import de.undercouch.gradle.tasks.download.Download
import org.gradle.kotlin.dsl.*

plugins {
    id("de.undercouch.download")
}

tasks.register<Download>("downloadSaveCli") {
    val saveCliVersion = readSaveCliVersion()
    val saveCliFileName = saveCliVersion.map { "save-$it-linuxX64.kexe" }
    val saveCliPath = saveCliVersion.zip(saveCliFileName) { version, fileName ->
        findProperty("saveCliPath")?.takeIf { version.isSnapshot() } as String?
            ?: "https://github.com/saveourtool/save-cli/releases/download/v$version/$fileName"
    }
    src { saveCliPath }
    dest { saveCliFileName.map { fileName -> "$buildDir/download/$fileName" } }

    overwrite(false)
}
