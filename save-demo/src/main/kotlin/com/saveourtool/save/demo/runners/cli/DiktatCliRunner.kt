package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoMode
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.diktat.*
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.toToolKey
import com.saveourtool.save.demo.utils.*
import com.saveourtool.save.utils.collectToFile
import com.saveourtool.save.utils.getLogger

import io.ktor.util.*
import org.springframework.stereotype.Component
import reactor.kotlin.core.publisher.switchIfEmpty

import java.io.FileNotFoundException
import java.nio.file.Path

import kotlin.io.path.*

/**
 * Class that allows to run diktat as command line application
 *
 * @property dependencyStorage
 */
@Component
class DiktatCliRunner(
    private val dependencyStorage: DependencyStorage,
) : CliRunner {
    override fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        demoRunRequest: DemoRunRequest,
    ): String = buildString {
        // TODO: this information should not be hardcoded but stored in database
        val ktlintExecutable = getExecutable(workingDir, DiktatDemoTool.KTLINT.toToolKey("ktlint"))
        append(ktlintExecutable)

        val diktatExecutable = getExecutable(workingDir, DiktatDemoTool.DIKTAT.toToolKey("diktat-1.2.3.jar"))
        append(" -R $diktatExecutable ")
        // disabling package-naming as far as it is useless in demo because there is no folder hierarchy
        append(" --disabled_rules=diktat-ruleset:package-naming,standard")

        append(" --reporter=plain,output=$outputPath ")
        if (demoRunRequest.mode == DemoMode.FIX) {
            append(" --format ")
        }
        append(testPath)
    }

    override fun getExecutable(workingDir: Path, toolKey: ToolKey): Path = with(toolKey) {
        dependencyStorage.findDependency(organizationName, projectName, version, fileName)
    }
        .switchIfEmpty {
            throw FileNotFoundException("Could not find file with key $toolKey")
        }
        .flatMapMany { dependencyStorage.download(it) }
        .collectToFile(workingDir / toolKey.fileName)
        .thenReturn(workingDir / toolKey.fileName)
        .block()
        .let { requireNotNull(it) }
        .apply {
            toFile().setExecutable(true, false)
        }

    override fun run(testPath: Path, demoRunRequest: DemoRunRequest): DemoResult {
        val workingDir = testPath.parent
        val outputPath = workingDir / REPORT_FILE_NAME
        val configPath = prepareFile(workingDir / DIKTAT_CONFIG_NAME, demoRunRequest.config?.joinToString("\n"))
        val launchLogPath = workingDir / LOG_FILE_NAME
        val command = getRunCommand(workingDir, testPath, outputPath, configPath, demoRunRequest)
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

        return DemoResult(
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
