package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestExecutionService
import org.cqfn.save.backend.service.TestService
import org.cqfn.save.test.TestDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 *  Controller used to initialize tests
 */
@RestController
class TestController {
    @Autowired
    private lateinit var testService: TestService

    @Autowired
    private lateinit var testExecutionService: TestExecutionService

    /**
     * @param testDtos list of [TestDto]s to save into the DB
     * @param executionId ID of the [Execution], during which these tests will be executed
     */
    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody testDtos: List<TestDto>, @RequestParam(required = false) executionId: Long?) {
        log.debug("Received the following tests for initialization under executionId=$executionId: $testDtos")
        val testsIds = testService.saveTests(testDtos)
        executionId?.let { testExecutionService.saveTestExecution(executionId, testsIds) }
    }

    /**
     * @param agentId
     * @return test batches
     */
    @GetMapping("/getTestBatches")
    fun testBatches(@RequestParam agentId: String) = testService.getTestBatches(agentId)

    companion object {
        private val log = LoggerFactory.getLogger(TestController::class.java)
    }
}
