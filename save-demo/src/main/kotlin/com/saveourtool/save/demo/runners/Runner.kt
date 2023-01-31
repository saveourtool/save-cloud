package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.DemoRunRequest
import java.nio.file.Path

/**
 * Interface should be implemented by all the runners.
 */
interface Runner {
    /**
     * @param testPath path to the test file
     * @param demoRunRequest params of type [DemoRunRequest]
     * @return tool's report as [DemoResult]
     */
    fun run(
        testPath: Path,
        demoRunRequest: DemoRunRequest,
    ): DemoResult
}
