package com.saveourtool.save.sandbox.service

/**
 * Abstract service interface for different demonstration services
 */
interface AbstractDemoService<R> {
    /**
     * Run demo
     *
     * @param demoFileLines input file that will be used for demo
     * @param demoAdditionalParams list of additional params
     * @return report of type [R]
     */
    fun runDemo(demoFileLines: String, demoAdditionalParams: List<String> = emptyList()): R
}
