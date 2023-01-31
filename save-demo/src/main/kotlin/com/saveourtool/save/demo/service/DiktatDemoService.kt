package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest

import com.saveourtool.save.demo.runners.cli.DiktatCliRunner
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
) : AbstractDemoService (diktatCliRunner) {
    private val tmpDir = Path.of(configProperties.fileStorage.location) / "tmp"

    /**
     * @param demoFileLines kotlin file to be checked
     * @param runRequest instance of [DemoRunRequest]
     */

    override fun launch(demoFileLines: List<String>, runRequest: DemoRunRequest): DemoResult = diktatCliRunner.runInTempDir(
        demoFileLines.joinToString("\n"),
        runRequest,
        tmpDir,
        testFileName = KOTLIN_TEST_NAME,
        additionalDirectoryTree = listOf("src"),
    )
}
