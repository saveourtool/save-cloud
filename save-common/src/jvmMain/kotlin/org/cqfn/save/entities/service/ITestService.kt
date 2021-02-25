package org.cqfn.save.entities.service

import org.cqfn.save.entities.TestFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ITestService {
    fun add(testFile: TestFile)
    fun findById(id: Int): Mono<TestFile>
    fun findAllByTestName(name: String): Flux<TestFile>
}