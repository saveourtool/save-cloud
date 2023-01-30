package com.saveourtool.save.demo.runners.cli

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.runners.Runner
import com.saveourtool.save.demo.utils.isWindows
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Interface that should be implemented by all the runners that use [ProcessBuilder] in order to run tools for demo.
 *
 * @param P additional params that are required for demo run - should implement [DemoAdditionalParams]
 * @param K storage key needed to let runner download tools from ToolStorage
 * @param R result of demo run - should implement [DemoAdditionalParams]
 */
@Component
interface CliRunner <in P : DemoAdditionalParams, in K : Any, out R : DiktatDemoResult> : Runner<P, K, R> {
    /**
     * Save [lines] into file with [filePath]
     *
     * @param filePath path to a new file
     * @param lines string that should be printed to file, if null, nothing is done
     * @return file with [lines]
     */
    fun prepareFile(filePath: Path, lines: String?): Path? = lines?.let { fileLines ->
        filePath.apply {
            writeText(fileLines)
        }
    }

    /**
     * @param workingDir the directory where the tool is run
     * @param key storage key to find requested tool
     * @return executable file (diktat or ktlint)
     */
    fun getExecutable(workingDir: Path, key: K): Path

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
        isWindows() -> ProcessBuilder("cmd.exe", "/C", "\"$runCommand\"")
        else -> ProcessBuilder("sh", "-c", runCommand)
    }

    /**
     * @param testLines code that will be consumed by the tool
     * @param params additional params of type [DemoAdditionalParams]
     * @param tempRootDir path to root of temp directories (somewhere in storage)
     * @param testFileName test file name that should be
     * @param additionalDirectoryTree additional directory names that should be in directory hierarchy to working dir (below randomly generated dir)
     * @return result as [DemoResult]
     */
    fun runInTempDir(
        testLines: String,
        params: P,
        tempRootDir: Path,
        testFileName: String,
        additionalDirectoryTree: List<String> = emptyList(),
    ) = run {
        if (!tempRootDir.exists()) {
            tempRootDir.createDirectory()
        }
    }
        .let {
            createTempDirectory(tempRootDir)
        }
        .let { currentTempRootPath ->
            currentTempRootPath to additionalDirectoryTree.fold(currentTempRootPath) { tempRootPath, pathItem ->
                tempRootPath / pathItem
            }
                .also { tempRootPath -> tempRootPath.createDirectories() }
        }
        .let { (createdTempDir, workingDir) ->
            try {
                val testPath = requireNotNull(prepareFile(workingDir / testFileName, testLines))
                run(testPath, params)
            } finally {
                createdTempDir.toFile().deleteRecursively()
            }
        }
}
