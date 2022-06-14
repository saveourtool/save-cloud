package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.service.TestExecutionService
import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.backend.service.TestSuitesService
import com.saveourtool.save.entities.Test
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 *  Controller used to initialize tests
 */
@RestController
@RequestMapping("/internal")
class TestController(
    private val testService: TestService,
    private val testExecutionService: TestExecutionService,
    private val testSuitesService: TestSuitesService,
    private val meterRegistry: MeterRegistry,
) {
    /**
     * @param testDtos list of [TestDto]s to save into the DB
     * @param executionId ID of the [Execution], during which these tests will be initialized
     * @return list of initialized [Tests][Test]
     */
    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody testDtos: List<TestDto>, @RequestParam executionId: Long): List<Test> =
            doInitializeTests(testDtos, executionId)

    /**
     * @param testDtos list of [TestDto]s to save into the DB
     * @param executionId ID of the [Execution], during which these tests will be initiliazed and executed
     */
    @PostMapping("/initializeAndExecuteTests")
    fun initializeAndExecuteTests(@RequestBody testDtos: List<TestDto>, @RequestParam executionId: Long) {
        val tests = doInitializeTests(testDtos, executionId)
        doExecuteTests(tests, executionId)
    }

    /**
     * @param testDtos list of [TestDto]s to save into the DB
     * @param executionId ID of the [Execution], during which these tests will be initiliazed and executed
     */
    @PostMapping("/executeTests")
    fun executeTests(@RequestBody testDtos: List<TestDto>, @RequestParam executionId: Long) {
        val tests = doInitializeTests(testDtos, executionId)
        doExecuteTests(tests, executionId)
    }

    private fun doInitializeTests(testDtos: List<TestDto>, executionId: Long): List<Test> {
        log.debug { "Received the following tests for initialization under executionId=$executionId: $testDtos" }
        return meterRegistry.timer("save.backend.saveTests").record<List<Test>> {
            testService.saveTests(testDtos)
        }!!
    }

    private fun doExecuteTests(tests: List<Test>, executionId: Long) {
        log.debug { "Received the following test ids for saving test execution under executionId=$executionId: $tests" }
        meterRegistry.timer("save.backend.saveTestExecution").record {
            testExecutionService.updateExecutionAndSaveTestExecutions(executionId, tests)
        }
    }

    /**
     * @param testSuiteId ID of the [TestSuite], for which all corresponding tests will be returned
     * @return list of tests
     */
    @GetMapping("/getTestsByTestSuiteId")
    fun getTestsByTestSuiteId(@RequestParam testSuiteId: Long): List<Test> = testService.findTestsByTestSuiteId(testSuiteId)

    /**
     * @param executionId ID of the [Execution][com.saveourtool.save.entities.Execution], for which all corresponding tests will be returned
     * @return list of tests
     */
    @GetMapping("/getTestsByExecutionId")
    fun getTestsByExecutionId(@RequestParam executionId: Long): List<Test> = testService.findTestsByExecutionId(executionId)

    /**
     * @param executionId ID of the [Execution], during which these tests will be executed
     * @param testSuiteId ID of the [TestSuite], for which there will be created execution in DB
     */
    @Suppress("UnsafeCallOnNullableType")
    @PostMapping("/saveTestExecutionsForStandardByTestSuiteId")
    fun saveTestExecutionsForStandardByTestSuiteId(@RequestBody executionId: Long, @RequestParam testSuiteId: Long) {
        val tests = testService.findTestsByTestSuiteId(testSuiteId)
        doExecuteTests(tests, executionId)
    }

    /**
     * @param executionId ID of the [Execution], during which these tests will be executed
     * @param testSuiteName name of the [TestSuite], for which there will be created execution in DB
     */
    @Suppress("UnsafeCallOnNullableType")
    @PostMapping("/saveTestExecutionsForStandardByTestSuiteName")
    fun saveTestExecutionsForStandardByTestSuiteName(@RequestBody executionId: Long, @RequestParam testSuiteName: String) {
        val tests = testSuitesService.findStandardTestSuitesByName(testSuiteName).flatMap {
            testService.findTestsByTestSuiteId(it.id!!)
        }
        doExecuteTests(tests, executionId)
    }

    /**
     * @param agentId
     * @return test batches
     */
    @GetMapping("/getTestBatches")
    fun testBatches(@RequestParam agentId: String) = testService.getTestBatches(agentId)

    companion object {
        private val log: Logger = getLogger<TestController>()
    }
}
