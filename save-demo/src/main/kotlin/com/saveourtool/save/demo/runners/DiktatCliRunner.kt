package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool
import com.saveourtool.save.demo.utils.prependPath
import com.saveourtool.save.utils.getLogger
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.readLines
import kotlin.io.path.readText

/**
 * Class that allows to run diktat/ktlint as command line application
 * @property tempDirPath absolute path to pre-generated temp dir
 * @property mode mode to run diktat/ktlint - FIX or WARN
 * @property tool tool to run - diktat or ktlint
 */
class DiktatCliRunner(
    private val tempDirPath: Path,
    private val mode: DiktatDemoMode = DiktatDemoMode.FIX,
    private val tool: DiktatDemoTool = DiktatDemoTool.DIKTAT,
) : CliRunner {
    override fun getExecutable(): File {
        val fileName = buildString {
            append(tool.name.lowercase())
            if (tool == DiktatDemoTool.KTLINT) {
                append("-cli")
            }
            if (!osName().startsWith("Linux", ignoreCase = true) && !osName().startsWith("Mac OS", ignoreCase = true)) {
                append(".cmd")
            }
        }

        val executableFile = ClassPathResource(fileName).file
        return if (executableFile.exists()) {
            executableFile
        } else {
            throw FileNotFoundException("Could not find diktat in resources folder.")
        }
    }

    override fun getRunCommand(
        testPath: String,
        outputPath: String,
        configPath: String?,
    ) = buildString {
        append(getExecutable())
        append(" -o $outputPath ")
        configPath?.let {
            append(" --config=$it ")
        }
        if (mode == DiktatDemoMode.FIX) {
            append(" --format ")
        }
        append(testPath)
    }
        .also {
            logger.debug("Running command [$it].")
        }

    override fun createProcessBuilder(
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
    ): ProcessBuilder {
        val command = getRunCommand(testPath.toString(), outputPath.toString(), configPath?.toString())
        return when {
            osName().startsWith("Linux", ignoreCase = true) || osName().startsWith("Mac", ignoreCase = true) ->
                ProcessBuilder("sh", "-c", command)
            else -> ProcessBuilder(command)
        }
    }

    override fun run(
        testPath: String,
        outputPath: String,
        configPath: String?,
    ): DiktatDemoResult {
        val testFilePath = tempDirPath / testPath
        val outputFilePath = tempDirPath / outputPath
        val configFilePath = configPath?.let {
            tempDirPath / configPath
        }
        val launchLogPath = tempDirPath / "log"
        val processBuilder = createProcessBuilder(
            testFilePath,
            outputFilePath,
            configFilePath,
        ).apply {
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

        val warnings = outputFilePath.readLines()
        val logs = launchLogPath.readText()

        if (logs.startsWith("Exception in thread")) {
            logger.error(logs)
        } else if (logs.isNotBlank()) {
            logger.debug(logs)
        }

        logger.debug("Found ${warnings.size} warning(s).")
        logger.trace("[${warnings.joinToString(", ")}]")

        return DiktatDemoResult(
            outputFilePath.readLines(),
            testFilePath.readText(),
        )
    }
    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        val logger = getLogger<DiktatCliRunner>()
    }
}
