package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.diktat.DiktatAdditionalParams
import com.saveourtool.save.demo.diktat.DiktatDemoResult
import com.saveourtool.save.demo.runners.Runner

/**
 * Abstract service interface for different demonstration services
 */
abstract class AbstractDemoService<in P : DiktatAdditionalParams, in K : Any, out R : DiktatDemoResult>(
    private val runner: Runner<P, K, R>,
) {
    /**
     * Run demo on [demoFileLines] with [diktatAdditionalParams] and return result as [DiktatDemoResult]
     *
     * @param demoFileLines list of lines of input file that will be used for demo
     * @param diktatAdditionalParams additional params as [DiktatAdditionalParams]
     * @return report as [DiktatDemoResult]
     */
    abstract fun launch(demoFileLines: List<String>, diktatAdditionalParams: P? = null): R
}
