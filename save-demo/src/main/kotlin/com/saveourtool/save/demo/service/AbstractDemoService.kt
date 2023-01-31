package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.Runner

/**
 * Abstract service interface for different demonstration services
 */
abstract class AbstractDemoService(
    private val runner: Runner,
) {
    /**
     * Run demo on [runRequest] and return result as [DemoResult]
     *
     * @param runRequest additional params as [DemoRunRequest]
     * @return report as [DemoResult]
     */
    abstract fun launch(runRequest: DemoRunRequest = DemoRunRequest.empty): DemoResult
}
