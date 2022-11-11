package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoResult
import java.io.File
import java.nio.file.Path

/**
 * Interface that should be implemented by all the runners that use [ProcessBuilder] in order to run tools for demo.
 */
interface CliRunner {
    /**
     * @return string that starts with Linux, Windows or macOS
     */
    fun osName(): String = System.getProperty("os.name")

    /**
     * @return executable file (diktat or ktlint)
     */
    fun getExecutable(): File

    /**
     * @param testPath path to test file
     * @param outputPath path to file with tool run report
     * @param configPath path to config file, if null, default config is used
     * @return run command that depends on the system it is run on
     */
    fun getRunCommand(testPath: String, outputPath: String, configPath: String?): String

    /**
     * @param testPath path to test file
     * @param outputPath path to file with tool run report
     * @param configPath path to config file, if null, default config is used
     * @return [ProcessBuilder] that is almost ready to be run
     */
    fun createProcessBuilder(testPath: Path, outputPath: Path, configPath: Path?): ProcessBuilder

    /**
     * @param testPath name of the test file
     * @param outputPath name of the file with tool run report
     * @param configPath name of the config file, if null, default config is used
     * @return tool's report as [DemoResult]
     */
    fun run(
        testPath: String,
        outputPath: String,
        configPath: String?,
    ): DemoResult
}
