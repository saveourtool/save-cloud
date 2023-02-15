/**
 * Simple runner that uses ProcessBuilder to run the tool
 */

package com.saveourtool.save.demo.agent

import com.saveourtool.save.core.files.readLines
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.demo.DemoAgentConfig
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.RunConfiguration
import com.saveourtool.save.utils.createAndWrite
import com.saveourtool.save.utils.createAndWriteIfNeeded
import com.saveourtool.save.utils.fs

import okio.Path
import okio.Path.Companion.toPath

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

private const val PROCESS_BUILDER_TIMEOUT_MILLIS = 3000L

/**
 * @param demoRunRequest
 * @param deferredConfig
 * @return [DemoResult] filled with the results of demo run
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runDemo(
    demoRunRequest: DemoRunRequest,
    deferredConfig: CompletableDeferred<DemoAgentConfig>,
): DemoResult {
    val config = deferredConfig.getCompleted().runConfiguration
    val (inputFile, configFile) = createRequiredFiles(demoRunRequest, config)

    val logFile = config.logFileName.toPath()
    val outputFile = config.outputFileName?.toPath()

    return run(config.runCommand, inputFile, logFile, outputFile).also {
        cleanUp(inputFile, configFile, logFile, outputFile)
    }
}

private fun createRequiredFiles(
    demoRunRequest: DemoRunRequest,
    config: RunConfiguration,
): Pair<Path, Path?> = fs.createAndWrite(config.inputFileName, demoRunRequest.codeLines) to
        config.configFileName?.let { fs.createAndWriteIfNeeded(it, demoRunRequest.config) }

private fun cleanUp(
    inputFile: Path,
    configFile: Path?,
    logFile: Path,
    outputFile: Path?
) {
    fs.delete(inputFile, false)
    fs.delete(logFile, false)
    outputFile?.let { fs.delete(it, false) }
    configFile?.let { fs.delete(it, false) }
}

private fun run(
    runCommand: String,
    inputFile: Path,
    logFile: Path,
    outputFile: Path?
): DemoResult {
    val pb = ProcessBuilder(true, fs)

    val executionResult = pb.exec(
        runCommand,
        ".",
        logFile,
        PROCESS_BUILDER_TIMEOUT_MILLIS
    )

    val warnings = outputFile?.let {
        if (fs.exists(it)) {
            fs.readLines(it)
        } else {
            emptyList()
        }
    }.orEmpty()

    return DemoResult(
        warnings = warnings,
        logs = fs.readLines(logFile),
        outputText = fs.readLines(inputFile),
        terminationCode = executionResult.code
    )
}
