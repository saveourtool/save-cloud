package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.diktat.DiktatDemoAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoMode
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.diktat.DiktatDemoTool

import com.saveourtool.save.demo.runners.DiktatCliRunner
import org.springframework.stereotype.Service

import java.io.File
import java.util.*

/**
 * Demo service implementation for ktlint-demo/diktat-demo
 */
@Service
class DiktatDemoService : AbstractDemoService<DiktatDemoAdditionalParams, DiktatDemoResult> {
    private fun generateName() = UUID.randomUUID().toString()
    private fun generateDemoFile(tempDirPath: String): File = File("$tempDirPath${File.separator}test.kt")
    private fun generateDemoConfig(tempDirPath: String): File = File("$tempDirPath${File.separator}config")

    /**
     * @param demoFileLines kotlin file to be checked
     * @param demoAdditionalParams instance of [DiktatDemoAdditionalParams]
     */
    override fun runDemo(
        demoFileLines: List<String>,
        demoAdditionalParams: DiktatDemoAdditionalParams?,
    ): DiktatDemoResult {
        val tool = demoAdditionalParams?.tool ?: DiktatDemoTool.DIKTAT
        val demoMode = demoAdditionalParams?.mode ?: DiktatDemoMode.WARN
        val demoConfigLines = demoAdditionalParams?.config

        val tempDir = File(generateName()).apply {
            mkdir()
        }

        val demoFile = prepareDemoFile(tempDir.absolutePath, demoFileLines.joinToString("\n"))
        val demoConfig = prepareDemoConfig(tempDir.absolutePath, demoConfigLines)

        return try {
            val runner = DiktatCliRunner(tempDir.toPath().toAbsolutePath(), demoMode, tool)
            runner.run(demoFile.name, "report", demoConfig?.name).formatWarnings()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun prepareDemoConfig(tempDirPath: String, configLines: String?) = configLines?.let {
        generateDemoConfig(tempDirPath)
            .apply {
                writeText(configLines)
            }
    }

    private fun prepareDemoFile(tempDirPath: String, fileLines: String) = generateDemoFile(tempDirPath)
        .apply {
            writeText(fileLines)
        }
}

private fun DiktatDemoResult.formatWarnings() = DiktatDemoResult(
    warnings.map { "[${it.substringAfter("[")}" },
    outputText
)
