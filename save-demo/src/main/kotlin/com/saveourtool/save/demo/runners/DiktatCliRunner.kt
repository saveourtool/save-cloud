package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.utils.prependPath
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class DiktatCliRunner : CliRunner {
    override fun getExecutable(): File {
        return object {}.javaClass.getResource("diktat")?.path?.let { File(it) }
            ?: throw FileNotFoundException("Could not find diktat in resources folder.")
    }

    override fun getRunCommand(
        testFilePath: String,
        outputFilePath: String,
        configFilePath: String?,
    ) = buildString {
        append(getExecutable())
        append(" --output=$outputFilePath ")
        configFilePath?.let {
            append(" --config=$it ")
        }
        append(testFilePath)
    }

    override fun createProcessBuilder(
        testFilePath: Path,
        outputFilePath: Path,
        configFilePath: Path?,
    ): ProcessBuilder {
        val command = getRunCommand(testFilePath.toString(), outputFilePath.toString(), configFilePath.toString())
        val systemName = System.getProperty("os.name")
        return when {
            systemName.startsWith("Linux", ignoreCase = true) || systemName.startsWith("Mac", ignoreCase = true) ->
                ProcessBuilder("sh", "-c", command)
            else -> ProcessBuilder(command)
        }
    }

    override fun run(
        tempDirPath: Path,
        testFileName: String,
        outputFileName: String,
        configFileName: String?
    ): DiktatDemoResult {
        val configPath = configFileName?.let {
            tempDirPath / configFileName
        }
        val testFilePath = tempDirPath / testFileName
        val outputFilePath = tempDirPath / outputFileName
        val launchLogPath = tempDirPath / "log"
        val processBuilder = createProcessBuilder(
            testFilePath,
            outputFilePath,
            configPath,
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

        return DiktatDemoResult(
            outputFilePath.toFile().readLines(),
            testFilePath.toFile().readText(),
        )
    }
}