package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult

/**
 * Abstract service interface for different demonstration services
 */
interface AbstractDemoService<in P : DemoAdditionalParams, out R : DemoResult> {
    /**
     * Run demo on [demoFileLines] with [demoAdditionalParams] and return result as [DemoResult]
     *
     * @param demoFileLines list of lines of input file that will be used for demo
     * @param demoAdditionalParams additional params as [DemoAdditionalParams]
     * @return report as [DemoResult]
     */
    fun runDemo(demoFileLines: List<String>, demoAdditionalParams: P? = null): R
}
