package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.diktat.DiktatAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.demo.utils.*
import com.saveourtool.save.utils.collectToFile
import com.saveourtool.save.utils.getLogger

import io.ktor.util.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

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
) : CliRunner<DiktatAdditionalParams, ToolKey, DiktatDemoResult> {
    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: DiktatAdditionalParams,
    ): String = buildString {
        // TODO: this information should not be hardcoded but stored in database
        val ktlintExecutable = getExecutable(workingDir, DiktatDemoTool.KTLINT.toToolKey("ktlint"))
        append(ktlintExecutable)
        if (params.tool == DiktatDemoTool.DIKTAT) {
            val diktatExecutable = getExecutable(workingDir, DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
            append(" -R $diktatExecutable ")
            // disabling package-naming as far as it is useless in demo because there is no folder hierarchy
            append(" --disabled_rules=diktat-ruleset:package-naming,standard")
        }
        append(" --reporter=plain,output=$outputPath ")
        if (params.mode == DiktatDemoMode.FIX) {
            append(" --format ")
        }
        append(testPath)
    }

    override fun getExecutable(workingDir: Path, key: ToolKey): Path = Mono.zip(
        key.toMono(),
        toolStorage.doesExist(key),
    )
        .filter { (_, doesExist) ->
            doesExist
        }
        .switchIfEmpty {
            throw FileNotFoundException("Could not find file with key $key")
        }
        .flatMapMany { (key, _) ->
            toolStorage.download(key)
        }
        .collectToFile(workingDir / key.executableName)
        .thenReturn(workingDir / key.executableName)
        .block()
        .let { requireNotNull(it) }
        .apply {
            toFile().setExecutable(true, false)
        }

    override fun run(testPath: Path, params: DiktatAdditionalParams): DiktatDemoResult {
        val workingDir = testPath.parent
        val outputPath = workingDir / REPORT_FILE_NAME
        val configPath = prepareFile(workingDir / DIKTAT_CONFIG_NAME, params.config.joinToString("\n"))
        val launchLogPath = workingDir / LOG_FILE_NAME
        val command = getRunCommand(workingDir, testPath, outputPath, configPath, params)
        val processBuilder = createProcessBuilder(command).apply {
            redirectErrorStream(true)
            redirectOutput(ProcessBuilder.Redirect.appendTo(launchLogPath.toFile()))

            /*
             * Inherit JAVA_HOME for the child process.
             */
            val javaHome = System.getProperty("java.home")
            environment()["JAVA_HOME"] = javaHome
            /*
             * Need to remove JAVA_TOOL_OPTIONS (and _JAVA_OPTIONS just in case) because JAVA_TOOL_OPTIONS is set by spring,
             * so it breaks ktlint's "java -version" parsing (for ktlint 0.46.1, fixed in ktlint 0.47.1)
             */
            environment().remove("JAVA_TOOL_OPTIONS")
            environment().remove("_JAVA_OPTIONS")
            prependPath(Path(javaHome) / "bin")
        }

        logger.debug("Running command [$command].")
        val terminationCode = processBuilder.start().waitFor()

        val logs = launchLogPath.readLines()

        @Suppress("TooGenericExceptionCaught")
        val warnings = try {
            outputPath.readLines()
        } catch (e: Exception) {
            logs.forEach(logger::error)
            throw e
        }

        logs.forEach(logger::trace)

        logger.trace("Found ${warnings.size} warning(s): [${warnings.joinToString(", ")}]")

        return DiktatDemoResult(
            outputPath.readLines().map { it.replace(testPath.absolutePathString(), testPath.name) },
            testPath.readLines(),
            logs,
            terminationCode,
        )
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DiktatCliRunner>()
    }
}
