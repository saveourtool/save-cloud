package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TestInitializeService {
    @Autowired
    private lateinit var testRepository: TestRepository

    fun saveTests(tests: List<Test>) {
        testRepository.saveAll(tests)
    }
}