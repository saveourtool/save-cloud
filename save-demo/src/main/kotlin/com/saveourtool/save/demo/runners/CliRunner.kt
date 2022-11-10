package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoResult
import java.io.File
import java.nio.file.Path

interface CliRunner {
    fun getExecutable(): File

    fun getRunCommand(testFilePath: String, outputFilePath: String, configFilePath: String?): String

    fun createProcessBuilder(testFilePath: Path, outputFilePath: Path, configFilePath: Path?): ProcessBuilder

    fun run(
        tempDirPath: Path,
        testFileName: String,
        outputFileName: String,
        configFileName: String?,
    ): DemoResult
}