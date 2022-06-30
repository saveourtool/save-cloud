package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import org.springframework.stereotype.Repository

/**
 * Repository of [TestSuitesSource]
 */
@Repository
interface TestSuitesSourceRepository : BaseEntityRepository<TestSuitesSource> {
    /**
     * @param name
     * @return found entity or null
     */
    fun findByName(name: String): TestSuitesSource?

    /**
     * @param locationId
     * @return found entity or null
     */
    fun findByLocationId(locationId: Long): TestSuitesSource?
}
