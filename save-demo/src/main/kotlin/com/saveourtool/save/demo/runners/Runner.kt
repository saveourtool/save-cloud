package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.diktat.DiktatAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import java.nio.file.Path

/**
 * Interface should be implemented by all the runners.
 *
 * @param P additional params that are required for demo run - should implement [DiktatAdditionalParams]
 * @param K storage key needed to let runner download tools from ToolStorage
 * @param R result of demo run - should implement [DiktatAdditionalParams]
 */
interface Runner<in P : DiktatAdditionalParams, in K : Any, out R : DiktatDemoResult> {
    /**
     * @param testPath path to the test file
     * @param params additional params of type [DiktatAdditionalParams]
     * @return tool's report as [DiktatAdditionalParams]
     */
    fun run(
        testPath: Path,
        params: P,
    ): R
}
