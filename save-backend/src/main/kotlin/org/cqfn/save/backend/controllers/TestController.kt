package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.TestService
import org.cqfn.save.entities.Test
import org.cqfn.save.test.TestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 *  Controller used to initialize tests
 */
@RestController
class TestController {
    @Autowired
    private lateinit var testService: TestService

    /**
     * @param tests
     */
    @PostMapping("/initializeTests")
    fun initializeTests(@RequestBody tests: List<Test>) {
        testService.saveTests(tests)
    }

    @GetMapping("/getTestBatches")
    fun testBatches(): Mono<List<TestDto>> {
        return testService.getTestBatches()
    }
}
