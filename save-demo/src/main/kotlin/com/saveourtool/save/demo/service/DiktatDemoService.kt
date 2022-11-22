package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.diktat.DemoAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoResult

import com.saveourtool.save.demo.runners.cli.DiktatCliRunner
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
) : AbstractDemoService<DemoAdditionalParams, DiktatDemoResult>(diktatCliRunner) {
    private val tmpDir = Path.of(configProperties.fileStorage.location) / "tmp"

    /**
     * @param demoFileLines kotlin file to be checked
     * @param demoAdditionalParams instance of [DemoAdditionalParams]
     */
    override fun launch(
        demoFileLines: List<String>,
        demoAdditionalParams: DemoAdditionalParams?,
    ): DiktatDemoResult = diktatCliRunner.runInTempDir(
        demoFileLines.joinToString("\n"),
        demoAdditionalParams ?: DemoAdditionalParams(),
        tmpDir,
    )
        .formatWarnings()
}

private fun DiktatDemoResult.formatWarnings() = DiktatDemoResult(
    warnings.map { "[${it.substringAfter("[")}" },
    outputText
)
