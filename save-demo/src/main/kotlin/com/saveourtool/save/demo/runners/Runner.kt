package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult
import java.nio.file.Path

/**
 * Interface should be implemented by all the runners.
 *
 * @param P additional params that are required for demo run - should implement [DemoAdditionalParams]
 * @param K storage key needed to let runner download tools from ToolStorage
 * @param R result of demo run - should implement [DemoAdditionalParams]
 */
interface Runner<in P : DemoAdditionalParams, in K : Any, out R : DemoResult> {
    /**
     * @param testPath name of the test file
     * @param params additional params of type [DemoAdditionalParams]
     * @return tool's report as [DemoResult]
     */
    fun run(
        testPath: Path,
        params: P,
    ): R
}
