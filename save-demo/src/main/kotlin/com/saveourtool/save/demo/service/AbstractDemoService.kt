package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import com.saveourtool.save.demo.runners.Runner
import reactor.core.publisher.Mono

/**
 * Abstract service interface for different demonstration services
 */
open class AbstractDemoService(
    private val runner: Runner,
) {
    /**
     * Run demo on [runRequest] and return result as [DemoResult]
     *
     * @param runRequest additional params as [DemoRunRequest]
     * @return report as [DemoResult]
     */
    fun run(runRequest: DemoRunRequest = DemoRunRequest.empty): Mono<DemoResult> = runner.run(runRequest)
}
