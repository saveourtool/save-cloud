package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.diktat.DiktatDemoAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.utils.prependPath
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.writeToFile
import io.ktor.util.*
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Class that allows to run diktat/ktlint as command line application
 *
 * @property toolStorage
 */
@Component
class DiktatCliRunner(
    private val toolStorage: ToolStorage,
) : CliRunner<DiktatDemoAdditionalParams, DiktatDemoResult> {
    override fun getExecutable(workingDir: Path, params: DiktatDemoAdditionalParams): File {
        val toolName = params.tool.name.lowercase()
        val version = if (params.tool == DiktatDemoTool.DIKTAT) {
            DIKTAT_VERSION
        } else {
            KTLINT_VERSION
        }
        val fileName = buildString {
            append(toolName)
            if (params.tool == DiktatDemoTool.KTLINT) {
                append("-cli")
            }
            if (!osName().startsWith("Linux", ignoreCase = true) && !osName().startsWith("Mac OS", ignoreCase = true)) {
                append(".cmd")
            }
        }
        val key = ToolKey(toolName, version, fileName)
        if (toolStorage.doesExist(key).block() != true) {
            throw FileNotFoundException("Could not find file with key $key")
        }

        val executableFile = toolStorage.download(key)
            .writeToFile(workingDir / key.executableName)
            .block()
            ?.toFile()

        requireNotNull(executableFile)

        return executableFile
    }

    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: DiktatDemoAdditionalParams
    ): String = buildString {
        val executable = getExecutable(workingDir, params)
        if (osName().startsWith("Linux", ignoreCase = true) || osName().startsWith("Mac OS", ignoreCase = true)) {
            append("chmod 777 $executable; ")
        }
        append(executable)
        append(" -o $outputPath ")
        configPath?.let {
            append(" --config=$it ")
        }
        if (params.mode == DiktatDemoMode.FIX) {
            append(" --format ")
        }
        append(testPath)
    }
        .also {
            logger.debug("Running command [$it].")
        }

    override fun run(
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: DiktatDemoAdditionalParams,
    ): DiktatDemoResult {
        val workingDir = testPath.parent
        val launchLogPath = workingDir / "log"
        val command = getRunCommand(workingDir, testPath, outputPath, configPath, params)
        val processBuilder = createProcessBuilder(command).apply {
            redirectErrorStream(true)
            redirectOutput(ProcessBuilder.Redirect.appendTo(launchLogPath.toFile()))

            /*
             * Inherit JAVA_HOME for the child process.
             */
            val javaHome = System.getProperty("java.home")
            environment()["JAVA_HOME"] = javaHome
            prependPath(Path(javaHome) / "bin")
        }

        processBuilder.start().waitFor()

        val logs = launchLogPath.readLines()

        @Suppress("TooGenericExceptionCaught")
        val warnings = try {
            outputPath.readLines()
        } catch (e: Exception) {
            logs.forEach { log ->
                logger.error(log)
            }
            throw e
        }

        logs.forEach { log ->
            logger.debug(log)
        }

        logger.debug("Found ${warnings.size} warning(s).")
        logger.trace("[${warnings.joinToString(", ")}]")

        return DiktatDemoResult(
            outputPath.readLines(),
            testPath.readText(),
        )
    }
    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DiktatCliRunner>()
        private const val DIKTAT_VERSION = "1.2.3"
        private const val KTLINT_VERSION = "0.47.1"
    }
}
