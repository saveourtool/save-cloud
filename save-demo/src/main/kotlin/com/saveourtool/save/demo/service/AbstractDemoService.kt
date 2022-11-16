package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoAdditionalParams
import com.saveourtool.save.demo.DemoResult
import com.saveourtool.save.demo.runners.cli.CliRunner
import org.springframework.stereotype.Service

/**
 * Abstract service interface for different demonstration services
 */
@Service
abstract class AbstractDemoService<in P : DemoAdditionalParams, out R : DemoResult>(
    private val runner: CliRunner<P, R>,
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
