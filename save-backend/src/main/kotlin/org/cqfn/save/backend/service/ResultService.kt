package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ResultRepository
import org.cqfn.save.entities.Result
import org.springframework.stereotype.Service

/**
 * Service for test result
 */
@Service
class ResultService(private val resultRepository: ResultRepository) {
    /**
     * @param testResults list of test result
     */
    fun addResults(testResults: List<Result>) {
        resultRepository.saveAll(testResults)
    }
}
