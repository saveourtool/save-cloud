package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.cli.DiktatCliRunner
import com.saveourtool.save.demo.utils.KOTLIN_TEST_NAME

import org.springframework.stereotype.Service

import kotlin.io.path.createTempDirectory

/**
 * Demo service implementation for diktat-demo
 */
@Service
class DiktatDemoService(
    private val diktatCliRunner: DiktatCliRunner,
) : AbstractDemoService (diktatCliRunner) {
    private val tmpDir = createTempDirectory("diktat-demo-")

    /**
     * @param runRequest instance of [DemoRunRequest]
     */
    override fun launch(runRequest: DemoRunRequest): DemoResult = diktatCliRunner.runInTempDir(
        runRequest,
        tmpDir,
        testFileName = KOTLIN_TEST_NAME,
        additionalDirectoryTree = listOf("src"),
    )
}
