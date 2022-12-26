package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.diktat.DiktatAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoResult

import com.saveourtool.save.demo.runners.cli.DiktatCliRunner
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.utils.KOTLIN_TEST_NAME
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Demo service implementation for ktlint-demo/diktat-demo
 */
@Service
class DiktatDemoService(
    private val diktatCliRunner: DiktatCliRunner,
    configProperties: ConfigProperties,
) : AbstractDemoService<DiktatAdditionalParams, ToolKey, DiktatDemoResult>(diktatCliRunner) {
    private val tmpDir = Path.of(configProperties.fileStorage.location) / "tmp"

    /**
     * @param demoFileLines kotlin file to be checked
     * @param diktatAdditionalParams instance of [DiktatAdditionalParams]
     */
    override fun launch(
        demoFileLines: List<String>,
        diktatAdditionalParams: DiktatAdditionalParams?,
    ): DiktatDemoResult = diktatCliRunner.runInTempDir(
        demoFileLines.joinToString("\n"),
        diktatAdditionalParams ?: DiktatAdditionalParams(),
        tmpDir,
        testFileName = KOTLIN_TEST_NAME,
        additionalDirectoryTree = listOf("src"),
    )
}
