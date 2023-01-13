package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.service.TestService
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.test.TestDto
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.trace

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 *  Controller used to initialize tests
 */
@RestController
@RequestMapping("/internal")
class TestController(
    private val testService: TestService,
    private val meterRegistry: MeterRegistry,
    private val testSuitesSourceService: TestSuitesSourceService,
) {
    /**
     * @param testDtos list of [TestDto]s to save into the DB
     */
    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody testDtos: List<TestDto>) {
        log.debug { "Received ${testDtos.size} tests for initialization" }
        log.trace { "Received the following tests for initialization: $testDtos" }
        meterRegistry.timer("save.backend.saveTests").record {
            testService.saveTests(testDtos)
        }
    }

    /**
     * @param testFilesRequest
     * @return [TestFilesContent] filled with test files
     */
    @PostMapping("/tests/get-content")
    fun getContent(@RequestBody testFilesRequest: TestFilesRequest): Mono<TestFilesContent> =
            testSuitesSourceService.getTestContent(testFilesRequest)

    companion object {
        private val log: Logger = getLogger<TestController>()
    }
}
