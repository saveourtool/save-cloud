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

private const val PROCESS_BUILDER_TIMEOUT_MILLIS = 20_000L

/**
 * @param demoRunRequest [DemoRunRequest] that contains all the information about current run request
 * @param deferredConfig [CompletableDeferred] filled with [DemoAgentConfig] corresponding to _this_ demo
 * @return [DemoResult] filled with the results of demo run
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runDemo(demoRunRequest: DemoRunRequest, deferredConfig: CompletableDeferred<DemoAgentConfig>): DemoResult {
    require(demoRunRequest.mode.isNotBlank()) { "Demo mode should not be blank." }

    val config = deferredConfig.getCompleted().runConfiguration
    val runCommand = requireNotNull(config.runCommands[demoRunRequest.mode]) {
        "Could not find run command for mode ${demoRunRequest.mode}."
    }

    val (inputFile, configFile) = createRequiredFiles(demoRunRequest, config)
    val outputFile = config.outputFileName?.toPath()

    return try {
        run(runCommand, inputFile, outputFile)
    } finally {
        cleanUp(inputFile, configFile, outputFile)
    }
}

private fun createRequiredFiles(
    demoRunRequest: DemoRunRequest,
    config: RunConfiguration,
): Pair<Path, Path?> = fs.createAndWrite(config.inputFileName, demoRunRequest.codeLines) to
        fs.createAndWriteIfNeeded(config.configFileName, demoRunRequest.config)

private fun cleanUp(inputFile: Path, configFile: Path?, outputFile: Path?) {
    fs.delete(inputFile, false)
    outputFile?.let { fs.delete(it, false) }
    configFile?.let { fs.delete(it, false) }
}

private fun run(
    runCommand: String,
    inputFile: Path,
    outputFile: Path?
): DemoResult {
    val pb = ProcessBuilder(false, fs)
    val executionResult = pb.exec(runCommand, ".", null, PROCESS_BUILDER_TIMEOUT_MILLIS)
    val warnings = outputFile?.takeIf { fs.exists(it) }?.let { fs.readLines(it) }.orEmpty()

    return DemoResult(
        warnings = warnings,
        stdout = executionResult.stdout,
        stderr = executionResult.stderr,
        outputText = fs.readLines(inputFile),
        terminationCode = executionResult.code,
    )
}
