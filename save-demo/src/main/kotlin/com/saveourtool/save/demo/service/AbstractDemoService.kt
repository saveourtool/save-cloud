package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.runners.Runner

/**
 * Abstract service interface for different demonstration services
 */
abstract class AbstractDemoService<in P : DemoAdditionalParams, in K : Any, out R : DemoResult>(
    private val runner: Runner<P, K, R>,
) {
    /**
     * Run demo on [demoFileLines] with [demoAdditionalParams] and return result as [DemoResult]
     *
     * @param demoFileLines list of lines of input file that will be used for demo
     * @param demoAdditionalParams additional params as [DemoAdditionalParams]
     * @return report as [DemoResult]
     */
    abstract fun launch(demoFileLines: List<String>, demoAdditionalParams: P? = null): R
}
