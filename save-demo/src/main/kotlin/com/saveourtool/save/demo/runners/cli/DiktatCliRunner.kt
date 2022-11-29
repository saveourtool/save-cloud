package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.diktat.DiktatDemoAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.demo.utils.prependPath
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
) : CliRunner<DiktatDemoAdditionalParams, ToolKey, DiktatDemoResult> {
    private fun getRunCommandForDiktat(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: DiktatDemoAdditionalParams,
    ): String = buildString {
        // TODO: this information should not be hardcoded but stored in database
        val ktlintExecutable = getExecutable(workingDir, DiktatDemoTool.KTLINT.toToolKey("ktlint"))
        val diktatExecutable = getExecutable(workingDir, DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
        append(ktlintExecutable)
        append(" -R $diktatExecutable ")
        append(" --disabled_rules=standard")
        append(" --reporter=plain,output=$outputPath ")
        configPath?.let {
            append(" --config=$it ")
        }
        if (params.mode == DiktatDemoMode.FIX) {
            append(" --format ")
        }
        append(testPath)
    }

    private fun getRunCommandForKtlint(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: DiktatDemoAdditionalParams,
    ): String = buildString {
        // TODO: this information should not be hardcoded but stored in database
        val executable = getExecutable(workingDir, DiktatDemoTool.KTLINT.toToolKey("ktlint"))
        append(executable)
        append(" --reporter=plain,output=$outputPath ")
        configPath?.let {
            append(" --config=$it ")
        }
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

    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: DiktatDemoAdditionalParams,
    ): String = when (params.tool) {
        DiktatDemoTool.DIKTAT -> getRunCommandForDiktat(workingDir, testPath, outputPath, configPath, params)
        DiktatDemoTool.KTLINT -> getRunCommandForKtlint(workingDir, testPath, outputPath, configPath, params)
    }

    override fun run(testPath: Path, params: DiktatDemoAdditionalParams): DiktatDemoResult {
        val workingDir = testPath.parent
        val outputPath = workingDir / "report"
        val configPath = prepareFile(workingDir / "config", params.config)
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

        logger.debug("Running command [$command].")
        processBuilder.start().waitFor()

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
            outputPath.readLines(),
            testPath.readText(),
        )
    }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val logger = getLogger<DiktatCliRunner>()
    }
}
