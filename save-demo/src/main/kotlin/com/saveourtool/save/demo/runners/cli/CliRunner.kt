package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.runners.Runner
import com.saveourtool.save.demo.utils.generateName
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Interface that should be implemented by all the runners that use [ProcessBuilder] in order to run tools for demo.
 */
@Component
interface CliRunner <in P : DemoAdditionalParams, out R : DemoResult> : Runner<P, R> {
    /**
     * @return string that starts with Linux, Windows or macOS
     */
    fun osName(): String = System.getProperty("os.name")

    /**
     * @param workingDir the directory where the tool is run
     * @param params additional params of type [DemoAdditionalParams]
     * @return executable file (diktat or ktlint)
     */
    fun getExecutable(workingDir: Path, params: P): File

    /**
     * @param workingDir the directory where the tool is run
     * @param testPath path to test file
     * @param outputPath path to file with tool run report
     * @param configPath path to config file, if null, default config is used
     * @param params additional params of type [DemoAdditionalParams]
     * @return run command that depends on the system it is run on
     */
    fun getRunCommand(
        workingDir: Path,
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: P
    ): String

    /**
     * @param runCommand command that should be used to run tool
     * @return [ProcessBuilder] that is almost ready to be run
     * @throws NotImplementedError when run on system that is not supported
     */
    fun createProcessBuilder(runCommand: String): ProcessBuilder = when {
        osName().startsWith("Linux", ignoreCase = true) || osName().startsWith("Mac", ignoreCase = true) ->
            ProcessBuilder("sh", "-c", runCommand)
        osName().startsWith("Windows", ignoreCase = true) -> ProcessBuilder(runCommand)
        else -> throw NotImplementedError("CliRunner can work only on Linux, Windows or Mac OS")
    }

    /**
     * @param testLines code that will be consumed by the tool
     * @param configLines tool config file
     * @param testFileName name of file with code - needed in order to support different formats of file
     * @param params additional params of type [DemoAdditionalParams]
     * @param tempRootDir path to root of temp directories (somewhere in storage)
     * @return result as [DemoResult]
     */
    fun runInTempDir(
        testLines: String,
        configLines: String?,
        testFileName: String,
        params: P,
        tempRootDir: Path,
    ) = generateName().let {
        tempRootDir / it
    }
        .toFile()
        .apply {
            mkdir()
        }
        .let { tmpDir ->
            val testPath = requireNotNull(prepareFile(tmpDir.toPath() / testFileName, testLines)?.toPath())
            val configPath = prepareFile(tmpDir.toPath() / testFileName, configLines)?.toPath()
            val outputPath = tmpDir.toPath() / "report"
            try {
                run(testPath, outputPath, configPath, params)
            } finally {
                tmpDir.deleteRecursively()
            }
        }
}
