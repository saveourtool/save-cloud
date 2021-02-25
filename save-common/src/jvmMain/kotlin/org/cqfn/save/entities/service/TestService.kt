package org.cqfn.save.entities.service

import org.cqfn.save.entities.TestFile
import org.cqfn.save.entities.repository.TestRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class TestService(private val testRepository: TestRepository): ITestService {

    override fun add(testFile: TestFile) {
        TODO("Not yet implemented")
    }

    override fun findById(id: Int): Mono<TestFile> {
        TODO("Not yet implemented")
    }

    override fun findAllByTestName(name: String): Flux<TestFile> {
        TODO("Not yet implemented")
    }
}