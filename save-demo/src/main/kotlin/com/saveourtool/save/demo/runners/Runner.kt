package com.saveourtool.save.demo.runners

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult
import java.nio.file.Path

/**
 * Interface should be implemented by all the runners.
 */
interface Runner<in P : DemoAdditionalParams, in K, out R : DemoResult> {
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
