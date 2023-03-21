/**
 * Utils to set up the environment for demo
 */

package com.saveourtool.save.demo.agent.utils

import com.saveourtool.save.core.files.getWorkingDirectory
import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo
import com.saveourtool.save.core.utils.ExecutionResult
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.demo.DemoConfiguration
import com.saveourtool.save.utils.*

import okio.Path.Companion.toPath

private const val SETUP_SH_LOGS_FILENAME = "setup.logs"
private const val CWD = "."

/**
 * Download all the required files from save-demo
 *
 * @param demoUrl url to save-demo
 * @param setupShTimeoutMillis amount of milliseconds to run setup.sh if it is present
 * @param demoConfiguration all the information required for tool download
 * @throws IllegalStateException when it was caught from [downloadDemoFiles]
 */
suspend fun setupEnvironment(demoUrl: String, setupShTimeoutMillis: Long, demoConfiguration: DemoConfiguration) {
    logInfo("Setting up the environment...")

    try {
        downloadDemoFiles(demoUrl, demoConfiguration)
    } catch (e: IllegalStateException) {
        logError("Error while downloading files to agent: ${e.message ?: e.toString()}.")
        throw e
    }

    markAllFilesAsExecutable(getWorkingDirectory())

    logDebug("All files successfully downloaded.")

    val executionResult = executeSetupSh(setupShTimeoutMillis)
    executionResult?.let {
        if (executionResult.code != 0) {
            logError("Setup script has finished with ${executionResult.code} code.")
        } else {
            logInfo("The environment is successfully set up.")
        }
    } ?: logInfo("No setup script was executed.")

    logInfo("The environment is successfully set up.")
}

private fun executeSetupSh(setupShTimeoutMillis: Long, setupShName: String = "setup.sh"): ExecutionResult? = setupShName.takeIf {
    fs.exists(it.toPath())
}
    ?.let { setupSh ->
        setupSh.toPath().markAsExecutable()
        ProcessBuilder(true, fs).exec(
            "./$setupSh",
            CWD,
            SETUP_SH_LOGS_FILENAME.toPath(),
            setupShTimeoutMillis,
        )
    }

private suspend fun downloadDemoFiles(demoUrl: String, demoConfiguration: DemoConfiguration) {
    val url = with(demoConfiguration) { "$demoUrl/demo/internal/files/$organizationName/$projectName/download-as-zip?version=$version" }
    downloadDemoFiles(url)
}

private suspend fun downloadDemoFiles(url: String) {
    val pathToArchive = "archive.zip".toPath()
    download("tool", url, pathToArchive)
    pathToArchive.extractZipHere()
    fs.delete(pathToArchive, mustExist = true)
    logDebug("Extracted archive into working dir and deleted $pathToArchive")
    logInfo("Downloaded and extracted zip-file from $url")
}
