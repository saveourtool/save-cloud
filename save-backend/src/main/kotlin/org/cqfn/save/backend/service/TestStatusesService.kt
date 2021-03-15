package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestStatusRepository
import org.cqfn.save.entities.TestStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("MISSING_KDOC_TOP_LEVEL")
@Service
open class TestStatusesService {
    @Autowired
    private lateinit var testStatusRepository: TestStatusRepository

    /**
     * @param statuses
     */
    fun saveTestStatuses(statuses: List<TestStatus>) {
        for (status in statuses) {
            testStatusRepository.save(status)
        }
    }
}
