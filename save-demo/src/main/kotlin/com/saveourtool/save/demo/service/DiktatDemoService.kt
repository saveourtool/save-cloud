package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.config.ConfigProperties
import com.saveourtool.save.demo.diktat.DiktatDemoAdditionalParams
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
) : AbstractDemoService<DiktatDemoAdditionalParams, DiktatDemoResult>(diktatCliRunner) {
    private val tmpDir = Path.of(configProperties.fileStorage.location) / "tmp"

    /**
     * @param demoFileLines kotlin file to be checked
     * @param demoAdditionalParams instance of [DiktatDemoAdditionalParams]
     */
    override fun launch(
        demoFileLines: List<String>,
        demoAdditionalParams: DiktatDemoAdditionalParams?,
    ): DiktatDemoResult = diktatCliRunner.runInTempDir(
        demoFileLines.joinToString("\n"),
        demoAdditionalParams?.config,
        "test.kt",
        demoAdditionalParams ?: DiktatDemoAdditionalParams(),
        tmpDir,
    )
        .formatWarnings()
}

private fun DiktatDemoResult.formatWarnings() = DiktatDemoResult(
    warnings.map { "[${it.substringAfter("[")}" },
    outputText
)
