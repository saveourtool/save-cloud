package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSourceLog
import java.util.*

/**
 * Repository for [TestSuitesSourceLog]
 */
interface TestSuitesSourceLogRepository : BaseEntityRepository<TestSuitesSourceLog> {
    /**
     * @param sourceId
     * @param version
     * @return [TestSuitesSourceLog] is found by provided values or empty
     */
    fun findBySourceIdAndVersion(sourceId: Long, version: String): Optional<TestSuitesSourceLog>
}
