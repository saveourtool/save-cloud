package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.preprocessor.service.TestDiscoveringService
import com.saveourtool.save.test.TestFilesContent
import com.saveourtool.save.test.TestFilesRequest

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * A Spring controller for managing downloaded resources
 */
@RestController
class DownloadedResourcesController(
    private val testDiscoveringService: TestDiscoveringService,
) {
    /**
     * @param testFilesRequest
     * @return [TestFilesContent] filled with test files
     */
    @PostMapping("/getTest")
    @Suppress("UnsafeCallOnNullableType")
    fun getTest(@RequestBody testFilesRequest: TestFilesRequest): Mono<TestFilesContent> = Mono.fromCallable {
        testDiscoveringService.getPublicTestFiles(testFilesRequest)
    }
}
