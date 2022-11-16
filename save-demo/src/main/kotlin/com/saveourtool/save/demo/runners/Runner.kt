package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult
import java.nio.file.Path

/**
 * Interface should be implemented by all the runners.
 */
interface Runner<in P : DemoAdditionalParams, out R : DemoResult> {
    /**
     * Save [lines] into file with [filePath]
     *
     * @param filePath path to a new file
     * @param lines string that should be printed to file, if null, nothing is done
     * @return file with [lines]
     */
    fun prepareFile(filePath: Path, lines: String?) = lines?.let { fileLines ->
        filePath.toFile().apply {
            writeText(fileLines)
        }
    }

    /**
     * @param testPath name of the test file
     * @param outputPath name of the file with tool run report
     * @param configPath name of the config file, if null, default config is used
     * @param params additional params of type [DemoAdditionalParams]
     * @return tool's report as [DemoResult]
     */
    fun run(
        testPath: Path,
        outputPath: Path,
        configPath: Path?,
        params: P,
    ): R
}
